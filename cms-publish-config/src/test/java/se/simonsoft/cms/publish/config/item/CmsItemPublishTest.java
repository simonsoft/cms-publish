package se.simonsoft.cms.publish.config.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobManifest;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobReport3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class CmsItemPublishTest {

	private static ObjectReader reader;
	@SuppressWarnings("unused")
	private static ObjectWriter writer;

	@BeforeClass
	public static void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		reader = mapper.reader(PublishJobReport3.class);
		writer = mapper.writer().forType(PublishJobManifest.class);
	}
	
	private CmsItemPublish getItem() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/databinds/resources/publish-report3-release.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
		
		PublishJobReport3 report3 = reader.readValue(resourceAsStream);
		return new CmsItemPublish(report3.getItems().get(0));
	}
	
	@Test
	public void testGetStatus() throws FileNotFoundException, IOException {
		CmsItem item = getItem();
		assertNotNull(item);
		assertEquals("Released", item.getStatus());
	}
	
	@Test
	public void testIsRelease() throws FileNotFoundException, IOException {
		CmsItemPublish item = getItem();
		assertEquals(true, item.isRelease());
	}

	
	@Test
	public void testIsTranslation() throws FileNotFoundException, IOException {
		CmsItemPublish item = getItem();
		assertEquals(false, item.isTranslation());
	}
	
	@Test
	public void testGetReleaseLabel() throws FileNotFoundException, IOException {
		CmsItemPublish item = getItem();
		assertEquals("B", item.getReleaseLabel());
	}
	
	@Test
	public void testGetTranslationLocale() throws FileNotFoundException, IOException {
		CmsItemPublish item = getItem();
		assertNull(item.getTranslationLocale());
	}
	
}
