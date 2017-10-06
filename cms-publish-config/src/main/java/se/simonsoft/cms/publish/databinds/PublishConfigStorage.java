package se.simonsoft.cms.publish.databinds;

public class PublishConfigStorage {
	private String type;
	private PublishConfigParams params;
	
	public PublishConfigStorage(String type, PublishConfigParams params) {
		super();
		this.type = type;
		this.params = params;
	}
	public PublishConfigStorage() {
		
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public PublishConfigParams getParams() {
		return params;
	}
	public void setParams(PublishConfigParams params) {
		this.params = params;
	}
	@Override
	public String toString() {
		return "PublishConfigStorage [type=" + type + ", params=" + params + "]";
	}
	
}
