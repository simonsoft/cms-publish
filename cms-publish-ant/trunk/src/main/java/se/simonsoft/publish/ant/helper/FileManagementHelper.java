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

	/*
	 *  Get the location to store the result in
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

	/*
	 * Moves a file
	 * @param String file to delete
	 */
	public void copyDirectory(String sourceDir, String destinationDir) throws IOException
	{
		FileUtils.copyDirectory(new File(sourceDir), new File(destinationDir));
	}

	/*
	 * Deletes a file
	 * @param String file to delete
	 */
	public void delete(File file)
	{
		logger.debug("start");
		try {
			FileDeleteStrategy.FORCE.delete(file);

		} catch(Exception e){
			logger.info("Could not delete file!", e);
			logger.debug(e.getMessage());
		}
		logger.debug("end");
	}

	/*
	 * Rename a file
	 * @param String file to rename
	 */
	public void rename(File oldFile, File newFile)
	{
		logger.debug("start");
		try {
			// Make a copy
			FileUtils.copyFile(oldFile, newFile, true);
			// Delete original
			this.delete(oldFile);
		} catch (IOException e) {
			logger.info("Could not rename file!", e);
			logger.debug(e.getMessage());
		}
		logger.debug("end");
	}

	
	/*
	 * Method that will zip file/dir
	 */
	public void zip(String zipFile, String sourceFolder) {
		List<String> fileList = new ArrayList<String>();
		this.generateFileList(fileList, new File(sourceFolder), sourceFolder);
		this.zipFile(fileList, zipFile, sourceFolder);
	}
	
	
	/**
	 * Zip it
	 * @param zipFile output ZIP file location
	 * Modified from http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
	 */
	private void zipFile(List<String> fileList, String zipFile, String sourceFolder) {

		byte[] buffer = new byte[1024];

		try{

			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);

			for(String file : fileList){

				ZipEntry ze= new ZipEntry(file);
				zos.putNextEntry(ze);

				FileInputStream in = new FileInputStream(sourceFolder + File.separator + file);

				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				in.close();
			}

			zos.closeEntry();
			//remember close it
			zos.close();

		}catch(IOException ex){
			logger.info("Could not rename file!", ex);  
		}
	}

	/**
	 * Traverse a directory and get all files,
	 * and add the file into fileList  
	 * @param node file or directory
	 * Modified from http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
	 */
	private void generateFileList(List<String> fileList, File node, String sourceFolder){

		//add file only
		if(node.isFile()){
			fileList.add(generateZipEntry(node.getAbsolutePath(), sourceFolder));
		}

		if(node.isDirectory()){
			String[] subNote = node.list();
			for(String filename : subNote){
				generateFileList(fileList, new File(node, filename), sourceFolder);
			}
		}
	}

	/**
	 * Format the file path for zip
	 * @param file file path
	 * @return Formatted file path
	 * Modified from http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
	 */
	private String generateZipEntry(String file, String sourceFolder)
	{
		//new File(sourceFolder).getAbsolutePath();
		return file.substring(new File(sourceFolder).getAbsolutePath().length()+1, file.length());
	}

	/*
	 * Unzips a file to an outputfolder and renames the files needed
	 * @param String zipfile
	 * @param String outputFolder
	 * @param String newFileName
	 */
	public void unZip(String zipFile, String destinationFolder, String newFileName, String sourceFolder){
		// Unzip and rename contents.

		byte[] buffer = new byte[1024];

		try {

			//create output directory is not exists
			File folder = new File(destinationFolder);

			if(!folder.exists()) {
				logger.debug("Create output dir " + folder.getPath());
				folder.mkdir();
			}
			if(!folder.isDirectory()) {
				folder.mkdir();
			}

			//get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFolder + File.separator + zipFile));
			//get the zipped file entry list
			ZipEntry ze = zis.getNextEntry();

			while(ze != null) {

				String fileName = ze.getName();

				// File in zip
				File fileToUnzip = new File(folder.getPath() + File.separator + fileName);
				File renamedFile = new File(folder.getPath() + File.separator + newFileName);

				logger.debug("file unzip : " + fileToUnzip.getAbsoluteFile());

				//create all non exists folders
				//else you will hit FileNotFoundException for compressed folder
				new File(fileToUnzip.getParent()).mkdirs();

				// Start getting the content
				FileOutputStream fos = new FileOutputStream(fileToUnzip);             

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				// Lets not use PEs stupid names
				if(fileName.equals("e3out.htm") || fileName.equals("e3out.xml")) {
					// Rename the file
					logger.debug("Rename " + fileToUnzip.getName());
					//this.forceRename(fileToUnzip, renamedFile);
					this.rename(fileToUnzip, renamedFile);
					/*
					if(fileToUnzip.renameTo(renamedFile)) {
						log("Renamed to " + renamedFile.getName());
					}else{
						log("Crap, could not rename file");
					}

					//*/
				}
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();

			logger.debug("End");

		} catch(IOException ex){
			logger.debug("Unzip failed: " + ex.getMessage());
		}
	}
}
