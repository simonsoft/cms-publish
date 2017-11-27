package se.simonsoft.cms.publish.export;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.export.CmsExportItemInputStream;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;

public class PublishStorageExportS3 implements PublishStorageExport {
	
	private final CmsExportWriter writer;
	private final String jobExtension = "zip";
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishStorageExportS3.class);
	
	@Inject //TODO: Inject or instantiate writer. Can not remember what we agreed upon.
	public PublishStorageExportS3(CmsExportAwsWriterSingle writer) { 
		this.writer = writer;
	}

	@Override
	public String exportJob(OutputStream os, PublishJobOptions jobOptions) {
		logger.debug("Preparing {} for export to s3", jobOptions.getPathname());
		
		PublishExportJob job = new PublishExportJob(jobOptions.getStorage(), this.jobExtension);
		
		CmsExportItemInputStream exportItem = new CmsExportItemInputStream(new ByteArrayInputStream(os.toString().getBytes()), new CmsExportPath(jobOptions.getPathname())); //TODO Is the item path correct this way?
		job.addExportItem(exportItem);
		
		writer.prepare(job);
		if (writer.isReady()) {
			writer.write();
		}
		
		return job.getJobPath();
	}

}
