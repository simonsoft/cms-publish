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
package se.simonsoft.cms.publish.config.databinds.config;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=false)
public class PublishConfig {

	protected boolean active;
	protected boolean visible;
	protected boolean exportable = true;
	protected String description; // Display in dialog, suppress in PublishJob.
	protected List<String> statusInclude;
	protected List<String> elementNameInclude;
	protected List<String> typeInclude;
	protected Boolean profilingInclude;
	protected List<String> profilingNameInclude = null; // No array means include all profiling recipes.
	protected boolean areaMainInclude = false; // Disable Main / Author area by default.
	private List<PublishConfigArea> areas = new ArrayList<PublishConfigArea>();
	private PublishConfigOptions options;
	
	public PublishConfig(PublishConfig pc) {
		this.active = pc.isActive();
		this.visible = pc.isVisible();
		this.statusInclude = pc.getStatusInclude();
		this.profilingNameInclude = pc.getProfilingNameInclude();
		this.areas = pc.getAreas();
		this.options = pc.getOptions();
	}
	public PublishConfig() {
		
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
	public boolean isExportable() {
		return exportable;
	}
	public void setExportable(boolean exportable) {
		this.exportable = exportable;
	}
	/**
	 * Description is available on the PublishConfig and suppressed for PublishJob.
	 * @return description intended for user (null on PublishJob)
	 */
	public String getDescription() {
		return this.description;
	}
	public List<String> getStatusInclude() {
		return statusInclude;
	}
	public void setStatusInclude(List<String> statusInclude) {
		this.statusInclude = statusInclude;
	}
	public List<String> getElementNameInclude() {
		return this.elementNameInclude;
	}
	public void setElementNameInclude(List<String> elementNameInclude) {
		this.elementNameInclude = elementNameInclude;
	}
	public List<String> getTypeInclude() {
		return this.typeInclude;
	}
	public void setTypeInclude(List<String> typeInclude) {
		this.typeInclude = typeInclude;
	}
	public Boolean getProfilingInclude() {
		return this.profilingInclude;
	}
	public void setProfilingInclude(Boolean profilingInclude) {
		this.profilingInclude = profilingInclude;
	}
	public List<String> getProfilingNameInclude() {
		return profilingNameInclude;
	}
	public void setProfilingNameInclude(List<String> profilingInclude) {
		this.profilingNameInclude = profilingInclude;
	}
	public boolean isAreaMainInclude() {
		return this.areaMainInclude;
	}
	public void setAreaMainInclude(boolean areaMainInclude) {
		this.areaMainInclude = areaMainInclude;
	}
	public List<PublishConfigArea> getAreas() {
		return areas;
	}
	public void setAreas(List<PublishConfigArea> areas) {
		this.areas = areas;
	}
	public PublishConfigOptions getOptions() {
		return options;
	}
	public void setOptions(PublishConfigOptions options) {
		this.options = options;
	}
}
