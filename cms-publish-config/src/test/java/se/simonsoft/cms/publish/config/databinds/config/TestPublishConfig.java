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
package se.simonsoft.cms.publish.config.databinds.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import junit.framework.TestCase;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;

public class TestPublishConfig extends TestCase {
	private ObjectReader reader;
	private ObjectWriter writer;

	@BeforeClass
	protected void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader().forType(PublishConfig.class);
		writer = mapper.writer().forType(PublishJob.class);
	}
	@Test
	public void testJsonDeserialization() throws JsonParseException, JsonMappingException, IOException {
		PublishConfig jsonPc = new PublishConfig();
		jsonPc = reader.readValue(getPublishConfigAsString());

		assertEquals("velocity-stuff.pdf", jsonPc.getAreas().get(0).getPathnameTemplate());
		assertEquals("*", jsonPc.getProfilingNameInclude().get(0));
		assertEquals("Review", jsonPc.getStatusInclude().get(0));
		assertEquals("Released", jsonPc.getStatusInclude().get(1));
		assertEquals(true, jsonPc.isActive());
		assertEquals(true, jsonPc.isVisible());
		assertEquals("Info in dialogs.", jsonPc.getDescription());
		assertEquals("abxpe", jsonPc.getOptions().getType());
		assertEquals("pdf", jsonPc.getOptions().getFormat());
		assertEquals("file.css", jsonPc.getOptions().getParams().get("stylesheet"));
		assertEquals("file.pdf", jsonPc.getOptions().getParams().get("pdfconfig"));
		assertEquals("great", jsonPc.getOptions().getParams().get("whatever"));
		assertEquals("s3", jsonPc.getOptions().getStorage().getType());
		assertEquals("parameter for future destination types", jsonPc.getOptions().getStorage().getParams().get("specific"));
		assertEquals("future stuff", jsonPc.getOptions().getPostprocess().getType());
		assertEquals("parameter for future destination types", jsonPc.getOptions().getPostprocess().getParams().get("specific"));
		assertEquals("webhook", jsonPc.getOptions().getDelivery().getType());
	}

	@Test
	public void testUnkownProperty() {
		//Getting data from Json
		try {
			@SuppressWarnings("unused")
			PublishConfig jsonPc = reader.readValue(getPublishConfig2AsString());
			fail("Expectected UnrecognizedPropertyException to be thrown");
			
		}catch(UnrecognizedPropertyException e){
			assertNotNull(e);
		}catch (JsonParseException e) {
			assertNotNull(e);
			e.printStackTrace();
		}catch(JsonMappingException e) {
			assertNotNull(e);
			e.printStackTrace();
		}catch(IOException e) {
			assertNotNull(e);
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetJobFromConfig() throws Exception {
		
		PublishConfig config = new PublishConfig();
		config = reader.readValue(getPublishConfigAsString());
		PublishJob job = new PublishJob(config);
		job.setArea(config.getAreas().get(0));
		
		assertEquals("velocity-stuff.pdf", job.getArea().getPathnameTemplate());
		assertEquals("*", job.getProfilingNameInclude().get(0));
		assertEquals("Review", job.getStatusInclude().get(0));
		assertEquals("Released", job.getStatusInclude().get(1));
		assertEquals(true, job.isActive());
		assertEquals(true, job.isVisible());
		assertNull(job.getDescription());
		assertEquals("abxpe", job.getOptions().getType());
		assertEquals("pdf", job.getOptions().getFormat());
		assertEquals("file.css", job.getOptions().getParams().get("stylesheet"));
		assertEquals("file.pdf", job.getOptions().getParams().get("pdfconfig"));
		assertEquals("great", job.getOptions().getParams().get("whatever"));
		assertEquals("s3", job.getOptions().getStorage().getType());
		assertNull("s3 storage adds bucket param, in cms-publish-rest", job.getOptions().getStorage().getParams().get("s3bucket"));
		assertEquals("parameter for future destination types", job.getOptions().getStorage().getParams().get("specific"));
		assertEquals("future stuff", job.getOptions().getPostprocess().getType());
		assertEquals("parameter for future destination types", job.getOptions().getPostprocess().getParams().get("specific"));
		assertEquals("webhook", job.getOptions().getDelivery().getType());
		
		String jobJson = writer.writeValueAsString(job);
		
		String expectedJobJson = "{\"active\":true,\"visible\":true,\"exportable\":true,\"statusInclude\":[\"Review\",\"Released\"],\"elementNameInclude\":null,\"typeInclude\":null,\"profilingInclude\":true,\"profilingNameInclude\":[\"*\"],\"areaMainInclude\":false,\"areas\":[]," + 
				"\"options\":{\"type\":\"abxpe\",\"format\":\"pdf\"," +
				"\"params\":{\"stylesheet\":\"file.css\",\"pdfconfig\":\"file.pdf\",\"whatever\":\"great\"}," +
				"\"manifest\":{\"job\":{},\"document\":{}}," + 
				"\"storage\":{\"type\":\"s3\",\"params\":{\"specific\":\"parameter for future destination types\"},\"pathversion\":null,\"pathcloudid\":null,\"pathconfigname\":null,\"pathdir\":null,\"pathnamebase\":null}," + 
				"\"preprocess\":{\"type\":null,\"params\":{}},\"postprocess\":{\"type\":\"future stuff\",\"params\":{\"specific\":\"parameter for future destination types\"}}," +
				"\"delivery\":{\"type\":\"webhook\",\"params\":{\"url\":\"https://target.example.com/something?secret=super\",\"presign\":\"true\"},\"headers\":{\"headername\":\"headerValue\"}}," +
				"\"pathname\":null," +
				"\"source\":null," +
				"\"profiling\":null," +
				"\"progress\":{\"params\":{}}" + 
				/*
				"\"paramsNameValue\":[{\"Name\":\"stylesheet\",\"Value\":\"file.css\"},{\"Name\":\"pdfconfig\",\"Value\":\"file.pdf\"},{\"Name\":\"whatever\",\"Value\":\"great\"}]" +
				*/
				"}," + 
				"\"configname\":null,\"type\":null,\"action\":null,\"area\":{\"type\":null,\"pathnameTemplate\":\"velocity-stuff.pdf\",\"docnoDocumentTemplate\":null,\"docnoMasterTemplate\":null},\"itemid\":null,\"userid\":null}";
		assertEquals("full job JSONs",  expectedJobJson, jobJson);
	}
	
	private String getPublishConfigAsString() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/config/databinds/config/publish-config.json";
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
	
	private String getPublishConfig2AsString() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/config/databinds/config/publish-config2.json";
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
