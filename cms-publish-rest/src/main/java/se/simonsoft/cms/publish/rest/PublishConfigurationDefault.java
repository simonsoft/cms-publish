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
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.config.CmsConfigOption;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.release.ReleaseProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

public class PublishConfigurationDefault implements PublishConfiguration {
	
	private final CmsRepositoryLookup repositoryLookup;
	private final List<PublishConfigFilter> filters;
	private final ObjectReader readerConfig;
	private final ObjectReader readerProfiling;

	private static final String PUBLISH_CONFIG_KEY = "cmsconfig-publish";
	
	private static final Logger logger = LoggerFactory.getLogger(PublishConfigurationDefault.class);

	@Inject
	public PublishConfigurationDefault(
			CmsRepositoryLookup repositoryLookup,
			List<PublishConfigFilter> filters,
			ObjectReader reader
			) {
		
		this.repositoryLookup = repositoryLookup;
		this.filters = filters;
		this.readerConfig = reader.forType(PublishConfig.class);
		this.readerProfiling = reader.forType(PublishProfilingSet.class);
	}

	

	@Override
	public Map<String, PublishConfig> getConfiguration(CmsItemId itemId) {
		
		CmsResourceContext context = getConfigurationParentFolder(itemId);
		return deserializeConfig(context);
	}
	
	
	public Map<String, PublishConfig> getConfigurationFiltered(CmsItemPublish item) {
		
		CmsResourceContext context = getConfigurationParentFolder(item.getId());
		Map<String, PublishConfig> allConfigs = deserializeConfig(context);
		return filterConfigs(item, allConfigs);
	}
	
	
	public PublishProfilingSet getItemProfilingSet(CmsItemPublish itemPublish) {
		
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
	
	
	private CmsResourceContext getConfigurationParentFolder(CmsItemId itemId) {
		
		// Getting config for the parent, which is always a Folder.
		CmsItemPath relPath = itemId.getRelPath().getParent();
		logger.debug("Configuration context: {} - ({})", relPath, itemId);
		CmsItemId folder = itemId.getRepository().getItemId(relPath, null); //Always getting config from HEAD.
		CmsResourceContext context = repositoryLookup.getConfig(folder, CmsItemKind.Folder);
		
		return context;
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
	
	private Map<String, PublishConfig> filterConfigs(CmsItemPublish item, Map<String, PublishConfig> configs) {
		
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


	
}
