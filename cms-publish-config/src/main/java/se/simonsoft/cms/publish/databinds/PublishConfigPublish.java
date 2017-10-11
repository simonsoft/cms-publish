package se.simonsoft.cms.publish.databinds;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=false)
public class PublishConfigPublish {

	private String type;
	private String format;
	private Map <String, String> params;
	private PublishConfigStorage storage;
	@JsonProperty("postprocess")
	private PublishConfigPostProcess postProcess;
	private PublishConfigDelivery delivery;
	
	public PublishConfigPublish() {

	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	public PublishConfigStorage getStorage() {
		return storage;
	}
	public void setStorage(PublishConfigStorage storage) {
		this.storage = storage;
	}
	public PublishConfigPostProcess getPostProcess() {
		return postProcess;
	}
	public void setPostProcess(PublishConfigPostProcess postProcess) {
		this.postProcess = postProcess;
	}
	public PublishConfigDelivery getDelivery() {
		return delivery;
	}
	public void setDelivery(PublishConfigDelivery delivery) {
		this.delivery = delivery;
	}
}
