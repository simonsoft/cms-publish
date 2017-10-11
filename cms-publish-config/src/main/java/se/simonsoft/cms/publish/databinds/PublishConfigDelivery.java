package se.simonsoft.cms.publish.databinds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=false)
public class PublishConfigDelivery {
	private String type;

	public PublishConfigDelivery() {
		super();
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
}