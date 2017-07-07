/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.keyguard;

import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.widget.Button;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;
import android.os.SystemProperties;
import android.text.TextUtils;

/**
 * This class implements a smart emergency button that updates itself based
 * on telephony state.  When the phone is idle, it is an emergency call button.
 * When there's a call in progress, it presents an appropriate message and
 * allows the user to return to the call.
 */
public class EmergencyButton extends Button {
    private static final Intent INTENT_EMERGENCY_DIAL = new Intent()
            .setAction("com.android.phone.EmergencyDialer.DIAL")
            .setPackage("com.android.phone")
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    /// M modify for MTK
    public static final int OPERATOR_TYPE_CHINA_MOBILE = 3;
    public final static String MTK_OPTR = "ro.operator.optr";
    public final static String MTK_OP_CMCC = "OP01";
    public final static String MTK_OP_CU = "OP02";
    public final static String MTK_OP_CT = "OP09";

    private static final String LOG_TAG = "EmergencyButton";

    final static String YULONG_OP_MODE = "persist.yulong.operators.mode";
    final static String YULONG_OP_MODE_ALL = "1";
    final static String YULONG_OP_MODE_CMCC = "3";
    final static String YULONG_OP_MODE_CU = "4";
    final static String YULONG_OP_MODE_CT = "2";

    final static String YULONG_ALLMODE_OPTR = "persist.yulong.allmode.operator";
    final static String YULONG_ALLMODE_CT = "1";
    final static String YULONG_ALLMODE_CMCC = "2";
    final static String YULONG_ALLMODE_CU = "3";



    KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onSimStateChanged(int subId, int slotId, State simState) {
            updateEmergencyCallButton();
        }

        @Override
        public void onPhoneStateChanged(int phoneState) {
            updateEmergencyCallButton();
        }
        @Override
		    public void onRefreshCarrierInfo() {
			      updateEmergencyCallButton();
		    }
    };

    public interface EmergencyButtonCallback {
        public void onEmergencyButtonClickedWhenInCall();
    }

    private LockPatternUtils mLockPatternUtils;
    private PowerManager mPowerManager;
    private EmergencyButtonCallback mEmergencyButtonCallback;

    private final boolean mIsVoiceCapable;
    private final boolean mEnableEmergencyCallWhileSimLocked;

    public EmergencyButton(Context context) {
        this(context, null);
    }

    public EmergencyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsVoiceCapable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
        mEnableEmergencyCallWhileSimLocked = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enable_emergency_call_while_sim_locked);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                takeEmergencyCallAction();
            }
        });
        updateEmergencyCallButton();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateEmergencyCallButton();
    }

    /**
     * Shows the emergency dialer or returns the user to the existing call.
     */
    public void takeEmergencyCallAction() {
        MetricsLogger.action(mContext, MetricsEvent.ACTION_EMERGENCY_CALL);
        // TODO: implement a shorter timeout once new PowerManager API is ready.
        // should be the equivalent to the old userActivity(EMERGENCY_CALL_TIMEOUT)
        mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        try {
            ActivityManagerNative.getDefault().stopSystemLockTaskMode();
        } catch (RemoteException e) {
            Slog.w(LOG_TAG, "Failed to stop app pinning");
        }
        if (isInCall()) {
            resumeCall();
            if (mEmergencyButtonCallback != null) {
                mEmergencyButtonCallback.onEmergencyButtonClickedWhenInCall();
            }
        } else {
            KeyguardUpdateMonitor.getInstance(mContext).reportEmergencyCallAction(
                    true /* bypassHandler */);
            getContext().startActivityAsUser(INTENT_EMERGENCY_DIAL,
                    ActivityOptions.makeCustomAnimation(getContext(), 0, 0).toBundle(),
                    new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
        }
    }

//    public void updateEmergencyCallButton() {
//        boolean visible = false;
//        if (mIsVoiceCapable) {
//            // Emergency calling requires voice capability.
//            if (isInCall()) {
//                visible = true; // always show "return to call" if phone is off-hook
//            } else {
//                final boolean simLocked = KeyguardUpdateMonitor.getInstance(mContext)
//                        .isSimPinVoiceSecure();
//                if (simLocked) {
//                    // Some countries can't handle emergency calls while SIM is locked.
//                    visible = mEnableEmergencyCallWhileSimLocked;
//                } else {
//                    // Only show if there is a secure screen (pin/pattern/SIM pin/SIM puk);
//                    visible = mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) ||
//                            SystemProperties.getBoolean("persist.radio.emgcy_btn_onswipe", false);
//                }
//            }
//        }
//        if (visible) {
//            setVisibility(View.VISIBLE);
//
//            int textId;
//            if (isInCall()) {
//                textId = com.android.internal.R.string.lockscreen_return_to_call;
//            } else {
//                textId = com.android.internal.R.string.lockscreen_emergency_call;
//            }
//            setText(textId);
//        } else {
//            setVisibility(View.GONE);
//        }
//    }
    
    public void updateEmergencyCallButton() {
        boolean visible = false;
//        boolean isChinaMobile = TelephonyManager.getProductOperator() == TelephonyManager.OPERATOR_TYPE_CHINA_MOBILE
//                || TelephonyManager.getProductOperatorForAllMode() == TelephonyManager.OPERATOR_TYPE_CHINA_MOBILE;
        /// M modify for MTK
        boolean isChinaMobile = isCMCC();

        if (mIsVoiceCapable) {
            // Emergency calling requires voice capability.
            if (isInCall()) {
                visible = true; // always show "return to call" if phone is off-hook
            } else {
                final boolean simLocked = KeyguardUpdateMonitor.getInstance(mContext)
                        .isSimPinVoiceSecure();
				if (simLocked) {
					// Some countries can't handle emergency calls while SIM is
					// locked.
					visible = mEnableEmergencyCallWhileSimLocked;
				} else {
					// Only show if there is a secure screen (pin/pattern/SIM
					// pin/SIM puk);
					if (getId() == R.id.keyguard_emergency_call_button_xw) {
						visible = isChinaMobile;
						Log.v("EmergencyButton", "Products for China Mobile, showing emergency button.");
					} else {
						visible = mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser());
					}
				}
			}
		}
		Log.v("EmergencyButton", "updateEmergencyCallButton() visible = " + visible);

		boolean isInService = false;
		boolean emergencyCallCapable = false;

		if (visible && isChinaMobile) {
			KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(mContext);

			List<SubscriptionInfo> subs = monitor.getSubscriptionInfo(true);
			final int N = subs.size();
			if (N < TelephonyManager.getDefault().getPhoneCount() /*has sim card not inserted */) {
				emergencyCallCapable = monitor.isAbsentSlotEmergencyCallCapable();
			}

			if (!emergencyCallCapable) {
				for (int i = 0; i < N; i++) {
					int subId = subs.get(i).getSubscriptionId();
					Log.v("EmergencyButton", "checking subId = " + subId);
					ServiceState ss = monitor.mServiceStates.get(subId);
					if (ss == null) {
						Log.v("EmergencyButton", "updateEmergencyCallButton() subId = " + subId + " service state not found.");
						continue;
					}
					if (ss.getDataRegState() == ServiceState.STATE_IN_SERVICE
							|| ss.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
						isInService = true;
						Log.v("EmergencyButton", "updateEmergencyCallButton() subId = " + subId + " is in service.");
						break;
					}
				}

//				if (!isInService && N == 0) {
				if (!isInService) {/// M moidfy for MTK
					Iterator<Entry<Integer, ServiceState>> iter = monitor.getServiceStates().entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<Integer, ServiceState> entry = (Entry<Integer, ServiceState>) iter.next();
						Integer subId = entry.getKey();
						Log.v("EmergencyButton", "ensure checking subId = " + subId);
						ServiceState serviceState = entry.getValue();
						if (serviceState.isEmergencyOnly()) {
							emergencyCallCapable = true;
							Log.v("EmergencyButton", "updateEmergencyCallButton() subId = " + subId + " is emergency only.");
							break;
						}
						if (serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE
								|| serviceState.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
							isInService = true;
							Log.v("EmergencyButton", "updateEmergencyCallButton() subId = " + subId + " is in service.");
							break;
						}
					}
				}
			}
		}

        if (visible && (!isChinaMobile || isInService || emergencyCallCapable)) {
            setVisibility(View.VISIBLE);
            int textId;
            if (isInCall()) {
                textId = com.android.internal.R.string.lockscreen_return_to_call;
            } else {
                textId = com.android.internal.R.string.lockscreen_emergency_call;
            }
            setText(textId);
        } else {
            setVisibility(View.GONE);
        }
    }

    public void setCallback(EmergencyButtonCallback callback) {
        mEmergencyButtonCallback = callback;
    }

    /**
     * Resumes a call in progress.
     */
    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    /**
     * @return {@code true} if there is a call currently in progress.
     */
    private boolean isInCall() {
        return getTelecommManager().isInCall();
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }
    
    /// M modify for MTK
    public static boolean isCMCC() {
        String optr = SystemProperties.get(MTK_OPTR);
        if(!TextUtils.isEmpty(optr)) {
            return MTK_OP_CMCC.equals(optr);
        }

        optr = SystemProperties.get(YULONG_OP_MODE);
        if(YULONG_OP_MODE_ALL.equals(optr)) {
            return YULONG_ALLMODE_CMCC.equals(SystemProperties.get(YULONG_ALLMODE_OPTR));
        } else {
            return YULONG_OP_MODE_CMCC.equals(optr);
        }
    }

}
