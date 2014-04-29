package se.simonsoft.cms.publish.abxpe;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import se.simonsoft.cms.publish.impl.PublishRequestDefault;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

import org.junit.Test;

public class PublishingEngineServiceTest {
	private String publishHost = "http://pds-suse-svn3.pdsvision.net";
	private String publishPath = "/e3/servlet/e3";
	private PublishTicket ticket;
	
	@Test
	public void publishRequestTest() {
		PublishingEngineService peService = new PublishingEngineService();
		PublishRequestDefault request = new PublishRequestDefault();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put("host", this.publishHost);
		config.put("path", this.publishPath); // Are we really going to build the url like this?
		
		request.setConfig(config);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("stylesheet", "$aptpath/application/se.simonsoft.flir/doctypes/FLIR/flir_technote_A4.style");
		params.put("app-config", "$aptpath/application/se.simonsoft.flir/app/standard.3sppdf");
		params.put("profile", "<LogicalExpression><LogicalGroup operator=\"OR\"><ProfileRef alias=\"Features\" value=\"FeatureX\"/><ProfileRef alias=\"Features\" value=\"FeatureD\"/></LogicalGroup></LogicalExpression>");
		request.setParams(params);
		
		//URL url = new URL("$aptpath/e3/e3/e3demo.xml"); // DEMO xml.
		CmsItemId id = new CmsItemIdArg("x-svn:///svn/flir^/thg-swe/td/td-xml/thg-swe/td-spec/03198-000.xml");
		
		PublishSourceCmsItemId source = new PublishSourceCmsItemId(id);
		
		request.setFile(source);
		request.setFormat("pdf");
		PublishTicket ticket = peService.requestPublish(request);
		
		assertNotNull("Ticket should not ne bnull", ticket.toString());
	}
	
	@Test
	public void publishIsCompleteTest(){
		PublishingEngineService peService = new PublishingEngineService();
		PublishRequestDefault request = new PublishRequestDefault();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put("host", this.publishHost);
		config.put("path", this.publishPath); // Are we really going to build the url like this?
		
		request.setConfig(config);
		PublishTicket ticket = new PublishTicket("10987");
		// Will fail if proceess is hanging
		assertTrue(peService.isCompleted(ticket, request));
	}
	
	@Test
	public void getResultStreamTest()
	{
		PublishingEngineService peService = new PublishingEngineService();
		PublishRequestDefault request = new PublishRequestDefault();
		
		Map<String, String> config = new HashMap<String, String>();
		config.put("host", this.publishHost);
		config.put("path", this.publishPath); // Are we really going to build the url like this?
		
		request.setConfig(config);
		
		PublishTicket ticket = new PublishTicket("10987");
		try {
			File test = File.createTempFile("se.simonsoft.publish.abxpe.test", "");
			FileOutputStream fopStream = new FileOutputStream(test);
			peService.getResultStream(ticket, request, fopStream);
			
			assertTrue("File shoudl exist in temporary store", test.exists());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
