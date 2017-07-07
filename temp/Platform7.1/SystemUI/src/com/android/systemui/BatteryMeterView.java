/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License.
 */
package com.android.systemui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.tuner.TunerService;

public class BatteryMeterView extends View implements
        BatteryController.BatteryStateChangeCallback, TunerService.Tunable {

//    private final BatteryMeterDrawable mDrawable;
//    private final String mSlotBattery;
    private BatteryController mBatteryController;
    
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    private static final boolean SINGLE_DIGIT_PERCENT = false;

    private static final int FULL = 100;

    private final Paint mBatteryPaint, mTextPaint;

    private final int mCriticalLevel;
    private final int mChargeColor;
    private final int mLowPowerColor;

    private final RectF mBatteryBody = new RectF();

    private final Path mShapePath = new Path();
    
    private Context mContext;
	private int mNormalColor;
	
	private int mBatteryLevel;
	private boolean mPlugged = false;
	private boolean mCharged = false;
	private int addCurrentTime = 0;
	private int mBatteryHeight = 0;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

//        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
//                defStyle, 0);
//        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
//                context.getColor(R.color.batterymeter_frame_color));
//        mDrawable = new BatteryMeterDrawable(context, new Handler(), frameColor);
//        atts.recycle();
//
//        mSlotBattery = context.getString(
//                com.android.internal.R.string.status_bar_battery);
//        setImageDrawable(mDrawable);
        
        mContext = context;

        final Resources res = context.getResources();
        mCriticalLevel = mContext.getResources().getInteger(R.integer.config_criticalBatteryWarningLevel);
        mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setStrokeWidth(0);
        mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG  
                | Paint.DEV_KERN_TEXT_FLAG);
        Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mNormalColor = res.getColor(R.color.status_bar_icon_color_white);
        mChargeColor = res.getColor(R.color.batterymeter_charge_color);
        mLowPowerColor = res.getColor(R.color.batterymeter_lowpower_color);
        
        mBatteryHeight = mContext.getResources().getDimensionPixelSize(R.dimen.battery_body_height_3px);   
        DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getMetrics(dm);
		int den = (int)dm.density;
		if(den == 3){
			mBatteryHeight = mContext.getResources().getDimensionPixelSize(R.dimen.battery_body_height_3px); 
		}else{
			mBatteryHeight = mContext.getResources().getDimensionPixelSize(R.dimen.battery_body_height_2px);
		}
    }
    
    public void updateFrameColor(int color){
    	mNormalColor = color;
    	invalidate();
    }

    
    public void setBatteryLevel(int level) {
    	mBatteryLevel = level;
	}
    
    public void setPlugged(boolean plugged){
    	mPlugged = plugged;
    	invalidate();
    }
    
    public void setCharged(boolean charged){
    	mCharged = charged;
    	invalidate();
    }
    @Override
    public void draw(Canvas canvas) {

        float drawFrac = (float) mBatteryLevel / 100f;
        
        final int width = mContext.getResources().getDimensionPixelSize(R.dimen.battery_body_width);
        final int height = mBatteryHeight;      
        final int leftOffset = mContext.getResources().getDimensionPixelSize(R.dimen.battery_body_left);
        final int topOffset = mContext.getResources().getDimensionPixelSize(R.dimen.battery_body_top);
        
        mBatteryBody.set(0, 0, width, height);
        mBatteryBody.offset(leftOffset, topOffset);

        // set the battery charging color
        
        if (mBatteryLevel >= FULL) {
            drawFrac = 1f;
        }
        if (mBatteryLevel <= mCriticalLevel) {
            mBatteryPaint.setColor(mCharged ? mChargeColor : mLowPowerColor/*getColorForLevel(level)*/);
        } else {
        	if(mCharged){
        		mBatteryPaint.setColor(mChargeColor);
        	}else{
            mBatteryPaint.setColor(mNormalColor);
        	}
		}
        
        float oldRight = mBatteryBody.right;
        float bodyWidth = mBatteryBody.width();
        final float levelRight =mBatteryBody.right - (bodyWidth * (1f - drawFrac));
        
        // define the battery shape
        mBatteryBody.right = levelRight;

        // draw the battery body, clipped to charging level
	     mShapePath.reset();
	     float addCurrentValue = 0;
	     addCurrentValue = mBatteryBody.right;
         mShapePath.moveTo(mBatteryBody.left, mBatteryBody.top);
         mShapePath.lineTo(addCurrentValue, mBatteryBody.top);
         mShapePath.lineTo(addCurrentValue, mBatteryBody.top);
         mShapePath.lineTo(addCurrentValue, mBatteryBody.bottom);
         mShapePath.lineTo(mBatteryBody.left, mBatteryBody.bottom);
         mShapePath.lineTo(mBatteryBody.left, mBatteryBody.top);
	     Log.d("", "drawing------mNormalColor:" + Integer.toHexString(mNormalColor));
	     canvas.drawPath(mShapePath, mBatteryPaint);
    }
    void updateIcons(){
    	invalidate();
    }
    

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
//        if (StatusBarIconController.ICON_BLACKLIST.equals(key)) {
//            ArraySet<String> icons = StatusBarIconController.getIconBlacklist(newValue);
//            setVisibility(icons.contains(mSlotBattery) ? View.GONE : View.VISIBLE);
//        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBatteryController.addStateChangedCallback(this);
//        mDrawable.startListening();
//        TunerService.get(getContext()).addTunable(this, StatusBarIconController.ICON_BLACKLIST);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBatteryController.removeStateChangedCallback(this);
//        mDrawable.stopListening();
//        TunerService.get(getContext()).removeTunable(this);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
//        setContentDescription(
//                getContext().getString(charging ? R.string.accessibility_battery_level_charging
//                        : R.string.accessibility_battery_level, level));
//        setBatteryLevel(level);
//        setPlugged(pluggedIn);
//        setCharged(charging);
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {

    }

    public void setBatteryController(BatteryController mBatteryController) {
        this.mBatteryController = mBatteryController;
//        mDrawable.setBatteryController(mBatteryController);
    }

    public void setDarkIntensity(float f) {
//        mDrawable.setDarkIntensity(f);
    }
}
