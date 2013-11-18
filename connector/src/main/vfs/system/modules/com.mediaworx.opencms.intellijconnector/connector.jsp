<%@page
		contentType="text/html; charset=UTF-8"
		pageEncoding="UTF-8"
		import="com.mediaworx.opencms.intellijconnector.OpenCmsIntelliJConnector"
%><%
	OpenCmsIntelliJConnector connector = new OpenCmsIntelliJConnector(pageContext);
	connector.streamResponse();
%>