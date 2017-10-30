package se.simonsoft.cms.publish.databinds.publish.job.cms.item;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import se.simonsoft.cms.item.Checksum;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishJobItemChecksum extends HashMap<String, String> implements Checksum {

	@Override
	public boolean has(Algorithm algorithm) {
		return this.containsKey(algorithm.toString());
	}
	@Override
	public String getMd5() throws UnsupportedOperationException {
		return getHex(Algorithm.MD5);
	}
	@Override
	public String getSha1() throws UnsupportedOperationException {
		return getHex(Algorithm.SHA1);
	}
	@Override
	public boolean equalsKnown(Checksum obj) {
		for (Algorithm a : Algorithm.values()) {
			if (has(a) && obj.has(a) && getHex(a).equals(obj.getHex(a))) return true;
		}
		return false;
	}
	@Override
	public String getHex(Algorithm algorithm) {
		return this.get(algorithm.toString());
	}
}
