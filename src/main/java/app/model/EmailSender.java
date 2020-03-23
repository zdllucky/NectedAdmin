package app.model;

import app.entities.Client;
import app.entities.EmailConfig;
import app.entities.Pair;
import kong.unirest.*;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailSender {
	public final static HashMap<String, String> CREDENTIALS;
	private static EmailSender emailSender;

	static {
		HashMap<String, String> credentials = new HashMap<>();
		credentials.put("informer_box_ru", "Информер Vpnnected.com <info@support.vpnnected.com>");
		credentials.put("informer_box_en", "Vpnnected.com Informer <info@support.vpnnected.com>");
		credentials.put("ads_box_ru", "Центр предложений Vpnnected.com <no-reply@offers.vpnnected.com>");
		credentials.put("ads_box_en", "Vpnnected.com Offers center <no-reply@offers.vpnnected.com>");
		credentials.put("moderator_box_ru", "Служба поддержки Vpnnected.com <moderator@support.vpnnected.com>");
		credentials.put("moderator_box_en", "Vpnnected.com Support center <moderator@support.vpnnected.com>");
		CREDENTIALS = credentials;
	}

	private EmailSender() {
	}

	public static EmailSender getInstance() {
		if (emailSender == null) {
			emailSender = new EmailSender();
		}
		return emailSender;
	}

	//Contact mailbox translator
	public String parseContact(Client client) {
		return client.getClientName() + " <" + client.getEmail() + ">";
	}

	public String parseContact(String email, String name) {
		return name + " <" + email + ">";
	}

	//Basic email sender API handler
	public Boolean sendEmail(String language,
	                         String from,
	                         String to,
	                         int config_id,
	                         String values) throws SQLException, UnirestException {
		EmailConfig config = DbHandler.getInstance().getEmailConfig(config_id, language, Arrays.asList(values.split(" *~ *")));
		StringBuilder vars = null;

		//Setting up email variables
		if (config.getValues() != null) {
			vars = new StringBuilder("\"" +
					config.getVariables().get(0) + "\": \"" +
					config.getValues().get(0) + "\"");
			for (int i = 1; i < config.getValues().size(); i++)
				vars
						.append(", \"")
						.append(config.getVariables().get(i))
						.append("\": \"")
						.append(config.getValues().get(i))
						.append("\"");
		}

		//Preparing email query
		MultipartBody emailBody = Unirest
				.post("https://api.mailgun.net/v3/" + Model.getInstance().getSystemConfigValue("support_mailbox_domain") + "/messages")
				.basicAuth("api", Model.getInstance().getSystemConfigValue("mailgun_api_key"))
				.field("from", from)
				.field("to", to)
				.field("subject", config.getSubject())
				.field("template", config.getTemplate());
		if (vars != null) {
			emailBody.field("h:X-Mailgun-Variables", "{" + vars.toString() + "}");
		}
		return emailBody.asJson().isSuccess();
	}

	//Basic email sender API handler
	public Boolean sendEmail(String from,
	                         String to,
	                         String subject,
	                         String body,
	                         List<Pair<String, String>> params) throws UnirestException {

		//Preparing email query
		MultipartBody multipartBody = Unirest
				.post("https://api.mailgun.net/v3/" + Model.getInstance().getSystemConfigValue("support_mailbox_domain") + "/messages")
				.basicAuth("api", Model.getInstance().getSystemConfigValue("mailgun_api_key"))
				.field("from", from)
				.field("to", to)
				.field("subject", subject)
				.field("html", body);


		if (params != null)
			for (Pair<String, String> tParam : params)
				multipartBody.field(tParam.getKey(), tParam.getValue());

		HttpResponse<JsonNode> s = multipartBody.asJson();
		return s.isSuccess();
	}

	public String parseMessage(String message, Client client, Map<String, String> logReferenceMap) {
		StringBuilder newString = new StringBuilder(message);

		for (int varBegin = newString.indexOf("<$") + 2;
		     varBegin != 1;
		     varBegin = newString.indexOf("<$") + 2) {
			int varEnd = newString.indexOf("$>");
			String var = newString.substring(varBegin, varEnd);
			String varResult = "";
			try {
				if (var.startsWith("CLIENT.")) {
					var = var.substring(7);

					if (var.startsWith("SERVER")) {
						var = var.substring(7);

						if (var.startsWith("COUNTRY.")) {
							var = var.substring(8);

							switch (var) {
								case "NAME_RU":
									varResult = DbHandler.getInstance().getCountryName(
											client.getServer().getCountry(),
											"ru");
									break;
								case "NAME_EN":
									varResult = DbHandler.getInstance().getCountryName(
											client.getServer().getCountry(),
											"en");
									break;
							}
						} else {
							switch (var) {
								case "COUNTRY":
									varResult = client.getServer().getCountry();
									break;
								case "IP_ADDR":
									varResult = client.getServer().getIp_addr();
									break;
								case "IPSECPSK":
									varResult = client.getServer().getIpSecPSK();
									break;
							}
						}
					} else if (var.startsWith("COUNTRY_FROM.")) {
						var = var.substring(13);

						switch (var) {
							case "NAME_RU":
								varResult = DbHandler.getInstance().getCountryName(
										client.getCountryFrom(),
										"ru");
								break;
							case "NAME_EN":
								varResult = DbHandler.getInstance().getCountryName(
										client.getCountryFrom(),
										"en");
								break;
						}
					} else {
						switch (var) {
							case "ID":
								varResult = String.valueOf(client.getId());
								break;
							case "EMAIL":
								varResult = client.getEmail();
								break;
							case "NAME":
								varResult = client.getClientName();
								break;
							case "CONN":
								varResult = client.getConn();
								break;
							case "LOGIN":
								varResult = client.getLogin();
								break;
							case "REG_DATE":
								varResult = new SimpleDateFormat("dd/MM/yyyy").format(client.getRegTime() * 1000L);
								break;
							case "EXPIRE_DATE":
								varResult = new SimpleDateFormat("dd/MM/yyyy").format(client.getSubscrTo() * 1000L);
								break;
							case "LANGUAGE":
								varResult = client.getLang().equals("ru") ? "Русский" : "English";
								break;
							case "REF_BONUS":
								varResult = String.valueOf(client.getRefDays());
								break;
							case "COUNTRY_FROM":
								varResult = client.getCountryFrom();
								break;
						}
					}
				} else if (var.startsWith("LOGREF.")) {
					var = var.substring(7);

					varResult = logReferenceMap.getOrDefault(var, "");
				} else if (var.startsWith("COUPON.")) {
					String addr = logReferenceMap.get("initiator");
					var = var.substring(7);

					String[] vals = var.split("\\.");

					varResult = Unirest
							.post("https://" + addr + "/coupons")
							.field("action", "new_api")
							.field("type", String.valueOf(vals[0].equals("PERC") ? 1 : 2))
							.field("value", String.valueOf(vals[0].equals("PERC")
									? Math.abs(Integer.parseInt(vals[2])) % 91
									: Math.abs(Integer.parseInt(vals[2]) - 7) % 177 + 7))
							.field("expiration_type", String.valueOf(vals[1].equals("DAYS") ? 1 : 2))
							.field("expired_by", String.valueOf(vals[2].equals("DAYS")
									? Math.abs(Integer.parseInt(vals[3])) % 732
									: Math.abs(Integer.parseInt(vals[3])) % 5000))
							.asString().getBody();
				}
			} catch (Exception ignored) {
			}

			newString.replace(varBegin - 2, varEnd + 2, varResult);
		}
		return newString.toString();
	}
}
