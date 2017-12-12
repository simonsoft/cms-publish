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
package se.simonsoft.cms.publish.config.export;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJob;

public class PublishExportJobTest {
	
	@Test
	public void testJobPath() throws Exception {
		
		PublishJobStorage storage = new PublishJobStorage();
		storage.setPathversion("cms4");
		storage.setPathcloudid("demo1");
		storage.setPathconfigname("simple-pdf");
		storage.setPathdir("vvab/release/B/xml/documents/900108.xml"); //Should not be preceded by a slash, CmsExportJob adds slash directly after prefix.
		storage.setPathnamebase("900108_r0000000145");
		
		PublishExportJob job = new PublishExportJob(storage, "zip");
		
		assertEquals("simple-pdf/vvab/release/B/xml/documents/900108.xml/900108_r0000000145.zip" ,job.getJobPath());
	}
}
