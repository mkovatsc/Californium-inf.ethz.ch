package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.VirtualNode;
import ch.ethz.inf.vs.californium.endpoint.resources.RDNodeResource;
import ch.ethz.inf.vs.californium.examples.AdminTool;
import ch.ethz.inf.vs.californium.util.Properties;

public class TableServlet extends HttpServlet{
	
	private AdminTool main;
		
	public TableServlet(){
		main = AdminTool.getInstance();
		
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		PrintWriter out = response.getWriter();
		
	     response.setContentType("text/html");
	     response.setHeader("Cache-control", "no-cache, no-store");
	     response.setHeader("Pragma", "no-cache");
	     response.setHeader("Expires", "-1");
		
	     HashMap<String, VirtualNode> aliveEndpoints = main.getCoapServer().getAliveEndpoint();
	     
	     String json="";
	     json+="{ \"aaData\": [";
		
	     for( VirtualNode item : main.getCoapServer().getEveryKnownEndpoint().values()){ 
				json+="[\"";
				json+=item.getEndpointIdentifier();
				json+="\",\"";
				json+=item.getDomain();
				json+="\",\"";
				if(item.getEndpointType().isEmpty()){
					json+="unkown";
				}
				else{
					json+=item.getEndpointType();
				}
				json+="\",\"";
				json+=item.getContext();
				json+="\",\"";
				if(aliveEndpoints.containsKey(item.getEndpointIdentifier())){
					json+="true";
				}
				else{
					json+="false";
				}
							
				json+="\",\"";
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				json+=dateFormat.format(main.getCoapServer().getLastHeardOf(item.getContext()));
				json+="\",\"";
				double rate = main.getCoapServer().getLossRate(item.getContext());
				if(rate < 0){
					json+="Not Received Enough Packets";
				}
				else{
					DecimalFormat df = new DecimalFormat("#.##");
					json+=df.format(rate)+"%";
				}
				json+="\"],";
			}
	   		if(json.endsWith(",")){
				json = json.substring(0, json.length()-1);
			}
			json +="]}";
			out.print(json);
	     
	     
	     
	    /*
		for( RDNodeResource ep : main.getCoapServer().getEndpointObjects()){ 
			json+="[\"";
			json+=ep.getEndpointIdentifier();
			json+="\",\"";
			json+=ep.getDomain();
			json+="\",\"";
			if(ep.getEndpointType().isEmpty()){
				json+="unkown";
			}
			else{
				json+=ep.getEndpointType();
			}
			json+="\",\"";
			json+=ep.getContext();
			json+="\",\"";
			json+=ep.isActive() ? "true" : "false";
			
			json+="\",\"";
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			json+=dateFormat.format(main.getCoapServer().getLastHeardOf(ep.getContext().substring(ep.getContext().indexOf("//")+2)));
			json+="\",\"";
			double rate = main.getCoapServer().getLossRate(ep.getContext().substring(ep.getContext().indexOf("//")+2));
			if(rate < 0){
				json+="Not Received Enough Packets";
			}
			else{
				DecimalFormat df = new DecimalFormat("#.##");
				json+=df.format(rate)+"%";
			}
			json+="\"],";
		}
		if(json.endsWith(",")){
			json = json.substring(0, json.length()-1);
		}
		json +="]}";
		out.print(json);

		/*
		for( RDNodeResource ep : main.getCoapServer().getEndpointObjects()){ 
			out.print("<tr class=\"endpointitem");
			if(!ep.isActive()) out.print("_inactive");
			out.print("\" id=\"");
			out.print(ep.getEndpointIdentifier());
			out.print("\"><td>");
			out.print(ep.getEndpointIdentifier());
			out.print("</td><td>");
			out.print(ep.getDomain());
			out.print("</td><td>");
			if(ep.getEndpointType().isEmpty()){
				out.print("unkown");
			}
			else{
				out.print(ep.getEndpointType());
			}
			out.print("</td><td>");
			out.print(ep.getContext());
			out.print("</td><td>");
			out.print(ep.getLocation());
			out.print("</td><td>");
			out.print(ep.isActive() ? "active" : "inactive");
			
			out.print("</td><td class=\"lastseenvalue\">");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			out.print(dateFormat.format(main.getCoapServer().getLastHeardOf(ep.getEndpointIdentifier())));
					
			out.print("</td><td class=\"lossratevalue\">");
			int actual = main.getCoapServer().getPacketsRecivedActual(ep.getEndpointIdentifier());
			int ideal = main.getCoapServer().getPacketsRecivedIdeal(ep.getEndpointIdentifier());
			if(ideal == 0){
				out.print("Not Yet Recieved A Packet");
			}
			else{
				DecimalFormat df = new DecimalFormat("#.##");
				out.print(df.format((double) (ideal-actual) / (double) ideal *100));
			}
			out.print("</td></tr>");
		
		}
			*/	
		out.flush();
		out.close();

	}
}
