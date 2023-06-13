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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;
import se.simonsoft.cms.reporting.CmsItemLookupReporting;

public class PublishStartService {

	private final CmsItemLookupReporting lookupReporting;
	private final PublishConfiguration publishConfiguration;
	private final PublishExecutor publishExecutor;
	private final PublishJobFactory jobFactory;
	
	@Inject
	public PublishStartService(
			CmsItemLookupReporting lookupReporting,
			PublishConfiguration publishConfiguration,
			PublishExecutor publishExecutor,
			PublishJobFactory jobFactory) {
		
		this.lookupReporting = lookupReporting;
		this.publishConfiguration = publishConfiguration;
		this.publishExecutor = publishExecutor;
		this.jobFactory = jobFactory;
	}
	
	
	public LinkedHashMap<String, String> doPublishStartItem(CmsItemId itemId, PublishStartOptions options) {
		
		PublishConfig config = getPublishConfiguration(itemId, options.getPublication());
		PublishJob publishJob = getPublishJob(itemId, options, config);
		
		Set<String> uuids = publishExecutor.startPublishJobs(new LinkedHashSet<PublishJob>(Arrays.asList(publishJob)));
		// TODO: Verify that one job was started, log the UUID.
		// Note that this UUID is not the same as exectionuuid in PublishStartOptions. 
		// The options.executionuuid is required before this UUID is known.
		
		LinkedHashMap<String, String> result = null; // TODO: Use PublishWebhookCommandHandler
		
		return result;
	}
	
		
	private PublishConfig getPublishConfiguration(CmsItemId itemId, String name) {
		
		Map<String, PublishConfig> configs = publishConfiguration.getConfiguration(itemId);
		return configs.get(name);
	}

	private PublishJob getPublishJob(CmsItemId itemId, PublishStartOptions options, PublishConfig config) {
		
		CmsItem item = this.lookupReporting.getItem(itemId);
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		TranslationLocalesMapping localesRfc = (TranslationLocalesMapping) this.publishConfiguration.getTranslationLocalesMapping(itemPublish);

		PublishProfilingRecipe profilingRecipe = null;
		// Use profilingname if set (get recipe from itemPublish)
		// Use startprofiling if set (must not contain 'name' or 'logicalexpr')
		// no profiling.
		
		// Verify that the config is intended for profiling, if profilingRecipe != null.
		
		
		// Set profilingRecipe.name to executionid if any of the start* parameters are provided (even if profilingRecipe == null).
		// Figure out how to manage profilingRecipe.name when no profiling is used (method validateFilter() throws exception when no filter parameters are set).
		// Need specific scenariotest setting 'startinput' but not 'startprofiling'.
		
		PublishJob job = jobFactory.getPublishJob(itemPublish, config, options.getPublication(), profilingRecipe, localesRfc, Optional.ofNullable(options.getStartpathname()), Optional.ofNullable(options.getStartcustom()));
		return job;
	}
	
}
