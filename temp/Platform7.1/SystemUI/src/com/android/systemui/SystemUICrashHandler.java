package com.android.systemui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * UncaughtException handler
 */
public class SystemUICrashHandler implements UncaughtExceptionHandler {

	/**
	 * sth to do
	 * @param context
	 */
	private void doSomething(Context context) {
		Log.e(TAG,"restore android lockscreen");
		SharedPreferences preferences = mContext.getSharedPreferences("KeyguardPreferences", Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString("cool_show_theme_path", "");
		editor.putBoolean("isZooking", false);
		editor.commit();
	}

	/**
	 * make sure only one instance active
	 */
	private SystemUICrashHandler() {
	}

	/**
	 * get actived instance
	 * @return
	 */
	public static SystemUICrashHandler getInstance() {
		if (instance == null)
			instance = new SystemUICrashHandler();
		return instance;
	}

	public void init(Context context) {
		mContext = context;
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e(TAG,"FATAL! SystemUI crashed: ",ex);
		if (!handleException(ex) && mDefaultHandler != null) {
			mDefaultHandler.uncaughtException(thread, ex);
		} else {
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(1);
		}
	}

	private boolean handleException(Throwable ex) {
		if (ex == null) {
			return false;
		}
		SharedPreferences preferences = mContext.getSharedPreferences("KeyguardPreferences", Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean("ignore_antitheft_state", true);
		editor.commit();
		if (filterKeyWork(ex)) {
			doSomething(mContext);
		}
		return true;
	}

	private boolean filterKeyWork(Throwable ex) {
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		return writer.toString().contains(FILTER_KEY_WORD_ZOOKING) || writer.toString().contains(FILTER_KEY_WORD_COOLSHOW)
				|| ex instanceof OutOfMemoryError;
	}
	public static final String TAG = "SystemUICrashHandler";
	public static final String FILTER_KEY_WORD_COOLSHOW = "coolshow";
	public static final String FILTER_KEY_WORD_ZOOKING = "zookingsoft";
	private Thread.UncaughtExceptionHandler mDefaultHandler;
	private static SystemUICrashHandler instance;
	private Context mContext;
}
