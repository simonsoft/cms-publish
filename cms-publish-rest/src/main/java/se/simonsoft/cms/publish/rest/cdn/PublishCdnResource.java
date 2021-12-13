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
package se.simonsoft.cms.publish.rest.cdn;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
	public Response getUrl(@QueryParam("uuid") String id) {
		
		UUID uuid = getUuid(id);
		
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
	
	
	static UUID getUuid(String id) {
		String uuid = id;
		if (id.contains(":")) {
			uuid = id.substring(1 + id.lastIndexOf(':'));
		}
		return UUID.fromString(uuid);
	}
	
}
