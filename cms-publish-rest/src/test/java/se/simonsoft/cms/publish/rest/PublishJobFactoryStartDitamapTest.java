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
package se.simonsoft.cms.publish.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

/**
 * CMS-2094: Publish with a supplied ditamap (startditamap).
 */
public class PublishJobFactoryStartDitamapTest {

	private static final String CONFIG = "{\n"
			+ "  \"active\": true,\n"
			+ "  \"visible\": true,\n"
			+ "  \"areas\": [ { \"pathnameTemplate\": \"DOC.pdf\" } ],\n"
			+ "  \"options\": {\n"
			+ "    \"type\": \"ditaot-pdf\",\n"
			+ "    \"format\": \"pdf\",\n"
			+ "    \"preprocess\": { \"type\": \"webapp-export-ditaot\", \"params\": { \"Transform00\": \"x.xsl\" } },\n"
			+ "    \"storage\": { \"type\": \"s3\" }\n"
			+ "  }\n"
			+ "}";

	private static final String CONFIG_NO_PREPROCESS = "{\n"
			+ "  \"active\": true,\n"
			+ "  \"visible\": true,\n"
			+ "  \"areas\": [ { \"pathnameTemplate\": \"DOC.pdf\" } ],\n"
			+ "  \"options\": {\n"
			+ "    \"type\": \"abxpe\",\n"
			+ "    \"format\": \"pdf\",\n"
			+ "    \"storage\": { \"type\": \"s3\" }\n"
			+ "  }\n"
			+ "}";

	private final ObjectMapper mapper = new ObjectMapper();
	private PublishJobFactory factory;
	private CmsItemPublish item;

	@Before
	public void setUp() {
		factory = new PublishJobFactory("cloudId", new PublishJobStorageFactory("cloudId", "cms-automation"));

		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/900108.ditamap"));
		itemIdArg.setHostname("ubuntu-cheftest1.pdsvision.net");
		CmsItemId itemId = itemIdArg.withPegRev(100L);
		item = mock(CmsItemPublish.class);
		when(item.getId()).thenReturn(itemId);
		when(item.isRelease()).thenReturn(false);
		when(item.isTranslation()).thenReturn(false);
		when(item.getProperties()).thenReturn(new CmsItemPropertiesMap("cms:status", "In_Work"));
		HashMap<String, Object> meta = new HashMap<>();
		meta.put("count_elements_topic", 42L);
		when(item.getMeta()).thenReturn(meta);
	}

	@Test
	public void testWithoutStartDitamap() throws Exception {
		PublishConfig config = mapper.readValue(CONFIG, PublishConfig.class);
		PublishJob job = factory.getPublishJob(item, config, "preview-pdf", null, null, Optional.empty(), Optional.empty(), Optional.empty());

		assertFalse(job.getOptions().getPreprocess().getParams().containsKey("startditamap"));
		assertFalse(job.getOptions().getManifest().getJob().containsKey("startditamap"));
		assertEquals("committed ditamap topic count from indexing", "42", job.getOptions().getManifest().getJob().get("topics"));
	}

	@Test
	public void testStartDitamap() throws Exception {
		PublishConfig config = mapper.readValue(CONFIG, PublishConfig.class);
		String ditamap = PublishStartDitamapTest.DITAMAP;
		PublishJob job = factory.getPublishJob(item, config, "preview-pdf", null, null, Optional.empty(), Optional.empty(), Optional.of(ditamap));

		// Routed to preprocess params, retaining configured params.
		assertEquals(ditamap, job.getOptions().getPreprocess().getParams().get("startditamap"));
		assertEquals("x.xsl", job.getOptions().getPreprocess().getParams().get("Transform00"));
		// The config params must not be modified (shared map).
		assertFalse(config.getOptions().getPreprocess().getParams().containsKey("startditamap"));

		// Manifest job section: SHA1 and topics from the supplied map.
		assertEquals(PublishStartDitamap.getSha1Hex(ditamap), job.getOptions().getManifest().getJob().get("startditamap"));
		assertEquals("3", job.getOptions().getManifest().getJob().get("topics"));
		assertEquals("preview-pdf", job.getOptions().getManifest().getJob().get("configname"));

		// Not in the manifest custom section (no docno template, so no custom).
		assertNull(job.getOptions().getManifest().getCustom());

		// Serialized manifest must not contain the ditamap itself.
		String manifestJson = mapper.writeValueAsString(job.getOptions().getManifest());
		assertFalse(manifestJson, manifestJson.contains("x-svn"));
		assertTrue(manifestJson, manifestJson.contains(PublishStartDitamap.getSha1Hex(ditamap)));
	}

	@Test
	public void testStartDitamapTopicsOmittedWhenUnparseable() throws Exception {
		PublishConfig config = mapper.readValue(CONFIG, PublishConfig.class);
		String ditamap = "<map><topicref></map>";
		PublishJob job = factory.getPublishJob(item, config, "preview-pdf", null, null, Optional.empty(), Optional.empty(), Optional.of(ditamap));

		assertEquals(PublishStartDitamap.getSha1Hex(ditamap), job.getOptions().getManifest().getJob().get("startditamap"));
		assertFalse("topics must not fall back to the committed map", job.getOptions().getManifest().getJob().containsKey("topics"));
	}

	@Test
	public void testStartDitamapRequiresPreprocess() throws Exception {
		PublishConfig config = mapper.readValue(CONFIG_NO_PREPROCESS, PublishConfig.class);
		try {
			factory.getPublishJob(item, config, "simple-pdf", null, null, Optional.empty(), Optional.empty(), Optional.of(PublishStartDitamapTest.DITAMAP));
			fail("Expected IllegalArgumentException when the config has no preprocess");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("startditamap"));
		}
	}
}
