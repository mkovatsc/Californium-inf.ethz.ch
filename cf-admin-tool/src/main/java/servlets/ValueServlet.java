package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.examples.AdminTool;

public class ValueServlet extends HttpServlet{
	
	private AdminTool main;
	
	public ValueServlet(){
		main = AdminTool.getInstance();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		 PrintWriter out = response.getWriter();
	     response.setContentType("text/html");
	     response.setHeader("Cache-control", "no-cache, no-store");
	     response.setHeader("Pragma", "no-cache");
	     response.setHeader("Expires", "-1");
	 
	     String id = "";
		 String type = "";
		 String[] ids = request.getParameterValues("id");
		 String[] types = request.getParameterValues("type");
		 
		 if(ids!=null && ids.length == 1){
			 id=ids[0]; 
		 }
			 
		 if (types!=null && types.length == 1 ){
		 	 type=types[0];
		 }
		 
		 if(type.equals("lastseenvalue")){
			 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			 out.print(dateFormat.format(main.getCoapServer().getLastHeardOf(id)));
			 
		 }
		 else if (type.equals("lossratevalue")){
			 	double rate = main.getCoapServer().getLossRate(id);
				if(rate < 0){
					out.print("Not Received Enough Packets");
				}
				else{
					DecimalFormat df = new DecimalFormat("#.##");
					out.print(df.format(rate)+"%");
				}
		 }
		 else if (type.equals("lastrssivalue")){
			 	String value;
			 	value = main.getCoapServer().getEndpointDebug(id, "rssi");
			 	if (value==null){
			 		out.print("Not Supported by Endpoint");
			 	}
			 	else{
			 		out.print(value);
			 	}
		 }
		 else if (type.equals("uptimevalue")){
			 	String value;
			 	value = main.getCoapServer().getEndpointDebug(id, "uptime");
			 	if (value==null){
			 		out.print("Not Supported by Endpoint");
			 	}
			 	else{
			 		out.print(value);
			 	}
		 }
		 else if (type.equals("versionvalue")){
			 String value;
			 value = main.getCoapServer().getEndpointDebug(id, "version");
			 if (value==null){
			 		out.print("Not Supported by Endpoint");
			 	}
			 	else{
			 		out.print(value);
			 }
			 /*
			 if (value==null){
				 GETRequest coapRequest = new GETRequest();
				 coapRequest.setURI("coap://"+id+"/debug/version");
				 coapRequest.enableResponseQueue(true);
				 Response coapResponse = null;
				 try{
					 coapRequest.execute();
					 coapResponse = coapRequest.receiveResponse();
					 if(coapResponse != null && coapResponse.getCode()==CodeRegistry.RESP_CONTENT){
						value=coapResponse.getPayloadString();
					 }
					 else{
						 value = "Not Supported by Endpoint";
					 }
				 }
				 catch (Exception e){
					 value = "Not Supported by Endpoint";
				 }
			 }
			 out.print(value);
			 */
		 }
		 		 
		 else if (type.equals("sensorvalue")){
			 out.print( main.getCoapServer().getLastValue(id));
			 
		 }
		 else if (type.equals("setvalue") || type.equals("configvalue") || type.equals("unsortedvalue")){
			 String value; 
			 GETRequest coapRequest = new GETRequest();
			 coapRequest.setURI("coap://"+id);
			 coapRequest.enableResponseQueue(true);
			 Response coapResponse = null;
			 try{
				 coapRequest.execute();
				 Thread.sleep(200);
				 coapResponse = coapRequest.receiveResponse();
				 if(coapResponse != null && coapResponse.getCode()==CodeRegistry.RESP_CONTENT){
					if(coapResponse.getPayloadString().isEmpty()){
						value="";
					}
					else{
						value=coapResponse.getPayloadString();
					}
				 }
				 else{
					 value = "Error Executing Request";
				 }
			 }
			 catch (Exception e){
				 value = "Error Executing Request";
			 }
		 
			 out.print(value);
		 }
		 
		 		 
	     out.flush();
	     out.close();
	     return;
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		 response.setContentType("text/html");
	     response.setHeader("Cache-control", "no-cache, no-store");
	     response.setHeader("Pragma", "no-cache");
	     response.setHeader("Expires", "-1");
	     
	     System.out.println("Servlet POST");
		 
		 String id = "";
		 String type = "";
		 String[] ids = request.getParameterValues("id");
		 String[] types = request.getParameterValues("type");
		 String value ="";
		 String[] values =request.getParameterValues("value");
		 if(ids!=null && types!=null && ids.length == 1 && types.length == 1){
			 id=ids[0];
			 type=types[0];
		 }
		 else{
			 response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			 return;
		 }
		 if(type.equals("reregister")){
			 main.getCoapServer().reregisterObserve(id);
			 response.setStatus(HttpServletResponse.SC_OK);
			 return;
		 }
		 
		 
		 if(values!=null && values.length == 1){
			 value=values[0];
		 }
		 else{
			 response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			 return;
		 }
		 PrintWriter out = response.getWriter();
		 
		 PUTRequest coapRequest = new PUTRequest();
		 coapRequest.setURI("coap://"+id);
		 coapRequest.enableResponseQueue(true);
		 coapRequest.setPayload(value);
		 
		 Response coapResponse = null;
		 try{
			 coapRequest.execute();
			 Thread.sleep(200);
			 coapResponse = coapRequest.receiveResponse();
			 if(coapResponse != null && (coapResponse.getCode()==CodeRegistry.RESP_CONTENT || coapResponse.getCode()==CodeRegistry.RESP_CHANGED)){
				 out.print(value);
			 }
			 else{
				 response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

			 }
		 }
		 catch (Exception e){
			 response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		 }
		 
		 		 
		 out.flush();
		 out.close();
		 return;
	}

}
