package com.sar.gp;

import java.util.HashMap;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;

import com.sar.gp.dld.DownloadTask;
import com.sar.gp.dld.DownloadTaskListener;

public class Dlg {
	
	private static Dlg _inst;
	private static final String TAG = Dlg.class.toString();
	private Map<String, DownloadTask> tasks;
	private Map<String, Map<String, Object>> dld_items;
	private DownloadTaskListener lsnr;
	
	private Dlg(){
		tasks = new HashMap<String, DownloadTask>();
		dld_items = new HashMap<String, Map<String,Object>>();
		lsnr = new DownloadTaskListener() {
			
			@Override
			public void updateProcess(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr) lsnr.updateProcess(data);
			}
			
			@Override
			public void preDownload(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr) lsnr.preDownload(data);
			}
			
			@Override
			public void finishDownload(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr) lsnr.finishDownload(data);
				rft(data);
			}
			
			@Override
			public void errorDownload(Map<String, Object> data) {
				DownloadTaskListener lsnr = gkla(data);
				if (null != lsnr) lsnr.errorDownload(data);
				rft(data);
			}
			
			private DownloadTaskListener gkla(Map<String, Object> data) {
				String save_path = (String) data.get("save_path");
				Map<String, Object> obj = dld_items.get(save_path);
				DownloadTaskListener lsnr = (DownloadTaskListener) obj.get("lsnr");
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
		if (null == _inst) _inst = new Dlg();
		return _inst;
	}
	
	public void dlg(Map<String, Object> data) {
		if (null == data) return;
		String save_path = (String) data.get("save_path");
		if (TextUtils.isEmpty(save_path)) return;
		if (null != tasks.get(save_path)) {
			Log.i(TAG, "task " + save_path + " exist, do not add again!");
			return;
		}
		String url = (String) data.get("url");
		DownloadTask task = new DownloadTask(url, lsnr, save_path);
		tasks.put(save_path, task);
		dld_items.put(save_path, data);
	}
	
}
