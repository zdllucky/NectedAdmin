package app.model;

import app.entities.*;
import com.jcraft.jsch.JSchException;
import org.sqlite.JDBC;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
public class DbHandler {
	private static final String DB_PATH = "jdbc:sqlite:/var/lib/tomcat9/NectedDB/main";
	private static DbHandler instance = null;
	private Connection connection;

	private DbHandler() throws SQLException {
		Properties SQLProps = new Properties();
		SQLProps.put("charset", "UTF8");
		SQLProps.put("useUnicode", "true");
		DriverManager.registerDriver(new JDBC());
		this.connection = DriverManager.getConnection(DB_PATH, SQLProps);
		this.connection.setReadOnly(false);
	}

	public static synchronized DbHandler getInstance() throws SQLException {
		if (instance == null)
			instance = new DbHandler();
		return instance;
	}

	/**
	 * @return {@link ArrayList<Server>} of all servers in database
	 * @throws SQLException if query error
	 */
	public List<Server> getServerList() throws SQLException {
		Statement statement = this.connection.createStatement();

		List<Server> list = new ArrayList<>();
		ResultSet resultSet = statement.executeQuery("SELECT id, ip_addr, conn, ipsecpsk, state, add_date, users_limit, country, property " +
				"FROM servers");
		while (resultSet.next()) {
			list.add(new Server(resultSet.getInt("id"),
					resultSet.getString("ip_addr"),
					resultSet.getString("conn"),
					resultSet.getString("ipsecpsk"),
					Server.State.parse(resultSet.getInt("state")),
					resultSet.getLong("add_date"),
					resultSet.getInt("users_limit"),
					resultSet.getString("country"),
					resultSet.getString("property")
			));
		}
		return list;
	}

	/**
	 * @param country {@code "XX"} as country identifier
	 * @return {@link ArrayList<Server>} of all servers of specified country
	 * @throws SQLException if query error
	 */
	List<Server> getServerList(String country) throws SQLException {
		Statement statement = this.connection.createStatement();

		List<Server> list = new ArrayList<>();
		ResultSet resultSet = statement.executeQuery("SELECT id, ip_addr, conn, ipsecpsk, state, add_date, users_limit, country, property " +
				"FROM servers " +
				"WHERE country LIKE '" + country + "'");
		while (resultSet.next()) {
			list.add(new Server(resultSet.getInt("id"),
					resultSet.getString("ip_addr"),
					resultSet.getString("conn"),
					resultSet.getString("ipsecpsk"),
					Server.State.parse(resultSet.getInt("state")),
					resultSet.getLong("add_date"),
					resultSet.getInt("users_limit"),
					resultSet.getString("country"),
					resultSet.getString("property")
			));
		}
		return list;
	}

	/**
	 * Returns server within specified id
	 *
	 * @param id unique int value identifier of a server
	 * @return {@link Server} or {@code null} if no server was found
	 * @throws SQLException if query error
	 */
	public Server getServer(int id) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT id, ip_addr, conn, ipsecpsk, state, add_date, users_limit, country, property " +
				"FROM servers " +
				"WHERE id=" + id);
		if (resultSet.next()) return new Server(resultSet.getInt("id"),
				resultSet.getString("ip_addr"),
				resultSet.getString("conn"),
				resultSet.getString("ipsecpsk"),
				Server.State.parse(resultSet.getInt("state")),
				resultSet.getLong("add_date"),
				resultSet.getInt("users_limit"),
				resultSet.getString("country"),
				resultSet.getString("property"));
		else return null;
	}

	/**
	 * Adds new server record to database
	 *
	 * @param ip_addr    {@value "ddd.ddd.ddd.ddd"} server IP address
	 * @param conn       alphanumeric both-case SSH "root" user password string
	 * @param ipsecpsk   alphanumeric both-case IPSec Public Shared Key string
	 * @param addDate    long value of record insertion UNIX timestamp (seconds)
	 * @param usersLimit max limit of user allowed for a new server
	 * @param country    {@value "XX"} code string server country
	 * @param state      server up status (see {@link Server.State} enumeration)
	 * @param property   {@value "LIN[instance ID]-[instance type]|[location name]|[user limit]"} linking value (used
	 *                   to associate database record with https://linode.com API)
	 * @return new server record id value
	 * @throws SQLException if query error
	 */
	public int insertServer(String ip_addr,
	                        String conn,
	                        String ipsecpsk,
	                        long addDate,
	                        int usersLimit,
	                        String country,
	                        Server.State state,
	                        String property) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT INTO servers(`ip_addr`, `conn`, `ipsecpsk`, `state`, `add_date`, `users_limit`, `country`, `property`) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		statement.setObject(1, ip_addr);
		statement.setObject(2, conn);
		statement.setObject(3, ipsecpsk);
		statement.setObject(4, Server.State.compose(state));
		statement.setObject(5, addDate);
		statement.setObject(6, usersLimit);
		statement.setObject(7, country);
		statement.setObject(8, property);

		statement.executeUpdate();
		return statement.getGeneratedKeys().getInt(1);
	}

	/**
	 * @param id server record id value
	 * @return clients amount banded to specified server
	 * @throws SQLException if query error
	 */
	public int getClientsAmount(int id) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet set = statement.executeQuery("SELECT COUNT(*) as count " +
				"FROM clients " +
				"WHERE server_id=" + id + " AND subscr_to > " + System.currentTimeMillis() / 1000L);
		return set.getInt(1);
	}

	/**
	 * @return total amount of clients with active subscription
	 * @throws SQLException if query error
	 */
	public int getActiveClientsAmount() throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet set = statement.executeQuery("SELECT COUNT(*) as count " +
				"FROM clients " +
				"WHERE subscr_to > " + System.currentTimeMillis() / 1000L);
		return set.getInt(1);
	}

	/**
	 * @return total amount of clients with expired subscription
	 * @throws SQLException if database query error
	 */
	public int getExpiredClientsAmount() throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet set = statement.executeQuery("SELECT COUNT(*) as count " +
				"FROM clients " +
				"WHERE subscr_to <= " + System.currentTimeMillis() / 1000L);
		return set.getInt(1);
	}

	/**
	 * @param offset says from which position to query clients
	 * @param limit  says how many clients to query
	 * @return {@link ArrayList<Client>} of active subscription clients (split by offset and limit)
	 * @throws SQLException if query error
	 */
	public List<Client> getActiveClientList(int offset, int limit) throws SQLException {
		Statement statement = this.connection.createStatement();
		List<Client> list = new ArrayList<>();
		ResultSet resultSet = statement.executeQuery("SELECT id, client_name, email, tel_num, server_id, conn, login, register_date, subscr_to, swap_server_attempt, noswap_to, country_from, ref_days, referred_from, clients.language " +
				"FROM clients " +
				"WHERE subscr_to >" + System.currentTimeMillis() / 1000L + " LIMIT " + offset + ", " + limit);
		List<Server> servers = DbHandler.getInstance().getServerList();
		while (resultSet.next()) {
			int server_id = resultSet.getInt("server_id");
			list.add(new Client(resultSet.getInt("id"),
					resultSet.getString("client_name"),
					resultSet.getString("email"),
					resultSet.getLong("tel_num"),
					servers.stream().filter(server -> server.getId() == server_id).findFirst().orElse(null),
					resultSet.getString("conn"),
					resultSet.getString("login"),
					resultSet.getLong("register_date"),
					resultSet.getLong("subscr_to"),
					resultSet.getInt("swap_server_attempt"),
					resultSet.getLong("noswap_to"),
					resultSet.getString("country_from"),
					resultSet.getInt("ref_days"),
					resultSet.getInt("referred_from"),
					resultSet.getString("language")));
		}
		return list;
	}

	/**
	 * @return {@link ArrayList<Client>} of clients with uncancelled, but expired subscription
	 * @throws SQLException if database query error
	 */
	List<Client> getExpiredNotClearedClients() throws SQLException {
		Statement statement = this.connection.createStatement();
		List<Client> list = new ArrayList<>();
		ResultSet resultSet = statement.executeQuery("SELECT id, client_name, email, tel_num, server_id, conn, login, register_date, subscr_to, swap_server_attempt, noswap_to, country_from, ref_days, referred_from, clients.language " +
				"FROM clients " +
				"WHERE subscr_to <=" + System.currentTimeMillis() / 1000L + " AND server_id!=-1");
		List<Server> servers = DbHandler.getInstance().getServerList();
		while (resultSet.next()) {
			int server_id = resultSet.getInt("server_id");
			list.add(new Client(resultSet.getInt("id"),
					resultSet.getString("client_name"),
					resultSet.getString("email"),
					resultSet.getLong("tel_num"),
					servers.stream().filter(server -> server.getId() == server_id).findFirst().orElse(null),
					resultSet.getString("conn"),
					resultSet.getString("login"),
					resultSet.getLong("register_date"),
					resultSet.getLong("subscr_to"),
					resultSet.getInt("swap_server_attempt"),
					resultSet.getLong("noswap_to"),
					resultSet.getString("country_from"),
					resultSet.getInt("ref_days"),
					resultSet.getInt("referred_from"),
					resultSet.getString("language")));
		}
		return list;
	}

	/**
	 * @param offset says from which position to query clients
	 * @param limit  says how many clients to query
	 * @return {@link ArrayList<Client>} of expired subscription clients (split by offset and limit)
	 * @throws SQLException if query error
	 */
	public List<Client> getExpiredClientList(int offset, int limit) throws SQLException {
		Statement statement = this.connection.createStatement();
		List<Client> list = new ArrayList<>();
		ResultSet resultSet = statement.executeQuery("SELECT id, client_name, email, tel_num, conn, login, register_date, subscr_to, swap_server_attempt, noswap_to, country_from, ref_days, referred_from, clients.language " +
				"FROM clients " +
				"WHERE subscr_to <=" + System.currentTimeMillis() / 1000L + " " +
				"ORDER BY subscr_to DESC " +
				"LIMIT " + offset + ", " + limit);
		while (resultSet.next()) {
			list.add(new Client(resultSet.getInt("id"),
					resultSet.getString("client_name"),
					resultSet.getString("email"),
					resultSet.getLong("tel_num"),
					null,
					resultSet.getString("conn"),
					resultSet.getString("login"),
					resultSet.getLong("register_date"),
					resultSet.getLong("subscr_to"),
					resultSet.getInt("swap_server_attempt"),
					resultSet.getLong("noswap_to"),
					resultSet.getString("country_from"),
					resultSet.getInt("ref_days"),
					resultSet.getInt("referred_from"),
					resultSet.getString("language")
			));
		}
		return list;
	}


	/**
	 * Inserts a new client database record (if called, then method
	 * {@link DbHandler#initialiseClient(int, String, String, String)}) must be called too to finish up client creation.
	 *
	 * @param name         client's name string
	 * @param email        client's email string
	 * @param registerTime long value of client registration UNIX timestamp (seconds)
	 * @param ref_id       referrer client id value (use {@value Client#NO_REFERRER_VALUE} if no referrer was attached)
	 * @return new client record id value
	 * @throws SQLException if query error
	 */
	public int insertClient(String name,
	                        String email,
	                        long registerTime,
	                        int ref_id) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("INSERT INTO clients(client_name, email, register_date, referred_from) " +
				"VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		statement.setObject(1, name);
		statement.setObject(2, email);
		statement.setObject(3, registerTime);
		statement.setObject(4, ref_id);
		statement.executeUpdate();
		return statement.getGeneratedKeys().getInt(1);
	}

	/**
	 * Initializes inserted client (MUST be called next to {@link DbHandler#insertClient(String, String, long, int)})
	 * to finish up client creation.
	 *
	 * @param id       client id
	 * @param login    new client VPN login small-case alphanumeric string
	 *                 (like {@code Client#getEmail().replaceAll("@.*$", "") + Client#getId()})
	 * @param conn     new client cabinet & VPN alphanumeric password string
	 * @param language client language code string ({@value "ru" or "en"})
	 * @throws SQLException if sql query
	 */
	public void initialiseClient(int id,
	                             String login,
	                             String conn,
	                             String language) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("UPDATE clients " +
				"SET login=(?), conn=(?), language=(?), subscr_to=register_date " +
				"WHERE id=(?)");
		statement.setObject(1, login.replaceAll("[^0-9a-zA-Z]", ""));
		statement.setObject(2, conn);
		statement.setObject(3, language);
		statement.setObject(4, id);
		statement.execute();
	}

	/**
	 * This complex method can be used for:
	 * 1. Activating user VPN subscription if it is not. It adds client login credentials to a VPN server, updates
	 * relative database information ({@code client.serverId}, {@code client.subscrTo}, {@code client.countryFrom}).
	 * 2. Changing client's VPN server, including smart server suggesting based on several parameters (e.g server
	 * availability, fulfillment and connection quality (based on {@link Server#getStrikes()} values)).
	 * 3. Adjusting, prolonging & cancellation client subscription (by modifying {@param subDays} amount).
	 * 4. Referral bonus days activation.
	 *
	 * @param clientId    client's id value
	 * @param subDays     amount of days to add or subtract (use {@value (int) 0} if you need to perform #2 cases)
	 * @param type        use {@value (int) 1} to add/increase strike record VPN server a client used to be connected to.
	 *                    use {@value (int) 3} to activate referrer bonus days and update database {@code ref_days} value.
	 *                    use other {@value (int) -2^31~2^31} if none of the above is needed.
	 * @param countryFrom an {@value "XX"} country identifier that is used for better country matching. Usually is set
	 *                    to China {@value "CN"}.
	 * @param countryTo   a {@value "XX"} country code to indicate a country to connect to;
	 *                    use {@value "OP"} ("Optimal") to set the country to
	 *                    {@code Model.getInstance().getSystemConfigs("default_country")};
	 *                    use {@value "--"} to prevent server exchanging (e.g. in #3 case).
	 * @throws SQLException           if query error.
	 * @throws JSchException          if server SHH connection error.
	 * @throws IOException            if server SSH I/O error.
	 * @throws NoServerFoundException if no suitable server found.
	 * @throws InterruptedException   if thread operation was interrupted.
	 */
	public synchronized void setPlan(int clientId,
	                                 long subDays,
	                                 int type,
	                                 String countryFrom,
	                                 String countryTo)
			throws SQLException,
			JSchException,
			IOException,
			NoServerFoundException,
			InterruptedException {

		//Getting client
		Client currentClient = DbHandler.getInstance().getClient(clientId);

		//Resolving server exchange reason
		//If referral balance was used
		if (type == 3) {
			DbHandler.getInstance().addRefDays(clientId, -currentClient.getRefDays());
		}
		//If no server exchange is required
		if (countryTo.contentEquals("--")) {
			PreparedStatement statement = this.connection.prepareStatement(
					"UPDATE clients SET subscr_to=subscr_to+" + subDays * 86400 + " WHERE id=" + clientId);
			statement.execute();
		} else {
			//Resolving server exchange reason
			//If server dissatisfied and got strike
			if (type == 1 && currentClient.getSubscrTo() > System.currentTimeMillis() / 1000L && currentClient.getServer().getState() == Server.State.RUNNING) {
				Strike currentStrike = currentClient
						.getServer()
						.getStrikes()
						.stream()
						.filter(tStrike -> tStrike.getCountry().contentEquals(countryFrom))
						.findFirst()
						.orElse(null);
				if (currentStrike == null) {
					DbHandler.getInstance().createStrike(currentClient.getServer(), countryFrom);
				} else {
					DbHandler.getInstance().addStrike(currentStrike.getId());
				}
			}

			Server optServer;
			List<Server> servers;
			String defaultCountry = Model.getInstance().getSystemConfigValue("default_country");
			//Providing suitable servers list if system default country is not specified
			if (countryTo.contentEquals("OP") && defaultCountry.contentEquals("--"))
				servers = DbHandler
						.getInstance()
						.getServerList()
						.stream()
						.filter(server -> server.getState() == Server.State.RUNNING && server.availableAmount() > 0)
						.collect(Collectors.toList());
			else {
				//Providing suitable servers list if default server country is specified
				if (countryTo.contentEquals("OP")) {
					servers = DbHandler
							.getInstance()
							.getServerList(defaultCountry)
							.stream()
							.filter(server -> server.getState() == Server.State.RUNNING && server.availableAmount() > 0)
							.collect(Collectors.toList());
				}
				//Providing suitable servers list if other country is specified
				else {
					servers = DbHandler
							.getInstance()
							.getServerList(countryTo)
							.stream()
							.filter(server -> server.getState() == Server.State.RUNNING && server.availableAmount() > 0)
							.collect(Collectors.toList());
				}
				//If no servers found within specified countries providing any available
				if (servers.size() == 0
						|| (currentClient.getServer() != null
						&& servers.size() == 1
						&& servers.get(0).getId() == currentClient.getServer().getId()))
					servers = DbHandler
							.getInstance()
							.getServerList()
							.stream()
							.filter(server -> server.getState() == Server.State.RUNNING && server.availableAmount() > 0)
							.collect(Collectors.toList());
			}

			//If after all no suitable servers were found thr NoServerFoundException
			if (servers.size() == 0
					|| (currentClient.getServer() != null
					&& servers.size() == 1
					&& servers.get(0).getId() == currentClient.getServer().getId()))
				throw new NoServerFoundException();

			//Sorting servers by a least strikes & free places amount
			boolean isAscendingOrder = Model.getInstance().getSystemConfigValue("server_selection_order").contentEquals("ascending");
			servers.sort(Comparator.comparing((Server server) -> server.getStrikesByCountry(countryFrom))
					.thenComparing(server1 -> isAscendingOrder
							? server1.getClientsAmount()
							: server1.getUsersLimit() - server1.getClientsAmount()));

			//Looking up for the server to deploy
			if (currentClient.getServer() != null && currentClient.getServer().getId() == servers.get(0).getId() && servers.size() > 1)
				optServer = servers.get(1);
			else optServer = servers.get(0);
/*
            //Preserved for unfilled countries server autodeletion
            if (currentClient.getServer() != null && !currentClient.getServer().getProperty().startsWith("IND")) {
                Thread oldServersConditionCheck
            }
*/
			//If autodeployment is ON then check fulfillment & add new instance
			if (Model.getInstance().getSystemConfigValue("linode_autodeployment").contentEquals("ON"))
				new Thread(new LinodeInstanceDeployer(optServer.getCountry())).start();

			//Adding client IPSEC login to a VPS via SSH
			try {
				Model.lockServer(optServer.getId());
				SSHConnector connector = new SSHConnector(optServer.getIp_addr(), optServer.getConn());
				connector.connect();
				connector.sendCommand("wget -O add_vpn_user.sh https://raw.githubusercontent.com/zdllucky/setup-ipsec-vpn/master/extras/add_vpn_user.sh && sudo sh add_vpn_user.sh '" +
						currentClient.getLogin() + "' '" +
						currentClient.getConn() + "' 'y'");
				connector.close();
				Model.unlockServer(optServer.getId());
			} catch (Exception e) {
				Model.unlockServer(optServer.getId());
				throw e;
			}
			//Removing client from the old server
			new Thread(() -> {
				if (currentClient.getServer() != null && currentClient.getServer().getState() == Server.State.RUNNING)
					Model.getInstance().deleteRemoteSubscription(currentClient);
			}).start();
			//Adding new server information to a client
			PreparedStatement statement = this.connection.prepareStatement("UPDATE clients " +
					"SET server_id=(?), subscr_to=(?), country_from=(?) " +
					"WHERE id=(?)");

			statement.setObject(1, optServer.getId());
			statement.setObject(2,
					Math.max(currentClient.getSubscrTo(), System.currentTimeMillis() / 1000L)
							+ subDays * 3600 * 24);
			statement.setObject(3, countryFrom);
			statement.setObject(4, currentClient.getId());
			statement.execute();
		}
	}

	/**
	 * Deletes server information from the client's record
	 *
	 * @param clientId affected client id
	 * @throws SQLException if query error
	 */
	void deleteSubscriptionServer(int clientId) throws SQLException {
		this.connection.prepareStatement("UPDATE clients " +
				"SET server_id=-1 " +
				"WHERE id=" + clientId)
				.execute();
	}

	/**
	 * Returns clients list attached to a specified server
	 *
	 * @param serverId specified server id
	 * @return {@link ArrayList<Client>} that contains active subscription clients on a specified server
	 * @throws SQLException if query error
	 */
	public List<Client> getActiveClientList(int serverId) throws SQLException {
		Statement statement = this.connection.createStatement();
		List<Client> list = new ArrayList<>();
		Server server = DbHandler.getInstance().getServer(serverId);
		ResultSet resultSet = statement.executeQuery("SELECT id, client_name, email, tel_num, conn, login, register_date, subscr_to, swap_server_attempt, noswap_to, country_from, ref_days, referred_from, clients.language " +
				"FROM clients " +
				"WHERE subscr_to >" + System.currentTimeMillis() / 1000L + " AND server_id=" + serverId);
		while (resultSet.next()) {
			list.add(new Client(resultSet.getInt("id"),
					resultSet.getString("client_name"),
					resultSet.getString("email"),
					resultSet.getLong("tel_num"),
					server,
					resultSet.getString("conn"),
					resultSet.getString("login"),
					resultSet.getLong("register_date"),
					resultSet.getLong("subscr_to"),
					resultSet.getInt("swap_server_attempt"),
					resultSet.getLong("noswap_to"),
					resultSet.getString("country_from"),
					resultSet.getInt("ref_days"),
					resultSet.getInt("referred_from"),
					resultSet.getString("language")
			));
		}
		return list;
	}

	/**
	 * Edits server settings (might be unsafe, be cautious for violent editing)
	 *
	 * @param id         specified server id
	 * @param ipAddr     server's new IP address (0 - 2^32 in "ddd.ddd.ddd.ddd" format)
	 * @param conn       alphanumeric both-case SSH "root" user password string
	 * @param ipsecpsk   alphanumeric both-case IPSec Public Shared Key string
	 * @param usersLimit max limit of user allowed for a new server
	 * @param country    {@value "XX"} code string server country
	 * @param state      server up status (see {@link Server.State} enumeration)
	 * @throws SQLException if query error
	 */
	public void editServer(int id,
	                       String ipAddr,
	                       String conn,
	                       String country,
	                       String ipsecpsk,
	                       Integer usersLimit,
	                       Server.State state) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE servers SET " +
				(ipAddr != null ? "ip_addr='" + ipAddr + "', " : " ") +
				(conn != null ? "conn='" + conn + "', " : " ") +
				(country != null ? "country='" + country + "', " : " ") +
				(ipsecpsk != null ? "ipsecpsk='" + ipsecpsk + "', " : " ") +
				(usersLimit != null ? "users_limit='" + usersLimit + "', " : " ") +
				"state='" + Server.State.compose(state) + "' WHERE id=" + id);
		statement.execute();

	}

	/**
	 * Switches clients from specified server to another similar and then deletes it's record in the database.
	 * The procedure might take much time so it was wrapped in a new {@link Thread}.
	 *
	 * @param id of the server that will be cleared from clients and  deletes it's record in the database
	 */
	public void removeServer(int id) {
		//Running this procedure in a new thread because it may consume much time
		new Thread(() -> {
			try {
				Model.lockServer(id);
				String s = DbHandler.getInstance().getServer(id).getCountry();
				List<Client> clientsLeft = DbHandler.getInstance().getActiveClientList(id);
				this.connection.createStatement().execute("UPDATE servers SET state=0 WHERE id=" + id);
				while (clientsLeft.size() > 0) {
					Client client = clientsLeft.get(0);
					DbHandler.getInstance().setPlan(
							client.getId(),
							0,
							2,
							client.getCountryFrom(),
							DbHandler.getInstance()
									.getServerList(s).stream()
									.filter(server -> server.getState() == Server.State.RUNNING && server.getId() != id)
									.collect(Collectors.summarizingInt(Server::availableAmount))
									.getSum() > 0
									? s
									: "OP");
					clientsLeft.remove(0);
				}
				this.connection.createStatement().execute("DELETE FROM servers " +
						"WHERE id=" + id);
				this.connection.createStatement().execute("DELETE FROM strikes " +
						"WHERE server_id=" + id);
				Model.unlockServer(id);
			} catch (Exception e) {
				Model.unlockServer(id);
			}
		}).start();
	}

	/**
	 * Totally removes client from the system
	 *
	 * @param id client's id
	 * @throws SQLException if query error
	 */
	public void removeClient(int id) throws SQLException {
		Client client = getClient(id);
		if (client.getServer() != null)
			Model.getInstance().deleteRemoteSubscription(client);
		this.connection.createStatement().execute("DELETE FROM clients " +
				"WHERE id=" + id);
	}

	/**
	 * Edits client information (might be unsafe)
	 *
	 * @param id             client id
	 * @param clear_attempts set {@code true} to reset server exchange attempts timer of a client
	 * @param email          new email address of a client
	 * @param clientName     new name of a client
	 * @throws SQLException if query error
	 */
	public void editClient(int id,
	                       boolean clear_attempts,
	                       String email,
	                       String clientName,
	                       Integer clientRefDays,
	                       Integer clientReferredFrom,
	                       String language) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE clients SET " +
				(email != null ? "email='" + email + "', " : " ") +
				(clientName != null ? "client_name='" + clientName + "', " : " ") +
				(clear_attempts ? "swap_server_attempt=0, noswap_to=0, " : " ") +
				(clientRefDays != null ? "ref_days=" + clientRefDays + ", " : " ") +
				(clientReferredFrom != null ? "referred_from=" + clientReferredFrom + ", " : " ") +
				(language != null ? "language='" + language + "', " : " ") +
				"login=login WHERE id=" + id);
		statement.execute();
	}

	/**
	 * @param exception a {@value "XX"} format code string of a country that will not be included into returned list
	 * @return {@link ArrayList<Pair>} of all countries, where {@code Pair.L} is a {@value "XX"} format country code
	 * key string and {@code Pair.R} is an English name string of a country.
	 * @throws SQLException if query error
	 */
	public List<Pair<String, String>> getCountriesList(String exception) throws SQLException {
		List<Pair<String, String>> countries = new ArrayList<>();
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT id, value_en " +
				"FROM countries " +
				"WHERE id NOT LIKE '" + exception + "'");

		while (resultSet.next()) countries.add(new Pair<>(resultSet.getString("id"), resultSet.getString("value_en")));
		return countries;
	}

	/**
	 * @param server specified server id
	 * @return {@link ArrayList<Strike>} of all strikes of a specified server
	 * @throws SQLException if query error
	 */
	public List<Strike> getServerStrikes(Server server) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT * " +
				"FROM strikes " +
				"WHERE server_id=" + server.getId());
		List<Strike> strikeList = new ArrayList<>();
		while (resultSet.next()) {
			strikeList.add(new Strike(
					resultSet.getInt("id"),
					resultSet.getInt("server_id"),
					resultSet.getString("country"),
					resultSet.getInt("amount")));
		}
		return strikeList;
	}

	/**
	 * @return {@link ArrayList<Strike>} of all strikes of all servers
	 * @throws SQLException if query erorr
	 */
	public List<Strike> getStrikes() throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT * " +
				"FROM strikes");
		List<Strike> strikeList = new ArrayList<>();
		while (resultSet.next()) {
			strikeList.add(new Strike(
					resultSet.getInt("id"),
					resultSet.getInt("server_id"),
					resultSet.getString("country"),
					resultSet.getInt("amount")));
		}
		return strikeList;
	}

	/**
	 * Increases strike value by one
	 *
	 * @param id skrike id
	 * @throws SQLException if query error
	 */
	public void addStrike(int id) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE strikes " +
				"SET amount=amount + 1 " +
				"WHERE id=" + id);
		statement.execute();
	}

	/**
	 * Add new country strike record to a database
	 *
	 * @param server  affected server id
	 * @param country {@value "XX"} format country code string
	 * @throws SQLException if query error
	 */
	private void createStrike(Server server, String country) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT INTO strikes(server_id, country, amount) " +
				"VALUES (?, ?, ?)");
		statement.setObject(1, server.getId());
		statement.setObject(2, country);
		statement.setObject(3, 1);
		statement.execute();
	}

	/**
	 * @param id client id
	 * @return {@link Client} object of a specified client id
	 * @throws SQLException if query error
	 */
	public Client getClient(int id) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery(
				"SELECT email, client_name, tel_num, server_id, conn, login, register_date, subscr_to, swap_server_attempt, noswap_to, country_from, ref_days, referred_from, clients.language FROM clients WHERE id=" + id);
		if (resultSet.next())
			return new Client(id,
					resultSet.getString("client_name"),
					resultSet.getString("email"),
					resultSet.getLong("tel_num"),
					resultSet.getLong("subscr_to") > System.currentTimeMillis() / 1000L ? DbHandler.getInstance().getServer(resultSet.getInt("server_id")) : null,
					resultSet.getString("conn"),
					resultSet.getString("login"),
					resultSet.getLong("register_date"),
					resultSet.getLong("subscr_to"),
					resultSet.getInt("swap_server_attempt"),
					resultSet.getLong("noswap_to"),
					resultSet.getString("country_from"),
					resultSet.getInt("ref_days"),
					resultSet.getInt("referred_from"),
					resultSet.getString("language"));
		return null;
	}

	/**
	 * Checks a potentially new user email whether it is disposable or already exists
	 *
	 * @param email email string
	 * @throws SQLException if query error
	 */
	public void checkEmail(String email) throws SQLException {
		if (!email.trim().contains("@"))
			throw new SQLException();
		Statement statement = this.connection.createStatement();
		if (statement.executeQuery("SELECT domains " +
				"FROM disposable_emails " +
				"WHERE domains " +
				"LIKE '" + email.substring(email.indexOf("@") + 1).trim() + "' " +
				"LIMIT 1").next())
			throw new SQLException();
		if (statement.executeQuery("SELECT email " +
				"FROM clients " +
				"WHERE email " +
				"LIKE '" + email + "' " +
				"LIMIT 1").next())
			throw new SQLException();
	}

	/**
	 * Validates administrators login & password
	 *
	 * @param username username
	 * @param password password
	 * @throws SQLException if query error
	 */
	public void validateLogin(String username, String password) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("SELECT * " +
				"FROM admins " +
				"WHERE login LIKE '" + username.replaceAll("[^0-9a-zA-Z]", "") + "' " +
				"AND password LIKE '" + password.replaceAll("[^0-9a-zA-Z]", "") + "'");
		if (!statement.executeQuery().next()) throw new SQLException();
	}

	/**
	 * Returns email configuration record from database
	 *
	 * @param config_id email configuration id
	 * @param language  {@value "en" or "ru"} string that specifies language of returned configuration
	 * @param values    values that will be placed to a declared key variables (see https://mailgun.com/ API
	 *                  documentation & {@link EmailSender} class description)
	 * @return {@link EmailConfig} object that can be used to generate & send an e-mail
	 * @throws SQLException if query error
	 */
	public EmailConfig getEmailConfig(int config_id, String language, List<String> values) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery(
				"SELECT subject_" + language + ", template_" + language + ", variables FROM email_configs WHERE id=" + config_id);
		return new EmailConfig(
				language,
				resultSet.getString("subject_" + language),
				resultSet.getString("template_" + language),
				Arrays.asList(resultSet.getString("variables").split(" *, *")),
				values);
	}

	/**
	 * @param email specifies client email
	 * @return id value of a client by his email address (returns -1 if no matches were found)
	 * @throws SQLException if query error
	 */
	public int getClientId(String email) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery(
				"SELECT id FROM clients WHERE email LIKE '" + email + "'");
		if (resultSet.next())
			return resultSet.getInt("id");
		return -1;
	}

	/**
	 * Validates client login
	 *
	 * @param clientId       client id value
	 * @param clientPassword client password string
	 * @return requested {@link Client} object if login procedure success or {@code null} login attempt failed
	 * @throws SQLException if query error
	 */
	public Client loginClientProcedure(int clientId, String clientPassword) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery(
				"SELECT email, client_name, tel_num, server_id, conn, login, register_date, subscr_to, swap_server_attempt, noswap_to, country_from, ref_days, referred_from, clients.language " +
						"FROM clients " +
						"WHERE id=" + clientId + " AND conn LIKE '" + clientPassword + "'");
		if (resultSet.next())
			return new Client(clientId,
					resultSet.getString("client_name"),
					resultSet.getString("email"),
					resultSet.getLong("tel_num"),
					resultSet.getLong("subscr_to") > System.currentTimeMillis() / 1000L ? DbHandler.getInstance().getServer(resultSet.getInt("server_id")) : null,
					resultSet.getString("conn"),
					resultSet.getString("login"),
					resultSet.getLong("register_date"),
					resultSet.getLong("subscr_to"),
					resultSet.getInt("swap_server_attempt"),
					resultSet.getLong("noswap_to"),
					resultSet.getString("country_from"),
					resultSet.getInt("ref_days"),
					resultSet.getInt("referred_from"),
					resultSet.getString("language"));
		else return null;
	}

	/**
	 * @param country {@value "XX"} format country string
	 * @param lang    {@value "ru" or "en"} required locale
	 * @return {@link String} local country name
	 * @throws SQLException if query error
	 */
	public String getCountryName(String country, String lang) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT value_" + lang + " " +
				"FROM countries " +
				"WHERE countries.id LIKE '" + country + "'");
		return resultSet.getString("value_" + lang);
	}

	/**
	 * Adds bonus VPN days to referrer
	 *
	 * @param id     referrer id value
	 * @param amount amount of days to add
	 * @throws SQLException if query error
	 */
	public void addRefDays(int id, int amount) throws SQLException {
		this.connection.prepareStatement("UPDATE clients " +
				"SET ref_days=ref_days+" + amount + " WHERE id=" + id)
				.execute();
	}

	/**
	 * Updates swap server timer to client after server change
	 *
	 * @param client client id
	 * @throws SQLException if query error
	 */
	public void addNoSwapTimer(Client client) throws SQLException {
		if (client.getSwapServerAttempt() == 0 ||
				(client.getSwapServerAttempt() > 0
						&& client.getNoswapTo() + 14400 < System.currentTimeMillis() / 1000L)) {
			DbHandler.getInstance().setNoSwapTo(client.getId(), 1, 0);
		} else if (client.getSwapServerAttempt() == 1) {
			DbHandler.getInstance().setNoSwapTo(client.getId(), 2, 300);
		} else if (client.getSwapServerAttempt() == 2) {
			DbHandler.getInstance().setNoSwapTo(client.getId(), 3, 1800);
		} else {
			DbHandler.getInstance().setNoSwapTo(client.getId(), 4, 14400);
		}
	}

	/**
	 * Sets noSwap timer duration
	 *
	 * @param id       client id
	 * @param attempt  attempt number
	 * @param duration "no swap to" duration (UNIX time in seconds)
	 * @throws SQLException if query error
	 */
	private void setNoSwapTo(int id, int attempt, long duration) throws SQLException {
		this.connection.prepareStatement(
				"UPDATE clients " +
						"SET swap_server_attempt=" + attempt + ", " +
						"noswap_to=" + (System.currentTimeMillis() / 1000L + duration) +
						" WHERE id=" + id)
				.execute();
	}

	/**
	 * @return {@link ArrayList<LinodeMarkup>} of all linode instance deployment markups
	 * @throws SQLException if query error
	 */
	public List<LinodeMarkup> getLinodeMarkupList() throws SQLException {
		ResultSet resultSet = this.connection
				.createStatement()
				.executeQuery("SELECT id, country, instance_type, user_limit, location_name, enabled " +
						"FROM linode_markups");
		List<LinodeMarkup> linodeMarkupList = new ArrayList<>();
		while (resultSet.next())
			linodeMarkupList.add(new LinodeMarkup(
					resultSet.getInt("id"),
					resultSet.getString("country"),
					resultSet.getString("instance_type"),
					resultSet.getInt("user_limit"),
					resultSet.getString("location_name"),
					resultSet.getInt("enabled") == 1));
		return linodeMarkupList;
	}

	/**
	 * @param initName {@value "XXX"} format code string of an instance provider
	 * @return {@link ArrayList<Server>} of all servers deployed by a specified instance provider
	 * @throws SQLException if query error
	 */
	public List<Server> getServersByInitiator(String initName) throws SQLException {
		List<Server> list = new ArrayList<>();
		ResultSet resultSet = this.connection.createStatement()
				.executeQuery("SELECT id, ip_addr, conn, ipsecpsk, state, add_date, users_limit, country, property " +
						"FROM servers " +
						"WHERE substr(property, 1, 3) LIKE '" + initName + "'");
		while (resultSet.next()) {
			list.add(new Server(resultSet.getInt("id"),
					resultSet.getString("ip_addr"),
					resultSet.getString("conn"),
					resultSet.getString("ipsecpsk"),
					Server.State.parse(resultSet.getInt("state")),
					resultSet.getLong("add_date"),
					resultSet.getInt("users_limit"),
					resultSet.getString("country"),
					resultSet.getString("property")
			));
		}
		return list;
	}

	/**
	 * Adds new Linode markup record to database. "Linode markup" itself is a class template that contains set variables
	 * that are used while creating new Linode instance (see https://linode.com API).
	 *
	 * @param country       {@value "XX"} format country code string of an instance
	 * @param instance_type instance specification by https://linode.com API
	 * @param user_limit    limit of users allowed for a new instances of this type
	 * @param location      instance location identifier by https://linode.com API
	 * @param enabled       markup switcher
	 * @throws SQLException if query error
	 */
	public void addLinodeMarkup(String country,
	                            String instance_type,
	                            int user_limit,
	                            String location,
	                            boolean enabled) throws SQLException {
		if (enabled)
			DbHandler.getInstance().toggleMarkupsCountry(country);

		PreparedStatement statement = this.connection.prepareStatement("INSERT " +
				"INTO linode_markups(country, instance_type, user_limit, location_name, enabled) " +
				"VALUES (?, ?, ?, ?, ?)");
		statement.setObject(1, country);
		statement.setObject(2, instance_type);
		statement.setObject(3, user_limit);
		statement.setObject(4, location);
		statement.setObject(5, enabled ? 1 : 0);
		statement.execute();
	}

	/**
	 * Toggles off every markup of a specified country
	 *
	 * @param country {@value "XX"} format country code string of an instance
	 * @throws SQLException if query error
	 */
	public void toggleMarkupsCountry(String country) throws SQLException {
		this.connection.prepareStatement("UPDATE linode_markups " +
				"SET enabled=0 " +
				"WHERE country LIKE '" + country + "'").execute();
	}

	/**
	 * Toggles on a specified linode markup while disabling any other of the same country as specified one
	 *
	 * @param id linode markup id
	 * @throws SQLException if query error
	 */
	public void toggleMarkup(int id) throws SQLException {
		LinodeMarkup markup = getMarkup(id);

		if (!markup.isEnabled())
			toggleMarkupsCountry(markup.getCountry());
		else if (markup.getCountry().contentEquals(Model.getInstance().getSystemConfigValue("default_country")))
			Model.getInstance().updateConfigOption("default_country", "--");

		this.connection.prepareStatement("UPDATE linode_markups " +
				"SET enabled=" + (markup.isEnabled() ? 0 : 1) +
				" WHERE id=" + id).execute();
		Logger.getInstance().add("Markup toggling", Logger.WARNING, "id: \"" + markup.getId() + "\", country:\"" + markup.getCountry() + "\", result: \"" + (markup.isEnabled() ? "Turned off" : "Turned on") + "\"");
	}

	/**
	 * @param id Linode markup id
	 * @return {@link LinodeMarkup} object of a specified id from the database or {@code null} if found nothing
	 * @throws SQLException if query error
	 */
	public LinodeMarkup getMarkup(int id) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT id, country, instance_type, user_limit, location_name, enabled " +
				"FROM linode_markups WHERE id=" + id);
		if (resultSet.next())
			return new LinodeMarkup(
					resultSet.getInt("id"),
					resultSet.getString("country"),
					resultSet.getString("instance_type"),
					resultSet.getInt("user_limit"),
					resultSet.getString("location_name"),
					resultSet.getInt("enabled") == 1);
		return null;
	}

	/**
	 * @param country {@value "XX"} format country code string of an instance
	 * @return {@link LinodeMarkup} object of an activated markup of a specified country from the database or
	 * {@code null} if found nothing
	 * @throws SQLException if query error
	 */
	public LinodeMarkup getMarkup(String country) throws SQLException {
		Statement statement = this.connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT id, country, instance_type, user_limit, location_name, enabled " +
				"FROM linode_markups " +
				"WHERE country LIKE '" + country + "' AND enabled=1");
		if (resultSet.next())
			return new LinodeMarkup(
					resultSet.getInt("id"),
					resultSet.getString("country"),
					resultSet.getString("instance_type"),
					resultSet.getInt("user_limit"),
					resultSet.getString("location_name"),
					true);
		return null;
	}

	/**
	 * Removes markup record from the database
	 *
	 * @param id Linode markup id of a record to be deleted
	 * @throws SQLException if query error
	 */
	public void deleteMarkup(int id) throws SQLException {
		LinodeMarkup markup = getMarkup(id);
		if (markup.isEnabled() && markup.getCountry().contentEquals(Model.getInstance().getSystemConfigValue("default_country")))
			Model.getInstance().updateConfigOption("default_country", "--");

		this.connection.prepareStatement("DELETE FROM linode_markups WHERE id=" + id).execute();
	}

	/**
	 * @param client      {@link Client} object of a client who's password needs to be updated
	 * @param newPassword new password (alphanumeric string)
	 * @throws SQLException if query error
	 */
	public void changeClientPassword(Client client, String newPassword) throws SQLException {
		this.connection.prepareStatement("UPDATE clients " +
				"SET conn='" + newPassword + "' " +
				"WHERE id=" + client.getId()).execute();
		if (client.getServer() != null) {
			new Thread(() -> {
				try {
					Model.lockServer(client.getServer().getId());

					SSHConnector connector = new SSHConnector(client.getServer().getIp_addr(), client.getServer().getConn());
					connector.connect();
					/*
					 * Seems to be explicit statement because add_vpn_user.sh script will update user password
					 * automatically if user exists already
					 *
					connector.sendCommand(
							"wget -O del_vpn_user.sh https://raw.githubusercontent.com/zdllucky/setup-ipsec-vpn/master/extras/del_vpn_user.sh && sudo sh del_vpn_user.sh '" +
									client.getLogin() + "' 'y'");
					 */
					connector.sendCommand("wget -O add_vpn_user.sh https://raw.githubusercontent.com/zdllucky/setup-ipsec-vpn/master/extras/add_vpn_user.sh && sudo sh add_vpn_user.sh '" +
							client.getLogin() + "' '" +
							newPassword + "' 'y'");
					connector.close();
				} catch (Exception ignored) {
				}
				Model.unlockServer(client.getServer().getId());
			}).start();
		}
	}

	/**
	 * Checks referrer existence
	 *
	 * @param ref_id referrer client id
	 * @return {@code true} if referrer id is valid
	 * @throws SQLException if query error
	 */
	public boolean checkReferrer(int ref_id) throws SQLException {
		return this.connection.createStatement().executeQuery("SELECT EXISTS(SELECT 1 FROM clients WHERE id=" + +ref_id + ") AS res").getInt("res") == 1;
	}

	/**
	 * @return platform configs records from the database stored as key and value {@link HashMap<>}
	 * @throws SQLException if query error
	 */
	public HashMap<String, String> getSystemConfigs() throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery(
				"SELECT * FROM nectedadmin_configs");
		HashMap<String, String> configs = new HashMap<>();
		while (resultSet.next()) {
			configs.put(resultSet.getString("config_name"), resultSet.getString("vars"));
		}
		return configs;
	}

	/**
	 * Updates the specified config's value
	 *
	 * @param option specified config option string (key value)
	 * @param value  some new value for the specified config
	 * @return {@code true} if config was found and updated
	 * @throws SQLException if query error
	 */
	public boolean updateConfigOption(String option, String value) throws SQLException {
		return this.connection.prepareStatement("UPDATE nectedadmin_configs SET vars='" + value + "' WHERE config_name LIKE '" + option + "'").executeUpdate() == 1;
	}

	/**
	 * Performs database raw query and returns requested result table for the SQL "SELECT" command and {@code null}
	 * for any other type of successful SQL commands performed. Also updates platform configs list after any queries
	 * except those with "SELECT" commands were queried
	 *
	 * @param sql_query raw SQL query string
	 * @return {@link ArrayList<ArrayList>} matrix of strings, where the very first row contains column names of a
	 * query result
	 * @throws SQLException if query error
	 */
	public List<List<String>> executeRawQuery(String sql_query) throws SQLException {
		if (sql_query.substring(0, 7).trim().toLowerCase().startsWith("select")) {
			List<List<String>> queryResult = new ArrayList<>();
			ResultSet resultSet = this.connection.createStatement().executeQuery(sql_query);

			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			queryResult.add(new ArrayList<>());

			for (int i = 1; i <= columnCount; i++)
				queryResult.get(0).add(metaData.getColumnName(i) + " (" + metaData.getColumnTypeName(i) + ")");

			while (resultSet.next()) {
				List<String> row = new ArrayList<>();
				for (int i = 1; i <= columnCount; i++) {
					row.add(String.valueOf(resultSet.getObject(i)));
				}
				queryResult.add(row);
			}
			return queryResult;
		} else {
			this.connection.prepareStatement(sql_query).execute();
			Model.getInstance().updateSystemConfigsList();
			return null;
		}
	}

	/**
	 * Adds a new payment record to a database
	 *
	 * @param paymentId         payment id (must be unique)
	 * @param fee               money received from a client
	 * @param daysAmount        amounts of days purchased
	 * @param coupon            applied coupon information (optional)
	 * @param clientId          customer client id
	 * @param currentTimeMillis current payment UNIX timestamp (milliseconds)
	 * @param paymentType       payment method that was used
	 * @throws SQLException if query error
	 */
	public int addPaymentRecord(String paymentId,
	                            String fee,
	                            String daysAmount,
	                            String coupon,
	                            int clientId,
	                            long currentTimeMillis,
	                            String paymentType) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"INSERT INTO payments(payment_id, fee, days_amount, coupon_info, client_id, time_stamp, payment_method) " +
						"VALUES(?, ?, ?, ?, ?, ?, ?)");

		statement.setObject(1, paymentId);
		statement.setObject(2, fee);
		statement.setObject(3, daysAmount);
		statement.setObject(4, coupon);
		statement.setObject(5, clientId);
		statement.setObject(6, currentTimeMillis);
		statement.setObject(7, paymentType);

		statement.executeUpdate();

		return statement.getGeneratedKeys().getInt(1);
	}

	/**
	 * @param clientId specified client identifier
	 * @return {@link List<Payment>} of all payments performed by a specified user
	 * @throws SQLException if query error
	 */
	public List<Payment> getClientPayments(int clientId) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT payment_id, fee, days_amount, coupon_info, time_stamp, payment_method " +
				"FROM payments " +
				"WHERE client_id=" + clientId);

		List<Payment> payments = new ArrayList<>();

		while (resultSet.next())
			payments.add(new Payment(
					resultSet.getString("payment_id"),
					resultSet.getString("fee"),
					resultSet.getInt("days_amount"),
					resultSet.getString("coupon_info"),
					clientId,
					Long.parseLong(resultSet.getString("time_stamp")),
					resultSet.getString("payment_method")
			));

		return payments;
	}

	public int addMassMailingTemplate(String subjectRu,
	                                  String subjectEn,
	                                  String bodyRu,
	                                  String bodyEn,
	                                  String label,
	                                  String credentials) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT " +
				"INTO email_templates(type, subject_ru, subject_en, body_ru, body_en, label, credentials) " +
				"VALUES(-1, ?, ?, ?, ?, ?, ?)");
		statement.setString(1, subjectRu);
		statement.setString(2, subjectEn);
		statement.setString(3, bodyRu);
		statement.setString(4, bodyEn);
		statement.setString(5, label);
		statement.setString(6, credentials);
		statement.executeUpdate();
		return statement.getGeneratedKeys().getInt(1);
	}

	public int addPersonalMailingTemplate(String type,
	                                      boolean state,
	                                      long timeToTrig,
	                                      String SQLApproval,
	                                      String subjectRu,
	                                      String subjectEn,
	                                      String bodyRu,
	                                      String bodyEn,
	                                      String label,
	                                      String credentials) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT " +
				"INTO email_templates(type, state, time_to_trig, sql_approval, subject_ru, subject_en, body_ru, body_en, label, credentials) " +
				"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		statement.setString(1, type);
		statement.setInt(2, state ? 1 : 0);
		statement.setLong(3, timeToTrig);
		statement.setString(4, SQLApproval);
		statement.setString(5, subjectRu);
		statement.setString(6, subjectEn);
		statement.setString(7, bodyRu);
		statement.setString(8, bodyEn);
		statement.setString(9, label);
		statement.setString(10, credentials);
		statement.executeUpdate();
		return statement.getGeneratedKeys().getInt(1);
	}

	public MailingTemplate getMailingTemplate(int id) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT * " +
				"FROM email_templates " +
				"WHERE id=" + id);

		if (resultSet.next()) {
			if (resultSet.getString("type").equals("-1")) {
				return new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						resultSet.getString("subject_ru"),
						resultSet.getString("subject_en"),
						resultSet.getString("body_ru"),
						resultSet.getString("body_en"),
						resultSet.getString("credentials"));
			} else {
				return new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						resultSet.getInt("state") == 1,
						resultSet.getString("type"),
						resultSet.getString("sql_approval"),
						resultSet.getLong("time_to_trig"),
						resultSet.getString("subject_ru"),
						resultSet.getString("subject_en"),
						resultSet.getString("body_ru"),
						resultSet.getString("body_en"),
						resultSet.getString("credentials"));
			}
		} else return null;
	}

	public int removeMailingTemplate(int id) throws SQLException {
		this.connection.prepareStatement("DELETE " +
				"FROM mailing_schedule " +
				"WHERE template_id = " + id).execute();
		return this.connection.prepareStatement("DELETE " +
				"FROM email_templates " +
				"WHERE id = " + id)
				.executeUpdate() == 1 ? id : -1;
	}

	public void editPersonalMailingTemplate(int id,
	                                        boolean state,
	                                        long timeToTrig,
	                                        String SQLApproval,
	                                        String subjectRu,
	                                        String subjectEn,
	                                        String bodyRu,
	                                        String bodyEn,
	                                        String label,
	                                        String credentials) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE email_templates " +
				"SET state = (?), time_to_trig = (?), sql_approval = (?), subject_ru = (?), subject_en = (?), body_ru = (?), body_en = (?), label= (?), credentials= (?) " +
				"WHERE id =" + id);
		statement.setInt(1, state ? 1 : 0);
		statement.setLong(2, timeToTrig);
		statement.setString(3, SQLApproval);
		statement.setString(4, subjectRu);
		statement.setString(5, subjectEn);
		statement.setString(6, bodyRu);
		statement.setString(7, bodyEn);
		statement.setString(8, label);
		statement.setString(9, credentials);
		statement.execute();
	}

	public void editMassMailingTemplate(int id,
	                                    String subjectRu,
	                                    String subjectEn,
	                                    String bodyRu,
	                                    String bodyEn,
	                                    String label,
	                                    String credentials) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE email_templates " +
				"SET subject_ru = (?), subject_en = (?), body_ru = (?), body_en = (?), label = (?), credentials = (?) " +
				"WHERE id = " + id);
		statement.setString(1, subjectRu);
		statement.setString(2, subjectEn);
		statement.setString(3, bodyRu);
		statement.setString(4, bodyEn);
		statement.setString(5, label);
		statement.setString(6, credentials);
		statement.execute();
	}

	public List<MailingTemplate> getMailingTemplateList(int page, int by) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT id, type, label, time_to_trig, state, sql_approval, credentials " +
				"FROM email_templates " + " " +
				"ORDER BY (CASE WHEN type = -1 THEN 0 ELSE state END) DESC, id DESC " +
				"LIMIT " + (page - 1) * by + ", " + by);

		List<MailingTemplate> templates = new ArrayList<>();
		while (resultSet.next()) {
			if (resultSet.getString("type").equals("-1"))
				templates.add(new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						null,
						null,
						null,
						null,
						resultSet.getString("credentials")));
			else
				templates.add(new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						resultSet.getInt("state") == 1,
						resultSet.getString("type"),
						resultSet.getString("sql_approval"),
						resultSet.getLong("time_to_trig"),
						null,
						null,
						null,
						null,
						resultSet.getString("credentials")));
		}
		return templates;
	}

	public List<MailingTemplate> getMailingTemplateList(String procedureName) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT id, type, sql_approval, time_to_trig, subject_ru, subject_en, body_ru, body_en, state, label, credentials " +
				"FROM email_templates " +
				"WHERE type LIKE '" + procedureName + "' AND state = 1");

		List<MailingTemplate> templates = new ArrayList<>();
		while (resultSet.next()) {
			if (resultSet.getString("sql_approval").isBlank())
				templates.add(new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						resultSet.getInt("state") == 1,
						resultSet.getString("type"),
						resultSet.getString("sql_approval"),
						resultSet.getLong("time_to_trig"),
						resultSet.getString("subject_ru"),
						resultSet.getString("subject_en"),
						resultSet.getString("body_ru"),
						resultSet.getString("body_en"),
						resultSet.getString("credentials")));
			else
				templates.add(new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						resultSet.getInt("state") == 1,
						resultSet.getString("type"),
						resultSet.getString("sql_approval"),
						resultSet.getLong("time_to_trig"),
						null,
						null,
						null,
						null,
						null));
		}
		return templates;
	}

	public int toggleMailingServlet(int id, boolean toState) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE email_templates " +
				"SET state = " + (toState ? 1 : 0) + " " +
				"WHERE id = " + id);
		return statement.executeUpdate() == 1 ? id : -1;
	}

	public int scheduleMassMailing(int templateId, long activationTime, String SQLSelection) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT " +
				"INTO mailing_schedule(selection, activation_time, template_id) " +
				"VALUES (?, ?, ?)");
		statement.setString(1, SQLSelection);
		statement.setLong(2, activationTime);
		statement.setInt(3, templateId);
		statement.executeUpdate();
		return statement.getGeneratedKeys().getInt(1);
	}


	public int schedulePersonalMailing(int associatedUser, MailingTemplate t, String logReference) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT " +
				"INTO mailing_schedule(selection, activation_time, template_id, log_reference) " +
				"VALUES (?, ?, ?, ?)");
		statement.setString(1, String.valueOf(associatedUser));
		statement.setLong(2, System.currentTimeMillis() + t.getTimeToTrig() * 1000L);
		statement.setInt(3, t.getId());
		statement.setString(4, logReference);
		statement.executeUpdate();
		return statement.getGeneratedKeys().getInt(1);
	}

	public int getTotalMailingTasksAmount(boolean selectOutdatedOnly) throws SQLException {
		return this.connection.createStatement().executeQuery("SELECT COUNT(*) " +
				"AS amount " +
				"FROM mailing_schedule " +
				"WHERE activation_time " +
				(selectOutdatedOnly ? "<" : ">") + " " +
				System.currentTimeMillis()).getInt("amount");
	}

	public List<MailingTask> getMailingTasksList(int page, int by, List<MailingTemplate> templates) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT id, selection, activation_time, template_id " +
				"FROM mailing_schedule " +
				"WHERE activation_time > " + System.currentTimeMillis() + " " +
				"ORDER BY activation_time " +
				"LIMIT " + (page - 1) * by + ", " + by);


		List<MailingTask> tasks = new ArrayList<>();
		while (resultSet.next()) {
			int templateId = resultSet.getInt("template_id");
			tasks.add(new MailingTask(
					resultSet.getLong("id"),
					resultSet.getString("selection"),
					resultSet.getLong("activation_time"),
					templates.stream().filter(template -> template.getId() == templateId).findFirst().orElse(null),
					""));
		}
		return tasks;
	}

	public List<MailingTask> getMailingTasksList(long date, List<MailingTemplate> templates) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT id, selection, activation_time, template_id, log_reference " +
				"FROM mailing_schedule " +
				"WHERE activation_time < " + date);


		List<MailingTask> tasks = new ArrayList<>();
		while (resultSet.next()) {
			int templateId = resultSet.getInt("template_id");
			tasks.add(new MailingTask(
					resultSet.getLong("id"),
					resultSet.getString("selection"),
					resultSet.getLong("activation_time"),
					templates.stream().filter(template -> template.getId() == templateId).findFirst().orElse(null),
					resultSet.getString("log_reference")
			));
		}
		return tasks;
	}

	public int removeMailingTask(long id) throws SQLException {
		return connection.prepareStatement("DELETE " +
				"FROM mailing_schedule " +
				"WHERE id = " + id)
				.executeUpdate();
	}

	public List<MailingTemplate> getMailingTemplateList(boolean isWholeDataRequired) throws SQLException {
		ResultSet resultSet = this.connection.createStatement().executeQuery("SELECT" + (isWholeDataRequired ? " * " : " id, type, label ") +
				"FROM email_templates");

		List<MailingTemplate> templates = new ArrayList<>();
		while (resultSet.next()) {
			if (resultSet.getString("type").equals("-1"))
				templates.add(new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						isWholeDataRequired ? resultSet.getString("subject_ru") : null,
						isWholeDataRequired ? resultSet.getString("subject_en") : null,
						isWholeDataRequired ? resultSet.getString("body_ru") : null,
						isWholeDataRequired ? resultSet.getString("body_en") : null,
						isWholeDataRequired ? resultSet.getString("credentials") : null));
			else
				templates.add(new MailingTemplate(
						resultSet.getInt("id"),
						resultSet.getString("label"),
						isWholeDataRequired && resultSet.getInt("state") == 1,
						isWholeDataRequired ? resultSet.getString("type") : null,
						isWholeDataRequired ? resultSet.getString("sql_approval") : null,
						isWholeDataRequired ? resultSet.getLong("time_to_trig") : -1,
						isWholeDataRequired ? resultSet.getString("subject_ru") : null,
						isWholeDataRequired ? resultSet.getString("subject_en") : null,
						isWholeDataRequired ? resultSet.getString("body_ru") : null,
						isWholeDataRequired ? resultSet.getString("body_en") : null,
						isWholeDataRequired ? resultSet.getString("credentials") : null));
		}
		return templates;
	}

	public HashMap<String, String> getClientCredentials(String SQLSelection, boolean onlyEmailRequired) throws SQLException {
		ResultSet resultSet;
		HashMap<String, String> credentials = new HashMap<>();
		if (onlyEmailRequired) {
			resultSet = this.connection.createStatement().executeQuery("SELECT email, `language` FROM clients WHERE " + SQLSelection);
			while (resultSet.next())
				credentials.put(
						resultSet.getString(1),
						resultSet.getString(2));
		} else {
			resultSet = this.connection.createStatement().executeQuery("SELECT client_name, email, `language` FROM clients WHERE " + SQLSelection);
			while (resultSet.next())
				credentials.put(EmailSender.getInstance().parseContact(
						resultSet.getString(2),
						resultSet.getString(1)),
						resultSet.getString(3));
		}
		return credentials;
	}

	public void clearMailingTaskList(long millisTime) throws SQLException {
		this.connection.prepareStatement("DELETE FROM mailing_schedule WHERE activation_time < " + millisTime).execute();
	}
}