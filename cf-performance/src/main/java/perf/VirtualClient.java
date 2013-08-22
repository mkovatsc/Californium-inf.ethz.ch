package perf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import ch.inf.vs.californium.perf.throughput.VeryEcoMessageProducer;

public class VirtualClient implements Runnable {

	public static final int TIMEOUT = 5000;
	public static final String TARGET = "hello";
	
	private DatagramSocket socket;
	private DatagramPacket pSend;
	private DatagramPacket pRecv;
	private VeryEcoMessageProducer producer;
	
	private boolean runnable;
	private int counter;
	private int lost;
	
	public VirtualClient(InetAddress address, int port) throws Exception {
		producer = new VeryEcoMessageProducer(address.getHostAddress()+":"+port+"/"+TARGET);
		socket = new DatagramSocket();
		socket.connect(address, port);
		socket.setSoTimeout(TIMEOUT);
		pSend = new DatagramPacket(new byte[0], 0);
		pRecv = new DatagramPacket(new byte[100], 100);
		this.runnable = true;
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
		pSend.setData(bytes);
		socket.send(pSend);
	}
	
	public void receiveResponse() throws IOException {
		try {
			socket.receive(pRecv);
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
	
	public int getLost() {
		return lost;
	}
}
