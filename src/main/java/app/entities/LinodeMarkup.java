package app.entities;

public class LinodeMarkup {
	private int id;
	private String country;
	private String instanceType;
	private int userLimit;
	private String locationName;
	private boolean enabled;

	public LinodeMarkup(int id, String country, String instanceType, int userLimit, String locationName, boolean enabled) {
		this.id = id;
		this.country = country;
		this.instanceType = instanceType;
		this.userLimit = userLimit;
		this.locationName = locationName;
		this.enabled = enabled;
	}

	public int getId() {
		return id;
	}

	public String getCountry() {
		return country;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public int getUserLimit() {
		return userLimit;
	}

	public String getLocationName() {
		return locationName;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
