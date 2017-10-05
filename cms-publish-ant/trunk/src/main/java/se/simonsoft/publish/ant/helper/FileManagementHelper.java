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
package se.simonsoft.publish.ant.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * For now we use apache commons.
 */
public class FileManagementHelper {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * 
	 * @param outputFolder
	 * @param fileName
	 * @return
	 */
	public FileOutputStream createStorageLocation(String outputFolder, String fileName) {

		File outputfile = null;

		FileOutputStream outputStream = null;

		try {
			// Make sure the outputfolder exists
			File folder = new File(outputFolder);
			if(!folder.exists()){
				folder.mkdir();
			}
			// Then we create the file in that location
			outputfile = new File(outputFolder + "/" + fileName);
			logger.debug("Absolutepath: " + outputfile.getAbsolutePath());

			outputfile.createNewFile();
			// Connect the stream to the file
			outputStream = new FileOutputStream(outputfile);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new BuildException("Could not store file at (file not found) " + outputfile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new BuildException("Could not store file at (IO) " + outputfile.getAbsolutePath());
		}

		return outputStream;
	}

	/**
	 * 
	 * @param file
	 */
	public void delete(File file)
	{
		logger.debug("start");
		try {
			FileDeleteStrategy.FORCE.delete(file);

		} catch(Exception e){
			logger.info("Could not delete file!");
			logger.debug(e.getMessage());
		}
		logger.debug("end");
	}
}
