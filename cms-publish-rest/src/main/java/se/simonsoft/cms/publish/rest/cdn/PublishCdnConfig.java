package se.simonsoft.cms.publish.rest.cdn;

import java.security.PrivateKey;

public interface PublishCdnConfig {

	String getHostname(String cdn);
	
	String getPrivateKeyId(String cdn);
	
	PrivateKey getPrivateKey(String cdn);
	
}
