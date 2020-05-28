package se.simonsoft.cms.publish.rest;

import java.util.HashSet;
import java.util.Iterator;
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
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatusInput;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
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

    public Set<WorkflowExecution> getStatus(PublishPackage publishPackage) {

        String publication = publishPackage.getPublication();
        PublishConfig publishConfig = publishPackage.getPublishConfig();
        Set<CmsItem> publishedItems = publishPackage.getPublishedItems();
        Set<WorkflowExecution> releaseExecutions = new HashSet<WorkflowExecution>();

        CmsItemId itemId = publishPackage.getReleaseItemId();
        Set<WorkflowExecution> executions = executionsStatus.getWorkflowExecutions(itemId, true);

        // FIXME: We currently ignore the RUNNING_STALE executions. It remains to be seen if this needs to be handled properly.
        executions.removeIf(execution -> execution.getStatus() == "RUNNING_STALE");
        // Filter out the executions not related to the current publication
        executions.removeIf(execution -> ((WorkflowExecutionStatusInput) execution.getInput()).getConfigname() != publication);
        logger.debug("Found {} relevant workflow executions.", executions.size());

        for (CmsItem item: publishedItems) {
            WorkflowExecution execution = null;
            Iterator<WorkflowExecution> iterator = executions.iterator();
            while (iterator.hasNext()) {
                execution = iterator.next();
                if (execution.getInput().getItemId().getLogicalId().equals(itemId.getLogicalId())) {
                    break;
                }
                execution = null;
            }

            if (execution != null) {
                releaseExecutions.add(execution);
            } else {
                PublishJobStorage storage = storageFactory.getInstance(publishConfig.getOptions().getStorage(), new CmsItemPublish(item), publication, null);
                execution = getUnknownWorkflowExecution(storage, itemId, publication);
                if (execution != null) {
                    releaseExecutions.add(execution);
                }
            }
        }

        return releaseExecutions;
    }

    WorkflowExecution getUnknownWorkflowExecution(PublishJobStorage storage, CmsItemId itemId, String publication) {

        if (storage == null || !storage.getType().equals("s3")) {
            String msg = MessageFormatter.format("Publication '{}' is not configured with S3 storage parameters or any for that matter.", publication).getMessage();
            throw new IllegalArgumentException(msg);
        }

        WorkflowExecution execution = null;
        CmsExportReader reader = exportProvider.getReader();
        CmsImportJob importJob = PublishExportJobFactory.getImportJobSingle(storage, "zip");
        WorkflowExecutionStatusInput input = new WorkflowExecutionStatusInput(itemId.getLogicalIdFull(), null, publication);
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
