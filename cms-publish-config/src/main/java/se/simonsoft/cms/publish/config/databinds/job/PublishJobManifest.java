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

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfigManifest;


@JsonIgnoreProperties (ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishJobManifest extends PublishConfigManifest {
	
	private LinkedHashMap<String, String> job = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> document = null;
	private LinkedHashMap<String, String> master = null;
	private LinkedHashMap<String, String> custom = null;
	private LinkedHashMap<String, String> meta = null;
	
	public PublishJobManifest() {
		super();
	}
	
	public PublishJobManifest(PublishConfigManifest config) {
		
		if (config != null) {
			setCustomTemplates(config.getCustomTemplates());
			setMetaTemplates(config.getMetaTemplates());
			setType(config.getType());
			setPathext(config.getPathext());
			setTemplate(config.getTemplate());
		}
	}
	
	
	public PublishJobManifest forPublish() {
		
		PublishJobManifest r = new PublishJobManifest();
		// Explicitly not setting type.
		r.setJob(new LinkedHashMap<String, String>(job));
		
		if (document != null) {
			r.setDocument(new LinkedHashMap<String, String>(document));
		}
		if (master != null) {
			r.setMaster(new LinkedHashMap<String, String>(master));
		}
		if (custom != null) {
			r.setCustom(new LinkedHashMap<String, String>(custom));
		}
		if (meta != null) {
			r.setMeta(new LinkedHashMap<String, String>(meta));
		}
		
		return r;
	}
	
	
	@JsonIgnore // Not included in job
	public LinkedHashMap<String, String> getCustomTemplates() {
		return customTemplates;
	}

	@JsonIgnore // Not included in job
	public LinkedHashMap<String, String> getMetaTemplates() {
		return metaTemplates;
	}

	public LinkedHashMap<String, String> getJob() {
		return this.job;
	}

	public void setJob(LinkedHashMap<String, String> job) {
		this.job = job;
	}

	public LinkedHashMap<String, String> getDocument() {
		return this.document;
	}

	public void setDocument(LinkedHashMap<String, String> document) {
		this.document = document;
	}

	public LinkedHashMap<String, String> getMaster() {
		return this.master;
	}

	public void setMaster(LinkedHashMap<String, String> master) {
		this.master = master;
	}

	public LinkedHashMap<String, String> getCustom() {
		return this.custom;
	}

	public void setCustom(LinkedHashMap<String, String> custom) {
		this.custom = custom;
	}

	public LinkedHashMap<String, String> getMeta() {
		return this.meta;
	}

	public void setMeta(LinkedHashMap<String, String> meta) {
		this.meta = meta;
	}

}
