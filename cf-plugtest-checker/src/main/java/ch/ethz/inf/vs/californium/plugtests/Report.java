package ch.ethz.inf.vs.californium.plugtests;

import java.util.ArrayList;
import java.util.List;

public class Report {

	private List<String> summary;
	
	public Report() {
		this.summary = new ArrayList<String>();
	}

	public List<String> getSummary() {
		return summary;
	}
	
	public void addEntry(String entry) {
		summary.add(entry);
	}
	
	public void print() {
		for (String entry:summary)
			System.out.println(entry);
	}
}
