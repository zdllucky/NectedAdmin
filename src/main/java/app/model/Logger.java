package app.model;

import app.entities.Client;
import app.entities.MailingTask;
import app.entities.MailingTemplate;
import app.entities.Pair;
import org.sqlite.JDBC;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

public class Logger {
	public static final short INFO = 1;
	public static final short WARNING = 0;
	public static final short ERROR = -1;
	private static final String LOGGER_DB_PATH = "jdbc:sqlite:/var/lib/tomcat9/NectedDB/logger";
	private static Logger instance = null;
	private Connection connection;

	private Logger() throws SQLException {
		Properties SQLProps = new Properties();
		SQLProps.put("charset", "UTF8");
		SQLProps.put("useUnicode", "true");
		DriverManager.registerDriver(new JDBC());
		this.connection = DriverManager.getConnection(LOGGER_DB_PATH, SQLProps);
		this.connection.setReadOnly(false);
	}

	public static synchronized Logger getInstance() {
		if (instance == null) {
			try {
				instance = new Logger();
			} catch (SQLException ignored) {
			}
		}
		return instance;
	}

	public static String parseException(Exception e) {
		StringWriter stringWriter = new StringWriter();
		e.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}

	public void add(String procedureName,
	                short result) {
		add(procedureName, result, "");
	}

	public void add(String procedureName,
	                short result,
	                String reference) {
		add(procedureName, -1, result, reference);
	}

	public void add(String procedureName,
	                int associatedUser,
	                short result,
	                String reference) {
		add(procedureName, System.currentTimeMillis() / 1000L, associatedUser, result, reference);

	}

	public void add(String procedureName,
	                long timestamp,
	                int associatedUser,
	                short result,
	                String reference) {
		new Thread(() -> {
			try {
				PreparedStatement statement = connection.prepareStatement("INSERT " +
						"INTO log_records(procedure_name, client_id, time_stamp, result, reference) " +
						"VALUES (?, ?, ?, ?, ?)");

				statement.setString(1, procedureName);
				statement.setInt(2, associatedUser);
				statement.setLong(3, timestamp);
				statement.setInt(4, result);
				statement.setString(5, reference);
				statement.execute();

				//Checking mailing requirements for the action performed
				if (associatedUser != -1 && result == Logger.INFO) {
					List<MailingTemplate> templates = DbHandler.getInstance().getMailingTemplateList(procedureName);
					for (MailingTemplate t : templates) {
						if (t.isInstant() && Model.getInstance().getSystemConfigValue("auto_mailing").equals("ON")) {
							Client client = DbHandler.getInstance().getClient(associatedUser);
							String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
							SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
							dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

							List<Pair<String, String>> params = new ArrayList<>();
							params.add(new Pair<>("o:deliverytime", dateFormat.format(System.currentTimeMillis() + t.getTimeToTrig() * 1000L)));

							String subject = EmailSender.getInstance().parseMessage(t.getSubject(client.getLang()), client, MailingTask.parseLogReference(reference));
							String body = EmailSender.getInstance().parseMessage(t.getBody(client.getLang()), client, MailingTask.parseLogReference(reference));

							EmailSender.getInstance().sendEmail(
									EmailSender.CREDENTIALS.get("informer_box_" + client.getLang()),
									EmailSender.getInstance().parseContact(client),
									subject,
									body,
									params);
						} else {
							DbHandler.getInstance().schedulePersonalMailing(associatedUser, t, reference);
						}
					}
				}
			} catch (Exception e) {
				Logger.getInstance().add(procedureName + " exception", associatedUser, Logger.ERROR, "exception: \"" + Logger.parseException(e) + "\"");
			}
		}).start();
	}

	/**
	 * Performs database raw query and returns requested result table for the SQL "SELECT" command and {@code null}
	 * for any other type of successful SQL commands performed. Also updates platform configs list after any queries
	 * except those with "SELECT" commands were queried
	 *
	 * @param sql_query raw SQL query string
	 * @return {@link ArrayList <ArrayList>} matrix of strings, where the very first row contains column names of a
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
}
