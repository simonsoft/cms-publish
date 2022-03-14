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
package se.simonsoft.cms.publish.worker.export;

import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportReader;
import se.simonsoft.cms.item.export.CmsExportUrlPresigner;
import se.simonsoft.cms.item.export.CmsExportWriter;

public class CmsExportProviderNotConfigured implements CmsExportProvider {
	
	private final String type;
	
	public CmsExportProviderNotConfigured(String type) {
		this.type = type;
	}

	@Override
	public CmsExportReader getReader() {
		throw new UnsupportedOperationException("System is not configured to support imports of type: " + type);
	}

	@Override
	public CmsExportWriter getWriter() {
		throw new UnsupportedOperationException("System is not configured to support exports of type: " + type);
	}

	@Override
	public CmsExportUrlPresigner getUrlPresigner() {
		throw new UnsupportedOperationException("System is not configured to support exports of type: " + type);
	}

}
