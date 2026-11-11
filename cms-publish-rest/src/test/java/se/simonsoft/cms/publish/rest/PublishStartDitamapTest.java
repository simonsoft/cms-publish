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
package se.simonsoft.cms.publish.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PublishStartDitamapTest {

	static final String DITAMAP = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">\n"
			+ "<map xmlns:cms=\"http://www.simonsoft.se/namespace/cms\" title=\"Preview\">\n"
			+ "  <mapref href=\"x-svn:///svn/demo1^/vvab/xml/keys.ditamap\" processing-role=\"resource-only\"/>\n"
			+ "  <topicref href=\"x-svn:///svn/demo1^/vvab/xml/topics/a.dita#topic_a\" cms:workingcopy=\"yes\">\n"
			+ "    <topicref href=\"x-svn:///svn/demo1^/vvab/xml/topics/b.xml#topic_b\"/>\n"
			+ "  </topicref>\n"
			+ "  <topicref href=\"x-svn:///svn/demo1^/vvab/xml/topics/c\" format=\"dita\"/>\n"
			+ "  <keydef keys=\"k1\" href=\"x-svn:///svn/demo1^/vvab/xml/topics/k.dita\" processing-role=\"resource-only\"/>\n"
			+ "  <topicref href=\"https://www.simonsoft.se/\" scope=\"external\" format=\"html\"/>\n"
			+ "  <topicref href=\"x-svn:///svn/demo1^/vvab/graphics/g.png\" format=\"png\"/>\n"
			+ "  <topichead navtitle=\"No href\"/>\n"
			+ "</map>\n";

	@Test
	public void testSha1Hex() {
		// echo -n "abc" | sha1sum
		assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", PublishStartDitamap.getSha1Hex("abc"));
		assertEquals(40, PublishStartDitamap.getSha1Hex(DITAMAP).length());
		assertEquals(PublishStartDitamap.getSha1Hex(DITAMAP), PublishStartDitamap.getSha1Hex(new String(DITAMAP)));
		assertNotEquals(PublishStartDitamap.getSha1Hex(DITAMAP), PublishStartDitamap.getSha1Hex(DITAMAP + " "));
	}

	@Test
	public void testSha1HexUtf8() {
		// echo -n "å" | sha1sum (UTF-8)
		assertEquals("cfb50dd6cf79fd470f3a558a720777d835bb45de", PublishStartDitamap.getSha1Hex("\u00e5"));
	}

	@Test
	public void testCountTopics() {
		assertEquals(3, PublishStartDitamap.countTopics(DITAMAP));
	}

	@Test
	public void testCountTopicsEmptyMap() {
		assertEquals(0, PublishStartDitamap.countTopics("<map/>"));
	}

	@Test
	public void testCountTopicsInvalid() {
		try {
			PublishStartDitamap.countTopics("<map><topicref></map>");
			fail("Expected IllegalArgumentException for non-wellformed ditamap");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testCountTopicsNoExternalEntities() {
		String xxe = "<?xml version=\"1.0\"?>\n<!DOCTYPE map [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n<map><topicref href=\"x-svn:///svn/demo1^/a.dita\">&xxe;</topicref></map>";
		try {
			// Either rejected (DOCTYPE with entity) or the entity not expanded, must not read the file.
			assertEquals(1, PublishStartDitamap.countTopics(xxe));
		} catch (IllegalArgumentException e) {
			// acceptable
		}
	}
}
