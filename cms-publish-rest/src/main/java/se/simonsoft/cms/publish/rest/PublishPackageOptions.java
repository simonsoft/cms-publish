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
