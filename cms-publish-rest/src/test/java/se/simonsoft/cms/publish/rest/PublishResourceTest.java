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

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.repos.web.PageInfo;
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
import se.simonsoft.cms.publish.rest.writers.PublishReleaseMessageBodyWriterHtml;
import se.simonsoft.cms.release.translation.CmsItemTranslation;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;
import se.simonsoft.cms.reporting.response.CmsItemRepositem;

public class PublishResourceTest {
	
	@Mock Map<CmsRepository, CmsItemLookupReporting> lookupMapMock;
	@Mock PublishConfigurationDefault publishConfigurationMock;
	@Mock CmsItemLookupReporting lookupReportingMock;
	@Mock PublishPackageZipBuilder packageZipMock;
	@Mock PublishPackageStatus packageStatusMock;
	@Mock CmsItemRepositem itemMock;
	@Mock ReposHtmlHelper htmlHelperMock;
	@Mock PublishJobStorageFactory storageFactoryMock;
	@Mock WorkflowExecutionStatus executionStatusMock;
	@Mock Map<CmsRepository, TranslationTracking> trackingMapMock;
	@Mock TranslationTracking translationTrackingMock;

	private PublishResource publishResource;

	private final RepoRevision revision = new RepoRevision(203, new Date());
	private final CmsItemIdArg itemId = new CmsItemIdArg("x-svn://demo.simonsoftcms.se/svn/demo1^/vvab/xml/Docs/Sa%20s.xml?p=9");

	private static final Logger logger = LoggerFactory.getLogger(PublishResourceTest.class);

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(htmlHelperMock.getHeadTags(any(PageInfo.class))).thenReturn("");

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
		executions1.add(new WorkflowExecution("1", "RUNNING", Instant.now(), null, publishJob1));

		PublishJob publishJob2 = new PublishJob();
		publishJob2.setConfigname("html");
		executions1.add(new WorkflowExecution("2", "RUNNING", Instant.now(), null, publishJob2));

		HashSet<WorkflowExecution> executions2 = new HashSet<WorkflowExecution>();

		PublishJob publishJob3 = new PublishJob();
		publishJob3.setConfigname("html");
		executions2.add(new WorkflowExecution("3", "RUNNING", Instant.now(), null, publishJob3));

		PublishJob publishJob4 = new PublishJob();
		publishJob4.setConfigname("pdf");
		executions2.add(new WorkflowExecution("4", "FAILED", Instant.now(), null, publishJob4));

		PublishJob publishJob5 = new PublishJob();
		publishJob5.setConfigname("pdf");
		executions2.add(new WorkflowExecution("5", "FAILED", Instant.now(), null, publishJob5));

		when(executionStatusMock.getWorkflowExecutions(itemId, true)).thenReturn(executions1);
		when(executionStatusMock.getWorkflowExecutions(translationItemId, false)).thenReturn(executions2);

		// Config setup
		PublishConfig config = new PublishConfig();
		config.setVisible(true);
		config.setOptions(new PublishConfigOptions());
		config.getOptions().setFormat("pdf");
		Map<String, PublishConfig> configMap = new HashMap<String, PublishConfig>();
		configMap.put("print", config);

		when(publishConfigurationMock.getConfigurationFiltered(any(CmsItemPublish.class))).thenReturn(configMap);

		// Profiling mock setup.
		PublishProfilingRecipe recipe = new PublishProfilingRecipe();
		recipe.setName("active");
		recipe.setLogicalexpr("profiling-logicalexpression");
		PublishProfilingSet ppSet = new PublishProfilingSet();
		ppSet.add(recipe);
		when(publishConfigurationMock.getItemProfilingSet(any(CmsItemPublish.class))).thenReturn(ppSet);

		publishResource = new PublishResource("localhost",
				executionStatusMock,
				lookupMapMock,
				publishConfigurationMock,
				packageZipMock,
				packageStatusMock,
				trackingMapMock,
				htmlHelperMock,
				storageFactoryMock,
				getVelocityEngine());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPublishResourceReleaseHtml() throws Exception {

		PublishRelease publishRelease = publishResource.getRelease(itemId);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PublishReleaseMessageBodyWriterHtml writer = new PublishReleaseMessageBodyWriterHtml(htmlHelperMock, getVelocityEngine());
		writer.writeTo(publishRelease, null, null, null, null, null, outputStream);
		String releaseForm = outputStream.toString();

		assertTrue(releaseForm.contains("Export Publications"));
		assertTrue(releaseForm.contains("http://demo.simonsoftcms.se/svn/demo1"));
		assertTrue(releaseForm.contains("cms-react-exportpublications/bundle.css"));
		assertTrue(releaseForm.contains("cms-react-exportpublications/bundle.js"));
		assertTrue(releaseForm.contains("export-publications-view"));
	}

	@Test
	@Ignore("This output is tested in se.simonsoft.cms.scenario.Demo1PublishTest.")
	public void testPublishResourceReleaseJson() throws Exception { }

	// TODO: Maybe we should inject a velocity engine in testSetup.
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
