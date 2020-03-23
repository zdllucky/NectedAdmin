<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ru">
<%
    HashMap<String, String> configs = (HashMap<String, String>) request.getAttribute("config_options");
    List<List<String>> queryResult = (List<List<String>>) request.getAttribute("query_result");
%>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Platform settings</title>
    <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-bundle.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.bundle.min.js"
            integrity="sha384-xrRywqdh3PHs8keKZN+8zzc5TX0GRTLCcmivcbNJWm2rs5C8PRhcEn3czEjhAO9o"
            crossorigin="anonymous"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/codemirror.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/sql.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/matchbrackets.js"></script>
    <link rel="stylesheet" href="https://vpnnected.com/src/bootstrap.min.css">
    <link rel="stylesheet" href="https://vpnnected.com/src/CodeMirror_files/codemirror.css">
    <link rel="stylesheet" href="https://vpnnected.com/src/excel-bootstrap-table-filter-style.css">


    <style>
        .CodeMirror {
            height: auto !important;
        }

        textarea.queryValue {
            background: transparent;
            border: none;
            width: 100% !important;
        }

        <%if (request.getSession().getAttribute("query_logs") != null) {%>
        thead th {
            font-size: 0 !important;
        }

        tbody td {
            font-size: xx-small;
        }


        thead th div {
            font-size: xx-small !important;
        }

        <%}%>
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
                        <a class="dropdown-item" href="${pageContext.request.contextPath}/servers/autodeploy/linode">Linode
                            autodeployment</a>
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
                <li class="nav-item active">
                    <a class="nav-link font-weight-bold" style="color: #ff9340;"
                       href="${pageContext.request.contextPath}/settings">
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
    <form method="POST" id="config_edit" hidden></form>
    <div class="row">
        <div class="col-12 my-3">
            <%
                if (session != null
                        && session.getAttribute("shout") != null) {
            %>
            <div class="alert h6 alert-<%=((String) session.getAttribute("shout")).startsWith("2") ? "success" : "danger"%>"
                 role="alert">
                <%=((String) session.getAttribute("shout")).substring(3)%>
            </div>
            <%
                    session.removeAttribute("shout");
                }%>
            <div class="display-4">
                Configs List
            </div>
        </div>
        <div class="col-12 mb-5">
            <div class="card ">
                <div class="card-header bg-warning h5">System options</div>
                <div class="card-body pb-0">
                    <form class="container" method="POST">
                        <input type="hidden" id="operation" name="operation" value="no">
                        <div class="form-group row">
                            <label for="options" class="col-auto align-self-center mb-3 ">Select option:</label>
                            <select id="options" class="custom-select bg-light ml-3 mb-3 w-auto col-auto" name="option"
                                    onchange="$(valueField).val(''); $.each(configs, function (key, value) {if (value.K === $('#options').children('option:selected').val()) { $(valueField).val(value.V); initialValue = value.V}});"></select>
                            <input id="valueField" type="text" class="col form-control bg-dark text-warning ml-3 mb-3"
                                   name="value"
                                   onblur="if (initialValue !== $(this).val()) {$('#blocker').css('display', 'block'); $('#operation').val('config'); submit();}">
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <div class="col-12">
            <div class="display-4 pb-3">
                Databases console
            </div>
            <form method="POST">
                <div class="custom-control custom-radio">
                    <input <%=request.getSession().getAttribute("query_logs") == null ? "checked " : " "%>type="radio"
                           id="radioDB" name="query_source" value="db" class="custom-control-input">
                    <label class="custom-control-label" for="radioDB">Query main database</label>
                </div>
                <div class="custom-control custom-radio">
                    <input <%=request.getSession().getAttribute("query_logs") != null ? "checked " : " "%>type="radio"
                           id="radioLog" name="query_source" value="logs" class="custom-control-input">
                    <label class="custom-control-label" for="radioLog">Query log database</label>
                </div>
                <br>
                <script>
                </script>
                <textarea id="code" name="sql_query"><%
                    if (request.getSession().getAttribute("prev_sql_query") != null) {
                        out.print(((String) session.getAttribute("prev_sql_query")).replace("<", "&lt;"));
                        session.removeAttribute("prev_sql_query");
                    }
                %></textarea>
                <button class="btn btn-primary my-3" type="submit">Execute SQL query</button>
                <%if (queryResult != null) {%>
                <table id="table"
                       class="table <%=queryResult.size() > 10 ? "table-sm " : ""%>table-hover table-responsive-sm table-striped table-intel table-listing">
                    <thead>
                    <tr class="bg-primary text-light">
                        <%for (String tString : queryResult.get(0)) {%>
                        <th scope="col" class="filter small"><%=tString%>
                        </th>
                        <%
                            }
                            queryResult.remove(0);
                        %>
                    </tr>
                    </thead>
                    <tbody>
                    <%for (List<String> tList : queryResult) {%>
                    <tr scope="row">
                        <%for (String tString : tList) {%>
                        <td><%=Pattern.compile("[<>]").matcher(tString).find() ? "<textarea rows=\"1\" contenteditable=\"false\" autocomplete=\"off\" autocapitalize=\"off\" spellcheck=\"false\" readonly size=\"0\" class=\"queryValue\">" + tString + "</textarea>" : tString%>
                        </td>
                        <%}%>
                    </tr>
                    <%}%>
                    </tbody>
                </table>
                <%}%>
            </form>
        </div>
    </div>
</div>
<script>
    const configs = [<%for (Map.Entry<String, String> tPair : configs.entrySet()) {%>{
        "K": "<%=tPair.getKey()%>",
        "V": "<%=tPair.getValue()%>"
    }, <%}%>{"K": "last", "V": "last"}];
    configs.length--;
    var initialValue = '';
    const option = $("<option/>", {
        "value": '',
        "html": "Option",
        "disabled": '',
        "selected": ''
    });
    const options = [option];
    var valueField = $("#valueField");
    $.each(configs, function (key, value) {
        options.push(option.clone().val(value.K).html(value.K).removeAttr("disabled").removeAttr("selected"));
    });
    $("#options").html(options);

    window.onload = function () {
        const mime = 'text/x-mariadb';
        window.editor = CodeMirror.fromTextArea(document.getElementById('code'), {
            mode: mime,
            indentWithTabs: true,
            smartIndent: true,
            lineNumbers: true,
            matchBrackets: true,
            autofocus: true,
            extraKeys: {"Ctrl-Space": "autocomplete"},
            hintOptions: {
                tables: {
                    users: ["name", "score", "birthDate"],
                    countries: ["name", "population", "size"]
                }
            }
        });
        $('#table').excelTableFilter();
        clock();
    };


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

    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    });
</script>
</body>
</html>
