package ch.ethz.inf.vs.californium.examples.android;

import java.net.SocketException;
import java.util.logging.Level;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.californium.util.Log;

public class CoapService extends Service {

	// exit codes for runtime errors
    public static final int ERR_INIT_FAILED = 1;
    
    private static final int DEFAULT_PORT = 5683;
    
    public static final String EXTRA_PORT = "ch.ethz.inf.vs.californium.extras.PORT";
    
    private static LocalEndpoint mServer;
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.setLevel(Level.ALL);
        Log.init();
        
        //int port = intent.getExtras().getInt(EXTRA_PORT, DEFAULT_PORT);
        int port = DEFAULT_PORT;
        
        
        // create server
        try {
            
            mServer = new TestServer(port);
            mServer.start();
            
            android.util.Log.d("cf-test-server-android", "ExampleServer listening on port " + mServer.getPort());
            
        } catch (SocketException e) {
            System.err.printf("Failed to create SampleServer: %s\n", e.getMessage());
            //System.exit(ERR_INIT_FAILED);
        }
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		mServer = null;
		super.onDestroy();
	}
}
