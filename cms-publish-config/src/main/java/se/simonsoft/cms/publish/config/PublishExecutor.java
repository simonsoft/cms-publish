package se.simonsoft.cms.publish.config;

import java.util.Set;

import se.simonsoft.cms.publish.config.databinds.job.PublishJob;

public interface PublishExecutor {

	Set<String> startPublishJobs(Set<PublishJob> jobs); 
}
