/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSPanel.Callback;
import com.android.systemui.qs.QuickQSPanel;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;

public class QuickStatusBarHeader extends BaseStatusBarHeader implements
        NextAlarmChangeCallback, OnClickListener, OnUserInfoChangedListener {

    private static final String TAG = "QuickStatusBarHeader";

    private static final float EXPAND_INDICATOR_THRESHOLD = .93f;

    private ActivityStarter mActivityStarter;
    private NextAlarmController mNextAlarmController;
    private View mSystemTimeLayout;
    private View mDateGroup;
    private View mClock;
    private TextView mTime;
    private TextView mAmPm;
    private TextView mDateCollapsed;
//    private TextView mDateExpanded;
    //private SettingsButton mSettingsButton;
    //protected View mSettingsContainer;
    private View mSettingsButton;
    private View mNotificationDelete;
    private View mNotificationSettings;
    private View mQuickSettings;

    private TextView mAlarmStatus;
//    private View mAlarmStatusCollapsed;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mAlarmShowing;

    //private ViewGroup mDateTimeGroup;
    private ViewGroup mDateTimeAlarmGroup;
    private TextView mEmergencyOnly;

    protected ExpandableIndicator mExpandIndicator;

    private boolean mListening;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private QuickQSPanel mHeaderQsPanel;
    private boolean mShowEmergencyCallsOnly;
    protected MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;

    private float mDateTimeTranslation;
    private float mDateTimeAlarmTranslation;
    private int mCollapsedHeight;
    private int mExpandedHeight;

    private int mMultiUserExpandedMargin;
    private int mMultiUserCollapsedMargin;

    private int mClockMarginBottomExpanded;
    private int mClockMarginBottomCollapsed;
    private int mMultiUserSwitchWidthCollapsed;
    private int mMultiUserSwitchWidthExpanded;

    private int mClockCollapsedSize;
    private int mClockExpandedSize;
    private int mClockTimeinflateSize;
    private int mClockTimeCollapsedSize;
    private float mDateScaleFactor;
    protected float mGearTranslation;

//    private TouchAnimator mSecondHalfAnimator;
//    private TouchAnimator mFirstHalfAnimator;
    //private TouchAnimator mDateSizeAnimator;
//    private TouchAnimator mAlarmTranslation;
//    protected TouchAnimator mSettingsAlpha;
    private float mExpansionAmount;
    private QSTileHost mHost;
    private boolean mShowFullAlarm;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mEmergencyOnly = (TextView) findViewById(R.id.header_emergency_calls_only);

        mDateTimeAlarmGroup = (ViewGroup) findViewById(R.id.date_time_alarm_group);
        mDateTimeAlarmGroup.setVisibility(View.GONE);
//        mDateTimeAlarmGroup.findViewById(R.id.empty_time_view).setVisibility(View.GONE);
//        mDateTimeGroup = (ViewGroup) findViewById(R.id.date_time_group);
//        mDateTimeGroup.setVisibility(View.GONE);
//        mDateTimeGroup.setPivotX(0);
//        mDateTimeGroup.setPivotY(0);
        mSystemTimeLayout = findViewById(R.id.one_switchnet_button_layout0);
        mDateGroup = findViewById(R.id.date_group);
        mClock = findViewById(R.id.clock);
        mTime = (TextView) findViewById(R.id.time_view);
        mAmPm = (TextView) findViewById(R.id.am_pm_view);
        mShowFullAlarm = getResources().getBoolean(R.bool.quick_settings_show_full_alarm);

        mExpandIndicator = (ExpandableIndicator) findViewById(R.id.expand_indicator);

        mHeaderQsPanel = (QuickQSPanel) findViewById(R.id.quick_qs_panel);

//        mSettingsButton = (SettingsButton) findViewById(R.id.settings_button);
//        mSettingsContainer = findViewById(R.id.settings_button_container);
//        mSettingsButton.setOnClickListener(this);
        mDateCollapsed = (TextView) findViewById(R.id.date_collapsed);
//        mDateExpanded = (TextView) findViewById(R.id.date_expanded);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mNotificationDelete = findViewById(R.id.ic_notification_delete);
        mNotificationSettings = findViewById(R.id.ic_notification_settings);
        mNotificationSettings.setOnClickListener(this);
        
        mQuickSettings = findViewById(R.id.ic_quick_settings);
        if(Utilities.showDragDownQuickSettings()){
        	mNotificationSettings.setVisibility(View.GONE);
        	mQuickSettings.setVisibility(View.VISIBLE);
        }else{
        	mNotificationSettings.setVisibility(View.VISIBLE);
        	mQuickSettings.setVisibility(View.GONE);
        }

//        mAlarmStatusCollapsed = findViewById(R.id.alarm_status_collapsed);
        mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        mAlarmStatus.setOnClickListener(this);

        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) mMultiUserSwitch.findViewById(R.id.multi_user_avatar);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view
//        ((RippleDrawable) mSettingsButton.getBackground()).setForceSoftware(true);
        ((RippleDrawable) mExpandIndicator.getBackground()).setForceSoftware(true);

       if(getResources().getConfiguration().locale.getCountry().equals("MM")||getResources().getConfiguration().locale.getCountry().equals("ZG")){
        	if(getResources().getConfiguration().locale.getCountry().equals("MM")){
        		mClockTimeinflateSize = Utilities.pixelToDip(mContext, getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size_mm));
        	}else{
        		LinearLayout.LayoutParams clockParams = (android.widget.LinearLayout.LayoutParams) mClock.getLayoutParams();
            	clockParams.height=dip2px(mContext, 53);
            	mClock.setLayoutParams(clockParams);
        		RelativeLayout.LayoutParams params = (LayoutParams) mSystemTimeLayout.getLayoutParams();
            	params.setMargins(0, dip2px(mContext, -13), 0, 0);
            	mSystemTimeLayout.setLayoutParams(params);
        		mClockTimeinflateSize = Utilities.pixelToDip(mContext, getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size_zg));
        	}
        	mTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mClockTimeinflateSize);
        	mTime.setIncludeFontPadding(true);
        	mAmPm.setIncludeFontPadding(true);
		}
        updateResources();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
        applyLayoutValues();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mEmergencyOnly, R.dimen.qs_emergency_calls_only_text_size);

        mGearTranslation = mContext.getResources().getDimension(R.dimen.qs_header_gear_translation);

        mDateTimeTranslation = mContext.getResources().getDimension(
                R.dimen.qs_date_anim_translation);
        mDateTimeAlarmTranslation = mContext.getResources().getDimension(
                R.dimen.qs_date_alarm_anim_translation);
        float dateCollapsedSize = mContext.getResources().getDimension(
                R.dimen.qs_date_collapsed_text_size);
        float dateExpandedSize = mContext.getResources().getDimension(
                R.dimen.qs_date_text_size);
        mDateScaleFactor = dateExpandedSize / dateCollapsedSize;
        updateDateTimePosition();

//        mSecondHalfAnimator = new TouchAnimator.Builder()
//                .addFloat(mShowFullAlarm ? mAlarmStatus : findViewById(R.id.date), "alpha", 0, 1)
//                .addFloat(mEmergencyOnly, "alpha", 0, 1)
//                .setStartDelay(.5f)
//                .build();
//        if (mShowFullAlarm) {
//            mFirstHalfAnimator = new TouchAnimator.Builder()
//                    .addFloat(mAlarmStatusCollapsed, "alpha", 1, 0)
//                    .setEndDelay(.5f)
//                    .build();
//        }
//        mDateSizeAnimator = new TouchAnimator.Builder()
//                .addFloat(mDateTimeGroup, "scaleX", 1, mDateScaleFactor)
//                .addFloat(mDateTimeGroup, "scaleY", 1, mDateScaleFactor)
//                .setStartDelay(.36f)
//                .build();

        updateSettingsAnimator();
    }

    protected void updateSettingsAnimator() {
//        mSettingsAlpha = new TouchAnimator.Builder()
//                .addFloat(mSettingsContainer, "translationY", -mGearTranslation, 0)
//                .addFloat(mMultiUserSwitch, "translationY", -mGearTranslation, 0)
//                .addFloat(mSettingsButton, "rotation", -90, 0)
//                .addFloat(mSettingsContainer, "alpha", 0, 1)
//                .addFloat(mMultiUserSwitch, "alpha", 0, 1)
//                .setStartDelay(QSAnimator.EXPANDED_TILE_DELAY)
//                .build();

//        final boolean isRtl = isLayoutRtl();
//        if (isRtl && mDateTimeGroup.getWidth() == 0) {
//            mDateTimeGroup.addOnLayoutChangeListener(new OnLayoutChangeListener() {
//                @Override
//                public void onLayoutChange(View v, int left, int top, int right, int bottom,
//                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                    mDateTimeGroup.setPivotX(getWidth());
//                    mDateTimeGroup.removeOnLayoutChangeListener(this);
//                }
//            });
//        } else {
//            mDateTimeGroup.setPivotX(isRtl ? mDateTimeGroup.getWidth() : 0);
//        }
    }

    private void updateRippleSize(RippleDrawable ripple, int centerX, int centerY, int width, int height) {
        final int cx = centerX;
        final int cy = centerY;
        int rad = width;//3*width/4;
        ripple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(mSettingsButton != null){
        	Drawable dr1 = mSettingsButton.getBackground();
            if(dr1 instanceof RippleDrawable){
            	int w = Utilities.dipToPixel(mContext, 24);//mSettingsButton.getWidth();
            	int h = w;
            	int cy = Utilities.dipToPixel(mContext, 38) + w/2;
            	int cx = mSettingsButton.getLeft() + w/2 + Utilities.dipToPixel(mContext, 16);
            	RippleDrawable rd1 = (RippleDrawable)dr1;
            	updateRippleSize(rd1, cx, cy, w, h);
            }
        }
        if(mNotificationDelete != null){
        	Drawable dr2 = mNotificationDelete.getBackground();
            if(dr2 instanceof RippleDrawable){
            	int w = Utilities.dipToPixel(mContext, 24);//mNotificationDelete.getWidth();
            	int h = w;
            	int cy = Utilities.dipToPixel(mContext, 38) + w/2;
            	int cx = mNotificationDelete.getLeft() + w/2 + Utilities.dipToPixel(mContext, 16);
            	RippleDrawable rd1 = (RippleDrawable)dr2;
            	updateRippleSize(rd1, cx, cy, w, h);
            }
        }
        if(mNotificationSettings != null){
        	Drawable dr3 = mNotificationSettings.getBackground();
            if(dr3 instanceof RippleDrawable){
            	int w = Utilities.dipToPixel(mContext, 24);//mNotificationSettings.getWidth();
            	int h = w;
            	int cy = Utilities.dipToPixel(mContext, 38) + w/2;
            	int cx = mNotificationSettings.getLeft() + w/2 + Utilities.dipToPixel(mContext, 16);
            	RippleDrawable rd1 = (RippleDrawable)dr3;
            	updateRippleSize(rd1, cx, cy, w, h);
            }
        }
        
        if(mQuickSettings != null){
        	Drawable dr3 = mQuickSettings.getBackground();
            if(dr3 instanceof RippleDrawable){
            	int w = Utilities.dipToPixel(mContext, 24);//mQuickSettings.getWidth();
            	int h = w;
            	int cy = Utilities.dipToPixel(mContext, 38) + w/2;
            	int cx = mQuickSettings.getLeft() + w/2 + Utilities.dipToPixel(mContext, 16);
            	RippleDrawable rd1 = (RippleDrawable)dr3;
            	updateRippleSize(rd1, cx, cy, w, h);
            }
        }
    }
    
    @Override
    public int getCollapsedHeight() {
        return getHeight();
    }

    @Override
    public int getExpandedHeight() {
        return getHeight();
    }

    @Override
    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
        mHeaderQsPanel.setExpanded(expanded);
        updateEverything();
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            String alarmString = KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm);
            mAlarmStatus.setText(alarmString);
            mAlarmStatus.setContentDescription(mContext.getString(
                    R.string.accessibility_quick_settings_alarm, alarmString));
//            mAlarmStatusCollapsed.setContentDescription(mContext.getString(
//                    R.string.accessibility_quick_settings_alarm, alarmString));
        }
        if (mAlarmShowing != (nextAlarm != null)) {
            mAlarmShowing = nextAlarm != null;
            updateEverything();
        }
    }

    @Override
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
//        mSecondHalfAnimator.setPosition(headerExpansionFraction);
//        if (mShowFullAlarm) {
//            mFirstHalfAnimator.setPosition(headerExpansionFraction);
//        }
//        mDateSizeAnimator.setPosition(headerExpansionFraction);
//        mAlarmTranslation.setPosition(headerExpansionFraction);
//        mSettingsAlpha.setPosition(headerExpansionFraction);

        updateAlarmVisibilities();

        mExpandIndicator.setExpanded(headerExpansionFraction > EXPAND_INDICATOR_THRESHOLD);
    }

    @Override
    protected void onDetachedFromWindow() {
        setListening(false);
        mHost.getUserInfoController().remListener(this);
        mHost.getNetworkController().removeEmergencyListener(this);
        super.onDetachedFromWindow();
    }

    private void updateAlarmVisibilities() {
        mAlarmStatus.setVisibility(mAlarmShowing && mShowFullAlarm ? View.VISIBLE : View.INVISIBLE);
//        mAlarmStatusCollapsed.setVisibility(mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateDateTimePosition() {
        // This one has its own because we have to rebuild it every time the alarm state changes.
//        mAlarmTranslation = new TouchAnimator.Builder()
//                .addFloat(mDateTimeAlarmGroup, "translationY", 0, mAlarmShowing
//                        ? mDateTimeAlarmTranslation : mDateTimeTranslation)
//                .build();
//        mAlarmTranslation.setPosition(mExpansionAmount);
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mHeaderQsPanel.setListening(listening);
        mListening = listening;
        updateListeners();
    }

    @Override
    public void updateEverything() {
    	applyLayoutValues();
        updateDateTimePosition();
        updateVisibilities();
        setClickable(false);
    }

    protected void updateVisibilities() {
        updateAlarmVisibilities();
        mEmergencyOnly.setVisibility(mExpanded && mShowEmergencyCallsOnly
                ? View.VISIBLE : View.INVISIBLE);
//        mSettingsContainer.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
//        mSettingsContainer.findViewById(R.id.tuner_icon).setVisibility(
//                TunerService.isTunerEnabled(mContext) ? View.VISIBLE : View.INVISIBLE);
//        mMultiUserSwitch.setVisibility(mExpanded && mMultiUserSwitch.hasMultipleUsers()
//                ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateListeners() {
        if (mListening) {
            mNextAlarmController.addStateChangedCallback(this);
        } else {
            mNextAlarmController.removeStateChangedCallback(this);
        }
    }

    @Override
    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
    }

    @Override
    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
        if (mQsPanel != null) {
            mMultiUserSwitch.setQsPanel(qsPanel);
        }
    }

    public void setupHost(final QSTileHost host) {
        mHost = host;
        host.setHeaderView(mExpandIndicator);
        mHeaderQsPanel.setQSPanelAndHeader(mQsPanel, this);
        mHeaderQsPanel.setHost(host, null /* No customization in header */);
        setUserInfoController(host.getUserInfoController());
        setBatteryController(host.getBatteryController());
        setNextAlarmController(host.getNextAlarmController());

        final boolean isAPhone = mHost.getNetworkController().hasVoiceCallingFeature();
        if (isAPhone) {
            mHost.getNetworkController().addEmergencyListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSettingsButton) {
//            MetricsLogger.action(mContext,
//                    MetricsProto.MetricsEvent.ACTION_QS_EXPANDED_SETTINGS_LAUNCH);
//            if (mSettingsButton.isTunerClick()) {
//                if (TunerService.isTunerEnabled(mContext)) {
//                    TunerService.showResetRequest(mContext, new Runnable() {
//                        @Override
//                        public void run() {
//                            // Relaunch settings so that the tuner disappears.
//                            startSettingsActivity();
//                        }
//                    });
//                } else {
//                    Toast.makeText(getContext(), R.string.tuner_toast, Toast.LENGTH_LONG).show();
//                    TunerService.setTunerEnabled(mContext, true);
//                }
//            }
            startSettingsActivity();
        } else if (v == mAlarmStatus && mNextAlarm != null) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            if (showIntent != null && showIntent.isActivity()) {
                mActivityStarter.startActivity(showIntent.getIntent(), true /* dismissShade */);
            }
        }
        else if (v == mNotificationDelete) {
        	if(QuickSettingsModel.spuerState){
        	}else{
        	}
        }
        else if (v == mNotificationSettings) {
        	if(QuickSettingsModel.spuerState){
        	}else{
    			//intent.setClass(mContext,com.android.systemui.statusbar.preferences.PackageListActivity.class);	
    	    	Intent intent = new Intent();
    	    	intent.setClass(mContext,com.android.systemui.statusbar.preferences.PackageListActivity.class);	
//    	    	ComponentName comp = new ComponentName("com.android.settings", 
//    	    	"com.android.settings.Settings$NotificationAppListActivity");
//    	    	intent.setComponent(comp);
    	    	intent.setAction("android.intent.action.VIEW");
    	    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    	    	mActivityStarter.startActivity(intent,
    	                true /* dismissShade */);
            }
        }
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS),
                true /* dismissShade */);
    }

    @Override
    public void setNextAlarmController(NextAlarmController nextAlarmController) {
        mNextAlarmController = nextAlarmController;
    }

    @Override
    public void setBatteryController(BatteryController batteryController) {
        // Don't care
    }

    @Override
    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(this);
    }

    @Override
    public void setCallback(Callback qsPanelCallback) {
        mHeaderQsPanel.setCallback(qsPanelCallback);
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
            }
        }
    }

    private void applyLayoutValues() {
    	mClock.setY(Utilities.dipToPixel(mContext,3));
    	Log.v("cunrrent-country:", getResources().getConfiguration().locale.getCountry());
    	if(getResources().getConfiguration().locale.getCountry().equals("MM")||getResources().getConfiguration().locale.getCountry().equals("ZG")){
        	if(getResources().getConfiguration().locale.getCountry().equals("MM")){
        		mClockTimeinflateSize = Utilities.pixelToDip(mContext, getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size_mm));
        	}else{
        		LinearLayout.LayoutParams clockParams = (android.widget.LinearLayout.LayoutParams) mClock.getLayoutParams();
            	clockParams.height=dip2px(mContext, 53);
            	clockParams.setMargins(0, 0, 0, 0);
            	mClock.setLayoutParams(clockParams);
        		RelativeLayout.LayoutParams params = (LayoutParams) mSystemTimeLayout.getLayoutParams();
        		params.setMargins(0, dip2px(mContext, -13), 0, dip2px(mContext, 0));
            	mSystemTimeLayout.setLayoutParams(params);
        		mClockTimeinflateSize = Utilities.pixelToDip(mContext, getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size_zg));
        	}
        	mTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mClockTimeinflateSize);
        	mTime.setIncludeFontPadding(true);
        	mAmPm.setIncludeFontPadding(true);
        }else{
        	mClockTimeCollapsedSize = Utilities.pixelToDip(mContext, getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size));
        	mTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mClockTimeCollapsedSize);
        	RelativeLayout.LayoutParams params = (LayoutParams) mSystemTimeLayout.getLayoutParams();
//        	LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) mSystemTimeLayout.getLayoutParams();
        	params.setMargins(0, 0, 0, 0);
        	mSystemTimeLayout.setLayoutParams(params);
        	LinearLayout.LayoutParams mClockParams = (android.widget.LinearLayout.LayoutParams) mClock.getLayoutParams(); 
        	mClockParams.height=dip2px(mContext, 42);
        	mClock.setLayoutParams(mClockParams);
        	mTime.setIncludeFontPadding(false);
        	mAmPm.setIncludeFontPadding(false);
        	
        }
    }
    
    public void onUserInfoChanged(String name, Drawable picture) {
        mMultiUserAvatar.setImageDrawable(picture);
    }
    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }  
}
