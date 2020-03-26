package app.model;

public class MailingChecker implements Runnable {
	private long creationTime;

	public MailingChecker() {
		creationTime = System.currentTimeMillis();
	}

	@Override
	public void run() {
		try {
			Model.getInstance().checkMailingTasks();
		} catch (Exception ignored) {
		}

		try {
			Thread.sleep(Long.parseLong(Model.getInstance().getSystemConfigValue("mailing_timer")) * 1000L);
			Model.getInstance().resetMailingCheckerThread(creationTime);
		} catch (Exception e) {
			Logger.getInstance().add("Mailing timer exception", Logger.ERROR, Logger.parseException(e));
		}
	}
}
