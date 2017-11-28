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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.RuntimeErrorException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AbortedException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.sf.saxon.value.StringValue;
import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobProgress;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobStorage;
import se.simonsoft.cms.publish.export.PublishExportS3Service;
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

					GetActivityTaskResult taskResult = null;
					try {
						taskResult = client.getActivityTask(new GetActivityTaskRequest().withActivityArn(activityArn).withWorkerName(startUpTime));
					} catch (AbortedException e) {
						logger.error("Client aborted getActivtyTask, start up time: {}", startUpTime);
					}

					if (taskResult != null && taskResult.getTaskToken() != null) {
						logger.debug("Got a task from workflow. {}", taskResult.getInput());
						PublishJobOptions options = deserializeInputToOptions(taskResult.getInput());

						if (hasTicket(options)) {
							logger.debug("Job has a ticket, checking if it is ready for export.");
							publishTicket = new PublishTicket(options.getProgress().getParams().get("ticket"));
							boolean jobCompleted = isJobCompleted(publishTicket);
							if (jobCompleted) {
								logger.debug("Job is completed, starting export...");
								exportPath = exportCompletedJob(publishTicket, options);
								progress = getJobProgress(publishTicket, jobCompleted);								
								sendTaskSuccessRequest(taskResult.getTaskToken(), progressAsJson);
								logger.debug("Job is exported to: {}", exportPath);
							} else {
								progress = getJobProgress(publishTicket, jobCompleted);
								progressAsJson = getProgressAsJson(progress);
								sendTaskSuccessRequest(taskResult.getTaskToken(), progressAsJson); //Should this be a named exception.
							}
						} else {
							logger.debug("Job has no ticket, requesting publish.");
							publishTicket = requestPublish(options);
							progressAsJson = getProgressAsJson(getJobProgress(publishTicket, false));
							sendTaskSuccessRequest(taskResult.getTaskToken(), progressAsJson);
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
	
	private PublishTicket requestPublish(PublishJobOptions options) {
		PublishTicket ticket = null;
		try {
			ticket = publishJobService.publishJob(options);
		} catch (InterruptedException | PublishException e) {
			logger.debug("Failed when requested job with PublishJobOptions: {}", Jackson.toJsonPrettyString(options));
			throw new RuntimeException(e);
		}
		return ticket;
	}
	
	private String exportCompletedJob(PublishTicket ticket, PublishJobOptions options) {
		String jobPath = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			publishJobService.getCompletedJob(ticket, baos);
			jobPath = exportService.exportJob(baos, options);
		} catch (PublishException | IOException e) {
			throw new RuntimeException(e); //TODO: better messages.
		}
		
		return jobPath;
	}
	
	private boolean hasTicket(PublishJobOptions options) {
		logger.debug("Checking if options has a ticket");
		return options.getProgress().getParams().containsKey("ticket");
	}
	
	private PublishJobOptions deserializeInputToOptions(String input) {
//		JsonNode jsonOptions = Jackson.jsonNodeOf(input).get("options"); //TODO: do we get more then options?
		PublishJobOptions options = null;
		try {
			options = reader.readValue(input);
		} catch (IOException e) {
			logger.error("Could not deserialize options recivied.", e);
			throw new IllegalArgumentException(e.getMessage());
		}

		return options;
	}
	
	private boolean isJobCompleted(PublishTicket ticket) {
		boolean completed;
		try {
			completed = publishJobService.isCompleted(ticket);
		} catch (PublishException e) {
			throw new RuntimeException(e);
		}
		return completed;
	}
	
	private void sendTaskSuccessRequest(String taskToken, String progressResultJson) {
		SendTaskSuccessRequest sendTaskSuccessRequest = new SendTaskSuccessRequest().withTaskToken(taskToken).withOutput(progressResultJson);
		client.sendTaskSuccess(sendTaskSuccessRequest);
	}
	
	private PublishJobProgress getJobProgress(PublishTicket ticket, boolean isCompleted) {
		PublishJobProgress progress = new PublishJobProgress();
		progress.getParams().put("ticket", ticket.toString());
		progress.getParams().put("completed", String.valueOf(isCompleted));
		return progress;
	}
	
	private String getProgressAsJson(PublishJobProgress progress) {
		ObjectWriter progressWriter = writer.forType(PublishJobProgress.class);
		String progressJson = null;
		try {
			progressJson = progressWriter.writeValueAsString(progress);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return progressJson;
	}
}
