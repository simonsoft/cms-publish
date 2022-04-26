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
package se.simonsoft.cms.publish.rest.config.filter;

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

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class PublishConfigFilterTest {
	
	private final ObjectReader reader = new ObjectMapper().readerFor(PublishConfig.class);
	
	private final String pathConfigPathnamebase = "se/simonsoft/cms/publish/rest/config/filter/publish-config-pathnamebase.json";
	private final String pathConfigSimple = "se/simonsoft/cms/publish/rest/config/filter/publish-config-simple.json";
	private final String pathConfigStatus = "se/simonsoft/cms/publish/rest/config/filter/publish-config-status.json";
	private final String pathConfigType = "se/simonsoft/cms/publish/rest/config/filter/publish-config-type.json";
	private final String pathConfigProfilingAll = "se/simonsoft/cms/publish/rest/config/filter/publish-config-profile-all.json";
	@SuppressWarnings("unused")
	private final String pathConfigProfilingOsx = "se/simonsoft/cms/publish/rest/config/filter/publish-config-profile-osx.json";
	
	
	@Test
	public void testStatusJacksonParse() throws Exception {
		
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigStatus);
		assertTrue(publishConfig.isActive());
		assertTrue(publishConfig.isVisible());
		assertEquals(2, publishConfig.getStatusInclude().size());
		assertTrue(publishConfig.getStatusInclude().contains("Review"));
		assertTrue(publishConfig.getStatusInclude().contains("Released"));
		
		assertNotNull(publishConfig.getOptions());
		assertEquals("abxpe", publishConfig.getOptions().getType());
		assertEquals("pdf", publishConfig.getOptions().getFormat());
		
		assertNotNull(publishConfig.getOptions().getParams());
		assertEquals("$aptpath/application/se.simonsoft.vvab/doctypes/VVAB/vvab.style", publishConfig.getOptions().getParams().get("stylesheet"));
		assertEquals("smallfile.pdfcf", publishConfig.getOptions().getParams().get("pdfconfig"));
		
		assertNotNull(publishConfig.getOptions().getStorage());
		assertEquals("s3", publishConfig.getOptions().getStorage().getType());
		
	}
	
	@Test
	public void testStatusFilter() throws Exception {
		PublishConfig publishConfigStatus = getConfigJsonTestData(pathConfigStatus);
		PublishConfigFilter filter = new PublishConfigFilterStatus();
		
		CmsItem itemMockReleased = mock(CmsItem.class);
		when(itemMockReleased.getStatus()).thenReturn("Released");
		assertTrue(filter.accept(publishConfigStatus, itemMockReleased));
		
		CmsItem itemMockReview = mock(CmsItem.class);
		when(itemMockReview.getStatus()).thenReturn("Review");
		assertTrue(filter.accept(publishConfigStatus, itemMockReview));
		
		CmsItem itemMockInWork = mock(CmsItem.class);
		when(itemMockInWork.getStatus()).thenReturn("In_Work");
		assertFalse(filter.accept(publishConfigStatus, itemMockInWork));
		
		publishConfigStatus.setStatusInclude(new ArrayList<String>());
		assertFalse(filter.accept(publishConfigStatus, itemMockReview));
		
		publishConfigStatus.setStatusInclude(null);
		assertTrue(filter.accept(publishConfigStatus, itemMockReleased));
		assertTrue(filter.accept(publishConfigStatus, itemMockReview));
		assertTrue(filter.accept(publishConfigStatus, itemMockInWork));
	}
	
	@Test
	public void testActiveFilter() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigStatus);
		PublishConfigFilter filter = new PublishConfigFilterActive();
		assertTrue(filter.accept(publishConfig, null));
	}
	
	
	@Test
	public void testAreaMainFilterDefault() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigSimple);
		PublishConfigFilter filter = new PublishConfigFilterAreaMain();
		
		CmsItem itemMock = mock(CmsItem.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		when(itemMock.getProperties()).thenReturn(props);
		assertFalse(new CmsItemPublish(itemMock).isRelease());
		assertFalse(filter.accept(publishConfig, new CmsItemPublish(itemMock)));
		
		CmsItem itemMockRelease = mock(CmsItem.class);
		CmsItemPropertiesMap propsRelease = new CmsItemPropertiesMap("cms:status", "Released");
		propsRelease.and("abx:ReleaseLabel", "B");
		propsRelease.and("abx:AuthorMaster", "Bogus");
		propsRelease.and("abx:ReleaseMaster", "Bogus");
		when(itemMockRelease.getProperties()).thenReturn(propsRelease);
		assertTrue(new CmsItemPublish(itemMockRelease).isRelease());
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMockRelease)));
		itemMockRelease = null;
		propsRelease = null;
		
		CmsItem itemMockTranslation = mock(CmsItem.class);
		CmsItemPropertiesMap propsTranslation = new CmsItemPropertiesMap("cms:status", "Released");
		propsTranslation.and("abx:ReleaseLabel", "B");
		propsTranslation.and("abx:TranslationLocale", "de-DE");
		propsTranslation.and("abx:AuthorMaster", "Bogus");
		propsTranslation.and("abx:TranslationMaster", "Bogus");
		when(itemMockTranslation.getProperties()).thenReturn(propsTranslation);
		assertFalse(new CmsItemPublish(itemMockTranslation).isRelease());
		assertTrue(new CmsItemPublish(itemMockTranslation).isTranslation());
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMockTranslation)));
	}
	
	
	@Test
	public void testAreaMainFilterInclude() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigType);
		PublishConfigFilter filter = new PublishConfigFilterAreaMain();
		
		CmsItem itemMock = mock(CmsItem.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		when(itemMock.getProperties()).thenReturn(props);
		assertFalse(new CmsItemPublish(itemMock).isRelease());
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMock)));
		
		CmsItem itemMockRelease = mock(CmsItem.class);
		CmsItemPropertiesMap propsRelease = new CmsItemPropertiesMap("cms:status", "Released");
		propsRelease.and("abx:ReleaseLabel", "B");
		propsRelease.and("abx:AuthorMaster", "Bogus");
		propsRelease.and("abx:ReleaseMaster", "Bogus");
		when(itemMockRelease.getProperties()).thenReturn(propsRelease);
		assertTrue(new CmsItemPublish(itemMockRelease).isRelease());
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMockRelease)));
	}
	
	
	@Test
	public void testTypeFilter() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigType);
		PublishConfigFilter filter = new PublishConfigFilterType();
		
		CmsItem itemMockTypeOperator = mock(CmsItem.class);
		Map<String, Object> meta = new HashMap<String, Object>();
		meta.put("embd_xml_a_type", "operator");
		when(itemMockTypeOperator.getMeta()).thenReturn(meta);
		assertTrue(filter.accept(publishConfig, itemMockTypeOperator));
		
		CmsItem itemMockTypeDita = mock(CmsItem.class);
		Map<String, Object> metaDita = new HashMap<String, Object>();
		metaDita.put("meta_s_s_xml_a_othermeta_cms-type", "operator");
		when(itemMockTypeDita.getMeta()).thenReturn(metaDita);
		assertTrue(filter.accept(publishConfig, itemMockTypeDita));
		
		CmsItem itemMockNoType = mock(CmsItem.class);
		Map<String, Object> emptyMeta = new HashMap<String, Object>();
		when(itemMockNoType.getMeta()).thenReturn(emptyMeta);
		assertFalse(filter.accept(publishConfig, itemMockNoType));
	}
	
	
	
	@Test
	public void testPathNameBaseFilter() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigPathnamebase);
		PublishConfigFilter filter = new PublishConfigFilterPathnamebaseRegex();
		
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		
		CmsItem itemXml = mock(CmsItem.class);
		CmsItemId itemId108 = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml");
		when(itemXml.getId()).thenReturn(itemId108);
		when(itemXml.getProperties()).thenReturn(props);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemXml)));
		
		CmsItem itemLong = mock(CmsItem.class);
		CmsItemId itemId108long = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108999.xml");
		when(itemLong.getId()).thenReturn(itemId108long);
		when(itemLong.getProperties()).thenReturn(props);
		assertFalse(filter.accept(publishConfig, new CmsItemPublish(itemLong)));
		
		
		CmsItem itemDita = mock(CmsItem.class);
		CmsItemId itemId255 = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900255.dita");
		when(itemDita.getId()).thenReturn(itemId255);
		when(itemDita.getProperties()).thenReturn(props);
		assertFalse(filter.accept(publishConfig, new CmsItemPublish(itemDita)));
	}
	
	
	@Test
	public void testPathextFilter() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigType);
		PublishConfigFilter filter = new PublishConfigFilterPathext();
		
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		
		CmsItem itemXml = mock(CmsItem.class);
		CmsItemId itemIdXml = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml");
		when(itemXml.getId()).thenReturn(itemIdXml);
		when(itemXml.getProperties()).thenReturn(props);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemXml)));
		
		CmsItem itemDita = mock(CmsItem.class);
		CmsItemId itemIdDita = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108.dita");
		when(itemDita.getId()).thenReturn(itemIdDita);
		when(itemDita.getProperties()).thenReturn(props);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemDita)));
		
		CmsItem itemDitamap = mock(CmsItem.class);
		CmsItemId itemIdDitamap = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108.ditamap");
		when(itemDitamap.getId()).thenReturn(itemIdDitamap);
		when(itemDitamap.getProperties()).thenReturn(props);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemDitamap)));
		
		CmsItem itemPng = mock(CmsItem.class);
		CmsItemId itemIdPng = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/graphics/0001.png");
		when(itemPng.getId()).thenReturn(itemIdPng);
		when(itemPng.getProperties()).thenReturn(props);
		assertFalse(filter.accept(publishConfig, new CmsItemPublish(itemPng)));
	}
	
	
	@Test
	public void testProfilingFilterNone() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigSimple);
		PublishConfigFilter filter = new PublishConfigFilterProfiling();
		
		CmsItem itemMock = mock(CmsItem.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		when(itemMock.getProperties()).thenReturn(props);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMock)));
		
		CmsItem itemMockProfiling = mock(CmsItem.class);
		CmsItemPropertiesMap propsProfiling = new CmsItemPropertiesMap("cms:status", "Released");
		propsProfiling.and("abx:Profiling", "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}, {\"name\":\"linux\",\"logicalexpr\":\"%3A\"}]");
		when(itemMockProfiling.getProperties()).thenReturn(propsProfiling);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMockProfiling)));
	}
	
	@Test
	public void testProfilingFilterFalse() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigStatus); // status sample config has "profilingInclude": false
		PublishConfigFilter filter = new PublishConfigFilterProfiling();
		
		CmsItem itemMock = mock(CmsItem.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		when(itemMock.getProperties()).thenReturn(props);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMock)));
		
		CmsItem itemMockProfiling = mock(CmsItem.class);
		CmsItemPropertiesMap propsProfiling = new CmsItemPropertiesMap("cms:status", "Released");
		propsProfiling.and("abx:Profiling", "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}, {\"name\":\"linux\",\"logicalexpr\":\"%3A\"}]");
		when(itemMockProfiling.getProperties()).thenReturn(propsProfiling);
		assertFalse(filter.accept(publishConfig, new CmsItemPublish(itemMockProfiling)));
	}
	
	@Test
	public void testProfilingFilterTrue() throws Exception {
		PublishConfig publishConfig = getConfigJsonTestData(pathConfigProfilingAll);
		PublishConfigFilter filter = new PublishConfigFilterProfiling();
		
		CmsItem itemMock = mock(CmsItem.class);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		when(itemMock.getProperties()).thenReturn(props);
		assertFalse(filter.accept(publishConfig, new CmsItemPublish(itemMock)));
		
		CmsItem itemMockProfiling = mock(CmsItem.class);
		CmsItemPropertiesMap propsProfiling = new CmsItemPropertiesMap("cms:status", "Released");
		propsProfiling.and("abx:Profiling", "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}, {\"name\":\"linux\",\"logicalexpr\":\"%3A\"}]");
		when(itemMockProfiling.getProperties()).thenReturn(propsProfiling);
		assertTrue(filter.accept(publishConfig, new CmsItemPublish(itemMockProfiling)));
	}
	
	
	private PublishConfig getConfigJsonTestData(String path) throws FileNotFoundException, IOException {
		
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(path);
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
