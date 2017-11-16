package se.simonsoft.cms.publish.config.filter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.simonsoft.cms.item.CmsItem;
import se.simonsoft.cms.item.config.CmsResourceContext;
import se.simonsoft.cms.item.impl.CmsItemIdArg;
import se.simonsoft.cms.item.info.CmsRepositoryLookup;
import se.simonsoft.cms.item.workflow.WorkflowExecutor;
import se.simonsoft.cms.item.workflow.WorkflowItemInput;
import se.simonsoft.cms.publish.databinds.publish.config.PublishConfig;
import se.simonsoft.cms.publish.rest.PublishItemChangedEventListener;

public class PublishItemChangedEventListenerTest {

	private ObjectMapper mapper = new ObjectMapper();
	
	@Test
	public void testDefaultItemEvent() throws Exception {
		
		CmsItem mockItem = mock(CmsItem.class);
		when(mockItem.getId()).thenReturn(new CmsItemIdArg(""));
		
		CmsRepositoryLookup mockLookup = mock(CmsRepositoryLookup.class);
		when(mockLookup.getConfig(mockItem.getId(), mockItem.getKind()));
		WorkflowExecutor<WorkflowItemInput> mockWorkflowExec = mock(WorkflowExecutor.class);
		
		List<PublishConfigFilter> filters = new ArrayList<PublishConfigFilter>();
		filters.add(new PublishConfigFilterActive());
		filters.add(new PublishConfigFilterType());
		filters.add(new PublishConfigFilterStatus());
		
		
		PublishItemChangedEventListener eventListener = new PublishItemChangedEventListener(mockLookup,
																mockWorkflowExec,
																filters,
																mapper.writer(),
																mapper.reader());
		eventListener.onItemChange(mockItem);
		
		verify(mockLookup, times(1)).getConfig(mockItem.getId(), mockItem.getKind());
		verify(mockWorkflowExec, atLeast(1)).startExecution(any(WorkflowItemInput.class));
		
	}
	
	private PublishConfig getPublishConfigStatus() throws FileNotFoundException, IOException {
		String jsonPath = "se/simonsoft/cms/publish/config/filter/publish-config-status.json";
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(jsonPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
		StringBuilder out = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			out.append(line);
		}
		br.close();
		return mapper.reader().forType(CmsResourceContext.class).readValue(out.toString());
	}
}
