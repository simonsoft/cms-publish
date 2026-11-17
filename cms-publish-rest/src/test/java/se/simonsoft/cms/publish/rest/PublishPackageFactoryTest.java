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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterTranslationLocale;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

public class PublishPackageFactoryTest {
	
	private final CmsItemId releaseId = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/release/B/xml/documents/900108.xml?p=100");
	private final CmsItemId translationDeId = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/lang/de-DE/B/xml/documents/900108.xml?p=101");
	private final CmsItemId translationSvId = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/lang/sv-SE/B/xml/documents/900108.xml?p=102");
	
	private CmsItemLookupReporting lookupReporting;
	private TranslationTracking translationTracking;
	private PublishConfigurationDefault publishConfiguration;
	private PublishConfig config;
	private PublishPackageFactory factory;
	
	@Before
	public void setUp() {
		lookupReporting = mock(CmsItemLookupReporting.class);
		translationTracking = mock(TranslationTracking.class);
		publishConfiguration = mock(PublishConfigurationDefault.class);
		
		CmsItem release = mockItem(releaseId, new CmsItemPropertiesMap("cms:status", "Released")
				.and("abx:ReleaseLabel", "B")
				.and("abx:ReleaseLocale", "en-GB"));
		mockItem(translationDeId, translationProps("de-DE"));
		mockItem(translationSvId, translationProps("sv-SE"));
		
		List<CmsItemTranslation> translations = new ArrayList<>();
		for (CmsItemId id: Arrays.asList(translationDeId, translationSvId)) {
			CmsItemTranslation t = mock(CmsItemTranslation.class);
			when(t.getTranslation()).thenReturn(id);
			translations.add(t);
		}
		when(translationTracking.getTranslations(releaseId)).thenReturn(translations);
		
		config = new PublishConfig();
		config.setStatusInclude(Arrays.asList("Released"));
		Map<String, PublishConfig> configs = new HashMap<>();
		configs.put("print", config);
		when(publishConfiguration.getConfiguration(release.getId())).thenReturn(configs);
		// Simulate the registered filter.
		when(publishConfiguration.getConfigurationFiltered(any(CmsItemPublish.class))).thenAnswer(inv -> {
			CmsItemPublish item = inv.getArgument(0);
			Map<String, PublishConfig> result = new HashMap<>();
			if (new PublishConfigFilterTranslationLocale().accept(config, item)) {
				result.put("print", config);
			}
			return result;
		});
		
		factory = new PublishPackageFactory(lookupReporting, translationTracking, publishConfiguration);
	}
	
	@Test
	public void testTranslationLocaleIncludeNull() {
		assertEquals(Set.of(releaseId, translationDeId, translationSvId), getPackageItemIds());
	}
	
	@Test
	public void testTranslationLocaleIncludeEmpty() {
		config.setTranslationLocaleInclude(new ArrayList<>());
		assertEquals(Set.of(releaseId, translationDeId, translationSvId), getPackageItemIds());
	}
	
	@Test
	public void testTranslationLocaleInclude() {
		config.setTranslationLocaleInclude(Arrays.asList("sv-SE"));
		// Excluded locale must be silently dropped, not fail as configuration mismatch.
		assertEquals(Set.of(releaseId, translationSvId), getPackageItemIds());
	}
	
	@Test
	public void testTranslationLocaleIncludeNoneMatching() {
		config.setTranslationLocaleInclude(Arrays.asList("fr-FR"));
		Set<CmsItemId> ids = getPackageItemIds();
		assertEquals(Set.of(releaseId), ids);
		assertTrue(factory.getPublishPackage(releaseId, "print", false, true, null).getPublishedItems().isEmpty());
	}
	
	private Set<CmsItemId> getPackageItemIds() {
		PublishPackage p = factory.getPublishPackage(releaseId, "print", true, true, null);
		return p.getPublishedItems().stream().map(CmsItemPublish::getId).collect(Collectors.toSet());
	}
	
	private static CmsItemPropertiesMap translationProps(String locale) {
		return new CmsItemPropertiesMap("cms:status", "Released")
				.and("abx:ReleaseLabel", "B")
				.and("abx:TranslationLocale", locale);
	}
	
	private CmsItem mockItem(CmsItemId id, CmsItemPropertiesMap props) {
		CmsItem item = mock(CmsItem.class);
		when(item.getId()).thenReturn(id);
		when(item.getProperties()).thenReturn(props);
		when(item.getStatus()).thenReturn(props.getString("cms:status"));
		when(lookupReporting.getItem(id)).thenReturn(item);
		return item;
	}
}
