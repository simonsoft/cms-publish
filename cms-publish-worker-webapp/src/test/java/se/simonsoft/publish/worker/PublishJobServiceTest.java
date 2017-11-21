package se.simonsoft.publish.worker;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class PublishJobServiceTest {
	
	ObjectMapper mapper = new ObjectMapper();
	ObjectReader reader = mapper.reader(PublishJob.class);
	PublishServicePe pe;
	
	@Test
	public void PublishJobTest() throws JsonProcessingException, IOException, InterruptedException {
		pe = Mockito.mock(PublishServicePe.class);
		Mockito.when(pe.isCompleted(Mockito.any(PublishTicket.class), Mockito.any(PublishRequestDefault.class))).thenReturn(true);
		PublishJobService service = new PublishJobService(pe);
		PublishJob job = reader.readValue(getJsonString());
		PublishRequestDefault request = Mockito.mock(PublishRequestDefault.class);
		
		PublishTicket ticket = service.PublishJob(job);
		
		assertTrue(pe.isCompleted(ticket, request));
	}
	
	public String getJsonString() throws IOException {
		String jsonPath = "resources/se/simonsoft/cms/webapp/resources/publish-job.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
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
