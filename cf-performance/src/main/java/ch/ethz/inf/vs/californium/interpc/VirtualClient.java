package ch.ethz.inf.vs.californium.interpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Random;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.producer.VeryEcoMessageProducer;

/**
 * A virtual client sends request to the server as fast as it can handle them.
 */
public class VirtualClient implements Runnable {

	public static final int TIMEOUT = 10000;
	public static String TARGET = "hello";
	public static boolean CHECK_CODE = true;
	
	private DatagramSocket socket;
	private DatagramPacket pSend;
	private DatagramPacket pRecv;
	private VeryEcoMessageProducer producer;
	
	private boolean runnable;
	private int counter;
	private int lost;
	
	private Random rand = new Random();
	private int[] ports;
	private InetAddress address;
	
	public VirtualClient(InetAddress address, int[] ports) throws Exception {
		this.address = address;
		this.ports = ports;
		producer = new VeryEcoMessageProducer(address.getHostAddress()+":"+ports[0]+"/"+TARGET);
		socket = new DatagramSocket();
//		socket.connect(address, port);
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
		pSend.setAddress(address);
		pSend.setPort(ports[rand.nextInt(ports.length)]);
		socket.send(pSend);
	}
	
	public void receiveResponse() throws IOException {
		try {
			socket.receive(pRecv);
			int c = pRecv.getData()[1];
			if (CHECK_CODE && c != CoAP.ResponseCode.CONTENT.value) {
				System.err.println("Did not receive Content as response code but "+c);
			}
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
