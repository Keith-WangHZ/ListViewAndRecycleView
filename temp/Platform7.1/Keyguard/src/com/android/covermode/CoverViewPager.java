package com.android.covermode;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CoverViewPager extends ViewPager {
	public CoverViewPager(Context context) {
		super(context, null);
	}
	
	public CoverViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		boolean isDismiss = false;
		if (HolsterFixableView.mVersion == 1) {
			isDismiss = HolsterFixableView.mStarryDismissed;
		}
		try {
			if (!isDismiss) {
				return false;
			}
			return super.onInterceptTouchEvent(event);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return false;
	}
}
