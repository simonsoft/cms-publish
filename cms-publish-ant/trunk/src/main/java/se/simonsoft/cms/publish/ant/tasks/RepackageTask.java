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
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Task;

import se.simonsoft.cms.publish.ant.PublishJob;
import se.simonsoft.cms.publish.ant.nodes.JobNode;
import se.simonsoft.publish.ant.helper.FileManagementHelper;

public class RepackageTask extends Task {

	protected String fileName;
	protected String zipOutput;
	protected String rootfilename;
	protected String type;
	protected String outputfolder;
	protected String zipped;
	private FileManagementHelper fileHelper = new FileManagementHelper();
	
	/**
	 * @return the zipped
	 */
	public String getZipped() {
		return zipped;
	}

	/**
	 * @param zipped the zipped to set
	 */
	public void setZipped(String zipped) {
		this.zipped = zipped;
	}
	
	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the zipOutput
	 */
	public String getZipOutput() {
		return zipOutput;
	}

	/**
	 * @param zipOutput the zipOutput to set
	 */
	public void setZipOutput(String zipOutput) {
		this.zipOutput = zipOutput;
	}

	/**
	 * @return the rootfilename
	 */
	public String getRootfilename() {
		return rootfilename;
	}

	/**
	 * @param rootfilename the rootfilename to set
	 */
	public void setRootfilename(String rootfilename) {
		this.rootfilename = rootfilename;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the outputfolder
	 */
	public String getOutputfolder() {
		return outputfolder;
	}

	/**
	 * @param outputfolder the outputfolder to set
	 */
	public void setOutputfolder(String outputfolder) {
		this.outputfolder = outputfolder;
	}

	public void execute()
	{
		this.repackage();
	}
	
	private void repackage()
	{
		String temporaryPath = "";
		// Unzip if we have a zip
		if(getZipOutput().equals("yes")) {
			temporaryPath = "export" + File.separator + getFileName() + "_temp";
			fileHelper.unZip(getFileName(), temporaryPath, getRootfilename(), "export");
		}
		
		if(getZipped().equals("yes")) {
			fileHelper.zip("export" + File.separator + getFileName(), temporaryPath);
		}
		
		fileHelper.delete(new File(temporaryPath));
		
		if(zipOutput.equals("no")) {
			
		}
		
		
		if(fileName.contains(".")) {
			
		}
	}
}
