package ch.ethz.inf.vs.californium.dtls;

public enum ContentType {

	CHANGE_CIPHER_SPEC(20), ALERT(21), HANDSHAKE(22), APPLICATION_DATA(23);

	private int id;

	public int getId() {
		return id;
	}

	ContentType(int id) {
		this.id = id;
	}

	public static ContentType getTypeByValue(int id) {
		switch (id) {
		case 20:
			return ContentType.CHANGE_CIPHER_SPEC;
		case 21:
			return ContentType.ALERT;
		case 22:
			return ContentType.HANDSHAKE;
		case 23:
			return ContentType.APPLICATION_DATA;

		default:
			// TODO what to return on default?
			return ContentType.ALERT;
		}
	}

	@Override
	public String toString() {
		switch (id) {
		case 20:
			return "Change Cipher Spec (20)";
		case 21:
			return "Alert (21)";
		case 22:
			return "Handshake (22)";
		case 23:
			return "Application Data (23)";

		default:
			return "Unknown Content Type";
		}
	}
}