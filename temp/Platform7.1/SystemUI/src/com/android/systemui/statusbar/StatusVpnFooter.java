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
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.phone.YulongConfig;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DataNetworkController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.securespaces.android.ssm.UserUtils;

public class StatusVpnFooter extends LinearLayout {
	private final static String TAG = "StatusVpnFooter";
	private SecurityController mSecurityController;
	private final Callback mCallback = new Callback();

    public StatusVpnFooter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public StatusVpnFooter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        refreshState();
    }
    
    private class Callback implements SecurityController.SecurityControllerCallback {
        @Override
        public void onStateChanged() {
            refreshState();
        }
    }
    
    public void setListening(boolean listening) {
    	if(mSecurityController!=null){
    		if (listening) {
                mSecurityController.addCallback(mCallback);
            } else {
                mSecurityController.removeCallback(mCallback);
            }
    	}
        
    }
    
    public void refreshState() {
    	post(new Runnable() {
            @Override
            public void run() {
            	boolean mVpnVisible = false;
            	if(mSecurityController != null){
            		mVpnVisible = mSecurityController.isVpnEnabled();
            	}
            	if (mVpnVisible) {
                   setVisibility(View.VISIBLE);
                }else{
                   setVisibility(View.GONE);
                }
                LogHelper.sd(TAG,"onStateChanged mVpnVisible="+mVpnVisible);
            }
        });
    	
    }
    
    public void setSecurityController(SecurityController securityController) {
        mSecurityController = securityController;
    }
    
    @Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		setListening(true);
		
	}
    
    @Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		setListening(false);
	}
    
}
