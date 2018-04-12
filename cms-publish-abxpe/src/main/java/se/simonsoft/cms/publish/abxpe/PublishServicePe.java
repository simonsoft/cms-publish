/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.publish.abxpe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
 */
public class PublishServicePe implements PublishService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private Set<PublishFormat> publishFormats;
	private String peUri = "/e3/servlet/e3";
	private RestClientJavaNet httpClient; 
	public static final String PE_URL_ENCODING = "UTF-8";
	
	public PublishServicePe(){
		this.publishFormats = new HashSet<PublishFormat>();
		this.publishFormats.add(new PublishFormatPDF());
		this.publishFormats.add(new PublishFormatWeb());
		this.publishFormats.add(new PublishFormatHTML());
		this.publishFormats.add(new PublishFormatXML());
		this.publishFormats.add(new PublishFormatRTF());	
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
		
		// TODO  Refactor: a nicer/better looking way of getting format
		try { 
			// If we did not find any matching formats.
			if(count == this.publishFormats.size()){
				throw new PublishException(format + " is not supported by the Publish Service");
			}
			} catch (PublishException e) {
				logger.info(e.getMessage());
				e.printStackTrace();
			}
		
		return null;
		
		
	}

	@Override
	public PublishTicket requestPublish(PublishRequest request) throws PublishException {
		logger.debug("Start");
		// Create the uri
		StringBuffer uri = new StringBuffer();
		// Start with host and pe path
		uri.append(this.peUri);
		// General always valid params
		uri.append("?f=convert");// This is always a convert operation
		uri.append("&response-format=xml"); // We always want a XML response to parse
		uri.append("&queue=yes"); // We always want to queue
		
		// Mandatory client params
		uri.append("&file=").append(urlencode(request.getFile().getURI()));// The file to convert
		uri.append("&type=").append(request.getFormat().getFormat()); // The output type
		
		logger.debug("URI: " + uri.toString());
		
		// Additional params if there are any
		if(request.getParams().size() > 0){
			for(Map.Entry<String, String> entry: request.getParams().entrySet()){
				uri.append("&" + entry.getKey() + "=" + urlencode(entry.getValue()));
			}
		}

		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
	
		try {
			
			final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
	
			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.debug("Got response with headers {}", headers);
					return byteOutputStream; // Returns to our outputstream
				}
			});
			// Keeps response in memory, BUT, in this case we know that response will not be to large
			return this.getQueueTicket( new ByteArrayInputStream(byteOutputStream.toByteArray())); 
		} catch (HttpStatusError e) {
			logger.debug("Publication Error! \n Response: {} \nStacktrace:{} ", e.getResponse(), e.getStackTrace());
			throw new PublishException("Publishing failed with message: " + e.getMessage(), e);
		} catch (IOException e) {
			logger.debug("IOException: Message: {}", e.getMessage(), e);
			throw new PublishException("Publishing failed (" + e.getClass().getName() + "): " + e.getMessage(), e);
		}
	}


	@Override
	public Boolean isCompleted(PublishTicket ticket, PublishRequest request) throws PublishException {
		logger.debug("Start");
		
		// Create the uri
		StringBuffer uri = new StringBuffer();
		boolean result = false;
		// Start with host and pe path
		
		uri.append(this.peUri);
		
		// Params for status check
		uri.append("?&f=qt-status");// This is a status request
		uri.append("&response-format=xml"); // We always want a XML response to parse
		uri.append("&id=" + ticket.toString()); // And ask publish with ticket id
		
		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
		
		try {
		
			final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
			
			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.debug("Got response from PE with headers {}", headers);
					return byteOutputStream; // The httpclient will stream the content to tempfile
				}
			});
			// Keeps response in memory, BUT, in this case we know that response will not be to large
			// If we find that the response says Complete, return true
			String parseResult = this.parseResponse("Transaction", "state", new ByteArrayInputStream(byteOutputStream.toByteArray()));
			if(parseResult.equals("Complete") || parseResult.equals("Cancelled")){
				result = true;
			}
			
			if (result) {
				StringBuffer jobHeadRequestUri = new StringBuffer(this.peUri);
				jobHeadRequestUri.append("?&f=qt-retrieve");// The retrieve req
				jobHeadRequestUri.append("&id=" + ticket.toString()); // And ask for publication with ticket id
				
				logger.debug("PE job status is complete. Doing a retrieve HEAD request to ensure the job is possible to retrieve");
				
				ResponseHeaders head = this.httpClient.head(jobHeadRequestUri.toString());
				byteOutputStream.reset();
				
				if (head.getStatus() != 200) {
					getErrorResponse(jobHeadRequestUri.toString(), byteOutputStream);
				}
			}
			
		} catch (HttpStatusError e) {
			throw new PublishException("Publishing failed with message: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException("Publishing Engine communication failed", e);
		}
		return result;
	}
	
	/**
	 * It is recommended to run a isComplete before trying request the job. If something is wrong e.g
	 * job is missing it will only return 500 response without any human readable message
	 */
	@Override
	public void getResultStream(PublishTicket ticket, PublishRequest request, OutputStream outStream) throws PublishException {
		
		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
		
			// Create the uri
		StringBuffer uri = new StringBuffer();
		uri.append(this.peUri);

		// General always mandatory params
		uri.append("?&f=qt-retrieve");// The retrieve req
		uri.append("&id=" + ticket.toString()); // And ask for publication with ticket id
		
		final OutputStream outputStream = outStream;
		
		try {
			//What if the job do no exist. This code will probably not handle that. Does it get a response body or does it throw a exception?
			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.debug("Got response from PE with status: " + headers.getStatus());
						
					return outputStream; // The httpclient will stream the content to tempfile
				}
			});
			
		} catch (HttpStatusError e) {
			logger.error("Error when trying to get completed job: {}", e.getMessage());
			throw new PublishException("Failed to get job with ticket: " + ticket + " check if job is completed before requesting job.", e);
		} catch (IOException e) {
			throw new RuntimeException("Publishing Engine communication failed", e);
		}
	}
	
	@Override
	public void getLogStream(PublishTicket ticket, PublishRequest request,
			OutputStream outStream) {
		this.httpClient = new RestClientJavaNet(request.getConfig().get("host"), null);
		// THIS IS NOT WORKING YET. 
		// Create the uri
		StringBuffer uri = new StringBuffer();
		uri.append(this.peUri);

		// General always mandatory params
		uri.append("?&f=qt-retrieve");// The retrieve req
		uri.append("&id=" + ticket.toString()); // And ask for publication with ticket id

		final OutputStream outputStream = outStream;

		try {
			this.httpClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
					logger.info("Got response from PE with status: " + headers.getStatus());

					return outputStream; // The httpclient will stream the content to tempfile
				}
			});

		} catch (HttpStatusError e) {
			//throw new PublishException("Publishing failed with message: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException("Publishing Engine communication failed", e);
		}
	}
	
	/**
	 * Parse the response XML from PE looking for specified attribtue value on specified element
	 * Is dependent on PE response XML not changing. 
	 * @param element
	 * @param attribute
	 * @param content
	 * @return
	 * @throws PublishException 
	 */
	private String parseResponse(String element, String attribute, InputStream content) throws PublishException{
		logger.debug("Start");
		logger.debug("Start parse response");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			
			DocumentBuilder db = dbf.newDocumentBuilder();
			// Start to parse the response
	    	Document doc = db.parse(content);
	    	
	    	String message;
	    	NodeList messageNode = doc.getElementsByTagName("Message");
	    	if (messageNode.getLength() <= 0) {
	    		message = "Failed with unknown reason";
	    	} else {
	    		message = messageNode.item(0).getTextContent();
	    	}
	    	
	    	Node node = doc.getElementsByTagName(element).item(0);
			Element foundElement = (Element) node;

			String attributeValue = null;
			if (foundElement != null) { // Not shure if the null check should be here.
				attributeValue = foundElement.getAttribute(attribute);
			} else {
				throw new PublishException(message);
			}
			
			logger.debug("{}:{} ",attribute, attributeValue);
			
			return attributeValue;
						
			// TODO handle the exceptions in some better way?
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.debug("End parse response");
		return null;
	}
	
	/**
	 * Use only when we now the response body is a PE failed HTML response. 
	 * The method is based on the assumption that all error body's first p element contains the error message (Should only be one p element).
	 * @param responseBody
	 * @return
	 */
	private String parseErrorResponseBody(String responseBody) {
		
		// all content between p tags, ok with new lines.
		final Pattern pattern = Pattern.compile("<p>[\\s\\S](.+?)[\\s\\S]</p>"); 
		final Matcher matcher = pattern.matcher(responseBody);
		matcher.find();
		return matcher.group(1);
	}
	
	private void getErrorResponse(String requestUri, final ByteArrayOutputStream baos) throws IOException, PublishException {
		
		try {
			this.httpClient.get(requestUri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
					logger.debug("Got response from PE with headers {}", headers);
					return baos; // The output stream will be empty, since this request always fails.
				}
			});
			
		//We now that this request will fail. The reponse body is included in the exception.
		} catch (HttpStatusError e) {
			String response = e.getResponse();
			String errorMessage = parseErrorResponseBody(response);
			throw new PublishException(errorMessage);
		}
	}
	
	
	/**
	 * Parse repsonse XML to get queue ID
	 * @param response
	 * @return
	 */
	private PublishTicket getQueueTicket(InputStream response) throws PublishException {
		logger.debug("Start");
		PublishTicket queueTicket = new PublishTicket(this.parseResponse("Transaction", "id", response));
		logger.debug("End");
		return queueTicket;
	}
	
	/**
	 * URL encode value
	 * @param value
	 * @return encoded value
	 */
	protected String urlencode(String value) {
		try {
			return URLEncoder.encode(value, PE_URL_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// encoding is a constant so we must consider this a fatal runtime environment issue
			throw new RuntimeException("Unexpected JVM behavior: failed to encode URL using " + PE_URL_ENCODING, e);
		}
	}
}
