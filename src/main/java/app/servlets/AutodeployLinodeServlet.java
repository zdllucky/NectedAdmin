package app.servlets;

import app.entities.LinodeMarkup;
import app.model.DbHandler;
import app.model.LinodeInstanceDeployer;
import app.model.Logger;
import app.model.Model;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class AutodeployLinodeServlet extends HttpServlet {
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		if (req.getSession(false) == null) req.getSession(true);
		if (req.getSession(false).getAttribute("logon") == null) {
			req.getSession(false).setAttribute("toPage", req.getContextPath() + "/servers/autodeploy/linode");
			resp.sendRedirect("/login");
			return;
		}
		try {
			req.setAttribute("linode_autodeployment", Model.getInstance().getSystemConfigValue("linode_autodeployment"));
			req.setAttribute("default_country", Model.getInstance().getSystemConfigValue("default_country"));
			req.setAttribute("allowed_fulfillment", Model.getInstance().getSystemConfigValue("allowed_fulfillment"));
			req.setAttribute("countries", DbHandler.getInstance().getCountriesList("OP"));
			req.setAttribute("markups", DbHandler.getInstance().getLinodeMarkupList(false));
			req.setAttribute("servers", DbHandler.getInstance().getServersByInitiator("LIN"));
			req.getRequestDispatcher("/views/linode_autodeploy_configs.jsp").forward(req, resp);
		} catch (SQLException e) {
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error gathering linode autodeploy config data!");
			resp.sendRedirect("servers");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		//Logout action
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			req.getSession(false).setAttribute("toPage", "servers");
			req.getSession(false).removeAttribute("logon");
			resp.sendRedirect("login");
			return;
		}

		//If configs edition
		if (req.getParameter("settings") != null) {
			try {
				if (!Model.getInstance().getSystemConfigValue("default_country").contentEquals(req.getParameter("default_country"))) {
					Model.getInstance().updateConfigOption("default_country", req.getParameter("default_country"));
					Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Settings were applied successfully");
					resp.sendRedirect("linode");
					return;
				} else if (!req.getParameter("allowed_fulfillment").contentEquals(Model.getInstance().getSystemConfigValue("allowed_fulfillment"))) {
					Model.getInstance().updateConfigOption("allowed_fulfillment", req.getParameter("allowed_fulfillment"));
					Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Settings were applied successfully");
					resp.sendRedirect("linode");
					return;
				} else
					Model.getInstance().updateConfigOption("linode_autodeployment", req.getParameter("settings"));
			} catch (SQLException e) {
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Settings were not applied");
				resp.sendRedirect("linode");
				return;
			}
		}
		//If new markup is needed
		else if (req.getParameter("new_markup") != null) {
			try {
				DbHandler.getInstance().addLinodeMarkup(
						req.getParameter("new_markup_country"),
						req.getParameter("new_markup_instance_type"),
						Integer.parseInt(req.getParameter("new_markup_user_limit")),
						req.getParameter("new_markup_location_name"),
						req.getParameter("new_markup_enable").contentEquals("yes"));
				Logger.getInstance().add("New markup adding", Logger.INFO, "Country: " + req.getParameter("new_markup_country"));
				if (Model.getInstance().getSystemConfigValue("linode_autodeployment").contentEquals("ON") && req.getParameter("new_markup_enable").contentEquals("yes"))
					new Thread(new LinodeInstanceDeployer(req.getParameter("new_markup_country"))).start();
				Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Markup added successfully");
				resp.sendRedirect("linode");
				return;
			} catch (SQLException e) {
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error inserting markup");
				resp.sendRedirect("linode");
				return;
			}
		}
		//If markup edition is needed
		else if (req.getParameter("toggle") != null) {
			try {
				DbHandler.getInstance().toggleMarkup(Integer.parseInt(req.getParameter("toggle")));
				LinodeMarkup markup = DbHandler.getInstance().getMarkup(Integer.parseInt(req.getParameter("toggle")));
				if (Model.getInstance().getSystemConfigValue("linode_autodeployment").contentEquals("ON") && markup.isEnabled()) {
					new Thread(new LinodeInstanceDeployer(markup.getCountry())).start();
				}
				Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Markup toggled successfully");
				resp.sendRedirect("linode");
				return;
			} catch (SQLException e) {
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error toggling markup");
				resp.sendRedirect("linode");
				return;
			}
		}
		//If markup deletion is needed
		else if (req.getParameter("delete") != null) {
			try {
				DbHandler.getInstance().deleteMarkup(Integer.parseInt(req.getParameter("delete")));
				Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Markup was deleted successfully");
				resp.sendRedirect("linode");
				Logger.getInstance().add("Markup deletion", Logger.WARNING, "ID #" + req.getParameter("delete"));
				return;
			} catch (SQLException e) {
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error deleting markup");
				resp.sendRedirect("linode");
				return;
			}
		}
		//If markup creation is needed
		else if (req.getParameter("add") != null) {
			try {
				new Thread(new LinodeInstanceDeployer(Integer.parseInt(req.getParameter("add")))).start();
				Model.getInstance().outputMessage(req.getSession(false), Model.SUCCESS, "Instance creation task started (markup#" + req.getParameter("add") + ")");
				resp.sendRedirect("linode");
				return;
			} catch (SQLException e) {
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Instance creation task init error (markup#" + req.getParameter("add") + ")");
				resp.sendRedirect("linode");
				return;
			}
		}
		resp.sendRedirect("linode");
	}
}
