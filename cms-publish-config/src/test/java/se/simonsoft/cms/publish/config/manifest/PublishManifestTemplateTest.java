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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigDelivery;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigManifest;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigOptions;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishManifestTemplateTest {

	private ObjectMapper mapper = new ObjectMapper();
	
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
	
	
	@Test
	public void testVelocityStaticMethods() throws Exception {
		CmsItemPublish item = mock(CmsItemPublish.class);
		when(item.getReleaseLabel()).thenReturn("BA");
		when(item.getRevisionChanged()).thenReturn(new RepoRevision(123, null));
		
		
		PublishConfigTemplateString pcts = new PublishConfigTemplateString();
		pcts.withEntry("item", item);
		pcts.withEntry("int", Integer.valueOf(33));
		pcts.withEntry("char", Character.valueOf('C'));
		
		assertEquals(10, Character.getNumericValue('A'));
		assertEquals("10", Integer.toString(Character.getNumericValue('A')));
		assertEquals("33", pcts.evaluate("${Integer.toString($int)}"));
		assertEquals("2.12", pcts.evaluate("2.${Integer.toString($Character.getNumericValue($char))}"));
		assertEquals("3.11", pcts.evaluate("3.${Character.getNumericValue($item.getReleaseLabel().charAt(0))}"));
		assertEquals("123", pcts.evaluate("${item.getRevisionChanged().getNumber()}"));
		
		assertEquals("000000000123", String.format("%012d", item.getRevisionChanged().getNumber()));
		assertEquals("4.000000000123", pcts.evaluate("4.$String.format(\"%012d\", ${item.getRevisionChanged().getNumber()})"));
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
