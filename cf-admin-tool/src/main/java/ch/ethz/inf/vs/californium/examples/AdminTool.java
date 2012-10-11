package ch.ethz.inf.vs.californium.examples;

import java.util.logging.Level;

import jetty.AppContextBuilder;
import jetty.JettyServer;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.URIUtil;

import ch.ethz.inf.vs.californium.endpoint.AdminToolEndpoint;
import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.californium.endpoint.RDEndpoint;
import ch.ethz.inf.vs.californium.endpoint.resource.ObserveTopResource;
import ch.ethz.inf.vs.californium.util.Log;

public class AdminTool{
	
	private static AdminTool adminTool = null;
	private static AdminToolEndpoint coapServer;
	private static JettyServer webServer;
	
	private AdminTool(){
		
	}
	
	public static AdminTool getInstance(){
		if(adminTool == null){
			adminTool = new AdminTool();
		}
		return adminTool;
	}
	
	
	
	
	public static void main(String[] args){
		Log.setLevel(Level.OFF);
		Log.init();
		AdminTool adminTool = AdminTool.getInstance();
		
		ContextHandlerCollection contexts= new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] {new AppContextBuilder().buildWebAppContext()});
						
		AdminTool.webServer = new JettyServer(8080);
		
		webServer.setHandler(contexts);
		
		try{
			AdminTool.coapServer = new AdminToolEndpoint();
			
			coapServer.start();
			
			webServer.start();
			
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	public AdminToolEndpoint getCoapServer(){
		return coapServer;
	}

	public JettyServer getWebServer(){
		return webServer;
	}
}
