package app.entities;

public class Strike {
	private int id;
	private int serverId;
	private String country;
	private int amount;

	public Strike(int id, int serverId, String country, int amount) {
		this.id = id;
		this.serverId = serverId;
		this.country = country;
		this.amount = amount;
	}

	public int getId() {
		return id;
	}

	public int getServerId() {
		return serverId;
	}

	public String getCountry() {
		return country;
	}

	public int getAmount() {
		return amount;
	}
}
