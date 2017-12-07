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
package se.simonsoft.cms.publish.databinds.publish.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.Checksum.Algorithm;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItem;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItemChecksum;

public class TestPublishJobReport3 {
	
	private ObjectMapper mapper = new ObjectMapper();
	private ObjectReader reportReader = mapper.reader(PublishJobReport3.class);
	private ObjectReader jobReader = mapper.reader(PublishJob.class);
	
	@Test
	public void testGettingItemsAsJsonString() throws JsonProcessingException, FileNotFoundException, IOException {
		PublishJobReport3 report3 = reportReader.readValue(getReport3JsonString());
		
		// Asserts for PublishJobItem
		PublishJobItem item = report3.getItems().get(0);
		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg", item.getLogicalhead());
		assertEquals("2013-11-06T17:36:05.941Z", item.getDate());
		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1", item.getRepourl());
		assertEquals("VV10084_25193", item.getNamebase());
		assertEquals("2013-11-06T17:36:05.941Z", item.getCommit().getDate());
		assertEquals(157, item.getCommit().getRevision());
		assertEquals("/svn/demo1/vvab/graphics/VV10084_25193.jpg", item.getUri());
		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1/vvab/graphics/VV10084_25193.jpg", item.getUrl());
		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg?p=157", item.getLogical());
		assertEquals(157, item.getRevision());
		assertEquals(true, item.isHead());
		assertEquals("/vvab/graphics/VV10084_25193.jpg", item.getPath());
		assertEquals(1278231, item.getFile().getSize());

		//Asserts for PublishJobItem's meta
		assertEquals("3958", item.getMeta().get("xmp_tiff.ImageWidth"));
		assertEquals("300.0", item.getMeta().get("xmp_tiff.XResolution"));
		assertEquals("2208", item.getMeta().get("xmp_tiff.ImageLength"));
		assertEquals("VV10084_25193.jpg", item.getName());

		//Asserts for PublishJobItemChecksum
		PublishJobItemChecksum checksum = item.getChecksum();
		assertEquals("36f526d2abd89abe071c122cfa4930021d1a49824a408174023028aa026dc3e0", checksum.get("SHA256"));
		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.get("SHA1"));
		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.get("MD5"));

		//Asserts for PublishJobItem's properties
		CmsItemPropertiesMap properties = (CmsItemPropertiesMap) item.getProperties();
		assertEquals("image/jpeg", properties.get("svn:mime-type"));
		assertEquals("photo", properties.get("cms:keywords"));
	}
	
//	Testing the Velocity template parsing function
	@Test
	public void testPublishConfigTemplateString() throws JsonProcessingException, FileNotFoundException, IOException {
		PublishJobReport3 jsonPjReport = reportReader.readValue(getReport3JsonString());
		PublishJob pj = jobReader.readValue(getJsonString());

		PublishConfigTemplateString templateString = new PublishConfigTemplateString(pj.getPathnameTemplate());
		templateString.withEntry("item", jsonPjReport.getItems().get(0));
		String evaluate = templateString.evaluate();
		
		assertEquals("DOC_VV10084_25193_In_Work.pdf" , evaluate);
	}
	
	//Test for implemented CheckSum.getHex() method in PublishJobCheckSum.java
	@Test
	public void testPublishJobItemCheckSum() throws JsonProcessingException, FileNotFoundException, IOException {
		PublishJobReport3 report3 = reportReader.readValue(getReport3JsonString());
		PublishJobItemChecksum checksum = report3.getItems().get(0).getChecksum();

		assertEquals("36f526d2abd89abe071c122cfa4930021d1a49824a408174023028aa026dc3e0", checksum.getHex(Algorithm.SHA256));
		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.getHex(Algorithm.MD5));
		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.getHex(Algorithm.SHA1));

		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.getMd5());
		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.getSha1());

		assertEquals(true, checksum.has(Algorithm.SHA256));
		assertEquals(true, checksum.equalsKnown(checksum));
	}
	
	//Tests if an exception is thrown when the .json file has an invalid dateformat
	@Test
	public void testImproperDate() throws JsonProcessingException, FileNotFoundException, IOException {
		try {
			PublishJobReport3 report3 = reportReader.readValue(getReport3JsonString());

			report3.getItems().get(0).setDate("09/22/1993T35:20:44.3821Z");
			RepoRevision repoR = report3.getItems().get(0).getRevisionChanged();
			fail("Expected IllegalArgumentException to be thrown");
		} catch(IllegalArgumentException e) {
			assertNotNull(e);
		}
	}
	
	//Tests if the implemented methods from CmsItem runs correctly
	public void testItemdIdMethods() throws JsonProcessingException, FileNotFoundException, IOException {
		PublishJobReport3 report3 = reportReader.readValue(getReport3JsonString());

		//Asserts getItemId
		PublishJobItem item = report3.getItems().get(0);
		CmsItemId itemId = item.getId();
		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1/vvab/graphics/VV10084_25193.jpg", itemId.getUrl());
		assertEquals(157, itemId.getPegRev().longValue());

		RepoRevision revision = item.getRevisionChanged();
		assertEquals("2013-11-06T17:36:05", revision.getDateIso());
		assertEquals("In_Work", item.getStatus());
		assertEquals(1278231, item.getFilesize());
	}
	
	private String getReport3JsonString() throws IOException {
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-report3.json";
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
	private String getJsonString() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-job.json";
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
