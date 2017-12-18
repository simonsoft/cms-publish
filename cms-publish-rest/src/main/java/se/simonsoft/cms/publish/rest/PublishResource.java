package se.simonsoft.cms.publish.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;

@Path("/publish4")
public class PublishResource {
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);
	
	@GET
	@Path("release")
	@Produces(MediaType.TEXT_HTML)
	public String getReleaseForm() throws Exception {
		//TODO: implement.
		return "Not implemented";
	}
	
	@GET
	@Path("release/download")
	@Produces(MediaType.TEXT_HTML)
	public String download(CmsItemId itemId, boolean incRelease, boolean incTranslation, String[] profiling, String publication) throws Exception {
		logger.debug("Not yet implemented");
		return "Succesfully called get release/download";
	}
	
	
}
