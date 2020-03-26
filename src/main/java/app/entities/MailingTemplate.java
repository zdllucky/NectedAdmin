package app.entities;

public class MailingTemplate {
	private int id;
	private boolean type;
	private String label;
	private boolean state;
	private String triggerLogName;
	private String SQLApproval;
	private long timeToTrig;
	private String subjectRu;
	private String subjectEn;
	private String bodyRu;
	private String bodyEn;
	private String credentials;

	public MailingTemplate(int id,
	                       String label,
	                       String subjectRu,
	                       String subjectEn,
	                       String bodyRu,
	                       String bodyEn,
	                       String credentials) {
		this.id = id;
		this.label = label;
		this.type = false;
		this.subjectRu = subjectRu;
		this.subjectEn = subjectEn;
		this.bodyRu = bodyRu;
		this.bodyEn = bodyEn;
		this.credentials = credentials;
	}

	public MailingTemplate(int id,
	                       String label,
	                       boolean state,
	                       String triggerLogName,
	                       String SQLApproval,
	                       long timeToTrig,
	                       String subjectRu,
	                       String subjectEn,
	                       String bodyRu,
	                       String bodyEn,
	                       String credentials) {
		this.id = id;
		this.type = true;
		this.label = label;
		this.state = state;
		this.triggerLogName = triggerLogName;
		this.SQLApproval = SQLApproval;
		this.timeToTrig = timeToTrig;
		this.subjectRu = subjectRu;
		this.subjectEn = subjectEn;
		this.bodyRu = bodyRu;
		this.bodyEn = bodyEn;
		this.credentials = credentials;
	}

	public int getId() {
		return id;
	}

	public boolean isPersonal() {
		return type;
	}

	public boolean isEnabled() {
		return state;
	}

	public String getTriggerLogName() {
		return triggerLogName;
	}

	public String getSQLApproval() {
		return SQLApproval;
	}

	public long getTimeToTrig() {
		return timeToTrig;
	}

	public String getSubject(String lang) {
		return lang.equals("ru") ? subjectRu : subjectEn;
	}

	public String getBody(String lang) {
		return lang.equals("ru") ? bodyRu : bodyEn;
	}

	public String getLabel() {
		return label;
	}

	public boolean isInstant() {
		return SQLApproval == null || SQLApproval.isBlank();
	}

	public String getCredentials() {
		return credentials;
	}
}
