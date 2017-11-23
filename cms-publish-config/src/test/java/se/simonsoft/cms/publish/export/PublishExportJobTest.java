package se.simonsoft.cms.publish.export;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.simonsoft.cms.publish.databinds.publish.job.PublishJobStorage;

public class PublishExportJobTest {
	
	@Test
	public void testJobPath() throws Exception {
		
		PublishJobStorage storage = new PublishJobStorage();
		storage.setPathversion("/cms4");
		storage.setPathcloudid("demo1");
		storage.setPathconfigname("simple-pdf");
		storage.setPathdir("/vvab/release/B/xml/documents/900108.xml");
		storage.setPathnamebase("900108_r0000000145");
		
		PublishExportJob job = new PublishExportJob(storage, "zip");
		
		assertEquals("demo1/simple-pdf/vvab/release/B/xml/documents/900108.xml/900108_r0000000145.zip" ,job.getJobPath());
	}
}