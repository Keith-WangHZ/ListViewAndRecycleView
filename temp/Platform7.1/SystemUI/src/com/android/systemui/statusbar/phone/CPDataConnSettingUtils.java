package com.android.systemui.statusbar.phone;

import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.telephony.TelephonyManager.SIM_STATE_READY;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.systemui.helper.LogHelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public class CPDataConnSettingUtils {
	private static final String TAG = "CPDataConnSettingUtils";

	public static boolean isCardPresent(int phoneId){
		try{
			Phone phone = PhoneFactory.getPhone(phoneId == 0 ? PhoneConstants.SIM_ID_1 : PhoneConstants.SIM_ID_2);
			if(phone != null ){
				 UiccCard uicc = phone.getUiccCard();
				 if(uicc != null){
					 CardState state = uicc.getCardState();
					 if(state != null){
						 return 0==state.compareTo(state.CARDSTATE_PRESENT)?true:false;
						 //===modify by ty
						 //return state.isCardPresent();
					 }
				 }
			}			
		}catch(Exception e){}
		return true;
	}
	
    public static boolean isMobileDataSupported(int phoneId) {
    	try{
			Phone phone = PhoneFactory.getPhone(phoneId);
			if(phone != null ){
				return phone.isDataConnectivityPossible();
			}
    	}catch(Exception e){}
		return false;
    }
    
    public static boolean isMobileDataSupported(Context context) {
        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        ConnectivityManager mConnectivityManager = ConnectivityManager.from(context);
        
        boolean support = mConnectivityManager.isNetworkSupported(TYPE_MOBILE)
                && mTelephonyManager.getSimState() == SIM_STATE_READY;
        // require both supported network and ready SIM
        LogHelper.sd(TAG, " isMobileDataSupported = " + support);
        return support;
    }

//    public static boolean isMobileDataEnabled(Context context) {
//        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
//        if(mTelephonyManager != null){
//        	try{
//        		return mTelephonyManager.getDataEnabled();
//        	}catch(Exception e){}
//        }
//        return false;
//    }
    
    public static void setMobileDataEnabled(Context context,boolean enabled) {
        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        if(mTelephonyManager != null){
//        	mTelephonyManager.setDataEnabled(enable);
//            for (int i = 0; i < mTelephonyManager.getPhoneCount(); i++) {
//                long[] subId = SubscriptionManager.getSubId(i);
//                if(subId != null && subId[0] > 0){
//                	mTelephonyManager.setDataEnabledUsingSubId(subId[0], enable);
//                	LogHelper.sd(TAG, "setMobileDataEnabled setDataEnabledUsingSubId phoneId:" + i + " subId[0]:" + subId[0]);
//                }
//            }
            mTelephonyManager.setDataEnabled(enabled);
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.MOBILE_DATA, (enabled) ? 1 : 0);
            int phoneCount = mTelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.MOBILE_DATA + i,
                        (enabled) ? 1 : 0);
                LogHelper.sd(TAG, "setMobileDataEnabled phoneId" + i + " enable:" + enabled);
            }       	
        }
        LogHelper.sd(TAG, "setMobileDataEnabled mTelephonyManager:" + mTelephonyManager + " enable:" + enabled);
    }    
    
    public static  boolean getMobileDataEnabled(Context context) {
    	TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        boolean isDataEnabled = true;
//        for (int i = 0; i < mTelephonyManager.getPhoneCount(); i++) {
//        	String mobile = Settings.Global.MOBILE_DATA + i;
//        	int mobileValue = Settings.Global.getInt(context.getContentResolver(),mobile,0);
//            isDataEnabled = isDataEnabled && (mobileValue == 1 ? true : false);                 
//            LogHelper.sd(TAG, "getMobileDataEnabled " + mobile + ":" + mobileValue);     
        if(mTelephonyManager != null){
        	isDataEnabled = mTelephonyManager.getDataEnabled();
        }
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "getMobileDataEnabled:" + isDataEnabled);     
        return isDataEnabled;
    }

    public static int getSimState(Context context,int phoneId){
        TelephonyManager mTelephonyManager = TelephonyManager.from(context);
        if(mTelephonyManager != null)
        	return mTelephonyManager.getSimState(phoneId);
        return TelephonyManager.SIM_STATE_UNKNOWN;    	
    }
	
	public static int getSubId(int phoneId){
		int[] subIds = SubscriptionManager.getSubId(phoneId);
		int subId = 0;
		if(subIds != null && subIds.length > 0)
			subId = subIds[0];
		if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "getSubId phoneId:" + phoneId + " subId:" + subId);
		return subId;
	}	
	public static int getDefaultDataNetwork(Context mContext) {
		int subId = SubscriptionManager.getDefaultDataSubscriptionId();//===modify by ty
		int phoneId = SubscriptionManager.getPhoneId(subId);
		if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "getDefaultDataNetwork getPhoneId:" + phoneId + " SubscriptionManager.getDefaultDataSubId():" + subId);
		return phoneId;
	}
	public static void setDefaultDataNetwork(Context mContext, int phoneId) {
		// TODO Auto-generated method stub
		int[] subIds = SubscriptionManager.getSubId(phoneId);
		int subId = 0;
		if(subIds != null && subIds.length > 0)
			subId = subIds[0];
		LogHelper.sd(TAG, "setDefaultDataNetwork phoneId:" + phoneId + " subId:" + subId);
		//SubscriptionManager.setDefaultDataSubId(subId);
		SubscriptionManager.from(mContext).setDefaultDataSubId(subId);
	}
	public static int getValidCardNum() {
		int validCardNum = SubscriptionManager.getDefaultDataSubscriptionId();
		if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "getValidCardNum SubscriptionManager.getActiveSubInfoCount():" + validCardNum);		
		return validCardNum;
	}
	private static final String PreferredapnUsingSubId =
	        "content://telephony/carriers/preferapn/subId/";	
	private static final String setPreferredapnUsingSubId =
	        "content://telephony/carriers/preferapn_no_update/subId/";	
	public static final String APN_ID = "apn_id";
	public static boolean setPreferApnById(Context context, String ApnId, long subId){
	    if(ApnId != null){
	        ContentResolver resolver = context.getContentResolver();
	        ContentValues values = new ContentValues();
	        Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID =
	                Uri.parse(PreferredapnUsingSubId + subId);
	        values.put(APN_ID, ApnId);
	        resolver.update(PREFERAPN_NO_UPDATE_URI_USING_SUBID, values, null, null);
	        LogHelper.sd(TAG, " setPreferApnById :" + PREFERAPN_NO_UPDATE_URI_USING_SUBID + " values:" + values);
	        return true;            
	    }
	    return false;     
	}
	public static final String URL_PREFERAPN = "telephony/carriers/preferapn";
	public static final String URL_PREFERAPN_USING_SUBID = "telephony/carriers/preferapn/subId/*";//*浠ｈ〃subId
	public static final String CARRIERS_URI = "content://telephony/carriers";	
}
