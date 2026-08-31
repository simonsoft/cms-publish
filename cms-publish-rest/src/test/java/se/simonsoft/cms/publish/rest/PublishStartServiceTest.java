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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.command.PublishWebhookCommandHandler;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobDelivery;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.TranslationTracking;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CMS-1997: PublishStartService#doPublishStartItem's named-recipe branch (options.getProfilingname())
 * reads a profiling recipe straight from the item's own embedded profiling set - the same source read by
 * PublishItemChangedEventListener's event-driven auto-publish, which enforces '_locale' via
 * PublishJobFactory.getPublishJobsProfiling's localeIncluded check. This single-recipe start path instead
 * hands the recipe straight to PublishJobFactory#getPublishJob, bypassing that check - a manual/explicit
 * start could start a job the event listener would skip for the exact same item and recipe. These tests
 * cover the added guard that closes the gap before merge.
 */
public class PublishStartServiceTest {

	@Mock PublishWebhookCommandHandler publishWebhookCommandHandlerMock;
	@Mock CmsItemLookupReporting lookupReportingMock;
	@Mock TranslationTracking translationTrackingMock;
	@Mock PublishConfiguration publishConfigurationMock;
	@Mock PublishExecutor publishExecutorMock;
	@Mock PublishJobFactory jobFactoryMock;

	private PublishStartService startService;

	private final ObjectMapper mapper = new ObjectMapper();
	private static final String PUBLICATION = "web-cdn-profiled";

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		startService = new PublishStartService(
				publishWebhookCommandHandlerMock,
				lookupReportingMock,
				translationTrackingMock,
				publishConfigurationMock,
				publishExecutorMock,
				jobFactoryMock,
				mapper.reader(),
				mapper.writer());
	}

	@Test
	public void testLocaleMismatchIsRejected() {

		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/900276.xml"));
		itemIdArg.setHostname("ubuntu-cheftest1.pdsvision.net");
		CmsItemId itemId = itemIdArg;

		// 'linux' is restricted to de-DE, but the item's own Translation Locale is en-GB.
		CmsItem item = getTranslationItem(itemId, "en-GB", "[{\"name\":\"linux\",\"logicalexpr\":\"%3A\",\"_locale\":\"de-DE\"}]");

		setUpCommonMocks(itemId, item);

		PublishStartOptions options = new PublishStartOptions();
		options.setPublication(PUBLICATION);
		options.setProfilingname("linux");
		options.setExecutionid("test-execution-id");

		try {
			startService.doPublishStartItem(itemId, options);
			fail("Expected IllegalArgumentException for locale-mismatched profiling recipe.");
		} catch (IllegalArgumentException e) {
			assertTrue("Exception message should mention the locale restriction: " + e.getMessage(),
					e.getMessage().contains("locale"));
		}
	}

	@Test
	public void testUnrestrictedRecipeIsNotRejected() throws CommandRuntimeException {

		CmsItemIdArg itemIdArg = new CmsItemIdArg(new CmsRepository("/svn", "demo1"), new CmsItemPath("/vvab/xml/documents/900277.xml"));
		itemIdArg.setHostname("ubuntu-cheftest1.pdsvision.net");
		CmsItemId itemId = itemIdArg;

		// 'osx' has no '_locale', valid for any Translation Locale (unchanged, pre-existing behavior).
		CmsItem item = getTranslationItem(itemId, "en-GB", "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}]");

		setUpCommonMocks(itemId, item);

		PublishJob mockJob = getMinimalPublishJob();
		when(jobFactoryMock.getPublishJob(any(CmsItemPublish.class), any(PublishConfig.class), any(String.class), anyObject(), anyObject(), anyObject(), anyObject()))
				.thenReturn(mockJob);
		when(publishWebhookCommandHandlerMock.getPostPayload(any(PublishJobDelivery.class), any(PublishJobStorage.class), any(Optional.class)))
				.thenReturn(new LinkedHashMap<String, String>());
		when(publishExecutorMock.startPublishJobs(any(java.util.Set.class)))
				.thenReturn(new HashSet<>(Collections.singletonList("arn:aws:states:eu-west-1:518993259802:execution:cms-travelonium-publish-v1:87fdcfe6-8dd4-4276-9d22-773f6e5315b1")));

		PublishStartOptions options = new PublishStartOptions();
		options.setPublication(PUBLICATION);
		options.setProfilingname("osx");
		options.setExecutionid("test-execution-id");

		LinkedHashMap<String, String> result = startService.doPublishStartItem(itemId, options);
		assertEquals("87fdcfe6-8dd4-4276-9d22-773f6e5315b1", result.get("executionid"));
	}

	private void setUpCommonMocks(CmsItemId itemId, CmsItem item) {
		RepoRevision revision = new RepoRevision(9, new Date());
		when(item.getRevisionChanged()).thenReturn(revision);

		// getPublishJob() re-fetches the item at the resolved peg rev; return the same item both times.
		when(lookupReportingMock.getItem(any(CmsItemId.class))).thenReturn(item);

		Map<String, PublishConfig> configs = new HashMap<>();
		PublishConfig config = new PublishConfig();
		config.setActive(true);
		configs.put(PUBLICATION, config);
		when(publishConfigurationMock.getConfiguration(itemId)).thenReturn(configs);
		when(publishConfigurationMock.getConfigurationFiltered(any(CmsItemPublish.class))).thenReturn(configs);
		when(publishConfigurationMock.getTranslationLocalesMapping(any(CmsItemPublish.class))).thenReturn(null);
	}

	private CmsItem getTranslationItem(CmsItemId itemId, String translationLocale, String profilingJson) {
		CmsItem.MetaCms item = mock(CmsItem.MetaCms.class);
		when(item.getId()).thenReturn(itemId);
		CmsItemPropertiesMap props = new CmsItemPropertiesMap("cms:status", "Released");
		props.and("abx:TranslationLocale", translationLocale);
		when(item.getProperties()).thenReturn(props);

		HashMap<String, Object> metaMap = new HashMap<>();
		metaMap.put("embd_cms_profiling", profilingJson);
		when(item.getMeta()).thenReturn(metaMap);

		return item;
	}

	private PublishJob getMinimalPublishJob() {
		PublishJob job = new PublishJob();
		PublishJobOptions options = new PublishJobOptions();
		options.setStorage(new PublishJobStorage());
		options.setDelivery(new PublishJobDelivery());
		options.setProgress(new PublishJobProgress());
		options.setManifest(new PublishJobManifest());
		job.setOptions(options);
		return job;
	}
}
