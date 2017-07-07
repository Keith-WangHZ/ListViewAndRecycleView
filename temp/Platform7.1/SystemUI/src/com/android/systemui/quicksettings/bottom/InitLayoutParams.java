package com.android.systemui.quicksettings.bottom;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.android.systemui.R;

public class InitLayoutParams {
	
	public static  LayoutParams InitLayoutParams(Context context){
    	LayoutParams mLayoutParams;
		mLayoutParams = new WindowManager.LayoutParams();
		mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		mLayoutParams.format = PixelFormat.RGBA_8888;
		mLayoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;

		mLayoutParams.width = LayoutParams.FILL_PARENT;
		
		mLayoutParams.height =context.getResources().getDimensionPixelSize(
				R.dimen.new_systemui_height);;
		return mLayoutParams;
    }
}
