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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

/**
 * CMS-1997: a profiling recipe tagged with '_locale' never produces a PublishJob for an item whose
 * locale doesn't match (see PublishJobFactory#getPublishJobsProfiling). PublishPackageStatus must
 * mirror that filtering instead of reporting a permanent UNKNOWN placeholder for such items.
 */
public class PublishPackageStatusTest {

	@Mock WorkflowExecutionStatus executionsStatusMock;
	@Mock CmsExportProvider exportProviderMock;
	@Mock PublishJobStorageFactory storageFactoryMock;

	private PublishPackageStatus packageStatus;

	private static final String PUBLICATION = "uk-web";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		packageStatus = new PublishPackageStatus(executionsStatusMock, exportProviderMock, storageFactoryMock);
	}

	@Test
	public void testLocaleRestrictedRecipeExcludesNonMatchingItem() {

		PublishProfilingRecipe recipeUk = getProfilingRecipe("uk", "en-GB");

		CmsItemPublish itemEnGB = getTranslationItem("900276_en-GB.xml", "en-GB");
		CmsItemPublish itemSvSE = getTranslationItem("900276_sv-SE.xml", "sv-SE");

		// Real, succeeded execution exists for the matching (en-GB) item.
		PublishJob job = new PublishJob();
		job.setConfigname(PUBLICATION);
		job.setItemid(itemEnGB.getId().getLogicalIdFull());
		PublishJobOptions options = new PublishJobOptions();
		options.setProfiling(recipeUk);
		job.setOptions(options);
		WorkflowExecution executionEnGB = new WorkflowExecution("1", "SUCCEEDED", Instant.now(), Instant.now(), job);

		when(executionsStatusMock.getWorkflowExecutions(itemEnGB.getId(), true)).thenReturn(new HashSet<>(Collections.singletonList(executionEnGB)));
		when(executionsStatusMock.getWorkflowExecutions(itemEnGB.getId(), false)).thenReturn(new HashSet<>(Collections.singletonList(executionEnGB)));
		// No execution was ever started for the locale-mismatched item, per PublishJobFactory.
		when(executionsStatusMock.getWorkflowExecutions(itemSvSE.getId(), true)).thenReturn(new HashSet<>());
		when(executionsStatusMock.getWorkflowExecutions(itemSvSE.getId(), false)).thenReturn(new HashSet<>());

		PublishConfig config = new PublishConfig();
		config.setActive(true);

		LinkedHashSet<CmsItemPublish> publishedItems = new LinkedHashSet<>();
		publishedItems.add(itemEnGB);
		publishedItems.add(itemSvSE);

		PublishPackage publishPackage = new PublishPackage(PUBLICATION, config, Collections.singleton(recipeUk), publishedItems, null, null);

		Set<WorkflowExecution> status = packageStatus.getStatus(publishPackage);

		// Only the matching item is reported, no UNKNOWN/INACTIVE placeholder for the locale-mismatched item.
		assertEquals(1, status.size());
		WorkflowExecution result = status.iterator().next();
		assertEquals("SUCCEEDED", result.getStatus());
		assertEquals(itemEnGB.getId(), ((PublishJob) result.getInput()).getItemId());
	}

	@Test
	public void testLocaleUnrestrictedRecipeCoversEveryItem() {

		// A recipe without '_locale' remains valid for any locale (unchanged, pre-existing behavior).
		PublishProfilingRecipe recipeAny = getProfilingRecipe("linux", null);

		CmsItemPublish itemEnGB = getTranslationItem("900276_en-GB.xml", "en-GB");
		CmsItemPublish itemSvSE = getTranslationItem("900276_sv-SE.xml", "sv-SE");

		PublishJob jobEnGB = new PublishJob();
		jobEnGB.setConfigname(PUBLICATION);
		jobEnGB.setItemid(itemEnGB.getId().getLogicalIdFull());
		PublishJobOptions optionsEnGB = new PublishJobOptions();
		optionsEnGB.setProfiling(recipeAny);
		jobEnGB.setOptions(optionsEnGB);
		WorkflowExecution executionEnGB = new WorkflowExecution("1", "SUCCEEDED", Instant.now(), Instant.now(), jobEnGB);

		PublishJob jobSvSE = new PublishJob();
		jobSvSE.setConfigname(PUBLICATION);
		jobSvSE.setItemid(itemSvSE.getId().getLogicalIdFull());
		PublishJobOptions optionsSvSE = new PublishJobOptions();
		optionsSvSE.setProfiling(recipeAny);
		jobSvSE.setOptions(optionsSvSE);
		WorkflowExecution executionSvSE = new WorkflowExecution("2", "SUCCEEDED", Instant.now(), Instant.now(), jobSvSE);

		when(executionsStatusMock.getWorkflowExecutions(itemEnGB.getId(), true)).thenReturn(new HashSet<>(Collections.singletonList(executionEnGB)));
		when(executionsStatusMock.getWorkflowExecutions(itemEnGB.getId(), false)).thenReturn(new HashSet<>(Collections.singletonList(executionEnGB)));
		when(executionsStatusMock.getWorkflowExecutions(itemSvSE.getId(), true)).thenReturn(new HashSet<>(Collections.singletonList(executionSvSE)));
		when(executionsStatusMock.getWorkflowExecutions(itemSvSE.getId(), false)).thenReturn(new HashSet<>(Collections.singletonList(executionSvSE)));

		PublishConfig config = new PublishConfig();
		config.setActive(true);

		LinkedHashSet<CmsItemPublish> publishedItems = new LinkedHashSet<>();
		publishedItems.add(itemEnGB);
		publishedItems.add(itemSvSE);

		PublishPackage publishPackage = new PublishPackage(PUBLICATION, config, Collections.singleton(recipeAny), publishedItems, null, null);

		Set<WorkflowExecution> status = packageStatus.getStatus(publishPackage);

		assertEquals(2, status.size());
		for (WorkflowExecution result: status) {
			assertEquals("SUCCEEDED", result.getStatus());
		}
	}

	private PublishProfilingRecipe getProfilingRecipe(String name, String locale) {
		Map<String, String> attributes = new HashMap<>();
		if (locale != null) {
			attributes.put("_locale", locale);
		}
		return new PublishProfilingRecipe(name, attributes);
	}

	private CmsItemPublish getTranslationItem(String filename, String translationLocale) {
		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/" + filename));
		itemIdArg.setHostname("ubuntu-cheftest1.pdsvision.net");
		CmsItemId itemId = itemIdArg.withPegRev(443L);

		CmsItem item = mock(CmsItem.class);
		when(item.getId()).thenReturn(itemId);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		props.and("abx:TranslationLocale", translationLocale);
		when(item.getProperties()).thenReturn(props);

		return new CmsItemPublish(item);
	}
}
