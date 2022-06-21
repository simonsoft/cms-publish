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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishSourceUrl;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

import org.junit.Ignore;
import org.junit.Test;

public class PublishingEngineServiceTest {
	private String publishHost = "http://pds-suse-svn3.pdsvision.net";
	private String publishPath = "/e3/servlet/e3";
	
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
		
		URL path = new URL("file:///C:/Program%20Files/e3/e3/e3demo.xml"); // DEMO xml.
		PublishSourceUrl url = new PublishSourceUrl(path);
		
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
