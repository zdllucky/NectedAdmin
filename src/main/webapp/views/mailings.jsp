<%@ page import="app.entities.MailingTask" %>
<%@ page import="app.entities.MailingTemplate" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.TimeZone" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Mailing Management</title>

    <!-- Load the plugin bundle. -->
    <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-bundle.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.bundle.min.js"
            integrity="sha384-xrRywqdh3PHs8keKZN+8zzc5TX0GRTLCcmivcbNJWm2rs5C8PRhcEn3czEjhAO9o"
            crossorigin="anonymous"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/codemirror.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/sql.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/matchbrackets.js"></script>

    <link rel="stylesheet" href="https://vpnnected.com/src/bootstrap.min.css">
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
    <link rel="stylesheet" href="https://vpnnected.com/src/CodeMirror_files/codemirror.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <style>
        .CodeMirror {
            height: auto !important;
            flex: 1 1 auto;
        }

        textarea.queryValue {
            background: transparent;
            border: none;
            width: 100% !important;
        }

        button.remove, button.remove:hover, button.remove:focus {
            text-decoration: none;
        }
    </style>
</head>
<body>
<div id="loader" class="loader">
    <div class="spinner-border m-auto text-primary" style="width: 5rem; height: 5rem;"></div>
</div>

<%
    List<MailingTemplate> templates = (List<MailingTemplate>) request.getAttribute("mailing_templates");
    List<MailingTask> tasks = (List<MailingTask>) request.getAttribute("mailing_tasks");

    int tTotalAmount = (int) request.getAttribute("t_total_amount");
    int tPage = (int) request.getAttribute("t_page");
    int tBy = (int) request.getAttribute("t_by");

    int mTotalAmount = (int) request.getAttribute("m_total_amount");
    int mPage = (int) request.getAttribute("m_page");
    int mBy = (int) request.getAttribute("m_by");

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
                <li class="nav-item"></li>
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
            <h1 class="display-4">Mailing templates list</h1>
        </div>
    </div>
    <div class="row mb-4">
        <div class="col">
            <div class="card-group mb-3">
                <div class="card ">
                    <div class="card-header bg-warning h5">Overview</div>
                    <div class="card-body">
                        <table class="table w-100 table-hover table-responsive-md mb-0">
                            <thead>
                            <tr>
                                <th scope="col">#</th>
                                <th scope="col">Label</th>
                                <th scope="col">Type</th>
                                <th scope="col">Trigger log name</th>
                                <th scope="col">Credentials</th>
                                <th scope="col">Time to trig</th>
                                <th scope="col" class="text-right pr-md-5">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <%for (MailingTemplate t : templates) {%>
                            <tr>
                                <th scope="row"><%=t.getId()%>
                                </th>
                                <td><%=t.getLabel()%>
                                </td>
                                <td><%=t.isPersonal() ? (t.isInstant() ? "Instant" : "Personal") : "Mass"%>
                                </td>
                                <td><%=t.isPersonal() ? t.getTriggerLogName() : ""%>
                                </td>
                                <td><%=t.getCredentials()%>
                                </td>
                                <td><%=t.isPersonal() ? t.getTimeToTrig() : ""%>
                                </td>
                                <td class="text-right">
                                    <form method="post" id="template<%=t.getId()%>" hidden><input type="hidden"
                                                                                                  name="id"
                                                                                                  value="<%=t.getId()%>"
                                                                                                  hidden></form>
                                    <%if (t.isPersonal()) {%>
                                    <div class="custom-control custom-switch d-inline-block">
                                        <input <%=t.isEnabled() ? "checked" : ""%> type="checkbox"
                                                                                   form="template<%=t.getId()%>"
                                                                                   id="t<%=t.getId()%>"
                                                                                   onclick="toggleSwitch('template<%=t.getId()%>', <%=t.isEnabled() ? "true" : "false"%>)"
                                                                                   class="custom-control-input">
                                        <label class="custom-control-label text-hide" for="t<%=t.getId()%>"></label>
                                    </div>
                                    <%} else {%>
                                    <button class="btn btn-sm text-decoration-none btn-link" type="button"
                                            value="<%=t.getId()%>" data-toggle="modal"
                                            data-target="#launchMailingDialog">➕
                                    </button>
                                    <%}%>
                                    <a class="btn btn-sm text-decoration-none btn-link"
                                       href="${pageContext.request.contextPath}/mailing/manage?id=<%=t.getId()%>">&#9998;</a>
                                    <button class="btn btn-sm remove btn-link" type="submit"
                                            form="template<%=t.getId()%>" name="action" value="removeTemplate">❌
                                    </button>
                                </td>
                            </tr>
                            <%}%>
                            </tbody>
                        </table>
                        <nav aria-label="Navigation" class="mt-3">
                            <ul class="pagination justify-content-center pagination-sm">
                                <%if (mPage > 1) {%>
                                <li class="page-item">
                                    <a class="page-link"
                                       href="${pageContext.request.contextPath}/mailing?mp=<%=mPage - 1%>&mby=<%=mBy%>&tp=<%=tPage%>&tby=<%=tBy%>"
                                       aria-label="Previous">
                                        <span aria-hidden="true">&laquo;</span>
                                    </a>
                                </li>
                                <%}%>
                                <li class="page-item active" aria-current="page"><span class="page-link"><%=mPage%>
                                    <span class="sr-only">(current)</span>
                                </span></li>
                                <%if (mTotalAmount > Math.abs(mPage * mBy)) {%>
                                <li class="page-item">
                                    <a class="page-link"
                                       href="${pageContext.request.contextPath}/mailing?mp=<%=mPage + 1%>&mby=<%=mBy%>&tp=<%=tPage%>&tby=<%=tBy%>"
                                       aria-label="Next">
                                        <span aria-hidden="true">&raquo;</span>
                                    </a>
                                </li>
                                <%}%>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!-- Modal -->
    <div class="modal fade" id="launchMailingDialog" tabindex="-1" role="dialog"
         aria-labelledby="launchMailingDialogTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="launchMailingDialogTitle"></h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <form id="newMailing" method="POST" hidden><input type="hidden" hidden id="newMailingId" name="id">
                    </form>
                    <div class="input-group mb-3">
                        <div class="input-group-prepend">
                            <label for="SQLCode" class="input-group-text text-left d-inline-block">SQL selection
                                <span style="
    display: block;
    font-size: smaller;
    font-family: monospace;
    margin: 0;
    color: brown;
    background: white;
    padding: .3rem .5rem;
    border-radius: 0.3rem;
    border: 1px solid #ccc;
">SELECT name, email, lang <br>FROM clients WHERE...</span>
                            </label>
                        </div>
                        <textarea class="form-control" id="SQLCode" name="sql_selection" form="newMailing"></textarea>
                    </div>
                    <div class="input-group">
                        <div class="input-group-prepend">
                            <label for="dateTime" class="input-group-text">Mailing time (UTC)</label>
                        </div>
                        <input id="dateTime" type="datetime-local" class="form-control" name="activation_time"
                               form="newMailing">
                    </div>
                    <script>
                        const db = "text/x-mariadb";
                        let codeMirror = null;
                        $('#launchMailingDialog').on('show.bs.modal', function (event) {
                            const button = $(event.relatedTarget);
                            const id = Number(button.val());
                            $('#newMailingId').val(id);
                            $('#launchMailingDialogTitle').text("Scheduling #" + id + " template mass mailing...");
                            $('#SQLCode').html("");

                            if (codeMirror == null) {
                                codeMirror = CodeMirror.fromTextArea(document.getElementById('SQLCode'), {
                                    mode: db,
                                    indentWithTabs: true,
                                    smartIndent: true,
                                    lineNumbers: false,
                                    lineWrapping: true,
                                    matchBrackets: true,
                                    showCursorWhenSelecting: true,
                                    autofocus: true,
                                    autocorrect: true,
                                    extraKeys: {"Ctrl-Space": "autocomplete"},
                                    hintOptions: {
                                        tables: {
                                            users: ["name", "score", "birthDate"],
                                            countries: ["name", "population", "size"]
                                        }
                                    }
                                });
                            } else {
                                codeMirror.setValue("");
                                codeMirror.refresh();
                                codeMirror.focus();
                            }

                        });
                    </script>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-dismiss="modal">Cancel</button>
                    <button type="submit" form="newMailing" name="action" value="launchMassMailing"
                            class="btn btn-primary">Add mailing to schedule
                    </button>
                </div>
            </div>
        </div>
    </div>
    <div class="row mt-4 mx-1">
        <div class="col">
            <h1 class="display-4">Mailing schedule</h1>
        </div>
    </div>
    <div class="row mx-1">
        <div class="col align-content-end">
            <nav aria-label="Navigation" class="mt-3">
                <ul class="pagination justify-content-center pagination-sm">
                    <%if (tPage > 1) {%>
                    <li class="page-item">
                        <a class="page-link"
                           href="${pageContext.request.contextPath}/mailing?tp=<%=tPage - 1%>&tby=<%=tBy%>&mp=<%=mPage%>&mby=<%=mBy%>"
                           aria-label="Previous">
                            <span aria-hidden="true">&laquo;</span>
                        </a>
                    </li>
                    <%}%>
                    <li class="page-item active" aria-current="page"><span class="page-link"><%=tPage%>
                                    <span class="sr-only">(current)</span>
                                </span></li>
                    <%if (tTotalAmount > Math.abs(tPage * tBy)) {%>
                    <li class="page-item">
                        <a class="page-link"
                           href="${pageContext.request.contextPath}/mailing?tp=<%=tPage + 1%>&tby=<%=tBy%>&mp=<%=mPage%>&mby=<%=mBy%>"
                           aria-label="Next">
                            <span aria-hidden="true">&raquo;</span>
                        </a>
                    </li>
                    <%}%>
                </ul>
            </nav>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <%if (tasks.isEmpty()) {%>
            <div class="alert alert-primary" role="alert">No upcoming tasks found now.</div>
            <%} else {%>
            <form method="POST" id="cancelMailingTask" hidden><input type="hidden" name="action"
                                                                     value="cancelMailingTask" hidden></form>
            <table id="table" class="table table-hover table-responsive-sm table-striped table-intel table-listing">
                <thead>
                <tr class="bg-primary text-light">
                    <th scope="col" class="filter">#</th>
                    <th scope="col" class="filter">Template label</th>
                    <th scope="col" class="filter">&amp; id</th>
                    <th scope="col" class="filter">Time</th>
                    <th scope="col" class="filter">User</th>
                    <td class="text-right pr-4 font-weight-bold">Actions</td>
                </tr>
                </thead>
                <tbody>
                <%for (MailingTask t : tasks) {%>
                <tr>
                    <td scope="row"><%=t.getId()%>
                    </td>
                    <td><%=t.getTemplate().getLabel()%>
                    </td>
                    <td>
                        <a href="${pageContext.request.contextPath}/mailing/manage?id=<%=t.getTemplate().getId()%>"><%=t.getTemplate().getId()%>
                        </a></td>
                    <td><%=dateFormat.format(t.getActivationTime())%>
                    </td>
                    <td><%=t.getTemplate().isPersonal() ? t.getSelection() : "<textarea rows=\"1\" contenteditable=\"false\" autocomplete=\"off\" autocapitalize=\"off\" spellcheck=\"false\" readonly size=\"0\" class=\"queryValue\">" + t.getSelection() + "</textarea>"%>
                    </td>
                    <td class="text-right pr-5">
                        <button class="btn btn-link text-decoration-none" type="submit" form="cancelMailingTask"
                                name="id" value="<%=t.getId()%>">❌
                        </button>
                    </td>
                </tr>
                <%}%>
                </tbody>
            </table>
            <%}%>
        </div>
    </div>
    <div class="row mx-1">
        <div class="col align-content-end">
            <nav aria-label="Navigation" class="mt-3">
                <ul class="pagination justify-content-center pagination-sm">
                    <%if (tPage > 1) {%>
                    <li class="page-item">
                        <a class="page-link"
                           href="${pageContext.request.contextPath}/mailing?tp=<%=tPage - 1%>&tby=<%=tBy%>&mp=<%=mPage%>&mby=<%=mBy%>"
                           aria-label="Previous">
                            <span aria-hidden="true">&laquo;</span>
                        </a>
                    </li>
                    <%}%>
                    <li class="page-item active" aria-current="page"><span class="page-link"><%=tPage%>
                                    <span class="sr-only">(current)</span>
                                </span></li>
                    <%if (tTotalAmount > Math.abs(tPage * tBy)) {%>
                    <li class="page-item">
                        <a class="page-link"
                           href="${pageContext.request.contextPath}/mailing?tp=<%=tPage + 1%>&tby=<%=tBy%>&mp=<%=mPage%>&mby=<%=mBy%>"
                           aria-label="Next">
                            <span aria-hidden="true">&raquo;</span>
                        </a>
                    </li>
                    <%}%>
                </ul>
            </nav>
        </div>
    </div>
</div>

<script>
    function toggleSwitch(formName, state) {
        let form = $('form#' + formName);
        form.append('<input type="hidden" hidden name="action" value="' + (state ? 'disable' : 'enable') + '">').submit();
    }

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
