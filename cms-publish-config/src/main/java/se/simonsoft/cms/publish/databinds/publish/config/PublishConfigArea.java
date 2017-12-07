package se.simonsoft.cms.publish.databinds.publish.config;

public class PublishConfigArea {

	private String type = null;
	private String pathnameTemplate;
	private String docnoDocumentTemplate;
	private String docnoMasterTemplate;
	
	public PublishConfigArea() {
		
	}
	
	
	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public String getPathnameTemplate() {
		return this.pathnameTemplate;
	}

	public void setPathnameTemplate(String pathnameTemplate) {
		this.pathnameTemplate = pathnameTemplate;
	}

	public String getDocnoDocumentTemplate() {
		return this.docnoDocumentTemplate;
	}

	public void setDocnoDocumentTemplate(String docnoDocumentTemplate) {
		this.docnoDocumentTemplate = docnoDocumentTemplate;
	}

	public String getDocnoMasterTemplate() {
		return this.docnoMasterTemplate;
	}

	public void setDocnoMasterTemplate(String docnoMasterTemplate) {
		this.docnoMasterTemplate = docnoMasterTemplate;
	}

	
	


}
