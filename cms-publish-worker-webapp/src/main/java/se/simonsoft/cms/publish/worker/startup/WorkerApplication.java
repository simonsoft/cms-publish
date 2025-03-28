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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import jakarta.ws.rs.core.Context;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

public class WorkerApplication extends ResourceConfig {

	private final Environment environment = new Environment();

	private static final String AWS_ACTIVITY_NAME = "abxpe";
	private static final String PUBLISH_FS_PATH_ENV = "PUBLISH_FS_PATH";
	private static final String PUBLISH_S3_BUCKET_ENV = "PUBLISH_S3_BUCKET";
	private static final String PUBLISH_S3_ACCELERATED_ENV = "PUBLISH_S3_ACCELERATED";
	private static final String BUCKET_NAME_DEFAULT = "cms-automation";

	private final String publishHost = "http://localhost:8080";

	private final CmsExportPrefix exportPrefix = new CmsExportPrefix("cms4");

	private AwsRegionProvider regionProvider = DefaultAwsRegionProviderChain.builder().build();
	private String cloudId;
	private String awsAccountId;
	private AwsCredentialsProvider credentials = DefaultCredentialsProvider.create();
	private String bucketName = BUCKET_NAME_DEFAULT;

	private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

	private static CmsComponentVersion webappVersion;

	public WorkerApplication(@Context ServletContext context)  {

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

            	// configure bucket
            	String envBucket = context.getInitParameter("bucket");
            	if (envBucket == null) {
            		envBucket = environment.getParamOptional(PUBLISH_S3_BUCKET_ENV);
            	}
            	if (envBucket != null) {
            		logger.debug("Bucket specified in environment / context: {}", envBucket);
            		bucketName = envBucket;
            	}
            	bind(bucketName).named("config:se.simonsoft.cms.publish.bucket").to(String.class);
            	
            	// configure cloudid
            	cloudId = context.getInitParameter("cloudId");
           		// context parameter overrides environment variable, enables multiple workers. 
            	if (cloudId == null) {
            		cloudId = environment.getParamOptional("CLOUDID");
            	}
            	
            	// configure AWS credentials
            	final String awsId = context.getInitParameter("aws.accessKeyId");
                final String awsSecret = context.getInitParameter("aws.secretKey");
                // override the DefaultCredentialsProvider if set in context parameters
        		if (isAwsSecretAndId(awsId, awsSecret)) {
        			credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(awsId, awsSecret));
        		}
            	
        		// configure AWS region
        		// currently no reason to override the location of the EC2 VM
        		// on-prem uses the fallback EU_WEST_1.
        		// fallback to the hard-coded region name for backwards compatibility
        		Region region = Region.EU_WEST_1;
        		try {
        			region = regionProvider.getRegion();
        		} catch (SdkClientException e) {
					logger.info("No AWS Region configured, using fallback EU_WEST_1: " + e.getMessage());
				}
        		logger.info("Region name: {}", region.id());
            	bind(region).to(Region.class);
            	awsAccountId = getAwsAccountId(credentials, region);
            	
            	S3Configuration.Builder s3ConfigBuilder = S3Configuration.builder();
            	String accelerated = environment.getParamOptional(PUBLISH_S3_ACCELERATED_ENV);
            	if (accelerated != null) {
            		logger.info("Worker S3 accelerated: {}", accelerated);
        			s3ConfigBuilder.accelerateModeEnabled(Boolean.valueOf(accelerated));
        		}
            	

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
            	exportProviders.put("s3", new CmsExportProviderAwsSingle(exportPrefix, cloudId, bucketName, region, credentials, s3ConfigBuilder.build(), Optional.empty()));
            	bind(exportProviders).to(Map.class);

            	//Bind AWS client
				Duration timeout = Duration.ofSeconds(70);
				ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
						.socketTimeout(timeout);
				SfnClient client = SfnClient.builder()
						.region(region)
						.credentialsProvider(credentials)
						.httpClientBuilder(httpClientBuilder)
						.build();

            	bind(client).to(SfnClient.class);

            	//Jackson binding reader for future usage.
        		ObjectMapper mapper = new ObjectMapper();
        		ObjectReader reader = mapper.reader();
        		ObjectWriter writer = mapper.writer();
        		bind(reader).to(ObjectReader.class);
        		bind(writer).to(ObjectWriter.class);
        		
        		PublishServicePe publishServicePe = new PublishServicePe(publishHost);
        		bind(publishServicePe).to(PublishServicePe.class);
        		PublishJobService publishJobService = new PublishJobService(exportProviders, publishServicePe, aptapplicationPrefix);
        		bind(publishJobService).to(PublishJobService.class);

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

	private String getAwsAccountId(AwsCredentialsProvider credentials, Region region) {

		logger.debug("Requesting aws to get a account Id");

		String accountId = null;
		try {
			StsClient stsClient = StsClient.builder()
					.region(region)
					.credentialsProvider(credentials)
					.build();
			GetCallerIdentityResponse response = stsClient.getCallerIdentity();
			accountId = response.account();
		} catch (Exception e) {
			logger.error("Could not get a AWS account id: {}", e.getMessage());
		}

		logger.debug("Determined AWS account id: {}", accountId);
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
	
	private static boolean isAwsSecretAndId(String awsId, String awsSecret) {
        return (awsId != null && !awsId.isEmpty() && awsSecret != null && !awsSecret.isEmpty());
    }

}
