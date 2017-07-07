package com.android.systemui.statusbar.phone;

import android.content.Context;

import com.android.systemui.helper.LogHelper;

public class DataControlerContain {
	private static final String TAG = "DataControler"; 
	private static DataControler sInstance;
	private static DataControler sInstanceSecure;
	public static DataControler getInstance(Context context, Boolean bPrivateSpace){
		if(sInstanceSecure == null){
			sInstanceSecure = new DataControler(context, true);
		}
		if(sInstance == null){
			sInstance = new DataControler(context, false);
		}
		if(bPrivateSpace){
			return sInstanceSecure;
		}else{
			return sInstance;
		}
		
	}
}
