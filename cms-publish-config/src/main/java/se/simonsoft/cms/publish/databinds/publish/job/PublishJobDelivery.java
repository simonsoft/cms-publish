package se.simonsoft.cms.publish.databinds.publish.job;

import java.util.Map;

public class PublishJobDelivery {
	private String type;
	private Map<String, String> params;

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
