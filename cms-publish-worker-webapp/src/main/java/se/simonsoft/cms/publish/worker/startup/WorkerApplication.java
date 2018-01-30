/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.publish.worker.startup;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.export.aws.CmsExportProviderAwsSingle;
import se.simonsoft.cms.item.export.CmsExportPrefix;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportProviderFsSingle;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.worker.AwsStepfunctionPublishWorker;
import se.simonsoft.cms.publish.worker.PublishJobService;
import se.simonsoft.cms.publish.worker.export.CmsExportProviderNotConfigured;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport;

public class WorkerApplication extends ResourceConfig {
	
	private final Environment environment = new Environment();
	
	private static final String AWS_ACTIVITY_NAME = "abxpe";
	private static final String PUBLISH_FS_PATH_ENV = "PUBLISH_FS_PATH";
	private static final String PUBLISH_S3_BUCKET_ENV = "PUBLISH_S3_BUCKET";
	private static final String BUCKET_NAME = "cms-automation";
	
	private final CmsExportPrefix exportPrefix = new CmsExportPrefix("cms4");
	
	private Region region; // The Region class will be in SDK 2.0 (Regions will be removed).
	private String cloudId; 
	private String awsAccountId;
	private AWSCredentialsProvider credentials = DefaultAWSCredentialsProviderChain.getInstance();
	private String bucketName = BUCKET_NAME; 
	
	private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

	public WorkerApplication()  {
		
		logger.info("Worker Webapp starting...");
		
		register(new AbstractBinder() {

			@Override
            protected void configure() {
            	
				String aptapplicationPrefix = getAptapplicationPrefix();
				
            	bind(new PublishServicePe()).to(PublishServicePe.class);
            	
            	PublishServicePe publishServicePe = new PublishServicePe();
            	PublishJobService publishJobService = new PublishJobService(publishServicePe, aptapplicationPrefix);
            	bind(publishJobService).to(PublishJobService.class);
            	
            	WorkerStatusReport workerStatusReport = new WorkerStatusReport();
            	bind(workerStatusReport).to(WorkerStatusReport.class);
            	
            	String envBucket = environment.getParamOptional(PUBLISH_S3_BUCKET_ENV);
            	if (envBucket != null) {
            		logger.debug("Will use bucket: {} specified in environment", envBucket);
            		bucketName = envBucket;
            	}
            	bind(bucketName).named("config:se.simonsoft.cms.publish.bucket").to(String.class);
            	cloudId = environment.getParamOptional("CLOUDID");
            	
            	region = Region.getRegion(Regions.fromName("eu-west-1")); // Currently hardcoded, might need different regions annotated per service. 
        		// Might need to determine which region we are running in for EC2. See SDK 2.0, might have a Region Provider Chain.
        		bind(region).to(Region.class);
            	awsAccountId = getAwsAccountId(credentials, region);
            	
            	String fsParent = environment.getParamOptional(PUBLISH_FS_PATH_ENV);
            	//Bind of export providers.
            	Map<String, CmsExportProvider> exportProviders = new HashMap<>();
            	CmsExportProvider cmsExportProviderFsSingle = null;
            	if (fsParent != null && !fsParent.trim().isEmpty()) {
					cmsExportProviderFsSingle = new CmsExportProviderFsSingle(new File(fsParent));
            	} else {
            		logger.warn("Could not instansiate CmsExportProviderFsSingle, will not be able to export to file system.");
            		cmsExportProviderFsSingle = new CmsExportProviderNotConfigured("fs");
            	}
            	
            	exportProviders.put("fs", cmsExportProviderFsSingle);
            	exportProviders.put("s3", new CmsExportProviderAwsSingle(exportPrefix, cloudId, envBucket, region, credentials));
            	bind(exportProviders).to(Map.class);
            	
            	//Bind AWS client
            	ClientConfiguration clientConfiguration = new ClientConfiguration();
        		clientConfiguration.setSocketTimeout((int)TimeUnit.SECONDS.toMillis(70));
            	AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
        				.withRegion(Regions.EU_WEST_1)
        				.withCredentials(credentials)
        				.withClientConfiguration(clientConfiguration)
        				.build();
            	
            	bind(client).to(AWSStepFunctions.class);
            	
            	//Jackson binding reader for future usage.
        		ObjectMapper mapper = new ObjectMapper();
        		ObjectReader reader = mapper.reader();
        		ObjectWriter writer = mapper.writer();
        		bind(reader).to(ObjectReader.class);
        		bind(writer).to(ObjectWriter.class);

				if (cloudId != null) {
					//Not the easiest thing to inject a singleton with hk2. We create a instance of it here and let it start it self from its constructor.
					logger.debug("Starting publish worker...");
					new AwsStepfunctionPublishWorker(
							exportProviders,
							reader,
							writer,
							client,
							region,
							awsAccountId,
							cloudId,
							AWS_ACTIVITY_NAME,
							publishJobService,
							workerStatusReport);

					logger.debug("publish worker started.");

				} else {
					logger.warn("Deferring AWS Worker startup, CLOUDID not configured.");
				}
            }
        });
		
	}
	
	private String getAwsAccountId(AWSCredentialsProvider credentials, Region region) {

		logger.debug("Requesting aws to get a account Id");

		String accountId = null;
		try {
			AWSSecurityTokenService securityClient = AWSSecurityTokenServiceClientBuilder.standard()
					.withCredentials(credentials)
					.withRegion(region.getName())
					.build();
			GetCallerIdentityRequest request = new GetCallerIdentityRequest();
			GetCallerIdentityResult response = securityClient.getCallerIdentity(request);
			accountId = response.getAccount();
		} catch (Exception e) {
			logger.error("Could not get a AWS account id: {}", e.getMessage());
		}

		logger.debug("Requested aws account id: {}", accountId);
		return accountId;
	}

	
	private String getAptapplicationPrefix() {
		String result = "$aptpath/application";
		
		EnvironmentPathList epl = new EnvironmentPathList(environment);
		if (epl.getPathFirst("APTAPPLICATION") != null) {
			result = epl.getPathFirst("APTAPPLICATION");
		}
		return result;
	}


}
