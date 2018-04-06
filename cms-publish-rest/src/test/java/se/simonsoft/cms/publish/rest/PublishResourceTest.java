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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
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
	@Mock WorkflowExecutionStatus executionStatusMock;
	@Mock Map<CmsRepository, TranslationTracking> trackingMapMock;
	@Mock TranslationTracking translationTrackingMock;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void publishResourceTest() throws Exception {
		
		//Mock item set up
		RepoRevision revision = new RepoRevision(203, new Date());
		CmsItemIdArg itemId = new CmsItemIdArg("x-svn://demo.simonsoftcms.se/svn/demo1^/vvab/xml/Docs/Sa%20s.xml?p=9");
		
		when(lookupMapMock.get(itemId.getRepository())).thenReturn(lookupReportingMock);
		when(lookupReportingMock.getItem(itemId)).thenReturn(itemMock);
		when(itemMock.getId()).thenReturn(itemId);
		when(itemMock.getRevisionChanged()).thenReturn(revision);
		when(trackingMapMock.get(any(CmsRepository.class))).thenReturn(translationTrackingMock);
		
		List<CmsItemTranslation> translations = new ArrayList<CmsItemTranslation>();
		CmsItemTranslation translationMock1 = mock(CmsItemTranslation.class);
		translations.add(translationMock1);
		CmsItemId translationItemId = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml");
		
		CmsItem itemTranslationMock = mock(CmsItem.class);
		when(itemTranslationMock.getId()).thenReturn(translationItemId);
		
		when(translationMock1.getTranslation()).thenReturn(translationItemId);
		when(lookupReportingMock.getItem(translationItemId)).thenReturn(itemTranslationMock);
		
		when(translationTrackingMock.getTranslations(any(CmsItemId.class))).thenReturn(translations);
		
		
		HashSet<WorkflowExecution> executions1 = new HashSet<WorkflowExecution>();
		
		PublishJob publishJob1 = new PublishJob();
		publishJob1.setConfigname("pdf");
		executions1.add(new WorkflowExecution("1", "RUNNING", new Date(), null, publishJob1));
		
		PublishJob publishJob2 = new PublishJob();
		publishJob2.setConfigname("html");
		executions1.add(new WorkflowExecution("2", "RUNNING", new Date(), null, publishJob2));
		
		
		HashSet<WorkflowExecution> executions2 = new HashSet<WorkflowExecution>();
		
		PublishJob publishJob3 = new PublishJob();
		publishJob3.setConfigname("html");
		executions2.add(new WorkflowExecution("3", "RUNNING", new Date(), null, publishJob3));
		
		PublishJob publishJob4 = new PublishJob();
		publishJob4.setConfigname("pdf");
		executions2.add(new WorkflowExecution("4", "FAILED", new Date(), null, publishJob4));
		
		PublishJob publishJob5 = new PublishJob();
		publishJob5.setConfigname("pdf");
		executions2.add(new WorkflowExecution("5", "FAILED", new Date(), null, publishJob5));
		
		
		when(executionStatusMock.getWorkflowExecutions(itemId, true)).thenReturn(executions1);
		when(executionStatusMock.getWorkflowExecutions(translationItemId, false)).thenReturn(executions2);
		
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
														executionStatusMock,
														lookupMapMock,
														publishConfigurationMock,
														packageZipMock,
														trackingMapMock,
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
		assertTrue(releaseForm.contains("Publish of the Release with config pdf is running."));
		assertTrue(releaseForm.contains("Publish of the Release with config html is running."));
		assertTrue(releaseForm.contains("Publish of the Translations with config html is running."));
		assertTrue(releaseForm.contains("Publish of the Translations with config pdf is failed."));
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
