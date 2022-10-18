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
package se.simonsoft.cms.publish.config.databinds.job;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.workflow.WorkflowItemInputUserId;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJob extends PublishConfig implements WorkflowItemInputUserId {

	private String configname;
	private String type;
	private String action;
	private PublishConfigArea area;
	private String itemid;
	private String userId;
	private String userRoles;
	private PublishJobOptions options;

	public PublishJob(PublishJob pj) {
		this.configname = pj.getConfigname();
		this.type = pj.getType();
		this.action = pj.getAction();
		this.area = pj.getArea();
		this.itemid = pj.getItemid();
		
		this.options = pj.getOptions();
		
		this.active = pj.isActive();
		this.visible = pj.isVisible();
		this.exportable = pj.isExportable();
		this.statusInclude = pj.getStatusInclude();
		this.elementNameInclude = pj.getElementNameInclude();
		this.typeInclude = pj.getTypeInclude();
		this.profilingInclude = pj.getProfilingInclude();
		this.profilingNameInclude = pj.getProfilingNameInclude();
		this.areaMainInclude = pj.isAreaMainInclude();
	}
	
	public PublishJob(PublishConfig pc) {
		
		this.options = new PublishJobOptions(pc.getOptions());
		
		this.active = pc.isActive();
		this.visible = pc.isVisible();
		this.exportable = pc.isExportable();
		this.statusInclude = pc.getStatusInclude();
		this.elementNameInclude = pc.getElementNameInclude();
		this.typeInclude = pc.getTypeInclude();
		this.profilingInclude = pc.getProfilingInclude();
		this.profilingNameInclude = pc.getProfilingNameInclude();
		this.areaMainInclude = pc.isAreaMainInclude();
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
	public PublishConfigArea getArea() {
		return area;
	}

	public void setArea(PublishConfigArea area) {
		this.area = area;
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
	public void setOptions(PublishJobOptions options) {
		this.options = options;
	}


	@Override
	@JsonIgnore // Suppress description from PublishJob JSON, not needed.
	public String getDescription() {
		return null;
	}

	@Override
	@JsonIgnore
	public CmsItemId getItemId() {
		return new CmsItemIdArg(this.itemid);
	}

	@Override
	@JsonGetter("userid") // Defined by the interface if the writer configure forType(WorkflowItemInputUserId.class). 
	public String getUserId() {
		return this.userId;
	}

	@Override
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@Override
	@JsonGetter("userroles") // Defined by the interface if the writer configure forType(WorkflowItemInputUserId.class). 
	public String getUserRoles() {
		return this.userRoles;
	}

	@Override
	public void setUserRoles(String userRoles) {
		this.userRoles = userRoles;
	}
}
