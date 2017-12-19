package se.simonsoft.cms.publish.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemId;

@Path("/publish4")
public class PublishResource {
	
	private static final Logger logger = LoggerFactory.getLogger(PublishResource.class);
	
	@GET
	@Path("release")
	@Produces(MediaType.TEXT_HTML)
	public String getReleaseForm(@QueryParam("item") CmsItemId itemId) throws Exception {
		
		if (itemId == null) {
			throw new IllegalArgumentException("Field 'item': required");
		}
		
		return "Not implemented";
	}
	
	@GET
	@Path("release/download")
	@Produces(MediaType.TEXT_HTML)
	public Response getDownload(@QueryParam("item") CmsItemId itemId,
						@QueryParam("includemaster") boolean includeMaster,
						@QueryParam("includetranslations") boolean includeTranslations,
						@QueryParam("profiling") String[] profiling,
						@QueryParam("publication") String publication) throws Exception {
		
		logger.debug("Not yet implemented");
		
		return Response.ok("Succesfully called get release/download").build();
	}
	
	
}
