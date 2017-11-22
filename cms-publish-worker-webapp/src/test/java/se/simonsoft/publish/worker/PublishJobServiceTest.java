package se.simonsoft.publish.worker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishFormatPDF;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class PublishJobServiceTest {
	
	ObjectMapper mapper = new ObjectMapper();
	ObjectReader reader = mapper.reader(PublishJob.class);
	PublishServicePe pe;
	
	@Test
	public void PublishJobTest() throws JsonProcessingException, IOException, InterruptedException, PublishException  {
		pe = Mockito.mock(PublishServicePe.class);
		PublishFormat format = new PublishFormatPDF();
		PublishJobService service = new PublishJobService(pe);
		PublishJob job = reader.readValue(getJsonString());
		PublishTicket publishTicket = new PublishTicket("2");
		
		when(pe.isCompleted(Mockito.any(PublishTicket.class), Mockito.any(PublishRequestDefault.class))).thenReturn(true);
		when(pe.getPublishFormat(Mockito.anyString())).thenReturn(format);
		when(pe.requestPublish(Mockito.any(PublishRequest.class))).thenReturn(publishTicket);
		
		PublishTicket ticket = service.publishJob(job);
		
		ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class); 
        verify(pe, times(1)).requestPublish(requestCaptor.capture());
        PublishRequest pr = requestCaptor.getValue();
        
        assertEquals("http://localhost:8080", pr.getConfig().get("host"));
        assertEquals("/e3/servlet/e3", pr.getConfig().get("path"));
        assertEquals("yes", pr.getParams().get("zip-output"));
        assertEquals(job.getOptions().getPathname(), pr.getParams().get("zip-root"));
        assertEquals(job.getOptions().getType(), pr.getParams().get("type"));
        assertEquals(job.getOptions().getFormat(), pr.getParams().get("format"));
        assertEquals(job.getOptions().getParams().get("stylesheet"), pr.getParams().get("stylesheet"));
        assertEquals(job.getOptions().getParams().get("pdfconfig"), pr.getParams().get("pdfconfig"));
        assertEquals(job.getOptions().getParams().get("whatever"), pr.getParams().get("whatever"));
        assertEquals(job.getItemid(), pr.getFile().getURI());
        assertEquals(format, pr.getFormat());
        
        ArgumentCaptor<PublishTicket> ticketCaptor = ArgumentCaptor.forClass(PublishTicket.class);
        verify(pe, times(1)).isCompleted(ticketCaptor.capture(), requestCaptor.capture());
        PublishTicket pt = ticketCaptor.getValue();
        
        assertEquals(publishTicket.toString(), pt.toString());
        assertEquals(publishTicket.toString(), ticket.toString());
	}
	
	public String getJsonString() throws IOException {
		String jsonPath = "resources/se/simonsoft/cms/webapp/resources/publish-job-options.json";
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
