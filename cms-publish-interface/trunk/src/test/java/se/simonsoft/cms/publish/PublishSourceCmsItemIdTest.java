/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
