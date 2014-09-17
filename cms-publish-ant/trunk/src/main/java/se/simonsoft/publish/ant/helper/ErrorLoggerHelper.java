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
package se.simonsoft.publish.ant.helper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLoggerHelper {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/*
	 * Write errors to our error log file
	 */
	public void addToErrorLog(String error){
	
		try {
			FileUtils.writeStringToFile(new File("errors.log"), error, "utf-8", true);
		} catch (IOException e) {
			logger.debug("Could not write to error log! " + e.getMessage());
		}
	}
}