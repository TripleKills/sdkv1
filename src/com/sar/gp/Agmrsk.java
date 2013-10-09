package com.sar.gp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.sar.gp.dld.AgrUtils;
import com.sar.gp.dld.DownloadTaskListener;
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
	private static Map<String, Object> session = new HashMap<String, Object>();
	
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
					System.out.println("is loadchpr_no_um? " + (null != session.get("loadchpr_no_um")));
					if (null != session.get("loadchpr_no_um")) {
						session.remove("loadchpr_no_um");
						final Context context = (Context) session.get("loadchpr_context");
						if (null != context) {
							Runnable r = new Runnable() {
								
								@Override
								public void run() {
									loadchpr(context);
								}
							};
							hdlr.post(r);
						}
					}
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
				dlpi(pkg, f, null);
			}
		} catch(Throwable t) {
			reporte(t);
		}
	}

	private static void dlpi(String pkg, File f, DownloadTaskListener lsnr) {
		String be = MobclickAgent.getConfigParams(mContext, pkg);
		try {
			JSONObject beobj = new JSONObject(be);
			String pug = beobj.getString("pic_url");
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("type", "pic");
			data.put("url", pug);
			data.put("save_path", f.getAbsolutePath());
			if (null != lsnr) data.put("lsnr", lsnr);
			Dlg.getIns().dlg(data);
		} catch (Throwable t) {
			return;
		}
	}
	
	//必须在主线程调用才有效
	private static void initr(Context context) throws Exception {
		if (null != hdlr) {
			return;
		}
		hdlr = new Handler(Looper.getMainLooper());
		mContext = context;
		AgrUtils.context = mContext;
		loaddr();
	}

	private static void loadchpr(final Context context) {
		if (!envavail()) return;
		String bes = MobclickAgent.getConfigParams(mContext, "bes");
		if (TextUtils.isEmpty(bes)) {
			System.out.println("loadchpr_no_um");
			session.put("loadchpr_no_um", true);
			session.put("loadchpr_context", context);
			return;
		}
		try {
			JSONArray arr = new JSONArray(bes);
			int agi = gunins(arr);
			if (agi == -1) return;
			JSONObject obj = arr.getJSONObject(agi);
			String name = obj.getString("name");
			final String pkg = obj.getString("pkg");
			File f = new File(FP + "/" + name + ".png");
			if (!f.exists()) {
				dlpi(pkg, f, new DownloadTaskListener() {
					
					@Override
					public void updateProcess(Map<String, Object> data) {}
					
					@Override
					public void preDownload(Map<String, Object> data) {}
					
					@Override
					public void finishDownload(Map<String, Object> data) {
						sohesw(pkg, context);
					}
					
					@Override
					public void errorDownload(Map<String, Object> data) {}
				});
			} else {
				sohesw(pkg, context);
			}
			
		} catch(Throwable t) {
			reporte(t);
		}
	}
	
	private static int gunins(JSONArray arr) {
		for (int i = 0; i < arr.length()*2; i++) {
			try {
				double r = Math.random();
				System.out.println("random is " + r);
				int agi = (int) (r*arr.length());
				System.out.println("random agi is " + agi);
				JSONObject obj = arr.getJSONObject(agi);
				String pkg = obj.getString("pkg");
				if (!AgrUtils.PackageInstalled(mContext, pkg)) return agi;
			} catch (JSONException e) {
				reporte(e);
			}
		}
		return -1;
	}
	
	private static void sohesw(final String pkrdsa, Context context) {
		String pkifdas = MobclickAgent.getConfigParams(mContext, pkrdsa);
		if (TextUtils.isEmpty(pkifdas)) return;
		try {
			JSONObject obj = new JSONObject(pkifdas);
			String pic_path = FP + "/" + obj.getString("name") + ".png";
			Bitmap bm = getItemBitmap(context, pic_path);
			if (null == bm) return;
			Builder builder = new Builder(context);
			ImageView img = new ImageView(context);
			builder.setView(img);
			img.setImageBitmap(bm);
			final AlertDialog dialog = builder.create();
			img.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ondld(pkrdsa);
					dialog.dismiss();
				}
			});
			dialog.show();
		} catch (Throwable e) {
			reporte(e);
		}
	//	ondld(pkrdsa);
	}
	
	private static Bitmap getItemBitmap(Context context, String pic_path) {
		DisplayMetrics dm = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay()
				.getMetrics(dm);
		int height = dm.heightPixels;
		int width = dm.widthPixels;
		float height_scale = (float) height / 800.f;
		float width_scale = (float) width / 480.f;
		Bitmap bitmap = AgrUtils.load(pic_path);
		if (null != bitmap) {
			bitmap = AgrUtils.scale(bitmap, width_scale, height_scale);
			bitmap = AgrUtils.getRoundedCornerBitmap(bitmap, 15);
		}
		return bitmap;
	}

	private static void ondld(String pkrdsa) {
		String pkifdas = MobclickAgent.getConfigParams(mContext, pkrdsa);
		if (TextUtils.isEmpty(pkifdas)) return;
		if (AgrUtils.PackageInstalled(mContext, pkrdsa)) return;
		try {
			JSONObject obj = new JSONObject(pkifdas);
			String save_path = FP + "/" + obj.getString("name") + ".apk";
			String show_name = obj.getString("show_name");
			if ((new File(save_path).exists())) {
				Dlg.ndlcp(save_path, show_name, pkrdsa);
				AgrUtils.installAPK(mContext, save_path);
				return;
			}
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("pn", obj.getString("pkg"));
			data.put("url", obj.getString("apk_url"));
			data.put("show_name", show_name);
			data.put("type", "pg");
			data.put("save_path", save_path);
			Dlg.getIns().dlg(data);
		} catch (Throwable e) {
			reporte(e);
		}
	}
	
	//是否已初始化,且有网络
	private static boolean envavail() {
		System.out.println("mcontext " + mContext);
		System.out.println("ntavail() " + ntavail());
		System.out.println("envavail() " + (null != mContext && ntavail()));
		return null != mContext && ntavail();
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
