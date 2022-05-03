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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.item.export.CmsExportJobSingle;
import se.simonsoft.cms.item.export.CmsExportProvider;
import se.simonsoft.cms.item.export.CmsExportWriter;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.config.command.PublishManifestExportCommandHandler;
import se.simonsoft.cms.publish.config.databinds.job.PublishJob;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobOptions;
import se.simonsoft.cms.publish.config.databinds.job.PublishJobProgress;
import se.simonsoft.cms.publish.config.export.PublishExportJobFactory;
import se.simonsoft.cms.publish.worker.export.CmsExportItemPublish;
import se.simonsoft.cms.publish.worker.startup.Environment;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport;
import se.simonsoft.cms.publish.worker.status.report.WorkerStatusReport.WorkerEvent;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.*;

@Singleton
public class AwsStepfunctionPublishWorker {

	private final String cloudId;
	private final SfnClient client;
	private final String activityArn;
	private final ExecutorService awsClientExecutor;
	private final PublishJobService publishJobService;
	private final WorkerStatusReport workerStatusReport;
	private final String jobExtension = "zip";
	private final Map<String, CmsExportProvider> exportProviders;

	private Date startUpTime;
	private ObjectReader readerJob;
	private ObjectReader readerOptions;
	private ObjectWriter writer;
	
	// #1553 The max wait time cannot be higher than Step Functions "TimeoutSeconds" for the Task.
	final Long MAX_WAIT = Long.valueOf(new Environment().getParamOptional("PUBLISH_MAX_BUSY", "14400")); // 4*3600L

	private static final Logger logger = LoggerFactory.getLogger(AwsStepfunctionPublishWorker.class);

	@Inject
	public AwsStepfunctionPublishWorker(
			Map<String, CmsExportProvider> exportProviders,
			ObjectReader reader,
			ObjectWriter writer,
			SfnClient client,
			Region region,
			String account,
			String cloudId,
			String activityName,
			PublishJobService publishJobService,
			WorkerStatusReport workerStatusReport
			) {

		this.exportProviders = exportProviders;
		this.readerJob = reader.forType(PublishJob.class);
		this.readerOptions = reader.forType(PublishJobOptions.class);
		this.writer = writer;
		this.client = client;
		this.cloudId = cloudId;
		this.activityArn = "arn:aws:states:" + region.id() + ":" + account + ":activity:cms-" + cloudId + "-" + activityName;
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
				String startMsg = MessageFormatter.format("AwsStepFunctionPublishWorker is running (max busy: {})", MAX_WAIT).getMessage();
				updateStatusReport("Worker Startup", new Date(), startMsg);
				final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				final String startupTimeFormatted = df.format(startUpTime);

				while(true) {
					GetActivityTaskResponse taskResponse = null;
					try {
						updateWorkerLoop("", new Date(), "AWS worker checking for activity");
						GetActivityTaskRequest taskRequest = GetActivityTaskRequest.builder().activityArn(activityArn).workerName(startupTimeFormatted).build();
						taskResponse = client.getActivityTask(taskRequest);
						// Can we get the workflow execution UUID?
						//updateStatusReport("AWS worker task", new Date(), taskResult.getTaskToken());
					} catch (AbortedException e) {
						logger.warn("Client aborted getActivtyTask, start up time: {}", startupTimeFormatted, e);
						updateWorkerError(new Date(), e);
					} catch (Exception e) {
						final int failureRetrySleep = 30;
						updateWorkerError(new Date(), e);
						logger.error("Client failed getActivtyTask: {}", e.getMessage(), e);
						logger.info("Client retry in {} seconds...", failureRetrySleep);
						try {
							Thread.sleep(failureRetrySleep*1000);
						} catch (InterruptedException e1) {
							logger.warn("Failed to sleep: {}", e.getMessage(), e);
						}
					}
					if (hasTaskToken(taskResponse)) {
						PublishTicket publishTicket = null;
						logger.debug("tasktoken: {}", taskResponse.taskToken());
						PublishJobOptions options = null;

						try {
							String taskInput = taskResponse.input();
							logger.debug("Got a task from workflow. {}", taskInput);
							options = deserializeInputToOptions(taskInput);
							
							// TODO: Support 'action' field, likely move implementation of publish to an ExternalCommandHandler.
							if (hasTicket(options)) {
								// The second activity is no longer used, moving towards a single activity.
								if (hasCompleted(options)) {
									logger.debug("Job is completed exporting manifest...");
									PublishManifestExportCommandHandler manifestExport = new PublishManifestExportCommandHandler(exportProviders.get(options.getStorage().getType()), writer);
									manifestExport.handleExternalCommand(new CmsItemIdArg(options.getManifest().getJob().get("itemid")), options);

									updateStatusReport("Completed manifest export", new Date(), "Ticket: " + options.getProgress().getParams().get("ticket"));
									sendTaskResult(taskResponse, getProgressAsJson(options.getProgress()));
								} else {
									// Removed the initial approach with 2 Tasks.
									throw new IllegalStateException("The legacy dual-task workflow is no longer supported.");
								}
							} else {
								// Request publish and wait for completion.
								PublishJobProgress progress = options.getProgress();
								updateStatusReport("Enqueueing", new Date(), getStatusItemDescription(options));
								logger.debug("Job has no ticket, requesting publish.");
								publishTicket = requestPublish(options);
								updateStatusReport("Enqueued", new Date(), "Ticket: " + publishTicket.toString());
								waitForJob(taskResponse, publishTicket);
								updateStatusReport("Retrieving", new Date(), "Ticket: " + publishTicket.toString());

								String exportPath = exportCompletedJob(publishTicket, options);
								progress.getParams().put("ticket", publishTicket.toString());
								progress.getParams().put("completed", "true");
								progress.getParams().put("pathResult", exportPath);
								sendTaskResult(taskResponse, getProgressAsJson(progress));
							}

						} catch (IOException | InterruptedException | PublishException e) {
							updateWorkerError(new Date(), e);
							logger.error("Exception: " + e.getMessage(), e);
							sendTaskResultBestEffort(taskResponse, new CommandRuntimeException("JobFailed", e));
						} catch (CommandRuntimeException e) {
							updateStatusReport("Command failed: " + e.getErrorName(), new Date(), e.getMessage());
							logger.warn("CommandRuntimeException: " + e.getErrorName(), e);
							sendTaskResultBestEffort(taskResponse, e);
						} catch (TaskTimedOutException e) {
							String errorMessage = "AWS Task has timed out while processed by the Worker (heartbeats too sparse or total time exceeded).";
							updateStatusReport("Task timeout", new Date(), errorMessage);
							logger.error(errorMessage, e);
						} catch (Exception e) {
							updateWorkerError(new Date(), e);
							logger.error("Unexpected exception: " + e.getMessage(), e);
							sendTaskResultBestEffort(taskResponse, new CommandRuntimeException("JobFailed", e));
						} catch (Throwable e) {
							updateWorkerError(new Date(), e);
							logger.error("Unexpected error: {}", e.getMessage(), e);
							sendTaskResultBestEffort(taskResponse, new CommandRuntimeException("JobFailed", e));
						}
					} else {

						try {
							logger.debug("No task to process for '{}'. Listening...", cloudId);
							Thread.sleep(1000); //From aws example code, will keep it even if the client will long poll.
						} catch (InterruptedException e) {
							updateWorkerError(new Date(), e);
							logger.error("Thread sleep interrupted: ", e.getMessage());
						}
					}
				}
			}
		});
	}


	private void waitForJob(GetActivityTaskResponse taskResponse, PublishTicket ticket) throws PublishException, IOException, CommandRuntimeException {

		final int interval = 10;
		final long iterations = MAX_WAIT / interval;

		for (int i = 0; i < iterations; i++) {
			try {
				if (isJobCompleted(ticket)) {
					logger.debug("Job is completed: {}", ticket);
					return;
				} else {
					if ((i % 6) == 0) {
						updateStatusReport("Waiting...", new Date(), "Ticket: " + ticket.toString());
					}
					Thread.sleep(interval * 1000);
					sendTaskHeartbeat(taskResponse);
				}
			} catch (PublishException e) {
				throw e;
			} catch (InterruptedException e) {
				logger.error("Thread sleep interrupted: {}", e.getMessage(), e);
				throw new CommandRuntimeException("JobInterrupted");
			} catch (Exception e) {
				throw new RuntimeException("Failed while waiting for job: " + e.getMessage(), e);
			}
		}
		throw new CommandRuntimeException("JobStuck");
	}


	private boolean hasTaskToken(GetActivityTaskResponse taskResponse) {
		return taskResponse != null && taskResponse.taskToken() != null && !taskResponse.taskToken().isEmpty();
	}

	private PublishTicket requestPublish(PublishJobOptions options) throws InterruptedException, PublishException {
		PublishTicket ticket = publishJobService.publishJob(options);

		logger.debug("JobService returned ticket: {}", ticket.toString());
		return ticket;
	}

	private String exportCompletedJob(PublishTicket ticket, PublishJobOptions options) throws IOException, PublishException {
		logger.debug("Preparing publishJob {} for export to {}", options.getPathname(), options.getStorage().getType());

		updateStatusReport("Exporting PublishJob", new Date(), "Ticket: " + ticket.toString() + " - " + getStatusItemDescription(options));

		// The file is already zipped, performing "Single" export.
		CmsExportJobSingle job = PublishExportJobFactory.getExportJobSingle(options.getStorage(), this.jobExtension);

		CmsExportItemPublish exportItem = new CmsExportItemPublish(ticket, options, publishJobService, null);
		job.addExportItem(exportItem);
		job.prepare();

		CmsExportWriter exportWriter = exportProviders.get(options.getStorage().getType()).getWriter();

		logger.debug("Preparing writer for export...");
		exportWriter.prepare(job);
		logger.debug("Writer is prepared. Writing job.");
		exportWriter.write();

		if (exportWriter instanceof CmsExportWriter.LocalFileSystem) {
			String exportPath = ((CmsExportWriter.LocalFileSystem) exportWriter).getExportPath().toString();
			options.getProgress().getParams().put("archive", exportPath);
		}

		logger.debug("Job has been exported.");
		updateStatusReport("Exported PublishJob", new Date(), "Ticket: " + ticket.toString() + " - " + getStatusItemDescription(options));

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

	private boolean hasCompleted(PublishJobOptions options) {
		logger.debug("Checking if options has completed already");

		boolean hasCompleted = false;
        if (options.getProgress() != null && options.getProgress().getParams().containsKey("completed")) {
        	hasCompleted = options.getProgress().getParams().get("completed").equalsIgnoreCase("true");
        }
        return hasCompleted;
	}

	
	private PublishJobOptions deserializeInputToOptions(String input) {
		PublishJobOptions options = null;
		
		// Forward compatibility where input is a PublishJob, like cms-webapp worker.
		try {
			PublishJob job = readerJob.readValue(input);
			if (job.getOptions() != null) {
				return job.getOptions();
			}
		} catch (IOException e) {
			logger.info("Could not deserialize input as job (CMS 5.1+)");
		}
		
		// Backwards compatibility (CMS 4.3 / CMS 5.0)
		try {
			options = readerOptions.readValue(input);
		} catch (IOException e) {
			logger.error("Could not deserialize options / job received.", e);
			logger.debug("Could not deserialize options / job received: {}", input);
			throw new IllegalArgumentException(e.getMessage());
		}
		return options;
	}

	
	private boolean isJobCompleted(PublishTicket ticket) throws PublishException {
		return publishJobService.isCompleted(ticket);
	}


	private void sendTaskHeartbeat(GetActivityTaskResponse taskResponse) {
		logger.debug("Sending heartbeat...");
		SendTaskHeartbeatRequest request = SendTaskHeartbeatRequest.builder()
				.taskToken(taskResponse.taskToken())
				.build();
		client.sendTaskHeartbeat(request);
	}

	private void sendTaskResult(GetActivityTaskResponse taskResponse, String resultJson) {
		SendTaskSuccessRequest request = SendTaskSuccessRequest.builder()
				.taskToken(taskResponse.taskToken())
				.output(resultJson)
				.build();
		SendTaskSuccessResponse response = client.sendTaskSuccess(request);
		logger.debug("Task successfully executed with request id: {}", response.responseMetadata().requestId());
	}


	private void sendTaskResult(GetActivityTaskResponse taskResponse, CommandRuntimeException e) {
		String cause = null;
		if (e.getMessage() != null && !e.getMessage().isEmpty()) {
			cause = e.getMessage();
		} else if (e.getCause() != null) {
			cause = e.getCause().getMessage();
		}
		SendTaskFailureRequest request = SendTaskFailureRequest.builder()
				.taskToken(taskResponse.taskToken())
				.error(e.getErrorName())
				.cause(cause)
				.build();
		client.sendTaskFailure(request);
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

	private void sendTaskResultBestEffort(GetActivityTaskResponse taskResponse, CommandRuntimeException cre) {
		try {
			sendTaskResult(taskResponse, cre);
		} catch (TaskTimedOutException timedOutEx) {
			String errorMessage = "AWS Task has timed out while processed by the Worker (heartbeats too sparse or total time exceeded).";
			updateStatusReport("Task timeout", new Date(), errorMessage);
			logger.error(errorMessage, timedOutEx);
		} catch (Exception e) {
			logger.error("Exception occured when trying to send taskResult", e);
		}
	}

	private void updateStatusReport(String action, Date timeStamp, String description) {
		WorkerEvent event = new WorkerEvent(action, timeStamp, description);
		workerStatusReport.addWorkerEvent(event);
	}

	private void updateWorkerError(Date timeStamp, Throwable e) {
		WorkerEvent event = new WorkerEvent("Error", timeStamp, e);
		workerStatusReport.addWorkerEvent(event);
	}

	private void updateWorkerLoop(String action, Date timeStamp, String description) {
		WorkerEvent event = new WorkerEvent("", timeStamp, description);
		workerStatusReport.setLastWorkerLoop(event);
	}
	
	private static String getStatusItemDescription(PublishJobOptions options) {
		String result = options.getSource();
		if (result == null && options.getStorage() != null) {
			result = options.getStorage().getPathdir() + "/" + options.getStorage().getPathnamebase();
		}
		return result;
	}
}
