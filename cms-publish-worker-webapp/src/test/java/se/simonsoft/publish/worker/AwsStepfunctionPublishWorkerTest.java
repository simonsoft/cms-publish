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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.stepfunctions.AWSStepFunctionsClient;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;
import se.simonsoft.cms.publish.export.PublishJobExportService;

public class AwsStepfunctionPublishWorkerTest {

	private ObjectMapper mapper = new ObjectMapper();
	private ObjectReader reader = mapper.reader();
	private ObjectWriter writer = mapper.writer();
	
	private final String jsonStringWithoutTicket = "resources/se/simonsoft/cms/webapp/resources/publish-job-no-ticket.json";
	private final String jsonStringNotCompletedTicket = "resources/se/simonsoft/cms/webapp/resources/publish-job-not-completed.json";
	private final String jsonStringWithTicketCompleted = "resources/se/simonsoft/cms/webapp/resources/publish-job-has-ticket-completed.json";
	
	@Mock AWSStepFunctionsClient mockClient;
	@Mock GetActivityTaskResult mockTaskResult;
	@Mock PublishJobService mockJobService;
	@Mock PublishJobExportService mockExportService;
	
	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
		
		when(mockTaskResult.getTaskToken()).thenReturn("1923904724");
		when(mockClient.getActivityTask(any(GetActivityTaskRequest.class))).thenReturn(mockTaskResult, null);
	}

	@Test
	public void testNoTicket() throws Exception {

		ObjectReader spyReader = spy(reader);
		
		PublishTicket ticket = new PublishTicket("44");

		when(mockTaskResult.getInput()).thenReturn(getJsonString(this.jsonStringWithoutTicket));
		when(mockJobService.publishJob(any(PublishJobOptions.class))).thenReturn(ticket);

		ArgumentCaptor<SendTaskSuccessRequest> argument = ArgumentCaptor.forClass(SendTaskSuccessRequest.class);

		new AwsStepfunctionPublishWorker(spyReader, writer, mockClient, "any_acitivtyArn", mockJobService, mockExportService);
		Thread.sleep(300);

		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResult, times(2)).getInput();
		verify(spyReader, times(1)).forType(PublishJobOptions.class);
		verify(mockJobService, times(1)).publishJob(any(PublishJobOptions.class));
		verify(mockClient, times(1)).sendTaskSuccess(argument.capture());
		
		SendTaskSuccessRequest value = argument.getValue();
		assertEquals("{\"params\":{\"ticket\":\"44\",\"completed\":\"false\"}}", value.getOutput());
	}

	@Test
	public void testHasTicketNotCompleted() throws Exception {

		ArgumentCaptor<SendTaskFailureRequest> requestCaptor = ArgumentCaptor.forClass(SendTaskFailureRequest.class);
		
		when(mockTaskResult.getInput()).thenReturn(getJsonString(this.jsonStringNotCompletedTicket));
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenReturn(false);
		
		
		new AwsStepfunctionPublishWorker(reader, writer, mockClient, "any_activityArn", mockJobService, mockExportService);
		Thread.sleep(300);
		
		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResult, times(2)).getInput();
		verify(mockClient, times(1)).sendTaskFailure(requestCaptor.capture());
		
		assertEquals("JobPending", requestCaptor.getValue().getError());
	}

	@Test
	public void testHasTicketCompleted() throws InterruptedException, IOException, PublishException {

		ArgumentCaptor<SendTaskSuccessRequest> requestCaptor = ArgumentCaptor.forClass(SendTaskSuccessRequest.class);
		
		when(mockTaskResult.getInput()).thenReturn(getJsonString(this.jsonStringWithTicketCompleted));
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenReturn(true);
		when(mockExportService.exportJob(any(ByteArrayOutputStream.class), any(PublishJobOptions.class))).thenReturn("Filepath on system");
		
		new AwsStepfunctionPublishWorker(reader, writer, mockClient, "any_activityArn", mockJobService, mockExportService);
		Thread.sleep(300);
		
		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResult, times(2)).getInput();
		verify(mockClient, times(1)).sendTaskSuccess(requestCaptor.capture());
		verify(mockExportService, times(1)).exportJob(any(OutputStream.class), any(PublishJobOptions.class));
		
		SendTaskSuccessRequest value = requestCaptor.getValue();
		assertEquals("{\"params\":{\"ticket\":\"1234\",\"completed\":\"true\"}}", value.getOutput());
	}
	
	@Test
	public void testHasTicketButPEHasLostTheJob() throws Exception {
		
		ArgumentCaptor<SendTaskFailureRequest> requestCaptor = ArgumentCaptor.forClass(SendTaskFailureRequest.class);
		
		when(mockTaskResult.getInput()).thenReturn(getJsonString(this.jsonStringNotCompletedTicket));
		//PeService should throw a PublishException if it has lost the job.
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenThrow(new PublishException("Transaction id 1234 is invalid."));
		
		new AwsStepfunctionPublishWorker(reader, writer, mockClient, "any_activityArn", mockJobService, mockExportService);
		Thread.sleep(300);
		
		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResult, times(2)).getInput();
		verify(mockClient, times(1)).sendTaskFailure(requestCaptor.capture());
		
		//TODO: Not implemented "JobMissing", consider risk of infinite loop.
		assertEquals("JobFailed", requestCaptor.getValue().getError());
	}

	public String getJsonString(String path) throws IOException {

		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			out.append(line);
		}
		reader.close();
		return out.toString();

	}
}
