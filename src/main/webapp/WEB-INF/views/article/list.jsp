<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <jsp:include page="../head/head.jsp"/>
    <title>게시판 - 목록</title>
</head>
<body>
<h2 class="ui header"> 게시판 목록 </h2>
<div id="list">
    <table class="ui selectable blue striped celled padded table">
        <thead>
        <tr>
            <th class="one wide center aligned">번호</th>
            <th class="nine wide center aligned">제목</th>
            <th class="three wide center aligned">글쓴이</th>
            <th class="one wide center aligned">조회</th>
            <th class="two wide center aligned">날짜</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${articleList}" var="article">
            <tr>
                <td class="center aligned">${article.id}</td>
                <td><a href="/article/read?id=${article.id}">
                        <c:forEach begin="1" end="${article.depthLevel}" step="1">└</c:forEach>
                        ${article.title}</a></td>
                <td class="center aligned">${article.nickName}</td>
                <td class="center aligned">${article.hit}</td>
                <td class="center aligned">${article.regdate}</td>
            </tr>
        </c:forEach>

        </tbody>

    </table>

    <button style="text-align: center" type="button" onclick="window.location.href='/article/write?categoryid=${categoryId}'" class="ui two wide column right floated primary button">
        <i class="icon edit"></i>
        글쓰기
    </button>

    <form method="post" action="/article/search">
        <select name="searchType">
            <option value="제목">제목</option>
            <option value="내용">내용</option>
            <option value="제목+내용">제목+내용</option>
            <option value="이름">이름</option>
        </select>
        <input type="search" name="searchWord">
        <input type="hidden" name="categoryId" value="${categoryId}">
        <button type="submit">검색</button>
    </form>
</div>

</body>
</html>
