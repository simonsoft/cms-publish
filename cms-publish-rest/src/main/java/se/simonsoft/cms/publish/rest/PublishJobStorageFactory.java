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

import javax.inject.Inject;
import javax.inject.Named;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigStorage;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishJobStorageFactory {
	
	private final String pathVersion = "cms4";
	private final String s3bucket = "cms-automation"; //TODO: Bucket may be injected.
	
	private final String cloudId;

	@Inject
	public PublishJobStorageFactory(@Named("config:se.simonsoft.cms.cloudid") String cloudId) {
		
		this.cloudId = cloudId;
	}
	
	public PublishJobStorage getInstance(PublishConfigStorage c, CmsItemPublish item, String configName, PublishProfilingRecipe profiling) {
		
		if (item == null) {
			throw new IllegalArgumentException("PublishJobStorageFactory needs a valid CmsItemPublish: " + item);
		}
		
		if (configName == null || configName.isEmpty()) {
			throw new IllegalArgumentException("PublishJobStorageFactory needs a valid configName: " + configName);
		}
		
		PublishJobStorage s = new PublishJobStorage();
		
		s.setPathdir(item.getId().getRelPath().getPath());
		s.setPathnamebase(getNameBase(item.getId(), profiling));
		s.setPathversion(pathVersion);
		s.setPathconfigname(configName);
		s.setPathcloudid(cloudId);
		
		if (c != null && c.getType() != null) {
			s.setType(c.getType());
			
			if (c.getType().equals("s3")) {
				s.getParams().put("s3bucket", s3bucket);
			}
		}
		
		return s;
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
}
