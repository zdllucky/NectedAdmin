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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.List;
import java.util.TimeZone;

public class MailingsServlet extends HttpServlet {
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		HttpSession session = req.getSession();
		//Authorization check
		if (session.getAttribute("logon") == null) {
			session.setAttribute("toPage", req.getContextPath() + "/mailing");
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}

		try {
			int mPage = req.getParameter("mp") != null ? Integer.parseInt(req.getParameter("mp")) : 1;
			int mBy = req.getParameter("mby") != null ? Integer.parseInt(req.getParameter("mby")) : 20;
			List<MailingTemplate> wholeTemplates = DbHandler.getInstance().getMailingTemplateList(false);
			int mTotalAmount = wholeTemplates.size();
			List<MailingTemplate> templates = DbHandler.getInstance().getMailingTemplateList(mPage, mBy);


			int tPage = req.getParameter("tp") != null ? Integer.parseInt(req.getParameter("tp")) : 1;
			int tBy = req.getParameter("tby") != null ? Integer.parseInt(req.getParameter("tby")) : 50;
			int tTotalAmount = DbHandler.getInstance().getTotalMailingTasksAmount();

			req.setAttribute("m_page", mPage);
			req.setAttribute("m_by", mBy);
			req.setAttribute("m_total_amount", mTotalAmount);
			req.setAttribute("t_page", tPage);
			req.setAttribute("t_by", tBy);
			req.setAttribute("t_total_amount", tTotalAmount);
			req.setAttribute("mailing_templates", templates);
			req.setAttribute("mailing_tasks", DbHandler.getInstance().getMailingTasksList(tPage, tBy, wholeTemplates));

			req.getRequestDispatcher("/views/mailings.jsp").forward(req, resp);
		} catch (Exception e) {
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			Model.getInstance().outputMessage(session, Model.DANGER, "Error loading page: " + req.getRequestURL() + (req.getQueryString() != null ? req.getQueryString() : "") + ". Reason: " + stringWriter.toString());
			resp.sendRedirect(req.getContextPath() + "/servers");
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		HttpSession session = req.getSession();
		String action = "" + req.getParameter("action");
		//Authorization check
		if (session.getAttribute("logon") == null) {
			session.setAttribute("toPage", req.getContextPath() + "/mailing");
			resp.sendRedirect(req.getContextPath() + "/login");
			return;
		}
		try {
			int id;
			switch (action) {
				case "enable":
					id = Integer.parseInt(req.getParameter("id"));
					if (DbHandler.getInstance().toggleMailingServlet(id, true) != id)
						throw new NullPointerException("Could not find template to enable, template #" + id);
					Model.getInstance().outputMessage(
							session, Model.INFO, "Personal template #" + id + " enabled successfully!");
					Logger.getInstance().add("Personal mailing enabling", Logger.INFO, "Personal template ID#" + id);
					break;
				case "disable":
					id = Integer.parseInt(req.getParameter("id"));
					if (DbHandler.getInstance().toggleMailingServlet(id, false) != id)
						throw new NullPointerException("Could not find template to disable, template #" + id);
					Model.getInstance().outputMessage(session, Model.INFO, "Personal template #" + id + " disabled successfully!");
					Logger.getInstance().add("Personal mailing disabling", Logger.INFO, "Personal template ID#" + id);
					break;
				case "removeTemplate":
					id = Integer.parseInt(req.getParameter("id"));
					if (DbHandler.getInstance().removeMailingTemplate(id) != id)
						throw new NullPointerException("Could not find template to remove, template #" + id);
					Model.getInstance().outputMessage(session, Model.WARNING, "Template #" + id + " was removed successfully!");
					Logger.getInstance().add("Template removal", Logger.WARNING, "Template ID#" + id);
					break;
				case "launchMassMailing":
					id = Integer.parseInt(req.getParameter("id"));
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
					dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
					long time = dateFormat.parse(req.getParameter("activation_time")).getTime();
					if (System.currentTimeMillis() > time)
						throw new DateTimeException("Selected time is too late to schedule");
					int taskId = DbHandler.getInstance().scheduleMassMailing(id, time, req.getParameter("sql_selection"));
					Model.getInstance().outputMessage(session, Model.SUCCESS, "Template #" + id + " was added to schedule successfully!");
					Logger.getInstance().add("Mailing task launch", Logger.INFO, "Task ID#" + taskId + ", template ID#" + id);
					break;
				case "cancelMailingTask":
					id = Integer.parseInt(req.getParameter("id"));
					if (DbHandler.getInstance().removeMailingTask(id) > 0) {
						Model.getInstance().outputMessage(session, Model.WARNING, "Task #" + id + " was removed from schedule!");
						Logger.getInstance().add("Mailing task removal", Logger.WARNING, "ID#" + id);
					} else
						Model.getInstance().outputMessage(session, Model.DANGER, "Template #" + id + " was not found!");
				default:
					throw new NullPointerException("Could not identify action type");
			}
		} catch (Exception e) {
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			Model.getInstance().outputMessage(session, Model.DANGER, "Action \"" + action + "\" error! Reason: " + stringWriter.toString());
		}
		resp.sendRedirect(req.getContextPath() + "/mailing");
	}
}
