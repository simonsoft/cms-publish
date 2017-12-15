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
	protected List<String> statusInclude;
	protected List<String> typeInclude;
	protected List<String> profilingNameInclude = new ArrayList<String>();
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
	public List<String> getStatusInclude() {
		return statusInclude;
	}
	public void setStatusInclude(List<String> statusInclude) {
		this.statusInclude = statusInclude;
	}
	public List<String> getTypeInclude() {
		return this.typeInclude;
	}
	public void setTypeInclude(List<String> typeInclude) {
		this.typeInclude = typeInclude;
	}
	public List<String> getProfilingNameInclude() {
		return profilingNameInclude;
	}
	public void setProfilingNameInclude(List<String> profilingInclude) {
		this.profilingNameInclude = profilingInclude;
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
