package com.sar.gp.dld;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.os.AsyncTask;
import android.util.Log;

import com.sar.gp.Agmrsk;

public class DownloadTask extends AsyncTask<Void, Integer, Long> {

	private final static int BUFFER_SIZE = 1024 * 8;

	private static final String TAG = "DownloadTask";
	private static final String TEMP_SUFFIX = ".download";

	private String save_path;
	private File file;
	private File tempFile;
	private String url;
	private RandomAccessFile outputStream;
	private DownloadTaskListener listener;
	private long downloadSize;
	private long previousFileSize;
	private long totalSize;
	private long downloadPercent;
	private Throwable error = null;
	private boolean interrupt = false;
	private long last_flush = 0;

	private final class ProgressReportingRandomAccessFile extends
			RandomAccessFile {
		private int progress = 0;

		public ProgressReportingRandomAccessFile(File file, String mode)
				throws FileNotFoundException {
			super(file, mode);
		}

		@Override
		public void write(byte[] buffer, int offset, int count)
				throws IOException {
			super.write(buffer, offset, count);
			progress += count;
			publishProgress(progress);
		}
	}

	public DownloadTask(String url, String save_path)
			throws MalformedURLException {
		this(url, null, save_path);
	}

	public DownloadTask(String url, DownloadTaskListener listener,
			String save_path) {
		super();
		AgrUtils.mkdir(save_path);
		this.save_path = save_path;
		this.url = url;
		this.listener = listener;
		Agmrsk.i("DownloadTask", "save_path =>" + save_path);
		this.file = new File(save_path);
		this.tempFile = new File(save_path + TEMP_SUFFIX);
	}

	private Map<String, Object> getData() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("url", url);
		data.put("percent", downloadPercent);
		data.put("save_path", save_path);
		data.put("error", error);
		return data;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null)
			listener.preDownload(getData());
	}

	@Override
	protected Long doInBackground(Void... params) {
		long result = -1;
		try {
			result = download();
		} catch (Exception e) {
			e.printStackTrace();
			error = e;
		} finally {
			if (client != null) {
				client.close();
			}
		}
		return result;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (progress.length > 1 && progress[1] == -1) {
			if (listener != null)
				listener.errorDownload(getData());
		} else {
			downloadSize = progress[0];
			downloadPercent = (downloadSize + previousFileSize) * 100
					/ totalSize;
			if (downloadPercent-last_flush > 5)
				Agmrsk.i("download percent: " + downloadPercent);
			if (listener != null && downloadPercent-last_flush > 5) {
				listener.updateProcess(getData());
				last_flush = downloadPercent;
			}
		}
	}

	@Override
	protected void onPostExecute(Long result) {
		if (result == -1 || interrupt || error != null) {
			if (error != null) {
				Log.d(TAG, "Download failed." + error.getMessage());
			}
			if (listener != null) {
				listener.errorDownload(getData());
			}
			return;
		}
		// finish download
		tempFile.renameTo(file);
		if (listener != null)
			listener.finishDownload(getData());
	}

	@Override
	public void onCancelled() {
		super.onCancelled();
		interrupt = true;
	}

	private AndroidHttpClient client;
	private HttpGet httpGet;
	private HttpResponse response;

	private long download() throws Exception {
		Log.d(TAG, "totalSize: " + totalSize);
		if (!Agmrsk.ntavail()) {
			throw new Exception("Network blocked.");
		}

		client = AndroidHttpClient.newInstance("DownloadTask");
		httpGet = new HttpGet(url);
		response = client.execute(httpGet);
		totalSize = response.getEntity().getContentLength();

		if (file.exists() && totalSize == file.length()) {
			Log.d(null, "Output file already exists. Skipping download.");
			throw new Exception(
					"Output file already exists. Skipping download.");
		} else if (tempFile.exists()) {
			httpGet.addHeader("Range", "bytes=" + tempFile.length() + "-");
			previousFileSize = tempFile.length();

			client.close();
			client = AndroidHttpClient.newInstance("DownloadTask");
			response = client.execute(httpGet);

			Log.d(TAG, "File is not complete, download now.");
			Log.d(TAG, "File length:" + tempFile.length() + " totalSize:"
					+ totalSize);
		}

		long storage = AgrUtils.getAvailableStorage();
		Agmrsk.i(TAG, "storage:" + storage + " totalSize:" + totalSize);

		if (totalSize - tempFile.length() > storage) {
			throw new Exception("SD card no memory.");
		}

		outputStream = new ProgressReportingRandomAccessFile(tempFile, "rw");

		publishProgress(0, (int) totalSize);
		InputStream input = response.getEntity().getContent();
		int bytesCopied = copy(input, outputStream);

		if ((previousFileSize + bytesCopied) != totalSize && totalSize != -1
				&& !interrupt) {
			throw new IOException("Download incomplete: " + bytesCopied
					+ " != " + totalSize);
		}
		Log.d(TAG, "Download completed successfully.");
		return bytesCopied;
	}

	public int copy(InputStream input, RandomAccessFile out) throws IOException {
		if (input == null || out == null) {
			return -1;
		}
		byte[] buffer = new byte[BUFFER_SIZE];
		BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
		Log.d(TAG, "length" + out.length());
		int count = 0, n = 0;
		try {
			out.seek(out.length());
			while (!interrupt) {
				n = in.read(buffer, 0, BUFFER_SIZE);
				if (n == -1) {
					break;
				}
				out.write(buffer, 0, n);
				count += n;
				if (!Agmrsk.ntavail()) {
					throw new IOException("Network blocked.");
				}
			}
		} finally {
			client.close(); // must close client first
			client = null;
			out.close();
			in.close();
			input.close();
		}
		return count;
	}
}
