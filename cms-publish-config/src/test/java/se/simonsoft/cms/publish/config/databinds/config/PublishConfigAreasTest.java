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
package se.simonsoft.cms.publish.config.databinds.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import se.simonsoft.cms.item.Checksum;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemKind;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.properties.CmsItemProperties;
import se.simonsoft.cms.item.properties.CmsItemPropertiesMap;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishConfigAreasTest {

	@Test
	public void testGetAreaRelease() {

		CmsItemPublish item = getItem(new CmsItemPropertiesMap("abx:ReleaseMaster", "x").and("abx:ReleaseLabel", "1.0"));

		PublishConfigArea fallback = getArea(null);
		PublishConfigArea release = getArea("release");
		PublishConfigArea translation = getArea("translation");

		PublishConfigArea result = PublishConfigAreas.getArea(item, Arrays.asList(fallback, release, translation));

		assertSame(release, result);
	}

	@Test
	public void testGetAreaTranslationFallsBackWhenNoTranslationArea() {

		CmsItemPublish item = getItem(new CmsItemPropertiesMap("abx:TranslationLocale", "en-GB"));

		PublishConfigArea fallback = getArea(null);
		PublishConfigArea release = getArea("release");

		PublishConfigArea result = PublishConfigAreas.getArea(item, Arrays.asList(fallback, release));

		assertSame(fallback, result);
	}

	@Test
	public void testGetAreaNoFallbackThrows() {

		CmsItemPublish item = getItem(new CmsItemPropertiesMap("abx:ReleaseMaster", "x").and("abx:ReleaseLabel", "1.0"));

		PublishConfigArea translation = getArea("translation");

		try {
			PublishConfigAreas.getArea(item, Arrays.asList(translation));
			fail("Should have thrown IllegalArgumentException, no fallback / release area configured");
		} catch (IllegalArgumentException e) {
			assertEquals("No fallback area configured, item is a Release.", e.getMessage());
		}
	}

	@Test
	public void testGetAreaDuplicateTypeThrows() {

		CmsItemPublish item = getItem(new CmsItemPropertiesMap("abx:ReleaseMaster", "x").and("abx:ReleaseLabel", "1.0"));

		PublishConfigArea release1 = getArea("release");
		PublishConfigArea release2 = getArea("release");

		try {
			PublishConfigAreas.getArea(item, Arrays.asList(release1, release2));
			fail("Should have thrown IllegalArgumentException, duplicate area type");
		} catch (IllegalArgumentException e) {
			assertEquals("Duplicate area objects with type: release", e.getMessage());
		}
	}

	private static PublishConfigArea getArea(String type) {
		PublishConfigArea area = new PublishConfigArea();
		area.setType(type);
		return area;
	}

	private static CmsItemPublish getItem(CmsItemProperties props) {
		return new CmsItemPublish(new CmsItemStub(props));
	}

	private static class CmsItemStub implements CmsItem {

		private final CmsItemProperties props;

		CmsItemStub(CmsItemProperties props) {
			this.props = props;
		}

		@Override
		public CmsItemId getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public RepoRevision getRevisionChanged() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRevisionChangedAuthor() {
			throw new UnsupportedOperationException();
		}

		@Override
		public CmsItemKind getKind() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getStatus() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCmsClass(String cmsClass) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Checksum getChecksum() {
			throw new UnsupportedOperationException();
		}

		@Override
		public CmsItemProperties getProperties() {
			return props;
		}

		@Override
		public Map<String, Object> getMeta() {
			return Collections.emptyMap();
		}

		@Override
		public long getFilesize() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void getContents(OutputStream receiver) {
			throw new UnsupportedOperationException();
		}

	}

}
