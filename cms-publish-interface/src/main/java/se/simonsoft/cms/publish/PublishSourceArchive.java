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
import java.util.function.Supplier;

public class PublishSourceArchive implements PublishSource {

	private final Supplier<InputStream> inputStream;
	private final Long inputLength;
	private final String inputEntry;
	
	public PublishSourceArchive(Supplier<InputStream> inputStream, Long inputLength, String inputEntry) {
		this.inputStream = inputStream;
		this.inputLength = inputLength;
		this.inputEntry = inputEntry;
	}
	
	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Supplier<InputStream> getInputStream() {
		return inputStream;
	}

	@Override
	public String getInputEntry() {
		return inputEntry;
	}
	
	
	public Long getInputLength() {
		return inputLength;
	}

}
