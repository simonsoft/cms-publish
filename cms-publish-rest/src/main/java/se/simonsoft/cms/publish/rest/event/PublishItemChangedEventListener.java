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
package se.simonsoft.cms.publish.rest.event;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.events.ItemChangedEventListener;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.PublishJobFactory;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;

public class PublishItemChangedEventListener implements ItemChangedEventListener {

	private final PublishConfiguration publishConfiguration;
	private final PublishExecutor publishExecutor;
	
	private final PublishJobFactory jobFactory;
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishItemChangedEventListener.class);

	@Inject
	public PublishItemChangedEventListener(
			PublishConfiguration publishConfiguration,
			PublishExecutor publishExecutor,
			List<PublishConfigFilter> filters,
			ObjectReader reader,
			PublishJobFactory jobFactory) {
		
		this.publishConfiguration = publishConfiguration;
		this.publishExecutor = publishExecutor;
		this.jobFactory = jobFactory;
	}

	
	@Override
	public void onItemChange(CmsItem item) {
		logger.debug("Item change event with id: {}", item.getId());
		if (item.getKind() != CmsItemKind.File) {
			return;
		}
		Set<PublishJob> jobs = getPublishJobs(item);
		publishExecutor.startPublishJobs(jobs);
	}
	
	public Set<PublishJob> getPublishJobs(CmsItem item) {
		if (item.getId().getPegRev() == null) {
			logger.error("Item requires a revision to be published: {}", item.getId().getLogicalId());
			throw new IllegalArgumentException("Item requires a revision to be published.");
		}
		
		if (!item.getId().getRepository().isHostKnown()) {
			logger.error("Item requires a known hostname to be published: {}", item.getId().getLogicalIdFull());
			throw new IllegalArgumentException("Item requires a known hostname to be published: " + item.getId().getLogicalIdFull());
		}
		
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		
		// Configs filtered for the item. Starting only Active configs based on event.
		Map<String, PublishConfig> publishConfigs = this.publishConfiguration.getConfigurationActive(itemPublish);
		TranslationLocalesMapping localesRfc = (TranslationLocalesMapping) this.publishConfiguration.getTranslationLocalesMapping(itemPublish);
		
		Set<PublishJob> jobs = new LinkedHashSet<PublishJob>();
		// Single item, looping over configs.
		for (Entry<String, PublishConfig> configEntry: publishConfigs.entrySet()) {
			String configName = configEntry.getKey();
			PublishConfig config = configEntry.getValue();
			
			// Verify filtering for condition not handled below: profilingInclude == false && hasProfiles == true
			if (itemPublish.hasProfiles() && config.getProfilingInclude() != null && Boolean.FALSE.equals(config.getProfilingInclude())) {
				throw new IllegalArgumentException("Item should not have profiling, filtering incorrect.");
			}
			
			if (Boolean.TRUE.equals(config.getProfilingInclude())) {
				// One or many jobs with profiling.
				// Will return empty List<PublishJob> if item has no profiles or filtered by 'profilingNameInclude'.
				PublishProfilingSet profilingSet = this.publishConfiguration.getItemProfilingSet(itemPublish);
				jobs.addAll(this.jobFactory.getPublishJobsProfiling(itemPublish, config, configName, profilingSet, localesRfc));
			} else {
				// Normal, non-profiling job.
				PublishJob pj = this.jobFactory.getPublishJob(itemPublish, config, configName, null, localesRfc, Optional.empty(), Optional.empty());
				jobs.add(pj);
			}
		}
		
		return jobs;
	}

}

