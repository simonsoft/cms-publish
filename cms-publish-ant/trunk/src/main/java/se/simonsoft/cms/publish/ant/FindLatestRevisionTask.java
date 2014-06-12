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
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.getProject().setProperty("previousrevision", revision);
	}
	
	private String readRevisionFile() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader("latestrev.txt"));
	
	    try {
	        StringBuilder sb = new StringBuilder();
	        String firstline = br.readLine();
	        return firstline;
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
	        br.close();
	    }
		return "";
	    
	}
}
