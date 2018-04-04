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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import se.simonsoft.cms.publish.config.databinds.job.PublishJobManifest;

public class CmsExportItemPublishManifestVelocityTest {

	
	@Test
	public void testPublishManifestVelocity() throws Exception {
		
		PublishJobManifest manifest = getManifest();
		manifest.setTemplate(getTestResourceAsString("se/simonsoft/cms/publish/databinds/resources/publish-manifest-escape.vm"));
		
		//Adding invalid chars
		manifest.getDocument().put("invalid", "i>nva<l&d");
		
		CmsExportItemPublishManifestVelocity exportItem = new CmsExportItemPublishManifestVelocity(manifest);
		exportItem.prepare();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		exportItem.getResultStream(baos);
		
		String content = baos.toString();
		final Pattern pattern = Pattern.compile("<invalid>(.+?)</invalid>");
		final Matcher matcher = pattern.matcher(content);
		matcher.find();
		String invalidContent = matcher.group(1);
		
		assertFalse(invalidContent.contains(">"));
		assertFalse(invalidContent.contains("<"));
		assertTrue("& should be escaped to amp." , invalidContent.contains("&amp;"));
		assertFalse("Null maps should not exists", content.contains("<master>"));
	}
	
	
	
	private PublishJobManifest getManifest() {
		PublishJobManifest manifest = new PublishJobManifest();
		
		LinkedHashMap<String, String> job = new LinkedHashMap<>();
		job.put("format", "pdf");
		job.put("itemid", "x-svn:///svn/demo1^/someItem.xml");
		manifest.setJob(job);
		
		LinkedHashMap<String, String> doc = new LinkedHashMap<>();
		doc.put("releaselabel", "B");
		doc.put("docno", "DOC_900108");
		doc.put("lang", "abc");
		doc.put("status", "Released"); // Illegal xl char ‹
		doc.put("baselinerevision", "0000000553");
		manifest.setDocument(doc);
		
		LinkedHashMap<String, String> custom = new LinkedHashMap<>();
		custom.put("type", "OPERATOR");
		custom.put("static", "value");
		manifest.setCustom(custom);
		
		LinkedHashMap<String, String> meta = new LinkedHashMap<>();
		meta.put("type", "operator");
		manifest.setMeta(meta);
		
		return manifest;
	}
	
	
	private String getTestResourceAsString(String resourcePath) throws FileNotFoundException, IOException {
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
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
