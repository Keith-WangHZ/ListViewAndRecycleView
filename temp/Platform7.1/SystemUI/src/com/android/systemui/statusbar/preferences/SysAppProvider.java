package com.android.systemui.statusbar.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.preferences.XmlDataBean;
import com.android.systemui.statusbar.preferences.XmlReaderModule;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class SysAppProvider {	
	static private SysAppProvider self = null;
	static public synchronized SysAppProvider getInstance(final Context context){
		if (self == null){
			self = new SysAppProvider(context);
			
			new AsyncTask<Void, Void, Integer>() {
    			@Override
    			protected Integer doInBackground(Void... args) {
    				initNtfConfigList(context);
    				return 0;
    			}
    		}.execute();
		}
		return self;
	}
	private SysAppProvider(Context context){
	}
	static private Map<String,Boolean> mapSysPackage = new HashMap<String,Boolean>();
	static private Map<String,Integer> mapPackageIcon = new HashMap<String,Integer>();
	
	
    public void mapAllSysPackage(Context context){
    	if (mapSysPackage.size() > 0)
    		return;
        List<PackageInfo> packageInfos =
        		context.getPackageManager().getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for(PackageInfo packageInfo:packageInfos){
            String packageName = packageInfo.packageName;
            if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            		|| (packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0){
            	mapSysPackage.put(packageName, true);
            }
            mapPackageIcon.put(packageName, packageInfo.applicationInfo.icon);
        }
        initNtfConfigList(context);
    }
    private static final String NTF_CONFIG_FILE_NAME = "seccenter_notice_applist.xml";
    private static XmlDataBean bean;
    private static XmlReaderModule xmlReader;
    private static Context mContext;
    private static Boolean mInitActual = false;
    private static String TAG = "SysAppProvider";
	public synchronized static Boolean initNtfConfigList(Context context) {
		
		if(xmlReader == null){
			xmlReader =  XmlReaderModule.getInstance(NTF_CONFIG_FILE_NAME);
		}
//		int DB_VERSION = 11;
//		int db_version = NotificationCollapseManage.getDefault(context).getDbVersion();
//		if(db_version != DB_VERSION){
//			NotificationCollapseManage.getDefault(context).setDbVersion();
//		}
		if(bean == null && xmlReader!=null){
		bean = xmlReader.getXmlDatas(context);
		}
		if(xmlReader == null){
		    LogHelper.sv("SysAppProvider","initNtfConfigList : bean == null");
		    return false;
		}else if(bean == null){
			 LogHelper.sv("SysAppProvider","initNtfConfigList : bean == null");
			 return false;
		}
		mContext = context;
		
		mInitActual = true;
		return true;
    }
    
    static private String sysPkg[]={
    	"com.android.systemui",
    	"com.android.server.usb",//USB
    	"com.android.systemui.usb",
    	"com.android.usbui",
    	"com.yulong.android.xtime",
    	"com.yulong.android.",
    	"com.android.mms",
    	"com.yulong.android.soundrecorder",
    	"com.android.bluetooth",
    	"com.android.incallui",
    	"com.android.phone",
    	"com.android.server.wifi",
    	"com.android.printspooler",
    	"com.yulong.android.show.retailmode",
    	"com.yulong.android.calendar",
    	"com.android.server.connectivity",
    	"com.yulong.android.backup",
    	"android.net.wifi",
    	"com.android.flashlight",
    	"com.android.email",
    	//"com.icoolme.android.weather",
    	//"android",
    	};
    
    static private String floatingNotificationPkg[]={
    	"com.android.incallui",
    	"com.yulong.android.calendar",
    	"com.android.mms",
    	"com.android.email",
    	
    	};
    
    static private String defaultNotificationInStatusbar[]={
    	"com.android.mms",
    	"com.android.incallui",
    	"com.android.phone", 
       	"com.android.providers.telephony", 
       	"com.yulong.android.contacts", 
       	"com.android.providers.contacts", 
//       	//"com.yulong.android.seccenter", 
//       	//"com.yulong.android.security", 
		"com.android.providers.calendar",
        "com.yulong.android.xtime", 
        "com.yulong.android.calendar", 
        "com.yulong.android.gpsview",// name=GPSView
        "com.yulong.android.agpssettings", 
        "com.android.systemui",
        
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.tencent.hd.qzone",
        "com.tencent.pb",
    	"com.tencent.pbvuh",
    	"com.tencent.androidqqmail",
    	"com.tencent.androidqqmailb",
    	"com.qzone",
    	"com.tencent.WBlog",
    	"com.tencent.news",
    	"com.whatsapp",//whatsapp
    	
    	"com.quicinc.fmradio",
    	"com.yulong.android.soundrecorder",
		"com.android.bluetooth",
    	"com.android.settings.bluetooth", 
    	"com.android.settings.wifi.TetherSettings",
//    	//"android",//softap
    	"com.securespaces.android.navigator",
//    	
    	"com.yulong.coolmessage",
    	"com.android.server.telecom",
    	"com.coolpad.music",
    	};
    static private String defaultNotificationNotInOthers[]={
    	"cn.wps.moffice_eng",
    	"com.tencent.androidqqmail",
    	"com.google.android.gm",
    	"com.asiainfo.android",
    	"com.intsig.BizCardReader",

    	"com.moji.mjweather",
    	"com.baidu.searchbox",
    	"com.wochacha",
    	"cn.etouch.ecalendar",
    	"net.qihoo.launcher.widget.clockweather",

    	"com.baidu.BaiduMap",
    	"com.autonavi.minimap",
    	"com.google.android.apps.maps",
    	"com.Qunar",
    	"com.mapbar.android.mapbarmap",

    	"com.duoduo.child.story",
    	"com.xiaobanlong.main",
    	"com.erdo.android.FJDXCartoon",
    	"com.appshare.android.ilisten",
    	"com.kunpeng.babyting",

    	"com.taobao.taobao",
    	"com.sankuai.meituan",
    	"com.tmall.wireless",
    	"com.jingdong.app.mall",
    	"com.lingdong.client.android",

    	"com.lingan.seeyou",
    	"me.chunyu.ChunyuDoctor",
    	"com.xikang.android.slimcoach",
    	"com.babytree.apps.pregnancy",
    	"com.dailyyoga.cn",

    	"com.youdao.dict",
    	"com.handsgo.jiakao.android",
    	"com.kingsoft",
    	"com.zzenglish.client",
    	"com.xtuone.android.syllabus",

    	"com.eg.android.AlipayGphone",
    	"com.alipay.android.app",
    	"com.hexin.plat.android",
    	"com.chinamworld.main",
    	"com.mymoney",

    	"com.mt.mtxx.mtxx",
    	"vStudio.Android.Camera360",
    	"com.meitu.meiyancamera",
    	"my.beautyCamera",
    	"cn.jingling.motu.photowonder",

    	"com.tencent.mobileqq",
    	"com.sina.weibo",
    	"com.tencent.mm",
    	"com.immomo.momo",
    	"com.sec.chaton",
    	"com.qzone",

    	"com.qihoo360.mobilesafe",
    	"com.sohu.inputmethod.sogou",
    	"com.UCMobile",
    	"com.snda.wifilocating",
    	"com.qihoo.browser",

    	"com.tencent.news",
    	"com.chaozh.iReaderFree",
    	"com.chaozh.iReaderFree15",
    	"com.shuqi.controller",
    	"com.ss.android.article.news",

    	"com.kugou.android",
    	"com.youku.phone",
    	"com.storm.smart",
    	"com.sds.android.ttpod",
    	"com.qiyi.video",

    	"com.qihoo360.launcher",
    	"com.androidesk",
    	"com.gau.go.launcherex",
    	"com.qigame.lock",
    	"com.nd.android.pandahome2",

    	"com.happyelements.AndroidAnimal",
    	"com.tuyoo.doudizhu.main",
    	"com.netease.my.qihoo",
    	"com.popcap.pvz2cthd360",
    	"com.kiloo.subwaysurf",
    	
    	"com.whatsapp",//whatsapp
        //"com.example.mynotification",
        "com.securespaces.android.navigator",
    	
       	"com.yulong.android.seccenter", 
       	"com.yulong.android.security", 
       	"com.yulong.android.ota",
       	"com.yulong.android.coolshow",
       	"com.yulong.android.contacts.discover",
       	"com.android.browser",
       	"com.android.bluetooth",
		"com.android.settings.bluetooth", 
		"com.android.providers.calendar", 
        "com.yulong.android.xtime", 
        "com.yulong.android.calendar", 
        "com.quicinc.fmradio",
        "com.yulong.android.soundrecorder",
        
    	};
    static public boolean isIconNotScale(String pkg){
       	//if(mapSysPackage.containsKey(pkg))
       	{     		
    		for(String s:sysPkg){
        		if(s.equalsIgnoreCase(pkg)){
        			return true;
        		}    			
    		}
       	}
       	return false;
    }
    
    public boolean isSysPackage(String pkg){

		if (bean!=null && bean.mSystemListMap!=null && bean.mSystemListMap.containsKey(pkg)){
    		if (bean.mSystemListMap.get(pkg).equalsIgnoreCase("1")){
        		return false;		
    		}
    		return true;
		}
		
    	if(mapSysPackage!=null && mapSysPackage.containsKey(pkg)){
 		
    		for(String s:sysPkg){
        		if(s.equalsIgnoreCase(pkg)){
        			return true;
        		}    			
    		}
    		
        	String keyFind = "";
        	if(bean!=null && bean.mSystemListMapStartWith!=null){
        		for(String key:bean.mSystemListMapStartWith.keySet()){
            		if (pkg.startsWith(key) && key.length()>keyFind.length()){
            			keyFind = key;
            		}
            	}
        	}
        	
        	if (bean!=null && bean.mSystemListMapStartWith!=null && bean.mSystemListMapStartWith.containsKey(keyFind)){
        		if (bean.mSystemListMapStartWith.get(keyFind).equalsIgnoreCase("1")){
            		return false;		
        		}
        		return true;	
        	}   		
    	}
    	return false;
    }
    
    static public boolean isFloatingNotificationPackage(String pkg){
    	if(mapSysPackage!=null && mapSysPackage.containsKey(pkg)){
 		
    		for(String s:floatingNotificationPkg){
        		if(s.equalsIgnoreCase(pkg)){
        			return true;
        		}    			
    		}
    	}
    	return false;
    }
    
    /**
     * 
     * @param pkg
     * @return
     */
    public int isShowNotificationInCenter(String pkg){
    	if (bean!=null && bean.mWhiteMapFinal!=null){
    		for (Map.Entry<String, Integer> entry : bean.mWhiteMapFinal.entrySet()) {  
    	 		   if(entry.getKey().equalsIgnoreCase(pkg)){
    	 			  int bit = (entry.getValue() & 0x08) >> 3;
    	 			   //LogHelper.sd("","isShowNotificationInCenter key= " + entry.getKey() + " and value= " + entry.getValue()+" bit="+bit );
    	 			   if(bit == 1){
    	 				   return 1; 
    	 			   }else{
    	 				   return 0;
    	 			   }
    	     		}
    	 		}  
    	}
    	return 2;
    }
    
    /**
	 *	<!--blacklist>
	 *	    <item key="com.yulong.logredirect" value="0"/>
	 *	    <item key="com.yulong.android.dev.gcoption" value="0"/>
	 *	</blacklist-->
     * @param pkg
     * @return
     */
    public boolean isNotifiyEnable(String pkg){
    	if(!mInitActual){
    		LogHelper.sd(TAG, "isNotifiyEnable load xml error pkg="+pkg);
    		if(!initNtfConfigList(null)){//read again
    			return true;
    		}
    	}
    	if (bean!=null && bean.mWhiteMapFinal!=null){
    		for (Map.Entry<String, Integer> entry : bean.mWhiteMapFinal.entrySet()) {  
    	 		   if(entry.getKey().equalsIgnoreCase(pkg)){
    	 			  int bit = (entry.getValue() & 0x04) >> 2;
    	 			   //LogHelper.sd("","isNotifiyEnable key= " + entry.getKey() + " and value= " + entry.getValue()+" bit="+bit );
    	 			   if(bit == 0){
    	 				   return false; 
    	 			   }else{
    	 				   return true;
    	 			   }
    	     		}
    	 		}  
    	}else if(bean == null){
    	}
    	
    	return true;
    }
    
    /**
     * @param pkg
     * @return
     */
    public boolean isNotifiyShowInStatusbar(String pkg){
    	if (bean!=null && bean.mWhiteMapFinal!=null){
    		for (Map.Entry<String, Integer> entry : bean.mWhiteMapFinal.entrySet()) {  
    	 		   if(entry.getKey().equalsIgnoreCase(pkg)){
    	 			  int bit = (entry.getValue() & 0x02) >> 1;
    	 			   //LogHelper.sd("","isNotifiyShowInStatusbar key= " + entry.getKey() + " and value= " + entry.getValue()+" bit="+bit );
    	 			   if(bit == 1){
    	 				   return true; 
    	 			   }else{
    	 				   return false;
    	 			   }
    	     		}
    	 		}  
    	}
    	
//    	for(String key:bean.mWhiteMapList2){
//    		if (pkg.equalsIgnoreCase(key)){
//        		return true;
//        	}
//    	}
//    	
//    	if(pkg!=null){
//    		return true;
//    	}
//    	if (pkg.equalsIgnoreCase("android")){
//    		return true;
//    	}
//    	
//    	for(String key:defaultNotificationInStatusbar){
//    		if (pkg.equalsIgnoreCase(key)){
//        		return true;
//        	}
//    	}
    	
    	
    	return false;    	
    }
    
    /**
     * @param pkg
     * @return
     */
    public boolean isNotifiyShowInOther(String pkg){
    	if(pkg==null){
    		return false;
    	}
    	
//    	if (bean!=null && bean.mWhiteMapFinal!=null){
//    		for (Map.Entry<String, Integer> entry : bean.mWhiteMapFinal.entrySet()) {  
//    	 		   if(entry.getKey().equalsIgnoreCase(pkg)){
//    	 			  int bit = (entry.getValue() & 0x02) >> 1;
//    	 			   //LogHelper.sd("","defaultNotificationInStatusbar key= " + entry.getKey() + " and value= " + entry.getValue()+" bit="+bit );
//    	 			   if(bit == 1){//InStatusbar
//    	 				   return false; 
//    	 			   }else{
//    	 				   return true;
//    	 			   }
//    	     		}
//    	 		}  
//    	}
    	
    	if(bean!=null && bean.mWhiteMapList2!=null){
    		for(String key:bean.mWhiteMapList2){
			if (pkg.equalsIgnoreCase(key)) {
				return false;
			}
		}
    	
        	}
    	
    	
    	
    	if (bean!=null && bean.mWhiteMapFinal!=null){
    		for (Map.Entry<String, Integer> entry : bean.mWhiteMapFinal.entrySet()) {  
    	 		   if(entry.getKey().equalsIgnoreCase(pkg)){
    	 			  int bit = (entry.getValue() & 0x01) >> 0;
    	 			   //LogHelper.sd("","defaultNotificationNotInOthers11 key= " + entry.getKey() + " and value= " + entry.getValue()+" bit="+bit );
    	 			   if(bit == 1){//not in
    	 				   return false; 
    	 			   }else{
    	 				   return true;
    	 			   }
    	     		}
    	 		}  
    	}
    	
//    	for(String key:bean.mWhiteMapList3){
//    		if (pkg.equalsIgnoreCase(key)){
//        		return false;
//        	}
//    	}
    	
//    	for(String key:defaultNotificationInStatusbar){
//    		if (pkg.equalsIgnoreCase(key)){
//        		return false;
//        	}
//    	}
//    	
//    	for(String key:defaultNotificationNotInOthers){//不显示到其他通知
//    		if (pkg.equalsIgnoreCase(key)){
//        		return false;
//        	}
//    	}
    	return true;
    }
    
    /**
     * @param pkg
     * @return
     */
    public boolean isNotifiyShowInFloating(String pkg){
    		return true;
    	}
    
    public boolean isNotifiyShowInLocksreen(String pkg){
    	if (bean!=null && bean.mWhiteMapFinal!=null){
    		for (Map.Entry<String, Integer> entry : bean.mWhiteMapFinal.entrySet()) {  
    	 		   if(entry.getKey().equalsIgnoreCase(pkg)){
    	 			  int bit = (entry.getValue() & 0x10) >> 4;
    	 			   //LogHelper.sd("","isShowNotificationInCenter key= " + entry.getKey() + " and value= " + entry.getValue()+" bit="+bit );
    	 			   if(bit == 1){
    	 				   return true; 
    	 			   }else{
    	 				   return false;
    	 			   }
    	     		}
    	 		}  
    	}
    	return true;
    }
    
    public boolean isNotifiyShowInDetail(String pkg){
		return true;
	}
    
    public boolean isNotifiyShowInDnd(String pkg){
		return false;
	}
    static public int getPackageIcon(String pkg){
    	if(mapPackageIcon!=null && mapPackageIcon.containsKey(pkg)){
    		return mapPackageIcon.get(pkg);
    	}
    	return 0;
    }
}