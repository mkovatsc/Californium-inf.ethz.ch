


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Pauser {

	private int number;
	
	private BlockingQueue<Object> q;
	
	public Pauser(int number) {
		this.number = number;
		this.q = new LinkedBlockingQueue<Object>();
	}
	
	public void pause() throws InterruptedException {
		q.take();
	}
	
	public void resume() {
		for (int i=0;i<number;i++)
			q.add("nix");
	}

}
