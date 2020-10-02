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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

    
	public Set<PublishJob> getJobsStartAllowed(PublishPackage publishPackage, Set<PublishJob> jobsAll) {

		List<String> allowed = Arrays.asList("FAILED", "UNKNOWN");
		Set<PublishJob> jobs = new LinkedHashSet<>(jobsAll);

		// Must avoid starting multiple executions for the same Job.
		// Verify with the status service.
		// Inactive configs have a special situation, will often be reported as UNKNOWN.
		// TODO: Consider adding support in the status service to report other status, e.g. INACTIVE.
		Set<WorkflowExecution> status = getStatus(publishPackage);
		// There should only be one for each itemId (single profiling in each call)
		Map<CmsItemId, WorkflowExecution> statusMap = new HashMap<>(status.size());
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
        
    	Set<WorkflowExecution> releaseExecutions = new HashSet<WorkflowExecution>();
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
        PublishConfig publishConfig = publishPackage.getPublishConfig();

        logger.debug("Found {} workflow executions for item: {}", executions.size(), item.getId());
        
        // FIXME: We currently ignore the RUNNING_STALE executions. It remains to be seen if this needs to be handled properly.
        executions.removeIf(execution -> execution.getStatus().equals("RUNNING_STALE"));
        // Filter out the executions not related to the current publication
        executions.removeIf(execution -> !((PublishJob) execution.getInput()).getConfigname().equals(publication));
        // Filter out profiling name.
        if (publishPackage.getProfilingSet() != null && publishPackage.getProfilingSet().size() > 0) {
        	executions.removeIf(execution -> !(((PublishJob) execution.getInput()).getOptions().getProfiling() != null && ((PublishJob) execution.getInput()).getOptions().getProfiling().getName().equals(publication)));
        }
        logger.debug("Found {} relevant workflow executions for item: {}", executions.size(), item.getId());
        
        // Can there be multiple at this point? Only if manual start has accidentally started another?
        WorkflowExecution execution = null;
        if (!executions.isEmpty()) {
        	execution = executions.iterator().next();
        }
        if (executions.size() > 1) {
        	logger.warn("Expected 1 workflow, found {} workflow executions for item: {}", executions.size(), item.getId());
        	// TODO: Figure out how to handle.
        	// For now, we prefer "SUCCEEDED" to prevent starting more executions. Likely not ideal if we want to support re-publish of recent publications.
        	Iterator<WorkflowExecution> iterator = executions.iterator();
            while (iterator.hasNext()) {
            	WorkflowExecution executionNext = iterator.next();
                if (!"SUCCEEDED".equals(execution.getStatus()) && "SUCCEEDED".equals(executionNext.getStatus())) {
                	execution = executionNext;
                }
            }
        }

        if (execution == null) {
        	PublishJobStorage storage = storageFactory.getInstance(publishConfig.getOptions().getStorage(), new CmsItemPublish(item), publication, null);
            execution = getUnknownWorkflowExecution(storage, item.getId(), publication);
        }
        // TODO: Consider special handling of inactive configs, e.g. return INACTIVE instead of UNKNOWN.
        return execution;
    }

    WorkflowExecution getUnknownWorkflowExecution(PublishJobStorage storage, CmsItemId itemId, String publication) {

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
            execution = new WorkflowExecution(null, "UNKNOWN", null, null, input);
        }

        return execution;
    }
}
