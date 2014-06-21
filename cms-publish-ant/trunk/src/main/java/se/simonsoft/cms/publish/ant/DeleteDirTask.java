package se.simonsoft.cms.publish.ant;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Task;

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
	 * Deletes a file
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
