package com.android.systemui.statusbar.phone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.quicksettings.bottom.MaterialRippleLayout;
import com.android.systemui.quicksettings.bottom.QuickSettingsPannelView;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;

public final class QuickSettingsItemView extends FrameLayout{

    private static final String TAG = "QuickSettingsItemView";
    private int mId = -1;
    private int mIndex = -1;
    private int mStatus,mTextStatus;
    private ImageView mIcon;
    private TextView mText;
    private ImageView mSecondaryIcon;
    private LinearLayout mLayout;
    Animation mShakeAnim;
    public boolean mIsConfigItem = false;
    private QuickSettingsController    mController ;
    private boolean isShown = true;

    public QuickSettingsItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mController = new QuickSettingsController(context);
    }

    public QuickSettingsItemView(Context context){
        this(context, null);
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (ImageView)findViewById(R.id.icon);
        mLayout = (LinearLayout)findViewById(R.id.textlayout);
        mText = (TextView)findViewById(R.id.text);
        mSecondaryIcon = (ImageView)findViewById(R.id.icon_secondary);
        mShakeAnim = AnimationUtils.loadAnimation(mContext, R.anim.shake_icon);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Drawable d = getBackground();
        int n = (int)(5 * mContext.getResources().getDisplayMetrics().density);
        if(d instanceof RippleDrawable){
            d.setHotspotBounds(0 + n, 0 + n, w - n, h - n);
        }
    }
    public void setViewColor(int color, Boolean b) {
        if (mIcon != null) {
            mIcon.clearColorFilter();
            if(b)mIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        if (mSecondaryIcon != null) {
            mSecondaryIcon.clearColorFilter();
            if(b)mSecondaryIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        invalidate();
    }
    private void setStatus(int status,int textStatus){
        if(Utilities.showDragDownQuickSettings() || mIsConfigItem){
            if (status == QuickSettingsModel.State.STATUS_CHANGING) {
                setViewColor(0xffbfbfbf,false);
                mText.setTextColor(0xAA0D0D0D);
            } else if (status == QuickSettingsModel.State.STATUS_DISABLE){
                setViewColor(0xffcfcfcf,true);
                mText.setTextColor(0x990D0D0D);
            } else{
                setViewColor(0xffbfbfbf,false);
                mText.setTextColor(0xFF0D0D0D);
            }
        }else{
            int alpha = 255;
            int colorId = 0;
            boolean clickable = true;
            if (status == QuickSettingsModel.State.STATUS_CHANGING) {
                clickable = false;
                alpha = 60;
                colorId = R.color.qs_text_color_changing;
                mText.setTextColor(mContext.getResources().getColor(colorId));
                //setViewColor(0xffbfbfbf,false);
            } else if (status == QuickSettingsModel.State.STATUS_DISABLE){
                colorId = R.color.qs_text_color_disabled;
                mText.setTextColor(mContext.getResources().getColor(colorId));
                //setViewColor(0xff7f7f7f,true);
            } else if (status == QuickSettingsModel.State.STATUS_ENABLE){
                colorId = R.color.qs_text_color_enabled;
                mText.setTextColor(mContext.getResources().getColor(colorId));
                //setViewColor(0xffbfbfbf,false);
            }else if (status == QuickSettingsModel.State.STATUS_FLAG){
                mText.setTextColor(0xFFFF0000);
                //setViewColor(0xffbfbfbf,false);
            }
        }
        mText.setTextColor(Color.parseColor("#FFFFFF"));
    }
    public void updateState(State state){
        if (state.textId > 0){
            mText.setText(state.textId);
        } else {
            mText.setText(state.text);
        }
        if (state.iconId > 0){
            try {
                Drawable d = mContext.getDrawable(state.iconId);
                mIcon.setImageDrawable(d);

            } catch (Exception e) {            	
            }			
		}else{
			mIcon.setImageResource(state.iconId);
		}
		setStatus(state.status,state.textStatus);
		setQuickSettingId(state.id);
		if (state.isVisible){
			setVisibility(View.VISIBLE);
		}else{
			setVisibility(View.GONE);			
		}
	}		
	public void shakeAnimate(boolean shake){
//		if(shake)
//			startAnimation(mShakeAnim);
//		else
//			clearAnimation();
	}
	public void setVisibleSecondary(State state){
		boolean isVisible = state.isVisibleSecondary;
		mSecondaryIcon.setVisibility(isVisible?View.VISIBLE:View.GONE);
	}
	public int getQuickSettingId(){
		return mId;
	}
	public void setQuickSettingId(int id){
		mId = id;
	}
	public void setAutoScaleTextSzie(boolean bEnable){
	}
	public int getStatusIndex(State state){
		return state.id;
	}
	public int getQuickSettingIndex() {
		return mIndex;
	}
	
	public String getQuickSettingText() {
		return mText.getText().toString().trim();
	}

	public void setQuickSettingIndex(int index) {
		mIndex = index;
	}
	//yulong begin: add for systemUI 6.0,2016.06.20
	public void setQuickSettingisShown(boolean show) {
		isShown = show;
	}
	
	public boolean  getQuickSettingisShown() {
		return isShown;
	}
	
	public void setTextViewIcon(){
		mText.setPadding((int)dip2px(8), 0, 0, 0);
		Drawable nav_up = getResources().getDrawable(R.drawable.coner_icon, null);
		nav_up.setBounds(0, -(int)dip2px(4), (int)dip2px(5),(int)dip2px(2));
		mText.setCompoundDrawables(null, null, nav_up, null);
	}
	
	public void removeTextViewIcon(){
		mText.setPadding(0, 0, 0, 0);
		mText.setCompoundDrawables(null, null, null, null);
	}
	
	public int  stateId  = 0;
	public void setTextViewOnclickListener(int id){
		stateId = id;
		//mController.setStatusBar(Statusbar);
        mLayout.setOnTouchListener(mTextLayoutTouchListener);
        mLayout.setOnClickListener(mTextLayoutClickListener);
    }
    public void setMobileTextViewOnclickListiner(int id){
        stateId = id;
        mLayout.setOnTouchListener(mTextLayoutTouchListener);
        mLayout.setOnClickListener(mMobileTextViewClickListener);
    }

    View.OnClickListener mTextLayoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mController.onSecondaryClickQuickSetting(stateId);
        }
    };

    View.OnClickListener mMobileTextViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mController.onTextViewClickListiner(stateId);
        }
    };


     View.OnTouchListener mTextLayoutTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View a, MotionEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    MaterialRippleLayout.on(a)
                    .rippleColor(Color.parseColor("#ffffff"))
                    .rippleAlpha(0.2f)
                    .rippleHover(true)
                    .create();
                }
                return true;
            }
     };

    protected float dip2px(int dip) {
        float scale = mContext.getResources().getDisplayMetrics().density;
        return (float) (dip * scale + 0.5f);
    }
    //yulong end
}
