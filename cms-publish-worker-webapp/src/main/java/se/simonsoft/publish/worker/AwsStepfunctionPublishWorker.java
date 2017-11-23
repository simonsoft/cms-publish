package se.simonsoft.publish.worker;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AbortedException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.databinds.publish.job.PublishJobOptions;

@Singleton
public class AwsStepfunctionPublishWorker {
	
	private final AWSStepFunctions client;
	private final String activityArn = " arn:aws:states:eu-west-1:148829428743:activity:cms-jandersson-abxpe"; //TODO: should be injected.
	private final ExecutorService awsClientExecutor;
	private final PublishJobService service;

	private String startUpTime;
	private ObjectReader reader;

	private static final Logger logger = LoggerFactory.getLogger(AwsStepfunctionPublishWorker.class);

	@Inject
	public AwsStepfunctionPublishWorker(ObjectReader reader,
			AWSStepFunctions client,
			String activityArn,
			PublishJobService service) {

		this.reader = reader.forType(PublishJobOptions.class);
		this.client = client;
		//		this.activityArn = activityArn; 
		this.awsClientExecutor = Executors.newSingleThreadExecutor();
		this.service = service;

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		this.startUpTime = df.format(new Date());

		startListen();
	}

	private void startListen() {
		awsClientExecutor.execute(new Runnable() {

			@Override
			public void run() {
				while(true) {
					GetActivityTaskResult taskResult = null;
					try {
						taskResult = client.getActivityTask(new GetActivityTaskRequest().withActivityArn(activityArn).withWorkerName(startUpTime));
					} catch (AbortedException e) {
						logger.error("Client aborted getActivtyTask, start up time: {}", startUpTime);
					}
					
					if (taskResult != null && taskResult.getTaskToken() != null) {
						logger.debug("Got a task from workflow. {}", taskResult.getTaskToken());
						
						String input = taskResult.getInput();
						JsonNode jsonOptions = Jackson.jsonNodeOf(input).get("options");
						PublishJobOptions options = deserializeToOptions(jsonOptions);
						
						try {
							service.publishJob(options);
						} catch (InterruptedException | PublishException e) {
							logger.error("Could not start publication with PublishOptions {}", jsonOptions.textValue());
							throw new IllegalArgumentException(e);
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
	
	
	private PublishJobOptions deserializeToOptions(JsonNode jsonOptions) {
		
		PublishJobOptions options = null;
		try {
			options = reader.readValue(jsonOptions);
		} catch (IOException e) {
			logger.error("Could not deserialize options recivied.", e);
			throw new IllegalArgumentException(e.getMessage());
		}
		
		return options;
	}
	
	





}
