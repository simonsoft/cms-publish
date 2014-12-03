package se.simonsoft.cms.publish.ant.filters;

import java.util.List;

import org.apache.tools.ant.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.RepoRevision;

public class MultipleLangPublishingFilter implements FilterPublishProperties {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private CmsItem item;
	private RepoRevision headRev;
	private Project project;
	private String publishTarget;
	
	public MultipleLangPublishingFilter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initFilter(CmsItem item, RepoRevision headRev,
			Project project, String publishTarget) {
		this.item = item;
		this.headRev = headRev;
		this.project = project;
		this.publishTarget = publishTarget;

	}

	@Override
	public void runFilter() {
		// TODO Auto-generated method stub
		this.publishItem(this.determineOutputPath());
		
	}
	
	private String determineOutputPath() 
	{	
		String outputpath = this.project.getProperty("outputfolder");
		String masterlang = this.project.getProperty("masterlang");
		logger.debug("outputpath {} masterlang {}", outputpath, masterlang);
		
		//${outputfolder}/${filename}/translations/${lang}
		if(!masterlang.equals(item.getProperties().getString("abx:lang"))) {
			logger.debug("Is translation");
			outputpath.concat("/" + this.item.getId().getRelPath().getNameBase());
			outputpath.concat("/translations/" + item.getProperties().getString("abx:lang"));
		} 
		
		return outputpath;
	}
	
	private void publishItem(String outputPath) 
	{
		this.project.setProperty("param.file",
				item.getId().withPegRev(this.headRev.getNumber()).toString());

		this.project.setProperty("filename",
				item.getId().getRelPath().getNameBase());

		this.project.setProperty("lang", item.getProperties().getString("abx:lang"));
		
		this.project.setProperty("outputpath", outputPath);
		
		// A test:
		this.project.getProperties().put("CMSITEM", this.item);
		
		// RepoRevision itemRepoRev = item.getRevisionChanged();
		logger.debug("file: {} filename: {} lang {} ", item.getId().withPegRev(this.headRev.getNumber()).toString(), item.getId().getRelPath().getNameBase(),item.getProperties().getString("abx:lang"));

		this.project.executeTarget(this.publishTarget);
	}

}
