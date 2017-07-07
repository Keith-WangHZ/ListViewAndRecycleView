package com.android.systemui.statusbar;

import java.text.DecimalFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.YulongConfig;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DataNetworkController;
import com.securespaces.android.ssm.UserUtils;

public class StatusNetSpeed extends LinearLayout {
	private final static String TAG = "StatusNetSpeed";
    private TextView mTextView;
    private static final String SHOW_NET_SPEED = "net_speed_on";//Settings.Global.AIRPLANE_MODE_ON;//
    private CObserver mCObserver;
    private DataNetworkController mDataNetworkController;
    private boolean mPrimary;
    private boolean mDataEnabled = false;;
    private boolean mWifiEnabled = false;
    private boolean mOldNetState = false;
    private long mLastFlow  = 0;
    private long mNowFlow  = 0;
    private long mSleepTime = 1000;
    private static final String AIRPLANE_MODE = Settings.Global.AIRPLANE_MODE_ON;

    public StatusNetSpeed(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public StatusNetSpeed(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        Handler handler = new Handler();
        mTextView = (TextView) findViewById(R.id.text_status_net_speed);
        mCObserver = new CObserver(handler);
		mCObserver.startObserving();
		
		Boolean bool = YulongConfig.getDefault().isMultiUserSpace();
		Boolean b = UserUtils.currentUserIsOwner();
		if((bool==true && b==true) || !bool){
			mPrimary = false;
		}else{
			mPrimary = true;
		}
		
		mDataNetworkController = DataNetworkController.getInstance(mContext, mPrimary);
		
		
		
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        filter.addAction(Intent.ACTION_USER_INITIALIZE);
        filter.addAction(Intent.ACTION_USER_FOREGROUND);
        filter.addAction(Intent.ACTION_USER_BACKGROUND);
	    mContext.registerReceiverAsUser(mQuickSettingsReceiver, UserHandle.ALL, filter,null,null);
    }
    
    @Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		setNetStartShow(false);
	}
    
    private BroadcastReceiver mQuickSettingsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "mQuickSettingsReceiver onReceive " + action);

			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
						WifiManager.WIFI_STATE_UNKNOWN);
				mWifiEnabled = state == WifiManager.WIFI_STATE_ENABLED;
			}else if (Intent.ACTION_USER_SWITCHED.equals(action)
                    || Intent.ACTION_USER_REMOVED.equals(action)
                    || Intent.ACTION_USER_ADDED.equals(action)
                    || Intent.ACTION_USER_INFO_CHANGED.equals(action)
                    || Intent.ACTION_USER_INITIALIZE.equals(action)
                    || Intent.ACTION_USER_FOREGROUND.equals(action)
                    || Intent.ACTION_USER_BACKGROUND.equals(action)) {
			    	Boolean b = UserUtils.currentUserIsOwner();
			    	LogHelper.sd(TAG,"bPrimary="+b);
			    	if (b) {
			    		mPrimary = false;
			    	}else{
			    		mPrimary = true;
			    	}
			    	mDataNetworkController = DataNetworkController.getInstance(mContext, mPrimary);
            }
		}
    };
		
    //mobile_data or wlan enabled
    public void setNetStartShow(boolean start){
    	if (start) {
    		mMyHandler.setDoAnimation(true);
        } else {
        	mMyHandler.setDoAnimation(false);
        }
    }
    
    void updateViews(){
    	String str = "0k/s";
    	boolean mCurrentNetState = getNetworkEnabled();
    	DecimalFormat decimalFormat = new DecimalFormat("0.00");
//    	Log.d("","mCurrentNetState="+mCurrentNetState);
    	if(mOldNetState != mCurrentNetState){
    		mOldNetState = mCurrentNetState;
    		
    		if(true == mCurrentNetState){
    			mTextView.setVisibility(View.VISIBLE);
    			
    			mNowFlow = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
        		long speed = (mNowFlow - mLastFlow) * 1000 / mSleepTime;
        		float fSpeed = speed / (1024.00f);
        		str = decimalFormat.format(fSpeed) + "k/s";
    			mLastFlow = mNowFlow;
    		}else{
    			mTextView.setVisibility(View.GONE);
    		}
    	}else if(true == mCurrentNetState){
    		mNowFlow = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
    		long speed = (mNowFlow - mLastFlow) * 1000 / mSleepTime;
    		float fSpeed = speed / (1024.00f);
    		str = decimalFormat.format(fSpeed) + "k/s";
			mLastFlow = mNowFlow;
    	}
    	
    	mTextView.setText(str);
    	invalidate();
    }
    
    private MyHandler mMyHandler = new MyHandler();
    private class MyHandler extends Handler {
    	private boolean isAnimating = false;
    	
    	public void setDoAnimation(boolean isStart){
    		if (isStart){
    			if (!isAnimating){
    				sendEmptyMessage(0);
    			}
    			isAnimating = true;
    		} else {
    			isAnimating = false;
    			removeMessages(0);
    		}
    	}
    	public void handleMessage(Message msg) {
    		updateViews();
    		sendEmptyMessageDelayed(0, mSleepTime);
    	}
    }
    
    private final class CObserver extends ContentObserver{
		public CObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			onPropertyChange();
			super.onChange(selfChange);
		}
		
		public void startObserving() {
			CurrentUserTracker.registerContentObserver(SHOW_NET_SPEED,false,this);
			onPropertyChange();
		}
	}
    
   

    /**
     *  The value of SHOW_NET_SPEED://==1;//
     *  0----switch close
     *  1----switch open, default state
     */
    private void onPropertyChange(){
        boolean show = CurrentUserTracker.getIntForCurrentUser(SHOW_NET_SPEED, 0) ==1;
        
		LogHelper.sd(TAG, "onPropertyChange show = " + show);
		
		mLastFlow = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
		if(show  && getNetworkEnabled()){
			mTextView.setVisibility(View.VISIBLE);
		}else{
			mTextView.setVisibility(View.GONE);
		}
		if(show){
			setNetStartShow(true);
		}else{
			setNetStartShow(false);
		}
		LogHelper.sd(TAG,"mDataEnabled="+mDataEnabled+" mWifiEnabled="+mWifiEnabled);
	}
    
    private boolean getNetworkEnabled(){
        if(mDataNetworkController != null){
        	 boolean enable = (Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0)==1);
    		mDataEnabled = mDataNetworkController.getMobiledataEnabled();
    		mDataEnabled = mDataEnabled && !enable;
		}
//        Log.d("","getNetworkEnabled mDataEnabled="+mDataEnabled+" mWifiEnabled="+mWifiEnabled);
       
		return (mDataEnabled || mWifiEnabled);
	}
}
