package com.android.systemui;

import java.lang.reflect.Method;
import java.util.ArrayList;

import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ISub;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityOptions.OnAnimationStartedListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.service.notification.Condition;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.View;
/*zhouxiaobing add for 5.0 5.1 jianrong*/
public class YLAdapter {
	public static int sdk = android.os.Build.VERSION.SDK_INT;
	public static final int REMOVE_TASK_KILL_PROCESS = 0x0001;
	public static Condition toTimeCondition(Context context, int minutesFromNow, int userHandle) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.service.notification.ZenModeConfig");
				Method method = zenmode.getMethod("toTimeCondition", Context.class, int.class, int.class);
				return (Condition) method.invoke(null, context, minutesFromNow, userHandle);
				
			}else {
				Class<?> zenmode = Class.forName("android.service.notification.ZenModeConfig");
				Method method = zenmode.getMethod("toTimeCondition",  int.class);
				return (Condition) method.invoke(null,  minutesFromNow);				
			}
		}catch(Exception e){
			
		}
		return null;
	}

	public static Condition toTimeCondition(Context context, long time, int minutes, long now,
            int userHandle) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.service.notification.ZenModeConfig");
				Method method = zenmode.getMethod("toTimeCondition", Context.class, long.class, int.class, long.class, int.class);
				return (Condition) method.invoke(null, context, time, minutes, now, userHandle);
				
			}else {
				Class<?> zenmode = Class.forName("android.service.notification.ZenModeConfig");
				Method method = zenmode.getMethod("toTimeCondition", long.class, int.class);
				return (Condition) method.invoke(null,  time, minutes);				
			}
		}catch(Exception e){
			
		}
		return null;
	}
	
	public static void setSystemUiVisibility(IStatusBarService mBarService, int vis, int mask, String cause) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("com.android.internal.statusbar.IStatusBarService");
				Method method = zenmode.getMethod("setSystemUiVisibility", int.class, int.class, String.class);
				method.invoke(mBarService, vis, mask, cause);
				
			}else {
				Class<?> zenmode = Class.forName("com.android.internal.statusbar.IStatusBarService");
				Method method = zenmode.getMethod("setSystemUiVisibility", int.class, int.class);
				method.invoke(mBarService,  vis, mask);				
			}
		}catch(Exception e){
			
		}		
	}
	
	public static int getPhoneId(int subid) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.telephony.SubscriptionManager");
				Method method = zenmode.getMethod("getPhoneId", int.class);
				return (Integer)method.invoke(null, subid);
				
			}else {
				Class<?> zenmode = Class.forName("android.telephony.SubscriptionManager");
				Method method = zenmode.getMethod("getPhoneId", long.class);
				return (Integer)method.invoke(null,  (long)subid);				
			}
		}catch(Exception e){
			
		}		
		return 0;
	}
	
	public static ActivityOptions makeThumbnailAspectScaleUpAnimation(View source,
            Bitmap thumbnail, int startX, int startY, int targetWidth, int targetHeight,
            Handler handler, OnAnimationStartedListener listener) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.app.ActivityOptions");
				Method method = zenmode.getMethod("makeThumbnailAspectScaleUpAnimation", View.class,
						Bitmap.class, int.class, int.class, int.class, int.class, Handler.class,
						OnAnimationStartedListener.class);
				return (ActivityOptions)method.invoke(null, source, thumbnail, startX, startY, targetWidth,
						targetHeight, handler, listener);
				
			}else {
				Class<?> zenmode = Class.forName("android.app.ActivityOptions");
				Method method = zenmode.getMethod("makeThumbnailAspectScaleUpAnimation", View.class,
						Bitmap.class, int.class, int.class, int.class, int.class, 
						OnAnimationStartedListener.class);
				return (ActivityOptions)method.invoke(null, source, thumbnail, startX, startY, targetWidth,
						targetHeight, listener);			
			}
		}catch(Exception e){
			
		}		
		return null;	
	}
	
	public static boolean removeTask(ActivityManager am, int taskId, int flags){
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.app.ActivityManager");
				Method method = zenmode.getMethod("removeTask", int.class);
				return (Boolean)method.invoke(am, taskId);
				
			}else {
				Class<?> zenmode = Class.forName("android.app.ActivityManager");
				Method method = zenmode.getMethod("removeTask", int.class, int.class);
				return (Boolean)method.invoke(am, taskId, flags);		
			}
		}catch(Exception e){
			
		}		
		return false;
    }
	
	public static boolean removeTask(ActivityManager am, ArrayList<Integer> taskIds, int flags){
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.app.ActivityManager");
				Method method = zenmode.getMethod("removeTask", int.class);
				for (int i = 0; i < taskIds.size(); i++) {
					method.invoke(am, taskIds.get(i));
				}
				return true;
				
			}else {
				Class<?> zenmode = Class.forName("android.app.ActivityManager");
				Method method = zenmode.getMethod("removeTask", int.class, int.class);
				for (int i = 0; i < taskIds.size(); i++) {
					method.invoke(am, taskIds.get(i), flags);
				}
				return true;		
			}
		}catch(Exception e){
			
		}		
		return false;
    }
	
	public static ActivityOptions makeThumbnailAspectScaleDownAnimation(View source,
            Bitmap thumbnail, int startX, int startY, int targetWidth, int targetHeight,
            Handler handler, OnAnimationStartedListener listener) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.app.ActivityOptions");
				Method method = zenmode.getMethod("makeThumbnailAspectScaleDownAnimation", View.class,
						Bitmap.class, int.class, int.class, int.class, int.class, Handler.class,
						OnAnimationStartedListener.class);
				return (ActivityOptions)method.invoke(null, source, thumbnail, startX, startY, targetWidth,
						targetHeight, handler, listener);
				
			}else {
				Class<?> zenmode = Class.forName("android.app.ActivityOptions");
				Method method = zenmode.getMethod("makeThumbnailAspectScaleDownAnimation", View.class,
						Bitmap.class, int.class, int.class, int.class, int.class, 
						OnAnimationStartedListener.class);
				return (ActivityOptions)method.invoke(null, source, thumbnail, startX, startY, targetWidth,
						targetHeight, listener);			
			}
		}catch(Exception e){
			
		}		
		return null;	
	}
	
	public static int[] getSubId(int slotId) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.telephony.SubscriptionManager");
				Method method = zenmode.getMethod("getSubId", int.class);
				Object object[] = (Object[]) method.invoke(null, slotId);
				int[] subids = new int[object.length];
				for (int i = 0; i < object.length; i++) {
					subids[i] = (int) object[i];
				}
				return subids;
				
			}else {
				Class<?> zenmode = Class.forName("android.telephony.SubscriptionManager");
				Method method = zenmode.getMethod("getSubId", int.class);
				Object object[] = (Object[]) method.invoke(null, slotId);
				int[] subids = new int[object.length];
				for (int i = 0; i < object.length; i++) {
					subids[i] = (int) object[i];
				}
				return subids;			
			}
		}catch(Exception e){
			
		}	
		return null;
	}
	
   public static String getNetworkOperator(TelephonyManager tm, int subId) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("android.telephony.TelephonyManager");
				Method method = zenmode.getMethod("getNetworkOperatorForSubscription", int.class);
				return (String)method.invoke(tm, subId);
				
			}else {
				Class<?> zenmode = Class.forName("android.telephony.TelephonyManager");
				Method method = zenmode.getMethod("getNetworkOperator", int.class);
				return (String)method.invoke(tm, subId);		
			}
		}catch(Exception e){
			
		}		
		return "";
        
     }
   
   public static void setDefaultDataSubId(int subId) {
       try {
           ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
           if (iSub != null) {
               iSub.setDefaultDataSubId(subId);
           }
       } catch (RemoteException ex) {
           // ignore it
       }
   }
   
   public static void onPanelRevealed(IStatusBarService mBarService, boolean clearNotificationEffects) {
		try{
			if (sdk > 21) {
				Class<?> zenmode = Class.forName("com.android.internal.statusbar.IStatusBarService");
				Method method = zenmode.getMethod("onPanelRevealed", boolean.class);
				method.invoke(mBarService, clearNotificationEffects);				
			}else {
				Class<?> zenmode = Class.forName("com.android.internal.statusbar.IStatusBarService");
				Method method = zenmode.getMethod("onPanelRevealed");
				method.invoke(mBarService);	
			}
		}catch(Exception e){
			
		}	
   }
   
   public static boolean isStrictEnable() {
		try{
			if (sdk > 21) {
				return false;
			}else {
				Class<?> zenmode = Class.forName("android.app.AppOpsManager");
				Method method = zenmode.getMethod("isStrictEnable");
				return (Boolean)method.invoke(null);	
			}
		}catch(Exception e){
			
		}
		return false;
   }
   
   public static int getAlternateLteLevel(SignalStrength mSignalStrength) {
		try{
			if (sdk > 21) {
				return 0;
			}else {
				Class<?> zenmode = Class.forName("android.telephony.SignalStrength");
				Method method = zenmode.getMethod("getAlternateLteLevel");
				return (Integer)method.invoke(mSignalStrength);	
			}
		}catch(Exception e){
			
		}
		return 0;	   
   }

   public static int getTdScdmaLevel(SignalStrength mSignalStrength) {
		try{
			String platform = SystemProperties.get("ro.board.platform", "android");
			if (platform.indexOf("mt") >= 0 || platform.indexOf("MT") >= 0) {
				Class<?> zenmode = Class.forName("android.telephony.SignalStrength");
				Method method = zenmode.getMethod("getCdmaLevel");
				return (Integer)method.invoke(mSignalStrength);	
			}else {
				Class<?> zenmode = Class.forName("android.telephony.SignalStrength");
				Method method = zenmode.getMethod("getTdScdmaLevel");
				return (Integer)method.invoke(mSignalStrength);	
			}
		}catch(Exception e){
			
		}
		return 0;	   
   }
   
}
