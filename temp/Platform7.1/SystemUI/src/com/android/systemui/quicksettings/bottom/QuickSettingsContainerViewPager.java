package com.android.systemui.quicksettings.bottom;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class QuickSettingsContainerViewPager extends ViewPager {

	public QuickSettingsContainerViewPager(Context context) {
		this(context, null);
	}

	public QuickSettingsContainerViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	float preX = 0;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		boolean res = super.onInterceptTouchEvent(event);
		/*if (event.getAction() == MotionEvent.ACTION_DOWN) {
			preX = event.getX();
		} else {
			if (Math.abs(event.getX() - preX) > 8) {
				return true;
			} else {
				preX = event.getX();
			}
		}*/
		return res;
	}

}