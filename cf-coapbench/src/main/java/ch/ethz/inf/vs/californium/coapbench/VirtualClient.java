package ch.ethz.inf.vs.californium.coapbench;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.producer.VeryEcoMessageProducer;

/**
 * A virtual client sends request to the server as fast as it can handle them.
 */
public class VirtualClient implements Runnable {

	public static final int TIMEOUT = 10000;
	
	private DatagramSocket socket;
	private DatagramPacket pSend;
	private DatagramPacket pRecv;
	private VeryEcoMessageProducer producer;
	
	private boolean runnable;
	private int counter;
	private int lost;
	
	private InetAddress address;
	private int port;
	private byte[] mid;
	private long timestamp;
	
	private IntArray latencies;
	
	private boolean checkMID = true;
	private boolean checkCode = true;
	
	public VirtualClient(URI uri) throws Exception {
		this.mid = new byte[2];
		this.latencies = new IntArray();
		this.producer = new VeryEcoMessageProducer();
		this.socket = new DatagramSocket();
		this.socket.setSoTimeout(TIMEOUT);
		this.pSend = new DatagramPacket(new byte[0], 0);
		this.pRecv = new DatagramPacket(new byte[100], 100);
		this.runnable = true;
		setURI(uri);
	}
	
	public void setURI(URI uri)  throws UnknownHostException {
		address = InetAddress.getByName(uri.getHost());
		port = uri.getPort();
		producer.setURI(uri);
	}
	
	public void run() {
		try {
			while (runnable) {
				sendRequest();
				receiveResponse();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendRequest() throws IOException {
		byte[] bytes = producer.next();
		saveMID(bytes);
		pSend.setData(bytes);
		pSend.setAddress(address);
		pSend.setPort(port);
		timestamp = System.nanoTime();
		socket.send(pSend);
	}
	
	public void receiveResponse() throws IOException {
		try {
			boolean mid_correct;
			long latency;
			do {
				socket.receive(pRecv);
				latency = System.nanoTime() - timestamp;
				byte[] resp = pRecv.getData();
				mid_correct = checkMID(resp);
				checkCode(resp);
			} while (!mid_correct);
			latencies.add((int) (latency / 1000000));
			counter++;
		} catch (SocketTimeoutException e) {
			System.out.println("Timeout occured");
			lost++;
		}
	}
	
	public void stop() {
		runnable = false;
	}
	
	public void reset() {
		runnable = true;
		counter = 0;
		lost = 0;
	}
	
	public int getCount() {
		return counter;
	}
	
	public int getTimeouted() {
		return lost;
	}
	
	public IntArray getLatencies() {
		return latencies;
	}
	
	private void saveMID(byte[] bytes) {
		mid[0] = bytes[2];
		mid[1] = bytes[3];
	}
	
	private boolean checkMID(byte[] bytes) {
		if (checkMID && 
				(bytes[2] != mid[0] || bytes[3]!=mid[1]) ) {
			System.err.println("Received message with wrong MID");
			return false;
		}
		return true;
	}
	
	private void checkCode(byte[] bytes) {
		byte c = bytes[1];
		if (checkCode && c != CoAP.ResponseCode.CONTENT.value) {
			System.err.println("Did not receive Content as response code but "+c);
		}
	}
}
