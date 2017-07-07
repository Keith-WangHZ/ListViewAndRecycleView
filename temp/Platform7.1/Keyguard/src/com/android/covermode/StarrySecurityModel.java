package com.android.covermode;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Log;

public class StarrySecurityModel {

	private final static String TAG = "StarrySecurityModel";
	private Context mContext;
	private LockPatternUtils mLockPatternUtils;
	private static StarrySecurityModel sInstance;

	enum SecurityMode {
		Invalid, None, Pattern, PIN
	}

	StarrySecurityModel(Context context) {
		mContext = context;
		mLockPatternUtils = new LockPatternUtils(context);
	}

	public static StarrySecurityModel getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new StarrySecurityModel(context);
		}
		return sInstance;
	}

	SecurityMode getSecurityModel() {
		SecurityMode mode = SecurityMode.None;
		final int security = mLockPatternUtils.getKeyguardStoredPasswordQuality(KeyguardUpdateMonitor.getCurrentUser());
		Log.v(TAG, "security = " + security);
		switch (security) {
		case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
			mode = mLockPatternUtils.isLockPasswordEnabled(KeyguardUpdateMonitor.getCurrentUser()) ? SecurityMode.PIN : SecurityMode.None;
			break;
		case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
		case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
			mode = mLockPatternUtils.isLockPatternEnabled(KeyguardUpdateMonitor.getCurrentUser()) ? SecurityMode.Pattern : SecurityMode.None;
			break;
		default:
			mode = SecurityMode.PIN;
		}
		Log.v(TAG, "mode =" + mode);
		return mode;

	}

}