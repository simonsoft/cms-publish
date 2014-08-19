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
package se.simonsoft.cms.publish.ant.tasks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Task;

public class FindLatestRevisionTask extends Task {

	public void execute() 
	{
		String revision = this.readRevisionFile();
		// Modify revisionnumber to use the next iteration ie: 
		// if last revision was 1000 we want to check for files with rev 1001 and onwards.
		log("revision:" + revision);
		
		Long revisionNumber = Long.parseLong(revision);
		//*
		if(revision.equals("0")) {
			revision = revisionNumber.toString();
		}else{
			revisionNumber = revisionNumber + 1;
			revision = revisionNumber.toString();
			log("Change to next revision: " + revision);
		}
		//*/
		//revision = revisionNumber.toString();
		
		// Only set property if we've got a value
		
		if(!revision.equals("")) {
			this.getProject().setProperty("previousrevision", revision);
		}
	}
	
	private String readRevisionFile()
	{
		List<String> strings;
		
		if(!FileUtils.getFile("latestrev.txt").exists()){
			log("No previois latestrev.txt file. Returning rev 0 as default");
			return "0";
		}
		
		try {
			strings = FileUtils.readLines(new File("latestrev.txt"), "utf-8");
			
			// Get the first line only
			return strings.get(0); 
		} catch (IOException e) {
			log("Could not read latestrev.txt: " + e.getMessage());
			//e.printStackTrace();
		}
		// Failed to get anything from file
		return ""; 
	}
}
