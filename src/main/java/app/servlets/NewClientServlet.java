package app.servlets;

import app.entities.Client;
import app.entities.NoServerFoundException;
import app.entities.Server;
import app.model.DbHandler;
import app.model.Logger;
import app.model.Model;
import com.jcraft.jsch.JSchException;
import io.ipinfo.api.errors.RateLimitedException;
import org.apache.commons.lang3.RandomStringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class NewClientServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        //Check logon
        if (req.getSession(false) == null) req.getSession(true);
        if (req.getSession(false).getAttribute("logon") == null) {
            req.getSession(false).setAttribute("toPage", "newclient");
            resp.sendRedirect("login");
            return;
        }

        try {
            req.setAttribute("ip_response", Model.getInstance().getIpData(!req.getServerName().contains("local") ? req.getRemoteAddr() : "124.64.18.135"));
            req.setAttribute("countries_list", DbHandler.getInstance().getCountriesList("OP"));
            req.setAttribute("countries", new ArrayList<>(new HashSet<>(DbHandler.getInstance().getServerList().stream().filter(server -> server.getState() == Server.State.RUNNING).map(Server::getCountry).collect(Collectors.toList()))));
        } catch (SQLException | RateLimitedException e) {
            e.printStackTrace();
            Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error preparing new client page!");
            resp.sendRedirect("clients");
            return;

        }
        RequestDispatcher requestDispatcher = req.getRequestDispatcher("/views/newclient.jsp");
        requestDispatcher.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        //Logout action
        boolean logout = req.getParameterValues("logout") != null;
        if (logout) {
            req.getSession(false).setAttribute("toPage", "newclient");
            req.getSession(false).removeAttribute("logon");
            resp.sendRedirect("login");
            return;
        }

        String email = req.getParameter("email").toLowerCase().trim();
        String name = req.getParameter("client_name");
        //long telNumber = Long.parseLong(req.getParameter("telephone"));
        long subs_days = (req.getParameter("sups_days").isBlank() ? 0 : Integer.parseInt(req.getParameter("sups_days")));
        long registerTime = System.currentTimeMillis() / 1000L;

        //Checking Email
        try {
            DbHandler.getInstance().checkEmail(email);
        } catch (SQLException e) {
            e.printStackTrace();
            Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "This E-mail seems to be a fakemail / or already exists!");
            resp.sendRedirect("newclient");
            return;
        }

        int id;
        //Inserting client
        try {
            id = DbHandler.getInstance().insertClient(name, email, registerTime, Client.NO_REFERRER_VALUE);
        } catch (SQLException e) {
            e.printStackTrace();
            Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error inserting client");
            resp.sendRedirect("newclient");
            return;
        }

        //Initialising client
        try {
            DbHandler.getInstance().initialiseClient(
                    id,
                    email.substring(0, email.indexOf("@")) + id,
                    RandomStringUtils.randomAlphanumeric(8, 12),
                    "en");
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                DbHandler.getInstance().removeClient(id);
            } catch (SQLException ex) {
                ex.printStackTrace();
                Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Critical error when deleting inserted user");
                resp.sendRedirect("newclient");
                return;
            }
            Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error initialising client");
            resp.sendRedirect("newclient");
            return;
        }

        //Setting client subscription
        if (subs_days > 0) {
            try {
                DbHandler.getInstance().setPlan(id, subs_days, 2, req.getParameter("countryFrom"), req.getParameter("countryTo"));
                Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Client was added successfully, subscription activated!");
                resp.sendRedirect("clients");
            } catch (SQLException | JSchException | NoServerFoundException | InterruptedException e) {
                try {
                    DbHandler.getInstance().removeClient(id);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Critical error when deleting inserted user");
                    resp.sendRedirect("newclient");
                    return;
                }
                Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error setting client subscription");
                resp.sendRedirect("newclient");
            }
        } else {
            Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Client was added successfully!");
            resp.sendRedirect("expired_clients");
        }
        Logger.getInstance().add("Client creation", id, Logger.INFO, name + " (" + email + ")");
    }
}
