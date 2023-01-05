package se.simonsoft.cms.publish.rest.cdn;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.algolia.search.models.apikeys.SecuredApiKeyRestriction;
import com.algolia.search.models.indexing.Query;
import com.algolia.search.util.HmacShaUtils;

public class PublishCdnSearchApiKeyGeneratorAlgolia {
	
private PublishCdnConfigSearch cdnConfig;

private Logger logger = LoggerFactory.getLogger(PublishCdnConfigSearch.class); 
	
	
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
	
	public String getSearchApiKeyDocument(String cdn, String docno) {
		if (this.cdnConfig == null) {
			return null;
		}
		logger.debug("Generating Algolia search key for '{}': {}", cdn, docno);
		
		// Get the global search key.
		String searchKey = this.cdnConfig.getSearchApiKeySearch(cdn);
		
		// Consider caching the SearchClient if expensive to instantiate.
		//SearchClient client = DefaultSearchClient.create("YourApplicationID", "YourWriteAPIKey");
		
		SecuredApiKeyRestriction restriction = new SecuredApiKeyRestriction()
				.setQuery(new Query().setFilters("docno:" + docno));
		// Wildcard should work in secured key as well:
		// https://discourse.algolia.com/t/create-secured-api-key-supports-wildcards-in-restrictindices/11999
		restriction.setRestrictIndices(Arrays.asList("cdn_" + cdn + "_v1*"));
		
		try {
			//String securedKey = client.generateSecuredAPIKey("SearchKey", restriction);
			String securedKey = HmacShaUtils.generateSecuredApiKey(searchKey, restriction);
			logger.debug("Generated Algolia search key for '{}': {}", cdn, securedKey);
			return securedKey;
		} catch (Exception e) {
			throw new RuntimeException("Could not generate Algolia search key.", e);
		}
	}

}