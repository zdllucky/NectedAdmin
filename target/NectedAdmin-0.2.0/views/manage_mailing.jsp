<%@ page import="app.entities.MailingTemplate" %>
<%@ page import="app.model.EmailSender" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%
    MailingTemplate template = (MailingTemplate) request.getAttribute("mailing_template");
    boolean isEditing = template != null;
%>
<html>
<head>
    <title><%=!isEditing ? "Add new mailing template" : "Edit template #" + request.getParameter("id")%>
    </title>

    <!-- Load the plugin bundle. -->
    <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-bundle.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.bundle.min.js"
            integrity="sha384-xrRywqdh3PHs8keKZN+8zzc5TX0GRTLCcmivcbNJWm2rs5C8PRhcEn3czEjhAO9o"
            crossorigin="anonymous"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/codemirror.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/sql.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/xml.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/javascript.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/css.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/htmlmixed.js"></script>
    <script src="https://vpnnected.com/src/CodeMirror_files/matchbrackets.js"></script>
    <link rel="stylesheet" href="https://vpnnected.com/src/bootstrap.min.css">
    <link rel="stylesheet" href="https://vpnnected.com/src/CodeMirror_files/codemirror.css">
    <link rel="stylesheet" href="https://vpnnected.com/src/excel-bootstrap-table-filter-style.css">
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/js/excel-bootstrap-table-filter-style.css">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <style>
        .CodeMirror {
            height: auto !important;
            flex: 1 1 auto;
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
            out.println("<div class=\"row mt-4 justify-content-center\"><div class=\"col-12\"><div class=\"alert alert-" + session.getAttribute("result") + " alert-dismissible fade show\" role=\"alert\">\n" +
                    session.getAttribute("shout") +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\">\n" +
                    "    <span aria-hidden=\"true\">&times;</span>\n" +
                    "  </button>\n" +
                    "</div></div></div>");
            session.removeAttribute("shout");
            session.removeAttribute("result");
        }
        Set<String> credentials = EmailSender.CREDENTIALS.keySet().stream().map(s -> s.substring(0, s.length() - 3)).collect(Collectors.toSet());
    %>
    <div class="row mt-4 justify-content-center">
        <div class="col-12">
            <h1 class="display-4"><%=!isEditing ? "Add new mailing template" : "Edit template #" + request.getParameter("id")%>
            </h1>
        </div>
    </div>
    <form method="post" class="row my-4">
        <%=isEditing ? "<input name=\"id\" value=\"" + template.getId() + "\" hidden>" : ""%>
        <div class="input-group input-group-sm mb-3 col-12 col-lg-4 pr-lg-1">
            <div class="input-group-prepend">
                <label for="mailingType" class="input-group-text">Mailing type</label>
            </div>
            <select id="mailingType" name="type" class="custom-select" required>
                <option selected value="-1">Mass</option>
                <option value="0">Personal</option>
                <option value="-2">Instant</option>
            </select>
        </div>
        <div class="input-group input-group-sm mb-3 col-12 col-lg-4 pr-lg-1">
            <div class="input-group-prepend">
                <label class="input-group-text" for="label">Template label</label>
            </div>
            <input id="label" name="label" type="text" class="form-control" required="required"
                   value="<%=isEditing ? template.getLabel() : ""%>">
        </div>
        <div class="input-group input-group-sm mb-3 col-12 col-lg-4">
            <div class="input-group-prepend">
                <label class="input-group-text" for="triggerLogName">Log name</label>
            </div>
            <input id="triggerLogName" name="type" type="text" class="form-control" disabled>
        </div>
        <div class="input-group input-group-sm mb-3 col-12 col-lg-4 pr-lg-1">
            <div class="input-group-prepend">
                <label class="input-group-text" for="timeToTrig">Mailing delay (sec.)</label>
            </div>
            <input id="timeToTrig" name="time_to_trig" type="number" class="form-control" disabled>
        </div>
        <div class="input-group input-group-sm mb-3 col-12 col-lg-4 pr-lg-1">
            <div class="input-group-prepend">
                <label class="input-group-text" for="credentials">Credentials</label>
            </div>
            <select id="credentials" name="credentials" type="text" class="form-control">
                <%for (String tCred : credentials) {%>
                <option value="<%=tCred%>" <%=isEditing && tCred.equals(template.getCredentials()) ? "selected" : ""%>><%=tCred%></option>
                <%}%>
            </select>
        </div>
        <div class="input-group input-group-sm mb-3 col-12 col-lg-4 pt-1">
            <div class="custom-control custom-switch">
                <input type="checkbox" class="custom-control-input" id="state" name="state"
                       disabled <%=isEditing && template.isEnabled() ? "checked" : ""%>>
                <label class="custom-control-label" for="state">Enable</label>
            </div>
        </div>
        <div class="input-group mb-3 mt-0 col-12" style="margin-top:-.5rem" id="approvalContainer">
            <div class="input-group-prepend">
                <label for="SQLCode" class="input-group-text">SQL approval<br>query</label>
            </div>
            <textarea id="SQLCode" class="form-control"
                      name="sql_approval"><%=isEditing && template.isPersonal() ? template.getSQLApproval() : ""%></textarea>
        </div>
        <!--
                <div class="input-group mb-3 col-12 col-lg-2 pt-1">
                    <div class="custom-control custom-switch">
                        <input type="checkbox" class="custom-control-input" id="addCoupon" name="coupon" disabled>
                        <label class="custom-control-label" for="addCoupon">Provide coupon</label>
                    </div>
                </div>
                <div class="input-group mb-3 col-12 col-lg-5">
                    <div class="input-group-prepend">
                        <label for="couponType" class="input-group-text">Coupon type</label>
                    </div>
                    <select id="couponType" name="coupon_type" class="custom-select" disabled>
                        <option selected value="1">Bonus days (%)</option>
                        <option value="2">Exact days (num.)</option>
                    </select>
                </div>
                <div class="input-group mb-3 col-12 col-lg-5">
                    <div class="input-group-prepend">
                        <label class="input-group-text" for="couponValue">Type value</label>
                    </div>
                    <input id="couponValue" name="coupon_value" type="number" class="form-control" disabled>
                </div>
        -->
        <div class="input-group mb-3 col-12">
            <div class="input-group-prepend">
                <label class="input-group-text" for="subjectRu">Subject</label>
            </div>
            <input id="subjectRu" name="subject_ru" type="text" class="form-control" placeholder="На русском"
                   value="<%=isEditing ? template.getSubject("ru") : ""%>">
            <input id="subjectEn" name="subject_en" type="text" class="form-control" placeholder="In english"
                   value="<%=isEditing ? template.getSubject("en") : ""%>">
        </div>
        <div class="col-12 my-2">
            <h1 class="h1 font-weight-light">Template body</h1>
        </div>
        <div class="input-group mb-3 col-12 col-lg-6" id="bodyContainer">
            <textarea id="bodyRu" name="body_ru" type="text" class="form-control"
                      placeholder="На русском"><%=isEditing ? template.getBody("ru") : ""%></textarea>
        </div>
        <div class="input-group mb-3 col-12 col-lg-6">
            <textarea id="bodyEn" name="body_en" type="text" class="form-control"
                      placeholder="In english"><%=isEditing ? template.getBody("en") : ""%></textarea>
        </div>
        <div class="input-group mb-3 col-auto">
            <button class="btn btn-primary" name="action"
                    value="<%=request.getParameter("id") == null ? "add" : "edit"%>"
                    type="submit"><%=request.getParameter("id") == null ? "Add template" : "Save changes"%>
            </button>
            <%if (isEditing) {%>
            <button class="ml-2 btn btn-outline-danger" name="action" value="delete" type="submit">Delete template
            </button>
            <%}%>
        </div>
    </form>
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
        const mailingType = $('#mailingType');
        const triggerLogName = $('#triggerLogName');
        const timeToTrig = $('#timeToTrig');
        const state = $('#state');
        //const addCoupon = $('#addCoupon');
        //const couponType = $('#couponType');
        //const couponValue = $('#couponValue');
        const db = "text/x-mariadb";
        const html = "text/html";

        // Apply the plugin
        const editor = CodeMirror.fromTextArea(document.getElementById('SQLCode'), {
            mode: db,
            indentWithTabs: true,
            smartIndent: true,
            lineNumbers: true,
            lineWrapping: true,
            matchBrackets: true,
            placeholder: "Leave empty to make template instant*",
            showCursorWhenSelecting: true,
            readOnly: true,
            autofocus: false,
            autocorrect: true,
            extraKeys: {"Ctrl-Space": "autocomplete"},
            hintOptions: {
                tables: {
                    users: ["name", "score", "birthDate"],
                    countries: ["name", "population", "size"]
                }
            }
        });

        CodeMirror.fromTextArea(document.getElementById("bodyRu"), {
            lineNumbers: true,
            mode: html,
            matchBrackets: true,
            lineWrapping: true,
            showCursorWhenSelecting: true,
            spellcheck: true,
            autocorrect: true
        });
        CodeMirror.fromTextArea(document.getElementById("bodyEn"), {
            lineNumbers: true,
            mode: html,
            matchBrackets: true,
            lineWrapping: true,
            showCursorWhenSelecting: true,
            spellcheck: true,
            autocorrect: true
        });
        const codeMirror = $('#approvalContainer .CodeMirror').css("background", "#e9ecef");
        $('#table').excelTableFilter();
        clock();
        <%if (!isEditing) {%>
        mailingType.on("change", function () {
            if ($("option:selected").val() === "0") {
                mailingType.removeAttr("name");
                triggerLogName.attr("required", true).removeAttr("disabled").attr("name", "type");
                timeToTrig.attr("required", true).removeAttr("disabled");
                state.removeAttr("disabled").prop("checked", false);
                //addCoupon.removeAttr("disabled").prop("checked", false);
                //coupon(false);
                editor.setOption("readOnly", false);
                editor.refresh();
                codeMirror.css("background", "#ffffff");
            } else if ($("option:selected").val() === "-1") {
                mailingType.attr("name", "type");
                triggerLogName.removeAttr("required").attr("disabled", true).removeAttr("name");
                timeToTrig.removeAttr("required").attr("disabled", true);
                state.attr("disabled", true).prop("checked", false);
                //addCoupon.attr("disabled", true).prop("checked", false);
                //coupon(false);
                editor.setOption("readOnly", true);
                editor.refresh();
                codeMirror.css("background", "#e9ecef");
            } else if ($("option:selected").val() === "-2") {
                mailingType.removeAttr("name");
                triggerLogName.attr("required", true).removeAttr("disabled").attr("name", "type");
                timeToTrig.attr("required", true).removeAttr("disabled");
                state.removeAttr("disabled").prop("checked", false);
                //addCoupon.removeAttr("disabled").prop("checked", false);
                //coupon(false);
                editor.setOption("readOnly", true);
                editor.refresh();
                codeMirror.css("background", "#e9ecef");
            }
        });
        /*
        addCoupon.change(function () {
            coupon(this.checked);
        });
        function coupon(proc) {
            if (proc) {
                couponType.removeAttr("disabled");
                couponValue.removeAttr("disabled");
            } else {
                couponType.attr("disabled", true);
                couponValue.attr("disabled", true);
            }
        }*/
        <%} else {
            if (template.isPersonal()) {%>
        mailingType.attr("disabled", true).val('0');
        triggerLogName.val("<%=template.getTriggerLogName()%>");
        timeToTrig.removeAttr("disabled").val(<%=template.getTimeToTrig()%>);
        state.removeAttr("disabled");
        editor.setOption("readOnly", false);
        editor.refresh();
        codeMirror.css("background", "#ffffff");
        <%} else {%>
        mailingType.attr("disabled", true).val(<%= template.isPersonal() ? "0" : "-1"%>);
        <%}
    }%>
    });


    $('form[method="post"]').on("submit", function () {
        $('#loader').addClass("d-flex");
    });

    function hidePsk(status) {
        document.getElementById('psk').disabled = status.checked === true;
    }

</script>
</body>
</html>
