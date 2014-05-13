/*******************************************************************************
 * Copyright 2014 Simonsoft Nordic AB
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package se.simonsoft.cms.publish.abxpe;

import se.simonsoft.cms.publish.PublishFormat;

public class PublishFormatPDF implements PublishFormat {

	@Override
	public String getFormat() {
		// PDF format
		return "pdf";
	}

	@Override
	public Compression getOutputCompression() {
		// PE does not compress PDFs by default.
		return null;
	}

}
