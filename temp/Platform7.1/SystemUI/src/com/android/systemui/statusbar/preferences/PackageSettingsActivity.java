package com.android.systemui.statusbar.preferences;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.Ranking;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.SystemBarTintManager;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.misc.YLUtils;
import com.android.systemui.recents.misc.YLUtils.SettingsDB;
import com.yulong.android.common.app.CommonActivity;
import com.yulong.android.common.view.TopBar;
import com.yulong.android.common.view.TopBar.TopBarStyle;
import com.yulong.android.common.widget.CommonSwitch;
import com.yulong.android.common.widget.CommonSwitch.OnCheckedChangeListener;
import com.yulong.android.view.tab.TabBar;

public class PackageSettingsActivity extends CommonActivity{
	private String packageName = "com.android.systemui.statusbar.preferences";
	private String appName;
	private final String TAG = "PackageSettingsActivity";
	private boolean mKeyValue_0;
	private boolean mKeyValue_1;
	private boolean mKeyValue_2;
	private boolean mKeyValue_3;
	private boolean mKeyValue_4;
	private boolean mKeyValue_5;
	private String mKeyString_0;
	private String mKeyString_1;
	private String mKeyString_2;
	private String mKeyString_3;
	private String mKeyString_4;
	private String mKeyString_5;
	
	
	private CommonSwitch mCmmonSwitch_0;
	private CommonSwitch mCmmonSwitch_1;
	private CommonSwitch mCmmonSwitch_2;
	private CommonSwitch mCmmonSwitch_3;
	private CommonSwitch mCmmonSwitch_4;
	private CommonSwitch mCmmonSwitch_5;
	private CheckBox mCheckBox_0;
	private CheckBox mCheckBox_1;
	private ImageView mImageView_0;
	private ImageView mImageView_1;
	private ImageView mImageView_2;
	private TextView mTextView;
    private TopBar mTopbar;
	private TabBar mTabbar;
	private int mTopBarHeight;
	private PackageManager mPm;
	private ViewGroup mViewGroup;
	private View mDetailControl;
	private View mDetailPicture;
	protected final NotificationBackend mBackend = new NotificationBackend();
	private com.android.systemui.statusbar.preferences.NotificationBackend.AppRow mAppRow;
	protected int mUid;
	protected PackageInfo mPkgInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mPm = getPackageManager();
		
		

	            
		LogHelper.sd(TAG,"onCreate");
		//setContentView(R.layout.preference_package_settings);
		View body = setBodyLayout(R.layout.preference_package_settings);
		body.setPadding(0, mTopBarHeight, 0, 0);
		
		//setStatusBarGradientColor();
		packageName = getIntent().getStringExtra("package");
		appName = null;//getIntent().getStringExtra("app_name");
		if (appName == null) {
			List<PackageInfo> packageInfosTemp = mPm
					.getInstalledPackages(
							PackageManager.GET_UNINSTALLED_PACKAGES);
			for (PackageInfo packageInfo : packageInfosTemp) {
				if (packageName.equalsIgnoreCase(packageInfo.packageName)) {
					appName = packageInfo.applicationInfo.loadLabel(
							mPm)
							.toString();
					mPkgInfo = packageInfo;
					break;
				}
			}
		}
		
//		mUid = getIntent().getIntExtra("app_uid", 0);//===modify by ty
//		mPkgInfo = findPackageInfo(packageName, mUid);
		 if (mPkgInfo != null) {
			 mUid = mPkgInfo.applicationInfo.uid;
	         mAppRow = mBackend.loadAppRow(getBaseContext(), mPm, mPkgInfo);
		 }
		
		mViewGroup = (ViewGroup)findViewById(R.id.preference_package_setting);
		mCmmonSwitch_0 = (CommonSwitch)findViewById(R.id.sub_switch_widget0);
		mCmmonSwitch_1 = (CommonSwitch)findViewById(R.id.sub_switch_widget1);
		mCmmonSwitch_2 = (CommonSwitch)findViewById(R.id.sub_switch_widget2);
		mCmmonSwitch_3 = (CommonSwitch)findViewById(R.id.sub_switch_widget3);
		mCmmonSwitch_4 = (CommonSwitch)findViewById(R.id.sub_switch_widget4);//statusbar
		mCmmonSwitch_5 = (CommonSwitch)findViewById(R.id.sub_switch_widget5);
		
		mImageView_0 = (ImageView)findViewById(R.id.sub_switch_widget0_image);
		mImageView_1 = (ImageView)findViewById(R.id.sub_switch_widget1_image);
		mImageView_2 = (ImageView)findViewById(R.id.sub_switch_widget2_image);
		
		mTextView = (TextView)findViewById(R.id.sub_switch_name3_show);
		mDetailControl = findViewById(R.id.sub_switch_name3_control);
		mDetailPicture = findViewById(R.id.sub_switch_widget1_picture);
		
		mCheckBox_0 = (CheckBox)findViewById(R.id.sub_switch_widget1_btn);//hide
		mCheckBox_1 = (CheckBox)findViewById(R.id.sub_switch_widget2_btn);
		mCheckBox_0.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){//hide
			@Override
			public void onCheckedChanged(CompoundButton toggle, boolean checked) {
				final Boolean b = (Boolean)checked;
				mCheckBox_1.setChecked(!b);
				if(b){
					mCheckBox_0.setEnabled(false);
					mCheckBox_1.setEnabled(true);
				}else{
					mCheckBox_0.setEnabled(true);
					mCheckBox_1.setEnabled(false);
				}
			}
		});
		mCheckBox_1.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton toggle, boolean checked) {
				final Boolean b = (Boolean)checked;
				mCheckBox_0.setChecked(!b);
				if(!b){
					mCheckBox_0.setEnabled(false);
					mCheckBox_1.setEnabled(true);
				}else{
					mCheckBox_0.setEnabled(true);
					mCheckBox_1.setEnabled(false);
				}
					setCheckBoxValue3(b);
					Boolean n = (Boolean) b;
					final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
					LogHelper.sd(TAG, "1000012 str="+mKeyString_3+" b="+n);
					se.putBoolean(mKeyString_3, n);
					se.commit();
					Intent intent = new Intent(
							"com.yulong.android.ntfmanager.RefreshStatusBar");
					//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
					sendBroadcastAsUser(intent, UserHandle.ALL);
					mTextView.setText(b?getResources().getString(R.string.preference_detail_show):
						getResources().getString(R.string.preference_detail_hide));
			}
		});
		createPreferenceHierarchy();
	}
	public void setChildrenViewVisible(Boolean visible) {
		if(mViewGroup != null){
			int count = mViewGroup.getChildCount();
	        for (int i = 0; i < count; i++) {
	            View view = mViewGroup.getChildAt(i);
	            if (i < 2) {
	            } else {
	            	view.setVisibility(visible?View.VISIBLE:View.GONE);
	            }
	        }
		}
	}
	
	@Override
	protected void onResume() {
	    super.onResume();

//		setActionBarTitle(appName);
//		setActionBarBackButtonVisibility(true);
	    mTopbar.setTopBarTitle(appName);
	    mTopbar.setTopBarTitleSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
	}
	
  @Override
    protected void onCreateTopBar(TopBar topBar) {
        super.onCreateTopBar(topBar);
        topBar.setTopBarStyle(TopBarStyle.TOP_BAR_NOTMAL_STYLE);
        mTopbar = topBar;
        mTabbar = topBar.getTabBar();
        mTopBarHeight = topBar.getHeight();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Intent intent = new Intent(
				"com.yulong.android.ntfmanager.RefreshStatusBar");
		//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
		sendBroadcastAsUser(intent, UserHandle.ALL);
		
	}
	
	public void setImage(Boolean a, Boolean b){
//		Drawable d = null;
//		if(a && b){
//			d = getResources().getDrawable(R.drawable.preference_detail_on_lockscreen_on);
//		}else if(a && !b){
//			d = getResources().getDrawable(R.drawable.preference_detail_on_lockscreen_off);
//		}else if(!a && b){
//			d = getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_on);
//		}else{
//			d = getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_off);
//		}
//		if(d != null){
//			mImageView_1.setImageDrawable(d);
//		}
//		mImageView_1.setImageDrawable(mKeyValue_2?
//				:
//					getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_off));
	}

	public void setCheckBoxValue3(Boolean newValue){
		Boolean n = (Boolean) newValue;
		final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
		
		se.putBoolean(mKeyString_3, n);
		se.commit();

		Intent intent = new Intent(
				"com.yulong.android.ntfmanager.RefreshStatusBar");
		//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
		sendBroadcastAsUser(intent, UserHandle.ALL);
	}
	
	public void createPreferenceHierarchy(){
		setPreferenceValue_0();
		setPreferenceValue_1();
		setPreferenceValue_2();
		setPreferenceValue_3();
		setPreferenceValue_4();
		setPreferenceValue_5();
	}
	
	public void setPreferenceValue_0(){
		
		LogHelper.sd(TAG,"setPreferenceValue_0");
		mKeyString_0 = NotificationCollapseManage
				.getNotificationKey(packageName);
		mKeyValue_0 = NotificationCollapseManage.getDefault(
				this).getNotificationKeyValue(packageName);
		
		final int importance = 
				mKeyValue_0 ? Ranking.IMPORTANCE_UNSPECIFIED :Ranking.IMPORTANCE_NONE;
        mBackend.setImportance(mPkgInfo.packageName, mUid, importance);
        
		LogHelper.sd(TAG,"setPreferenceValue_0 "+mKeyString_0+mKeyValue_0);
		if(mCmmonSwitch_0 != null){
			mCmmonSwitch_0.setChecked(mKeyValue_0);
			setChildrenViewVisible(mKeyValue_0);
			mCmmonSwitch_0.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CommonSwitch arg0, boolean newValue) {
					// TODO Auto-generated method stub
					Boolean n = (Boolean) newValue;
					final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
					
					se.putBoolean(mKeyString_0, n);
					se.commit();
					
                    final int importance = //IMPORTANCE_UNSPECIFIED=enable
                    		newValue ? Ranking.IMPORTANCE_UNSPECIFIED :Ranking.IMPORTANCE_NONE;
                    mBackend.setImportance(mPkgInfo.packageName, mUid, importance);
                    
					YLUtils.putIntForAllUser(getApplicationContext(), SettingsDB.SECURE, mKeyString_0, n?1:0);
					mCmmonSwitch_1.setEnabled(n);
					mCmmonSwitch_2.setEnabled(n);
					mCmmonSwitch_3.setEnabled(n);
					mCmmonSwitch_4.setEnabled(n);
					mCmmonSwitch_5.setEnabled(n);

					setChildrenViewVisible(n);
					Intent intent = new Intent(
							"com.yulong.android.ntfmanager.RefreshStatusBar");
					//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
					sendBroadcastAsUser(intent, UserHandle.ALL);
				}
			});
		}
		
	}
	public void setPreferenceValue_1(){
		
		mKeyString_1 = NotificationCollapseManage
				.getFloatingNotificationKey(packageName);
		mKeyValue_1 = NotificationCollapseManage.getDefault(this)
				.getFloatingNotificationKeyValue(packageName);
		mCmmonSwitch_1.setEnabled(mKeyValue_0);
		mCmmonSwitch_1.setChecked(mKeyValue_1);
		
		if(mKeyValue_0){
			final int importance = //Ranking.IMPORTANCE_LOW;
					mKeyValue_1 ? Ranking.IMPORTANCE_UNSPECIFIED : Ranking.IMPORTANCE_LOW;
	        mBackend.setImportance(mPkgInfo.packageName, mUid, importance);
		}
		
		//boolean blocked = importance == Ranking.IMPORTANCE_NONE || banned;
		
		mImageView_0.setImageDrawable(mKeyValue_1?
				getResources().getDrawable(R.drawable.preference_float_notification_on):
					getResources().getDrawable(R.drawable.preference_float_notification_off));
		
		mCmmonSwitch_1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CommonSwitch arg0, boolean newValue) {
				// TODO Auto-generated method stub
				Boolean n = (Boolean) newValue;
				final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
				
				se.putBoolean(mKeyString_1, n);
				se.commit();
				
                final int importance =
                		newValue ? Ranking.IMPORTANCE_UNSPECIFIED : Ranking.IMPORTANCE_LOW;
                mBackend.setImportance(mPkgInfo.packageName, mUid, importance);

				mImageView_0.setImageDrawable(n?
						getResources().getDrawable(R.drawable.preference_float_notification_on):
							getResources().getDrawable(R.drawable.preference_float_notification_off));
				
				Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
				sendBroadcastAsUser(intent, UserHandle.ALL);
			}
		});
	}
	
    private int getGlobalVisibility() {
        int globalVis = Ranking.VISIBILITY_NO_OVERRIDE;
        if (!getLockscreenNotificationsEnabled()) {
            globalVis = Notification.VISIBILITY_SECRET;
        } else if (!getLockscreenAllowPrivateNotifications()) {
            globalVis = Notification.VISIBILITY_PRIVATE;
        }
        return globalVis;
    }

    protected boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    protected boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }
    
	public void setPreferenceValue_2(){
		
		mKeyString_2 = NotificationCollapseManage
				.getLocksreenNotificationKey(packageName);
		mKeyValue_2 = NotificationCollapseManage.getDefault(this)
				.getLocksreenNotificationKeyValue(packageName);
		mCmmonSwitch_2.setEnabled(mKeyValue_0);
		mCmmonSwitch_2.setChecked(mKeyValue_2);
		
		mBackend.setVisibilityOverride(mPkgInfo.packageName, mUid, mKeyValue_2?
				Ranking.VISIBILITY_NO_OVERRIDE:Notification.VISIBILITY_SECRET);
		mDetailControl.setVisibility(mKeyValue_2 ? View.VISIBLE : View.GONE);
		mDetailPicture.setVisibility(mKeyValue_2 ? View.VISIBLE : View.GONE);
//		Settings.Secure.putIntForUser(getContentResolver(),
//                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0, mUid);
//        Settings.Secure.putIntForUser(getContentResolver(),
//                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, mKeyValue_2 ? 1 : 0, mUid);

		
		mImageView_1.setImageDrawable(mKeyValue_2?
				getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_on):
					getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_off));
		mImageView_2.setImageDrawable(mKeyValue_2?
				getResources().getDrawable(R.drawable.preference_detail_on_lockscreen_on):
					getResources().getDrawable(R.drawable.preference_detail_on_lockscreen_off));
		mCmmonSwitch_2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CommonSwitch arg0, boolean newValue) {
				// TODO Auto-generated method stub
				Boolean n = (Boolean) newValue;
				final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
				
				se.putBoolean(mKeyString_2, n);
				se.commit();
				
				mBackend.setVisibilityOverride(mPkgInfo.packageName, mUid, n?
						Ranking.VISIBILITY_NO_OVERRIDE:Notification.VISIBILITY_SECRET);
//				Settings.Secure.putIntForUser(getContentResolver(),
//		                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, mKeyValue_2 ? 1 : 0, mUid);

				Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
				sendBroadcastAsUser(intent, UserHandle.ALL);
				mDetailControl.setVisibility(n ? View.VISIBLE : View.GONE);
				mDetailPicture.setVisibility(n ? View.VISIBLE : View.GONE);
				
				mImageView_1.setImageDrawable(n?
						getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_on):
							getResources().getDrawable(R.drawable.preference_detail_off_lockscreen_off));
				mImageView_2.setImageDrawable(n?
						getResources().getDrawable(R.drawable.preference_detail_on_lockscreen_on):
							getResources().getDrawable(R.drawable.preference_detail_on_lockscreen_off));
			}
		});
	}
	public void setPreferenceValue_3(){
		
		mKeyString_3 = NotificationCollapseManage
				.getDetailNotificationKey(packageName);
		mKeyValue_3 = NotificationCollapseManage.getDefault(this)
				.getDetailNotificationKeyValue(packageName);
		mCmmonSwitch_3.setEnabled(mKeyValue_0);
		mCmmonSwitch_3.setChecked(mKeyValue_3);
		
//		if(mKeyValue_2){
//			mBackend.setVisibilityOverride(mPkgInfo.packageName, mUid, mKeyValue_3?
//					Ranking.VISIBILITY_NO_OVERRIDE:Notification.VISIBILITY_PRIVATE);
//		}
		
		
		if(mKeyValue_3){
			mCheckBox_1.setChecked(mKeyValue_3);
			mCheckBox_0.setEnabled(true);
			mCheckBox_1.setEnabled(false);
		}else{
			mCheckBox_0.setChecked(!mKeyValue_3);
			mCheckBox_0.setEnabled(false);
			mCheckBox_1.setEnabled(true);
		}
		mTextView.setText(mKeyValue_3?getResources().getString(R.string.preference_detail_show):
			getResources().getString(R.string.preference_detail_hide));
		
		mCmmonSwitch_3.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CommonSwitch arg0, boolean newValue) {
				// TODO Auto-generated method stub
				Boolean n = (Boolean) newValue;
				final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
				
//				LogHelper.sd(TAG, "1000011 str="+mKeyString_3+" b="+n);
				se.putBoolean(mKeyString_3, n);
				se.commit();
				
//				if(mKeyValue_2){
//					mBackend.setVisibilityOverride(mPkgInfo.packageName, mUid, mKeyValue_3?
//							Ranking.VISIBILITY_NO_OVERRIDE:Notification.VISIBILITY_PRIVATE);
//				}

				Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
				sendBroadcastAsUser(intent, UserHandle.ALL);
			}
		});
	}
	public void setPreferenceValue_4(){
		mKeyString_4 = NotificationCollapseManage
				.getStatusBarKey(packageName);
		mKeyValue_4 = NotificationCollapseManage.getDefault(this)
				.getStatusBarKeyValue(packageName);
		mCmmonSwitch_4.setEnabled(mKeyValue_0);
		mCmmonSwitch_4.setChecked(mKeyValue_4);
		mCmmonSwitch_4.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CommonSwitch arg0, boolean newValue) {
				// TODO Auto-generated method stub
				Boolean n = (Boolean) newValue;
				final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
				
				se.putBoolean(mKeyString_4, n);
				se.commit();

				Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
				sendBroadcastAsUser(intent, UserHandle.ALL);
			}
		});
	}
	
	public void setPreferenceValue_5(){
		mKeyString_5 = NotificationCollapseManage
				.getDndKey(packageName);
		mKeyValue_5 = NotificationCollapseManage.getDefault(this)
				.getDndKeyValue(packageName);
		mCmmonSwitch_5.setEnabled(mKeyValue_0);
		mCmmonSwitch_5.setChecked(mKeyValue_5);
		
		mBackend.setBypassZenMode(mPkgInfo.packageName, mUid, (Boolean)mKeyValue_5);
		
		mCmmonSwitch_5.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CommonSwitch arg0, boolean newValue) {
				// TODO Auto-generated method stub
				Boolean n = (Boolean) newValue;
				final SharedPreferences.Editor se = PreferenceManager.getDefaultSharedPreferences(PackageSettingsActivity.this).edit();
				
				se.putBoolean(mKeyString_5, n);
				se.commit();
				
				mBackend.setBypassZenMode(mPkgInfo.packageName, mUid, n);

				Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				//CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
				sendBroadcastAsUser(intent, UserHandle.ALL);
			}
		});
	}
	public void setStatusBarGradientColor(){
		int mStyle = Utilities.getCoolpadThemeStyle();
    	if (1 == mStyle) {//black gold
    		return;
        }
    	 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
 			setTranslucentStatus(true);//
 		}
         SystemBarTintManager tintManager = new SystemBarTintManager(this);
         tintManager.setStatusBarTintEnabled(true);
         tintManager.setStatusBarTintResource(R.drawable.gradient_bg);
    }
    @TargetApi(19) 
	private void setTranslucentStatus(boolean on) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}
    
    private PackageInfo findPackageInfo(String pkg, int uid) {
        final String[] packages = mPm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    try {
                        return mPm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }
}
