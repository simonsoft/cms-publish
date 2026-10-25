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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
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
import se.simonsoft.cms.item.export.CmsExportImportJob;
import se.simonsoft.cms.item.export.CmsExportJobNotFoundException;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
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

	@Test
	public void testJobsStartAllowedMultipleProfiling() {

		// CMS-2069: start allowed is decided per (item, profiling recipe), not per item.
		PublishProfilingRecipe recipeLinux = getProfilingRecipe("linux", null);
		PublishProfilingRecipe recipeWindows = getProfilingRecipe("windows", null);

		CmsItemPublish itemEnGB = getTranslationItem("900276_en-GB.xml", "en-GB");

		PublishJob jobLinux = getPublishJob(itemEnGB, recipeLinux);
		PublishJob jobWindows = getPublishJob(itemEnGB, recipeWindows);
		Set<WorkflowExecution> executions = new HashSet<>(Arrays.asList(
				new WorkflowExecution("1", "SUCCEEDED", Instant.now(), Instant.now(), getPublishJob(itemEnGB, recipeLinux)),
				new WorkflowExecution("2", "FAILED", Instant.now(), Instant.now(), getPublishJob(itemEnGB, recipeWindows))));
		mockExecutions(itemEnGB, executions);

		PublishPackage publishPackage = getPublishPackage(new LinkedHashSet<>(Arrays.asList(recipeLinux, recipeWindows)), itemEnGB);
		Set<PublishJob> jobsAll = new LinkedHashSet<>(Arrays.asList(jobLinux, jobWindows));

		Set<PublishJob> jobs = packageStatus.getJobsStartAllowed(publishPackage, jobsAll, false);
		assertEquals(1, jobs.size());
		assertTrue("only the FAILED recipe is restarted", jobs.contains(jobWindows));

		Set<PublishJob> jobsAdvanced = packageStatus.getJobsStartAllowed(publishPackage, jobsAll, true);
		assertEquals("advanced allows restart of SUCCEEDED", 2, jobsAdvanced.size());
	}

	@Test
	public void testJobsStartAllowedMultipleProfilingLocale() {

		// CMS-2069 / CMS-1997: '_locale' restricted recipe combined with an unrestricted recipe.
		PublishProfilingRecipe recipeUk = getProfilingRecipe("uk", "en-GB");
		PublishProfilingRecipe recipeLinux = getProfilingRecipe("linux", null);

		CmsItemPublish itemEnGB = getTranslationItem("900276_en-GB.xml", "en-GB");
		CmsItemPublish itemSvSE = getTranslationItem("900276_sv-SE.xml", "sv-SE");

		// Jobs as produced by PublishJobFactory, no job for (sv-SE, uk).
		PublishJob jobEnGBUk = getPublishJob(itemEnGB, recipeUk);
		PublishJob jobEnGBLinux = getPublishJob(itemEnGB, recipeLinux);
		PublishJob jobSvSELinux = getPublishJob(itemSvSE, recipeLinux);

		mockExecutions(itemEnGB, new HashSet<>(Arrays.asList(
				new WorkflowExecution("1", "SUCCEEDED", Instant.now(), Instant.now(), getPublishJob(itemEnGB, recipeUk)),
				new WorkflowExecution("2", "FAILED", Instant.now(), Instant.now(), getPublishJob(itemEnGB, recipeLinux)))));
		mockExecutions(itemSvSE, new HashSet<>(Collections.singletonList(
				new WorkflowExecution("3", "ABORTED", Instant.now(), Instant.now(), getPublishJob(itemSvSE, recipeLinux)))));

		PublishPackage publishPackage = getPublishPackage(new LinkedHashSet<>(Arrays.asList(recipeUk, recipeLinux)), itemEnGB, itemSvSE);
		Set<PublishJob> jobsAll = new LinkedHashSet<>(Arrays.asList(jobEnGBUk, jobEnGBLinux, jobSvSELinux));

		Set<PublishJob> jobs = packageStatus.getJobsStartAllowed(publishPackage, jobsAll, false);
		assertEquals(2, jobs.size());
		assertTrue(jobs.contains(jobEnGBLinux));
		assertTrue(jobs.contains(jobSvSELinux));
	}

	@Test
	public void testJobsStartAllowedMultipleProfilingUnknown() throws Exception {

		// CMS-2069: a recipe without any execution gets the UNKNOWN fallback, using the storage path of that recipe.
		PublishProfilingRecipe recipeLinux = getProfilingRecipe("linux", null);
		PublishProfilingRecipe recipeWindows = getProfilingRecipe("windows", null);

		CmsItemPublish itemEnGB = getTranslationItem("900276_en-GB.xml", "en-GB");

		PublishJob jobLinux = getPublishJob(itemEnGB, recipeLinux);
		PublishJob jobWindows = getPublishJob(itemEnGB, recipeWindows);
		mockExecutions(itemEnGB, new HashSet<>(Collections.singletonList(
				new WorkflowExecution("1", "SUCCEEDED", Instant.now(), Instant.now(), getPublishJob(itemEnGB, recipeLinux)))));

		PublishJobStorage storageWindows = new PublishJobStorage();
		storageWindows.setType("s3");
		storageWindows.setPathconfigname(PUBLICATION);
		storageWindows.setPathdir("/vvab/xml/documents/900276_en-GB.xml");
		storageWindows.setPathnamebase("900276_en-GB_windows_r0000000443");
		when(storageFactoryMock.getInstance(any(), any(), eq(PUBLICATION), eq(recipeWindows))).thenReturn(storageWindows);
		CmsExportReader readerMock = mock(CmsExportReader.class);
		doThrow(new CmsExportJobNotFoundException(mock(CmsExportImportJob.class), "missing", null)).when(readerMock).prepare(any());
		when(exportProviderMock.getReader()).thenReturn(readerMock);

		PublishPackage publishPackage = getPublishPackage(new LinkedHashSet<>(Arrays.asList(recipeLinux, recipeWindows)), itemEnGB);
		Set<PublishJob> jobsAll = new LinkedHashSet<>(Arrays.asList(jobLinux, jobWindows));

		Set<PublishJob> jobs = packageStatus.getJobsStartAllowed(publishPackage, jobsAll, false);
		assertEquals(1, jobs.size());
		assertTrue("recipe without execution (UNKNOWN) is started", jobs.contains(jobWindows));
		verify(storageFactoryMock).getInstance(any(), any(), eq(PUBLICATION), eq(recipeWindows));
		verify(storageFactoryMock, never()).getInstance(any(), any(), eq(PUBLICATION), eq(recipeLinux));
	}

	private void mockExecutions(CmsItemPublish item, Set<WorkflowExecution> executions) {
		// Return a new set for each call, the status service filters the set.
		when(executionsStatusMock.getWorkflowExecutions(item.getId(), true)).thenAnswer(invocation -> new HashSet<>(executions));
		when(executionsStatusMock.getWorkflowExecutions(item.getId(), false)).thenAnswer(invocation -> new HashSet<>(executions));
	}

	private PublishPackage getPublishPackage(Set<PublishProfilingRecipe> profilingSet, CmsItemPublish... items) {
		PublishConfig config = new PublishConfig();
		config.setActive(true);
		config.setOptions(new PublishConfigOptions());
		return new PublishPackage(PUBLICATION, config, profilingSet, new LinkedHashSet<>(Arrays.asList(items)), null, null);
	}

	private PublishJob getPublishJob(CmsItemPublish item, PublishProfilingRecipe profiling) {
		PublishJob job = new PublishJob();
		job.setConfigname(PUBLICATION);
		job.setItemid(item.getId().getLogicalIdFull());
		PublishJobOptions options = new PublishJobOptions();
		options.setProfiling(profiling);
		job.setOptions(options);
		return job;
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
