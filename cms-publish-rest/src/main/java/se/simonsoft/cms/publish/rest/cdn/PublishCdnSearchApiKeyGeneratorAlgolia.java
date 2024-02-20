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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.algolia.search.models.apikeys.SecuredApiKeyRestriction;
import com.algolia.search.models.indexing.Query;
import com.algolia.search.util.HmacShaUtils;

import se.simonsoft.cms.item.encoding.CmsItemURLEncoder;
import se.simonsoft.cms.publish.config.cdn.PublishCdnConfig;
import se.simonsoft.cms.publish.config.cdn.PublishCdnConfigSearch;

public class PublishCdnSearchApiKeyGeneratorAlgolia {
	
private PublishCdnConfigSearch cdnConfig;

private static final Logger logger = LoggerFactory.getLogger(PublishCdnConfigSearch.class); 
private static final CmsItemURLEncoder encoder = new CmsItemURLEncoder(); // There might be a few too many safe chars.
	
	
	public PublishCdnSearchApiKeyGeneratorAlgolia(PublishCdnConfig cdnConfig) {
		if (cdnConfig instanceof PublishCdnConfigSearch) {
			this.cdnConfig = (PublishCdnConfigSearch) cdnConfig;
		} else {
			this.cdnConfig = null;
		}
	}
	
	public String getSearchAppId(String cdn) {
		if (this.cdnConfig == null) {
			return null;
		}
		return this.cdnConfig.getSearchAppId(cdn);
	}
	
	/* Opted to call HmacShaUtils directly, avoiding the creation of an Algolia client instance.
	 * 
	 * Documented API for generating the secured key: 
	 * SearchClient client = DefaultSearchClient.create("YourApplicationID", "YourWriteAPIKey");
	 * String securedKey = client.generateSecuredAPIKey("SearchKey", restriction);
	 */
	
	
	public String getSearchApiKeyDocument(String cdn, String docno) {
		if (this.cdnConfig == null) {
			return null;
		}
		logger.debug("Generating Algolia search key for '{}': {}", cdn, docno);
		
		// Get the global search key.
		String searchKey = this.cdnConfig.getSearchApiKeySearch(cdn);
		
		SecuredApiKeyRestriction restriction = new SecuredApiKeyRestriction()
				.setQuery(new Query().setFilters("docno:" + encoder.encode(docno)));
		// Wildcard should work in secured key as well:
		// https://discourse.algolia.com/t/create-secured-api-key-supports-wildcards-in-restrictindices/11999
		restriction.setRestrictIndices(Arrays.asList("cdn_" + cdn + "_v1*"));
		
		try {
			String securedKey = HmacShaUtils.generateSecuredApiKey(searchKey, restriction);
			logger.debug("Generated Algolia search key for '{}': {}", cdn, securedKey);
			return securedKey;
		} catch (Exception e) {
			throw new RuntimeException("Could not generate Algolia search key.", e);
		}
	}
	
	
	public String getSearchApiKey(String cdn, Integer visibility) {
		if (this.cdnConfig == null) {
			return null;
		}
		logger.debug("Generating Algolia search key for '{}': visibility>{}", cdn, visibility);
		
		// Get the global search key.
		String searchKey = this.cdnConfig.getSearchApiKeySearch(cdn);
		
		SecuredApiKeyRestriction restriction = new SecuredApiKeyRestriction();
		if (visibility != null) {
			restriction.setQuery(new Query().setNumericFilters(Arrays.asList(Arrays.asList("visibility>" + visibility.toString()))));
		}
		// Wildcard should work in secured key as well:
		// https://discourse.algolia.com/t/create-secured-api-key-supports-wildcards-in-restrictindices/11999
		restriction.setRestrictIndices(Arrays.asList("cdn_" + cdn + "_v1*"));
		
		try {
			String securedKey = HmacShaUtils.generateSecuredApiKey(searchKey, restriction);
			logger.debug("Generated Algolia search key for '{}': {}", cdn, securedKey);
			return securedKey;
		} catch (Exception e) {
			throw new RuntimeException("Could not generate Algolia search key.", e);
		}
	}

}
