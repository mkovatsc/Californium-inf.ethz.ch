package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.ethz.inf.vs.californium.endpoint.resources.RDNodeResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.examples.AdminTool;

public class EndpointServlet extends HttpServlet {
	
	private AdminTool main;
	
	
	public EndpointServlet(){
		
		main = AdminTool.getInstance();
		
	}
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
    	String[] idsArray = request.getParameterValues("id");
        if(idsArray.length!=1){
        	return;
        }
        String id = idsArray[0];
        
        response.setContentType("text/html");
        response.setHeader("Cache-control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        
        
        PrintWriter out = response.getWriter();
        Set<Resource> subResource = main.getCoapServer().getEPResources(id);
        RDNodeResource node = main.getCoapServer().getEndpoint(id);
        if(node==null){
        	return;
        }
        
        
        TreeSet<Resource> epSensors = new TreeSet<Resource>();
        TreeSet<Resource> epConfig = new TreeSet<Resource>();
        TreeSet<Resource> epSet = new TreeSet<Resource>();
        TreeSet<Resource> epDebug =  new TreeSet<Resource>();
        TreeSet<Resource> epUnsorted = new TreeSet<Resource>();
        
        
        for(Resource subres : subResource){
        	if(subres.getPath().contains("/sensor")){
        		epSensors.add(subres);
        	}
        	else if (subres.getPath().contains("/config")){
        		epConfig.add(subres);
        	}
        	else if (subres.getPath().contains("/set")){
        		epSet.add(subres);
        	}
        	else if (subres.getPath().contains(".well-known")){
        		continue;
        	}
        	else if (subres.getPath().contains("/debug")){
        		epDebug.add(subres);
        	}
        	else{
        		continue;
        		//epUnsorted.add(subres);
        	}
        }
        
        
        if (node.getEndpointType().equalsIgnoreCase("honeywell")){
        	/*
        	 * Special Dialog for Honeywell
        	 * 
        	 * 
        	 */
        	Resource tmpRes = node.getResource("/sensors/error");
        	if(tmpRes!=null){
        		epSensors.remove(tmpRes);
        	}
        	
          	
        }
         /*
         * General Endpoint show all Resources
         * Search for different Type of Resources and Build corresponding Tab
         * 
         */
        StringBuilder sensorTab = null;
        StringBuilder configTab = null;
        StringBuilder setTab = null;
        StringBuilder debugTab = null;
        StringBuilder unsortedTab = null;


       	        
        if (!epDebug.isEmpty()){
        	debugTab = new StringBuilder("<div id=\"debugtab\">");
	        debugTab.append("<div class=\"tabouter\" id=\"");
	        debugTab.append(node.getContext().substring(node.getContext().indexOf("//")+2));
	        debugTab.append("\"><div class=\"tableft\">Version</div><div class=\"versionvalue\">Fetching</div><div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div></div>");
	        debugTab.append("<div class=\"tabouter\" id=\"");
	        debugTab.append(node.getContext().substring(node.getContext().indexOf("//")+2));
	        debugTab.append("\"><div class=\"tableft\">Last Seen</div><div class=\"lastseenvalue\">Fetching</div><div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div></div>");
	        debugTab.append("<div class=\"tabouter\" id=\"");
	        debugTab.append(node.getContext().substring(node.getContext().indexOf("//")+2));
	        debugTab.append("\"><div class=\"tableft\">Last RSSI</div><div class=\"lastrssivalue\">Fetching</div><div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div></div>");
	        debugTab.append("<div class=\"tabouter\" id=\"");
	        debugTab.append(node.getContext().substring(node.getContext().indexOf("//")+2));
	        debugTab.append("\"><div class=\"tableft\">Loss Rate</div><div class=\"lossratevalue\">Fetching</div><div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div></div>");
	        debugTab.append("<div class=\"tabouter\" id=\"");
	        debugTab.append(node.getContext().substring(node.getContext().indexOf("//")+2));
	        debugTab.append("\"><div class=\"tableft\">Uptime</div><div class=\"uptimevalue\">Fetching</div><div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div></div>");
	        debugTab.append("</div>");
        }
                
        if (!epSensors.isEmpty()){
        	sensorTab = new StringBuilder("<div id=\"sensortab\">");
        	for(Resource res : epSensors){
        		
        		String uri = node.getContext().substring(node.getContext().indexOf("//")+2)+res.getPath().substring(node.getPath().length());
        		 sensorTab.append("<div class=\"tabouter\" id=\"");
        		 sensorTab.append(uri);
        		 sensorTab.append("\"><div class=\"tableft\">");
        		 sensorTab.append(res.getPath().substring(res.getPath().indexOf("/sensors/")+9));
        		 sensorTab.append("</div><div class=\"sensorvalue\">Fetching..</div>" +
        		 		"<div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div>" +
        		 		"<div class=\"tabgraph\" onclick=\"openGraphDialog(this);\">Graph</div></div>");
        		 
        	}
            sensorTab.append("</div>");       	
        }
        
        if (!epConfig.isEmpty()){
        	configTab = new StringBuilder("<div id=\"configtab\">");
        	for(Resource res : epConfig){
        		
        		String uri = node.getContext().substring(node.getContext().indexOf("//")+2)+res.getPath().substring(node.getPath().length());
        		configTab.append("<div class=\"tabouter\" id=\"");
        		configTab.append(uri);
        		configTab.append("\"><div class=\"tableft\">");
        		configTab.append(res.getPath().substring(res.getPath().indexOf("/config/")+8));
        		configTab.append("</div><div class=\"configvalue\">Fetching..</div>" +
        				"<div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div>" +
        				"<div class=\"tabsend\" onclick=\"setValue(this);\">Set</div>" +
        				"</div>");
        	}
            configTab.append("</div>");       	
        }
        
        if (!epSet.isEmpty()){
        	setTab = new StringBuilder("<div id=\"settab\">");
        	for(Resource res : epSet){
        		
        		String uri = node.getContext().substring(node.getContext().indexOf("//")+2)+res.getPath().substring(node.getPath().length());
        		setTab.append("<div class=\"tabouter\" id=\"");
        		setTab.append(uri);
        		setTab.append("\"><div class=\"tableft\">");
        		setTab.append(res.getPath().substring(res.getPath().indexOf("/set/")+5));
        		setTab.append("</div><div class=\"setvalue\">Fetching..</div>" +
        				"<div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div>" +
        				"<div class=\"tabsend\" onclick=\"setValue(this);\">Set</div>" +
        				"</div>");
        	}
            setTab.append("</div>");       	
        }
        
        
        if (!epUnsorted.isEmpty()){
        	unsortedTab = new StringBuilder("<div id=\"unsortedtab\">");
        	for(Resource res : epUnsorted){
        		
        		String uri = node.getContext().substring(node.getContext().indexOf("//")+2)+res.getPath().substring(node.getPath().length());
        		unsortedTab.append("<div class=\"tabouter\" id=\"");
        		unsortedTab.append(uri);
        		unsortedTab.append("\"><div class=\"tableft\">");
        		unsortedTab.append(res.getPath());
        		unsortedTab.append("</div><div class=\"unsortedvalue\">Fetching..</div><div class=\"tabrefresh\" onclick=\"refreshValue(this);\">Refresh</div></div>");
           	}
            unsortedTab.append("</div>");       	
        }
        
    
        StringBuilder tabBar = new StringBuilder("<div class=\"eptabbar\"><ul>");
        if (sensorTab != null){
        	tabBar.append("<li><a href=\"#sensortab\">Sensor</a></li>");
        }
        if (configTab !=null){
        	tabBar.append("<li><a href=\"#configtab\">Config</a></li>");
        }
        if (setTab !=null){
        	tabBar.append("<li><a href=\"#settab\">Set</a></li>");
        }
        
        if (unsortedTab != null){
        	tabBar.append("<li><a href=\"#unsortedtab\">Unsorted</a></li>");
        }
        if (debugTab != null){
        	tabBar.append("<li><a href=\"#debugtab\">Debug</a></li>");
        }
            
        tabBar.append("</ul>");
        
        StringBuilder tabEnd  = new StringBuilder("</div>");
       
        out.write(tabBar.toString());
        if (sensorTab != null){
        	out.write(sensorTab.toString());
        }
        if (configTab !=null){
        	out.write(configTab.toString());
        }
        if (setTab !=null){
        	out.write(setTab.toString());
        }
        
        if (unsortedTab != null){
        	out.write(unsortedTab.toString());
        }
        if (debugTab != null){
        	out.write(debugTab.toString());
        }

        out.write(tabEnd.toString());
    
        
        
        out.flush();
        out.close();
        
    }
 

}
