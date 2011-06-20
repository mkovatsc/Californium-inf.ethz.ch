package example;

import java.util.HashMap;
import java.util.Map;

enum Enum
{
	// Enumerations ////////////////////////////////////////////////////////////
	
	AAA(5, "Item_A"),
	BBB(6, "Item_B"),
	CCC(7, "Item_C");
	
	// Conversion //////////////////////////////////////////////////////////////
	
	public static Enum fromCode(int code) {
		return codeMap.get(code);
	}
	public static Enum fromString(String string) {
		return stringMap.get(string);
	}
	
	public int toCode() {
		return code;
	}

	@Override
	public String toString() {
		return string;
	}

	// Private Constructor /////////////////////////////////////////////////////
	
	private Enum(int code, String string) {
		this.code = code;
		this.string = string;
	}
	
	// Attributes //////////////////////////////////////////////////////////////
	
	private int code;
	private String string;
	
	// Static Members //////////////////////////////////////////////////////////
	
	private static Map<String, Enum> stringMap
		= new HashMap<String, Enum>();
	private static Map<Integer, Enum> codeMap
		= new HashMap<Integer, Enum>();
	
	// Class Initialization ////////////////////////////////////////////////////
	
	static {
		for (Enum e : Enum.values()) {
			stringMap.put(e.string, e);
			codeMap.put(e.code, e);
		}
	}
}

public class EnumTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(Enum.AAA);
	}

}
