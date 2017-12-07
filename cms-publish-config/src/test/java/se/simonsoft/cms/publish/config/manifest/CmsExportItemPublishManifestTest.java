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
package se.simonsoft.cms.publish.config.manifest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigManifest;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobManifest;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobManifestTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class CmsExportItemPublishManifestTest {

	private static ObjectReader reader;
	private static ObjectWriter writer;

	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishJobManifest.class);
		writer = mapper.writer().forType(PublishJobManifest.class);
	}

	@Test
	public void testManifest1() throws IOException {
	
		PublishConfigManifest config = PublishJobManifestTest.getTestManifestConfig1();
		PublishJobManifest job = PublishJobManifestTest.getTestManifestJob1(config);
	
		CmsExportItemPublishManifest exportItem = new CmsExportItemPublishManifest(writer, job);
		assertFalse(exportItem.isReady());
		exportItem.prepare();
		assertTrue(exportItem.isReady());
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		exportItem.getResultStream(stream);
		
		assertEquals("{\"job\":{},\"document\":{},\"meta\":{\"static\":\"value\"}}", stream.toString());
		
		PublishJobManifest parsed = reader.readValue(stream.toString());
		assertNotNull(parsed);
	}

}
