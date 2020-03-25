<%--
  Created by IntelliJ IDEA.
  User: anime
  Date: 07.10.2019
  Time: 23:58
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>NectedAdmin</title>
    <!-- Load the plugin bundle. -->
    <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.bundle.min.js"
            integrity="sha384-xrRywqdh3PHs8keKZN+8zzc5TX0GRTLCcmivcbNJWm2rs5C8PRhcEn3czEjhAO9o"
            crossorigin="anonymous"></script>
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

</head>
<body>
<div id="loader" class="loader">
    <div class="spinner-border m-auto text-primary" style="width: 5rem; height: 5rem;"></div>
</div>
<div class="container" style="height: 100%;">
    <div class="row justify-content-sm-center align-items-end" style="height: 50%;">
        <div class="col col-sm-4">
            <img src="${pageContext.request.contextPath}/src/logo.png" class="img-fluid" alt="NectedAdmin">
        </div>
    </div>
    <div class="row justify-content-sm-center align-items-start" style="height: 50%;">
        <div class="col col-sm-4">
            <form method="post">
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <label class="input-group-text" id="username">Login</label>
                    </div>
                    <input name="username" type="text" class="form-control" aria-label="Username"
                           aria-describedby="username">
                </div>
                <div class="form-row">
                    <div class="col-9">
                        <div class="input-group mb-3">
                            <div class="input-group-prepend">
                                <label class="input-group-text" id="passsword">Password</label>
                            </div>
                            <input name="password" type="password" class="form-control" aria-label="Password"
                                   aria-describedby="password">
                        </div>
                    </div>
                    <div class="col-3">
                        <button class="btn btn-primary" autofocus style="width: 100%;" type="submit">Enter</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
<script>
    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    })
</script>
</body>
</html>
