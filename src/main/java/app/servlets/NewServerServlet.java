package app.servlets;

import app.entities.Server;
import app.model.*;
import com.jcraft.jsch.JSchException;
import io.ipinfo.api.errors.RateLimitedException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class NewServerServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		//Logon check
		if (req.getSession(false) == null) req.getSession(true);
		if (req.getSession(false).getAttribute("logon") == null) {
			req.getSession(false).setAttribute("toPage", "newserver");
			resp.sendRedirect("login");
			return;
		}
		try {
			req.setAttribute("countries_list", DbHandler.getInstance().getCountriesList("OP"));
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(), Model.DANGER, "Error preparing page");
			resp.sendRedirect("/servers");
		}
		RequestDispatcher requestDispatcher = req.getRequestDispatcher("views/newserver.jsp");
		requestDispatcher.forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		//Logout action
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			req.getSession(false).setAttribute("toPage", "newserver");
			req.getSession(false).removeAttribute("logon");
			resp.sendRedirect("login");
			return;
		}

		String ip_addr = req.getParameter("ip_addr");
		String conn = req.getParameter("conn");
		String ipsecpsk = req.getParameter("ipsecpsk");
		int usersLimit = (req.getParameter("users_limit").isBlank() ? 20 : Integer.parseInt(req.getParameter("users_limit")));
		boolean ifSetupRequired = req.getParameterValues("auto_setup") != null;
		int serverId;
		String country;

		//Resolving IP location
		try {
			country = (req.getParameter("country").contentEquals("OP") ? Model.getInstance().getIpData(ip_addr).getCountryCode() : req.getParameter("country"));
		} catch (RateLimitedException e) {
			e.printStackTrace();
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error resolving IP location! Please select server location manually.");
			resp.sendRedirect("newserver");
			return;
		}

		//Checking existence
		List<Server> servers;
		try {
			servers = DbHandler.getInstance().getServerList();
			if (servers != null) {
				for (Server server : servers) {
					if (server.getIp_addr().contentEquals(ip_addr)) {
						Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "IP <a href=\"/manage_server?id=" + server.getId() + "\">" + ip_addr + "</a> already exists! ");
						resp.sendRedirect("newserver");
						return;
					}
				}
			} else {
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error loading servers database.");
				resp.sendRedirect("newserver");
				return;
			}
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error loading servers database.");
			resp.sendRedirect("newserver");
			return;
		}


		//Checking connectivity
		try {
			new SSHConnector(ip_addr, conn).checkConnection();
		} catch (JSchException e) {
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Can not establish SSH connection with a server");
			resp.sendRedirect("newserver");
			return;
		}

		//Server is valid, adding to DB
		try {
			serverId = DbHandler.getInstance().insertServer(
					ip_addr,
					conn,
					ifSetupRequired ? "" : ipsecpsk,
					System.currentTimeMillis() / 1000L,
					usersLimit,
					country,
					ifSetupRequired ? Server.State.NOT_SET_UP : Server.State.RUNNING,
					"IND");

			Model.getInstance().outputMessage(req.getSession(false), ifSetupRequired ? Model.WARNING : Model.SUCCESS, "Server was added successfully!" + (ifSetupRequired ? " Started server VPN configuration..." : ""));
			resp.sendRedirect("manage_server?id=" + serverId);
			Logger.getInstance().add("New server data added", Logger.INFO, "ID#" + serverId + ", country: " + country);
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Database error during server insertion");
			resp.sendRedirect("newserver");
			return;
		}

		//Setting up VPN if needed
		if (ifSetupRequired) {
			try {
				ServerConfigurer VPNConfigurer = new ServerConfigurer(DbHandler.getInstance().getServer(serverId));
				new Thread(VPNConfigurer).start();
			} catch (SQLException e) {
				e.printStackTrace();
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Database error during VPN configuration");
				resp.sendRedirect("newserver");
			}
		}
	}
}
