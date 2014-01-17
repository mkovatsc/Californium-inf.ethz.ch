package ch.ethz.inf.vs.californium.coapbench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Log file. Works like System.out.println() but stores all Strings into a file.
 */
public class LogFile {

	private boolean verbose;
	private BufferedWriter out;
	
	public LogFile(String name) throws Exception {
		File file = createLogFile(name);
		out = new BufferedWriter(new FileWriter(file));
		System.out.println("Created log file "+file.getAbsolutePath());
	}
	
	public void println(String line) {
		try {
			while (line.startsWith("\n")) {
//				if (verbose)
					System.out.println();
				out.write("\r\n");
				line = line.substring(1);
			}
			if (verbose)
				System.out.println(line);
			out.write(line+"\r\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void format(String str, Object... args){
		try {
			String line = String.format(str, args);
//			if (verbose)
				System.out.print(line);
			out.write(line.replace("\n", "\r\n"));
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void errln(String line) {
		try {
//			if (verbose)
				System.err.println(line);
			out.write(line+"\r\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private File createLogFile(String name) throws Exception {
//		String date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
//		name = name + "_" + date;
		File file = new File(name);
		if (!file.exists()) {
			file.createNewFile();
			return file;
		} else {
			int c = 0;
			do {
				file = new File(name+"("+(++c)+")");
			} while (file.exists());
			file.createNewFile();
			return file;
		}
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
