package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublishJobPathName {
	@JsonProperty("items#")
	private List<Object> items;
}
