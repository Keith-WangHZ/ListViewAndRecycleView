package com.android.systemui.statusbar;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MultiSignalView extends LinearLayout {

    
    public static final int TYPE_SINGLE = 0;
    public static final int TYPE_MAIN = 1;
    public static final int TYPE_SLAVE = 2;
    public static final int TYPE_MAX = 3;
    
    private static final String TAG = "MultiSignalView";
    private ViewGroup mMode_0, mMode_1;
    private boolean mSingle = true;
    private ImageView mSignals[] = new ImageView[TYPE_MAX];
    private ImageView mTypes[]   = new ImageView[TYPE_MAX];
    private ImageView mRoamings[] = new ImageView[2];
    
    // 增加离线模式图标控制
    private ImageView mOfflineMode;
    // 增加无卡图标显示
    private ImageView mNoSimCardIcon;
    private ViewGroup mSimCardReady;
    private ViewGroup mSignalReady;
    public MultiSignalView(Context context) {
        this(context, null);
    }

    public MultiSignalView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public MultiSignalView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        View.inflate(context, R.layout.multi_signal_view, this);
        
        mSignals[TYPE_SINGLE] = (ImageView) findViewById(R.id.signal_level);
        mSignals[TYPE_MAIN] = (ImageView) findViewById(R.id.signal_level_main);
        mSignals[TYPE_SLAVE] = (ImageView) findViewById(R.id.signal_level_slave);
        
        mTypes[TYPE_MAIN] = (ImageView) findViewById(R.id.signal_type_main);
        mTypes[TYPE_SINGLE]   = (ImageView) findViewById(R.id.signal_type);
        mTypes[TYPE_SLAVE] = (ImageView) findViewById(R.id.signal_type_slave);
        
        mRoamings[TYPE_SINGLE] = (ImageView) findViewById(R.id.roaming_mode_0);
        mRoamings[TYPE_MAIN]   = (ImageView) findViewById(R.id.roaming_mode_1);
        
        mMode_0 = (ViewGroup) findViewById(R.id.mode_0);
        mMode_1 = (ViewGroup) findViewById(R.id.mode_1);
                
        mOfflineMode = (ImageView) findViewById(R.id.offline_type);
        
        mNoSimCardIcon = (ImageView) findViewById(R.id.sim_card_absent);
        mSimCardReady = (ViewGroup) findViewById(R.id.sim_card_isready);
        mSignalReady = (ViewGroup) findViewById(R.id.signal_isready);        
    }
    
    public void setSignal(int type, int resId){
        mSignals[type].setImageResource(resId);
    }
    
    public void setType(int type, int resId){
        mTypes[type].setImageResource(resId);
    }
    
    public void setSingle(boolean bSingle){
        mSingle = bSingle;
        if (mSingle){
            mMode_0.setVisibility(VISIBLE);
            mMode_1.setVisibility(GONE);
        } else {
            mMode_0.setVisibility(GONE);
            mMode_1.setVisibility(VISIBLE);
        }
    }
    
    public void setRoaming(int resId){
        mRoamings[0].setImageResource(resId);
        mRoamings[1].setImageResource(resId);
    }    
    
    public void setOffLine(boolean bOffLine) {
    	mOfflineMode.setVisibility(bOffLine ? VISIBLE : GONE);
    	mSignalReady.setVisibility(!bOffLine ? VISIBLE : GONE);
    }
    
    public void setNoSimCard(boolean bNoSimCard) {
    	mNoSimCardIcon.setVisibility(bNoSimCard ? VISIBLE : GONE);
    	mSimCardReady.setVisibility(!bNoSimCard ? VISIBLE : GONE);
    }
    public void setNoSimCardIcon(int nIcon) {
    	mNoSimCardIcon.setImageResource(nIcon);
    }    
    
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
    
    protected void applyIconTint(int tint, float darkIntensity, Rect tintArea) {
    	mIconTint = tint;
        mDarkIntensity = darkIntensity;
        mTintArea.set(tintArea);
    	for(int i = 0; i< TYPE_MAX; i++){
			if (mSignals[i] != null) {
				setTint(mSignals[i], StatusBarIconController.getTint(mTintArea, mSignals[i], mIconTint));
			}
			if (mTypes[i] != null) {
				setTint(mTypes[i], StatusBarIconController.getTint(mTintArea, mTypes[i], mIconTint));
			}
		}
		for(int j = 0; j < 2; j++){
			if (mRoamings[j] != null) {
				setTint(mRoamings[j], StatusBarIconController.getTint(mTintArea, mRoamings[j], mIconTint));
			}
		}
		if (mOfflineMode != null) {
			setTint(mOfflineMode, StatusBarIconController.getTint(mTintArea, mOfflineMode, mIconTint));
		}
		if(mNoSimCardIcon != null){
			setTint(mNoSimCardIcon, StatusBarIconController.getTint(mTintArea, mNoSimCardIcon, mIconTint));
		}
    	
	}
    public void setSignalViewColor(int color) {
		for(int i = 0; i< TYPE_MAX; i++){
			if (mSignals[i] != null) {
				mSignals[i].clearColorFilter();
				mSignals[i].setColorFilter(color, PorterDuff.Mode.SRC_IN);
			}
			if (mTypes[i] != null) {
				mTypes[i].clearColorFilter();
				mTypes[i].setColorFilter(color, PorterDuff.Mode.SRC_IN);
			}
		}
		for(int j = 0; j < 2; j++){
			if (mRoamings[j] != null) {
				mRoamings[j].clearColorFilter();
				mRoamings[j].setColorFilter(color, PorterDuff.Mode.SRC_IN);
			}
		}
		if (mOfflineMode != null) {
			mOfflineMode.clearColorFilter();
			mOfflineMode.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		if(mNoSimCardIcon != null){
			mNoSimCardIcon.clearColorFilter();
			mNoSimCardIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		invalidate();
	}
}
