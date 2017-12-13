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
package se.simonsoft.publish.worker;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import se.simonsoft.publish.worker.status.report.WorkerStatusReport;
import se.simonsoft.publish.worker.status.report.WorkerStatusReport.WorkerEvent;

@Path("status")
public class StatusView {
	
	private WorkerStatusReport statusReport;
	
	@Inject
	public void getWorkerStatusReport(WorkerStatusReport statusReport) {
		this.statusReport = statusReport;
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getStatus() throws Exception {
		VelocityEngine engine = new VelocityEngine();
		Properties p = new Properties();
		p.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		p.setProperty("runtime.references.strict", "true");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		engine.init(p);
		
		List<WorkerEvent> workerEvents = statusReport.getWorkerEvents();
		WorkerEvent workerLoop = statusReport.getLastWorkerLoop();
		
		VelocityContext context = new VelocityContext();
		context.put("workerEvents", workerEvents);
		context.put("workerLoop", workerLoop);
		
		Template template = engine.getTemplate("se/simonsoft/publish/worker/templates/StatusTemplate.vm");
		
		StringWriter wr = new StringWriter();
		template.merge(context, wr);
		return wr.toString();
	}
}
