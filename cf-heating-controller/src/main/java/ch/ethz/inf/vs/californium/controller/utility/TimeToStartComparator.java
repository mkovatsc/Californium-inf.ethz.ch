package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Collection;
import java.util.Comparator;

public class TimeToStartComparator implements Comparator<HeatingEvent>{

	@Override
	public int compare(HeatingEvent o1, HeatingEvent o2) {
		if(o1.getStart() < o2.getStart()){
			return 1;
		}
		else if(o1.getStart() > o2.getStart()){
			return -1;
		}
		else {
			if(o1.getTemperature()>o2.getTemperature()){
				return 1;
			}
			else{
				return -1;
			}
		}
	}
		

}
