package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.examples.AdminTool;
import ch.ethz.inf.vs.californium.util.Properties;

public class GraphServlet extends HttpServlet{
	
	private AdminTool main;
	String psURI;
	boolean hasPS;
	
	
	public GraphServlet(){
		main = AdminTool.getInstance();
		hasPS=false;
		if(Properties.std.containsKey("PS_ADDRESS")){
			String psHost = Properties.std.getStr("PS_ADDRESS");
			psURI = "coap://"+psHost+"/persistingservice/tasks";
			hasPS=true;
		}
	}
	
	private boolean isNumeric(String input){
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(input, pos);
		return (input.length()==pos.getIndex());
		//return input.matches("[-+]?\\d+(\\.\\d+)?");
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		PrintWriter out = response.getWriter();
	     
		response.setContentType("text/html");
	    response.setHeader("Cache-control", "no-cache, no-store");
	    response.setHeader("Pragma", "no-cache");
	    response.setHeader("Expires", "-1");
	 
	    String id = "";
		String[] ids = request.getParameterValues("id");
				
		 
		if(ids!=null && ids.length == 1 ){
			id=ids[0];
			id=id.replace("[","").replace("]","");
		}
		if(!hasPS){
			out.write("Now Persisting Service vailable");
			out.flush();
			out.close();
			return;
		}
		GETRequest graphRequest = new GETRequest();
		if(!graphRequest.setURI(psURI+"/"+id+"/history/since")){
			out.write("No Persisting Service Available");
			return;
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH,-1);
	//	String lastMonth = "date="+dateFormat.format(cal.getTime());
		graphRequest.addOption(new Option("date="+dateFormat.format(cal.getTime()),OptionNumberRegistry.URI_QUERY));
		
		graphRequest.addOption(new Option("withdate=true", OptionNumberRegistry.URI_QUERY));
		graphRequest.enableResponseQueue(true);
		graphRequest.execute();
		Response graphResponse = null;
		try {
			graphResponse=graphRequest.receiveResponse();
		} catch (InterruptedException e) {
			
		}
		if(graphResponse!=null){
			if(graphResponse.getCode()==CodeRegistry.RESP_CONTENT){
				String dataset = "";
				BufferedReader bufReader = new BufferedReader(new StringReader(graphResponse.getPayloadString()));
				String line=null;
				while( (line=bufReader.readLine()) != null )
				{
					String date = line.substring(line.indexOf(";")+1).trim();
					String value = line.substring(0, line.indexOf(";")).trim();
					if(isNumeric(value)){ //remove non numeric entries
						dataset += "\n"+date+","+value+dataset;
					}
				}
				 dataset = "Date,"+id.substring(id.lastIndexOf("/")+1) +dataset;
				
				out.print(dataset);
			}
			if(graphResponse.getCode()==CodeRegistry.RESP_NOT_FOUND){
				out.print("Resource not in Persisting Service, no history data available");
			}
		}
		out.flush();
		out.close();
		
	}
}
