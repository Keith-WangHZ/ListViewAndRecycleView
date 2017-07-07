package com.android.systemui.statusbar.phone;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;

import com.android.covermode.HolsterFixableView;
import com.android.ibimuyu.LockscreenWrapper;
import com.android.systemui.R;
import com.android.systemui.recents.misc.YLUtils;

import dalvik.system.DexClassLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Manages creating, showing, hiding and releasing the theme view within
 * keyguard.
 * 
 * @author mengludong 2016.1.12
 */
public class StatusBarThemeViewManager {

	public static final String TAG = "StatusBarThemeViewManager";

	private static final String ZOOKING_PACKAGE_NAME = "com.zookingsoft.themestore";
	private static final String COOLSHOW_PACKAGE_NAME = "com.yulong.android.coolshow";

	private Context mContext;
	private boolean mIsVlifeTheme;
	private LockscreenWrapper mLockscreenWrapper;
	private View mLockThemeView;
	// private Drawable mCoolShowThemeWallpaper;
	private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;

	private static Class<?> vlifeLockClass;
	private static Object vlifeLockInstance;
	private static ClassLoader vlifeClassLoader;

	private Runnable mDismissRunnable;

	private final boolean mIsZookingStore;
	private PackageManager pm;

	public static final int MSG_THEME_VIEW_LOADED = 100;
	public static final int MSG_THEME_LOAD_FAILED = 101;

	public Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_THEME_VIEW_LOADED:
				mStatusBarKeyguardViewManager.onLockThemeLoaded(true);
				break;
			case MSG_THEME_LOAD_FAILED:
				mStatusBarKeyguardViewManager.onLockThemeLoaded(false);
				break;
			default:
				break;
			}
		}
	};

	public StatusBarThemeViewManager(Context context, StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
		mContext = context;
		pm = mContext.getPackageManager();
		mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
		mIsZookingStore = YLUtils.isSpecificPackageExist(mContext, ZOOKING_PACKAGE_NAME);
		Log.v(TAG, "mIsZookingStore = " + mIsZookingStore);
	}

	/**
	 * show theme view
	 */
	public void show() {
		Log.v(TAG, "show() mIsVlifeTheme = " + mIsVlifeTheme);
		if (mIsVlifeTheme && vlifeClassLoader != null) {
			onShowVlife();
		} else if (mLockscreenWrapper != null) {
			mLockscreenWrapper.show(true);
		}
	}

	/**
	 * show vlife theme view
	 */
	private void onShowVlife() {
		try {
			Method m = vlifeLockClass.getDeclaredMethod("onShow");
			m.invoke(vlifeLockInstance);
		} catch (Exception e) {
			Log.e(TAG, e + "");
		}
	}

	/**
	 * hide theme view
	 */
	public void hide() {
		Log.v(TAG, "hide() mIsVlifeTheme = " + mIsVlifeTheme);
		if (mIsVlifeTheme && vlifeClassLoader != null) {
			onHideVlife();
		} else if (mLockscreenWrapper != null) {
			mLockscreenWrapper.hide();
		}
	}

	/**
	 * hide vlife theme view
	 */
	private void onHideVlife() {
		try {
			Method m = vlifeLockClass.getDeclaredMethod("onHide");
			m.invoke(vlifeLockInstance);
		} catch (Exception e) {
			Log.e(TAG, e + "");
		}
	}

	/**
	 * load theme view, destory resource if failed
	 * 
	 * @param path
	 *            view file path
	 * @param reload
	 *            true if SystemUI process restart
	 * @param isVlife
	 *            true if it's vlife theme
	 * @return true if theme view load success
	 */
	public void loadThemeView(String path, boolean reload, boolean isVlife) {
		if (mStatusBarKeyguardViewManager.isCovered()) {
			loadCoverModeView();
			return;
		}
		if (TextUtils.isEmpty(path)) {
			mIsVlifeTheme = false;
			mStatusBarKeyguardViewManager.onLockThemeLoaded(false);
			releaseLockTheme();
		} else {
			if (isVlife) {
				// avoid ANR
				new LoadThemeViewThread(path, reload, isVlife).start();
			} else {
				loadThemeViewInternal(path, reload, isVlife);
			}
		}
	}

	private void loadCoverModeView() {
		mLockThemeView = (HolsterFixableView) View.inflate(mContext, R.layout.starry_main_view, null);
		if (mLockThemeView != null) {
			mHandler.sendEmptyMessage(MSG_THEME_VIEW_LOADED);
		} else {
			mHandler.sendEmptyMessage(MSG_THEME_LOAD_FAILED);
		}
	}

	private class LoadThemeViewThread extends Thread {

		private String mPath;
		private boolean mReload;
		private boolean mIsVlife;

		public LoadThemeViewThread(String path, boolean reload, boolean isVlife) {
			mPath = path;
			mReload = reload;
			mIsVlife = isVlife;
		}

		@Override
		public void run() {
			loadThemeViewInternal(mPath, mReload, mIsVlife);
		}

	}

	/**
	 * set lock theme may cause systemui crash, so we set this flag before load
	 * zooking theme view. we read this flag when restart,
	 * see @KeyguardViewMediator.onSystemReady()
	 * 
	 * @param donntLock
	 */
	public static void setDonntLockFlag(Context context, boolean donntLock) {
		SharedPreferences preferences = context.getSharedPreferences("KeyguardPreferences", Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean("donnt_lock_when_restart", donntLock);
		editor.commit();
	}

	private void loadThemeViewInternal(String path, boolean reload, boolean isVlife) {
		if (isVlife) {
			loadVlifeThemeView(path);
		} else {
			loadZookingThemeView(path, reload);
		}
		if (mLockThemeView != null && mIsVlifeTheme != isVlife) {
			mIsVlifeTheme = isVlife;
			if (mIsVlifeTheme) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						releaseZookingWrapper();
					}
				});
			} else {
				releaseVlifeResources();
			}
		}
		
		//ONLY FOR DEBUG
		/*if (path == "null") {
			mLockThemeView = null;
		} else {
			TextView txv = new TextView(mContext);
			txv.setText("hello theme");
			mLockThemeView = txv;
		}*/
		//end
		
		if (mLockThemeView != null) {
			// mCoolShowThemeWallpaper = mIsVlifeTheme ? getVlifeBackground() :
			// getZookingBackground();
			mHandler.sendEmptyMessage(MSG_THEME_VIEW_LOADED);
		} else {
			mHandler.sendEmptyMessage(MSG_THEME_LOAD_FAILED);
			releaseLockTheme();
		}
	}

	private void loadZookingThemeView(String path, boolean reload) {
		if (mLockscreenWrapper == null) {
			initWrapper();
			setUnlockRunnable(false/* isVlife */);
		}
		if (!reload) {
			setDonntLockFlag(mContext, true);
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File(path));
			mLockThemeView = reload ? mLockscreenWrapper.load(true, true)
					: mLockscreenWrapper.load("thirdlockscreen", fis, true, true);
			fis.close();
			if (!reload) {
				setDonntLockFlag(mContext, false);
			}
			Log.v(TAG, mLockThemeView == null ? "failed to load lock theme view." : "lock theme view loaded.");
		} catch (Exception e) {
			mLockThemeView = null;
			Log.v(TAG, "load lock theme error : " + e);
		}
	}

	private void loadVlifeThemeView(String path) {
		boolean classLoaded = vlifeClassLoader != null;
		if (!classLoaded) {
			classLoaded = initVlifeClassLoader();
			initVlife(mContext);
		}
		if (classLoaded) {
			mLockThemeView = loadVlifeLockView(path);
			setUnlockRunnable(true/* isVlife */);
		} else {
			mLockThemeView = null;
		}
	}

	private void releaseLockTheme() {
		mLockThemeView = null;
		// mCoolShowThemeWallpaper = null;
		releaseZookingWrapper();
		releaseVlifeResources();
		mDismissRunnable = null;
	}

	private void setUnlockRunnable(boolean isVlife) {
		if (mDismissRunnable == null) {
			mDismissRunnable = new Runnable() {
				@Override
				public void run() {
					mStatusBarKeyguardViewManager.dismiss();
				}
			};
		}
		if (isVlife) {
			setVlifeUnlockRunnable();
		} else {
			setZookingUnlcokRunnable();
		}
	}

	private void setZookingUnlcokRunnable() {
		if (mLockscreenWrapper != null) {
			mLockscreenWrapper.setUnlockRunnable(mDismissRunnable);
		}
	}

	private void setVlifeUnlockRunnable() {
		try {
			Method m = vlifeLockClass.getDeclaredMethod("setUnlockRunnable", Runnable.class);
			m.invoke(vlifeLockInstance, mDismissRunnable);
		} catch (Exception e) {
			Log.e(TAG, e + "");
		}
	}

	private void initVlife(Context context) {
		try {
			Method m = vlifeLockClass.getMethod("init", Context.class);
			m.invoke(vlifeLockInstance, context);
		} catch (Exception e) {
			Log.e(TAG, e + "");
		}
	}

	private boolean initVlifeClassLoader() {
		Log.v(TAG, "initVlifeClassLoader()");
		// PackageManager pm = mContext.getPackageManager();
		PackageInfo packageInfo = null;
		try {
			packageInfo = pm.getPackageInfo("com.vlife.ivvilock.wallpaper", PackageManager.GET_META_DATA);
		} catch (Exception e) {
			Log.e(TAG, e + "");
		}
		if (packageInfo != null) {
			String apkPath = packageInfo.applicationInfo.sourceDir;
			final File dexFile = new File(mContext.getDir("dex", 0).getAbsolutePath() + "/plugin/");
			dexFile.mkdirs();
			final File libFile = new File(mContext.getDir("lib", 0).getAbsolutePath() + "/plugin/");
			libFile.mkdirs();
			vlifeClassLoader = new DexClassLoader(apkPath, dexFile.getAbsolutePath(), libFile.getAbsolutePath(),
					ClassLoader.getSystemClassLoader());
			try {
				vlifeLockClass = vlifeClassLoader.loadClass("com.vlife.VlifeLockScreenForIvvi");
				vlifeLockInstance = vlifeLockClass.newInstance();
			} catch (Exception e) {
				Log.e(TAG, "loadClass error : " + e);
				return false;
			}
			return true;
		}
		Log.v(TAG, "initClassLoader failed.");
		return false;
	}

	private View loadVlifeLockView(String path) {
		Log.v(TAG, "loadVlifeLockView()");
		View view = null;
		Method m;
		try {
			m = vlifeLockClass.getDeclaredMethod("getLockView", String.class);
			view = (View) m.invoke(vlifeLockInstance, path);
		} catch (Exception e) {
			Log.v(TAG, "Load vlife lock theme view error : " + e);
		}
		return view;
	}

	private void releaseZookingWrapper() {
		Log.v(TAG, "releaseZookingWrapper()");
		if (mLockscreenWrapper != null) {
			mLockscreenWrapper.unLoad();
			mLockscreenWrapper.destory();
			mLockscreenWrapper = null;
		}
	}

	private void releaseVlifeResources() {
		Log.v(TAG, "releaseVlifeResources()");
		if (vlifeClassLoader != null) {
			try {
				Method m = vlifeLockClass.getDeclaredMethod("onDestroy");
				m.invoke(vlifeLockInstance);
			} catch (Exception e) {
				Log.e(TAG, e + "");
			}
		}
	}

	private void initWrapper() {
		Log.v(TAG, "initWrapper()");
		try {
			mLockscreenWrapper = new LockscreenWrapper(mContext,
					mIsZookingStore ? ZOOKING_PACKAGE_NAME : COOLSHOW_PACKAGE_NAME);
			mLockscreenWrapper.create();
		} catch (Exception e) {
			Log.v(TAG, "initWrapper() error: " + e);
		}
	}

	public View getThemeView() {
		return mLockThemeView;
	}

	private Drawable getVlifeBackground() {
		if (vlifeClassLoader != null) {
			Bitmap bm = null;
			try {
				Method m = vlifeLockClass.getDeclaredMethod("getBackground");
				bm = (Bitmap) m.invoke(vlifeLockInstance);
			} catch (Exception e) {
				Log.e(TAG, e + "");
			}
			if (bm != null) {
				return new BitmapDrawable(null, bm);
			}
		}
		return null;
	}

	private Drawable getZookingBackground() {
		return mLockscreenWrapper != null && mLockscreenWrapper.hasBackground()
				? new BitmapDrawable(null, mLockscreenWrapper.getBackground()) : null;
	}

	public Drawable getThemeBackground() {
		if(mIsVlifeTheme){
			return getVlifeBackground();
		}else{
			return getZookingBackground();
		}
	}

	public boolean isVlifeTheme() {
		return mIsVlifeTheme;
	}
}
