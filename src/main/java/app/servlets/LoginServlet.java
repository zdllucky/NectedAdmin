package app.servlets;

import app.model.DbHandler;
import app.model.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        String password = req.getParameter("password");


        try {
            DbHandler.getInstance().validateLogin(username, password);
            HttpSession session = req.getSession(false);
            session.setAttribute("logon", true);

            Logger.getInstance().add("Admin platform logon", Logger.INFO, "Logged on as " + username);
            resp.sendRedirect((session.getAttribute("toPage") != null) ? (String) session.getAttribute("toPage") : "servers");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendRedirect("login");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (req.getSession(false) == null) req.getSession(true);
        if (req.getSession(false).getAttribute("logon") == null) {
            RequestDispatcher requestDispatcher = req.getRequestDispatcher("views/login.jsp");
            requestDispatcher.forward(req, resp);
        } else {
            resp.sendRedirect((req.getSession().getAttribute("toPage") != null) ? (String) req.getSession().getAttribute("toPage") : "servers");
        }
    }
}
