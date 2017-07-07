package com.android.systemui.recents;

import java.util.Arrays;
import java.util.HashSet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;

import com.android.systemui.helper.LogHelper;

public class YLSecurityManager {
    private static final String TAG = "YLSecurityManager";
    public static final String REQUEST_SECURITY_ACTION = "yulong.intent.action.UPDATE_APP_LOCKED_REQUEST";
    public static final String SECURITY_CHANGED_ACTION = "yulong.intent.action.UPDATE_APP_LOCKED";
    HashSet<String> mSecurityAppSet = new HashSet<String>();
    Bitmap mBitmap;
    static YLSecurityManager pthis;
    Context mContext;
    SecurityReceiver mSecurityReceiver;
    private YLSecurityManager() {
        mSecurityReceiver = new SecurityReceiver();
    }
    public static YLSecurityManager createInstance(Context context) {
        pthis = new YLSecurityManager();
        pthis.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(SECURITY_CHANGED_ACTION);
        context.registerReceiver(pthis.mSecurityReceiver, filter);
        LogHelper.sd(TAG, "YLSecurityManager createInstance pthis = " + pthis);
        return pthis;
    }
    
    public void sendBroadcastForRequest(){
    	mContext.sendBroadcast(new Intent(REQUEST_SECURITY_ACTION));
    }
    public static YLSecurityManager getInstance() {
        LogHelper.sv(TAG, "YLSecurityManager getInstance pthis = " + pthis);
        return pthis;
    }
    public void onDestory() {
        mContext.unregisterReceiver(mSecurityReceiver);
        LogHelper.sv(TAG, "YLSecurityManager onDestory pthis = " + pthis);
        YLSecurityManager.releasePthis();
    }
    private static void releasePthis() {
    	YLSecurityManager.pthis = null;
    }
    public class SecurityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
           
            try {
                String action = intent.getAction();

                if (action.equals(SECURITY_CHANGED_ACTION)) {
                	String[] packages = intent.getStringArrayExtra("UPDATE_APP_LOCKED");
                	LogHelper.sd(TAG, " UPDATE_APP_LOCKED == " + Arrays.toString(packages));
                    mSecurityAppSet.clear();
                    if(packages != null) {
                        for(String str:packages) {
                        	if(str.equals("")) {
                        		continue;
                        	}
                            mSecurityAppSet.add(str);
                        } 
                    }
                }
            } catch (Exception e) {
                LogHelper.se(TAG, "e == " + e);
                e.printStackTrace();
            }
        }
    }

    public boolean isSecurity(String key) {
    	boolean ret = this.mSecurityAppSet.contains(key);
    	LogHelper.sd(TAG, "isSecurity pkg:" + key + " ret:" + ret);
        return ret;
    }

}
