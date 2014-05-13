package se.simonsoft.cms.publish.impl;

import java.io.OutputStream;
import java.util.HashMap;
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
	
	private Map<String, String> config = new HashMap<String, String>();
	private Map<String, String> params = new HashMap<String, String>();
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
		return this.format; // TODO: return copy
	}

	@Override
	public Map<String, String> getParams() {
		return this.params;  // TODO: return copy
	}
	
	// Add k,v config to config map
	public void addConfig(String key, String value){
		this.config.put(key, value);
	}
	// Add k,v param to param map
	public void addParam(String key, String value){
		this.params.put(key, value);
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
