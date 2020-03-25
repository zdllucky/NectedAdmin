<%@ page import="app.entities.Client" %>
<%@ page import="app.entities.Pair" %>
<%@ page import="app.entities.Server" %>
<%@ page import="app.entities.Strike" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %><%--
  Created by IntelliJ IDEA.
  User: anime
  Date: 25.09.2019
  Time: 1:01
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Manage Server</title>

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

    <style>
        #toggle-input {
            display: none;
        }

        .toggle-label {
            display: block;
            cursor: pointer;
            padding: 12px;
            font-weight: 600;
        }

        .toggle-label:after {
            content: "─";
        }

        .module {
            padding: 25px;
        }

        .toggle-content {
            max-height: 0;
            overflow: hidden;
            -moz-transition: ease 0.5s;
            -o-transition: ease 0.5s;
            -webkit-transition: ease 0.5s;
            transition: ease 0.5s;
        }

        #toggle-input:checked ~ .toggle-content {
            max-height: 1000px;
        }

        #toggle-input:checked + .toggle-label:after {
            content: "▼";
        }
    </style>
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
                <li class="nav-item dropdown active">
                    <a class="nav-link dropdown-toggle font-weight-bold" style="color: #ff9340;" href="#"
                       id="serverDropdown" role="button" data-toggle="dropdown" aria-haspopup="true"
                       aria-expanded="false">
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
                <li class="nav-item dropdown ">
                    <a class="nav-link dropdown-toggle text-info font-weight-bold" href="#" id="clientDropdown"
                       role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
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

<%
    Server server = (Server) request.getAttribute("server");
    List<Client> serverClients = (List<Client>) request.getAttribute("clients");
    List<Pair<String, String>> countries = (List<Pair<String, String>>) request.getAttribute("countries");
    long date;
%>

<div class="container">
    <%
        if (session.getAttribute("shout") != null) {
            out.println("<div class=\"row mt-4\"><div class=\"col\"><div class=\"alert alert-" + session.getAttribute("result") + " alert-dismissible fade show\" role=\"alert\">\n" +
                    session.getAttribute("shout") +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\">\n" +
                    "    <span aria-hidden=\"true\">&times;</span>\n" +
                    "  </button>\n" +
                    "</div></div></div>");
            session.removeAttribute("shout");
            session.removeAttribute("result");
        }

    %>
    <div class="row mt-4 mx-1">
        <div class="col">
            <h1 class="display-4">Server<small class="text-info">#<%out.print(server.getId());%></small> brief</h1>
        </div>
    </div>
    <div class="row mb-4">
        <div class="col">
            <div class="card-group mb-3">
                <div class="card ">
                    <div class="card-header bg-warning h5">Server details</div>
                    <div class="card-body">
                        <dl class="row px-3">
                            <dt class="col-5" style=" margin-bottom: 15px;">Server IP:</dt>
                            <dd class="col-7" style=" margin-bottom: 15px;">
                                <div class="input-group input-group-sm">
                                    <input class="form-control border-dark bg-white" aria-label="IPSec pre-shared key"
                                           type="text" value="<%out.print(server.getIp_addr());%>" disabled>
                                    <div class="input-group-append">
                                        <span class="input-group-text bg-warning border-dark"><%out.print(server.getCountry());%></span>
                                    </div>
                                </div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Server
                                password:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm">
                                    <input class="form-control border-dark bg-white" aria-label="IPSec pre-shared key"
                                           type="text" value="<%out.print(server.getConn());%>" disabled>
                                </div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Server
                                IPSec PSK
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm">
                                    <input class="form-control border-dark bg-white" aria-label="IPSec pre-shared key"
                                           type="text" value="<%out.print(server.getIpSecPSK());%>" disabled>
                                </div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Server
                                state:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><strong>
                            <span class="text-<%=server.getState() == Server.State.RUNNING ? "success\">Running" : server.getState() == Server.State.DEPRECATED ? "danger\">Deprecated" : server.getState() == Server.State.SETTING_UP ? "warning\">Configuring VPN" : "secondary\"Not set up"%>
                            </span></strong>
                        </dd>
                        <dt class=" col-5" style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top:
                                15px">Server strikes amount:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <%
                                    try {
                                        if (server.getStrikes().size() > 0) {
                                            List<Strike> strikes = server.getStrikes();
                                            for (Strike strike : strikes) {
                                                out.print(countries.stream().filter(country -> country.getKey().equals(strike.getCountry())).collect(Collectors.toList()).get(0).getValue() +
                                                        "(" + strike.getCountry() + ") - " + "<strong class=text-" + (strike.getAmount() < 3 ? "dark" : strike.getAmount() < 8 ? "warning" : "danger") + ">" + strike.getAmount() + "</strong><br>");
                                            }
                                        } else {
                                            out.print("<strong class=text-success>No strikes</strong>");
                                        }
                                    } catch (SQLException e) {
                                        out.print("<strong class=text-danger>Database error</strong>");
                                        e.printStackTrace();
                                    }
                                %>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Server
                                clients/limits:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%out.print(serverClients.size() + " / " + server.getUsersLimit());%></dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Provider
                                info:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <%=server.getProperty()%>
                            </dd>
                        </dl>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header bg-info text-white h5">Change server settings</div>
                    <div class="card-body">
                        <form method="post">
                            <div class="form-row ml-3">
                                <div class="input-group mb-3 col-8">
                                    <div class="input-group-prepend">
                                        <label for="a2" class="input-group-text">Change server state</label>
                                    </div>
                                    <select class="custom-select" aria-label="Deprecation" id="a2"
                                            name="deprecate_post">
                                        <option value="1"<%out.print(server.getState() == Server.State.RUNNING ? " selected" : "");%>>
                                            Running
                                        </option>
                                        <option value="0"<%out.print(server.getState() == Server.State.DEPRECATED ? " selected" : "");%>>
                                            Deprecated
                                        </option>
                                        <option value="-1"<%out.print(server.getState() == Server.State.SETTING_UP ? " selected" : "");%>>
                                            Setting up
                                        </option>
                                        <option value="-2"<%out.print(server.getState() == Server.State.NOT_SET_UP ? " selected" : "");%>>
                                            Not Setup
                                        </option>
                                    </select>
                                </div>
                                <div class="input-group mb-3 col-4">
                                    <button type="submit" class="btn btn-primary">Apply settings</button>
                                </div>
                            </div>
                            <input type="checkbox" id="toggle-input" value="selected">
                            <label class="toggle-label text-primary" for="toggle-input">Advanced options </label>
                            <div class="toggle-content">
                                <div class="module">
                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">IP address</span>
                                        </div>
                                        <input id="a3" type="text" class="form-control" placeholder="Example: 1.1.1.1"
                                               aria-label="IP address" name="ip_addr_post" minlength="7" maxlength="15"
                                               size="15"
                                               pattern="^((\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.){3}(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$"
                                               value="<%out.print(server.getIp_addr());%>">
                                    </div>
                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <label for="country" class="input-group-text">Server location</label>
                                        </div>
                                        <select name="country_post" class="custom-select" id="country">
                                            <%
                                                for (Pair<String, String> country : countries) {
                                                    out.println("<option value=\"" + country.getKey() + "\" " +
                                                            (country.getKey().contentEquals(server.getCountry()) ? "selected" : " ") + ">" + country.getValue() + " (" + country.getKey() + ")</option>");
                                                }

                                            %>
                                        </select>
                                    </div>

                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">SSH password</span>
                                        </div>
                                        <input id="a5" class="form-control" aria-label="SSH password" type="password"
                                               name="conn_post" value="<%out.print(server.getConn());%>">
                                    </div>
                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">IPSec pre-shared key</span>
                                        </div>
                                        <input id="a6" class="form-control" aria-label="IPSec pre-shared key"
                                               type="text" name="ipsecpsk_post" placeholder="20 symbols string only"
                                               value="<%out.print(server.getIpSecPSK());%>">
                                    </div>
                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">Users limit</span>
                                        </div>
                                        <input id="a7" class="form-control" aria-label="IPSec pre-shared key"
                                               type="number" name="users_limit_post" placeholder="Example: 20"
                                               value="<%out.print(server.getUsersLimit());%>">
                                    </div>
                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <div class="input-group-text bg-white border-danger">
                                                <input type="checkbox" aria-label="Delete server"
                                                       onclick="disableEl(this)" name="delete_post" value="delete">
                                            </div>
                                        </div>
                                        <input type="text" class="form-control bg-danger border-danger text-white"
                                               aria-label="IP address" value="Remove this server from the system!"
                                               disabled>
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row mt-4 mx-1">
        <div class="col">
            <h1 class="display-4">On server clients</h1>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <%
                if (!serverClients.isEmpty()) {
                    out.println("<table id=\"table\" class=\"table " + (serverClients.size() > 100 ? "table-sm " : "") + "table-hover table-responsive-sm table-striped table-intel table-listing\"><thead><tr class=\"bg-primary text-light\">" +
                            "<th scope=\"col\" class=\"filter\">Id</th>" +
                            "<th scope=\"col\" class=\"filter\">Client name</th>" +
                            "<th scope=\"col\" class=\"filter\">E-mail</th>" +
                            "<th scope=\"col\" class=\"filter\">Ref ID</th>" +
                            "<th scope=\"col\" class=\"filter\">Bonuses</th>" +
                            "<th scope=\"col\" class=\"filter\">Connected from</th>" +
                            "<th scope=\"col\" class=\"filter\">Days left</th>" +
                            "</tr></thead><tbody>");

                    for (Client serverClient : serverClients) {
                        date = (serverClient.getSubscrTo() - System.currentTimeMillis() / 1000) / 3600 / 24;
                        out.print("<tr scope=\"row\"><td><a href=\"" + getServletConfig().getServletContext().getContextPath() + "/manage_client?id=" + serverClient.getId() + "\">" + serverClient.getId() + "</a></td><td>" +
                                serverClient.getClientName() + "</td><td>" +
                                serverClient.getEmail() + "</td><td>" +
                                (serverClient.getReferredFrom() != -1 ? "<a href=\"" + request.getContextPath() + "/manage_client?id=" + serverClient.getReferredFrom() + "\">" + serverClient.getReferredFrom() + "</a>" : "") + "</td><td>" +
                                serverClient.getRefDays() + "</td><td>" +
                                countries.stream().filter(stringStringPair -> stringStringPair.getKey().contentEquals(serverClient.getCountryFrom())).findFirst().get().getValue() + " (" + serverClient.getCountryFrom() + ")</td><td>" +
                                date + " days</td></tr>");
                    }

                    out.println("</tbody></table>");
                } else {
                    out.println("<div class=\"alert alert-primary\" role=\"alert\">\n" +
                            "No clients connected on this server." +
                            "</div>");
                }
            %>
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

    function disableEl(status) {
        if (status.checked === true) {
            document.getElementById('a2').disabled = true;
            document.getElementById('a3').disabled = true;
            document.getElementById('country').disabled = true;
            document.getElementById('a5').disabled = true;
            document.getElementById('a6').disabled = true;
            document.getElementById('a7').disabled = true;
        } else {
            document.getElementById('a2').disabled = false;
            document.getElementById('a3').disabled = false;
            document.getElementById('country').disabled = false;
            document.getElementById('a5').disabled = false;
            document.getElementById('a6').disabled = false;
            document.getElementById('a7').disabled = false;
        }

    }

    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    });


</script>
</body>
</html>
