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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishJobStorageFactoryTest {
	
	private final String configName = "simple-pdf";
	private final String bucketName = "cms-automation";
	private CmsItemPublish mockItem;
	
	@Before
	public void setup() {
		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/900108.xml"));
		CmsItemId itemId = itemIdArg.withPegRev(100L);
		mockItem = mock(CmsItemPublish.class);
		when(mockItem.getId()).thenReturn(itemId);
	}

	@Test
	public void testPublishJobStorageInstance() throws Exception {
		
		PublishConfigStorage cs = new PublishConfigStorage();
		cs.setType("s3");
		
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId", bucketName);
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName, null);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		assertEquals("s3", s.getType());
		assertEquals("cms-automation", s.getParams().get("s3bucket"));
		assertEquals("s3://cms-automation/cms4/cloudId/simple-pdf/vvab/xml/documents/900108.xml/", s.getParams().get("s3urlbase"));		
	}
	
	@Test
	public void testPublishJobStorageTypeFS() {
		
		PublishConfigStorage cs = new PublishConfigStorage();
		cs.setType("fs");
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId", bucketName);
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName, null);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		assertEquals("fs", s.getType());
		assertEquals(null, s.getParams().get("s3bucket"));
		assertEquals(null, s.getParams().get("s3baseurl"));
	}
	
	@Test
	public void testPublishJobStorageFactoryProfiling() throws Exception {
		
		PublishConfigStorage cs = new PublishConfigStorage();
		cs.setType("fs");
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId", bucketName);
		PublishProfilingRecipe profiling = new PublishProfilingRecipe();
		profiling.setName("test_name");
		profiling.setLogicalexpr("mock");
		
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName, profiling);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100_test_name", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		assertEquals("fs", s.getType());
		assertEquals(null, s.getParams().get("s3bucket"));
	}
	
	@Test
	public void testPublishJobStorageFactoryTypeNull() throws Exception {
		
		PublishConfigStorage cs = new PublishConfigStorage();
		cs.setType(null);
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId", bucketName);
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName, null);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		assertEquals("s3", s.getType());
		assertEquals("cms-automation", s.getParams().get("s3bucket"));	
	}
	
	@Test
	public void testPublishJobStorageFactoryTypeEmptyString() throws Exception {
		
		PublishConfigStorage cs = new PublishConfigStorage();
		cs.setType("");
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId", bucketName);
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName, null);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		assertEquals("s3", s.getType());
		assertEquals("cms-automation", s.getParams().get("s3bucket"));
	}
	
	@Test
	public void testPublishJobStorageFactoryNullConfig() throws Exception {
		
		//No config will default the storage to S3.
		PublishConfigStorage cs = null;
		
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId", bucketName);
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName, null);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		assertEquals("s3", s.getType());
		assertEquals("cms-automation", s.getParams().get("s3bucket"));
		assertEquals("s3://cms-automation/cms4/cloudId/simple-pdf/vvab/xml/documents/900108.xml/", s.getParams().get("s3urlbase"));	
	}
	
}
