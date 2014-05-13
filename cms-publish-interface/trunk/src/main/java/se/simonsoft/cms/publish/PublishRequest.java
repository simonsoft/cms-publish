/*******************************************************************************
 * Copyright 2014 Simonsoft Nordic AB
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
