package se.simonsoft.cms.publish.databinds;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublishConfigPublish {
	
	private String type;
	private String format;
	private PublishConfigParams params;
	private PublishConfigStorage storage;
	@JsonProperty("postprocess")
	private PublishConfigPostProcess postProcess;
	private PublishConfigDelivery delivery;
	
	public PublishConfigPublish(String type, String format, PublishConfigParams params, PublishConfigStorage storage,
			PublishConfigPostProcess postProcess, PublishConfigDelivery delivery) {
		super();
		this.type = type;
		this.format = format;
		this.params = params;
		this.storage = storage;
		this.postProcess = postProcess;
		this.delivery = delivery;
	}
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
	public PublishConfigParams getParams() {
		return params;
	}
	public void setParams(PublishConfigParams params) {
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
	@Override
	public String toString() {
		return "PublishConfigPublish [type=" + type + ", format=" + format + ", params=" + params + ", storage="
				+ storage + ", postProcess=" + postProcess + ", delivery=" + delivery + "]";
	}
}
