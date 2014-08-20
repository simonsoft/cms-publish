package se.simonsoft.publish.ant.helper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLoggerHelper {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/*
	 * Write errors to our error log file
	 */
	public void addToErrorLog(String error){
	
		try {
			FileUtils.writeStringToFile(new File("errors.log"), error, "utf-8", true);
		} catch (IOException e) {
			logger.debug("Could not write to error log! " + e.getMessage());
		}
	}
}
