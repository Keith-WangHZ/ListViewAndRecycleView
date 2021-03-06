package com.android.systemui.helper;

import android.util.Slog;
import android.view.MotionEvent;

public class LogHelper {

	private static final String GLOBAL_TAG = "STATUS_BAR";
	private static final String TOUCH_TAG = GLOBAL_TAG + "_TRACH_TOUCH"; // STATUS_BAR_TRACH_TOUCH
	private static final String TEMP_TAG = GLOBAL_TAG + "_TEMP";
	
	public static final Boolean NOLOGGING = false;
	
	public static void sd(String tag, String msg) {
		Slog.d(GLOBAL_TAG, tag + " " + msg);
	}

	public static void se(String tag, String msg) {
		Slog.e(GLOBAL_TAG, tag + " " + msg);
	}
	
	public static void traceTouch(String tag, String fun, MotionEvent e){
		Slog.d(TOUCH_TAG, tag + "." + fun + " ==> " + MotionEvent.actionToString(e.getAction()));
	}
	
	public static void temp(String tag, String msg){
		Slog.d(TEMP_TAG, tag + " " + msg);
	}
	
	public static void sv(String tag, String msg){
		Slog.v(GLOBAL_TAG, tag + " " + msg);
	}
}
