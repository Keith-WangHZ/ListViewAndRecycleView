/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.qs;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.phone.PanelView;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;


//qiku tongyuhui 20150610 add on the lock screen drop-down qspanel start
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.quicksettings.bottom.QuickSettingLauncher;
import com.android.systemui.statusbar.phone.QuickSettingsConfigView;
/**
 * The container with notification stack scroller and quick settings inside.
 */
public class QSPanelContainer extends FrameLayout {
	public static final boolean DEBUG = false;
	public static final String TAG = "QSPanelContainer";
	private String mViewName;
	private int mFullHeight = 0;
	private float mExpandedHeight = 0;
	private int mCellHeight;
	private int mQsPanelDraggerDeight;
	private int mQsPanelDraggerMarginBottomHeight;
	private View mPanelDragger;//dragger on the Qs
	private int mQsContainerHeight;
	private QSPanel mQSPanel;
	//private QuickSettingsConfigView mQuickSettingsConfigView;
	private View mBrightnessView;
	private float mOldHeight = 0;
	public float getExpandedHeight() {
		return mExpandedHeight;
	}

	public void setExpandedHeight(float mExpandedHeight) {
		this.mExpandedHeight = mExpandedHeight;
		//LogHelper.sd(TAG, " QSPanelContainer mExpandedHeight="+mExpandedHeight);
	}

	private final void logf(String fmt, Object... args) {
		Log.v(TAG,
				(mViewName != null ? (mViewName + ": ") : "")
						+ String.format(fmt, args));
	}

	public QSPanelContainer(Context context, AttributeSet attrs) {
		super(context, attrs);
		loadDimens();
	}

	protected float dip2px(int dip) {
		float scale = mContext.getResources().getDisplayMetrics().density;
		return (float) (dip * scale + 0.5f);
	}
	
	protected void loadDimens() {
		final Resources res = getContext().getResources();
		mCellHeight = res.getDimensionPixelSize(R.dimen.qs_tile_height) - (int)dip2px(20);
		mQsPanelDraggerDeight = res.getDimensionPixelSize(R.dimen.qs_panel_dragger_height);
		mQsPanelDraggerMarginBottomHeight = res.getDimensionPixelSize(R.dimen.qs_panel_dragger_margin_bottom_height);
		mQsContainerHeight = res.getDimensionPixelSize(R.dimen.qs_container_height);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mViewName = getResources().getResourceName(getId());
		mPanelDragger = findViewById(R.id.qs_panel_dragger);
		
		mQSPanel = (com.android.systemui.qs.QSPanel)findViewById(R.id.quick_settings_panel);
//		mQuickSettingsConfigView = (QuickSettingsConfigView)mQSPanel.findViewById(R.id.setting_config_container);
		mBrightnessView = mQSPanel.findViewById(R.id.qs_brightness_dialog);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		if (DEBUG)
			logf("onMeasure(%d, %d) -> (%d, %d)", widthMeasureSpec,
					heightMeasureSpec, getMeasuredWidth(), getMeasuredHeight());

		// Did one of our children change size?
//		int newHeight = getMeasuredHeight();
//		if (newHeight != mFullHeight) {
//			mFullHeight = newHeight;
			// If the user isn't actively poking us, let's rubberband to the
			// content
			// if (!mTracking && !mRubberbanding && !mTimeAnimator.isStarted()
			// && mExpandedHeight > 0 && mExpandedHeight != mFullHeight) {
			// mExpandedHeight = mFullHeight;
			// }
//		}
//		 heightMeasureSpec = MeasureSpec.makeMeasureSpec((int)
//		 mExpandedHeight,
//		 MeasureSpec.AT_MOST); // MeasureSpec.getMode(heightMeasureSpec));
//		 setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
//		setMeasuredDimension(width, mCellHeight);
		int mQH = 0;//mQuickSettingsConfigView.getHeight() - (int)dip2px(15);
		int mBvH = 0;//mBrightnessView.getHeight();
		//LogHelper.sd(TAG,"mQH="+mQH+" mBvH="+mBvH);
		mQsContainerHeight = Math.max(mQH+mBvH, mQsContainerHeight);
		
		if(!Utilities.showDragDownQuickSettings()){
			setMeasuredDimension(width,0);
			mPanelDragger.setY(0);
		}else{
		boolean keyguardShowing = QuickSettingLauncher.getInstance(mContext).getStatusBar().isKeyguard();
			//LogHelper.sd(TAG,"mExpandedHeight="+mExpandedHeight);
		if (keyguardShowing) {
			setMeasuredDimension(width, mQsContainerHeight
					+ mQsPanelDraggerDeight - mQsPanelDraggerMarginBottomHeight);
			mPanelDragger.setY(mQsContainerHeight
					- mQsPanelDraggerMarginBottomHeight);
			mPanelDragger.setVisibility(View.GONE);
		} else {
			mPanelDragger.setVisibility(View.VISIBLE);
			if (mExpandedHeight <= mCellHeight + mQsPanelDraggerDeight
					- mQsPanelDraggerMarginBottomHeight) {
				setMeasuredDimension(width, mCellHeight + mQsPanelDraggerDeight
						- mQsPanelDraggerMarginBottomHeight);
				mPanelDragger.setY(mCellHeight
						- mQsPanelDraggerMarginBottomHeight);
			} else {
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(
						(int) mExpandedHeight, MeasureSpec.AT_MOST); // MeasureSpec.getMode(heightMeasureSpec));
				setMeasuredDimension(width, heightMeasureSpec);
				mPanelDragger.setY(mExpandedHeight - mQsPanelDraggerDeight);

			}
		}
		}
		//LogHelper.sd(TAG, "QSPanalContainer --> onMeasure Alpha="+getAlpha() + "  >>>>>>>>>>>>");
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}

	public void updateQSPanelContainerLayout() {
		if(mExpandedHeight != mOldHeight){
			mOldHeight = mExpandedHeight;
		requestLayout();
		}
	}

}
