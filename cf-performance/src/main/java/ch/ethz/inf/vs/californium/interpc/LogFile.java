package ch.ethz.inf.vs.californium.interpc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Log file. Works like System.out.println() but stores all Strings into a file.
 */
public class LogFile {

	public static final String SUFFIX = ".txt";
	
	private BufferedWriter out;
	
	public LogFile(String name) throws Exception {
		File file = createLogFile(name);
		out = new BufferedWriter(new FileWriter(file));
		System.out.println("Created log file "+file.getAbsolutePath());
		
	}
	
	public void println(String line) {
		try {
			while (line.startsWith("\n")) {
				System.out.println();
				out.write("\r\n");
				line = line.substring(1);
			}
			System.out.println("< "+line);
			out.write(line+"\r\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void format(String str, Object... args){
		try {
			String line = String.format(str, args);
			System.out.print("< "+line);
			out.write(line.replace("\n", "\r\n"));
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void errln(String line) {
		try {
			System.err.println("< "+line);
			out.write(line+"\r\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private File createLogFile(String name) throws Exception {
		String date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
		name = name + "_" + date;
		File file = new File(name + SUFFIX);
		if (!file.exists()) {
			file.createNewFile();
			return file;
		} else {
			int c = 0;
			do {
				file = new File(name+"("+(++c)+")" + SUFFIX);
			} while (file.exists());
			file.createNewFile();
			return file;
		}
	}
}
