package se.simonsoft.publish.worker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkerStatusReport {
	private List<WorkerEvent> events = new ArrayList<WorkerEvent>();

	public List<WorkerEvent> getWorkerEvents(){
		return this.events;
	}

	public void addWorkerEvent(WorkerEvent event) {
		events.add(event);
		//		if(events.size() > 100) {
		//			events.remove(0);
		//		}
	}


	public static class WorkerEvent {
		private String timeStamp;
		private String action;
		private String description;

		public WorkerEvent(String action, String timeStamp, String description) {
			this.action = action;
			this.timeStamp = timeStamp;
			this.description = description;
		}

		public String getTimeStamp() {
			return timeStamp;
		}

		public void setTimeStamp(String timeStamp) {
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
