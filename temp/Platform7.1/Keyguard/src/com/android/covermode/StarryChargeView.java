package com.android.covermode;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class StarryChargeView extends TextView {

	private static final String TAG = "StarryChargeView";

	KeyguardUpdateMonitor mUpdateMonitor;
	private Context mContext;

	// are we showing battery information?
	boolean mShowingBatteryInfo = false;

	// is the bouncer up?
	boolean mShowingBouncer = false;

	// last known plugged in state
	boolean mCharging = false;

	// / boolean isStartAnima = false;

	// last known battery level
	int mBatteryLevel = 100;
	protected boolean mBatteryCharged;
	protected boolean mBatteryIsLow;
	private boolean isFirst = true; 

	public StarryChargeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.v(TAG, "----------StarryChargeView init()-----------------------");
		// TODO Auto-generated constructor stub
		mContext = context;
	}

	private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
			Log.v(TAG, "----------------onRefreshBatteryInfo---------------------");
			mShowingBatteryInfo = status.isPluggedIn() || status.isBatteryLow();
			mCharging = status.status == BatteryManager.BATTERY_STATUS_CHARGING || status.status == BatteryManager.BATTERY_STATUS_FULL;
			mBatteryLevel = status.level;
			mBatteryCharged = status.isCharged();
			mBatteryIsLow = status.isBatteryLow();
			Log.v(TAG, "mBatteryLevel = " + mBatteryLevel);
			if (isFirst) {
				update(false);
				isFirst = false;
			} else {
				update(false);
			}
		}
		
		@Override
		public void onWeatherChanged(com.android.keyguard.KeyguardUpdateMonitor.WeatherData weatherData) {
			update(false);
		};
	};

	private Runnable resetRunable = new Runnable() {
		@Override
		public void run() {
			update(false);
		}
	};

	public void update(boolean isStartAnima) {
		CharSequence status = getChargeInfo();
		CharSequence status1 = mContext.getString(R.string.starry_unlock_info);
		Log.v(TAG, "status = " + status);
		if (isStartAnima) {
			setText(status1);
		} else {
			setText(status);
		}
	}
	
	public void notice(String notice) {
		if (notice.isEmpty()) {
			return;
		}
		Log.v(TAG, "notice = " + notice);
		setText(notice);
		Handler handler = getHandler();
		if (handler != null) {
			handler.removeCallbacks(resetRunable);
			handler.postDelayed(resetRunable, 3000);
		}
	}

	private CharSequence getChargeInfo() {
		CharSequence string = null;
		if (mShowingBatteryInfo) {
			// Battery status
			if (mCharging) {
				// Charging, charged or waiting to charge.
				string = getContext().getString(mBatteryCharged ? R.string.keyguard_charged : R.string.keyguard_plugged_in, mBatteryLevel + "%");
			} else if (mBatteryIsLow) {
				// Battery is low
				string = getContext().getString(R.string.keyguard_low_battery);
			}
		}
		return string;
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
	}

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
	}

}
