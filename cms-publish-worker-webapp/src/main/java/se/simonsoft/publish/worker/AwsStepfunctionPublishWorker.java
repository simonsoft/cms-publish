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
package se.simonsoft.publish.worker;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AbortedException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import se.simonsoft.cms.item.command.CommandRuntimeException;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobProgress;
import se.simonsoft.cms.publish.export.PublishJobExportService;

@Singleton
public class AwsStepfunctionPublishWorker {

	private final AWSStepFunctions client;
	private final String activityArn;
	private final ExecutorService awsClientExecutor;
	private final PublishJobService publishJobService;
	private final PublishJobExportService exportService;

	private String startUpTime;
	private ObjectReader reader;
	private ObjectWriter writer;

	private static final Logger logger = LoggerFactory.getLogger(AwsStepfunctionPublishWorker.class);

	@Inject
	public AwsStepfunctionPublishWorker(ObjectReader reader,
			ObjectWriter writer,
			AWSStepFunctions client,
			String activityArn,
			PublishJobService publishJobService,
			PublishJobExportService exportService) {

		this.reader = reader.forType(PublishJobOptions.class);
		this.writer = writer;
		this.client = client;
		this.activityArn = activityArn; 
		this.awsClientExecutor = Executors.newSingleThreadExecutor();
		this.publishJobService = publishJobService;
		this.exportService = exportService;

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		this.startUpTime = df.format(new Date());

		startListen();
	}

	private void startListen() {
		awsClientExecutor.execute(new Runnable() {

			@Override
			public void run() {

				while(true) {
					String exportPath = null;
					PublishTicket publishTicket = null;
					PublishJobProgress progress = null;
					String progressAsJson = null;
					String taskToken = null;

					GetActivityTaskResult taskResult = null;
					try {
						logger.debug("Client getting activity task");
						taskResult = client.getActivityTask(new GetActivityTaskRequest().withActivityArn(activityArn).withWorkerName(startUpTime));
					} catch (AbortedException e) {
						logger.error("Client aborted getActivtyTask, start up time: {}", startUpTime);
					}

					if (hasTaskToken(taskResult)) {
						taskToken = taskResult.getTaskToken();
						try {
							logger.debug("Got a task from workflow. {}", taskResult.getInput());
							PublishJobOptions options = deserializeInputToOptions(taskResult.getInput());

							if (hasTicket(options)) {
								logger.debug("Job has a ticket, checking if it is ready for export.");
								publishTicket = new PublishTicket(options.getProgress().getParams().get("ticket"));

								boolean jobCompleted = false;
								jobCompleted = isJobCompleted(publishTicket);
								progress = getJobProgress(publishTicket, jobCompleted);
								progressAsJson = getProgressAsJson(progress);

								if (jobCompleted) { 
									logger.debug("Job is completed, starting export...");
									exportPath = exportCompletedJob(publishTicket, options);
									sendTaskResult(taskToken, progressAsJson);
									logger.debug("Job is exported to: {}", exportPath);
								} else {
									logger.debug("Job is not completed send result JobPending");
									sendTaskResult(taskToken, "", new CommandRuntimeException("JobPending"));
								}

							} else {
								logger.debug("Job has no ticket, requesting publish.");
								publishTicket = requestPublish(taskToken, options);
								progressAsJson = getProgressAsJson(getJobProgress(publishTicket, false));
								sendTaskResult(taskToken, progressAsJson);
							}
						} catch (IOException | InterruptedException | PublishException e) {
							sendTaskResult(taskToken, e.getMessage(), new CommandRuntimeException("JobFailed"));
						}
					} else {
						try {
							logger.debug("Did not get a response. Will continue to listen...");
							Thread.sleep(1000); //From aws example code, will keep it even if the client will long poll.
						} catch (InterruptedException e) {
							logger.error("Could not sleep thread", e.getMessage());
						}
					}
				}
			}
		});
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
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			publishJobService.getCompletedJob(ticket, baos);
			String jobPath = exportService.exportJob(baos, options);
		return jobPath;
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
			logger.error("Could not deserialize options recivied.", e);
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
}
