package se.simonsoft.cms.publish.rest;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import se.simonsoft.cms.export.aws.CmsExportProviderAwsSingle;
import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.export.*;
import se.simonsoft.cms.item.workflow.WorkflowExecution;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatus;
import se.simonsoft.cms.item.workflow.WorkflowExecutionStatusInput;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfig;
import se.simonsoft.cms.publish.config.databinds.config.PublishConfigOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobStorage;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

public class PublishPackageStatus {

    private final WorkflowExecutionStatus executionsStatus;
    private final PublishJobStorageFactory storageFactory;

    private static final Logger logger = LoggerFactory.getLogger(PublishPackageStatus.class);

    @Inject
    public PublishPackageStatus(
            @Named("config:se.simonsoft.cms.aws.workflow.publish.executions") WorkflowExecutionStatus executionStatus,
            PublishJobStorageFactory storageFactory
    ) {
        this.executionsStatus = executionStatus;
        this.storageFactory = storageFactory;
    }

    public Set<WorkflowExecution> getStatus(PublishPackage publishPackage) {

        String publication = publishPackage.getPublication();
        PublishConfig publishConfig = publishPackage.getPublishConfig();
        PublishConfigOptions jobOptions = publishConfig.getOptions();
        PublishJobStorage storage = (PublishJobStorage) jobOptions.getStorage();
        Set<CmsItem> publishedItems = publishPackage.getPublishedItems();
        Set<WorkflowExecution> releaseExecutions = new HashSet<WorkflowExecution>();

        CmsItemId itemId = publishPackage.getReleaseItemId();
        Set<WorkflowExecution> executions = executionsStatus.getWorkflowExecutions(itemId, true);

        if (storage != null) {
            String type = storage.getType();
            if (type != null && !type.equals("s3")) {
                String msg = MessageFormatter.format("Field 'publication': publication name '{}' can not be exported (configured for non-default storage).", publication).getMessage();
                throw new IllegalStateException(msg);
            }
        }

        // FIXME: We currently ignore the RUNNING_STALE executions. It remains to be seen if this needs to be handled properly.
        executions.removeIf(execution -> execution.getStatus() == "RUNNING_STALE");

        // Filter out the executions not related to the current publication
        executions.removeIf(execution -> ((WorkflowExecutionStatusInput) execution.getInput()).getConfigname() != publication);

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
                execution = getUnknownWorkflowExecution(storage, itemId, publication);
                if (execution != null) {
                    releaseExecutions.add(execution);
                }
            }
        }

        return releaseExecutions;
    }

    WorkflowExecution getUnknownWorkflowExecution(PublishJobStorage storage, CmsItemId itemId, String publication) {

        WorkflowExecution execution = null;

        if (storage != null && storage.getType().equals("s3")) {
            String cloudId = storage.getPathcloudid();
            String bucketName = storage.getParams().get("s3bucket");
            AwsCredentialsProvider credentials = DefaultCredentialsProvider.create();
            Region region = DefaultAwsRegionProviderChain.builder().build().getRegion();
            CmsExportPrefix exportPrefix = new CmsExportPrefix(storage.getPathversion());
            CmsExportProviderAwsSingle exportProvider = new CmsExportProviderAwsSingle(exportPrefix, cloudId, bucketName, region, credentials);
            CmsExportReader reader = exportProvider.getReader();
            CmsImportJob importJob = PublishExportJobFactory.getImportJobSingle(storage, "zip");
            WorkflowExecutionStatusInput input = new WorkflowExecutionStatusInput(itemId.getLogicalIdFull(), null, publication);
            try {
                reader.prepare(importJob);
                // TODO: id must be set
                execution = new WorkflowExecution(null, "SUCCEEDED", null, null, input);
            } catch (CmsExportJobNotFoundException | CmsExportAccessDeniedException e) {
                logger.debug("Unknown publication {} did not exist on S3 at {}.", importJob.getJobName(), importJob.getJobPath());
                // TODO: id must be set
                execution = new WorkflowExecution(null, "UNKNOWN", null, null, input);
            }
        }

        return execution;
    }
}
