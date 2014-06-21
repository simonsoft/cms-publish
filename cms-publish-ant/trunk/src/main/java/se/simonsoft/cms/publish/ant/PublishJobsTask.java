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
import org.apache.tools.ant.Task;

import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishSourceCmsItemId;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

/*
 * Publish a an item to the number of outputs (jobs).
 * The task also manages the output and unzips jobs if requested. 
 * Perhaps working with the result should be another task later
 */
public class PublishJobsTask extends Task {
	protected ConfigsNode configs;
	protected String publishservice;
	protected JobsNode jobs;
	protected String zipoutput;
	protected String outputfolder;
	private ArrayList<PublishJob> publishedJobs;
	private PublishServicePe publishService;
	protected boolean fail;
	
	
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

	/**
	 * @return the zipoutput
	 */
	public String getZipoutput() {
		return zipoutput;
	}

	/**
	 * @param zipoutput the zipoutput to set
	 */
	public void setZipoutput(String zipoutput) {
		this.zipoutput = zipoutput;
	}
	
	/**
	 * @return the fail
	 */
	public boolean isFail() {
		return fail;
	}

	/**
	 * @param fail the fail to set
	 */
	public void setFail(boolean fail) {
		this.fail = fail;
	}

	/**
	 * @return the configs
	 */
	public ConfigsNode getConfigs() {
		return configs;
	}

	/**
	 * @param jobs the jobs to set
	 */
	public void addConfiguredJobs(JobsNode jobs) {
		this.jobs = jobs;
	}
	
	/**
	 * @return the jobs
	 */
	public JobsNode getJobs() {
		return jobs;
	}

	/**
	 * @param configs the configs to set
	 */
	public void addConfiguredConfigs(ConfigsNode configs) {
		this.configs = configs;
	}

	/**
	 * @return the publishservice
	 */
	public String getPublishservice() {
		return publishservice;
	}

	/**
	 * @param publishservice the publishservice to set
	 */
	public void setPublishservice(String publishservice) {
		this.publishservice = publishservice;
	}
	
	public void execute() {
		
		this.publishService = new PublishServicePe(); // Create a PE service
		
		this.publishJobs(); // Publish the jobs
		
		try {
			
			if(this.isCompleted()) { // Check if the jobs are ready
				this.getResult(); // Download the result
				this.handleResult(); //  output is unzipped if it needs to be
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Publish each job
	 */
	private void publishJobs()
	{
		this.publishedJobs = new ArrayList<PublishJob>();
		
		for (final JobNode job : jobs.getJobs()) {
			// Create the request
			PublishRequestDefault publishRequest = this.createRequestDefault(job.getParams());
			
			// Send the request
			PublishTicket ticket = publishService.requestPublish(publishRequest);
		
			if(ticket == null){
				log("Could not send request to PublishingEngine");
				// Here we would like to output PE error.
				this.addToErrorLog("PublicationException: Did not get response back from PE for file: " + publishRequest.getFile().getURI());
			}else {
				// Store the request and id as a job
				PublishJob publishJob = new PublishJob(ticket, publishRequest, job.getFilename());
				// Save the job
				this.publishedJobs.add(publishJob);
			}
		}
	}
	
	/*
	 * Create the default request and set the config, params and other properties
	 */
	private PublishRequestDefault createRequestDefault(ParamsNode paramsNode)
	{
		log("Create a publishrequest");
		PublishRequestDefault publishRequest = new PublishRequestDefault();
		
		// set config
		if (null != configs && configs.isValid()) {
			for (final ConfigNode config : configs.getConfigs()) {
				log("Configs: " + config.getName() + ":" + config.getValue());
				publishRequest.addConfig(config.getName(), config.getValue());
			}
		}
		
		// set params
		if (null != paramsNode && paramsNode.isValid()) {
			for (final ParamNode param : paramsNode.getParams()) {
				log("Params: " + param.getName() + ":" + param.getValue());
				if(param.getName().equals("file")) {
					// We remove the .xml file ending for naming convention.
					
					// TODO set correct publish source (now we default to cmsitem)
					// Should set this as a parameter to the job?
					publishRequest.setFile(
							new PublishSourceCmsItemId(
									new CmsItemIdArg(param.getValue())
									));
					log("PublishRequestFile: " + publishRequest.getFile().getURI());
				}
				// For the type we create a publishformat to set (this is a fix, need to do this another way, by asking publish service)
				if(param.getName().equals("type")) {
					final String type = param.getValue();
					publishRequest.setFormat(this.publishService.getPublishFormat(param.getValue()));
					log("publishRequest Format: " + publishRequest.getFormat().getFormat());
				}
				// For arbitrary params just add them
				publishRequest.addParam	(param.getName(), param.getValue());
			}
		}
		return publishRequest;
	}
	
	/*
	 * Go through all publish jobs and check if they are completed
	 */
	private boolean isCompleted() throws InterruptedException{
		log("Check if publish requests are completed");
		boolean isAllComplete = false;
		int completedCount = 0;
		int checks = 1;
		while(!isAllComplete){
		
			for (PublishJob publishJob : this.publishedJobs) {
				boolean isComplete = false;
				if(!publishJob.isCompleted()) {
					isComplete = publishService.isCompleted(publishJob.getTicket(), publishJob.getPublishRequest());
					//log("Ticket id " + publishJob.getTicket().toString() + " check. Total checks: #" + checks++);
					if(isComplete) {
						publishJob.setCompleted(isComplete);
						log("Ticket id: " + publishJob.getTicket().toString() +" completed after " + checks + " checks.");					
						completedCount++;
					}
				}
				// Timeout. This is an arbitrary number. What should count as a timeout?
				// TODO timeout at all?
				/*
				if(checks > 2000){
					return false;
				}
				//*/
				checks++;
			}
			// Are all jobs completed?
			if(completedCount == this.publishedJobs.size()) {
				isAllComplete = true;
			}
			Thread.sleep(2000); // Be patient
		}
		return true;
	}
	
	/*
	 * Downloads the result from PE
	 */
	private void getResult() {
		log("Getting the results");
		for (PublishJob publishJob : this.publishedJobs) {
			try {	
				this.publishService.getResultStream(publishJob.getTicket(), publishJob.getPublishRequest(), this.resultLocation(publishJob.getFilename()));
			} catch (PublishException e) {
				log("Ticket: " + publishJob.getTicket().toString() + " failed for file: " + publishJob.getPublishRequest().getFile().getURI());
				
				this.addToErrorLog("PublishException for ticket: " + publishJob.getTicket().toString() + ". Publish failed for file: " + publishJob.getPublishRequest().getFile().getURI() + " with errors: " + e.getMessage() + "\n");
				// Let's also remove the output
				this.deleteFile(new File(outputfolder + "/" + publishJob.getFilename()));
				
			}
		}
	}
	
	/*
	 * Write errors to our error log file
	 */
	private void addToErrorLog(String error){
	
		try {
			FileUtils.writeStringToFile(new File("errors.log"), error, "utf-8", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 *  Get the location to store the result in
	 */
	private FileOutputStream resultLocation(String filename) {

		File outputfile = null;

		FileOutputStream outputStream = null;
		
		try {
			// Make sure the outputfolder exists
			File folder = new File(outputfolder);
			if(!folder.exists()){
				folder.mkdir();
			}
			// Then we create the file in that location
			outputfile = new File(outputfolder + "/" + filename);
			log("Absolutepath: " + outputfile.getAbsolutePath());

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
	
	/* TODO: Should all this file management be a another Task instead? I think so.
	 * Loops over the jobs and the result to unzip the archives 
	 * that needs to be delivered as folders.
	 */
	private void handleResult() {
		// TODO unzip the ones that should no be zipped archives..
		log("Handle zip outputs");
		for (PublishJob publishJob : this.publishedJobs) {
			
			for (final JobNode job : jobs.getJobs()) {

				if(job.getFilename().equals(publishJob.getFilename())) {

					if(job.getZipoutput() != null && job.getZipoutput().equals("no")) {
						log("Manage zip to folder");

						if(publishJob.getFilename().contains(".")) {
							// A little flaky but for now we assume that packages that
							// like 1195437-en-US.html.zip is always the ones 
							// that should be unzipped.
							String slices[] = publishJob.getFilename().split("\\.");
							String newFileName = slices[0] + "." + slices[1];
							String tempFolderPath = this.outputfolder + File.separator + job.getRootfilename() + "_temp";
							// Unzip and rename file.
							// We can assume that the outpufolders has been created, so it's safe to crate the new subfolder
							this.unZip(publishJob.getFilename(), tempFolderPath, job.getRootfilename());
							
							// Move contents to outputfolder
							this.moveUnzippedResult(new File(tempFolderPath), new File(this.outputfolder));
							
							//this.moveFilesFromFolder(new File(this.outputfolder + File.separator + job.getRootfilename()), new File(this.outputfolder));
							
							this.deleteFile(new File(this.outputfolder + File.separator + publishJob.getFilename())); // Then delete the original zip
							this.deleteFile(new File(tempFolderPath)); // Delete temp folder
						}
						// Unzip and rename contents.
						//this.unZip(publishJob.getFilename(), outputfolder, );
					}else if(job.getZipoutput() != null && job.getZipoutput().equals("yes")) {
						// TODO unzip and rename file
						//*
						log("Manage zip to zip");
						if(publishJob.getFilename().contains(".")) {

							String slices[] = publishJob.getFilename().split("\\.");
							String newFileName = slices[0];
							String tempFolderPath = this.outputfolder + File.separator + job.getRootfilename() + "_temp";
							if(publishJob.getPublishRequest().getFormat().getFormat().equals("html")) {
									
								newFileName = newFileName + ".html";
								
								this.unZip(publishJob.getFilename(), tempFolderPath, job.getRootfilename());
								
								// Path to unzipped folder
								String pathToFolder = this.outputfolder + File.separator + job.getRootfilename();
								log("PathToFolder: " + pathToFolder);

								List<String> fileList = new ArrayList<String>(); 

								this.generateFileList(fileList, new File(tempFolderPath), tempFolderPath);

								this.zipIt(fileList, this.outputfolder + File.separator + publishJob.getFilename(), tempFolderPath);
								this.deleteFile(new File(tempFolderPath));
							}
							
							if(publishJob.getPublishRequest().getFormat().getFormat().equals("xml")) {

								newFileName = newFileName + ".xml";
								// Unzip to folder and set root file name 
								this.unZip(publishJob.getFilename(), tempFolderPath, job.getRootfilename());

								// Path to unzipped folder
								String pathToFolder = this.outputfolder + File.separator + newFileName;
								log("PathToFolder: " + pathToFolder);

								List<String> fileList = new ArrayList<String>(); 

								this.generateFileList(fileList, new File(tempFolderPath), tempFolderPath);

								this.zipIt(fileList, this.outputfolder + File.separator + publishJob.getFilename(), tempFolderPath);
								this.deleteFile(new File(tempFolderPath));
							}
						}

						//*/
					}
				}
			}	
		}	
	}
	
	/*
	 * 
	 */
	private void moveUnzippedResult(File source, File destination)
	{
		log("Move files from folder");
		
		try {
			FileUtils.copyDirectory(source, destination);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Deletes a file
	 * @param String file to delete
	 */
	private void deleteFile(File file)
	{
		try {
			log("Deleted file " + file.getName());
			FileDeleteStrategy.FORCE.delete(file);
			
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void renameFile(File oldFile, File newFile)
	{
		try {
			// Make a copy
			FileUtils.copyFile(oldFile, newFile, true);
			// Delete original
			this.deleteFile(oldFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
     * Zip it
     * @param zipFile output ZIP file location
     * Modified from http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
     */
    private void zipIt(List<String> fileList, String zipFile, String sourceFolder){
 
     byte[] buffer = new byte[1024];
 
     try{
 
    	FileOutputStream fos = new FileOutputStream(zipFile);
    	ZipOutputStream zos = new ZipOutputStream(fos);
 
    	log("Output to Zip : " + zipFile);
 
    	for(String file : fileList){
 
    		log("File Added : " + file);
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
 
    	System.out.println("Done");
    }catch(IOException ex){
       ex.printStackTrace();   
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
    		log("Add to filelist: " + node.getAbsolutePath());
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
	private void unZip(String zipFile, String outputFolder, String newFileName){
		// Unzip and rename contents.
		log("Unzip to " + outputFolder);
		
		byte[] buffer = new byte[1024];

		try {

			//create output directory is not exists
			File folder = new File(outputFolder);
			
			if(!folder.exists()) {
				log("Create output dir " + folder.getPath());
				folder.mkdir();
			}
			if(!folder.isDirectory()) {
				folder.mkdir();
			}

			//get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(this.outputfolder + File.separator + zipFile));
			//get the zipped file entry list
			ZipEntry ze = zis.getNextEntry();

			while(ze != null) {

				String fileName = ze.getName();
				// File in zip
				File fileToUnzip = new File(folder.getPath() + File.separator + fileName);
				File renamedFile = new File(folder.getPath() + File.separator + newFileName);

				log("file unzip : " + fileToUnzip.getAbsoluteFile());
					
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
					log("Rename " + fileToUnzip.getName());
					//this.forceRename(fileToUnzip, renamedFile);
					this.renameFile(fileToUnzip, renamedFile);
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

			log("Done");

		}catch(IOException ex){
			ex.printStackTrace(); 
		}
	}    
}
