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
package se.simonsoft.cms.publish.worker;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

import se.simonsoft.cms.item.export.CmsExportTagKey;
import se.simonsoft.cms.item.export.CmsExportTagValue;
import se.simonsoft.cms.publish.PublishSourceArchive;

public class PublishSourceArchiveMeta extends PublishSourceArchive {
	
	private transient Map<CmsExportTagKey, CmsExportTagValue> tagMap;
	
	public PublishSourceArchiveMeta(Supplier<InputStream> inputStream, Long inputLength, String inputEntry, Map<CmsExportTagKey, CmsExportTagValue> tagMap) {
		super(inputStream, inputLength, inputEntry);
		this.tagMap = tagMap;
	}

	public Map<CmsExportTagKey, CmsExportTagValue> getTagMap() {
		return tagMap;
	}

}
