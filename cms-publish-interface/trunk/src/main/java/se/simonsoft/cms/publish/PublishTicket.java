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
