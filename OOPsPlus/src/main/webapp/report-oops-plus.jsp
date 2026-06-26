<%--
SPDX-FileCopyrightText: 2014 Maria Poveda Villalon <mpovedavillalon@gmail.com>
SPDX-FileCopyrightText: 2025 Pieter Hijma <info@pieterhijma.net>
SPDX-FileCopyrightText: 2025 Robin Vobruba <hoijui.quaero@gmail.com>

SPDX-License-Identifier: Apache-2.0
--%>

<%@ page contentType="text/html; charset=utf-8"
	import="com.fasterxml.jackson.databind.ObjectMapper, es.upm.fi.oeg.oops.Utils, java.nio.charset.StandardCharsets, java.nio.file.Files, java.nio.file.Path, java.util.Map" errorPage=""%>

<%!
private static final Path ANALYSES_DIR = Path.of("/data/oops/analyses");
private static final ObjectMapper STATUS_MAPPER = new ObjectMapper();

private String asText(final Object value) {
	return value == null ? "" : value.toString();
}

private String escaped(final Object value) {
	return Utils.escapeForHtml(asText(value));
}

private String readReportFile(final Object pathValue) {
	if (pathValue == null) {
		return "";
	}
	try {
		final Path path = Path.of(pathValue.toString()).normalize();
		if (!Files.isRegularFile(path)) {
			return "";
		}
		return Files.readString(path, StandardCharsets.UTF_8);
	} catch (final Exception exc) {
		return "Could not read report file: " + exc.getMessage();
	}
}

%>

<%
final String analysisId = request.getParameter("analysisId");
Map<String, Object> statusData = null;
String pageError = null;
String status = "";
boolean refresh = false;

if (analysisId == null || analysisId.isBlank()) {
	pageError = "Missing analysisId parameter.";
} else if (!analysisId.matches("[A-Za-z0-9._-]+")) {
	pageError = "Invalid analysisId parameter.";
} else {
	final Path analysisDir = ANALYSES_DIR.resolve(analysisId).normalize();
	final Path statusPath = analysisDir.resolve("status.json").normalize();
	if (!analysisDir.startsWith(ANALYSES_DIR)) {
		pageError = "Invalid analysis path.";
	} else if (!Files.isRegularFile(statusPath)) {
		status = "QUEUED";
		refresh = true;
	} else {
		statusData = STATUS_MAPPER.readValue(statusPath.toFile(), Map.class);
		status = asText(statusData.get("status"));
		refresh = "QUEUED".equals(status) || "PROCESSING".equals(status);
	}
}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta name="Author" content="Maria Poveda Villalon" />
	<meta name="Language" content="English" />
	<meta name="Keywords" content="ontology, ontology evaluation, pitfalls, OOPS+" />
	<meta name="Description" content="OOPS+ report status and results" />
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<%
	if (refresh) {
	%>
	<meta http-equiv="refresh" content="5">
	<%
	}
	%>
	<link
		href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.1/dist/css/bootstrap.min.css"
		rel="stylesheet"
		integrity="sha384-+0n0xVW2eSR5OomGNYDnhzAbDsOXxcvSN1TPprVMTNDbiYZCxYbOOl7+AMvyTG2x"
		crossorigin="anonymous">
	<link href="css/style.css" rel="stylesheet" type="text/css" />
	<link rel="icon" type="image/png" href="images/favicon.png">
	<title>OOPS+ - Report</title>
</head>
<body>

<%@ include file="part-menu.html" %>

	<div id="wrap">
		<div id=main>
			<br><br>
			<h1>OOPS+ report</h1>

			<%
			if (pageError != null) {
			%>
				<div class="txt">
					<h3>Something went wrong.</h3>
					<p><%= Utils.escapeForHtml(pageError) %></p>
				</div>
			<%
			} else if ("QUEUED".equals(status) || "PROCESSING".equals(status)) {
			%>
				<div class="txt">
					<h3>Processing report</h3>
					<p>The OOPS+ report for analysis <strong><%= Utils.escapeForHtml(analysisId) %></strong> is currently <strong><%= Utils.escapeForHtml(status) %></strong>.</p>
					<p>This page refreshes automatically every 5 seconds.</p>
				</div>
			<%
			} else if ("FAILED".equals(status)) {
			%>
				<div class="txt">
					<h3>OOPS+ analysis failed</h3>
					<p>Analysis: <strong><%= Utils.escapeForHtml(analysisId) %></strong></p>
					<p><strong>Error:</strong> <%= escaped(statusData == null ? "" : statusData.get("error")) %></p>
				</div>
			<%
			} else if ("COMPLETED".equals(status)) {
				final String reportHtml = readReportFile(statusData.get("reportHtmlPath"));
			%>
				<div class="txt">
					<h3>OOPS+ analysis completed</h3>
					<p>Analysis: <strong><%= Utils.escapeForHtml(analysisId) %></strong></p>
				</div>
				<br>
				<%= reportHtml %>
			<%
			} else {
			%>
				<div class="txt">
					<h3>Unknown report status</h3>
					<p>Analysis: <strong><%= Utils.escapeForHtml(analysisId) %></strong></p>
					<p>Status: <strong><%= Utils.escapeForHtml(status) %></strong></p>
				</div>
			<%
			}
			%>
		</div>
	</div>

<%@ include file="part-footer-scripts.html" %>

</body>
</html>
