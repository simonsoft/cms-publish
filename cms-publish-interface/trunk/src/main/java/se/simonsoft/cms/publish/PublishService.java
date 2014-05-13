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

import java.io.OutputStream;
import java.util.Set;

/**
 * Represents a service that can publish a file into one or many formats.
 * Both the config and the per-publish parameters are provided in each PublishRequest.
 * 
 * Each publication request produces a single output file, which might be an archive of publication files.
 * @author takesson
 *
 */
public interface PublishService {

	/**
	 * Useful for generic user interfaces to list supported formats.
	 * This is a forward looking method that may throw UnsupportedOperation at this time.
	 * @return Non exhaustive list of formats that the service supports.
	 */
	public Set<PublishFormat> getPublishFormats();
	
	/**
	 * 
	 * @param format as a string
	 * @return the PublishFormat that specifies how the specific service provides that format. 
	 * @throws PublishException if the format is not supported by the service
	 */
	public PublishFormat getPublishFormat(String format);

	/**
	 * Start publishing a CMS item into a specified format. 
	 * The publish job will be added to the queue (implementation specific).
	 * A PublishTicket will be added to the request.
	 * @param item
	 * @param format
	 * @param params
	 * @return
	 */
	PublishTicket requestPublish(PublishRequest request);
	
	
	/**
	 * @param ticket
	 * @param request
	 * @return true if the publish has completed, which does not necessarily mean it was successful.
	 */
	Boolean isCompleted(PublishTicket ticket, PublishRequest request);

	
	/** Provide the result as a stream.
	 * @param ticket
	 * @param request
	 * @param outStream where the result will be written
	 * @throws PublishException if the publish was not successful
	 */
	void getResultStream(PublishTicket ticket, PublishRequest request, OutputStream outStream) throws PublishException;



	/** Provide the log as a stream. Might be empty if the publish was a successful. 
	 * These have the concept: PE, Saxon, DITA-OT (lots of console, any readable?), Jenkins (console) 
	 */
	void getLogStream(PublishTicket ticket, PublishRequest request, OutputStream outStream);

	
	
	
}
