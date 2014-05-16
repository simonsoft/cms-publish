/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishSourceUrl;
import se.simonsoft.cms.publish.PublishTicket;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

import org.junit.Test;

public class PublishingEngineServiceTest {
	private String publishHost = "http://pds-suse-svn3.pdsvision.net";
	private String publishPath = "/e3/servlet/e3";
	
	@Test
	public void publishRequestTest() throws MalformedURLException, InterruptedException, PublishException {
		PublishServicePe peService = new PublishServicePe();
		PublishRequestDefault request = new PublishRequestDefault();
		
		// Add config
		request.addConfig("host", this.publishHost);
		request.addConfig("path", this.publishPath);
		// Add Params
		request.addParam("profile", "<LogicalExpression><LogicalGroup operator=\"OR\"><ProfileRef alias=\"Features\" value=\"FeatureX\"/><ProfileRef alias=\"Features\" value=\"FeatureD\"/></LogicalGroup></LogicalExpression>");
		request.addParam("stylesheet", "$aptpath/application/se.simonsoft.flir/doctypes/FLIR/flir_technote_A4.style");
		request.addParam("app-config", "$aptpath/application/se.simonsoft.flir/app/standard.3sppdf");
		
		//URL path = new URL("C:/Program Files/e3/e3/e3demo.xml"); // DEMO xml.
		//PublishSourceUrl url = new PublishSourceUrl(path);
		CmsItemId id = new CmsItemIdArg("x-svn:///svn/flir^/thg-swe/td/td-xml/thg-swe/td-spec/03198-000.xml");
		
		PublishSourceCmsItemId source = new PublishSourceCmsItemId(id);
		
		request.setFile(source);
		request.setFormat("pdf");
		PublishTicket ticket = peService.requestPublish(request);
		assertNotNull("Ticket should not be null", ticket.toString());
	
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
}
