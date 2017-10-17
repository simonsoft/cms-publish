package se.simonsoft.cms.publish.databinds.publish.job;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItem;
import se.simonsoft.cms.publish.databinds.publish.job.cms.item.PublishJobItemChecksum;

public class TestPublishJob {
	private static ObjectReader reader;

	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishJob.class);
	}

	@Test
	public void testJsonToPublishJob() throws JsonProcessingException, FileNotFoundException, IOException {
		PublishJob jsonPj = new PublishJob();
		jsonPj = reader.readValue(getJsonString());
		
		//Asserts for PublishJob
		assertEquals("name-from-cmsconfig-publish", jsonPj.getConfigname());
		assertEquals("publish", jsonPj.getType());
		assertEquals("publish-report3", jsonPj.getAction());
		assertEquals(true, jsonPj.isActive());
		assertEquals(true, jsonPj.isVisible());
		assertEquals("Review", jsonPj.getStatusInclude().get(0));
		assertEquals("Released", jsonPj.getStatusInclude().get(1));
		assertEquals("*", jsonPj.getProfilingInclude().get(0));
		assertEquals("velocity-stuff.pdf", jsonPj.getPathnameTemplate());
		assertEquals("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml?p=123", jsonPj.getItemid());
		
		//Asserts for PublishJobParams
		PublishJobPublish publish = jsonPj.getPublish();
		assertEquals("abxpe",publish.getType());
		assertEquals("pdf/html/web/rtf/...", publish.getFormat());
		
		//Asserts for PublishJob Params
		Map<String, String> params = jsonPj.getPublish().getParams();
		assertEquals("stylesheet.css", params.get("stylesheet"));
		assertEquals("config.pdf", params.get("pdfconfig"));
		assertEquals("great", params.get("whatever"));
		
		//Asserts for PublishJobProfiling
		PublishJobProfiling profiling = jsonPj.getPublish().getProfiling();
		assertEquals("profilingName", profiling.getName());
		assertEquals("logical.expr", profiling.getLogicalexpr());
		
		//Asserts for PublishJobReport3
		PublishJobMeta meta = jsonPj.getPublish().getReport3().getMeta();
		assertEquals("evaluated from pathname-template", meta.getPathname());
		
		//Asserts for PublishJobStorage
		PublishJobStorage storage = jsonPj.getPublish().getStorage();
		assertEquals("s3 / fs / ...", storage.getType());
		assertEquals("/cms4", storage.getPathprefix());
		assertEquals("/name-from-cmsconfig-publish", storage.getPathconfigname());
		assertEquals("/vvab/xml/documents/900108.xml", storage.getPathdir());
		assertEquals("900108 or profiling.name", storage.getPathnamebase());
		
		//Asserts for PublishJobStorage's params
		Map<String, String> pJSParams = jsonPj.getPublish().getStorage().getParams();
		assertEquals("parameter for future destination types", pJSParams.get("specific"));
		assertEquals("cms-automation", pJSParams.get("s3bucket"));
		assertEquals("\\\\?\\C:\\my_dir", pJSParams.get("fspath"));
		
		//Asserts for PbulishJobPostProcess
		PublishJobPostProcess postProcess = jsonPj.getPublish().getPostprocess();
		assertEquals("future stuff", postProcess.getType());
		assertEquals("parameter for future destination types", postProcess.getParams().get("specific"));
		
		//Testing PublishJobDelivery
		assertEquals("webhook / s3copy", jsonPj.getPublish().getDelivery().getType());
		
		// Asserts for PublishJobItem
		PublishJobItem item = jsonPj.getPublish().getReport3().getItems().get(0);
		assertEquals("x-svn://demo-dev.simonsoftcms.se/svn/demo1^/vvab/graphics/VV10084_25193.jpg", item.getLogicalhead());
		assertEquals("2013-11-06T17:36:05.941Z", item.getDate());
		assertEquals("http://demo-dev.simonsoftcms.se/svn/demo1", item.getRepourl());
		assertEquals(true, item.getKind().isFile());
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
		PublishJobItemChecksum checksum = jsonPj.getPublish().getReport3().getItems().get(0).getChecksum();
		assertEquals("36f526d2abd89abe071c122cfa4930021d1a49824a408174023028aa026dc3e0", checksum.get("SHA256"));
		assertEquals("2f55113d0efbf47f3888742346e29bde582942a0", checksum.get("SHA1"));
		assertEquals("280f99fb1e13e5834209fa70e65fa322", checksum.get("MD5"));
		
		//Asserts for PublishJobItem's properties
		CmsItemPropertiesMap properties = (CmsItemPropertiesMap) jsonPj.getPublish().getReport3().getItems().get(0).getProperties();
		assertEquals("image/jpeg", properties.get("svn:mime-type"));
		assertEquals("photo", properties.get("cms:keywords"));
		
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
