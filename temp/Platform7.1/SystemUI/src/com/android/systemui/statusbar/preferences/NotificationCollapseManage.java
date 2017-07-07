package com.android.systemui.statusbar.preferences;

import java.util.Map;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.misc.YLUtils;
import com.android.systemui.recents.misc.YLUtils.SettingsDB;
import com.android.systemui.statusbar.phone.MultiUserSwitch;

public class NotificationCollapseManage {
	static private NotificationCollapseManage self = null;
	private static final String DB_VERSION_STR = "DB_VERSION";
	private static final String DB_DEFAULT_VERSION = "12";
	private static Context mContext;
	static public NotificationCollapseManage getDefault(Context context){
		if (self == null){
			mContext = context;
			self = new NotificationCollapseManage(context);
			
			String DB_CUR_VERSION = DB_DEFAULT_VERSION;//android.os.Build.DISPLAY;
			String db_version = NotificationCollapseManage.getDefault(context).getDbVersion();
			if(!db_version.equalsIgnoreCase(DB_CUR_VERSION)){
				LogHelper.sd("","getDefaultSharedPreferences db_version!=DB_CUR_VERSION");
				//PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit(); 
				NotificationCollapseManage.getDefault(context).setDbVersion(DB_CUR_VERSION);
			}
			LogHelper.sd("","getDefaultSharedPreferences db_version="+db_version+
					" DB_CUR_VERSION="+DB_CUR_VERSION);
		}
		return self;
	}
	private SharedPreferences mShareData;
	private SharedPreferences.Editor mShareDataEditor;

	private NotificationCollapseManage(Context context){
        PreferenceManager.setDefaultValues(context,R.xml.notification_list,false);
		mShareData = PreferenceManager.getDefaultSharedPreferences(context);//context.getSharedPreferences("NotificationCollapse", Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);
		mShareDataEditor=mShareData.edit();
	}
	public void setPkgCollapse(String pkg,boolean bCollapse){
		mShareDataEditor.putInt(pkg, bCollapse?1:0);
		mShareDataEditor.commit();
	}
	public void removePrivateSpaceSharePreference(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				Map<String, ?> map = mShareData.getAll();
				int i = 0;
				for (Map.Entry<String, ?> entry : map.entrySet()) {
					String key = entry.getKey();
					if (key.endsWith("_private")) {
						mShareDataEditor.remove(key);
						i++;
//						LogHelper.sd("", "11getDefaultSharedPreferences i=" + i
//								+ " key=" + key + " val=" + entry.getValue());
					}
				}
				mShareDataEditor.commit();
			}
		}).start();
	}
	public boolean getPkgCollapse(String pkg){
		return mShareData.getInt(pkg, 1)==1 ? true:false;
	}
	public boolean getNotificationKeyValue(String packageName){
		boolean b1 = false;
		boolean b2 = false;
		if(false && Utilities.isSupportOversea()){
			b1 = true;
		}else{
			b1 = SysAppProvider.getInstance(null).isNotifiyEnable(packageName);
		}
		b2 = mShareData.getBoolean(getNotificationKey(packageName), b1);
		return b2;
	}
	public boolean getStatusBarKeyValue(String packageName){
		boolean b1 = false;
		boolean b2 = false;
		if(false && Utilities.isSupportOversea()){
			b1 = true;
		}else{
			b1 = SysAppProvider.getInstance(null).isNotifiyShowInStatusbar(packageName);
		}
		b2 = mShareData.getBoolean(getStatusBarKey(packageName), b1);
		return b2;
	}
	public boolean getOtherNotificationKeyValue(String packageName){
		boolean b1 = false;
		boolean b2 = false;
		if(false && Utilities.isSupportOversea()){
			b1 = false;
		}else{
			b1 = SysAppProvider.getInstance(null).isNotifiyShowInOther(packageName);
		}
		b2 = mShareData.getBoolean(getOtherNotificationKey(packageName), b1);
		return b2;
	}
	
	public boolean getFloatingNotificationKeyValue(String packageName){
		boolean defFloatingValue = SysAppProvider.getInstance(null).isNotifiyShowInFloating(packageName);
		return mShareData.getBoolean(getFloatingNotificationKey(packageName),defFloatingValue);
	}
	
	public boolean getLocksreenNotificationKeyValue(String packageName){
		if (false && Utilities.isSupportOversea()) {
			return true;
		}
		boolean defValue = SysAppProvider.getInstance(null).isNotifiyShowInLocksreen(packageName);
		return mShareData.getBoolean(getLocksreenNotificationKey(packageName),defValue);
	}
	
	public boolean getDetailNotificationKeyValue(String packageName){
		boolean defValue = SysAppProvider.getInstance(null).isNotifiyShowInDetail(packageName);
		return mShareData.getBoolean(getDetailNotificationKey(packageName),defValue);
	}
	public boolean getDndKeyValue(String packageName){
		boolean defValue = SysAppProvider.getInstance(null).isNotifiyShowInDnd(packageName);
		return mShareData.getBoolean(getDndKey(packageName),defValue);
	}
	
	public boolean getMasterNotificationKeyValue(){
		boolean defValue = true;
		return mShareData.getBoolean(getMasterNotificationKey(),defValue);
	}
	
	public static String getNotificationKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_notification";
		} else {
			return packageName + "_notification_private";
		}
	}
	public static String getStatusBarKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_statubar";
		} else {
			return packageName + "_statubar_private";
		}
	}
	public static String getOtherNotificationKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_other";
		} else {
			return packageName + "_other_private";
	}	
	}
	public static String getFloatingNotificationKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_floating";
		} else {
			return packageName + "_floating_private";
		}
	}
	public static String getLocksreenNotificationKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_locksreen";
		} else {
			return packageName + "_locksreen_private";
		}
	}
	public static String getDetailNotificationKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_detail";
		} else {
			return packageName + "_detail_private";
		}
	}
	public static String getMasterNotificationKey(){
		if (Utilities.isPrimaryUser()) {
		return "master_statusbar";
		} else {
			return "master_statusbar_private";
		}
	}
	public static String getDndKey(String packageName){
		if (Utilities.isPrimaryUser()) {
		return packageName + "_dnd";
		} else {
			return packageName + "_dnd_private";
		}
	}
	public void setNotifiyEnable(String pkg, Boolean bNotify) {
		String mKeyString_1 = NotificationCollapseManage
				.getNotificationKey(pkg);
		mShareDataEditor.putBoolean(mKeyString_1, bNotify);
		mShareDataEditor.commit();
		YLUtils.putIntForAllUser(mContext, SettingsDB.SECURE, mKeyString_1, bNotify?1:0);
	}
	public void setDbVersion(final String curVersion){
		mShareDataEditor.putString(DB_VERSION_STR, curVersion);
		mShareDataEditor.commit();
	}
	public String getDbVersion(){
		try {
			return mShareData.getString(DB_VERSION_STR, "0");
		} catch (Exception e) {
			// TODO: handle exception
			LogHelper.sd("","getString error");
		}
		return "";
	}
	public boolean isSystemNotification(String sbnPackage){
		if (sbnPackage == null) {
			return false;
		}
		return "android".equalsIgnoreCase(sbnPackage)
		  		|| sbnPackage.startsWith("com.android.") 
		  		|| sbnPackage.startsWith("com.yulong.")
		  		|| sbnPackage.startsWith("com.ivvi.")
//		  		|| sbnPackage.startsWith("com.icoolme.")
		  		|| sbnPackage.startsWith("com.securespaces.")
		  		|| sbnPackage.startsWith("com.coolpad.")
		  		|| "com.ivvi.android.appstore".equalsIgnoreCase(sbnPackage)
		  		|| "com.redstone.ota.ui".equalsIgnoreCase(sbnPackage)
//		  		|| sbnPackage.startsWith("com.tencent.")
//		  		|| sbnPackage.startsWith("com.baidu.")
//		  		|| sbnPackage.startsWith("com.google.")
//		  		|| sbnPackage.startsWith("com.qiku.") 
//		  		|| sbnPackage.startsWith("com.qihoo") 
//		  		|| sbnPackage.startsWith("com.kugou.") 
//		  		|| sbnPackage.startsWith("com.youku.") 
		  		|| "com.tencent.mm".equalsIgnoreCase(sbnPackage) 
		  		|| "com.tencent.mobileqq".equalsIgnoreCase(sbnPackage)
		  		|| "com.baidu.BaiduMap".equalsIgnoreCase(sbnPackage)
		  		|| "com.google.android.apps.maps".equalsIgnoreCase(sbnPackage)
		  		|| "com.google.android.gm".equalsIgnoreCase(sbnPackage)
		  		|| "com.UCMobile".equalsIgnoreCase(sbnPackage)
		  		|| "com.UCMobile.intl.coolpad".equalsIgnoreCase(sbnPackage)
		  		|| "com.UCMobile.intl.ivvi".equalsIgnoreCase(sbnPackage)
		  		|| "com.quicinc.fmradio".equalsIgnoreCase(sbnPackage)
		  		|| "com.mediatek.mtklogger".equalsIgnoreCase(sbnPackage)
		  		|| "com.sina.weibo".equalsIgnoreCase(sbnPackage)
		  		|| "com.immomo.momo".equalsIgnoreCase(sbnPackage)
		  		|| "com.whatsapp".equalsIgnoreCase(sbnPackage);
	}
	public boolean isUsedSelfNotificationPng(String sbnPackage){
		if (sbnPackage == null) {
			return false;
		}
		return "android".equalsIgnoreCase(sbnPackage)
		  		|| "com.android.mms".equalsIgnoreCase(sbnPackage) 
		  		|| "com.yulong.coolmessage".equalsIgnoreCase(sbnPackage)
		  		|| "com.android.server.telecom".equalsIgnoreCase(sbnPackage)
		  		|| "com.android.dialer".equalsIgnoreCase(sbnPackage)
		  		|| "com.android.incallui".equalsIgnoreCase(sbnPackage)
		  		|| "com.android.phone".equalsIgnoreCase(sbnPackage)
		  		|| "com.android.systemui".equalsIgnoreCase(sbnPackage)
//		  		|| "com.yulong.android.screenshot".equalsIgnoreCase(sbnPackage) 
//		  		|| "com.yulong.android.coolfunscreenshot".equalsIgnoreCase(sbnPackage) 
//		  		|| "com.yulong.android.security".equalsIgnoreCase(sbnPackage) 
//		  		|| "com.android.usbui".equalsIgnoreCase(sbnPackage) 
//		  		|| "com.yulong.android.calendar".equalsIgnoreCase(sbnPackage) 
//		  		|| "com.android.screenrecord".equalsIgnoreCase(sbnPackage) 
		  		|| "com.icoolme.android.weather".equalsIgnoreCase(sbnPackage);
	}
}
