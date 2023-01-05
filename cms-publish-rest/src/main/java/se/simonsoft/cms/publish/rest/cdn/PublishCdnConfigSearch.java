package se.simonsoft.cms.publish.rest.cdn;

/**
 * Tentative interface for search provider API key configuration.
 *
 */
public interface PublishCdnConfigSearch {

	String getSearchAppId(String cdn);
	String getSearchApiKeySearch(String cdn);
}
