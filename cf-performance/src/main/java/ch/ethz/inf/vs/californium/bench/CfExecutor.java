package ch.ethz.inf.vs.californium.bench;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CfExecutor implements ScheduledExecutorService {

	private Random random = new Random();
	
	private int nThreads;
	
	private Worker[] workers;
	
	public CfExecutor(int nThreads) {
		System.out.println("Create Cf executor of size "+nThreads);
		this.nThreads = nThreads;
		
		this.workers = new Worker[nThreads];
		for (int i=0;i<nThreads;i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
	}
	
	@Override
	public void execute(Runnable command) {
		int r = random.nextInt(nThreads);
		workers[r].tasks.add(command);
	}
	
	@Override
	public void shutdown() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean isShutdown() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean isTerminated() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public Future<?> submit(Runnable task) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
			long initialDelay, long period, TimeUnit unit) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit) {
		throw new RuntimeException("Not implemented yet");
	}

	private static class Worker extends Thread {
		
		private BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
		
		public void run() {
			while (true) {
				try {
					Runnable task = tasks.take();
					task.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
}
