package com.android.systemui.statusbar.preferences;

import android.os.Bundle;

import com.android.systemui.R;

public class NotificationAndControlCenterActivity extends YLPreferenceActivity {
	private String packageName = "com.android.systemui.statusbar.preferences";
	private final String TAG = "NotificationAndControlCenterActivity";
	public final static String LOCKSCREEN_PREFS_NAME = "systemui_lockscreen_dragdown_permission";
	public final static String DRAG_DOWN_KEY="key_lockscreen_dragdown";
	public final static String SHOW_CARRIER_PREFS_NAME = "systemui_show_carrier_permission";
	public final static String SHOW_CARRIER_KEY="key_show_carrier";
    private String appName;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		appName=getResources().getString(R.string.item_center_notifcaiton_title); 
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
		setActionBarTitle(appName);
		setActionBarBackButtonVisibility(true);
		
	}
	
}
