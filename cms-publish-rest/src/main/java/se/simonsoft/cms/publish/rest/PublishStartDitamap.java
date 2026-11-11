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
package se.simonsoft.cms.publish.rest;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * CMS-2094: Helpers for a ditamap supplied when starting a publish (start option 'startditamap').
 *
 * The supplied ditamap is passed to Preprocess in the preprocess params (key {@link #PARAM_NAME}).
 * The manifest records the SHA1 of the ditamap (job.startditamap) and the number of topicrefs (job.topics).
 */
public class PublishStartDitamap {

	/**
	 * The key in options.preprocess.params and in manifest.job.
	 */
	public static final String PARAM_NAME = "startditamap";

	private PublishStartDitamap() {
		// Static helpers only.
	}

	/**
	 * @param ditamap the supplied ditamap string
	 * @return SHA1 hex (lowercase) of the UTF-8 bytes of the ditamap string
	 */
	public static String getSha1Hex(String ditamap) {
		if (ditamap == null) {
			throw new IllegalArgumentException("The startditamap must not be null.");
		}
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(ditamap.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	/**
	 * Count the topicrefs in the supplied ditamap that reference DITA topics.
	 * Excludes external references, resource-only references (e.g. keydef), submaps and non-DITA references.
	 *
	 * @param ditamap the supplied ditamap string
	 * @return the number of topic references
	 * @throws IllegalArgumentException if the ditamap can not be parsed
	 */
	public static long countTopics(String ditamap) {
		Document doc = parse(ditamap);
		NodeList elements = doc.getDocumentElement().getElementsByTagName("*");
		long count = 0;
		for (int i = 0; i < elements.getLength(); i++) {
			if (isTopicref((Element) elements.item(i))) {
				count++;
			}
		}
		return count;
	}

	static boolean isTopicref(Element e) {
		String href = e.getAttribute("href");
		if (href.isEmpty()) {
			return false;
		}
		if ("external".equals(e.getAttribute("scope"))) {
			return false;
		}
		if ("resource-only".equals(e.getAttribute("processing-role"))) {
			return false;
		}
		String format = e.getAttribute("format");
		if (!format.isEmpty()) {
			return "dita".equals(format);
		}
		String path = href;
		int hash = path.indexOf('#');
		if (hash >= 0) {
			path = path.substring(0, hash);
		}
		int query = path.indexOf('?');
		if (query >= 0) {
			path = path.substring(0, query);
		}
		path = path.toLowerCase(Locale.ROOT);
		return path.endsWith(".dita") || path.endsWith(".xml");
	}

	private static Document parse(String ditamap) {
		if (ditamap == null || ditamap.isBlank()) {
			throw new IllegalArgumentException("The startditamap must not be empty.");
		}
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(false);
			dbf.setExpandEntityReferences(false);
			dbf.setXIncludeAware(false);
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			// Allow a DOCTYPE declaration but never load the DTD or external entities.
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new InputSource(new StringReader(ditamap)));
		} catch (Exception e) {
			throw new IllegalArgumentException("The startditamap is not well-formed XML: " + e.getMessage(), e);
		}
	}
}
