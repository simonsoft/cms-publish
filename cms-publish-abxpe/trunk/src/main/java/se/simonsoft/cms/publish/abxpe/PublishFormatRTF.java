/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.publish.abxpe;

import se.simonsoft.cms.publish.PublishFormat;

public class PublishFormatRTF implements PublishFormat {

	@Override
	public String getFormat() {
		return "rtf";
	}

	@Override
	/*
	 * With RTF output with embedded graphics we should not need a package.
	 * But at least now, this is what we need.
	 * @see se.simonsoft.cms.publish.PublishFormat#getOutputCompression()
	 */
	public Compression getOutputCompression() {
		return Compression.zip;
	}

}
