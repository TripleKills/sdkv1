package com.sar.gp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.sar.gp.dld.AgrUtils;
import com.sar.gp.dld.DownloadTask;
import com.sar.gp.dld.DownloadTaskListener;

public class Dlg {

	private static Dlg _inst;
	private static final String TAG = Dlg.class.toString();
	private Map<String, DownloadTask> tasks;
	private Map<String, Map<String, Object>> dld_items;
	private DownloadTaskListener lsnr;

	private Dlg() {
		tasks = new HashMap<String, DownloadTask>();
		dld_items = new HashMap<String, Map<String, Object>>();
		lsnr = new DownloadTaskListener() {

			@Override
			public void updateProcess(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr)
					lsnr.updateProcess(data);
				String save_path = (String) data.get("save_path");
				Map<String, Object> obj = dld_items.get(save_path);
				String type = (String) obj.get("type");
				if ("pg".equals(type)) {
					String pn = (String) obj.get("pn");
					long p = (Long) data.get("percent");
					ndli(p, pn);
				}
			}

			@Override
			public void preDownload(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr)
					lsnr.preDownload(data);
				String save_path = (String) data.get("save_path");
				Map<String, Object> obj = dld_items.get(save_path);
				String type = (String) obj.get("type");
				if ("pg".equals(type)) {
					String pn = (String) obj.get("pn");
					String msg = (String) obj.get("show_name");
					nstdl(msg, pn);
				}
			}

			@Override
			public void finishDownload(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr)
					lsnr.finishDownload(data);
				String save_path = (String) data.get("save_path");
				Map<String, Object> obj = dld_items.get(save_path);
				String type = (String) obj.get("type");
				if ("pg".equals(type)) {
					String pn = (String) obj.get("pn");
					String apn = (String) obj.get("show_name");
					ccn(pn);
					ndlcp(save_path, apn, pn);
					AgrUtils.installAPK(Agmrsk.mContext, save_path);
				}
				rft(data);
			}

			@Override
			public void errorDownload(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr)
					lsnr.errorDownload(data);
				String save_path = (String) data.get("save_path");
				Map<String, Object> obj = dld_items.get(save_path);
				String type = (String) obj.get("type");
				if ("pg".equals(type)) {
					String pn = (String) obj.get("pn");
					ccn(pn);
				}
				rft(data);
			}

			private DownloadTaskListener gkla(Map<String, Object> data) {
				String save_path = (String) data.get("save_path");
				Map<String, Object> obj = dld_items.get(save_path);
				DownloadTaskListener lsnr = (DownloadTaskListener) obj
						.get("lsnr");
				return lsnr;
			}

			private void rft(Map<String, Object> data) {
				String save_path = (String) data.get("save_path");
				tasks.remove(save_path);
				dld_items.remove(save_path);
			}
		};
	}

	public static Dlg getIns() {
		if (null == _inst)
			_inst = new Dlg();
		return _inst;
	}

	public void dlg(Map<String, Object> data) {
		if (null == data)
			return;
		String save_path = (String) data.get("save_path");
		if (TextUtils.isEmpty(save_path))
			return;
		if (null != tasks.get(save_path)) {
			Log.i(TAG, "task " + save_path + " exist, do not add again!");
			if (null != data.get("lsnr")) {
				Map<String, Object> dli = dld_items.get(save_path);
				dli.put("lsnr", data.get("lsnr"));
				Log.i(TAG, "lsnr had been replaced");
			}
			return;
		}
		String url = (String) data.get("url");
		DownloadTask task = new DownloadTask(url, lsnr, save_path);
		tasks.put(save_path, task);
		dld_items.put(save_path, data);
		task.execute();
	}

	private static Map<String, Notification> nos = new HashMap<String, Notification>();

	/**
	 * 发送开始下载的通知，msg：显示在tickertext和contenttitle，tag:包名
	 */
	private static void nstdl(String msg, String tag) {
		Intent intent = new Intent();
		PendingIntent pi = PendingIntent.getActivity(Agmrsk.mContext, 0,
				intent, 0);
		Notification notification = new Notification();
		notification.icon = android.R.drawable.stat_sys_download;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.tickerText = msg;
		notification.contentIntent = pi;
		rfrp(0, notification, tag);
		nos.put(tag, notification);
	}

	private static void rfrp(long p, Notification no, String tag) {
		NotificationManager msrg = (NotificationManager) Agmrsk.mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		no.setLatestEventInfo(Agmrsk.mContext, no.tickerText,
				"正在下载 " + p + "%", no.contentIntent);
		msrg.notify(tag, 0, no);
	}

	/**
	 * 刷新下载通知的进度，p:进度，tag：包名
	 */
	private static void ndli(long p, String tag) {
		if (p < 0 || p > 100)
			return;
		Notification no = nos.get(tag);
		if (null == no)
			return;
		rfrp(p, no, tag);
	}

	/**
	 * 通知下载完成
	 * 
	 * @param pt
	 *            保存地址
	 * @param apn
	 *            应用名称
	 * @param pn
	 *            包名
	 */
	public static void ndlcp(String pt, String apn, String pn) {
		String msg = apn + "下载完成";
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(pt)),
				"application/vnd.android.package-archive");
		PendingIntent pi = PendingIntent.getActivity(Agmrsk.mContext, 0,
				intent, 0);
		Notification no = new Notification();
		no.icon = android.R.drawable.stat_sys_download_done;
		no.flags |= Notification.FLAG_NO_CLEAR;
		no.tickerText = msg;
		no.setLatestEventInfo(Agmrsk.mContext, "点击安装", msg, pi);
		NotificationManager msrg = (NotificationManager) Agmrsk.mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		msrg.notify(pn, 0, no);
		nos.put(pn, no);
	}

	/**
	 * --cancel掉某个通知 tg:对于下载通知来说就是包名
	 */
	public static void ccn(String tg) {
		nos.remove(tg);
		NotificationManager msrg = (NotificationManager) Agmrsk.mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		msrg.cancel(tg, 0);
	}
}
