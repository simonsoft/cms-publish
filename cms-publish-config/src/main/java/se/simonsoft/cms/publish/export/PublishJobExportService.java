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
package se.simonsoft.cms.publish.export;


import java.io.InputStream;

import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;

public interface PublishJobExportService {
	//TODO: consider sending a CmsExportItem
	String exportJob(InputStream os, PublishJobOptions jobOptions);
	
	String exportJobManifest(PublishJobOptions jobOptions);
	
}
