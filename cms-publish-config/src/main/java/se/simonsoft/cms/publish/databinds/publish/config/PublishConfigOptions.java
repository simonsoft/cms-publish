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
package se.simonsoft.cms.publish.databinds.publish.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties (ignoreUnknown=false)
public class PublishConfigOptions {

	private String type;
	private String format;
	private Map <String, String> params = new HashMap<>();
	private PublishConfigStorage storage;
	private PublishConfigPostProcess postprocess;
	private PublishConfigDelivery delivery;
	private PublishConfigManifest manifest;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	public PublishConfigStorage getStorage() {
		return storage;
	}
	public void setStorage(PublishConfigStorage storage) {
		this.storage = storage;
	}
	public PublishConfigPostProcess getPostprocess() {
		return postprocess;
	}
	public void setPostprocess(PublishConfigPostProcess postProcess) {
		this.postprocess = postProcess;
	}
	public PublishConfigDelivery getDelivery() {
		return delivery;
	}
	public void setDelivery(PublishConfigDelivery delivery) {
		this.delivery = delivery;
	}
	public PublishConfigManifest getManifest() {
		return manifest;
	}
	public void setManifest(PublishConfigManifest manifest) {
		this.manifest = manifest;
	}
}
