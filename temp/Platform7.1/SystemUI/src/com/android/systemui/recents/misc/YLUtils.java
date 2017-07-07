package com.android.systemui.recents.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ListActivity;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.NameValueTable;
import android.util.Log;
import android.view.View;

public class YLUtils {
    private static final String TAG = "YLUtils";
    
    /* yulong begin, add */
    /* for add secure communication, yanglun, 2015.03.14 */
    public static final String PROPERTY_METHOD_SYSTEMPROPERTY = "SystemProperty.set.get.method";
    private static final String IT_PROPERTY_NAME = "property_name";
    private static final String IT_PROPERTY_VALUE = "property_value";
    private static final String IT_PROPERTY_METHOD = "property_method";
    private static PackageManager pm;
    private static ActivityManager activityManager;
    /* yulong end */
    
    /* yulong begin, add */
    /* for set StatusBar Overlaying Activity, pengbin, 2014.12.26 */
    public static void setStatusBarOverlayingActivity(Activity p_Activity){
        View decorView = p_Activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);

        /*if(VERSION.SDK_INT >= VERSION_CODES.KITKAT){

            p_Activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            Log.d("test","VERSION.SDK_INT =" + VERSION.SDK_INT);
        }else{
            //setupSystemUI();

        }*/
    }
    /* yulong end */

    /* yulong begin, add */
    /* for get StatusBar height, pengbin, 2014.12.26 */
    public static int getStatusBarHeight(Activity activity){
        int statusHeight = 0;
        Rect localRect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);
        statusHeight = localRect.top;
        if (0 == statusHeight){
            Class<?> localClass;
            try {
                localClass = Class.forName("com.android.internal.R$dimen");
                Object localObject = localClass.newInstance();
                int nHeightValue = Integer.parseInt(localClass.getField("status_bar_height").get(localObject).toString());
                statusHeight = activity.getResources().getDimensionPixelSize(nHeightValue);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return statusHeight;
    }
    /* yulong end */


    /* yulong begin, add */
    /* Features.IS_SUPPORT_GMS_INSTALL */
    /* for GMS install, lunan, 2014.12.29 */
    public static boolean isPackageExist(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        if (intent != null) {
        	if(null == pm){
        		pm = context.getPackageManager(); 
        	}
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            Log.d("CP_SysSettings", "YLLOG: Utils isPackageExist listSize = " + listSize);
            if (listSize > 0) {
                return true;
            }
        }
        return false;
    }


    /**
     * get coolmanager package name
     * current package name: com.yulong.android.security

     * @return
     */
    public static String getSecCenterPackageName(Context context){
        boolean newSecCenterExist = isSpecificPackageExist(context, "com.yulong.android.security");
        if(newSecCenterExist){
            Log.d(TAG, "getSecCenterPackageName() exist com.yulong.android.security package");
            return "com.yulong.android.security";
        } else {
            Log.d(TAG, "getSecCenterPackageName() not exist com.yulong.android.security package");
            return "com.yulong.android.seccenter";
        }
    }
    
    public static boolean isSpecificPackageExist(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
        	if(null == pm){
        		pm = context.getPackageManager(); 
        	}
            ApplicationInfo info = pm
                    .getApplicationInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
    
    public static boolean isUSBStorageAvailable(Context context) {
        StorageManager  mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (null == mStorageManager) {
           return false;
        }
        StorageVolume[] volumeStorages = mStorageManager.getVolumeList();
        if (null == volumeStorages) {
           return false;
        }
        int volumeCount = volumeStorages.length;
        for (int i = 0; i < volumeCount; i++) {
           if (volumeStorages[i].isRemovable() && volumeStorages[i].getPath().contains("usb")) {
                   return true;
           }
       }
        
       return false;
    }
    
    public static boolean isExternalSdMounted(Context context) {
        StorageManager mStorageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        if (null == mStorageManager) {
            return false;
        }
        StorageVolume[] volumeStorage = mStorageManager.getVolumeList();
        if (null == volumeStorage) {
            return false;
        }
        int volumeCount = volumeStorage.length;
        boolean externalState = false;
        for (int i = 0; i < volumeCount; i++) {
            if (volumeStorage[i].isRemovable()) {
                if (volumeStorage[i].getDescription(context).contains("SD")) {
                    String externalPath = volumeStorage[i].getPath();
                    if (mStorageManager.getVolumeState(externalPath).equals(
                            Environment.MEDIA_MOUNTED)
                            || mStorageManager.getVolumeState(externalPath)
                                    .equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                        externalState = true;
                        break;
                    }
                }
            }
        }
        return externalState;
    } 
    /* yulong end */


    /* yulong begin, add */
    /* for check the activity, lunan, 2014.12.29 */
    public static boolean isSpecificActivityExsit(Context context, Intent intent) {
        // find "com.yulong.android.seccenter" is exist or not.
        if (intent == null) {
            Log.d(TAG, "YLLOG: intent == null");

            /* yulong begin, add */
            /* Fix Coverity Bug, lunan, 2015.07.23 */
            return false;
            /* yulong end */
        }
        if(null == pm){
    		pm = context.getPackageManager(); 
    	}
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        int listSize = list.size();
        if (listSize > 0) {
            Log.d(TAG, "YLLOG: " + intent.getAction() +": is exsit");
            return true;
        } else {
            Log.d(TAG, "YLLOG: " + intent.getAction() +": is not exsit");
            return false;
        }
    }
    /* yulong end */


    /* yulong begin, add */
    /* for isSED system, lunan, 2014.12.29 */
    public static boolean isSED() {
        String flagString = SystemProperties.get("ro.secure.system", "false");
        if (flagString != null && flagString.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSED(Context context) {
        String flagString = SystemProperties.get("ro.secure.system", "false");
        boolean hasOpened = Settings.System.getInt(context.getContentResolver(), "toggle_for_net", 0) == 1;
        if (flagString != null && flagString.equalsIgnoreCase("true") && !hasOpened) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * yulong patch, add this to setBackground of activity
     */
    public static void setBackground(ListActivity context) {
//        context.getListView().setPadding(0, 0, 0, 0); 
//        context.getListView().setBackgroundResource(R.color.yl_preference_background_color);
    }
    /* yulong end */


    /* yulong end */


    /* yulong begin, add */
    /* for syncField between SED and PPD system, lunan, 2015.01.20 */
    private static final String INTENT_SVC_PKG_NAME = "com.redbend.intentservice";
    private static final String TO_SED_INTENT = "com.redbend.event.DMA_MSG_MNG_VM_SED_INTENT";
    private static final String TO_PPD_INTENT = "com.redbend.event.DMA_MSG_MNG_VM_PPD_INTENT"; 
    private static final String EXTRA_PKG_NAME = "DMA_VAR_INTENT_PKG_NAME";
    private static final String EXTRA_DATA = "DMA_VAR_INTENT_DATA";
    private static final String RECEIVE_PERMISSION = "com.redbend.permission.EVENT_INTENT";

    //the object to sync
    private static final String EXTRA_INTERACTIVE_TYPE = "interactive_type";
    private static final String INTERACTIVE_TYPE_FIELD = "field";
    private static final String INTERACTIVE_TYPE_BROADCAST = "broadcast";
    private static final String INTERACTIVE_TYPE_SYSTEM_PROPERTY = "property";
    private static final String INTERACTIVE_TYPE_LOCKSCREEN = "lockscreen";
    private static final String INTERACTIVE_TYPE_ACTIVITY = "activity";
    private static final String INTERACTIVE_TYPE_UNMOUNT_SDCARD = "unmount_sdcard";

    private static final String IT_FIELD_DATABASE_NAME = "database_name";
    private static final String IT_FIELD_NAME = "field_name";
    private static final String IT_FIELD_VALUE = "field_value";
    private static final String IT_FIELD_TYPE = "field_type";
    

    public static void syncFieldFromSEDToPPD(Context context, String databaseName, String fieldName, String fieldValue, String fieldType){
        Intent intent = new Intent(TO_PPD_INTENT);
        intent.setPackage(INTENT_SVC_PKG_NAME);
        intent.putExtra("TYPE", "SYS_SYSSETTING_SYNC".getBytes()); 
        
        intent.putExtra(EXTRA_INTERACTIVE_TYPE, INTERACTIVE_TYPE_FIELD);
        intent.putExtra(IT_FIELD_DATABASE_NAME, databaseName);
        intent.putExtra(IT_FIELD_NAME, fieldName);
        intent.putExtra(IT_FIELD_VALUE, fieldValue);
        intent.putExtra(IT_FIELD_TYPE, fieldType);
        
        Log.d(TAG, "sync field="+fieldName+", filedValue="+fieldValue+", fieldType="+fieldType+" from sed to ppd");
        
        /* forward the data only to the same application on the other side */
        intent.putExtra(EXTRA_PKG_NAME, /*"com.yulong.android.settings"*/context.getPackageName());
        
        context.sendBroadcast(intent, RECEIVE_PERMISSION);
    }




    /**
     * yulong patch, add this to whether GmsAlreadyInstalled, pengbin
    */
    public static boolean isGmsAlreadyInstalled(Context context) {
    	if(null == pm){
    		pm = context.getPackageManager(); 
    	}
        List<PackageInfo> gmsList = pm.getInstalledPackages(0);
        int installedListSize = gmsList.size();
        boolean isInstallGms = false;
        for (int i = 0; i < installedListSize; i++) {
            PackageInfo tmp = gmsList.get(i);
            if ((tmp.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (tmp.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                if ("com.google.android.gms".equalsIgnoreCase(tmp.packageName)
                        || "com.google.android.gm".equalsIgnoreCase(tmp.packageName)) {
                    isInstallGms = true;
                }
            } 
        }
        return isInstallGms;
    }

    public static void reboot(final Context context) {
        new Thread() {
            public void run() {
                PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE); 
                pm.reboot("reboot");}
        }.start();
    }
    /* yulong end */

     /**
      * 2015.03.04 Yulong huajie add for tranfer from dp to px base on resolving power
      * @param context -- context of application, dpValue -- dp
      * @return dx
      */
     public static int dip2px(Context context, float dpValue) {
         final float scale = context.getResources().getDisplayMetrics().density;
         return (int) (dpValue * scale + 0.5f);
     }

     /* yulong begin, add */
     /* for add secure communication, yanglun, 2015.03.14 */
     public static void syncSystemPropertyFromSEDToPPD(Context context, String propertyName, String propertyValue){
         syncSystemPropertyFromSEDToPPD(context, propertyName, propertyValue, PROPERTY_METHOD_SYSTEMPROPERTY);
     }

     public static void syncSystemPropertyFromSEDToPPD(Context context, String propertyName, String propertyValue, String propertyMethod){
         Intent intent = new Intent(TO_PPD_INTENT);
         intent.setPackage(INTENT_SVC_PKG_NAME);
         intent.putExtra("TYPE", "SYS_SYSSETTING_SYNC".getBytes());

         intent.putExtra(EXTRA_INTERACTIVE_TYPE, INTERACTIVE_TYPE_SYSTEM_PROPERTY);
         intent.putExtra(IT_PROPERTY_NAME, propertyName);
         intent.putExtra(IT_PROPERTY_VALUE, propertyValue);
         intent.putExtra(IT_PROPERTY_METHOD, propertyMethod);

         Log.d(TAG, "sync property="+propertyName+", propertyValue="+propertyValue+", propertyMethod="+propertyMethod+" from sed to ppd");

         /* forward the data only to the same application on the other side */
         intent.putExtra(EXTRA_PKG_NAME, /*"com.yulong.android.settings"*/context.getPackageName());

         context.sendBroadcast(intent, RECEIVE_PERMISSION);
     }


     private static final int MIN_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;


     public static void deleteSystemDatabaseField(Context context, String fieldName){
         ContentResolver cr = context.getContentResolver();
         Uri uri = Settings.System.CONTENT_URI;

         String selection = NameValueTable.NAME+"=\""+fieldName+"\"";

         try {
             cr.delete(uri, selection, null);
         } catch (Exception e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
     }

     public static boolean isHasPunctuation(final String source){
         Pattern p = Pattern.compile("^.*\\p{Punct}.*$");
         Matcher m = p.matcher(source);
         if (m.matches()) {
             return true;
         } else {
             return false;
         }
     }
     /* yulong end */

     /* yulong begin, add */
     /* add yulong multiuser(for qcom, liuyanxin), lunan, 2015.05.05 */
     public static boolean isDoubleSpace() {
         String isDoubleSpace = SystemProperties.get("ro.ss.version", "");
         return !isDoubleSpace.equals("");
     }
     /* yulong end */
     /**
      * printDeskShortcutApps
      * @param context
      * @param launcherPkgName
      * 2016.6.28 by ty
      */
//	public static boolean isDeskShortcutApps(Context context, String launcherPkgName) {
//		if (launcherPkgName == null) {
//			return false;
//		}
//
//		if(null == pm){
//     		pm = context.getPackageManager(); 
//     	}
//
//		List<PackageInfo> packs = pm.getInstalledPackages(0);
//
//		for (PackageInfo info : packs) {
//			String lableName = info.applicationInfo.loadLabel(pm).toString();
//			if(info.packageName.equalsIgnoreCase(launcherPkgName)){
//				Log.d("test", "4444410 desktop pkgName = " + info.packageName+" nam="+lableName);
//				if (hasShortcut(context, lableName, launcherPkgName)) {
//					String pkgName = info.packageName;
//					Log.d("test", "4444412 desktop pkgName = " + pkgName);
//					return true;
//				}else{
//					break;
//				}
//			}
//
//		}
//
//		Log.d("test", "4444413 desktop Not pkgName = " + launcherPkgName);
//		return false;
//	}

	/**
	 * getLauncherPkgName
	 * @param context
	 * @return
	 * 2016.6.28 by ty
	 */
//	private static String getLauncherPkgName(Context context) {
//		if(null == activityManager){
//			activityManager = (ActivityManager) context
//					.getSystemService(Context.ACTIVITY_SERVICE);
//		}
//		List<RunningAppProcessInfo> list = activityManager
//				.getRunningAppProcesses();
//		for (RunningAppProcessInfo info : list) {
//			String pkgName = info.processName;
//			if (pkgName.contains("launcher") && pkgName.contains("android")) {
//				Log.d(TAG, "launcherPkg =  " + pkgName);
//				return pkgName;
//			}
//
//		}
//		return null;
//	}

	/**
	 * hasShortcut
	 * Judge exist launcher shortcut or not
	 * 2016.6.28 by ty
	 **/
//	private static boolean hasShortcut(Context context, String lableName,
//			String launcherPkgName) {
//
//		String url = "";
//		url = "content://" + launcherPkgName
//				+ ".settings/favorites?notify=true";
//
//		ContentResolver resolver = context.getContentResolver();
//		Cursor cursor = resolver.query(Uri.parse(url), null, "title=?",
//				new String[] { lableName }, null);
//
//		if (cursor == null) {
//			return false;
//		}
//
//		if (cursor.getCount() > 0) {
//			cursor.close();
//			return true;
//		} else {
//			cursor.close();
//			return false;
//		}
//	}
	
	/**
	 * getOnTheLauncher
	 * @param context
	 * @return
	 * 2016.6.28 by ty
	 */
	public static Boolean getOnTheLauncher(Context context, String pkgName) {
		if(pkgName.equalsIgnoreCase("com.android.systemui")){
			return false;
		}
		/*List<String> names = null;
		if (null == pm) {
			pm = context.getPackageManager();
		}
		Intent it = new Intent(Intent.ACTION_MAIN);
		it.addCategory(Intent.CATEGORY_LAUNCHER);//CATEGORY_HOME
		List<ResolveInfo> ra = pm.queryIntentActivities(it, 0);
		if (ra.size() != 0) {
			names = new ArrayList<String>();
		}
		for (int i = 0; i < ra.size(); i++) {
			String packageName = ra.get(i).activityInfo.packageName;
			names.add(packageName);
			
			if(packageName.equalsIgnoreCase(pkgName)){
			}
		}
		return false;
		*/
		return true;
	}

     /* yulong begin, modify */
     /* getIcon since 2015.08.27 of Launcher, lunan, 2015.08.26 */
     private static final Uri URI_LAUNCHER3_INTERFACE_PROVIDER = Uri.parse("content://com.yulong.android.launcher3.InterfaceProvider");
     private static final Uri URI_LAUNCHERL_INTERFACE_PROVIDER = Uri.parse("content://com.yulong.android.launcherL.IconProvider");
     private static final String METHOD_GET_ICON = "getIcon";
     private static final String KEY_PACKAGE_NAME = "packageName";
     private static final String KEY_ACTIVITY_NAME = "activityName";
     private static final String KEY_BITMAP = "bitmap";
     public static Bitmap getIcon(Context context, String packageName, String activityName, int uid) {
         //if (activityName == null) 
             activityName = getActivities(context,packageName);

         Uri uri;
         if(isSpecificPackageExist(context, "com.yulong.android.launcherL")) {
             uri = URI_LAUNCHERL_INTERFACE_PROVIDER;
         } else {
             uri = URI_LAUNCHER3_INTERFACE_PROVIDER;
         }
         try {
             ContentResolver cr = context.getContentResolver();
             Bundle extras = new Bundle();
             extras.putString(KEY_PACKAGE_NAME, packageName);
             if(activityName == null){
            	 extras.putString(KEY_ACTIVITY_NAME, packageName);
             }else{
             extras.putString(KEY_ACTIVITY_NAME, activityName);
             }
             extras.putInt("uid", uid);
             extras = cr.call(uri, METHOD_GET_ICON, null, extras);
             Bitmap bitmap = extras.getParcelable(KEY_BITMAP);
             if(bitmap != null){
                 return bitmap;
             }
         } catch (Exception e) {
             e.printStackTrace();
             return null;
         }
         return null;
     }

     public static String getActivities(Context context,String packageName) {
         Intent localIntent = new Intent("android.intent.action.MAIN", null);
         localIntent.addCategory("android.intent.category.LAUNCHER");
         if(null == pm){
     		pm = context.getPackageManager(); 
     	}
         List<ResolveInfo> appList =  pm.queryIntentActivities(localIntent, 0);
         for (int i = 0; i < appList.size(); i++) {
             ResolveInfo resolveInfo = appList.get(i);
             String packageStr = resolveInfo.activityInfo.packageName;
             if (packageStr.equals(packageName)) {
                 return resolveInfo.activityInfo.name;
             }
         }
         return null;
     }
     /* yulong end */

     public static String getFingerName(String info){
         String[] infos = info.split("&");

         String name = "";
         if(infos != null && infos.length > 0){
             if(infos[0] != null){
                 name = infos[0].split("=")[1];
             }
         }

         return name;
     }

     public static String getFingerAction(String info){
         String[] infos = info.split("&");

         String className = "null";
         if(infos != null && infos.length > 1){
             if(infos[1] != null){
                 className = infos[1].split("=")[1];
             }
         }

         return className;
     }

     /* yulong begin, add */
     /* Check bucket_display_name first to avoid exception, lunan, 2015.07.22 */
     private static boolean alreadyDidIsSupportBucketDisplayName = false;
     private static boolean isSupportBucketDisplayNameResult = false;
     public static boolean isSupportBucketDisplayName(Context context) {
         if (alreadyDidIsSupportBucketDisplayName) {
             return isSupportBucketDisplayNameResult;
         }

         Cursor musicCursor = context.getContentResolver().query(
                 MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
                 null, null);
         if (null != musicCursor) {

             if (musicCursor.getColumnIndex("bucket_display_name") != -1) {
                 isSupportBucketDisplayNameResult = true;
             }
             musicCursor.close() ;
         }
         alreadyDidIsSupportBucketDisplayName = true;

         return isSupportBucketDisplayNameResult;
     }
     /* yulong end */

     /* yulong begin ,add */
     /* Add PhoneInfoSettings, lunan, 2015.08.03 */
     public static double getSectorSize(){
         double totalSecSize = 0;
         BufferedReader br = null;
         try {
             File totalNode = null;
             totalNode = new File("/sys/bus/mmc/devices/mmc0:0001/block/mmcblk0/size");
             br = new BufferedReader(new FileReader(totalNode));
			String total;
			total = br.readLine();
			if (total != null) {
             Log.d(TAG, "YLLOG:total = " + total);
             totalSecSize = Double.parseDouble(total);
			}
         } catch (FileNotFoundException e1) {
             // TODO Auto-generated catch block
             e1.printStackTrace();
         } catch (IOException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } finally {
             try {
                 if(br != null){
                     br.close();
                 }
             } catch (IOException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             }
         }
         return totalSecSize;
     }

     public static long getSectorSizeByGUnit() {
         double totalMemory = getSectorSize()/2.0/1024.0/1024.0/0.91;
         long totalMemorySize = Math.round(totalMemory); //si she wu ru
         Log.d(TAG, "totalMemorySize is :" + totalMemorySize + "G");
         if (totalMemorySize >= 15 && totalMemorySize <= 16) {
             totalMemorySize = 16;
         }
         return totalMemorySize;
     }

     /* yulong end */
     /*yulong begin, add getInstance for get contacts icon, yangjiajia_2015-8-8*/
     public static Drawable getContactsIcon(Context context, String phoneNumber){
//         Uri uriNumber2Contacts = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
//
//         Cursor cursor = context.getContentResolver().query(uriNumber2Contacts, null, null, null, null);
//         if (cursor.getCount() > 0) {
//             cursor.moveToFirst();
//             Long contactID = cursor.getLong(cursor.getColumnIndex("contact_id"));
//             Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID); 
//             cursor.close();
//
//             InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri); 
//             Bitmap icon = BitmapFactory.decodeStream(input);
//
//             Drawable circled = CircleFramedDrawable.getInstance(context, icon, 64.0f);
//             return circled;
//         }
         return null;
     }
     /*yulong end.*/

     /* yulong begin, add */
     /* Another way to getIcon, lunan, 2015.08.25 */
     public static String getYLThemePath() {
         boolean themeAvailable = SystemProperties.getBoolean(
                "persist.sys.ui.theme.enable", false);
         if (themeAvailable) {
             return SystemProperties.get("persist.sys.ui.theme.path", null);
         }
         return null;
      }
     public static Drawable getDrawableIcon(Context context, String activityName) {
         Uri iconUri = getIconUri(activityName);
         if (iconUri != null) {
             try {
                 return Drawable.createFromStream(context.getContentResolver().openInputStream(iconUri), null);
             } catch (FileNotFoundException e) {
                 e.printStackTrace();
                 return null;
             }
         } else {
             return null;
         }
     }
     public static Uri getIconUri(String activityName) {

         if (activityName == null) {
             return null;
         }
         activityName = activityName.replace('.', '_');
         activityName = activityName.replace('$', '_');
         activityName = activityName.toLowerCase();

         String yLThemePath = getYLThemePath();
         if (yLThemePath == null) {
             Log.d(TAG,"yLThemePath == null");
             return null;
         }

         String[] paths = new String[] {
                 yLThemePath + "/icon/" + activityName + ".png",
                 yLThemePath + "/com.yulong.android.launcher3/res/drawable-"
                         + "hdpi" + "/" + activityName + ".png" ,
                 yLThemePath + "/com.yulong.android.launcher3/res/drawable-"
                         + "xhdpi" + "/" + activityName + ".png" ,
                 yLThemePath + "/com.yulong.android.launcher3/res/drawable-"
                         + "xxhdpi" + "/" + activityName + ".png",
                 yLThemePath + "/com.yulong.android.launcher3/res/drawable-"
                         + "xxxhdpi" + "/" + activityName + ".png" };
         String path = null;
         File file = null;
         int i = 0;
         for (i = 0; i < paths.length; i++) {
             path = paths[i];
             file = new File(path);
             if (file.exists()) {
                break;
             }
         }
         if (i >= paths.length) {
             return null;
         }

         return Uri.fromFile(file);
      }
     /* yulong end */

     /* yulong begin, add */
     /* simulation setting killed by task manager, yangjiajia_2015-11-2 */
     public static boolean isSettingSimulationKilled(Context ctx){
         ActivityManager am = (ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);

         List<ActivityManager.RecentTaskInfo> list = am.getRecentTasksForUser(100, ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS |
                                                                 ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                                                                 ActivityManager.RECENT_INCLUDE_PROFILES |
                                                                 ActivityManager.RECENT_WITH_EXCLUDED | 
                                                                 /*ActivityManager.RECENT_IGNORE_TASKS*/16, ctx.getUserId());
         for (ActivityManager.RecentTaskInfo info : list) {
             Log.d(TAG, "---------"+info.baseIntent.getComponent().getPackageName());
             if (info.baseIntent.getComponent().getPackageName().equals(ctx.getPackageName())) {
                 Log.d(TAG, "Settings is not killed");
                 return false;
             }
         }
         Log.d(TAG, "Settings is killed");
         return true;
     }
     /* yulong end */

     /* yulong begin, add */
     /* Remove mSmartScroll for ivvi product(liumin1), lunan, 2015.05.28 */
     public static String YulongFeatureUtils_getBrand() {
         String ret = "";
         try {
             Class sYulongFeatureUtils = Class.forName("com.yulong.android.feature.YulongFeatureUtils");
             Method sYulongFeatureUtils_getBrand = null;
             if (sYulongFeatureUtils != null) {
                 sYulongFeatureUtils_getBrand = sYulongFeatureUtils.getMethod("getBrand");
             }
             if (sYulongFeatureUtils_getBrand != null) {
                 ret = (String)sYulongFeatureUtils_getBrand.invoke(null);
             }
         } catch (Exception e) {
                 Log.d(TAG, "sSystemUtil_getRPValue Error:" + e.getMessage());
         }
         return ret;
     }
     /* yulong end */

     /* yulong begin, add */
     /* modify show remain power for all users, zhangmingmin, 2015.11.23 */
     public enum SettingsDB {
         /*GLOBAL,*/
         SYSTEM,
         SECURE,
     }
     public static final String SYSTEM_DB_NAME = "System";
     public static final String SECURE_DB_NAME = "Secure";
     public static final String INT_VALUE_TYPE = "int";
     public static final String STRING_VALUE_TYPE = "String";

     public static void putIntForAllUser(Context context, SettingsDB settings, String key, int value) {
         UserManager mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
         List<UserInfo> userInfos = mUserManager.getUsers();
         for(UserInfo userInfo : userInfos) {
             int userId = userInfo.id;
             if(userId == 99) {
                 continue;
             }
            switch (settings) {
            case SYSTEM:
                Settings.System.putIntForUser(context.getContentResolver(), key, value, userId);
                break;
            case SECURE:
                Settings.Secure.putIntForUser(context.getContentResolver(), key, value, userId);
                break;
            default:
                break;
            }
         }
//
//         ContentResolver resolver = context.getContentResolver();
//         int secureSpaceId = Settings.System.getIntForUser(resolver, "theBankingSpaceInfo", 0, 0);
//         /* The owner space is 0 , and the secure space not exists when secureSpaceId < 10 */
//
//         if (isDoubleSpace() && Process.myUserHandle().isOwner() && (secureSpaceId < 10)) {
//             SettingsCacheUtils.setBackSettingDir(context.getFilesDir());
//             SettingCache cache = new SettingCache(key, settings == SettingsDB.SYSTEM ? SYSTEM_DB_NAME : SECURE_DB_NAME, INT_VALUE_TYPE);
//             SettingsCacheUtils.writeSettingCaches(cache);
//         }
     }

     public static void putStringForAllUser(Context context, SettingsDB settings, String key, String value) {
//         UserManager mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
//         List<UserInfo> userInfos = mUserManager.getUsers();
//         for(UserInfo userInfo : userInfos) {
//             int userId = userInfo.id;
//             if(userId == 99) {
//                 continue;
//             }
//            switch (settings) {
//            case SYSTEM:
//                Settings.System.putStringForUser(context.getContentResolver(), key, value, userId);
//                break;
//            case SECURE:
//                Settings.Secure.putStringForUser(context.getContentResolver(), key, value, userId);
//                break;
//            default:
//                break;
//            }
//         }
//
//         ContentResolver resolver = context.getContentResolver();
//         int secureSpaceId = Settings.System.getIntForUser(resolver, "theBankingSpaceInfo", 0, 0);
//         /* The owner space is 0 , and the secure space not exists when secureSpaceId < 10 */
//
//         if (isDoubleSpace() && Process.myUserHandle().isOwner() && (secureSpaceId < 10)) {
//             SettingsCacheUtils.setBackSettingDir(context.getFilesDir());
//             SettingCache cache = new SettingCache(key, settings == SettingsDB.SYSTEM ? SYSTEM_DB_NAME : SECURE_DB_NAME, STRING_VALUE_TYPE);
//             SettingsCacheUtils.writeSettingCaches(cache);
//         }
     }
     /* yulong end */

}
