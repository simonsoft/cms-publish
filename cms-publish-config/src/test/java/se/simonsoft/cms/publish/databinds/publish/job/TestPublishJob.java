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
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class TestPublishJob {
	private static ObjectReader reader;
	private static ObjectMapper mapper = new ObjectMapper();


	@BeforeClass
	public static void setUp() {
		reader = mapper.reader(PublishJob.class);
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
		assertEquals("DOC_${item.getId().getRelPath().getNameBase()}_${item.getProperties().getString(\"cms:status\")}.pdf", jsonPj.getPathnameTemplate());
		assertEquals("x-svn:///svn/demo1^/vvab/xml/documents/900108.xml?p=123", jsonPj.getItemid());


		//Asserts for PublishJobOptions
		PublishJobOptions publish = jsonPj.getOptions();
		assertEquals("abxpe",publish.getType());
		assertEquals("pdf/html/web/rtf/...", publish.getFormat());
		assertEquals("evaluated from pathname-template", publish.getPathname());

		//Asserts for PublishJobProgress
		PublishJobOptions options = jsonPj.getOptions();
		assertEquals("1234", options.getProgress().getParams().get("ticket"));
		assertEquals("false", options.getProgress().getParams().get("isComplete"));

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
