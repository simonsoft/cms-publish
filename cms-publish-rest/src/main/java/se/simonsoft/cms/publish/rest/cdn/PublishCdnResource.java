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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.info.CmsAuthenticationException;
import se.simonsoft.cms.item.info.CmsCurrentUser;
import se.simonsoft.cms.item.info.CmsItemLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutionId;
import se.simonsoft.cms.publish.config.cdn.PublishCdnConfig;
import se.simonsoft.cms.publish.config.cdn.PublishCdnItem;
import se.simonsoft.cms.publish.config.cdn.PublishCdnUrlSignerCloudFront;

@Path("/cdn4")
public class PublishCdnResource {
	private final Map<CmsRepository, CmsItemLookup> lookupMap;
	private final SolrClient solrClient;
	private PublishCdnConfig cdnConfig;
	// Consider making an interface after API stabilization.
	private PublishCdnUrlSignerCloudFront cdnUrlSigner;
	private final PublishCdnSearchApiKeyGeneratorAlgolia cdnSearchKeyGenerator;
	private CmsCurrentUser currentUser;
	
	private Logger logger = LoggerFactory.getLogger(PublishCdnResource.class);
	
	@Inject
	public PublishCdnResource(Map<CmsRepository, CmsItemLookup> lookupMap, @Named("repositem") SolrClient solrClient, PublishCdnConfig cdnConfig, CmsCurrentUser currentUser) {
		this.lookupMap = lookupMap;
		this.solrClient = solrClient;
		this.cdnConfig = cdnConfig;
		this.cdnUrlSigner = new PublishCdnUrlSignerCloudFront(this.cdnConfig);
		this.cdnSearchKeyGenerator = new PublishCdnSearchApiKeyGeneratorAlgolia(this.cdnConfig);
		this.currentUser = currentUser;
	}
	
	
	@GET
	@Path("auth/{cdn}")
	public Response getAuthRedirect(@PathParam("cdn") String cdn, @QueryParam("returnpath") String returnPath) {
		// Add support for return path. 
		// Potentially risk of infinite redirect if CDN makes no distinction btw 'Not Found' and 'Not Authenticated'.
		// The referrer header must be captured before authentication redirects.
		
		if (cdn == null || cdn.isBlank()) {
			throw new IllegalArgumentException();
		}
		
		Optional<String> path;
		if (returnPath == null || returnPath.isBlank()) {
			logger.info("CDN '{}' auth without 'returnpath'", cdn);
			path = Optional.empty();
		} else if (returnPath.startsWith("/")) { // A query parameter is decoded (once) by REST framework
			// TODO: Properly match valid path with regex, see Baeldung.
			logger.info("CDN '{}' auth with 'returnpath': {}", cdn, returnPath);
			try {
				// Decoding to completely decoded path for the Signer API.
				path = Optional.of(URLDecoder.decode(returnPath, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		} else {
			logger.info("CDN '{}' auth with invalid 'returnpath': {}", cdn, returnPath);
			path = Optional.empty();
		}
		
		
		// Shorter since signature applies to whole CDN.
		// Consider configurable signature duration.
		Instant expires = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		Response response;
		try {
			// Should always sign the root url even if the redirect captures the initially requested document.
			String url = cdnUrlSigner.getUrlSiteSigned(cdn, path, this.currentUser, expires);
			response = Response.status(302)
				.header("Location", url)
				.build();
		} catch (NoSuchElementException e) {
			// CDN does not exist.
			response = Response.status(404).build();
		} catch (CmsAuthenticationException e) {
			response = Response.status(403).build();
		}
		return response;
	}
	
	
	/**
	 * EXPERIMENTAL: Awaiting specification.
	 * @param cdn
	 * @return
	 */
	@GET
	@Path("search/portal/{cdn}")
	public Response getSearchPortal(@PathParam("cdn") String cdn, @QueryParam("visibility") @DefaultValue(value = "200") Integer visibility) {
		if (visibility == null || visibility < 0 || visibility > 999) {
			throw new IllegalArgumentException("parameter visibility is required [0-999]");
		}
		// Setting visibility filter to '0' is equivalent to no filter (since negative values are not supported at this time).
		// Useful when some documents were published with CMS 5.1, before visibility was introduced.
		if (visibility == 0) {
			visibility = null;
		}
		// Generate and log the key before testing access control, useful for administrators.
		String key = this.cdnSearchKeyGenerator.getSearchApiKey(cdn, visibility);
		logger.info("CDN '{}' search key (visibility>{}): {}", cdn, visibility, key);
		
		// Test access control, allowing this method for internal CDN configurations.
		Instant expires = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		@SuppressWarnings("unused")
		String url = cdnUrlSigner.getUrlSiteSigned(cdn, Optional.empty(), this.currentUser, expires);
		
		// TODO: Consider returning an object containing AppId, key, ...
		return Response.ok().entity(key).build();
	}
	
		
	
	@GET
	@Path("execution/url")
	@Produces("application/json")
	public Response getUrl(@QueryParam("uuid") String id) {
		
		PublishCdnItem p = getCdnPublish(id);
		
		CmsRepository repository = p.getItemId().getRepository();
		
		// Get information from index, type = publish-cdn
		String cdn = p.getCdn();
		//String docno = p.getDocno();
		// TODO: Determine when the signature should be based on pathdocument vs docno (Lang-dropdown requires docno for full functionality)
		CmsItemPath pathDocument = new CmsItemPath(p.getPathdocument());
		String path = getPath(p); // TODO: Generalize the path, currently pathformat with addition of pathname.pdf for PDF only.
		Instant expires = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		
		// Verify access control.
		// Get the repository from indexing query.
		CmsItemLookup lookup = lookupMap.get(repository);
		// Throws exception is user does not have access.
		lookup.getItem(p.getItemId());
		
		// Sign the path, if needed.
		Set<String> result = new LinkedHashSet<String>(3);
		try {
			String url = cdnUrlSigner.getUrlDocumentSigned(cdn, pathDocument, path, expires);
			result.add(url);
		} catch (Exception e) {
			logger.error("Publish CDN failed to sign CDN url.", e);
			throw new IllegalStateException("Failed to provide URL on Delivery Service: " + e.getMessage(), e);
		}
		
		// Requiring GenericEntity for Iterable<?>.
		GenericEntity<Iterable<String>> ge = new GenericEntity<Iterable<String>>(result) {};
		Response response = Response.ok(ge)
				.header("Vary", "Accept")
				.build();
		return response;
	}
	
	private String getPath(PublishCdnItem p) {
		
		StringBuilder result = new StringBuilder();
		result.append(p.getPathformat());
		// Need to add additional single-file formats or introduce some field in manifest.job that specifies single/multi.
		// Could also add manifest.document.pathext for single-file only.
		if (p.getFormat().equals("pdf")) {
			result.append(p.getPathname());
			result.append('.');
			result.append("pdf");
		}
		return result.toString();
	}
	
	
	// TODO: Consider separation into interface / impl.
	PublishCdnItem getCdnPublish(String id) {
		
		WorkflowExecutionId executionId = new WorkflowExecutionId(id);
		if (!executionId.hasUuid()) {
			throw new IllegalArgumentException("Publish Cdn requires id ending with UUID: " + id);
		}
		
		QueryResponse response;
		SolrQuery query;
		String queryString;
		SolrDocumentList docs;
		
		queryString = "embd_publish-cdn_job_id:" + executionId.getUuid();
		query = new SolrQuery(queryString);
		// No need to filter on repo since we search based on UUID and follow up with access control.
		//query.addFilterQuery("repo:" + repo);

		//query.setSort("rev", ORDER.desc);
		query.setFields("repo", "repoparent", "repohost", "rev", "embd_publish-cdn_path", "embd_publish-cdn_uuid", "embd_publish-cdn_status", "embd_publish-cdn_job_format", "embd_publish-cdn_document_docno", "embd_publish-cdn_document_pathname", "embd_publish-cdn_custom_cdn", "embd_publish-cdn_progress_pathformat", "embd_publish-cdn_progress_pathdocument", "text_error");
		query.setRows(3);
		
		try {
			response = solrClient.query(query);
		} catch (SolrServerException | IOException ex) {
			throw new RuntimeException("Unable to query SolrServer.", ex);
		}		
		docs = response.getResults();

		if (docs.getNumFound() == 0) {
			logger.warn("Publish Cdn found no SolR docs for id: {}", id);
			throw new IllegalStateException("Publish CDN not known for " + executionId.getUuid());
		}
		
		if (docs.getNumFound() > 1) {
			logger.warn("Publish Cdn found multiple SolR docs for id: {}", id);
		}
		
		LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
		logger.debug("Publish CDN document has {} fields.", docs.get(0).size());
		docs.get(0).forEach(e -> m.put(e.getKey(), e.getValue().toString()));
		docs.get(0).forEach(e -> logger.debug("Publish CDN: {} = {}", e.getKey(), e.getValue()));
		
		if (m.containsKey("text_error")) {
			throw new RuntimeException("Publish CDN error: " + m.get("text_error"));
		}
		
		if (!m.containsKey("embd_publish-cdn_job_format")) {
			throw new RuntimeException("Publish CDN found no CDN job format in index: " + id);
		}
		
		if (!m.containsKey("embd_publish-cdn_document_docno")) {
			throw new RuntimeException("Publish CDN found no CDN document docno in index: " + id);
		}

		if (!m.containsKey("embd_publish-cdn_document_pathname")) {
			throw new RuntimeException("Publish CDN found no CDN document pathname in index: " + id);
		}
		
		if (!m.containsKey("embd_publish-cdn_progress_pathformat")) {
			throw new RuntimeException("Publish CDN found no CDN document pathformat in index: " + id);
		}
		
		if (!m.containsKey("embd_publish-cdn_progress_pathdocument")) {
			throw new RuntimeException("Publish CDN found no CDN document pathdocument in index: " + id);
		}
		
		CmsRepository repository = new CmsRepository("https", m.get("repohost"), m.get("repoparent"), m.get("repo"));
		// Item path in CMS repository.
		CmsItemPath path = new CmsItemPath(m.get("embd_publish-cdn_path"));
		// Ignoring revision for now.
		
		PublishCdnItem p = new PublishCdnItem();
		p.setItemId(repository.getItemId(path, null));
		// No longer required to fall back to "preview".
		if (m.containsKey("embd_publish-cdn_custom_cdn")) {
			p.setCdn(m.get("embd_publish-cdn_custom_cdn"));
		} else {
			p.setCdn("preview");
		}
		// Existence verified above.
		p.setFormat(m.get("embd_publish-cdn_job_format"));
		p.setDocno(m.get("embd_publish-cdn_document_docno"));
		p.setPathname(m.get("embd_publish-cdn_document_pathname"));
		p.setPathformat(m.get("embd_publish-cdn_progress_pathformat"));
		p.setPathdocument(m.get("embd_publish-cdn_progress_pathdocument"));
		
		return p;
	}
	
	
	static UUID getUuid(String id) {
		String uuid = id;
		if (id.contains(":")) {
			uuid = id.substring(1 + id.lastIndexOf(':'));
		}
		return UUID.fromString(uuid);
	}
	
}
