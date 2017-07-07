package com.android.covermode;

import com.android.covermode.StarryPINView.DismissPin;
import com.android.covermode.StarryPatternView.DismissAction;
import com.android.covermode.StarrySecurityModel.SecurityMode;
import com.android.keyguard.R;

import android.R.bool;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;//liusanjun
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;//liusanjun
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class StarryShortcutView extends LinearLayout {

	private static final String TAG = "StarryShortcutView";
	private static final String ACTION_DIAL_STARRYWINDOW = "com.yulong.dial.StarryWindow";
	private ImageButton mDialButton;
	private ImageButton mCameraButton;
	private ImageButton mFlashLight;
	private ImageButton mCalculatorButton;

	private StarrySecurityModel mStarrySecurityModel;
	private StarryPatternView mStarryPatternView;
	private StarryPINView mStarryPINView;
	private ViewGroup parent1;
	private ViewGroup parent2;
	private IntentFilter intentFilter;
	int[] starryBackground = new int[] { R.drawable.starry_up_menu_blue, R.drawable.starry_up_menu_purple, R.drawable.starry_up_menu_green,
			R.drawable.starry_up_menu_yellow };

	// For FlashLight-liusanjun:2014.08.26
	private static boolean mIsFlashLightOpen = false;
	private BroadcastReceiver mBroadcastReceiver = null;

	private void registerReceiver(){
		if (mBroadcastReceiver == null) {
			mBroadcastReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i(TAG, "receive broadcast:" + intent.getAction());
					if ("com.android.intent.action.FlashLight_On_Flag".equals(intent
							.getAction())
							) {
						mFlashLight.setBackground(getResources().getDrawable(R.drawable.starry_flashlight_pressed));
						mIsFlashLightOpen = true;
					}else if("com.android.intent.action.FlashLight_Close_Flag".equals(intent
							.getAction())
							) {
						mFlashLight.setBackground(getResources().getDrawable(R.drawable.starry_flashlight_normal));
						mIsFlashLightOpen = false;
					}
				}

			};
			if (intentFilter == null) {
				intentFilter = new IntentFilter();
				intentFilter
						.addAction("com.android.intent.action.FlashLight_On_Flag");
				intentFilter
						.addAction("com.android.intent.action.FlashLight_Close_Flag");
			}
			if(mBroadcastReceiver!=null&&intentFilter!=null){
			    mContext.registerReceiver(mBroadcastReceiver, intentFilter);
			}
		}
	}

	public void unRegisterReceiver() {
		mContext.unregisterReceiver(mBroadcastReceiver);
		mBroadcastReceiver = null;
		intentFilter = null;
	}

	private DismissAction mDismissAction = new DismissAction() {

		@Override
		public boolean Dismiss() {
			// TODO Auto-generated method stub
			Log.v(TAG, "---------- ShortCut mDismissAction---------------");
			openMiniDialpad();
			parent1.removeView(mStarryPatternView);
			return false;
		}

	};

	private DismissPin mDismissPin = new DismissPin() {

		@Override
		public boolean Dismiss() {
			// TODO Auto-generated method stub
			Log.v(TAG, "---------- ShortCut mDismissPin---------------");
			openMiniDialpad();
			parent2.removeView(mStarryPINView);
			return false;
		}

	};

	public StarryShortcutView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mStarrySecurityModel = StarrySecurityModel.getInstance(mContext);
		init();

	}

	@SuppressLint("NewApi")
	private void init() {
		int index = Settings.System.getInt(mContext.getContentResolver(), "view_window_bgcolor_index", 0);
		if (index < 0 || index > 3) {
			index = 0;
		}
		Drawable Background = this.getResources().getDrawable(starryBackground[index]);
		this.setBackground(Background);
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mCameraButton = (ImageButton) this.findViewById(R.id.camera);
		mDialButton = (ImageButton) this.findViewById(R.id.dial);
		mFlashLight = (ImageButton) this.findViewById(R.id.flashlight);
		mCalculatorButton = (ImageButton) this.findViewById(R.id.calculator);
		mCalculatorButton.setOnClickListener(mClickListener);
		mDialButton.setOnClickListener(mClickListener);
		mCameraButton.setOnClickListener(mClickListener);
		mFlashLight.setOnClickListener(mClickListener);
		mStarryPatternView = (StarryPatternView) View.inflate(mContext, R.layout.starry_pattern_view, null);
		mStarryPINView = (StarryPINView) View.inflate(mContext, R.layout.starry_pin_view, null);
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		unRegisterReceiver();
	}

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		registerReceiver();
	}

	private OnClickListener mClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int vid = v.getId();
			if (vid == R.id.dial) {
				if (HolsterFixableView.mStarryDismissed) {
					Log.v(TAG, "StarryHostViewNew.isDismiss() is true");
					openMiniDialpad();
					return;
				}
				switch (mStarrySecurityModel.getSecurityModel()) {
				case None:
					Log.v(TAG, "SecurityModel = None");
					openMiniDialpad();
					break;
				case Pattern:
					Log.v(TAG, "SecurityModel = Pattern");
					mStarryPatternView.setDismissAction(mDismissAction);
					parent1 = (ViewGroup) getParent().getParent();
					parent1.addView(mStarryPatternView, 0);
					break;
				case PIN:
					Log.v(TAG, "SecurityModel = PIN");
					mStarryPINView.setDismissPin(mDismissPin);
					parent2 = (ViewGroup) getParent().getParent();
					parent2.addView(mStarryPINView, 0);

					break;
				default:
					Log.v(TAG, "ERROR");

				}
			} else if (vid == R.id.camera) {
				openMiniCamera();
			} else if (vid == R.id.calculator) {
				openMiniCalculator();
			} else if (vid == R.id.flashlight) {
				Log.v(TAG, "---------- FlashLight---------------");
				if (mIsFlashLightOpen) {
					mFlashLight.setBackground(getResources().getDrawable(R.drawable.starry_flashlight_normal));
					closeLight();
				} else {
					mFlashLight.setBackground(getResources().getDrawable(R.drawable.starry_flashlight_pressed));
					openLight();
				}
			}
		}
	};

	private void openMiniDialpad() {
		Intent intent = null;
		intent = new Intent(ACTION_DIAL_STARRYWINDOW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			mContext.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.d(TAG, "dialwin not found");
		}
	}

	private void openMiniCamera() {

		boolean isLock = false;
		if (mStarrySecurityModel.getSecurityModel() == SecurityMode.Pattern || mStarrySecurityModel.getSecurityModel() == SecurityMode.PIN) {

			isLock = true;
		}
		boolean isSecurity = (!HolsterFixableView.mStarryDismissed);
		isSecurity = (isLock && isSecurity);
		Intent mIntent = new Intent();
		mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mIntent.setAction(isSecurity ? "android.media.action.STILL_IMAGE_CAMERA_SECURE" : "com.android.camera.action.CAMERA_FAST_CAPTURE");
		mIntent.putExtra("request", "SkyWindow");
		mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
		try {
			mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
		} catch (ActivityNotFoundException e) {
			Log.d(TAG, "camera not found");
		}

	}

	private void openMiniCalculator() {
		Intent intent = new Intent("android.com.calculator2.hallmode");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("state", 1);
		try {
			mContext.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.d(TAG, "calculator not found");
		}

	}

	private void openLight() {
		Intent i = new Intent("com.android.intent.action.Open_FlashLight");
		mContext.sendBroadcast(i);
	}

	private void closeLight() {
		Intent i = new Intent("com.android.intent.action.Close_FlashLight");
		mContext.sendBroadcast(i);
	}

	public void updateBg() {
		init();
		postInvalidate();
	}
}
