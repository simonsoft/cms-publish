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
import se.simonsoft.cms.publish.workflow.WorkflowItemInputPublish;

public class PublishItemChangedEventListener implements ItemChangedEventListener {

	private final CmsRepositoryLookup lookup;
	private final WorkflowExecutor<WorkflowItemInput> workflowExecutor;
	
	private List<PublishConfigFilter> filters;
	private ObjectReader reader;
	
	private final String pathPrefix = "/cms4";
	private final String s3Bucket = "cms-automation";
	
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
		CmsResourceContext context = this.lookup.getConfig(item.getId(), item.getKind());
		
		Map<String, PublishConfig> publishConfigs = deserializeConfig(context);
		publishConfigs = filterConfigs(publishConfigs, item);
		
		Iterator<String> iterator = publishConfigs.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			executeJobWithConfig(key, publishConfigs.get(key), item);
		}
	}
	
	private void executeJobWithConfig(String configName, PublishConfig c, CmsItem item) {
		logger.debug("Building PublishJob and starting execution...");
		
		PublishJob pj = new PublishJob(c);
		pj.setItemid(item.getId().getLogicalId());
		pj.setAction("publish-noop");
		pj.setType("publish-job");
		pj.setConfigname(configName);
		
		PublishJobStorage storage = pj.getOptions().getStorage();
		storage.setPathdir(item.getId().getRelPath().getPath());
		storage.setPathnamebase(item.getId().getRelPath().getNameBase());
		storage.setPathprefix(this.pathPrefix);
		storage.setPathconfigname("/".concat(configName));
		if (!storage.getParams().containsKey("s3bucket")) {
			storage.getParams().put("s3bucket", this.s3Bucket);
		}
		
		//TODO: set reporting3 should be json string from reporting service.
		
		String pathname = evaluatePathNameTmpl(c.getPathnameTemplate(), item);
		pj.getOptions().setPathname(pathname);
		
		workflowExecutor.startExecution(new WorkflowItemInputPublish(item.getId(), pj));
		logger.debug("Execution started.");
	}

	private Map<String, PublishConfig> deserializeConfig(CmsResourceContext context) {
		logger.debug("Starting deserialization of configs with namespace {}...", PUBLISH_CONFIG_KEY);
		Map<String, PublishConfig> configs = new LinkedHashMap<>();
		Iterator<CmsConfigOption> iterator = context.iterator();
		while (iterator.hasNext()) {
			CmsConfigOption next = iterator.next();
			String configOptionName = next.getNamespace();
			if (configOptionName.startsWith(PUBLISH_CONFIG_KEY)) {
				try {
					PublishConfig publishConfig = reader.readValue(next.getValueString());
					configs.put(next.getKey(), publishConfig);
				} catch (JsonProcessingException e) {
					logger.error("Could not deserialize config: {} to new PublishConfig", configOptionName.concat(":" + next.getKey()));
					throw new RuntimeException(e);
				} catch (IOException e) {
					logger.error("Could not deserialize config: {} to new PublishConfig", configOptionName);
					throw new RuntimeException(e);
				}
			}
		}
		
		logger.debug("Context had {} number of valid cmsconfig-publish objects", configs.size());
		
		return configs;
	}
	
	private Map<String, PublishConfig> filterConfigs(Map<String, PublishConfig> configs, CmsItem item) {
		
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
	
	private String evaluatePathNameTmpl(String template, CmsItem item) {
		PublishConfigTemplateString tmplStr = new PublishConfigTemplateString(template);
		tmplStr.withEntry("item", item);
		return tmplStr.evaluate();
	}
}

