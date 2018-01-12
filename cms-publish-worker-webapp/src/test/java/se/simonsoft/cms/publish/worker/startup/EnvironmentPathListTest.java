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
package se.simonsoft.cms.publish.worker.startup;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mockito;

public class EnvironmentPathListTest {

	@Test
	public void testGetPathFirst() {
		
		Environment env = Mockito.mock(Environment.class);
		EnvironmentPathList epl = new EnvironmentPathList(env);
		
		Mockito.when(env.getParamOptional("APTAPPLICATION")).thenReturn("C:\\temp");
		assertEquals("C:/temp", epl.getPathFirst("APTAPPLICATION"));
		
		Mockito.when(env.getParamOptional("APTAPPLICATION")).thenReturn("C:\\temp;C:\\bogus");
		assertEquals("C:/temp", epl.getPathFirst("APTAPPLICATION"));
		
		Mockito.when(env.getParamOptional("APTAPPLICATION")).thenReturn(null);
		assertNull("C:/temp", epl.getPathFirst("APTAPPLICATION"));
		
		Mockito.when(env.getParamOptional("APTAPPLICATION")).thenReturn(" ");
		assertNull(epl.getPathFirst("APTAPPLICATION"));
		
		Mockito.when(env.getParamOptional("APTAPPLICATION")).thenReturn(";");
		assertNull(epl.getPathFirst("APTAPPLICATION"));
	}

}
