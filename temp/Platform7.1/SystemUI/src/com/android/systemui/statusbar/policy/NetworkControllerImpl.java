/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.MathUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.AsyncChannel;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;

/** Platform implementation of the network controller. **/
public class NetworkControllerImpl extends BroadcastReceiver
        implements NetworkController, DemoMode, DataUsageController.NetworkNameProvider {
    // debug
    static final String TAG = "NetworkController";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    // additional diagnostics, but not logspew
    static final boolean CHATTY =  true;//Log.isLoggable(TAG + "Chat", Log.DEBUG);

    private static final int EMERGENCY_NO_CONTROLLERS = 0;
    private static final int EMERGENCY_FIRST_CONTROLLER = 100;
    private static final int EMERGENCY_VOICE_CONTROLLER = 200;
    private static final int EMERGENCY_NO_SUB = 300;
    private static final String ACTION_EMBMS_STATUS = "com.qualcomm.intent.EMBMS_STATUS";

    protected final Context mContext;
    protected final TelephonyManager mPhone;
    protected final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final SubscriptionManager mSubscriptionManager;
    protected final boolean mHasMobileDataFeature;
    private final SubscriptionDefaults mSubDefaults;
    private final DataSaverController mDataSaverController;
    private Config mConfig;

    // Subcontrollers.
    @VisibleForTesting
    final WifiSignalController mWifiSignalController;

    @VisibleForTesting
    final EthernetSignalController mEthernetSignalController;

    @VisibleForTesting
    final Map<Integer, MobileSignalController> mMobileSignalControllers =
            new HashMap<Integer, MobileSignalController>();
    // When no SIMs are around at setup, and one is added later, it seems to default to the first
    // SIM for most actions.  This may be null if there aren't any SIMs around.
    private MobileSignalController mDefaultSignalController;
    private final AccessPointControllerImpl mAccessPoints;
    private final DataUsageController mDataUsageController;

    protected boolean mInetCondition; // Used for Logging and demo.

    // BitSets indicating which network transport types (e.g., TRANSPORT_WIFI, TRANSPORT_MOBILE) are
    // connected and validated, respectively.
    private final BitSet mConnectedTransports = new BitSet();
    private final BitSet mValidatedTransports = new BitSet();

    // States that don't belong to a subcontroller.
    protected boolean mAirplaneMode = false;
    private boolean mHasNoSims;
    private Locale mLocale = null;
    // This list holds our ordering.
    private List<SubscriptionInfo> mCurrentSubscriptions = new ArrayList<>();

    @VisibleForTesting
    boolean mListening;

    // The current user ID.
    private int mCurrentUserId;

    private OnSubscriptionsChangedListener mSubscriptionListener;

    // Handler that all broadcasts are received on.
    //private final Handler mReceiverHandler;
    // Handler that all callbacks are made on.
    private final CallbackHandler mCallbackHandler;

    private int mEmergencySource;
    private boolean mIsEmergency;

    @VisibleForTesting
    ServiceState mLastServiceState;
    private boolean mUserSetup;
    
    //===modify by ty begin
    // telephony
    boolean mHspaDataDistinguishable;
    boolean mDataConnected;
    IccCardConstants.State mSimState = IccCardConstants.State.READY;
    int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    int mDataState = TelephonyManager.DATA_DISCONNECTED;
    int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
    ServiceState mServiceState;
    SignalStrength mSignalStrength;
    int[] mDataIconList = TelephonyIcons.DATA_G[0];
    String mNetworkName;
    String mNetworkNameDefault;
    String mNetworkNameSeparator;
    String mSpn;
    String mPlmn;
    String mOriginalTelephonyPlmn;
    String mOriginalTelephonySpn;
    int mPhoneSignalIconId;
    int mQSPhoneSignalIconId;
    int mDataDirectionIconId; // data + data direction on phones
    int mDataSignalIconId;
    int mDataTypeIconId;
    int mQSDataTypeIconId;
    int mAirplaneIconId;
    int mNoSimIconId;
    int mLastSimIconId;
    int mMobileActivityIconId; // overlay arrows for data direction
    int mLastMobileActivityIconId;
    private boolean mIsEmbmsActive;
    boolean mDataActive;
    boolean mNoSim;
    int mLastSignalLevel;
    boolean mShowPhoneRSSIForData = false;
    boolean mShowAtLeastThreeGees = false;
    boolean mShowSpn = false;
    boolean mShowPlmn = false;
    boolean mAlwaysShowCdmaRssi = false;
    boolean mShow4GforLTE = false;
    boolean mShowRsrpSignalLevelforLTE = false;

    private String mCarrierText = "";

    String mContentDescriptionPhoneSignal;
    String mContentDescriptionWifi;
    String mContentDescriptionWimax;
    String mContentDescriptionCombinedSignal;
    String mContentDescriptionDataType;

    // wifi
    boolean mWifiEnabled, mWifiConnected;
    int mWifiRssi, mWifiLevel;
    String mWifiSsid;
    int mWifiIconId = 0;
    int mQSWifiIconId = 0;
    int mWifiActivityIconId = 0; // overlay arrows for wifi direction
    int mWifiActivity = WifiManager.DATA_ACTIVITY_NONE;

    // bluetooth
    protected boolean mBluetoothTethered = false;
    protected int mBluetoothTetherIconId =
        com.android.internal.R.drawable.stat_sys_tether_bluetooth;

    //wimax
    protected boolean mWimaxSupported = false;
    protected boolean mIsWimaxEnabled = false;
    protected boolean mWimaxConnected = false;
    protected boolean mWimaxIdle = false;
    protected int mWimaxIconId = 0;
    protected int mWimaxSignal = 0;
    protected int mWimaxState = 0;
    protected int mWimaxExtraState = 0;
    protected int mDataServiceState = ServiceState.STATE_OUT_OF_SERVICE;

    // data connectivity (regardless of state, can we access the internet?)
    // state of inet connection - 0 not connected, 100 connected
    protected boolean mConnected = false;
    protected int mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
    protected String mConnectedNetworkTypeName;
    protected int mLastConnectedNetworkType = ConnectivityManager.TYPE_NONE;

    protected int mLastInetCondition = 0;
    protected static final int INET_CONDITION_THRESHOLD = 50;

    protected boolean mLastAirplaneMode = true;

    private Locale mLastLocale = null;
    
    // our ui
    ArrayList<ImageView> mPhoneSignalIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mDataDirectionIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mDataDirectionOverlayIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mWifiIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mWimaxIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mCombinedSignalIconViews = new ArrayList<ImageView>();
    ArrayList<ImageView> mDataTypeIconViews = new ArrayList<ImageView>();
    ArrayList<TextView> mCombinedLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mMobileLabelViews = new ArrayList<TextView>();
    ArrayList<TextView> mWifiLabelViews = new ArrayList<TextView>();
    //ArrayList<StatusBarHeaderView> mEmergencyViews = new ArrayList<StatusBarHeaderView>();//===modify by ty
    ArrayList<SignalCluster> mSignalClusters = new ArrayList<SignalCluster>();
    ArrayList<NetworkSignalChangedCallback> mSignalsChangedCallbacks =
            new ArrayList<NetworkSignalChangedCallback>();
    int mLastPhoneSignalIconId = -1;
    int mLastDataDirectionIconId = -1;
    int mLastDataDirectionOverlayIconId = -1;
    int mLastWifiIconId = -1;
    int mLastWifiActivityIconId = -1;
    int mLastWimaxIconId = -1;
    int mLastCombinedSignalIconId = -1;
    int mLastDataTypeIconId = -1;
    String mLastCombinedLabel = "";
    protected static final String ACTION_NETWORK_OFFLINE_MODE = "yulong.intent.action.ACTION_NETWORK_OFFLINE_MODE";
    protected static final String EXTRA_OFFLINE_MODE = "mOfflineMode";
    protected static final String EXTRA_SLOT_ID = "mSlotId";
    
    public interface SignalCluster {//according to NetworkController.SignalCallback
        void setWifiIndicators(boolean visible, int strengthIcon,
		int activityIcon, String contentDescription);
        void setMobileDataIndicators(boolean visible, int strengthIcon,
	        int activityIcon, int typeIcon, String contentDescription,
		String typeContentDescription, boolean roaming,
                boolean isTypeIconWide, int noSimIcon);
        void setIsAirplaneMode(boolean is, int airplaneIcon);
    }
    //===modify by ty end

    /**
     * Construct this controller object and register for updates.
     */
    public NetworkControllerImpl(Context context, Looper bgLooper) {
        this(context, (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE),
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE),
                SubscriptionManager.from(context), Config.readConfig(context), bgLooper,
                new CallbackHandler(),
                new AccessPointControllerImpl(context, bgLooper),
                new DataUsageController(context),
                new SubscriptionDefaults());
        //mReceiverHandler.post(mRegisterListeners);
//        registerListeners();
    }

    @VisibleForTesting
    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager,
            TelephonyManager telephonyManager, WifiManager wifiManager,
            SubscriptionManager subManager, Config config, Looper bgLooper,
            CallbackHandler callbackHandler,
            AccessPointControllerImpl accessPointController,
            DataUsageController dataUsageController,
            SubscriptionDefaults defaultsHandler) {
        mContext = context;
        mConfig = config;
        //mReceiverHandler = new Handler(bgLooper);
        mCallbackHandler = callbackHandler;
        mDataSaverController = new DataSaverController(context);

        mSubscriptionManager = subManager;
        mSubDefaults = defaultsHandler;
        mConnectivityManager = connectivityManager;
        mHasMobileDataFeature =
                mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);

        // telephony
        mPhone = telephonyManager;
        TelephonyIcons.readIconsFromXml(context);
//        registerPhoneStateListener(context);

        // wifi
        mWifiManager = wifiManager;

        mLocale = mContext.getResources().getConfiguration().locale;
        mAccessPoints = accessPointController;
        mDataUsageController = dataUsageController;
        mDataUsageController.setNetworkController(this);
        // TODO: Find a way to move this into DataUsageController.
        mDataUsageController.setCallback(new DataUsageController.Callback() {
            @Override
            public void onMobileDataEnabled(boolean enabled) {
                mCallbackHandler.setMobileDataEnabled(enabled);
            }
        });
        mWifiSignalController = new WifiSignalController(mContext, mHasMobileDataFeature,
                mCallbackHandler, this);

        mEthernetSignalController = new EthernetSignalController(mContext, mCallbackHandler, this);

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode(true /* force callback */);
        registerListeners();
    }

    public DataSaverController getDataSaverController() {
        return mDataSaverController;
    }

    private void registerListeners() {
    	registerPhoneStateListener(mContext);
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.registerListener();
        }
        if (mSubscriptionListener == null) {
            mSubscriptionListener = new SubListener();
        }
        mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionListener);

        // broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        filter.addAction(ACTION_NETWORK_OFFLINE_MODE);
        filter.addAction(TelephonyIntents.ACTION_CA_STATE_CHANGED);        
        filter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mWimaxSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (MobileSignalController.isCarrierOneSupported()) {
            filter.addAction(ACTION_EMBMS_STATUS);
        }
        if(mWimaxSupported) {
            filter.addAction(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION);
        }
        mContext.registerReceiver(this, filter);//, null, mReceiverHandler
        mListening = true;

        updateMobileControllers();
    }

    private void unregisterListeners() {
        mListening = false;
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.unregisterListener();
        }
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionListener);
        mContext.unregisterReceiver(this);
    }

    public int getConnectedWifiLevel() {
        return mWifiSignalController.getState().level;
    }

    @Override
    public AccessPointController getAccessPointController() {
        return mAccessPoints;
    }

    @Override
    public DataUsageController getMobileDataController() {
        return mDataUsageController;
    }

    public void addEmergencyListener(EmergencyListener listener) {
        mCallbackHandler.setListening(listener, true);
        mCallbackHandler.setEmergencyCallsOnly(isEmergencyOnly());
    }

    public void removeEmergencyListener(EmergencyListener listener) {
        mCallbackHandler.setListening(listener, false);
    }

    public boolean hasMobileDataFeature() {
        return mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    private MobileSignalController getDataController() {
        int dataSubId = mSubDefaults.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(dataSubId)) {
            if (DEBUG) Log.e(TAG, "No data sim selected");
            return mDefaultSignalController;
        }
        if (mMobileSignalControllers.containsKey(dataSubId)) {
            return mMobileSignalControllers.get(dataSubId);
        }
        if (DEBUG) Log.e(TAG, "Cannot find controller for data sub: " + dataSubId);
        return mDefaultSignalController;
    }

    public String getMobileDataNetworkName() {
        MobileSignalController controller = getDataController();
        return controller != null ? controller.getState().networkNameData : "";
    }

    public boolean isEmergencyOnly() {
        if (mMobileSignalControllers.size() == 0) {
            // When there are no active subscriptions, determine emengency state from last
            // broadcast.
            mEmergencySource = EMERGENCY_NO_CONTROLLERS;
            return mLastServiceState != null && mLastServiceState.isEmergencyOnly();
        }
        int voiceSubId = mSubDefaults.getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(voiceSubId)) {
            for (MobileSignalController mobileSignalController :
                                            mMobileSignalControllers.values()) {
                if (!mobileSignalController.getState().isEmergency) {
                    mEmergencySource = EMERGENCY_FIRST_CONTROLLER
                            + mobileSignalController.mSubscriptionInfo.getSubscriptionId();
                    if (DEBUG) Log.d(TAG, "Found emergency " + mobileSignalController.mTag);
                    return false;
                }
            }
        }
        if (mMobileSignalControllers.containsKey(voiceSubId)) {
            mEmergencySource = EMERGENCY_VOICE_CONTROLLER + voiceSubId;
            if (DEBUG) Log.d(TAG, "Getting emergency from " + voiceSubId);
            return mMobileSignalControllers.get(voiceSubId).getState().isEmergency;
        }
        if (DEBUG) Log.e(TAG, "Cannot find controller for voice sub: " + voiceSubId);
        mEmergencySource = EMERGENCY_NO_SUB + voiceSubId;
        // Something is wrong, better assume we can't make calls...
        return true;
    }

    /**
     * Emergency status may have changed (triggered by MobileSignalController),
     * so we should recheck and send out the state to listeners.
     */
    void recalculateEmergency() {
        mIsEmergency = isEmergencyOnly();
        mCallbackHandler.setEmergencyCallsOnly(mIsEmergency);
    }

    public void addSignalCallback(SignalCallback cb) {
        cb.setSubs(mCurrentSubscriptions);
        cb.setIsAirplaneMode(new IconState(mAirplaneMode,
                TelephonyIcons.FLIGHT_MODE_ICON, R.string.accessibility_airplane_mode, mContext));
        cb.setNoSims(mHasNoSims);
        mWifiSignalController.notifyListeners(cb);
        mEthernetSignalController.notifyListeners(cb);
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners(cb);
        }
        mCallbackHandler.setListening(cb, true);
    }

    @Override
    public void removeSignalCallback(SignalCallback cb) {
        mCallbackHandler.setListening(cb, false);
    }

    @Override
    public void setWifiEnabled(final boolean enabled) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                // Disable tethering if enabling Wifi
                final int wifiApState = mWifiManager.getWifiApState();
                if (enabled && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                        (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                    mWifiManager.setWifiApEnabled(null, false);
                }

                mWifiManager.setWifiEnabled(enabled);
                return null;
            }
        }.execute();
    }

    @Override
    public void onUserSwitched(int newUserId) {
        mCurrentUserId = newUserId;
        mAccessPoints.onUserSwitched(newUserId);
        updateConnectivity();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CHATTY) {
            Log.d(TAG, "onReceive: intent=" + intent);
        }
        final String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity();
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            refreshLocale();
            updateAirplaneMode(false);
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED)) {
            // We are using different subs now, we might be able to make calls.
            recalculateEmergency();
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
            // Notify every MobileSignalController so they can know whether they are the
            // data sim or not.
            for (MobileSignalController controller : mMobileSignalControllers.values()) {
                controller.handleBroadcast(intent);
            }
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            // Might have different subscriptions now.
            updateMobileControllers();
        } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
            for (MobileSignalController controller : mMobileSignalControllers.values()) {
                controller.handleBroadcast(intent);
            }
        } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
            mLastServiceState = ServiceState.newFromBundle(intent.getExtras());
            if (mMobileSignalControllers.size() == 0) {
                // If none of the subscriptions are active, we might need to recalculate
                // emergency state.
                recalculateEmergency();
            }
        } else if (action.equals(ACTION_EMBMS_STATUS)) {
            mIsEmbmsActive = intent.getBooleanExtra("ACTIVE", false);
            Log.d(TAG,"EMBMS_STATUS On Receive:isEmbmsactive=" + mIsEmbmsActive);
            for (MobileSignalController controller : mMobileSignalControllers.values()) {
                 controller.notifyListeners();
            }
        } else {
            int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                if (mMobileSignalControllers.containsKey(subId)) {
                    mMobileSignalControllers.get(subId).handleBroadcast(intent);
                } else {
                    // Can't find this subscription...  We must be out of date.
                    updateMobileControllers();
                }
            } else {
                // No sub id, must be for the wifi.
                mWifiSignalController.handleBroadcast(intent);
            }
        }
    }

    public void onConfigurationChanged() {
        mConfig = Config.readConfig(mContext);
//        mReceiverHandler.post(new Runnable() {
//            @Override
//            public void run() {
                handleConfigurationChanged();
    }

    @VisibleForTesting
    void handleConfigurationChanged() {
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.setConfiguration(mConfig);
        }
        refreshLocale();
    }

    public boolean isEmbmsActive() {
        return mIsEmbmsActive;
    }
    protected void updateMobileControllers() {
        if (!mListening) {
            return;
        }
        doUpdateMobileControllers();
    }

    @VisibleForTesting
    void doUpdateMobileControllers() {
        List<SubscriptionInfo> subscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            subscriptions = Collections.emptyList();
        }
        // If there have been no relevant changes to any of the subscriptions, we can leave as is.
        if (hasCorrectMobileControllers(subscriptions)) {
            // Even if the controllers are correct, make sure we have the right no sims state.
            // Such as on boot, don't need any controllers, because there are no sims,
            // but we still need to update the no sim state.
            updateNoSims();
            return;
        }
        setCurrentSubscriptions(subscriptions);
        updateNoSims();
        recalculateEmergency();
    }

    @VisibleForTesting
    protected void updateNoSims() {
        boolean hasNoSims = mHasMobileDataFeature && mMobileSignalControllers.size() == 0;
        if (hasNoSims != mHasNoSims) {
            mHasNoSims = hasNoSims;
            mCallbackHandler.setNoSims(mHasNoSims);
        }
    }

    @VisibleForTesting
    void setCurrentSubscriptions(List<SubscriptionInfo> subscriptions) {
        Collections.sort(subscriptions, new Comparator<SubscriptionInfo>() {
            @Override
            public int compare(SubscriptionInfo lhs, SubscriptionInfo rhs) {
                return lhs.getSimSlotIndex() == rhs.getSimSlotIndex()
                        ? lhs.getSubscriptionId() - rhs.getSubscriptionId()
                        : lhs.getSimSlotIndex() - rhs.getSimSlotIndex();
            }
        });
        mCurrentSubscriptions = subscriptions;

        HashMap<Integer, MobileSignalController> cachedControllers =
                new HashMap<Integer, MobileSignalController>(mMobileSignalControllers);
        mMobileSignalControllers.clear();
        final int num = subscriptions.size();
        for (int i = 0; i < num; i++) {
            int subId = subscriptions.get(i).getSubscriptionId();
            // If we have a copy of this controller already reuse it, otherwise make a new one.
            if (cachedControllers.containsKey(subId)) {
                mMobileSignalControllers.put(subId, cachedControllers.remove(subId));
            } else {
                MobileSignalController controller = new MobileSignalController(mContext, mConfig,
                        mHasMobileDataFeature, mPhone, mCallbackHandler,
                        this, subscriptions.get(i), mSubDefaults, mContext.getMainLooper());//mReceiverHandler.getLooper()
                controller.setUserSetupComplete(mUserSetup);
                mMobileSignalControllers.put(subId, controller);
                if (subscriptions.get(i).getSimSlotIndex() == 0) {
                    mDefaultSignalController = controller;
                }
                if (mListening) {
                    controller.registerListener();
                }
            }
        }
        if (mListening) {
            for (Integer key : cachedControllers.keySet()) {
                if (cachedControllers.get(key) == mDefaultSignalController) {
                    mDefaultSignalController = null;
                }
                cachedControllers.get(key).unregisterListener();
            }
        }
        mCallbackHandler.setSubs(subscriptions);
        notifyAllListeners();

        // There may be new MobileSignalControllers around, make sure they get the current
        // inet condition and airplane mode.
        pushConnectivityToSignals();
        updateAirplaneMode(true /* force */);
    }

    public void setUserSetupComplete(final boolean userSetup) {
//        mReceiverHandler.post(new Runnable() {
//            @Override
//            public void run() {
                handleSetUserSetupComplete(userSetup);
    }

    @VisibleForTesting
    void handleSetUserSetupComplete(boolean userSetup) {
        mUserSetup = userSetup;
        for (MobileSignalController controller : mMobileSignalControllers.values()) {
            controller.setUserSetupComplete(mUserSetup);
        }
    }

    @VisibleForTesting
    boolean hasCorrectMobileControllers(List<SubscriptionInfo> allSubscriptions) {
        if (allSubscriptions.size() != mMobileSignalControllers.size()) {
            return false;
        }
        for (SubscriptionInfo info : allSubscriptions) {
            if (!mMobileSignalControllers.containsKey(info.getSubscriptionId())) {
                return false;
            }
        }
        return true;
    }

    protected void updateAirplaneMode(boolean force) {
        boolean airplaneMode = (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
        if (airplaneMode != mAirplaneMode || force) {
            mAirplaneMode = airplaneMode;
            for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
                mobileSignalController.setAirplaneMode(mAirplaneMode);
            }
            notifyListeners();
        }
    }

    protected void refreshLocale() {
        Locale current = mContext.getResources().getConfiguration().locale;
        if (!current.equals(mLocale)) {
            mLocale = current;
            notifyAllListeners();
        }
    }

    /**
     * Forces update of all callbacks on both SignalClusters and
     * NetworkSignalChangedCallbacks.
     */
    private void notifyAllListeners() {
        notifyListeners();
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
        mWifiSignalController.notifyListeners();
        mEthernetSignalController.notifyListeners();
    }

    /**
     * Notifies listeners of changes in state of to the NetworkController, but
     * does not notify for any info on SignalControllers, for that call
     * notifyAllListeners.
     */
    private void notifyListeners() {
        mCallbackHandler.setIsAirplaneMode(new IconState(mAirplaneMode,
                TelephonyIcons.FLIGHT_MODE_ICON, R.string.accessibility_airplane_mode, mContext));
        mCallbackHandler.setNoSims(mHasNoSims);
    }

    /**
     * Update the Inet conditions and what network we are connected to.
     */
    protected void updateConnectivity() {
        mConnectedTransports.clear();
        mValidatedTransports.clear();
        for (NetworkCapabilities nc :
                mConnectivityManager.getDefaultNetworkCapabilitiesForUser(mCurrentUserId)) {
            for (int transportType : nc.getTransportTypes()) {
                mConnectedTransports.set(transportType);
                if (nc.hasCapability(NET_CAPABILITY_VALIDATED)) {
                    mValidatedTransports.set(transportType);
                }
            }
        }

        if (CHATTY) {
            Log.d(TAG, "updateConnectivity: mConnectedTransports=" + mConnectedTransports);
            Log.d(TAG, "updateConnectivity: mValidatedTransports=" + mValidatedTransports);
        }

        mInetCondition = !mValidatedTransports.isEmpty();

        pushConnectivityToSignals();
    }

    /**
     * Pushes the current connectivity state to all SignalControllers.
     */
    private void pushConnectivityToSignals() {
        // We want to update all the icons, all at once, for any condition change
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.updateConnectivity(mConnectedTransports, mValidatedTransports);
        }
        mWifiSignalController.updateConnectivity(mConnectedTransports, mValidatedTransports);
        mEthernetSignalController.updateConnectivity(mConnectedTransports, mValidatedTransports);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");

        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());

        pw.println("  - connectivity ------");
        pw.print("  mConnectedTransports=");
        pw.println(mConnectedTransports);
        pw.print("  mValidatedTransports=");
        pw.println(mValidatedTransports);
        pw.print("  mInetCondition=");
        pw.println(mInetCondition);
        pw.print("  mAirplaneMode=");
        pw.println(mAirplaneMode);
        pw.print("  mLocale=");
        pw.println(mLocale);
        pw.print("  mLastServiceState=");
        pw.println(mLastServiceState);
        pw.print("  mIsEmergency=");
        pw.println(mIsEmergency);
        pw.print("  mEmergencySource=");
        pw.println(emergencyToString(mEmergencySource));

        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.dump(pw);
        }
        mWifiSignalController.dump(pw);

        mEthernetSignalController.dump(pw);

        mAccessPoints.dump(pw);
    }

    private static final String emergencyToString(int emergencySource) {
        if (emergencySource > EMERGENCY_NO_SUB) {
            return "NO_SUB(" + (emergencySource - EMERGENCY_NO_SUB) + ")";
        } else if (emergencySource > EMERGENCY_VOICE_CONTROLLER) {
            return "VOICE_CONTROLLER(" + (emergencySource - EMERGENCY_VOICE_CONTROLLER) + ")";
        } else if (emergencySource > EMERGENCY_FIRST_CONTROLLER) {
            return "FIRST_CONTROLLER(" + (emergencySource - EMERGENCY_FIRST_CONTROLLER) + ")";
        } else if (emergencySource == EMERGENCY_NO_CONTROLLERS) {
            return "NO_CONTROLLERS";
        }
        return "UNKNOWN_SOURCE";
    }

    private boolean mDemoMode;
    private boolean mDemoInetCondition;
    private WifiSignalController.WifiState mDemoWifiState;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            if (DEBUG) Log.d(TAG, "Entering demo mode");
            unregisterListeners();
            mDemoMode = true;
            mDemoInetCondition = mInetCondition;
            mDemoWifiState = mWifiSignalController.getState();
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            if (DEBUG) Log.d(TAG, "Exiting demo mode");
            mDemoMode = false;
            // Update what MobileSignalControllers, because they may change
            // to set the number of sim slots.
            updateMobileControllers();
            for (MobileSignalController controller : mMobileSignalControllers.values()) {
                controller.resetLastState();
            }
            mWifiSignalController.resetLastState();
            //mReceiverHandler.post(mRegisterListeners);
            registerListeners();
            notifyAllListeners();
        } else if (mDemoMode && command.equals(COMMAND_NETWORK)) {
            String airplane = args.getString("airplane");
            if (airplane != null) {
                boolean show = airplane.equals("show");
                mCallbackHandler.setIsAirplaneMode(new IconState(show,
                        TelephonyIcons.FLIGHT_MODE_ICON, R.string.accessibility_airplane_mode,
                        mContext));
            }
            String fully = args.getString("fully");
            if (fully != null) {
                mDemoInetCondition = Boolean.parseBoolean(fully);
                BitSet connected = new BitSet();

                if (mDemoInetCondition) {
                    connected.set(mWifiSignalController.mTransportType);
                }
                mWifiSignalController.updateConnectivity(connected, connected);
                for (MobileSignalController controller : mMobileSignalControllers.values()) {
                    if (mDemoInetCondition) {
                        connected.set(controller.mTransportType);
                    }
                    controller.updateConnectivity(connected, connected);
                }
            }
            String wifi = args.getString("wifi");
            if (wifi != null) {
                boolean show = wifi.equals("show");
                String level = args.getString("level");
                if (level != null) {
                    mDemoWifiState.level = level.equals("null") ? -1
                            : Math.min(Integer.parseInt(level), WifiIcons.WIFI_LEVEL_COUNT - 1);
                    mDemoWifiState.connected = mDemoWifiState.level >= 0;
                }
                mDemoWifiState.enabled = show;
                mWifiSignalController.notifyListeners();
            }
            String sims = args.getString("sims");
            if (sims != null) {
                int num = MathUtils.constrain(Integer.parseInt(sims), 1, 8);
                List<SubscriptionInfo> subs = new ArrayList<>();
                if (num != mMobileSignalControllers.size()) {
                    mMobileSignalControllers.clear();
                    int start = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
                    for (int i = start /* get out of normal index range */; i < start + num; i++) {
                        subs.add(addSignalController(i, i));
                    }
                    mCallbackHandler.setSubs(subs);
                }
            }
            String nosim = args.getString("nosim");
            if (nosim != null) {
                mHasNoSims = nosim.equals("show");
                mCallbackHandler.setNoSims(mHasNoSims);
            }
            String mobile = args.getString("mobile");
            if (mobile != null) {
                boolean show = mobile.equals("show");
                String datatype = args.getString("datatype");
                String slotString = args.getString("slot");
                int slot = TextUtils.isEmpty(slotString) ? 0 : Integer.parseInt(slotString);
                slot = MathUtils.constrain(slot, 0, 8);
                // Ensure we have enough sim slots
                List<SubscriptionInfo> subs = new ArrayList<>();
                while (mMobileSignalControllers.size() <= slot) {
                    int nextSlot = mMobileSignalControllers.size();
                    subs.add(addSignalController(nextSlot, nextSlot));
                }
                if (!subs.isEmpty()) {
                    mCallbackHandler.setSubs(subs);
                }
                // Hack to index linearly for easy use.
                MobileSignalController controller = mMobileSignalControllers
                        .values().toArray(new MobileSignalController[0])[slot];
                controller.getState().dataSim = datatype != null;
                if (datatype != null) {
                    controller.getState().iconGroup =
                            datatype.equals("1x") ? TelephonyIcons.ONE_X :
                            datatype.equals("3g") ? TelephonyIcons.THREE_G :
                            datatype.equals("4g") ? TelephonyIcons.FOUR_G :
                            datatype.equals("4g+") ? TelephonyIcons.FOUR_G_PLUS :
                            datatype.equals("e") ? TelephonyIcons.E :
                            datatype.equals("g") ? TelephonyIcons.G :
                            datatype.equals("h") ? TelephonyIcons.H :
                            datatype.equals("lte") ? TelephonyIcons.LTE :
                            datatype.equals("roam") ? TelephonyIcons.ROAMING :
                            TelephonyIcons.UNKNOWN;
                }
                int[][] icons = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH;
                String level = args.getString("level");
                if (level != null) {
                    controller.getState().level = level.equals("null") ? -1
                            : Math.min(Integer.parseInt(level), icons[0].length - 1);
                    controller.getState().connected = controller.getState().level >= 0;
                }
                controller.getState().enabled = show;
                controller.notifyListeners();
            }
            String carrierNetworkChange = args.getString("carriernetworkchange");
            if (carrierNetworkChange != null) {
                boolean show = carrierNetworkChange.equals("show");
                for (MobileSignalController controller : mMobileSignalControllers.values()) {
                    controller.setCarrierNetworkChangeMode(show);
                }
            }
        }
    }

    private SubscriptionInfo addSignalController(int id, int simSlotIndex) {
    	//ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
        SubscriptionInfo info = new SubscriptionInfo(id, "", simSlotIndex, "", "", 0, 0, "", 0,
                null, 0, 0, ""/*, SubscriptionManager.SIM_PROVISIONED*/);
        mMobileSignalControllers.put(id, new MobileSignalController(mContext,
                mConfig, mHasMobileDataFeature, mPhone, mCallbackHandler, this, info,
                mSubDefaults, mContext.getMainLooper()));//mReceiverHandler.getLooper()
        return info;
    }

    private class SubListener extends OnSubscriptionsChangedListener {
        @Override
        public void onSubscriptionsChanged() {
            updateMobileControllers();
        }
    }

    /**
     * Used to register listeners from the BG Looper, this way the PhoneStateListeners that
     * get created will also run on the BG Looper.
     */
    private final Runnable mRegisterListeners = new Runnable() {
        @Override
        public void run() {
            registerListeners();
        }
    };

    public static class SubscriptionDefaults {
        public int getDefaultVoiceSubId() {
            return SubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        public int getDefaultDataSubId() {
            return SubscriptionManager.getDefaultDataSubscriptionId();
        }
    }

    @VisibleForTesting
    static class Config {
        boolean showAtLeast3G = false;
        boolean alwaysShowCdmaRssi = false;
        boolean show4gForLte = false;
        boolean hspaDataDistinguishable;
        boolean readIconsFromXml;
        boolean showRsrpSignalLevelforLTE;
        boolean showLocale;
        boolean showRat;

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources res = context.getResources();

            config.showAtLeast3G = res.getBoolean(R.bool.config_showMin3G);
            config.alwaysShowCdmaRssi =
                    res.getBoolean(com.android.internal.R.bool.config_alwaysUseCdmaRssi);
            config.show4gForLte = res.getBoolean(R.bool.config_show4GForLTE);
            config.hspaDataDistinguishable =
                    res.getBoolean(R.bool.config_hspa_data_distinguishable);
            config.readIconsFromXml = res.getBoolean(R.bool.config_read_icons_from_xml);
            config.showRsrpSignalLevelforLTE =
                    res.getBoolean(R.bool.config_showRsrpSignalLevelforLTE);
            config.showLocale = true;
 //                   res.getBoolean(com.android.internal.R.bool.config_monitor_locale_change);
            config.showRat = true;
 //                   res.getBoolean(com.android.internal.R.bool.config_display_rat);

            return config;
        }
    }
    
    //===modify by ty begin
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (DEBUG) {
                Log.d(TAG, "onSignalStrengthsChanged signalStrength=" + signalStrength +
                    ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
            }
            mSignalStrength = signalStrength;
//            updateIconSet();
//            updateTelephonySignalStrength();
//            refreshViews();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (DEBUG) {
                Log.d(TAG, "onServiceStateChanged voiceState=" + state.getVoiceRegState()
                        + " dataState=" + state.getDataRegState());
            }
            mServiceState = state;
            if (false) {
                /*
                 * if combined_signal is set to true only then consider data
                 * service state for signal display
                 */
                mDataServiceState = mServiceState.getDataRegState();
                if (DEBUG) {
                    Log.d(TAG, "Combining data service state " + mDataServiceState + " for signal");
                }
            }
//            updateIconSet();
//            updateTelephonySignalStrength();
//            updateDataNetType();
//            updateDataIcon();
//            updateNetworkName(mShowSpn, mSpn , mShowPlmn , mPlmn);
//            refreshViews();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DEBUG) {
                Log.d(TAG, "onCallStateChanged state=" + state);
            }
            // In cdma, if a voice call is made, RSSI should switch to 1x.
//            if (isCdma()) {
//                updateTelephonySignalStrength();
//                refreshViews();
//            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (DEBUG) {
                Log.d(TAG, "onDataConnectionStateChanged: state=" + state
                        + " type=" + networkType);
            }
            mDataState = state;
            mDataNetType = networkType;
//            updateIconSet();
//            updateDataNetType();
//            updateDataIcon();
//            refreshViews();
        }

        @Override
        public void onDataActivity(int direction) {
            if (DEBUG) {
                Log.d(TAG, "onDataActivity: direction=" + direction);
            }
            mDataActivity = direction;
//            updateDataIcon();
//            refreshViews();
        }
    };
    
    protected String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }
    protected void registerPhoneStateListener(Context context) {
        // telephony
        //mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhone.listen(mPhoneStateListener,
                          PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY);
    }
    
    protected void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;

        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            final NetworkInfo networkInfo = (NetworkInfo)
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean wasConnected = mWifiConnected;
            mWifiConnected = networkInfo != null && networkInfo.isConnected();
            // If Connected grab the signal strength and ssid
            if (mWifiConnected) {
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
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            mWifiRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            try {
            	mWifiLevel = (Integer) WifiManager.class.getMethod("YLcalculateSignalLevel",int.class,int.class).invoke(null, mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT-1);
            	mWifiLevel+=1;
            	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "updateWifiState YLcalculateSignalLevel:" + mWifiLevel);
			} catch (Exception e) {
				LogHelper.sd(TAG, "updateWifiState " + e.toString());
				mWifiLevel = WifiManager.calculateSignalLevel(mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
			}            
        }

        updateWifiIcons();
    }
    
    protected void updateWifiIcons() {//===modify by ty
//        int inetCondition = inetConditionForNetwork(ConnectivityManager.TYPE_WIFI);
//		LogHelper.sd(TAG, "updateWifiIcons before mWifiLevel:" + mWifiLevel);
//        if (mWifiConnected) {
//            mWifiIconId = WifiIcons.WIFI_SIGNAL_STRENGTH[inetCondition][mWifiLevel];
//            mQSWifiIconId = WifiIcons.QS_WIFI_SIGNAL_STRENGTH[inetCondition][mWifiLevel];
//            mContentDescriptionWifi = mContext.getString(
//                    AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH[mWifiLevel]);
//        } else {
//            if (mDataAndWifiStacked) {
//                mWifiIconId = 0;
//                mQSWifiIconId = 0;
//            } else {
//                mWifiIconId = 0;//mWifiEnabled ? R.drawable.stat_sys_wifi_signal_0 : 0;//stat_sys_wifi_signal_null
//                mQSWifiIconId = mWifiEnabled ? R.drawable.ic_qs_wifi_no_network : 0;
//            }
//            mContentDescriptionWifi = mContext.getString(R.string.accessibility_no_wifi);
//        }
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
    
 // ===== Wimax ===================================================================
    protected final void updateWimaxState(Intent intent) {
        final String action = intent.getAction();
        boolean wasConnected = mWimaxConnected;
        if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.EXTRA_4G_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mIsWimaxEnabled = (wimaxStatus ==
                    WimaxManagerConstants.NET_4G_STATE_ENABLED);
        } else if (action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION)) {
            mWimaxSignal = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_SIGNAL_LEVEL, 0);
        } else if (action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            mWimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxExtraState = intent.getIntExtra(
                    WimaxManagerConstants.EXTRA_WIMAX_STATE_DETAIL,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxConnected = (mWimaxState ==
                    WimaxManagerConstants.WIMAX_STATE_CONNECTED);
            mWimaxIdle = (mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE);
        }
//        updateDataNetType();//===modify by ty
//        updateWimaxIcons();
    }

    protected void updateWimaxIcons() {
//        if (mIsWimaxEnabled) {
//            if (mWimaxConnected) {
//                int inetCondition = inetConditionForNetwork(ConnectivityManager.TYPE_WIMAX);
//                if (mWimaxIdle)
//                    mWimaxIconId = WimaxIcons.WIMAX_IDLE;
//                else
//                    mWimaxIconId = WimaxIcons.WIMAX_SIGNAL_STRENGTH[inetCondition][mWimaxSignal];
//                mContentDescriptionWimax = mContext.getString(
//                        AccessibilityContentDescriptions.WIMAX_CONNECTION_STRENGTH[mWimaxSignal]);
//            } else {
//                mWimaxIconId = WimaxIcons.WIMAX_DISCONNECTED;
//                mContentDescriptionWimax = mContext.getString(R.string.accessibility_no_wimax);
//            }
//        } else {
//            mWimaxIconId = 0;
//        }
    }
    
    // ===== Telephony ==============================================================
    protected String getLocaleString(String originalCarrier) {
		String localeCarrier = originalCarrier.toString().toUpperCase(mContext.getResources().getConfiguration().locale);
        return localeCarrier;
    }
    
    public String appendRatToNetworkName(String operator, ServiceState state) {
        String opeartorName = "";
        if (state.getDataRegState() == ServiceState.STATE_IN_SERVICE ||
                state.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
            int voiceNetType = state.getVoiceNetworkType();
            int dataNetType =  state.getDataNetworkType();
            int chosenNetType =
                    ((dataNetType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                    ? voiceNetType : dataNetType);
            TelephonyManager tm = (TelephonyManager)mContext.getSystemService(
                    Context.TELEPHONY_SERVICE);
          //===modify by ty
            String ratString = //tm.networkTypeToString(chosenNetType);
            		"" + chosenNetType;
            opeartorName = new StringBuilder().append(operator).append(" ").append(ratString).
                    toString();
        } else {
            opeartorName = operator;
        }
        return opeartorName;
    }
    
    void notifySignalsChangedCallbacks(NetworkSignalChangedCallback cb) {
        // only show wifi in the cluster if connected or if wifi-only
        boolean wifiEnabled = mWifiEnabled && (mWifiConnected || !mHasMobileDataFeature);
        String wifiDesc = wifiEnabled ?
                mWifiSsid : null;
        boolean wifiIn = wifiEnabled && mWifiSsid != null
                && (mWifiActivity == WifiManager.DATA_ACTIVITY_INOUT
                || mWifiActivity == WifiManager.DATA_ACTIVITY_IN);
        boolean wifiOut = wifiEnabled && mWifiSsid != null
                && (mWifiActivity == WifiManager.DATA_ACTIVITY_INOUT
                || mWifiActivity == WifiManager.DATA_ACTIVITY_OUT);
        cb.onWifiSignalChanged(mWifiEnabled, mWifiConnected, mQSWifiIconId, wifiIn, wifiOut,
                mContentDescriptionWifi, wifiDesc);

        boolean mobileIn = mDataConnected && (mDataActivity == TelephonyManager.DATA_ACTIVITY_INOUT
                || mDataActivity == TelephonyManager.DATA_ACTIVITY_IN);
        boolean mobileOut = mDataConnected && (mDataActivity == TelephonyManager.DATA_ACTIVITY_INOUT
                || mDataActivity == TelephonyManager.DATA_ACTIVITY_OUT);
        if (isEmergencyOnly()) {
            cb.onMobileDataSignalChanged(false, mQSPhoneSignalIconId,
                    mContentDescriptionPhoneSignal, mQSDataTypeIconId, mobileIn, mobileOut,
                    mContentDescriptionDataType, null, mNoSim, isQsTypeIconWide(mQSDataTypeIconId));
        } else {
            if (mIsWimaxEnabled && mWimaxConnected) {
                // Wimax is special
                cb.onMobileDataSignalChanged(true, mQSPhoneSignalIconId,
                        mContentDescriptionPhoneSignal, mQSDataTypeIconId, mobileIn, mobileOut,
                        mContentDescriptionDataType, mNetworkName, mNoSim,
                        isQsTypeIconWide(mQSDataTypeIconId));
            } else {
                // Normal mobile data
                cb.onMobileDataSignalChanged(mHasMobileDataFeature, mQSPhoneSignalIconId,
                        mContentDescriptionPhoneSignal, mQSDataTypeIconId, mobileIn, mobileOut,
                        mContentDescriptionDataType, mNetworkName, mNoSim,
                        isQsTypeIconWide(mQSDataTypeIconId));
            }
        }
        cb.onAirplaneModeChanged(mAirplaneMode);
    }
    
    private boolean isQsTypeIconWide(int iconId) {
        return TelephonyIcons.QS_ICON_LTE == iconId || TelephonyIcons.QS_ICON_1X == iconId
                || TelephonyIcons.QS_ICON_3G == iconId || TelephonyIcons.QS_ICON_4G == iconId;
    }
  

	@Override
	public void addNetworkSignalChangedCallback(NetworkSignalChangedCallback cb) {
		mSignalsChangedCallbacks.add(cb);
        notifySignalsChangedCallbacks(cb);
	}

	@Override
	public void removeNetworkSignalChangedCallback(
			NetworkSignalChangedCallback cb) {
		mSignalsChangedCallbacks.remove(cb);
	}
	
	class WifiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
//                        mWifiChannel.sendMessage(Message.obtain(this,//===modify by ty
//                                AsyncChannel.CMD_CHANNEL_FULL_CONNECTION));
                    } else {
                        Log.e(TAG, "Failed to connect to wifi");
                    }
                    break;
                case WifiManager.DATA_ACTIVITY_NOTIFICATION:
                    if (msg.arg1 != mWifiActivity) {
                        mWifiActivity = msg.arg1;
//                        refreshViews();//===modify by ty
                    }
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }
	//===modify by ty end
}
