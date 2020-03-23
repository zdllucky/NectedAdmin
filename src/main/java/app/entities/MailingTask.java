package app.entities;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class MailingTask {
	private long id;
	private String selection;
	private long activationTime;
	private MailingTemplate template;
	private String logReference;
	private Map<String, String> logReferenceMap = null;

	public MailingTask(long id, String selection, long activationTime, MailingTemplate template, String logReference) {
		this.id = id;
		this.selection = selection;
		this.activationTime = activationTime;
		this.template = template;
		this.logReference = logReference;
	}

	public static Map<String, String> parseLogReference(String reference) {

		return Arrays
				.stream(reference.split(","))
				.map(s -> s.split(":", 2))
				.collect(Collectors.toMap(s -> s[0].trim(), s -> s[1].trim().substring(1, s[1].trim().length() - 1)));
	}

	public boolean isPersonal() {
		return template.isPersonal();
	}

	public long getId() {
		return id;
	}

	public String getSelection() {
		return selection;
	}

	public long getActivationTime() {
		return activationTime;
	}

	public MailingTemplate getTemplate() {
		return template;
	}

	public String getLogReference() {
		return logReference;
	}

	public Map<String, String> getLogReferenceMap() {
		if (logReferenceMap == null)
			return logReferenceMap = MailingTask.parseLogReference(logReference);
		else return logReferenceMap;
	}

}
