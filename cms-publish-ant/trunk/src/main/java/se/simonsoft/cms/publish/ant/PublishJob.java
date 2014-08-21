/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
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
package se.simonsoft.cms.publish.ant;

import se.simonsoft.cms.publish.PublishRequest;
import se.simonsoft.cms.publish.PublishTicket;
/*
 * A single PublishJob
 */
public class PublishJob {
	private PublishTicket ticket;
	private PublishRequest publishRequest;
	private String filename;
	private boolean completed = false;
	private int numberOfTries = 2;
	
	
	public PublishJob(PublishTicket ticket, PublishRequest publishRequest, String filename)
	{
		this.ticket = ticket;
		this.publishRequest = publishRequest;
		this.filename = filename;
	}
	
	public PublishJob(PublishRequest publishRequest, String filename)
	{
		this.publishRequest = publishRequest;
		this.filename = filename;
	}
	
	/**
	 * @return the numberOfTries
	 */
	public int getNumberOfTries() {
		return numberOfTries;
	}

	/**
	 * @param numberOfTries the numberOfTries to set
	 */
	public void setNumberOfTries(int numberOfTries) {
		this.numberOfTries = numberOfTries;
	}
	
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the completed
	 */
	public boolean isCompleted() {
		return completed;
	}

	/**
	 * @param completed the completed to set
	 */
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
	/**
	 * @return the ticket
	 */
	public PublishTicket getTicket() {
		return ticket;
	}
	/**
	 * @param ticket the ticket to set
	 */
	public void setTicket(PublishTicket ticket) {
		this.ticket = ticket;
	}
	/**
	 * @return the publishRequest
	 */
	public PublishRequest getPublishRequest() {
		return publishRequest;
	}
	/**
	 * @param publishRequest the publishRequest to set
	 */
	public void setPublishRequest(PublishRequest publishRequest) {
		this.publishRequest = publishRequest;
	}
}
