package app.model;

public class ClientChecker implements Runnable {
	@Override
	public void run() {
		long creationTime = System.currentTimeMillis();
		try {
			Model.getInstance().checkSubscriptions();
		} catch (Exception ignored) {
		}

		try {
			Thread.sleep(Long.parseLong(Model.getInstance().getSystemConfigValue("autoremoval_timer")) * 1000L);
			Model.getInstance().resetClientCheckerThread(creationTime);
		} catch (Exception e) {
			Logger.getInstance().add("Subs removal timer exception", Logger.ERROR, "exception: \"" + Logger.parseException(e) + "\"");
		}
	}
}
