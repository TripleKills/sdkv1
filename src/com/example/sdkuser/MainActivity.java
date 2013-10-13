package com.example.sdkuser;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.sar.gp.Agmrsk;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Agmrsk.DEBUG = true;
		Agmrsk.init(this);
		Agmrsk.loadchp(this);
		/*new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Agmrsk.loadchp(MainActivity.this);
			}
		}).start();*/
		//ImageView v = (ImageView) findViewById(R.id.imageView1);android.R.drawable.ic
		Agmrsk.checkupdr(this);
	}
	
	@Override
	protected void onResume() {
		MobclickAgent.onResume(this);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		MobclickAgent.onPause(this);
		super.onPause();
	}

}
