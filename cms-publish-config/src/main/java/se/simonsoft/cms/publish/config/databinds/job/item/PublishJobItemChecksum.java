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
package se.simonsoft.cms.publish.config.databinds.job.item;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.item.Checksum;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJobItemChecksum extends HashMap<String, String> implements Checksum {

	@Override
	public boolean has(Algorithm algorithm) {
		return this.containsKey(algorithm.toString());
	}
	@Override
	public String getMd5() throws UnsupportedOperationException {
		return getHex(Algorithm.MD5);
	}
	@Override
	public String getSha1() throws UnsupportedOperationException {
		return getHex(Algorithm.SHA1);
	}
	@Override
	public boolean equalsKnown(Checksum obj) {
		for (Algorithm a : Algorithm.values()) {
			if (has(a) && obj.has(a) && getHex(a).equals(obj.getHex(a))) return true;
		}
		return false;
	}
	@Override
	public String getHex(Algorithm algorithm) {
		return this.get(algorithm.toString());
	}
}
