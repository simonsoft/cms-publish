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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishJobStorageFactoryTest {
	
	private final String configName = "simple-pdf";

	@Test
	public void testPublishJobStorageInstance() throws Exception {
		
		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/900108.xml"));
		CmsItemId itemId = itemIdArg.withPegRev(100L);
		CmsItemPublish mockItem = mock(CmsItemPublish.class);
		when(mockItem.getId()).thenReturn(itemId);
		
		PublishConfigStorage cs = new PublishConfigStorage();
		cs.setType("s3");
		
		PublishJobStorageFactory factory = new PublishJobStorageFactory("cloudId");
		PublishJobStorage s = factory.getInstance(cs ,mockItem, configName);
	
		assertEquals("cloudId", s.getPathcloudid());
		assertEquals("simple-pdf", s.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", s.getPathdir());
		assertEquals("900108_r0000000100", s.getPathnamebase());
		assertEquals("cms4", s.getPathversion());
		
		
	}
	
}
