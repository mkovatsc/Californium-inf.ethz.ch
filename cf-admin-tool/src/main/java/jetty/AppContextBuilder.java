package jetty;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

public class AppContextBuilder {
	
	private WebAppContext webAppContext;
	
	public WebAppContext buildWebAppContext(){
		
		webAppContext = new WebAppContext();
		webAppContext.setResourceBase("../cf-admin-tool/webapp/page");
		webAppContext.setDescriptor("../cf-admin-tool/webapp/WEB-INF/web.xml");
		
		webAppContext.setContextPath("/");
				
		webAppContext.setParentLoaderPriority(true);
		return webAppContext;
		
		
	}
}
