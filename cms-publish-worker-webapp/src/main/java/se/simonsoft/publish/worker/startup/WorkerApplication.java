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
package se.simonsoft.publish.worker.startup;

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
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.config.export.PublishJobExportS3Service;
import se.simonsoft.cms.publish.config.status.report.WorkerStatusReport;
import se.simonsoft.publish.worker.AwsStepfunctionPublishWorker;
import se.simonsoft.publish.worker.PublishJobService;

public class WorkerApplication extends ResourceConfig {
	
	private final Environment environment = new Environment();
	
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
            	
            	
            	final String awsId = environment.getVariable("cms.aws.key.id");
            	final String awsSecret = environment.getVariable("cms.aws.key.secret");
            	final String awsCloudId = environment.getVariable("cms.aws.cloud.id");
            	
            	AWSCredentialsProvider credentials;
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
        		
        		//TODO: Bucket should be injected.
        		PublishJobExportS3Service exportService = new PublishJobExportS3Service(awsCloudId, "cms-review-jandersson", credentials, writer);
        		
        		//Not the easiest thing to inject a singleton with hk2. We create a instance of it here and let it start it self from its constructor.
        		logger.debug("Starting publish worker...");
        		new AwsStepfunctionPublishWorker(reader, writer, client, "arn:aws:states:eu-west-1:148829428743:activity:cms-jandersson-abxpe", publishJobService, exportService, workerStatusReport);
        		logger.debug("publish worker started.");
            }
        });
		
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
