package app.servlets;

import app.entities.MailingTemplate;
import app.model.DbHandler;
import app.model.Logger;
import app.model.Model;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.SkipPageException;
import java.io.IOException;

public class ManageMailingServlet extends HttpServlet {
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		HttpSession session = req.getSession();
		//If new mailing creation
		if (req.getServletPath().startsWith("/mailing/new")) {
			//Authorization check
			if (session.getAttribute("logon") == null) {
				session.setAttribute("toPage", "/mailing/new");
				resp.sendRedirect(req.getContextPath() + "/login");
				return;
			}
			session.removeAttribute("template");
			req.getRequestDispatcher("/views/manage_mailing.jsp").forward(req, resp);
		}
		//If editing existing mailing
		else if (req.getServletPath().startsWith("/mailing/manage") && req.getParameter("id") != null) {
			//Authorization check
			if (session.getAttribute("logon") == null) {
				session.setAttribute("toPage", "/mailing/manage?id=" + req.getParameter("id"));
				resp.sendRedirect(req.getContextPath() + "/login");
				return;
			}
			try {
				MailingTemplate template = DbHandler.getInstance().getMailingTemplate(Integer.parseInt(req.getParameter("id")));
				if (template != null)
					req.setAttribute("mailing_template", template);
				else throw new SkipPageException("Page not found");
				req.getRequestDispatcher("/views/manage_mailing.jsp").forward(req, resp);
			} catch (Exception e) {
				Model.getInstance().outputMessage(session, Model.DANGER, "Error loading page: " + req.getRequestURL() + req.getQueryString() + ". Reason: " + e.toString());
				resp.sendRedirect(req.getContextPath() + "/mailing");
			}
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		HttpSession session = req.getSession();


		//Authorization check
		if (session.getAttribute("logon") == null) {
			session.setAttribute("toPage", req.getContextPath() + "/mailing");
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		try {
			String action = "" + req.getParameter("action");
			//If new mailing creation
			int id;
			switch (action) {
				case "add":
					String type = req.getParameter("type");
					int newTemplateId;
					//If adding personal template
					if (!type.equals("-1")) {
						newTemplateId = DbHandler.getInstance().addPersonalMailingTemplate(
								type,
								req.getParameter("state") != null,
								Long.parseLong(req.getParameter("time_to_trig")),
								req.getParameter("sql_approval"),
								req.getParameter("subject_ru"),
								req.getParameter("subject_en"),
								req.getParameter("body_ru"),
								req.getParameter("body_en"),
								req.getParameter("label"));
					}
					//If adding mass mail template
					else {
						newTemplateId = DbHandler.getInstance().addMassMailingTemplate(
								req.getParameter("subject_ru"),
								req.getParameter("subject_en"),
								req.getParameter("body_ru"),
								req.getParameter("body_en"),
								req.getParameter("label"));
					}
					Logger.getInstance().add("Mailing template creation", Logger.INFO, "ID#" + newTemplateId);
					Model.getInstance().outputMessage(session, Model.SUCCESS, "Mailing template #" + newTemplateId + " was created successfully!");
					resp.sendRedirect(req.getContextPath() + "/mailing/manage?id=" + newTemplateId);
					break;
				//If deletion needed
				case "delete":
					id = Integer.parseInt(req.getParameter("id"));
					DbHandler.getInstance().removeMailingTemplate(id);
					Logger.getInstance().add("Mailing template removal", Logger.WARNING, "ID#" + id);
					Model.getInstance().outputMessage(session, Model.WARNING, "Mailing template #" + id + " was removed from the system!");
					resp.sendRedirect(req.getContextPath() + "/mailing");
					break;
				//If edition needed
				case "edit":
					MailingTemplate template = DbHandler.getInstance().getMailingTemplate(Integer.parseInt(req.getParameter("id")));
					id = Integer.parseInt(req.getParameter("id"));

					if (template.isPersonal()) {
						DbHandler.getInstance().editPersonalMailingTemplate(
								id,
								req.getParameter("state") != null,
								Long.parseLong(req.getParameter("time_to_trig")),
								req.getParameter("sql_approval"),
								req.getParameter("subject_ru"),
								req.getParameter("subject_en"),
								req.getParameter("body_ru"),
								req.getParameter("body_en"),
								req.getParameter("label"));
					} else {
						DbHandler.getInstance().editMassMailingTemplate(
								id,
								req.getParameter("subject_ru"),
								req.getParameter("subject_en"),
								req.getParameter("body_ru"),
								req.getParameter("body_en"),
								req.getParameter("label"));
					}
					Logger.getInstance().add("Mailing template editing", Logger.WARNING, "ID#" + template.getId());
					Model.getInstance().outputMessage(session, Model.SUCCESS, "Mailing template #" + template.getId() + " was updated!");
					resp.sendRedirect(req.getContextPath() + "/mailing/manage?id=" + template.getId());
					break;
				default:
					throw new NullPointerException("Action not found");
			}
		} catch (Exception e) {
			Model.getInstance().outputMessage(session, Model.DANGER, "Action error! Reason: " + e.toString());
			resp.sendRedirect(req.getContextPath() + "/mailing/new");
		}
	}
}
