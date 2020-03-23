package app.servlets;

import app.model.DbHandler;
import app.model.Model;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class ExpiredClientsServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		if (req.getSession(false) == null) req.getSession(true);
		if (req.getSession(false).getAttribute("logon") == null) {
			req.getSession(false).setAttribute("toPage", "expired_clients");
			resp.sendRedirect("login");
			return;
		}
		try {
			req.setAttribute("rows_amount", DbHandler.getInstance().getExpiredClientsAmount());
			if (req.getParameter("page") != null) {
				int page = Integer.parseInt(req.getParameter("page"));
				if (page > 0) {
					if (req.getParameter("by") != null) {
						int by = Integer.parseInt(req.getParameter("by"));
						if (by < 1000 && by > 0) {
							req.setAttribute("clients", DbHandler.getInstance().getExpiredClientList((page - 1) * by, by));
							req.setAttribute("page_number", page);
							req.setAttribute("by", by);
						} else {
							req.setAttribute("clients", DbHandler.getInstance().getExpiredClientList((page - 1) * 100, 100));
							req.setAttribute("page_number", page);
							req.setAttribute("by", 100);
						}
					} else {
						req.setAttribute("clients", DbHandler.getInstance().getExpiredClientList((page - 1) * 100, 100));
						req.setAttribute("page_number", page);
						req.setAttribute("by", 100);
					}
				} else {
					req.setAttribute("clients", DbHandler.getInstance().getExpiredClientList((page - 1) * 100, 100));
					req.setAttribute("page_number", page);
					req.setAttribute("by", 100);
				}
			} else {
				req.setAttribute("clients", DbHandler.getInstance().getExpiredClientList(0, 100));
				req.setAttribute("page_number", 1);
				req.setAttribute("by", 100);
			}
			req.getRequestDispatcher("views/expired_clients.jsp").forward(req, resp);
		} catch (NumberFormatException e) {
			try {
				req.setAttribute("clients", DbHandler.getInstance().getExpiredClientList(0, 100));
				req.setAttribute("page_number", 1);
				req.setAttribute("by", 100);
				req.getRequestDispatcher("views/expired_clients.jsp").forward(req, resp);
			} catch (SQLException ex) {
				ex.printStackTrace();
				Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error gathering clients data!");
				resp.sendRedirect("servers");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Model.getInstance().outputMessage(req.getSession(false), Model.DANGER, "Error gathering clients data!");
			resp.sendRedirect("servers");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		boolean logout = req.getParameterValues("logout") != null;
		if (logout) {
			req.getSession(false).setAttribute("toPage", "expired_clients");
			req.getSession(false).removeAttribute("logon");
			resp.sendRedirect("login");
		}
	}
}
