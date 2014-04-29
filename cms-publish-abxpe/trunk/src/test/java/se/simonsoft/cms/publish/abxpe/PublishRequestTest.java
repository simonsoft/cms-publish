package se.simonsoft.cms.publish.abxpe;

import static org.junit.Assert.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.junit.Test;

public class PublishRequestTest {

	private String publishHost = "pds-suse-svn3.pdsvision.net";
	private String publishPath = "/e3/servlet/e3";
	
	
	@Test
	public void transactionResult(){
		// ?f=qt-retrieve&id=2681
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		try {
			// Create the uri
			URI uri = new URIBuilder()
	        .setScheme("http")
	        .setHost(this.publishHost)
	        .setPath(this.publishPath)
	        .setParameter("f", "qt-retrieve")
	        .setParameter("id", "2713") 
	        .build();
			
			
			HttpGet httpget = new HttpGet(uri);
			System.out.println(httpget.getURI());
			CloseableHttpResponse response = httpclient.execute(httpget);
			System.out.println("StatusCode: " + response.getStatusLine().getStatusCode());
			// Assert that we get a OK response
			assertEquals(200, response.getStatusLine().getStatusCode());
			
			HttpEntity entity = response.getEntity();
		    if (entity != null) {
		    	
		    	String contentType = entity.getContentType().toString().substring(14);
		    	System.out.println("Content Type: " + contentType);
		    	// Assert that we recieve a PDF
		    	// TODO Refactor test, and add more in depth tests perhaps.
		    	assertEquals("application/pdf", contentType);
		    	// Might do some tests on the content
		        InputStream instream = entity.getContent();
		    	
		        try {
		            // do something useful
		        } finally {
		            instream.close();
		        }
		    }
			response.close();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void transactionQueue() {
		
		// http://abxpe6.pdsvision.net:8080/e3/servlet/e3?file=x-svn:///svn/flir^/thg-swe/td/td-xml/thg-swe/td-spec/03198-000.xml
		// &type=pdf
		// &f=convert
		// &stylesheet=$aptpath/application/se.simonsoft.flir/doctypes/FLIR/prod_catalog_flir.style
		// &app-config=$aptpath/application/se.simonsoft.flir/app/standard.3sppdf
		// &profile=%3CLogicalExpression%3E%3CProfileRef%20alias%3D%22Features%22%20value%3D%22FeatureX%3E%3C%2FLogicalExpression%3E
		// &queue=yes
		// &response-format=xml
		// &zip-output=yes&zip-root=03198-000_en.xml
		
		
		String url = "http://pds-suse-svn3.pdsvision.net/e3/servlet/e3?file=x-svn:///svn/flir^/thg-swe/td/td-xml/thg-swe/td-spec/03198-000.xml&type=pdf&f=convert&stylesheet=$aptpath/application/se.simonsoft.flir/doctypes/FLIR/prod_catalog_flir.style&app-config=$aptpath/application/se.simonsoft.flir/app/standard.3sppdf&profile=%3CLogicalExpression%3E%3CProfileRef%20alias%3D%22Features%22%20value%3D%22FeatureX%3E%3C%2FLogicalExpression%3E&queue=yes&response-format=xml";
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new URL(url).openStream());
			Node node = doc.getElementsByTagName("Transaction").item(0);
			Element element = (Element)node;
			System.out.println("trasnactionID: " + element.getAttribute("id"));
			
			// Assert that we recieve a transactionID. 
			// TODO Refactor test 
			assertNotNull("Transaction id should not be null", element.getAttribute("id"));
			
			
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (ParserConfigurationException e) {

			e.printStackTrace();
		} catch (SAXException e) {

			e.printStackTrace();
		}
		
	}

}
