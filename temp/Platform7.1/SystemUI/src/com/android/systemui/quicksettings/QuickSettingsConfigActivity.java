package com.android.systemui.quicksettings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsConfigView;
import com.android.systemui.statusbar.phone.QuickSettingsItemView;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
import com.yulong.android.common.app.CommonActivity;
import com.yulong.android.common.view.TopBar;
import com.yulong.android.common.view.TopBar.TopBarStyle;

public class QuickSettingsConfigActivity  extends CommonActivity{
	private static final String TAG = QuickSettingsConfigActivity.class.getSimpleName();
	private QuickSettingsItemView editQuickSettings;
	private QuickSettingsConfigView configView;
	private State editState;
	private int mTopBarHeight;
	   @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			View bodyView = setBodyLayout(R.layout.quick_settings_config_layout);
			bodyView.setPadding(0, mTopBarHeight, 0, 0);
			bodyView.setSystemUiVisibility(View.STATUS_BAR_TRANSLUCENT);
			bodyView.setSystemUiVisibility(View.NAVIGATION_BAR_TRANSLUCENT);
			bodyView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//	        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this); 
//	        final Drawable wallpaperDrawable = wallpaperManager.getDrawable(); 
//	        wallpaperDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
//	        bodyView.setBackground(wallpaperDrawable);
			configView = (QuickSettingsConfigView)findViewById(R.id.setting_config_container);
			editQuickSettings = (QuickSettingsItemView)findViewById(R.id.quick_settings_edit);
			editQuickSettings.mIsConfigItem = true;
			editState = new State(-1);
			editState.status = State.STATUS_DISABLE;
			editState.textId = R.string.quicksettings_edit_disable;
			editState.iconId = R.drawable.quicksettings_edit_disable;
			//editQuickSettings.updateState(editState);
			editQuickSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(editState.status == State.STATUS_DISABLE){
						editState.status = State.STATUS_ENABLE;
						editState.textId = R.string.quicksettings_edit_enable;
						editState.iconId = R.drawable.quicksettings_edit_enable;
						editQuickSettings.updateState(editState);							
						configView.setEditMode(true);
					}else{
						editState.status = State.STATUS_DISABLE;
						editState.textId = R.string.quicksettings_edit_disable;
						editState.iconId = R.drawable.quicksettings_edit_disable;
						editQuickSettings.updateState(editState);	
						configView.setEditMode(false);
					}
				}
			});
	    }
	   
		@Override
		protected void onCreateTopBar(TopBar topBar) {
			super.onCreateTopBar(topBar);
			// 设置顶部栏样式
			topBar.setTopBarStyle(TopBarStyle.TOP_BAR_NOTMAL_STYLE);
			// 设置顶部栏标题
			topBar.setTopBarTitle(R.string.accessibility_desc_quick_settings);
			topBar.setTopBarTitleSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
			//topBar.setTopBarSubtitle("SubTitle");

			// 设置顶部栏是否显示返回上一级按钮
			topBar.setDisplayUpView(true);
			mTopBarHeight = topBar.getHeight();
		}
		
}
