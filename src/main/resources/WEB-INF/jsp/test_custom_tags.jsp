<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ex" uri="mytags"%>
<HTML>
<BODY>
Hello, world.
<%= System.getenv("PATH") %>
<br>
<a href="<c:url value="http://undertow.io"/>">Powered by undertow</a>
Current Date and Time is: <ex:today/>
</BODY>
</HTML>
