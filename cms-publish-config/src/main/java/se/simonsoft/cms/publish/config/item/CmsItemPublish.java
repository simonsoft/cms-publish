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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingSet;


public class CmsItemPublish implements CmsItem {

	private final CmsItem item;
	
	private static final String FIELD_ATTRIBUTE_LANG = "embd_xml_a_xml.lang";
	private static final String PROPNAME_LANGATTR = "abx:lang";
	
	/**
	 * @param item from reporting
	 * @throws IllegalArgumentException if item is null
	 */
	public CmsItemPublish(CmsItem item) {
		if (item == null) {
			throw new IllegalArgumentException("CmsItemPublish must be constructed with a non-null CmsItem");
		}
		/*
		if (item instanceof CmsItemPublish) {
			// Avoid wrapping CmsItemPublish in CmsItemPublish, which would be a no-op.
			throw new IllegalArgumentException("CmsItemPublish must not be constructed with another CmsItemPublish, use directly instead");
		}
		*/
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
		// Release prepared before CMS 4.3 will not have the ReleaseLocale property.
		if (isRelease() && rl == null) {
			// Fallback to lang attr, only on a Release.
			rl = item.getProperties().getString("abx:lang");
			if (rl != null && rl.trim().isEmpty()) {
				throw new IllegalStateException("Property 'abx:lang' must not be an empty string");
			}
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
	
	public String getLocale() {
		if (isTranslation()) {
			return getTranslationLocale();
		} else if (isRelease()) {
			return getReleaseLocale();
		} else {
			String lang = (String) item.getMeta().get(FIELD_ATTRIBUTE_LANG);
			if (lang == null || lang.isBlank()) {
				// Using the traditional property as fallback. Might be able to provide basic support for old schemas that don't use xml:lang. 
				lang = item.getProperties().getString(PROPNAME_LANGATTR);
			}
			if (lang == null || lang.isBlank()) {
				throw new IllegalStateException("The Author Area document does not define a language attribute.");
			}
			return lang;
		}
	}
	
	/**
	 * 
	 * @return true if the item has one or more profiling recipes
	 * @throws IllegalStateException if the CmsItemPublish was not constructed from a CmsItemReporting
	 */
	public boolean hasProfiles() {
		String profilingJson = getProfilingJson();
		if (profilingJson == null || profilingJson.trim().isEmpty()) {
			return false;
		} else if (profilingJson.equals("[]")) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param readerProfiling
	 * @return PublishProfilingSet preferring the indexed profiling JSON over the property.
	 * @throws IllegalStateException if the CmsItemPublish was not constructed from a CmsItemReporting
	 */
	public PublishProfilingSet getProfilingSet(ObjectReader readerProfiling) {
		if (!hasProfiles()) {
			// Prefer empty set instead of null, avoiding NPE if chaining getProfilingSetPublish / getProfilingSetRelease.
			return new PublishProfilingSet(); 
		}
		String profilingJson = getProfilingJson();
		try {
			PublishProfilingSet set = readerProfiling.readValue(profilingJson);
			return set;
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid Profiling definition in indexing or property: " + profilingJson);
		}
	}
	
	private String getProfilingJson() {
		// #1295 Not allowed if constructing a CmsItemPublish without CmsItemReporting.
		verifyMetaCms();
		
		String profilingJson = null;
		// #1295 Prefer indexed profiling JSON since property might be out of date.
		if (item.getMeta().containsKey("embd_cms_profiling")) {
			profilingJson = (String) item.getMeta().get("embd_cms_profiling");
		} else {
			// Temporary fallback to Abx property, potentially useful during 5.3 avoiding hard requirement to reindex.
			profilingJson = item.getProperties().getString("abx:Profiling");
		}
		return profilingJson;
	}
	
	
	private void verifyMetaCms() {
		if (item instanceof CmsItem.MetaCms) {
			return;
		} else if (item instanceof CmsItemPublish) {
			((CmsItemPublish) item).verifyMetaCms();
		} else {
			throw new IllegalStateException("CmsItemPublish must be constructed from reporting (with CmsItem.MetaCms), not " + item.getClass().getName());
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

	@Override
	public boolean isCmsClass(String cmsClass) {
		return item.isCmsClass(cmsClass);
	}

}
