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
package se.simonsoft.cms.publish.config.databinds.job;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfigManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;

public class PublishJobManifestTest {

	@SuppressWarnings("unused")
	private static ObjectReader reader;
	private static ObjectWriter writer;

	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishJob.class);
		writer = mapper.writer();
	}

	@Test
	public void testManifestNoTemplates() throws JsonProcessingException {
		
		PublishConfigManifest config = getTestManifestConfig1();
		PublishJobManifest job = getTestManifestJob1(config);
		
		assertNotNull(job.getMetaTemplates());
		assertEquals(1, job.getMetaTemplates().size());
		
		String jobS = writer.writeValueAsString(job);
		assertEquals("{\"type\":\"test\",\"job\":{},\"document\":{},\"meta\":{\"static\":\"value\"}}", jobS);
		
		PublishJobManifest jobP = job.forPublish();
		assertEquals("{\"job\":{},\"document\":{},\"meta\":{\"static\":\"value\"}}", writer.writeValueAsString(jobP));
		
		assertEquals(1, jobP.getMeta().size());
		jobP.getMeta().put("one", "more");
		assertEquals(2, jobP.getMeta().size());
		assertEquals(1, job.getMeta().size());
		
	}

	public static PublishConfigManifest getTestManifestConfig1() {
		
		PublishConfigManifest config = new PublishConfigManifest();
		
		LinkedHashMap<String, String> metaTemplates = new LinkedHashMap<String, String>();
		metaTemplates.put("static", "template");
		config.setMetaTemplates(metaTemplates);
		
		return config;
	}
	
	public static PublishJobManifest getTestManifestJob1(PublishConfigManifest config) {
		
		PublishJobManifest job = new PublishJobManifest(config);
		job.setType("test");
		
		LinkedHashMap<String, String> meta = new LinkedHashMap<String, String>();
		meta.put("static", "value");
		job.setMeta(meta);
		
		return job;
	}
	
}
