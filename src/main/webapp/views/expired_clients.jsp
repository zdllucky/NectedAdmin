<%@ page import="app.entities.Client" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Expired Clients List</title>
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
    List<Client> clients = (List<Client>) request.getAttribute("clients");
    int total_amount = (int) request.getAttribute("rows_amount");
    int pageNumber = (int) request.getAttribute("page_number");
    int by = (int) request.getAttribute("by");
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
                <li class="nav-item dropdown">
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
            <h1 class="display-4">Expired clients brief</h1>
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
                            <dd class="col-7" style=" margin-bottom: 15px;"><%%></dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Active
                                machines:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%%></dd>
                            <dt class="col-5"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px">Exposed
                                machines:
                            </dt>
                            <dd class="col-7"
                                style="border-top: 1px solid #e6e6e6; margin-bottom: 15px; padding-top: 15px"><%%></dd>
                            <dt class="col-5" style="border-top: 1px solid #e6e6e6; padding-top: 15px">Up users /
                                limit:
                            </dt>
                            <dd class="col-7" style="border-top: 1px solid #e6e6e6; padding-top: 15px"><%%></dd>
                        </dl>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header bg-warning h5">Load by countries</div>
                    <div class="card-body">
                        <%

                        %>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row mt-4 mx-1">
        <div class="col">
            <h1 class="display-4">Expired clients list</h1>
        </div>
        <div class="col">
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-end mt-4 mb-0">
                    <%
                        if (pageNumber > 3) out.print("<li class=\"page-item\">" +
                                "<a class=\"page-link\" href=\"" +
                                getServletConfig().getServletContext().getContextPath() + "?page=1&by=" + by + "\">1</a></li>");

                        if (pageNumber > 4)
                            out.print("<li class=\"disabled page-item\"><a class=\"page-link\" href=\"#\">...</a></li>");

                        int pages_amount = (total_amount / by) + (total_amount % by > 0 ? 1 : 0);

                        for (int i = Math.max(1, pageNumber - 2); i < Math.min(pages_amount, pageNumber + 2) + 1; i++)
                            out.println("<li  class=\"" + (i == pageNumber ? "disabled " : "") + "page-item\">" +
                                    "<a class=\"page-link\" href=\"" +
                                    getServletConfig().getServletContext().getContextPath() + "?page=" + i + "&by=" + by + "\">" +
                                    i + "</a></li>");

                        if (pageNumber < pages_amount - 3)
                            out.print("<li class=\"disabled page-item\"><a class=\"page-link\" href=\"#\">...</a></li>");

                        if (pageNumber < pages_amount - 2) out.print("<li class=\"page-item\">" +
                                "<a class=\"page-link\" href=\"" +
                                getServletConfig().getServletContext().getContextPath() + "?page=" + pages_amount + "&by=" + by + "\">" + pages_amount + "</a></li>");
                    %>
                </ul>
            </nav>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <%
                if (clients.size() > 0) {
                    out.println("<table id=\"table\" class=\"table " + (clients.size() > 100 ? "table-sm " : "") + "table-hover table-responsive-sm table-striped table-intel table-listing\"><thead><tr class=\"bg-primary text-light\">" +
                            "<th scope=\"col\" class=\"filter\">Id</th>" +
                            "<th scope=\"col\" class=\"filter\">Name</th>" +
                            "<th scope=\"col\" class=\"filter\">E-mail</th>" +
                            "<th scope=\"col\" class=\"filter\">Ref ID</th>" +
                            "<th scope=\"col\" class=\"filter\">Bonuses</th>" +
                            "<th scope=\"col\" class=\"filter\">Expired at</th>" +
                            "</tr></thead><tbody>");

                    for (Client client : clients) {
                        out.print("<tr scope=\"row\"><td><a href=\"" + getServletConfig().getServletContext().getContextPath() + "/manage_client?id=" + client.getId() + "\">" + client.getId() + "</a></td><td>" +
                                client.getClientName() + "</td><td>" +
                                client.getEmail() + "</td><td>" +
                                (client.getReferredFrom() != -1 ? "<a href=\"" + request.getContextPath() + "/manage_client?id=" + client.getReferredFrom() + "\">" + client.getReferredFrom() + "</a>" : "") + "</td><td>" +
                                client.getRefDays() + "</td><td>" +
                                new SimpleDateFormat("dd/MM/yyyy").format(client.getSubscrTo() != 0 ? client.getSubscrTo() * 1000L : client.getRegTime() * 1000L) + "</td></tr>");
                    }

                    out.println("</tbody></table>");
                } else {
                    out.println("<div class=\"alert alert-primary\" role=\"alert\">\n" +
                            "No clients found." +
                            "</div>");
                }
            %>
        </div>
    </div>
    <div class="row mx-1">
        <div class="col align-content-end">
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-end">
                    <%
                        if (pageNumber > 3) out.print("<li class=\"page-item\">" +
                                "<a class=\"page-link\" href=\"" +
                                getServletConfig().getServletContext().getContextPath() + "?page=1&by=" + by + "\">1</a></li>");

                        if (pageNumber > 4)
                            out.print("<li class=\"disabled page-item\"><a class=\"page-link\" href=\"#\">...</a></li>");

                        for (int i = Math.max(1, pageNumber - 2); i < Math.min(pages_amount, pageNumber + 2) + 1; i++)
                            out.println("<li  class=\"" + (i == pageNumber ? "disabled " : "") + "page-item\">" +
                                    "<a class=\"page-link\" href=\"" +
                                    getServletConfig().getServletContext().getContextPath() + "?page=" + i + "&by=" + by + "\">" +
                                    i + "</a></li>");

                        if (pageNumber < pages_amount - 3)
                            out.print("<li class=\"disabled page-item\"><a class=\"page-link\" href=\"#\">...</a></li>");

                        if (pageNumber < pages_amount - 2) out.print("<li class=\"page-item\">" +
                                "<a class=\"page-link\" href=\"" +
                                getServletConfig().getServletContext().getContextPath() + "?page=" + pages_amount + "&by=" + by + "\">" + pages_amount + "</a></li>");
                    %>
                </ul>
            </nav>
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
