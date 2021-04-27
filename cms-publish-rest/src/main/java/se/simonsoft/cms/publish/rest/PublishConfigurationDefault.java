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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.config.CmsConfigOption;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.export.CmsExportPrefix;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.publish.config.PublishConfiguration;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilter;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterActive;
import se.simonsoft.cms.publish.rest.config.filter.PublishConfigFilterVisible;
import se.simonsoft.cms.release.ReleaseProperties;
import se.simonsoft.cms.release.translation.TranslationLocalesMapping;
import se.simonsoft.cms.release.translation.TranslationLocalesMappingProvider;

public class PublishConfigurationDefault implements PublishConfiguration {
	
	private final CmsRepositoryLookup repositoryLookup;
	private final List<PublishConfigFilter> filters;
	private final PublishConfigFilter filterActive = new PublishConfigFilterActive();
	private final PublishConfigFilter filterVisible = new PublishConfigFilterVisible();
	private final ObjectReader readerConfig;
	private final ObjectReader readerProfiling;

	private static final String PUBLISH_CONFIG_KEY = "cmsconfig-publish";
	
	private static final Logger logger = LoggerFactory.getLogger(PublishConfigurationDefault.class);

	@Inject
	public PublishConfigurationDefault(
			@Named("global") CmsRepositoryLookup repositoryLookup,
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
		return deserializeConfig(context, true);
	}
	
	@Override
	public Map<String, PublishConfig> getConfigurationFiltered(CmsItemPublish item) {
		
		CmsResourceContext context = getConfigurationParentFolder(item.getId());
		Map<String, PublishConfig> allConfigs = deserializeConfig(context, true);
		return filterConfigs(item, allConfigs, null);
	}

	@Override
	public Map<String, PublishConfig> getConfigurationActive(CmsItemPublish item) {
		// Relaxed validation to ensure that valid configs are started by event.
		// TODO: Consider introducing a separate method / view specifically for validating the configs.
		CmsResourceContext context = getConfigurationParentFolder(item.getId());
		Map<String, PublishConfig> allConfigs = deserializeConfig(context, false);
		return filterConfigs(item, allConfigs, this.filterActive);
	}
	
	@Override
	public Map<String, PublishConfig> getConfigurationVisible(CmsItemPublish item) {
		// Strict validation to ensure that invalid configs are reported in UI.
		CmsResourceContext context = getConfigurationParentFolder(item.getId());
		Map<String, PublishConfig> allConfigs = deserializeConfig(context, true);
		return filterConfigs(item, allConfigs, this.filterVisible);
	}
	
	
	@Override
	public PublishProfilingSet getItemProfilingSet(CmsItemPublish itemPublish) {
		
		if (!itemPublish.hasProfiles()) {
			return null;
		}
		
		String profilesProp = itemPublish.getProperties().getString(ReleaseProperties.PROPNAME_PROFILING);
		try {
			PublishProfilingSet set = this.readerProfiling.readValue(profilesProp);
			// #1305: Filter recipes not intended for Publish.
			return set.getProfilingSetPublish();
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid property 'abx:Profiling': " + profilesProp);
		}
	}
	
	@Override
	public TranslationLocalesMapping getTranslationLocalesMapping(CmsItemId itemId) {
		CmsResourceContext context = getConfigurationParentFolder(itemId);
		return TranslationLocalesMappingProvider.getTranslationLocalesMapping(itemId.getRepository(), context);
	}
	
	
	private CmsResourceContext getConfigurationParentFolder(CmsItemId itemId) {
		
		// Getting config for the parent, which is always a Folder.
		CmsItemPath relPath = itemId.getRelPath().getParent();
		logger.debug("Configuration context: {} - ({})", relPath, itemId);
		CmsItemId folder = itemId.getRepository().getItemId(relPath, null); //Always getting config from HEAD.
		CmsResourceContext context = repositoryLookup.getConfig(folder, CmsItemKind.Folder);
		
		return context;
	}
	
	
	// When strict=false, configs that pass validation/parsing will be returned instead of exception.
	private Map<String, PublishConfig> deserializeConfig(CmsResourceContext context, boolean strict) {
		logger.debug("Starting deserialization of configs with namespace {}...", PUBLISH_CONFIG_KEY);
		Map<String, PublishConfig> configs = new TreeMap<>();
		Iterator<CmsConfigOption> iterator = context.iterator();
		while (iterator.hasNext()) {
			CmsConfigOption configOption = iterator.next();
			String configOptionNamespace = configOption.getNamespace();
			if (configOptionNamespace.startsWith(PUBLISH_CONFIG_KEY)) {
				try {
					// Export framework will validate the prefix when publishing / Export Publications.
					// Also validating early, to avoid starting incorrectly named configs.
					CmsExportPrefix key = new CmsExportPrefix(configOption.getKey());
					PublishConfig publishConfig = readerConfig.readValue(configOption.getValueString());
					configs.put(key.toString(), publishConfig);
				} catch (IllegalArgumentException e) {
					String msg = MessageFormatter.format("Illegal publish configuration name: '{}'", configOptionNamespace.concat(":" + configOption.getKey())).getMessage();
					logger.error(msg, e);
					if (strict) {
						throw new RuntimeException(msg);
					}
				} catch (JsonProcessingException e) {
					logger.error("Could not deserialize config: {} to new PublishConfig", configOptionNamespace.concat(":" + configOption.getKey()));
					String msg = MessageFormatter.format("Illegal publish configuration '{}':\n{}", configOptionNamespace.concat(":" + configOption.getKey()), e.getMessage()).getMessage();
					logger.error(msg, e);
					if (strict) {
						throw new RuntimeException(msg);
					}
				}
			}
		}
		
		logger.debug("Context had {} number of valid cmsconfig-publish objects", configs.size());
		
		return configs;
	}
	
	
	private Map<String, PublishConfig> filterConfigs(CmsItemPublish item, Map<String, PublishConfig> configs, PublishConfigFilter additional) {
		
		Map<String, PublishConfig> filteredConfigs = new TreeMap<String, PublishConfig>();
		for (Entry<String, PublishConfig> config: configs.entrySet()) {
			Set<PublishConfigFilter> filters = new LinkedHashSet<>(this.filters.size() + 1);
			filters.addAll(this.filters);
			if (additional != null) {
				filters.add(additional);
			}
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
