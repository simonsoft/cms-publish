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

import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true) // Allow future changes.
public class PublishPackageOptions {

	private String publication;
	private boolean includeRelease;
	private boolean includeTranslations;
	private LinkedHashSet<String> profiling = null;
	
	
	public String getPublication() {
		return publication;
	}
	public void setPublication(String publication) {
		this.publication = publication;
	}
	public boolean isIncludeRelease() {
		return includeRelease;
	}
	public void setIncludeRelease(boolean includeRelease) {
		this.includeRelease = includeRelease;
	}
	public boolean isIncludeTranslations() {
		return includeTranslations;
	}
	public void setIncludeTranslations(boolean includeTranslations) {
		this.includeTranslations = includeTranslations;
	}
	public LinkedHashSet<String> getProfiling() {
		return profiling;
	}
	public void setProfiling(LinkedHashSet<String> profiling) {
		this.profiling = profiling;
	}
	
}
