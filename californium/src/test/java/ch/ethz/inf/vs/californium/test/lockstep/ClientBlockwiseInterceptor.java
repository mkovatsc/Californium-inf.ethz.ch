package ch.ethz.inf.vs.californium.test.lockstep;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.interceptors.MessageInterceptor;

public class ClientBlockwiseInterceptor implements MessageInterceptor {

	private StringBuilder buffer = new StringBuilder();
		
	@Override
	public void sendRequest(Request request) {
		buffer.append(
				String.format("\n%s [MID=%d], %s, /%s%s%s%s    ----->",
				request.getType(), request.getMID(), request.getCode(),
				request.getOptions().getURIPathString(),
				blockOptionString(1, request.getOptions().getBlock1()),
				blockOptionString(2, request.getOptions().getBlock2()),
				observeString(request.getOptions())));
	}

	@Override
	public void sendResponse(Response response) {
		buffer.append("ERROR: Server received "+response+"\n");
	}

	@Override
	public void sendEmptyMessage(EmptyMessage message) {
		buffer.append(
				String.format("\n%-19s                       ----->",
				String.format("%s [MID=%d], 0",message.getType(), message.getMID())
				));
	}

	@Override
	public void receiveRequest(Request request) {
		buffer.append("\nERROR: Server sent "+request+"\n");
	}

	@Override
	public void receiveResponse(Response response) {
		buffer.append(
				String.format("\n<-----   %s [MID=%d], %s%s%s%s    ",
				response.getType(), response.getMID(), response.getCode(),
				blockOptionString(1, response.getOptions().getBlock1()),
				blockOptionString(2, response.getOptions().getBlock2()),
				observeString(response.getOptions())));
	}

	@Override
	public void receiveEmptyMessage(EmptyMessage message) {
		buffer.append(
				String.format("\n<-----   %s [MID=%d], 0",
				message.getType(), message.getMID()));
	}
	
	public void log(String str) {
		buffer.append(str);
	}
	
	private String blockOptionString(int nbr, BlockOption option) {
		if (option == null) return "";
		return String.format(", %d:%d/%d/%d", nbr, option.getNum(),
				option.isM()?1:0, option.getSize());
	}
	
	private String observeString(OptionSet options) {
		if (options == null) return "";
		else if (!options.hasObserve()) return "";
		else return ", (observe="+options.getObserve()+")";
	}
	
	public String toString() {
		return buffer.append("\n").substring(1);
	}
	
	public void clear() {
		buffer = new StringBuilder();
	}

}
