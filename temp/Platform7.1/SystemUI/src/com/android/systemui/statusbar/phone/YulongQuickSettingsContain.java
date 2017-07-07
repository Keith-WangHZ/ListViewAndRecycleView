package com.android.systemui.statusbar.phone;

import android.content.Context;

public class YulongQuickSettingsContain {
	private static final String TAG = "YulongQuickSettings";
	private static YulongQuickSettings sInstance;
	private static YulongQuickSettings sInstanceSecure;

	public static YulongQuickSettings getInstance(Context context, Boolean bPrimaryUser) {
		if (sInstanceSecure == null) {
			sInstanceSecure = new YulongQuickSettings(context, true);
		}
		if (sInstance == null) {
			sInstance = new YulongQuickSettings(context, false);
		}
		if (bPrimaryUser) {
			return sInstance;
		} else {
			return sInstanceSecure;
		}
	}

	public static void initInstance() {
		sInstance = null;
		sInstanceSecure = null;
	}
}
