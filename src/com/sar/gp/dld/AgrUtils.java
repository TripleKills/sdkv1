package com.sar.gp.dld;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.FloatMath;
import android.util.Log;

import com.sar.gp.Agmrsk;

public class AgrUtils {

	public static String getVersion(Context context) {
		try {
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(),
					0);
			return info.versionName;
		} catch (Exception e) {
			e.printStackTrace();
			return "1.0";
		}
	}

	public static Bitmap load(String filePath) {
		return load(filePath, 1);
	}

	public static Bitmap load(String filePath, final int maxHeight,
			final int maxWidth) {
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, opts);
		if (null == opts.outMimeType) {
			Agmrsk.i("the file is not picture");
			return null;
		}
		float widthScale = opts.outWidth > maxWidth ? (float) opts.outWidth
				/ (float) maxWidth : 1f;
		float heightScale = opts.outHeight > maxHeight ? (float) opts.outHeight
				/ (float) maxHeight : 1f;
		int minSampleSize = (int) FloatMath.ceil(Math.max(widthScale,
				heightScale));
		return load(opts, minSampleSize, filePath);
	}

	public static Bitmap load(String filePath, final int minSampleSize) {
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, opts);
		return load(opts, minSampleSize, filePath);
	}

	public static Bitmap load(Options opts, final int minSampleSize,
			String filePath) {
		try {
			opts.inJustDecodeBounds = false;
			opts.inSampleSize = minSampleSize;
			Log.i("BitmapLoader", "load " + filePath
					+ ", in sample size at least " + minSampleSize);
			if (null == opts || null == opts.outMimeType) {
				// 给出的地址不是图片
				Agmrsk.i("is not picture");
				return null;
			}
			int size = getSize(opts.outWidth, opts.outHeight, minSampleSize);
			boolean canLoad = isMemAvailable(size);
			if (canLoad) {
				// 真正把图片加载进内存
				Log.i("BitmapLoader", "can load directly");
				Bitmap bitmap = BitmapFactory.decodeFile(filePath, opts);
				Log.i("BitmapLoader", "bitmap is " + bitmap);
				return bitmap;
			} else {
				Log.i("BitmapLoader", "can not load directly");
				int sampleSize = reSampleSize(opts);
				if (sampleSize == -1) {
					// 图片无法加载进内存
					Agmrsk.i("can not load after add sample size");
					return null;
				} else {
					sampleSize = sampleSize > minSampleSize ? sampleSize
							: minSampleSize;
					size = getSize(opts.outWidth, opts.outHeight, sampleSize);
					if (!isMemAvailable(size)) {
						// 图片无法加载进内存
						System.err
								.println("can not load after get correct sample size");
						return null;
					} else {
						// 真正把图片加载进内存
						opts.inSampleSize = sampleSize;
						Log.i("BitmapLoader", "can load after add sample size "
								+ sampleSize);
						Bitmap bitmap = BitmapFactory
								.decodeFile(filePath, opts);
						return bitmap;
					}
				}
			}
		} catch (OutOfMemoryError e) {
			System.err
					.println("re size is ok, but the oom exception is trhowed, catch it and return null bitmap");
			return null;
		}
	}

	public static Bitmap load(InputStream stream) {
		return load(stream, 1);
	}

	public static Bitmap load(InputStream stream, final int maxHeight,
			final int maxWidth) {
		byte[] is = getCopy(stream);
		if (null == is || is.length == 0) {
			Agmrsk.i("get copy of stream error, return null");
			return null;
		}
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(is, 0, is.length, opts);
		if (null == opts.outMimeType) {
			return null;
		}
		float widthScale = opts.outWidth > maxWidth ? (float) opts.outWidth
				/ (float) maxWidth : 1f;
		float heightScale = opts.outHeight > maxHeight ? (float) opts.outHeight
				/ (float) maxHeight : 1f;
		int minSampleSize = (int) FloatMath.ceil(Math.max(widthScale,
				heightScale));
		return load(is, minSampleSize, opts);
	}

	public static Bitmap load(InputStream stream, final int minSampleSize) {
		byte[] is = getCopy(stream);
		if (null == is || is.length == 0) {
			Agmrsk.i("get copy of stream error, return null");
			return null;
		}
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(is, 0, is.length, opts);

		return load(is, minSampleSize, opts);
	}

	private static Bitmap load(byte[] buffer, final int minSampleSize,
			Options opts) {
		try {
			opts.inJustDecodeBounds = false;
			opts.inSampleSize = minSampleSize;
			Log.i("BitmapLoader", "load " + buffer
					+ ", in sample size at least " + minSampleSize);
			if (null == opts || null == opts.outMimeType) {
				// 给出的地址不是图片
				Agmrsk.i("is not picture");
				return null;
			}
			int size = getSize(opts.outWidth, opts.outHeight, minSampleSize);
			boolean canLoad = isMemAvailable(size);
			if (canLoad) {
				// 真正把图片加载进内存
				Log.i("BitmapLoader", "can load directly");
				Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0,
						buffer.length, opts);
				Log.i("BitmapLoader", "bitmap is " + bitmap);
				return bitmap;
			} else {
				Log.i("BitmapLoader", "can not load directly");
				int sampleSize = reSampleSize(opts);
				if (sampleSize == -1) {
					// 图片无法加载进内存
					Agmrsk.i("can not load after add sample size");
					return null;
				} else {
					sampleSize = sampleSize > minSampleSize ? sampleSize
							: minSampleSize;
					size = getSize(opts.outWidth, opts.outHeight, sampleSize);
					if (!isMemAvailable(size)) {
						// 图片无法加载进内存
						System.err
								.println("can not load after get correct sample size");
						return null;
					} else {
						// 真正把图片加载进内存
						opts.inSampleSize = sampleSize;
						Log.i("BitmapLoader", "can load after add sample size "
								+ sampleSize);
						Bitmap bitmap = BitmapFactory.decodeByteArray(buffer,
								0, buffer.length, opts);
						return bitmap;
					}
				}
			}
		} catch (OutOfMemoryError e) {
			System.err
					.println("re size is ok, but the oom exception is trhowed, catch it and return null bitmap");
			return null;
		}
	}

	public static Bitmap load(Options opts, final int minSampleSize,
			InputStream stream) {
		return load(getCopy(stream), minSampleSize, opts);
	}

	private static int getSize(int width, int height, int sampleSize) {
		return ((int) FloatMath.ceil((float) width / (float) sampleSize))
				* ((int) FloatMath.ceil((float) height / (float) sampleSize))
				* 4;
	}

	private static byte[] getCopy(InputStream is) {
		if (null == is) {
			return null;
		}
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read;
			while ((read = is.read(buffer)) != -1) {
				baos.write(buffer, 0, read);
			}
			baos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} catch (OutOfMemoryError oom) {
			oom.printStackTrace();
			return null;
		} finally {
			if (null != baos) {
				try {
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static final int MAX_SAMPLE_SIZE = 5;

	public static Context context = null;

	private static final float SAFETY_FACTOR = 0.8f;

	/**
	 * 检查内存是否够用
	 * 
	 * @param needMem
	 *            所需内存，单位Byte
	 * @return
	 */
	public static boolean isMemAvailable(int needMem) {
		ActivityManager mgr = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		int memClass = mgr.getMemoryClass();// unit is MB
		Debug.MemoryInfo info = mgr.getProcessMemoryInfo(new int[] { Process
				.myPid() })[0];
		int available = (int) ((memClass * 1024 * 1024 - info.dalvikPrivateDirty * 1024) * SAFETY_FACTOR);
		Log.i("BitmapMemoryHelper", "need mem is " + needMem
				+ ", available is " + available);
		return available > needMem;
	}

	/**
	 * 获取最小的能够加载进入内存的sampleSize值
	 * 
	 * @param filePath
	 *            图片的地址
	 * @return 返回正数代表最小的sampleSize,返回-1代表无法加载进内存
	 */
	public static int reSampleSize(String filePath) {
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, opts);
		return reSampleSize(opts);
	}

	/**
	 * 获取最小的能够加载进入内存的sampleSize值
	 * 
	 * @param stream
	 *            输入流，这个输入流应该能生成一个Bitmap
	 * @return 返回正数代表最小的sampleSize,返回-1代表无法加载进内存
	 */
	public static int reSampleSize(InputStream stream) {
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(stream, null, opts);
		return reSampleSize(opts);
	}

	/**
	 * 获取最小的能够加载进入内存的sampleSize值
	 * 
	 * @param opts
	 *            已经decode过Bitmap尺寸的Options
	 * @return 返回正数代表最小的sampleSize,返回-1代表无法加载进内存
	 */
	public static int reSampleSize(Options opts) {
		if (null == opts || null == opts.outMimeType) {
			return -1;
		}
		int size = opts.outWidth * opts.outHeight * 4;
		int sampleSize = 1;
		while (!isMemAvailable(size) && sampleSize <= MAX_SAMPLE_SIZE) {
			sampleSize++;
			float width = (float) opts.outWidth / (float) sampleSize;
			float height = (float) opts.outHeight / (float) sampleSize;
			int wth = (int) FloatMath.ceil(width);
			int hht = (int) FloatMath.ceil(height);
			size = wth * hht * 4;
		}
		if (!isMemAvailable(size)) {
			return -1;
		}
		return sampleSize;
	}

	public static Bitmap scale(Bitmap bmp, float width_scale, float height_scale) {
		int bmpWidth = bmp.getWidth();
		int bmpHeight = bmp.getHeight();
		Log.i("BitmapUtil", "original (" + bmpHeight + ", " + bmpWidth + ")");
		float scaleWidth = (float) (bmpWidth * width_scale);
		float scaleHeight = (float) (bmpHeight * height_scale);
		Log.i("BitmapUtil", "scale to (" + scaleHeight + ", " + scaleWidth
				+ ")");
		/* 产生reSize后的Bitmap对象 */
		Matrix matrix = new Matrix();
		matrix.postScale(width_scale, height_scale);
		Bitmap resizeBmp = Bitmap.createBitmap(bmp, 0, 0, bmpWidth, bmpHeight,
				matrix, true);
		// bmp.recycle();
		return resizeBmp;
	}

	/**
	 * 画一个圆角图
	 * 
	 * @param bitmap
	 * @param roundPx
	 * @return
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		int color = 0xff424242;
		Paint paint = new Paint();
		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		RectF rectF = new RectF(rect);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	private static final long LOW_STORAGE_THRESHOLD = 1024 * 1024 * 10;

	public static boolean mkdir(String path) {
		if (TextUtils.isEmpty(path))
			return false;
		File f = new File(path);
		if (f.exists())
			return true;
		return f.getParentFile().mkdirs();
	}

	public static boolean isSdCardWrittenable() {
		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;
	}

	public static long getAvailableStorage() {
		String storageDirectory = null;
		storageDirectory = Environment.getExternalStorageDirectory().toString();
		try {
			StatFs stat = new StatFs(storageDirectory);
			long avaliableSize = ((long) stat.getAvailableBlocks() * (long) stat
					.getBlockSize());
			return avaliableSize;
		} catch (RuntimeException ex) {
			return 0;
		}
	}

	public static boolean checkAvailableStorage() {
		if (getAvailableStorage() < LOW_STORAGE_THRESHOLD) {
			return false;
		}
		return true;
	}

	public static Bitmap getLoacalBitmap(String url) {
		try {
			FileInputStream fis = new FileInputStream(url);
			return BitmapFactory.decodeStream(fis); // /把流转化为Bitmap图片

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String size(long size) {

		if (size / (1024 * 1024) > 0) {
			float tmpSize = (float) (size) / (float) (1024 * 1024);
			DecimalFormat df = new DecimalFormat("#.##");
			return "" + df.format(tmpSize) + "MB";
		} else if (size / 1024 > 0) {
			return "" + (size / (1024)) + "KB";
		} else
			return "" + size + "B";
	}

	/**
	 * 安装file path指定路径的APK，若APK不存在返回false
	 * 
	 * @param filePath
	 * @param context
	 * @return
	 */
	public static boolean installAPK(Context context, String filePath) {
		if (null == filePath) {
			throw new IllegalArgumentException("file path can't be null");
		}
		File file = new File(filePath);
		if (file.exists()) {
			Intent intent = new Intent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(file),
					"application/vnd.android.package-archive");
			context.startActivity(intent);
			return true;
		}
		return false;
	}

	public static void delf(String path) {
		File f = new File(path);
		if (f.exists())
			f.delete();
	}

	public static boolean delete(File path) {
		boolean result = true;
		if (path.exists()) {
			if (path.isDirectory()) {
				for (File child : path.listFiles()) {
					result &= delete(child);
				}
				result &= path.delete(); // Delete empty directory.
			}
			if (path.isFile()) {
				result &= path.delete();
			}
			if (!result) {
				Log.e(null, "Delete failed;");
			}
			return result;
		} else {
			Log.e(null, "File does not exist.");
			return false;
		}
	}

	public static boolean PackageInstalled(Context context, String pkg_name) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(pkg_name, 0);
			return packageInfo != null;
		} catch (NameNotFoundException e) {
			return false;
		}
	}
}
