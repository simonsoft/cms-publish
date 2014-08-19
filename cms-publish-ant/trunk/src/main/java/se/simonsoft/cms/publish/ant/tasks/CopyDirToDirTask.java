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

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CopyDirToDirTask extends Task {
	
	protected String destination;
	protected String source;
	
	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}


	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}


	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}


	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}


	public void execute() 
	{
		if(this.getSource() == null) {
			this.doCopyDirectoryToDirectory(new File("export"), new File(this.getDestination()));
		}
		
		if(this.getDestination() == null) {
			log("You must specify a destination. Could not perform copy.");
			return;
		}
		
		this.doCopyDirectoryToDirectory(new File(this.getSource()), new File(this.getDestination()));
	}
	
	private void doCopyDirectoryToDirectory(File source, File destination)
	{
		try {
			// Copy source to destination and try to preserve timestamps
			FileUtils.copyDirectory(source, destination, true);
		} catch (IOException e) {
			throw new BuildException("Could not copy directory due to error: " + e.getMessage());
			
		}

	}
}
