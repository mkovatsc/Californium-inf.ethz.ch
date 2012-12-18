package jetty;

import org.eclipse.jetty.webapp.WebAppContext;

public class AppContextBuilder {
	
	private WebAppContext webAppContext;
	
	public WebAppContext buildWebAppContext(){
		
		webAppContext = new WebAppContext();
		webAppContext.setResourceBase("src/main/webapp/page");
		webAppContext.setDescriptor("src/main/webapp/WEB-INF/web.xml");
		
		webAppContext.setContextPath("/");
				
		webAppContext.setParentLoaderPriority(true);
		return webAppContext;
	}
}
