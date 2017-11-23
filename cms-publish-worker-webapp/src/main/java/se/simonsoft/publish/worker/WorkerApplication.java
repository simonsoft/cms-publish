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

	public WorkerApplication()  {
		
		System.out.println("WORKER CONFIG");
		
		register(new AbstractBinder() {
            @Override
            protected void configure() {
            	bind("injected").to(String.class).named("TEST");
            	bind(new PublishServicePe()).to(PublishServicePe.class);
            	bind(new ObjectMapper()).to(ObjectMapper.class);
            	bind(new PublishRequestDefault()).to(PublishRequestDefault.class);
            	bind(PublishJobService.class); //TODO: This will probably not work.
            	
            	ClientConfiguration clientConfiguration = new ClientConfiguration();
        		clientConfiguration.setSocketTimeout((int)TimeUnit.SECONDS.toMillis(70));
            	AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
        				.withRegion(Regions.EU_WEST_1)
        				.withCredentials(getCredentials("AKIAIGW4IM6AQNOBQ2IA", "Nv2h5OVbfvMue5FQbfMoW+0JhftuNRp3OMXstGkC"))
        				.withClientConfiguration(clientConfiguration)
        				.build();
            	
            	bind(client).to(AWSStepFunctions.class);
            	
            	
            	//Jackson
        		ObjectMapper mapper = new ObjectMapper();
        		bind(mapper.reader()).to(ObjectReader.class);
            	
            	bind(AwsStepfunctionPublishWorker.class).to(Singleton.class); // this will probably not work.
            }
        });
		
		
		
		//register(MyResource.class);
	    //register(new MyProvider());
	    //packages("se.simonsoft.publish.worker");
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
