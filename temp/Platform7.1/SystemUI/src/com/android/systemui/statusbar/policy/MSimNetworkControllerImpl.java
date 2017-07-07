/*
 * Copyright (c) 2012-2014 The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.net.wimax.WimaxManagerConstants;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settingslib.net.DataUsageController;

import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
//import yulong.model.CPCAInfo;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.util.AsyncChannel;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.SignalClusterViewYuLong;
import com.android.systemui.statusbar.phone.CPDataConnSettingUtils;
import com.android.systemui.statusbar.phone.YulongConfig;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults;

public class MSimNetworkControllerImpl extends NetworkControllerImpl {

	
    // debug
    static final String TAG = "StatusBar.MSimNetworkController";
    static final boolean DEBUG = true;
    static final boolean CHATTY = true; // additional diagnostics, but not logspew
    protected int mDataUsingPhoneId = -1;// 使用的PhoneId
    protected int mDataUsingPhoneIconId = 0;// 
    protected int mLastDataUsingPhoneIconId = -1;// 
    private int CA_State =0;
    private int mDefaultPhoneId;
    int mPhoneCount = 0;
    int PHONE_ID1 = PhoneConstants.SIM_ID_1;
    int PHONE_ID2 = PhoneConstants.SIM_ID_2;
    private HashMap<Integer, Integer> mSubIdPhoneIdMap;
    ArrayList<SignalClusterViewYuLong> mSimSignalClusters = new ArrayList<SignalClusterViewYuLong>();
    ArrayList<TextView> mSubsLabelViews = new ArrayList<TextView>();
    int[] mSignalNullIconId = {R.drawable.stat_sys_signal_null, R.drawable.stat_sys_signal_null,R.drawable.stat_sys_signal_null};
    boolean mBigDataDirectionIcon = false;
    boolean mAutoHideAbsentCard;
    private int numPhones;
    String mNosimText;
    String mNoServiceText;
    
    public interface YulongSignalCluster {
        void setWifiSignalIndicator(int strengthIcon,int dataIcon);    	
        
        // 设置信号图标和类型图标phoneId 0,1,3分别表示勿1，卡2，卡3...
        void setSignalIndicators(int phoneId, int strengthIcon, int typeIcon);
        void setSlaveSignalIndicators(int phoneId, int strengthIcon, int typeIcon);
        // 设置主卡标识
        void setMainCardIndicators(int phoneId, int backgroundIcon);
        // 设置信号图标是否可见
        void setSignalVisible(int phoneId, boolean visable);
        void setRoamingIndicator(int phoneId, int iconId);        
        void setDataNetworkIndicators(int typeIconId, int activityIconId);
        void setDataNetworkVisible(boolean visible);
        // dsds模式下，离线图标显示
        void setOffLine(int phoneId, boolean bOffLine);
        // 无卡标记显示
        void setNoSimCard(int phoneId, boolean bNoSimCard);
        void setNoSimCardIcon(int phoneId,int nIcon);        
        //数据业务的卡1勿2标示
        void setDataNetworkSign(int nIcon);
        void setDataNetworkSignVisible(boolean bShow);
        
        void setAirplaneMode(boolean airplaneMode);        
        void setSignalSingleMode(int phoneId, boolean signalSingleMode);
    }
    
    /**
     * Construct this controller object and register for updates.
     */

	MSimNetworkControllerImpl(Context context,
			ConnectivityManager connectivityManager,
			TelephonyManager telephonyManager, WifiManager wifiManager,
			SubscriptionManager subManager, Config config, Looper bgLooper,
			CallbackHandler callbackHandler,
			AccessPointControllerImpl accessPointController,
			DataUsageController dataUsageController,
			SubscriptionDefaults defaultsHandler) {
		super(context, connectivityManager, telephonyManager, wifiManager, subManager,
				config, bgLooper, callbackHandler, accessPointController,
				dataUsageController, defaultsHandler);
		 //numPhones = TelephonyManager.getDefault().getPhoneCount();//
	        LogHelper.sd(TAG, "registerPhoneStateListener numPhones: " + numPhones);
	}
    public MSimNetworkControllerImpl(Context context, Looper bgLooper) {
//        super(context, bgLooper);
        this(context, (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE),
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE),
                SubscriptionManager.from(context), Config.readConfig(context), bgLooper,
                new CallbackHandler(),
                new AccessPointControllerImpl(context, bgLooper),
                new DataUsageController(context),
                new SubscriptionDefaults());
         numPhones = TelephonyManager.getDefault().getPhoneCount();
         if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "registerPhoneStateListener numPhones: " + numPhones);
    }

//    protected void createWifiHandler() {//===modify by ty
//        // wifi
//        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//        Handler handler = new MSimWifiHandler();
//        mWifiChannel = new AsyncChannel();
//        Messenger wifiMessenger = mWifiManager.getWifiServiceMessenger();
//        if (wifiMessenger != null) {
//            mWifiChannel.connect(mContext, handler, wifiMessenger);
//        }
//    }
    
    LocalPhoneStateListener []mMSimPhoneStateListener;
    @Override
    protected void registerPhoneStateListener(Context context) {
        // telephony
        //mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        //List<SubInfoRecord> subInfoList = SubscriptionManager.getActivatedSubInfoList(context);
        //if (subInfoList != null) {
            //int subCount = subInfoList.size();
            mSubIdPhoneIdMap = new HashMap<Integer, Integer>();
            mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
            if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "registerPhoneStateListener: " + mPhoneCount);
            mMSimPhoneStateListener = new LocalPhoneStateListener[mPhoneCount];
            for (int i=0; i < mPhoneCount; i++) {
                int[] subIdtemp = SubscriptionManager.getSubId(i);
                if (subIdtemp != null) {
                    int subId = subIdtemp[0];
                    LogHelper.sd(TAG, "registerPhoneStateListener subId: "+ subId);
                    LogHelper.sd(TAG, "registerPhoneStateListener slotId: "+ i);
                    //if (subInfoList.get(i).mSubId >= 0) {
                    if (subId > 0) {
                        mSubIdPhoneIdMap.put(subId, i);
                        mMSimPhoneStateListener[i] = new LocalPhoneStateListener(subId,i);
                        mPhone.listen(mMSimPhoneStateListener[i],
                                        PhoneStateListener.LISTEN_SERVICE_STATE
                                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                                        | PhoneStateListener.LISTEN_CALL_STATE
                                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                                        | PhoneStateListener.LISTEN_DATA_ACTIVITY);
                    } else {
                        //mMSimPhoneStateListener[i] = null;
                    }
                }
                if(mMSimPhoneStateListener[i] == null){
                    mMSimPhoneStateListener[i] = new LocalPhoneStateListener(i,i);                	
                }
                switch(CPDataConnSettingUtils.getSimState(mContext, i)){
                	case TelephonyManager.SIM_STATE_UNKNOWN:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.UNKNOWN;
                		break;
                	case TelephonyManager.SIM_STATE_ABSENT:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.ABSENT;
                		break;
                	case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.PIN_REQUIRED;
                		break;
                	case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.PUK_REQUIRED;
                		break;
                	case TelephonyManager.SIM_STATE_READY:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.READY;
                		break;
                	case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.CARD_IO_ERROR;
                		break;   
                	case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                	default:
                		mMSimPhoneStateListener[i].mMSimState = IccCardConstants.State.NOT_READY;
                		break;                		
                }
                updateSimIcon(i);
            }
        //}
            mDefaultPhoneId = getDefaultPhoneId();
            mDataConnected = mMSimPhoneStateListener[mDefaultPhoneId].mMSimDataConnected;
            mSimState = mMSimPhoneStateListener[mDefaultPhoneId].mMSimState;
            mDataActivity = mMSimPhoneStateListener[mDefaultPhoneId].mMSimDataActivity;
            mDataServiceState = mMSimPhoneStateListener[mDefaultPhoneId].mMSimDataServiceState;
            mServiceState = mMSimPhoneStateListener[mDefaultPhoneId].mMSimServiceState;
            mSignalStrength = mMSimPhoneStateListener[mDefaultPhoneId].mMSimSignalStrength;
            mPhoneStateListener = null/*mMSimPhoneStateListener[mDefaultPhoneId].mMSimPhoneStateListener*/;

            mNetworkName = mMSimPhoneStateListener[mDefaultPhoneId].mMSimNetworkName;
            mPhoneSignalIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimPhoneSignalIconId;
            mLastPhoneSignalIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimLastPhoneSignalIconId;
            // data + data direction on phones
            mDataDirectionIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimDataDirectionIconId;
            mDataSignalIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimDataSignalIconId;
            mDataTypeIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimDataTypeIconId;
            mNoSimIconId = mMSimPhoneStateListener[mDefaultPhoneId].mNoMSimIconId;

            mContentDescriptionPhoneSignal = mMSimPhoneStateListener[mDefaultPhoneId].mMSimContentDescriptionPhoneSignal;
            mContentDescriptionCombinedSignal = mMSimPhoneStateListener[mDefaultPhoneId].mMSimContentDescriptionCombinedSignal;
            mContentDescriptionDataType = mMSimPhoneStateListener[mDefaultPhoneId].mMSimContentDescriptionDataType;

            mLastDataDirectionIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimLastDataDirectionIconId;
            mLastCombinedSignalIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimLastCombinedSignalIconId;
            mLastDataTypeIconId = mMSimPhoneStateListener[mDefaultPhoneId].mMSimLastDataTypeIconId;
            mLastSimIconId = -1;//mMSimPhoneStateListener[mDefaultPhoneId].mMSimLastSimIconId;            
    }

    private int getDefaultPhoneId() {
        int phoneId;
        phoneId = getSlotId(SubscriptionManager.getDefaultSubscriptionId());//getDefaultSubId
        if ( phoneId < 0 || phoneId >= numPhones) {
            phoneId = 0;
        }
        return phoneId;
    }

    private int getSlotId(int subId) {
    	int slotId = 0;
    	for (LocalPhoneStateListener listen:mMSimPhoneStateListener){
			if (listen !=null && listen.mThisSubId == subId) {
    			slotId = listen.mSlotId;
    			break;
    		}
    	}
        LogHelper.sd(TAG, "getSlotId slotId: " + slotId);
        return slotId;
    }    

    private void unregisterPhoneStateListener() {
        for (int i = 0 ; i < mPhoneCount ; i++) {
            if (mMSimPhoneStateListener[i] != null) {
                mPhone.listen(mMSimPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
            }
        }
    }

    public void addSignalCluster(SignalClusterViewYuLong cluster, int phoneId) {
        mSimSignalClusters.add(cluster);
        refreshSignalCluster(cluster, phoneId);
    }

    public void refreshSimCardVisible(SignalClusterViewYuLong cluster){
        if(numPhones > 1 ){
        	int nCountValidateSim = 0;
        	for(int i=0; i < numPhones; i++){
        		if(mMSimPhoneStateListener[i].mMSimState != IccCardConstants.State.ABSENT){
        			nCountValidateSim++;
        		}
        	}
        	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster refreshSimCardVisible mPhoneCount:" + numPhones+ " nCountValidateSim:" + nCountValidateSim);
        if(YulongConfig.getDefault().getNetworkType().endsWith("WG")){
        	   cluster.setDataNetworkSignVisible(nCountValidateSim >= 1);
           }else if(YulongConfig.getDefault().getNetworkType().endsWith("CG") && !isProductMode()){ 
        	   if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster refreshSimCardVisible CG and !isProductMode()");
        	   cluster.setDataNetworkSignVisible(false);
           }else{
        	   cluster.setDataNetworkSignVisible(nCountValidateSim > 1);
           }
        	//电商项目在有䶿个卡启动的时候要自动隐藏无卡图标
        	mAutoHideAbsentCard = YulongConfig.getDefault().isDS();
        	if(mAutoHideAbsentCard){
	        	for(int i=0; i < numPhones; i++){
	        		if(mMSimPhoneStateListener[i].mMSimState == IccCardConstants.State.ABSENT){
	        			cluster.setSignalVisible(i,nCountValidateSim == 0);
	        		}else{
	        			cluster.setSignalVisible(i,true);        			
	        		}
	        	}        	
        	}
        }else{
        	cluster.setDataNetworkSignVisible(false);
        }    	
    }
    
    public void refreshSignalCluster(SignalClusterViewYuLong cluster, int phoneId) {

    	cluster.setWifiSignalIndicator(mWifiIconId, mWifiActivityIconId);
    	if (phoneId == mDataUsingPhoneId){
    		cluster.setDataNetworkVisible(mMSimPhoneStateListener[phoneId].mMSimDataConnected && !mWifiConnected);
    		cluster.setDataNetworkIndicators(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId,mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId);
    		cluster.setDataNetworkSign(mDataUsingPhoneIconId);
    	}
    	cluster.setAirplaneMode(mAirplaneMode);
   	cluster.setNoSimCard(phoneId, 
    			mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.ABSENT
    			||mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.UNKNOWN
    			||mMSimPhoneStateListener[phoneId].mNoMSimIconId != 0
    			);   	
    	  setCarrierNoSimCardIocnYL(phoneId);

        cluster.setSignalIndicators(phoneId, mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId, mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId);
        cluster.setSlaveSignalIndicators(phoneId, mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId, mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId);
        cluster.setRoamingIndicator(phoneId, mMSimPhoneStateListener[phoneId].mCurRoamingIconId);
        refreshSimCardVisible(cluster);
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
        			+ " mWifiIconId:" + getResourceName(mWifiIconId)
        			+ " mWifiActivityIconId:" + getResourceName(mWifiActivityIconId)
        			+ " mMSimDataConnected:" + mMSimPhoneStateListener[phoneId].mMSimDataConnected
         			+ " mMSimDataTypeIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId)
        			+ " mMSimDataDirectionIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId)
        			+ " mDataUsingPhoneIconId:" + getResourceName(mDataUsingPhoneIconId)
        			+ " mAirplaneMode:" + mAirplaneMode
        			+ " mMSimState:" + mMSimPhoneStateListener[phoneId].mMSimState.toString()
        			+ " mMSimPhoneSignalIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId)
                    + " mPhoneSignalTypeIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId)
                    + " mPhoneSignalExIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId)
                    + " mPhoneSignalTypeExIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId)
                    + " mCurRoamingIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mCurRoamingIconId));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED)) {
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
        } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
        } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
        } else {
            int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
            } else {
                mWifiSignalController.handleBroadcast(intent);
            }
        }
        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            updateWifiState(intent);
            refreshViews(mDefaultPhoneId);
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED) ||
        		action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
            updateSimState(intent);
            for (int sub = 0; sub < TelephonyManager.getDefault().getPhoneCount(); sub++) {
                updateDataIcon(sub);
                refreshViews(sub);
            }
        } else if (action.equals(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION)) {
            final int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, 0);
            Slog.d(TAG, "Received SPN update on subId :" + subId);
            Integer phoneId = getSlotId(subId);
            Slog.d(TAG, "Received SPN update on phoneId :" + phoneId);
            mMSimPhoneStateListener[phoneId].mShowSpn = intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false);
            mMSimPhoneStateListener[phoneId].mSpn = intent.getStringExtra(TelephonyIntents.EXTRA_SPN);
            mMSimPhoneStateListener[phoneId].mShowPlmn = intent.getBooleanExtra(
                    TelephonyIntents.EXTRA_SHOW_PLMN, false);
            mMSimPhoneStateListener[phoneId].mPlmn = intent.getStringExtra(TelephonyIntents.EXTRA_PLMN);
            mMSimPhoneStateListener[phoneId].mOriginalSpn = mMSimPhoneStateListener[phoneId].mSpn;
            mMSimPhoneStateListener[phoneId].mOriginalPlmn = mMSimPhoneStateListener[phoneId].mPlmn;
          //===modify by ty
//            mContext.getResources().getBoolean(com.android.internal.R.bool.
//                    config_monitor_locale_change)
            if (true) {
                if (mMSimPhoneStateListener[phoneId].mShowSpn && mMSimPhoneStateListener[phoneId].mSpn != null) {
                	mMSimPhoneStateListener[phoneId].mSpn = getLocaleString(mMSimPhoneStateListener[phoneId].mOriginalSpn);
                }
                if (mMSimPhoneStateListener[phoneId].mShowPlmn && mMSimPhoneStateListener[phoneId].mPlmn != null) {
                	mMSimPhoneStateListener[phoneId].mPlmn = getLocaleString(mMSimPhoneStateListener[phoneId].mOriginalPlmn);
                }
            }

            updateNetworkName(mMSimPhoneStateListener[phoneId].mShowSpn, mMSimPhoneStateListener[phoneId].mSpn, mMSimPhoneStateListener[phoneId].mShowPlmn,
            		mMSimPhoneStateListener[phoneId].mPlmn, phoneId);
            updateCarrierText(phoneId);
            refreshViews(phoneId);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                 action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity(intent);
            refreshViews(mDefaultPhoneId);
        } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
            //parse the string to current language string in public resources
        	//===modify by ty
//        	mContext.getResources().getBoolean(com.android.internal.R.
//                    bool.config_monitor_locale_change)
            if (true) {
                for (int i = 0; i < mPhoneCount; i++) {
                    if (mMSimPhoneStateListener[i].mShowSpn && mMSimPhoneStateListener[i].mSpn != null) {
                    	mMSimPhoneStateListener[i].mSpn = getLocaleString(mMSimPhoneStateListener[i].mOriginalSpn);
                    }
                    if (mMSimPhoneStateListener[i].mShowPlmn && mMSimPhoneStateListener[i].mPlmn != null) {
                    	mMSimPhoneStateListener[i].mPlmn = getLocaleString(mMSimPhoneStateListener[i].mOriginalPlmn);
                    }

                    updateNetworkName(mMSimPhoneStateListener[i].mShowSpn, mMSimPhoneStateListener[i].mSpn, mMSimPhoneStateListener[i].mShowPlmn, 
                    		mMSimPhoneStateListener[i].mPlmn, i);
                    updateCarrierText(i);
                    refreshViews(i);
                }
                
            } 
        }else if(action.endsWith(ACTION_NETWORK_OFFLINE_MODE)) {
    			boolean isOfflineMode = intent.getBooleanExtra(EXTRA_OFFLINE_MODE, false);
    			int isSlotId = intent.getIntExtra(EXTRA_SLOT_ID, -1);
    			LogHelper.sd(TAG, "isOfflineMode = " + isOfflineMode + "; isSlotId = " + isSlotId);
    	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
    	        	cluster.setOffLine(isSlotId, isOfflineMode);
    	        }
          
                refreshViews(mDefaultPhoneId);
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            updateAirplaneMode(false);
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                updateSimIcon(i);
                updateCarrierText(i);
            }
            refreshViews(mDefaultPhoneId);
        } else if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
                action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            updateWimaxState(intent);
            refreshViews(mDefaultPhoneId);
        } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
        	
        	boolean change = false;
            for (int i=0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                int[] subIdtemp = SubscriptionManager.getSubId(i);
                if (subIdtemp != null) {
                	int subId = subIdtemp[0];
                	if(!mSubIdPhoneIdMap.containsKey(subId)){
                		change = true;
                		LogHelper.sd(TAG, " ACTION_SUBINFO_RECORD_UPDATED can not find key " + subId);
                		break;                		
                	}
                	if(mSubIdPhoneIdMap.get(subId) != i){
                		change = true;
                		LogHelper.sd(TAG, " ACTION_SUBINFO_RECORD_UPDATED  key value not match " + subId);
                		break;
                	}
                }
            }        	        
    		if(change){
                unregisterPhoneStateListener();
                registerPhoneStateListener(mContext);
                mDefaultPhoneId = getDefaultPhoneId();
                for (int i=0 ; i < mPhoneCount ; i++) {
                    updateCarrierText(i);
                    updateTelephonySignalStrength(i);
                    updateDataNetType(i);
                    updateDataIcon(i);
                    refreshViews(i);
                }
    		}
    	 //There is no ref-jar here in current time just for yulong.
//       }else if(action.equals(TelephonyIntents.ACTION_CA_STATE_CHANGED)){//===modify by ty
//        	
//            CPCAInfo caInfo = CPCAInfo.newFromBundle(intent.getExtras());
//            int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, SubscriptionManager.INVALID_SIM_SLOT_INDEX);
//            if (caInfo != null) {
//                CA_State = caInfo.getScellInfo_State();
//                LogHelper.sd(TAG, "ACTION_CA_STATE_CHANGED CA_State =" + CA_State );
//
//                    refreshViews(slotId);
//            }        
         }
    }
  private void setCarrierNoSimCardIocnYL(int phoneId){
    	if(YulongConfig.getDefault().getNetworkType().endsWith("WG")){
	
    		if (mSimSignalClusters != null){
    	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
    	        	LogHelper.sd("eee","refreshSignalCluster.........setCarrierNoSimCardIocnYL phoneId =" +phoneId);
    	        	if(phoneId == 0){
    	        	cluster.setNoSimCardIcon(0,R.drawable.stat_sys_signal_null_one_wg);
    	        	}else if(phoneId == 1){
    	        	cluster.setNoSimCardIcon(1,R.drawable.stat_sys_signal_null_two_wg);
    	        	}
    	        }        
            }
    	}else if(YulongConfig.getDefault().getNetworkType().endsWith("GG")){
    		
    	}else if(YulongConfig.getDefault().getNetworkType().endsWith("CG") || YulongConfig.getDefault().getNetworkType().endsWith("SC")){
    		// this is CG or SC
    		if (mSimSignalClusters != null){
    	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
    	        	if(phoneId == 0){
        	        	cluster.setNoSimCardIcon(0,R.drawable.stat_sys_no_sim_cg);
        	        }else if(phoneId == 1){
        	        	cluster.setNoSimCardIcon(1,R.drawable.stat_sys_no_sim_cg);
        	        }
    	        }  
            }
    	}
    	
    }

    public void addSubsLabelView(TextView v) {
        mSubsLabelViews.add(v);
    }
    private void updateCarrierText(int sub) {
        int textResId = 0;
        if (mAirplaneMode) {
            textResId = R.string.lockscreen_airplane_mode_on;
        } else {
            if (DEBUG) {
                LogHelper.sd(TAG, "updateCarrierText for sub:" + sub + " simState =" + mMSimPhoneStateListener[sub].mMSimState);
            }

            switch (mMSimPhoneStateListener[sub].mMSimState) {
                case ABSENT:
                case UNKNOWN:
                case NOT_READY:
                    textResId = R.string.kg_no_uim;
                    break;
                case PIN_REQUIRED:
                    textResId = R.string.lockscreen_sim_locked_message;
                    break;
                case PUK_REQUIRED:
                    textResId = R.string.lockscreen_sim_puk_locked_message;
                    break;
                case READY:
                    // If the state is ready, set the text as network name.
                	mMSimPhoneStateListener[sub].mCarrierTextSub = mMSimPhoneStateListener[sub].mMSimNetworkName;
                    break;
                case PERM_DISABLED:
                    textResId = R.string.lockscreen_permanent_disabled_sim_message_short;
                    break;
                case CARD_IO_ERROR:
                    textResId = R.string.lockscreen_sim_error_message_short;
                    break;
                default:
                    textResId = R.string.kg_no_uim;
                    break;
            }
        }

        if (textResId != 0) {
        	mMSimPhoneStateListener[sub].mCarrierTextSub = mContext.getString(textResId);
        }
    }

    private void setCarrierText() {
    	String carrierName;
    	mNosimText = mContext.getString(R.string.kg_no_uim);
    	mNoServiceText = mContext.getString(R.string.kg_no_service);
        String carrierName1 = mMSimPhoneStateListener[PHONE_ID1].mCarrierTextSub;
        String carrierName2 = "";
        if(mMSimPhoneStateListener.length > PHONE_ID2)
        	carrierName2 = mMSimPhoneStateListener[PHONE_ID2].mCarrierTextSub;   
        if(!enableShowCarrierText(carrierName1) && !enableShowCarrierText(carrierName2)){
        	carrierName = (String)carrierName1;
        }else if(enableShowCarrierText(carrierName1) && !enableShowCarrierText(carrierName2)){
        	carrierName = (String)carrierName1;
        }else if(!enableShowCarrierText(carrierName1) && enableShowCarrierText(carrierName2)){
        	carrierName = (String)carrierName2;
        }else if(enableShowCarrierText(carrierName1) && enableShowCarrierText(carrierName2)){
        	carrierName =(String)carrierName1 +" / "+ (String)carrierName2;
        }else{
        	carrierName =(String)carrierName1 +" / "+ (String)carrierName2;
        }  	        	        	
//        for (int i = 1; i < mPhoneCount; i++) {
//            carrierName = carrierName + "    " + mMSimPhoneStateListener[i].mCarrierTextSub;
//            Log.v("louxiaobo","mMSimPhoneStateListener[" + i + "].mCarrierTextSub = " + mMSimPhoneStateListener[i].mCarrierTextSub);
//        } 
      //There is no ref-jar here in current time just for yulong.
        if (false){//mContext.getResources().getBoolean(R.bool.config_showDataConnectionView)) {//modify by ty
            for (int i = 0; i < mSubsLabelViews.size(); i++) {
                TextView v = mSubsLabelViews.get(i);
                v.setText(carrierName);
                v.setVisibility(View.GONE);//add //no show carrier
            }
        } else {
            for (int i = 0; i < mMobileLabelViews.size(); i++) {
                TextView v = mMobileLabelViews.get(i);
                v.setText(carrierName);
                v.setVisibility(View.GONE);//no show carrier
            }
        }
    }

    private boolean enableShowCarrierText(String carrierText){
   	   if(carrierText == null) return false;
   	   if(carrierText.equals(mNosimText)|| carrierText.equals(mNoServiceText)){
   		   return false;
   	   }else{
   		   return true; 
   	   }	
   }

    // ===== Telephony ==============================================================
    private class LocalPhoneStateListener extends  PhoneStateListener{
    	public final int mThisSubId;
    	public final int mSlotId;
    	//全部封装到这个监听类中，因为这些数据是必定和监听绑定瘿
        // telephony 
        boolean mMSimDataConnected;
        boolean mLastMSimDataConnected;
        IccCardConstants.State mMSimState;
        int mMSimDataActivity;
        int mMSimDataServiceState;
        ServiceState mMSimServiceState;
        SignalStrength mMSimSignalStrength;
//        private LocalPhoneStateListener mMSimPhoneStateListener;
        private String mCarrierTextSub;

        String mMSimNetworkName;
        String mOriginalSpn;
        String mOriginalPlmn;
        int mMSimPhoneSignalIconId;
        int mMSimLastPhoneSignalIconId;
        private int mMSimIconId;
        int mMSimDataDirectionIconId; // data + data direction on phones
        int mMSimDataSignalIconId;
        int mMSimDataTypeIconId;
        int mNoMSimIconId;
        int mMSimMobileActivityIconId; // overlay arrows for data direction

        String mMSimContentDescriptionPhoneSignal;
        String mMSimContentDescriptionCombinedSignal;
        String mMSimContentDescriptionDataType;
       
        int mMSimLastDataDirectionIconId;
        int mMSimLastCombinedSignalIconId;
        int mMSimLastDataTypeIconId;
        int mMSimcombinedSignalIconId;
        int mMSimcombinedActivityIconId;
        int mMSimLastcombinedActivityIconId;
        IccCardConstants.State mMSimLastState = IccCardConstants.State.ABSENT;
        boolean mShowSpn;
        boolean mShowPlmn;
        String mSpn;
        String mPlmn;    	
        //yulong增加的属徿
        int mPhoneSignalTypeIconId;   
        int mPhoneSignalExIconId;
        int mPhoneSignalTypeExIconId;
        int mLastPhoneSignalTypeIconId;
        int mLastPhoneSignalExIconId;
        int mLastPhoneSignalTypeExIconId;    
     	boolean mSignalSingleMode;
        // roaming
        int mCurRoamingIconId = 0;
        int mLastRoamingIconId = -1;
        //
        int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        int mDataState = TelephonyManager.DATA_DISCONNECTED;
        int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
        
    	public LocalPhoneStateListener(int subId,int slotId){
    		super(subId);
    		mThisSubId = subId;
    		mSlotId = slotId;
            mMSimSignalStrength = new SignalStrength();
            mMSimServiceState = new ServiceState();
            mMSimState = IccCardConstants.State.ABSENT;
            // phone_signal
            mMSimPhoneSignalIconId = 0;
            mMSimDataSignalIconId = 0;
            mMSimLastPhoneSignalIconId = -1;
            mMSimLastDataTypeIconId = -1;
            mMSimDataConnected = false;
            mMSimLastDataDirectionIconId = -1;
            mMSimLastCombinedSignalIconId = -1;
            mMSimcombinedSignalIconId = 0;
            mMSimcombinedActivityIconId = 0;
            mMSimLastcombinedActivityIconId = 0;
            mMSimDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
            //mMSimLastSimIconId = -1;
            mMSimNetworkName = mNetworkNameDefault;
            mMSimDataServiceState = ServiceState.STATE_OUT_OF_SERVICE;  	
            
            mPhoneSignalTypeIconId = 0;
            mPhoneSignalExIconId = 0;
            mPhoneSignalTypeExIconId = 0;
            mLastPhoneSignalTypeIconId = 0;
            mLastPhoneSignalExIconId = 0;
            mLastPhoneSignalTypeExIconId = 0;            
    	}
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (DEBUG) {
                Slog.d(TAG, "onSignalStrengthsChanged received on phoneId :"
                    + mSlotId + "signalStrength=" + signalStrength +
                    ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
            }
            int phoneId = mSlotId;            
            mMSimSignalStrength = signalStrength;
            updateIconSet(phoneId);
            updateTelephonySignalStrength(phoneId);
            refreshViews(phoneId);
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            int phoneId = mSlotId;
            if (DEBUG) {
                Slog.d(TAG, "onServiceStateChanged received on phoneId :"
                    + phoneId + " state=" + state.getState());
            }
            mMSimServiceState = state;
            mServiceState = MSimNetworkControllerImpl.this.mMSimPhoneStateListener[mDefaultPhoneId].mMSimServiceState;
            if (SystemProperties.getBoolean("ro.config.combined_signal", true)) {
                /*
                 * if combined_signal is set to true only then consider data
                 * service state for signal display
                 */
                mMSimDataServiceState =
                    mMSimServiceState.getDataRegState();
                if (DEBUG) {
                    LogHelper.sd(TAG, "Combining data service state " +
                            mMSimDataServiceState + " for signal");
                }
            }
            updateIconSet(phoneId);
            updateTelephonySignalStrength(phoneId);
            updateDataNetType(phoneId);
            updateDataIcon(phoneId);
            updateNetworkName(mShowSpn, mSpn,mShowPlmn, mPlmn, phoneId);
            updateCarrierText(phoneId);
            refreshViews(phoneId);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            int phoneId = mSlotId;
            if (DEBUG) {
                Slog.d(TAG, "onCallStateChanged received on phoneId :"
                + phoneId + "state=" + state);
            }
            // In cdma, if a voice call is made, RSSI should switch to 1x.
            if (isCdma(phoneId)) {
                updateTelephonySignalStrength(phoneId);
                refreshViews(phoneId);
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            int phoneId = mSlotId;
            LogHelper.sd(TAG, "onDataConnectionStateChanged phoneId:" + phoneId
            		+ " mDataState :" + state 
                    + " networkType:" + networkType);
            this.mDataState = state;
            this.mDataNetType = networkType;
            updateIconSet(phoneId);
            updateTelephonySignalStrength(phoneId);
            updateDataNetType(phoneId);
            updateDataIcon(phoneId);
            refreshViews(phoneId);
        }

        @Override
        public void onDataActivity(int direction) {
            int phoneId = mSlotId;
            LogHelper.sd(TAG, "onDataActivity phoneId:" + phoneId
            		+ " direction:" + direction );            
            MSimNetworkControllerImpl.this.mDataActivity = direction;
            this.mMSimDataActivity = direction;
            this.mDataActivity = direction;
            updateDataIcon(phoneId);
            refreshViews(phoneId);
        }    	
    }

    // ===== Wifi ===================================================================

    class MSimWifiHandler extends WifiHandler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case WifiManager.DATA_ACTIVITY_NOTIFICATION:
                    if (msg.arg1 != mWifiActivity) {
                    	
                    	mWifiActivity = msg.arg1;
                        int dataSub = SubscriptionManager.getPhoneId(
                                SubscriptionManager.getDefaultDataSubscriptionId());
                        if (!SubscriptionManager.isValidPhoneId(dataSub)) {
                            dataSub = 0;
                        }
                        refreshViews(dataSub);
                        
                    	/*new AsyncTask<Void, Void, Integer>() {
                			@Override
                			protected Integer doInBackground(Void... args) {
                				 mWifiActivity = msg.arg1;
                                 int dataSub = SubscriptionManager.getPhoneId(
                                         SubscriptionManager.getDefaultDataSubscriptionId());
                                 if (!SubscriptionManager.isValidPhoneId(dataSub)) {
                                     dataSub = 0;
                                 }
                				return dataSub;
                			}

                			@Override
                			protected void onPostExecute(Integer result) {
                				refreshViews(result);
                				super.onPostExecute(result);
                			}
                		}.execute();*/
                		
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    protected void updateSimState(Intent intent) {
        IccCardConstants.State simState;
        String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        // Obtain the phoneId info from intent.
        //long subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, 0);
        int phoneId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
        //Integer sub = mSubIdPhoneIdMap.get(subId);
        LogHelper.sd(TAG, "updateSimState for phoneId :" + phoneId + " stateExtra:" + stateExtra);
        if (phoneId >= 0) {
            if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra) || IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(stateExtra)) {
                simState = IccCardConstants.State.ABSENT;
            }else if(IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(stateExtra)){
            	  simState = IccCardConstants.State.NOT_READY;
            	  LogHelper.sd(TAG, "updateSimState for phoneId :" + phoneId + " getSimState(phoneId):" + TelephonyManager.getDefault().getSimState(phoneId));
            	  if(TelephonyManager.getDefault().getSimState(phoneId) == 1){           		  
            		  simState = IccCardConstants.State.ABSENT;
            	  }  
            }else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)
                    || IccCardConstants.INTENT_VALUE_ICC_IMSI.equals(stateExtra)
                    || IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(stateExtra)) {
                simState = IccCardConstants.State.READY;
            }else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
                final String lockedReason = intent.getStringExtra(IccCardConstants.
                                                                INTENT_KEY_LOCKED_REASON);
                if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                    simState = IccCardConstants.State.PIN_REQUIRED;
                }
                else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                    simState = IccCardConstants.State.PUK_REQUIRED;
                }
                else {
                    simState = IccCardConstants.State.NETWORK_LOCKED;
                }
            } else {
                simState = IccCardConstants.State.UNKNOWN;
            }
            // Update the sim state and carrier text.
            if (simState != IccCardConstants.State.UNKNOWN && simState != mMSimPhoneStateListener[phoneId].mMSimState) {
            	mMSimPhoneStateListener[phoneId].mMSimState = simState;
                updateCarrierText(phoneId);
                LogHelper.sd(TAG, "updateSimState mMSimState = " + mMSimPhoneStateListener[phoneId].mMSimState);
            }
            updateIconSet(phoneId);
            updateDataIcon(phoneId);
            updateTelephonySignalStrength(phoneId);
            updateSimIcon(phoneId);
        }
    }

    private boolean isCdma(int phoneId) {
        return (mMSimPhoneStateListener[phoneId].mMSimSignalStrength != null) &&
                !mMSimPhoneStateListener[phoneId].mMSimSignalStrength.isGsm();
    }

    private boolean hasService(int phoneId) {
        ServiceState ss = mMSimPhoneStateListener[phoneId].mMSimServiceState;
        if (ss != null) {
            switch (ss.getState()) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    return false;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private final void updateTelephonySignalStrength(int phoneId) {
    	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "updateTelephonySignalStrength: phoneId =" + phoneId);
    	updateSignalStrength(phoneId);
//    	
//        int dataSub = SubscriptionManager.getPhoneId(
//                SubscriptionManager.getDefaultDataSubId());
//        if ((!hasService(phoneId) &&
//                (mMSimPhoneStateListener[phoneId].mMSimDataServiceState != ServiceState.STATE_IN_SERVICE))
//                || mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.ABSENT) {
//            if (DEBUG) LogHelper.sd(TAG, " No service");
//            mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId =
//                    TelephonyIcons.getSignalNullIcon(phoneId);
//            mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId =
//            		mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId;
//            if (phoneId == dataSub) {
//                mQSPhoneSignalIconId = R.drawable.ic_qs_signal_no_signal;
//            }
//        } else {
//            if (mMSimPhoneStateListener[phoneId].mMSimSignalStrength == null || (mMSimPhoneStateListener[phoneId].mMSimServiceState == null)) {
//                if (DEBUG) {
//                    LogHelper.sd(TAG, " Null object, mMSimSignalStrength= "
//                            + mMSimPhoneStateListener[phoneId].mMSimSignalStrength
//                            + " mMSimServiceState " + mMSimPhoneStateListener[phoneId].mMSimServiceState);
//                }
//                mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId =
//                        TelephonyIcons.getSignalNullIcon(phoneId);
//                mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId = mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId;
//                mMSimPhoneStateListener[phoneId].mMSimContentDescriptionPhoneSignal =
//                        TelephonyIcons.getSignalStrengthDes(phoneId, 0);
//                if (phoneId == dataSub) {
//                    mQSPhoneSignalIconId = R.drawable.ic_qs_signal_no_signal;
//                }
//            } else {
//                int iconLevel;
//                if (isCdma(phoneId) && mAlwaysShowCdmaRssi) {
//                    mLastSignalLevel = iconLevel = mMSimPhoneStateListener[phoneId].mMSimSignalStrength.getCdmaLevel();
//                    if(DEBUG) Slog.d(TAG, "mAlwaysShowCdmaRssi= " + mAlwaysShowCdmaRssi
//                            + " set to cdmaLevel= "
//                            + mMSimPhoneStateListener[phoneId].mMSimSignalStrength.getCdmaLevel()
//                            + " instead of level= " + mMSimPhoneStateListener[phoneId].mMSimSignalStrength.getLevel());
//                } else {
//                    mLastSignalLevel = iconLevel = mMSimPhoneStateListener[phoneId].mMSimSignalStrength.getLevel();
//                    if (mShowRsrpSignalLevelforLTE) {
//                        if (mMSimPhoneStateListener[phoneId].mMSimServiceState.getDataNetworkType() ==
//                                TelephonyManager.NETWORK_TYPE_LTE) {
//                            int level = mMSimPhoneStateListener[phoneId].mMSimSignalStrength.getAlternateLteLevel();
//                            mLastSignalLevel = iconLevel = (level == -1 ? 0 : level);
//                            Slog.d(TAG, "updateTelephonySignalStrength, data type is lte, level = "
//                                + level + " | " + mMSimPhoneStateListener[phoneId].mMSimSignalStrength);
//                        }
//                    }
//                }
//
//                mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId =
//                        TelephonyIcons.getSignalStrengthIcon(phoneId, mInetCondition?1:0,
//                        iconLevel, isRoaming(phoneId));
//
//                mMSimPhoneStateListener[phoneId].mMSimContentDescriptionPhoneSignal =
//                        TelephonyIcons.getSignalStrengthDes(phoneId, iconLevel);
//
//                mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId = mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId;
//
//                if (phoneId == dataSub) {
//                    mQSPhoneSignalIconId = TelephonyIcons
//                            .QS_TELEPHONY_SIGNAL_STRENGTH[mInetCondition?1:0][iconLevel];
//                }
//
//                if (DEBUG) {
//                    Slog.d(TAG, "updateTelephonySignalStrength, sub: " + phoneId
//                        + " level=" + iconLevel
//                        + " mInetCondition?1:0=" + mInetCondition?1:0
//                        + " mMSimPhoneSignalIconId[" + phoneId + "]="
//                        + mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId
//                        + "/" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId));
//                }
//            }
//        }
    }

    private boolean isRoaming(int phoneId) {
    	if(LogHelper.NOLOGGING)LogHelper.sd(TAG,    "isCdma(phoneId) = "
                              + isCdma(phoneId)
                              + ", isCdmaEri(phoneId) = "
                              + isCdmaEri(phoneId)
                              + "mMSimPhoneStateListener[phoneId].mMSimServiceState = "+(mMSimPhoneStateListener[phoneId].mMSimServiceState != null)
                              + ", mMSimPhoneStateListener[phoneId].mMSimServiceState.getRoaming() = "
                              + (mMSimPhoneStateListener[phoneId].mMSimServiceState != null && mMSimPhoneStateListener[phoneId].mMSimServiceState
                                            .getRoaming()));
        return (isCdma(phoneId) ? isCdmaEri(phoneId)
                : mMSimPhoneStateListener[phoneId].mMSimServiceState != null && mMSimPhoneStateListener[phoneId].mMSimServiceState.getRoaming());
    }

    private final void updateDataNetType(final int phoneId) {
        // DSDS case: Data is active only on DDS. Clear the icon for NON-DDS
//        new AsyncTask<Void, Void, Integer>() {
//			@Override
//			protected Integer doInBackground(Void... args) {
//				int dataSub = SubscriptionManager.getPhoneId(
//		                SubscriptionManager.getDefaultDataSubscriptionId());
//				return dataSub;
//			}
//
//			@Override
//			protected void onPostExecute(Integer dataSub) {
				int dataSub = SubscriptionManager.getPhoneId(
		                SubscriptionManager.getDefaultDataSubscriptionId());
				if (phoneId != dataSub) {
		            LogHelper.sd(TAG,"updateDataNetType: phoneId" + phoneId
		                    + " is not DDS(=SUB" + dataSub + ")!");
		            mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = 0;
		        } else {
		            mNetworkName = mMSimPhoneStateListener[phoneId].mMSimNetworkName;
		            if (mIsWimaxEnabled && mWimaxConnected) {
		                // wimax is a special 4g network not handled by telephony
		            	mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = R.drawable.stat_sys_data_fully_connected_4g;
		                mQSDataTypeIconId = TelephonyIcons.QS_DATA_4G[mInetCondition?1:0];
		                mMSimPhoneStateListener[phoneId].mMSimContentDescriptionDataType = mContext.getString(
		                        R.string.accessibility_data_connection_4g);
		            } else {
		                LogHelper.sd(TAG,"updateDataNetType sub = " + phoneId
		                        + " mDataNetType = " + mDataNetType);
		                mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId =
		                        TelephonyIcons.getDataTypeIcon(phoneId);
		                mMSimPhoneStateListener[phoneId].mMSimContentDescriptionDataType =
		                        TelephonyIcons.getDataTypeDesc(phoneId)+"";
		                mQSDataTypeIconId =
		                        TelephonyIcons.getQSDataTypeIcon(phoneId);
		            }
		        }

		        boolean setQSDataTypeIcon = false;
		       if(YulongConfig.getDefault().getNetworkType().endsWith("WG") ){
		        	if(mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.ABSENT){
		        		mMSimPhoneStateListener[phoneId].mCurRoamingIconId = 0;
		        	}else{
		        	    mMSimPhoneStateListener[phoneId].mCurRoamingIconId = phoneId==0 ? R.drawable.stat_sys_roaming_sign_wg1:R.drawable.stat_sys_roaming_sign_wg2;
		        	}
		    	}else{
		             mMSimPhoneStateListener[phoneId].mCurRoamingIconId = 0;
		    	}
//		        if (isCdma(phoneId)) {
//		            if (isCdmaEri(phoneId)) {
//		            	mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
//		                setQSDataTypeIcon = true;
//		                mMSimPhoneStateListener[phoneId].mCurRoamingIconId = getRoamingIcon(phoneId);//R.drawable.stat_sys_roaming;
//		                if (phoneId == dataSub) {
//		                    mQSDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
//		                }
//		            }
//		        } 
		        if (isRoaming(phoneId)) {
		        	mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
		            setQSDataTypeIcon = true;
		            mMSimPhoneStateListener[phoneId].mCurRoamingIconId = R.drawable.stat_sys_roaming;
		            if (phoneId == dataSub) {
		                mQSDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
		            }
		        }

		        if (setQSDataTypeIcon && phoneId == dataSub) {
		            mQSDataTypeIconId = TelephonyIcons.QS_DATA_R[mInetCondition?1:0];
		         }
//				super.onPostExecute(dataSub);
//			}
//		}.execute();
		
        
    }
     

    boolean isCdmaEri(int phoneId) {
        if ((mMSimPhoneStateListener[phoneId].mMSimServiceState != null)
                && (hasService(phoneId) || (mMSimPhoneStateListener[phoneId].mMSimDataServiceState
                == ServiceState.STATE_IN_SERVICE))) {
            final int iconIndex = mMSimPhoneStateListener[phoneId].mMSimServiceState.getCdmaEriIconIndex();
            if (iconIndex != EriInfo.ROAMING_INDICATOR_OFF) {
                final int iconMode = mMSimPhoneStateListener[phoneId].mMSimServiceState.getCdmaEriIconMode();
                if (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH) {
                    return true;
                }
            }
        }
        return false;
    }

    private final void updateSimIcon(int phoneId) {
     if (mMSimPhoneStateListener[phoneId].mMSimState ==  IccCardConstants.State.ABSENT) {
        	mMSimPhoneStateListener[phoneId].mNoMSimIconId = TelephonyIcons.getNoSimIcon(phoneId);
        } else if(mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.PIN_REQUIRED 
        		|| mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.PUK_REQUIRED){
        	if((YulongConfig.getDefault().getNetworkType().endsWith("WG") || YulongConfig.getDefault().getNetworkType().endsWith("SW"))
        			&& !isProductMode()){
        		LogHelper.sd("eeee","........WG or SW......PIN_REQUIRED or PUK_REQUIRED is done");
        		mMSimPhoneStateListener[phoneId].mNoMSimIconId = TelephonyIcons.getNoSimIcon(phoneId);
        	}    
        }else{
        	mMSimPhoneStateListener[phoneId].mNoMSimIconId = 0;
        }
        if (mSimSignalClusters != null){
            LogHelper.sd(TAG,"In updateSimIcon card =" + phoneId + ", simState= " + mMSimPhoneStateListener[phoneId].mMSimState
            		+ " mNoMSimIconId[phoneId]" + mMSimPhoneStateListener[phoneId].mNoMSimIconId);        	
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	        	cluster.setNoSimCard(phoneId, mMSimPhoneStateListener[phoneId].mNoMSimIconId != 0);  
	        	setCarrierNoSimCardIocnYL(phoneId);
	        }        
        }
    }

    private void updateIconSet(int phoneId) {
    	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "updateIconSet, phoneId = " + phoneId);
        ServiceState state = mMSimPhoneStateListener[phoneId].mMSimServiceState;
        int dataNetworkType = state.getDataNetworkType();
        int voiceNetworkType = state.getVoiceNetworkType();
        mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId = getPsNetworkTypeIcon(dataNetworkType,phoneId);
        mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId = getCsNetworkTypeIcon(voiceNetworkType);
        mMSimPhoneStateListener[phoneId].mSignalSingleMode = false;
        if ( TelephonyManager.getNetworkClass(dataNetworkType) == TelephonyManager.getNetworkClass(voiceNetworkType)
        		|| state.getDataNetworkType() == 0 || state.getVoiceNetworkType()==0
        		|| !isChinaTelecomSIM()){
        	mMSimPhoneStateListener[phoneId].mSignalSingleMode = true;//只显示单挿
        	
        	if(mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId == 0){
        		mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId = mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId;
        	}
        }  
        
        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
         	cluster.setSignalSingleMode(phoneId, mMSimPhoneStateListener[phoneId].mSignalSingleMode);
         }
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "updateIconSet, voice network type is: " + voiceNetworkType
            + "/" + TelephonyManager.getNetworkTypeName(voiceNetworkType)
            + ", data network type is: " + dataNetworkType
            + "/" + TelephonyManager.getNetworkTypeName(dataNetworkType));
//
//        int chosenNetworkType = ((dataNetorkType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
//                    ? voiceNetorkType : dataNetorkType);
//
//        LogHelper.sd(TAG, "updateIconSet, chosenNetworkType=" + chosenNetworkType
//            + " hspaDataDistinguishable=" + String.valueOf(mHspaDataDistinguishable)
//            + " hspapDistinguishable=" + "false"
//            + " showAtLeastThreeGees=" + String.valueOf(mShowAtLeastThreeGees));
//
//        TelephonyIcons.updateDataType(phoneId, chosenNetworkType, mShowAtLeastThreeGees,
//            mShow4GforLTE, mHspaDataDistinguishable, mInetCondition?1:0);
    }
    
  private boolean isChinaTelecomSIM(){
    	String mSimOperator = TelephonyManager.getDefault().getSimOperator();
    	boolean isChinaTelecom = false;
         if(mSimOperator != null ){
        	 if(mSimOperator.endsWith("46003")||
        	    mSimOperator.endsWith("46005")||
        	    mSimOperator.endsWith("46011")||
        	    mSimOperator.endsWith("46012")||
        	    mSimOperator.endsWith("46013")){
        		 isChinaTelecom = true;
        	 }
        }
    	return isChinaTelecom;
    	
    }
    

    private final void updateDataIcon(int phoneId) {
    	LogHelper.sd(TAG,"updateDataIcon phoneId =" + phoneId);
        int iconId = 0;
        boolean visible = true;
        
//        int dataSub = SubscriptionManager.getPhoneId(
//                SubscriptionManager.getDefaultDataSubId());
//
//        LogHelper.sd(TAG,"updateDataIcon dataSub =" + dataSub);
//        // DSDS case: Data is active only on DDS. Clear the icon for NON-DDS
//        if (phoneId != dataSub) {
//        	mMSimPhoneStateListener[phoneId].mMSimDataConnected = false;
//            LogHelper.sd(TAG,"updateDataIconi: phoneId" + phoneId
//                     + " is not DDS.  Clear the mMSimDataConnected Flag and return");
//            return;
//        }

        LogHelper.sd(TAG,"updateDataIcon  when mMSimState =" + mMSimPhoneStateListener[phoneId].mMSimState);
        if (mMSimPhoneStateListener[phoneId].mDataNetType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            visible = false;
        } else {
        	LogHelper.sd(TAG,"updateDataIcon  when gsm mMSimState =" + mMSimPhoneStateListener[phoneId].mMSimState);
            if (mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.READY ||
            		mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.UNKNOWN) {
                mNoSim = false;
                if (mMSimPhoneStateListener[phoneId].mDataState == TelephonyManager.DATA_CONNECTED) {
                    iconId = TelephonyIcons.getDataActivity(phoneId, mDataActivity);
                    mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId = iconId;
                    switch (mMSimPhoneStateListener[phoneId].mDataActivity) {
                    case TelephonyManager.DATA_ACTIVITY_IN: // 1
                    	iconId = mBigDataDirectionIcon ? R.drawable.stat_sys_data_in : R.drawable.stat_sys_data_connected_in;
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT: // 2
                    	iconId = mBigDataDirectionIcon ? R.drawable.stat_sys_data_out : R.drawable.stat_sys_data_connected_out;
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT: // 3
                    	iconId = mBigDataDirectionIcon ? R.drawable.stat_sys_data_inandout : R.drawable.stat_sys_data_connected_inout;
                        break;
                    default:
                    	iconId = mBigDataDirectionIcon ? R.drawable.stat_sys_data_connected : R.drawable.stat_sys_data_connected_no;                    	   
                    }
                    mDataUsingPhoneId = phoneId;
	                if (phoneId == PHONE_ID1) {
	                    mDataUsingPhoneIconId = R.drawable.stat_sys_signal_type_card1;
	                }
	                else if(phoneId == PHONE_ID2) {//
	                    mDataUsingPhoneIconId = R.drawable.stat_sys_signal_type_card2;
	                }                    
                } else {
                    iconId = 0;
                    visible = false;
                }
            } else {
            	LogHelper.sd(TAG,"updateDataIcon when no sim");
                mNoSim = true;
                iconId = TelephonyIcons.getNoSimIcon();
                visible = false; // no SIM? no data
            }
        }

        mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId = iconId;
        
        if(!mBigDataDirectionIcon){
        	 if(YulongConfig.getDefault().getNetworkType().endsWith("WG") || YulongConfig.getDefault().getNetworkType().endsWith("SW")){
        	    mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = 0;
        	}else{
        		  mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = getPsNetworkTypeIcon(mMSimPhoneStateListener[phoneId].mDataNetType,phoneId);	
        	}
        }
        mMSimPhoneStateListener[phoneId].mMSimDataConnected = visible;
        mDataConnected = visible;

        if(LogHelper.NOLOGGING)LogHelper.sd(TAG,"updateDataIcon when mMSimDataConnected[" + phoneId + "] ="
            + mMSimPhoneStateListener[phoneId].mMSimDataConnected
            + " mMSimDataDirectionIconId[" + phoneId +"] = "
            + mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId
            + " mDataUsingPhoneId = " + mDataUsingPhoneId);
    }

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn,
            int phoneId) {
        if (DEBUG) {
            LogHelper.sd(TAG, "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
        	//===modify by ty
            if(
            		//mContext.getResources().getBoolean(com.android.internal.R.bool.config_display_rat) &&
            		mMSimPhoneStateListener[phoneId].mMSimServiceState != null) {
                plmn = appendRatToNetworkName(plmn, mMSimPhoneStateListener[phoneId].mMSimServiceState);
            }
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if(something){
               Slog.d(TAG,"Do not display spn string when showPlmn and showSpn are both true"
                       + "and plmn string is not null");
            } else {
              //===modify by ty
                if(//mContext.getResources().getBoolean(com.android.internal.R.bool.config_display_rat) && 
                		mMSimPhoneStateListener[phoneId].mMSimServiceState != null) {
                    spn = appendRatToNetworkName(spn, mMSimPhoneStateListener[phoneId].mMSimServiceState);
                }
                str.append(spn);
                something = true;
            }
        }
        if (something) {
        	mMSimPhoneStateListener[phoneId].mMSimNetworkName = str.toString();
        } else {
        	mMSimPhoneStateListener[phoneId].mMSimNetworkName = mNetworkNameDefault;
        }
        LogHelper.sd(TAG, "mMSimNetworkName[phoneId] " + mMSimPhoneStateListener[phoneId].mMSimNetworkName
                                                      + "phoneId " + phoneId);
    }

    // ===== Full or limited Internet connectivity ==================================
    protected void updateConnectivity(Intent intent) {
        if (CHATTY) {
            Slog.d(TAG, "updateConnectivity: intent=" + intent);
        }

        final ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = connManager.getActiveNetworkInfo();

        // Are we connected at all, by any interface?
        mConnected = info != null && info.isConnected();
        if (mConnected) {
            mConnectedNetworkType = info.getType();
            mConnectedNetworkTypeName = info.getTypeName();
        } else {
            mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
            mConnectedNetworkTypeName = null;
        }

        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);

        if (CHATTY) {
            LogHelper.sd(TAG, "updateConnectivity: networkInfo=" + info);
            LogHelper.sd(TAG, "updateConnectivity: connectionStatus=" + connectionStatus);
        }

        //mInetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);//===modify by ty
        if (info != null && info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
            mBluetoothTethered = info.isConnected();
        } else {
            mBluetoothTethered = false;
        }

        // We want to update all the icons, all at once, for any condition change
        updateWimaxIcons();
        for (int sub = 0; sub < TelephonyManager.getDefault().getPhoneCount(); sub++) {
            updateDataNetType(sub);
            updateDataIcon(sub);
            updateTelephonySignalStrength(sub);
        }
        updateWifiIcons();
    }
    @Override
    protected void updateWifiIcons() {
    	super.updateWifiIcons();
    	if(mWifiConnected && mSimSignalClusters != null && !mSimSignalClusters.isEmpty()){
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	        	cluster.setDataNetworkVisible(false);
	        }    		
    	}
    }
    // ===== Update the views =======================================================

    protected void refreshViews(int phoneId) {
        Context context = mContext;

        String combinedLabel = "";
        String mobileLabel = "";
        String wifiLabel = "";
        int N;
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG,"refreshViews phoneId =" + phoneId + " mMSimDataConnected ="
                + mMSimPhoneStateListener[phoneId].mMSimDataConnected);
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG,"refreshViews mMSimDataActivity =" + mMSimPhoneStateListener[phoneId].mMSimDataActivity);
        int dataSub = phoneId;/*SubscriptionManager.getPhoneId(
                SubscriptionManager.getDefaultDataSubscriptionId());*/
//        try {
//        CA_State = TelephonyManager.from(mContext).getCAState(phoneId);
//		} catch (Exception e) {
//			e.printStackTrace();
//			 LogHelper.sd(TAG, "refreshViews, printStackTrace CA_State = " + CA_State);
//		}
        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshViews, CA_State = " + CA_State);
        if (!mHasMobileDataFeature) {
        	mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId = mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId = 0;
            mobileLabel = "";
            mQSPhoneSignalIconId = 0;
        } else {
            // We want to show the carrier name if in service and either:
            //   - We are connected to mobile data, or
            //   - We are not connected to mobile data, as long as the *reason* packets are not
            //     being routed over that link is that we have better connectivity via wifi.
            // If data is disconnected for some other reason but wifi (or ethernet/bluetooth)
            // is connected, we show nothing.
            // Otherwise (nothing connected) we show "No internet connection".

            if (mMSimPhoneStateListener[phoneId].mMSimDataConnected) {
                mobileLabel = mMSimPhoneStateListener[phoneId].mMSimNetworkName;
            } else if (mConnected) {
                if (hasService(phoneId)) {
                    mobileLabel = mMSimPhoneStateListener[phoneId].mMSimNetworkName;
                } else {
                    mobileLabel = "";
                }
            } else {
                mobileLabel
                    = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }

            // Now for things that should only be shown when actually using mobile data.
            if (mMSimPhoneStateListener[phoneId].mMSimDataConnected) {
            	mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId = mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId;

                combinedLabel = mobileLabel;
                mMSimPhoneStateListener[phoneId].mMSimcombinedActivityIconId = mMSimPhoneStateListener[phoneId].mMSimMobileActivityIconId;
                // set by updateDataIcon()
                mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId = mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId;
                mMSimPhoneStateListener[phoneId].mMSimContentDescriptionCombinedSignal =
                		mMSimPhoneStateListener[phoneId].mMSimContentDescriptionDataType;
            } else {
            	mMSimPhoneStateListener[phoneId].mMSimMobileActivityIconId = 0;
            	mMSimPhoneStateListener[phoneId].mMSimcombinedActivityIconId = 0;
            }
        }

        mWifiActivityIconId = 0;
        if (mWifiConnected) {
            if (mWifiSsid == null) {
                wifiLabel = context.getString(
                        R.string.status_bar_settings_signal_meter_wifi_nossid);
            } else {
                wifiLabel = mWifiSsid;
                if (DEBUG) {
                    wifiLabel += "xxxxXXXXxxxxXXXX";
                }

                switch (mWifiActivity) {
                    case WifiManager.DATA_ACTIVITY_IN:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_datain;
                        break;
                    case WifiManager.DATA_ACTIVITY_OUT:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_dataout;
                        break;
                    case WifiManager.DATA_ACTIVITY_INOUT:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_datainout;
                        break;
                    case WifiManager.DATA_ACTIVITY_NONE:
                        mWifiActivityIconId = 0;
                        break;
                }
            }

            mMSimPhoneStateListener[phoneId].mMSimcombinedActivityIconId = mWifiActivityIconId;
            combinedLabel = wifiLabel;
            mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId = mWifiIconId; // set by updateWifiIcons()
            mMSimPhoneStateListener[phoneId].mMSimContentDescriptionCombinedSignal = mContentDescriptionWifi;
        } else {
            if (mHasMobileDataFeature) {
                wifiLabel = "";
            } else {
                wifiLabel = context.getString(
                        R.string.status_bar_settings_signal_meter_disconnected);
            }
        }
    	
        if (mBluetoothTethered) {
            combinedLabel = mContext.getString(R.string.bluetooth_tethered);
            mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId = mBluetoothTetherIconId;
            mMSimPhoneStateListener[phoneId].mMSimContentDescriptionCombinedSignal = mContext.getString(
                    R.string.accessibility_bluetooth_tether);
        }

        final boolean ethernetConnected = (mConnectedNetworkType ==
                ConnectivityManager.TYPE_ETHERNET);
        if (ethernetConnected) {
            // TODO: icons and strings for Ethernet connectivity
            combinedLabel = mConnectedNetworkTypeName;
        }

        if (mAirplaneMode &&
                (mMSimPhoneStateListener[phoneId].mMSimServiceState == null || (!hasService(phoneId)
                    && !mMSimPhoneStateListener[phoneId].mMSimServiceState.isEmergencyOnly()))) {
            // Only display the flight-mode icon if not in "emergency calls only" mode.

            // look again; your radios are now airplanes
        	mMSimPhoneStateListener[phoneId].mMSimContentDescriptionPhoneSignal = mContext.getString(
                    R.string.accessibility_airplane_mode);
            mAirplaneIconId = R.drawable.stat_sys_airplane_mode;
            mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId = mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId
                    = mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = 0;
            if (phoneId == dataSub) {
                mQSDataTypeIconId = 0;
                mNetworkName = mNetworkNameDefault;
            }

            // combined values from connected wifi take precedence over airplane mode
            if (mWifiConnected) {
                // Suppress "No internet connection." from mobile if wifi connected.
                mobileLabel = "";
            } else {
                if (mHasMobileDataFeature) {
                    // let the mobile icon show "No internet connection."
                    wifiLabel = "";
                } else {
                    wifiLabel = context.getString(
                            R.string.status_bar_settings_signal_meter_disconnected);
                    combinedLabel = wifiLabel;
                }
                mMSimPhoneStateListener[phoneId].mMSimContentDescriptionCombinedSignal =
                        mContentDescriptionPhoneSignal;
                mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId = mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId;
            }
            mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = 0;
            if (phoneId == dataSub) {
                mQSDataTypeIconId = 0;
            }

            mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId = mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId;
        }
        else if (!mMSimPhoneStateListener[phoneId].mMSimDataConnected && !mWifiConnected && !mBluetoothTethered &&
                !mWimaxConnected && !ethernetConnected) {
            // pretty much totally disconnected

            combinedLabel = context.getString(
                    R.string.status_bar_settings_signal_meter_disconnected);
            // On devices without mobile radios, we want to show the wifi icon
            mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId =
                    mHasMobileDataFeature ? mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId : mWifiIconId;
                    mMSimPhoneStateListener[phoneId].mMSimContentDescriptionCombinedSignal = mHasMobileDataFeature
                    ? mMSimPhoneStateListener[phoneId].mMSimContentDescriptionDataType : mContentDescriptionWifi;
        }

        if (!mMSimPhoneStateListener[phoneId].mMSimDataConnected) {
        	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshViews: Data not connected!! Set no data type icon / Roaming for"
                    + " phoneId: " + phoneId);
            mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = 0;
            if (phoneId == dataSub) {
                mQSDataTypeIconId = 0;
            }
            if (isCdma(phoneId)) {
                if (isCdmaEri(phoneId)) {
                	mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId =
                            R.drawable.stat_sys_data_fully_connected_roam;
                    if (phoneId == dataSub) {
                        mQSDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
                    }
                }
            } else if (isRoaming(phoneId)) {
            	mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
                if (phoneId == dataSub) {
                    mQSDataTypeIconId = R.drawable.stat_sys_data_fully_connected_roam;
                }
            }
        }
        if (DEBUG) {
            LogHelper.sd(TAG, "refreshViews connected={"
                    + (mWifiConnected?" wifi":"")
                    + (mMSimPhoneStateListener[phoneId].mMSimDataConnected?" data":"")
                    + " } level="
                    + ((mMSimPhoneStateListener[phoneId].mMSimSignalStrength == null)?"??":Integer.toString
                            (mMSimPhoneStateListener[phoneId].mMSimSignalStrength.getLevel()))
                    + " mMSimcombinedSignalIconId=0x"
                    + Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId)
                    + "/" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId)
                    + " mMSimcombinedActivityIconId=0x" + Integer.toHexString
                            (mMSimPhoneStateListener[phoneId].mMSimcombinedActivityIconId)
                    + " mAirplaneMode=" + mAirplaneMode
                    + " mMSimDataActivity=" + mMSimPhoneStateListener[phoneId].mMSimDataActivity
                    + " mMSimPhoneSignalIconId=0x" + Integer.toHexString
                            (mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId)
                    + "/" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId)
                    + " mMSimDataDirectionIconId=0x" + Integer.toHexString
                            (mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId)
                    + " mMSimDataSignalIconId=0x" + Integer.toHexString
                            (mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId)
                    + " mMSimDataTypeIconId=0x" + Integer.toHexString
                            (mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId)
                    + "/" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId)
                    + " mNoMSimIconId=0x" + Integer.toHexString(mMSimPhoneStateListener[phoneId].mNoMSimIconId)
                    + "/" + getResourceName(mMSimPhoneStateListener[phoneId].mNoMSimIconId)
                    + " mMSimMobileActivityIconId=0x"
                    + Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimMobileActivityIconId)
                    + "/" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimMobileActivityIconId)
                    + " mWifiIconId=0x" + Integer.toHexString(mWifiIconId)
                    + " mBluetoothTetherIconId=0x" + Integer.toHexString(mBluetoothTetherIconId));
        }
        
        // update QS
        for (NetworkSignalChangedCallback cb : mSignalsChangedCallbacks) {
            notifySignalsChangedCallbacks(cb);
        }
        if (mLastWifiActivityIconId != mWifiActivityIconId || mLastWifiIconId != mWifiIconId){
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	        	cluster.setWifiSignalIndicator(mWifiIconId, mWifiActivityIconId);
	        }
	        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
        			+ " mWifiIconId:" + getResourceName(mWifiIconId)
        			+ " mWifiActivityIconId:" + getResourceName(mWifiActivityIconId));	        
	        mLastWifiActivityIconId = mWifiActivityIconId;
	        mLastWifiIconId = mWifiIconId;	        
        }
        
        if (mMSimPhoneStateListener[phoneId].mMSimLastDataDirectionIconId != mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId 
        		|| mMSimPhoneStateListener[phoneId].mMSimLastDataTypeIconId != mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId
        		|| mMSimPhoneStateListener[phoneId].mLastMSimDataConnected != mMSimPhoneStateListener[phoneId].mMSimDataConnected
        		|| mLastDataUsingPhoneIconId != mDataUsingPhoneIconId
        		|| CA_State >= 0){
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	        	if (phoneId == mDataUsingPhoneId){
	        	if(CA_State > 0){
//	        		   cluster.setDataNetworkIndicators(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId,mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId);
	        		cluster.setDataNetworkIndicators(R.drawable.stat_sys_signal_type_4g_enhance,mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId);
	        		}else{
//	        			cluster.setDataNetworkIndicators(R.drawable.stat_sys_signal_type_4g_enhance,mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId);
	        			cluster.setDataNetworkIndicators(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId,mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId);
	        		}
	        		cluster.setDataNetworkVisible(mMSimPhoneStateListener[phoneId].mMSimDataConnected  && !mWifiConnected);
	        		cluster.setDataNetworkSign(mDataUsingPhoneIconId);
	        	}
	        }
	        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
        			+ " mMSimDataConnected:" + mMSimPhoneStateListener[phoneId].mMSimDataConnected
         			+ " mMSimDataTypeIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId)
        			+ " mMSimDataDirectionIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId)
        			+ " mDataUsingPhoneIconId:" + getResourceName(mDataUsingPhoneIconId));
	        mLastDataUsingPhoneIconId = mDataUsingPhoneIconId;
	        mMSimPhoneStateListener[phoneId].mMSimLastDataDirectionIconId = mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId;
	        mMSimPhoneStateListener[phoneId].mMSimLastDataTypeIconId = mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId;
	        mMSimPhoneStateListener[phoneId].mLastMSimDataConnected = mMSimPhoneStateListener[phoneId].mMSimDataConnected;
	        int networkType = 0;
	        if( mMSimPhoneStateListener[phoneId].mMSimServiceState != null){
	        	networkType = mMSimPhoneStateListener[phoneId].mMSimServiceState.getDataNetworkType();
	        	if (networkType == 0){
	        		networkType = mMSimPhoneStateListener[phoneId].mMSimServiceState.getVoiceNetworkType();
	        	}
	        }	        
	        for (NetworkSignalChangedCallback cb : mSignalsChangedCallbacks) {
	            cb.onNetworkStateChange(phoneId,networkType);
	        }	        
        }
        
        if (mLastAirplaneMode != mAirplaneMode){
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	        	cluster.setAirplaneMode(mAirplaneMode);
	        }
	        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
        			+ " mAirplaneMode:" + mAirplaneMode);	        
	        mLastAirplaneMode = mAirplaneMode;
        }      
        if (mMSimPhoneStateListener[phoneId].mMSimLastState != mMSimPhoneStateListener[phoneId].mMSimState){
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	          cluster.setNoSimCard(phoneId, mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.ABSENT || mMSimPhoneStateListener[phoneId].mNoMSimIconId != 0);
	        	setCarrierNoSimCardIocnYL(phoneId);
	        }
	        for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
	        	refreshSimCardVisible(cluster);	   
	        }
	        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
        			+ " mMSimState:" + mMSimPhoneStateListener[phoneId].mMSimState.toString());	        
	        mMSimPhoneStateListener[phoneId].mMSimLastState = mMSimPhoneStateListener[phoneId].mMSimState;
        }        
        
        if (mMSimPhoneStateListener[phoneId].mMSimLastPhoneSignalIconId != mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId
         || mLastWimaxIconId                != mWimaxIconId
         || mMSimPhoneStateListener[phoneId].mLastPhoneSignalTypeIconId != mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId
         || mMSimPhoneStateListener[phoneId].mLastPhoneSignalExIconId != mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId
         || mMSimPhoneStateListener[phoneId].mLastPhoneSignalTypeExIconId != mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId)
        {
            // NB: the mLast*s will be updated later
            for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
                cluster.setSignalIndicators(phoneId, mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId, mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId); 
                cluster.setSlaveSignalIndicators(phoneId, mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId, mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId);                
            }
            if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
        			+ " mMSimPhoneSignalIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId)
                    + " mPhoneSignalTypeIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId)
                    + " mPhoneSignalExIconId:"+ getResourceName(mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId)
                    + " mPhoneSignalTypeExIconId:"+ getResourceName(mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId));             
            mMSimPhoneStateListener[phoneId].mMSimLastPhoneSignalIconId = mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId;
            mLastWimaxIconId = mWimaxIconId;        
            mMSimPhoneStateListener[phoneId].mLastPhoneSignalTypeIconId = mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId;
            mMSimPhoneStateListener[phoneId].mLastPhoneSignalExIconId = mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId;
            mMSimPhoneStateListener[phoneId].mLastPhoneSignalTypeExIconId = mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId;
        }

        if ( mMSimPhoneStateListener[phoneId].mLastRoamingIconId != mMSimPhoneStateListener[phoneId].mCurRoamingIconId){
            for (SignalClusterViewYuLong cluster : mSimSignalClusters) {
            	cluster.setRoamingIndicator(phoneId, mMSimPhoneStateListener[phoneId].mCurRoamingIconId);
            }
            if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshSignalCluster, phoneId:" + phoneId
                    + " mCurRoamingIconId:" + getResourceName(mMSimPhoneStateListener[phoneId].mCurRoamingIconId));
            mMSimPhoneStateListener[phoneId].mLastRoamingIconId = mMSimPhoneStateListener[phoneId].mCurRoamingIconId;
        }      
        // the phone icon on phones
        if (mMSimPhoneStateListener[phoneId].mMSimLastPhoneSignalIconId != mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId) {
        	mMSimPhoneStateListener[phoneId].mMSimLastPhoneSignalIconId = mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId;
        }

        // the data icon on phones
        if (mMSimPhoneStateListener[phoneId].mMSimLastDataDirectionIconId != mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId) {
        	mMSimPhoneStateListener[phoneId].mMSimLastDataDirectionIconId = mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId;
        }

        // the wimax icon on phones
        if (mLastWimaxIconId != mWimaxIconId) {
            mLastWimaxIconId = mWimaxIconId;
        }
        // the combined data signal icon
        if (mMSimPhoneStateListener[phoneId].mMSimLastCombinedSignalIconId !=
        		mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId) {
        	mMSimPhoneStateListener[phoneId].mMSimLastCombinedSignalIconId = mMSimPhoneStateListener[phoneId].mMSimcombinedSignalIconId;
        }
        // the combined data activity icon
        if (mMSimPhoneStateListener[phoneId].mMSimLastcombinedActivityIconId !=
        		mMSimPhoneStateListener[phoneId].mMSimcombinedActivityIconId) {
        	mMSimPhoneStateListener[phoneId].mMSimLastcombinedActivityIconId
                    = mMSimPhoneStateListener[phoneId].mMSimcombinedActivityIconId;
        }
        // the data network type overlay
        if (mMSimPhoneStateListener[phoneId].mMSimLastDataTypeIconId != mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId) {
        	mMSimPhoneStateListener[phoneId].mMSimLastDataTypeIconId = mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId;
        }

      // the combinedLabel in the notification panel
        if (mLastCombinedLabel != null && !mLastCombinedLabel.equals(combinedLabel)) {//===modify by ty
            mLastCombinedLabel = combinedLabel;
            if(mCombinedLabelViews != null){
            	N = mCombinedLabelViews.size();
                for (int i=0; i<N; i++) {
                    TextView v = mCombinedLabelViews.get(i);
                    v.setText(combinedLabel);
                }
            }
        }

        // wifi label
        N = mWifiLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mWifiLabelViews.get(i);
            v.setText(wifiLabel);
            if ("".equals(wifiLabel)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }

        // mobile label
        N = mMobileLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mMobileLabelViews.get(i);
            v.setText(mobileLabel);
            if ("".equals(mobileLabel)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
            }
        }
        setCarrierText();
    }

    protected int getDefaultNetworkTypeIcon(){
    	return 0;
    }
    protected int getDefaultDataTypeIcon(){
    	return 0;
    }
    //是不是安全域，不要显示数据制庿 PS 信号
    public static boolean isSEDRegion () {
    	boolean ret =  "true".equals(SystemProperties.get("ro.secure.system", "false"));
    	return ret;
    }    
    private final int [] mSignalIconList = { 
    		R.drawable.stat_sys_signal_0, 
    		R.drawable.stat_sys_signal_0_1,
    		R.drawable.stat_sys_signal_0_2,
    		R.drawable.stat_sys_signal_0_3,
    		R.drawable.stat_sys_signal_0_4,
    		R.drawable.stat_sys_signal_0_5 };   
    private final int [] mSignalSmallIconList = { 
    		R.drawable.stat_sys_signal_small_0,
			R.drawable.stat_sys_signal_small_0_1,
			R.drawable.stat_sys_signal_small_0_2,
			R.drawable.stat_sys_signal_small_0_3,
			R.drawable.stat_sys_signal_small_0_4,
			R.drawable.stat_sys_signal_small_0_5 };
    protected void calculatePhoneSignalIcon(int phoneId, int iconLvl, int slaveIconLvl) {
        int icon = mSignalNullIconId[phoneId];
        int slaveIcon = 0;

        if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "calculatePhoneSignalIcon phoneId = " + phoneId + " iconLvl = " + iconLvl + " slaveIconLvl = " + slaveIconLvl
        		+ " mSignalSingleMode:" + mMSimPhoneStateListener[phoneId].mSignalSingleMode 
        		+ " mPhoneSignalTypeIconId:" + mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId
        		+ " mPhoneSignalTypeExIconId:" + mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId);
        
        if (iconLvl == -1 && slaveIconLvl == -1){
        	String networktype = YulongConfig.getDefault().getNetworkType();
        	if(networktype.equalsIgnoreCase("CG")){
        		mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId = R.drawable.stat_sys_signal_null_cg;
        		icon = 0;        		
        	}else if(networktype.equalsIgnoreCase("SC")){
        		mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId = R.drawable.stat_sys_signal_null_sc;
        		icon = 0;        		        		
        	}else{
        		mMSimPhoneStateListener[phoneId].mPhoneSignalTypeIconId = mSignalNullIconId[phoneId];
        		icon = R.drawable.stat_sys_signal_0;
        	}
        } else if ( mMSimPhoneStateListener[phoneId].mSignalSingleMode || slaveIconLvl == -1 || iconLvl == -1){ //  单信卿
        	if (isSEDRegion()){
        		if(slaveIconLvl == -1 || mMSimPhoneStateListener[phoneId].mPhoneSignalTypeExIconId == 0){
        			icon = R.drawable.stat_sys_signal_0;      			
        		}else{
        			icon = mSignalIconList[slaveIconLvl];     
        		}
            }else if (iconLvl == -1){
        		icon = mSignalIconList[slaveIconLvl];
        	}else{
        		icon = mSignalIconList[iconLvl];
        	}
        	
        } else {                  // 上下双层信号
            if (isSEDRegion()){
            	icon = mSignalIconList[slaveIconLvl];  
            }else{
                icon = mSignalSmallIconList[iconLvl];
                slaveIcon = mSignalSmallIconList[slaveIconLvl];                	
            }
        }
        mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId = icon;
        mMSimPhoneStateListener[phoneId].mPhoneSignalExIconId = slaveIcon;
    }
       
    protected void updateCdmaOrEvdo(int phoneId, boolean isCdma){
    	
    }
       
    protected int getPsNetworkTypeIcon(int nNetworkType,int phoneId){
        int icon = 0;
        int nNetworkClass = TelephonyManager.getNetworkClass(nNetworkType);
        ServiceState state = mMSimPhoneStateListener[phoneId].mMSimServiceState;
        int voiceNetworkType = state.getVoiceNetworkType();
        switch (nNetworkType) {
    	//NETWORK_CLASS_2_G
        case TelephonyManager.NETWORK_TYPE_GPRS:
        case TelephonyManager.NETWORK_TYPE_GSM:
        	  if(YulongConfig.getDefault().getNetworkType().endsWith("CG") || YulongConfig.getDefault().getNetworkType().endsWith("SC")){
              icon = R.drawable.stat_sys_signal_type_2g;
        	  }else{
        	    icon = R.drawable.stat_sys_signal_type_g;
        	  }
        	break;
        case TelephonyManager.NETWORK_TYPE_EDGE:
        if(YulongConfig.getDefault().getNetworkType().endsWith("WG") || YulongConfig.getDefault().getNetworkType().endsWith("SW") ){
        		icon = R.drawable.stat_sys_signal_type_g;
        	}else if(YulongConfig.getDefault().getNetworkType().endsWith("GG") || YulongConfig.getDefault().getNetworkType().endsWith("SG")){
        		icon = R.drawable.stat_sys_signal_type_e;
        	}else if(YulongConfig.getDefault().getNetworkType().endsWith("CG") || YulongConfig.getDefault().getNetworkType().endsWith("SC")){
        		icon = R.drawable.stat_sys_signal_type_2g;
        	}else{
        		icon = R.drawable.stat_sys_signal_type_e;
        	}
        	break;
        case TelephonyManager.NETWORK_TYPE_IDEN:
        	icon = R.drawable.stat_sys_signal_type_2g;
        	break;
        case TelephonyManager.NETWORK_TYPE_CDMA:
        case TelephonyManager.NETWORK_TYPE_1xRTT:
        	icon = R.drawable.stat_sys_signal_type_2g;      	
        	break;
        //NETWORK_CLASS_3_G;
        case TelephonyManager.NETWORK_TYPE_UMTS:
        case TelephonyManager.NETWORK_TYPE_EVDO_0:
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
        case TelephonyManager.NETWORK_TYPE_HSDPA:
        case TelephonyManager.NETWORK_TYPE_HSUPA:
        case TelephonyManager.NETWORK_TYPE_HSPA:
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
        case TelephonyManager.NETWORK_TYPE_EHRPD:
          if(YulongConfig.getDefault().getNetworkType().endsWith("WG")
        	|| YulongConfig.getDefault().getNetworkType().endsWith("SW")){
        		icon = R.drawable.stat_sys_signal_type_h;
        	}else{
        		icon = R.drawable.stat_sys_signal_type_3g;
        	}
          	break;
        
        case TelephonyManager.NETWORK_TYPE_HSPAP:/** Current network is HSPA+ */
	        {
	        	String networkType = YulongConfig.getDefault().getNetworkType();
	          	if(YulongConfig.getDefault().getNetworkType().endsWith("WG")
	          	   || YulongConfig.getDefault().getNetworkType().endsWith("SW")){
	        		icon = R.drawable.stat_sys_signal_type_hp;
	          	}else if (networkType.endsWith("USA")
	          			|| networkType.endsWith("AGG") || networkType.endsWith("ACG")) {
	                icon = R.drawable.stat_sys_signal_type_4g;
	        	}else{
	        		icon = R.drawable.stat_sys_signal_type_3g;//modify by ty stat_sys_signal_type_3g
	        	}
	        }
         	break;
        //NETWORK_CLASS_4_G
        case TelephonyManager.NETWORK_TYPE_LTE:
        	if(YulongConfig.getDefault().getNetworkType().endsWith("WG")
        		|| YulongConfig.getDefault().getNetworkType().endsWith("SW")){
        		icon = R.drawable.stat_sys_data_type_4glte;
        	}else{
        		icon = R.drawable.stat_sys_signal_type_4g;
        	}
        	break;
        	
        case TelephonyManager.NETWORK_TYPE_IWLAN:
            if(voiceNetworkType == TelephonyManager.NETWORK_TYPE_GSM  ||
                     voiceNetworkType == TelephonyManager.NETWORK_TYPE_GPRS
                     ||voiceNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if(YulongConfig.getDefault().getNetworkType().endsWith("CG") || YulongConfig.getDefault().getNetworkType().endsWith("SC")){
                icon = R.drawable.stat_sys_signal_type_2g;
              }else{
                icon = R.drawable.stat_sys_signal_type_g;
              }
            }else if(voiceNetworkType ==TelephonyManager.NETWORK_TYPE_CDMA  ||
                     voiceNetworkType ==TelephonyManager.NETWORK_TYPE_1xRTT ) {
                     icon = R.drawable.stat_sys_signal_type_2g;  
            }else if (mContext.getResources().getBoolean(R.bool.config_show4gForIWlan)) {
            	icon = R.drawable.stat_sys_signal_type_4g;//===modify by ty
            }else{
            	       icon = R.drawable.stat_sys_signal_type_2g; 
            }
            
          break;
        //NETWORK_CLASS_UNKNOWN
        default:
        	if (nNetworkClass == TelephonyManager.NETWORK_CLASS_2_G){
        		icon = R.drawable.stat_sys_signal_type_2g;
        	}else if(nNetworkClass == TelephonyManager.NETWORK_CLASS_3_G){
				String networkType = YulongConfig.getDefault().getNetworkType();
				if ((networkType.endsWith("USA") || networkType.endsWith("AWG"))
						&& TelephonyManager.NETWORK_TYPE_HSPAP == nNetworkClass) {
					icon = R.drawable.stat_sys_signal_type_4g;// American HSPA+
				} else {
					icon = R.drawable.stat_sys_signal_type_3g;
				}
        		icon = R.drawable.stat_sys_signal_type_3g;
        	}else if(nNetworkClass == TelephonyManager.NETWORK_CLASS_4_G){
        		  if(YulongConfig.getDefault().getNetworkType().endsWith("WG")
             	    || YulongConfig.getDefault().getNetworkType().endsWith("SW")){
                 		icon = R.drawable.stat_sys_data_type_4glte;
                 	}else{
                 		icon = R.drawable.stat_sys_signal_type_4g;
                 	}
        	}else{
        		icon = getDefaultNetworkTypeIcon();
        	}
        }
        LogHelper.sd(TAG, "getPsNetworkTypeIcon  NetworkType = " + nNetworkType + ":" + TelephonyManager.getNetworkTypeName(nNetworkType)
        		+ " icon = " + icon + ":" + getResourceName(icon));
        return icon;    	
    }
    protected int getCsNetworkTypeIcon(int nNetworkType){
        int icon = 0;
        int nNetworkClass = TelephonyManager.getNetworkClass(nNetworkType);
        switch (nNetworkType) {
        case TelephonyManager.NETWORK_TYPE_GPRS:
        case TelephonyManager.NETWORK_TYPE_GSM:
        	  if(YulongConfig.getDefault().getNetworkType().endsWith("CG") || YulongConfig.getDefault().getNetworkType().endsWith("SC")){
        		   icon = R.drawable.stat_sys_signal_type_2g;
            }else{
        	    icon = R.drawable.stat_sys_signal_type_g;
        	  }
        	break;
        case TelephonyManager.NETWORK_TYPE_CDMA:
        case TelephonyManager.NETWORK_TYPE_1xRTT:
        	icon = R.drawable.stat_sys_signal_type_2g;     	
        	break;
        default:
        	if (nNetworkClass == TelephonyManager.NETWORK_CLASS_2_G){
        		icon = R.drawable.stat_sys_signal_type_2g;
        	}else if(nNetworkClass == TelephonyManager.NETWORK_CLASS_3_G){
        		icon = R.drawable.stat_sys_signal_type_3g;
        	}else if(nNetworkClass == TelephonyManager.NETWORK_CLASS_4_G){
          	  if(YulongConfig.getDefault().getNetworkType().endsWith("WG")
        	  	   || YulongConfig.getDefault().getNetworkType().endsWith("SW")){
            		icon = R.drawable.stat_sys_data_type_4glte;
            	}else{
            		icon = R.drawable.stat_sys_signal_type_4g;
            	}
        	}else{
        		icon = getDefaultNetworkTypeIcon();
        	}
        }
        LogHelper.sd(TAG, "getCsNetworkTypeIcon  NetworkType = " + nNetworkType + ":" + TelephonyManager.getNetworkTypeName(nNetworkType)
        		+ " icon = " + icon + ":" + getResourceName(icon));
        return icon;     	
    }
    
    protected int getNetworkTypeLevel(SignalStrength signal,int nNetworkType){
    	int nLevel = -1;
    	try{
    		nLevel = (Integer)SignalStrength.class.getMethod("getNetworkTypeLevel",SignalStrength.class,int.class).invoke(signal,nNetworkType);
    	}
    	catch(Exception e){
    		//LogHelper.sd(TAG, "SignalStrength.getNetworkTypeLevel exception = " + e);
    		LogHelper.sv(TAG, "getNetworkTypeLevel nNetworkType = " + TelephonyManager.getNetworkTypeName(nNetworkType));    		
	    	switch(nNetworkType){
	        case TelephonyManager.NETWORK_TYPE_UNKNOWN:
	        	break;
	        case TelephonyManager.NETWORK_TYPE_GPRS:
	        case TelephonyManager.NETWORK_TYPE_EDGE:	        	
	        case TelephonyManager.NETWORK_TYPE_IDEN:	        	
	        case TelephonyManager.NETWORK_TYPE_GSM:   
	        	nLevel = signal.getGsmLevel();
	        	LogHelper.sv(TAG, "getNetworkTypeLevel getGsmLevel = " + nLevel);
	        	break;
	        case TelephonyManager.NETWORK_TYPE_1xRTT:        	
	        case TelephonyManager.NETWORK_TYPE_CDMA:
	        	nLevel = signal.getCdmaLevel();
	        	LogHelper.sv(TAG, "getNetworkTypeLevel getCdmaLevel = " + nLevel);
	        	break;     
	        case TelephonyManager.NETWORK_TYPE_EVDO_0:
	        case TelephonyManager.NETWORK_TYPE_EVDO_A:
	        case TelephonyManager.NETWORK_TYPE_EVDO_B:
	        case TelephonyManager.NETWORK_TYPE_EHRPD: 
	        	nLevel = signal.getEvdoLevel();
	        	LogHelper.sv(TAG, "getNetworkTypeLevel getEvdoLevel = " + nLevel);
	        	break;        	
	        case TelephonyManager.NETWORK_TYPE_HSDPA:
	        case TelephonyManager.NETWORK_TYPE_HSUPA:
	        case TelephonyManager.NETWORK_TYPE_HSPA:
	        case TelephonyManager.NETWORK_TYPE_HSPAP:
	        case TelephonyManager.NETWORK_TYPE_UMTS:        	
	        case 17://case TelephonyManager.NETWORK_TYPE_TD_SCDMA
	        	nLevel = signal.getTdScdmaLevel();//signal.getTdscdmaLevel();
	        	LogHelper.sv(TAG, "getNetworkTypeLevel getTdscdmaLevel = " + nLevel);
	        	if(nLevel <= 0 ){
		        	nLevel = signal.getGsmLevel();//signal.getTdscdmaLevel();
		        	LogHelper.sv(TAG, "getNetworkTypeLevel getGsmLevel = " + nLevel);	        		
	        	}
	        	break;
	        case TelephonyManager.NETWORK_TYPE_LTE:
	        case TelephonyManager.NETWORK_TYPE_LTE_CA:
	        	nLevel = signal.getLteLevel();
	        	LogHelper.sv(TAG, "getNetworkTypeLevel getLteLevel = " + nLevel);
	        	break;
	    	}    	
    	}
    	return nLevel;
    }
    protected int getPSLevel(SignalStrength signal,ServiceState state){  
    	return getNetworkTypeLevel(signal,state.getDataNetworkType());
    }
    protected int getCSLevel(SignalStrength signal,ServiceState state){
    	return getNetworkTypeLevel(signal,state.getVoiceNetworkType());
    }  
    public boolean isProductMode(){
        String COMM_PROPERTY_DEFAULT_RUN_MODE = "persist.yulong.comm.runmode";
        String defaultRunMode = SystemProperties.get(COMM_PROPERTY_DEFAULT_RUN_MODE, "0000");
        if(defaultRunMode.length() > 1){
            if(defaultRunMode.substring(0, 1).equalsIgnoreCase("0")){
                return true;
            }
        }
        return false;
    }    
    protected void updateSignalStrength(int phoneId){
        ServiceState state = mMSimPhoneStateListener[phoneId].mMSimServiceState;
        SignalStrength signal = mMSimPhoneStateListener[phoneId].mMSimSignalStrength;    	
        int iconLvl = -1;
        int slaveIconLvl = -1;
        if ((state != null && state.getState() == ServiceState.STATE_POWER_OFF &&
                (mMSimPhoneStateListener[phoneId].mMSimDataServiceState != ServiceState.STATE_IN_SERVICE))
                || mMSimPhoneStateListener[phoneId].mMSimState == IccCardConstants.State.ABSENT){
        	if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "updateSignalStrength phoneid = " + phoneId + " no service!");                	       	
        }else if(state != null && signal != null){
        	iconLvl = getPSLevel(signal,state);
        	slaveIconLvl = getCSLevel(signal,state);
        	if (iconLvl == -1 && slaveIconLvl == -1){
        		switch(state.getState()){
        		case ServiceState.STATE_IN_SERVICE:
        			iconLvl = signal.getLevel();
        			break;        			
        		case ServiceState.STATE_OUT_OF_SERVICE:
        		case ServiceState.STATE_EMERGENCY_ONLY:
            		if(isProductMode()){
            			iconLvl = 0;
            		}
            		break;
        		}
        	}
            LogHelper.sd(TAG, "updateSignalStrength phoneid = " + phoneId + " getDataNetworkType() = " + state.getDataNetworkType() + " getVoiceNetworkType() = " + state.getVoiceNetworkType()
            		+" getState() = " + state.getState());                	
        }
        LogHelper.sd(TAG, "updateSignalStrength phoneid = " + phoneId + " iconLvl = " + iconLvl + " slaveIconLvl = " + slaveIconLvl);        
        calculatePhoneSignalIcon(phoneId, iconLvl,slaveIconLvl);
    }    
    
    protected void updateAirplaneMode(boolean force) {
    	super.updateAirplaneMode(force);
    }
 
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args, int phoneId) {
        pw.println("NetworkController for SUB : " + phoneId + " state:");
        pw.println(String.format("  %s network type %d (%s)",
                mConnected?"CONNECTED":"DISCONNECTED",
                mConnectedNetworkType, mConnectedNetworkTypeName));
        pw.println("  - telephony ------");
        pw.print("  hasService()=");
        pw.println(hasService(phoneId));
        pw.print("  mHspaDataDistinguishable=");
        pw.println(mHspaDataDistinguishable);
        pw.print("  mMSimDataConnected=");
        pw.println(mMSimPhoneStateListener[phoneId].mMSimDataConnected);
        pw.print("  mMSimState=");
        pw.println(mMSimPhoneStateListener[phoneId].mMSimState);
        pw.print("  mPhoneState=");
        pw.println(mPhoneState);
        pw.print("  mDataState=");
        pw.println(mDataState);
        pw.print("  mMSimDataActivity=");
        pw.println(mMSimPhoneStateListener[phoneId].mMSimDataActivity);
        pw.print("  mDataNetType=");
        pw.print(mDataNetType);
        pw.print("/");
        pw.println(TelephonyManager.getNetworkTypeName(mDataNetType));
        pw.print("  mMSimServiceState=");
        pw.println(mMSimPhoneStateListener[phoneId].mMSimServiceState);
        pw.print("  mMSimSignalStrength=");
        pw.println(mMSimPhoneStateListener[phoneId].mMSimSignalStrength);
        pw.print("  mLastSignalLevel");
        pw.println(mLastSignalLevel);
        pw.print("  mMSimNetworkName=");
        pw.println(mMSimPhoneStateListener[phoneId].mMSimNetworkName);
        pw.print("  mNetworkNameDefault=");
        pw.println(mNetworkNameDefault);
        pw.print("  mNetworkNameSeparator=");
        pw.println(mNetworkNameSeparator.replace("\n","\\n"));
        pw.print("  mMSimPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimPhoneSignalIconId));
        pw.print("  mMSimDataDirectionIconId=");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataDirectionIconId));
        pw.print("  mMSimDataSignalIconId=");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataSignalIconId));
        pw.print("  mMSimDataTypeIconId=");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimDataTypeIconId));

        pw.println("  - wifi ------");
        pw.print("  mWifiEnabled=");
        pw.println(mWifiEnabled);
        pw.print("  mWifiConnected=");
        pw.println(mWifiConnected);
        pw.print("  mWifiRssi=");
        pw.println(mWifiRssi);
        pw.print("  mWifiLevel=");
        pw.println(mWifiLevel);
        pw.print("  mWifiSsid=");
        pw.println(mWifiSsid);
        pw.println(String.format("  mWifiIconId=0x%08x/%s",
                    mWifiIconId, getResourceName(mWifiIconId)));
        pw.print("  mWifiActivity=");
        pw.println(mWifiActivity);

        if (mWimaxSupported) {
            pw.println("  - wimax ------");
            pw.print("  mIsWimaxEnabled="); pw.println(mIsWimaxEnabled);
            pw.print("  mWimaxConnected="); pw.println(mWimaxConnected);
            pw.print("  mWimaxIdle="); pw.println(mWimaxIdle);
            pw.println(String.format("  mWimaxIconId=0x%08x/%s",
                        mWimaxIconId, getResourceName(mWimaxIconId)));
            pw.println(String.format("  mWimaxSignal=%d", mWimaxSignal));
            pw.println(String.format("  mWimaxState=%d", mWimaxState));
            pw.println(String.format("  mWimaxExtraState=%d", mWimaxExtraState));
        }

        pw.println("  - Bluetooth ----");
        pw.print("  mBtReverseTethered=");
        pw.println(mBluetoothTethered);

        pw.println("  - connectivity ------");
        pw.print("  mInetCondition?1:0=");
        pw.println(mInetCondition?1:0);

        pw.println("  - icons ------");
        pw.print("  mMSimLastPhoneSignalIconId=0x");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimLastPhoneSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimLastPhoneSignalIconId));
        pw.print("  mMSimLastDataDirectionIconId=0x");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimLastDataDirectionIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimLastDataDirectionIconId));
        pw.print("  mLastWifiIconId=0x");
        pw.print(Integer.toHexString(mLastWifiIconId));
        pw.print("/");
        pw.println(getResourceName(mLastWifiIconId));
        pw.print("  mMSimLastCombinedSignalIconId=0x");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimLastCombinedSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimLastCombinedSignalIconId));
        pw.print("  mMSimLastDataTypeIconId=0x");
        pw.print(Integer.toHexString(mMSimPhoneStateListener[phoneId].mMSimLastDataTypeIconId));
        pw.print("/");
        pw.println(getResourceName(mMSimPhoneStateListener[phoneId].mMSimLastDataTypeIconId));
        pw.print("  mMSimLastCombinedLabel=");
        pw.print(mLastCombinedLabel);
        pw.println("");
    }
}
