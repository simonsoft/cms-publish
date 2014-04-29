package se.simonsoft.cms.publish.abxpe;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import se.repos.restclient.HttpStatusError;
import se.repos.restclient.RestGetClient;
import se.repos.restclient.RestResponse;

public class RestClientPE implements RestGetClient {

	@Override
	public void get(String uri, RestResponse response) throws IOException, HttpStatusError {
		
		HttpURLConnection conn;
		URL url = new URL(uri);
		
		conn = (HttpURLConnection) url.openConnection();
		
	}

}
