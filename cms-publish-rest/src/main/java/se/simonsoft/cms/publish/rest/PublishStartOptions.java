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
package se.simonsoft.cms.publish.rest;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;

@JsonIgnoreProperties(ignoreUnknown = true) // Allow future changes.
public class PublishStartOptions {
	
	private String publication; // The config name
	private String locale; // Select Release if empty or equal to release language.
	private String profilingname; // One of profilingname/startprofiling.
	private PublishProfilingRecipe startprofiling;
	private LinkedHashMap<String, String> startcustom; // Override manifest.custom on a field-by-field basis. Validate max length in the service, alternatively size of the whole options JSON.
	private String startpathname; // Validate max length in the service.

	private String executionid; // Added by the REST Service / Step Functions definition.

	
	
	public String getPublication() {
		return publication;
	}

	public void setPublication(String publication) {
		this.publication = publication;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getProfilingname() {
		return profilingname;
	}

	public void setProfilingname(String profilingname) {
		this.profilingname = profilingname;
	}

	public PublishProfilingRecipe getStartprofiling() {
		return startprofiling;
	}

	public void setStartprofiling(PublishProfilingRecipe startprofiling) {
		this.startprofiling = startprofiling;
	}

	public LinkedHashMap<String, String> getStartcustom() {
		return startcustom;
	}
	
	public void setStartcustom(LinkedHashMap<String, String> startcustom) {
		this.startcustom = startcustom;
	}

	public String getStartpathname() {
		return startpathname;
	}

	public void setStartpathname(String startpathname) {
		this.startpathname = startpathname;
	}

	public String getExecutionid() {
		return executionid;
	}

	public void setExecutionid(String executionid) {
		this.executionid = executionid;
	}
}
