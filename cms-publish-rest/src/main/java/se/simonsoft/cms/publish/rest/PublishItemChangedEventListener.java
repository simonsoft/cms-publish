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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.config.CmsConfigOption;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.events.ItemChangedEventListener;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.config.filter.PublishConfigFilter;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJob;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobStorage;
import se.simonsoft.cms.reporting.rest.itemlist.ItemIdListForPresentation;

public class PublishItemChangedEventListener implements ItemChangedEventListener {

	private final CmsRepositoryLookup lookup;
	private final WorkflowExecutor<WorkflowItemInput> workflowExecutor;
	
	private final List<PublishConfigFilter> filters;
	private final ObjectReader reader;
	
	private final String pathVersion = "cms4";
	private final String s3Bucket = "cms-automation";
	private final String type = "publish-job";
	
	private static final String PUBLISH_CONFIG_KEY = "cmsconfig-publish";  
	
	private static final Logger logger = LoggerFactory.getLogger(PublishItemChangedEventListener.class);

	@Inject
	public PublishItemChangedEventListener(
			CmsRepositoryLookup lookup,
			@Named("config:se.simonsoft.cms.aws.publish.workflow") WorkflowExecutor<WorkflowItemInput> workflowExecutor,
			List<PublishConfigFilter> filters,
			ObjectReader reader) {
		
		this.lookup = lookup;
		this.workflowExecutor = workflowExecutor;
		this.filters = filters;
		this.reader = reader.forType(PublishConfig.class);
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
		
		List<PublishJob> jobs = new ArrayList<PublishJob>();
		Iterator<String> iterator = publishConfigs.keySet().iterator();
		while (iterator.hasNext()) {
			String configName = iterator.next();
			PublishJob pj = getPublishJob(item, publishConfigs.get(configName), configName);
			jobs.add(pj);
		}
		
		logger.debug("Starting executions for {} number of PublishJobs", jobs.size());
		for (PublishJob job: jobs) {
			workflowExecutor.startExecution(job);
		}
	}
	
	private PublishJob getPublishJob(CmsItem item, PublishConfig c, String configName) {
		PublishJob pj = new PublishJob(c);
		pj.setItemid(item.getId().getLogicalId());
		pj.setAction("publish-noop");
		pj.setType(this.type);
		pj.setConfigname(configName);
		
		pj.getOptions().setSource(item.getId().getLogicalId());;
		
		PublishJobStorage storage = pj.getOptions().getStorage();
		storage.setPathdir(item.getId().getRelPath().getPath());
		storage.setPathnamebase(getNameBase(item.getId()));
		storage.setPathversion(this.pathVersion);
		storage.setPathconfigname(configName);
		storage.setPathcloudid(item.getId().getRepository().getName());
		if (!storage.getParams().containsKey("s3bucket")) {
			storage.getParams().put("s3bucket", this.s3Bucket);
		}
		
		String pathname = evaluatePathNameTmpl(item, c.getPathnameTemplate());
		pj.getOptions().setPathname(pathname);
		
		logger.debug("Created PublishJob from config: {}", configName);
		return pj;
	}
	
	public String getNameBase(CmsItemId itemId) {
		String idRevision = String.format("_r%010d", itemId.getPegRev());
		return itemId.getRelPath().getNameBase().concat(idRevision);
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
					PublishConfig publishConfig = reader.readValue(configOption.getValueString());
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
		
		Iterator<String> iterator = configs.keySet().iterator();
		Map<String, PublishConfig> filteredConfigs = new LinkedHashMap<String, PublishConfig>();
		while (iterator.hasNext()) {
			String next = iterator.next();
			for (PublishConfigFilter f: filters) {
				if (f.accept(configs.get(next), item)) {
					logger.debug("Config {} where accepted.", next);
					filteredConfigs.put(next, configs.get(next));
				}
			}
		}
		return configs;
	}
	
	private String evaluatePathNameTmpl(CmsItem item, String template) {
		PublishConfigTemplateString tmplStr = new PublishConfigTemplateString(template);
		tmplStr.withEntry("item", item);
		return tmplStr.evaluate();
	}
}

