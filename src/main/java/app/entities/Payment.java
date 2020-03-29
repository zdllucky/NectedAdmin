package app.entities;

public class Payment {
	private String id;
	private String fee;
	private int daysAmount;
	private String couponInfo;
	private int clientId;
	private long timeStamp;
	private String paymentMethod;

	public Payment(String id, String fee, int daysAmount, String couponInfo, int clientId, long timeStamp, String paymentMethod) {
		this.id = id;
		this.fee = fee;
		this.daysAmount = daysAmount;
		this.couponInfo = couponInfo;
		this.clientId = clientId;
		this.timeStamp = timeStamp;
		this.paymentMethod = paymentMethod;
	}

	public String getId() {
		return id;
	}

	public String getFee() {
		return fee;
	}

	public int getDaysAmount() {
		return daysAmount;
	}

	public String getCouponInfo() {
		return couponInfo;
	}

	public int getClientId() {
		return clientId;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	@Override
	public String toString() {
		return "id: \"" + id + '\"' +
				", fee: \"" + fee + '\"' +
				", daysAmount: \"" + daysAmount + '\"' +
				", couponInfo: \"" + couponInfo + '\"' +
				", clientId: \"" + clientId + '\"' +
				", timeStamp: \"" + timeStamp + '\"' +
				", paymentMethod: \"" + paymentMethod + '\"';
	}
}
