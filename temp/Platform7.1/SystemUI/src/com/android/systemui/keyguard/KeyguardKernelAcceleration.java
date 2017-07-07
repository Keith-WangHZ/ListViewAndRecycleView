package com.android.systemui.keyguard;

import java.lang.reflect.Method;
import android.content.Context;
import android.util.Log;

public class KeyguardKernelAcceleration {

	private static final String TAG = "HKModeKernelAcceleration";
	private Class<?> mPerf = null;
	boolean bIsPerfBoostEnabled = false;
	private int lBoostTimeOut = 0;
	private int lBoostCpuBoost = 0;
	private int lBoostSchedBoost = 0;
	private int lBoostPcDisblBoost = 0;
	private int lBoostKsmBoost = 0;
	private int lBoostPreferIdle = 0;
	private static KeyguardKernelAcceleration mHKModeKernelAcceleration;

	private KeyguardKernelAcceleration(Context mContext) {
		// TODO Auto-generated constructor stub
		bIsPerfBoostEnabled = true;
		if (bIsPerfBoostEnabled) {
			lBoostSchedBoost = 0x1E01;
			lBoostTimeOut = 0;
			lBoostCpuBoost = 0x20D;
			lBoostPcDisblBoost = 0;
			lBoostKsmBoost = 0;
			lBoostPreferIdle = 0x3E01;
		}

		if (mPerf == null && bIsPerfBoostEnabled) {
			try {
				mPerf = Class.forName("org.codeaurora.Performance");
			} catch (Exception e) {
			}
		}
	}

	public static KeyguardKernelAcceleration getInstance(Context mContext) {
		if (mHKModeKernelAcceleration == null) {
			mHKModeKernelAcceleration = new KeyguardKernelAcceleration(mContext);
		}
		return mHKModeKernelAcceleration;
	}

	private void setBoosTimeOut(int timeout){
		lBoostTimeOut = timeout;
	}
	public void perfLockAcquire() {
		if (mPerf != null) {
			Log.d(TAG, "perfLockAcquire()");
			if (0 == lBoostTimeOut) {
				lBoostTimeOut = 3000;
			}
			try {
				int[] param = { lBoostPcDisblBoost, lBoostSchedBoost, lBoostCpuBoost, lBoostKsmBoost,
						lBoostPreferIdle };
				Method method = mPerf.getMethod("perfLockAcquire", int.class, int[].class);
				method.invoke(mPerf.newInstance(), lBoostTimeOut, param);
			} catch (Exception e) {
				Log.v(TAG, "perfLockAcquire() error : " + e);
			}
		}
	}

}
