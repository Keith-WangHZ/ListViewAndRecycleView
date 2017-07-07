package com.android.systemui;

import java.util.Locale;

import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.tuner.TunerService;

import android.R.integer;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * BatteryViewGroup class control the battery charge icon showing or hiding
 */
public class BatteryViewGroup extends LinearLayout implements
	BatteryController.BatteryStateChangeCallback, TunerService.Tunable {
	
    private static final String SHOW_BATTERY_PERCENT_SETTING = "status_bar_show_battery_percent";
	
	private static final String TAG = "BatteryViewGroup";
	
	private ImageView mBatteryFrame;
	private BatteryMeterView mBatteryBody;
	private ImageView mBatteryCharge;
	private TextView mBatteryLevel;
	
	private BatteryController mBatteryController;
	private boolean mPowerSaveEnabled;
	private boolean mIsCharging;
	
	private int mIconColor;
//	private int mKeyguardIconColor;
//	private int mKeyguardTextColor;
    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;
	
    boolean mShowPercent = false;
	private Context mContext;
	private Handler mHandler = new Handler();
	
	private ContentObserver mBatterPercentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
       	 final boolean show = 0 != Settings.System.getInt(
                 mContext.getContentResolver(), SHOW_BATTERY_PERCENT_SETTING, 0);
        	showBatteryLevel(show);
        }
    };
    
    public BatteryViewGroup(Context context) {
        this(context, null, 0);
    }
    
    public BatteryViewGroup(Context context, AttributeSet attrs) {
    	this(context, attrs, 0);
    }

    public BatteryViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        mDarkModeBackgroundColor =
                context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
    mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
    mLightModeBackgroundColor =
            context.getColor(R.color.light_mode_icon_color_dual_tone_background);
    mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);
    }
    
    
    public void registerContentObserver() { 
        mContext.getContentResolver().registerContentObserver(Settings.System
				.getUriFor(SHOW_BATTERY_PERCENT_SETTING), false,
                mBatterPercentObserver);
	}
    
    public void unregisterContentObserver() {
    	mContext.getContentResolver().unregisterContentObserver(mBatterPercentObserver);
    	mHandler = null;
	}
    
    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mBatteryController.addStateChangedCallback(this);
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        registerContentObserver();
        final boolean show = 0 != Settings.System.getInt(
                mContext.getContentResolver(), SHOW_BATTERY_PERCENT_SETTING, 0);
        mBatteryFrame = (ImageView) findViewById(R.id.battery_frame);
        mBatteryBody = (BatteryMeterView) findViewById(R.id.battery);
        mBatteryCharge = (ImageView) findViewById(R.id.battery_charge);
        mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        showBatteryLevel(show);
    }
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Respect font size setting.
//        mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
//                getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBatteryController.removeStateChangedCallback(this);
        mBatteryFrame = null;
        mBatteryBody = null;
        mBatteryCharge = null;
        mBatteryLevel = null;
        unregisterContentObserver();
    }

	@Override
	public void onBatteryLevelChanged(int level, boolean pluggedIn,
			boolean charging) {
		// TODO Auto-generated method stub
		boolean changed = mIsCharging != charging;
		mIsCharging = charging;
		mBatteryCharge.setVisibility(charging ? View.VISIBLE : View.GONE);
//		String str = String.format("%d", level)+"%";//getResources().getString(R.string.battery_level_template, level)
		String str = String.format(Locale.US, "%d",level)+"%";
        mBatteryLevel.setText(str);
        if(mBatteryBody != null){
        	 mBatteryBody.setBatteryLevel(level);//===modify by ty
        	 mBatteryBody.setPlugged(pluggedIn);
        	 mBatteryBody.setCharged(mIsCharging);
        }
		invalidate();
	}

//	@Override
//	public void onPowerSaveChanged() {
//		// TODO Auto-generated method stub
//		mPowerSaveEnabled = mBatteryController.isPowerSave();
//        invalidate();
//	}
	

    protected void applyDarkIntensity(float darkIntensity, View lightIcon, View darkIcon) {
        lightIcon.setAlpha(1 - darkIntensity);
        darkIcon.setAlpha(darkIntensity);
    }

    protected void setTint(ImageView v, int tint) {
        v.setImageTintList(ColorStateList.valueOf(tint));
    }
    protected final Rect mTintArea = new Rect();
    protected int mIconTint = Color.WHITE;
    protected float mDarkIntensity;
    
    public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
        boolean changed = tint != mIconTint || darkIntensity != mDarkIntensity
                || !mTintArea.equals(tintArea);
        mIconTint = tint;
        int oldColor = mIconColor;
        mIconColor = StatusBarIconController.getTint(mTintArea, mBatteryFrame, mIconTint);//getFillColor(darkIntensity);
        //LogHelper.sd("", "fffff mIconColor="+Integer.toHexString(mIconColor));
        mDarkIntensity = darkIntensity;
        mTintArea.set(tintArea);
        if (changed || (oldColor != mIconColor)/*&& isAttachedToWindow()*/) {
            applyIconTint();
        }
    }
    
    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }
    
    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }
    
    private void applyIconTint() {
        setTint(mBatteryFrame, StatusBarIconController.getTint(mTintArea, mBatteryFrame, mIconTint));
        setTint(mBatteryCharge, StatusBarIconController.getTint(mTintArea, mBatteryCharge, mIconTint));
        mBatteryLevel.setTextColor(StatusBarIconController.getTint(mTintArea, mBatteryLevel, mIconTint));
        mBatteryBody.updateFrameColor(mIconColor);
    }
    
    public void applyIconTintColor(int color) {
    	if (isAttachedToWindow()) {
    		mBatteryFrame.setImageTintList(ColorStateList.valueOf(color));
        	mBatteryFrame.setImageTintList(ColorStateList.valueOf(color));
            mBatteryLevel.setTextColor(color);
            mIconColor = color;//((color&0x00ffffff) > 0x7f7f7f) ? Color.BLACK: Color.WHITE;
            mBatteryBody.updateFrameColor(mIconColor);
    	}
    }
    
	public void updateBatteryColor(int iconColor) {
		if (mIconColor != iconColor) {
			mIconColor = iconColor;
			if (mBatteryFrame != null) {
					mBatteryFrame.clearColorFilter();
					mBatteryFrame.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
				}
				if (mBatteryBody != null) {
					mBatteryBody.updateFrameColor(mIconColor);//===modify by ty
				}
				if (mBatteryCharge != null) {
					mBatteryCharge.clearColorFilter();
					mBatteryCharge.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
			}
			if (mBatteryLevel != null) {
				mBatteryLevel.setTextColor(mIconColor);
			}
		}
	}
	
	public void showBatteryLevel(boolean show) {
		// TODO Auto-generated method stub
		if (mBatteryLevel != null && show != mShowPercent) {
			mShowPercent = show;
			mBatteryLevel.setVisibility(mShowPercent ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void onPowerSaveChanged(boolean isPowerSave) {
		mPowerSaveEnabled = mBatteryController.isPowerSave();
        invalidate();
	}
	@Override
	public void onTuningChanged(String key, String newValue) {
	}
}
