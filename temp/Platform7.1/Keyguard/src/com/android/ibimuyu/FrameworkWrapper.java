package com.android.ibimuyu;

import java.lang.reflect.Method;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FrameworkWrapper {
	private static final int WRAPPER_VERSION = 2;
	private Context mContext;
	private Object mInterface;
	private static ClassLoader mPkgClassloader = null;
	// private static final String FRAMEWORK_PACKAGE_NAME =
	// "com.ibimuyu.lockscreen";
	private static final String FRAMEWORK_CLASS_NAME = "com.zookingsoft.framework.lockscreen.load.LockscreenService";

	protected static final int MSG_W2F_CREATE = 1;
	protected static final int MSG_W2F_DESTROY = 2;
	protected static final int MSG_W2F_STOP = 3;
	protected static final int MSG_W2F_PLAY = 4;
	protected static final int MSG_W2F_LOAD_BY_STREAM = 5;
	protected static final int MSG_W2F_RELOAD = 6;
	protected static final int MSG_W2F_DISPATCH_KEY_EVENT = 7;
	protected static final int MSG_W2F_UPDATE_WALLPAPER = 8;
	protected static final int MSG_W2F_GET_BACKGROUND = 9;
	protected static final int MSG_W2F_CHECK_BACKGROUND = 10;
	protected static final int MSG_W2F_UNLOAD = 11;

	protected static final int MSG_W2F_PHONE_INFO_CHANGGE = 12;

	protected static final int MSG_W2F_SET_THEMEID = 13;
	protected static final int MSG_W2F_SHOW = 14;
	protected static final int MSG_W2F_HIDE = 15;

	public static final int MSG_W2F_MULTI_SET_SHOW_THEMES = 20;
	public static final int MSG_W2F_MULTI_ADD_SHOW_THEMES = 21;
	public static final int MSG_W2F_MULTI_DEL_SHOW_THEMES = 22;
	public static final int MSG_W2F_MULTI_GET_VIEW = 23;
	public static final int MSG_W2F_MULTI_HIDE = 24;
	public static final int MSG_W2F_MULTI_SHOW_NEXT = 25;
	public static final int MSG_W2F_MULTI_GET_SHOW_THEMES = 26;
	protected static final int MSG_W2F_MULTI_GET_CURRENT_THEME_ID = 27;
	protected static final int MSG_W2F_MULTI_GET_THEME_BY_ID = 28;
	protected static final int MSG_W2F_MULTI_RELEASE_THEME_BY_ID = 29;

	public static final int MSG_F2W_UNLOCK = 1;

	public static final int ERROR_ID_NOERROR = 0;
	private String mPakcageName;
	private static final String TAG = "zooking.FrameworkWrapper";

	public boolean create(Context context, String packageName) {
		mContext = context;
		Class<?> obj = loadClass(packageName);
		
		if (obj == null) {
			destory();
			return false;
		}

		try {
			mInterface = obj.newInstance();
		} catch (Exception e) {
			destory();
			e.printStackTrace();
			return false;
		}

		if (!checkVersion()) {
			destory();
			return false;
		}
		mPakcageName = packageName;
		return true;
	}

	public boolean init(Handler handler, int flag) {
		boolean ok = false;
		if (mInterface != null) {
			Method method = null;
			if (!ok) {
				try {
					method = mInterface.getClass().getMethod("onInit", Context.class, Handler.class, int.class, String.class);
					ok = (Boolean) method.invoke(mInterface, mContext, handler, flag, mPakcageName);
				} catch (Exception e) {
					Log.d(TAG, "no onInit context handler int String");
					ok = false;
				}
			}

			if (!ok) {
				try {
					method = mInterface.getClass().getMethod("onInit", Context.class, Handler.class, int.class);
					ok = (Boolean) method.invoke(mInterface, mContext, handler, flag);
				} catch (Exception e) {
					Log.d(TAG, "no onInit context handler int");
					ok = false;
				}
			}

			if (!ok) {
				try {
					method = mInterface.getClass().getMethod("onInit", Context.class, Handler.class);
					ok = (Boolean) method.invoke(mInterface, mContext, handler);
				} catch (Exception e) {
					Log.d(TAG, "no onInit context handler");
					ok = false;
				}
			}
		}
		return ok;
	}

	void destory() {
		mContext = null;
		mInterface = null;
	}

	private Class<?> loadClass(String packageName) {
		Class<?> cls = null;
		try {
			cls = mContext.getClassLoader().loadClass(FRAMEWORK_CLASS_NAME);
		} catch (Exception e) {

		}
		if (cls == null) {
			try {
				if (mPkgClassloader == null) {
					Context packageContext = mContext.getApplicationContext().createPackageContext(packageName,
							Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
					mPkgClassloader = packageContext.getClassLoader();
				}

				cls = mPkgClassloader.loadClass(FRAMEWORK_CLASS_NAME);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return cls;
	}

	private boolean checkVersion() {
		boolean ok = false;
		try {
			Method method = mInterface.getClass().getMethod("checkVersion", int.class);
			ok = (Boolean) method.invoke(mInterface, WRAPPER_VERSION);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ok;
	}

	public Object callFWMethod(Message msg) {
		Object result = null;
		try {
			Method method = mInterface.getClass().getMethod("onWrapperCall", Message.class);
			result = method.invoke(mInterface, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		msg.recycle();
		return result;
	}

	public boolean sendMsgToFW(Message msg) {
		boolean ok = false;
		try {
			Method method = mInterface.getClass().getMethod("onWrapperMsg", Message.class);
			ok = (Boolean) method.invoke(mInterface, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		msg.recycle();
		return ok;
	}

	public void setAppTag(String appTag) {
		try {
			Method method = mInterface.getClass().getMethod("setAppTag", String.class);
			method.invoke(mInterface, appTag);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
