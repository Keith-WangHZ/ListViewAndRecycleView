package com.android.keyguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

public class TouchProtectScreen {
	private static TouchProtectScreen sInstance;
	private Context mContext;
	WindowManager mWindowManager;
	WindowManager.LayoutParams mLayoutParams;
	private static final String CANCEL_TOUCH_PROTECT_SCREEN_ACTION = "yulong.intent.action.cancel_touchProtectScreen";
	private static final String TAG = "TouchProtectScreen";
	private LinearLayout mView;
	SensorManager mSensorManager;
	Sensor mProximitySensor;
	boolean mSensorBeCovered = false;

	boolean mShowing = false;

	boolean mKeyguardShowing = false;

	boolean mRegisterSensorListener = false;

	boolean mScreenOn = true;

	boolean mSmallKeyguardModeEnable = false;
	boolean mHasInit = false;
	boolean mViewInited = false;
	boolean mHideByUser = false;
	boolean mShowed = false;
	// boolean mTimeOut = false;
	private boolean mEnabled = false;
//	private boolean mOccluded;

	public synchronized static TouchProtectScreen getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new TouchProtectScreen(context);
		}
		return sInstance;
	}

	private TouchProtectScreen() {
		throw new RuntimeException("no context");
	}

	private TouchProtectScreen(Context context) {
		mContext = context;
		init();
	}

	private void init() {
		mHandler.sendEmptyMessage(INIT);
	}

	public void onUserActivity() {
		setView();
	}

	public boolean isSensorBeCoveredAndNotShowing() {
		return mEnabled && mSensorBeCovered && !mShowing;
	}

	private void setView() {
		if (!mEnabled) {
			return;
		}
		if (!mHasInit) {
			return;
		}
		mHandler.sendEmptyMessage(SET_VIEW);
	}

	private void onScreenOn() {
		if (!mEnabled) {
			return;
		}
		mScreenOn = true;
		setSensorListener();
		// setView();
		// mHandler.sendEmptyMessageDelayed(TIME_OUT, 1000);
	}

	private void onScreenOff() {
		// mHandler.removeMessages(TIME_OUT);
		getEnableFlag();
		if (mEnabled) {
			mShowed = false;
			mScreenOn = false;
			// mTimeOut = false;
			mHideByUser = false;
			setSensorListener();
			setView();
		}
	}

	private void getEnableFlag() {
		int open = Settings.System.getInt(mContext.getContentResolver(), "mistouch_prevention_state", 0);
		mEnabled = open == 1;
	}

	private void smallKeyguardModeEnable(boolean enabled) {
		mSmallKeyguardModeEnable = enabled;
		setSensorListener();
		setView();
	}

	private static final int INIT = 104;
	private static final int SET_VIEW = 105;
	// private static final int TIME_OUT = 106;

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case INIT:
				handleInit();
				break;
			case SET_VIEW:
				handleSetView();
				break;
			/*
			 * case TIME_OUT: handleTimeOut(); break;
			 */
			}
		}
	};

	private KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onStartedWakingUp() {
			onScreenOn();
		};

		@Override
		public void onFinishedGoingToSleep(int why) {
			onScreenOff();
		};

		@Override
		public void onKeyguardVisibilityChanged(boolean showing) {
//			mOccluded = KeyguardUpdateMonitor.getInstance(mContext).isOccluded();
//			mKeyguardShowing = showing /*|| mOccluded*/;
			if (!showing) {
				setView();	
			}
			setSensorListener();
		};

		@Override
		public void onHallStateChanged(int state) {
			smallKeyguardModeEnable(state == 1);
		};
	};

	// private void handleTimeOut() {
	// mTimeOut = true;
	// if (!mShowing) {
	// setSensorListener();
	// }
	// }
	private boolean isKeyguardShowing(){
		mKeyguardShowing = KeyguardUpdateMonitor.getInstance(mContext).isKeyguardShowing();
		Log.d(TAG, "isKeyguardShowing()   mKeyguardShowing= "+mKeyguardShowing);
		return mKeyguardShowing;
	}


	private void handleInit() {
		mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mLayoutParams = new WindowManager.LayoutParams();
		mLayoutParams.type = LayoutParams.TYPE_MAGNIFICATION_OVERLAY;
		mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_NO_LIMITS
				| LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		mLayoutParams.format = PixelFormat.RGBA_8888;
		DisplayMetrics displayMetrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
		mLayoutParams.height = displayMetrics.heightPixels;
		mLayoutParams.width = LayoutParams.MATCH_PARENT;
		mLayoutParams.systemUiVisibility |= 0x00000020;
		mLayoutParams.gravity = Gravity.START | Gravity.TOP;
		mLayoutParams.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		String state = SystemProperties.get("persist.sys.hallsensor.switch");
		Log.d(TAG, "persist.sys.hallsensor.switch state =" + state);
		if (state.equals("1")) {
			mSmallKeyguardModeEnable = true;
		} else {
			mSmallKeyguardModeEnable = false;
		}
		mHasInit = true;
		getEnableFlag();
		setView();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(CANCEL_TOUCH_PROTECT_SCREEN_ACTION);
		intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
		mContext.registerReceiver(mCancelProtectScreentReceiver, intentFilter);
		KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUpdateCallback);
	}

	private void setSensorListener() {
		Log.d(TAG, "setting sensor listener");
		boolean occluded = KeyguardUpdateMonitor.getInstance(mContext).isOccluded();
		if (mEnabled && mScreenOn && (/*mKeyguardShowing || occluded*/isKeyguardShowing())&& !mSmallKeyguardModeEnable) {
			if (!mRegisterSensorListener) {
				mSensorBeCovered = false;
				Log.d(TAG, "SensorListener: mMySensorEventListener = " + mMySensorEventListener
						+ ", mProximitySensor = " + mProximitySensor + ", mSensorManager = " + mSensorManager);
				if (mMySensorEventListener != null && mProximitySensor != null && mSensorManager != null) {
					mSensorManager.registerListener(mMySensorEventListener, mProximitySensor,
							SensorManager.SENSOR_DELAY_GAME, mHandler);
				} else {
					Log.v(TAG, "mMySensorEventListener is null");
					return;
				}
				mRegisterSensorListener = true;
				Log.e(TAG, "sensor enable ");
			}
		} else {
			if (mRegisterSensorListener) {
				mSensorBeCovered = false;
				if (mSensorManager != null && mMySensorEventListener != null) {
					mSensorManager.unregisterListener(mMySensorEventListener);
				}
				mRegisterSensorListener = false;
				Log.d(TAG, "sensor disable ");
			}
		}
		Log.d(TAG, "mEnabled =" + mEnabled + ", mScreenOn ="
				+ mScreenOn + ", mKeyguardShowing =" + mKeyguardShowing 
				+ ", mSmallKeyguardModeEnable =" + mSmallKeyguardModeEnable + ", mSensorBeCovered1 =" + mSensorBeCovered
				+ ", mRegisterSensorListener =" + mRegisterSensorListener + ";isOccluded = " + occluded);
	}

	private void handleSetView() {
		boolean occluded = KeyguardUpdateMonitor.getInstance(mContext).isOccluded();
		Log.d(TAG, "setting view, mEnabled =" + mEnabled + ", mSensorBeCovered = " + mSensorBeCovered
				+ ", mSmallKeyguardModeEnable = " + mSmallKeyguardModeEnable + ", mShowing = " + mShowing + ";occluded = " + occluded);
		if (mEnabled && mSensorBeCovered && !mSmallKeyguardModeEnable && !mHideByUser  && mScreenOn
				&& (/*mKeyguardShowing || occluded*/isKeyguardShowing())) {
			show();
		} else {
			hide();
		}
	}

	private void hide() {
		if (!mShowing)
			return;
		mWindowManager.removeView(mView);
		Log.e(TAG, "touch protect view bas been removed");
		mShowing = false;
		try {
			SystemProperties.set("yulong.sys.homekey.disable", "0");
		} catch (Exception e) {
			Log.e(TAG, "set system property error : " + e);
		}
		setSensorListener();
	}

	private void show() {
		if (mShowing)
			return;
		initBeforeShow();
		mView.setAlpha(0f);
		mWindowManager.addView(mView, mLayoutParams);
		mView.animate().alpha(1f).setDuration(300).start();
		Log.e(TAG, "protect view added");
		mShowing = true;
		mShowed = true;
		try {
			SystemProperties.set("yulong.sys.homekey.disable", "1");
		} catch (Exception e) {
			Log.e(TAG, "set system property error : " + e);
			/*
			 * mViewShow = false; mHasShow = false;
			 * mWindowManager.removeView(mView);
			 */
		}
	}

	private void initBeforeShow() {
		if (mViewInited) {
			return;
		}
		mView = (LinearLayout) View.inflate(mContext, R.layout.touch_protect_view, null);
		mViewInited = true;
	}

	private final SensorEventListener mMySensorEventListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			int type = event.sensor.getType();
			if (type == Sensor.TYPE_PROXIMITY) {
				double distance = event.values[0];
				Log.d(TAG, "onSensorChanged start distance == " + distance);
				mSensorBeCovered = distance >= 0.0f && distance < mProximitySensor.getMaximumRange();
//				if (!mSensorBeCovered || KeyguardUpdateMonitor.getInstance(mContext).isOccluded()) {
					setView();
//				}
			}
		}
	};

	private BroadcastReceiver mCancelProtectScreentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (CANCEL_TOUCH_PROTECT_SCREEN_ACTION.equals(intent.getAction())) {
				Log.i(TAG, "Received CANCEL_TOUCH_PROTECT_SCREEN_ACTION.");
				mHideByUser = true;
				setView();
			} else if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
				mViewInited = false;
			}
		}
	};
}
