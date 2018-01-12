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
