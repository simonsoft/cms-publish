package se.simonsoft.cms.publish.export;

import se.simonsoft.cms.item.export.CmsExportJob;
import se.simonsoft.cms.item.export.CmsExportPrefix;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobStorage;

public class PublishExportJob extends CmsExportJob {

	public PublishExportJob(PublishJobStorage storage, String jobExtension) {
		super(createJobPrefix(storage), createJobName(storage), jobExtension);
	}
	
	private static CmsExportPrefix createJobPrefix(PublishJobStorage storage) {
		return new CmsExportPrefix(storage.getPathcloudid());
	}

	private static String createJobName(PublishJobStorage storage) {
		StringBuilder sb = new StringBuilder(storage.getPathconfigname());
		sb.append(storage.getPathdir());
		sb.append("/");
		sb.append(storage.getPathnamebase());
		return sb.toString();
	}
}