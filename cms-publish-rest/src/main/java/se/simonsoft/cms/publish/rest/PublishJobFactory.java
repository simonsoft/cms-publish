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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishJobFactory {


	private final String cloudId;
	private final String type = "publish-job";
	private final String action = "publish-preprocess"; // Preprocess is the first stage in Workflow (CMS 4.4), can potentially request webapp work (depends on preprocess.type).
	private final PublishJobStorageFactory storageFactory;
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishJobFactory.class);

	@Inject
	public PublishJobFactory(
			@Named("config:se.simonsoft.cms.cloudid") String cloudId,
			PublishJobStorageFactory storageFactory) {
		
		this.cloudId = cloudId;
		this.storageFactory = storageFactory;
	}
	
	
	
	public List<PublishJob> getPublishJobsProfiling(CmsItemPublish itemPublish, PublishConfig config, String configName, PublishProfilingSet profilingSet) {
		List<PublishJob> profiledJobs = new ArrayList<PublishJob>();
				
		for (PublishProfilingRecipe profilesRecipe: profilingSet) {
			List<String> profilingNames = config.getProfilingNameInclude();
			// Filter on profilesNameInclude if set.
			if (profilingNames == null || profilingNames.contains(profilesRecipe.getName())) {
				profiledJobs.add(getPublishJob(itemPublish, config, configName, profilesRecipe));
			}
		}
		return profiledJobs;
	}
	

	public PublishJob getPublishJob(CmsItemPublish item, PublishConfig c, String configName, PublishProfilingRecipe profiling) {
		PublishConfigTemplateString templateEvaluator = getTemplateEvaluator(item, configName, profiling);
		PublishJobManifestBuilder manifestBuilder = new PublishJobManifestBuilder(templateEvaluator);
		
		PublishConfigArea area = PublishJobManifestBuilder.getArea(item, c.getAreas());
		PublishJob pj = new PublishJob(c);
		pj.setArea(area); 
		pj.setItemid(item.getId().getLogicalIdFull()); // Event workflow has itemid hostname so publish workflow should as well.
		pj.setAction(this.action); 
		pj.setType(this.type);
		pj.setConfigname(configName);
		
		// The source attribute is used by publishing engines with the ability to directly fetch data from the CMS.
		// When Preprocess is configured, the source attribute should be null because the preprocessed / exported data should be used instead.
		if (pj.getOptions().getPreprocess().getType() == null || pj.getOptions().getPreprocess().getType().equals("none")) {
			pj.getOptions().setSource(item.getId().getLogicalId());			
		}
		if (profiling != null) {
			pj.getOptions().setProfiling(profiling);
		}
		
		PublishConfigStorage configStorage = pj.getOptions().getStorage();
		PublishJobStorage storage = storageFactory.getInstance(configStorage, item, configName, profiling);
		pj.getOptions().setStorage(storage);
		
		String pathname = templateEvaluator.evaluate(area.getPathnameTemplate());
		pj.getOptions().setPathname(pathname);
		
		// Build the Manifest, modifies the existing manifest object.
		manifestBuilder.build(item, pj);
		
		// Decided that template evaluation of params is NOT the long term solution, at least for now.
		// Distribution implementations should use the Manifest for template evaluation, similar to external consumers.
		// Evaluate Velocity for params
		// Normally we evaluate config fields '...Templates'.
		
		// Need to refresh the template evaluator to contain the storage object.
		// Decided against exposing the storage object. This is internal and should be understood by workflow Tasks.
		/*
		templateEvaluator = getTemplateEvaluator(item, profiling, storage);
		manifestBuilder = new PublishJobManifestBuilder(templateEvaluator);
		*/
		// Decided that template evaluation of params is NOT the long term solution. At least for now.
		// The below temporary implementation only evaluates the options.params map, not deeper options.*.params maps. 
		/*
		pj.getOptions().setParams(manifestBuilder.buildMap(item, pj.getOptions().getParams()));
		*/
		
		logger.debug("Created PublishJob from config: {}", configName);
		return pj;
	}
	

	
	private PublishConfigTemplateString getTemplateEvaluator(CmsItemPublish item, String configName, PublishProfilingRecipe profiling/*, PublishJobStorage storage*/) {
		PublishConfigTemplateString tmplStr = new PublishConfigTemplateString();
		// Define "$aptpath" transparently to allow strict references without escape requirement in JSON.
		// Important if allowing evaluation of params in the future.
		tmplStr.withEntry("aptpath", "$aptpath");
		// Add the cloudid, might be useful for delivery.
		tmplStr.withEntry("cloudid", this.cloudId);
		// Add config name, might be useful for delivery.
		tmplStr.withEntry("configname", configName);
		// Add the item
		tmplStr.withEntry("item", item);
		// Add profiling object, can be null;
		tmplStr.withEntry("profiling", profiling);
		// Add storage object to allow configuration of parameters with S3 key etc.
		//tmplStr.withEntry("storage", storage);
		return tmplStr;
	}
}

