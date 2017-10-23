package se.simonsoft.cms.publish.databinds.publish.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties (ignoreUnknown=false)
public class PublishConfigPostProcess {
	private String type;
	private Map <String, String> params;
	
	public PublishConfigPostProcess() { }
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
}
