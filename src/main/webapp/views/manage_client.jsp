<%@ page import="app.entities.Client" %>
<%@ page import="app.entities.Pair" %>
<%@ page import="app.entities.Payment" %>
<%@ page import="java.util.List" %>
<%--
  Created by IntelliJ IDEA.
  User: anime
  Date: 02.10.2019
  Time: 20:02
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <title>Manage Client</title>
    <meta charset="UTF-8">
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
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle text-info font-weight-bold" href="#" id="serverDropdown"
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
                    <a class="nav-link dropdown-toggle font-weight-bold" href="#" style="color: #ff9340;"
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
                    <form method="post" accept-charset="UTF-8" class="m-0">
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
    Client client = (Client) request.getAttribute("client");
    List<String> countries = (List<String>) request.getAttribute("countries");
    List<Pair<String, String>> countryList = (List<Pair<String, String>>) request.getAttribute("country_list");
    List<Payment> payments = (List<Payment>) request.getAttribute("payments");

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
            <h1 class="display-4">Client<small class="text-info">#<%=client.getId()%>
            </small> brief</h1>
        </div>
    </div>
    <div class="row mb-4">
        <div class="col-12">
            <div class="card-group mb-3">
                <div class="card ">
                    <div class="card-header bg-warning h5">Client details</div>
                    <div class="card-body">
                        <dl class="row px-3">
                            <dt class="col-5" style="margin-bottom: 15px;">Client e-mail:</dt>
                            <dd class="col-7" style="margin-bottom: 15px;">
                                <div class="input-group input-group-sm"><%=client.getEmail()%>
                                </div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Client
                                name:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm"><%=client.getClientName()%>
                                </div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                Referral bonuses:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm"><%=client.getRefDays()%> days</div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Client
                                subscription:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm">
                                    <%
                                        out.print(client.getSubscrTo() > System.currentTimeMillis() / 1000L ?
                                                "<strong class=\"text-success\">Active till " +
                                                        new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(client.getSubscrTo() * 1000L)) +
                                                        " (" + (client.getSubscrTo() - System.currentTimeMillis() / 1000L) / 86400L + ")" + "</strong>" :
                                                "<strong class=\"text-danger\">Expired at " +
                                                        new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(client.getSubscrTo() * 1000L)) + "</strong>");
                                    %>
                                </div>
                            </dd>
                            <%
                                if (client.getSubscrTo() > System.currentTimeMillis() / 1000L) {
                                    out.print("<dt class=\"col-5\" style=\"border-top: 1px solid #e6e6e6; padding-top: 15px; margin-bottom: 15px;\">Connected to <a href=\"" + getServletConfig().getServletContext().getContextPath() + "/manage_server?id=" + client.getServer().getId() + "\">" + "server</a>: </dt><dd class=\"col-7\" style=\"border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px;\">" +
                                            "<div class=\"input-group input-group-sm\"><input class=\"form-control border-dark bg-white\" aria-label=\"IP address\" type=\"text\" value=\"" +
                                            client.getServer().getIp_addr() + "\" disabled><div class=\"input-group-append\"><span class=\"input-group-text bg-warning border-dark\">" +
                                            client.getServer().getCountry() + "</span></div></div></dd>");

                                    out.print("<dt class=\"col-5\" style=\"border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px\">Server IPSec PSK</dt>\n" +
                                            "<dd class=\"col-7\" style=\"border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px\">\n" +
                                            "<div class=\"input-group input-group-sm\"><input class=\"form-control border-dark bg-white\" aria-label=\"IPSec pre-shared key\" type=\"text\" value=\"" +
                                            client.getServer().getIpSecPSK() + "\" disabled></div></dd>");
                                }
                            %>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">VPN
                                username:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm">
                                    <input class="form-control border-dark bg-white" aria-label="IPSec pre-shared key"
                                           type="text" value="<%out.print(client.getLogin());%>" disabled>
                                </div>
                            </dd>

                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">VPN
                                password:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <div class="input-group input-group-sm">
                                    <input class="form-control border-dark bg-white" aria-label="IPSec pre-shared key"
                                           type="text" value="<%out.print(client.getConn());%>" disabled>
                                </div>
                            </dd>

                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Registered
                                at:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%out.print(new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(client.getRegTime() * 1000)));%></dd>
                        </dl>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header bg-info text-white h5">Change user settings</div>
                    <div class="card-body">
                        <form method="post" accept-charset="UTF-8">
                            <div class="form-row ml-3 mr-4">
                                <div class="input-group ml-1 mr-2 mb-3">
                                    <div class="input-group-prepend">
                                        <%out.print("<span class=\"input-group-text" + (client.getSubscrTo() < System.currentTimeMillis() / 1000L ? " bg-warning\"><strong >Mount VPN</strong>" : "\">Adjust VPN length") + "</span>");%>
                                    </div>
                                    <input id="vpn_days" type="number" class="form-control" placeholder="Example: 5"
                                           aria-label="VPN days" name="vpn_days_post"
                                           value="<%out.print(client.getSubscrTo() < System.currentTimeMillis() / 1000L ? "" : (client.getSubscrTo() - System.currentTimeMillis() / 1000L) / 3600 / 24);%>"
                                        <%out.print(client.getServer() != null ? "required" : "");%>
                                           pattern="^[0-9]<%out.print(client.getServer() != null ? "+" : "*");%>$">
                                    <div class="input-group-append">
                                        <span class="input-group-text bg-white">days left</span>
                                    </div>
                                </div>
                                <div class="input-group ml-1 mr-2 mb-3">
                                    <div class="input-group-prepend">
                                        <label class="input-group-text" for="countryFrom">Connect from</label>
                                    </div>
                                    <select class="custom-select" id="countryFrom" name="country_from_post">
                                        <%
                                            for (Pair<String, String> tCountry : countryList) {
                                                out.print("<option " + (tCountry.getKey().contentEquals(client.getCountryFrom()) ? "selected" : "") + " value=\"" + tCountry.getKey() + "\">" + tCountry.getValue() + " (" + tCountry.getKey() + ")</option>");
                                            }
                                        %>
                                    </select>
                                    <label class="input-group-text"
                                           style="border-radius: 0; border-left: 0; border-right: 0;" for="countryTo">to
                                        server at</label>
                                    <select class="custom-select" id="countryTo" name="country_to_post">
                                        <%
                                            if (client.getSubscrTo() > System.currentTimeMillis() / 1000L) {
                                                out.print("<option value=\"--\" selected>Current (" + client.getServer().getCountry() + ")</option>");
                                            }
                                        %>
                                        <option value="OP">Optimal</option>
                                        <%
                                            for (String tCountry : countries) {
                                                out.print("<option value=\"" + tCountry + "\">" + countryList.stream().filter(country -> country.getKey().contentEquals(tCountry)).findFirst().orElse(new Pair<>("--", "None")).getValue() + " (" + tCountry + ")</option>");
                                            }
                                        %>
                                    </select>
                                </div>
                            </div>
                            <div class="form-row ml-3">
                                <div class="input-group mb-3 col-8">
                                    <input type="text" class="form-control" aria-label="Inform user"
                                           value="Inform user via e-mail" disabled>
                                    <div class="input-group-append">
                                        <div class="input-group-text bg-white">
                                            <input id="inform" type="checkbox" checked aria-label="Inform user"
                                                   value="inform" name="inform_post">
                                        </div>
                                    </div>
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
                                            <span class="input-group-text">Client Name</span>
                                        </div>
                                        <input id="client-name" type="text" class="form-control"
                                               aria-label="Client Name"
                                               name="clientname_post" placeholder="Example: John Doe"
                                               value="<%out.print(client.getClientName());%>">
                                    </div>
                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">E-mail</span>
                                        </div>
                                        <input id="email" type="email" class="form-control" aria-label="E-mail"
                                               name="email_post" placeholder="Example: ivan08@mail.com"
                                               value="<%out.print(client.getEmail());%>">
                                    </div>

                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">Referral bonus</span>
                                        </div>
                                        <input id="refDays" type="number" class="form-control" aria-label="Ref Days"
                                               name="ref_days_post" value="<%=client.getRefDays()%>">
                                        <div class="input-group-append">
                                            <span class="input-group-text bg-white">days</span>
                                        </div>
                                    </div>

                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">Referred from (type -1 if none)</span>
                                            <span class="input-group-text bg-white">user #</span>
                                        </div>
                                        <input id="referredFrom" type="number" class="form-control"
                                               aria-label="referred from"
                                               name="referred_from_post" value="<%=client.getReferredFrom()%>">
                                    </div>

                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <span class="input-group-text">Interface language</span>
                                        </div>
                                        <select id="lang" class="form-control"
                                                aria-label="interface language"
                                                name="lang_post">
                                            <option value="ru" <%=client.getLang().equals("ru") ? "selected" : ""%>>
                                                Russian
                                            </option>
                                            <option value="en" <%=client.getLang().equals("en") ? "selected" : ""%>>
                                                English
                                            </option>
                                        </select>
                                    </div>

                                    <%if (client.getSwapServerAttempt() != 0) {%>
                                    <div class="input-group mb-3">
                                        <input type="text" class="form-control" aria-label="Clear attempts"
                                               value="Clear VPN server change attempts" disabled>
                                        <div class="input-group-append">
                                            <div class="input-group-text bg-white">
                                                <input id="clear_attempts" type="checkbox" aria-label="Clear attempts"
                                                       value="clear_attempts_post" name="clear_attempts_post">
                                            </div>
                                        </div>
                                    </div>
                                    <%}%>

                                    <div class="input-group mb-3">
                                        <div class="input-group-prepend">
                                            <div class="input-group-text bg-white border-danger">
                                                <input type="checkbox" aria-label="Delete client"
                                                       onclick="disableElForDeletion(this)" name="delete_post"
                                                       value="delete">
                                            </div>
                                        </div>
                                        <input type="text" class="form-control bg-danger border-danger text-white"
                                               aria-label="Delete client" value="Remove this client from the system!"
                                               disabled>
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-12">
            <div class="display-4 pb-3">
                Payments history
            </div>
            <%if (payments != null && !payments.isEmpty()) {%>
            <table id="table"
                   class="table <%=payments.size() > 100 ? "table-sm " : ""%>table-hover table-responsive-sm table-striped table-intel table-listing">
                <thead>
                <tr class="bg-primary text-light">
                    <th scope="col" class="filter">#</th>
                    <th scope="col" class="filter">Time</th>
                    <th scope="col" class="filter">Days</th>
                    <th scope="col" class="filter">Fee</th>
                    <th scope="col" class="filter">Paym. Met.</th>
                    <th scope="col" class="filter">Coupon</th>
                </tr>
                </thead>
                <tbody>
                <%for (Payment tPayment : payments) {%>
                <tr scope="row">
                    <td class="small"><%=tPayment.getId()%>
                    </td>
                    <td><%=new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(tPayment.getTimeStamp() * 1000L))%>
                    </td>
                    <td><%=tPayment.getDaysAmount()%>
                    </td>
                    <td>$<%=tPayment.getFee()%>
                    </td>
                    <td><%=tPayment.getPaymentMethod()%>
                    </td>
                    <td><%=tPayment.getCouponInfo()%>
                    </td>
                </tr>
                <%}%>
                </tbody>
            </table>
            <%} else {%>
            <div class="alert alert-primary">No payments done yet</div>
            <%}%>
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

    function disableElForDeletion(status) {
        if (status.checked === true) {
            document.getElementById('vpn_days').disabled = true;
            document.getElementById('client-name').disabled = true;
            document.getElementById('countryFrom').disabled = true;
            document.getElementById('countryTo').disabled = true;
            document.getElementById('email').disabled = true;
            document.getElementById('refDays').disabled = true;
            document.getElementById('referredFrom').disabled = true;
            document.getElementById('lang').disabled = true;
            document.getElementById('clear_attempts').disabled = true;
        } else {
            document.getElementById('vpn_days').disabled = false;
            document.getElementById('client-name').disabled = false;
            document.getElementById('countryFrom').disabled = false;
            document.getElementById('countryTo').disabled = false;
            document.getElementById('email').disabled = false;
            document.getElementById('refDays').disabled = false;
            document.getElementById('referredFrom').disabled = false;
            document.getElementById('lang').disabled = false;
            document.getElementById('clear_attempts').disabled = false;
        }

    }


    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    });

</script>
</body>
</html>