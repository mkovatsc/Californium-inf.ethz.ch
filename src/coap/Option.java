package coap;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * This class describes the functionality of the CoAP messages
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class Option {

	// Constructors ////////////////////////////////////////////////////////////

	/*
	 * This method creates a new Option object with dynamic type corresponding
	 * to its option number.
	 * 
	 * @param nr The option number
	 * 
	 * @return A new option with a given number based and corresponding 
	 * dynamic type
	 * 
	 */
	static Option fromNumber(int nr) {
		switch (nr) {
		case OptionNumberRegistry.BLOCK1:
		case OptionNumberRegistry.BLOCK2:
			return new BlockOption(nr);
		default:
			return new Option(nr);
		}
	}

	/*
	 * This is a constructor for a new option with a given number.
	 * 
	 * @param nr The option number
	 * 
	 * @return A new option with a given number based on a byte array
	 */
	public Option (int nr) {
		setOptionNr(nr);
	}
	
	/*
	 * This is a constructor for a new option with a given number, based on a
	 * given byte array
	 * 
	 * @param raw The byte array
	 * 
	 * @param nr The option number
	 * 
	 * @return A new option with a given number based on a byte array
	 */
	public Option(byte[] raw, int nr) {
		setValue(raw);
		setOptionNr(nr);
	}

	/*
	 * This is a constructor for a new option with a given number, based on a
	 * given string
	 * 
	 * @param str The string
	 * 
	 * @param nr The option number
	 * 
	 * @return A new option with a given number based on a string
	 */
	public Option(String str, int nr) {
		setStringValue(str);
		setOptionNr(nr);
	}

	/*
	 * This is a constructor for a new option with a given number, based on a
	 * given integer value
	 * 
	 * @param val The integer value
	 * 
	 * @param nr The option number
	 * 
	 * @return A new option with a given number based on a integer value
	 */
	public Option(int val, int nr) {
		setIntValue(val);
		setOptionNr(nr);
	}

	// Procedures //////////////////////////////////////////////////////////////

	/*
	 * This method sets the data of the current option based on a string input
	 * 
	 * @param str The string representation of the data which is stored in the
	 * current option.
	 */
	public void setStringValue(String str) {
		value = ByteBuffer.wrap(str.getBytes());
	}

	/*
	 * This method sets the data of the current option based on a integer value
	 * 
	 * @param val The integer representation of the data which is stored in the
	 * current option.
	 */
	public void setIntValue(int val) {
		int neededBytes = 4;
		if (val == 0) {
			value = ByteBuffer.allocate(1);
			value.put((byte) 0);
		} else {
			ByteBuffer aux = ByteBuffer.allocate(4);
			aux.putInt(val);
			for (int i = 3; i >= 0; i--) {
				if (aux.get(3 - i) == 0x00) {
					neededBytes--;
				} else {
					break;
				}
			}
			value = ByteBuffer.allocate(neededBytes);
			for (int i = neededBytes - 1; i >= 0; i--) {
				value.put(aux.get(3 - i));
			}
		}
	}

	/*
	 * This method sets the number of the current option
	 * 
	 * @param nr The option number.
	 */
	public void setOptionNr(int nr) {
		optionNr = nr;
	}

	/*
	 * This method sets the current option's data to a given byte array
	 * 
	 * @param value The byte array.
	 */
	public void setValue(byte[] value) {
		this.value = ByteBuffer.wrap(value);
	}

	// Functions ///////////////////////////////////////////////////////////////

	/*
	 * This method returns the data of the current option as byte array
	 * 
	 * @return The byte array holding the data
	 */
	public byte[] getRawValue() {
		return value.array();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + optionNr;
		result = prime * result + Arrays.hashCode(getRawValue());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Option other = (Option) obj;
		if (optionNr != other.optionNr)
			return false;
		if (getRawValue() == null) {
			if (other.getRawValue() != null)
				return false;
		} else if (!Arrays.equals(this.getRawValue(), other.getRawValue()))
			return false;
		return true;
	}

	/*
	 * This method returns the option number of the current option
	 * 
	 * @return The option number as integer
	 */
	public int getOptionNumber() {
		return optionNr;
	}

	/*
	 * This method returns the name that corresponds to the option number.
	 * 
	 * @return The name of the option
	 */
	public String getName() {
		return OptionNumberRegistry.toString(optionNr);
	}

	/*
	 * This method returns the length of the option's data in the ByteBuffer
	 * 
	 * @return The length of the data stored in the ByteBuffer as number of
	 * bytes
	 */
	public int getLength() {
		return value.capacity();
	}

	/*
	 * This method returns the value of the option's data as string
	 * 
	 * @return The string representation of the current option's data
	 */
	public String getStringValue() {
		String result = "";
		try {
			result = new String(value.array(), "UTF8");
		} catch (UnsupportedEncodingException e) {
			System.err.println("String conversion error");
		}
		return result;
	}

	/*
	 * This method returns the value of the option's data as integer
	 * 
	 * @return The integer representation of the current option's data
	 */
	public int getIntValue() {
		int byteLength = value.capacity();
		ByteBuffer temp = ByteBuffer.allocate(4);
		for (int i = 0; i < (4 - byteLength); i++) {
			temp.put((byte) 0);
		}
		for (int i = 0; i < byteLength; i++) {
			temp.put(value.get(i));
		}

		int val = temp.getInt(0);
		return val;
	}

	protected static String hex(byte[] data) {

		final String digits = "0123456789ABCDEF";

		if (data != null) {

			StringBuilder builder = new StringBuilder(data.length * 3);
			for (int i = 0; i < data.length; i++) {
				builder.append(digits.charAt((data[i] >> 4) & 0xF));
				builder.append(digits.charAt(data[i] & 0xF));
				if (i < data.length - 1) {
					builder.append(' ');
				}
			}
			return builder.toString();
		} else {
			return null;
		}
	}

	/*
	 * Returns a human-readable string representation of the option's value
	 * 
	 * @Return The option value represented as a string
	 */
	public String getDisplayValue() {
		switch (optionNr) {
		case OptionNumberRegistry.CONTENT_TYPE:
			return MediaTypeRegistry.toString(getIntValue());
		case OptionNumberRegistry.MAX_AGE:
			return String.format("%d s", getIntValue());
		case OptionNumberRegistry.PROXY_URI:
			return getStringValue();
		case OptionNumberRegistry.ETAG:
			return hex(getRawValue());
		case OptionNumberRegistry.URI_HOST:
			return getStringValue();
		case OptionNumberRegistry.LOCATION_PATH:
			return getStringValue();
		case OptionNumberRegistry.URI_PORT:
			return String.valueOf(getIntValue());
		case OptionNumberRegistry.LOCATION_QUERY:
			return getStringValue();
		case OptionNumberRegistry.URI_PATH:
			return getStringValue();
		case OptionNumberRegistry.OBSERVE:
			return String.valueOf(getIntValue());
		case OptionNumberRegistry.TOKEN:
			return hex(getRawValue());
		case OptionNumberRegistry.URI_QUERY:
			return getStringValue();
		case OptionNumberRegistry.BLOCK1:
		case OptionNumberRegistry.BLOCK2:
			// this case is actually handled
			// in subclass BlockOption
			return String.valueOf(getIntValue());
		default:
			return hex(getRawValue());
		}
	}

	public boolean isDefaultValue() {
		switch (optionNr) {
		case OptionNumberRegistry.MAX_AGE:
			return getIntValue() == 60;
		case OptionNumberRegistry.TOKEN:
			return getLength() == 0;
		default:
			return false;
		}
	}
	
	// Static methods //////////////////////////////////////////////////////////

	public static List<Option> split(int optionNumber, String s,
			String delimiter) {

		// create option list
		List<Option> options = new ArrayList<Option>();

		if (s != null) {
			for (String segment : s.split(delimiter)) {

				// handle non-empty segments only
				if (!segment.isEmpty()) {

					// create a new option from the segment
					// and add it to the list
					options.add(new Option(segment, optionNumber));
				}
			}
		}

		return options;
	}

	public static String join(List<Option> options, String delimiter) {
		if (options != null) {
			StringBuilder builder = new StringBuilder();
			for (Option opt : options) {
				builder.append(delimiter);
				builder.append(opt.getStringValue());
			}
			return builder.toString();
		} else {
			return "";
		}
	}

	// Attributes //////////////////////////////////////////////////////////////

	// The current option's data
	private ByteBuffer value;

	// The current option's number
	private int optionNr;
}
