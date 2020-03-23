package app.servlets;

import app.entities.Client;
import app.entities.NoServerFoundException;
import app.entities.Server;
import app.model.DbHandler;
import app.model.Logger;
import app.model.Model;
import com.jcraft.jsch.JSchException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ManageClientServlet extends HttpServlet {
	private Client client;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		//Logon check
		HttpSession session = req.getSession();
		if (session.getAttribute("logon") == null) {
			session.setAttribute("toPage", "manage_client?id=" + req.getParameter("id"));
			resp.sendRedirect("login");
			return;
		}

		//Gathering request attribute data
		RequestDispatcher requestDispatcher = req.getRequestDispatcher("views/manage_client.jsp");
		try {
			client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));
			req.setAttribute("country_list", DbHandler.getInstance().getCountriesList("OP"));
			req.setAttribute("client", client);
			req.setAttribute("payments", DbHandler.getInstance().getClientPayments(client.getId()));
			req.setAttribute("countries", new ArrayList<>(new HashSet<>(DbHandler.getInstance().getServerList().stream().filter(server -> server.getState() == Server.State.RUNNING).map(Server::getCountry).collect(Collectors.toList()))));
			requestDispatcher.forward(req, resp);
		} catch (SQLException e) {
			e.printStackTrace();
			Model.getInstance().outputMessage(session, Model.DANGER, "Error gathering user data!");
			resp.sendRedirect("clients");
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		HttpSession session = req.getSession();

		//Login check
		if (session.getAttribute("logon") == null) {
			session.setAttribute("toPage", "manage_client?id=" + req.getParameter("id"));
			resp.sendRedirect("login");
			return;
		}

		//Logout action
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			session.setAttribute("toPage", "manage_client?id=" + req.getParameter("id"));
			session.removeAttribute("logon");
			resp.sendRedirect("login");
			return;
		}

		//User settings edition request
		// if user setup to be deleted
		boolean delete = req.getParameterValues("delete_post") != null;
		if (delete) {
			try {
				DbHandler.getInstance().removeClient(client.getId());
				Model.getInstance().outputMessage(session, Model.SUCCESS, "Client#" + client.getId() + " was totally removed from the system!");
				Logger.getInstance().add("Client removal", client.getId(), Logger.WARNING, client.getClientName() + " (" + client.getEmail() + ")");
				if (client.getServer() != null)
					resp.sendRedirect("clients");
				else
					resp.sendRedirect("expired_clients");
				return;
			} catch (SQLException | IOException e) {
				Model.getInstance().outputMessage(session, Model.DANGER, "Error removing client, SSH connection or database error!");
				resp.sendRedirect("manage_client?id=" + client.getId());
				return;
			}
		} else {
			//Preparing editting data
			boolean inform = req.getParameter("inform_post") != null;
			boolean clear_attempts = req.getParameter("clear_attempts_post") != null;
			String email = (req.getParameter("email_post").contentEquals(client.getEmail()) ? null : req.getParameter("email_post"));
			String clientName = (req.getParameter("clientname_post").contentEquals(client.getClientName()) ? null : req.getParameter("clientname_post"));
			Integer clientRefDays = Integer.parseInt(req.getParameter("ref_days_post")) == client.getRefDays() ? null : Integer.parseInt(req.getParameter("ref_days_post"));
			Integer clientReferredFrom = Integer.parseInt(req.getParameter("referred_from_post")) == client.getReferredFrom() ? null : Integer.parseInt(req.getParameter("referred_from_post"));
			String clientLang = (req.getParameter("lang_post").contentEquals(client.getLang()) ? null : req.getParameter("lang_post"));

			int subs_days = (!req.getParameter("vpn_days_post").isBlank() ?
					(int) (Integer.parseInt(req.getParameter("vpn_days_post")) - ((client.getSubscrTo() - System.currentTimeMillis() / 1000L) / 24 / 3600)) : -1);
			String countryFrom = req.getParameter("country_from_post");
			String countryTo = req.getParameter("country_to_post");

			//Updating subscription info if needed
			if ((subs_days != 0 || !countryTo.contentEquals("--")) && subs_days != -1) {
				try {
					String exSubsInfo = (subs_days == 0 ? "" : client.getSubscrTo() + "|") + (client.getServer() != null ? client.getServer().getId() + "|" + client.getServer().getCountry() : "No server");
					DbHandler.getInstance().setPlan(client.getId(), subs_days, 2, countryFrom, countryTo);
					Model.getInstance().outputMessage(session, Model.SUCCESS, "Subscription updated!");
					Server server = DbHandler.getInstance().getClient(client.getId()).getServer();
					Logger.getInstance().add("Client subscription edition", client.getId(), Logger.WARNING, "New: " + subs_days + "|" + (server != null ? server.getId() + "|" + server.getCountry() : "No server") + ", ex: " + exSubsInfo);
				} catch (SQLException | JSchException | NoServerFoundException | InterruptedException e) {
					Model.getInstance().outputMessage(session, Model.DANGER, "Some settings were not applied, please check!");
					resp.sendRedirect("manage_client?id=" + +client.getId());
					return;
				}
			}

			//Updating user info
			String temp = session.getAttribute("shout") != null ? (String) session.getAttribute("shout") : "";
			if (clear_attempts || email != null || clientName != null || clientRefDays != null || clientReferredFrom != null || clientLang != null)
				try {
					String oldData = client.getEmail() + "|" + client.getClientName() + "|" + client.getRefDays() + "|" + client.getReferredFrom() + "|" + client.getLang();
					DbHandler.getInstance().editClient(client.getId(),
							clear_attempts,
							email,
							clientName,
							clientRefDays,
							clientReferredFrom,
							clientLang);
					Model.getInstance().outputMessage(
							session,
							Model.SUCCESS,
							temp + " Personal information updated");

					Logger.getInstance().add("Client data edition", client.getId(), Logger.WARNING, "Old data: " + oldData);
				} catch (SQLException e) {
					Model.getInstance().outputMessage(session, Model.DANGER,
							temp + " Some settings were not applied, please check!");
					resp.sendRedirect("manage_client?id=" + +client.getId());
					return;
				}
		}
		resp.sendRedirect("manage_client?id=" + +client.getId());
	}
}
