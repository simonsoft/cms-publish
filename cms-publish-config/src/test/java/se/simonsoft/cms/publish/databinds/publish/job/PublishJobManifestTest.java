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
		
		PublishConfigManifest config = new PublishConfigManifest();
		
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("static", "value");
		config.setMetaTemplates(meta);
		
		
		PublishJobManifest job = new PublishJobManifest(config);
		job.setType("test");
		job.setMeta(meta);
		
		assertNotNull(job.getMetaTemplates());
		assertEquals(1, job.getMetaTemplates().size());
		
		String jobS = writer.writeValueAsString(job);
		assertEquals("{\"type\":\"test\",\"meta\":{\"static\":\"value\"}}", jobS);
		
		PublishJobManifest jobP = job.forPublish();
		assertEquals("{\"meta\":{\"static\":\"value\"}}", writer.writeValueAsString(jobP));
	}

}
