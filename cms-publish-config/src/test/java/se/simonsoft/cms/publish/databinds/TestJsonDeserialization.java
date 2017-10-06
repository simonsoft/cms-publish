package se.simonsoft.cms.publish.databinds;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import junit.framework.TestCase;

public class TestJsonDeserialization extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	@Test
	public void testJsonDeserialization()throws JsonParseException, JsonMappingException, IOException {
		JsonDeserialization jDeserialize = new JsonDeserialization();
		
		List <String> statusInclude = new ArrayList();
		statusInclude.add("Review");
		statusInclude.add("Released");
		List <String> profilingInclude = new ArrayList();
		profilingInclude.add("*");
		
		PublishConfigParams pcParams = new PublishConfigParams("...", "...,", "great", null);
		PublishConfigParams pcParams2 = new PublishConfigParams(null, null, null, "parameter for future destination types");
		PublishConfigStorage pcStorage = new PublishConfigStorage("s3", pcParams2);
		PublishConfigPostProcess pcPostProcess = new PublishConfigPostProcess("future stuff", pcParams2);
		PublishConfigDelivery pcDelivery = new PublishConfigDelivery("webhook");
		PublishConfigPublish pcPublish = new PublishConfigPublish("abxpe", "pdf", pcParams, pcStorage, pcPostProcess, pcDelivery);
		PublishConfig pc = new PublishConfig(true, true, statusInclude, profilingInclude, "velocity-stuff.pdf", pcPublish);
		PublishConfig jsonPc = new PublishConfig();
		
		jsonPc = jDeserialize.JsonDeserialize("/home/anton/git/cms-publish/cms-publish-config/src/main/java/se/simonsoft/cms/publish/databinds/publish-config.json");
		
		assertEquals(pc.toString(), jsonPc.toString());
	}
	@Test
	public void testCompare() {
		assertTrue("1"=="1");
		assertTrue(new String("1") == new String("1"));
	}
}
