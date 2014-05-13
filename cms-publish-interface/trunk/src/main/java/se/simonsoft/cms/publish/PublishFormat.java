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
package se.simonsoft.cms.publish;

/**
 * Represents a format, e.g. PDF, WEB, EPUB.
 * Format string approved by {@link PublishService#getPublishFormat(String)}.
 * {@link #toString()} can be a label if {@link #getFormat()} is a human-unreadable identifier.
 * @author takesson
 *
 */
public interface PublishFormat {

	public enum Compression {
		zip,
		tgz
	}
	
	/**
	 * @return Format identifier as understood by the publish service.
	 */
	String getFormat();
	
	/**
	 * 
	 * @return non-null if output file for the format is an archive
	 */
	Compression getOutputCompression();
	
}
