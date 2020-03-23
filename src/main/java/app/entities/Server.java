package app.entities;

import app.model.DbHandler;

import java.sql.SQLException;
import java.util.List;

public class Server {
	private int id;
	private String ip_addr;
	private String conn;
	private String ipSecPSK;
	private State state;
	private long addDate;
	private int usersLimit;
	private String country;
	private String property;

	public Server(int id, String ip_addr, String conn, String ipSecPSK, State state, long addDate, int usersLimit, String country, String property) {
		this.id = id;
		this.ip_addr = ip_addr;
		this.conn = conn;
		this.ipSecPSK = ipSecPSK;
		this.state = state;
		this.addDate = addDate;
		this.usersLimit = usersLimit;
		this.country = country;
		this.property = property;
	}

	public long getAddDate() {
		return addDate;
	}

	public int getUsersLimit() {
		return usersLimit;
	}

	public String getCountry() {
		return country;
	}

	public List<Strike> getStrikes() throws SQLException {
		return DbHandler.getInstance().getServerStrikes(this);
	}

	public int getStrikesByCountry(String country) {
		try {
			return DbHandler.getInstance()
					.getServerStrikes(this).stream()
					.filter(strike -> strike.getCountry().contentEquals(country))
					.findFirst()
					.orElse(new Strike(0, 0, country, Integer.MAX_VALUE))
					.getAmount();
		} catch (SQLException e) {
			return Integer.MAX_VALUE;
		}
	}

	public String getProperty() {
		return property;
	}

	public int availableAmount() {
		return this.usersLimit - this.getClientsAmount();
	}

	public int getId() {
		return id;
	}

	public String getIp_addr() {
		return ip_addr;
	}

	public String getConn() {
		return conn;
	}

	public String getIpSecPSK() {
		return ipSecPSK;
	}

	public State getState() {
		return state;
	}

	public int getClientsAmount() {
		try {
			return DbHandler.getInstance().getClientsAmount(id);
		} catch (SQLException e) {
			return usersLimit;
		}
	}

	public enum State {
		NOT_SET_UP, SETTING_UP, DEPRECATED, RUNNING;

		public static State parse(int state) {
			try {
				return values()[state + 2];
			} catch (Exception e) {
				return DEPRECATED;
			}
		}

		public static int compose(State state) {
			switch (state) {
				case RUNNING:
					return 1;
				case DEPRECATED:
					return -0;
				case SETTING_UP:
					return -1;
				case NOT_SET_UP:
					return -2;
				default:
					return 0;
			}
		}
	}
}
