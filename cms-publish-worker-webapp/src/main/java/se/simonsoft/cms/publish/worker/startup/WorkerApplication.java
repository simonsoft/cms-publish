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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
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
import se.simonsoft.cms.version.CmsComponentVersion;
import se.simonsoft.cms.version.CmsComponentVersionManifest;
import se.simonsoft.cms.version.CmsComponents;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

public class WorkerApplication extends ResourceConfig {

	private final Environment environment = new Environment();

	private static final String AWS_ACTIVITY_NAME = "abxpe";
	private static final String PUBLISH_FS_PATH_ENV = "PUBLISH_FS_PATH";
	private static final String PUBLISH_S3_BUCKET_ENV = "PUBLISH_S3_BUCKET";
	private static final String BUCKET_NAME = "cms-automation";

	private final CmsExportPrefix exportPrefix = new CmsExportPrefix("cms4");

	private Region region = DefaultAwsRegionProviderChain.builder().build().getRegion();
	private String cloudId;
	private String awsAccountId;
	private AwsCredentialsProvider credentials = DefaultCredentialsProvider.create();
	private AWSCredentialsProvider credentialsLegacy = DefaultAWSCredentialsProviderChain.getInstance();
	private String bucketName = BUCKET_NAME;
	private ServletContext context;

	private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

	private static CmsComponentVersion webappVersion;

	public WorkerApplication(@Context ServletContext context)  {

		this.context = context;
		logger.info("Worker Webapp starting with context: " + context);

		setWebappVersion(context);
		context.setAttribute("buildName", getWebappVersionString());
		CmsComponents.logAllVersions();

		register(new AbstractBinder() {

			@Override
            protected void configure() {

				String aptapplicationPrefix = getAptapplicationPrefix();

            	WorkerStatusReport workerStatusReport = new WorkerStatusReport();
            	bind(workerStatusReport).to(WorkerStatusReport.class);

            	String envBucket = environment.getParamOptional(PUBLISH_S3_BUCKET_ENV);
            	if (envBucket != null) {
            		logger.debug("Will use bucket: {} specified in environment", envBucket);
            		bucketName = envBucket;
            	}
            	bind(bucketName).named("config:se.simonsoft.cms.publish.bucket").to(String.class);
            	cloudId = environment.getParamOptional("CLOUDID");
            	if (region == null) {
            		// fallback to the hard-coded region name for backwards compatibility
            		region = Region.EU_WEST_1;
            	}
            	logger.info("Region name: {}", region.id());
            	bind(region).to(Region.class);
            	awsAccountId = getAwsAccountId(credentialsLegacy, region);

            	bind(new PublishServicePe()).to(PublishServicePe.class);
            	PublishServicePe publishServicePe = new PublishServicePe();
            	PublishJobService publishJobService = new PublishJobService(publishServicePe, aptapplicationPrefix, credentials, region);
            	bind(publishJobService).to(PublishJobService.class);

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
            	exportProviders.put("s3", new CmsExportProviderAwsSingle(exportPrefix, cloudId, bucketName, region, credentials));
            	bind(exportProviders).to(Map.class);

            	//Bind AWS client
            	ClientConfiguration clientConfiguration = new ClientConfiguration();
        		clientConfiguration.setSocketTimeout((int)TimeUnit.SECONDS.toMillis(70));
            	AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
        				.withRegion(Regions.fromName(region.id()))
        				.withCredentials(credentialsLegacy)
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
					.withRegion(region.id())
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

	private void setWebappVersion(ServletContext context) {
		InputStream manifestIn = context.getResourceAsStream("/META-INF/MANIFEST.MF");
		if (manifestIn != null) {
			Manifest mf = null;
			try {
				mf = new Manifest(manifestIn);
				webappVersion = new CmsComponentVersionManifest(mf);
				logger.info("Build: {}", webappVersion);
			} catch (IOException e) {
				logger.debug("Failed to read webapp manifest", e.getMessage());
			}
		}
	}

	public static String getWebappVersionString() {
		if (webappVersion != null) {
			return webappVersion.toString();
		}
		return "dev";
	}

}
