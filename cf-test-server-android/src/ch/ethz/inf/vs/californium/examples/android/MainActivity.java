package ch.ethz.inf.vs.californium.examples.android;

import ch.ethz.inf.vs.californium.examples.android.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	private Button btnStartStop;
	private boolean running = false;
	private Intent coapServerIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btnStartStop = (Button) findViewById(R.id.button1);
		coapServerIntent = new Intent(this, CoapService.class);
		
		btnStartStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!running){
					running = true;
					btnStartStop.setText("Stop");
					startService(coapServerIntent);
				} else {
					stopService(coapServerIntent);
					btnStartStop.setText("Start");
					running = false;
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onStop() {
		if(running){
			stopService(coapServerIntent);
		}
		super.onStop();
	}

}
