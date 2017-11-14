package se.simonsoft.publish.worker;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import se.simonsoft.cms.publish.PublishException;
import se.simonsoft.cms.publish.PublishTicket;
import se.simonsoft.cms.publish.abxpe.PublishServicePe;
import se.simonsoft.cms.publish.impl.PublishRequestDefault;

public class TestPageTest {
	
	@Mock
	private PublishServicePe pe = Mockito.mock(PublishServicePe.class);
	private TestPage resource = new TestPage(pe);

	@Before
	public void setUp() throws Exception {
		
	}
	
	@Test
	public void testValidUrlCall() throws InterruptedException, IOException, PublishException {
		PublishTicket ticket = new PublishTicket("44");
		Mockito.when(pe.requestPublish(Mockito.any(PublishRequestDefault.class))).thenReturn(ticket);
		Mockito.when(pe.isCompleted(Mockito.any(PublishTicket.class), Mockito.any(PublishRequestDefault.class))).thenReturn(true);
		String urlCall = resource.urlCall("C:\\Program Files\\PTC\\Arbortext PE\\e3\\e3\\e3demo.xml", "html");
		assertEquals("PE is done! Your ticket number is: 44", urlCall);
	}

	@Test
	public void testParameterException() throws InterruptedException, IOException, PublishException {
		try {
			resource.urlCall("", "");
			fail("Should throw IllegalArgumentException");
		}catch ( IllegalArgumentException e ) {
			assertNotNull(e);
			e.printStackTrace();
		}
		
		try {
			resource.getResult("");
			fail("Should throw IllegalArgumentException");
		}catch ( IllegalArgumentException e ) {
			assertNotNull(e);
			e.printStackTrace();
		}
	}
}
