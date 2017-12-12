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
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import junit.framework.TestCase;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;

public class TestPublishConfig extends TestCase {
	private ObjectReader reader;

	@BeforeClass
	protected void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishConfig.class);
	}
	@Test
	public void testJsonDeserialization() throws JsonParseException, JsonMappingException, IOException {
		PublishConfig jsonPc = new PublishConfig();
		jsonPc = reader.readValue(getPublishConfigAsString());

		assertEquals("velocity-stuff.pdf", jsonPc.getAreas().get(0).getPathnameTemplate());
		assertEquals("*", jsonPc.getProfilingInclude().get(0));
		assertEquals("Review", jsonPc.getStatusInclude().get(0));
		assertEquals("Released", jsonPc.getStatusInclude().get(1));
		assertEquals(true, jsonPc.isActive());
		assertEquals(true, jsonPc.isVisible());
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
			PublishConfig jsonPc = new PublishConfig();
			ObjectReader r = new ObjectMapper().reader(PublishConfig.class);
			jsonPc = r.readValue(getPublishConfig2AsString());
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
		assertEquals("*", job.getProfilingInclude().get(0));
		assertEquals("Review", job.getStatusInclude().get(0));
		assertEquals("Released", job.getStatusInclude().get(1));
		assertEquals(true, job.isActive());
		assertEquals(true, job.isVisible());
		assertEquals("abxpe", job.getOptions().getType());
		assertEquals("pdf", job.getOptions().getFormat());
		assertEquals("file.css", job.getOptions().getParams().get("stylesheet"));
		assertEquals("file.pdf", job.getOptions().getParams().get("pdfconfig"));
		assertEquals("great", job.getOptions().getParams().get("whatever"));
		assertEquals("s3", job.getOptions().getStorage().getType());
		assertEquals("parameter for future destination types", job.getOptions().getStorage().getParams().get("specific"));
		assertEquals("future stuff", job.getOptions().getPostprocess().getType());
		assertEquals("parameter for future destination types", job.getOptions().getPostprocess().getParams().get("specific"));
		assertEquals("webhook", job.getOptions().getDelivery().getType());
	}
	
	private String getPublishConfigAsString() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-config.json";
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
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-config2.json";
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