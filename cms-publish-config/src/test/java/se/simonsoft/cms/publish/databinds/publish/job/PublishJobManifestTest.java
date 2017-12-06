package se.simonsoft.cms.publish.databinds.publish.job;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import se.simonsoft.cms.publish.databinds.publish.config.PublishConfigManifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class PublishJobManifestTest {

	@SuppressWarnings("unused")
	private static ObjectReader reader;
	private static ObjectWriter writer;

	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishJob.class);
		writer = mapper.writer();
	}

	@Test
	public void testManifestNoTemplates() throws JsonProcessingException {
		
		PublishConfigManifest config = getTestManifestConfig1();
		PublishJobManifest job = getTestManifestJob1(config);
		
		assertNotNull(job.getMetaTemplates());
		assertEquals(1, job.getMetaTemplates().size());
		
		String jobS = writer.writeValueAsString(job);
		assertEquals("{\"type\":\"test\",\"job\":{},\"document\":{},\"meta\":{\"static\":\"value\"}}", jobS);
		
		PublishJobManifest jobP = job.forPublish();
		assertEquals("{\"job\":{},\"document\":{},\"meta\":{\"static\":\"value\"}}", writer.writeValueAsString(jobP));
		
		assertEquals(1, jobP.getMeta().size());
		jobP.getMeta().put("one", "more");
		assertEquals(2, jobP.getMeta().size());
		assertEquals(1, job.getMeta().size());
		
	}

	public static PublishConfigManifest getTestManifestConfig1() {
		
		PublishConfigManifest config = new PublishConfigManifest();
		
		Map<String, String> metaTemplates = new HashMap<String, String>();
		metaTemplates.put("static", "template");
		config.setMetaTemplates(metaTemplates);
		
		return config;
	}
	
	public static PublishJobManifest getTestManifestJob1(PublishConfigManifest config) {
		
		PublishJobManifest job = new PublishJobManifest(config);
		job.setType("test");
		
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("static", "value");
		job.setMeta(meta);
		
		return job;
	}
	
}
