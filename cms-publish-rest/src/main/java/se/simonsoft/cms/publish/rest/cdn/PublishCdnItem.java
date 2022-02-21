package se.simonsoft.cms.publish.rest.cdn;

import se.simonsoft.cms.item.CmsItemId;

public class PublishCdnItem {

	private CmsItemId itemId;
	private String cdn;
	private String pathformat;
	
	
	public CmsItemId getItemId() {
		return itemId;
	}
	public void setItemId(CmsItemId itemId) {
		this.itemId = itemId;
	}
	public String getCdn() {
		return cdn;
	}
	public void setCdn(String cdn) {
		this.cdn = cdn;
	}
	public String getPathformat() {
		return pathformat;
	}
	public void setPathformat(String pathformat) {
		this.pathformat = pathformat;
	}
	
	
	
}
