package se.simonsoft.cms.publish;

import static org.junit.Assert.*;

import org.junit.Test;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

public class PublishSourceCmsItemIdTest {

	@Test
	public void testGetURI() {
		CmsItemId id = new CmsItemIdArg("x-svn:///svn/repo1^/demo/Documents/Introduction.xml");
		assertEquals("x-svn:///svn/repo1^/demo/Documents/Introduction.xml", new PublishSourceCmsItemId(id).getURI());
	}

	@Test
	public void testGetURIRev() {
		CmsItemId id = new CmsItemIdArg("x-svn:///svn/repo1^/demo/Documents/Introduction.xml?p=51");
		assertEquals("x-svn:///svn/repo1^/demo/Documents/Introduction.xml?p=51", new PublishSourceCmsItemId(id).getURI());		
	}	
	
}
