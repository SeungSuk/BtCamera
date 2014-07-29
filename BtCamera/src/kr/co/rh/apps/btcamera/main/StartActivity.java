package kr.co.rh.apps.btcamera.main;

import kr.co.rh.apps.btcamera.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends Activity {

	private Button btnServer, btnClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_start);

		btnServer = (Button) findViewById(R.id.btnServer);
		btnClient = (Button) findViewById(R.id.btnClient);

		btnServer.setOnClickListener(mBtnClick);
		btnClient.setOnClickListener(mBtnClick);
		
	}

	private View.OnClickListener mBtnClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = null;
			switch (v.getId()) {
			case R.id.btnServer:
				intent = new Intent(StartActivity.this, ServerActivity.class);
				break;
			case R.id.btnClient:
				intent = new Intent(StartActivity.this, ClientActivity.class);
				break;

			default:
				break;
			}

			if (intent != null) {
				startActivity(intent);
			}

		}
	};
}
