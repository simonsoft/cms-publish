<%--

    Copyright (C) 2009-2017 Simonsoft Nordic AB

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<html>
	<head>
		<meta charset="UTF-8">
		<title>SimonsoftCMS Publish Worker</title>
		<style>
			.webappVersion {
				color: grey;
				font-size: 12px;
				margin-top: -20px;
			}
		
		</style>
	</head>
	<body>
    	<h2>SimonsoftCMS Publish Worker</h2>
    	<p class="webappVersion"><a href="rest/info/version/">${buildName}</a></p>
    	<p><a href="rest/status/">Status</a></p>
    	<p><a href="rest/test/publish/document/">Publish Document</a></p>
    	<p><a href="rest/test/publish/job/">Publish Job</a></p>
    	<p><a href="rest/test/ticket/">Get Ticket</a></p>
	</body>
</html>
