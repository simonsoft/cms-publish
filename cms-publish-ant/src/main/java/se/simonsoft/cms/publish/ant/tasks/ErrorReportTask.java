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
package se.simonsoft.cms.publish.ant.tasks;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ErrorReportTask extends Task {

	public void execute()
	{
		
		if(this.provideErrorsList()) {
			this.cleanup();
			// After we've listed all errors, throw the exception
			throw new BuildException("We've found errors");
		}
		
	}
	/*
	 * Read errors from error log and output them
	 */
	private boolean provideErrorsList()
	{
		
		List<String> errors;
		
		try {
			File errorslog = new File("errors.log"); 
			//*
			if(!errorslog.exists()) {
				errorslog.createNewFile();
			}
			//*/
			errors = FileUtils.readLines(errorslog, "utf-8");
			// If we have any errors to show, log them out
			if(errors != null && errors.size() > 0) {
				log("Found " + errors.size() +" error(s)");
				for(String error: errors) {
					log(error);
				}
				return true;
			}else {
				log("No errors found!");
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	/*
	 * Save the current log for backup and empty errors.log
	 */
	private void cleanup() 
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		Date date = new Date();
		
		try {
			log("Cleanup log");
			FileUtils.copyFile(new File("errors.log"), new File(dateFormat.format(date) + "_errors.log"), true);
			FileDeleteStrategy.FORCE.delete(new File("errors.log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
