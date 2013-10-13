package com.sar.gp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NrService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		ShuaManager sm = ShuaManager.getInstance();
		sm.init(getApplicationContext());
		sm.start();
		super.onCreate();
	}

}
