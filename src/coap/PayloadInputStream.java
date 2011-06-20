package coap;

import java.io.IOException;
import java.io.InputStream;

public class PayloadInputStream extends InputStream {

	public PayloadInputStream(Message message) {
		this.message = message;
		this.pos = 0;
	}

	@Override
	public int read() throws IOException {

		int value = message.readPayload(pos);
		if (value != -1) {
			++pos;
		}
		return value;
	}

	public String readString(String charsetName) throws IOException {
		int size = available();
		if (size > 0) {
			byte[] bytes = new byte[size];
			int bytesRead = read(bytes);
			if (bytesRead >= 0) {
				return new String(bytes, charsetName);
			}
		}
		return null;
	}

	public String readString() throws IOException {
		return readString("UTF-8");
	}

	@Override
	public int available() {
		return message.payloadSize() - pos;
	}

	protected Message message;
	protected int pos;
}
