<%@ page import="app.entities.LinodeMarkup" %>
<%@ page import="app.entities.Pair" %>
<%@ page import="app.entities.Server" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Linode autodeployment configuration</title>

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
        button.remove, button.remove:hover, button.remove:focus {
            margin-left: 1rem;
            text-decoration: none;
        }

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
<%
    List<Pair<String, String>> countries = (List<Pair<String, String>>) request.getAttribute("countries");
    List<LinodeMarkup> markups = (List<LinodeMarkup>) request.getAttribute("markups");
    List<Server> servers = (List<Server>) request.getAttribute("servers");
    Set<String> involvedCountries = markups.stream().filter(LinodeMarkup::isEnabled).map(LinodeMarkup::getCountry).collect(Collectors.toSet());
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
            <h1 class="display-4">Linode Autodeployment</h1>
        </div>
    </div>
    <div class="row mb-4">
        <div class="col">
            <div class="card-group mb-3">
                <div class="card ">
                    <div class="card-header bg-warning h5">Configuration overview</div>
                    <div class="card-body">
                        <dl class="row px-3">
                            <dt class="col-5" style=" margin-bottom: 15px;">Servers amount:</dt>
                            <dd class="col-7" style=" margin-bottom: 15px;"><%=servers.size()%>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Countries
                                configured:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%=involvedCountries.size()%>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Countries:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%=String.join(", ", involvedCountries)%>
                            </dd>
                        </dl>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header bg-warning h5">Deployment Settings</div>
                    <div class="card-body">
                        <form method="POST" id="settings" hidden><input id="setting_mark" type="hidden" name="settings"
                                                                        value="true"></form>
                        <form method="POST" id="newMarkup" hidden><input type="hidden" name="new_markup" value="true">
                        </form>
                        <dl class="row px-3">
                            <dt class="col-5" style=" margin-bottom: 15px;">Autodepl. switcher:</dt>
                            <dd class="col-7" style=" margin-bottom: 15px;">
                                <div class="custom-control custom-switch">
                                    <input id="linodeAutodeploymentSwitch" type="checkbox" form="settings"
                                           onclick="$('#setting_mark').attr('value',$('#linodeAutodeploymentSwitch').is(':checked') ? 'ON' : 'OFF');$('#settings').submit();"
                                           class="custom-control-input"
                                           name="linode_autodeployment" <%=((String) request.getAttribute("linode_autodeployment")).contentEquals("ON") ? "checked" : ""%>>
                                    <label class="custom-control-label" for="linodeAutodeploymentSwitch"></label>
                                </div>
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Per
                                country limit(%):
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <input type="number" form="settings"
                                       onblur="if($(this).attr('value') !== '<%=request.getAttribute("allowed_fulfillment")%>'){$('#settings').submit();}"
                                       class="form-control form-control-sm" name="allowed_fulfillment"
                                       value="<%=request.getAttribute("allowed_fulfillment")%>">
                            </dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Default
                                country:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">
                                <select class="custom-select custom-select-sm" form="settings"
                                        onchange="document.getElementById('settings').submit();" name="default_country">
                                    <option value="--">None</option>
                                    <%
                                        for (String country : involvedCountries)
                                            out.print("<option " +
                                                    (((String) request.getAttribute("default_country")).contentEquals(country) ? "selected" : "") +
                                                    " value=\"" + country + "\">" +
                                                    countries.stream().filter(stringStringPair -> stringStringPair.getKey().contentEquals(country)).findFirst().orElse(new Pair<>("--", "None")).getValue() + " (" + country + ")</option>");
                                    %>
                                </select>
                            </dd>
                        </dl>
                        <input type="checkbox" id="toggle-input" value="selected">
                        <label class="toggle-label text-primary" for="toggle-input">Add new markup </label>
                        <div class="toggle-content">
                            <div class="module">
                                <div class="input-group mb-3">
                                    <div class="input-group-prepend">
                                        <label for="country" class="input-group-text">Markup country</label>
                                    </div>
                                    <select required name="new_markup_country" class="custom-select" id="country"
                                            form="newMarkup">
                                        <%
                                            for (Pair<String, String> country : countries)
                                                out.print("<option value=\"" + country.getKey() + "\">" + country.getValue() + " (" + country.getKey() + ")</option>");
                                        %>
                                    </select>
                                </div>
                                <div class="input-group mb-3">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text">Instance type</span>
                                    </div>
                                    <input required type="text" class="form-control" form="newMarkup"
                                           name="new_markup_instance_type">
                                </div>
                                <div class="input-group mb-3">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text">User limit</span>
                                    </div>
                                    <input required type="number" class="form-control" form="newMarkup"
                                           name="new_markup_user_limit">
                                </div>
                                <div class="input-group mb-3">
                                    <div class="input-group-prepend">
                                        <span class="input-group-text">Location name</span>
                                    </div>
                                    <input required type="text" class="form-control" form="newMarkup"
                                           name="new_markup_location_name">
                                </div>
                                <div class="input-group mb-3">
                                    <div class="input-group-prepend">
                                        <label for="enable" class="input-group-text">Enable markup</label>
                                    </div>
                                    <select required class="form-control" form="newMarkup" id="enable"
                                            name="new_markup_enable">
                                        <option selected value="no">No</option>
                                        <option value="yes">Yes</option>
                                    </select>
                                    <div class="input-group-append">
                                        <button type="submit" class="btn btn-primary" form="newMarkup">Add Markup
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
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
        <div class="col-12">
            <form method="POST" id="editMarkup" hidden><input type="hidden" id="markupAction"></form>
            <%
                if (!markups.isEmpty()) {
                    out.println("<table id=\"table\" class=\"mb-5 table " + (servers.size() > 100 ? "table-sm " : "") + "table-hover table-responsive-sm table-striped table-intel table-listing\"><thead><tr class=\"bg-primary text-light\">" +
                            "<th scope=\"col\" class=\"filter\">#</th>" +
                            "<th scope=\"col\" class=\"filter\">Instance type</th>" +
                            "<th scope=\"col\" class=\"filter\">Country</th>" +
                            "<th scope=\"col\" class=\"filter\">Location name</th>" +
                            "<th scope=\"col\" class=\"filter\">User limit</th>" +
                            "<th scope=\"col\" class=\"filter\">Opts</th>" +
                            "</tr></thead><tbody>");

                    for (LinodeMarkup linodeMarkup : markups) {
                        out.print("<tr scope=\"row\"><td>" + linodeMarkup.getId() + "</td><td>" +
                                linodeMarkup.getInstanceType() + "</td><td>" +
                                countries.stream().filter(stringStringPair -> stringStringPair.getKey().contentEquals(linodeMarkup.getCountry())).findFirst().orElse(new Pair<>("--", "None")).getValue() + " (" + linodeMarkup.getCountry() + ")</td><td>" +
                                linodeMarkup.getLocationName() + "</td><td>" +
                                linodeMarkup.getUserLimit() + "</td><td>" +
                                "<div class=\"custom-control custom-switch d-inline\">" +
                                "<input " + (linodeMarkup.isEnabled() ? "checked" : "") + " id=\"on" + linodeMarkup.getId() + "\" type=\"checkbox\" form=\"settings\" onclick=\"$('#markupAction').attr('name', 'toggle').attr('value', '" + linodeMarkup.getId() + "');$('#editMarkup').submit();\" class=\"custom-control-input\">" +
                                "<label class=\"custom-control-label text-hide\" for=\"on" + linodeMarkup.getId() + "\">" + (linodeMarkup.isEnabled() ? "On" : "Off") + "</label></div>" +
                                "<button class=\"remove btn p-0 btn-link\" href=\"#\" onclick=\"$('#markupAction').attr('name', 'add').attr('value', '" + linodeMarkup.getId() + "');$('#editMarkup').submit();\">➕</button>" +
                                "<button class=\"remove btn p-0 btn-link\" href=\"#\" onclick=\"$('#markupAction').attr('name', 'delete').attr('value', '" + linodeMarkup.getId() + "');$('#editMarkup').submit();\">❌</button>" +
                                "</td></tr>"
                        );
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
