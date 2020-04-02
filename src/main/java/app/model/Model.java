package app.model;

import app.entities.Client;
import app.entities.MailingTask;
import app.entities.Pair;
import io.ipinfo.api.IPInfo;
import io.ipinfo.api.cache.SimpleCache;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;

import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Model {
	public static final String DANGER = "danger";
	public static final String WARNING = "warning";
	public static final String SUCCESS = "success";
	public static final String INFO = "info";
	private static Model instance;
	private static volatile List<Integer> lockedServers = new ArrayList<>();
	private static IPInfo ipInfo;
	private static HashMap<String, String> systemConfigs;
	private static Thread clientCheckerThread = null;
	private static Thread mailingCheckerThread = null;

	private static Long systemStartTime = Long.MAX_VALUE;

	/**
	 * @return {@link Model} object singleton
	 */
	public synchronized static Model getInstance() {
		if (instance == null) {
			instance = new Model();

			try {
				//Initialisation
				//Database & system configs initialisation
				Model.getInstance().updateSystemConfigsList();

				//IPResolver initialisation
				ipInfo = IPInfo
						.builder()
						.setToken(Model.getInstance().getSystemConfigValue("ipinfo_token"))
						.setCache(new SimpleCache(Duration.ofDays(5)))
						.build();

				//Email sending service initialisation
				EmailSender.getInstance();

				systemStartTime = System.currentTimeMillis();

				//Starting expired clients server data cleanup
				if (instance.getSystemConfigValue("subscriptions_autoremoval").equals("ON"))
					(clientCheckerThread = new Thread(new ClientChecker())).start();

				//Starting expired clients server data cleanup
				if (instance.getSystemConfigValue("auto_mailing").equals("ON"))
					(clientCheckerThread = new Thread(new MailingChecker())).start();

				Logger.getInstance().add("System initialization", Logger.INFO, instance.getSystemConfigsList().toString());
			} catch (Exception e) {
				Logger.getInstance().add("System initialization", Logger.ERROR, Logger.parseException(e));
			}
		}
		return instance;
	}

	/**
	 * @param remoteAddr IP address of a remote host
	 * @return the DNS name string associated with that IP address
	 */
	public static String getHostName(String remoteAddr) {
		try {
			return InetAddress.getByName(remoteAddr).getHostName();
		} catch (UnknownHostException ignored) {
			return "undefined";
		}
	}

	/**
	 * Adds the specified server id to {@code lockedServers} list.
	 * If the specified server is busy, then waits upon its unlocking.
	 * The {@code lockedServers} list contains list of servers id
	 * those have an opened SSH session.
	 *
	 * @param serverId specified server id
	 * @throws InterruptedException if thread is broken
	 */
	public static synchronized void lockServer(int serverId) throws InterruptedException {
		while (ifServerLocked(serverId))
			Thread.sleep(50);
		lockedServers.add(serverId);
	}

	/**
	 * Removes the specified server id from {@code lockedServers} list,
	 * making it free to create another SSH session on that server.
	 *
	 * @param serverId specified server id
	 */
	public static synchronized void unlockServer(int serverId) {
		lockedServers.remove((Integer) serverId);
	}

	/**
	 * Checks if the specified server is in the {@code lockedServers} list.
	 *
	 * @param serverId specified server id
	 * @return {@code true} if the server has no active SSH sessions.
	 */
	public static synchronized boolean ifServerLocked(int serverId) {
		return lockedServers.contains(serverId);
	}

	/**
	 * System method that is used to restart a new {@link ClientChecker} thread
	 * after the current thread finished its job and system param
	 * 'subscriptions_autoremoval' is set to 'ON'.
	 * May produce some "ghost" threads!!! If that occurs then restart the tomcat
	 * server!
	 *
	 * @param checkerCreationTime the creation UNIX time of a current thread, that
	 *                            *                        is used to stop thread if it war run before
	 *                            *                        context reload
	 */
	public void resetClientCheckerThread(long checkerCreationTime) {
		if (Model.getInstance().getSystemConfigValue("subscriptions_autoremoval").equals("ON")) {
			if (Thread.currentThread().getName().equals(clientCheckerThread.getName())
					&& checkerCreationTime > systemStartTime) {
				clientCheckerThread = new Thread(new ClientChecker());
				clientCheckerThread.setName(String.valueOf(System.currentTimeMillis()));
				clientCheckerThread.start();
			}

		} else {
			clientCheckerThread = null;
		}
	}

	/**
	 * System method that is used to restart a new {@link ClientChecker} thread
	 * after the current thread finished its job and system param 'auto_mailing'
	 * is set to 'ON'.
	 * May produce some "ghost" threads!!! If that occurs then restart the tomcat
	 * server!
	 *
	 * @param checkerCreationTime the creation UNIX time of a current thread, that
	 *                            is used to stop thread if it war run before
	 *                            context reload
	 */
	public void resetMailingCheckerThread(long checkerCreationTime) {
		if (Model.getInstance().getSystemConfigValue("auto_mailing").equals("ON")) {
			if (Thread.currentThread().getName().equals(mailingCheckerThread.getName())
					&& checkerCreationTime > systemStartTime) {
				mailingCheckerThread = new Thread(new MailingChecker());
				mailingCheckerThread.setName(String.valueOf(System.currentTimeMillis()));
				mailingCheckerThread.start();
			}
		} else {
			mailingCheckerThread = null;
		}
	}

	/**
	 * Finds expired subscription clients and deletes their server information
	 *
	 * @throws SQLException if database operation error
	 */
	public void checkSubscriptions() throws SQLException {
		List<Client> expiredClients = DbHandler.getInstance().getExpiredNotClearedClients();
		String expiredClientsString = "total_amount: " + expiredClients.size() + ", client_ids_list: \"" + expiredClients.stream().map(client -> String.valueOf(client.getId())).collect(Collectors.joining(", ")) + "\"";
		Logger.getInstance().add("Expired subs removal", Logger.INFO, expiredClientsString);
		while (expiredClients.size() > 0) {
			for (Client tClient : expiredClients) {
				if (tClient.getServer() == null) {
					DbHandler.getInstance().deleteSubscriptionServer(tClient.getId());
					continue;
				}
				Model.getInstance().deleteRemoteSubscription(tClient);
				DbHandler.getInstance().deleteSubscriptionServer(tClient.getId());
				Logger.getInstance().add("Subscription expired", tClient.getSubscrTo(), tClient.getId(), Logger.INFO, "");
			}
			expiredClients = DbHandler.getInstance().getExpiredNotClearedClients();
		}
	}


	/**
	 * Finds mailing tasks awaiting for the sending and initiates email sending
	 *
	 * @throws SQLException if database operation error
	 */
	public void checkMailingTasks() throws SQLException {
		List<MailingTask> pendingTasks = DbHandler.getInstance().getTotalMailingTasksAmount(true) > 0
				? DbHandler.getInstance().getMailingTasksList(
				System.currentTimeMillis(),
				DbHandler.getInstance().getMailingTemplateList(true))
				: new ArrayList<>();

		for (MailingTask task : pendingTasks) {
			try {
				List<Pair<String, String>> params = new ArrayList<>();
				if (task.getTemplate().getCredentials().equals("ads_box"))
					params.add(new Pair<>("o:tag", "offer"));

				if (task.isPersonal()
						&& DbHandler.getInstance().executeRawQuery((task.getTemplate().getSQLApproval()).replace("<$CLIENT.ID$>", task.getSelection())).get(1).get(0).equals("true")) {
					Client client = DbHandler.getInstance().getClient(Integer.parseInt(task.getSelection()));

					String subject = EmailSender.getInstance().parseMessage(
							task.getTemplate().getSubject(client.getLang()),
							client,
							task.getLogReferenceMap());
					String body = EmailSender.getInstance().parseMessage(
							task.getTemplate().getBody(client.getLang()),
							client,
							task.getLogReferenceMap());

					EmailSender.getInstance().sendEmail(
							EmailSender.CREDENTIALS.get(task.getTemplate().getCredentials() + "_" + client.getLang()),
							EmailSender.getInstance().parseContact(client),
							subject,
							body,
							params);
				} else if (!task.isPersonal()) {
					HashMap<String, String> mailingClientCredentials = DbHandler.getInstance().getClientCredentials(task.getSelection());

					EmailSender.getInstance().sendEmail(
							EmailSender.CREDENTIALS.get(task.getTemplate().getCredentials() + "_en"),
							mailingClientCredentials.entrySet()
									.stream()
									.filter(c -> c.getValue().equals("en"))
									.map(Map.Entry::getKey)
									.collect(Collectors.joining(",")),
							task.getTemplate().getSubject("en"),
							task.getTemplate().getBody("en"),
							params);

					EmailSender.getInstance().sendEmail(
							EmailSender.CREDENTIALS.get(task.getTemplate().getCredentials() + "_ru"),
							mailingClientCredentials.entrySet()
									.stream()
									.filter(c -> c.getValue().equals("ru"))
									.map(Map.Entry::getKey)
									.collect(Collectors.joining(",")),
							task.getTemplate().getSubject("ru"),
							task.getTemplate().getBody("ru"),
							params);
				}
				DbHandler.getInstance().removeMailingTask(task.getId());
			} catch (Exception ignored) {
				DbHandler.getInstance().removeMailingTask(task.getId());
			}
		}
		Logger.getInstance().add("Automailing",
				Logger.INFO,
				"total_amount: \"" + pendingTasks.size() + "\", personal_and_instant_amount: \"" + pendingTasks.stream().filter(MailingTask::isPersonal).count() + "\", mass_amount: \"" + pendingTasks.stream().filter(t -> !t.isPersonal()).count() + "\"");
	}

	/**
	 * Connects to remote server and removes client VPN profile from it
	 *
	 * @param client specified client
	 */
	public void deleteRemoteSubscription(Client client) {
		try {
			Model.lockServer(client.getServer().getId());
			SSHConnector connector = new SSHConnector(client.getServer().getIp_addr(), client.getServer().getConn());
			connector.connect();
			connector.sendCommand(
					"wget -O del_vpn_user.sh https://raw.githubusercontent.com/zdllucky/setup-ipsec-vpn/master/extras/del_vpn_user.sh && sudo sh del_vpn_user.sh '" +
							client.getLogin() + "' 'y'");
			connector.close();
			Model.unlockServer(client.getServer().getId());
		} catch (Exception e) {
			Model.unlockServer(client.getServer().getId());
		}
	}

	/**
	 * Adds specified action message to a current session
	 *
	 * @param session     session of a user
	 * @param messageType can be {@value INFO}, {@value WARNING},
	 *                    {@value DANGER} or {@value SUCCESS}
	 * @param message     message contents string
	 */
	public void outputMessage(HttpSession session, String messageType, String message) {
		session.setAttribute("result", messageType);
		session.setAttribute("shout", message);
	}

	/**
	 * @param ip specified IP address string
	 * @return object containing all data related to a specified IP address
	 * @throws RateLimitedException if ip address requests amount exceed account limit
	 */
	public IPResponse getIpData(String ip) throws RateLimitedException {
		return ipInfo.lookupIP(ip);
	}

	/**
	 * @return system configs list
	 */
	public HashMap<String, String> getSystemConfigsList() {
		return systemConfigs;
	}

	/**
	 * Updates system configs list
	 *
	 * @throws SQLException if query error
	 */
	public void updateSystemConfigsList() throws SQLException {
		systemConfigs = DbHandler.getInstance().getSystemConfigs();
	}

	/**
	 * @param key specified parameter key string
	 * @return parameter value
	 */
	public String getSystemConfigValue(String key) {
		return systemConfigs.getOrDefault(key, "");
	}

	/**
	 * Changes specified config option && watches changes of critical variables
	 *
	 * @param option specified parameter key string
	 * @param value  new value
	 * @throws SQLException if query error
	 */
	public void updateConfigOption(String option, String value) throws SQLException {
		String exValue = Model.getInstance().getSystemConfigValue(option);
		if (DbHandler.getInstance().updateConfigOption(option, value)) {
			updateSystemConfigsList();
			Logger.getInstance().add("System configs edition", Logger.INFO, "Option: \"" + option + "\", from \"" + exValue + "\" to \"" + value + "\"");
		} else throw new SQLException();

		if ((option.equals("autoremoval_timer") && systemConfigs.get("subscriptions_autoremoval").equals("ON"))
				|| option.equals("subscriptions_autoremoval") && value.equals("ON"))
			(clientCheckerThread = new Thread(new ClientChecker())).start();

		if (option.equals("auto_mailing") && value.equals("ON"))
			DbHandler.getInstance().clearMailingTaskList(System.currentTimeMillis());

		if ((option.equals("mailing_timer") && systemConfigs.get("auto_mailing").equals("ON"))
				|| option.equals("auto_mailing") && value.equals("ON"))
			(mailingCheckerThread = new Thread(new MailingChecker())).start();
	}

}
