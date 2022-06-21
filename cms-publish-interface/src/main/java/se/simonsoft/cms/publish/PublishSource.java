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

public interface PublishSource {

	/**
	 * @return string representation of URI where source can be downloaded
	 * @deprecated
	 */
	String getURI();

	/**
	 * A {@link Supplier} of {@code InputStream} is used in
	 * case the request needs to be repeated, as the content is not buffered.
	 * The {@code Supplier} may return {@code null} on subsequent attempts,
	 * in which case the request fails.
	 * 
	 * The stream will be closed.
	 * 
	 * @return supplier of inputstream with source data, potentially an archive
	 */
	Supplier<InputStream> getInputStream();

	/**
	 * @return input entry name when {@link #getInputStream()} is an archive
	 */
	String getInputEntry();

}
