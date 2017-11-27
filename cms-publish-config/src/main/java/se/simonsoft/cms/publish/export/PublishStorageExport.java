package se.simonsoft.cms.publish.export;


import java.io.OutputStream;

import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;

public interface PublishStorageExport {
	
	String exportJob(OutputStream os, PublishJobOptions jobOptions);
	
}
