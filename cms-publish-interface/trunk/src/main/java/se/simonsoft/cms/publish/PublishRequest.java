package se.simonsoft.cms.publish;

import java.util.Map;

/**
 * Passed to {@link PublishService} to request a job.
 */
public interface PublishRequest {

	
	/**
	 * Configuration of the Publish Service, e.g. URL, credentials etc.
	 * @return all config parameters for the service
	 */
	public Map<String,String> getConfig();
	
	/**
	 * @return the file to be published, typically a CmsItemId but could be any other 
	 * url/path that the service understands and can access. 
	 */
	public PublishSource getFile();
	
	
	/**
	 * @return the requested output format
	 */
	public PublishFormat getFormat();
	
	
	/**
	 * @return all publish parameters except the format
	 */
	public Map<String,String> getParams();
	
}
