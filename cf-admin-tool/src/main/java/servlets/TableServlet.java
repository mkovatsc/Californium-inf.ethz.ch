package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.inf.vs.californium.endpoint.VirtualNode;
import ch.ethz.inf.vs.californium.examples.AdminTool;

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
			json+=dateFormat.format(main.getCoapServer().getLastHeardOfId(item.getEndpointIdentifier()));
			json+="\",\"";
			double rate = main.getCoapServer().getLossRateId(item.getEndpointIdentifier());
			if(rate < 0){
				json+="Not enough packets yet";
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

		out.flush();
		out.close();

	}
}
