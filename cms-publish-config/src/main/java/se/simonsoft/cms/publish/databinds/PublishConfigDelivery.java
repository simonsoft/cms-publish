package se.simonsoft.cms.publish.databinds;

public class PublishConfigDelivery {
	private String type;

	public PublishConfigDelivery(String type) {
		super();
		this.type = type;
	}
	public PublishConfigDelivery() {
		
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	@Override
	public String toString() {
		return "PublishConfigDelivery [type=" + type + ", getType()=" + getType() + "]";
	}
}
