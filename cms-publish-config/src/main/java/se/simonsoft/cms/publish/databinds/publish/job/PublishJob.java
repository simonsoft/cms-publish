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
package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJob extends PublishConfig {

	private String configname;
	private String type;
	private String action;
	private String itemid;
	private PublishJobOptions options;
	private String pathnameTemplate;

	public PublishJob(PublishJob pj) {
		this.configname = pj.getConfigname();
		this.type = pj.getType();
		this.action = pj.getAction();
		this.itemid = pj.getItemid();
		this.options = pj.getOptions();
		this.active = pj.isActive();
		this.visible = pj.isVisible();
		this.statusInclude = pj.getStatusInclude();
		this.profilingInclude = pj.getProfilingInclude();
		this.pathnameTemplate = pj.getPathnameTemplate();
	}
	public PublishJob() {
		super();
	}
	public String getConfigname() {
		return configname;
	}
	public void setConfigname(String configname) {
		this.configname = configname;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getItemid() {
		return itemid;
	}
	public void setItemid(String itemid) {
		this.itemid = itemid;
	}
	public PublishJobOptions getOptions() {
		return options;
	}
	public void setOptions(PublishJobOptions publish) {
		this.options = publish;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	public List<String> getStatusInclude() {
		return statusInclude;
	}
	public void setStatusInclude(List<String> statucInclude) {
		this.statusInclude = statucInclude;
	}
	public List<String> getProfilingInclude() {
		return profilingInclude;
	}
	public void setProfilingInclude(List<String> profilingInclude) {
		this.profilingInclude = profilingInclude;
	}
	public String getPathnameTemplate() {
		return pathnameTemplate;
	}
	public void setPathnameTemplate(String pathnameTemplate) {
		this.pathnameTemplate = pathnameTemplate;
	}
}
