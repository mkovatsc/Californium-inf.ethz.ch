package ch.inf.vs.californium.coap;

public class CoAP {
	
	public enum Code {
		GET(1), POST(2), PUT(3), DELETE(4);
		
		public final int value;
		
		Code(int value) {
			this.value = value;
		}
		
		public static Code valueOf(int value) {
			switch (value) {
				case 1: return GET;
				case 2: return POST;
				case 3: return PUT;
				case 4: return DELETE;
				default: throw new IllegalArgumentException("Unknwon CoAP code "+value);
			}
		}
	}
	
	public enum Type {
		CON(0), NCON(1), ACK(2), RST(3);
		
		public final int value;
		
		Type(int value) {
			this.value = value;
		}
		
		public static Type valueOf(int value) {
			switch (value) {
				case 0: return CON;
				case 1: return NCON;
				case 2: return ACK;
				case 3: return RST;
				default: throw new IllegalArgumentException("Unknown CoAP type "+value);
			}
		}
	}
	

	// This is overkill... remove it again
//	public enum OptionInfo {
//		IF_MATCH(true, false, false, true, OptionFormat.OPAQUE);
//		
//		public final boolean critical;
//		public final boolean unsafe;
//		public final boolean no_cache;
//		public final boolean repeatable;
//		public final OptionFormat format;
//		
//		OptionInfo(boolean c, boolean u, boolean n, boolean r, OptionFormat f) {
//			this.critical = c;
//			this.unsafe = u;
//			this.no_cache = n;
//			this.repeatable = r;
//			this.format = f;
//		}
//	}
//	public enum OptionFormat {
//		STRING, UINT, OPAQUE;
//	}
	
	// draft-ietf-core-coap-14
	public static final int RESERVED_0 =      0;
	public static final int IF_MATCH =        1;
	public static final int URI_HOST =        3;
	public static final int ETAG =            4;
	public static final int IF_NONE_MATCH =   5;
	public static final int URI_PORT =        7;
	public static final int LOCATION_PATH =   8;
	public static final int URI_PATH =       11;
	public static final int CONTENT_TYPE =   12;
	public static final int MAX_AGE =        14;
	public static final int URI_QUERY =      15;
	public static final int ACCEPT =         16;
	public static final int LOCATION_QUERY = 20;
	public static final int PROXY_URI =      35;
	public static final int PROXY_SCHEME =   39;

	// draft-ietf-core-observe-08
	public static final int OBSERVE = 6;

	// draft-ietf-core-block-10
	public static final int BLOCK2 = 23;
	public static final int BLOCK1 = 27;
	public static final int SIZE =   28;
	
	
}
