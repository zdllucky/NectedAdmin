<%@ page import="app.entities.Pair" %>
<%@ page import="app.entities.Server" %>
<%@ page import="app.entities.Strike" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %>
<%--
  Created by IntelliJ IDEA.
  User: anime
  Date: 24.09.2019
  Time: 10:39
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Servers overview</title>

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
<%
    List<Server> servers = (List<Server>) request.getAttribute("servers");
    List<Pair<String, String>> countries = (List<Pair<String, String>>) request.getAttribute("countries");
    List<Strike> allStrikes = (List<Strike>) request.getAttribute("strikes");
    HashSet<String> countryStrikes = allStrikes.stream().map(Strike::getCountry).collect(Collectors.toCollection(HashSet::new));


    int exposedServers, activeServers, usersOn, usersLimit;
    activeServers = exposedServers = usersOn = usersLimit = 0;

    for (Server server : servers) {
        if (server.getState() == Server.State.RUNNING) {
            activeServers++;
            usersOn += server.getClientsAmount();
            usersLimit += server.getUsersLimit();
        } else {
            exposedServers++;
        }
    }

    List<Pair<String, Integer>> countryStats = new ArrayList<>();
    if (servers.size() > 0)
        countryStats.add(new Pair<>(servers.get(0).getCountry(), 0));


    for (Server server : servers) {
        for (int j = 0; ; j++) {
            if (server.getCountry().contentEquals(countryStats.get(j).getKey())) {
                if (server.getState() == Server.State.RUNNING)
                    countryStats.set(j, new Pair<>(countryStats.get(j).getKey(), countryStats.get(j).getValue() + 1));
                break;
            } else if (j == countryStats.size() - 1) {
                countryStats.add(new Pair<>(server.getCountry(), (server.getState() == Server.State.RUNNING ? 1 : 0)));
                break;
            }
        }
    }

    List<Integer> clientsByCountry = new ArrayList<>();
    List<Integer> limitsByCountry = new ArrayList<>();
    int byCountryLimitsTemp, byCountryClientsTemp;

    for (Pair<String, Integer> countryStat : countryStats) {
        byCountryClientsTemp = byCountryLimitsTemp = 0;

        for (Server server : servers) {
            if (server.getState() == Server.State.RUNNING && countryStat.getKey().contentEquals(server.getCountry())) {
                byCountryClientsTemp += server.getClientsAmount();
                byCountryLimitsTemp += server.getUsersLimit();
            }
        }

        clientsByCountry.add(byCountryClientsTemp);
        limitsByCountry.add(byCountryLimitsTemp);
    }
%>
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
                <li class="nav-item dropdown">
                    <a class="nav-link text-info dropdown-toggle font-weight-bold" href="#" id="clientDropdown"
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
            <h1 class="display-4">Servers brief</h1>
        </div>
    </div>
    <div class="row mb-4">
        <div class="col">
            <div class="card-group mb-3">
                <div class="card ">
                    <div class="card-header bg-warning h5">Servers overview</div>
                    <div class="card-body">
                        <dl class="row px-3">
                            <dt class="col-5" style=" margin-bottom: 15px;">Machines amount:</dt>
                            <dd class="col-7" style=" margin-bottom: 15px;"><%out.print(servers.size());%></dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Active
                                machines:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%out.print(activeServers);%></dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Exposed
                                machines:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%out.print(exposedServers);%></dd>
                            <dt class="col-5" style="border-top: 1px solid #e6e6e6; padding-top: 15px">Up users /
                                limit:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; padding-top: 15px"><%out.print(usersOn + " / " + usersLimit);%></dd>
                        </dl>

                        <h5 class="card-title text-danger mt-5">Strikes by country</h5>
                        <dl class="row px-3">
                            <%
                                for (String countryStrike : countryStrikes) {
                                    out.print("");
                                    out.print("<dt class=\"col-5\" style=\"border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px\">" +
                                            "<div class=\"custom-control custom-radio\">" +
                                            "<input type=\"radio\" class=\"sel-country custom-control-input\" id=\"" + countryStrike + "\">" +
                                            "<label class=\"custom-control-label\" for=\"" + countryStrike + "\">" +
                                            countries.stream().filter(country -> country.getKey().contentEquals(countryStrike)).findFirst().orElse(new Pair<>("--", "None")).getValue() + " (" + countryStrike + ")" +
                                            "</label></div></dt><dd class=\"col-7\" style=\"border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px\"><strong class=text-danger>" +
                                            allStrikes.stream().filter(strike -> strike.getCountry().contentEquals(countryStrike)).collect(Collectors.summarizingInt(Strike::getAmount)).getSum() +
                                            " strikes</strong></dd>");
                                }
                            %>
                        </dl>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header bg-warning h5">Load by countries</div>
                    <div class="card-body">
                        <%
                            for (int i = 0; i < countryStats.size(); i++) {
                                if (limitsByCountry.get(i) != 0) {
                                    String tCountry = countryStats.get(i).getKey();
                                    out.println("<h5 class=\"card-title\">" + countries.stream().filter(country -> country.getKey().equals(tCountry)).collect(Collectors.toList()).get(0).getValue() + " (" + countryStats.get(i).getKey() + ")</h5>");
                                    out.println("<p class=\"card-text\"> Servers: <strong>" + countryStats.get(i).getValue() +
                                            "</strong>  &nbsp;  &nbsp; Fulfillment: <strong>" + clientsByCountry.get(i) * 100 / limitsByCountry.get(i) + "%</strong> (" +
                                            clientsByCountry.get(i) + "/" + limitsByCountry.get(i) + ")</p><hr>");
                                }
                            }
                        %>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row mt-4 mx-1">
        <div class="col">
            <h1 class="display-4">Servers list</h1>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <%
                if (!servers.isEmpty()) {
                    out.println("<table id=\"table\" class=\"mb-5 table " + (servers.size() > 100 ? "table-sm " : "") + "table-hover table-responsive-sm table-striped table-intel table-listing\"><thead><tr class=\"bg-primary text-light\">" +
                            "<th scope=\"col\" class=\"filter\">#</th>" +
                            "<th scope=\"col\" class=\"filter\">IP</th>" +
                            "<th scope=\"col\" class=\"filter\">Country</th>" +
                            "<th scope=\"col\" class=\"filter\">IPSec PSKey</th>" +
                            "<th scope=\"col\" class=\"filter\">Clients</th>" +
                            "<th scope=\"col\" class=\"filter\">Limits</th>" +
                            "<th scope=\"col\" class=\"filter\">Strikes (<span id=filteredCountry></span>)</th>" +
                            "<th scope=\"col\" class=\"filter\">State</th>" +
                            "<th scope=\"col\" class=\"filter\">Provider</th>" +
                            "</tr></thead><tbody>");

                    for (Server server : servers) {
                        try {
                            out.print("<tr scope=\"row\"><td><a href=\"" + getServletConfig().getServletContext().getContextPath() + "/manage_server?id=" + server.getId() + "\">" +
                                    server.getId() + "</a></td><td>" +
                                    server.getIp_addr() + "</td><td>" +
                                    countries.stream().filter(stringStringPair -> stringStringPair.getKey().contentEquals(server.getCountry())).findFirst().orElse(new Pair<>("--", "None")).getValue() + " (" + server.getCountry() + ")</td><td>" +
                                    server.getIpSecPSK() + "</td><td>" +
                                    server.getClientsAmount() + "</td><td>" +
                                    server.getUsersLimit() + "</td><td>"
                            );

                            List<Strike> strikes = server.getStrikes();
                            for (Strike tStrike : strikes)
                                out.print("<div style=\"display: none;\" class=" + tStrike.getCountry() + "><strong class=\"" + (tStrike.getAmount() < 4 ? "text-secondary" : tStrike.getAmount() < 7 ? "text-warning" : "text-danger") + "\">" + tStrike.getAmount() + "</strong></div>");

                            out.print("</td><td>" +
                                    "<span class=\"text-" + (server.getState() == Server.State.RUNNING ? "success\">Running" : server.getState() == Server.State.DEPRECATED ? "danger\">Deprecated" : server.getState() == Server.State.SETTING_UP ? "warning\">Configuring VPN" : "secondary\"Not set up") + "</span></td>" +
                                    "<td>" + server.getProperty().substring(0, 3) +
                                    "</td></tr>");
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    out.println("</tbody></table>");
                } else {
                    out.println("<div class=\"alert alert-primary\" role=\"alert\">\n" +
                            "No servers deployed right now." +
                            "</div>");
                }
            %>
        </div>
    </div>
</div>

<script>
    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    });

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

    $('input[type="radio"]').on("click", DoSomething);

    function DoSomething() {
        $.each($('input[type="radio"]'), function (index, element) {
            if ($(element) !== $(this) && $(element).hasClass("sel-country")) {
                $(element).prop("checked", false);
                $.each(document.getElementsByClassName($(element).attr('id')), function (index, subElement) {
                    $(subElement).css("display", "none");
                });
            }
        });
        if ($(this).hasClass("sel-country")) {
            $(this).prop("checked", true);
            $.each(document.getElementsByClassName($(this).attr('id')), function (index, element) {
                $(element).css("display", "block");
            });
        }
        $('#filteredCountry').text($(this).attr('id'));
        $.each($('div[class="dropdown-filter-dropdown"]'), function (index, element) {
            $(element).remove();
        });
        $('#table').excelTableFilter();
    }

</script>
</body>
</html>
