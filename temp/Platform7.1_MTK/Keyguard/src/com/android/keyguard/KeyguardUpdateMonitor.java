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

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.IUserSwitchObserver;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.collect.Lists;
import com.yulong.android.feature.FeatureConfig;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor.WeatherData;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import static android.os.BatteryManager.BATTERY_HEALTH_UNKNOWN;
import static android.os.BatteryManager.BATTERY_STATUS_FULL;
import static android.os.BatteryManager.BATTERY_STATUS_UNKNOWN;
import static android.os.BatteryManager.EXTRA_HEALTH;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_MAX_CHARGING_CURRENT;
import static android.os.BatteryManager.EXTRA_MAX_CHARGING_VOLTAGE;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_STATUS;

/**
 * Watches for updates that may be interesting to the keyguard, and provides the
 * up to date information as well as a registration for callbacks that care to
 * be updated.
 *
 * Note: under time crunch, this has been extended to include some stuff that
 * doesn't really belong here. see {@link #handleBatteryUpdate} where it
 * shutdowns the device, and {@link #getFailedUnlockAttempts()},
 * {@link #reportFailedAttempt()} and {@link #clearFailedUnlockAttempts()}.
 * Maybe we should rename this 'KeyguardContext'...
 */
public class KeyguardUpdateMonitor implements TrustManager.TrustListener {

	private static final String TAG = "KeyguardUpdateMonitor";
	private static final boolean DEBUG = KeyguardConstants.DEBUG;
	private static final boolean DEBUG_SIM_STATES = KeyguardConstants.DEBUG_SIM_STATES;
	private static final int LOW_BATTERY_THRESHOLD = 20;

	private static final String ACTION_FACE_UNLOCK_STARTED = "com.android.facelock.FACE_UNLOCK_STARTED";
	private static final String ACTION_FACE_UNLOCK_STOPPED = "com.android.facelock.FACE_UNLOCK_STOPPED";

	private static final String ACTION_STRONG_AUTH_TIMEOUT = "com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT";
	private static final String USER_ID = "com.android.systemui.USER_ID";

	private static final String PERMISSION_SELF = "com.android.systemui.permission.SELF";

	// 2016.08.15 add by mengludong
	private static final String ACTION_YULONG_HALLSENSOR = "yulong.intent.action.HallSensor";
	private static final String HALL_VIEW_FINISH_WEATHER = "android.icoolme.intent.action.WEATHER_PDATE_FINISH";
	private static final String HALL_VIEW_UPDATE_WEATHER_FINISH = "com.icoolme.android.weather.action.UPDATE_COMPLETE";
	private static final String HALL_VIEW_SHOURD_UPDATE_WEATHER = "com.icoolme.android.weather.action.SHOURD_UPDATE";
	private static final String HALL_VIEW_GET_WEATHER = "android.icoolme.intent.action.WEATHER_DATA";
	private static final String COOLSHOW_SET_THEME_LOCKSCREEN = "coolshow_set_theme_lockscreen";
	private static final String VLIFE_SET_WITHIN_KEYGUARD = "vlife_set_within_keyguard";
	private static final String SYSTEMUI_KILL_SELF = "systemui_kill_self";
	public static final String SHOWNUM_CHANGED_ACTION = "yulong.intent.action.SHOW_NUM_CHANGED";
	private int mHallState = -1;
	private WeatherData mWeatherData = null;
	public static final String PHONE_NUM_PACKAGE = "com.yulong.coolmessage/com.yulong.android.contacts.dial.DialActivity";
	public static final String MSG_NUM_PACKAGE = "com.yulong.coolmessage/com.yulong.android.mms.ui.MmsConversationListActivity";
	// add end

	/**
	 * Milliseconds after unlocking with fingerprint times out, i.e. the user
	 * has to use a strong auth method like password, PIN or pattern.
	 */
	private static final long FINGERPRINT_UNLOCK_TIMEOUT_MS = 72 * 60 * 60 * 1000;

	// Callback messages
	private static final int MSG_TIME_UPDATE = 301;
	private static final int MSG_BATTERY_UPDATE = 302;
	private static final int MSG_SIM_STATE_CHANGE = 304;
	private static final int MSG_RINGER_MODE_CHANGED = 305;
	private static final int MSG_PHONE_STATE_CHANGED = 306;
	private static final int MSG_DEVICE_PROVISIONED = 308;
	private static final int MSG_DPM_STATE_CHANGED = 309;
	private static final int MSG_USER_SWITCHING = 310;
	private static final int MSG_KEYGUARD_RESET = 312;
	private static final int MSG_BOOT_COMPLETED = 313;
	private static final int MSG_USER_SWITCH_COMPLETE = 314;
	private static final int MSG_USER_INFO_CHANGED = 317;
	private static final int MSG_REPORT_EMERGENCY_CALL_ACTION = 318;
	private static final int MSG_STARTED_WAKING_UP = 319;
	private static final int MSG_FINISHED_GOING_TO_SLEEP = 320;
	private static final int MSG_STARTED_GOING_TO_SLEEP = 321;
	private static final int MSG_KEYGUARD_BOUNCER_CHANGED = 322;
	private static final int MSG_FACE_UNLOCK_STATE_CHANGED = 327;
	private static final int MSG_SIM_SUBSCRIPTION_INFO_CHANGED = 328;
	private static final int MSG_AIRPLANE_MODE_CHANGED = 329;
	private static final int MSG_SERVICE_STATE_CHANGE = 330;
	private static final int MSG_SCREEN_TURNED_ON = 331;
	private static final int MSG_SCREEN_TURNED_OFF = 332;
	private static final int MSG_LOCALE_CHANGED = 500;

	// 2015.11.09 add by mengludong
	private static final int MSG_HALL_STATE_CHANGED = 731;
	private static final int MSG_WEATHER_DATA_CHANGED = 732;
	private static final int MSG_ANTITHEFT_STATE_CHANGED = 733;
	private static final int MSG_NETWORK_ROAMING = 734;
	private static final int MSG_COOLSHOW_THEME_CHANGED = 735;
	private static final int MSG_KEYGUARD_SHOWING_CHANGED = 736;
	private static final int MSG_KEYGUARD_BACKGROUND_COLOR_CHANGED = 737;
	private static final int MSG_SHOW_NUM_CHANGED = 738;
	public static final int BRIGHTNESS_THRESHOLD = 180;
	// add end

	/** Fingerprint state: Not listening to fingerprint. */
	private static final int FINGERPRINT_STATE_STOPPED = 0;

	/** Fingerprint state: Listening. */
	private static final int FINGERPRINT_STATE_RUNNING = 1;

	/**
	 * Fingerprint state: Cancelling and waiting for the confirmation from
	 * FingerprintService to send us the confirmation that cancellation has
	 * happened.
	 */
	private static final int FINGERPRINT_STATE_CANCELLING = 2;

	/**
	 * Fingerprint state: During cancelling we got another request to start
	 * listening, so when we receive the cancellation done signal, we should
	 * start listening again.
	 */
	private static final int FINGERPRINT_STATE_CANCELLING_RESTARTING = 3;

	private static final int DEFAULT_CHARGING_VOLTAGE_MICRO_VOLT = 5000000;

	private static KeyguardUpdateMonitor sInstance;

	private final Context mContext;
	HashMap<Integer, SimData> mSimDatas = new HashMap<Integer, SimData>();
	HashMap<Integer, ServiceState> mServiceStates = new HashMap<Integer, ServiceState>();
	HashMap<String, Integer> mShowNumMap = new HashMap<String, Integer>();
	HashMap<Integer, ServiceState> mServiceStatesForAbsent = new HashMap<Integer, ServiceState>();

	private int mRingMode;
	private int mPhoneState;
	private boolean mKeyguardIsVisible;

	/**
	 * If true, fingerprint was already authenticated and we don't need to start
	 * listening again until the Keyguard has been dismissed.
	 */
	private boolean mFingerprintAlreadyAuthenticated;
	private boolean mGoingToSleep;
	private boolean mBouncer;
	private boolean mBootCompleted;

	// Device provisioning state
	private boolean mDeviceProvisioned;

	// Battery status
	private BatteryStatus mBatteryStatus;

	// Password attempts
	private SparseIntArray mFailedAttempts = new SparseIntArray();

	/**
	 * Tracks whether strong authentication hasn't been used since quite some
	 * time per user.
	 */
	private ArraySet<Integer> mStrongAuthNotTimedOut = new ArraySet<>();
	private final StrongAuthTracker mStrongAuthTracker;

	private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = Lists.newArrayList();
	private ContentObserver mDeviceProvisionedObserver;

	private boolean mSwitchingUser;

	private boolean mDeviceInteractive;
	private boolean mScreenOn;
	private SubscriptionManager mSubscriptionManager;
	private AlarmManager mAlarmManager;
	private List<SubscriptionInfo> mSubscriptionInfo;
	private TrustManager mTrustManager;
	private int mFingerprintRunningState = FINGERPRINT_STATE_STOPPED;
  private String IS_SUPPORT_FRONT_FINGERPRINT = "is_support_front_fingerprint";
	// 2015.11.09 add by mengludong
	private boolean mIsAntitheftPattern;
	private boolean mKeyguardOccluded;
	private boolean mIsNetWorkRoaming;
	private static boolean mIsDarkColor;
	private Toast mToast;
	private boolean mKeyguardShowing;
	protected boolean mSkipNextCoolShowBroadcast;
	// add end
	
	private static boolean mIsCTSIntalled;

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIME_UPDATE:
				handleTimeUpdate();
				break;
			case MSG_BATTERY_UPDATE:
				handleBatteryUpdate((BatteryStatus) msg.obj);
				break;
			case MSG_SIM_STATE_CHANGE:
				handleSimStateChange(msg.arg1, msg.arg2, (State) msg.obj);
				break;
			case MSG_RINGER_MODE_CHANGED:
				handleRingerModeChange(msg.arg1);
				break;
			case MSG_PHONE_STATE_CHANGED:
				handlePhoneStateChanged((String) msg.obj);
				break;
			case MSG_DEVICE_PROVISIONED:
				handleDeviceProvisioned();
				break;
			case MSG_DPM_STATE_CHANGED:
				handleDevicePolicyManagerStateChanged();
				break;
			case MSG_USER_SWITCHING:
				handleUserSwitching(msg.arg1, (IRemoteCallback) msg.obj);
				break;
			case MSG_USER_SWITCH_COMPLETE:
				handleUserSwitchComplete(msg.arg1);
				break;
			case MSG_KEYGUARD_RESET:
				handleKeyguardReset();
				break;
			case MSG_KEYGUARD_BOUNCER_CHANGED:
				handleKeyguardBouncerChanged(msg.arg1);
				break;
			case MSG_BOOT_COMPLETED:
				handleBootCompleted();
				break;
			case MSG_USER_INFO_CHANGED:
				handleUserInfoChanged(msg.arg1);
				break;
			case MSG_REPORT_EMERGENCY_CALL_ACTION:
				handleReportEmergencyCallAction();
				break;
			case MSG_STARTED_GOING_TO_SLEEP:
				handleStartedGoingToSleep(msg.arg1);
				break;
			case MSG_FINISHED_GOING_TO_SLEEP:
				handleFinishedGoingToSleep(msg.arg1);
				break;
			case MSG_STARTED_WAKING_UP:
				handleStartedWakingUp();
				break;
			case MSG_FACE_UNLOCK_STATE_CHANGED:
				handleFaceUnlockStateChanged(msg.arg1 != 0, msg.arg2);
				break;
			case MSG_SIM_SUBSCRIPTION_INFO_CHANGED:
				handleSimSubscriptionInfoChanged();
				break;
			case MSG_AIRPLANE_MODE_CHANGED:
				handleAirplaneModeChanged();
				break;
			case MSG_SERVICE_STATE_CHANGE:
				handleServiceStateChange(msg.arg1, (ServiceState) msg.obj);
				break;
			case MSG_SCREEN_TURNED_ON:
				handleScreenTurnedOn();
				break;
			case MSG_SCREEN_TURNED_OFF:
				handleScreenTurnedOff();
			case MSG_LOCALE_CHANGED:
				handleLocaleChanged();
				break;
			// 2015.11.09 add by mengludong
			case MSG_HALL_STATE_CHANGED:
				handleHallStateChange((int) msg.obj);
				break;
			case MSG_WEATHER_DATA_CHANGED:
				handleWeatherDataChange((WeatherData) msg.obj);
				break;
			case MSG_ANTITHEFT_STATE_CHANGED:
				handleAntitheftStateChange((Boolean) msg.obj);
				break;
			case MSG_NETWORK_ROAMING:
				handleRoamingStateChange();
				break;
			case MSG_COOLSHOW_THEME_CHANGED:
				handleCoolShowThemeChange(msg.arg1 != 0, (String) msg.obj);
				break;
			case MSG_KEYGUARD_SHOWING_CHANGED:
				handleKeyguardShowingChanged(msg.arg1);
				break;
			case MSG_KEYGUARD_BACKGROUND_COLOR_CHANGED:
				handleKeyguardBackgroundColorChanged(msg.arg1 != 0);
				break;
			case MSG_SHOW_NUM_CHANGED:
				handleShowNumChanged(msg.arg1, (String) msg.obj);
				break;
			// add end
			}
		}
	};

	private OnSubscriptionsChangedListener mSubscriptionListener = new OnSubscriptionsChangedListener() {
		@Override
		public void onSubscriptionsChanged() {
			mHandler.sendEmptyMessage(MSG_SIM_SUBSCRIPTION_INFO_CHANGED);
		}
	};

	private SparseBooleanArray mUserHasTrust = new SparseBooleanArray();
	private SparseBooleanArray mUserTrustIsManaged = new SparseBooleanArray();
	private SparseBooleanArray mUserFingerprintAuthenticated = new SparseBooleanArray();
	private SparseBooleanArray mUserFaceUnlockRunning = new SparseBooleanArray();
	private int mFingerPrintFailedTimes = 0;

	private static int sCurrentUser;

	public synchronized static void setCurrentUser(int currentUser) {
		sCurrentUser = currentUser;
	}

	public synchronized static int getCurrentUser() {
		return sCurrentUser;
	}

	@Override
	public void onTrustChanged(boolean enabled, int userId, int flags) {
		mUserHasTrust.put(userId, enabled);
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onTrustChanged(userId);
				if (enabled && flags != 0) {
					cb.onTrustGrantedWithFlags(flags, userId);
				}
			}
		}
	}

	protected void handleSimSubscriptionInfoChanged() {
		if (DEBUG_SIM_STATES) {
			Log.v(TAG, "onSubscriptionInfoChanged()");
			List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();
			if (sil != null) {
				for (SubscriptionInfo subInfo : sil) {
					Log.v(TAG, "SubInfo:" + subInfo);
				}
			} else {
				Log.v(TAG, "onSubscriptionInfoChanged: list is null");
			}
		}
		List<SubscriptionInfo> subscriptionInfos = getSubscriptionInfo(true /* forceReload */);

		// Hack level over 9000: Because the subscription id is not yet valid
		// when we see the
		// first update in handleSimStateChange, we need to force refresh all
		// all SIM states
		// so the subscription id for them is consistent.
		ArrayList<SubscriptionInfo> changedSubscriptions = new ArrayList<>();
		for (int i = 0; i < subscriptionInfos.size(); i++) {
			SubscriptionInfo info = subscriptionInfos.get(i);
			boolean changed = refreshSimState(info.getSubscriptionId(), info.getSimSlotIndex());
			if (changed) {
				changedSubscriptions.add(info);
			}
		}
		for (int i = 0; i < changedSubscriptions.size(); i++) {
			SimData data = mSimDatas.get(changedSubscriptions.get(i).getSubscriptionId());
			for (int j = 0; j < mCallbacks.size(); j++) {
				KeyguardUpdateMonitorCallback cb = mCallbacks.get(j).get();
				if (cb != null) {
					cb.onSimStateChanged(data.subId, data.slotId, data.simState);
				}
			}
		}
		for (int j = 0; j < mCallbacks.size(); j++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(j).get();
			if (cb != null) {
				cb.onRefreshCarrierInfo();
			}
		}
	}

	private void handleAirplaneModeChanged() {
		for (int j = 0; j < mCallbacks.size(); j++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(j).get();
			if (cb != null) {
				cb.onRefreshCarrierInfo();
			}
		}
	}

	/** @return List of SubscriptionInfo records, maybe empty but never null */
	List<SubscriptionInfo> getSubscriptionInfo(boolean forceReload) {
		List<SubscriptionInfo> sil = mSubscriptionInfo;
		if (sil == null || forceReload) {
			sil = mSubscriptionManager.getActiveSubscriptionInfoList();
		}
		if (sil == null) {
			// getActiveSubscriptionInfoList was null callers expect an empty
			// list.
			mSubscriptionInfo = new ArrayList<SubscriptionInfo>();
		} else {
			mSubscriptionInfo = sil;
		}
		return mSubscriptionInfo;
	}

    public boolean isEmergencyOnly() {
        boolean isEmerg = false;
        ServiceState state;
        for (int slotId = 0; slotId < TelephonyManager.getDefault().getPhoneCount(); slotId++) {
            state = null;
            int[] subId = mSubscriptionManager.getSubId(slotId);
            if (subId != null && subId.length > 0) {
                state = mServiceStates.get(subId[0]);
            }
            if (state != null) {
                if (state.getVoiceRegState() == ServiceState.STATE_IN_SERVICE)
                    return false;
                else if (state.isEmergencyOnly()) {
                    isEmerg = true;
                }
            }
        }
        return isEmerg;
    }

    public int getPresentSubId() {
        for (int slotId = 0; slotId < TelephonyManager.getDefault().getPhoneCount(); slotId++) {
            int[] subId = mSubscriptionManager.getSubId(slotId);
            if (subId != null && subId.length > 0 && getSimState(subId[0]) != State.ABSENT) {
                return subId[0];
            }
        }
        return -1;
    }
	@Override
	public void onTrustManagedChanged(boolean managed, int userId) {
		mUserTrustIsManaged.put(userId, managed);

		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onTrustManagedChanged(userId);
			}
		}
	}

	private void onFingerprintAuthenticated(int userId) {
		mUserFingerprintAuthenticated.put(userId, true);

		// If fingerprint unlocking is allowed, this event will lead to a
		// Keyguard dismiss or to a
		// wake-up (if Keyguard is not showing), so we don't need to listen
		// until Keyguard is
		// fully gone.
		mFingerprintAlreadyAuthenticated = isUnlockingWithFingerprintAllowed();
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFingerprintAuthenticated(userId);
			}
		}
	}

	private void handleFingerprintAuthFailed() {
		mFingerPrintFailedTimes++;
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFingerprintAuthFailed();
			}
		}
		handleFingerprintHelp(-1, mContext.getString(R.string.fingerprint_not_recognized));
	}

	private void handleFingerprintAcquired(int acquireInfo) {
		if (acquireInfo != FingerprintManager.FINGERPRINT_ACQUIRED_GOOD) {
			return;
		}
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFingerprintAcquired();
			}
		}
	}

	private void handleFingerprintAuthenticated() {
		try {
			final int userId;
			try {
				userId = ActivityManagerNative.getDefault().getCurrentUser().id;
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to get current user id: ", e);
				return;
			}
			if (isFingerprintDisabled(userId)) {
				Log.d(TAG, "Fingerprint disabled by DPM for userId: " + userId);
				return;
			}
			onFingerprintAuthenticated(userId);
		} finally {
			setFingerprintRunningState(FINGERPRINT_STATE_STOPPED);
		}
	}

	private void handleFingerprintHelp(int msgId, String helpString) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFingerprintHelp(msgId, helpString);
			}
		}
	}

	private void handleFingerprintError(int msgId, String errString) {
		if (msgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED
				&& mFingerprintRunningState == FINGERPRINT_STATE_CANCELLING_RESTARTING) {
			setFingerprintRunningState(FINGERPRINT_STATE_STOPPED);
			startListeningForFingerprint();
		} else {
			setFingerprintRunningState(FINGERPRINT_STATE_STOPPED);
		}
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFingerprintError(msgId, errString);
			}
		}
	}

	public boolean shouldWakeUpForFingerprint() {
		return mFingerPrintFailedTimes >= 5;
	}
	
	private void handleFingerprintLockoutReset() {
		updateFingerprintListeningState();
	}

	private void setFingerprintRunningState(int fingerprintRunningState) {
		boolean wasRunning = mFingerprintRunningState == FINGERPRINT_STATE_RUNNING;
		boolean isRunning = fingerprintRunningState == FINGERPRINT_STATE_RUNNING;
		mFingerprintRunningState = fingerprintRunningState;

		// Clients of KeyguardUpdateMonitor don't care about the internal state
		// about the
		// asynchronousness of the cancel cycle. So only notify them if the
		// actualy running state
		// has changed.
		if (wasRunning != isRunning) {
			notifyFingerprintRunningStateChanged();
		}
	}

	private void notifyFingerprintRunningStateChanged() {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFingerprintRunningStateChanged(isFingerprintDetectionRunning());
			}
		}
	}

	private void handleFaceUnlockStateChanged(boolean running, int userId) {
		mUserFaceUnlockRunning.put(userId, running);
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFaceUnlockStateChanged(running, userId);
			}
		}
	}

	public boolean isFaceUnlockRunning(int userId) {
		return mUserFaceUnlockRunning.get(userId);
	}

	public boolean isFingerprintDetectionRunning() {
		return mFingerprintRunningState == FINGERPRINT_STATE_RUNNING;
	}

	private boolean isTrustDisabled(int userId) {
		// Don't allow trust agent if device is secured with a SIM PIN. This is
		// here
		// mainly because there's no other way to prompt the user to enter their
		// SIM PIN
		// once they get past the keyguard screen.
		final boolean disabledBySimPin = isSimPinSecure();
		return disabledBySimPin;
	}

	private boolean isFingerprintDisabled(int userId) {
		final DevicePolicyManager dpm = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
		return dpm != null && (dpm.getKeyguardDisabledFeatures(null, userId)
				& DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0 || isSimPinSecure();
	}

	public boolean getUserCanSkipBouncer(int userId) {
		return getUserHasTrust(userId)
				|| (mUserFingerprintAuthenticated.get(userId) && isUnlockingWithFingerprintAllowed());
	}

	public boolean getUserHasTrust(int userId) {
		return !isTrustDisabled(userId) && mUserHasTrust.get(userId);
	}

	public boolean getUserTrustIsManaged(int userId) {
		return mUserTrustIsManaged.get(userId) && !isTrustDisabled(userId);
	}

	public boolean isUnlockingWithFingerprintAllowed() {
//		return mStrongAuthTracker.isUnlockingWithFingerprintAllowed() && !hasFingerprintUnlockTimedOut(sCurrentUser);
    return true;
	}

	public StrongAuthTracker getStrongAuthTracker() {
		return mStrongAuthTracker;
	}

	/**
	 * @return true if the user hasn't use strong authentication (pattern, PIN,
	 *         password) since a while and thus can't unlock with fingerprint,
	 *         false otherwise
	 */
	public boolean hasFingerprintUnlockTimedOut(int userId) {
		return !mStrongAuthNotTimedOut.contains(userId);
	}

	public void reportSuccessfulStrongAuthUnlockAttempt() {
		mStrongAuthNotTimedOut.add(sCurrentUser);
		scheduleStrongAuthTimeout();
		if (mFpm != null) {
			byte[] token = null; /*
									 * TODO: pass real auth token once fp HAL
									 * supports it
									 */
			mFpm.resetTimeout(token);
		}
	}

	private void scheduleStrongAuthTimeout() {
		long when = SystemClock.elapsedRealtime() + FINGERPRINT_UNLOCK_TIMEOUT_MS;
		Intent intent = new Intent(ACTION_STRONG_AUTH_TIMEOUT);
		intent.putExtra(USER_ID, sCurrentUser);
		PendingIntent sender = PendingIntent.getBroadcast(mContext, sCurrentUser, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, when, sender);
		notifyStrongAuthStateChanged(sCurrentUser);
	}

	private void notifyStrongAuthStateChanged(int userId) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onStrongAuthStateChanged(userId);
			}
		}
	}

	static class DisplayClientState {
		public int clientGeneration;
		public boolean clearing;
		public PendingIntent intent;
		public int playbackState;
		public long playbackEventTime;
	}

	private DisplayClientState mDisplayClientState = new DisplayClientState();

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (DEBUG)
				Log.d(TAG, "received broadcast " + action);

			if (Intent.ACTION_TIME_TICK.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)
					|| Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
				mHandler.sendEmptyMessage(MSG_TIME_UPDATE);
			} else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				final int status = intent.getIntExtra(EXTRA_STATUS, BATTERY_STATUS_UNKNOWN);
				final int plugged = intent.getIntExtra(EXTRA_PLUGGED, 0);
				final int level = intent.getIntExtra(EXTRA_LEVEL, 0);
				final int health = intent.getIntExtra(EXTRA_HEALTH, BATTERY_HEALTH_UNKNOWN);

				final int maxChargingMicroAmp = intent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1);
				int maxChargingMicroVolt = intent.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, -1);
				final int maxChargingMicroWatt;

				if (maxChargingMicroVolt <= 0) {
					maxChargingMicroVolt = DEFAULT_CHARGING_VOLTAGE_MICRO_VOLT;
				}
				if (maxChargingMicroAmp > 0) {
					// Calculating muW = muA * muV / (10^6 mu^2 / mu); splitting
					// up the divisor
					// to maintain precision equally on both factors.
					maxChargingMicroWatt = (maxChargingMicroAmp / 1000) * (maxChargingMicroVolt / 1000);
				} else {
					maxChargingMicroWatt = -1;
				}
				final Message msg = mHandler.obtainMessage(MSG_BATTERY_UPDATE,
						new BatteryStatus(status, level, plugged, health, maxChargingMicroWatt));
				mHandler.sendMessage(msg);
			} else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
				SimData args = SimData.fromIntent(intent);
				if (DEBUG_SIM_STATES) {
					Log.v(TAG,
							"action " + action + " state: "
									+ intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE) + " slotId: "
									+ args.slotId + " subid: " + args.subId);
				}
				mHandler.obtainMessage(MSG_SIM_STATE_CHANGE, args.subId, args.slotId, args.simState).sendToTarget();
			} else if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_RINGER_MODE_CHANGED,
						intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1), 0));
			} else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
				String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
				mHandler.sendMessage(mHandler.obtainMessage(MSG_PHONE_STATE_CHANGED, state));
			} else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
				mHandler.sendEmptyMessage(MSG_AIRPLANE_MODE_CHANGED);
			} else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
				dispatchBootCompleted();
			} else if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
				ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
				int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
						SubscriptionManager.INVALID_SUBSCRIPTION_ID);
				if (DEBUG) {
					Log.v(TAG, "action " + action + " serviceState=" + serviceState + " subId=" + subId);
				}
				mHandler.sendMessage(mHandler.obtainMessage(MSG_SERVICE_STATE_CHANGE, subId, 0, serviceState));
				// 2015.12.01 add by mengludong to show double clock view when
				// net work roaming
				boolean isRoaming = isNetWorkRoaming();
				if (isRoaming != mIsNetWorkRoaming) {
					mIsNetWorkRoaming = isRoaming;
					mHandler.sendMessage(mHandler.obtainMessage(MSG_NETWORK_ROAMING));
				}
				// add end
			} else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
				mHandler.sendEmptyMessage(MSG_LOCALE_CHANGED);
			} // 2015.12.03 add by mengludong, for coolshow lock theme
			else if (COOLSHOW_SET_THEME_LOCKSCREEN.equals(action)) {
				if (mSkipNextCoolShowBroadcast) {
					mSkipNextCoolShowBroadcast = false;
					return;
				}
				boolean isZooking = intent.getBooleanExtra("isZooking", false);
				String path = null;
				if (isZooking)
					path = intent.getStringExtra("path");
				if (DEBUG)
					Log.v(TAG, "COOLSHOW_SET_THEME_LOCKSCREEN isZooking = " + isZooking + ", path = " + path);
				mHandler.sendMessage(mHandler.obtainMessage(MSG_COOLSHOW_THEME_CHANGED, 0, 0, path));
			} else if (VLIFE_SET_WITHIN_KEYGUARD.equals(action)) {
				boolean isZooking = intent.getBooleanExtra("isZooking", false);
				String path = null;
				if (isZooking) {
					path = intent.getStringExtra("path");
				}
				if (DEBUG)
					Log.v(TAG, "VLIFE_SET_WITHIN_KEYGUARD isZooking = " + isZooking + ", path = " + path);
				mSkipNextCoolShowBroadcast = true;
				mHandler.sendMessage(mHandler.obtainMessage(MSG_COOLSHOW_THEME_CHANGED, 1, 0, path));
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mSkipNextCoolShowBroadcast = false;
					}
				}, 1000);
			} else if (SYSTEMUI_KILL_SELF.equals(action)) {
				String pw = intent.getStringExtra("passWord");
				if ("coollifeui".equals(pw)) {
					android.os.Process.killProcess(android.os.Process.myPid());
					System.exit(1);
				}
			} else if (SHOWNUM_CHANGED_ACTION.equals(action)) {
				String packageName = intent.getStringExtra("packageName");
				String className = intent.getStringExtra("className");
				int showNum = intent.getIntExtra("showNum", 0);
				Log.d(TAG, "received SHOWNUM_CHANGED_ACTION num = " + showNum + ", packageName = " + packageName
						+ ", className = " + className);
				String key = packageName + "/" + className;
				mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_NUM_CHANGED, showNum, 0, key));
			} else if (ACTION_YULONG_HALLSENSOR.equals(action)) {
				int state = intent.getIntExtra("state", 0);
				mHandler.sendMessage(mHandler.obtainMessage(MSG_HALL_STATE_CHANGED, state));
				if (DEBUG) Log.d(TAG, "received yulong.intent.action.HallSensor state = " + state);
			} else if (HALL_VIEW_FINISH_WEATHER.equals(action) || HALL_VIEW_UPDATE_WEATHER_FINISH.equals(action)
					|| HALL_VIEW_SHOURD_UPDATE_WEATHER.equals(action)) {
				context.sendBroadcastAsUser(new Intent("android.icoolme.intent.action.GET_WEATHER_DATA"), UserHandle.ALL);
			} else if (HALL_VIEW_GET_WEATHER.equals(action)) {
				WeatherData weatherData = getWeatherDataForm(intent);
				if (DEBUG) Log.v(TAG, "HALL_VIEW_GET_WEATHER weatherData : " + weatherData);
				mHandler.sendMessage(mHandler.obtainMessage(MSG_WEATHER_DATA_CHANGED, weatherData));
			}
			// add end
		}
	};

	private final BroadcastReceiver mBroadcastAllReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			String data = intent.getDataString();
			if (AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED.equals(action)) {
				mHandler.sendEmptyMessage(MSG_TIME_UPDATE);
			} else if (Intent.ACTION_USER_INFO_CHANGED.equals(action)) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_USER_INFO_CHANGED,
						intent.getIntExtra(Intent.EXTRA_USER_HANDLE, getSendingUserId()), 0));
			} else if (ACTION_FACE_UNLOCK_STARTED.equals(action)) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_FACE_UNLOCK_STATE_CHANGED, 1, getSendingUserId()));
			} else if (ACTION_FACE_UNLOCK_STOPPED.equals(action)) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_FACE_UNLOCK_STATE_CHANGED, 0, getSendingUserId()));
			} else if (DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED.equals(action)) {
				mHandler.sendEmptyMessage(MSG_DPM_STATE_CHANGED);
			} else if (null != data) {
				data = data.substring(8);
				if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
					Log.e(TAG, "ACTION_PACKAGE_ADDED:" + data);
					if ("com.android.cts.ui".contains(data)) {
						mIsCTSIntalled = true;
					}
				} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
					Log.e(TAG, "ACTION_PACKAGE_REMOVED:" + data);
					if ("com.android.cts.ui".contains(data)) {
						mIsCTSIntalled = false;
					}
				}
			}
		}
	};

	private final BroadcastReceiver mStrongAuthTimeoutReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_STRONG_AUTH_TIMEOUT.equals(intent.getAction())) {
				int userId = intent.getIntExtra(USER_ID, -1);
				mStrongAuthNotTimedOut.remove(userId);
				notifyStrongAuthStateChanged(userId);
			}
		}
	};

	private final FingerprintManager.LockoutResetCallback mLockoutResetCallback = new FingerprintManager.LockoutResetCallback() {
		@Override
		public void onLockoutReset() {
			handleFingerprintLockoutReset();
		}
	};

	private FingerprintManager.AuthenticationCallback mAuthenticationCallback = new AuthenticationCallback() {

		@Override
		public void onAuthenticationFailed() {
			handleFingerprintAuthFailed();
			if (DEBUG)
				Log.v(TAG, "onAuthenticationFailed()");
		};

		@Override
		public void onAuthenticationSucceeded(AuthenticationResult result) {
			Log.d("fingerWakeupSlowLog:", "Keyguardupdatemonitor,onAuthenticationSucceeded()");
			handleFingerprintAuthenticated();
			if (DEBUG)
				Log.v(TAG, "onAuthenticationSucceeded()");
		}

		@Override
		public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
			if (helpString == null) {
				helpString = "";
			}
			handleFingerprintHelp(helpMsgId, helpString.toString());
			if (DEBUG)
				Log.v(TAG, "handleFingerprintHelp() helpMsgId = " + helpMsgId + ", helpString = " + helpString);
		}

		@Override
		public void onAuthenticationError(int errMsgId, CharSequence errString) {
			if (errString == null) {
				errString = "";
			}
			handleFingerprintError(errMsgId, errString.toString());
			if (DEBUG)
				Log.v(TAG, "handleFingerprintError() errMsgId = " + errMsgId + ", errString = " + errString);
		}

		@Override
		public void onAuthenticationAcquired(int acquireInfo) {
			handleFingerprintAcquired(acquireInfo);
			if (DEBUG)
				Log.v(TAG, "onAuthenticationAcquired() acquireInfo = " + acquireInfo);
		}
	};
	private CancellationSignal mFingerprintCancelSignal;
	private FingerprintManager mFpm;

	/**
	 * When we receive a
	 * {@link com.android.internal.telephony.TelephonyIntents#ACTION_SIM_STATE_CHANGED}
	 * broadcast, and then pass a result via our handler to
	 * {@link KeyguardUpdateMonitor#handleSimStateChange}, we need a single
	 * object to pass to the handler. This class helps decode the intent and
	 * provide a {@link SimCard.State} result.
	 */
	private static class SimData {
		public State simState;
		public int slotId;
		public int subId;

		SimData(State state, int slot, int id) {
			simState = state;
			slotId = slot;
			subId = id;
		}

		static SimData fromIntent(Intent intent) {
			State state;
			if (!TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
				throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
			}
			String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
			int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
			int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
					SubscriptionManager.INVALID_SUBSCRIPTION_ID);
			if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
				final String absentReason = intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);

				if (IccCardConstants.INTENT_VALUE_ABSENT_ON_PERM_DISABLED.equals(absentReason)) {
					state = IccCardConstants.State.PERM_DISABLED;
				} else {
					state = IccCardConstants.State.ABSENT;
				}
			} else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
				state = IccCardConstants.State.READY;
			} else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
				final String lockedReason = intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
				if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
					state = IccCardConstants.State.PIN_REQUIRED;
				} else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
					state = IccCardConstants.State.PUK_REQUIRED;
				} else {
					state = IccCardConstants.State.UNKNOWN;
				}
			} else if (IccCardConstants.INTENT_VALUE_LOCKED_NETWORK.equals(stateExtra)) {
				state = IccCardConstants.State.NETWORK_LOCKED;
			} else if (IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(stateExtra)) {
				state = IccCardConstants.State.CARD_IO_ERROR;
			} else if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(stateExtra)
					|| IccCardConstants.INTENT_VALUE_ICC_IMSI.equals(stateExtra)) {
				// This is required because telephony doesn't return to "READY"
				// after
				// these state transitions. See bug 7197471.
				state = IccCardConstants.State.READY;
			} else {
				state = IccCardConstants.State.UNKNOWN;
			}
			return new SimData(state, slotId, subId);
		}

		@Override
		public String toString() {
			return "SimData{state=" + simState + ",slotId=" + slotId + ",subId=" + subId + "}";
		}
	}

	public static class BatteryStatus {
		public static final int CHARGING_UNKNOWN = -1;
		public static final int CHARGING_SLOWLY = 0;
		public static final int CHARGING_REGULAR = 1;
		public static final int CHARGING_FAST = 2;

		public final int status;
		public final int level;
		public final int plugged;
		public final int health;
		public final int maxChargingWattage;

		public BatteryStatus(int status, int level, int plugged, int health, int maxChargingWattage) {
			this.status = status;
			this.level = level;
			this.plugged = plugged;
			this.health = health;
			this.maxChargingWattage = maxChargingWattage;
		}

		/**
		 * Determine whether the device is plugged in (USB, power, or wireless).
		 * 
		 * @return true if the device is plugged in.
		 */
		public boolean isPluggedIn() {
			return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
					|| plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
		}

		/**
		 * Whether or not the device is charged. Note that some devices never
		 * return 100% for battery level, so this allows either battery level or
		 * status to determine if the battery is charged.
		 * 
		 * @return true if the device is charged
		 */
		public boolean isCharged() {
			return status == BATTERY_STATUS_FULL || level >= 100;
		}

		/**
		 * Whether battery is low and needs to be charged.
		 * 
		 * @return true if battery is low
		 */
		public boolean isBatteryLow() {
			return level < LOW_BATTERY_THRESHOLD;
		}

		public final int getChargingSpeed(int slowThreshold, int fastThreshold) {
			return maxChargingWattage <= 0 ? CHARGING_UNKNOWN
					: maxChargingWattage < slowThreshold ? CHARGING_SLOWLY
							: maxChargingWattage > fastThreshold ? CHARGING_FAST : CHARGING_REGULAR;
		}
	}

	public class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
		public StrongAuthTracker(Context context) {
			super(context);
		}

		public boolean isUnlockingWithFingerprintAllowed() {
			int userId = getCurrentUser();
			return isFingerprintAllowedForUser(userId);
		}

		public boolean hasUserAuthenticatedSinceBoot() {
			int userId = getCurrentUser();
			return (getStrongAuthForUser(userId) & STRONG_AUTH_REQUIRED_AFTER_BOOT) == 0;
		}

		@Override
		public void onStrongAuthRequiredChanged(int userId) {
			notifyStrongAuthStateChanged(userId);
		}
	}

	public static KeyguardUpdateMonitor getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new KeyguardUpdateMonitor(context);
		}
		return sInstance;
	}

	protected void handleStartedWakingUp() {
		updateFingerprintListeningState();
		final int count = mCallbacks.size();
		for (int i = 0; i < count; i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onStartedWakingUp();
			}
		}
	}

	protected void handleStartedGoingToSleep(int arg1) {
		clearFingerprintRecognized();
		final int count = mCallbacks.size();
		for (int i = 0; i < count; i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onStartedGoingToSleep(arg1);
			}
		}
		mGoingToSleep = true;
		mFingerprintAlreadyAuthenticated = false;
		updateFingerprintListeningState();
	}

	protected void handleFinishedGoingToSleep(int arg1) {
		mGoingToSleep = false;
		final int count = mCallbacks.size();
		for (int i = 0; i < count; i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onFinishedGoingToSleep(arg1);
			}
		}
		updateFingerprintListeningState();
	}

	private void handleScreenTurnedOn() {
		final int count = mCallbacks.size();
		for (int i = 0; i < count; i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onScreenTurnedOn();
			}
		}
	}

	private void handleScreenTurnedOff() {
		final int count = mCallbacks.size();
		for (int i = 0; i < count; i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onScreenTurnedOff();
			}
		}
	}

	/**
	 * IMPORTANT: Must be called from UI thread.
	 */
	public void dispatchSetBackground(Bitmap bmp) {
		if (DEBUG)
			Log.d(TAG, "dispatchSetBackground");
		final int count = mCallbacks.size();
		for (int i = 0; i < count; i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onSetBackground(bmp);
			}
		}
	}

	private void handleUserInfoChanged(int userId) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onUserInfoChanged(userId);
			}
		}
	}

	private KeyguardUpdateMonitor(Context context) {
		mContext = context;
		mSubscriptionManager = SubscriptionManager.from(context);
		mAlarmManager = context.getSystemService(AlarmManager.class);
		mDeviceProvisioned = isDeviceProvisionedInSettingsDb();
		mStrongAuthTracker = new StrongAuthTracker(context);

		// Since device can't be un-provisioned, we only need to register a
		// content observer
		// to update mDeviceProvisioned when we are...
		if (!mDeviceProvisioned) {
			watchForDeviceProvisioning();
		}

		// Take a guess at initial SIM state, battery status and PLMN until we
		// get an update
		mBatteryStatus = new BatteryStatus(BATTERY_STATUS_UNKNOWN, 100, 0, 0, 0);

		// Watch for interesting updates
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
		filter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
		filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
		// 2016.08.15 add by mengludong
		filter.addAction(ACTION_YULONG_HALLSENSOR);
		filter.addAction(HALL_VIEW_FINISH_WEATHER);
		filter.addAction(HALL_VIEW_UPDATE_WEATHER_FINISH);
		filter.addAction(HALL_VIEW_SHOURD_UPDATE_WEATHER);
		filter.addAction(HALL_VIEW_GET_WEATHER);
		filter.addAction(COOLSHOW_SET_THEME_LOCKSCREEN);
		filter.addAction(VLIFE_SET_WITHIN_KEYGUARD);
		filter.addAction(SYSTEMUI_KILL_SELF);
		filter.addAction(SHOWNUM_CHANGED_ACTION);
		// add end

		// 2016.08.01 add by ty
		filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		// add end
		context.registerReceiver(mBroadcastReceiver, filter);

		final IntentFilter bootCompleteFilter = new IntentFilter();
		bootCompleteFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		bootCompleteFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
		context.registerReceiver(mBroadcastReceiver, bootCompleteFilter);

		final IntentFilter allUserFilter = new IntentFilter();
		allUserFilter.addAction(Intent.ACTION_USER_INFO_CHANGED);
		allUserFilter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
		allUserFilter.addAction(ACTION_FACE_UNLOCK_STARTED);
		allUserFilter.addAction(ACTION_FACE_UNLOCK_STOPPED);
		allUserFilter.addAction(DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED);
		allUserFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		allUserFilter.addDataScheme("package");
		allUserFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		context.registerReceiverAsUser(mBroadcastAllReceiver, UserHandle.ALL, allUserFilter, null, null);

		mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionListener);
		try {
			ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
				@Override
				public void onUserSwitching(int newUserId, IRemoteCallback reply) {
					mHandler.sendMessage(mHandler.obtainMessage(MSG_USER_SWITCHING, newUserId, 0, reply));
				}

				@Override
				public void onUserSwitchComplete(int newUserId) throws RemoteException {
					mHandler.sendMessage(mHandler.obtainMessage(MSG_USER_SWITCH_COMPLETE, newUserId, 0));
				}

				@Override
				public void onForegroundProfileSwitch(int newProfileId) {
					// Ignore.
				}
			}, TAG);//ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		IntentFilter strongAuthTimeoutFilter = new IntentFilter();
		strongAuthTimeoutFilter.addAction(ACTION_STRONG_AUTH_TIMEOUT);
		context.registerReceiver(mStrongAuthTimeoutReceiver, strongAuthTimeoutFilter, PERMISSION_SELF,
				null /* handler */);
		mTrustManager = (TrustManager) context.getSystemService(Context.TRUST_SERVICE);
		mTrustManager.registerTrustListener(this);
		new LockPatternUtils(context).registerStrongAuthTracker(mStrongAuthTracker);

		mFpm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
		updateFingerprintListeningState();
		if (mFpm != null) {
			mFpm.addLockoutResetCallback(mLockoutResetCallback);
		}
	}

	private void updateFingerprintListeningState() {
		boolean shouldListenForFingerprint = shouldListenForFingerprint();
		Log.d(TAG, "mFingerprintRunningState = " + mFingerprintRunningState + ", shouldListenForFingerprint = " + shouldListenForFingerprint);
		if (mFingerprintRunningState == FINGERPRINT_STATE_RUNNING && !shouldListenForFingerprint) {
			if(!isOccluded()){
				stopListeningForFingerprint();
			}
			
		} else if (mFingerprintRunningState != FINGERPRINT_STATE_RUNNING && shouldListenForFingerprint) {
			startListeningForFingerprint();
		}
	}

	private boolean shouldListenForFingerprint() {
		if (DEBUG)
			Log.d(TAG, "mKeyguardIsVisible = " + mKeyguardIsVisible
					+"mKeyguardShowing = " + mKeyguardShowing
					+ ", mSwitchingUser = " + mSwitchingUser
					+ ", isFingerConfigEnable() = " + isFingerConfigEnable()
					+ ", isFingerprintDisabled(getCurrentUser()) =" + isFingerprintDisabled(getCurrentUser())
					+ ", mFingerprintAlreadyAuthenticated = "
					+ mFingerprintAlreadyAuthenticated + ", mDeviceInteractive = " + mDeviceInteractive);
		return (mKeyguardIsVisible || !mDeviceInteractive /*|| mBouncer || mGoingToSleep*/) && !mSwitchingUser
				&& isFingerConfigEnable() && !mFingerprintAlreadyAuthenticated && !isFingerprintDisabled(getCurrentUser());
	}
	
    // Whether fingerprint is enabled or not
	public boolean isFingerConfigEnable() {
		boolean IS_SUPPORT_PRE_FINGERPRINT = FeatureConfig.getIntValue(IS_SUPPORT_FRONT_FINGERPRINT)!= 0 ? true : false;
		int mFingerPrintEnable = Integer.parseInt(SystemProperties.get("persist.sys.fp.image", "0"));
		int mFrontFingerPrintEnable = Integer.parseInt(SystemProperties.get("persist.yulong.fp.unlock","0"));		 
		Log.v(TAG, "isFingerConfigEnable() mFingerPrintEnable = " + mFingerPrintEnable +"  mFrontFingerPrintEnable="+mFrontFingerPrintEnable
				+"IS_SUPPORT_PRE_FINGERPRINT="+IS_SUPPORT_PRE_FINGERPRINT);
		if(IS_SUPPORT_PRE_FINGERPRINT){
			if(mFrontFingerPrintEnable == 1){
			     return true;
			}else{
				   return false;
			}
		}

		return mFingerPrintEnable == 1;
	}
	

	private void startListeningForFingerprint() {
		if (mFingerprintRunningState == FINGERPRINT_STATE_CANCELLING) {
			setFingerprintRunningState(FINGERPRINT_STATE_CANCELLING_RESTARTING);
			return;
		}
		if (DEBUG)
			Log.v(TAG, "startListeningForFingerprint()");
		int userId = ActivityManager.getCurrentUser();
		if (isUnlockWithFingerprintPossible(userId) && isFingerConfigEnable()) {
			mFingerPrintFailedTimes = 0;
			/*if (mFingerprintCancelSignal != null) {
				mFingerprintCancelSignal.cancel();
			}*/
			mFingerprintCancelSignal = new CancellationSignal();
			mFpm.authenticate(null, mFingerprintCancelSignal, 0, mAuthenticationCallback, null, userId);
			setFingerprintRunningState(FINGERPRINT_STATE_RUNNING);
		}
	}

	public boolean isUnlockWithFingerprintPossible(int userId) {
		return mFpm != null && mFpm.isHardwareDetected() && !isFingerprintDisabled(userId)
				&& mFpm.getEnrolledFingerprints(userId).size() > 0;
	}

	public void stopListeningForFingerprint() {
		if (DEBUG)
			Log.v(TAG, "stopListeningForFingerprint()");
		if (mFingerprintRunningState == FINGERPRINT_STATE_RUNNING) {
			mFingerprintCancelSignal.cancel();
			mFingerprintCancelSignal = null;
			setFingerprintRunningState(FINGERPRINT_STATE_CANCELLING);
		}
		if (mFingerprintRunningState == FINGERPRINT_STATE_CANCELLING_RESTARTING) {
			setFingerprintRunningState(FINGERPRINT_STATE_CANCELLING);
		}
	}

	private boolean isDeviceProvisionedInSettingsDb() {
		return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0) != 0;
	}

	private void watchForDeviceProvisioning() {
		mDeviceProvisionedObserver = new ContentObserver(mHandler) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				mDeviceProvisioned = isDeviceProvisionedInSettingsDb();
				if (mDeviceProvisioned) {
					mHandler.sendEmptyMessage(MSG_DEVICE_PROVISIONED);
				}
				if (DEBUG)
					Log.d(TAG, "DEVICE_PROVISIONED state = " + mDeviceProvisioned);
			}
		};

		mContext.getContentResolver().registerContentObserver(
				Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED), false, mDeviceProvisionedObserver);

		// prevent a race condition between where we check the flag and where we
		// register the
		// observer by grabbing the value once again...
		boolean provisioned = isDeviceProvisionedInSettingsDb();
		if (provisioned != mDeviceProvisioned) {
			mDeviceProvisioned = provisioned;
			if (mDeviceProvisioned) {
				mHandler.sendEmptyMessage(MSG_DEVICE_PROVISIONED);
			}
		}
	}

	/**
	 * Handle {@link #MSG_DPM_STATE_CHANGED}
	 */
	protected void handleDevicePolicyManagerStateChanged() {
		updateFingerprintListeningState();
		for (int i = mCallbacks.size() - 1; i >= 0; i--) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onDevicePolicyManagerStateChanged();
			}
		}
	}

	/**
	 * Handle {@link #MSG_USER_SWITCHING}
	 */
	protected void handleUserSwitching(int userId, IRemoteCallback reply) {
		mSwitchingUser = true;
		updateFingerprintListeningState();

		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onUserSwitching(userId);
			}
		}
		try {
			reply.sendResult(null);
		} catch (RemoteException e) {
		}
	}

	/**
	 * Handle {@link #MSG_USER_SWITCH_COMPLETE}
	 */
	protected void handleUserSwitchComplete(int userId) {
		mSwitchingUser = false;
		updateFingerprintListeningState();

		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onUserSwitchComplete(userId);
			}
		}
	}

	/**
	 * This is exposed since {@link Intent#ACTION_BOOT_COMPLETED} is not sticky.
	 * If keyguard crashes sometime after boot, then it will never receive this
	 * broadcast and hence not handle the event. This method is ultimately
	 * called by PhoneWindowManager in this case.
	 */
	public void dispatchBootCompleted() {
		mHandler.sendEmptyMessage(MSG_BOOT_COMPLETED);
	}

	/**
	 * Handle {@link #MSG_BOOT_COMPLETED}
	 */
	protected void handleBootCompleted() {
		if (mBootCompleted)
			return;
		mBootCompleted = true;
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onBootCompleted();
			}
		}
	}

	/**
	 * We need to store this state in the KeyguardUpdateMonitor since this class
	 * will not be destroyed.
	 */
	public boolean hasBootCompleted() {
		return mBootCompleted;
	}

	/**
	 * Handle {@link #MSG_DEVICE_PROVISIONED}
	 */
	protected void handleDeviceProvisioned() {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onDeviceProvisioned();
			}
		}
		if (mDeviceProvisionedObserver != null) {
			// We don't need the observer anymore...
			mContext.getContentResolver().unregisterContentObserver(mDeviceProvisionedObserver);
			mDeviceProvisionedObserver = null;
		}
	}

	/**
	 * Handle {@link #MSG_PHONE_STATE_CHANGED}
	 */
	protected void handlePhoneStateChanged(String newState) {
		if (DEBUG)
			Log.d(TAG, "handlePhoneStateChanged(" + newState + ")");
		if (TelephonyManager.EXTRA_STATE_IDLE.equals(newState)) {
			mPhoneState = TelephonyManager.CALL_STATE_IDLE;
		} else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(newState)) {
			mPhoneState = TelephonyManager.CALL_STATE_OFFHOOK;
		} else if (TelephonyManager.EXTRA_STATE_RINGING.equals(newState)) {
			mPhoneState = TelephonyManager.CALL_STATE_RINGING;
		}
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onPhoneStateChanged(mPhoneState);
			}
		}
	}

	/**
	 * Handle {@link #MSG_RINGER_MODE_CHANGED}
	 */
	protected void handleRingerModeChange(int mode) {
		if (DEBUG)
			Log.d(TAG, "handleRingerModeChange(" + mode + ")");
		mRingMode = mode;
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onRingerModeChanged(mode);
			}
		}
	}

	/**
	 * Handle {@link #MSG_TIME_UPDATE}
	 */
	private void handleTimeUpdate() {
		if (DEBUG)
			Log.d(TAG, "handleTimeUpdate");
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onTimeChanged();
			}
		}
	}

	/**
	 * Handle {@link #MSG_BATTERY_UPDATE}
	 */
	private void handleBatteryUpdate(BatteryStatus status) {
		if (DEBUG)
			Log.d(TAG, "handleBatteryUpdate");
		final boolean batteryUpdateInteresting = isBatteryUpdateInteresting(mBatteryStatus, status);
		mBatteryStatus = status;
		if (batteryUpdateInteresting) {
			for (int i = 0; i < mCallbacks.size(); i++) {
				KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
				if (cb != null) {
					cb.onRefreshBatteryInfo(status);
				}
			}
		}
	}

	/**
	 * Handle {@link #MSG_SIM_STATE_CHANGE}
	 */
	private void handleSimStateChange(int subId, int slotId, State state) {

		if (DEBUG_SIM_STATES) {
			Log.d(TAG, "handleSimStateChange(subId=" + subId + ", slotId=" + slotId + ", state=" + state + ")");
		}

		if (!SubscriptionManager.isValidSubscriptionId(subId)) {
			Log.w(TAG, "invalid subId in handleSimStateChange()");
			return;
		}

		SimData data = mSimDatas.get(subId);
		final boolean changed;
		if (data == null) {
			data = new SimData(state, slotId, subId);
			mSimDatas.put(subId, data);
			changed = true; // no data yet; force update
		} else {
			changed = (data.simState != state || data.subId != subId || data.slotId != slotId);
			data.simState = state;
			data.subId = subId;
			data.slotId = slotId;
		}
		if (changed && state != State.UNKNOWN) {
			for (int i = 0; i < mCallbacks.size(); i++) {
				KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
				if (cb != null) {
					cb.onSimStateChanged(subId, slotId, state);
				}
			}
		}
	}

	/**
	 * Handle {@link #MSG_LOCALE_CHANGED}
	 */
	private void handleLocaleChanged() {
		for (int j = 0; j < mCallbacks.size(); j++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(j).get();
			if (cb != null) {
				cb.onRefreshCarrierInfo();
			}
		}
	}

	/**
	 * Handle {@link #MSG_SERVICE_STATE_CHANGE}
	 */
	private void handleServiceStateChange(int subId, ServiceState serviceState) {
		if (DEBUG) {
			Log.d(TAG, "handleServiceStateChange(subId=" + subId + ", serviceState=" + serviceState);
		}

		/// M modify for MTK
        int phoneId = SubscriptionManager.getPhoneId(subId);

		if (!SubscriptionManager.isValidSubscriptionId(subId)) {
			Log.w(TAG, "invalid subId in handleServiceStateChange()");
//			mServiceStatesForAbsent.put(subId, serviceState);
			/// M modify for MTK
            mServiceStatesForAbsent.put(phoneId, serviceState);
			return;
		}else{
			/// M modify for MTK
            Log.v(TAG, "YLH: valid subId in handleServiceStateChange() remove phoneId=" + phoneId);
            mServiceStatesForAbsent.remove(phoneId);
		}

		mServiceStates.put(subId, serviceState);

		for (int j = 0; j < mCallbacks.size(); j++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(j).get();
			if (cb != null) {
				cb.onRefreshCarrierInfo();
			}
		}
	}

	/**
	 * Notifies that the visibility state of Keyguard has changed.
	 *
	 * <p>
	 * Needs to be called from the main thread.
	 */
	public void onKeyguardVisibilityChanged(boolean showing) {
		if (DEBUG)
			Log.d(TAG, "onKeyguardVisibilityChanged(" + showing + ")");
		mKeyguardIsVisible = showing;
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onKeyguardVisibilityChangedRaw(showing);
			}
		}
		if (!showing) {
			mFingerprintAlreadyAuthenticated = false;
		}
		updateFingerprintListeningState();
	}

	/**
	 * Handle {@link #MSG_KEYGUARD_RESET}
	 */
	private void handleKeyguardReset() {
		if (DEBUG)
			Log.d(TAG, "handleKeyguardReset");
		updateFingerprintListeningState();
	}

	/**
	 * Handle {@link #MSG_KEYGUARD_BOUNCER_CHANGED}
	 * 
	 * @see #sendKeyguardBouncerChanged(boolean)
	 */
	private void handleKeyguardBouncerChanged(int bouncer) {
		if (DEBUG)
			Log.d(TAG, "handleKeyguardBouncerChanged(" + bouncer + ")");
		boolean isBouncer = (bouncer == 1);
		mBouncer = isBouncer;
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onKeyguardBouncerChanged(isBouncer);
			}
		}
		updateFingerprintListeningState();
	}

	/**
	 * Handle {@link #MSG_REPORT_EMERGENCY_CALL_ACTION}
	 */
	private void handleReportEmergencyCallAction() {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onEmergencyCallAction();
			}
		}
	}

	private static boolean isBatteryUpdateInteresting(BatteryStatus old, BatteryStatus current) {
		final boolean nowPluggedIn = current.isPluggedIn();
		final boolean wasPluggedIn = old.isPluggedIn();
		final boolean stateChangedWhilePluggedIn = wasPluggedIn == true && nowPluggedIn == true
				&& (old.status != current.status);

		// change in plug state is always interesting
		if (wasPluggedIn != nowPluggedIn || stateChangedWhilePluggedIn) {
			return true;
		}

		// change in battery level while plugged in
		if (nowPluggedIn && old.level != current.level) {
			return true;
		}

		// change where battery needs charging
		if (!nowPluggedIn && current.isBatteryLow() && current.level != old.level) {
			return true;
		}

		// change in charging current while plugged in
		if (nowPluggedIn && current.maxChargingWattage != old.maxChargingWattage) {
			return true;
		}

		return false;
	}

	/**
	 * Remove the given observer's callback.
	 *
	 * @param callback
	 *            The callback to remove
	 */
	public void removeCallback(KeyguardUpdateMonitorCallback callback) {
		if (DEBUG)
			Log.v(TAG, "*** unregister callback for " + callback);
		for (int i = mCallbacks.size() - 1; i >= 0; i--) {
			if (mCallbacks.get(i).get() == callback) {
				mCallbacks.remove(i);
			}
		}
	}

	/**
	 * Register to receive notifications about general keyguard information (see
	 * {@link InfoCallback}.
	 * 
	 * @param callback
	 *            The callback to register
	 */
	public void registerCallback(KeyguardUpdateMonitorCallback callback) {
		if (DEBUG)
			Log.v(TAG, "*** register callback for " + callback);
		// Prevent adding duplicate callbacks
		for (int i = 0; i < mCallbacks.size(); i++) {
			if (mCallbacks.get(i).get() == callback) {
				if (DEBUG)
					Log.e(TAG, "Object tried to add another callback", new Exception("Called by"));
				return;
			}
		}
		mCallbacks.add(new WeakReference<KeyguardUpdateMonitorCallback>(callback));
		removeCallback(null); // remove unused references
		sendUpdates(callback);
	}

	private void sendUpdates(KeyguardUpdateMonitorCallback callback) {
		// Notify listener of the current state
		callback.onRefreshBatteryInfo(mBatteryStatus);
		callback.onTimeChanged();
		callback.onRingerModeChanged(mRingMode);
		callback.onPhoneStateChanged(mPhoneState);
		callback.onRefreshCarrierInfo();
		callback.onClockVisibilityChanged();
		for (Entry<Integer, SimData> data : mSimDatas.entrySet()) {
			final SimData state = data.getValue();
			callback.onSimStateChanged(state.subId, state.slotId, state.simState);
		}
		// add by mengludong, 2015.12.01
		callback.onRoamingStateChange(mIsNetWorkRoaming);
		callback.onFingerprintRunningStateChanged(isFingerprintDetectionRunning());
		callback.onWeatherChanged(mWeatherData);
		callback.onAntitheftStateChanged(mIsAntitheftPattern);
		if (!mIsAntitheftPattern) {
			callback.onHallStateChanged(mHallState);
		}
		callback.onUpdateKeyguardTextColor(mIsDarkColor);
		for (String pkg : mShowNumMap.keySet()) {
			callback.onShowNumChanged(pkg, mShowNumMap.get(pkg));
		}
		// add end

		SharedPreferences preferences = mContext.getSharedPreferences("KeyguardPreferences", Context.MODE_PRIVATE);
		String path = preferences.getString("cool_show_theme_path", null);
		boolean isVlife = preferences.getBoolean("isVlife", false);
		if (preferences.getBoolean("isZooking", false)) {
			callback.onCoolShowThemeChanged(path, true/* reload */, isVlife);
			if (DEBUG)
				Log.v(TAG, "Send cool show lock theme path.");
		}
	}

	public void sendKeyguardReset() {
		mHandler.obtainMessage(MSG_KEYGUARD_RESET).sendToTarget();
	}
	
	public void sendKeyguardShowingChanged(boolean showing) {
        if (DEBUG) Log.d(TAG, "sendKeyguardShowingChanged(" + showing + ")");
        Message message = mHandler.obtainMessage(MSG_KEYGUARD_SHOWING_CHANGED);
        message.arg1 = showing ? 1 : 0;
        message.sendToTarget();
    }

	/**
	 * @see #handleKeyguardBouncerChanged(int)
	 */
	public void sendKeyguardBouncerChanged(boolean showingBouncer) {
		if (DEBUG)
			Log.d(TAG, "sendKeyguardBouncerChanged(" + showingBouncer + ")");
		Message message = mHandler.obtainMessage(MSG_KEYGUARD_BOUNCER_CHANGED);
		message.arg1 = showingBouncer ? 1 : 0;
		message.sendToTarget();
	}

	/**
	 * Report that the user successfully entered the SIM PIN or PUK/SIM PIN so
	 * we have the information earlier than waiting for the intent broadcast
	 * from the telephony code.
	 *
	 * NOTE: Because handleSimStateChange() invokes callbacks immediately
	 * without going through mHandler, this *must* be called from the UI thread.
	 */
	public void reportSimUnlocked(int subId) {
		if (DEBUG_SIM_STATES)
			Log.v(TAG, "reportSimUnlocked(subId=" + subId + ")");
		int slotId = SubscriptionManager.getSlotId(subId);
		handleSimStateChange(subId, slotId, State.READY);
	}

	/**
	 * Report that the emergency call button has been pressed and the emergency
	 * dialer is about to be displayed.
	 *
	 * @param bypassHandler
	 *            runs immediately.
	 *
	 *            NOTE: Must be called from UI thread if bypassHandler == true.
	 */
	public void reportEmergencyCallAction(boolean bypassHandler) {
		if (!bypassHandler) {
			mHandler.obtainMessage(MSG_REPORT_EMERGENCY_CALL_ACTION).sendToTarget();
		} else {
			handleReportEmergencyCallAction();
		}
	}

	/**
	 * @return Whether the device is provisioned (whether they have gone through
	 *         the setup wizard)
	 */
	public boolean isDeviceProvisioned() {
		return mDeviceProvisioned;
	}

	public void clearFailedUnlockAttempts() {
		mFailedAttempts.delete(sCurrentUser);
	}

	public int getFailedUnlockAttempts(int userId) {
		return mFailedAttempts.get(userId, 0);
	}

	public void reportFailedStrongAuthUnlockAttempt(int userId) {
		mFailedAttempts.put(userId, getFailedUnlockAttempts(userId) + 1);
	}

	public void clearFingerprintRecognized() {
		mUserFingerprintAuthenticated.clear();
	}

	public boolean isSimPinVoiceSecure() {
		// TODO: only count SIMs that handle voice
		return isSimPinSecure();
	}

	public boolean isSimPinSecure() {
		// True if any SIM is pin secure
		for (SubscriptionInfo info : getSubscriptionInfo(false /* forceReload */)) {
			if (isSimPinSecure(getSimState(info.getSubscriptionId())))
				return true;
		}
		return false;
	}

	public State getSimState(int subId) {
		if (mSimDatas.containsKey(subId)) {
			return mSimDatas.get(subId).simState;
		} else {
			return State.UNKNOWN;
		}
	}

	/**
	 * @return true if and only if the state has changed for the specified
	 *         {@code slotId}
	 */
	private boolean refreshSimState(int subId, int slotId) {

		// This is awful. It exists because there are two APIs for getting the
		// SIM status
		// that don't return the complete set of values and have different
		// types. In Keyguard we
		// need IccCardConstants, but TelephonyManager would only give us
		// TelephonyManager.SIM_STATE*, so we retrieve it manually.
		final TelephonyManager tele = TelephonyManager.from(mContext);
		int simState = tele.getSimState(slotId);
		State state;
		try {
			state = State.intToState(simState);
		} catch (IllegalArgumentException ex) {
			Log.w(TAG, "Unknown sim state: " + simState);
			state = State.UNKNOWN;
		}
		SimData data = mSimDatas.get(subId);
		final boolean changed;
		if (data == null) {
			data = new SimData(state, slotId, subId);
			mSimDatas.put(subId, data);
			changed = true; // no data yet; force update
		} else {
			changed = data.simState != state;
			data.simState = state;
		}
		return changed;
	}

	public static boolean isSimPinSecure(IccCardConstants.State state) {
		final IccCardConstants.State simState = state;
		return (simState == IccCardConstants.State.PIN_REQUIRED || simState == IccCardConstants.State.PUK_REQUIRED
				|| simState == IccCardConstants.State.PERM_DISABLED);
	}

	public DisplayClientState getCachedDisplayClientState() {
		return mDisplayClientState;
	}

	// TODO: use these callbacks elsewhere in place of the existing
	// notifyScreen*()
	// (KeyguardViewMediator, KeyguardHostView)
	public void dispatchStartedWakingUp() {
		synchronized (this) {
			mDeviceInteractive = true;
		}
		mHandler.sendEmptyMessage(MSG_STARTED_WAKING_UP);
	}

	public void dispatchStartedGoingToSleep(int why) {
		mHandler.sendMessage(mHandler.obtainMessage(MSG_STARTED_GOING_TO_SLEEP, why, 0));
	}

	public void dispatchFinishedGoingToSleep(int why) {
		synchronized (this) {
			mDeviceInteractive = false;
		}
		mHandler.sendMessage(mHandler.obtainMessage(MSG_FINISHED_GOING_TO_SLEEP, why, 0));
	}

	public void dispatchScreenTurnedOn() {
		synchronized (this) {
			mScreenOn = true;
		}
		mHandler.sendEmptyMessage(MSG_SCREEN_TURNED_ON);
	}

	public void dispatchScreenTurnedOff() {
		synchronized (this) {
			mScreenOn = false;
		}
		mHandler.sendEmptyMessage(MSG_SCREEN_TURNED_OFF);
	}

	public boolean isDeviceInteractive() {
		return mDeviceInteractive;
	}

	public boolean isGoingToSleep() {
		return mGoingToSleep;
	}

	/**
	 * Find the next SubscriptionId for a SIM in the given state, favoring lower
	 * slot numbers first.
	 * 
	 * @param state
	 * @return subid or {@link SubscriptionManager#INVALID_SUBSCRIPTION_ID} if
	 *         none found
	 */
	public int getNextSubIdForState(State state) {
		List<SubscriptionInfo> list = getSubscriptionInfo(false /* forceReload */);
		int resultId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
		int bestSlotId = Integer.MAX_VALUE; // Favor lowest slot first
		for (int i = 0; i < list.size(); i++) {
			final SubscriptionInfo info = list.get(i);
			final int id = info.getSubscriptionId();
			int slotId = SubscriptionManager.getSlotId(id);
			if (state == getSimState(id) && bestSlotId > slotId) {
				resultId = id;
				bestSlotId = slotId;
			}
		}
		return resultId;
	}

	public SubscriptionInfo getSubscriptionInfoForSubId(int subId) {
		List<SubscriptionInfo> list = getSubscriptionInfo(false /* forceReload */);
		for (int i = 0; i < list.size(); i++) {
			SubscriptionInfo info = list.get(i);
			if (subId == info.getSubscriptionId())
				return info;
		}
		return null; // not found
	}

	public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
		pw.println("KeyguardUpdateMonitor state:");
		pw.println("  SIM States:");
		for (SimData data : mSimDatas.values()) {
			pw.println("    " + data.toString());
		}
		pw.println("  Subs:");
		if (mSubscriptionInfo != null) {
			for (int i = 0; i < mSubscriptionInfo.size(); i++) {
				pw.println("    " + mSubscriptionInfo.get(i));
			}
		}
		pw.println("  Service states:");
		for (int subId : mServiceStates.keySet()) {
			pw.println("    " + subId + "=" + mServiceStates.get(subId));
		}
		if (mFpm != null && mFpm.isHardwareDetected()) {
			final int userId = ActivityManager.getCurrentUser();
			final int strongAuthFlags = mStrongAuthTracker.getStrongAuthForUser(userId);
			pw.println("  Fingerprint state (user=" + userId + ")");
			pw.println("    allowed=" + isUnlockingWithFingerprintAllowed());
			pw.println("    auth'd=" + mUserFingerprintAuthenticated.get(userId));
			pw.println("    authSinceBoot=" + getStrongAuthTracker().hasUserAuthenticatedSinceBoot());
			pw.println("    disabled(DPM)=" + isFingerprintDisabled(userId));
			pw.println("    possible=" + isUnlockWithFingerprintPossible(userId));
			pw.println("    strongAuthFlags=" + Integer.toHexString(strongAuthFlags));
			pw.println("    timedout=" + hasFingerprintUnlockTimedOut(userId));
			pw.println("    trustManaged=" + getUserTrustIsManaged(userId));
		}
	}

	/**
	 * broadcast hall sensor state change (for cover mode) Handle
	 * {@link #MSG_HALL_STATE_CHANGED}
	 * 
	 * @param state
	 * @add by mengludong 2015.11.09
	 */
	private void handleHallStateChange(int state) {
		mHallState = state;
		if (mIsAntitheftPattern) {
			if (DEBUG)
				Log.v(TAG, "isFangdaoPattern ignore cover state");
			return;
		}
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onHallStateChanged(state);
			}
		}
	}

	/**
	 * Handle {@link #MSG_WEATHER_DATA_CHANGED}
	 * 
	 * @param weatherData
	 * @add by mengludong 2015.11.09
	 */
	private void handleWeatherDataChange(WeatherData weatherData) {
		if (weatherData != null && !weatherData.equals(mWeatherData)) {
			mWeatherData = weatherData;
			for (int i = 0; i < mCallbacks.size(); i++) {
				KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
				if (cb != null) {
					cb.onWeatherChanged(weatherData);
				}
			}
		}
	}

	/**
	 * Parse weather data form intent
	 * 
	 * @param intent
	 * @return weather data
	 * @add by mengludong 2015.11.09
	 */
	protected WeatherData getWeatherDataForm(Intent intent) {
		if (intent == null) {
			return null;
		}
		return new WeatherData(intent.getStringExtra("state"), intent.getStringExtra("city"),
				intent.getStringExtra("lTemp"), intent.getStringExtra("hTemp"), intent.getIntExtra("weatherType", -999),
				intent.getStringExtra("weatherDes"), intent.getStringExtra("cTemp"), intent.getStringExtra("cityid"));
	}

	/**
	 * Record weather data
	 * 
	 * @add by mengludong 2015.11.09
	 */
	public static class WeatherData {
		public String mState;
		public String mCity;
		public String mLowTemperature;
		public String mHighTemperature;
		public int mWeatherType;
		public String mWeatherDes;
		public String mCurrentTemperature;
		public String mCityId;

		public WeatherData(String state, String city, String lowTemp, String highTemp, int weatherType,
				String weatherDes, String curTemp, String cityId) {
			mState = state == null || state.isEmpty() ? "0" : state;
			mCity = city == null ? "" : city;
			mLowTemperature = lowTemp == null || lowTemp.isEmpty() ? "0" : lowTemp;
			mHighTemperature = highTemp == null || highTemp.isEmpty() ? "0" : highTemp;
			mWeatherDes = weatherDes == null ? "" : weatherDes;
			mWeatherType = weatherType;
			mCurrentTemperature = curTemp == null || curTemp.isEmpty() ? "0" : curTemp;
			mCityId = cityId == null || cityId.isEmpty() ? "-999" : cityId;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null) {
				return false;
			}
			if (!(other instanceof WeatherData)) {
				return false;
			}
			final WeatherData otherWeather = (WeatherData) other;
			return mState.equals(otherWeather.mState) && mCity.equals(otherWeather.mCity)
					&& mLowTemperature.equals(otherWeather.mLowTemperature)
					&& mHighTemperature.equals(otherWeather.mHighTemperature)
					&& mWeatherDes.equals(otherWeather.mWeatherDes) && mWeatherType == otherWeather.mWeatherType
					&& mCurrentTemperature.equals(otherWeather.mCurrentTemperature)
					&& mCityId.equals(otherWeather.mCityId);
		}

		@Override
		public int hashCode() {
			return (mCity.hashCode() + mCurrentTemperature.hashCode()) * mWeatherType;
		}

		@Override
		public String toString() {
			return mState + ", " + mCity + ", " + mLowTemperature + ", " + mHighTemperature + ", " + mWeatherType + ", "
					+ mWeatherDes + ", " + mCurrentTemperature + ", " + mCityId;
		}
	}

	/**
	 * Antitheft screen showing state changed
	 * 
	 * @param showing
	 * @add by mengludong 2015.11.11
	 */
	public void sendAntitheftStateChanged(boolean showing) {
		if (DEBUG)
			Log.v(TAG, "fangdao state changed showing ? " + showing);
		mHandler.sendMessage(mHandler.obtainMessage(MSG_ANTITHEFT_STATE_CHANGED, showing));
	}

	/**
	 * Handle {@link MSG_ANTITHEFT_STATE_CHANGED} message
	 * 
	 * @param showing
	 * @add by mengludong 2015.11.11
	 */
	protected void handleAntitheftStateChange(boolean showing) {
		mIsAntitheftPattern = showing;
		if (!showing) {
			handleHallStateChange(mHallState);
		}
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onAntitheftStateChanged(showing);
			}
		}
	}

	// add by mengludong, return Antitheft Pattern state
	public boolean isAntitheftPattern() {
		return mIsAntitheftPattern;
	}
	// add end

	public boolean isScreenOn() {
		return mScreenOn;
	}

	// 2015.11.16 add by mengludong, record keyguard occluded state
	public void setOccluded(boolean occluded) {
		mKeyguardOccluded = occluded;
		if (!occluded) {
//			stopListeningForFingerprint();
			startListeningForFingerprint();
		}
	}

	public boolean isOccluded() {
		return mKeyguardOccluded;
	}
	// add end
	
	public boolean isKeyguardShowing(){
		return mKeyguardShowing;
	}

	// 2015.12.01 add by mengludong to show double clock view when net work
	// roaming
	private boolean isNetWorkRoaming() {
		boolean result = false;
		TelephonyManager tm = TelephonyManager.getDefault();
		int phoneCount = tm.getPhoneCount();
		for (int i = 0; i < phoneCount; i++) {
			int[] subId = SubscriptionManager.getSubId(i);
			if (tm.getNetworkOperatorForPhone(i).startsWith("460")) {
				Log.d(TAG, "isNetWorkRoaming() operator starts with 460, return false. phone id = " + i);
				return false;// needn't show double clock view in China
			}
			if (subId != null && tm.isNetworkRoaming(subId[0])) {
				Log.d(TAG, "isNetWorkRoaming() subId = " + subId[0] + " is roaming");
				result = true;
			}
		}
		return result;
	}
	// add end

	/**
	 * Handle {@link MSG_NETWORK_ROAMING} message
	 * 
	 * @add by mengludong 2015.12.01
	 */
	private void handleRoamingStateChange() {
		Log.v(TAG, "onRoamingStateChange() mIsNetWorkRoaming = " + mIsNetWorkRoaming);
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onRoamingStateChange(mIsNetWorkRoaming);
			}
		}
	}

	/**
	 * Handle {@link #MSG_COOLSHOW_THEME_CHANGED}
	 * 
	 * @param isVlifeTheme
	 * @param path
	 */
	private void handleCoolShowThemeChange(boolean isVlifeTheme, String path) {
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onCoolShowThemeChanged(path, false/* reload */, isVlifeTheme);
			}
		}
		SharedPreferences preferences = mContext.getSharedPreferences("KeyguardPreferences", Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString("cool_show_theme_path", path);
		editor.putBoolean("isZooking", path != null);
		editor.putBoolean("isVlife", isVlifeTheme);
		editor.commit();
	}

	/**
	 * judge the product support oversea or not
	 * 
	 * @return
	 */
	public static boolean isSupportOversea() {
		boolean supportOversea = false;
		try {
			String featureString = "";
			Class<?> featureClass = Class.forName("com.yulong.android.feature.FeatureString");
			Log.d(TAG, "featureClass is getted");
			Object featureObject = featureClass.newInstance();
			Field featureField = featureClass.getDeclaredField("IS_SUPPORT_OVERSEA");
			featureField.setAccessible(true);
			featureString = featureField.get(featureObject).toString();
			Log.d(TAG, "wanghuazhi1" + featureString);
			Class<?> ConfigClass = Class.forName("com.yulong.android.feature.FeatureConfig");
			Method method = ConfigClass.getMethod("getBooleanValue", String.class);
			Object configoObject = ConfigClass.newInstance();
			supportOversea = (Boolean) method.invoke(configoObject, featureString);
		} catch (Exception e) {
			Log.v(TAG, "cann't find field IS_SUPPORT_OVERSEA: " + e);
		}
		if (DEBUG)
			Log.v(TAG, "IS_SUPPORT_OVERSEA: " + supportOversea);

		return supportOversea;
	}

	public synchronized void showToastMsg(String msg) {
		if (mToast != null) {
			mToast.setText(msg);
		} else {
			mToast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
			//===modify for ty compile error
//			mToast.setLayoutType(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
		}
		mToast.show();
	}

	public synchronized void cancelToast() {
		if (mToast != null) {
			mToast.cancel();
			mToast = null;
		}
	}

	public static boolean isProductMode() {
		String defaultRunMode = SystemProperties.get("persist.yulong.comm.runmode", "0000");
		if (defaultRunMode.length() > 1) {
			if (defaultRunMode.substring(0, 1).equalsIgnoreCase("0")) {
				if (DEBUG)
					Log.v(TAG, "is product mode.");
				return true;
			}
		}
		if (DEBUG)
			Log.v(TAG, "not product mode.");
		return false;
	}

	/** MTBF test string key */
	private static final String KEY_NETWORK_SETTING = "persist.yulong.operators.mode";
	private static final int WHOLE_NETWORK_MODE = 1;
	private static final int C_NETWORK_MODE = 2;
	private static final int T_NETWORK_MODE = 3;
	private static final int W_NETWORK_MODE = 4;

	private static final String KEY_WHOLE_NETWORK_SETTING = "persist.yulong.allmode.operator";
	private static final int WHOLE_NETWORK_PUBLIC = 0;
	private static final int WHOLE_NETWORK_CT = 1;
	private static final int WHOLE_NETWORK_CMCC = 2;
	private static final int WHOLE_NETWORK_CU = 3;

	/**
	 * get yulong carrier settings
	 * 
	 * @return carrier code, 0: whole network support, 1:China Telecom, 2:China
	 *         Mobile, 3:China Unicom
	 */
	public static int getYLNetWorkSetting() {
		int carrierCode = -1;
		try {
			carrierCode = SystemProperties.getInt(KEY_NETWORK_SETTING, -1);
			if (carrierCode == WHOLE_NETWORK_MODE) {
				carrierCode = SystemProperties.getInt(KEY_WHOLE_NETWORK_SETTING, -1);
			} else {
				carrierCode--;
			}
		} catch (Exception e) {
			Log.v(TAG, "Failed to get system properties, e: " + e);
		}
		if (DEBUG)
			Log.v(TAG, "getYLNetWorkSetting() carrierCode = " + carrierCode);
		return carrierCode;
	}

	public HashMap<Integer, ServiceState> getServiceStates() {
		return mServiceStates;
	}

	public void onKeyguardBackgroundColorChanged(int keyguardTextColor, int wallpaperKeyguardAreaBrightnes) {
		Log.d(TAG, "onKeyguardBackgroundColorChanged() brightness: " + wallpaperKeyguardAreaBrightnes
				+ ", mIsDarkColor = " + mIsDarkColor);
		Message message = mHandler.obtainMessage(MSG_KEYGUARD_BACKGROUND_COLOR_CHANGED);
		message.arg1 = wallpaperKeyguardAreaBrightnes > BRIGHTNESS_THRESHOLD ? 1 : 0;
		message.sendToTarget();
	}

	protected void handleKeyguardBackgroundColorChanged(boolean darkerColor) {
		if (mIsDarkColor == darkerColor) {
			return;
		}
		mIsDarkColor = darkerColor;
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onUpdateKeyguardTextColor(darkerColor);
			}
		}
	}

	protected void handleShowNumChanged(int num, String pkg) {
		if (!pkg.equals(PHONE_NUM_PACKAGE) && !pkg.equals(MSG_NUM_PACKAGE)) {
			return;
		}
		mShowNumMap.put(pkg, num);
		for (int i = 0; i < mCallbacks.size(); i++) {
			KeyguardUpdateMonitorCallback cb = mCallbacks.get(i).get();
			if (cb != null) {
				cb.onShowNumChanged(pkg, num);
			}
		}
	}

	public static boolean isDarkerColor() {
		return mIsDarkColor;
	}

	public boolean isAbsentSlotEmergencyCallCapable() {
		Log.v("EmergencyButton", "isAbsentSlotEmergencyCallCapable()");
		for (int subId : mServiceStatesForAbsent.keySet()) {
			if (mServiceStatesForAbsent.get(subId).isEmergencyOnly()) {
				Log.v("EmergencyButton", "isAbsentSlotEmergencyCallCapable() subId = " + subId + " is emergency only.");
				return true;
			}
		}
		return false;
	}

	/**
	 * Handle {@link #MSG_KEYGUARD_SHOWING_CHANGED}
	 */
	private void handleKeyguardShowingChanged(int showing) {
		if (DEBUG)
			Log.d(TAG, "handleKeyguardVisibilityChanged(" + showing + ")");
		boolean isShowing = (showing == 1);
		mKeyguardShowing = isShowing;
		updateFingerprintListeningState();
	}

	//add by huazhi for CTS test
	public static boolean isCTSInstalled() {
		return mIsCTSIntalled;
	}
}