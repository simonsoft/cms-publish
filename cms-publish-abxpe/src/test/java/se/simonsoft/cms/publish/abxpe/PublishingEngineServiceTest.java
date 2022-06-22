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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.http.HttpHeaders;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.junit.Ignore;
import org.junit.Test;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.stream.ByteArrayInOutStream;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishSource;
import se.simonsoft.cms.publish.PublishSourceArchive;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class PublishingEngineServiceTest {
	private String publishHost = "http://pds-suse-svn3.pdsvision.net";
	private String publishPath = "/e3/servlet/e3";
	
	private static BiPredicate<String, String> alwaysBiPredicate = new BiPredicate<String, String>() {
		@Override
		public boolean test(String t, String u) {
			return true;
		}
	};
	
	// This test is actually communicating with a PE, used during dev.
	@Test @Ignore
	public void publishRequestTest() throws MalformedURLException, InterruptedException, PublishException {
		PublishServicePe peService = new PublishServicePe(publishHost);
		PublishRequestDefault request = new PublishRequestDefault();
		
		// Add config
		//request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		// Add Params
		request.addParam("profile", "<LogicalExpression><LogicalGroup operator=\"OR\"><ProfileRef alias=\"Features\" value=\"FeatureX\"/><ProfileRef alias=\"Features\" value=\"FeatureD\"/></LogicalGroup></LogicalExpression>");
		request.addParam("stylesheet", "$aptpath/application/se.simonsoft.techdoc/doctypes/techdoc/techdoc.style");
		request.addParam("app-config", "$aptpath/application/se.simonsoft.techdoc/app/standard.3sppdf");
		
		//URL path = new URL("C:/Program Files/e3/e3/e3demo.xml"); // DEMO xml.
		//PublishSourceUrl url = new PublishSourceUrl(path);
		CmsItemId id = new CmsItemIdArg("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml");
		
		PublishSourceCmsItemId source = new PublishSourceCmsItemId(id);
		
		request.setFile(source);
		request.setFormat(new PublishFormatPDF());
		PublishTicket ticket = peService.requestPublish(request);
		assertNotNull("Ticket should not be null", ticket);
		assertFalse("Ticket should not be empty", ticket.toString().isEmpty());
	
		boolean isComplete = false;
		while(!isComplete){
			Thread.sleep(1000); // Sleep for a second before asking PE again
			isComplete = peService.isCompleted(ticket, request);
		}
		// Perform the test, by now isComplete should be true
		assertTrue(isComplete);
		
		// Test completed output
		 // TODO: Check for Mime 
		try {
			File temp = File.createTempFile("se.simonsoft.publish.abxpe.test", "");
			FileOutputStream fopStream = new FileOutputStream(temp);
			peService.getResultStream(ticket, request, fopStream);
			
			assertTrue("File should exist in temporary store", temp.exists());
		
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	@Test
	public void publishRequestUnknownHostTest() throws MalformedURLException, InterruptedException, PublishException {
		PublishServicePe peService = new PublishServicePe("http://bogus.pdsvision.net");
		PublishRequestDefault request = new PublishRequestDefault();
		
		// Add config
		//request.addConfig("host", "http://bogus.pdsvision.net");
		request.addConfig("path", this.publishPath);
		// Add Params
		request.addParam("stylesheet", "$aptpath/application/se.simonsoft.techdoc/doctypes/techdoc/techdoc.style");
		request.addParam("app-config", "$aptpath/application/se.simonsoft.techdoc/app/standard.3sppdf");
		
		ByteArrayInOutStream baios = new ByteArrayInOutStream();
		PublishSource url = new PublishSourceArchive(() -> baios.getInputStream(), 123L, "_document.xml");
		
		request.setFile(url);
		request.setFormat(new PublishFormatPDF());
		try {
			PublishTicket ticket = peService.requestPublish(request);
			assertNotNull("Ticket should not be null", ticket);
			assertFalse("Ticket should not be empty", ticket.toString().isEmpty());
		} catch (PublishException e) {
			//e.printStackTrace();
			// Java 11 wraps its UnresolvedAddressException twice and sets message to null.
			// Restclient works around that and throws the traditional UnknownHostException with hostname.
			assertEquals("Publishing failed (java.net.UnknownHostException): bogus.pdsvision.net", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	
	@Test
	public void testParseTicket() throws PublishException {
		PublishServicePe pe = new PublishServicePe(publishHost);
		Map<String, List<String>> map = new HashMap<String, List<String>>(1);
		map.put("Location", Arrays.asList("/e3/jsp/jobstatus.jsp?id=120"));
		HttpHeaders headers = HttpHeaders.of(map, alwaysBiPredicate);
		PublishTicket ticket = pe.getQueueTicket(headers);
		assertEquals("120", ticket.toString());
	}
	
	
	@Test
	public void testParseQueueWorking() throws PublishException {
		// PE used to report state for non-completed tickets:
		// - state:Queued 
		// - state:Processing 
		
		PublishServicePe pe = new PublishServicePe(publishHost);
		ByteArrayInOutStream baios = new ByteArrayInOutStream(getResponseQueueWorking8131());
		String parsed = pe.parseResponseQueue(baios.getInputStream());
		assertEquals("assuming Queued/Processing", "Queued", parsed);
	}
	
	@Test
	public void testParseQueueCompleted() throws PublishException {
		PublishServicePe pe = new PublishServicePe(publishHost);
		ByteArrayInOutStream baios = new ByteArrayInOutStream(getResponseQueueCompleted());
		String parsed = pe.parseResponseQueue(baios.getInputStream());
		assertEquals("Complete", parsed);
	}
	
	@Test
	public void testParseHtmlErrorResponse() throws Exception {
		
		PublishServicePe pe = new PublishServicePe(publishHost);
		String parsed = pe.parseErrorResponseBody(getPEStandardFomratedResponse());
		
		assertFalse("Do not start with p tag", parsed.startsWith("<p>"));
		assertFalse("Do not start with p tag", parsed.endsWith("</p>"));
		assertTrue("text in p tag", parsed.contains("stylesheet &#39;C:\\Program Files\\PTC\\Arbortext PE/simonsoft.vabb/doctypes/VVAB/vvab.style&#39;"));
		
		
		pe.parseErrorResponseBody(getResponseWithNewLinesInP());
		
		assertFalse("Do not start with p tag", parsed.startsWith("<p>"));
		assertFalse("Do not start with p tag", parsed.endsWith("</p>"));
		assertTrue("text in p tag", parsed.contains("stylesheet &#39;C:\\Program Files\\PTC\\Arbortext PE/simonsoft.vabb/doctypes/VVAB/vvab.style&#39;"));
	}
	
	
	@Test
	public void testParseNull() {
		PublishServicePe pe = new PublishServicePe(publishHost);
		String parsed = pe.parseErrorResponseBody(null);
		assertEquals(parsed, "");
	}
	
	@Test
	public void testParseResponseNoP() {
		PublishServicePe pe = new PublishServicePe(publishHost);
		String parsed = pe.parseErrorResponseBody("<html> <body> <div>content</div> </body> </html>");
		
		assertEquals("No match should return empty string." ,"", parsed);
	}
	
	// This format is broken in PE 8.1.3.1.
	private String getResponseQueueWorking8131() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<PEFunctionResult operation=\"Get Transaction Status\" success=\"Yes\">\n"
				+ "  <FunctionParameters>\n"
				+ "    <Parameter name=\"f\" value=\"qt-status\"/>\n"
				+ "    <Parameter name=\"id\" value=\"130\"/>\n"
				+ "    <Parameter name=\"response-format\" value=\"xml\"/>\n"
				+ "  </FunctionParameters>\n"
				+ "  <FunctionOutput/>\n"
				+ "</PEFunctionResult>";
	}
	
	
	private String getResponseQueueCompleted() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<PEFunctionResult operation=\"Get Transaction Status\" success=\"Yes\">\n"
				+ "  <FunctionParameters>\n"
				+ "    <Parameter name=\"f\" value=\"qt-status\"/>\n"
				+ "    <Parameter name=\"id\" value=\"130\"/>\n"
				+ "    <Parameter name=\"response-format\" value=\"xml\"/>\n"
				+ "  </FunctionParameters>\n"
				+ "  <FunctionOutput>\n"
				+ "    <Transaction id=\"130\" held=\"No\" priority=\"3\" name=\"tran-130\" queueId=\"default-queue\" submitTime=\"1655889755764\" startTime=\"1655889762838\" endTime=\"1655889780380\" duration=\"17542\" state=\"Complete\" hostAddress=\"172.31.26.49\">\n"
				+ "      <Request submitTime=\"1655889755764\">\n"
				+ "        <Header name=\"content-length\" value=\"1920711\"/>\n"
				+ "        <Header name=\"expect\" value=\"100-continue\"/>\n"
				+ "        <Header name=\"host\" value=\"localhost:8080\"/>\n"
				+ "        <Header name=\"content-type\" value=\"application/zip\"/>\n"
				+ "        <Header name=\"user-agent\" value=\"curl/7.79.1\"/>\n"
				+ "        <Header name=\"accept\" value=\"*/*\"/>\n"
				+ "        <Data name=\"locale\" value=\"en_US\"/>\n"
				+ "        <Data name=\"isSecure\" value=\"false\"/>\n"
				+ "        <Data name=\"serverPort\" value=\"8080\"/>\n"
				+ "        <Data name=\"authType\" value=\"\"/>\n"
				+ "        <Data name=\"characterEncoding\" value=\"\"/>\n"
				+ "        <Data name=\"contextPath\" value=\"/e3\"/>\n"
				+ "        <Data name=\"peDescription\" value=\"Convert to PDF\"/>\n"
				+ "        <Data name=\"method\" value=\"POST\"/>\n"
				+ "        <Data name=\"pathTranslated\" value=\"C:\\Program Files\\PTC\\Arbortext PE\\e3\\e3\\e3\"/>\n"
				+ "        <Data name=\"protocol\" value=\"HTTP/1.1\"/>\n"
				+ "        <Data name=\"queryString\" value=\"f=convert&amp;queue=yes&amp;type=pdf&amp;file-type=zip&amp;input-entry=_document.xml&amp;zip-root=900276_M_en-GB&amp;zip-output=yes\"/>\n"
				+ "        <Data name=\"remoteAddr\" value=\"127.0.0.1\"/>\n"
				+ "        <Data name=\"remoteHost\" value=\"127.0.0.1\"/>\n"
				+ "        <Data name=\"remoteUser\" value=\"\"/>\n"
				+ "        <Data name=\"requestURI\" value=\"/e3/servlet/e3\"/>\n"
				+ "        <Data name=\"scheme\" value=\"http\"/>\n"
				+ "        <Data name=\"serverName\" value=\"localhost\"/>\n"
				+ "        <Data name=\"servletPath\" value=\"/servlet\"/>\n"
				+ "        <Parameter name=\"file-type\" value=\"zip\"/>\n"
				+ "        <Parameter name=\"zip-root\" value=\"900276_M_en-GB\"/>\n"
				+ "        <Parameter name=\"zip-output\" value=\"yes\"/>\n"
				+ "        <Parameter name=\"f\" value=\"convert\"/>\n"
				+ "        <Parameter name=\"input-entry\" value=\"_document.xml\"/>\n"
				+ "        <Parameter name=\"type\" value=\"pdf\"/>\n"
				+ "        <Parameter name=\"queue\" value=\"yes\"/>\n"
				+ "      </Request>\n"
				+ "      <Response status=\"200\" hasBody=\"Yes\" bodySize=\"2069928\">\n"
				+ "        <Header name=\"content-disposition\" value=\"attachment; filename=pe.zip\"/>\n"
				+ "        <Header name=\"content-type\" value=\"application/octet-stream\"/>\n"
				+ "      </Response>\n"
				+ "    </Transaction>\n"
				+ "  </FunctionOutput>\n"
				+ "</PEFunctionResult>";
	}
	
	
	private String getResponseWithNewLinesInP() {
		return "<html>\n" + 
				"<head>\n" + 
				"<link rel=\"stylesheet\" href=\"/e3/pestyle.css\" type=\"text/css\"><title>\n" + 
				"Servigistics Arbortext Publishing Engine Convert Internal Error\n" + 
				"</title>\n" + 
				"</head>\n" + 
				"<body topmargin=\"0\" leftmargin=\"0\">\n" + 
				"<table border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">\n" + 
				"<tr><td><img src=\"/e3/e3.jpg\" alt=\"Servigistics Arbortext Publishing Engine\"></td>\n" + 
				"<td width=\"100%\" style=\"background-color: #FFFFFF\"></td>\n" + 
				"</tr>\n" + 
				"</table>\n" + 
				"<div style=\"margin-left: 6pt\">\n" + 
				"<h2>\n" + 
				"Servigistics Arbortext Publishing Engine Convert Internal Error\n" + 
				"</h2>\n" + 
				"<p>\n" + 
				"\n" + 
				"e3::convert:  stylesheet &#39;C:\\Program Files\\PTC\\Arbortext PE/simonsoft.vabb/doctypes/VVAB/vvab.style&#39; does not exist.<p><B>Other information</B><BR>&nbsp;&nbsp;&nbsp;<B>Version:&nbsp;</B>7.0 M080 <BR>&nbsp;&nbsp;&nbsp;<B>Platform:&nbsp;</B>Windows Server 2012 R2 Standard Evaluation <BR>&nbsp;&nbsp;&nbsp;<B>Stylesheet:&nbsp;</B>\n" + 
				"$aptpath/simonsoft.vabb/doctypes/VVAB/vvab.style\n" + 
				" <BR></P>\n" + 
				"</p>\n" + 
				"</div>\n" + 
				"</body>\n" + 
				"</html>";
	}
	
	
	private String getPEStandardFomratedResponse() {
		return "<html>\n" + 
				"<head>\n" + 
				"<link rel=\"stylesheet\" href=\"/e3/pestyle.css\" type=\"text/css\"><title>\n" + 
				"Servigistics Arbortext Publishing Engine Convert Internal Error\n" + 
				"</title>\n" + 
				"</head>\n" + 
				"<body topmargin=\"0\" leftmargin=\"0\">\n" + 
				"<table border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">\n" + 
				"<tr><td><img src=\"/e3/e3.jpg\" alt=\"Servigistics Arbortext Publishing Engine\"></td>\n" + 
				"<td width=\"100%\" style=\"background-color: #FFFFFF\"></td>\n" + 
				"</tr>\n" + 
				"</table>\n" + 
				"<div style=\"margin-left: 6pt\">\n" + 
				"<h2>\n" + 
				"Servigistics Arbortext Publishing Engine Convert Internal Error\n" + 
				"</h2>\n" + 
				"<p>\n" + 
				"e3::convert:  stylesheet &#39;C:\\Program Files\\PTC\\Arbortext PE/simonsoft.vabb/doctypes/VVAB/vvab.style&#39; does not exist.<p><B>Other information</B><BR>&nbsp;&nbsp;&nbsp;<B>Version:&nbsp;</B>7.0 M080 <BR>&nbsp;&nbsp;&nbsp;<B>Platform:&nbsp;</B>Windows Server 2012 R2 Standard Evaluation <BR>&nbsp;&nbsp;&nbsp;<B>Stylesheet:&nbsp;</B>$aptpath/simonsoft.vabb/doctypes/VVAB/vvab.style <BR></P>\n" + 
				"</p>\n" + 
				"</div>\n" + 
				"</body>\n" + 
				"</html>";
	}
}
