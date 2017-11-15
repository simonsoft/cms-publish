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
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigOptions;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigStorage;

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
	
	public PublishJob(PublishConfig publishConfig) {
		
// 		Following setters in PublishJob can not be satisfied by PublishConfig alone.		
//		this.type = publishConfig.get
//		this.configname = publishConfig.getOptions().get
//		this.action = publishConfig.getAction();
//		this.itemid = publishConfig.getItemid();
		
		PublishJobOptions publishJobOptions = new PublishJobOptions();
		
		PublishJobDelivery publishJobDelivery = new PublishJobDelivery();
		publishJobDelivery.setParams(publishConfig.getOptions().getDelivery().getParams());
		publishJobDelivery.setType(publishConfig.getOptions().getDelivery().getType());
		
		publishJobOptions.setDelivery(publishJobDelivery);
		publishJobOptions.setFormat(publishConfig.getOptions().getFormat());
		publishJobOptions.setParams(publishConfig.getOptions().getParams());
		
		PublishJobPostProcess publishJobPostProcess = new PublishJobPostProcess();
		publishJobPostProcess.setParams(publishConfig.getOptions().getPostprocess().getParams());
		publishJobPostProcess.setType(publishConfig.getOptions().getPostprocess().getType());
		publishJobOptions.setPostprocess(publishJobPostProcess);
		
		PublishJobStorage storage = new PublishJobStorage();
		storage.setParams(publishConfig.getOptions().getStorage().getParams());
		storage.setType(publishConfig.getOptions().getStorage().getType());
//		storage.setPathconfigname(confStorage.get);
//		storage.setPathdir(pathdir);
//		storage.setPathnamebase(pathnamebase);
//		storage.setPathprefix(pathprefix);
		publishJobOptions.setStorage(storage);
		publishJobOptions.setType(publishConfig.getOptions().getType());
		
		
// 		Following setters in option can not be satisfied by PublishConfig alone. 		
//		this.options.setPathname();
//		this.options.setProfiling();
//		this.options.setProgress();
//		this.options.setReport3();
		
		this.options = publishJobOptions;
		this.active = publishConfig.isActive();
		this.visible = publishConfig.isVisible();
		this.statusInclude = publishConfig.getStatusInclude();
		this.profilingInclude = publishConfig.getProfilingInclude();
		this.pathnameTemplate = publishConfig.getPathnameTemplate();
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
