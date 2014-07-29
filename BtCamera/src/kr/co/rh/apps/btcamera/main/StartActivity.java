package kr.co.rh.apps.btcamera.main;

import kr.co.rh.apps.btcamera.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_start);
		
		Button btnServer = (Button)findViewById(R.id.btnServer);
		
		btnServer.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(StartActivity.this, MainActivity.class);
				startActivity(intent);
			}
		});		
	}
}
