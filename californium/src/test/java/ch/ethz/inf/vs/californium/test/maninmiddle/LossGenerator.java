package ch.ethz.inf.vs.californium.test.maninmiddle;

import java.util.ArrayList;
import java.util.List;

public class LossGenerator {

	/**
	 * Algorithm to produce all series of numbers which can be tested in the
	 * LossyBlockwiseTransferTest.
	 */
	public static void main(String[] args) {
		
		List<Integer> list = new ArrayList<Integer>();
		for (int i=0;i<200;i++) {
			list.clear();
			for (int j=0;j<32;j++)
				if ( ((i>>j) & 1) == 1)
					list.add(j);
			
			System.out.println(list);
		}
		
			
	}
	
	

}
