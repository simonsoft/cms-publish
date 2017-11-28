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
package se.simonsoft.publish.worker;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class WorkerApplication extends ResourceConfig {
	
	
	private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

	public WorkerApplication()  {
		
		System.out.println("WORKER CONFIG");
		
		register(new AbstractBinder() {
            @Override
            protected void configure() {
            	bind("injected").to(String.class).named("TEST");
            	bind(new PublishServicePe()).to(PublishServicePe.class);
            	PublishServicePe publishServicePe = new PublishServicePe();
            	PublishJobService publishJobService = new PublishJobService(publishServicePe);
            	bind(publishJobService).to(PublishJobService.class);
            	
            	ClientConfiguration clientConfiguration = new ClientConfiguration();
        		clientConfiguration.setSocketTimeout((int)TimeUnit.SECONDS.toMillis(70));
            	AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
        				.withRegion(Regions.EU_WEST_1)
        				.withCredentials(getCredentials("AKIAIGW4IM6AQNOBQ2IA", "Nv2h5OVbfvMue5FQbfMoW+0JhftuNRp3OMXstGkC")) // Credentials, has to be injected.
        				.withClientConfiguration(clientConfiguration)
        				.build();
            	
            	bind(client).to(AWSStepFunctions.class);
//            	
//            	//Jackson binding reader for future useage.
        		ObjectMapper mapper = new ObjectMapper();
        		ObjectReader reader = mapper.reader();
        		bind(reader).to(ObjectReader.class);
//            	
//        		//Not the easiest thing to inject a singleton. We create a instance of it here and let it start it self form its constructor.
        		logger.debug("Starting publish worker...");
        		new AwsStepfunctionPublishWorker(reader, client, "arn:aws:states:eu-west-1:148829428743:activity:cms-jandersson-abxpe", publishJobService);
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
	

}
