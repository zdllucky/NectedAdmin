package app.servlets;

import app.entities.Pair;
import app.entities.Server;
import app.entities.Strike;
import app.model.DbHandler;
import app.model.Model;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class ServersServlet extends HttpServlet {
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		//Logout action
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			req.getSession(false).setAttribute("toPage", "servers");
			req.getSession(false).removeAttribute("logon");
			resp.sendRedirect("login");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		if (req.getSession(false) == null) req.getSession(true);
		if (req.getSession(false).getAttribute("logon") == null) {
			req.getSession(false).setAttribute("toPage", "servers");
			resp.sendRedirect("login");
			return;
		}
		List<Server> servers;
		try {
			servers = DbHandler.getInstance().getServerList();
			servers.sort(Comparator.comparing(Server::getState).reversed().thenComparing(Server::getAddDate));
			List<Pair<String, String>> countries = DbHandler.getInstance().getCountriesList("OP");
			List<Strike> allStrikes = DbHandler.getInstance().getStrikes();
			req.setAttribute("strikes", allStrikes);
			req.setAttribute("countries", countries);
			req.setAttribute("servers", servers);
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Database error");
			e.printStackTrace();
		}

		RequestDispatcher requestDispatcher = req.getRequestDispatcher("views/servers.jsp");
		requestDispatcher.forward(req, resp);
	}
}
