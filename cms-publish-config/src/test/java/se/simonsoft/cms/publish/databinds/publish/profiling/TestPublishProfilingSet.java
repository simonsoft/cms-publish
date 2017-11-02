package se.simonsoft.cms.publish.databinds.publish.profiling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
		PublishProfilingSet ppSetSecondJson = reader.readValue(getSecondJsonString());
		
		//Testing first string
		assertEquals("osx", ppSet.get("osx").getName());
		assertEquals("%20", ppSet.get("osx").getLogicalexpr());
		assertEquals(" ", ppSet.get("osx").getLogicalExprDecoded());
		
		assertEquals("linux", ppSet.get("linux").getName());
		assertEquals("%3A", ppSet.get("linux").getLogicalexpr());
		assertEquals(":", ppSet.get("linux").getLogicalExprDecoded());
		
		//Testing second String
		assertEquals("internal", ppSetSecondJson.get("internal").getName());
		assertEquals("%3CProfileRef%20alias%3D%22Profiling%22%20value%3D%22internal%22%2F%3E", ppSetSecondJson.get("internal").getLogicalexpr());
		assertEquals("<ProfileRef alias=\"Profiling\" value=\"internal\"/>", ppSetSecondJson.get("internal").getLogicalExprDecoded());
		
		
	}
	@Test
	public void testImplementedMethods() throws JsonProcessingException, IOException {
		PublishProfilingSet ppSetSecondJson = reader.readValue(getSecondJsonString());
		PublishProfilingSet ppSet = reader.readValue(getJsonString());
		PublishProfilingSet emptySet = new PublishProfilingSet();
		
		assertEquals(2, ppSet.size());
		assertEquals(true, emptySet.isEmpty());
		assertEquals(true, ppSetSecondJson.contains("internal"));
		
		//Testing PublishProfilingSet.add(PublishProfilingRecipe e)
		PublishProfilingRecipe recipe = new PublishProfilingRecipe();
		recipe.setName("testData");
		ppSet.add(recipe);
		assertEquals(recipe, ppSet.get("testData"));
		
		//Testing PublishProfilingSet.remove(Object o)
		ppSet.remove("testData");
		assertFalse(ppSet.contains("testData"));
		
		//Testing PublishProfilingSet.addAll(Collection<?> c)
		PublishProfilingRecipe recipe2 = new PublishProfilingRecipe();
		recipe2.setName("testData2");
		Collection<PublishProfilingRecipe> collection = new ArrayList<PublishProfilingRecipe>();
		collection.add(recipe);
		collection.add(recipe2);
		ppSet.addAll(collection);
		assertTrue(ppSet.contains("testData"));
		assertTrue(ppSet.contains("testData2"));
		
		ppSet.clear();
		assertTrue(ppSet.isEmpty());
	}
	private String getJsonString() {
		return "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}, {\"name\":\"linux\",\"logicalexpr\":\"%3A\"}]";
	}
	private String getSecondJsonString() {
		return "[{\"logicalexpr\":\"%3CProfileRef%20alias%3D%22Profiling%22%20value%3D%22internal%22%2F%3E\",\"name\":\"internal\",\"profiling\":\"internal\"}]";
	}
	
}
