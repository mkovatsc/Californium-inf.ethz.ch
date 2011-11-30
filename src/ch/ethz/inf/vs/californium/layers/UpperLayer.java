package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.Message;


public abstract class UpperLayer extends Layer {

	public void sendMessageOverLowerLayer(Message msg) throws IOException {

		// check if lower layer assigned
		if (lowerLayer != null) {

			lowerLayer.sendMessage(msg);
		} else {
			System.out.printf("[%s] ERROR: No lower layer present", getClass()
					.getName());
		}
	}

	public void setLowerLayer(Layer layer) {

		// unsubscribe from old lower layer
		if (lowerLayer != null) {
			lowerLayer.unregisterReceiver(this);
		}

		// set new lower layer
		lowerLayer = layer;

		// subscribe to new lower layer
		if (lowerLayer != null) {
			lowerLayer.registerReceiver(this);
		}
	}

	public Layer getLowerLayer() {
		return lowerLayer;
	}

	private Layer lowerLayer;
}
