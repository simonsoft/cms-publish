package se.simonsoft.cms.publish.databinds;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublishConfigParams {
	private String stylesheet;
	@JsonProperty ("pdfconfig")
	private String pdfConfig;
	private String whatever;
	private String specific;
	
	public PublishConfigParams(String stylesheet, String pdfConfig, String whatever, String specific) {
		super();
		this.stylesheet = stylesheet;
		this.pdfConfig = pdfConfig;
		this.whatever = whatever;
		this.specific = specific;
	}
	public PublishConfigParams() {
		
	}
	public String getStylesheet() {
		return stylesheet;
	}
	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}
	public String getPdfConfig() {
		return pdfConfig;
	}
	public void setPdfConfig(String pdfConfig) {
		this.pdfConfig = pdfConfig;
	}
	public String getWhatever() {
		return whatever;
	}
	public void setWhatever(String whatever) {
		this.whatever = whatever;
	}
	public String getSpecific() {
		return specific;
	}
	public void setSpecific(String specific) {
		this.specific = specific;
	}
	@Override
	public String toString() {
		return "PublishConfigParams [stylesheet=" + stylesheet + ", pdfConfig=" + pdfConfig + ", whatever=" + whatever
				+ ", specific=" + specific + "]";
	}
}
