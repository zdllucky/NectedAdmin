<%@ page import="app.entities.Pair" %>
<%@ page import="io.ipinfo.api.model.IPResponse" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Add Client</title>

    <!-- Load the plugin bundle. -->
    <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-bundle.js"></script>
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
<nav class="navbar sticky-top navbar-light navbar-expand-lg" style="background-color: #e3f2fd;">
    <div class="container">
        <a class="navbar-brand mb-0 h1" href="${pageContext.request.contextPath}/"><img
                src="${pageContext.request.contextPath}/src/logo.png" alt="NectedAdmin" height="60"></a>

        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
                aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse " id="navbarSupportedContent">
            <ul class="navbar-nav mr-auto">
                <li class="nav-item dropdown ">
                    <a class="nav-link text-info dropdown-toggle font-weight-bold" href="#" id="serverDropdown"
                       role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        Servers
                    </a>
                    <div class="dropdown-menu" aria-labelledby="serverDropdown">
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/servers">Servers list</a>
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/servers/autodeploy/linode"
                           role="button">Linode autodeployment</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/newserver">Add new server</a>
                    </div>
                </li>
                <li class="nav-item dropdown active">
                    <a class="nav-link dropdown-toggle font-weight-bold" style="color: #ff9340;" href="#"
                       id="clientDropdown" role="button" data-toggle="dropdown" aria-haspopup="true"
                       aria-expanded="false">
                        Clients
                    </a>
                    <div class="dropdown-menu" aria-labelledby="clientDropdown">
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/clients">Active subscription
                            clients</a>
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/expired_clients">Expired
                            subscription clients</a>
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/mailing">E-mailing</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/newclient">Add new client</a>
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/mailing/new">Add new mailing
                            template</a>
                    </div>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-info font-weight-bold"
                       href="${pageContext.request.contextPath}/settings" role="button">
                        Platform settings
                    </a>
                </li>
                <li class="nav-item">
                    <form method="post" class="m-0">
                        <input name="logout" value="1" style="display: none">
                        <button class="btn btn-link nav-link text-info font-weight-bold" type="submit"
                                aria-expanded="false">
                            Logout
                        </button>
                    </form>
                </li>
            </ul>
        </div>
        <div class=""><span class="font-weight-bold h6 text-info" id="doc_time"></span></div>
    </div>
</nav>

<div class="container">
    <%
        if (session.getAttribute("shout") != null) {
            out.println("<div class=\"row mt-4 justify-content-center\"><div class=\"col-md-6\"><div class=\"alert alert-" + session.getAttribute("result") + " alert-dismissible fade show\" role=\"alert\">\n" +
                    session.getAttribute("shout") +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\">\n" +
                    "    <span aria-hidden=\"true\">&times;</span>\n" +
                    "  </button>\n" +
                    "</div></div></div>");
            session.removeAttribute("shout");
            session.removeAttribute("result");
        }

        IPResponse ipResponse = (IPResponse) request.getAttribute("ip_response");
        List<Pair<String, String>> countries = (List<Pair<String, String>>) request.getAttribute("countries_list");
        List<String> countriesTo = (List<String>) request.getAttribute("countries");

    %>
    <div class="row mt-4 mx-1 justify-content-center">
        <div class="col-md-6">
            <h1 class="display-4">Add new client</h1>
        </div>
    </div>
    <div class="row my-4 justify-content-center">
        <div class="col-md-6">
            <form method="post">
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <span class="input-group-text">Client name</span>
                    </div>
                    <input type="text" class="form-control" aria-label="E-mail"
                           name="client_name" placeholder="Example: John Doe" required>
                </div>
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <span class="input-group-text">E-mail</span>
                    </div>
                    <input type="email" class="form-control" aria-label="E-mail"
                           name="email" placeholder="Example: ivan08@mail.com" required>
                </div>
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <span class="input-group-text bg-warning border-warning">For instant VPN</span>
                        <span class="input-group-text">Duration</span>
                    </div>
                    <input type="number" class="form-control" aria-label="VPN duration"
                           name="sups_days" pattern="^[0-9]+$" placeholder="Days amount">
                </div>
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <label for="countryFrom" class="input-group-text">Your location (Autodetected)</label>
                    </div>
                    <select name="countryFrom" class="custom-select" id="countryFrom">
                        <%
                            for (Pair<String, String> country : countries) {
                                out.println("<option value=\"" + country.getKey() + "\" " +
                                        (country.getKey().contentEquals(ipResponse.getCountryCode()) ? "selected" : " ") + ">" + country.getValue() + " (" + country.getKey() + ")</option>");
                            }

                        %>
                    </select>
                </div>
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <label for="countryTo" class="input-group-text">Connect to location</label>
                    </div>
                    <select name="countryTo" class="custom-select" id="countryTo">
                        <option value="OP" selected>Optimal</option>
                        <%
                            for (String tCountry : countriesTo) {
                                out.print("<option value=\"" + tCountry + "\">" + countries.stream().filter(country -> country.getKey().contentEquals(tCountry)).collect(Collectors.toList()).get(0).getValue() + " (" + tCountry + ")</option>");
                            }
                        %>
                    </select>
                    <div class="input-group-append">
                        <button type="submit" class="btn btn-primary">Add client</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
<script>

    function clock() {
        let d = new Date();
        d.setUTCMinutes(d.getUTCMinutes() + d.getTimezoneOffset());
        const month_num = d.getMonth();
        let day = d.getDate();
        let hours = d.getHours();
        let minutes = d.getMinutes();
        let seconds = d.getSeconds();

        const month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

        if (day <= 9) day = "0" + day;
        if (hours <= 9) hours = "0" + hours;
        if (minutes <= 9) minutes = "0" + minutes;
        if (seconds <= 9) seconds = "0" + seconds;

        const date_time = day + " " + month[month_num] + " " + d.getFullYear() +
            " " + hours + ":" + minutes + ":" + seconds;
        if (document.layers) {
            document.layers.doc_time.document.write(date_time);
            document.layers.doc_time.document.close();
        } else document.getElementById("doc_time").innerHTML = date_time;
        setTimeout("clock()", 1000);
    }

    // Use the plugin once the DOM has been loaded.
    $(function () {
        // Apply the plugin
        $('#table').excelTableFilter();
        clock();
    });

    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    });

</script>
</body>
</html>
