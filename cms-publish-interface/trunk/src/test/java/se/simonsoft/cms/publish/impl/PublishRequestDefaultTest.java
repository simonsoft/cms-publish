package se.simonsoft.cms.publish.impl;

import static org.junit.Assert.*;


import org.junit.Test;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;

public class PublishRequestDefaultTest {
	
	private String publishHost = "pds-suse-svn3.pdsvision.net";
	private String publishPath = "/e3/servlet/e3";
	
		
	@Test
	public void testGetFile(){
		// Might be overkill to test.
		PublishRequestDefault pubreq = new PublishRequestDefault();
		CmsItemId id = new CmsItemIdArg("x-svn:///svn/repo1^/demo/Documents/Introduction.xml");
		pubreq.setFile(new PublishSourceCmsItemId(id));
		
		assertEquals("x-svn:///svn/repo1^/demo/Documents/Introduction.xml", pubreq.getFile().getURI());
	}
}
