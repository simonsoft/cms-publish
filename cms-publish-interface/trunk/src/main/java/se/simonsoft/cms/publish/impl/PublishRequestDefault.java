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
package se.simonsoft.cms.publish.impl;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishService;
import se.simonsoft.cms.publish.PublishSource;

/**
 * Default Publish Request (PE Request)
 * @author joakimdurehed
 *
 */
public class PublishRequestDefault implements PublishRequest {
	
	private Map<String, String> config = new HashMap<String, String>();
	private Map<String, String> params = new HashMap<String, String>();
	private PublishFormat format;
	private PublishSource publishSource;
	
	
	@Override
	public Map<String, String> getConfig() {
		return this.config;
	}

	@Override
	public PublishSource getFile() {	
		return this.publishSource;
	}

	@Override
	public PublishFormat getFormat() {
		return this.format; 
	}

	@Override
	public Map<String, String> getParams() {
		return this.params;  // TODO: return copy
	}
	
	// Add k,v config to config map
	public void addConfig(String key, String value){
		this.config.put(key, value);
	}
	// Add k,v param to param map
	public void addParam(String key, String value){
		this.params.put(key, value);
	}
	
	public void setFormat(PublishFormat format){
		// Just for now
		this.format = format;
		/*
		this.format =  new PublishFormat() {
			@Override
			public String getFormat() {
				// PDF format
				return "pdf";
			}

			@Override
			public Compression getOutputCompression() {
				return null;
			}
		};
		//*/
	}
	
	public void setFile(PublishSource source){
		this.publishSource = source;
	}

}
