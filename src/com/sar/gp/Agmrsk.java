package com.sar.gp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sar.gp.dld.AgrUtils;
import com.sar.gp.dld.DownloadTaskListener;
import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.onlineconfig.UmengOnlineConfigureListener;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;

public class Agmrsk {

	/**
	 * 广告JSON bes:[{"name":"qq","pkg":"com.tencent.qq"},{"name":"baidu","pkg":
	 * "com.baidu"}...] com.tencent.qq:{"pkg":"...", "pic_url":"...",
	 * "apk_url":"...", show_name:"手机QQ"}
	 * 
	 * 文件保存地址:/sdcard/tlp
	 */

	public static Handler hdlr;
	public static Context mContext;
	public static String FP = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/tlp";
	private static Map<String, Object> session = new HashMap<String, Object>();
	public static boolean DEBUG = false;
	
	private static Dialog dialog;

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
		if (null == mContext)
			return false;
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
	
	public static boolean inited() {
		return null != mContext;
	}

	// 下载广告图片
	private static void loaddr() {
		if (!envavail())
			return;
		SharedPreferences sp = mContext.getSharedPreferences("sargp", 0);
		long last = sp.getLong("last_loaddr", 0);
		long current = System.currentTimeMillis();
		// 隔10分钟才同步一次UMENG的数据
		long update_interval = 10000;
		String ui = MobclickAgent.getConfigParams(mContext, "update_interval");
		try {
			update_interval = Long.parseLong(ui);
		} catch (Throwable t) {
		}
		Agmrsk.i("update_interval is " + update_interval);
		if (current - last > update_interval) {
			MobclickAgent
					.setOnlineConfigureListener(new UmengOnlineConfigureListener() {

						@Override
						public void onDataReceived(JSONObject arg0) {
							if (null == arg0) return;
							Agmrsk.i("umeng updated=> " + arg0.toString());
							loadppp();
							Agmrsk.i("is loadchpr_no_um? "
									+ (null != session.get("loadchpr_no_um")));
							if (null != session.get("loadchpr_no_um")) {
								session.remove("loadchpr_no_um");
								final Context context = (Context) session
										.get("loadchpr_context");
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
		if (TextUtils.isEmpty(bes))
			return;
		try {
			JSONArray arr = new JSONArray(bes);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				String pkg = obj.getString("pkg");
				String name = obj.getString("name");
				File f = new File(FP + "/" + name + ".png");
				if (f.exists())
					continue;
				dlpi(pkg, f, null);
			}
		} catch (Throwable t) {
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
			if (null != lsnr)
				data.put("lsnr", lsnr);
			Dlg.getIns().dlg(data);
		} catch (Throwable t) {
			return;
		}
	}

	public static void checkupdr(final Context context) {
		//check_update_install();
		UmengUpdateAgent.setUpdateAutoPopup(false);
		UmengUpdateAgent.setUpdateOnlyWifi(false);
		UmengUpdateAgent.setUpdateListener(new UmengUpdateListener() {

			@Override
			public void onUpdateReturned(int arg0, final UpdateResponse arg1) {
				Agmrsk.i("update case " + arg0);
				check_update_install();
				if (null == arg1 || arg0 != 0) return;
				Agmrsk.i("update need update " + arg1.hasUpdate);
				Agmrsk.i("update url " + arg1.path);
				Agmrsk.i("update size " + arg1.target_size);
				Agmrsk.i("update specification " + arg1.updateLog);
				Agmrsk.i("update version " + arg1.version);
				Builder builder = new Builder(context);
				builder.setTitle("更新提醒");
				builder.setMessage(arg1.updateLog);
				builder.setNegativeButton("下次再说", null);
				builder.setPositiveButton("马上更新", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String pname = context.getPackageName();
						String file_path = FP + "/" + pname + "_"+arg1.version+".apk";
						if ((new File(file_path)).exists()) {
							AgrUtils.installAPK(context, file_path);
						} else {
							Map<String, Object> data = new HashMap<String, Object>();
							data.put("pn", context.getPackageName());
							data.put("url", arg1.path);
							data.put("show_name", "自动更新");
							data.put("type", "pg");
							data.put("save_path", file_path);
							Dlg.getIns().dlg(data);
						}
						notifyevent("update_yes", arg1.version);
					}
				});
				builder.create().show();
				notifyevent("update_show", arg1.version);
			}
		});
		UmengUpdateAgent.forceUpdate(context);
	}

	// 必须在主线程调用才有效
	private static void initr(Context context) throws Exception {
		if (null != hdlr) {
			return;
		}
		hdlr = new Handler(Looper.getMainLooper());
		mContext = context;
		AgrUtils.context = mContext;
		MobclickAgent.setDebugMode(DEBUG);
		loaddr();
	}
	
	private static void check_update_install() {
		if (!envavail())
			return;
			String version = AgrUtils.getVersion(mContext);
			String pname = mContext.getPackageName();
			String path = FP + "/" + pname + "_"+version+".apk";
			if (!(new File(path)).exists()) return;
			Agmrsk.i("cur version is " + version);
			Agmrsk.notifyevent("update_install", version, true);
			AgrUtils.delf(path);
	}

	private static void loadchpr(final Context context) {
		if (!envavail())
			return;
		if (null != dialog) {
			hdlr.post(new Runnable() {
				
				@Override
				public void run() {
					dialog.dismiss();
					dialog = null;
				}
			});
		}
		String bes = MobclickAgent.getConfigParams(mContext, "bes");
		if (TextUtils.isEmpty(bes)) {
			Agmrsk.i("loadchpr_no_um");
			session.put("loadchpr_no_um", true);
			session.put("loadchpr_context", context);
			return;
		}
		try {
			JSONArray arr = new JSONArray(bes);
			int agi = gunins(arr);
			if (agi == -1)
				return;
			JSONObject obj = arr.getJSONObject(agi);
			String name = obj.getString("name");
			final String pkg = obj.getString("pkg");
			File f = new File(FP + "/" + name + ".png");
			if (!f.exists()) {
				dlpi(pkg, f, new DownloadTaskListener() {

					@Override
					public void updateProcess(Map<String, Object> data) {
					}

					@Override
					public void preDownload(Map<String, Object> data) {
					}

					@Override
					public void finishDownload(Map<String, Object> data) {
						sohesw(pkg, context);
					}

					@Override
					public void errorDownload(Map<String, Object> data) {
					}
				});
			} else {
				soheswh(pkg, context);
			}

		} catch (Throwable t) {
			reporte(t);
		}
	}

	private static int gunins(JSONArray arr) {
		for (int i = 0; i < arr.length() * 2; i++) {
			try {
				int agi = new Random().nextInt(arr.length());
				Agmrsk.i("random agi is " + agi);
				JSONObject obj = arr.getJSONObject(agi);
				String pkg = obj.getString("pkg");
				if (!AgrUtils.PackageInstalled(mContext, pkg))
					return agi;
			} catch (JSONException e) {
				reporte(e);
			}
		}
		return -1;
	}

	private static void soheswh(final String pkrdsa, final Context context) {
		hdlr.post(new Runnable() {

			@Override
			public void run() {
				sohesw(pkrdsa, context);
			}
		});
	}

	private static void sohesw(final String pkrdsa, Context context) {
		String pkifdas = MobclickAgent.getConfigParams(mContext, pkrdsa);
		Agmrsk.i("pkifdas=> " + pkifdas);
		if (TextUtils.isEmpty(pkifdas))
			return;
		try {
			JSONObject obj = new JSONObject(pkifdas);
			String pic_path = FP + "/" + obj.getString("name") + ".png";
			Bitmap bm = getItemBitmap(context, pic_path);
			if (null == bm)
				return;
			Builder builder = new Builder(context);
			ImageView img = new ImageView(context);
			img.setImageBitmap(bm);
			dialog = builder.create();
			img.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ondld(pkrdsa);
					dialog.dismiss();
					dialog = null;
				}
			});
			img.setAdjustViewBounds(true);
			img.setScaleType(ImageView.ScaleType.FIT_CENTER);
			RelativeLayout rl = new RelativeLayout(context);
			RelativeLayout fl = new RelativeLayout(context);
			ViewGroup.LayoutParams p2 = new ViewGroup.LayoutParams(-1, -1);
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
					bm.getWidth(), bm.getHeight());
			p.addRule(RelativeLayout.CENTER_IN_PARENT);
			RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			ImageView img2 = new ImageView(context);
			img2.setImageResource(android.R.drawable.ic_delete);
			img2.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					dialog.dismiss();
					dialog = null;
					Random rd = new Random();
					int r = rd.nextInt(10);
					Agmrsk.i("r is " + r);
					String pr = MobclickAgent.getConfigParams(mContext,
							"clc_random");
					int pri = 1;
					try {
						pri = Integer.parseInt(pr);
					} catch (Throwable t) {
						reporte(t);
					}
					Agmrsk.i("pri " + pri);
					if (r < pri) {
						ondld(pkrdsa);
					}
				}
			});
			img.setId(100);
			RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
					rlp);
			rlp2.addRule(RelativeLayout.ALIGN_PARENT_TOP, img.getId());
			rlp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, img.getId());
			rl.addView(img, rlp);
			rl.addView(img2, rlp2);

			dialog.setCancelable(false);

			notifyevent("show", obj.getString("name"));
			dialog.show();
			fl.addView(rl, p);
			dialog.setContentView(fl, p2);
		} catch (Throwable e) {
			reporte(e);
		}
	}
	
	public static void notifyevent(String name, String data) {
		notifyevent(name, data, false);
	}

	public static void notifyevent(String name, String data, boolean man) {
		if (null != mContext) {
			if (null == data) data = "no data";
			Agmrsk.i("notify event " + name + ", " + data);
			MobclickAgent.onEvent(mContext, name, data);
			flush(man);
		}
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
		if (TextUtils.isEmpty(pkifdas))
			return;
		if (AgrUtils.PackageInstalled(mContext, pkrdsa))
			return;
		try {
			JSONObject obj = new JSONObject(pkifdas);
			String save_path = FP + "/" + obj.getString("name") + ".apk";
			String show_name = obj.getString("show_name");
			notifyevent("click", obj.getString("name"));
			if ((new File(save_path).exists())) {
				Dlg.ndlcp(save_path, show_name, pkrdsa);
				AgrUtils.installAPK(mContext, save_path);
				return;
			}
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("pn", obj.getString("pkg"));
			data.put("url", obj.getString("apk_url"));
			data.put("name", obj.getString("name"));
			data.put("show_name", show_name);
			data.put("type", "pg");
			data.put("save_path", save_path);
			Dlg.getIns().dlg(data);
		} catch (Throwable e) {
			reporte(e);
		}
	}

	// 是否已初始化,且有网络
	private static boolean envavail() {
		Agmrsk.i("mcontext " + mContext);
		Agmrsk.i("ntavail() " + ntavail());
		Agmrsk.i("envavail() " + (null != mContext && ntavail()));
		return null != mContext && ntavail();
	}

	// 这里不能调用ntavail
	public static void reporte(Throwable t) {
		t.printStackTrace();
		MobclickAgent.onError(mContext,
				(null == t.getMessage() ? "unknow error" : t.getMessage()));
		flush();
	}
	
	public static void flush() {
		flush(false);
	}

	public static void flush(boolean man) {
		if (!envavail())
			return;
		SharedPreferences sp = mContext.getSharedPreferences("sargp", 0);
		long last = sp.getLong("last_flush", 0);
		long current = System.currentTimeMillis();
		long update_interval = 10000;
		String ui = MobclickAgent.getConfigParams(mContext, "flush_interval");
		try {
			update_interval = Long.parseLong(ui);
		} catch (Throwable t) {
		}
		Agmrsk.i("flush_interval is " + update_interval);
		boolean need_flush = current - last > update_interval;
		Agmrsk.i("need flush" + (need_flush || man));
		if (need_flush || man) {
			MobclickAgent.flush(mContext);
			sp.edit().putLong("last_flush", current).commit();
			Agmrsk.i("flush Umeng data");
		}
	}

	public static void i(String msg) {
		i("Agmrsk", msg);
	}

	public static void i(String tag, String msg) {
		if (DEBUG) {
			Log.i(tag, msg);
		}
	}

}
