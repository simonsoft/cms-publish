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
package se.simonsoft.cms.publish.config.databinds.profiling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;

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
	public void testImplementedSetMethods() throws JsonProcessingException, IOException {
		PublishProfilingSet ppSetSecondJson = reader.readValue(getSecondJsonString());
		PublishProfilingSet ppSet = reader.readValue(getJsonString());
		PublishProfilingSet emptySet = new PublishProfilingSet();

		assertEquals(2, ppSet.size());
		assertEquals(true, emptySet.isEmpty());
		assertEquals(true, ppSetSecondJson.contains("internal"));

		//Testing implementation of PublishProfilingSet.iterator()
		Iterator<PublishProfilingRecipe> iterator = ppSetSecondJson.iterator();
		assertNotNull(iterator);
		PublishProfilingRecipe e = iterator.next();
		assertEquals("internal", e.getName());
		
		Object[] ppSetArray = ppSet.toArray();
		assertNotNull(ppSetArray);

		//Testing PublishProfilingSet.add(PublishProfilingRecipe e) (only Jackson should be using this method)
		PublishProfilingRecipe recipe = new PublishProfilingRecipe();
		recipe.setName("testData");
		ppSet.add(recipe);
		assertEquals(recipe, ppSet.get("testData"));


	}
	@Test
	public void testUnsupportedMethods() throws JsonProcessingException, IOException {
		PublishProfilingSet ppSet = reader.readValue(getSecondJsonString());

		try {
			Object[] a = null;
			ppSet.toArray(a);
			fail("Should throw UnsupportedOperationException");
		}catch (UnsupportedOperationException e) {
			assertEquals("PublishProfilingSet.toArray(T[] a) is not supported", e.getMessage());
		}

		try {
			ppSet.remove("internal");
			fail("Should throw UnsupportedOperationException");
		}catch (UnsupportedOperationException e){
			assertEquals("PublishingProfilingSet.remove() is not supported", e.getMessage());
		}

		try {
			Collection<?> c = null;
			ppSet.containsAll(c);
			fail("Should throw UnsupportedOperationException");
		}catch (UnsupportedOperationException e) {
			assertEquals("PublishProfilingSet.containsAll(Collection<?> c) is not supported", e.getMessage());
		}

		try {
			Collection<? extends PublishProfilingRecipe> c = null;
			ppSet.addAll(c);
			fail("Should throw UnsupportedOperationException");
		}catch (UnsupportedOperationException e) {
			assertEquals("PublishingProfilingSet.addAll(Collectio<? extends PublishProfilingRecipe> c) is not supported", e.getMessage());
		}

		try {
			Collection<?> c = null;
			ppSet.retainAll(c);
			fail("Should throw UnsupportedOperationException");
		} catch(UnsupportedOperationException e) {
			assertEquals("PublishProfilingSet.retainAll(Collection<?> c) is not supported", e.getMessage());
		}

		try {
			Collection<?> c = null;
			ppSet.removeAll(c);
			fail("Should throw UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			assertEquals("PublishProfilingSet.removeAll(Collection<?> c) is not supported", e.getMessage());
		}

		try {
			ppSet.clear();
			fail("Should throw UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			assertEquals("PublishingProfilingSet.clear() is not supported", e.getMessage());
		}
	}
	@Test
	public void testMethodParameterInput() throws JsonProcessingException, IOException {
		PublishProfilingSet ppSet = reader.readValue(getSecondJsonString());

		try {
			int i = 44;
			ppSet.contains(i);
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals("The input of PublishProfilingSet.contains(Object o) must be a string", e.getMessage());
		}
	}
	@Test
	public void testDuplicateNames() throws JsonProcessingException, IOException {
		PublishProfilingSet ppSet = reader.readValue(getSecondJsonString());

		try {
			PublishProfilingRecipe ppRecipe = new PublishProfilingRecipe();
			ppRecipe.setName("Nicke");
			ppSet.add(ppRecipe);
			PublishProfilingRecipe ppRecipe2 = new PublishProfilingRecipe();
			ppRecipe2.setName("Nicke");
			ppSet.add(ppRecipe2);
			
			fail("Should throw IllegalArgumentException, duplicate names are not allowed in PublishProfilingSet");
			
		} catch(IllegalArgumentException e) {
			assertEquals("Duplicate names in PublishProfilingSet is not allowed.", e.getMessage());
		}
	}
	private String getJsonString() {
		return "[{\"name\":\"osx\",\"logicalexpr\":\"%20\"}, {\"name\":\"linux\",\"logicalexpr\":\"%3A\"}]";
	}
	private String getSecondJsonString() {
		return "[{\"logicalexpr\":\"%3CProfileRef%20alias%3D%22Profiling%22%20value%3D%22internal%22%2F%3E\",\"name\":\"internal\",\"profiling\":\"internal\"}]";
	}

}
