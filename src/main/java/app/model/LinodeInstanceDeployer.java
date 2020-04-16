package app.model;

import app.entities.LinodeMarkup;
import app.entities.Server;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LinodeInstanceDeployer implements Runnable {
	private static final List<String> initedCountries = new LinkedList<>();

	//Country to be checked for overflow and might init autodeployment
	private final String country;
	private LinodeMarkup markup = null;

	public LinodeInstanceDeployer(String country) {
		this.country = country;
	}

	public static boolean checkInitedCountryBusiness(String country) {
		return initedCountries.contains(country);
	}

	public LinodeInstanceDeployer(int markupId) throws SQLException {
		this.markup = DbHandler.getInstance().getMarkup(markupId);
		this.country = markup.getCountry();
	}

	@Override
	public void run() {
		String rootPass;
		String ipAddr;
		int serverId;
		HttpResponse<JsonNode> response;

		//Checking requirement & getting suitable markup
		if (markup != null)
			initedCountries.add(markup.getCountry());
		else
			markup = checkRequirements();

		if (markup != null) {
			try {
				//Preparing new instance parameters
				rootPass = RandomStringUtils.randomAlphanumeric(12, 16);
				JSONObject object = new JSONObject()
						.put("region", markup.getLocationName())
						.put("type", markup.getInstanceType())
						.put("backups_enabled", false)
						.put("booted", true)
						.put("image", "linode/ubuntu19.10")
						.put("root_pass", rootPass);

				//Sending instance creation API request to Linode
				response = Unirest.post("https://api.linode.com/v4/linode/instances")
						.header("Content-Type", "application/json")
						.header("Authorization", "Bearer " + Model.getInstance().getSystemConfigValue("linode_token"))
						.body(object)
						.charset(Charset.defaultCharset())
						.asJson();

				//Checking whether instance is created & deployed & running
				String state = "";
				if (response.isSuccess()) {
					ipAddr = response.getBody().getObject().getJSONArray("ipv4").getString(0);
					while (!state.contentEquals("running")) {
						state = Unirest.get("https://api.linode.com/v4/linode/instances/" + response.getBody().getObject().getString("id"))
								.header("Authorization", "Bearer c6a65776e7ee252e26dc74a7411b2cacfd398bd490b64dbd449b0305bea534cd")
								.header("Content-Type", "application/json")
								.asJson().getBody().getObject().getString("status");
						//noinspection BusyWait
						Thread.sleep(3000);
					}

					Thread.sleep(20000);
					//Adding new server instance to DB
					try {
						serverId = DbHandler.getInstance().insertServer(
								ipAddr,
								rootPass,
								"",
								System.currentTimeMillis() / 1000L,
								markup.getUserLimit(),
								country,
								Server.State.SETTING_UP,
								"LIN" + response.getBody().getObject().getString("id") + "-" + markup.getInstanceType() + "|" + markup.getLocationName() + "|" + markup.getUserLimit());
						//Starting server configuration process
						Unirest.put("https://api.linode.com/v4/linode/instances/" + response.getBody().getObject().getString("id"))
								.header("Authorization", "Bearer c6a65776e7ee252e26dc74a7411b2cacfd398bd490b64dbd449b0305bea534cd")
								.header("Content-Type", "application/json")
								.body(new JSONObject().put("label", "lin_" + country.toLowerCase() + "_" + serverId))
								.asEmpty();
						Logger.getInstance().add("New instance creation & adding", Logger.WARNING, "id: \"" + serverId + "\", country: \"" + country + "\", markup: \"" + markup.getId() + "\"");
						new ServerConfigurer(DbHandler.getInstance().getServer(serverId)).run();
					} catch (SQLException e) {
						Logger.getInstance().add("New instance creation & adding", Logger.ERROR, "country: \"" + country + "\", exception: \"" + Logger.parseException(e) + "\"");
						//If server adding to DB failed destroying instance
						Unirest.delete("https://api.linode.com/v4/linode/instances/" + response.getBody().getObject().getString("id"))
								.header("Authorization", "Bearer c6a65776e7ee252e26dc74a7411b2cacfd398bd490b64dbd449b0305bea534cd")
								.asEmpty();
					}
				}
			} catch (Exception ex) {
				Logger.getInstance().add("New instance creation & adding exception", Logger.ERROR, "Country: " + country + "; " + Logger.parseException(ex));
			}
			//Removing country lock
			initedCountries.remove(country);
		}
	}

	//Check if the country client amount exceeds N% of the limit & returns associated markup
	private synchronized LinodeMarkup checkRequirements() {
		//If country is already being checked up / autodeployed and if no, sets country to lock array
		if (!initedCountries.contains(country))
			initedCountries.add(country);
		else return null;

		//If country was not checked up yet
		try {
			LinodeMarkup markup = DbHandler.getInstance().getMarkup(country);
			//Suitable markup existence check
			if (markup == null) return null;
			List<Server> servers = DbHandler.getInstance().getServerList(country);
			int allowedFulfillment = Integer.parseInt(Model.getInstance().getSystemConfigValue("allowed_fulfillment"));
			long maxAllowed = servers.stream()
					.filter(server -> server.getState() == Server.State.RUNNING)
					.collect(Collectors.summarizingInt(Server::getUsersLimit))
					.getSum();
			//Servers users overflow (>N%) check
			if (maxAllowed == 0 || servers
					.stream()
					.filter(server -> server.getState() == Server.State.RUNNING)
					.collect(Collectors.summarizingInt(Server::getClientsAmount))
					.getSum() * 100 / maxAllowed
					> allowedFulfillment) {
				Logger.getInstance().add("New instance request", Logger.WARNING, "Country: " + country);
				return markup;
			}
		} catch (Exception e) {
			Logger.getInstance().add("Country fulfillment check fault", Logger.ERROR, "Country: " + country + "; " + Logger.parseException(e));
		}
		return null;
	}
}
