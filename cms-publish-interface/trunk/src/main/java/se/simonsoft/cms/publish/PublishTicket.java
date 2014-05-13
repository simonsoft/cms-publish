/*******************************************************************************
 * Copyright 2014 Simonsoft Nordic AB
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package se.simonsoft.cms.publish;



/**
 * Represents a ticket for a queued publish operation.
 * The ticket must always be possible to represent as a String, potentially JSON.
 * The purpose of this class is simply to make stronger typed APIs. 
 * @author takesson
 *
 */
public class PublishTicket {

	private String id = null;
	
	public PublishTicket(String id) {
		
		this.id = id;
	}
	
	public String toString() {
		
		return id;
	}
	
	public boolean equals(Object obj) {
		
		return id.equals(obj);
	}
	
	public int hashCode() {
		
		return id.hashCode();
	}
 	
}
