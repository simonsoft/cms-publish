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
package se.simonsoft.cms.publish.config.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class PublishConfigFilterTest {
	
	private ObjectReader reader = new ObjectMapper().reader(PublishConfig.class);
	
	@Test
	public void testStatusJacksonParse() throws Exception {
		
		PublishConfig publishConfig = getPublishConfigStatus();
		assertTrue(publishConfig.isActive());
		assertTrue(publishConfig.isVisible());
		assertEquals(2, publishConfig.getStatusInclude().size());
		assertTrue(publishConfig.getStatusInclude().contains("Review"));
		assertTrue(publishConfig.getStatusInclude().contains("Released"));
		
		assertNotNull(publishConfig.getOptions());
		assertEquals("abxpe", publishConfig.getOptions().getType());
		assertEquals("pdf", publishConfig.getOptions().getFormat());
		
		assertNotNull(publishConfig.getOptions().getParams());
		assertEquals("axdocbook.style", publishConfig.getOptions().getParams().get("stylesheet"));
		assertEquals("smallfile.pdfcf", publishConfig.getOptions().getParams().get("pdfconfig"));
		
		assertNotNull(publishConfig.getOptions().getStorage());
		assertEquals("s3", publishConfig.getOptions().getStorage().getType());
		
	}
	
	@Test
	public void testStatusFilter() throws Exception {
		PublishConfig publishConfig = getPublishConfigStatus();
		PublishConfigFilter filter = new PublishConfigFilterStatus();
		
		CmsItem itemMockReleased = mock(CmsItem.class);
		when(itemMockReleased.getStatus()).thenReturn("Released");
		assertTrue(filter.accept(publishConfig, itemMockReleased));
		
		CmsItem itemMockReview = mock(CmsItem.class);
		when(itemMockReview.getStatus()).thenReturn("Review");
		assertTrue(filter.accept(publishConfig, itemMockReview));
		
		CmsItem itemMockInWork = mock(CmsItem.class);
		when(itemMockInWork.getStatus()).thenReturn("In_work");
		assertFalse(filter.accept(publishConfig, itemMockInWork));
		
		publishConfig.setStatusInclude(new ArrayList<String>());
		assertFalse(filter.accept(publishConfig, itemMockReview));
		
		publishConfig.setStatusInclude(null);
		assertTrue(filter.accept(publishConfig, itemMockReview));
		
	}
	
	@Test
	public void testActiveFilter() throws Exception {
		PublishConfig publishConfig = getPublishConfigStatus();
		PublishConfigFilter filter = new PublishConfigFilterActive();
		assertTrue(filter.accept(publishConfig, null));
	}
	
	@Test
	public void testTypeFilter() throws Exception {
		PublishConfig publishConfig = getPublishConfigStatus();
		PublishConfigFilter filter = new PublishConfigFilterType();
		
		CmsItem itemMockTypeAbxpe = mock(CmsItem.class);
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("embd_xml_a_type", "abxpe");
		when(itemMockTypeAbxpe.getMeta()).thenReturn(meta);
		assertTrue(filter.accept(publishConfig, itemMockTypeAbxpe));
		
		CmsItem itemMockNoType = mock(CmsItem.class);
		Map<String, Object> emptyMeta = new HashMap<String, Object>();
		when(itemMockNoType.getMeta()).thenReturn(emptyMeta);
		assertFalse(filter.accept(publishConfig, itemMockNoType));
		
	}
	
	
	private PublishConfig getPublishConfigStatus() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/config/filter/publish-config-status.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			out.append(line);
		}
		br.close();
		return reader.readValue(out.toString());
	}

}
