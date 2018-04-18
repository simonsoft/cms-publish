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
package se.simonsoft.cms.publish.config.item;

import java.io.OutputStream;
import java.util.Map;

import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;


public class CmsItemPublish implements CmsItem {

	private final CmsItem item;
	
	public CmsItemPublish(CmsItem item) {
		this.item = item;
	}

	public boolean isRelease() {
		return item.getProperties().containsProperty("abx:ReleaseMaster") && item.getProperties().containsProperty("abx:ReleaseLabel");
	}
	
	public boolean isTranslation() {
		return item.getProperties().containsProperty("abx:TranslationLocale");
	}
	
	public String getReleaseLabel() {
		String rl = item.getProperties().getString("abx:ReleaseLabel");
		if (rl != null && rl.trim().isEmpty()) {
			throw new IllegalStateException("Property 'ReleaseLabel' must not be an empty string");
		}
		return rl;
	}
	
	public String getReleaseLocale() {
		String rl = item.getProperties().getString("abx:ReleaseLocale");
		if (rl != null && rl.trim().isEmpty()) {
			throw new IllegalStateException("Property 'ReleaseLocale' must not be an empty string");
		}
		return rl;
	}
	
	public String getTranslationLocale() {
		String tl = item.getProperties().getString("abx:TranslationLocale");
		if (tl != null && tl.trim().isEmpty()) {
			throw new IllegalStateException("Property 'TranslationLocale' must not be an empty string");
		}
		return tl; 
	}
	
	public boolean hasProfiles() {
		
		String profilesProp = this.getProperties().getString("abx:Profiling");
		if (profilesProp == null || profilesProp.trim().isEmpty()) {
			return false;
		} else if (profilesProp.equals("[]")) {
			return false;
		} else {
			return true;
		}
	}
	
	
	// START: Passthrough methods.
	
	@Override
	public CmsItemId getId() {
		return item.getId();
	}

	@Override
	public RepoRevision getRevisionChanged() {
		return item.getRevisionChanged();
	}

	@Override
	public String getRevisionChangedAuthor() {
		return item.getRevisionChangedAuthor();
	}

	@Override
	public CmsItemKind getKind() {
		return item.getKind();
	}

	@Override
	public String getStatus() {
		return item.getStatus();
	}

	@Override
	public Checksum getChecksum() {
		return item.getChecksum();
	}

	@Override
	public CmsItemProperties getProperties() {
		return item.getProperties();
	}

	@Override
	public Map<String, Object> getMeta() {
		return item.getMeta();
	}

	@Override
	public long getFilesize() {
		return item.getFilesize();
	}

	@Override
	public void getContents(OutputStream receiver) throws UnsupportedOperationException {
		item.getContents(receiver);
	}

}
