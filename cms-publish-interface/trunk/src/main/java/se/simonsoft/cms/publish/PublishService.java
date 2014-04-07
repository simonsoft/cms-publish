package se.simonsoft.cms.publish;

import java.io.OutputStream;

/**
 * Represents a service that can publish a file into one or many formats.
 * Both the config and the per-publish parameters are provided in each PublishRequest.
 * @author takesson
 *
 */
public interface PublishService {

	
	
	/* Will all publish services actually be able to enumerate formats?
	public Set<PublishFormat> getPublishFormats()
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
	
	
	Boolean isCompleted(PublishTicket ticket, PublishRequest request);

	// Is a stream the way we want to get the result? Also as a File?
	void getResultStream(PublishTicket ticket, PublishRequest request, OutputStream outStream);



	/** investigate if we can provide the publish log this way. These have the concept: PE, Saxon, DITA-OT (lots of console, any readable?), Jenkins (console) */
	void getLogStream(PublishTicket ticket, PublishRequest request, OutputStream outStream);

	
	
	
}
