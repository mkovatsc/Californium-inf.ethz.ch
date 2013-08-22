package perf;

import java.util.HashMap;
import java.util.Map;

public class Command {

	public static final int ALL = -1;
	
	private final String complete;
	private Map<String, String> map;
	
	public Command(String command) {
		this.complete = command;
		this.map = new HashMap<>();
		String[] parts = command.split(" ");
		int ptr = 0;
		while (ptr < parts.length) {
			String option = parts[ptr];
			if (option.startsWith("-")) {
				if (ptr+1 < parts.length && !parts[ptr+1].startsWith("-")) {
					map.put(option, parts[ptr+1]);
					ptr += 2;
					continue;
				}
			}
			map.put(option, "");
			ptr++;
		}
	}
	
	public int getAt() {
		if (complete.startsWith("@")) { // e.g.: "@3 do -whatever"
			return Integer.parseInt(complete.split(" ")[0].substring(1));
		} else {
			return ALL;
		}
	}
	
	public String getBody() {
		if (complete.startsWith("@")) { // e.g.: "@3 do -whatever"
			return complete.substring(complete.indexOf(" ")).trim();
		} else {
			return complete;
		}
	}
	
	public boolean has(String option) {
		return map.containsKey(option);
	}
	
	public int get(String option) {
		if (!has(option))
			throw new CommandException("Command has no option "+option);
		return Integer.parseInt(map.get(option));
	}
	
	@Override
	public String toString() {
		return complete + " => " + map;
	}
	
	public static class CommandException extends RuntimeException {
		
		public CommandException(String message) {
			super(message);
		}
		
	}
}
