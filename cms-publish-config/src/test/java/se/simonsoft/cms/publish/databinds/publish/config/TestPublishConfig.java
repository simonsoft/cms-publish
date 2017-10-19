package se.simonsoft.cms.publish.databinds.publish.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import junit.framework.TestCase;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;

public class TestPublishConfig extends TestCase {
	private ObjectReader reader;

	@BeforeClass
	protected void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishConfig.class);
	}
	@Test
	public void testJsonDeserialization() throws JsonParseException, JsonMappingException, IOException {
		PublishConfig jsonPc = new PublishConfig();
		jsonPc = reader.readValue(getJsonString());

		assertEquals("velocity-stuff.pdf", jsonPc.getPathNameTemplate());
		assertEquals("*", jsonPc.getProfilingInclude().get(0));
		assertEquals("Review", jsonPc.getStatusInclude().get(0));
		assertEquals("Released", jsonPc.getStatusInclude().get(1));
		assertEquals(true, jsonPc.isActive());
		assertEquals(true, jsonPc.isVisible());
		assertEquals("abxpe", jsonPc.getPublish().getType());
		assertEquals("pdf", jsonPc.getPublish().getFormat());
		assertEquals("file.css", jsonPc.getPublish().getParams().get("stylesheet"));
		assertEquals("file.pdf", jsonPc.getPublish().getParams().get("pdfconfig"));
		assertEquals("great", jsonPc.getPublish().getParams().get("whatever"));
		assertEquals("s3", jsonPc.getPublish().getStorage().getType());
		assertEquals("parameter for future destination types", jsonPc.getPublish().getStorage().getParams().get("specific"));
		assertEquals("future stuff", jsonPc.getPublish().getPostProcess().getType());
		assertEquals("parameter for future destination types", jsonPc.getPublish().getPostProcess().getParams().get("specific"));
		assertEquals("webhook", jsonPc.getPublish().getDelivery().getType());
	}

	@Test
	public void testUnkownProperty() {
		//Getting data from Json
		try {
			PublishConfig jsonPc = new PublishConfig();
			ObjectReader r = new ObjectMapper().reader(PublishConfig.class);
			jsonPc = r.readValue(getJsonString2());

			fail("Expectected UnrecognizedPropertyException to be thrown");
		}catch(UnrecognizedPropertyException e){
			assertNotNull(e);
			e.printStackTrace();
		}catch (JsonParseException e) {
			assertNotNull(e);
			e.printStackTrace();
		}catch(JsonMappingException e) {
			assertNotNull(e);
			e.printStackTrace();
		}catch(IOException e) {
			assertNotNull(e);
			e.printStackTrace();
		}
	}
	private String getJsonString() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-config.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			out.append(line);
		}
		reader.close();
		return out.toString();
	}
	private String getJsonString2() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-config2.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			out.append(line);
		}
		reader.close();
		return out.toString();
	}
}
