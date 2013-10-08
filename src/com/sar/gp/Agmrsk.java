package com.sar.gp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.onlineconfig.UmengOnlineConfigureListener;

public class Agmrsk {
	
	/**
	 * 广告JSON
	 * bes:[{"name":"qq","pkg":"com.tencent.qq"},{"name":"baidu","pkg":"com.baidu"}...]
	 * com.tencent.qq:{"pkg":"...", "pic_url":"...", "apk_url":"...", show_name:"手机QQ"}
	 * 
	 * 文件保存地址:/sdcard/tlp
	 */

	public static Handler hdlr;
	public static Context mContext;
	public static String FP = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tlp";
	
	public static void init(Context context) {
		try {
			initr(context);
		} catch (Throwable t) {
			reporte(t);
		}
	}
	
	public static void loadchp(Context context) {
		try {
			loadchpr(context);
		} catch (Throwable t) {
			reporte(t);
		}
	}
	
	public static void loadd() {
		try {
			loaddr();
		} catch (Throwable t) {
			reporte(t);
		}
	}
	
	public static boolean ntavail() {
		if (null == mContext) return false;
		boolean isOK = false;
		try {
			ConnectivityManager cm = (ConnectivityManager) mContext
	                .getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo info = cm.getActiveNetworkInfo();
	        isOK = info != null && info.isConnected() && info.isAvailable();
		} catch (Throwable t) {
			reporte(t);
		}
		return isOK;
	}
	
	//下载广告图片
	private static void loaddr() {
		if (!envavail()) return;
		SharedPreferences sp = mContext.getSharedPreferences("sargp", 0);
		long last = sp.getLong("last_loaddr", 0);
		long current = System.currentTimeMillis();
		//隔10分钟才同步一次UMENG的数据
		if (current - last > 10 * 60 * 1000) {
			MobclickAgent.setOnlineConfigureListener(new UmengOnlineConfigureListener() {
				
				@Override
				public void onDataReceived(JSONObject arg0) {
					loadppp();
				}
			});
			MobclickAgent.updateOnlineConfig(mContext);
			sp.edit().putLong("last_loaddr", current).commit();
		} else {
			loadppp();
		}
	}
	
	private static void loadppp() {
		String bes = MobclickAgent.getConfigParams(mContext, "bes");
		if (TextUtils.isEmpty(bes)) return;
		try {
			JSONArray arr = new JSONArray(bes);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				String pkg = obj.getString("pkg");
				String name = obj.getString("name");
				File f = new File(FP + "/" + name + ".png");
				if (f.exists()) continue;
				String be = MobclickAgent.getConfigParams(mContext, pkg);
				try {
					JSONObject beobj = new JSONObject(be);
					String pug = beobj.getString("pic_url");
					Map<String, String> data = new HashMap<String, String>();
					data.put("type", "pic");
					data.put("url", pug);
					Dlg.getIns().dlg(data);
				} catch (Throwable t) {
					continue;
				}
			}
		} catch(Throwable t) {
			reporte(t);
		}
	}
	
	//必须在主线程调用才有效
	private static void initr(Context context) throws Exception {
		if (null != hdlr) {
			return;
		}
		hdlr = new Handler(Looper.getMainLooper());
		mContext = context;
		loaddr();
	}

	private static void loadchpr(Context context) {
		if (!envavail()) return;
	}
	
	//是否已初始化,且有网络
	private static boolean envavail() {
		return null == mContext && !ntavail();
	}
	
	//这里不能调用ntavail
	public static void reporte(Throwable t) {
		t.printStackTrace();
		MobclickAgent.onError(mContext, (null == t.getMessage() ? "unknow error" : t.getMessage()));
		flush();
	}
	
	public static void flush() {
		if (!envavail()) return;
		SharedPreferences sp = mContext.getSharedPreferences("sargp", 0);
		long last = sp.getLong("last_flush", 0);
		long current = System.currentTimeMillis();
		//隔10秒钟才同步一次UMENG的数据
		if (current - last > 10 * 1000) {
			MobclickAgent.flush(mContext);
			sp.edit().putLong("last_flush", current).commit();
		}
	}

}
