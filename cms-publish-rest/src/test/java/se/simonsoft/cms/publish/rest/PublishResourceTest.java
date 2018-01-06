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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.junit.Test;
import org.mockito.Mockito;

import se.repos.web.ReposHtmlHelper;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigOptions;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

public class PublishResourceTest {
	
	@Test
	public void publishResourceTest() throws Exception {
		Map<CmsRepository, CmsItemLookupReporting> lookupMapMock = mock(Map.class);
		PublishConfigurationDefault publishConfigurationMock = mock(PublishConfigurationDefault.class);
		CmsItemLookupReporting lookupReportingMock = mock(CmsItemLookupReporting.class);
		PublishPackageZip packageZipMock = mock(PublishPackageZip.class);
		CmsItem itemMock = mock(CmsItem.class);
		TranslationTracking translationTrackingMock = mock(TranslationTracking.class);
		ReposHtmlHelper htmlHelperMock = mock(ReposHtmlHelper.class);
		PublishJobStorageFactory storageFactoryMock = mock(PublishJobStorageFactory.class);
		
		Map<CmsRepository, TranslationTracking> ttMap = new HashMap<CmsRepository, TranslationTracking>();
		
		RepoRevision revision = new RepoRevision(203, new Date());
		CmsItemIdArg itemId = new CmsItemIdArg("x-svn://demo.simonsoftcms.se/svn/demo1^/vvab/xml/Docs/Sa%20s.xml?p=9");
		
		PublishConfig config = new PublishConfig();
		config.setVisible(true);
		config.setOptions(new PublishConfigOptions());
		config.getOptions().setFormat("pdf");
		Map<String, PublishConfig> configMap = new HashMap();
		configMap.put("config", config);
		
		PublishProfilingRecipe recipe = new PublishProfilingRecipe();
		recipe.setName("Active");
		PublishProfilingSet ppSet = new PublishProfilingSet();
		ppSet.add(recipe);
				
		Mockito.when(lookupMapMock.get(itemId.getRepository())).thenReturn(lookupReportingMock);
		Mockito.when(lookupReportingMock.getItem(itemId)).thenReturn(itemMock);
		Mockito.when(itemMock.getId()).thenReturn(itemId);
		Mockito.when(itemMock.getRevisionChanged()).thenReturn(revision);
		Mockito.when(publishConfigurationMock.getConfigurationFiltered(Mockito.any(CmsItemPublish.class))).thenReturn(configMap);
		Mockito.when(publishConfigurationMock.getItemProfilingSet(Mockito.any(CmsItemPublish.class))).thenReturn(ppSet);
		PublishResource resource = new PublishResource("localhost", lookupMapMock, publishConfigurationMock, packageZipMock, ttMap, htmlHelperMock, storageFactoryMock, new VelocityEngine());
		
		String releaseForm = resource.getReleaseForm(itemId);
		
		assertTrue(releaseForm.contains("http://demo.simonsoftcms.se/svn/demo1"));
		assertTrue(releaseForm.contains("Sa s.xml"));
		assertTrue(releaseForm.contains("203"));
		assertTrue(releaseForm.contains("/vvab/xml/Docs/Sa s.xml"));
		assertTrue(releaseForm.contains("pdf"));
		assertTrue(releaseForm.contains("Active"));
		
//		File tmpFile = new File("apa.html");
//		FileOutputStream output = new FileOutputStream(tmpFile);
//		output.write(releaseForm.getBytes());
//		output.close();
		
	}
}
