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

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties (ignoreUnknown=true)
public class PublishConfigManifest {
	
	private String type = null;
	private String template = null;
	private String pathext = null;
	protected LinkedHashMap<String, String> customTemplates = new LinkedHashMap<String, String>();
	protected LinkedHashMap<String, String> metaTemplates = new LinkedHashMap<String, String>();
	
	public String getType() {
		return this.type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public LinkedHashMap<String, String> getCustomTemplates() {
		return customTemplates;
	}
	
	public void setCustomTemplates(LinkedHashMap<String, String> customTemplates) {
		this.customTemplates = customTemplates;
	}

	public LinkedHashMap<String, String> getMetaTemplates() {
		return metaTemplates;
	}

	public void setMetaTemplates(LinkedHashMap<String, String> metaTemplates) {
		this.metaTemplates = metaTemplates;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getPathext() {
		return pathext;
	}

	public void setPathext(String pathext) {
		this.pathext = pathext;
	}
	
}
