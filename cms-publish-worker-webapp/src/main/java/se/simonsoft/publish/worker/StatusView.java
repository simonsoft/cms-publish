package se.simonsoft.publish.worker;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("status")
public class StatusView {
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getStatus() {
		
		
		return "Temporary text";
		
	}
}
