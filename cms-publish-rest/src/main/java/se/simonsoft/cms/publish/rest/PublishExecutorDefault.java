package se.simonsoft.cms.publish.rest;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.workflow.WorkflowExecutionException;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.config.PublishExecutor;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;

public class PublishExecutorDefault implements PublishExecutor {
	
	private final WorkflowExecutor<WorkflowItemInput> workflowExecutor;
	
	private static final Logger logger = LoggerFactory.getLogger(PublishExecutorDefault.class);

	@Inject
	public PublishExecutorDefault(@Named("config:se.simonsoft.cms.aws.publish.workflow") WorkflowExecutor<WorkflowItemInput> workflowExecutor) {
		this.workflowExecutor = workflowExecutor;
	}

	@Override
	public Set<String> startPublishJobs(Set<PublishJob> jobs) {
		logger.debug("Starting executions for {} number of PublishJobs", jobs.size());
		Set<String> result = new LinkedHashSet<>(jobs.size());
		for (PublishJob job: jobs) {
			String id = startPublishJob(job);
			result.add(id);
		}
		return result;
	}
	
	public String startPublishJob(PublishJob job) {
		try {
			String id = workflowExecutor.startExecution(job);
			return id;
		} catch (WorkflowExecutionException e) {
			logger.error("Failed to start execution for itemId '{}': {}", job.getItemId(), e.getMessage());
			throw new RuntimeException("Publish execution failed: " + e.getMessage(), e);
		}
	}

}
