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
package se.simonsoft.cms.publish.worker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.export.aws.CmsExportProviderAwsSingle;
import se.simonsoft.cms.export.storage.CmsExportAwsReaderSingle;
import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportProviderFsSingle;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.*;

public class AwsStepfunctionPublishWorkerTest {

	private ObjectMapper mapper = new ObjectMapper();
	private ObjectReader reader = mapper.reader();
	private ObjectWriter writer = mapper.writer();
	private Map<String, CmsExportProvider> exportProviders = new HashMap<>();

	private final String jsonStringWithoutTicket = "se/simonsoft/cms/publish/worker/publish-job-no-ticket.json";
	private final String jsonStringNotCompletedTicket = "se/simonsoft/cms/publish/worker/publish-job-not-completed.json";
	private final String jsonStringWithTicketCompleted = "se/simonsoft/cms/publish/worker/publish-job-has-ticket-completed.json";

	private final String cloudId = "test-cloudId";
	private final String awsAccountId = "test-accountid";
	private final String activityName = "abxpe";

	@Mock SfnClient mockClient;
	@Mock GetActivityTaskResponse mockTaskResponse;
	@Mock PublishJobService mockJobService;
	@Mock WorkerStatusReport mockWorkerStatusReport;
	@Mock AwsCredentialsProvider credentials;
	@Mock CmsExportAwsWriterSingle mockExportWriter;
	@Mock SendTaskSuccessResponse mockTaskSuccessResponse;
	@Mock SfnResponseMetadata mockResponseMetadata;
	@Mock CmsExportProviderFsSingle mockExportFsProvider;
	@Mock CmsExportProviderAwsSingle mockExportAwsProvider;
	@Mock CmsExportAwsWriterSingle mockExportAwsWriterSingle;
	@Mock CmsExportAwsReaderSingle mockExportAwsReaderSingle;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);

		exportProviders.put("fs", mockExportFsProvider);
    	exportProviders.put("s3", mockExportAwsProvider);

		when(mockTaskResponse.taskToken()).thenReturn("1923904724");
		when(mockTaskSuccessResponse.responseMetadata()).thenReturn(mockResponseMetadata);
		when(mockResponseMetadata.requestId()).thenReturn("mockRequestId");
		when(mockClient.sendTaskSuccess(any(SendTaskSuccessRequest.class))).thenReturn(mockTaskSuccessResponse);
		when(mockExportAwsProvider.getWriter()).thenReturn(mockExportAwsWriterSingle);
		when(mockExportAwsProvider.getReader()).thenReturn(mockExportAwsReaderSingle);
		when(mockClient.getActivityTask(any(GetActivityTaskRequest.class))).thenAnswer(new Answer<GetActivityTaskResponse>() {

			boolean first = true;

			@Override
			public GetActivityTaskResponse answer(InvocationOnMock invocation) throws Throwable {
				if (first) {
					first = false;
					return mockTaskResponse;
				} else {
					Thread.sleep(60000);
					return null;
				}
			}
		});
	}

	@Test
	public void testNoTicket() throws Exception {

		ObjectReader spyReader = spy(reader);

		PublishTicket ticket = new PublishTicket("44");

		when(mockTaskResponse.input()).thenReturn(getJsonString(this.jsonStringWithoutTicket));
		when(mockJobService.publishJob(any(PublishJobOptions.class))).thenReturn(ticket);
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenReturn(false, true, true);

		ArgumentCaptor<SendTaskSuccessRequest> success = ArgumentCaptor.forClass(SendTaskSuccessRequest.class);
		ArgumentCaptor<SendTaskFailureRequest> failure = ArgumentCaptor.forClass(SendTaskFailureRequest.class);

		new AwsStepfunctionPublishWorker(exportProviders, spyReader, writer, mockClient, Region.EU_WEST_1, awsAccountId, cloudId, activityName, mockJobService, mockWorkerStatusReport);
		Thread.sleep(11000);

		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResponse, times(1)).input();
		verify(spyReader, times(1)).forType(PublishJobOptions.class);
		verify(mockJobService, times(1)).publishJob(any(PublishJobOptions.class));
		verify(mockJobService, times(2)).isCompleted(ticket);

		verify(mockClient, times(0)).sendTaskFailure(failure.capture());
		verify(mockClient, times(1)).sendTaskHeartbeat(any(SendTaskHeartbeatRequest.class));
		verify(mockClient, times(1)).sendTaskSuccess(success.capture());

		SendTaskSuccessRequest value = success.getValue();
		assertEquals("{\"params\":{\"ticket\":\"44\",\"completed\":\"true\",\"pathResult\":\"name-from-cmsconfig-publish/vvab/xml/documents/900108.xml/900108.zip\"}}", value.output());
	}

	@Test // No longer a valid state, only single task workflow supported.
	// TODO: Consider throwing a specific exception.
	public void testHasTicketNotCompleted() throws Exception {

		ArgumentCaptor<SendTaskFailureRequest> requestCaptor = ArgumentCaptor.forClass(SendTaskFailureRequest.class);

		when(mockTaskResponse.input()).thenReturn(getJsonString(this.jsonStringNotCompletedTicket));
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenReturn(false);


		new AwsStepfunctionPublishWorker(exportProviders, reader, writer, mockClient, Region.EU_WEST_1, awsAccountId, cloudId, activityName, mockJobService, mockWorkerStatusReport);
		Thread.sleep(300);

		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResponse, times(1)).input();
		verify(mockClient, times(1)).sendTaskFailure(requestCaptor.capture());

		//The "JobPending" route no longer exists in workflow.
		assertEquals("JobFailed", requestCaptor.getValue().error());
	}

	@Test @Deprecated
	public void testHasTicketCompleted() throws InterruptedException, IOException, PublishException {

		ArgumentCaptor<SendTaskSuccessRequest> requestCaptor = ArgumentCaptor.forClass(SendTaskSuccessRequest.class);

		when(mockTaskResponse.input()).thenReturn(getJsonString(this.jsonStringWithTicketCompleted));
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenReturn(true);

		new AwsStepfunctionPublishWorker(exportProviders, reader, writer, mockClient, Region.EU_WEST_1, awsAccountId, cloudId, activityName, mockJobService, mockWorkerStatusReport);
		Thread.sleep(300);

		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResponse, times(1)).input();
		verify(mockClient, times(1)).sendTaskSuccess(requestCaptor.capture());

		SendTaskSuccessRequest value = requestCaptor.getValue();
		assertEquals("{\"params\":{\"ticket\":\"1234\",\"completed\":\"true\"}}", value.output());
	}

	@Test @Deprecated
	public void testHasTicketButPEHasLostTheJob() throws Exception {

		ArgumentCaptor<SendTaskFailureRequest> requestCaptor = ArgumentCaptor.forClass(SendTaskFailureRequest.class);

		when(mockTaskResponse.input()).thenReturn(getJsonString(this.jsonStringNotCompletedTicket));
		//PeService should throw a PublishException if it has lost the job.
		when(mockJobService.isCompleted(any(PublishTicket.class))).thenThrow(new PublishException("Transaction id 1234 is invalid."));

		new AwsStepfunctionPublishWorker(exportProviders, reader, writer, mockClient, Region.EU_WEST_1, awsAccountId, cloudId, activityName, mockJobService, mockWorkerStatusReport);
		Thread.sleep(300);

		verify(mockClient, times(2)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResponse, times(1)).input();
		verify(mockClient, times(1)).sendTaskFailure(requestCaptor.capture());

		//TODO: Not implemented "JobMissing", consider risk of infinite loop.
		assertEquals("JobFailed", requestCaptor.getValue().error());
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
