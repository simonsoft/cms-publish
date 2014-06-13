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
package se.simonsoft.cms.publish.ant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.Task;

public class FindLatestRevisionTask extends Task {

	public void execute() 
	{
		String revision = "";
		try {
			revision = this.readRevisionFile();
			// Modify revisionnumber to use the next iteration ie: 
			// if last revision was 1000 we want to check for files with rev 1001 and onwards.
			Long revisionNumber = Long.parseLong(revision);
			revisionNumber = revisionNumber + 1;
			revision = revisionNumber.toString();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Only set property if we've got a value
		if(revision.equals("")) {
			this.getProject().setProperty("previousrevision", revision);
		}
		
	}
	
	private String readRevisionFile() throws IOException
	{
		BufferedReader br = null;
	
	    try {
	    	br = new BufferedReader(new FileReader("latestrev.txt"));
	        StringBuilder sb = new StringBuilder();
	        String firstline = br.readLine();
	        return firstline;
	    } catch (IOException e) {
	    	log("No latestrev.txt file found.");
	    	//e.printStackTrace();
		} finally {
	        br.close();
	    }
		return "";
	    
	}
}
