package se.simonsoft.cms.publish.databinds.publish.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties (ignoreUnknown=true)
public class PublishConfigManifest {
	
	private Map<String, String> customTemplates;
	private Map<String, String> metaTemplates;
	
	public Map<String, String> getCustomTemplates() {
		return customTemplates;
	}
	
	public void setCustomTemplates(Map<String, String> customTemplates) {
		this.customTemplates = customTemplates;
	}

	public Map<String, String> getMetaTemplates() {
		return metaTemplates;
	}

	public void setMetaTemplates(Map<String, String> metaTemplates) {
		this.metaTemplates = metaTemplates;
	}
}
