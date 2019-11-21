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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishJobManifestBuilder {

	private final PublishConfigTemplateString templateEvaluator;
	
	private static final String DEFAULT_TYPE = "default";
	private static final String DEFAULT_PATHEXT = "json";
	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobManifestBuilder.class);
	
	
	public PublishJobManifestBuilder(PublishConfigTemplateString templateEvaluator) {
	
		this.templateEvaluator = templateEvaluator;
	}

	
	public void build(CmsItemPublish item, PublishJob job) {
		
		PublishJobManifest manifest = job.getOptions().getManifest();
		
		if (manifest.getType() == null) {
			manifest.setType(DEFAULT_TYPE);
		}
		
		if (manifest.getPathext() == null) {
			manifest.setPathext(DEFAULT_PATHEXT);
		}
		
		manifest.setJob(buildJob(item, job));
		
		if (hasDocnoTemplate(job)) {
			manifest.setDocument(buildDocument(item, job));
			if (hasDocnoMasterTemplate(job) && isTranslation(item)) {
				manifest.setMaster(buildMaster(item, job));
			} else {
				manifest.setMaster(null);
			}
			manifest.setCustom(buildMap(item, manifest.getCustomTemplates()));
			manifest.setMeta(buildMap(item, manifest.getMetaTemplates()));
		} else {
			// Prevent incomplete manifest when docno has not been configured.
			manifest.setDocument(null);
			manifest.setMaster(null);
			manifest.setCustom(null);
			manifest.setMeta(null);
		}
	}
	
	
	private LinkedHashMap<String, String> buildJob(CmsItemPublish item, PublishJob job) {
		
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		result.put("configname", job.getConfigname());
		result.put("format", job.getOptions().getFormat());
		result.put("itemid", job.getItemId().getLogicalId());
		
		return result;
	}
	
	
	private LinkedHashMap<String, String> buildDocument(CmsItemPublish item, PublishJob job) {
	
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		String docnoTemplate = job.getArea().getDocnoDocumentTemplate();
		
		String docno = templateEvaluator.evaluate(docnoTemplate);
		result.put("docno", docno);
		if (item.isRelease() || item.isTranslation()) {
			result.put("versionrelease", item.getReleaseLabel());
		}
		result.put("versioniteration", String.format("%010d", job.getItemId().getPegRev()));
		
		result.put("status", item.getStatus());
		if (item.isRelease()) {
			String releaseLocale = item.getReleaseLocale(); // Available on Release since CMS 4.3 only.
			if (releaseLocale == null) {
				logger.warn("Fallback to abx:lang property for item: {}", item.getId());
				releaseLocale = item.getProperties().getString("abx:lang");
			}
			result.put("lang", releaseLocale);
		} else if (item.isTranslation()) {
			result.put("lang", item.getTranslationLocale());
		} // Currently no lang attribute from author area, no guarantee that abx:lang exists.
		
		return result;
	}
	
	
	private LinkedHashMap<String, String> buildMaster(CmsItemPublish item, PublishJob job) {
		
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		String docnoTemplate = job.getArea().getDocnoMasterTemplate();
		
		String docno = templateEvaluator.evaluate(docnoTemplate);
		result.put("docno", docno);
		result.put("versionrelease", item.getReleaseLabel());
		result.put("lang", item.getReleaseLocale()); // Available on Translations since CMS 3.0.2
		
		return result;
	}
	
	
	public LinkedHashMap<String, String> buildMap(CmsItemPublish item, Map<String, String> map) {
		
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if (map == null || map.isEmpty()) {
			return null;
		}
		
		for (Entry<String, String> entry: map.entrySet()) {
			result.put(entry.getKey(), templateEvaluator.evaluate(entry.getValue()));
		}
		
		return result;
	}
	
	
	private boolean hasDocnoTemplate(PublishJob job) {
	
		String docnoTemplate = job.getArea().getDocnoDocumentTemplate();
		if (docnoTemplate == null || docnoTemplate.trim().isEmpty()) {
			return false;
		}
		return true;
	}
	
	private boolean hasDocnoMasterTemplate(PublishJob job) {
		
		String docnoTemplate = job.getArea().getDocnoMasterTemplate();
		if (docnoTemplate == null || docnoTemplate.trim().isEmpty()) {
			return false;
		}
		return true;
	}
	
	private boolean isTranslation(CmsItem item) {
		
		String locale = item.getProperties().getString("abx:TranslationLocale");
		return (locale != null && !locale.isEmpty());
	}
	
	
	public static PublishConfigArea getArea(CmsItemPublish item, List<PublishConfigArea> areas) {
		
		HashMap<String, PublishConfigArea> areaMap = getAreaMap(areas);
		
		PublishConfigArea fallback = areaMap.get(null);
		
		if (item.isTranslation() && areaMap.containsKey("translation")) {
			return areaMap.get("translation");
		} else if (item.isRelease() && areaMap.containsKey("release")) {
			return areaMap.get("release");
		} else if (fallback != null) {
			return fallback;
		// Differentiating error messages to simplify troubleshooting.	
		} else if (item.isTranslation()) {
			throw new IllegalArgumentException("No fallback area configured, item is a Translation.");
		} else if (item.isRelease()) {
			throw new IllegalArgumentException("No fallback area configured, item is a Release.");
		} else {
			throw new IllegalArgumentException("No fallback area configured, item is NOT a Release / Translation.");
		}
	}
	
	
	private static HashMap<String, PublishConfigArea> getAreaMap(List<PublishConfigArea> areas) {
		
		HashMap<String, PublishConfigArea> result = new HashMap<String, PublishConfigArea>(areas.size());
		
		for (PublishConfigArea area: areas) {
			String type = area.getType();
			
			PublishConfigArea prev = result.put(type, area);
			if (prev != null) {
				throw new IllegalArgumentException("Duplicate area objects with type: " + type);
			}
		}
		return result;
	}
}
