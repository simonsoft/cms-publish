package se.simonsoft.cms.publish.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;

@JsonIgnoreProperties(ignoreUnknown = true) // Allow future changes.
public class PublishStartOptions {
	
	private String publication; // The config name
	private String profilingname; // One of profilingname/profilingrecipe.
	private PublishProfilingRecipe profilingrecipe;
	private String locale; // Select Release if empty or equal to release language.
	private String startinput; // Validate max length in the service.

	public String getPublication() {
		return publication;
	}
	public void setPublication(String publication) {
		this.publication = publication;
	}
	public String getProfilingname() {
		return profilingname;
	}
	public void setProfilingname(String profilingname) {
		this.profilingname = profilingname;
	}
	public PublishProfilingRecipe getProfilingrecipe() {
		return profilingrecipe;
	}
	public void setProfilingrecipe(PublishProfilingRecipe profilingrecipe) {
		this.profilingrecipe = profilingrecipe;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getStartinput() {
		return startinput;
	}
	public void setStartinput(String startinput) {
		this.startinput = startinput;
	}
}
