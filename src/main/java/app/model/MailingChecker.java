package app.model;

public class MailingChecker implements Runnable {
	@Override
	public void run() {
		long creationTime = System.currentTimeMillis();
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
