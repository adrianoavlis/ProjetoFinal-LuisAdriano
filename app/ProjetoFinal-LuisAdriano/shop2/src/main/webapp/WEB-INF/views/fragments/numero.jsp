<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:choose>
  <c:when test="${not empty param.valor}">
    <fmt:formatNumber value="${param.valor}" type="number" minFractionDigits="2" maxFractionDigits="2" />
  </c:when>
  <c:otherwise>-</c:otherwise>
</c:choose>
