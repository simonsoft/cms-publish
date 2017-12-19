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
package se.simonsoft.cms.publish.worker;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AbortedException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.export.storage.CmsExportAwsWriterSingle;
import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.export.CmsExportPath;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.export.PublishExportJob;
import se.simonsoft.cms.publish.worker.export.CmsExportItemPublishJob;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport.WorkerEvent;

@Singleton
public class AwsStepfunctionPublishWorker {

	private final AWSStepFunctions client;
	private final String activityArn;
	private final ExecutorService awsClientExecutor;
	private final PublishJobService publishJobService;
	private final WorkerStatusReport workerStatusReport;
	private final String jobExtension = "zip";
	
	private CmsExportAwsWriterSingle exportWriter; // Can not be final, protected setMethod to be able to mock it.
	private Date startUpTime;
	private ObjectReader reader;
	private ObjectWriter writer;

	private static final Logger logger = LoggerFactory.getLogger(AwsStepfunctionPublishWorker.class);

	@Inject
	public AwsStepfunctionPublishWorker(@Named("config:se.simonsoft.cms.cloudid") String cloudId,
			@Named("config:se.simonsoft.cms.publish.bucket") String bucketName,
			AWSCredentialsProvider credentials,
			ObjectReader reader,
			ObjectWriter writer,
			AWSStepFunctions client,
			String activityArn,
			PublishJobService publishJobService,
			WorkerStatusReport workerStatusReport
			) {

		this.exportWriter = new CmsExportAwsWriterSingle(cloudId, bucketName, credentials);
		this.reader = reader.forType(PublishJobOptions.class);
		this.writer = writer;
		this.client = client;
		this.activityArn = activityArn; 
		this.awsClientExecutor = Executors.newSingleThreadExecutor();
		this.publishJobService = publishJobService;
		this.workerStatusReport = workerStatusReport;
		this.startUpTime = new Date();

		startListen();
	}

	private void startListen() {
		awsClientExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				updateStatusReport("Worker Startup", new Date(), "AwsStepFunctionPublishWorker is running");
				final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				final String startupTimeFormatted = df.format(startUpTime);
				
				while(true) {
					GetActivityTaskResult taskResult = null;
					
					try {
						logger.debug("Client getting activity task");
						updateWorkerLoop("", new Date(), "AWS worker checking for activity");
						taskResult = client.getActivityTask(new GetActivityTaskRequest().withActivityArn(activityArn).withWorkerName(startupTimeFormatted));
					} catch (AbortedException e) {
						updateWorkerError(new Date(), e);
						logger.error("Client aborted getActivtyTask, start up time: {}", startupTimeFormatted);
					}
					if (hasTaskToken(taskResult)) {
						PublishTicket publishTicket = null;
						String progressAsJson = null;
						final String taskToken = taskResult.getTaskToken();
						logger.debug("tasktoken: {}", taskToken);
						updateStatusReport("Enqueue", new Date(), "ActivityArn: " + activityArn);
						PublishJobOptions options = null;
						
						try {
							logger.debug("Got a task from workflow. {}", taskResult.getInput());
							options = deserializeInputToOptions(taskResult.getInput());

							if (hasTicket(options)) {
								updateStatusReport("Retrieving", new Date(), "ActivityArn: " + activityArn + " Ticket: " + options.getProgress().getParams().get("ticket"));
								progressAsJson = exportJob(options, taskToken);
								sendTaskResult(taskToken, progressAsJson);
							} else {
								updateStatusReport("Enqueue", new Date(), "ActivityArn: " + activityArn);
								logger.debug("Job has no ticket, requesting publish.");
								publishTicket = requestPublish(taskToken, options);
								progressAsJson = getProgressAsJson(getJobProgress(publishTicket, false));
								sendTaskResult(taskToken, progressAsJson);
								updateStatusReport("Enqueued", new Date(), "ActivityArn: "+ activityArn + " Ticket: " + publishTicket.toString());
							}
						} catch (IOException | InterruptedException | PublishException e) {
							updateWorkerError(new Date(), e);
							sendTaskResult(taskToken, e.getMessage(), new CommandRuntimeException("JobFailed"));
						} catch (CommandRuntimeException e) {
							updateStatusReport("Retrieving" ,
									new Date(), 
									e.getMessage() + ", ActivityArn: "+ activityArn + " Ticket: " + options.getProgress().getParams().get("ticket"));
							
							sendTaskResult(taskToken, e.getMessage(), e);
						} catch (Exception e) {
							updateWorkerError(new Date(), e);
							sendTaskResult(taskToken, e.getMessage(), new CommandRuntimeException("JobFailed"));
						}
					} else {
						
						try {
							logger.debug("Did not get a response. Will continue to listen...");
							Thread.sleep(1000); //From aws example code, will keep it even if the client will long poll.
						} catch (InterruptedException e) {
							updateWorkerError(new Date(), e);
							logger.error("Could not sleep thread", e.getMessage());
						}
					}
				}
			}
		});
	}
	
	private String exportJob(PublishJobOptions options, String taskToken) throws PublishException, IOException, CommandRuntimeException {
		logger.debug("Job has a ticket, checking if it is ready for export.");
		
		final PublishTicket publishTicket = new PublishTicket(options.getProgress().getParams().get("ticket"));
		final boolean jobCompleted = isJobCompleted(publishTicket);
		final PublishJobProgress progress = getJobProgress(publishTicket, jobCompleted);
		final String progressAsJson = getProgressAsJson(progress);
		
		String exportPath = null;

		if (jobCompleted) { 
			logger.debug("Job is completed, starting export...");
			exportPath = exportCompletedJob(publishTicket, options);
			logger.debug("Job is exported to: {}", exportPath);
		} else {
			logger.debug("Job is not completed send fail result JobPending");
			throw new CommandRuntimeException("JobPending");
		}
		return progressAsJson;
	}
	
	private boolean hasTaskToken(GetActivityTaskResult taskResult) {
		return taskResult != null && taskResult.getTaskToken() != null && !taskResult.getTaskToken().isEmpty();
	}
	
	private PublishTicket requestPublish(String taskToken, PublishJobOptions options) throws InterruptedException, PublishException {
		PublishTicket ticket = publishJobService.publishJob(options);
		
		logger.debug("JobService returned ticket: {}", ticket.toString());
		return ticket;
	}
	
	private String exportCompletedJob(PublishTicket ticket, PublishJobOptions options) throws IOException, PublishException {
		logger.debug("Preparing publishJob {} for export to s3", options.getPathname());
		
		PublishExportJob job = new PublishExportJob(options.getStorage(), this.jobExtension);
		
		PublishTicket publishTicket = new PublishTicket(options.getProgress().getParams().get("ticket"));
		CmsExportItemPublishJob exportItem = new CmsExportItemPublishJob(publishTicket,
				publishJobService,
				new CmsExportPath("/".concat(options.getStorage().getPathnamebase().concat(".zip"))));
		job.addExportItem(exportItem);
		job.prepare();
		
		logger.debug("Preparing writer for export...");
		exportWriter.prepare(job);
		logger.debug("Writer is prepared. Writing job to S3.");
		exportWriter.write();

		logger.debug("Job has been exported to S3.");
		updateStatusReport("Exporting PublishJob to s3", new Date(), activityArn);
		
		return job.getJobPath();
	}
	
	private boolean hasTicket(PublishJobOptions options) {
		logger.debug("Checking if options has a ticket");
		
		boolean hasTicket = false;
        if (options.getProgress() != null) {
            hasTicket = options.getProgress().getParams().containsKey("ticket");
        }
        return hasTicket;
	}
	
	private PublishJobOptions deserializeInputToOptions(String input) {
		PublishJobOptions options = null;
		try {
			options = reader.readValue(input);
		} catch (IOException e) {

			logger.error("Could not deserialize options recieved.", e);
			throw new IllegalArgumentException(e.getMessage());
		}

		return options;
	}
	
	private boolean isJobCompleted(PublishTicket ticket) throws PublishException {
		return publishJobService.isCompleted(ticket);
	}
	
	private void sendTaskResult(String taskToken, String progressResultJson) {
		SendTaskSuccessRequest sendTaskSuccessRequest = new SendTaskSuccessRequest().withTaskToken(taskToken).withOutput(progressResultJson);
		client.sendTaskSuccess(sendTaskSuccessRequest);
	}
	
	private void sendTaskResult(String taskToken, String readableCause, CommandRuntimeException e) {
		SendTaskFailureRequest failReq = new SendTaskFailureRequest();
		failReq.setTaskToken(taskToken);
		failReq.setCause(readableCause);
		failReq.setError(e.getErrorName());
		client.sendTaskFailure(failReq);
	}
	
	private PublishJobProgress getJobProgress(PublishTicket ticket, boolean isCompleted) {
		PublishJobProgress progress = new PublishJobProgress();
		progress.setParams(new HashMap<String, String>());
		progress.getParams().put("ticket", ticket.toString());
		progress.getParams().put("completed", String.valueOf(isCompleted));
		return progress;
	}
	
	private String getProgressAsJson(PublishJobProgress progress) {
		logger.debug("Serializing progress");
		ObjectWriter progressWriter = writer.forType(PublishJobProgress.class);
		String progressJson = null;
		try {
			progressJson = progressWriter.writeValueAsString(progress);
		} catch (JsonProcessingException e) {
			logger.error("Could not serialize PublishJobProgess");
			throw new RuntimeException(e);
		}
		logger.debug("Progress is serialized {}", progressJson);
		return progressJson;
	}
	
	private void updateStatusReport(String action, Date timeStamp, String description) {
		WorkerEvent event = new WorkerEvent(action, timeStamp, description);
		workerStatusReport.addWorkerEvent(event);
	}
	//Necessary for tests. Tests has to be able to set the writer to a mock instance.
	protected void setExportWriter(CmsExportAwsWriterSingle writer) {
		this.exportWriter = writer;
	}
	
	private void updateWorkerError(Date timeStamp, Exception e) {
		WorkerEvent event = new WorkerEvent("Error", timeStamp, e);
		workerStatusReport.addWorkerEvent(event);
	}
	
	private void updateWorkerLoop(String action, Date timeStamp, String description) {
		WorkerEvent event = new WorkerEvent("", timeStamp, description);
		workerStatusReport.setLastWorkerLoop(event);
	}
}
