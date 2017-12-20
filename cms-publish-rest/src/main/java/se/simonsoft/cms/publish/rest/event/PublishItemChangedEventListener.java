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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.config.CmsConfigOption;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.events.ItemChangedEventListener;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.PublishJobManifestBuilder;
import se.simonsoft.cms.publish.rest.PublishJobStorageFactory;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.release.ReleaseProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

public class PublishItemChangedEventListener implements ItemChangedEventListener {

	private final CmsRepositoryLookup lookup;
	private final WorkflowExecutor<WorkflowItemInput> workflowExecutor;
	
	private final List<PublishConfigFilter> filters;
	private final ObjectReader readerConfig;
	private final ObjectReader readerProfiling;
	
	private final String pathVersion = "cms4";
	private final String s3Bucket = "cms-automation";
	private final String type = "publish-job";
	private final PublishJobStorageFactory storageFactory;
	
	private static final String PUBLISH_CONFIG_KEY = "cmsconfig-publish";  
	
	private static final Logger logger = LoggerFactory.getLogger(PublishItemChangedEventListener.class);

	@Inject
	public PublishItemChangedEventListener(
			CmsRepositoryLookup lookup,
			@Named("config:se.simonsoft.cms.aws.publish.workflow") WorkflowExecutor<WorkflowItemInput> workflowExecutor,
			List<PublishConfigFilter> filters,
			ObjectReader reader,
			PublishJobStorageFactory storageFactory) {
		
		this.lookup = lookup;
		this.workflowExecutor = workflowExecutor;
		this.filters = filters;
		this.readerConfig = reader.forType(PublishConfig.class);
		this.readerProfiling = reader.forType(PublishProfilingSet.class);
    this.storageFactory = storageFactory;
	}

	@Override
	public void onItemChange(CmsItem item) {
		logger.debug("Got an item change event with id: {}", item.getId());
		if (item.getId().getPegRev() == null) {
			logger.error("Given item is missing a revision: {}", item.getId().getLogicalId());
			throw new IllegalArgumentException("Item requires a revision to be published.");
		}
		CmsResourceContext context = this.lookup.getConfig(item.getId(), item.getKind());
		
		Map<String, PublishConfig> publishConfigs = deserializeConfig(context);
		publishConfigs = filterConfigs(item, publishConfigs);
		
		CmsItemPublish itemPublish = new CmsItemPublish(item);
		
		List<PublishJob> jobs = new ArrayList<PublishJob>();
		for (Entry<String, PublishConfig> configEntry: publishConfigs.entrySet()) {
			String configName = configEntry.getKey();
			PublishConfig config = configEntry.getValue();
			
			if (Boolean.TRUE.equals(config.getProfilingInclude())) {
				// One or many jobs with profiling.
				jobs.addAll(getPublishJobsProfiling(itemPublish, config, configName));
			} else {
				// Normal, non-profiling job.
				PublishJob pj = getPublishJob(itemPublish, config, configName, null);
				jobs.add(pj);
			}
		}
		
		
		logger.debug("Starting executions for {} number of PublishJobs", jobs.size());
		for (PublishJob job: jobs) {
			workflowExecutor.startExecution(job);
		}
	}
	
	private List<PublishJob> getPublishJobsProfiling(CmsItemPublish itemPublish, PublishConfig config, String configName) {
		List<PublishJob> profiledJobs = new ArrayList<PublishJob>();
		
		PublishProfilingSet profilesSet = getItemProfiles(itemPublish);
		
		for (PublishProfilingRecipe profilesRecipe: profilesSet) {
			
			List<String> profilingNames = config.getProfilingNameInclude();
			// Filter on profilesNameInclude if set.
			if (profilingNames == null || profilingNames.contains(profilesRecipe.getName())) {
				profiledJobs.add(getPublishJob(itemPublish, config, configName, profilesRecipe));
			}
		}
		
		return profiledJobs;
	}
	
	private PublishProfilingSet getItemProfiles(CmsItemPublish itemPublish) {
		
		if (!itemPublish.hasProfiles()) {
			return null;
		}
		
		String profilesProp = itemPublish.getProperties().getString(ReleaseProperties.PROPNAME_PROFILING);
		try {
			return this.readerProfiling.readValue(profilesProp);
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid property 'abx:Profiling': " + profilesProp);
		}
	}
	

	private PublishJob getPublishJob(CmsItemPublish item, PublishConfig c, String configName, PublishProfilingRecipe profiling) {
		PublishConfigTemplateString templateEvaluator = getTemplateEvaluator(item, profiling);
		PublishJobManifestBuilder manifestBuilder = new PublishJobManifestBuilder(templateEvaluator);
		
		PublishConfigArea area = PublishJobManifestBuilder.getArea(item, c.getAreas());
		PublishJob pj = new PublishJob(c);
		pj.setArea(area); 
		pj.setItemid(item.getId().getLogicalId());
		pj.setAction("publish-noop");
		pj.setType(this.type);
		pj.setConfigname(configName);
		
		pj.getOptions().setSource(item.getId().getLogicalId());
		if (profiling != null) {
			pj.getOptions().setProfiling(profiling.getPublishJobProfiling());
		}
		
		PublishConfigStorage configStorage = pj.getOptions().getStorage();
		PublishJobStorage storage = storageFactory.getInstance(configStorage, item, configName, profiling);
		pj.getOptions().setStorage(storage);

		String pathname = templateEvaluator.evaluate(area.getPathnameTemplate());
		pj.getOptions().setPathname(pathname);
		
		// Build the Manifest, modifies the existing manifest object.
		manifestBuilder.build(item, pj);
		
		logger.debug("Created PublishJob from config: {}", configName);
		return pj;
	}
	
	public String getNameBase(CmsItemId itemId, PublishProfilingRecipe profiling) {
		StringBuilder sb = new StringBuilder();
		
		if (profiling == null) {
			sb.append(itemId.getRelPath().getNameBase());
		} else {
			sb.append(profiling.getName());
		}
		sb.append(String.format("_r%010d", itemId.getPegRev()));
		return sb.toString();
	}
	
	private Map<String, PublishConfig> deserializeConfig(CmsResourceContext context) {
		logger.debug("Starting deserialization of configs with namespace {}...", PUBLISH_CONFIG_KEY);
		Map<String, PublishConfig> configs = new LinkedHashMap<>();
		Iterator<CmsConfigOption> iterator = context.iterator();
		while (iterator.hasNext()) {
			CmsConfigOption configOption = iterator.next();
			String configOptionName = configOption.getNamespace();
			if (configOptionName.startsWith(PUBLISH_CONFIG_KEY)) {
				try {
					PublishConfig publishConfig = readerConfig.readValue(configOption.getValueString());
					configs.put(configOption.getKey(), publishConfig);
				} catch (JsonProcessingException e) {
					logger.error("Could not deserialize config: {} to new PublishConfig", configOptionName.concat(":" + configOption.getKey()));
					throw new RuntimeException(e);
				} catch (IOException e) {
					logger.error("Could not deserialize config: {} to new PublishConfig", configOptionName.concat(":" + configOption.getKey()));
					throw new RuntimeException(e);
				}
			}
		}
		
		logger.debug("Context had {} number of valid cmsconfig-publish objects", configs.size());
		
		return configs;
	}
	
	private Map<String, PublishConfig> filterConfigs(CmsItem item, Map<String, PublishConfig> configs) {
		
		Map<String, PublishConfig> filteredConfigs = new LinkedHashMap<String, PublishConfig>();
		for (Entry<String, PublishConfig> config: configs.entrySet()) {
			List<String> filtered = new ArrayList<String>(filters.size());
			for (PublishConfigFilter f: filters) {
				if (!f.accept(config.getValue(), item)) {
					filtered.add(f.getClass().getName());
				}
			}
			if (filtered.isEmpty()) {
				logger.debug("Config '{}' was accepted.", config.getKey());
				filteredConfigs.put(config.getKey(), config.getValue());
			} else {
				logger.debug("Config '{}' was filtered: {}", config.getKey(), filtered);
			}
		}
		return filteredConfigs;
	}
	
	private PublishConfigTemplateString getTemplateEvaluator(CmsItem item, PublishProfilingRecipe profiling) {
		PublishConfigTemplateString tmplStr = new PublishConfigTemplateString();
		tmplStr.withEntry("item", item);
		// Add profiling object, can be null;
		tmplStr.withEntry("profiling", profiling);
		return tmplStr;
	}
}

