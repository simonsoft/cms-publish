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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
import se.simonsoft.cms.item.info.CmsItemLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutionId;

@Path("/cdn4")
public class PublishCdnResource {
	private final Map<CmsRepository, CmsItemLookup> lookupMap;
	private final SolrClient solrClient;
	private PublishCdnConfig cdnConfig;
	// Consider making an interface after API stabilization.
	private PublishCdnUrlSignerCloudFront cdnUrlSigner;
	
	private Logger logger = LoggerFactory.getLogger(PublishCdnResource.class);
	
	@Inject
	public PublishCdnResource(Map<CmsRepository, CmsItemLookup> lookupMap, @Named("repositem") SolrClient solrClient, PublishCdnConfig cdnConfig) {
		this.lookupMap = lookupMap;
		this.solrClient = solrClient;
		this.cdnConfig = cdnConfig;
		this.cdnUrlSigner = new PublishCdnUrlSignerCloudFront(this.cdnConfig);
	}
	
	
	@GET
	@Path("execution/url")
	@Produces("application/json")
	public Response getUrl(@QueryParam("uuid") String id) {
		
		PublishCdnItem p = getCdnPublish(id);
		
		CmsRepository repository = p.getItemId().getRepository();
		
		// Get information from index, type = publish-cdn
		String cdn = p.getCdn();
		String path = p.getPathformat();
		Instant expires = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
		
		// Verify access control.
		// Get the repository from indexing query.
		CmsItemLookup lookup = lookupMap.get(repository);
		// Throws exception is user does not have access.
		lookup.getItem(p.getItemId());
		
		// Sign the path, if needed.
		Set<String> result = new LinkedHashSet<String>(3);
		try {
			String url = cdnUrlSigner.getUrlDocumentSigned(cdn, path, expires);
			result.add(url);
		} catch (Exception e) {
			logger.error("Publish CDN failed to sign CDN url.", e);
			throw new IllegalStateException("Failed to provide URL on Delivery.");
		}
		
		// Requiring GenericEntity for Iterable<?>.
		GenericEntity<Iterable<String>> ge = new GenericEntity<Iterable<String>>(result) {};
		Response response = Response.ok(ge)
				.header("Vary", "Accept")
				.build();
		return response;
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
		query.setFields("repo", "repoparent", "repohost", "rev", "embd_publish-cdn_path", "embd_publish-cdn_uuid", "embd_publish-cdn_status", "embd_publish-cdn_custom_cdn", "embd_publish-cdn_progress_pathformat", "text_error");
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
		
		if (!m.containsKey("embd_publish-cdn_progress_pathformat")) {
			throw new RuntimeException("Publish CDN found no CDN document path in index: " + id);
		}
		
		CmsRepository repository = new CmsRepository("https", m.get("repohost"), m.get("repoparent"), m.get("repo"));
		CmsItemPath path = new CmsItemPath(m.get("embd_publish-cdn_path"));
		// Ignoring revision for now.
		
		PublishCdnItem p = new PublishCdnItem();
		p.setItemId(repository.getItemId(path, null));
		if (m.containsKey("embd_publish-cdn_custom_cdn")) {
			p.setCdn(m.get(m.get("embd_publish-cdn_custom_cdn")));
		} else {
			p.setCdn("preview");
		}
		// Existence verified above.
		p.setPathformat(m.get("embd_publish-cdn_progress_pathformat"));
		
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
