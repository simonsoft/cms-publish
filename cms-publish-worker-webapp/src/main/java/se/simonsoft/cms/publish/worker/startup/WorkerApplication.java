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

import java.util.concurrent.TimeUnit;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
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

import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.config.status.report.WorkerStatusReport;
import se.simonsoft.cms.publish.worker.AwsStepfunctionPublishWorker;
import se.simonsoft.cms.publish.worker.PublishJobService;

public class WorkerApplication extends ResourceConfig {
	
	private final Environment environment = new Environment();
	private final String bucketName = "cms-review-jandersson";
	
	private static String AWS_REGION = Regions.EU_WEST_1.getName();
	private static String AWS_ARN_STATE_START = "arn:aws:states";
	private static String AWS_ACTIVITY_NAME = "abxpe";
	
	private String awsId;
	private String awsSecret;
	private String cloudId; 
	private AWSCredentialsProvider credentials;
	
	private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

	public WorkerApplication()  {
		
		System.out.println("WORKER CONFIG");
		
		register(new AbstractBinder() {


			@Override
            protected void configure() {
            	
            	bind(new PublishServicePe()).to(PublishServicePe.class);
            	PublishServicePe publishServicePe = new PublishServicePe();
            	PublishJobService publishJobService = new PublishJobService(publishServicePe);
            	bind(publishJobService).to(PublishJobService.class);
            	WorkerStatusReport workerStatusReport = new WorkerStatusReport();
            	bind(workerStatusReport).to(WorkerStatusReport.class);
            	
            	
            	awsId = environment.getParam("cms.aws.key.id");
            	awsSecret = environment.getParam("cms.aws.key.secret");
            	cloudId = environment.getParam("cms.cloudid");
            	
            	if (isAwsSecretAndId(awsId, awsSecret)) {
            		credentials = getCredentials(awsId, awsSecret);
            	} else {
            		credentials = new DefaultAWSCredentialsProviderChain();
            	}
            	
            	ClientConfiguration clientConfiguration = new ClientConfiguration();
        		clientConfiguration.setSocketTimeout((int)TimeUnit.SECONDS.toMillis(70));
            	AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
        				.withRegion(Regions.EU_WEST_1)
        				.withCredentials(credentials)
        				.withClientConfiguration(clientConfiguration)
        				.build();
            	
            	bind(client).to(AWSStepFunctions.class);
            	
            	//Jackson binding reader for future useage.
        		ObjectMapper mapper = new ObjectMapper();
        		ObjectReader reader = mapper.reader();
        		ObjectWriter writer = mapper.writer();
        		bind(reader).to(ObjectReader.class);
        		bind(writer).to(ObjectWriter.class);
        		
        		//Not the easiest thing to inject a singleton with hk2. We create a instance of it here and let it start it self from its constructor.
        		logger.debug("Starting publish worker...");
        		new AwsStepfunctionPublishWorker(cloudId,
        				bucketName,
        				credentials,
        				reader,
        				writer,
        				client,
        				getAwsArn("activity", AWS_ACTIVITY_NAME),
        				publishJobService,	
        				workerStatusReport);
        		
        		logger.debug("publish worker started.");
            }
        });
		
	}
	
	private String getAwsArn(String type, String name) {

		final String arnDelimiter = ":";
		final String nameDelimiter = "-";
		final String namePrefix = "cms";

		final StringBuilder sb = new StringBuilder(AWS_ARN_STATE_START);
		sb.append(arnDelimiter);
		sb.append(AWS_REGION);
		sb.append(arnDelimiter);
		sb.append(getAwsAccountId(credentials));
		sb.append(arnDelimiter);
		sb.append(type);
		sb.append(arnDelimiter);
		sb.append(namePrefix);
		sb.append(nameDelimiter);
		sb.append(cloudId);
		sb.append(nameDelimiter);
		sb.append(name);
		
		return sb.toString();
	}

	private String getAwsAccountId(AWSCredentialsProvider credentials) {
		AWSSecurityTokenService securityClient = AWSSecurityTokenServiceClientBuilder.standard().withCredentials(credentials).withRegion(AWS_REGION).build();
		GetCallerIdentityRequest request = new GetCallerIdentityRequest();
		GetCallerIdentityResult response = securityClient.getCallerIdentity(request);
		return response.getAccount();

	}
	
	private AWSCredentialsProvider getCredentials(final String id, final String secret) {
        return new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new BasicAWSCredentials(id, secret);
            }

            @Override
            public void refresh() {

            }
        };
    }
	
	private static boolean isAwsSecretAndId(String awsId, String awsSecret) {
        return (awsId != null && !awsId.isEmpty() && awsSecret != null && !awsSecret.isEmpty());
    }
	

}
