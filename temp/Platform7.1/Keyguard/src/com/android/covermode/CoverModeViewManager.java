package com.android.covermode;

import com.android.keyguard.R;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class CoverModeViewManager {
//
//	private String TAG = "CoverModeViewManager";
//	private Context mContext;
//	private WindowManager mWindowManager;
//	private WindowManager.LayoutParams mLayoutParams;
//	private HolsterFixableView mMainView;
//	private Boolean mReady = false;
//	private Boolean mShowing = false;
//	public static Boolean mDismissed = false;
//
//	public CoverModeViewManager(Context context) {
//		mContext = context;
//		getReady();
//	}
//
//	public static CoverModeViewManager mCoverModeViewManager;
//
//	public static CoverModeViewManager getInstance(Context context) {
//		if (mCoverModeViewManager == null) {
//			mCoverModeViewManager = new CoverModeViewManager(context);
//		}
//		return mCoverModeViewManager;
//	}
//
//	public void show() {
//		if (!mReady) {
//			getReady();
//		}
//		if (mShowing) {
//			Log.v(TAG, "show(): CoverMode not showing because it's already showing");
//			return;
//		}
//		if (!shouldShow()) {
//			Log.v(TAG, "show(): CoverMode not showing because it shouldn't show");
//			return;
//		}
//		mWindowManager.addView(mMainView, mLayoutParams);
//		mShowing = true;
//	}
//
//	public void hide() {
//		if (!mReady || !mShowing) {
//			Log.v(TAG, "hide(): CoverMode not hiding because it's already hided or not ready");
//			return;
//		}
//		mWindowManager.removeView(mMainView);
//		mShowing = false;
//	}
//
//	private void getReady() {
//		if (mMainView == null) {
//			mMainView = (HolsterFixableView) View.inflate(mContext, R.layout.starry_main_view, null);
//		}
//		mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
//
//		int flags = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN 
//				| WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
//				| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
//
//		final int stretch = ViewGroup.LayoutParams.MATCH_PARENT;
//		final int type = WindowManager.LayoutParams.TYPE_PHONE;
//
//		mLayoutParams = new WindowManager.LayoutParams(stretch, stretch, type, flags, PixelFormat.OPAQUE);
//		mLayoutParams.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//
//		mReady = true;
//	}
//
//	public Boolean isCoverModeShowing() {
//		return mShowing;
//	}
//
//	private Boolean shouldShow() {
//		return true;
//	}
}
