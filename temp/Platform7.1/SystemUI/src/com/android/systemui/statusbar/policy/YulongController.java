/**
 * zhangyang1
 * 2012-09-04
 */

package com.android.systemui.statusbar.policy;



import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.YulongConfig;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
public class YulongController {
    public static final int HEADSET_NOTIFY_ID = 100;
    private static final String TAG = "YulongController";
    private Context mContext;  
    private static double mLaunchPrivateModeMinVelocity;
    private static double mLaunchPrivateModeMinDistance;
    private static YulongController sDefaultYulongController;

    public static YulongController getDefault(){
        return sDefaultYulongController;
    }

    public YulongController(Context context,DisplayMetrics dm, int statusHeight) {    
        mContext = context;      
        calculatePrivateModeParams(dm);
        sDefaultYulongController = this;
    }

    public static boolean tryLaunchPrivateMode(Context context, int vel, int distance){
    	
    	if (true || !YulongConfig.isPrivateModeVailidate())
    		return false;
    	       StatusBarManager statusBar = (StatusBarManager) context
                .getSystemService(Context.STATUS_BAR_SERVICE);
        LogHelper.sd(TAG, "tryLaunchPrivateMode vel = " + vel
                + " distance = " + distance
                + " mLaunchPrivateModeMinVelocity = " + mLaunchPrivateModeMinVelocity
                + " mLaunchPrivateModeMinDistance = " + mLaunchPrivateModeMinDistance);
        if (vel > mLaunchPrivateModeMinVelocity && distance > mLaunchPrivateModeMinDistance){
        	LogHelper.sd(TAG,"...............tryLaunchPrivateMode is done");
            try {
                CurrentUserTracker.sendBroadcastAsCurrentUser(new Intent("yulong.intent.action.LAUNCH_PRIVATE_MODE"));
                   if (statusBar != null) {
                       statusBar.collapsePanels();
                    }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private void calculatePrivateModeParams(DisplayMetrics dm){
        int height = Math.max(dm.widthPixels, dm.heightPixels);
        if (height == 1280){
            mLaunchPrivateModeMinVelocity = 6000;
            mLaunchPrivateModeMinDistance = 600;
        } else if (height == 960){
            mLaunchPrivateModeMinVelocity = 4800;
            mLaunchPrivateModeMinDistance = 450;
        } else if (height == 800){
            mLaunchPrivateModeMinVelocity = 4000;
            mLaunchPrivateModeMinDistance = 348;
        } else if (height == 480){
            mLaunchPrivateModeMinVelocity = 2400;
            mLaunchPrivateModeMinDistance = 230;
        }else if(height == 1920){ 
        	  mLaunchPrivateModeMinVelocity = 9600;
            mLaunchPrivateModeMinDistance = height / 2.5;
         }else{
            mLaunchPrivateModeMinDistance = height / 1.5;
            mLaunchPrivateModeMinVelocity = mLaunchPrivateModeMinDistance * 12;
        }
    }
    
}
