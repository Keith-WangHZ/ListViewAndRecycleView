package com.android.ibimuyu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class LockscreenWrapper extends BaseWrapper {

	private static class LoadStreamObj {
		public String name;
		public InputStream stream;

		LoadStreamObj(String name, InputStream stream) {
			this.name = name;
			this.stream = stream;
		}
	}

	public LockscreenWrapper(Context context) {
		super(context);
	}

	public LockscreenWrapper(Context context, String packageName) {
		super(context, packageName);
	}

	public boolean create() {
		super.create();
		if (mFrameWrapper != null) {
			return mFrameWrapper.init(mCallbackHandler, 0);
		}
		return false;
	}

	public View load(String name, InputStream istrean, boolean autoShow, boolean autoPlay) {

		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_LOAD_BY_STREAM;
			msg.arg1 = autoShow ? 1 : 0;
			msg.arg2 = autoPlay ? 1 : 0;
			LoadStreamObj obj = new LoadStreamObj(name, istrean);
			msg.obj = obj;

			View view = (View) mFrameWrapper.callFWMethod(msg);
			return updateLayout(view);
		}
		return null;
	}

	public View load(boolean autoShow, boolean autoPlay) {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_RELOAD;
			msg.arg1 = autoShow ? 1 : 0;
			msg.arg2 = autoPlay ? 1 : 0;
			View view = (View) mFrameWrapper.callFWMethod(msg);
			return updateLayout(view);
		}

		return null;
	}

	public boolean unLoad() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_UNLOAD;
			Object obj = mFrameWrapper.callFWMethod(msg);
			return obj != null && (Boolean) obj;
		}
		return false;
	}

	public boolean show(boolean autoPlay) {
		if (mFrameWrapper != null) {
			mLayout.setVisibility(View.VISIBLE);
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_SHOW;
			msg.arg1 = autoPlay ? 1 : 0;
			Object obj = mFrameWrapper.callFWMethod(msg);
			return obj != null && (Boolean) obj;
		}
		return false;
	}

	public boolean hide() {
		if (mFrameWrapper != null) {
			mLayout.setVisibility(View.GONE);
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_HIDE;
			Object obj = mFrameWrapper.callFWMethod(msg);
			return obj != null && (Boolean) obj;
		}
		return false;
	}

	public void setUnlockRunnable(Runnable runbale) {
		mUnlockRunnable = runbale;
	}

	public void updateWallpaper(String newWallpaperPath, String lockScreenDir) {
		setWallpaper(mContext, newWallpaperPath, lockScreenDir + "/default_lock_wallpaper.jpg");
		updateWallpaper();
	}

	public void updateWallpaper(Bitmap bitmap, String lockScreenDir) {
		setWallpaper(mContext, bitmap, lockScreenDir + "/default_lock_wallpaper.jpg");
		updateWallpaper();
	}

	public void updateWallpaper(Runnable copyWallpaper2lockScreenDir) {
		if (copyWallpaper2lockScreenDir != null) {
			copyWallpaper2lockScreenDir.run();
			updateWallpaper();
		}
	}

	public void updateWallpaper() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_UPDATE_WALLPAPER;
			mFrameWrapper.sendMsgToFW(msg);
		}
	}

	public boolean hasBackground() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_CHECK_BACKGROUND;
			Object obj = mFrameWrapper.callFWMethod(msg);
			return obj != null && (Boolean) obj;
		}
		return false;
	}

	public Bitmap getBackground() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_GET_BACKGROUND;
			return (Bitmap) mFrameWrapper.callFWMethod(msg);
		}
		return null;
	}

	public void onMissCallChange(int num) {
		changePhoneData(1, (double) num);
	}

	public void onUnreadMsgChange(int num) {
		changePhoneData(0, (double) num);
	}

	public void onBatteryChange(int num) {
		changePhoneData(2, (double) num);
	}

	public void changePhoneData(int type, double data) {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_PHONE_INFO_CHANGGE;
			msg.arg1 = type;
			msg.obj = data;
			mFrameWrapper.sendMsgToFW(msg);
		}
	}

	public void setNewThemeId(String themeId) {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_SET_THEMEID;
			msg.obj = themeId;
			mFrameWrapper.sendMsgToFW(msg);
		}
	}

	public static void setWallpaper(Context context, Bitmap bitmap, String defaultLockWallpaperPath) {
		String file = defaultLockWallpaperPath;
		File wallpaper = new File(file);
		wallpaper.delete();
		try {
			wallpaper.createNewFile();
			FileOutputStream stream = new FileOutputStream(wallpaper);
			bitmap.compress(CompressFormat.JPEG, 100, stream);
			stream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setWallpaper(Context context, String path, String defaultLockWallpaperPath) {
		File wallpaper = new File(path);
		wallpaper.delete();
		FileOutputStream fos = null;
		FileInputStream is = null;
		try {
			wallpaper.createNewFile();
			fos = new FileOutputStream(wallpaper);
			byte[] buffer = new byte[1024 * 8];
			is = new FileInputStream(path);
			while (true) {
				int count = is.read(buffer);
				if (count <= 0) {
					break;
				}
				fos.write(buffer, 0, count);
				fos.flush();
			}
			is.close();
		} catch (Exception e) {

		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
