package ch.ethz.inf.vs.californium.misc;


import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Tests the accuracy of an executor with support for scheduling.
 */
public class ScheduleTest {

	public static final int DELAY = 1500;
	
	private ScheduledExecutorService executor;
	
	public ScheduleTest() {
//		this.executor = Executors.newScheduledThreadPool(4);
//		this.executor = Executors.newSingleThreadScheduledExecutor();
		this.executor = new ScheduledThreadPoolExecutor(4);

//		new ScheduleTask().schedule();
		executor.scheduleAtFixedRate(new ScheduleTask(), DELAY, DELAY, TimeUnit.MILLISECONDS);
		
		for (int i=0;i<100;i++)
			executor.execute(new Task());
		
		
	}
	
	private class ScheduleTask implements Runnable {
		int counter = 0;
		public void run() {
			System.out.println("nah, do nothing, "+counter++);
			schedule();
		}
		
		public void schedule() {
			executor.schedule(this, DELAY, TimeUnit.MILLISECONDS);
		}
	}
	
	public static int nix; // to warte CPU time
	
	private static class Task implements Runnable {
		public void run() {
			long t0 = System.nanoTime();
			for (int j=0;j<10000;j++)
				for (int i=0;i<1000000;i++)
					nix++;
			long dt = System.nanoTime() - t0;
			System.out.println("needed "+dt/1000000+" ms");
			Thread.yield();
		}
	}
	
	public static void main(String[] args) {
		new ScheduleTest();
	}
}
