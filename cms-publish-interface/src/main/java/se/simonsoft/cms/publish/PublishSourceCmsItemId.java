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
package se.simonsoft.cms.publish;

import java.io.InputStream;

import se.simonsoft.cms.item.CmsItemId;

/**
 * Logical ID without hostname (or should we require hostname at construction and return full logical id?).
 */
public class PublishSourceCmsItemId implements PublishSource {

	private CmsItemId id;

	public PublishSourceCmsItemId(CmsItemId id) {
		this.id = id;
	}
	
	@Override
	public String getURI() {
		return id.getLogicalId();
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public String getInputEntry() {
		return null;
	}

}
