
package com.android.systemui.statusbar;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.SignalClusterView.PhoneState;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.MSimNetworkControllerImpl;
import com.android.systemui.statusbar.policy.NetworkController.IconState;

public class SignalClusterViewYuLong
        extends SignalClusterView
        implements MSimNetworkControllerImpl.YulongSignalCluster {

    private static final String TAG = "SignalClusterViewYuLong";
    private static final boolean DEBUG = true;
    private static final long duration = 400;
//    private static final int mPhoneCount = 2;
    private FrameLayout   mWifiGroup;
    private ImageView   mWifiSignal;
    private ImageView   mWifiData;
    
    private View mDataNetworkGroup;
    private ImageView	mDataNetwork;
    private ImageView 	mDataNetworkType;    
    private ImageView mDataNetworkSign;
    
    private View mAirplaneGroup;
    private MultiSignalView[] mSignalView;// = new MultiSignalView [mPhoneCount];

    private int [] mSignalIconId;// = new int [mPhoneCount];
    private int [] mSignalSlaveIconId;// = new int [mPhoneCount];
    private int [] mSignalTypeIconId;// = new int [mPhoneCount];
    private int [] mSignalSlaveTypeIconId;// = new int [mPhoneCount];
    private int [] mSignalBackIconId;// = new int [mPhoneCount];
    private int mDataNetworkIcons;
    private int mDataNetworkTypeIcon;
//    private int [] mRoamingIcons = new int [2];
    private ImageView mFlightMode;
    private GradientDrawable mFilghtModeBackground;
    private ObjectAnimator mFlightInAnimator;
    private ObjectAnimator mFlightOutAnimator;
    private boolean bFirstInit = true;
    private int mIconColor;
    
    private int[] mRoamingIcons;
    private int mPhoneCount;
    private int[] SIGNAL_GROUP = { R.id.mobile_signal_group_0, R.id.mobile_signal_group_1, R.id.mobile_signal_group_2 };

    public SignalClusterViewYuLong(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        mSignalView = new MultiSignalView[mPhoneCount];
        mSignalIconId = new int[mPhoneCount];
        mSignalSlaveIconId = new int[mPhoneCount];
        mSignalTypeIconId = new int[mPhoneCount];
        mSignalSlaveTypeIconId = new int[mPhoneCount];
        mSignalBackIconId = new int[mPhoneCount];
        mRoamingIcons = new int[mPhoneCount];
    }

    @Override
    protected void onFinishInflate() {
    	mFlightMode = (ImageView)findViewById(R.id.scene_flight_mode);
        mDataNetwork = (ImageView) findViewById(R.id.mobile_data_network_yulong_0);
        mDataNetworkType = (ImageView) findViewById(R.id.mobile_data_network_type_yulong_0);
        
        mDataNetworkGroup = findViewById(R.id.mobile_data_group);
//        mSignalView[0] = (MultiSignalView) findViewById(R.id.mobile_signal_group_0);
//        mSignalView[1] = (MultiSignalView) findViewById(R.id.mobile_signal_group_1);
//        //TelephonyManager.getDefault().isMultiSimEnabled()
//        if(TelephonyManager.getDefault().getPhoneCount()==1){
//        	mSignalView[1].setVisibility(View.GONE);
//        }
//		mSignalView[0].setPadding(0, 0, 0, 0);
//        mSignalView[1].setPadding(0, 0, 0, 0);
      /// M modify for MTK
        for (int i = 0; i < mSignalView.length; i++) {
            mSignalView[i] = (MultiSignalView) findViewById(SIGNAL_GROUP[i]);
            mSignalView[i].setPadding(0, 0, 0, 0);
            mSignalView[i].setVisibility(View.VISIBLE);
        }
        if(TelephonyManager.getDefault().getPhoneCount()==1){
        	for (int i = 0; i < mSignalView.length; i++) {
        		if(i == 0){
        			continue;
        		}
                mSignalView[i].setVisibility(View.GONE);
            }
        }

        mWifiGroup = (FrameLayout) findViewById(R.id.wifi_signal_group);
        mWifiSignal = (ImageView) findViewById(R.id.wifi_signal_icon);
        mWifiData = (ImageView) findViewById(R.id.wifi_data_icon);
        mAirplaneGroup = findViewById(R.id.airplane_showhide);
        mDataNetworkSign = (ImageView)findViewById(R.id.mobile_data_network_sign);
        super.onFinishInflate();
        apply();
        LogHelper.sd(TAG, "SignalClusterViewYuLong.onFinishInflate");
    }
    
    @Override
    protected void onAttachedToWindow() {
        LogHelper.sd(TAG, "SignalClusterViewYuLong.onAttachedToWindow");
        apply();
        super.onAttachedToWindow();
    }

    @Override
    public void setWifiSignalIndicator(int strengthIcon,int wifiData) {
//        if (strengthIcon > 0){
//            mWifiSignal.setImageResource(strengthIcon);
//            //mWifiSignal.setVisibility(View.VISIBLE);
//            //mWifiData.setImageResource(wifiData);
//            mWifiData.setImageResource(wifiData);
//            //mWifiData.setVisibility(View.VISIBLE);
//            mWifiGroup.setVisibility(View.VISIBLE);
//        } else {
//            //mWifiSignal.setVisibility(View.GONE);
//            //mWifiData.setVisibility(View.GONE);
//            mWifiGroup.setVisibility(View.GONE);
//        }              
    }
    
    @Override
    public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
            boolean activityIn, boolean activityOut, String description) {

    	mWifiVisible = statusIcon.visible && !mBlockWifi;
        mWifiStrengthId = statusIcon.icon;
        mWifiActivityId = getWifiActivityId(activityIn, activityOut);
        mWifiDescription = statusIcon.contentDescription;
        
        if (mWifiVisible){// && mWifiStrengthId > 0
            mWifiSignal.setImageResource(mWifiStrengthId);
            mWifiData.setImageResource(mWifiActivityId);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }   
    }
    
    //
    @Override
	public void setSignalIndicators(int phoneId, int strengthIcon, int typeIcon) {
    	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, " setSignalIndicators phoneId = " + phoneId + " strengthIcon = "
						+ strengthIcon + " typeIcon = " + typeIcon);
		if (phoneId >= 0 && phoneId < mPhoneCount) {
			mSignalIconId[phoneId] = strengthIcon;
			mSignalTypeIconId[phoneId] = typeIcon;
		}
		apply();
	}
    
    @Override
    public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType,
            int qsType, boolean activityIn, boolean activityOut, int dataActivityId,
            int mobileActivityId, int stackedDataId, int stackedVoiceId,
            String typeContentDescription, String description, boolean isWide, int subId) {
        Boolean mMobileVisible = statusIcon.visible && !mBlockMobile;
        int mMobileStrengthId = statusIcon.icon;
        int mMobileTypeId = statusType;
        String mMobileDescription = statusIcon.contentDescription;
        String mMobileTypeDescription = typeContentDescription;
        Boolean mIsMobileTypeIconWide = statusType != 0 && isWide;
        int mDataActivityId = dataActivityId;
        int mMobileActivityId = mobileActivityId;
        int mStackedDataId = stackedDataId;
        int mStackedVoiceId = stackedVoiceId;

        //setSignalIndicators(mStackedDataId mMobileStrengthId, mMobileTypeId, mStackedVoiceId);
        //setDataNetworkIndicators(mMobileTypeId, mDataActivityId);
        //apply();
    }
    
    @Override
    public void setSlaveSignalIndicators(int phoneId, int strengthIcon, int typeIcon) {
    	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, " setSlaveSignalIndicators phoneId = " + phoneId + " strengthIcon = " + strengthIcon + " typeIcon = " + typeIcon);
        if (phoneId >= 0 && phoneId < mPhoneCount) {
        	mSignalSlaveIconId[phoneId] = strengthIcon;
          	mSignalSlaveTypeIconId[phoneId] = typeIcon;
      	}
      	apply();
    }

    @Override
    public void setMainCardIndicators(int phoneId, int backgroundIcon) {
    	LogHelper.sd(TAG, "setMainSignalIndicators phoneId = " + phoneId + " backgroundIcon = " + backgroundIcon);
        for (int i = 0; i < mSignalBackIconId.length; ++i) {
            if (phoneId == i) {
                mSignalBackIconId[i] = backgroundIcon;
            } else {
                mSignalBackIconId[i] = 0;
            }
        }        
        apply();
    }
    
	@Override
	public void setSignalVisible(int phoneId, boolean visable) {
		if (phoneId >= 0 && phoneId < mPhoneCount){
			if (mSignalView[phoneId] != null){
			    mSignalView[phoneId].setVisibility(visable ? View.VISIBLE : View.GONE);
			}		
		}
	}
	@Override
    public void setDataNetworkIndicators(int typeIconId, int activityIconId){
        LogHelper.sd(TAG, "setDataNetworkIndicators " + " typeIconId = " + typeIconId+ " activityIconId = " + activityIconId);
            mDataNetworkIcons= activityIconId;
            mDataNetworkTypeIcon = typeIconId;    
        apply();
    }
    @Override
    public void setDataNetworkVisible(boolean visible){
        if (mDataNetworkGroup != null){
            mDataNetworkGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void setRoamingIndicator(int phoneId, int iconId){
        mRoamingIcons[phoneId] = iconId;
        apply();
    }
	
    public void apply(){        
        for (int i = 0; i < mSignalView.length; ++i){
            mSignalView[i].setSignal(MultiSignalView.TYPE_MAIN, mSignalIconId[i]);
            mSignalView[i].setSignal(MultiSignalView.TYPE_SINGLE, mSignalIconId[i]);
            mSignalView[i].setSignal(MultiSignalView.TYPE_SLAVE, mSignalSlaveIconId[i]);            
            mSignalView[i].setType(MultiSignalView.TYPE_MAIN, mSignalTypeIconId[i]);
            mSignalView[i].setType(MultiSignalView.TYPE_SINGLE, mSignalTypeIconId[i]);
            mSignalView[i].setType(MultiSignalView.TYPE_SLAVE, mSignalSlaveTypeIconId[i]);           
            mSignalView[i].setRoaming(mRoamingIcons[i]);            
            mSignalView[i].setBackgroundResource(mSignalBackIconId[i]);
            mSignalView[i].setSingle(mSignalSlaveIconId[i] == 0);                        
        }
        mDataNetwork.setImageResource(mDataNetworkIcons);           
        mDataNetworkType.setImageResource(mDataNetworkTypeIcon); 
    	if (mDataNetworkTypeIcon != 0){
            mDataNetworkType.setVisibility(View.VISIBLE);
    	}else{
            mDataNetworkType.setVisibility(View.GONE);            		
   		}        
    }


	@Override
	public void setAirplaneMode(boolean airplaneMode) {
		setAirplaneModeAnimator(airplaneMode);
	}
	boolean mAirplaneMode = false;
	public void setAirplaneModeAnimator(boolean airplaneMode) {
		
		if (bFirstInit){
			bFirstInit = false;
	        float widthFlightMode = mFlightMode.getWidth();
	        if (widthFlightMode < 1f){
	        	widthFlightMode = mFlightMode.getMeasuredWidth();
	        }
	        if (widthFlightMode < 1f){
		        LogHelper.sv("mFlightOutAnimator", "mFlightMode.getWidth() = " + mFlightMode.getWidth() + " mFlightMode.getMeasuredWidth() = " + mFlightMode.getMeasuredWidth());
	        	widthFlightMode = 35;
	        }
	        mFilghtModeBackground = new GradientDrawable(Orientation.TL_BR, new int[] {0x88000000,0x110000});
	        mFilghtModeBackground.setGradientType(GradientDrawable.RADIAL_GRADIENT);
	        mFilghtModeBackground.setGradientRadius(widthFlightMode/2);
	        mFilghtModeBackground.setGradientCenter(0.5f, 0.5f);
			mFlightMode.setVisibility(airplaneMode?View.VISIBLE:View.INVISIBLE);
			mAirplaneGroup.setVisibility(airplaneMode?View.GONE:View.VISIBLE);
			mAirplaneMode = airplaneMode;
			return;
		}
		if(airplaneMode == mAirplaneMode){
			return;
		}
		mAirplaneMode = airplaneMode;
		if (airplaneMode == true){
			//飞机飞入动画
			if (mFlightOutAnimator != null){
				mFlightOutAnimator.cancel();
			}
			if (mFlightInAnimator != null){
				mFlightInAnimator.cancel();
			}
	        mFlightMode.setVisibility(View.VISIBLE);
	        mFlightMode.setTranslationX(0);
	        mFlightMode.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			mFlightMode.setBackground(mFilghtModeBackground);	
	        
	        final float translationX = mAirplaneGroup.getWidth() - mFlightMode.getWidth();
			mFlightInAnimator = ObjectAnimator.ofFloat(mFlightMode, View.TRANSLATION_X, 0, translationX);
//			AccelerateInterpolator interpolator = new AccelerateInterpolator();
//			mFlightInAnimator.setInterpolator(interpolator);
			mFlightInAnimator.setDuration(duration);
			mFlightInAnimator.addListener(new AnimatorListenerAdapter() {
	            public void onAnimationEnd(Animator animation) {
	            	//mFlightMode.setLayerType(View.LAYER_TYPE_NONE, null);
	            	mFlightMode.setBackground(null);
	            	mFlightMode.setTranslationX(0);
	                mAirplaneGroup.setVisibility(View.GONE);
	                mAirplaneGroup.setAlpha(1.0f);
	                mFlightInAnimator = null;
	            }
	        });
			mFlightInAnimator.addUpdateListener(new AnimatorUpdateListener() {
	            public void onAnimationUpdate(ValueAnimator animation) {
	            	float pos = mFlightMode.getTranslationX();
	            	float alpha = 0.3f;
	            	if (translationX != 0){
	            		alpha -= pos/translationX;
	            	}
	            	else{
	            		alpha = 0.3f;	            		
	            	}
	            	mAirplaneGroup.setAlpha(alpha);
	            }
	        });	        
			mFlightInAnimator.start();			
		}else{
			//飞机飞出动画
			if (mFlightOutAnimator != null){
				mFlightOutAnimator.cancel();
			}
			if (mFlightInAnimator != null){
				mFlightInAnimator.cancel();
			}
            mAirplaneGroup.setVisibility(View.VISIBLE);
            mAirplaneGroup.setAlpha(0f);
	        mFlightMode.setVisibility(View.VISIBLE);
	        mFlightMode.setLayerType(View.LAYER_TYPE_HARDWARE, null);
	        mFlightMode.setBackground(mFilghtModeBackground);
	        mAirplaneGroup.measure(0, 0);
	        float widthAirplaneGroup = mAirplaneGroup.getWidth();
	        if (widthAirplaneGroup < 1f){
	        	widthAirplaneGroup = mAirplaneGroup.getMeasuredWidth();
	        }
	        float widthFlightMode = mFlightMode.getWidth();
	        if (widthFlightMode < 1f){
	        	widthFlightMode = mFlightMode.getMeasuredWidth();
	        }
	        final float translationXStart = widthAirplaneGroup - widthFlightMode;
	        mFlightMode.setTranslationX(translationXStart);
	        final float translationXEnd = translationXStart +  widthFlightMode;
	        LogHelper.sv("mFlightOutAnimator", "translationXStart = " + translationXStart + " translationXEnd = " + translationXEnd);
			mFlightOutAnimator = ObjectAnimator.ofFloat(mFlightMode, View.TRANSLATION_X, translationXStart, translationXEnd);
			AccelerateInterpolator interpolator = new AccelerateInterpolator();
			mFlightOutAnimator.setInterpolator(interpolator);
			mFlightOutAnimator.setDuration(duration);
			mFlightOutAnimator.addListener(new AnimatorListenerAdapter() {
	            public void onAnimationEnd(Animator animation) {
	            	//mFlightMode.setLayerType(View.LAYER_TYPE_NONE, null);
	            	mFlightMode.setVisibility(View.GONE);
	                mAirplaneGroup.setAlpha(1.0f);
	                mFlightOutAnimator = null;
	            }
	        });
			mFlightOutAnimator.addUpdateListener(new AnimatorUpdateListener() {
	            public void onAnimationUpdate(ValueAnimator animation) {
	            	float pos = mFlightMode.getTranslationX();
	            	float alpha = 0f;
	            	if (translationXEnd - translationXStart > 0){
	            		alpha += (pos - translationXStart)/(translationXEnd - translationXStart);
	            	}
	            	else{
	            		alpha = 0.4f;	            		
	            	}
	            	mAirplaneGroup.setAlpha(alpha);
	            }
	        });	        
			mFlightOutAnimator.start();			
		}
	}

	@Override
	public void setOffLine(int phoneId, boolean bOffLine) {
		if(phoneId >=0 && phoneId < mPhoneCount)
			mSignalView[phoneId].setOffLine(bOffLine);		
	}
	@Override
	public void setNoSimCard(int phoneId, boolean bNoSimCard) {
		if(phoneId >=0 && phoneId < mPhoneCount)
			mSignalView[phoneId].setNoSimCard(bNoSimCard);		
	}
	@Override
    public void setNoSimCardIcon(int phoneId,int nIcon) {
		if(phoneId >=0 && phoneId < mPhoneCount)
			mSignalView[phoneId].setNoSimCardIcon(nIcon);
    }	
	public void setDataNetworkSign(int nIcon){
		mDataNetworkSign.setImageResource(nIcon);
    }
    
	public void setDataNetworkSignVisible(boolean bShow){   
		mDataNetworkSign.setVisibility(bShow?View.VISIBLE:View.GONE);
    }

	@Override
	public void setSignalSingleMode(int phoneId, boolean signalSingleMode) {
		// TODO Auto-generated method stub
//		if(signalSingleMode){
//			mSignalView[phoneId].setPadding(0, 0, 0, 0);
	        //mSignalView[1].setPadding(0, 0, 0, 0);
//		}else{
//			mSignalView[phoneId].setPadding(0, Utilities.dipToPixel(mContext, 1), 0, 0);
	        //mSignalView[1].setPadding(0, Utilities.dipToPixel(mContext, 1), 0, 0);
//		}
	}
	
	public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
        boolean changed = tint != mIconTint || darkIntensity != mDarkIntensity
                || !mTintArea.equals(tintArea);
        mIconTint = tint;
        mDarkIntensity = darkIntensity;
        mTintArea.set(tintArea);
        if (changed && isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private void applyIconTint() {
        setTint(mFlightMode, StatusBarIconController.getTint(mTintArea, mFlightMode, mIconTint));
        setTint(mWifiSignal, StatusBarIconController.getTint(mTintArea, mWifiSignal, mIconTint));
        setTint(mWifiData, StatusBarIconController.getTint(mTintArea, mWifiData, mIconTint));
        setTint(mDataNetwork, StatusBarIconController.getTint(mTintArea, mDataNetwork, mIconTint));
        setTint(mDataNetworkType, StatusBarIconController.getTint(mTintArea, mDataNetworkType, mIconTint));
        setTint(mDataNetworkSign, StatusBarIconController.getTint(mTintArea, mDataNetworkSign, mIconTint));
        for(int i = 0; i< 2; i++){
			if(mSignalView[i] != null){
				mSignalView[i].applyIconTint(mIconTint, mDarkIntensity, mTintArea);
			}
		}
    }
    
    public void updateSignalIconColor(int color) {
    	if (color != 0) {
    		if (mIconColor != color) {
    			mIconColor = color;
    		}
        	if (mFlightMode != null) {
        		mFlightMode.clearColorFilter();
        		mFlightMode.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    		}
        	if(mWifiSignal != null){
        		mWifiSignal.clearColorFilter();
        		mWifiSignal.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        	}
    		if(mWifiData != null){
    			mWifiData.clearColorFilter();
    			mWifiData.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    		}
    		if(mDataNetwork != null){
    			mDataNetwork.clearColorFilter();
    			mDataNetwork.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
			}
    		if(mDataNetworkType != null){
    			mDataNetworkType.clearColorFilter();
    			mDataNetworkType.setColorFilter(mIconColor);
    		}
    		if(mDataNetworkSign != null){
    			mDataNetworkSign.clearColorFilter();
    			mDataNetworkSign.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
			}
    		for(int i = 0; i< 2; i++){
    			if(mSignalView[i] != null){
    				mSignalView[i].setSignalViewColor(mIconColor);
    			}
    		}
		}
    	invalidate();
    }
}
