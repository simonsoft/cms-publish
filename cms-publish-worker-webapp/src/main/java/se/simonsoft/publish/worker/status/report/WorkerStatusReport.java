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
package se.simonsoft.publish.worker.status.report;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkerStatusReport {

	private List<WorkerEvent> events = new ArrayList<WorkerEvent>();
	public static final int MAX_LENGTH = 100;
	private WorkerEvent workerLoop = new WorkerEvent();

	public List<WorkerEvent> getWorkerEvents(){
		return this.events;
	}

	public void addWorkerEvent(WorkerEvent event) {
		events.add(event);
		if(events.size() > MAX_LENGTH) {
			events.remove(1);
		}
	}
	
	public WorkerEvent getLastWorkerLoop() {
		return this.workerLoop;
	}
	
	public void setLastWorkerLoop(WorkerEvent event) {
		workerLoop = event;
	}

	public static class WorkerEvent {
		private Date timeStamp;
		private String action;
		private String description;


		public WorkerEvent(String action, Date timeStamp, String description) {
			this.action = action;
			this.timeStamp = timeStamp;
			this.description = description;
		}
		
		public WorkerEvent() {
			
		}

		public String getTimeStamp() {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			return df.format(timeStamp);
		}

		public void setTimeStamp(Date timeStamp) {
			this.timeStamp = timeStamp;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
