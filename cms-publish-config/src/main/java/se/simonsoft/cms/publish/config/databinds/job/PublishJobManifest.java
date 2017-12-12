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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfigManifest;


@JsonIgnoreProperties (ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishJobManifest extends PublishConfigManifest {
	
	private String type = null;
	private Map<String, String> job = new HashMap<String, String>();
	private Map<String, String> document = new HashMap<String, String>();;
	private Map<String, String> master = null;
	private Map<String, String> custom = null;
	private Map<String, String> meta = null;
	
	public PublishJobManifest() {
		super();
	}
	
	public PublishJobManifest(PublishConfigManifest config) {
		
		if (config != null) {
			setCustomTemplates(config.getCustomTemplates());
			setMetaTemplates(config.getMetaTemplates());
		}
	}
	
	
	public PublishJobManifest forPublish() {
		
		PublishJobManifest r = new PublishJobManifest();
		// Explicitly not setting type.
		r.setJob(new HashMap<String, String>(job));
		r.setDocument(new HashMap<String, String>(document));
		

		if (master != null) {
			r.setMaster(new HashMap<String, String>(master));
		}
		if (custom != null) {
			r.setCustom(new HashMap<String, String>(custom));
		}
		if (meta != null) {
			r.setMeta(new HashMap<String, String>(meta));
		}
		
		return r;
	}
	
	
	@JsonIgnore // Not included in job
	public Map<String, String> getCustomTemplates() {
		return customTemplates;
	}

	@JsonIgnore // Not included in job
	public Map<String, String> getMetaTemplates() {
		return metaTemplates;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getJob() {
		return this.job;
	}

	public void setJob(Map<String, String> job) {
		this.job = job;
	}

	public Map<String, String> getDocument() {
		return this.document;
	}

	public void setDocument(Map<String, String> document) {
		this.document = document;
	}

	public Map<String, String> getMaster() {
		return this.master;
	}

	public void setMaster(Map<String, String> master) {
		this.master = master;
	}

	public Map<String, String> getCustom() {
		return this.custom;
	}

	public void setCustom(Map<String, String> custom) {
		this.custom = custom;
	}

	public Map<String, String> getMeta() {
		return this.meta;
	}

	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}

}
