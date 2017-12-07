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
package se.simonsoft.cms.publish.config.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigDelivery;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigManifest;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigOptions;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigStorage;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class PublishManifestTemplateTest {

	private ObjectMapper mapper = new ObjectMapper();
	
	private ObjectReader readerJob = mapper.reader(PublishJob.class);
	private ObjectReader readerManifest = mapper.reader(PublishConfigManifest.class); // TODO: PublishJobManifest.class
	//private String pathJobStatus = "se/simonsoft/cms/publish/config/filter/publish-job-status.json";
	private String pathManifestJson = "se/simonsoft/cms/publish/config/manifest/manifest-json.vm";
	
	@Test
	public void testManifestJson() throws Exception {
		// TODO: Refactor into demonstration of alternative manifest formats, e.g. XML. 
		
		PublishJob job = getPublishJob1();
		PublishJobOptions o = job.getOptions();
		PublishConfigManifest m = job.getOptions().getManifest();
		
		assertNotNull(m);
		
		m.getCustomTemplates().put("apa", "banan");
		assertEquals(1, m.getCustomTemplates().keySet().size());
		m.getCustomTemplates().put("bil", "bmw/audi");
		assertEquals(2, m.getCustomTemplates().keySet().size());
		
		String vtl = getTestData(pathManifestJson);
		PublishConfigTemplateString pcts = new PublishConfigTemplateString();
		pcts.withEntry("options", o);
		pcts.withEntry("manifest", m);
		pcts.withEntry("jobid", "uuid");
		
		String result = pcts.evaluate(vtl);
		System.out.println(result);
		
		assertEquals("{  \"job\": {    \"id\": \"uuid\",    \"format\": \"pdf\"  },  \"custom\": {    \"apa\": \"banan\",    \"bil\": \"bmw/audi\"  }}", result);
		
		PublishConfigManifest manifestReparsed = readerManifest.readValue(result);
		assertNotNull(manifestReparsed);
	}
	
	
	private PublishJob getPublishJob1() {
		
		PublishConfigOptions co = new PublishConfigOptions();
		co.setFormat("pdf");
		co.setManifest(new PublishConfigManifest());
		co.setDelivery(new PublishConfigDelivery());
		co.setStorage(new PublishConfigStorage());
		co.setType("abxpe");
		
		PublishConfig c = new PublishConfig();
		c.setActive(true);
		c.setOptions(co);
		
		
		
		PublishJob pj = new PublishJob(c);
		pj.getOptions().setManifest(new PublishConfigManifest());
		
		return pj;
		
	}
	
	
	private String getTestData(String path) throws FileNotFoundException, IOException {
		
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			out.append(line);
		}
		br.close();
		return out.toString();
	}

}
