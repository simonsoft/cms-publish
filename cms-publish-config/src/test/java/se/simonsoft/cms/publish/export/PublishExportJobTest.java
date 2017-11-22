package se.simonsoft.cms.publish.export;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import se.simonsoft.cms.publish.databinds.publish.job.PublishJobStorage;
import se.simonsoft.cms.publish.export.PublishExportJob;

public class PublishExportJobTest {
	
	@Test
	@Ignore
	public void testJobPath() throws Exception {
		
		PublishJobStorage storage = new PublishJobStorage();
		storage.setPathversion("/cms4");
		storage.setPathcloudid("demo1");
		storage.setPathconfigname("simple-pdf");
		storage.setPathdir("/vvab/release/B/xml/documents/900108.xml");
		storage.setPathnamebase("900108_r0000000145");
		
		PublishExportJob job = new PublishExportJob(storage, "zip");
		
		assertEquals("demo1/simple-pdf/vvab/release/B/xml/documents/900108.xml/900108_r0000000145" ,job.getJobPath());
		//demo1/900108_r0000000145/simple-pdf/vvab/release/B/xml/documents/900108.xml.zip
	}

}
