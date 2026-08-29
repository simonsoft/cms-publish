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
package se.simonsoft.cms.publish.config;

import java.util.HashMap;
import java.util.List;

import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigArea;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

/**
 * Selects the applicable {@link PublishConfigArea} for an item, out of a {@link PublishConfig}'s configured areas.
 * Shared between {@code PublishJobManifestBuilder}/{@code PublishJobFactory} (cms-publish-rest, when actually
 * starting a publish job) and any consumer that needs to predict area-dependent config (e.g. docno templates)
 * without starting a job.
 */
public class PublishConfigAreas {

	private PublishConfigAreas() {
		// Static utility class.
	}

	public static PublishConfigArea getArea(CmsItemPublish item, List<PublishConfigArea> areas) {

		HashMap<String, PublishConfigArea> areaMap = getAreaMap(areas);

		PublishConfigArea fallback = areaMap.get(null);

		if (item.isTranslation() && areaMap.containsKey("translation")) {
			return areaMap.get("translation");
		} else if (item.isRelease() && areaMap.containsKey("release")) {
			return areaMap.get("release");
		} else if (fallback != null) {
			return fallback;
		// Differentiating error messages to simplify troubleshooting.
		} else if (item.isTranslation()) {
			throw new IllegalArgumentException("No fallback area configured, item is a Translation.");
		} else if (item.isRelease()) {
			throw new IllegalArgumentException("No fallback area configured, item is a Release.");
		} else {
			throw new IllegalArgumentException("No fallback area configured, item is NOT a Release / Translation.");
		}
	}


	private static HashMap<String, PublishConfigArea> getAreaMap(List<PublishConfigArea> areas) {

		HashMap<String, PublishConfigArea> result = new HashMap<String, PublishConfigArea>(areas.size());

		for (PublishConfigArea area: areas) {
			String type = area.getType();

			PublishConfigArea prev = result.put(type, area);
			if (prev != null) {
				throw new IllegalArgumentException("Duplicate area objects with type: " + type);
			}
		}
		return result;
	}

}
