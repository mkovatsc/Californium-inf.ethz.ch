package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		
	    String json="";
		json+="{ \"aaData\": [";
		
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
			json+=ep.getLocation();
			json+="\",\"";
			json+=ep.isActive() ? "active" : "inactive";
			
			json+="\",\"";
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			json+=dateFormat.format(main.getCoapServer().getLastHeardOf(ep.getEndpointIdentifier()));
					
			json+="\",\"";
			int actual = main.getCoapServer().getPacketsRecivedActual(ep.getEndpointIdentifier());
			int ideal = main.getCoapServer().getPacketsRecivedIdeal(ep.getEndpointIdentifier());
			if(ideal == 0){
				json+="Not Yet Recieved A Packet";
			}
			else{
				DecimalFormat df = new DecimalFormat("#.##");
				json+=df.format((double) (ideal-actual) / (double) ideal *100);
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
