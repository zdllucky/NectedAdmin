package app.servlets;

import app.model.DbHandler;
import app.model.Logger;
import app.model.Model;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

public class PlatformSettingsServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		req.setCharacterEncoding("UTF-8");
		if (req.getSession().getAttribute("logon") != null) {
			req.setAttribute("config_options", Model.getInstance().getSystemConfigsList());
			req.getRequestDispatcher("/views/platform_settings.jsp").forward(req, resp);
		} else {
			req.getSession().setAttribute("toPage", req.getContextPath() + "/settings");
			resp.sendRedirect(req.getContextPath() + "/login");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		req.setCharacterEncoding("UTF-8");
		HttpSession session = req.getSession();

		//Authorization check
		if (req.getSession().getAttribute("logon") == null) {
			session.setAttribute("toPage", req.getContextPath() + "/settings");
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		//Logout action
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			session.setAttribute("toPage", req.getContextPath() + "/settings");
			session.removeAttribute("logon");
			resp.sendRedirect(req.getContextPath() + "/login");
		}
		//if config-edition request
		else if (req.getParameter("operation") != null && req.getParameter("operation").equals("config")) {
			try {
				Model.getInstance().updateConfigOption(req.getParameter("option"), req.getParameter("value"));
				req.getSession().setAttribute("shout", "200Config updated");
			} catch (SQLException e) {
				StringWriter stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));
				req.getSession().setAttribute("shout", "400" + stringWriter.toString());
			}
			resp.sendRedirect("settings");
		}
		//If Db Query
		else if (req.getParameter("sql_query") != null) {
			try {
				req.getSession().setAttribute("prev_sql_query", req.getParameter("sql_query"));
				if (req.getParameter("query_source").equals("db")) {
					req.getSession().removeAttribute("query_logs");
					req.setAttribute("query_result", DbHandler.getInstance().executeRawQuery(req.getParameter("sql_query")));
				} else {
					req.getSession().setAttribute("query_logs", "true");
					req.setAttribute("query_result", Logger.getInstance().executeRawQuery(req.getParameter("sql_query")));
				}
				req.setAttribute("config_options", Model.getInstance().getSystemConfigsList());
				req.getSession().setAttribute("shout", "200SQL queried successfully");
				if (!req.getParameter("sql_query").trim().toLowerCase().startsWith("select")) {
					Logger.getInstance().add((req.getParameter("query_source").equals("db") ? "Main" : "Logs") + " database edition", Logger.INFO, req.getParameter("sql_query"));
				}

				req.getRequestDispatcher("/views/platform_settings.jsp").forward(req, resp);
			} catch (SQLException e) {
				StringWriter stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));
				req.getSession().setAttribute("shout", "400" + stringWriter.toString());
				resp.sendRedirect("settings");
			}
		}
	}
}
