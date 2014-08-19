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
import org.apache.tools.ant.Task;

// Perhaps misleading name, deletets contents of dir.
public class DeleteDirTask extends Task {
	
	protected String directory;

	/**
	 * @return the directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * @param directory the directory to set
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public void execute() 
	{	
		this.deleteFile(new File(this.getDirectory()));
	}
	
	/*
	 * Deletets contents of directory.
	 * @param String file to delete
	 */
	private void deleteFile(File file)
	{
		try {
			log("Deleted: " + file.getName());
			//FileDeleteStrategy.FORCE.delete(file);
			FileUtils.cleanDirectory(file);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
}
