package app.servlets;

import app.entities.Client;
import app.entities.LinodeMarkup;
import app.entities.Server;
import app.model.DbHandler;
import app.model.LinodeInstanceDeployer;
import app.model.Logger;
import app.model.Model;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class APIServlet extends HttpServlet {
	private final JsonObject jsonReply = new JsonObject();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setStatus(200);
		Model.getInstance();
		String command = req.getHeader("operation");
		int clientId;
		Client client = null;

		try {
			switch (command) {
				case "checkEmail":
					String email = req.getParameter("email");
					clientId = 0;
					if (email != null && email.matches("^[0-9a-z._-]+@[0-9a-z._-]*[0-9a-z_-]+\\.[0-9a-z_-]+$")) {
						clientId = DbHandler.getInstance().getClientId(email);
						if (clientId == -1)
							try {
								DbHandler.getInstance().checkEmail(email);
							} catch (SQLException e) {
								clientId = 0;
							}
					}
					jsonReply.addProperty("id", clientId);
					jsonReply.addProperty("email", email);
					break;

				case "loginProcedure":
					clientId = Integer.parseInt(req.getParameter("client_id"));
					String clientPassword = req.getParameter("client_password");
					client = DbHandler.getInstance().loginClientProcedure(clientId, clientPassword);
					if (client == null) {
						jsonReply.addProperty("logon", -1);
						break;
					}
					jsonReply.addProperty("logon", 1);
					jsonReply.addProperty("client_id", clientId);
					jsonReply.addProperty("client_email", client.getEmail());
					jsonReply.addProperty("client_name", client.getClientName());
					jsonReply.addProperty("client_login", client.getLogin());
					jsonReply.addProperty("client_conn", client.getConn());
					jsonReply.addProperty("client_reg_date", client.getRegTime());
					jsonReply.addProperty("client_subscr_to", client.getSubscrTo());
					jsonReply.addProperty("client_ref_id", client.getReferredFrom());
					jsonReply.addProperty("client_noswap_to", client.getNoswapTo());
					Logger.getInstance().add("Client logon", clientId, Logger.INFO,
							"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\"");
					break;

				case "getAvailableCountries":
					HashSet<String> countries = DbHandler.getInstance().getServerList().stream().filter(server -> server.availableAmount() > 0 && server.getState() == Server.State.RUNNING).map(Server::getCountry).collect(Collectors.toCollection(HashSet::new));
					JsonArray array = new JsonArray(countries.size());
					for (String tCountry : countries)
						array.add(tCountry);

					List<LinodeMarkup> deployableCountries = DbHandler.getInstance().getLinodeMarkupList(true);
					JsonArray deployableCountriesJSONArray = new JsonArray(deployableCountries.size());

					for (LinodeMarkup tMarkup : deployableCountries)
						deployableCountriesJSONArray.add(tMarkup.getCountry());

					jsonReply.add("deployableCountries", deployableCountriesJSONArray);
					jsonReply.add("countries", array);
					break;

				case "registerNewAccount":
					String new_name = req.getParameter("name").replaceAll("[^0-9a-zA-Zа-яА-Я ]", "").trim();
					String new_email = req.getParameter("email").toLowerCase().replaceAll("['\"`=~^+;]", "").trim();
					String new_password = RandomStringUtils.randomAlphanumeric(8, 12);
					String lang = req.getParameter("lang");
					int ref = req.getParameter("ref") != null
							? Integer.parseInt(req.getParameter("ref"))
							: Client.NO_REFERRER_VALUE;
					int id = -1;
					try {
						String newNameTrimmed = new_name.substring(0, Math.min(new_name.length(), 20)).trim();
						id = DbHandler.getInstance().insertClient(
								newNameTrimmed,
								new_email,
								System.currentTimeMillis() / 1000L,
								ref
						);
						DbHandler.getInstance().initialiseClient(
								id,
								new_email.substring(0, new_email.indexOf("@")) + id,
								new_password,
								lang);
						jsonReply.addProperty("name", newNameTrimmed);
/*
						EmailSender.getInstance().sendEmail(
								lang,
								EmailSender.CREDENTIALS.get("informer_box_" + lang),
								EmailSender.getInstance().parseContact(new_email, newNameTrimmed),
								2,
								new_name + "~" + new_email + "~" + new_password + "~" + req.getParameter("login_addr"));
 */
						Logger.getInstance().add("Client registration", id, Logger.INFO,
								"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\"");
					} catch (SQLException e) {
						jsonReply.addProperty("error", e.toString());
						resp.setStatus(400);
						Logger.getInstance().add("Client registration exception", Logger.ERROR, Logger.parseException(e));
						if (id != -1)
							DbHandler.getInstance().removeClient(id);
					}
					break;

				case "addTrialDay":
					DbHandler.getInstance().setPlan(Integer.parseInt(req.getParameter("id")),
							1,
							2,
							req.getParameter("from"),
							req.getParameter("to"));

					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));
					jsonReply.addProperty("client_subscr_date", client.getSubscrTo());
					jsonReply.addProperty("client_server_country", client.getServer().getCountry());
/*
					//Trial day email notification
					lang = req.getParameter("lang");
					EmailSender.getInstance().sendEmail(
							lang,
							EmailSender.CREDENTIALS.get("informer_box_" + lang),
							EmailSender.getInstance().parseContact(client.getEmail(), client.getClientName()),
							5,
							client.getClientName() + "~" + req.getParameter("instructions_url") + "~" + req.getParameter("personal_page_url"));
 */
					Logger.getInstance().add("Trial day activation", client.getId(), Logger.INFO,
							"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\", " +
									"to: \"" + client.getServer().getId() + "\"");
					break;

				case "changeServer":
					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));

					String exServerId = client.getServer() != null
							? String.valueOf(client.getServer().getId())
							: "-1";

					DbHandler.getInstance().setPlan(client.getId(),
							0,
							Integer.parseInt(req.getParameter("type")),
							req.getParameter("from"),
							req.getParameter("to"));

					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));
/*
					//Server exchange email notification (uncomment after template will be ready)
					lang = req.getParameter("lang");
					EmailSender.getInstance().sendEmail(
							lang,
							EmailSender.CREDENTIALS.get("informer_box_" + lang),
							EmailSender.getInstance().parseContact(client.getEmail(), client.getClientName()),
							6,
							client.getClientName() + "~" + DbHandler.getInstance().getCountryName(client.getServer().getCountry(), lang) + "~" + req.getParameter("instructions_url") + "~" + req.getParameter("personal_page_url"));
 */
					DbHandler.getInstance().addNoSwapTimer(client);

					jsonReply.addProperty("client_server_country", client.getServer() != null ? client.getServer().getCountry() : "--");
					jsonReply.addProperty("client_noswap_to", client.getNoswapTo());

					Logger.getInstance().add("Server exchange",
							client.getId(),
							Logger.INFO,
							"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\", " +
									"from: \"" + req.getParameter("from") + "\", " +
									"to: \"" + (client.getServer() != null
									? client.getServer().getId()
									: -1) + "\", " +
									"ex_server_id: \"" + exServerId + "\"");
					break;

				case "sendPasswordRecoveryEmail":
					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));
/*
					lang = req.getParameter("lang");
					EmailSender.getInstance().sendEmail(
							lang,
							EmailSender.CREDENTIALS.get("informer_box_" + lang),
							EmailSender.getInstance().parseContact(client),
							3,
							client.getClientName() + "~" + client.getEmail() + "~" + req.getParameter("reset_password_url") + "~" + req.getParameter("support_url"));
 */
					Logger.getInstance().add("Password recovery request",
							client.getId(),
							Logger.INFO,
							"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\", " +
									"reset_url: \"" + req.getParameter("reset_password_url") + "\"");
					break;

				case "resetClientPassword":
					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));
					String exPassword = client.getConn();

					DbHandler.getInstance().changeClientPassword(client, req.getParameter("password"));
/*
					lang = req.getParameter("lang");
					EmailSender.getInstance().sendEmail(
							lang,
							EmailSender.CREDENTIALS.get("informer_box_" + lang),
							EmailSender.getInstance().parseContact(client),
							4,
							client.getClientName() + "~" + client.getEmail() + "~" + req.getParameter("support_url"));
 */
					jsonReply.addProperty("client_name", client.getClientName());
					jsonReply.addProperty("client_email", client.getEmail());

					Logger.getInstance().add("Password reset", client.getId(), Logger.INFO, "initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\", " +
							"ex_pass: \"" + exPassword + "\"");
					break;

				case "getClientData":
					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));

					jsonReply.addProperty("client_subscr_to", client.getSubscrTo());
					jsonReply.addProperty("client_ref_days", client.getRefDays());

					if (client.getServer() != null) {
						jsonReply.addProperty("client_server_ip_addr", client.getServer().getIp_addr());
						jsonReply.addProperty("client_server_ipsec_psk", client.getServer().getIpSecPSK());
						jsonReply.addProperty("client_server_country", client.getServer().getCountry());
					}
					break;

				case "checkReferrer":
					if (!DbHandler.getInstance().checkReferrer(Integer.parseInt(req.getParameter("ref_id"))))
						throw new SQLException();
					break;

				case "proceedPayment": {
					//Gathering client data
					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));

					//Payment record id
					String paymentRecordId;

					long daysAmount;
					long totalDaysAmount;
					long bonusDaysAmount;
					String fee = "0.00";
					String currLabel = "RUB";
					String paymentType = req.getParameter("payment_type");

					//Checking payment type
					//If payment type is by static day coupon
					if (paymentType.equals("coupon_type_2")) {
						paymentType = "Coupon";
						daysAmount = Integer.parseInt(req.getParameter("days_amount"));
						totalDaysAmount = client.getReferredFrom() != -1 && client.getSubscrTo() - client.getRegTime() < 5 * 86400
								? (long) (daysAmount * (1.0 + Double.parseDouble(req.getParameter("referred_bonus"))))
								: daysAmount;
						bonusDaysAmount = totalDaysAmount - daysAmount;

						//Setting plan referral ?bonus
						DbHandler.getInstance().setPlan(
								client.getId(),
								totalDaysAmount,
								2,
								req.getParameter("country_from"),
								client.getServer() != null
										? "--"
										: "OP");

						//Checking referrer insistence
						if (client.getReferredFrom() != -1)
							//Adding bonus to referral
							DbHandler.getInstance().addRefDays(
									client.getReferredFrom(),
									(int) (daysAmount * Double.parseDouble(req.getParameter("referred_cashback"))));

						//Adding successful payment record
						DbHandler.getInstance().addPaymentRecord(
								client.getId() + "T" + System.currentTimeMillis(),
								currLabel + fee,
								String.valueOf(totalDaysAmount),
								req.getParameter("coupon"),
								client.getId(),
								System.currentTimeMillis() / 1000L,
								"COUPON"
						);

						paymentRecordId = client.getId() + "T" + System.currentTimeMillis();
					} else if (paymentType.equals("Robokassa")) {
						//Setting plan referral ?bonus
						double couponBonus = 0.00, refBonus = 0.00;
						if (!req.getParameter("coupon").isBlank())
							couponBonus = Double.parseDouble(req.getParameter("coupon").replaceAll("^.*\\|", "")) / 100.00;

						if (client.getReferredFrom() != -1 && client.getSubscrTo() - client.getRegTime() < 5 * 86400)
							refBonus = Double.parseDouble(req.getParameter("referred_bonus"));

						daysAmount = Integer.parseInt(req.getParameter("days_amount"));
						totalDaysAmount = (long) (daysAmount * (1.0 + refBonus + couponBonus));
						bonusDaysAmount = totalDaysAmount - daysAmount;
						fee = req.getParameter("fee");

						DbHandler.getInstance().setPlan(
								client.getId(),
								totalDaysAmount,
								2,
								req.getParameter("country_from"),
								client.getServer() != null
										? "--"
										: "OP");

						//Checking referrer insistence
						if (client.getReferredFrom() != -1)
							//Adding bonus to referral
							DbHandler.getInstance().addRefDays(
									client.getReferredFrom(),
									(int) (daysAmount * Double.parseDouble(req.getParameter("referred_cashback"))));

						//Adding successful payment record
						paymentRecordId = req.getParameter("inv_id");
						DbHandler.getInstance().addPaymentRecord(
								req.getParameter("inv_id"),
								currLabel + fee,
								String.valueOf(totalDaysAmount),
								req.getParameter("coupon"),
								client.getId(),
								System.currentTimeMillis() / 1000L,
								paymentType
						);
					} else {
						Logger.getInstance().add("Platform intrusion detected!!!", Logger.WARNING, "remote_addr: \"" + req.getRemoteAddr() + "\"");
						return;
					}

					Logger.getInstance().add(
							"Payment",
							client.getId(),
							Logger.INFO,
							"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\", " +
									"payment_id: \"" + paymentRecordId + "\", " +
									"days_amount: \"" + daysAmount + "\", " +
									"payment_type: \"" + paymentType + "\", " +
									"fee: \"" + fee + "\", " +
									"curr_label: \"" + currLabel + "\", " +
									"bonus_days_amount: \"" + bonusDaysAmount + "\", " +
									"total_days_amount: \"" + totalDaysAmount + "\"");
					break;
				}
				case "activateReferralBonus":
					//Gathering client data
					client = DbHandler.getInstance().getClient(Integer.parseInt(req.getParameter("id")));
					resp.setStatus(400);
					String paymentRecordId;

					if (client.getRefDays() > 0) {
						int refDaysAmount = client.getRefDays();
						DbHandler.getInstance().setPlan(
								client.getId(),
								refDaysAmount,
								3,
								client.getCountryFrom(),
								client.getServer() != null
										? "--"
										: "OP");
						//Adding successful payment record
						paymentRecordId = client.getId() + "T" + System.currentTimeMillis();
						DbHandler.getInstance().addPaymentRecord(
								client.getId() + "T" + System.currentTimeMillis(),
								"RUB0.00",
								String.valueOf(refDaysAmount),
								"",
								client.getId(),
								System.currentTimeMillis() / 1000L,
								"REFERRAL"
						);
						resp.setStatus(200);

						Logger.getInstance().add("Referral bonus activation", client.getId(), Logger.INFO,
								"initiator: \"" + Model.getHostName(req.getRemoteAddr()) + "\", " +
										"payment_id: \"" + paymentRecordId + "\", " +
										"days_amount: \"" + refDaysAmount + "\"");
					} else {
						throw new NullPointerException("ref_days amount is 0");
					}
					break;
				case "deployNewCountry":
					String country = req.getParameter("country");
					if (LinodeInstanceDeployer.checkInitedCountryBusiness(country))
						resp.setStatus(405);
					int markupId = DbHandler.getInstance()
							.getMarkup(country)
							.getId();

					new Thread(() -> {
						int clientIdNew = Integer.parseInt(req.getParameter("client_id"));
						String initiator = Model.getHostName(req.getRemoteAddr());
						try {
							new LinodeInstanceDeployer(markupId).run();
							Logger.getInstance().add(
									"Country deployment",
									clientIdNew,
									Logger.INFO,
									"initiator: \"" + initiator + "\", " +
											"country: \"" + country + "\", " +
											"country_name_ru: \"" + DbHandler.getInstance().getCountryName(country, "ru") + "\", " +
											"country_name_en: \"" + DbHandler.getInstance().getCountryName(country, "en") + "\", " +
											"markup_id: \"" + markupId + "\"");
						} catch (Exception e) {
							Logger.getInstance().add("Country deployment log error", Logger.ERROR, "ex: \"" + Logger.parseException(e) + "\"");
						}
					}).start();
					break;
				default:
					throw new IllegalArgumentException("Headers not defined!");
			}
		} catch (Exception e) {
			if (client == null)
				Logger.getInstance().add("\"" + command + "\"" + " API command exception", Logger.ERROR, Logger.parseException(e));
			else
				Logger.getInstance().add("\"" + command + "\"" + " API command exception", client.getId(), Logger.ERROR, Logger.parseException(e));

			jsonReply.addProperty("error", Logger.parseException(e));
			resp.setStatus(400);
		}
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		resp.setHeader("response", "response");
		out.print(jsonReply);
		out.flush();
		out.close();
	}
}
