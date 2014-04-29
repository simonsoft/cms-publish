package se.simonsoft.cms.publish.abxpe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import se.repos.restclient.HttpStatusError;
import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestResponse;
import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishService;
import se.simonsoft.cms.publish.PublishTicket;



/**
 * Arbortext Publishing Engine implementation of PublishService
 * @author joakimdurehed
 *
 */
public class PublishingEngineService implements PublishService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private Set<PublishFormat> publishFormats;
	private String peUri = "/e3/servlet/e3";
	private RestClientJavaNet httpClient;
	public static final String PE_URL_ENCODING = "UTF-8";
	
	private File tempfile;
	
	public PublishingEngineService(){
		
		this.publishFormats = new HashSet<PublishFormat>();
		this.publishFormats.add(new PublishFormatPDF());
		this.publishFormats.add(new PublishFormatWeb());
		
	}
	
	@Override
	public Set<PublishFormat> getPublishFormats() {
		return this.publishFormats;
	}

	@Override
	public PublishFormat getPublishFormat(String format) {
		/*
		 *  PE Formats spec and content-type
		 *  dmp-input, dmp-image, dmp-web, dmp-help, and dmp-update returns application/zip
		 *  epub 		application/epub+zip
		 *  html		text/html
		 *  htmlhelp	application/octet-stream
		 *  pdf			application/pdf
		 *  postscript	application/postscript
		 *  rtf			text/rtf
		 *  sgml		text/sgml
		 *  web			application/zip
		 *  xml			text/xml
		 */
		int count = 0;
		for(PublishFormat pFormat: this.publishFormats)
		{
			if(pFormat.getFormat().equals(format)){
				return pFormat;
			}else{
				count++;
			}
		}
		
		// Todo. Make this a throws on method
		try { 
			// If we did not find any matching formats.
			if(count == this.publishFormats.size()){
			
				throw new PublishException(format + " is not supported by thus Publish Service");
			}
			} catch (PublishException e) {
				logger.info(e.getMessage());
				e.printStackTrace();
			}finally{
				return null;
			}
		
		
	}

	@Override
	/*
	 * Implements the requestPublish method that will send the Publish Request to Publishing Engine.
	 * (non-Javadoc)
	 * @see se.simonsoft.cms.publish.PublishService#requestPublish(se.simonsoft.cms.publish.PublishRequest)
	 */
	public PublishTicket requestPublish(PublishRequest request) {
		logger.debug("Start");
		// Create the uri
		StringBuffer uri = new StringBuffer();
		// Start with host and pe path
		uri.append(this.peUri);
		// General always valid params
		uri.append("?f=convert");// This is always a conversion operation
		uri.append("&response-format=xml"); // We always want a XML response to parse
		uri.append("&queue=yes"); // We always want to queue
		
		// Mandatory client params
		uri.append("&file=").append(urlencode(request.getFile().getURI()));// The file to convert
		//q.append("&type=").append(request.getFormat()); // The output type
		uri.append("&type=pdf");; // Only for now. Use above later.
		
		logger.debug("URI: " + uri.toString());
		
		// Additional params if there are any
		if(request.getParams().size() > 0){
			for(Map.Entry<String, String> entry: request.getParams().entrySet()){
				uri.append("&" + entry.getKey() + "=" + urlencode(entry.getValue()));
			}
		}

		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
		///*
		try {
			//*
			this.tempfile = File.createTempFile("se.simonsoft.publish.abxpe", "");
			logger.debug("Temp file crated at: " + this.tempfile.getAbsolutePath());
			final FileOutputStream fopStream = new FileOutputStream(this.tempfile);
		
			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.info("Got response from PE" );
						
					return fopStream; // The httpclient will stream the content to tempfile
				}
			});
			
			return this.getQueueTicket();
		} catch (HttpStatusError e) {
			logger.debug(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("finally")
	@Override
	public Boolean isCompleted(PublishTicket ticket, PublishRequest request) {
		logger.debug("Start");
		// Take the ticket and ask for the resulting document. 
		// The requests is probably not needed for PE.
		
		// Create the uri
		StringBuffer uri = new StringBuffer();
		// Start with host and pe path
		
		uri.append(this.peUri);
		
		// General always valid params
		uri.append("?&f=qt-status");// This is a status request
		uri.append("&response-format=xml"); // We always want a XML response to parse
		uri.append("&id=" + ticket.toString()); // And ask for the ticket id
		
		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
		
		try {
			
			this.tempfile = File.createTempFile("se.simonsoft.publish.abxpe", "");
			logger.debug("Temp file crated at: " + this.tempfile.getAbsolutePath());
			final FileOutputStream fopStream = new FileOutputStream(this.tempfile);

			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.info("Got response from PE" );
						
					return fopStream; // The httpclient will stream the content to tempfile
				}
			});
			
			// If we find that the response says Complete, return true
			if(this.parseResponse("Transaction", "state").equals("Complete")){
				return true;
			}
			
		} catch (HttpStatusError e) {
			try {
				throw new PublishException(e.getMessage());
			} catch (PublishException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			return false;
		}
	}

	@Override
	public void getResultStream(PublishTicket ticket, PublishRequest request, OutputStream outStream) {
		
		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
		
		// Create the uri
		StringBuffer uri = new StringBuffer();
		// Start with host and pe path
		
		uri.append(this.peUri);
		
		// General always valid params
		uri.append("?&f=qt-retrieve");// The retrieve req
		uri.append("&id=" + ticket.toString()); // And ask for publication with ticket id
		
		final OutputStream outputStream = outStream;
		
		try {
			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.info("Got response from PE" );
						
					return outputStream; // The httpclient will stream the content to tempfile
				}
			});
		
	
		} catch (HttpStatusError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void getLogStream(PublishTicket ticket, PublishRequest request,
			OutputStream outStream) {
		// TODO Auto-generated method stub

	}
	
	/*
	 * Method to parse the response XML from PE and find specific values on
	 * on a specific element (in our case the Transaction ele
	 * @author jdurehed
	 */
	private String parseResponse(String element, String attribute){
		logger.debug("Start");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			// Start to parse the response
	    	Document doc = db.parse(this.tempfile);
	    	Node node = doc.getElementsByTagName(element).item(0);
			Element foundElement = (Element)node;
			String attributeValue = foundElement.getAttribute(attribute);
			logger.debug("attributeValue: " + attributeValue);
			return attributeValue;
						
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("End");
		return null;
	}
	/*
	 * Method to parse the response XML from PE and get the queue ID.
	 * @author jdurehed
	 */
	private PublishTicket getQueueTicket(){
		logger.debug("Start");
		PublishTicket queueTicket = new PublishTicket(this.parseResponse("Transaction", "id"));
		logger.debug("End");
		return queueTicket;
	}
	
	protected String urlencode(String value) {
		try {
			return URLEncoder.encode(value, PE_URL_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// encoding is a constant so we must consider this a fatal runtime environment issue
			throw new RuntimeException("Unexpected JVM behavior: failed to encode URL using " + PE_URL_ENCODING, e);
		}
	}
}
