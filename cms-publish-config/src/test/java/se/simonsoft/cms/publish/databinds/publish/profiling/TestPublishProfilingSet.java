package se.simonsoft.cms.publish.databinds.publish.profiling;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class TestPublishProfilingSet {
	private static ObjectReader reader;
	
	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishProfilingSet.class);
	}
	@Test
	public void testDeserialize() throws JsonProcessingException, IOException {
		PublishProfilingSet ppSet = reader.readValue(getJsonString());
		
		assertEquals("osx", ppSet.get(0).getName());
		assertEquals("%20", ppSet.get(0).getLogicalexpr());
		assertEquals("linux", ppSet.get(1).getName());
		assertEquals("%3A", ppSet.get(1).getLogicalexpr());
		assertEquals(" ", ppSet.get(0).getLogicalExprDecoded());
		assertEquals(":", ppSet.get(1).getLogicalExprDecoded());
	}
	private String getJsonString() {
		return "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}, {\"name\":\"linux\",\"logicalexpr\":\"%3A\"}]";
		
	}
}
