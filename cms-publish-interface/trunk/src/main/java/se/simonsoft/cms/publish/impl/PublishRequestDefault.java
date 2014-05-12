package se.simonsoft.cms.publish.impl;

import java.io.OutputStream;
import java.util.Map;

import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishSource;

/**
 * Default Publish Request (PE Request)
 * @author joakimdurehed
 *
 */
public class PublishRequestDefault implements PublishRequest {
	
	private Map<String, String> config;
	private Map<String, String> params;
	private PublishFormat format;
	private PublishSource publishSource;
	
	
	@Override
	public Map<String, String> getConfig() {
		return this.config;
	}

	@Override
	public PublishSource getFile() {	
		return this.publishSource;
	}

	@Override
	public PublishFormat getFormat() {
		return this.format;
	}

	@Override
	public Map<String, String> getParams() {
		return this.params;
	}
	
	public void setConfig(Map<String, String> config){
		this.config = config;
	}
	
	public void setParams(Map<String, String> params){
		this.params = params;
	}
	
	public void setFormat(String format){
		// Just for now
		this.format =  new PublishFormat() {
			@Override
			public String getFormat() {
				// PDF format
				return "pdf";
			}

			@Override
			public Compression getOutputCompression() {
				return null;
			}
		};
	}
	
	public void setFile(PublishSource source){
		this.publishSource = source;
	}

}
