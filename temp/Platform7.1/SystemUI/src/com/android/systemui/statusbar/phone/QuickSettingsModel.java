package com.android.systemui.statusbar.phone;

import java.lang.reflect.Method;
import java.util.List;

import android.app.ActivityManager;
import com.android.internal.view.RotationPolicy;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.quicksettings.bottom.QuickSettingLauncher;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DataNetworkController;
import com.android.systemui.statusbar.policy.NightModeController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import com.android.systemui.tuner.TunerService;
//import com.android.systemui.statusbar.policy.DataNetworkController;
//import com.yulong.android.internal.telephony.PhoneModeConstants;
//import com.yulong.android.internal.telephony.PhoneModeManager;
import com.yulong.android.server.systeminterface.GlobalKeys;
import com.yulong.android.server.systeminterface.SystemManager;
//import com.yulong.android.telephony.CPDataConnSettingUtils;

import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;

import android.service.notification.ZenModeConfig;
import android.telephony.SubscriptionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class QuickSettingsModel{
  public static  interface IUpdateView{
    void updateView(State state);
    void resetQsView(Boolean bReinitialize);
  }
  public static  interface IUpdateData{
    void updateData(State state);
    void setOrder(int id,int order);
    State getState(int id);
    void RefreshView(int id);
  }
    private static final String TAG = "QuickSettingsModel";
    private static final String ACTION_SCENE_MODE_CHANGED = "com.yulong.android.scenemode.SCENE_MODE_CHANGED";
    private static final String UPDATE_SETTINGS_EXPAND = "yulong.intent.action.STATUS_BAR_EXPAND_VIEW_UPDATE_SETTINGS";
    private static final String EXTRA_STATUSBAR_EXPAND = "setting";
    private static final String UPDATE_STATUSBAR_EXPAND = "yulong.intent.action.STATUS_BAR_EXPAND_VIEW_UPDATE_ICON";
    private static final String ACTION_MUTEKEY_SWITCH_CHANGED = "yulong.intent.action.MUTEKEY_SWITCH";

    private static final String REQUEST_SETTING_EXPAND = "yulong.intent.action.REQUEST_SETTING_EXPAND";
    private static final String REQUEST_RESULT_EXPAND = "yulong.intent.action. REQUEST_RESULT_EXPAND ";
    private static final String ACTION_ROAMING_STATE_CHANGED = "yulong.intent.action.ROAMING_STATE_CHANGED";
    private static final String SHOW_ROAMING_QUICKSETTING = "yulong.intent.action.SHOW_ROAMING_QUICKSETTING";
    private static final String HIDE_ROAMING_QUICKSETTING =  "yulong.intent.action.HIDE_ROAMING_QUICKSETTING";
    private static final String ACTION_USER_SWITCHED =  "yulong.intent.action.ACTION_USER_SWITCHED";
    private static final String ACTION_SUPER_SAVING_POWER = "com.yulong.android.SavePowerManager.SUPERSAVING";
    private static final String ACTION_RINGER_MODE_CHANGED = "android.media.RINGER_MODE_CHANGED";
    private static final String ACTION_SUPER_SEC_MODE = "yulong.intent.action.supermode";
    private static final String MAGIC_SOUND_SETTING = "magic_sound_setting";
    private static final String MAGIC_SOUND_WHITE_BLACK = "screen_color_mode";
    private static final String CURRENT_4G_TYPE = "yulong_3g_4g_switch";
    private static final String VIPLIST_NO_DISTURB = Settings.Global.ZEN_MODE;//"zen_mode";//QS_ID_VIPLIST
    private static final String ACTION_FLASHLIGHT_ON_FLAG = "com.android.intent.action.FlashLight_On_Flag";
    private static final String ACTION_FLASHLIGHT_CLOSE_FLAG = "com.android.intent.action.FlashLight_Close_Flag";
    private static final String ACTION_WIFI_CALLING_CHANGED = "com.android.intent.action.WIFI_CALLING_CHANGED";
    private static final String DATA_ROAMING = "data_roaming_0";//Settings.Global.DATA_ROAMING;
    private static final String DATA_ROAMING2 = "data_roaming_1";//"data_roaming_2";
    private static final String SHOW_CBUTTON = "isShowCButton";
    private static final String DRIVING_MODE = "drive_mode_switch_key";
    private static final String CURRENT_NETWORK_TYPE = "yulong_3g_4g_switch";
    private static final String AIRPLANE_MODE = Settings.Global.AIRPLANE_MODE_ON;
    private static final String RINGER_MODE = "vibrate_silent_mode_state";
    private static final String VIBRATE_RING_MODE = "vibrate_ring_mode_state";
    private static final String SINGLE_HAND_OPERATION_MODE = "isShrinkageScreen";
    private boolean mHwMuteEnable = false;
    SettingManager mSettingManager;
    private int mSceneMode = 0;
    private static boolean mNetworkCanSelected[] = {false,false};
    private boolean mNetworkOn=false;
    public static boolean spuerState = false;
    
  //Yulong begin:add QS_ID_4G Observer ,wangshaocheng,2016.3.30
    
    private final class NetworkObserver extends ContentObserver{

        public NetworkObserver(Handler handler) {
          super(handler);
          // TODO Auto-generated constructor stub
        }

        @Override
        public void onChange(boolean selfChange) {
        	onNetworkChanged();
          super.onChange(selfChange);
        }

        public void startObserving() {
          CurrentUserTracker.registerContentObserver(CURRENT_4G_TYPE,true,this);
          LogHelper.sv(TAG, "registerContentObserver  NetworkObserver uri = " + Settings.System.getUriFor(CURRENT_4G_TYPE)
              + " value = " + CurrentUserTracker.getIntForCurrentUser(CURRENT_4G_TYPE,0));
          onNetworkChanged();
          }
      }
      public void onNetworkChanged(){
        boolean isEnableChange = true;
        int newType = CurrentUserTracker.getIntForCurrentUser(CURRENT_4G_TYPE, -1);
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "onNetworkChanged newType = " + newType);
        if(newType == -1){ 
          newType = 1;
        }

        State sMobile = getQuickSettingState(QuickSettingsData.QS_ID_MOBILEDATA);
        boolean bEnable = true;
            if (sMobile != null) {
              bEnable = sMobile.status == State.STATUS_ENABLE;
            }

        State s = getQuickSettingState(QuickSettingsData.QS_ID_4G);
        if ( s != null){
          if(isEnableChange && bEnable){
              s.setIcon((newType == 0) ? R.drawable.ic_qs_4g_disable : R.drawable.ic_qs_4g_enable)
              .setText(R.string.qs_label_4g );
          }else{
            s.setIcon(R.drawable.ic_qs_4g_disable)
              .setText(R.string.qs_label_4g);
          }
          refreshQuickSetting(QuickSettingsData.QS_ID_4G);
        }
      }

      public void Selete4GType(){
        int newType = CurrentUserTracker.getIntForCurrentUser(CURRENT_4G_TYPE, 0);
        LogHelper.sd(TAG, "Seleted4GType newType = " + newType);

        if(newType == -1){
          newType = 0;
        }
        Settings.System.putIntForUser(mContext.getContentResolver(), CURRENT_4G_TYPE, (newType == 1) ? 0 : 1,CurrentUserTracker.getCurrentUserId());
      }

    //Yulong end
    
  //Yulong begin:add QS_ID_WHITE_BLACK Observer ,wangshaocheng,2016.3.26
    
    private final class WhiteAndBlackModeObserver extends ContentObserver{
    	
        public WhiteAndBlackModeObserver(Handler handler) {
          super(handler);
          // TODO Auto-generated constructor stub
        }

        @Override
        public void onChange(boolean selfChange) {
          onWhiteAndBlackModeChange();
          super.onChange(selfChange);
        }
        
        public void startObserving() {
          CurrentUserTracker.registerContentObserver(MAGIC_SOUND_WHITE_BLACK,false,this);
          LogHelper.sv(TAG, "registerContentObserver " + MAGIC_SOUND_WHITE_BLACK + " = " + Settings.System.getUriFor(MAGIC_SOUND_WHITE_BLACK));
          onWhiteAndBlackModeChange();
        }
      }
    
    public void onWhiteAndBlackModeChange(){
        boolean enable = CurrentUserTracker.getIntForCurrentUser( MAGIC_SOUND_WHITE_BLACK, 1) == 0;
        LogHelper.sd(TAG, "onWhiteAndBlackModeChange enable = " + enable);

        State s = getQuickSettingState(QuickSettingsData.QS_ID_WHITE_BLACK);
        if ( s != null){
          s.setIcon(enable ? R.drawable.ic_qs_whiteblack_mode_on : R.drawable.ic_qs_whiteblack_mode_off)
          .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
          refreshQuickSetting(QuickSettingsData.QS_ID_WHITE_BLACK);
        }
      }

      public void changeWhiteAndBlackMode(){
            int value = CurrentUserTracker.getIntForCurrentUser(MAGIC_SOUND_WHITE_BLACK, 0);
            int newValue = value == 0 ? 1 : 0;
            LogHelper.sd(TAG, String.format("changeWhiteAndBlackMode value = %d nevValue = %d", value, newValue));
            CurrentUserTracker.putIntForCurrentUser(MAGIC_SOUND_WHITE_BLACK, newValue);
          }
      
      public boolean whiteAndBlackMode(){
          boolean enable = CurrentUserTracker.getIntForCurrentUser( MAGIC_SOUND_WHITE_BLACK, 1) == 0;
          return  !enable;
        }

      //Yulong end
    
    

    private final class MagicalModeObserver extends ContentObserver{

    public MagicalModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onMagicalModeChange();
      super.onChange(selfChange);
    }
    public void startObserving() {
      CurrentUserTracker.registerContentObserver(MAGIC_SOUND_SETTING,false,this);
      LogHelper.sv(TAG, "registerContentObserver " + MAGIC_SOUND_SETTING + " = " + Settings.System.getUriFor(MAGIC_SOUND_SETTING));
      onMagicalModeChange();
    }
  }

    public void onMagicalModeChange(){
    boolean enable = CurrentUserTracker.getIntForCurrentUser( MAGIC_SOUND_SETTING, 0) == 1;
    LogHelper.sd(TAG, "onMagicalModeChange enable = " + enable);

    State s = getQuickSettingState(QuickSettingsData.QS_ID_MAGICAL);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_magical_on : R.drawable.ic_qs_magical_off)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_MAGICAL);
    }
  }
  //Yulong begin:add QS_ID_VIPLIST Observer ,wangshaocheng,2016.3.3
  private final class ViplistModeObserver extends ContentObserver{

    public ViplistModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onViplistModeChange();
      super.onChange(selfChange);
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserverGlobal(VIPLIST_NO_DISTURB,false,this);
      LogHelper.sv(TAG, "registerContentObserver " + VIPLIST_NO_DISTURB + " = " + Settings.System.getUriFor(VIPLIST_NO_DISTURB));
      onViplistModeChange();
    }
  }

  private void onViplistModeChange(){
    //boolean enable = CurrentUserTracker.getIntForCurrentUser( VIPLIST_NO_DISTURB, 0) == 1;
    boolean enable = (Settings.Global.getInt(mContext.getContentResolver(),Settings.Global.ZEN_MODE,0) != 0);
    LogHelper.sd(TAG, "onViplistModeChange enable = " + enable);

    State s = getQuickSettingState(QuickSettingsData.QS_ID_VIPLIST);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_viplist_enable : R.drawable.ic_qs_viplist_disable)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_VIPLIST);
    }
  }

  public void changeViplistMode(){
        int value = CurrentUserTracker.getIntForCurrentUser(VIPLIST_NO_DISTURB, 0);
        int newValue = value == 0 ? 1 : 0;
        LogHelper.sd(TAG, String.format("changeViplistMode value = %d nevValue = %d", value, newValue));
        CurrentUserTracker.putIntForCurrentUser(VIPLIST_NO_DISTURB, newValue);
      }
  //Yulong end

  private class BrightnessObserver extends ContentObserver {
    public BrightnessObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      onBrightnessChange();
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(Settings.System.SCREEN_BRIGHTNESS_MODE, false,
          this);
      CurrentUserTracker.registerContentObserver(Settings.System.SCREEN_BRIGHTNESS, false,
          this);
    }
  }

  private final class LocationObserver extends ContentObserver {

    public LocationObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      // TODO Auto-generated method stub
      super.onChange(selfChange);
      onLocationChange();
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(Settings.Secure.LOCATION_PROVIDERS_ALLOWED,true, this);
    }
  }
  private final RotationPolicy.RotationPolicyListener mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
    @Override
    public void onChange() {
      onRotationLockChanged();
    }
  };

  private final class CButtonObserver extends ContentObserver{

    public CButtonObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onCButtonChange();
      super.onChange(selfChange);
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(SHOW_CBUTTON,false,this);
      onCButtonChange();
    }
  }

  private final class DataRoamingObserver extends ContentObserver{

    public DataRoamingObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onDataRoamingChange("");
      super.onChange(selfChange);
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(DATA_ROAMING,false,this);
      CurrentUserTracker.registerContentObserver(DATA_ROAMING2,false,this);
      LogHelper.sv(TAG, "registerContentObserver " + DATA_ROAMING + " = " + Settings.System.getUriFor(DATA_ROAMING)
          + " "+ DATA_ROAMING2 + " = " + Settings.System.getUriFor(DATA_ROAMING2));
      onDataRoamingChange("");
    }
  }
  private final class AirplaneModeObserver extends ContentObserver{

    public AirplaneModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onAirplaneMode();
      super.onChange(selfChange);
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(AIRPLANE_MODE,false,this);
      LogHelper.sv(TAG, "registerContentObserver " + AIRPLANE_MODE + " = " + Settings.System.getUriFor(AIRPLANE_MODE));
      onAirplaneMode();
    }
  }

  private final class RingerModeObserver extends ContentObserver{

    public RingerModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onRingerMode(-1);
      onVibrateRing();
      super.onChange(selfChange);
    }

    public void startObserving() {
//      CurrentUserTracker.registerContentObserver(RINGER_MODE,false,this);
//      LogHelper.sv(TAG, "registerContentObserver " + RINGER_MODE + " = " + Settings.System.getUriFor(RINGER_MODE));
      onRingerMode(-1);
      onVibrateRing();
    }
  }

  private final class VibrateRingModeObserver extends ContentObserver{

    public VibrateRingModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onRingerMode(-1);
      onVibrateRing();
      super.onChange(selfChange);
    }

    public void startObserving() {
//      CurrentUserTracker.registerContentObserver(VIBRATE_RING_MODE,false,this);
//      LogHelper.sv(TAG, "registerContentObserver " + VIBRATE_RING_MODE + " = " + Settings.System.getUriFor(VIBRATE_RING_MODE));
      AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
      int value=(0==mAudioManager.getVibrateSetting(0))?0:1;//mAudioManager.getVibrateSetting(0);
      //Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, value);//yaolihui@yulong.com_20160722:fix vibrate switcher was reseted in sys_setting after restart phone
//      mAudioManager.setVibrateSetting(0, value);//yaolihui@yulong.com_20160725:fix vibrate switcher was reseted in sys_setting after restart phone
//      mAudioManager.setVibrateSetting(1, value);//yaolihui@yulong.com_20160725:fix vibrate switcher was reseted in sys_setting after restart phone
      onRingerMode(-1);
      onVibrateRing();
    }
  }


  private final class SingleHandOperationModeObserver extends ContentObserver{

    public SingleHandOperationModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onSingleHandOperationMode();
      super.onChange(selfChange);
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(SINGLE_HAND_OPERATION_MODE,false,this);
      LogHelper.sv(TAG, "registerContentObserver " + SINGLE_HAND_OPERATION_MODE + " = " + Settings.System.getUriFor(SINGLE_HAND_OPERATION_MODE));
      onSingleHandOperationMode();
    }
  }
  private final class SelectNetworkObserver extends ContentObserver{
    public static final String MULTI_SIM_DATA_CALL_SUBSCRIPTION = "multi_sim_data_call";

    public SelectNetworkObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      onNetworkSelected();
      super.onChange(selfChange);
    }

    public void startObserving() {
      final ContentResolver cr = mContext.getContentResolver();
      cr.registerContentObserver(Settings.Global
          .getUriFor(MULTI_SIM_DATA_CALL_SUBSCRIPTION), true,
          this,UserHandle.USER_ALL);
      LogHelper.sv(TAG, "registerContentObserver uri = " + Settings.Global.getUriFor(MULTI_SIM_DATA_CALL_SUBSCRIPTION));
      onNetworkSelected();
    }
  }

  private final class SwitchNetworkTypeObserver extends ContentObserver{

    public SwitchNetworkTypeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      onNetworkTypeChanged();
      super.onChange(selfChange);
    }

    public void startObserving() {
      CurrentUserTracker.registerContentObserver(CURRENT_NETWORK_TYPE,true,this);
      LogHelper.sv(TAG, "registerContentObserver  SwitchNetworkTypeObserver uri = " + Settings.System.getUriFor(CURRENT_NETWORK_TYPE)
          + " value = " + CurrentUserTracker.getIntForCurrentUser(CURRENT_NETWORK_TYPE,0));
      onNetworkTypeChanged();
      }
  }
  public void onNetworkTypeChanged(){
    boolean isEnableChange = true;
//    int cardType = PhoneModeManager.getDefault().getCardTypeBySlotId(0);
//    LogHelper.sd(TAG, "onNetworkTypeSelected cardType = " + cardType);
//    if (cardType == PhoneModeConstants.CardType_NO || cardType == PhoneModeConstants.CardType_SIM){
//      isEnableChange = false;
//    }

    int newworkType = CurrentUserTracker.getIntForCurrentUser(CURRENT_NETWORK_TYPE, -1);
    if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "onNetworkTypeSelected newworkType = " + newworkType);
    if(newworkType == -1){  
      newworkType = 1;
    }

    State sMobile = getQuickSettingState(QuickSettingsData.QS_ID_MOBILEDATA);
    boolean bEnable = true;
        if (sMobile != null) {
          bEnable = sMobile.status == State.STATUS_ENABLE;
        }

    State s = getQuickSettingState(QuickSettingsData.QS_ID_SWITCHNETWORKTYPE);
    if ( s != null){
      if(isEnableChange && bEnable){
          s.setIcon((newworkType == 0) ? R.drawable.ic_qs_3g_priority : R.drawable.ic_qs_4g_priority)
          .setText((newworkType == 0) ? R.string.status_bar_4g_priority : R.string.status_bar_3g_priority);
      }else{
        s.setIcon(R.drawable.ic_qs_3g_priority_disable)
          .setText(R.string.status_bar_4g_priority);
      }
      refreshQuickSetting(QuickSettingsData.QS_ID_SWITCHNETWORKTYPE);
    }
  }

  public void SeleteNetworkType(){
//    int cardType = PhoneModeManager.getDefault().getCardTypeBySlotId(0);
//    LogHelper.sd(TAG, "onNetworkTypeSelected cardType = " + cardType);
//    if (cardType == PhoneModeConstants.CardType_NO || cardType == PhoneModeConstants.CardType_SIM){
//      return;
//    }
    int newworkType = CurrentUserTracker.getIntForCurrentUser( CURRENT_NETWORK_TYPE, 0);
    LogHelper.sd(TAG, "SeletedNetworkType newworkType = " + newworkType);

    if(newworkType == -1){  
      newworkType = 0;
    }
    Settings.System.putIntForUser(mContext.getContentResolver(), CURRENT_NETWORK_TYPE, (newworkType == 1) ? 0 : 1,CurrentUserTracker.getCurrentUserId());
  }

  private final class DrivingModeObserver extends ContentObserver{

    public DrivingModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      // TODO Auto-generated method stub
      onDrivingModeChange();
      super.onChange(selfChange);
    }

    public void startObserving(){
      final ContentResolver cr = mContext.getContentResolver();
      cr.registerContentObserver(
          Settings.System.getUriFor(DRIVING_MODE),
          false,
          this);
      onDrivingModeChange();
    }
  }
  private final class TTWindowModeObserver extends ContentObserver{


    public TTWindowModeObserver(Handler handler) {
      super(handler);
      // TODO Auto-generated constructor stub
    }

    @Override
    public void onChange(boolean selfChange) {
      // TODO Auto-generated method stub
      onTTWindowModeChange();
      super.onChange(selfChange);
    }

    public void startObserving(){
      final ContentResolver cr = mContext.getContentResolver();
      cr.registerContentObserver(
          SettingManager.EXTRADATA_URI,
          true,
          this);
      onTTWindowModeChange();
    }
  }
  //add by wz
  //ContentObserver for MultiWindow
    private  class MultiWindowObserver extends ContentObserver {
        private ContentResolver resolver;

        public MultiWindowObserver(Handler handler) {
            super(handler);
            resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("isMultiwindow"), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            int state=updateSettings();
            LogHelper.sd(TAG, "MultiWindow onChange state="+state);
            onMultiWindowStateChange(state);
        }

        private int updateSettings (){
            return Settings.System.getInt(resolver, "isMultiwindow", 0);
        }

    }
  //end by wz
  private NetworkObserver mNetworkObserver;
  private WhiteAndBlackModeObserver mWhiteAndBlackModeObserver;
  private MagicalModeObserver mMagicalModeObserver;
  private ViplistModeObserver mViplistModeObserver;
  private BrightnessObserver mBrightnessObserver;
  private LocationObserver   mLocationObserver;
  private DataNetworkRefreshCallback mDataNetworkCallback;
  private DataNetworkController mDataNetworkController;
  private CButtonObserver mCButtonObserver;
  private DataRoamingObserver mDataRoamingObserver;
  private SelectNetworkObserver mSelectNetworkObserver;
  private DrivingModeObserver mDrivingModeObserver;
  private TTWindowModeObserver mTTWindowModeObserver;
  private MultiWindowObserver mMultiWindowObserver;
  private AirplaneModeObserver mAirplaneModeObserver;
  private RingerModeObserver mRingerModeObserver;
  private VibrateRingModeObserver mVibrateRingModeObserver;
  private SingleHandOperationModeObserver mSingleHandOperationModeObserver;
  private SwitchNetworkTypeObserver mSwitchNetworkTypeObserver;
  private NightModeControllerCallback mNightModeControllerCallback;
  private NightModeController mNightModeController;
  private final Context mContext;
  private Boolean mPrimary;
  private IUpdateView mUpdateViewCallback;

    private String mWifiSsid;
    private boolean mWifiConnected;
    private boolean mWifiEnabled;
    private WifiManager mWifiManager;
    private boolean bHasDataRoaming = false;

    public void setHasDataRoaming(boolean bHas){
      bHasDataRoaming = bHas;
    }
  private IUpdateData mUpdateData;
  private IUpdateView mUupdateView;
  public QuickSettingsModel(Context context,Boolean bPrimary,IUpdateView updateView,IUpdateData updateData) {
    mContext = context;
    mPrimary = bPrimary;
    mUpdateData = updateData;
    mUupdateView = updateView;
  }
    private void showTaostMsg(String msg){
    	DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        Toast toast = Toast.makeText(mContext,
                msg,
                Toast.LENGTH_SHORT);
        //===modify by ty
//        if (!toast.isLayoutTypeSet()) {
//            toast.setLayoutType(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
//        }
        toast.setGravity(Gravity.TOP, 0, dm.heightPixels-dip2px(mContext,450));
        toast.show();
    }
    
    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }
    
  public static boolean getMiscInterfaceResult(String keyName) {

         boolean resultStr = false;

         try {
             Class<?> mClass = Class.forName("com.yulong.android.feature.FeatureConfig");

             Object mObject = mClass.newInstance();

             Method method = mClass.getMethod("getBooleanValue", String.class);

             resultStr = (Boolean) method.invoke(mObject, keyName);

         } catch (Exception e) {
             resultStr = false;
         }

         return resultStr;
     }

  public ZenModeController mZenModeController;
  public void initialize() {

    mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

    mSettingManager = SettingManager.createInstance(mContext);
    mNightModeController = new NightModeController(mContext);
    Handler handler = new Handler();
    mZenModeController = new ZenModeControllerImpl(mContext, handler);
    // WLAN
    State state = mUpdateData.getState(QuickSettingsData.QS_ID_WLAN);

    if (mWifiManager != null) {
      onWifiStateChange(mWifiManager.getWifiState());
    } else {
      LogHelper.se(TAG, "wifiManager is null");
      onWifiStateChange(WifiManager.WIFI_STATE_DISABLED);//add by wz
    }
    // Brightness
    state = mUpdateData.getState(QuickSettingsData.QS_ID_BRIGHTNESS);
    state.textId = R.string.qs_label_brightness;
    mBrightnessObserver = new BrightnessObserver(handler);
    mBrightnessObserver.startObserving();
    onBrightnessChange();

    state = mUpdateData.getState(QuickSettingsData.QS_ID_BLUETOOTH);
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter != null) {
      int s = adapter.getState();
      onBluetoothChange(s);
      LogHelper.sd(TAG, "bluetooth init state = " + state);
    } else {
      LogHelper.sd(TAG, "getDefaultAdapter fail");
      onBluetoothChange(BluetoothAdapter.STATE_OFF);//add by wz
    }

    state = mUpdateData.getState(QuickSettingsData.QS_ID_ROTATION);
    state.setText(R.string.qs_label_rotation);
    onRotationLockChanged();
    RotationPolicy.registerRotationPolicyListener(mContext,
        mRotationPolicyListener
     ,UserHandle.USER_ALL );

     // mobile data
    state = mUpdateData.getState(QuickSettingsData.QS_ID_MOBILEDATA);
    state.setIcon(R.drawable.ic_qs_mobile_disable);

      // apn
        state = mUpdateData.getState(QuickSettingsData.QS_ID_APNLIST);
        state.setIcon(R.drawable.ic_qs_mobile_disable).setText("no");

    // scenemode
    state = mUpdateData.getState(QuickSettingsData.QS_ID_SCENEMODE);
    state.setText(R.string.qs_label_meeting_mode);
//    try {
//      //===modify by ty
//      SystemManager systemManager = null;
//      //systemManager = (SystemManager) this.mContext.getSystemService(GlobalKeys.SYS_SERVICE);
//      if(systemManager != null)mSceneMode = systemManager.getCurrentModel();
//    } catch (Exception e) {
//      LogHelper.se(TAG, "SystemManager is null");
//    }
    onSceneModeChange();

    state = mUpdateData.getState(QuickSettingsData.QS_ID_POWERMODE);
    state.setIcon(R.drawable.ic_qs_power_disable).setText(R.string.qs_label_saving);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE);
    state.setIcon(R.drawable.ic_qs_super_saving_power_disable).setText(R.string.qs_label_super_saving).setStatus(State.STATUS_DISABLE);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_DOUBLECARD);
    state.setIcon(R.drawable.ic_qs_double_card).setText(R.string.qs_label_doublecard).setStatus(State.STATUS_ENABLE);
    state = mUpdateData.getState(QuickSettingsData.QS_ID_LOCATION);
    state.setText(R.string.qs_label_location);
    mLocationObserver = new LocationObserver(handler);
    mLocationObserver.startObserving();
    onLocationChange();

    state = mUpdateData.getState(QuickSettingsData.QS_ID_SOFTAP);
    state.setIcon(R.drawable.ic_qs_sap_disable).setText(R.string.qs_label_softap).setStatus(State.STATUS_DISABLE);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_VIPLIST);
    state.setIcon(R.drawable.ic_qs_viplist_disable).setText(R.string.qs_label_viplist2);
//    if (getMiscInterfaceResult("kavass_new_support")){
//      state.setText(R.string.qs_label_viplist);
//    }
    mViplistModeObserver = new ViplistModeObserver(handler);
    mViplistModeObserver.startObserving();
    onViplistModeChange();


    state = mUpdateData.getState(QuickSettingsData.QS_ID_SPEED);
    state.setIcon(R.drawable.ic_qs_speed).setText(R.string.qs_label_speed).setStatus(State.STATUS_ENABLE);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_SAFESWITCH);
    state.setIcon(R.drawable.ic_qs_flow_monitor_disable).setText(R.string.qs_label_flow_monitor).setStatus(State.STATUS_DISABLE);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_SDCARD);
    state.setIcon(R.drawable.ic_qs_sdcard_out).setText(R.string.qs_label_sdcard_install).setStatus(State.STATUS_DISABLE);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_BLACKLIST);
    state.setIcon(R.drawable.ic_qs_blacklist_disable).setText(R.string.qs_label_blacklist);
    if (getMiscInterfaceResult("kavass_new_support")){
      state.setText(R.string.qs_label_blacklist2);
    }

    state = mUpdateData.getState(QuickSettingsData.QS_ID_QUICK_PAY);
    state.setIcon(R.drawable.ic_qs_alipay);
    state.setText(R.string.qs_label_quick_pay);
    //wangshaocheng
         state = mUpdateData.getState(QuickSettingsData.QS_ID_CALCULATOR);
         state.setIcon(R.drawable.ic_qs_calculator_off);
         state.setText(R.string.qs_label_calculator);

         state = mUpdateData.getState(QuickSettingsData.QS_ID_BLASTER);
         state.setIcon(R.drawable.ic_qs_blaster_off);
         state.setText(R.string.qs_label_blaster);

        // Yulong begin:add QS_ID_4G init state ,wangshaocheng,2016.3.8
        // 4G
        state = mUpdateData.getState(QuickSettingsData.QS_ID_4G);
        state.setIcon(R.drawable.ic_qs_4g_disable);
        state.setText(R.string.qs_label_4g).setStatus(State.STATUS_DISABLE);
        mNetworkObserver = new NetworkObserver(handler);
        mNetworkObserver.startObserving();//780ms
        refreshQuickSetting(QuickSettingsData.QS_ID_4G);
        // Yulong end
        
        state = mUpdateData.getState(QuickSettingsData.QS_ID_WHITE_BLACK);
        state.setIcon(R.drawable.ic_qs_whiteblack_mode_off);
        state.setText(R.string.qs_label_white_black_patterns);
        mWhiteAndBlackModeObserver = new WhiteAndBlackModeObserver(handler);
        mWhiteAndBlackModeObserver.startObserving();
        onWhiteAndBlackModeChange();

        state = mUpdateData.getState(QuickSettingsData.QS_ID_MAGICAL);
        state.setIcon(R.drawable.ic_qs_magical_off);
        state.setText(R.string.qs_label_magical);
        mMagicalModeObserver = new MagicalModeObserver(handler);
        mMagicalModeObserver.startObserving();
        onMagicalModeChange();

         state = mUpdateData.getState(QuickSettingsData.QS_ID_CAMERA);
         state.setIcon(R.drawable.ic_qs_camera_off);
         state.setText(R.string.qs_label_camera);
        state = mUpdateData.getState(QuickSettingsData.QS_ID_NIGHT_MODE);
        state.setIcon(R.drawable.ic_qs_night_mode_disable);//ic_night_mode
        state.setText(R.string.night_mode);
        onNightModeChange();
        state = mUpdateData.getState(QuickSettingsData.QS_ID_DND_MODE);
        state.setIcon(R.drawable.ic_qs_dnd_off);//ic_dnd_disable
        state.setText(R.string.quick_settings_dnd_label);
        onDndModeChange();
        state = mUpdateData.getState(QuickSettingsData.QS_ID_WFC_MODE);
        state.setText(R.string.qs_label_wfc);
        String isWfcOn = SystemProperties.get("sys.yulong.wificalling", "0");
        int mWfc = -1;
        if(isWfcOn.equals("1")){
        	mWfc = 1;
        }else{
        	mWfc = 0;
        }
        onWfcModeChange(mWfc);

        //settings
        state = mUpdateData.getState(QuickSettingsData.QS_ID_SETTING);
        state.setIcon(R.drawable.ic_qs_setting_disable);
        state.setText(R.string.qs_label_setting);
        state.setStatus(false ? State.STATUS_ENABLE : State.STATUS_DISABLE);
	    refreshQuickSetting(QuickSettingsData.QS_ID_SETTING);

         //SLIENCE
        state = mUpdateData.getState(QuickSettingsData.QS_ID_SLIENCE);
        state.setIcon(R.drawable.ic_qs_screenshots_disable);
        state.setText(R.string.qs_label_slience);
        state.setStatus(false ? State.STATUS_ENABLE : State.STATUS_DISABLE);
	    refreshQuickSetting(QuickSettingsData.QS_ID_SLIENCE);

        //recordscreen
        state = mUpdateData.getState(QuickSettingsData.QS_ID_RECORD_SCREEN);
        state.setIcon(R.drawable.ic_qs_screen_recording_disable);
        state.setText(R.string.qs_label_recording);
        state.setStatus(false ? State.STATUS_ENABLE : State.STATUS_DISABLE);
	    refreshQuickSetting(QuickSettingsData.QS_ID_RECORD_SCREEN);

        //controlcentor
        state = mUpdateData.getState(QuickSettingsData.QS_ID_CONTROL_CENTOR);
        state.setIcon(R.drawable.ic_qs_control_center_disable);
        state.setText(R.string.qs_label_controlcentor);
        state.setStatus(false ? State.STATUS_ENABLE : State.STATUS_DISABLE);
	    refreshQuickSetting(QuickSettingsData.QS_ID_CONTROL_CENTOR);


    mDataNetworkController = DataNetworkController.getInstance(mContext, mPrimary);
    mDataNetworkCallback = new DataNetworkRefreshCallback();
    mDataNetworkController.addNetStatuNotifyCallBack(mDataNetworkCallback);


    mNightModeControllerCallback = new NightModeControllerCallback();
    mNightModeController.addListener(mNightModeControllerCallback);
    mZenModeController.addCallback(new ZenModeControllerCallback());
    // c-button
    state = mUpdateData.getState(QuickSettingsData.QS_ID_CBUTTON);
    state.setIcon(R.drawable.ic_qs_cbutton_disable).setText(R.string.qs_label_cbutton);

    mCButtonObserver = new CButtonObserver(handler);
    mCButtonObserver.startObserving();

    // NFC
    state = mUpdateData.getState(QuickSettingsData.QS_ID_NFC);
    state.setIcon(R.drawable.ic_qs_nfc_disable).setText(R.string.qs_label_nfc);
    
    //SEARCH
    state = mUpdateData.getState(QuickSettingsData.QS_ID_SEARCH);
    state.setIcon(R.drawable.ic_qs_search_disable).setText(R.string.accessibility_search_light);
    
        try {
          NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
          //NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter();
          int nfcstate=nfcAdapter.getAdapterState();
          if(nfcstate==NfcAdapter.STATE_TURNING_OFF){
            nfcstate=NfcAdapter.STATE_OFF;
          }
          else if(nfcstate==NfcAdapter.STATE_TURNING_ON){
            nfcstate=NfcAdapter.STATE_ON;
          }
          onNfcStateChange(nfcstate);
          //edit wz
    } catch (Exception e) {
      LogHelper.sd(TAG, e.toString());
      onNfcStateChange(NfcAdapter.STATE_OFF);//add by wz
    }

        state = mUpdateData.getState(QuickSettingsData.QS_ID_MUTLWINDOW);
        state.setIcon(R.drawable.ic_qs_mutiwindow_disable).setText(R.string.qs_label_mutlwindow);
        MultiWindowObserver mMultiWindowObserver= new MultiWindowObserver(handler);//add by wz
        onMultiWindowStateChange(mMultiWindowObserver.updateSettings());//add by wz

        state = mUpdateData.getState(QuickSettingsData.QS_ID_SECURE_MODE);
        state.setIcon(R.drawable.ic_qs_secure_mode_disable).setText(R.string.qs_label_secure_mode);

        String superType = SystemProperties.get("persist.yulong.supermode", "0");
        state = mUpdateData.getState(QuickSettingsData.QS_ID_SUPER_SECURE_MODE);
        if(superType.equals("1")){
          state.setIcon(R.drawable.ic_qs_super_secure_mode_enable).setText(R.string.qs_label_super_secure_mode);
          state.setStatus(State.STATUS_FLAG);
        }else if(superType.equals("0")){
          state.setIcon(R.drawable.ic_qs_super_secure_mode_disable).setText(R.string.qs_label_super_secure_mode);
          state.setStatus(State.STATUS_DISABLE);
        }

        state = mUpdateData.getState(QuickSettingsData.QS_ID_DRIVING_MODE);
        state.setIcon(R.drawable.ic_qs_driving_mode_disable).setText(R.string.qs_label_driving_mode);
        mDrivingModeObserver = new DrivingModeObserver(handler);
        mDrivingModeObserver.startObserving();

        state = mUpdateData.getState(QuickSettingsData.QS_ID_TTWINDOW);
        state.setIcon(R.drawable.ic_qs_ttwindow_disable).setText(R.string.qs_label_ttwindow_mode);
        mTTWindowModeObserver = new TTWindowModeObserver(handler);
        mTTWindowModeObserver.startObserving();
        
        state = mUpdateData.getState(QuickSettingsData.QS_ID_SOUND);
        state.setIcon(R.drawable.ic_qs_sound_disable).setText(R.string.qs_label_sound);
        onSoundChange(-1);
        
    state = mUpdateData.getState(QuickSettingsData.QS_ID_AIRPLANE_MODE);
    state.setIcon(R.drawable.ic_qs_airplane_disable).setText(R.string.qs_label_airplane);
    mAirplaneModeObserver = new AirplaneModeObserver(handler);
      mAirplaneModeObserver.startObserving();
      
    state = mUpdateData.getState(QuickSettingsData.QS_ID_RINGER_MODE);
    state.setIcon(R.drawable.ic_qs_ringer_disable).setText(R.string.qs_label_ringer);
    mRingerModeObserver = new RingerModeObserver(handler);
    mRingerModeObserver.startObserving();

    state = mUpdateData.getState(QuickSettingsData.QS_ID_VIBRATE_RING_MODE);
    state.setIcon(R.drawable.ic_qs_vibrate_ring_disable).setText(R.string.qs_label_vibrate_ring);
    mVibrateRingModeObserver = new VibrateRingModeObserver(handler);
    mVibrateRingModeObserver.startObserving();
    
        state = mUpdateData.getState(QuickSettingsData.QS_ID_DATA_ROAMING);
        state.setIcon(R.drawable.ic_qs_data_roaming_disable).setText(R.string.qs_label_data_roaming);
        state.setVisible(false);
        
    mDataRoamingObserver = new DataRoamingObserver(handler);
    mDataRoamingObserver.startObserving();//320ms
    
        state = mUpdateData.getState(QuickSettingsData.QS_ID_SELECTNETWORK);
        state.setIcon(R.drawable.ic_qs_data_network1).setText(R.string.status_bar_select_network1);
        mSelectNetworkObserver = new SelectNetworkObserver(handler);
        mSelectNetworkObserver.startObserving();

    // multiple accounts
    state = mUpdateData.getState(QuickSettingsData.QS_ID_MUL_ACCOUNTS);
        state.setIcon(R.drawable.ic_qs_multiple_accounts).setText(R.string.qs_label_multiple_accounts_name);

        state = mUpdateData.getState(QuickSettingsData.QS_ID_SWITCHNETWORKTYPE);
        state.setIcon(R.drawable.ic_qs_3g_priority).setText(R.string.status_bar_4g_priority).setStatus(State.STATUS_DISABLE);
        mSwitchNetworkTypeObserver = new SwitchNetworkTypeObserver(handler);
        mSwitchNetworkTypeObserver.startObserving();
    refreshQuickSetting(QuickSettingsData.QS_ID_SWITCHNETWORKTYPE);

    state = mUpdateData.getState(QuickSettingsData.QS_ID_SINGLE_HAND_OPERATION_MODE);
    state.setIcon(R.drawable.ic_qs_single_hand_disable).setText(R.string.qs_label_shrink_screen);
    mSingleHandOperationModeObserver = new SingleHandOperationModeObserver(handler);
      mSingleHandOperationModeObserver.startObserving();
    onSingleHandOperationMode();

    String isOpen = SystemProperties.get("sys.yulong.flashlight", "0");
    state = mUpdateData.getState(QuickSettingsData.QS_ID_FLASHLIGHT_MODE);
    Boolean enable = false;
    if(isOpen.equals("0")){
      state.setIcon(R.drawable.ic_qs_flashlight_off).setText(R.string.qs_label_flashlight);
    }else if(isOpen.equals("1")){
      enable = true;
      state.setIcon(R.drawable.ic_qs_flashlight_on).setText(R.string.qs_label_flashlight);
    }else{
      enable = false;
      state.setIcon(R.drawable.ic_qs_flashlight_off).setText(R.string.qs_label_flashlight);
    }
    State s = getQuickSettingState(QuickSettingsData.QS_ID_FLASHLIGHT_MODE);
    if (s != null){
      s.setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
    }


    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    intentFilter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
    intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    intentFilter.addAction("android.bluetooth.adapter.action.yulong.STATE_CHANGED");
    intentFilter.addAction(ACTION_SCENE_MODE_CHANGED);
    intentFilter.addAction(UPDATE_SETTINGS_EXPAND);
    intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    intentFilter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
//    intentFilter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
    intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
    intentFilter.addAction(AudioManager.VIBRATE_SETTING_CHANGED_ACTION);
    intentFilter.addAction(ACTION_MUTEKEY_SWITCH_CHANGED);
    intentFilter.addAction(ACTION_ROAMING_STATE_CHANGED);
    intentFilter.addAction(SHOW_ROAMING_QUICKSETTING);
    intentFilter.addAction(HIDE_ROAMING_QUICKSETTING);
    intentFilter.addAction(ACTION_USER_SWITCHED);  //jinxiaofeng 20140318 add
    intentFilter.addAction(ACTION_SUPER_SAVING_POWER);
    //intentFilter.addAction(ACTION_RINGER_MODE_CHANGED);
    intentFilter.addAction(ACTION_SUPER_SEC_MODE);
    intentFilter.addAction(ACTION_FLASHLIGHT_ON_FLAG);
    intentFilter.addAction(ACTION_FLASHLIGHT_CLOSE_FLAG);
    mContext.registerReceiverAsUser(mQuickSettingsReceiver, UserHandle.ALL, intentFilter,null,null);

        Intent requestIntent = new Intent(REQUEST_SETTING_EXPAND);
        CurrentUserTracker.sendBroadcastAsCurrentUser(requestIntent);

  }

  public void setCbuttonStatus(int status){
//      State s = mQsStates.get(QuickSettingsData.QS_ID_CBUTTON);
//      if ( s != null){
//          s.status = status;
//          s.iconId = status == State.STATUS_ENABLE ? R.drawable.ic_qs_cbutton_enable : R.drawable.ic_qs_cbutton_disable;
//          mRefreshCallback.refreshView(QuickSettingsData.QS_ID_CBUTTON, s);
//          Intent intent = new Intent("yulong.intent.action.cbutton.status");
//          intent.putExtra("run", s.status == State.STATUS_ENABLE);
//          mContext.sendBroadcast(intent);
//      }
    int value = status == State.STATUS_ENABLE ? 1 : 0;
//    Settings.System.putInt(mContext.getContentResolver(), SHOW_CBUTTON, value);
//    Settings.System.putInt(mContext.getContentResolver(), SHOW_CBUTTON,value);
    CurrentUserTracker.putIntForCurrentUser(SHOW_CBUTTON, value);
    LogHelper.sd(TAG, "setCbuttonStatus value = " + value);

  }
  private State getQuickSettingState(int id) {
    return mUpdateData.getState(id);
  }
  private void refreshQuickSetting(int id){
    mUpdateData.RefreshView(id);
  }


  private void onWifiStateChange(int state) {
    int iconId = R.drawable.ic_qs_wlan_enable;
    int textId = R.string.qs_label_wlan;
    int status = State.STATUS_CHANGING;
    switch (state) {
    case WifiManager.WIFI_STATE_DISABLING:
//      textId = R.string.qs_label_turning_off;
      iconId = R.drawable.ic_qs_wlan_disable;
      status = State.STATUS_DISABLE;
      break;
    case WifiManager.WIFI_STATE_ENABLING:
      textId = R.string.qs_label_turning_on;
      break;
    case WifiManager.WIFI_STATE_ENABLED:
      status = State.STATUS_ENABLE;
      break;
    case WifiManager.WIFI_STATE_DISABLED:
    default:
      iconId = R.drawable.ic_qs_wlan_disable;
      status = State.STATUS_DISABLE;
      break;
    }

    LogHelper.sd(TAG, "onWifiStateChange state = " + state);
    State s = getQuickSettingState(QuickSettingsData.QS_ID_WLAN);
    if (s != null) {
      s.setIcon(iconId).setText(textId).setStatus(status);
      refreshQuickSetting(QuickSettingsData.QS_ID_WLAN);
    }

    mWifiEnabled = state == WifiManager.WIFI_STATE_ENABLED;

    updateWifiName();
  }

  public void onClickWlanState(){
    int iconId = R.drawable.ic_qs_wlan_enable;
    int textId = R.string.qs_label_wlan;
    int status = State.STATUS_CHANGING;

    if (mWifiManager != null) {
      switch (mWifiManager.getWifiState()) {
      case WifiManager.WIFI_STATE_ENABLED:
        status = State.STATUS_ENABLE;
//        textId = R.string.qs_label_turning_off;
//        refreshWlan(iconId,textId,status);
        mWifiManager.setWifiEnabled(false);
        break;
      case WifiManager.WIFI_STATE_DISABLED:
        textId = R.string.qs_label_turning_on;
        status = State.STATUS_DISABLE;
        refreshWlan(iconId,textId,status);

        int wifiApState = mWifiManager.getWifiApState();
        if ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING)
            || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED)) {
          mWifiManager.setWifiApEnabled(null, false);
          // wjf add fox fix wifi_direct can't open wifi when
          // soft ap is enable
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        mWifiManager.setWifiEnabled(true);
        break;
      }

    }
  }

     public void onClickBtState() {
          BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
          if (adapter != null) {
             if (adapter.isEnabled()) {
                 adapter.disable();
                } else {
                adapter.enable();
              }
         }
      }
    private void refreshWlan(int iconId, int textId, int status) {
        State s = getQuickSettingState(QuickSettingsData.QS_ID_WLAN);
        if (s != null) {
            s.setIcon(iconId).setText(textId).setStatus(status);
            refreshQuickSetting(QuickSettingsData.QS_ID_WLAN);
        }
    }

  private void onWifiConnectStateChange(Intent intent){
    final NetworkInfo networkInfo = (NetworkInfo)
                intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        boolean wasConnected = mWifiConnected;
        mWifiConnected = networkInfo != null && networkInfo.isConnected();
        // If we just connected, grab the inintial signal strength and ssid
        if (mWifiConnected && !wasConnected) {
            // try getting it out of the intent first
            WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (info == null) {
                info = mWifiManager.getConnectionInfo();
            }
            if (info != null) {
                mWifiSsid = huntForSsid(info);
            } else {
                mWifiSsid = null;
            }
        } else if (!mWifiConnected) {
            mWifiSsid = null;
        }

        updateWifiName();
  }

    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        if(networks == null){
          return null;
        }
        for (WifiConfiguration net : networks) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }

    private void updateWifiName(){
      State s = getQuickSettingState(QuickSettingsData.QS_ID_WLAN);
      if (mWifiEnabled && s != null){
          if (mWifiConnected && mWifiSsid != null){
              s.setText(mWifiSsid.substring(1, mWifiSsid.length()-1));
          } else {
            s.setText(R.string.qs_label_wlan);
          }
          refreshQuickSetting(QuickSettingsData.QS_ID_WLAN);
      }
    }

  private void onBrightnessChange() {
    int mode =CurrentUserTracker.getIntForCurrentUser(Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    int screen = CurrentUserTracker.getIntForCurrentUser(Settings.System.SCREEN_BRIGHTNESS, 0);
    LogHelper.sd(TAG, "onBrightnessChange screen = " + screen + " mode = " + mode);
    int status = State.STATUS_ENABLE;
    int iconId = 0;
    if (mode == 1) {
      iconId = R.drawable.ic_qs_brightness_auto;
    } else if (screen <= 20) {
      status = State.STATUS_DISABLE;
      iconId = R.drawable.ic_qs_brightness_minimum;
    } else if (screen < android.os.PowerManager.BRIGHTNESS_ON) {
      iconId = R.drawable.ic_qs_brightness_default;
    } else if (screen == android.os.PowerManager.BRIGHTNESS_ON) {
      iconId = R.drawable.ic_qs_brightness_height;
    }
    State state = getQuickSettingState(QuickSettingsData.QS_ID_BRIGHTNESS);
    if (state != null) {
      state.setIcon(iconId).setStatus(status);
      refreshQuickSetting(QuickSettingsData.QS_ID_BRIGHTNESS);
    }
  }

  private void onBluetoothChange(int state) {

    int textId = R.string.qs_label_bluetooth;
    int iconId = R.drawable.ic_qs_bluetooth_disable;
    int status = State.STATUS_CHANGING;
    switch (state) {

    case BluetoothAdapter.STATE_ON:
      iconId = R.drawable.ic_qs_bluetooth_enable;
      status = State.STATUS_ENABLE;
      break;

    case BluetoothAdapter.STATE_TURNING_OFF:
      textId = R.string.qs_label_turning_off;
      break;
    case BluetoothAdapter.STATE_TURNING_ON:
      textId = R.string.qs_label_turning_on;
      break;
    case BluetoothAdapter.STATE_OFF:
    default:
      status = State.STATUS_DISABLE;
      break;
    }

    State s = getQuickSettingState(QuickSettingsData.QS_ID_BLUETOOTH);
    if (s != null) {
      s.setIcon(iconId).setText(textId).setStatus(status);
      refreshQuickSetting(QuickSettingsData.QS_ID_BLUETOOTH);
    }
  }

  private void onRotationLockChanged() {
    int status = State.STATUS_ENABLE;
    int iconId = R.drawable.ic_qs_rotation_unlock;
    boolean lock = RotationPolicy.isRotationLocked(mContext);
    LogHelper.sd(TAG, "onRotationLockChanged lock = " + lock);
    if (lock) {
      status = State.STATUS_DISABLE;
      iconId = R.drawable.ic_qs_rotation_lock;
    }
    State s = getQuickSettingState(QuickSettingsData.QS_ID_ROTATION);
    if (s != null) {
      s.setIcon(iconId).setStatus(status);
      refreshQuickSetting(QuickSettingsData.QS_ID_ROTATION);
    }
  }

  private void onSceneModeChange() {
	    int mode0 = mSceneMode;
	    LogHelper.sd(TAG, "onSceneModeChange mode = " + mode0);
	    boolean bEnable = false;
	    int iconId = 0;
	    int text = 0;
	    
	    State s = getQuickSettingState(QuickSettingsData.QS_ID_SCENEMODE);
	    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	    int mode = mAudioManager.getRingerMode();
	        LogHelper.sv(TAG, "onMeetingMode mAudioManager.getRingerMode() = " + mode + " mHwMuteEnable = " + mHwMuteEnable);
	        if (mHwMuteEnable && mode == AudioManager.RINGER_MODE_NORMAL){
	          s.setIcon(R.drawable.ic_qs_meeting_disable);
	          s.setStatus(State.STATUS_ENABLE);
	            LogHelper.sv(TAG, "onSceneMode set ic_qs_meeting_enable");
	        }else{
	          switch (mode)
	          {
	          case AudioManager.RINGER_MODE_NORMAL:
	          case AudioManager.RINGER_MODE_SILENT:
	          s.setIcon(R.drawable.ic_qs_meeting_disable);
	            s.setStatus(State.STATUS_DISABLE);
	              LogHelper.sv(TAG, "onSceneMode set ic_qs_meeting_disable");

	              break;
	          default:
	            s.setIcon(R.drawable.ic_qs_meeting_enable);
	            s.setStatus(State.STATUS_ENABLE);
	              LogHelper.sv(TAG, "onSceneMode set ic_qs_meeting_enable");
	          }
	        }
	        refreshQuickSetting(QuickSettingsData.QS_ID_SCENEMODE);

	    //boolean enable = CurrentUserTracker.getIntForCurrentUser( RINGER_MODE, 0) == 1;
	    //LogHelper.sd(TAG, "onRingerMode enable = " + enable);
  }

  private void onLocationChange() {
        String location = Settings.Secure.getStringForUser(mContext.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, CurrentUserTracker.getCurrentUserIdAquire());
        boolean bEnable = location.contains("gps") || location.contains("network");
        int iconId = bEnable ? R.drawable.ic_qs_location_enable : R.drawable.ic_qs_location_disable;
        LogHelper.sd(TAG, " location = " + location);

        State s = getQuickSettingState(QuickSettingsData.QS_ID_LOCATION);
        if (s != null) {
      s.setIcon(iconId).setStatus(bEnable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_LOCATION);
    }
  }

  private void onSoftApChange(int state){

    LogHelper.sd(TAG, "onSoftApChange = " + state);
    int iconId = R.drawable.ic_qs_sap_disable;
    int textId = R.string.qs_label_softap;
    int status  = State.STATUS_DISABLE;
    if (state == WifiManager.WIFI_AP_STATE_ENABLED){
      iconId = R.drawable.ic_qs_sap_enable;
      status  = State.STATUS_ENABLE;
    }
        State s = getQuickSettingState(QuickSettingsData.QS_ID_SOFTAP);
        if (s != null) {
      s.setIcon(iconId).setText(textId).setStatus(status);
      refreshQuickSetting(QuickSettingsData.QS_ID_SOFTAP);
    }
  }

  private void onCButtonChange(){
//    boolean enable = Settings.System.getInt(mContext.getContentResolver(), SHOW_CBUTTON,0)== 1;
        boolean enable = CurrentUserTracker.getIntForCurrentUser(SHOW_CBUTTON, 0) ==1 ;
    LogHelper.sd(TAG, "onCButtonChange enable = " + enable);

      State s = getQuickSettingState(QuickSettingsData.QS_ID_CBUTTON);
      if ( s != null){
          s.setIcon(enable ? R.drawable.ic_qs_cbutton_enable : R.drawable.ic_qs_cbutton_disable)
           .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
          refreshQuickSetting(QuickSettingsData.QS_ID_CBUTTON);
      }
        Intent intent = new Intent("yulong.intent.action.cbutton.status");
        intent.putExtra("run", enable);
        CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
        if (enable){
            final StatusBarManager statusBar = (StatusBarManager) mContext
                    .getSystemService(Context.STATUS_BAR_SERVICE);
            if (statusBar != null) {
                statusBar.collapsePanels();
            }
        }
  }

  private void onDrivingModeChange(){
    boolean enable = CurrentUserTracker.getIntForCurrentUser( DRIVING_MODE, 0) == 1;
    LogHelper.sd(TAG, "onDrivingModeChange enable = " + enable);

    State s = getQuickSettingState(QuickSettingsData.QS_ID_DRIVING_MODE);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_driving_mode_enable : R.drawable.ic_qs_driving_mode_disable)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_DRIVING_MODE);
    }
  }

  private void onSuperSavingPowerMode(boolean isSavingPower){
    if(isSavingPower){
      State s = getQuickSettingState(QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE);
      if ( s != null){
        s.setIcon( R.drawable.ic_qs_super_saving_power_enable)
        .setStatus(State.STATUS_ENABLE);
        refreshQuickSetting(QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE);
      }

        final StatusBarManager statusBar = (StatusBarManager) mContext
            .getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBar != null) {
          statusBar.collapsePanels();
          statusBar.disable(View.STATUS_BAR_DISABLE_EXPAND);
        }

    }else{
      State s = getQuickSettingState(QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE);
      if ( s != null){
        s.setIcon( R.drawable.ic_qs_super_saving_power_disable)
        .setStatus(State.STATUS_DISABLE);
        refreshQuickSetting(QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE);
      }
        final StatusBarManager statusBar = (StatusBarManager) mContext
            .getSystemService(Context.STATUS_BAR_SERVICE);
        if (statusBar != null) {
          statusBar.disable(0);
        }
    }
  }

  private void guessModeHideSettings(boolean bGuestMode){
    boolean isVisible = !bGuestMode;
    if (YulongConfig.getDefault()!= null){
      int settingId[]=YulongConfig.getDefault().getGuestModeHideSettings();;
      for(int sId:settingId){
        State s = getQuickSettingState(sId);
        if (s != null){
          s.setVisible(isVisible);
        }
      }
      refreshQuickSetting(-1);//
    }
  }
  private void onMultipleAccountsChange(String username){
//    UserManager  userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
//    String username = userManager. getUserName();
//    int userId = UserHandle.getUserId(Binder.getCallingUid());
    //no user
//    int userId = ActivityManager.getCurrentUser();//
//    boolean bGuestMode = username.equals("fdasf" || (userId==10);
//    LogHelper.sd(TAG, "onMultipleAccountsChange username = " + username + " bGuestMode = " + bGuestMode + " userId = " + userId);
//
//    if(username == null){
//      username = mContext.getString(R.string.qs_label_multiple_accounts_name);
//    }
//    guessModeHideSettings(bGuestMode);
//
//    State s = getQuickSettingState(QuickSettingsData.QS_ID_MUL_ACCOUNTS);
//    if ( s != null){
//      s.setText(username);
//    }
//    refreshQuickSetting(QuickSettingsData.QS_ID_MUL_ACCOUNTS);
  }

  private void onTTWindowModeChange(){
    //mSettingManager.loadData();//modify
    boolean enable = mSettingManager.getBooleanData(SettingManager.SHOW_TTWINDOW_KEY);
    State s = getQuickSettingState(QuickSettingsData.QS_ID_TTWINDOW);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_ttwindow_enable : R.drawable.ic_qs_ttwindow_disable)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_TTWINDOW);
    }
  }

  private void onNfcStateChange(int state){
    int iconId = R.drawable.ic_qs_nfc_enable;
    int status = State.STATUS_CHANGING;
    int textId = R.string.qs_label_nfc;
    switch (state) {
    case NfcAdapter.STATE_ON:
      status = State.STATUS_ENABLE;
      break;
    case NfcAdapter.STATE_TURNING_ON:
      textId = R.string.qs_label_turning_on;
      break;
    case NfcAdapter.STATE_TURNING_OFF:
      textId = R.string.qs_label_turning_off;
      break;
    case NfcAdapter.STATE_OFF:
    default:
      iconId = R.drawable.ic_qs_nfc_disable;
      status = State.STATUS_DISABLE;
      break;
    }

    State s = getQuickSettingState(QuickSettingsData.QS_ID_NFC);
    if (s != null){
      s.setIcon(iconId).setStatus(status).setText(textId);
      refreshQuickSetting(QuickSettingsData.QS_ID_NFC);
    }
  }
  private void onMultiWindowStateChange(int state)
  {
    int iconId = R.drawable.ic_qs_mutiwindow_disable;
    int status = State.STATUS_DISABLE;
    int textId = R.string.qs_label_mutlwindow;
    if(state==1){
      status=State.STATUS_ENABLE;
      iconId=R.drawable.ic_qs_mutiwindow_enable;
    }
    State s = getQuickSettingState(QuickSettingsData.QS_ID_MUTLWINDOW);
    if (s != null){
      s.setIcon(iconId).setStatus(status).setText(textId);
      refreshQuickSetting(QuickSettingsData.QS_ID_MUTLWINDOW);
    }
  }
  private void onOtherQsChange(int value){
    LogHelper.sd(TAG, "onOtherQsChange = " + value);

    int id = -1;
    int textId = -1;
    int iconId = -1;
    int status = -1;
    switch(value){
    case 7 :
      id = QuickSettingsData.QS_ID_POWERMODE;
      textId = R.string.qs_label_saving;
      iconId = R.drawable.ic_qs_power_disable;
      status = State.STATUS_DISABLE;
      break;
    case 8:
      id = QuickSettingsData.QS_ID_POWERMODE;
      textId = R.string.qs_label_saving;
      iconId = R.drawable.ic_qs_power_enable;
      status = State.STATUS_ENABLE;
      break;
    case 9:
      id = QuickSettingsData.QS_ID_POWERMODE;
      textId = R.string.qs_label_switching;
      iconId = R.drawable.ic_qs_power_enable;
      status = State.STATUS_CHANGING;
      break;
//    case 39:
//      id = QuickSettingsData.QS_ID_VIPLIST;
//      status = State.STATUS_DISABLE;
//      iconId = R.drawable.ic_qs_viplist_disable;
//      break;
//    case 40:
//      id = QuickSettingsData.QS_ID_VIPLIST;
//      status = State.STATUS_ENABLE;
//      iconId = R.drawable.ic_qs_viplist_enable;
//      break;
    case 14:
      id = QuickSettingsData.QS_ID_SAFESWITCH;
      status = State.STATUS_ENABLE;
      iconId = R.drawable.ic_qs_flow_monitor_enable;
      break;
    case 15:
      id = QuickSettingsData.QS_ID_SAFESWITCH;
      status = State.STATUS_DISABLE;
      iconId = R.drawable.ic_qs_flow_monitor_disable;
      break;
    case 41:
      id = QuickSettingsData.QS_ID_SDCARD;
      iconId = R.drawable.ic_qs_sdcard_out;
      textId = R.string.qs_label_sdcard_uninstall;
      status = State.STATUS_DISABLE;
      break;
    case 42:
      id = QuickSettingsData.QS_ID_SDCARD;
      iconId = R.drawable.ic_qs_sdcard_in;
      textId = R.string.qs_label_sdcard_install;
      status = State.STATUS_ENABLE;
      break;
    case 43:
      id = QuickSettingsData.QS_ID_SDCARD;
      iconId = R.drawable.ic_qs_sdcard_out;
      textId = R.string.qs_label_sdcard_install;
      status = State.STATUS_DISABLE;
      break;
    case 44:
      id = QuickSettingsData.QS_ID_MUTLWINDOW;
      iconId = R.drawable.ic_qs_mutiwindow_enable;
      textId = R.string.qs_label_mutlwindow;
      status = State.STATUS_ENABLE;
      break;
    case 45:
      id = QuickSettingsData.QS_ID_MUTLWINDOW;
      iconId = R.drawable.ic_qs_mutiwindow_disable;
      textId = R.string.qs_label_mutlwindow;
      status = State.STATUS_DISABLE;
      break;
    case 46:
      id = QuickSettingsData.QS_ID_SECURE_MODE;
      iconId = R.drawable.ic_qs_secure_mode_enable;
      textId = R.string.qs_label_secure_mode;
      status = State.STATUS_ENABLE;
      break;
    case 47:
      id = QuickSettingsData.QS_ID_SECURE_MODE;
      iconId = R.drawable.ic_qs_secure_mode_disable;
      textId = R.string.qs_label_secure_mode;
      status = State.STATUS_DISABLE;
      break;
//    case 48:
//      id = QuickSettingsData.QS_ID_DRIVING_MODE;
//      iconId = R.drawable.ic_qs_driving_mode_enable;
//      textId = R.string.qs_label_driving_mode;
//      status = State.STATUS_ENABLE;
//      break;
//    case 49:
//      id = QuickSettingsData.QS_ID_DRIVING_MODE;
//      iconId = R.drawable.ic_qs_driving_mode_disable;
//      textId = R.string.qs_label_driving_mode;
//      status = State.STATUS_DISABLE;
//      break;
    default:
      break;
    }
    State state = getQuickSettingState(id);
    if (state != null){
      if (iconId >= 0){
        state.setIcon(iconId);
      }
      if (textId >= 0){
        state.setText(textId);
      }
      if (status >= 0)
        state.setStatus(status);
      refreshQuickSetting(id);
    }
  }
  private BroadcastReceiver mQuickSettingsReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      LogHelper.sd(TAG, "mQuickSettingsReceiver onReceive " + action);

      if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
            WifiManager.WIFI_STATE_UNKNOWN);
        onWifiStateChange(state);
      } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) || action.equals("android.bluetooth.adapter.action.yulong.STATE_CHANGED")) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
            BluetoothAdapter.ERROR);
        onBluetoothChange(state);
      } else if (action.equals(ACTION_SCENE_MODE_CHANGED)) {
        mSceneMode = intent.getIntExtra("sceneModeType", 1);
        onSceneModeChange();
      } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_DISABLED);
        onSoftApChange(state);
      } else if (action.equals(UPDATE_SETTINGS_EXPAND)){
        int value = intent.getIntExtra(EXTRA_STATUSBAR_EXPAND, -1);
        onOtherQsChange(value);
      } else if(action.equals(ACTION_SUPER_SAVING_POWER)){
        spuerState = intent.getBooleanExtra("state", false);
        LogHelper.sd(TAG, "mQuickSettingsReceiver ACTION_SUPER_SAVING_POWER state" + spuerState);
        onSuperSavingPowerMode(spuerState);
      } else if(action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)){
        int state = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, 0);
        LogHelper.sd(TAG, "mQuickSettingsReceiver ACTION_RINGER_MODE_CHANGED state" + state);
        onRingerMode(state);
        onSceneModeChange();
        onVibrateRing();
      } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
              onWifiConnectStateChange(intent);
      } else if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)){
              int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);
              LogHelper.sd(TAG,"NfcAdapter.ACTION_ADAPTER_STATE_CHANGED state="+state);
              onNfcStateChange(state);
            } else if (action.equals(AudioManager.VOLUME_CHANGED_ACTION)
                ){
//              int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE,AudioManager.STREAM_SYSTEM);
//              int volume =  intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
//              int oldVolume = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
//              if (streamType == AudioManager.STREAM_SYSTEM)
//              onSoundChange(volume);
              onSoundChange(-1);
              onRingerMode(-1);
              onVibrateRing();
            } else if (intent.getAction().equals(AudioManager.VIBRATE_SETTING_CHANGED_ACTION)) {
              onRingerMode(-1);
              onVibrateRing();
              LogHelper.sd(TAG, "mQuickSettingsReceiver AudioManager.VIBRATE_SETTING_CHANGED_ACTION");
            }else if(action.equals(ACTION_MUTEKEY_SWITCH_CHANGED)){
                mHwMuteEnable = (intent.getIntExtra("state", -1) == 1);
                LogHelper.sd(TAG, "mQuickSettingsReceiver ACTION_MUTEKEY_SWITCH_CHANGED state=" + mHwMuteEnable);
                onSoundChange(-1);
                onRingerMode(-1);
                onVibrateRing();
      }else if(action.equals(ACTION_USER_SWITCHED)){  //jinxiaofeng 20140318 add
        String username = intent.getStringExtra("username");
        onMultipleAccountsChange(username);
            }
            else if (action.equals(ACTION_ROAMING_STATE_CHANGED)){
              //onDataRoamingChange();
              if (intent.getBooleanExtra("mRoaming",false)){
                State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
                s.setVisible(true);
                refreshQuickSetting(QuickSettingsData.QS_ID_DATA_ROAMING);
              }else{
                State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
                  s.setVisible(false);
                refreshQuickSetting(QuickSettingsData.QS_ID_DATA_ROAMING);
              }
              LogHelper.sv(TAG,"" + ACTION_ROAMING_STATE_CHANGED + " mRoaming = " + intent.getBooleanExtra("mRoaming",false));
//              intent.putExtra("mRoaming", roaming);
//              intent.putExtra("mRoamingChanged", mRoamingChanged);//register different network when roaming
//              intent.putExtra("mPhoneId", mPhoneId);//phoneId
//              intent.putExtra(Phone.SLOT_ID_KEY,getPhone().getSlotId());//slotId
            } else if (action.equals(SHOW_ROAMING_QUICKSETTING)){
            State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
            s.setVisible(true);
            refreshQuickSetting(QuickSettingsData.QS_ID_DATA_ROAMING);
            } else if (action.equals(HIDE_ROAMING_QUICKSETTING)){
            State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
              s.setVisible(false);
            refreshQuickSetting(QuickSettingsData.QS_ID_DATA_ROAMING);
            } else if(action.equals(ACTION_SUPER_SEC_MODE)){
              onSuperSecureModeChange();
            }else if(action.equals(ACTION_FLASHLIGHT_CLOSE_FLAG)){
              onFlashLightStateChange();
            }else if(action.equals(ACTION_FLASHLIGHT_ON_FLAG)){
              onFlashLightStateChange();
            }else if(action.equals(ACTION_WIFI_CALLING_CHANGED)){
            	int state = intent.getIntExtra("state", 0);//1: ON     0: OFF
            	onWfcModeChange(state);
            }
    }
  };

  public static class State {
    public int id = -1;
    public int order = -1;
    public int iconId;
    public int textId;
    public int status;
    public int textStatus;
    boolean isVisible=true;
    boolean isVisibleSecondary=false;
    String text;
        public static int STATUS_FLAG = 3;
        public static int STATUS_ENABLE = 0;
        public static int STATUS_DISABLE = 1;
        public static int STATUS_CHANGING = 2;

    public State(int id, int tid, int iid) {
      this.id = id;
      textId = tid;
      iconId = iid;
      status = STATUS_DISABLE;
      textStatus = -1;
    }

    public State(int id) {
      this(id, -1, -1);
    }

    public State setText(int textId) {
      this.textId = textId;
      return this;
    }

    public State setText(String text){
      this.textId = 0;
      this.text = text;
      return this;
    }

    public State setIcon(int iconId) {
      this.iconId = iconId;
      return this;
    }

    public State setStatus(int status) {
      this.status = status;
      return this;
    }
    public State setVisible(boolean isVisible){
      this.isVisible = isVisible;
      return this;
    }
    public State setVisibleSecondary(boolean isVisible){
      this.isVisibleSecondary = isVisible;
      return this;
    }
  }

  private void refreshNetworkAble(){
    int status = State.STATUS_DISABLE;
        boolean enable = (Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0)==1);
//      if (mSceneMode != GlobalKeys.SceneMode.MODEL_FLIGHT && mNetworkOn){
        if (!enable && mNetworkOn){
      status = State.STATUS_ENABLE;
    }
    State s = getQuickSettingState(QuickSettingsData.QS_ID_MOBILEDATA);
        if (s != null) {
      s.setStatus(status);
        s.textStatus = status;
      refreshQuickSetting(QuickSettingsData.QS_ID_MOBILEDATA);
    }

        s = getQuickSettingState(QuickSettingsData.QS_ID_APNLIST);
        if (s != null){
          s.setStatus(status);
        s.textStatus = status;
          refreshQuickSetting(QuickSettingsData.QS_ID_APNLIST);
        }
        onNetworkSelected();
        onNetworkChanged();
        onNetworkTypeChanged(); 
  }
  private class DataNetworkRefreshCallback implements DataNetworkController.NetworkStatNotify {

    @Override
    public void notifyNetworkApn(boolean bNetworkOn, boolean bRoaming, String apnText) {
    	if(LogHelper.NOLOGGING)LogHelper.sv(TAG, "notifyNetworkApn bNetworkOn = " + bNetworkOn
          + " bRoaming = " + bRoaming
          + " apnText = " + apnText);
      int iconId = R.drawable.ic_qs_mobile_disable;
      int textId = YulongConfig.getDefault().mYulongResStartNetwork;
      int status = State.STATUS_DISABLE;
      int apnIconId = R.drawable.ic_qs_apn_disable;
      mNetworkOn = bNetworkOn;
      if (bNetworkOn){
        iconId = R.drawable.ic_qs_mobile_enable;
        apnIconId = R.drawable.ic_qs_apn_enable;
        textId = YulongConfig.getDefault().mYulongResStopNetwork;
        status = State.STATUS_ENABLE;
      }

      State s = getQuickSettingState(QuickSettingsData.QS_ID_MOBILEDATA);
          if (s != null) {
        s.setIcon(iconId).setText(textId).setStatus(status);
        refreshQuickSetting(QuickSettingsData.QS_ID_MOBILEDATA);
      }

          s = getQuickSettingState(QuickSettingsData.QS_ID_APNLIST);
          if (s != null){
            s.setText(apnText).setIcon(apnIconId).setStatus(status);
            refreshQuickSetting(QuickSettingsData.QS_ID_APNLIST);
          }
          refreshNetworkAble();
    }
    @Override
    public void notifyNetworkSelected(boolean cardOneUsing,boolean cardTwoUsing) {
      mNetworkCanSelected[0] = cardOneUsing;
      mNetworkCanSelected[1] = cardTwoUsing;
      onNetworkSelected();
      if(LogHelper.NOLOGGING)LogHelper.sv(TAG,"notifyNetworkSelected mNetworkCanSelected[0]:" + mNetworkCanSelected[0] + " mNetworkCanSelected[1]:" + mNetworkCanSelected[1]);
    }


  }

  public void changeDataNetworkState(){
    //if (mSceneMode == GlobalKeys.SceneMode.MODEL_FLIGHT){
    LogHelper.sd(TAG, "changeDataNetworkState...................mSceneMode=" + mSceneMode);
    LogHelper.sd(TAG, "changeDataNetworkState...................enable=" + mSceneMode);
    boolean enable = (Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0)==1);
    if (enable){
      showTaostMsg(mContext.getString(R.string.status_bar_airplane_mode_send_note));
      return;
    }
    mDataNetworkController.changeState();
    if (!bHasDataRoaming){
//      changeDataRoaming();
    }
  }

  public void changeApn(){
       boolean enable = (Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0)==1);
//     if (mSceneMode == GlobalKeys.SceneMode.MODEL_FLIGHT){
       if(enable){
      showTaostMsg(mContext.getString(R.string.status_bar_airplane_mode_send_note));
      return;
    }
    mDataNetworkController.changeApn();
  }

  public void changeCButton(){
      State s = getQuickSettingState(QuickSettingsData.QS_ID_CBUTTON);
      if (s != null){
          int newV = s.status == State.STATUS_ENABLE ? State.STATUS_DISABLE : State.STATUS_ENABLE;
          setCbuttonStatus(newV);
      }
  }
  public void changeNfc() {
    State s = getQuickSettingState(QuickSettingsData.QS_ID_NFC);
    if (s != null){
      try {
//        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(mContext);
        //NfcAdapter adapter = NfcAdapter.getDefaultAdapter();
        NfcAdapter adapter = NfcAdapter.getNfcAdapter(mContext);
        int state=adapter.getAdapterState();
        LogHelper.sd(TAG, "changeNfc NfcAdapter.getDefaultAdapter mContext" + mContext+" state="+state);
        if(s.status==State.STATUS_ENABLE&&NfcAdapter.STATE_OFF==state) {
          LogHelper.sd(TAG, "changeNfc s.status==State.STATUS_ENABLE && NfcAdapter.STATE_OFF==state return");
          onNfcStateChange(state);
          return;
        }
                if(s.status==State.STATUS_DISABLE&&NfcAdapter.STATE_ON==state) {
                  LogHelper.sd(TAG, "changeNfc s.status==State.STATUS_DISABLE&&NfcAdapter.STATE_ON==state return");
                  onNfcStateChange(state);
                  return;
        }
        //end wz
        if (s.status == State.STATUS_ENABLE){
          LogHelper.sd(TAG, "changeNfc adapter.disable()");
          adapter.disable();
        } else if (s.status == State.STATUS_DISABLE) {
          LogHelper.sd(TAG, "changeNfc adapter.enable()");
          adapter.enable();
        }
      } catch (Exception e) {
        LogHelper.sd(TAG, "changeNfc " + e.toString());
        e.printStackTrace();
      }
    }
  }

  public void changeDrivingMode(){
    int value = CurrentUserTracker.getIntForCurrentUser(DRIVING_MODE, 0);
    int newValue = value == 0 ? 1 : 0;
    LogHelper.sd(TAG, String.format("changeDrivingMode value = %d nevValue = %d", value, newValue));
    CurrentUserTracker.putIntForCurrentUser(DRIVING_MODE, newValue);
  }
  //wangshaocheng
  public void changeMagicalMode(){
    int value = CurrentUserTracker.getIntForCurrentUser(MAGIC_SOUND_SETTING, 0);
    int newValue = value == 0 ? 1 : 0;
    LogHelper.sd(TAG, String.format("changeMagicalMode value = %d nevValue = %d", value, newValue));
    CurrentUserTracker.putIntForCurrentUser(MAGIC_SOUND_SETTING, newValue);
  }

  public void changeTTWindowMode() {
//    mSettingManager.loadData();//modify
//    boolean isOpenTTWindow = mSettingManager.getBooleanData(SettingManager.SHOW_TTWINDOW_KEY);
//    if(isOpenTTWindow) {
//      mSettingManager.setData(SettingManager.SHOW_TTWINDOW_KEY, false);
//    } else {
//      mSettingManager.setData(SettingManager.SHOW_TTWINDOW_KEY, true);
//    }
  }
  public void changeMultiWindow(){
    int value=CurrentUserTracker.getIntForCurrentUser("isMultiwindow", 0);
    if(value==1){
      CurrentUserTracker.putIntForCurrentUser("isMultiwindow",0); 
    } else {
      CurrentUserTracker.putIntForCurrentUser("isMultiwindow",1); 
    }

  }

  public void changeSound(){
    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        switch (mAudioManager.getRingerMode())
        {
        case AudioManager.RINGER_MODE_SILENT:
          mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_VIBRATE = " + AudioManager.RINGER_MODE_VIBRATE);
            break;
//        case AudioManager.RINGER_MODE_NORMAL:
//            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
//            break;
        case AudioManager.RINGER_MODE_VIBRATE:
          if(mHwMuteEnable)
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
          else
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            break;
        default:
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_SILENT = " + AudioManager.RINGER_MODE_SILENT);
        }
        onSoundChange(-1);
  }
  public void onSoundChange(int volume){
    onSceneModeChange();
    State s = getQuickSettingState(QuickSettingsData.QS_ID_SOUND);
    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    int mode = mAudioManager.getRingerMode();
        LogHelper.sv(TAG, "onSoundChange mAudioManager.getRingerMode() = " + mode + " mHwMuteEnable = " + mHwMuteEnable);
        if (mHwMuteEnable && mode == AudioManager.RINGER_MODE_NORMAL){
        s.setIcon(R.drawable.ic_qs_vibrate_enable);
          s.setStatus(State.STATUS_ENABLE);
            LogHelper.sv(TAG, "onSoundChange set ic_qs_vibrate_enable");
        }else{
          switch (mode)
          {
          case AudioManager.RINGER_MODE_NORMAL:
          s.setIcon(R.drawable.ic_qs_sound_enable);
            s.setStatus(State.STATUS_ENABLE);
              LogHelper.sv(TAG, "onSoundChange set ic_qs_sound_enable");
              break;
          case AudioManager.RINGER_MODE_VIBRATE:
          s.setIcon(R.drawable.ic_qs_vibrate_enable);
            s.setStatus(State.STATUS_ENABLE);
              LogHelper.sv(TAG, "onSoundChange set ic_qs_vibrate_enable");
              break;
          case AudioManager.RINGER_MODE_SILENT:
          default:
          s.setIcon(R.drawable.ic_qs_sound_disable);
            s.setStatus(State.STATUS_DISABLE);
              LogHelper.sv(TAG, "onSoundChange set ic_qs_sound_disable");
          }
        }
        refreshQuickSetting(QuickSettingsData.QS_ID_SOUND);
  }

  public void onSuperSecureModeChange(){
    String superType = SystemProperties.get("persist.yulong.supermode", "0");
    int iconId = R.drawable.ic_qs_super_secure_mode_disable;
    int status = State.STATUS_DISABLE;
    int textId = R.string.qs_label_super_secure_mode;
    LogHelper.sd(TAG, "onSuperSecureModeChange superType = " + superType);
    if(superType.equals("1")){
      status=State.STATUS_FLAG;
      iconId=R.drawable.ic_qs_super_secure_mode_enable;
    }else if(superType.equals("0")){
      status=State.STATUS_DISABLE;
      iconId=R.drawable.ic_qs_super_secure_mode_disable;
    }
    State s = getQuickSettingState(QuickSettingsData.QS_ID_SUPER_SECURE_MODE);
    if (s != null){
      s.setIcon(iconId).setStatus(status).setText(textId);
      refreshQuickSetting(QuickSettingsData.QS_ID_SUPER_SECURE_MODE);
    }
  }

  public void onFlashLightStateChange(){
    String isOpen = SystemProperties.get("sys.yulong.flashlight", "0");
    LogHelper.sd(TAG, "onFlashLightStateChange isOpen="+isOpen);
    Boolean enable = false;
    int iconId = R.drawable.ic_qs_flashlight_off;
    int textId = R.string.qs_label_flashlight;
    if(isOpen.equals("0")){
      enable = false;
      iconId = R.drawable.ic_qs_flashlight_off;
    }else if(isOpen.equals("1")){
      enable = true;
      iconId = R.drawable.ic_qs_flashlight_on;
    }

    State s = getQuickSettingState(QuickSettingsData.QS_ID_FLASHLIGHT_MODE);
    if (s != null){
      s.setIcon(iconId).setText(textId)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_FLASHLIGHT_MODE);
    }
  }
    public void sendSuperSecureModeBroadcast(){
//      yulong.intent.action.supermode
      int superType = SystemProperties.get("persist.yulong.supermode", "0").equals("0")?1:0;
        Intent intent = new Intent("yulong.intent.action.REQUEST_SUPERMODE");
      if(intent != null){
        intent.putExtra("state",superType);
        LogHelper.sd(TAG, "sendSuperSecureModeBroadcast superType =" + superType + " intent:" + intent);
          mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
      }
    }

     private void setAirplaneModeOn(boolean enabling) {
         Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                                 enabling ? 1 : 0);
         // Post the intent
         Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
         intent.putExtra("state", enabling);
         mContext.sendBroadcastAsUser(intent, UserHandle.ALL);

     }
  public void changeAirplaneMode_1(){
    int value = Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0);
    int newValue = (value == 0 ? 1 : 0);
    LogHelper.sd(TAG, String.format("changeAirplaneMode value = %d nevValue = %d", value, newValue));
    setAirplaneModeOn((newValue==1)?true:false);
    onAirplaneMode();
  }
  public void changeAirplaneMode(){
	    //QuickSettingLauncher.getInstance(mContext).goneBottomView();
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				int value = Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0);
			    int newValue = (value == 0 ? 1 : 0);
			    LogHelper.sd(TAG, String.format("changeAirplaneMode value = %d nevValue = %d", value, newValue));
			    boolean enable = (newValue == 0 ? false : true);
			    
			    Settings.Global.putInt(mContext.getContentResolver(),
						Settings.Global.AIRPLANE_MODE_ON, (newValue == 1) ? 1 : 0);
			    
			    newValue = Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0);
			    
				return newValue;
			}

			@Override
			protected void onPostExecute(Integer result) {
				int newValue = result;
				boolean enable = (newValue == 0 ? false : true);

				Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				intent.putExtra("state", (newValue == 1) ? true : false);
				mContext.sendBroadcastAsUser(intent, UserHandle.ALL);

				LogHelper.sd(TAG,"sendBroadcastForAirplaneMode is done, airplaneMode:"
								+ ((newValue == 0) ? "close" : "open"));
				Intent intent1 = new Intent(
						"yulong.intent.action.SCENE_MODE_CHANGED");
				if (newValue == 0) {
					intent1.putExtra("sceneMode", 1);
				} else {
					intent1.putExtra("sceneMode", 2);
				}
				intent1.putExtra("fromSysUI", true);
				CurrentUserTracker.sendBroadcastAsCurrentUser(intent1);
				
				State s = getQuickSettingState(QuickSettingsData.QS_ID_AIRPLANE_MODE);
				if (s != null) {
					s.setIcon(
							enable ? R.drawable.ic_qs_airplane_enable
									: R.drawable.ic_qs_airplane_disable)
							.setStatus(
									enable ? State.STATUS_ENABLE
											: State.STATUS_DISABLE);
					refreshQuickSetting(QuickSettingsData.QS_ID_AIRPLANE_MODE);
				}
				
				refreshNetworkAble();
				super.onPostExecute(result);
			}
		}.execute();
      
  }
  private void onAirplaneMode(){
    boolean enable = (Settings.Global.getInt(mContext.getContentResolver(), AIRPLANE_MODE,0)==1);
    LogHelper.sd(TAG, "onAirplaneMode enable = " + enable);

    State s = getQuickSettingState(QuickSettingsData.QS_ID_AIRPLANE_MODE);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_airplane_enable : R.drawable.ic_qs_airplane_disable)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_AIRPLANE_MODE);
    }

    refreshNetworkAble();
  }
  private void setVibrateSettingState(boolean vibrate) {
	  AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
      if (vibrate) {
          mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_ON);
          mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,AudioManager.VIBRATE_SETTING_ON);
      } else {
          mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_OFF);
          mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,AudioManager.VIBRATE_SETTING_OFF);
      }
      Settings.System.putInt(mContext.getContentResolver(),Settings.System.VIBRATE_WHEN_RINGING, vibrate ? 1 : 0);
  }
  //enable: NoSound+vibrate 
  //unable: 
  public void changeMeetingMode() {
	  AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
      switch (mAudioManager.getRingerMode())
      {
      case AudioManager.RINGER_MODE_SILENT:
      case AudioManager.RINGER_MODE_NORMAL:
    	  mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
          LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_VIBRATE = " + AudioManager.RINGER_MODE_VIBRATE);
          break;
      default:
    	  mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
          LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_NORMAL = " + AudioManager.RINGER_MODE_NORMAL);
          break;
      }
      setVibrateSettingState(true);
      onSceneModeChange();
      onRingerMode(-1);
      updateVolume();
  }

  //enable: AudioManager.RINGER_MODE_SILENT NoSound+NoVibrate
  //disable:AudioManager.RINGER_MODE_NORMAL 
  public void changeRingerMode(){
    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        switch (mAudioManager.getRingerMode())
        {
        case AudioManager.RINGER_MODE_NORMAL:
        case AudioManager.RINGER_MODE_VIBRATE:
        	mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        	setVibrateSettingState(false);
            LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_SILENT = " + AudioManager.RINGER_MODE_SILENT);
            break;
        default:
        	mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        	setVibrateSettingState(true);
            LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_NORMAL = " + AudioManager.RINGER_MODE_NORMAL);
            break;
        }
        onRingerMode(-1);
        onSceneModeChange();
        updateVolume();
  }

  private void onRingerMode(int isExtraRingerMode){
    State s = getQuickSettingState(QuickSettingsData.QS_ID_RINGER_MODE);
    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    int mode = mAudioManager.getRingerMode();
        LogHelper.sv(TAG, "onRingerMode mAudioManager.getRingerMode() = " + mode + " mHwMuteEnable = " + mHwMuteEnable);
        if (mHwMuteEnable && mode == AudioManager.RINGER_MODE_NORMAL){
          s.setIcon(R.drawable.ic_qs_ringer_enable);
          s.setStatus(State.STATUS_ENABLE);
            LogHelper.sv(TAG, "onRingerMode set ic_qs_ringer_enable");
        }else{
          switch (mode)
          {
          case AudioManager.RINGER_MODE_NORMAL:
          case AudioManager.RINGER_MODE_VIBRATE:
          s.setIcon(R.drawable.ic_qs_ringer_disable);
            s.setStatus(State.STATUS_DISABLE);
              LogHelper.sv(TAG, "onRingerMode set ic_qs_ringer_disable");

              break;
          default:
            s.setIcon(R.drawable.ic_qs_ringer_enable);
            s.setStatus(State.STATUS_ENABLE);
              LogHelper.sv(TAG, "onRingerMode set ic_qs_ringer_enable");
          }
        }
        refreshQuickSetting(QuickSettingsData.QS_ID_RINGER_MODE);

    //boolean enable = CurrentUserTracker.getIntForCurrentUser( RINGER_MODE, 0) == 1;
    //LogHelper.sd(TAG, "onRingerMode enable = " + enable);
  }
  
  public void changeVibrateRing(){
    int value = 0;
    int newValue = 0;

    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        /*switch (mAudioManager.getRingerMode())
        {
        case AudioManager.RINGER_MODE_SILENT:
          value = CurrentUserTracker.getIntForCurrentUser(RINGER_MODE, 0);
        newValue = (value == 0 ? 1 : 0);
            LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_NORMAL = " + AudioManager.RINGER_MODE_NORMAL);
            LogHelper.sd(TAG, String.format("changeVibrateRing value = %d nevValue = %d", value, newValue));
        CurrentUserTracker.putIntForCurrentUser(RINGER_MODE, newValue);
            break;
        default:
          value = CurrentUserTracker.getIntForCurrentUser(VIBRATE_RING_MODE, 0);
        newValue = (value == 0 ? 1 : 0);
            LogHelper.sv(TAG,"changeSound to AudioManager.RINGER_MODE_SILENT = " + AudioManager.RINGER_MODE_SILENT);
            LogHelper.sd(TAG, String.format("changeVibrateRing value = %d nevValue = %d", value, newValue));
        CurrentUserTracker.putIntForCurrentUser(VIBRATE_RING_MODE, newValue);
        }
        */

        newValue=(0==mAudioManager.getVibrateSetting(0))?1:0;
        LogHelper.sv(TAG,"mAudioManager.getVibrateSetting newValue = " + newValue);
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, newValue);
    if(newValue==1)
    {
      mAudioManager.setVibrateSetting(0, 1); 
      mAudioManager.setVibrateSetting(1, 1); 
    }
    else
    {
      mAudioManager.setVibrateSetting(0, 0); 
      mAudioManager.setVibrateSetting(1, 0); 
    }
    onRingerMode(-1);
    onVibrateRing();
  }
  private static final int OP_SET_ICON    = 1;
  private static final int MSG_SHIFT  = 16;
  private static final int MSG_ICON                       = 1 << MSG_SHIFT;
  private void onVibrateRing(){
    boolean enable = false;

    AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        /*switch (mAudioManager.getRingerMode())
        {
        case AudioManager.RINGER_MODE_SILENT:
          enable = CurrentUserTracker.getIntForCurrentUser( RINGER_MODE, 0) == 1;
          LogHelper.sd(TAG, "onVibrateRingMode RINGER_MODE enable = " + enable);

            break;
        default:
          enable = CurrentUserTracker.getIntForCurrentUser( VIBRATE_RING_MODE, 0) == 1;
          LogHelper.sd(TAG, "onVibrateRingMode VIBRATE_RING_MODE enable = " + enable);
        }
        */

        enable=(0==mAudioManager.getVibrateSetting(0))?false:true;
        LogHelper.sd(TAG,"getVibrateSetting enable="+enable);
    State s = getQuickSettingState(QuickSettingsData.QS_ID_VIBRATE_RING_MODE);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_vibrate_ring_enable : R.drawable.ic_qs_vibrate_ring_disable)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_VIBRATE_RING_MODE);
    }
    updateVolume();
  }
  private boolean mVolumeVisible;
  private int curModel = 1;
  private static boolean mRingerIconVisible=false;
  private final void updateVolume() {
	    StatusBarManager mService = (StatusBarManager)mContext.getSystemService(Context.STATUS_BAR_SERVICE);
	    SystemManager mSystemManager = null;
	    try {
	      //===modify by ty
	      //mSystemManager = (SystemManager) this.mContext.getSystemService(GlobalKeys.SYS_SERVICE);
	    } catch (Exception e) {
	      // TODO: handle exception
	    }
	    if(mSystemManager != null)curModel = mSystemManager.getCurrentModel();

	    AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	        final int ringerMode = audioManager.getRingerMode();
	        boolean visible = ringerMode == AudioManager.RINGER_MODE_SILENT ||
	                ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
	                mHwMuteEnable;
	        final int iconId;
	        String contentDescription = null;
	        LogHelper.sv(TAG, "updateVolume() ringerMode = " + ringerMode + " visible = " + visible
	            + " mHwMuteEnable = " + mHwMuteEnable
	            + " curModel = " + curModel);

	        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	        boolean enable = false;
	        enable = (0==mAudioManager.getVibrateSetting(0))?true:false;

	        if(curModel == GlobalKeys.SceneMode.MODEL_QUIET){
	          visible = mHwMuteEnable;
	        }

	        LogHelper.sv(TAG, "updateVolume() visible="+visible);

	        if (ringerMode == AudioManager.RINGER_MODE_SILENT){
	            iconId =  R.drawable.stat_sys_ringer_silent;
	            contentDescription = mContext.getString(R.string.accessibility_ringer_silent);
	            if (visible)
	            {
	                mService.setIcon("volume", iconId, 0, contentDescription);
	                mService.setIconVisibility("volume", visible);
	                mRingerIconVisible=visible;
	            }

	        } else if(ringerMode == AudioManager.RINGER_MODE_VIBRATE){
	            iconId = R.drawable.stat_sys_meeting_mode;
	            contentDescription = mContext.getString(R.string.accessibility_ringer_vibrate);
	            if (visible)
	            {
	                mService.setIcon("volume", iconId, 0, contentDescription);
	                mService.setIconVisibility("volume", visible);
	                mRingerIconVisible=visible;
	            }

	        }else{
	          iconId = R.drawable.stat_sys_meeting_mode;//stat_sys_ringer_vibrate;
	            contentDescription = mContext.getString(R.string.accessibility_ringer_vibrate);
	          if (!visible && mRingerIconVisible){
	                mService.setIcon("volume", iconId, 0, contentDescription);
	                mService.setIconVisibility("volume", visible);
	                mRingerIconVisible=visible;
	            }
	        }
	    }

  public void changeDataRoaming(){
    State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
    if (!bHasDataRoaming){
      s = getQuickSettingState(QuickSettingsData.QS_ID_MOBILEDATA);
    }
    int iphoneid = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);
    LogHelper.sv(TAG, "changeDataRoaming (s.status == State.STATUS_DISABLE)= " + (s.status == State.STATUS_DISABLE) + " iphoneid = " + iphoneid);
    if (s.status == State.STATUS_DISABLE){//
      if (iphoneid == 1){
        Settings.Global.putInt(mContext.getContentResolver(), DATA_ROAMING,1);
      }else{
        Settings.Global.putInt(mContext.getContentResolver(), DATA_ROAMING2,1);
      }
      if (bHasDataRoaming)
        showTaostMsg(mContext.getString(R.string.status_bar_enable_data_roaming));
    }else{
      if (iphoneid == 1){
        Settings.Global.putInt(mContext.getContentResolver(), DATA_ROAMING,0);
      }else{
        Settings.Global.putInt(mContext.getContentResolver(), DATA_ROAMING2,0);
      }
      if (bHasDataRoaming)
        showTaostMsg(mContext.getString(R.string.status_bar_disable_data_roaming));
    }
  }
  public void changeSingleHandOperationMode(){
    int value = CurrentUserTracker.getIntForCurrentUser(SINGLE_HAND_OPERATION_MODE, 0);
    int newValue = (value == 0 ? 1 : 0);
    LogHelper.sd(TAG, String.format("changeSingleHandOperationMode value = %d nevValue = %d", value, newValue));
    CurrentUserTracker.putIntForCurrentUser(SINGLE_HAND_OPERATION_MODE, newValue);
    onSingleHandOperationMode();
  }
  private void onSingleHandOperationMode(){
    boolean enable = CurrentUserTracker.getIntForCurrentUser( SINGLE_HAND_OPERATION_MODE, 0) == 1;
    LogHelper.sd(TAG, "onSingleHandOperationMode enable = " + enable);

    State s = getQuickSettingState(QuickSettingsData.QS_ID_SINGLE_HAND_OPERATION_MODE);
    if ( s != null){
      s.setIcon(enable ? R.drawable.ic_qs_single_hand_enable : R.drawable.ic_qs_single_hand_disable)
      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
      refreshQuickSetting(QuickSettingsData.QS_ID_SINGLE_HAND_OPERATION_MODE);
    }
  }
  public void onDataRoamingChange(String roaming){
    int iphoneid = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);
    if (!("0".equals(roaming) || "1".equals(roaming))){
      if (iphoneid == 1){
        roaming = Settings.Global.getString(mContext.getContentResolver(), DATA_ROAMING);
      }else{
        roaming = Settings.Global.getString(mContext.getContentResolver(), DATA_ROAMING2);
      }
    }
    boolean bRoaming = "1".equals(roaming);
    LogHelper.sv(TAG, "onDataRoamingChange roaming = " + bRoaming + " iphoneid = " + iphoneid);
    State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
    s.setStatus(bRoaming?State.STATUS_ENABLE:State.STATUS_DISABLE);
    s.setIcon(bRoaming?R.drawable.ic_qs_data_roaming_enable:R.drawable.ic_qs_data_roaming_disable);
    refreshQuickSetting(QuickSettingsData.QS_ID_DATA_ROAMING);
  }
  public void selectNetwork(){
    onNetworkSelected();
    State s = getQuickSettingState(QuickSettingsData.QS_ID_DATA_ROAMING);
    int nNetworkSelected = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);//CurrentUserTracker.getIntForCurrentUser( "default_data_network", 1);
    LogHelper.sv(TAG, " default_data_network = " + nNetworkSelected + " mNetworkCanSelected[0] = "
    + mNetworkCanSelected[0] + " mNetworkCanSelected[1] = " + mNetworkCanSelected[1]);
    if (nNetworkSelected == 0 && mNetworkCanSelected[1]){
      CPDataConnSettingUtils.setDefaultDataNetwork(mContext,1);
      //Settings.System.putInt(mContext.getContentResolver(), "default_data_network", 2);
      LogHelper.sv(TAG, " change default_data_network to 2");
      Intent intent = new Intent("android.yulong.intent.action.MANUAL_SWITCH_4G_NETWORK");
      intent.putExtra("phone_id", 2);
       CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
//       startSelectedNetwork(2);
      LogHelper.sd(TAG, "send broadcast intent=" + intent + ",phone_id=" + 2);
    }else if (nNetworkSelected == 1 && mNetworkCanSelected[0]){
      CPDataConnSettingUtils.setDefaultDataNetwork(mContext,0);
      //Settings.System.putInt(mContext.getContentResolver(), "default_data_network", 1);
      LogHelper.sv(TAG, " change default_data_network to 1");
      Intent intent = new Intent("android.yulong.intent.action.MANUAL_SWITCH_4G_NETWORK");
      intent.putExtra("phone_id", 1);
       CurrentUserTracker.sendBroadcastAsCurrentUser(intent);
//       startSelectedNetwork(1);
      LogHelper.sd(TAG, "send broadcast intent=" + intent + ",phone_id=" + 1);
    }
    mDataNetworkController.postRefreshApn();
  }


 public void startSelectedNetwork(int phoneid){
    Intent intent = new Intent("android.settings.switchdata.MODESELECT");
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("phoneID", phoneid);
    CurrentUserTracker.startActivityAsCurrentUser(intent);
    final StatusBarManager statusBar = (StatusBarManager) mContext
        .getSystemService(Context.STATUS_BAR_SERVICE);
    if (statusBar != null) {
      statusBar.collapsePanels();
    }
//    mContext.startActivity(intent);

  }

  public void onNetworkSelected(){
    int nNetworkSelected = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);//CurrentUserTracker.getIntForCurrentUser( "default_data_network", 1);
    if(LogHelper.NOLOGGING)LogHelper.sv(TAG, "onNetworkSelected nNetworkSelected = " + nNetworkSelected
    + " mNetworkCanSelected[0] = " + mNetworkCanSelected[0] + " mNetworkCanSelected[1] = " + mNetworkCanSelected[1]);
    State s = getQuickSettingState(QuickSettingsData.QS_ID_SELECTNETWORK);
    boolean bAble = false;
    int nSelectNextText;
    if (nNetworkSelected == 0 && mNetworkCanSelected[1]){
      bAble = true;
      nSelectNextText = R.string.status_bar_select_network2;
    }else if (nNetworkSelected == 1 && mNetworkCanSelected[0]){
      bAble = true;
      nSelectNextText = R.string.status_bar_select_network1;
    }else if(nNetworkSelected == 1){
      nSelectNextText = R.string.status_bar_select_network1;
    }else {
      nSelectNextText = R.string.status_bar_select_network2;
    }
    if (s != null){
      State sMobile = getQuickSettingState(QuickSettingsData.QS_ID_MOBILEDATA);
      boolean bEnable = true;
          if (sMobile != null) {
            bEnable = sMobile.status == State.STATUS_ENABLE;
          s.setStatus(sMobile.status);
          }
          if(LogHelper.NOLOGGING)LogHelper.sv(TAG, "onNetworkSelected bEnable = " + bEnable + " + bAble = " + bAble);
          if (bEnable){
            if (bAble)
              s.status = State.STATUS_ENABLE;
            else
              s.status = State.STATUS_DISABLE;
          }else{
            s.status = State.STATUS_DISABLE;
          }
          int id = 0;
          if(nNetworkSelected ==0){
            id = (bEnable?R.drawable.ic_qs_data_network1:R.drawable.ic_qs_data_network1_disable);
          }else if(nNetworkSelected ==1){
            id = (bEnable?R.drawable.ic_qs_data_network2:R.drawable.ic_qs_data_network2_disable);
          }else{
            id = (bEnable?R.drawable.ic_qs_data_network1:R.drawable.ic_qs_data_network1_disable);
          }
      s.setIcon(id);
      s.setText(nSelectNextText);
      refreshQuickSetting(QuickSettingsData.QS_ID_SELECTNETWORK);
    }
  }
	public void changeNightMode() {
		mNightModeController.setNightMode(!mNightModeController.isEnabled());
		onNightModeChange();
	}
	  private class NightModeControllerCallback implements NightModeController.Listener {
		@Override
		public void onNightModeChanged() {
			onNightModeChange();
		}
		@Override
		public void onTwilightAutoChanged() {
		}
	  }
	public void onNightModeChange(){
		boolean enable = mNightModeController.isEnabled();;
	    LogHelper.sd(TAG,"onNightModeChanged enable="+enable);
	    State s = getQuickSettingState(QuickSettingsData.QS_ID_NIGHT_MODE);
	    if ( s != null){
	      s.setIcon(enable ? R.drawable.ic_qs_night_mode_enable
	                : R.drawable.ic_qs_night_mode_disable)
	      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
	      refreshQuickSetting(QuickSettingsData.QS_ID_NIGHT_MODE);
	    }
	}
	private class ZenModeControllerCallback extends ZenModeController.Callback{
		@Override
        public void onZenChanged(int zen) {
			onDndModeChange();
        }
        @Override
        public void onConfigChanged(ZenModeConfig config) {
        }
	}
	public void onDndModeChange(){
		boolean enable = mZenModeController.isZenAvailable() && 
				mZenModeController.getZen() != Global.ZEN_MODE_OFF;
	    LogHelper.sd(TAG,"onDndModeChange enable="+enable);
	    State s = getQuickSettingState(QuickSettingsData.QS_ID_DND_MODE);
	    if ( s != null){
	      s.setIcon(enable ? R.drawable.ic_qs_dnd_on
	                : R.drawable.ic_qs_dnd_off)
	      .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
	      refreshQuickSetting(QuickSettingsData.QS_ID_DND_MODE);
	    }
	}
	public void onWfcModeChange(int state) {
		boolean enable = state == 1 ? true : false;
		LogHelper.sd(TAG, "onWfcModeChange enable=" + state);
		State s = getQuickSettingState(QuickSettingsData.QS_ID_WFC_MODE);
		if (s != null) {
			s.setIcon(enable ? R.drawable.ic_qs_wifi_calling_on
							: R.drawable.ic_qs_wifi_calling_off)
			 .setStatus(enable ? State.STATUS_ENABLE : State.STATUS_DISABLE);
			refreshQuickSetting(QuickSettingsData.QS_ID_WFC_MODE);
		}
	}
}
