/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.publish.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.export.*;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.databinds.profiling.PublishProfilingRecipe;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.config.item.CmsItemPublish;

public class PublishPackageStatus {

    private final WorkflowExecutionStatus executionsStatus;
    private final PublishJobStorageFactory storageFactory;
    private final CmsExportProvider exportProvider;

    private static final Logger logger = LoggerFactory.getLogger(PublishPackageStatus.class);

    @Inject
    public PublishPackageStatus(
            @Named("config:se.simonsoft.cms.aws.workflow.publish.executions") WorkflowExecutionStatus executionStatus,
            @Named("config:se.simonsoft.cms.publish.export") CmsExportProvider exportProvider,
            PublishJobStorageFactory storageFactory
    ) {
        this.executionsStatus = executionStatus;
        this.exportProvider = exportProvider;
        this.storageFactory = storageFactory;
    }

    
	/**
     * Filter the jobs to be started, prevents restarting RUNNING and SUCCEEDED.
     * 
     * Note: Important to filter SUCCEEDED by default because inactive config can often have mix of SUCCEEDED and INACTIVE (e.g. recently changed status).
     * 
	 * @param publishPackage
	 * @param jobsAll set of jobs to filter
	 * @param allowSucceeded advanced mode allowing restart of SUCCEEDED
	 * @return
	 */
	public Set<PublishJob> getJobsStartAllowed(PublishPackage publishPackage, Set<PublishJob> jobsAll, boolean allowSucceeded) {

		// Important to filter SUCCEEDED by default, see Javadoc.
		final List<String> allowed = new ArrayList<>(Arrays.asList("ABORTED", "FAILED", "UNKNOWN", "INACTIVE"));
		if (allowSucceeded) {
			allowed.add("SUCCEEDED");
		}
		Set<PublishJob> jobs = new LinkedHashSet<>(jobsAll);

		// Must avoid starting multiple executions for the same Job.
		// Verify with the status service.
		// Inactive configs have a special situation, will often be reported as UNKNOWN.
		Set<WorkflowExecution> status = getStatus(publishPackage);
		// There should only be one for each itemId (single profiling in each call)
		Map<CmsItemId, WorkflowExecution> statusMap = new HashMap<>(status.size());
		// Build map: CmsItemId -> status
		status.forEach(wf -> statusMap.put(wf.getInput().getItemId(), wf));
		status.forEach(wf -> logger.debug("Start publish, current status: {} - {}", wf.getStatus(), wf.getInput().getItemId()));
		// Suppress start for items that does not have an allowed execution status.
		jobs.removeIf(job -> !allowed.contains(statusMap.get(job.getItemId()).getStatus()));

		return jobs;
	}

    public Set<WorkflowExecution> getStatus(PublishPackage publishPackage) {

    	if (publishPackage.getProfilingSet() != null && publishPackage.getProfilingSet().size() > 1) {
    		throw new IllegalArgumentException("publish status can be requested for a single profiling recipe");
    	}
    	
    	// Request executions with refresh once.
    	executionsStatus.getWorkflowExecutions(publishPackage.getPublishedItems().iterator().next().getId(), true);
        
    	Set<WorkflowExecution> releaseExecutions = new LinkedHashSet<WorkflowExecution>();
    	for (CmsItem item: publishPackage.getPublishedItems()) {
    		// No refresh on subsequent calls.
    		Set<WorkflowExecution> executions = executionsStatus.getWorkflowExecutions(item.getId(), false);
    		releaseExecutions.add(getStatus(publishPackage, item, executions));
    	}
        return releaseExecutions;
    }
    
    
    public WorkflowExecution getStatus(PublishPackage publishPackage, CmsItem item) {

    	if (publishPackage.getProfilingSet() != null && publishPackage.getProfilingSet().size() > 1) {
    		throw new IllegalArgumentException("publish status can be requested for a single profiling recipe");
    	}
    	
    	Set<WorkflowExecution> executions = executionsStatus.getWorkflowExecutions(item.getId(), true);
    	return getStatus(publishPackage, item, executions);
    }
    
    private WorkflowExecution getStatus(PublishPackage publishPackage, CmsItem item, Set<WorkflowExecution> executions) {
    	
        String publication = publishPackage.getPublication();
        // Empty profiling set is not possible, disallowed by PublishPackage.
        final PublishProfilingRecipe profiling = publishPackage.getProfilingSet() == null ? null : publishPackage.getProfilingSet().iterator().next();
        PublishConfig publishConfig = publishPackage.getPublishConfig();

        logger.trace("Found {} workflow executions for item: {}", executions.size(), item.getId());
        
        // FIXME: We currently ignore the RUNNING_STALE executions. It remains to be seen if this needs to be handled properly.
        // #1525 The state RUNNING_STALE is probably going away.
        executions.removeIf(execution -> execution.getStatus().equals("RUNNING_STALE"));
        // Filter out the executions not related to the current publication
        executions.removeIf(execution -> !((PublishJob) execution.getInput()).getConfigname().equals(publication));
        
        logger.trace("Found {} workflow executions '{}' for item: {}", executions.size(), publication, item.getId());
        // Filter out profiling name.
        if (publishPackage.getProfilingSet() != null && publishPackage.getProfilingSet().size() > 0) {
        	logger.debug("Filtering workflow executions (profiling '{}' for item: {}", profiling.getName(), item.getId());
        	executions.removeIf(execution -> !(((PublishJob) execution.getInput()).getOptions().getProfiling() != null && ((PublishJob) execution.getInput()).getOptions().getProfiling().getName().equals(profiling.getName())));
        }
        logger.trace("Found {} relevant workflow executions for item: {}", executions.size(), item.getId());
        
        // Can there be multiple at this point? Only if manual start has accidentally started another?
        // - Aborted and then restarted.
        // - Restarted using the advanced param.
        WorkflowExecution execution = null;
        if (!executions.isEmpty()) {
        	execution = executions.iterator().next();
        }
        if (executions.size() > 1) {
        	logger.debug("Found {} workflow executions (selecting latest started) for item: {}", executions.size(), item.getId());
        	// TODO: Figure out how to handle.
        	// 5.0.3 and below: For now, we prefer "SUCCEEDED" to prevent starting more executions. Likely not ideal if we want to support re-publish of recent publications.
        	// Prefer the latest based on start time.
        	Iterator<WorkflowExecution> iterator = executions.iterator();
            while (iterator.hasNext()) {
            	WorkflowExecution executionNext = iterator.next();
                if (execution.getStartDate() == null || (executionNext.getStartDate() != null && executionNext.getStartDate().isAfter(execution.getStartDate()))) {
                	execution = executionNext;
                }
            }
        }

        if (execution == null) {
        	PublishJobStorage storage = storageFactory.getInstance(publishConfig.getOptions().getStorage(), new CmsItemPublish(item), publication, profiling);
        	// Special handling of inactive config, return INACTIVE instead of UNKNOWN.
            String fallbackStatus = "UNKNOWN";
            if (!publishConfig.isActive()) {
            	fallbackStatus = "INACTIVE";
            }
        	execution = getUnknownWorkflowExecution(storage, item.getId(), publication, fallbackStatus);
        }
        return execution;
    }

    WorkflowExecution getUnknownWorkflowExecution(PublishJobStorage storage, CmsItemId itemId, String publication, String fallbackStatus) {

        if (storage == null || !storage.getType().equals("s3")) {
            String msg = MessageFormatter.format("Publication '{}' is not configured with S3 storage.", publication).getMessage();
            // TODO: Log or exception?
            throw new IllegalArgumentException(msg);
        }

        WorkflowExecution execution = null;
        CmsExportReader reader = exportProvider.getReader();
        CmsImportJob importJob = PublishExportJobFactory.getImportJobSingle(storage, "zip");
        PublishStatusItemInput input = new PublishStatusItemInput(itemId.getLogicalIdFull(), null);
        try {
            reader.prepare(importJob);
            execution = new WorkflowExecution(null, "SUCCEEDED", null, null, input);
        } catch (CmsExportJobNotFoundException | CmsExportAccessDeniedException e) {
            logger.debug("Unknown publication {} did not exist on S3 at {}.", importJob.getJobName(), importJob.getJobPath());
            execution = new WorkflowExecution(null, fallbackStatus, null, null, input);
        }

        return execution;
    }
}
