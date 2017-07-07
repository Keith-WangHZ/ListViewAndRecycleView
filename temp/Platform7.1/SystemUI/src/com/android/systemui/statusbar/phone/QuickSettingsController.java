package com.android.systemui.statusbar.phone;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
//import com.android.systemui.quicksettings.QuickSettingsActivity;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
import com.android.systemui.statusbar.policy.CurrentUserTracker;

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.NfcAdapter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.quicksettings.bottom.QuickSettingLauncher;
import com.android.systemui.quicksettings.bottom.QuickSettingsPannelView;
import com.android.systemui.quicksettings.bottom.SimInfo;
import com.android.systemui.screenshot.TakeScreenshotService;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController;
//import com.yulong.android.launcherL.en;

public class QuickSettingsController {

    private final String TAG = "QuickSettingsController";
    private QuickSettingsModel  mQuickSettingsModel;
    private Handler mHandler = new Handler();

    private SparseArray<String> mQuickSettingOperations = new SparseArray<String>();
    private static final String EXTRA_STATUSBAR_EXPAND = "setting";
    private static final String UPDATE_STATUSBAR_EXPAND = "yulong.intent.action.STATUS_BAR_EXPAND_VIEW_UPDATE_ICON";
    private static final String QUICK_ITEM_LONG_PRESSED = "yulong.intent.action.STATUS_BAR_QUICK_ITEM_LONG_PRESSED";
    private static final String ACTION_MUlTIUSER_SWITCH_CHANGED = "yulong.intent.action.MULTIUSER_SWITCH";
    private static final String DEFAULT_LAUNCHERL_PACKAGE_NAME="com.yulong.android.launcherL";
    private Context mContext;
    private SparseIntArray mIdToOrderTable = new SparseIntArray();
    private ArrayList<Integer> mOrderToIdTable = new ArrayList<Integer>();
    protected PhoneStatusBar mStatusBar;
    private PackageManager mPm;
    private ComponentName currentDefaultHome;
    public QuickSettingsController(Context context) {
        mContext = context;
        mPm = mContext.getPackageManager();
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        currentDefaultHome  = mPm.getHomeActivities(homeActivities);
    }

	public void setStatusBar(PhoneStatusBar bar) {
		if (bar == null) {
			return;
		}
		mStatusBar = bar;
	}
    public QuickSettingsItemView createQuickSettingsItemView(LayoutInflater inflater,ViewGroup parent){
        QuickSettingsItemView v = (QuickSettingsItemView) inflater.inflate(R.layout.quick_setting_item, parent, false);
        v.setOnClickListener(mQuickSettingsClickListener);
        v.setLongClickable(true);
        v.setOnLongClickListener(mQuickSettingsLongClickListener);
        return v;
    }

    View.OnClickListener mQuickSettingsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            QuickSettingsItemView view = (QuickSettingsItemView) v;
            onClickQuickSetting(view.getQuickSettingId());
        }
    };

    View.OnClickListener mQuickSettingsSecondaryClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            QuickSettingsItemView view = (QuickSettingsItemView) v;
            onSecondaryClickQuickSetting(view.getQuickSettingId());
        }
    };
    View.OnLongClickListener mQuickSettingsLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            QuickSettingsItemView view = (QuickSettingsItemView) v;
            onLongClickQuickSetting(view.getQuickSettingId());
            v.setPressed(false);
            return true;
        }
    };

    private boolean isGmsInstalled(){
        File f1 = new File( "/data/app/GmsCore.apk");
        if(f1.exists()) return true;
        File f2 = new File( "/system/app/GmsCore.apk");
        if(f2.exists()) return true;

        return false;
    }
    public void initialize(QuickSettingsModel model){
        mQuickSettingsModel = model;

        mQuickSettingOperations.put(QuickSettingsData.QS_ID_WLAN, "wifi");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_BRIGHTNESS, "brightness");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_BLUETOOTH, "bluetooth");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_ROTATION, "gravitysensor");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_POWERMODE, "powermode");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_LOCATION, "gps");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SOFTAP, "softap");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_VIPLIST, "viplist");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SPEED, "speed");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SAFESWITCH, "safeswitch");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SDCARD, "sdcard");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SECURE_MODE, "securemode");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_DRIVING_MODE, "drivingmode");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_MUTLWINDOW, "multiwindow");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SLIENCE, "slience");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_SETTING, "settings");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_RECORD_SCREEN, "recordscreen");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_CONTROL_CENTOR, "controlcentor");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_CALCULATOR, "calculator");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_BLASTER, "blaster");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_MAGICAL, "magical");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_CAMERA, "camera");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_CAMERA, "night");
        mQuickSettingOperations.put(QuickSettingsData.QS_ID_WFC_MODE, "wfc");
    }
    //wangshaocheng
        public  void onTextViewClickListiner(int id){
            Intent intent;
            switch (id) {
            case QuickSettingsData.QS_ID_BLUETOOTH:
//              Intent btIntent = new Intent("com.android.systemui.BT_LISTVIEW");
//              btIntent.setClassName("com.android.systemui","com.android.systemui.quicksettings.bottom.BtSettingsListView");
//              btIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//              QuickSettingsPannelView.setBottomPanelVisible(false);
//              mContext.startActivity(btIntent);
                break;
            case QuickSettingsData.QS_ID_WLAN:
//              Intent wlanIntent = new Intent("com.android.systemui.WLAN_LISTVIEW");
//              wlanIntent.setClassName("com.android.systemui","com.android.systemui.quicksettings.bottom.WifiSettingsListView");
//              wlanIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//              QuickSettingsPannelView.setBottomPanelVisible(false);
//              mContext.startActivity(wlanIntent);
                break;
            case QuickSettingsData.QS_ID_MOBILEDATA:
                ArrayList<SimInfo> pSiminfos = (ArrayList<SimInfo>) SimInfo.getActiveSimInfoList(mContext);
                if(pSiminfos == null || pSiminfos.size() < 2){
                    showToastMsg(mContext.getResources().getString(R.string.status_bar_expanded_check_uimsim));
                    //Toast.makeText(mContext, mContext.getResources().getString(R.string.status_bar_expanded_check_uimsim), Toast.LENGTH_SHORT).show();
                    break;
                }
                // airmode don't open listview
                boolean enable = (Settings.Global.getInt(mContext.getContentResolver(),Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
                if(enable){
                    break;
                }
                boolean isDataEnabled = CPDataConnSettingUtils.getMobileDataEnabled(mContext);
                if( !isDataEnabled){
                    CPDataConnSettingUtils.setMobileDataEnabled(mContext, true);
                }
                if(QuickSettingLauncher.getInstance(mContext) != null)QuickSettingLauncher.getInstance(mContext).setMobilePanelViewVisible(true);
                break;

            default:
                return;
            }
        }

        private void setVisible(boolean isShow){
            if(QuickSettingLauncher.getInstance(mContext) != null)QuickSettingLauncher.getInstance(mContext).setBottomPanelVisible(isShow);
        }

    public void onSecondaryClickQuickSetting(int id){
        boolean ret = "1".equals(SystemProperties.get("persist.yulong.supermode", "0"));
        LogHelper.sd(TAG, "onSecondaryClickQuickSetting onClick id = " + id);
        if (id == QuickSettingsData.QS_ID_WLAN){
        	WifiTile wifiTile = (WifiTile)getStatusbar().getWifiTile();
            if(wifiTile!=null)wifiTile.handleSecondaryClick();
        }else if (id == QuickSettingsData.QS_ID_BLUETOOTH){
            BluetoothTile bt = (BluetoothTile)getStatusbar().getBluetoothTile();
            if(bt!=null)bt.handleSecondaryClick();
        }
    }

    private PhoneStatusBar getStatusbar() {
		if (mStatusBar == null && QuickSettingLauncher.getInstance(mContext) != null) {
			mStatusBar = QuickSettingLauncher.getInstance(mContext).getStatusBar();
		}
		return mStatusBar;
	}

	public void onClickQuickSetting(int id){
        boolean ret = "1".equals(SystemProperties.get("persist.yulong.supermode", "0"));
        LogHelper.sd(TAG, "QuickSettingsClickListener onClick id = " + id);
        //Log.v(TAG, "QuickSettingsClickListener onClick id = " + id, new Throwable().fillInStackTrace());
        if (id == QuickSettingsData.QS_ID_SCENEMODE){
//            startActivity(new Intent("yulong.intent.action.SWITCH_SCENE_MODE"));
//            setVisible(false);
        	 mQuickSettingsModel.changeMeetingMode();
        }

        //wangshaocheng
        else if (id == QuickSettingsData.QS_ID_SETTING){
            unLockApp();
            
            Intent intent =  new Intent(android.provider.Settings.ACTION_SETTINGS);//"android.intent.action.MAIN"
            if(mStatusBar != null){
            	mStatusBar.startActivity(intent, true);
            }
            setVisible(false);
		} else if (id == QuickSettingsData.QS_ID_SEARCH) {
            unLockApp();
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.putExtra("goToSearchPanel", true);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setClassName("com.yulong.android.launcherL", "com.yulong.android.launcherL.Launcher");
            startActivity(home);
            setVisible(false);
		} else if (id == QuickSettingsData.QS_ID_QUICK_PAY) {
            unLockApp();
            Intent in = new Intent();
			in.setPackage("com.ivvi.quickscan");
			in.setAction("com.ivvi.quickscan.MainSerivce");
			mContext.startServiceAsUser(in,UserHandle.OWNER);
            setVisible(false);
        }else if (id == QuickSettingsData.QS_ID_WHITE_BLACK){
            //mQuickSettingsModel.changeWhiteAndBlackMode();
            boolean onchang=mQuickSettingsModel.whiteAndBlackMode();
            Intent mIntent = new Intent("yulong.intent.action.SET_DISPLAYMODE");
            mIntent.putExtra("isBlackWhite", onchang);
            mContext.sendBroadcast(mIntent);
        }else if (id == QuickSettingsData.QS_ID_DOUBLECARD){
            startActivity(new Intent("com.yulong.android.dev.gcoption"));
            setVisible(false);
        } else if (id == QuickSettingsData.QS_ID_BLACKLIST) {
            startBlackListActivity();
        } else if (id == QuickSettingsData.QS_ID_MOBILEDATA){
            if(ret){
               showToastMsg(mContext.getString(R.string.status_bar_super_secure_system_mode));
               return;
            }
            mQuickSettingsModel.changeDataNetworkState();
        } else if (id == QuickSettingsData.QS_ID_APNLIST){
            mQuickSettingsModel.changeApn();
        } else if (id == QuickSettingsData.QS_ID_CBUTTON){
            mQuickSettingsModel.changeCButton();
        } else if (id == QuickSettingsData.QS_ID_NFC){
            mQuickSettingsModel.changeNfc();
        } else if (id == QuickSettingsData.QS_ID_DRIVING_MODE){
            mQuickSettingsModel.changeDrivingMode();
        } else if (id == QuickSettingsData.QS_ID_SLIENCE) {
            setVisible(false);
            final StatusBarManager statusBar = (StatusBarManager) mContext
                    .getSystemService(Context.STATUS_BAR_SERVICE);
            if (statusBar != null) {
                statusBar.collapsePanels();
            }
            final Handler handler = new Handler();
            final Runnable updateThread = new Runnable(){
                public void run(){
                    //Intent intent = new Intent();
                    startServiceSlience();
                    }
                };
           handler.postDelayed(updateThread, 380);
        } else if (id == QuickSettingsData.QS_ID_RECORD_SCREEN) {
            setVisible(false);
            final Handler handler = new Handler();
            final Runnable updateThread = new Runnable(){
                public void run(){
                    //Intent intent = new Intent();
                    startactivityscreen();
                    }
                };
           handler.postDelayed(updateThread, 380);
        }  else if (id == QuickSettingsData.QS_ID_TTWINDOW) {
            mQuickSettingsModel.changeTTWindowMode();
        } else if (id == QuickSettingsData.QS_ID_SOUND){
//          startActivityByClass("com.android.settings",
//                  "com.android.settings.SoundSettings");
            mQuickSettingsModel.changeSound();
        } else if (id == QuickSettingsData.QS_ID_DATA_ROAMING){
            mQuickSettingsModel.changeDataRoaming();
        } else if(id==QuickSettingsData.QS_ID_MUTLWINDOW){//add by wz
            mQuickSettingsModel.changeMultiWindow();
        } else if (id == QuickSettingsData.QS_ID_SELECTNETWORK){
            mQuickSettingsModel.selectNetwork();
        } else if (id == QuickSettingsData.QS_ID_MUL_ACCOUNTS){ //jinxiaofeng add
            sendBroadcastForMulipleAccounts();
        } else if (id == QuickSettingsData.QS_ID_VIPLIST) { //wangshaocheng add
            Intent mIntent =new Intent();
            mIntent.setAction("yulong.android.action.ZEN_MODE_TOGGLED");
            mContext.sendBroadcast(mIntent);
            //mQuickSettingsModel.changeViplistMode();
        } else if (id == QuickSettingsData.QS_ID_4G){
            mQuickSettingsModel.Selete4GType();
        } else if (id == QuickSettingsData.QS_ID_SWITCHNETWORKTYPE){
            mQuickSettingsModel.SeleteNetworkType();
        } else if(id == QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE){
        	  if(!isDeaultLauncherL()){
        		   return;
        	   }
            sendBroadcastForSuperSavingMode();
            setVisible(false);
        } else if(id == QuickSettingsData.QS_ID_AIRPLANE_MODE){
            sendBroadcastForAirplaneMode();
            mQuickSettingsModel.changeAirplaneMode();
        } else if(id == QuickSettingsData.QS_ID_RINGER_MODE){
            mQuickSettingsModel.changeRingerMode();
        } else if(id == QuickSettingsData.QS_ID_VIBRATE_RING_MODE){
            mQuickSettingsModel.changeVibrateRing();
        } else if(id == QuickSettingsData.QS_ID_SUPER_SECURE_MODE){
            mQuickSettingsModel.sendSuperSecureModeBroadcast();
        } else if(id == QuickSettingsData.QS_ID_SINGLE_HAND_OPERATION_MODE){
            mQuickSettingsModel.changeSingleHandOperationMode();
        } else if(id == QuickSettingsData.QS_ID_FLASHLIGHT_MODE){
            sendBroadcastForFlashLight();
            setVisible(false);
        } else if(id == QuickSettingsData.QS_ID_CALCULATOR){
            startactivitycalculator();
            setVisible(false);
        }  else if(id == QuickSettingsData.QS_ID_BLASTER){
            startactivityblaster();
            setVisible(false);
        } else if (id == QuickSettingsData.QS_ID_MAGICAL) {
            mQuickSettingsModel.changeMagicalMode();
		} else if (id == QuickSettingsData.QS_ID_CONTROL_CENTOR) {
			unLockApp();
			setVisible(false);
			Intent i = new Intent("android.intent.action.CENTER_CONTROL");
			startActivitySecurity(i);
		} else if (id == QuickSettingsData.QS_ID_CAMERA) {
            /* unimplment */
		} else if (id == QuickSettingsData.QS_ID_NIGHT_MODE) {
			mQuickSettingsModel.changeNightMode();
		} else if (id == QuickSettingsData.QS_ID_WFC_MODE) {
			sendBroadcastForWifiCalling();
		} else if (id == QuickSettingsData.QS_ID_DND_MODE) {
			DndTile dndTile = (DndTile)getStatusbar().getDndTile();
			dndTile.handleSecondaryClick();
        } else if (id == QuickSettingsData.QS_ID_BLUETOOTH) {
        	 mQuickSettingsModel.onClickBtState();
        } else if (id == QuickSettingsData.QS_ID_WLAN) {
            mQuickSettingsModel.onClickWlanState();
//            String operation = mQuickSettingOperations.get(id);
//            if (operation != null){
//               sendQucikSettingChange(operation);
//            }
    }else {// QS_ID_ROTATION
            LogHelper.sd(TAG, "QuickSettingsClickListener onClick ret =" + ret);
            if(ret && ((id == QuickSettingsData.QS_ID_LOCATION) || (id == QuickSettingsData.QS_ID_WLAN))){
                showToastMsg(mContext.getString(R.string.status_bar_super_secure_system_mode));
                LogHelper.sd(TAG, "QuickSettingsClickListener onClick ret is done" );
                return;
            }
            String operation = mQuickSettingOperations.get(id);
            if (operation != null){
                sendQucikSettingChange(operation);
                if (id == QuickSettingsData.QS_ID_LOCATION){
                    final boolean isGmsEnable = isGmsInstalled();
                    String location = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
					if (isGmsEnable && location != null
							&& !(location.equalsIgnoreCase("gps,network") || location.equalsIgnoreCase("network,gps"))) {
                        final StatusBarManager statusBar = (StatusBarManager) mContext
                                .getSystemService(Context.STATUS_BAR_SERVICE);
                        if (statusBar != null) {
                            statusBar.collapsePanels();
                        }
                    }
                }
            }
        }
    }



    private void sendBroadcastForFlashLight(){

        String isOpen = SystemProperties.get("sys.yulong.flashlight", "0");
        LogHelper.sd(TAG, "sendBroadcastForFlashLight  isOpen = " +isOpen);
        String flashLight = "com.android.intent.action.Close_FlashLight";
        if(isOpen.equals("0")){
            flashLight = "com.android.intent.action.Open_FlashLight";
        }else if(isOpen.equals("1")){
            flashLight = "com.android.intent.action.Close_FlashLight";
        }
        Intent intent = new Intent(flashLight);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);

    }
    private void sendBroadcastForWifiCalling(){
        String isOpen = SystemProperties.get("sys.yulong.wificalling", "0");
        LogHelper.sd(TAG, "sendBroadcastForWifiCalling  isOpen = " +isOpen);
        String wifiCalling = "org.codeaurora.ims.wificalling";
        int state = -1;
        if(isOpen.equals("1")){
        	state = 0;
        }else{
        	state = 1;
        }
        Intent intent = new Intent(wifiCalling);
        intent.putExtra("state", state);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
    }

    //yulong begin: add for systemUI6.0,2015.01.20
private void startactivitycalculator(){

        Intent intent = new Intent();
        intent.setClassName("com.android.calculator2","com.android.calculator2.Calculator");
        startActivitySecurity(intent);
    }

    //wangshaocheng
    private void startServiceSlience() {
        Intent intent = new Intent(mContext, TakeScreenshotService.class);
        mContext.bindServiceAsUser(intent, new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Messenger messenger = new Messenger(service);
                Message msg = Message.obtain(null, 1);
                msg.replyTo = new Messenger(new Handler());
                try {
                    //messenger.wait(500);
                    messenger.send(msg);
                } catch (RemoteException e) {
                    Log.e("@wangshaocheng@", "RemoteException:" + e);
                    e.printStackTrace();
                }
            }
        }, Context.BIND_AUTO_CREATE, UserHandle.CURRENT);
    }
    
      
    private Boolean isDeaultLauncherL(){
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        currentDefaultHome  = mPm.getHomeActivities(homeActivities);
    	  boolean isDeaultLauncherL =true;
    	  if(currentDefaultHome != null){
    		  if(currentDefaultHome.getPackageName().equals(DEFAULT_LAUNCHERL_PACKAGE_NAME)){	
    		  			
    		  }else{
    			   isDeaultLauncherL =false; 
    		  }
    	}
    	LogHelper.sd(TAG,"..............isDeaultLauncherL="+isDeaultLauncherL);
    	return isDeaultLauncherL;
    }

     private void startactivityscreen() {
          Intent intent = new Intent();
          intent.setComponent(
                  new ComponentName("com.android.screenrecord", "com.android.screenrecord.ScreenRecordService"));
          intent.setAction("yulong.intent.action.ACTION_START_SCREEN_RECORD");
          mContext.startService(intent);
    }

    private void startactivityblaster() {
        Intent intent = new Intent();
        intent.setAction("android.provider.MediaStore.RECORD_SOUND");
          startActivity(intent);

    }
private void startactivitymagical(){

    Intent intent = new Intent();
    intent.setAction("com.yulong.android.record.soundchange");
    startActivity(intent);
}
//yulong end
    private void showToastMsg(String msg){
        Toast toast = Toast.makeText(mContext,
                msg,
                Toast.LENGTH_SHORT);
      //===modify by ty
//        if (!toast.isLayoutTypeSet()) {
//            toast.setLayoutType(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
//        }
        toast.show();
    }

    private void sendBroadcastForMulipleAccounts() {  //jinxiaofeng add
        LogHelper.sd(TAG, "sendBroadcastForMulipleAccounts");
        Intent intent = new Intent(ACTION_MUlTIUSER_SWITCH_CHANGED);
        //mContext.sendBroadcast(intent);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);

        final StatusBarManager statusBar = (StatusBarManager) mContext
                .getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBar != null) {
            statusBar.collapsePanels();
        }
    }

   private void sendBroadcastForSuperSavingMode(){
        LogHelper.sd(TAG, "sendBroadcastForSuperSavingMode is done" );
        Intent intent = new Intent("com.yulong.android.SavePowerManager.SUPERSAVING");
        intent.putExtra("state", true);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
    }


   private void sendBroadcastForAirplaneMode(){

       int value = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,0);
       int newValue = (value == 0 ? 1 : 0);

       LogHelper.sd(TAG, "sendBroadcastForAirplaneMode is done, airplaneMode:"+((newValue==0)?"close":"open"));
       Intent intent = new Intent("yulong.intent.action.SCENE_MODE_CHANGED");
       if(newValue==0)
       {
           intent.putExtra("sceneMode", 1);
       }
       else
       {
           intent.putExtra("sceneMode", 2);
       }
       intent.putExtra("fromSysUI", true);
       CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
   }

   private void sendBroadcastForRingerMode(){
       LogHelper.sd(TAG, "sendBroadcastForRingerMode is done" );
       Intent intent = new Intent("android.media.RINGER_MODE_CHANGED");
       intent.putExtra(AudioManager.EXTRA_RINGER_MODE, 0);
       CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
   }

   private void sendBroadcastForVibrateRingMode(){
    }

    public boolean onLongClickQuickSetting(final int id) {

        mHandler.post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                LogHelper.temp(TAG, "onLongClickQuickSetting id = " + id);
                String operation = mQuickSettingOperations.get(id);
                if (id == QuickSettingsData.QS_ID_WLAN) {
                    unLockApp();
//                    startActivityByClass("com.android.settings.wifi",
//                            "com.android.settings.wifi.WifiSettings");
                    Intent intent =  new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                    setVisible(false);
              
                } else if (id == QuickSettingsData.QS_ID_WHITE_BLACK){
                    unLockApp();
                    Intent intent = new Intent();
                    intent.setAction("yulong.intent.action.DISPLAYMODE");
                    startActivity(intent);
                    setVisible(false);	
                } else if (id == QuickSettingsData.QS_ID_QUICK_PAY) {
                    unLockApp();
/*                    Intent in = new Intent();
                    ComponentName comp = new ComponentName("com.ivvi.quickscan",
        					"com.ivvi.quickscan");
        			in.setComponent(comp);
        			in.setAction("android.intent.action.MAIN");
                    startActivity(in);*/
                    Intent in = new Intent();
					in.setPackage("com.ivvi.quickscan");
					in.setAction("com.ivvi.quickscan.MainSerivce");
					mContext.startServiceAsUser(in,UserHandle.OWNER);
                    setVisible(false);
                }else if (id == QuickSettingsData.QS_ID_SOFTAP) {
                    unLockApp();
                    startActivityByClass("com.android.settings.wifi",
                            "com.android.settings.wifi.TetherSettings");
                    setVisible(false);
                } else if (id == QuickSettingsData.QS_ID_BLUETOOTH) {
                    unLockApp();
//                    startActivityByClass("com.android.settings.bluetooth",
//                            "com.android.settings.bluetooth.BluetoothSettings");
                    Intent intent =  new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                    setVisible(false);
                } else if (id == QuickSettingsData.QS_ID_SCENEMODE) {
                    startActivity(new Intent("yulong.intent.action.SWITCH_SCENE_MODE"));
                    setVisible(false);
                } else if (id == QuickSettingsData.QS_ID_DOUBLECARD) {
                    startActivity(new Intent("com.yulong.android.dev.gcoption"));
                } else if (id == QuickSettingsData.QS_ID_APNLIST) {
//                    startActivityByClass("com.android.settings",
//                            "com.android.settings.ApnSettings");
                    Intent intent =  new Intent(android.provider.Settings.ACTION_APN_SETTINGS);
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                } else if (id == QuickSettingsData.QS_ID_BLACKLIST) {
                    startBlackListActivity();
                } else if (id == QuickSettingsData.QS_ID_SPEED) {
                    startSpeedActivity();

                } else if (id == QuickSettingsData.QS_ID_VIPLIST) {
                    unLockApp();
                    startVipListActivity();
                } else if (id == QuickSettingsData.QS_ID_SAFESWITCH) {
                    startSafeSwitchActivity();
                } else if (id == QuickSettingsData.QS_ID_SECURE_MODE){
                    startSecureModeActivity();
                } else if (id == QuickSettingsData.QS_ID_DRIVING_MODE){
                    startActivity(new Intent("yulong.android.mms.action.DRIVE_MODE_SETTING"));
                } else if (id == QuickSettingsData.QS_ID_MOBILEDATA){
                    unLockApp();
//                    Intent intent = new Intent();
                    Intent intent =  new Intent("com.android.settings.sim.SIM_SUB_INFO_SETTINGS");
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                   //mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                    LogHelper.sd(TAG, "sendBroadcastAsUser com.android.settings.sim.SIM_SUB_INFO_SETTINGS");

                    final StatusBarManager statusBar = (StatusBarManager) mContext
                            .getSystemService(Context.STATUS_BAR_SERVICE);
                    if (statusBar != null) {
                        statusBar.collapsePanels();
                    }
                    setVisible(false);
                } else if (id == QuickSettingsData.QS_ID_LOCATION){
                    unLockApp();
//                    Intent intent = new Intent();
//                    intent.setAction("android.settings.LOCATION_SOURCE_SETTINGS");
                    Intent intent =  new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                    setVisible(false);
                }else if (id == QuickSettingsData.QS_ID_NFC){
                	unLockApp();
//                    Intent intent = new Intent();
//                    intent.setAction("android.settings.WIRELESS_SETTINGS");
                	Intent intent =  new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                    setVisible(false);
                } else if (id == QuickSettingsData.QS_ID_SOUND){
//                    startActivityByClass("com.android.settings",
//                            "com.android.settings.SoundSettings");
                    Intent intent =  new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS);
                    if(mStatusBar != null){
                    	mStatusBar.startActivity(intent, true);
                    }
                    setVisible(false);
//              }else if (id == QuickSettingsData.QS_ID_DATA_ROAMING){
//                  startActivityByClass("com.android.phone",
//                          "com.android.phone.MobileNetworkSettings");
                }else if(id==QuickSettingsData.QS_ID_MUTLWINDOW){//add by wz
                    mQuickSettingsModel.changeMultiWindow();
                }else if  (id == QuickSettingsData.QS_ID_MUL_ACCOUNTS){  //jinxiaofeng add
                    sendBroadcastForMulipleAccounts();
                }else if (operation != null) {
                    sendQuickSettingLongPressed(operation);
                } else if(id == QuickSettingsData.QS_ID_SUPER_SECURE_MODE){
                    mQuickSettingsModel.sendSuperSecureModeBroadcast();

                } else if(id == QuickSettingsData.QS_ID_FLASHLIGHT_MODE){
                    sendBroadcastForFlashLight();
                //yulong begin: add for systemUi 6.0 , by shenyupeng ,2016.01.20
                } else if(id == QuickSettingsData.QS_ID_CALCULATOR){
                    startactivitycalculator();
                }  else if(id == QuickSettingsData.QS_ID_BLASTER){
                    startactivityblaster();
                }  else if(id == QuickSettingsData.QS_ID_MAGICAL){
                    unLockApp();
                    startactivitymagical();
                //yulong end
				} else if (id == QuickSettingsData.QS_ID_CONTROL_CENTOR) {
					if (Utilities.isPrimaryUser()) {
						unLockApp();
						setVisible(false);
						Intent i = new Intent("android.intent.action.CENTER_CONTROL");
						startActivitySecurity(i);
					}
				} else {
                    LogHelper.sd(TAG, "onLongClickQuickSetting no option return false!!!");
                }
            }
		});
		if (id == QuickSettingsData.QS_ID_WLAN || id == QuickSettingsData.QS_ID_MOBILEDATA
				|| id == QuickSettingsData.QS_ID_BLUETOOTH) {
			return true;
		}
		return false;
	}
   private void unLockApp(){
    if(getStatusbar()!=null){
        getStatusbar().showBouncer();
    }
   }
    private boolean startActivityByClass(String mAppPackageName, String mAppClassName) {
        Intent intent = new Intent();
        intent.setClassName(mAppPackageName, mAppClassName);
        LogHelper.sd(TAG, "start activity pkg = " + mAppPackageName + " class = " + mAppClassName);
        return startActivity(intent);
    }


//    private void startActivityByPackageName(String appPkgName) {
//        Intent intent = new Intent();
//        intent.setAction(appPkgName);
//
//      LogHelper.sd(TAG, "start activity pkg = " + appPkgName);
//        startActivity(intent);
//    }

    private boolean startActivity(Intent intent){
//        try {
//            // Dismiss the lock screen when Settings starts.
//            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();//===modify
//
//        } catch (RemoteException e) {
//        }

        try {
            final StatusBarManager statusBar = (StatusBarManager) mContext
                    .getSystemService(Context.STATUS_BAR_SERVICE);
            if (statusBar != null) {
                statusBar.collapsePanels();
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            CurrentUserTracker.startActivityAsCurrentUser(intent);
            //mContext.startActivity(intent);\
            return true;
        } catch (ActivityNotFoundException e) {
            LogHelper.se(TAG, " e = " + e.toString());
            return false;
        }
    }
    private boolean startActivitySecurity(Intent intent){
        try {
            final StatusBarManager statusBar = (StatusBarManager) mContext
                    .getSystemService(Context.STATUS_BAR_SERVICE);
            if (statusBar != null) {
                statusBar.collapsePanels();
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivityAsUser(intent, UserHandle.OWNER);
            return true;
        } catch (ActivityNotFoundException e) {
            LogHelper.se(TAG, " e = " + e.toString());
            return false;
        }
    }

    private void startSpeedActivity(){

        try{
            LogHelper.sd(TAG, "startSpeedActivity()" );
            Intent mIntent_security = new Intent("com.yulong.android.security.action.ONE_KEY_IMPROVE");
            List<ResolveInfo> acts = mPm.queryIntentActivities(mIntent_security, 0);
            LogHelper.sd(TAG, "startSpeedActivity() acts.size()="+acts.size() );
            if (acts.size() > 0){
                startActivity(mIntent_security);
            } else {
             if(!startActivityByClass("com.yulong.android.secclearmaster", "com.yulong.android.secclearmaster.ui.activity.improve.SpeedImproveActivity")){
                startActivityByClass("com.yulong.android.softmanager", "com.yulong.android.softmanager.SpeedupActivity");
               }
            }

        } catch (ActivityNotFoundException e) {
            LogHelper.sd(TAG, "startSpeedActivity()  exception" );
            e.printStackTrace();
        }

      }


    private void startVipListActivity(){

              LogHelper.sd(TAG, "startVipListActivity()" );
//                  Intent mIntent_security = new Intent();
//                  mIntent_security.setAction("android.settings.ZEN_MODE_AUTOMATION_SETTINGS");
                  Intent intent =  new Intent(android.provider.Settings.ACTION_ZEN_MODE_SETTINGS);
                  if(mStatusBar != null){
                  	mStatusBar.startActivity(intent, true);
                  }
                  setVisible(false);
    }

    private void startSafeSwitchActivity(){

          try{
              LogHelper.sd(TAG, "startSafeSwitchActivity()" );

              Intent mIntent_security = new Intent("com.yulong.android.security.FLOW_MONITOR");
              List<ResolveInfo> acts = mPm.queryIntentActivities(mIntent_security, 0);
              LogHelper.sd(TAG, "startSafeSwitchActivity() acts.size()="+acts.size() );
              if (acts.size() > 0){
                  startActivity(mIntent_security);
              } else {
                  startActivityByClass("com.yulong.android.seccenter",
                             "com.yulong.android.flowmonitor.ui.activity.FlowMtMainActivity");

              }

          } catch (ActivityNotFoundException e) {
              LogHelper.sd(TAG, "startSafeSwitchActivity() exception" );
              e.printStackTrace();
          }

    }
    private void startBlackListActivity(){

          try{
              LogHelper.sd(TAG, "startBlackListActivity()" );

              Intent mIntent_security = new Intent("com.yulong.android.security.KAVASS");
              List<ResolveInfo> acts = mPm.queryIntentActivities(mIntent_security, 0);
              LogHelper.sd(TAG, "startBlackListActivity() acts.size()="+acts.size() );
              if (acts.size() > 0){
                 startActivity(mIntent_security);
              } else {
                  startActivityByClass("com.yulong.android.blacklist",
                          "com.yulong.android.blacklist.activity.BlackListMainActivity");

              }

          } catch (ActivityNotFoundException e) {
              LogHelper.sd(TAG, "startBlackListActivity() exception" );
              e.printStackTrace();
          }

    }
    private void startSecureModeActivity(){

          try{
              LogHelper.sd(TAG, "startSecureModeActivity()" );

              Intent mIntent_security = new Intent("com.yulong.android.security.SECURITY_MODE");
              List<ResolveInfo> acts = mPm.queryIntentActivities(mIntent_security, 0);
              LogHelper.sd(TAG, "startSecureModeActivity() acts.size()="+acts.size() );
              if (acts.size() > 0){
                  startActivity(mIntent_security);
              } else {
                  startActivityByClass("com.yulong.android.seccenter",
                          "com.yulong.android.seccenter.SecurityModeActivity");

              }

          } catch (ActivityNotFoundException e) {
              LogHelper.sd(TAG, "startSecureModeActivity() exception" );
              e.printStackTrace();
          }

    }

    private void sendQucikSettingChange(String operation) {
        LogHelper.sd(TAG, "sendQucikSettingChange extra = " + operation);
        Intent intent = new Intent(UPDATE_STATUSBAR_EXPAND+"."+operation);
 //       intent.putExtra(EXTRA_STATUSBAR_EXPAND, operation);
        LogHelper.sd(TAG, "sendQucikSettingChange " + UPDATE_STATUSBAR_EXPAND+"."+operation);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
    }

    private void sendQuickSettingLongPressed(String opString){
        LogHelper.sd(TAG, "sendQuickSettingLongPressed extra = " + opString);
        Intent intent = new Intent(QUICK_ITEM_LONG_PRESSED);
        intent.putExtra(EXTRA_STATUSBAR_EXPAND, opString);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
    }
}

