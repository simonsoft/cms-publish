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

}
