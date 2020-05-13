# cms-publish

## cms-publish-worker-webapp

- It is installed near the Arbortext Publishing Engine (PE), typically in the same Tomcat instance.
- It acts as an Activity Worker connecting to AWS Step Functions via the long poll HTTP API and receives tasks requesting PE to publish different formats such as PDF, Web, etc. This ensures that all connections are made from the PE server to AWS eliminating the need to configure inbound firewall rules when installed on-premises.
- When given a task by Step Functions, the worker webapp will request a publication from PE via its REST API (typically via http://localhost:8080/e3).
- The PE will return a queue ticket indicating that it has accepted the job.
- The Worker Webapp will periodically poll PE for the publication job/ticket completion and sends heartbeats to AWS Step Functions indicating that the activity is alive.
- When the PE has completed the publication job, the Worker Webapp will get the resulting ZIP file from PE and upload it to the S3.
- Finally, the Worker Webapp informs Step Functions that the activity task has completed causing that workflow execution to proceed to the next step.

