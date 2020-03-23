package app.entities;

import java.util.List;

public class EmailConfig {
	private String language;
	private String subject;
	private String template;
	private List<String> variables;
	private List<String> values;

	public EmailConfig(String language, String subject, String template, List<String> variables, List<String> values) {
		this.language = language;
		this.subject = subject;
		this.template = template;
		this.variables = variables;
		this.values = values;
	}

	public String getLanguage() {
		return language;
	}

	public String getSubject() {
		return subject;
	}

	public String getTemplate() {
		return template;
	}

	public List<String> getVariables() {
		return variables;
	}

	public List<String> getValues() {
		return values;
	}
}
