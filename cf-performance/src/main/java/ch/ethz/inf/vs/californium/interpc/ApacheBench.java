package ch.ethz.inf.vs.californium.interpc;

import java.util.Scanner;

/**
 * Wrapper to invoke apache bench
 */
public class ApacheBench {

	// Format: Conurrency Level, Time for tests, completed req, req per sec
	// NOTE: The -t option must come before the -n option. The order matters here!
	
	public static final String PATH_APACHE_BENCH = "C:\\Users\\Javimka\\Desktop\\";
	public static final String REQ_PER_SEC = "Requests per second";
	public static final String CON_LEV = "Concurrency Level";
	public static final String COM_REQ = "Complete requests";
	public static final String TIME = "Time taken for tests";

	public static final String LOG_FILE = "ab_log";
	
	private LogFile log;
	
	public ApacheBench() throws Exception {
		this.log = new LogFile(LOG_FILE);
		log.println("Conurrency Level, Time for tests, completed req, req per sec");
	}
	
	public void start(Command command) {
		try {
			Process p = Runtime.getRuntime().exec(PATH_APACHE_BENCH + command.getBody());
			StringBuilder buffer = new StringBuilder("ab,");
			try (Scanner scanner = new Scanner(p.getInputStream())) {
				while (scanner.hasNext()) {
					String line = scanner.nextLine();
					System.out.println(":"+line);
					if (line.startsWith(CON_LEV))
						buffer.append(new Scanner(line.split(":")[1]).nextInt()).append(",");
					if (line.startsWith(TIME))
						buffer.append(new Scanner(line.split(":")[1]).nextDouble()).append(",");
					if (line.startsWith(COM_REQ))
						buffer.append(new Scanner(line.split(":")[1]).nextInt()).append(",");
					if (line.startsWith(REQ_PER_SEC))
						buffer.append(new Scanner(line.split(":")[1]).nextDouble()).append(",");
				}
			}
			p.destroy();
			log.println(buffer.append(";").toString());
		} catch (Exception e) {
			e.printStackTrace();
			log.println("ERROR: "+command);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Command command = new Command("ab -n 10000 -c 20 -k http://192.168.1.37:8000/benchmark/");
		new ApacheBench().start(command);
	}
}
