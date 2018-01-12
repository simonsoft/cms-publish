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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
	
	@Mock Map<CmsRepository, CmsItemLookupReporting> lookupMapMock;
	@Mock PublishConfigurationDefault publishConfigurationMock;
	@Mock CmsItemLookupReporting lookupReportingMock;
	@Mock PublishPackageZip packageZipMock;
	@Mock CmsItem itemMock;
	@Mock ReposHtmlHelper htmlHelperMock;
	@Mock PublishJobStorageFactory storageFactoryMock;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void publishResourceTest() throws Exception {
		
		Map<CmsRepository, TranslationTracking> ttMap = new HashMap<CmsRepository, TranslationTracking>();
		
		//Mock item set up
		RepoRevision revision = new RepoRevision(203, new Date());
		CmsItemIdArg itemId = new CmsItemIdArg("x-svn://demo.simonsoftcms.se/svn/demo1^/vvab/xml/Docs/Sa%20s.xml?p=9");
		
		when(lookupMapMock.get(itemId.getRepository())).thenReturn(lookupReportingMock);
		when(lookupReportingMock.getItem(itemId)).thenReturn(itemMock);
		when(itemMock.getId()).thenReturn(itemId);
		when(itemMock.getRevisionChanged()).thenReturn(revision);
		
		//Config setup
		PublishConfig config = new PublishConfig();
		config.setVisible(true);
		config.setOptions(new PublishConfigOptions());
		config.getOptions().setFormat("pdf");
		Map<String, PublishConfig> configMap = new HashMap<String, PublishConfig>();
		configMap.put("print", config);
		
		when(publishConfigurationMock.getConfigurationFiltered(any(CmsItemPublish.class))).thenReturn(configMap);
		
		//Profiling mock setup.
		PublishProfilingRecipe recipe = new PublishProfilingRecipe();
		recipe.setName("Active");
		PublishProfilingSet ppSet = new PublishProfilingSet();
		ppSet.add(recipe);
		when(publishConfigurationMock.getItemProfilingSet(any(CmsItemPublish.class))).thenReturn(ppSet);
		
		PublishResource resource = new PublishResource("localhost",
														lookupMapMock,
														publishConfigurationMock,
														packageZipMock,
														ttMap,
														htmlHelperMock,
														storageFactoryMock,
														getVelocityEngine());
		
		String releaseForm = resource.getReleaseForm(itemId);
		
		assertTrue(releaseForm.contains("http://demo.simonsoftcms.se/svn/demo1"));
		assertTrue(releaseForm.contains("Sa s.xml"));
		assertTrue(releaseForm.contains("203"));
		assertTrue(releaseForm.contains("/vvab/xml/Docs/Sa s.xml"));
		assertTrue(releaseForm.contains("print"));
		assertTrue(releaseForm.contains("Active"));
	}
	
	
	//TODO: Maybe we should inject a velocity engine in testSetup. 
	private VelocityEngine getVelocityEngine() {

		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("runtime.references.strict", true);

		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		//Disable velocity logging.
		p.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");

		try {
			engine.init(p);
		} catch (Exception e) {
			throw new RuntimeException("Could not initilize Velocity engine with given properties.");
		}

		return engine;
	}
}
