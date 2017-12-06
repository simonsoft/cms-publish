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
