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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.Checksum.Algorithm;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigTemplateString;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItem;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItemChecksum;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItemCommit;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItemFile;

public class TestPublishJob {
	private static ObjectReader reader;
	private static ObjectWriter writer;

	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishJob.class);
		writer = mapper.writer();
	}

	@Test
	public void testJsonToPublishJob() throws JsonProcessingException, FileNotFoundException, IOException {
		PublishJob jsonPj = new PublishJob();
		jsonPj = reader.readValue(getJsonString());

		//Asserts for PublishJob
		assertEquals("name-from-cmsconfig-publish", jsonPj.getConfigname());
		assertEquals("publish-job", jsonPj.getType());
		assertEquals("publish-noop", jsonPj.getAction());
		assertEquals(true, jsonPj.isActive());
		assertEquals(true, jsonPj.isVisible());
		assertEquals("Review", jsonPj.getStatusInclude().get(0));
		assertEquals("Released", jsonPj.getStatusInclude().get(1));
		assertEquals("*", jsonPj.getProfilingInclude().get(0));
		assertEquals("DOC_${item.getId().getRelPath().getNameBase()}_${item.getProperties().getString(\"cms:status\")}.pdf", jsonPj.getArea().getPathnameTemplate());
		assertEquals("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml?p=123", jsonPj.getItemid());
		

		//Asserts for PublishJobOptions
		PublishJobOptions publish = jsonPj.getOptions();
		assertEquals("abxpe",publish.getType());
		assertEquals("pdf/html/web/rtf/...", publish.getFormat());
		assertEquals("evaluated from pathname-template", publish.getPathname());
		
		//Asserts for PublishJobProgress
		PublishJobOptions options = jsonPj.getOptions();
		assertEquals("whatever engine needs to store",options.getProgress().get("engine").get("map"));
		assertEquals("value", options.getProgress().get("engine").get("key"));
		assertEquals("whatever they need to store", options.getProgress().get("webapp").get("map"));
		assertEquals("value", options.getProgress().get("webapp").get("key"));
		
		//Asserts for PublishJobParams
		Map<String, String> params = jsonPj.getOptions().getParams();
		assertEquals("stylesheet.css", params.get("stylesheet"));
		assertEquals("config.pdf", params.get("pdfconfig"));
		assertEquals("great", params.get("whatever"));

		//Asserts for PublishJobProfiling
		PublishJobProfiling profiling = jsonPj.getOptions().getProfiling();
		assertEquals("profilingName", profiling.getName());
		assertEquals("logical.expr", profiling.getLogicalexpr());

		//Asserts for PublishJobStorage
		PublishJobStorage storage = jsonPj.getOptions().getStorage();
		assertEquals("s3 / fs / ...", storage.getType());
		assertEquals("cms4", storage.getPathversion());
		assertEquals("name-from-cmsconfig-publish", storage.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", storage.getPathdir());
		
		//Asserts for PublishJobStorage's params
		Map<String, String> pJSParams = jsonPj.getOptions().getStorage().getParams();
		assertEquals("parameter for future destination types", pJSParams.get("specific"));
		assertEquals("cms-automation", pJSParams.get("s3bucket"));
		assertEquals("\\\\?\\C:\\my_dir", pJSParams.get("fspath"));

		//Asserts for PbulishJobPostProcess
		PublishJobPostProcess postProcess = jsonPj.getOptions().getPostprocess();
		assertEquals("future stuff", postProcess.getType());
		assertEquals("parameter for future postprocess stuff", postProcess.getParams().get("specific"));

		//Testing PublishJobDelivery
		assertEquals("webhook / s3copy", jsonPj.getOptions().getDelivery().getType());

		// Asserts for PublishJobItem
//		PublishJobItem item = jsonPj.getOptions().getReport3().getItems().get(0);
//		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg", item.getLogicalhead());
//		assertEquals("2013-11-06T17:36:05.941Z", item.getDate());
//		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1", item.getRepourl());
//		assertEquals("VV10084_25193", item.getNamebase());
//		assertEquals("2013-11-06T17:36:05.941Z", item.getCommit().getDate());
//		assertEquals(157, item.getCommit().getRevision());
//		assertEquals("/svn/demo1/vvab/graphics/VV10084_25193.jpg", item.getUri());
//		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1/vvab/graphics/VV10084_25193.jpg", item.getUrl());
//		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg?p=157", item.getLogical());
//		assertEquals(157, item.getRevision());
//		assertEquals(true, item.isHead());
//		assertEquals("/vvab/graphics/VV10084_25193.jpg", item.getPath());
//		assertEquals(1278231, item.getFile().getSize());

		//Asserts for PublishJobItem's meta
//		assertEquals("3958", item.getMeta().get("xmp_tiff.ImageWidth"));
//		assertEquals("300.0", item.getMeta().get("xmp_tiff.XResolution"));
//		assertEquals("2208", item.getMeta().get("xmp_tiff.ImageLength"));
//		assertEquals("VV10084_25193.jpg", item.getName());

		//Asserts for PublishJobItemChecksum
//		PublishJobItemChecksum checksum = jsonPj.getOptions().getReport3().getItems().get(0).getChecksum();
//		assertEquals("36f526d2abd89abe071c122cfa4930021d1a49824a408174023028aa026dc3e0", checksum.get("SHA256"));
//		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.get("SHA1"));
//		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.get("MD5"));

		//Asserts for PublishJobItem's properties
//		CmsItemPropertiesMap properties = (CmsItemPropertiesMap) jsonPj.getOptions().getReport3().getItems().get(0).getProperties();
//		assertEquals("image/jpeg", properties.get("svn:mime-type"));
//		assertEquals("photo", properties.get("cms:keywords"));
	}
	
	//Tests if the implemented methods from CmsItem runs correctly
	@Test //TODO: Really strange name of a test method. Test implemented methods do we ever test anything else?
	@Ignore
	public void testImplementedMethods() throws JsonProcessingException, FileNotFoundException, IOException {
//		PublishJob jsonPj = new PublishJob();
//		jsonPj = reader.readValue(getJsonString());

		//Asserts getItemId
//		PublishJobItem item = jsonPj.getOptions().getReport3().getItems().get(0);
//		CmsItemId itemId = item.getId();
//		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1/vvab/graphics/VV10084_25193.jpg", itemId.getUrl());
//		assertEquals(157, itemId.getPegRev().longValue());

//		RepoRevision revision = item.getRevisionChanged();
//		assertEquals("2013-11-06T17:36:05", revision.getDateIso());
//		assertEquals("In_Work", item.getStatus());
//		assertEquals(1278231, item.getFilesize());
	}
	//Tests if an exception is thrown when the .json file has an invalid dateformat
	@Test
	@Ignore
	public void testImproperDate() throws JsonProcessingException, FileNotFoundException, IOException {
//		try {
//			PublishJob jsonPj = new PublishJob();
//			jsonPj = reader.readValue(getJsonString());
//
//			jsonPj.getOptions().getReport3().getItems().get(0).setDate("09/22/1993T35:20:44.3821Z");
//			RepoRevision repoR = jsonPj.getOptions().getReport3().getItems().get(0).getRevisionChanged();
//			fail("Expected IllegalArgumentException to be thrown");
//		} catch(IllegalArgumentException e) {
//			assertNotNull(e);
//		}
	}
	//Test for implemented CheckSum.getHex() method in PublishJobCheckSum.java
	@Test
	@Ignore
	public void testPublishJobItemCheckSum() throws JsonProcessingException, FileNotFoundException, IOException {
//		PublishJob jsonPj = new PublishJob();
//		jsonPj = reader.readValue(getJsonString());
//		PublishJobItemChecksum checksum = jsonPj.getOptions().getReport3().getItems().get(0).getChecksum();

//		assertEquals("36f526d2abd89abe071c122cfa4930021d1a49824a408174023028aa026dc3e0", checksum.getHex(Algorithm.SHA256));
//		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.getHex(Algorithm.MD5));
//		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.getHex(Algorithm.SHA1));
//
//		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.getMd5());
//		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.getSha1());
//
//		assertEquals(true, checksum.has(Algorithm.SHA256));
//		assertEquals(true, checksum.equalsKnown(checksum));
	}
	@Test
	@Ignore
	public void testGettingItemsAsJsonString() throws JsonProcessingException, FileNotFoundException, IOException {
//		PublishJob jsonPj = new PublishJob();
//		jsonPj = reader.readValue(getJsonString());

//		PublishJobReport3 report3 = jsonPj.getOptions().getReport3();
//		String writeValueAsString = writer.writeValueAsString(report3);
//
//		ObjectReader report3JsonReader = reader.forType(PublishJobReport3Json.class);
//		PublishJobReport3Json report3Json = report3JsonReader.readValue(writeValueAsString);
//
//
//		PublishJobItem pjItem = new PublishJobItem();
//		pjItem.setLogicalhead("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg");
//		pjItem.setDate("2013-11-06T17:36:05.941Z");
//		pjItem.setRepourl("http://demo-dev.simonsoftcms.se/svn/demo1");
//		pjItem.setKind("file");
//		pjItem.setNamebase("VV10084_25193");
//		PublishJobItemCommit pjCommit = new PublishJobItemCommit();
//		pjCommit.setDate("2013-11-06T17:36:05.941Z");
//		pjCommit.setRevision(157);
//		pjItem.setCommit(pjCommit);
//		pjItem.setUri("/svn/demo1/vvab/graphics/VV10084_25193.jpg");
//		pjItem.setUrl("http://demo-dev.simonsoftcms.se/svn/demo1/vvab/graphics/VV10084_25193.jpg");
//		pjItem.setLogical("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg?p=157");
//		pjItem.setRevision(157);
//		pjItem.setHead(true);
//		pjItem.setPath("/vvab/graphics/VV10084_25193.jpg");
//		PublishJobItemFile pjFile = new PublishJobItemFile();
//		pjFile.setSize(1278231);
//		pjItem.setFile(pjFile);
//		pjItem.setMeta(null);
//		pjItem.setName("VV10084_25193.jpg");
//		PublishJobItemChecksum checksum = new PublishJobItemChecksum();
//		pjItem.setChecksum(checksum);
//		pjItem.setProperties(null);
//
//		PublishJobReport3Json reportJson = new PublishJobReport3Json();
//		List<PublishJobItem> items = new ArrayList();
//		items.add(pjItem);
//		reportJson.setItems(items);
//
//		jsonPj.getOptions().setReport3(reportJson);
//
//		PublishJobReport3 pjReport = jsonPj.getOptions().getReport3();
//
//		PublishJobItem publishJobItem = jsonPj.getOptions().getReport3().getItems().get(0);
//		assertEquals("2013-11-06T17:36:05.941Z", publishJobItem.getDate());
//		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg", publishJobItem.getLogicalhead());
//		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1", publishJobItem.getRepourl());
//		assertEquals("VV10084_25193", publishJobItem.getNamebase());
//		assertEquals("2013-11-06T17:36:05.941Z", publishJobItem.getCommit().getDate());
//		assertEquals(157, publishJobItem.getCommit().getRevision());
//		assertEquals("/svn/demo1/vvab/graphics/VV10084_25193.jpg", publishJobItem.getUri());
//		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1/vvab/graphics/VV10084_25193.jpg", publishJobItem.getUrl());
//		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg?p=157", publishJobItem.getLogical());
//		assertEquals(157 , publishJobItem.getRevision());
//		assertEquals(true, publishJobItem.isHead());
//		assertEquals("/vvab/graphics/VV10084_25193.jpg", publishJobItem.getPath());
//		assertEquals(1278231, publishJobItem.getFile().getSize());
//		assertEquals(null, publishJobItem.getMeta());
//		assertEquals("VV10084_25193.jpg", publishJobItem.getName());
//		assertEquals(null, publishJobItem.getProperties());
	}
	//Testing the Velocity template parsing function
	@Test //TODO: Why is this test based on report3? Is there not a itemId set in the PublishJob that you could use?
	@Ignore
	public void testPublishCOnfigTemplateString() throws JsonProcessingException, FileNotFoundException, IOException {
//		PublishJob jsonPj = new PublishJob();
//		jsonPj = reader.readValue(getJsonString());
//
//		PublishConfigTemplateString templateString = new PublishConfigTemplateString(jsonPj.getPathnameTemplate());

//		templateString.withEntry("item", jsonPj.getOptions().getReport3().getItems().get(0));
//		String evaluate = templateString.evaluate();
//		
//		assertEquals("DOC_VV10084_25193_In_Work.pdf" , evaluate);
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
