package se.simonsoft.cms.publish.rest.cdn;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class PublishCdnResourceTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	public void testGetUuid() {
		assertEquals("0f4e9e27-4bb3-4f7a-859f-d96814632cd0", PublishCdnResource.getUuid("arn:aws:states:eu-west-1:518993259802:execution:cms-demo-dev-publish-v1:0f4e9e27-4bb3-4f7a-859f-d96814632cd0").toString());
		assertEquals("0f4e9e27-4bb3-4f7a-859f-d96814632cd0", PublishCdnResource.getUuid("0f4e9e27-4bb3-4f7a-859f-d96814632cd0").toString());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetUuidTooLong() {
		PublishCdnResource.getUuid("0f4e9e27-4bb3-4f7a-859f-d96814632cd0a");
	}

}
