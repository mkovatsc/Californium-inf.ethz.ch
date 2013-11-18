package ch.ethz.inf.vs.californium.test.lockstep;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.serializer.DataParser;
import ch.ethz.inf.vs.californium.network.serializer.Serializer;
import ch.ethz.inf.vs.elements.RawData;
import ch.ethz.inf.vs.elements.RawDataChannel;
import ch.ethz.inf.vs.elements.UDPConnector;

public class LockstepEndpoint {

	private UDPConnector connector;
	private InetSocketAddress destination;
	private LinkedBlockingQueue<RawData> incoming;
	
	private HashMap<String, Object> storage;

	public LockstepEndpoint() {
		this.storage = new HashMap<String, Object>();
		this.incoming = new LinkedBlockingQueue<RawData>();
		this.connector = new UDPConnector(new InetSocketAddress(0));
		this.connector.setRawDataReceiver(new RawDataChannel() {
			public void receiveData(RawData raw) {
				incoming.offer(raw);
			}
		});
		
		try {
			connector.start();
			Thread.sleep(100);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public RequestExpectation expectRequest() {
		return new RequestExpectation();
	}
	
	public ResponseExpecation expectResponse() {
		return new ResponseExpecation();
	}
	
	public ResponseExpecation expectResponse(Type type, ResponseCode code, byte[] token, int mid) {
		return expectResponse().type(type).code(code).token(token).mid(mid);
	}
	
	public RequestProperty sendRequest(Type type, Code code, byte[] token, int mid) {
		if (type == null) throw new NullPointerException();
		if (code == null) throw new NullPointerException();
		if (token == null) throw new NullPointerException();
		if (mid < 0 || mid > (2<<16)-1) throw new RuntimeException();
		return new RequestProperty(type, code, token, mid);
	}
	
	public EmptyMessageProperty sendEmpty(Type type) {
		if (type == null) throw new NullPointerException();
		return sendEmpty(type, Message.NONE);
	}
	
	public EmptyMessageProperty sendEmpty(Type type, int mid) {
		return new EmptyMessageProperty(type, mid);
	}
	
	public void send(RawData raw) {
		if (raw.getAddress() == null)
			if (destination != null)
				raw.setAddress(destination.getAddress());
			else throw new RuntimeException("Message has no destination address");
		if (raw.getPort() == 0)
			if (destination != null)
				raw.setPort(destination.getPort());
			else throw new RuntimeException("Message has no destination port");
		
		connector.send(raw);
	}
	
	public void setDestination(InetSocketAddress destination) {
		this.destination = destination;
	}
	
	public abstract class MessageExpectation implements Action {
		
		private List<Expectation<Message>> expectations = new LinkedList<LockstepEndpoint.Expectation<Message>>();
		
		public MessageExpectation mid(final int mid) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					Assert.assertEquals("Wrong MID:", mid, message.getMID());
				}
			});
			return this;
		}

		public MessageExpectation type(final Type type) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					Assert.assertEquals("Wrong type:", type, message.getType());
				}
			});
			return this;
		}

		public MessageExpectation token(final byte[] token) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					org.junit.Assert.assertArrayEquals("Wrong token:", token, message.getToken());
				}
			});
			return this;
		}
		
		public MessageExpectation payload(final String payload) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					Assert.assertEquals("Wrong payload:", payload, message.getPayloadString());
				}
			});
			return this;
		}
		
		public MessageExpectation block1(final int num, final boolean m, final int size) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					Assert.assertTrue("No Block1 option:", message.getOptions().hasBlock1());
					BlockOption block1 = message.getOptions().getBlock1();
					Assert.assertEquals("Wrong Block1 num:", num, block1.getNum());
					Assert.assertEquals("Wrong Block1 m:", m, block1.isM());
					Assert.assertEquals("Wrong Block1 size:", size, block1.getSize());
				}
			});
			return this;
		}
		
		public MessageExpectation block2(final int num, final boolean m, final int size) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					Assert.assertTrue("No Block2 option:", message.getOptions().hasBlock2());
					BlockOption block2 = message.getOptions().getBlock2();
					Assert.assertEquals("Wrong Block2 num:", num, block2.getNum());
					Assert.assertEquals("Wrong Block2 m:", m, block2.isM());
					Assert.assertEquals("Wrong Block2 size:", size, block2.getSize());
				}
			});
			return this;
		}
		
		public MessageExpectation observe(final int observe) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					Assert.assertTrue("No observe option:", message.getOptions().hasObserve());
					int actual = message.getOptions().getObserve();
					Assert.assertEquals("Wrong observe sequence number:", observe, actual);
				}
			});
			return this;
		}
		
		public MessageExpectation noOption(final int... numbers) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					List<Option> options = message.getOptions().asSortedList();
					for (Option option:options) {
						for (int n:numbers) {
							if (option.getNumber() == n) {
								Assert.assertTrue("Must not have option number "+n+" but has", false);
							}
						}
					}
				}
			});
			return this;
		}
		
		public MessageExpectation storeMID(final String var) {
			expectations.add(new Expectation<Message>() {
				public void check(Message message) {
					storage.put(var, message.getMID());
				}
			});
			return this;
		}
		
		public void check(Message message) {
			for (Expectation<Message> expectation:expectations)
				expectation.check(message);
		}
	}
	
	public class RequestExpectation extends MessageExpectation {
		
		private List<Expectation<Request>> expectations = new LinkedList<LockstepEndpoint.Expectation<Request>>();
		
		public RequestExpectation mid(final int mid) {
			super.mid(mid); return this;
		}

		public RequestExpectation type(final Type type) {
			super.type(type); return this;
		}

		public RequestExpectation token(final byte[] token) {
			super.token(token); return this;
		}

		public RequestExpectation payload(final String payload) {
			super.payload(payload); return this;
		}
		
		public RequestExpectation block1(final int num, final boolean m, final int size) {
			super.block1(num, m, size); return this;
		}
		
		public RequestExpectation block2(final int num, final boolean m, final int size) {
			super.block2(num, m, size); return this;
		}
		
		public RequestExpectation observe(final int observe) {
			super.observe(observe); return this;
		}

		public RequestExpectation noOption(final int... numbers) {
			super.noOption(numbers); return this;
		}
		
		public RequestExpectation storeMID(final String var) {
			super.storeMID(var); return this;
		}
		
		public void code(final Code code) {
			expectations.add(new Expectation<Request>() {
				public void check(Request request) {
					Assert.assertEquals(code, request.getCode());
				}
			});
		}
		
		public void check(Request request) {
			super.check(request);
			for (Expectation<Request> expectation:expectations)
				expectation.check(request);
		}

		public void go() throws Exception {
			RawData raw = incoming.poll(1, TimeUnit.SECONDS); // or take()?
			DataParser parser = new DataParser(raw.getBytes());
			
			if (parser.isRequest()) {
				Request request = parser.parseRequest();
				request.setSource(raw.getAddress());
				request.setSourcePort(raw.getPort());
				check(request);
				
			} else {
				throw new RuntimeException("Expected request but did not receive one");
			}
		}
	}
	
	public class ResponseExpecation extends MessageExpectation {
		
		private List<Expectation<Response>> expectations = new LinkedList<LockstepEndpoint.Expectation<Response>>();
		
		public ResponseExpecation mid(final int mid) {
			super.mid(mid); return this;
		}

		public ResponseExpecation type(final Type type) {
			super.type(type); return this;
		}

		public ResponseExpecation token(final byte[] token) {
			super.token(token); return this;
		}

		public ResponseExpecation payload(final String payload) {
			super.payload(payload); return this;
		}
		
		public ResponseExpecation block1(final int num, final boolean m, final int size) {
			super.block1(num, m, size); return this;
		}
		
		public ResponseExpecation block2(final int num, final boolean m, final int size) {
			super.block2(num, m, size); return this;
		}
		
		public ResponseExpecation observe(final int observe) {
			super.observe(observe); return this;
		}

		public ResponseExpecation noOption(final int... numbers) {
			super.noOption(numbers); return this;
		}
		
		public ResponseExpecation storeMID(final String var) {
			super.storeMID(var); return this;
		}
		
		public ResponseExpecation code(final ResponseCode code) {
			expectations.add(new Expectation<Response>() {
				public void check(Response request) {
					Assert.assertEquals(code, request.getCode());
				}
			});
			return this;
		}
		
		public void check(Response response) {
			super.check(response);
			for (Expectation<Response> expectation:expectations)
				expectation.check(response);
		}

		public void go() throws Exception {
			RawData raw = incoming.poll(1, TimeUnit.SECONDS); // or take() ?
			Assert.assertNotNull(raw);
			DataParser parser = new DataParser(raw.getBytes());
			
			if (parser.isResponse()) {
				Response response = parser.parseResponse();
				response.setSource(raw.getAddress());
				response.setSourcePort(raw.getPort());
				check(response);
				
			} else {
				throw new RuntimeException("Expected response but did not receive one");
			}
		}
	}
	
	public static interface Expectation<T> {
		public void check(T t);
	}
	
	public static interface Property<T> {
		public void set(T t);
	}
	
	public abstract class MessageProperty implements Action {
		
		private List<Property<Message>> properties = new LinkedList<LockstepEndpoint.Property<Message>>();
		
		private Type type;
		private byte[] token;
		private int mid;
		
		public MessageProperty(Type type, byte[] token, int mid) {
			this.type = type;
			this.token = token;
			this.mid = mid;
		}
		
		public void setProperties(Message message) {
			message.setType(type);
			message.setToken(token);
			message.setMID(mid);
			for (Property<Message> property:properties)
				property.set(message);
		}
		
		public MessageProperty block1(final int num, final boolean m, final int size) {
			properties.add(new Property<Message>() {
				public void set(Message message) {
					message.getOptions().setBlock1(BlockOption.size2Szx(size), m, num);
				}
			});
			return this;
		}
		
		public MessageProperty block2(final int num, final boolean m, final int size) {
			properties.add(new Property<Message>() {
				public void set(Message message) {
					message.getOptions().setBlock2(BlockOption.size2Szx(size), m, num);
				}
			});
			return this;
		}
		
		public MessageProperty observe(final int observe) {
			properties.add(new Property<Message>() {
				public void set(Message message) {
					message.getOptions().setObserve(observe);
				}
			});
			return this;
		}
		
		public MessageProperty loadMID(final String var) {
			properties.add(new Property<Message>() {
				public void set(Message message) {
					int mid = (Integer) storage.get(var);
					message.setMID(mid);
				}
			});
			return this;
		}
	}
	
	public class EmptyMessageProperty extends MessageProperty {

		public EmptyMessageProperty(Type type, int mid) {
			super(type, new byte[0], mid);
		}
	
		public void go() {
			EmptyMessage message = new EmptyMessage(null);
			setProperties(message);
			
			Serializer serializer = new Serializer();
			RawData raw = serializer.serialize(message);
			send(raw);
		}
	}
	
	public class RequestProperty extends MessageProperty {
		
		private List<Property<Request>> properties = new LinkedList<LockstepEndpoint.Property<Request>>();
		
		private Code code;
		
		public RequestProperty(Type type, Code code, byte[] token, int mid) {
			super(type, token, mid);
			this.code = code;
		}
		
		public RequestProperty block1(final int num, final boolean m, final int size) {
			super.block1(num, m, size); return this;
		}
		
		public RequestProperty block2(final int num, final boolean m, final int size) {
			super.block2(num, m, size); return this;
		}
		
		public RequestProperty observe(final int observe) {
			super.observe(observe); return this;
		}
		
		public RequestProperty payload(final String payload) {
			properties.add(new Property<Request>() {
				public void set(Request request) {
					request.setPayload(payload);
				}
			});
			return this;
		}
		
		public RequestProperty path(final String path) {
			properties.add(new Property<Request>() {
				public void set(Request request) {
					request.getOptions().setURIPath(path);
				}
			});
			return this;
		}
		
		public void setProperties(Request request) {
			super.setProperties(request);
			for (Property<Request> property:properties)
				property.set(request);
		}

		public void go() {
			Request request = new Request(code);
			setProperties(request);
			
			Serializer serializer = new Serializer();
			RawData raw = serializer.serialize(request);
			send(raw);
		}
	}
	
	public static interface Action {
		public void go() throws Exception;
	}
	
}
