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

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.export.aws.CmsExportProviderAwsSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportProviderFsSingle;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.stream.ByteArrayInOutStream;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishFormatPDF;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class TestPublishJobService {
	
	private static final String aptapplicationPrefix = "bogus";
	
	ObjectMapper mapper = new ObjectMapper();
	ObjectReader reader = mapper.readerFor(PublishJobOptions.class);
	PublishServicePe pe;
	private Map<String, CmsExportProvider> exportProviders = new HashMap<>();
	
	@Mock CmsExportProviderFsSingle mockExportFsProvider;
	@Mock CmsExportProviderAwsSingle mockExportAwsProvider;
	
	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);

		exportProviders.put("fs", mockExportFsProvider);
    	exportProviders.put("s3", mockExportAwsProvider);
	}
	
	@Test
	public void PublishJobTest() throws JsonProcessingException, IOException, InterruptedException, PublishException  {
		pe = Mockito.mock(PublishServicePe.class);
		CmsExportReader exportReader = Mockito.mock(CmsExportReader.class);
		when(mockExportAwsProvider.getReader()).thenReturn(exportReader);
		when(exportReader.getContents()).thenReturn(new ByteArrayInOutStream().getInputStream());
		
		PublishFormat format = new PublishFormatPDF();
		PublishJobService service = new PublishJobService(exportProviders, pe, aptapplicationPrefix);
		PublishJobOptions job = reader.readValue(getJsonString());
		PublishTicket publishTicket = new PublishTicket("2");
		
		when(pe.isCompleted(Mockito.any(PublishTicket.class), Mockito.any(PublishRequestDefault.class))).thenReturn(true);
		when(pe.getPublishFormat(Mockito.anyString())).thenReturn(format);
		when(pe.requestPublish(Mockito.any(PublishRequest.class))).thenReturn(publishTicket);
		
		// Call PublishJobService.
		PublishTicket ticket = service.publishJob(job);
		boolean completed = service.isCompleted(ticket);
		assertTrue(completed);
		
		// Capture Request sent to undelying communication layer (PublishServicePe)
		ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class); 
        verify(pe, times(1)).requestPublish(requestCaptor.capture());
        PublishRequest pr = requestCaptor.getValue();
        
        assertEquals("/e3/servlet/e3", pr.getConfig().get("path"));
        assertEquals("pdf", pr.getFormat().getFormat());
        assertEquals("format/type is handled by setFormat(..)", null, pr.getParams().get("type"));
        assertEquals("yes", pr.getParams().get("zip-output"));
        assertEquals("DOC_900108_Released.pdf", pr.getParams().get("zip-root"));
        assertEquals("bogus/axdocbook.style", pr.getParams().get("stylesheet"));
        assertEquals("DOC_900108_Released.pdf/somepath", pr.getParams().get("pathname"));
        assertEquals("smallfile.pdfcf", pr.getParams().get("pdfconfig"));
        assertEquals(null, pr.getFile().getURI());
        assertEquals("_document.xml", pr.getFile().getInputEntry());
        assertEquals("pdf", pr.getFormat().getFormat());
        
        ArgumentCaptor<PublishTicket> ticketCaptor = ArgumentCaptor.forClass(PublishTicket.class);
        verify(pe, times(1)).isCompleted(ticketCaptor.capture(), requestCaptor.capture());
        PublishTicket pt = ticketCaptor.getValue();
        
        assertEquals(publishTicket.toString(), pt.toString());
        assertEquals(publishTicket.toString(), ticket.toString());
	}
	
	public String getJsonString() throws IOException {
		String jsonPath = "se/simonsoft/cms/publish/worker/publish-job-options.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		resourceAsStream.transferTo(baos);
		return baos.toString();
	}
}
