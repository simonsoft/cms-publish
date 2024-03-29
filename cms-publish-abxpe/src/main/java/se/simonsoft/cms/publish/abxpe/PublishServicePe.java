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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
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
import se.repos.restclient.javase.RestClientJavaHttp;
import se.simonsoft.cms.item.stream.ByteArrayInOutStream;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishFormat;
import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishService;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishTicket;



/**
 * Arbortext Publishing Engine implementation of PublishService
 */
public class PublishServicePe implements PublishService {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private Set<PublishFormat> publishFormats;
	private String serverRootUrl;
	private String peUri = "/e3/servlet/e3";
	private RestClientJavaHttp restClient; 
	public static final String PE_URL_ENCODING = "UTF-8";
	
	public PublishServicePe(String serverRootUrl){
		this.publishFormats = new HashSet<PublishFormat>();
		this.publishFormats.add(new PublishFormatPDF());
		this.publishFormats.add(new PublishFormatPostscript());
		this.publishFormats.add(new PublishFormatWeb());
		this.publishFormats.add(new PublishFormatHTML());
		this.publishFormats.add(new PublishFormatXML());
		this.publishFormats.add(new PublishFormatRTF());
	
		this.serverRootUrl = serverRootUrl;
		this.restClient = new RestClientJavaHttp(serverRootUrl, null);
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

		for (PublishFormat pFormat : this.publishFormats) {
			if (pFormat.getFormat().equals(format)) {
				return pFormat;
			}
		}
		
		logger.info("Publish format fallback: {}", format);
		return new PublishFormatFallback(format);
	}

	@Override
	public PublishTicket requestPublish(PublishRequest request) throws PublishException {
		
		PublishTicket ticket;
		if (request.getFile().getURI() != null) {
			ticket = requestPublishGet(request);
		} else {
			ticket = requestPublishPost(request);
		}
		if (ticket == null) {
			throw new IllegalStateException("No ticket returned from PE");
		}
		return ticket;
	}
	
	
	// Deprecated
	public PublishTicket requestPublishGet(PublishRequest request) throws PublishException {
		logger.trace("Start");
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
		uri.append("&file-type=zip");
		uri.append("&type=").append(request.getFormat().getFormat()); // The output type
		
		logger.debug("URI: " + uri.toString());
		
		// Additional params if there are any
		if(request.getParams().size() > 0){
			for(Map.Entry<String, String> entry: request.getParams().entrySet()){
				uri.append("&" + entry.getKey() + "=" + urlencode(entry.getValue()));
			}
		}

		try {
			final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
	
			this.restClient.get(uri.toString(), new RestResponse() {
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
	

	public PublishTicket requestPublishPost(PublishRequest request) throws PublishException {
		logger.trace("Start");
		PublishSource source = request.getFile();
		Supplier<InputStream> inputStream = Objects.requireNonNull(source.getInputStream(), "Source InputStream must not be null");
		Long inputLength = Objects.requireNonNull(source.getInputLength(), "Source InputLength must not be null (chunked POST not supported by PE)");
		
		// Create the uri
		StringBuffer uri = new StringBuffer();
		// Start with host and pe path
		uri.append(this.peUri);
		// General always valid params
		uri.append("?f=convert");// This is always a convert operation
		// PE 8.1.3.1 fails when POSTing in combination with response-format=xml.
		//uri.append("&response-format=xml"); // We always want a XML response to parse
		uri.append("&queue=yes"); // We always want to queue
		
		// Mandatory client params
		uri.append("&type=").append(request.getFormat().getFormat()); // The output type
		
		String contentType = "application/xml";
		if (source.getInputEntry() != null) {
			uri.append("&file-type=zip"); // Assume zip, the only supported archive format.
			uri.append("&input-entry=").append(source.getInputEntry());
			contentType = "application/zip";
		}
		
		logger.debug("URI: " + uri.toString());
		
		// Additional params if there are any
		if (request.getParams().size() > 0) {
			for(Map.Entry<String, String> entry: request.getParams().entrySet()){
				uri.append("&" + entry.getKey() + "=" + urlencode(entry.getValue()));
			}
		}

		try {
			logger.info("POSTing source to PE: {}", uri);
			HttpClient httpClient = this.restClient.getClientPost();
			
			BodyPublisher bpis = HttpRequest.BodyPublishers.ofInputStream(inputStream);
			BodyPublisher bpisWithLength = HttpRequest.BodyPublishers.fromPublisher(bpis, inputLength);
			
	        HttpRequest postRequest = HttpRequest.newBuilder()
	                .POST(bpisWithLength)
	                .uri(URI.create(this.serverRootUrl + uri.toString()))
	                .header("Content-Type", contentType)
	                .build();

	        HttpResponse<String> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

	        logger.info("POSTed source to PE: {}", response.body());
	        // Get ticket ID from the location header.
			return this.getQueueTicket(response.headers()); 
		} catch (HttpStatusError e) {
			logger.debug("Publication Error! \n Response: {} \nStacktrace:{} ", e.getResponse(), e.getStackTrace());
			throw new PublishException("Publishing failed with message: " + e.getMessage(), e);
		} catch (IOException e) {
			logger.debug("IOException: Message: {}", e.getMessage(), e);
			
			IOException exception = this.restClient.check(e);
			
			throw new PublishException("Publishing failed (" + exception.getClass().getName() + "): " + exception.getMessage(), exception);
		} catch (InterruptedException e) {
			logger.error("Interrupted: {}", e.getMessage(), e.getStackTrace());
			throw new PublishException("Publishing failed (" + e.getClass().getName() + "): " + e.getMessage(), e);
		}
	}


	@Override
	public Boolean isCompleted(PublishTicket ticket, PublishRequest request) throws PublishException {
		logger.trace("Start");
		
		// Create the uri
		StringBuffer uri = new StringBuffer();
		boolean result = false;
		// Start with host and pe path
		
		uri.append(this.peUri);
		
		// Params for status check
		uri.append("?&f=qt-status");// This is a status request
		uri.append("&response-format=xml"); // We always want a XML response to parse
		uri.append("&id=" + ticket.toString()); // And ask publish with ticket id
		
		try {
		
			final ByteArrayInOutStream baios = new ByteArrayInOutStream();
			
			this.restClient.get(uri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
						logger.debug("Got response from PE with headers {}", headers);
					return baios;
				}
			});
			// Keeps response in memory, BUT, in this case we know that response will not be to large
			// If we find that the response says Complete, return true
			String parseResult = parseResponseQueue(baios.getInputStream());
			if(parseResult.equals("Complete") || parseResult.equals("Cancelled")){
				result = true;
			}
			
			if (result) {
				String jobRequestURI = getJobRequestURI(ticket);
				
				logger.debug("PE job status is complete. Doing a retrieve HEAD request to ensure the job is possible to retrieve");
				
				ResponseHeaders head = this.restClient.head(jobRequestURI.toString());
				
				if (head.getStatus() != 200) {
					logger.debug("HEAD request validating that job status is complete, returned status: {}", head.getStatus());
					getErrorResponseMessageHTML(jobRequestURI.toString());
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
		
			// Create the uri
		String uri = getJobRequestURI(ticket);
		final OutputStream outputStream = outStream;
		logger.debug("Getting result of job with ticket: {}", ticket);
		
		try {
			//What if the job do no exist. This code will probably not handle that. Does it get a response body or does it throw a exception?
			this.restClient.get(uri.toString(), new RestResponse() {
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
		// THIS IS NOT WORKING YET. 
		// Create the uri
		String uri = getJobRequestURI(ticket);

		final OutputStream outputStream = outStream;

		try {
			this.restClient.get(uri.toString(), new RestResponse() {
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
	
	
	String parseResponseQueue(InputStream content) throws PublishException {
		
		return parseResponse("Transaction", "state", content);
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
		logger.trace("Start parse response");
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
	    	
	    	NodeList outputNode = doc.getElementsByTagName("FunctionOutput");
	    	if (outputNode.getLength() == 1) {
	    		Element e = (Element) outputNode.item(0);
	    		if (!e.hasChildNodes()) {
	    			logger.warn("Queue status output is broken in PE 8.1.3.x, assuming Queued/Processing");
	    			return "Queued";
	    		}
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
		logger.trace("End parse response");
		return null;
	}
	
	/**
	 * Use only when we know the response body is a PE failed HTML response. 
	 * The method is based on the assumption that all error body's first p element contains the error message (Should only be one p element).
	 * @param responseBody
	 * @return
	 */
	protected String parseErrorResponseBody(String responseBody) {
		
		if (responseBody == null) {
			return "";
		}
		
		String res = "";
		try {
			//Replacing new lines and charachter returns to make the regex more simple.
			responseBody = responseBody.replaceAll("\n", " ");
			responseBody = responseBody.replaceAll("\r", "");
			final Pattern pattern = Pattern.compile("<p>(.*)</p>"); // all content between p tags 
			final Matcher matcher = pattern.matcher(responseBody);
			matcher.find();
			res = matcher.group(1);
		} catch (Exception e) {
			logger.debug("Could not match p element in response body: {}", responseBody);
		}
		
		return res;
	}
	
	private void getErrorResponseMessageHTML(String requestUri) throws IOException, PublishException {
		
		try {
			this.restClient.get(requestUri.toString(), new RestResponse() {
				@Override
				public OutputStream getResponseStream(
						ResponseHeaders headers) {
					logger.debug("Got response from PE with headers {}", headers);
					return null; // The output stream will be empty, since this request always fails.
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
		logger.trace("Start");
		PublishTicket queueTicket = new PublishTicket(this.parseResponse("Transaction", "id", response));
		logger.trace("End");
		return queueTicket;
	}
	
	/**
	 * Parse queue ID from Location header.
	 * @param response
	 * @return
	 */
	PublishTicket getQueueTicket(HttpHeaders headers) throws PublishException {
		logger.trace("Start");
		// Have seen instances where PE fails to return a Location header.
		String location = headers.firstValue("Location").orElseThrow(() -> new IllegalStateException("Location header in PE response is missing, should provide the queue id."));
		// Location: /e3/jsp/jobstatus.jsp?id=120
		String[] s = location.split("id=");
		if (s.length != 2) {
			throw new IllegalStateException("Location header in PE response has unexpected format: " + location);
		}
		PublishTicket queueTicket = new PublishTicket(s[1]);
		logger.trace("End");
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
	
	private String getJobRequestURI(PublishTicket ticket) {
		
		StringBuffer uriBuffer = new StringBuffer();
		uriBuffer.append(this.peUri);

		// General always mandatory params
		uriBuffer.append("?&f=qt-retrieve");// The retrieve req
		uriBuffer.append("&id=" + ticket.toString()); // And ask for publication with ticket id
		
		return uriBuffer.toString();
	}
}
