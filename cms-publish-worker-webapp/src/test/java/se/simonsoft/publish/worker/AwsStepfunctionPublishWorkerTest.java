package se.simonsoft.publish.worker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.mockito.Matchers.any;

import org.junit.Test;

import com.amazonaws.services.stepfunctions.AWSStepFunctionsClient;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;
import se.simonsoft.cms.publish.export.PublishJobExportService;

public class AwsStepfunctionPublishWorkerTest {
	
	private String publishJobOptionsPath = "resources/se/simonsoft/cms/webapp/resources/publish-job-options.json";

	
	@Test
	public void testNoTicket() throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectReader reader = mapper.reader();
		ObjectReader spyReader = spy(reader);
		ObjectWriter writer = mapper.writer();
		
		AWSStepFunctionsClient mockClient = mock(AWSStepFunctionsClient.class);
		GetActivityTaskResult mockTaskResult = mock(GetActivityTaskResult.class);
		when(mockTaskResult.getTaskToken()).thenReturn("1923904724");
		when(mockTaskResult.getInput()).thenReturn(getJsonString(this.publishJobOptionsPath));
		when(mockClient.getActivityTask(any(GetActivityTaskRequest.class))).thenReturn(mockTaskResult);
		
		PublishJobService mockJobService = mock(PublishJobService.class);
		PublishJobExportService mockExportService = mock(PublishJobExportService.class);
		
		AwsStepfunctionPublishWorker worker = new AwsStepfunctionPublishWorker(spyReader, writer, mockClient, "any_acitivtyArn", mockJobService, mockExportService);
		
		//No ticket
		verify(mockClient, times(1)).getActivityTask(any(GetActivityTaskRequest.class));
		verify(mockTaskResult, times(1)).getInput();
		verify(spyReader, times(1)).forType(PublishJobOptions.class);
		verify(mockJobService, times(1)).publishJob(any(PublishJobOptions.class));
	}
	
	
	@Test
	public void testHasTicketNotCompleted() {
		//TODO Change input(given json) to progress.ticket = some number 
	}
	
	@Test
	public void testHasTicketCompleted() {
		//TODO Change input(given json) to progress.completed = true 
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
