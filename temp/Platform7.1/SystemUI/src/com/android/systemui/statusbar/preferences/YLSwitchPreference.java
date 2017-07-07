package com.android.systemui.statusbar.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.yulong.android.common.widget.CommonSwitch;
import com.yulong.android.server.systeminterface.GlobalKeys;
import com.yulong.android.server.systeminterface.SystemManager;

public class YLSwitchPreference extends Preference {

    private ImageView mImvIcon;
    private TextView mTvTitle;
    private TextView mTvSummary;
    private CommonSwitch mSwitchButton;

    private View mSwitchPreferenceView;
    private Boolean mChecked;
    
    public YLSwitchPreference(Context context) {
        this(context, null);
    }
    
    public YLSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        Drawable mIcon = null;
        String mTitle="";
        String mSummary="";
        boolean bChecked = false;
        
        TypedArray typedArray = null;
        
        if(attrs != null){
            typedArray= context.obtainStyledAttributes(attrs,
                R.styleable.CoolpadSwitchPreference);
            mTitle = typedArray
                    .getString(R.styleable.CoolpadSwitchPreference_switch_preference_title);
            mSummary = typedArray
                    .getString(R.styleable.CoolpadSwitchPreference_switch_preference_summary);
            
            mIcon = typedArray.getDrawable(R.styleable.CoolpadSwitchPreference_switch_preference_icon);
            
            bChecked = typedArray.getBoolean(R.styleable.CoolpadSwitchPreference_switch_preference_checked, false);
        } 
        
        mSwitchPreferenceView = LayoutInflater.from(context).inflate(
                R.layout.yl_switch_preference, null);
        //add by wanggang for old modle start
//        try {
//        	 SystemManager mSystemManager = (SystemManager) this.getContext().getSystemService(GlobalKeys.SYS_SERVICE);
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
        //add by wanggang for old modle end
        
        mTvTitle = (TextView) mSwitchPreferenceView
                .findViewById(R.id.switch_preference_title);
        mTvSummary = (TextView) mSwitchPreferenceView
                .findViewById(R.id.switch_preference_summary);
        mSwitchButton = (CommonSwitch) mSwitchPreferenceView
                .findViewById(R.id.enabledswitch);
        LinearLayout layoutWidgetFrame = (LinearLayout) mSwitchPreferenceView.findViewById(R.id.widget_frame);
        layoutWidgetFrame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Test",
                        "linear layout be clicked" + mSwitchButton.isEnabled());
            }
        });
        

        if (mTitle != null) {
            mTvTitle.setText(mTitle);
        } else {
            mTvTitle.setText("");
        }

        if (mSummary != null && !mSummary.isEmpty()) {
            mTvSummary.setText(mSummary);
            mTvSummary.setVisibility(View.VISIBLE);
        } else {
            mTvSummary.setVisibility(View.GONE);
        }
        
        if(typedArray!=null){
            typedArray.recycle();
        }
    }
    

    @Override
    protected View onCreateView(ViewGroup parent) {
        return mSwitchPreferenceView;
    }

    public void setTitle(String strTitle) {
        mTvTitle.setText(strTitle);
    }
    
    public void setTitle(int nResId) {
        mTvTitle.setText(nResId);
    }

    public void setSummary(String strSummary) {
        if(mTvSummary.getVisibility()!=View.VISIBLE){
            mTvSummary.setVisibility(View.VISIBLE);
        }
        mTvSummary.setText(strSummary);
    }
    
    public void setSummary(int nResId) {
        mTvSummary.setText(nResId);
    }
    
    public void setSummaryVisible(boolean bFlag){
        if(bFlag){
            mTvSummary.setVisibility(View.VISIBLE); 
        }else{
            mTvSummary.setVisibility(View.GONE); 
        }
    }
    
//    public void setIcon(Drawable drawableIcon) {
//        mImvIcon.setImageDrawable(drawableIcon);
//    }
    
//    public void setIcon(int nResId) {
//        mImvIcon.setImageResource(nResId);
//    }
    
//    public void setIconVisible(boolean bFlag){
//        if(bFlag){
//            mImvIcon.setVisibility(View.VISIBLE); 
//        }else{
//            mImvIcon.setVisibility(View.GONE); 
//        }
//    }

    public CommonSwitch getSwitchButton() {
        return mSwitchButton;
    }
    
    public void setSwitchButtonEnabled(boolean p_bEnabled){
        if(mSwitchButton!=null){
            mSwitchButton.setEnabled(p_bEnabled);
        }
    }
    
    public void setSwitchButtonChecked(boolean p_bIsChecked){
    	mChecked=p_bIsChecked;
        if(mSwitchButton!=null){
            mSwitchButton.setChecked(p_bIsChecked);
        }
    }
    
    public boolean isSwitchButtonChecked(){
        return mSwitchButton.isChecked();
    }
}
