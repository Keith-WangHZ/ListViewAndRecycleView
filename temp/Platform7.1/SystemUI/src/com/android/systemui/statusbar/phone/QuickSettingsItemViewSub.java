package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;


public final class QuickSettingsItemViewSub extends LinearLayout{
	private static final String TAG = "QuickSettingsItemViewSub";
	private int mId = -1;
	private QuickSettingsItemView mView;
	public QuickSettingsItemViewSub(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public QuickSettingsItemViewSub(Context context){
		this(context, null);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();//
	}
	
	public void setQuickSettingsItemView(QuickSettingsItemView v){
		mView = v;
	}

	public int getQuickSettingId() {
		if(mView!=null){
			return mView.getQuickSettingId();
		}
		return mId;
	}
}
