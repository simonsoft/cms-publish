package se.simonsoft.cms.publish.rest.cdn;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.info.CmsItemLookup;

@Path("/cdn4")
public class PublishCdnResource {
	private final Map<CmsRepository, CmsItemLookup> lookupMap;
	private PublishCdnConfig cdnConfig;
	// Consider making an interface after API stabilization.
	private PublishCdnUrlSignerCloudFront cdnUrlSigner;
	
	
	@Inject
	public PublishCdnResource(Map<CmsRepository, CmsItemLookup> lookupMap, PublishCdnConfig cdnConfig) {
		this.lookupMap = lookupMap;
		this.cdnConfig = cdnConfig;
		this.cdnUrlSigner = new PublishCdnUrlSignerCloudFront(this.cdnConfig);
	}
	
	
	@GET
	@Path("execution/url")
	@Produces("application/json")
	public Response getUrl(@QueryParam("uuid") String uuid) {
		
		// TODO: Get information from index, type = publish
		String cdn = "preview";
		String path = "/en-GB/SimonsoftCMS-User-manual/latest/WhatsNewIn-D2810D06.html";
		Instant expires = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		
		// TODO: Verify access control.
		// Get the repository from indexing query.
		
		// Sign the path (unless cdn = public)
		String url;
		if ("public".equals(cdn)) {
			url = cdnUrlSigner.getUrlDocument(cdn, path);
		} else {
			url = cdnUrlSigner.getUrlDocumentSigned(cdn, path, expires);
		}
		
		Set<String> result = new LinkedHashSet<String>(3);
		result.add(url);
		
		// Requiring GenericEntity for Iterable<?>.
		GenericEntity<Iterable<String>> ge = new GenericEntity<Iterable<String>>(result) {};
		Response response = Response.ok(ge)
				.header("Vary", "Accept")
				.build();
		return response;
	}
	
	
}
