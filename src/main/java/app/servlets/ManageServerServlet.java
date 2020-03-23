package app.servlets;

import app.entities.Server;
import app.model.DbHandler;
import app.model.Logger;
import app.model.Model;
import kong.unirest.Unirest;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

public class ManageServerServlet extends HttpServlet {
	private Server server;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		//Logon check
		if (req.getSession(false) == null) req.getSession(true);
		if (req.getSession(false).getAttribute("logon") == null) {
			req.getSession(false).setAttribute("toPage", "manage_server?id=" + req.getParameter("id"));
			resp.sendRedirect("login");
			return;
		}

		//Parsing page
		try {
			server = DbHandler.getInstance().getServer(Integer.parseInt(req.getParameter("id")));
			RequestDispatcher requestDispatcher = req.getRequestDispatcher("views/manage_server.jsp");
			req.setAttribute("server", server);
			req.setAttribute("countries", DbHandler.getInstance().getCountriesList("OP"));
			req.setAttribute("clients", DbHandler.getInstance().getActiveClientList(Integer.parseInt(req.getParameter("id"))));
			requestDispatcher.forward(req, resp);
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(false), "danger", "Error displaying server#" + Integer.parseInt(req.getParameter("id")));
			resp.sendRedirect("servers");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		//Logout action
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			req.getSession(false).setAttribute("toPage", "manage_server?id=" + req.getParameter("id"));
			req.getSession(false).removeAttribute("logon");
			resp.sendRedirect("login");
			return;
		}

		try {
			server = DbHandler.getInstance().getServer(Integer.parseInt(req.getParameter("id")));
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(false), "danger", "Error retrieving server information!");
			resp.sendRedirect("manage_server?id=" + server.getId());
		}

		//If delete action
		boolean delete = req.getParameterValues("delete_post") != null;
		if (delete) {
			try {
				if ((server.getProperty().startsWith("LIN")) && Unirest.get("https://api.linode.com/v4/linode/instances/" + server.getProperty().substring(3, server.getProperty().indexOf("-")))
						.header("Authorization", "Bearer c6a65776e7ee252e26dc74a7411b2cacfd398bd490b64dbd449b0305bea534cd")
						.asJson().isSuccess())
					Unirest.delete("https://api.linode.com/v4/linode/instances/" + server.getProperty().substring(3, server.getProperty().indexOf("-")))
							.header("Authorization", "Bearer c6a65776e7ee252e26dc74a7411b2cacfd398bd490b64dbd449b0305bea534cd")
							.asEmpty();
				DbHandler.getInstance().removeServer(server.getId());
				Model.getInstance().outputMessage(req.getSession(false), "warning", "Server#" + server.getId() + " removal started!");
				resp.sendRedirect("servers");
				Logger.getInstance().add("Server removal", Logger.WARNING, "Server #" + server.getId() + "|" + server.getCountry());
			} catch (SQLException | SocketTimeoutException e) {
				Model.getInstance().outputMessage(req.getSession(false), "danger", "Error removing server from the system!");
				resp.sendRedirect("manage_server?id=" + server.getId());
			}
		} else {
			int state = Integer.parseInt(req.getParameter("deprecate_post"));
			String ip_addr = req.getParameter("ip_addr_post");
			String conn = req.getParameter("conn_post");
			String country = req.getParameter("country_post");
			String ipsecpsk = req.getParameter("ipsecpsk_post");
			int users_limit = Integer.parseInt(req.getParameter("users_limit_post"));
			String exServerData = server.getState() + "|" + server.getIp_addr() + "|" + server.getConn() + "|" + server.getCountry() + "|" + server.getIpSecPSK() + "|" + server.getUsersLimit();

			try {
				DbHandler.getInstance().editServer(server.getId(),
						ip_addr.contentEquals(server.getIp_addr()) ? null : ip_addr,
						conn.contentEquals(server.getConn()) ? null : conn,
						country.contentEquals(server.getCountry()) ? null : country,
						ipsecpsk.contentEquals(server.getIpSecPSK()) ? null : ipsecpsk,
						users_limit == server.getUsersLimit() ? null : users_limit,
						Server.State.parse(state));
				Model.getInstance().outputMessage(req.getSession(false), "success", "Successfully updated server settings!");
				resp.sendRedirect("manage_server?id=" + server.getId());
				Logger.getInstance().add("Server data edition", Logger.WARNING, "Ex server #" + server.getId() + " data: " + exServerData);
			} catch (SQLException e) {
				Model.getInstance().outputMessage(req.getSession(false), "danger", "Error updating settings!");
				resp.sendRedirect("manage_server?id=" + server.getId());
			}
		}


	}
}
