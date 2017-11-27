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

public class PublishExportS3Service implements PublishJobExportService {
	
	private final CmsExportWriter writer;
	private final String jobExtension = "zip";
	
	
	private static final Logger logger = LoggerFactory.getLogger(PublishExportS3Service.class);
	
	@Inject //TODO: Inject or instantiate writer. Can not remember what we agreed upon.
	public PublishExportS3Service(CmsExportAwsWriterSingle writer) { 
		this.writer = writer;
	}

	@Override
	public String exportJob(OutputStream os, PublishJobOptions jobOptions) {
		logger.debug("Preparing publishJob {} for export to s3", jobOptions.getPathname());
		
		PublishExportJob job = new PublishExportJob(jobOptions.getStorage(), this.jobExtension);
		
		CmsExportItemInputStream exportItem = new CmsExportItemInputStream(new ByteArrayInputStream(os.toString().getBytes()), new CmsExportPath(jobOptions.getPathname())); //TODO Is the item path correct this way?
		job.addExportItem(exportItem);
		
		logger.debug("Prepareing writer for export...");
		writer.prepare(job);
		if (writer.isReady()) {
			logger.debug("Writer is prepared. Wrting job to S3.");
			writer.write();
		}
		
		logger.debug("Job has been exported to S3.");
		return job.getJobPath();
	}

}
