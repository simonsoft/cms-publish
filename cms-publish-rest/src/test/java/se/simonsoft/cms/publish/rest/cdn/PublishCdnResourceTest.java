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
