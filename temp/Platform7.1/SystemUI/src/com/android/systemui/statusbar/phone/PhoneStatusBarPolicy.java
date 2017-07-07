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

package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.StatusBarManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telecom.TelecomManager;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothController.Callback;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastController.CastDevice;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.UserInfoController;
import android.app.ActivityManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.UserManager;
import java.util.List;

/**
 * This class contains all of the policy about which icons are installed in the status
 * bar at boot time.  It goes through the normal API for icons, even though it probably
 * strictly doesn't need to.
 */
public class PhoneStatusBarPolicy implements Callback, RotationLockController.RotationLockControllerCallback, DataSaverController.Listener {
    private static final String TAG = "PhoneStatusBarPolicy";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

   
    private final String mSlotCast;
    private final String mSlotHotspot;
    private final String mSlotBluetooth;
    private final String mSlotTty;
    private final String mSlotZen;
    private final String mSlotVolume;
    private final String mSlotAlarmClock;
    private final String mSlotManagedProfile;
    private final String mSlotRotate;
    private final String mSlotHeadset;
    private final String mSlotDataSaver;

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private final CastController mCast;
    private final HotspotController mHotspot;
    private final AlarmManager mAlarmManager;
    private final UserInfoController mUserInfoController;
    private final UserManager mUserManager;
    private final StatusBarIconController mIconController;
    private final RotationLockController mRotationLockController;
    private final DataSaverController mDataSaver;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;

    // Assume it's all good unless we hear otherwise.  We don't always seem
    // to get broadcasts that it *is* there.
    IccCardConstants.State mSimState = IccCardConstants.State.READY;

    private boolean mZenVisible;
    private boolean mVolumeVisible;
    private boolean mCurrentUserSetup;

    private int mZen;
    private static final String SLOT_UNDISTURB = "undisturb";
    private static final String SLOT_ALARM_CLOCK = "alarm_clock";
    private boolean mManagedProfileFocused = false;
    private boolean mManagedProfileIconVisible = false;
    private boolean mManagedProfileInQuietMode = false;

    private BluetoothController mBluetooth;

    private UndisturbModeObserver mUndisturbModeObserver;
    private final StatusBarManager mService;
    
    private final class UndisturbModeObserver extends ContentObserver{

        public UndisturbModeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateUndisturb();
            super.onChange(selfChange);
        }

        public void startObserving(){
            CurrentUserTracker.registerContentObserverGlobal("zen_mode", false, this);
            updateUndisturb();
        }

    }
    
    public PhoneStatusBarPolicy(Context context, StatusBarIconController iconController,
            CastController cast, HotspotController hotspot, UserInfoController userInfoController,
            BluetoothController bluetooth, RotationLockController rotationLockController,
            DataSaverController dataSaver) {
        mContext = context;
        mIconController = iconController;
        mCast = cast;
        mHotspot = hotspot;
        mBluetooth = bluetooth;
        mBluetooth.addStateChangedCallback(this);
        mService = (StatusBarManager)context.getSystemService(Context.STATUS_BAR_SERVICE);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mUserInfoController = userInfoController;
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mRotationLockController = rotationLockController;
        mDataSaver = dataSaver;

        mSlotCast = context.getString(com.android.internal.R.string.status_bar_cast);
        mSlotHotspot = context.getString(com.android.internal.R.string.status_bar_hotspot);
        mSlotBluetooth = context.getString(com.android.internal.R.string.status_bar_bluetooth);
        mSlotTty = context.getString(com.android.internal.R.string.status_bar_tty);
        mSlotZen = context.getString(com.android.internal.R.string.status_bar_zen);
        mSlotVolume = context.getString(com.android.internal.R.string.status_bar_volume);
        mSlotAlarmClock = context.getString(com.android.internal.R.string.status_bar_alarm_clock);
        mSlotManagedProfile = context.getString(
                com.android.internal.R.string.status_bar_managed_profile);
        mSlotRotate = context.getString(com.android.internal.R.string.status_bar_rotate);
        mSlotHeadset = context.getString(com.android.internal.R.string.status_bar_headset);
        mSlotDataSaver = context.getString(com.android.internal.R.string.status_bar_data_saver);

        mRotationLockController.addRotationLockControllerCallback(this);

        // listen for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_ALARM_CHANGED);
        filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelecomManager.ACTION_CURRENT_TTY_MODE_CHANGED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);

      //UNDISTURB(VIPLIST_NO_DISTURB)
        Handler handler = new Handler();
        mUndisturbModeObserver = new UndisturbModeObserver(handler);
        mUndisturbModeObserver.startObserving();
        try{
            mService.setIcon(SLOT_UNDISTURB, R.drawable.stat_sys_not_interruptions_on, 0, null);
        }catch(Exception e){
            LogHelper.se(TAG, e.toString());
        }
        updateUndisturb();
        
        // listen for user / profile change.
      //ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(mUserSwitchListener, TAG);
        } catch (RemoteException e) {
            // Ignore
        }

        // TTY status
        mIconController.setIcon(mSlotTty,  R.drawable.stat_sys_tty_mode, null);
        mIconController.setIconVisibility(mSlotTty, false);

        // bluetooth status
        updateBluetooth();

        // Alarm clock
 //       mIconController.setIcon(mSlotAlarmClock, R.drawable.stat_sys_alarm, null);
 //       mIconController.setIconVisibility(mSlotAlarmClock, false);

        mIconController.setIcon("alarm_clock", R.drawable.stat_sys_alarm, null);
        String enable = Settings.System.getString( mContext.getContentResolver(), "next_alarm_formatted");
        if( enable!=null && !enable.isEmpty()){
        	mIconController.setIconVisibility("alarm_clock", true);
        } else {
        	mIconController.setIconVisibility("alarm_clock", false);
        }
        // zen
//        mIconController.setIcon(mSlotZen, R.drawable.stat_sys_zen_important, null);
//        mIconController.setIconVisibility(mSlotZen, false);

        // volume
        mIconController.setIcon(mSlotVolume, R.drawable.stat_sys_ringer_vibrate, null);
        mIconController.setIconVisibility(mSlotVolume, false);
        updateVolumeZen();

        // cast
        mIconController.setIcon(mSlotCast, R.drawable.stat_sys_cast, null);
        mIconController.setIconVisibility(mSlotCast, false);
        mCast.addCallback(mCastCallback);

        // hotspot
//        mIconController.setIcon(mSlotHotspot, R.drawable.stat_sys_hotspot,
//                mContext.getString(R.string.accessibility_status_bar_hotspot));
//        mIconController.setIconVisibility(mSlotHotspot, mHotspot.isHotspotEnabled());
//        mHotspot.addCallback(mHotspotCallback);

        // managed profile
        mIconController.setIcon(mSlotManagedProfile, R.drawable.stat_sys_managed_profile_status,
                mContext.getString(R.string.accessibility_managed_profile));
        mIconController.setIconVisibility(mSlotManagedProfile, mManagedProfileIconVisible);

        // data saver
        mIconController.setIcon(mSlotDataSaver, R.drawable.stat_sys_data_saver,
                context.getString(R.string.accessibility_data_saver_on));
        mIconController.setIconVisibility(mSlotDataSaver, false);
        mDataSaver.addListener(this);
    }

    public void setStatusBarKeyguardViewManager(
            StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public void setZenMode(int zen) {
        mZen = zen;
        updateVolumeZen();
    }
    
    private Boolean mAlarmSet = false;
    private Boolean mHasAlarm = false;
    private final void updateAlarm(Intent intent) {
        boolean alarmSet = intent.getBooleanExtra("alarmSet", false);
        mAlarmSet = alarmSet;
        LogHelper.sv(TAG, "alarm_clock = " + alarmSet);
        if(mHasAlarm && !mAlarmSet){
        	
        }else{
        	mIconController.setIconVisibility("alarm_clock", alarmSet);
        }
    }

//    private void updateAlarm() {
//        final AlarmClockInfo alarm = mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
//        final boolean hasAlarm = alarm != null && alarm.getTriggerTime() > 0;
//        final boolean zenNone = mZen == Global.ZEN_MODE_NO_INTERRUPTIONS;
//        mIconController.setIcon(mSlotAlarmClock, zenNone ? R.drawable.stat_sys_alarm_dim
//                : R.drawable.stat_sys_alarm, null);
//        mIconController.setIconVisibility(mSlotAlarmClock, mCurrentUserSetup && hasAlarm);
//    }
    
    
    private final void updateAlarm() {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
                final List<UserInfo> users = um.getUsers();
                final int currentUserId = ActivityManager.getCurrentUser();
                boolean hasAlarm = false;
                for (UserInfo userInfo : users) {
                	//ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
                    if (/*!userInfo.isHidden() || */(userInfo.id == currentUserId)) {
                        AlarmClockInfo alarm = mAlarmManager.getNextAlarmClock(userInfo.id);
                        if (alarm != null) {
                            hasAlarm = alarm != null && alarm.getTriggerTime() > 0;
                            break;
                        }
                    }
                }
        final boolean zenNone = mZen == Global.ZEN_MODE_NO_INTERRUPTIONS;
        mHasAlarm = mCurrentUserSetup && hasAlarm;
        LogHelper.sv(TAG, "hasAlarm = " + hasAlarm+" zenNone="+zenNone+" setup="+mCurrentUserSetup);
        mIconController.setIcon(SLOT_ALARM_CLOCK, zenNone ? R.drawable.stat_sys_alarm
                : R.drawable.stat_sys_alarm,null);
        if(!mHasAlarm && mAlarmSet){
        	
        }else{
           mIconController.setIconVisibility(SLOT_ALARM_CLOCK, mHasAlarm);
        }
    }

    private final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            mSimState = IccCardConstants.State.ABSENT;
        } else if (IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(stateExtra)) {
            mSimState = IccCardConstants.State.CARD_IO_ERROR;
        } else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
            mSimState = IccCardConstants.State.READY;
        } else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            final String lockedReason =
                    intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
            if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                mSimState = IccCardConstants.State.PIN_REQUIRED;
            } else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                mSimState = IccCardConstants.State.PUK_REQUIRED;
            } else {
                mSimState = IccCardConstants.State.NETWORK_LOCKED;
            }
        } else {
            mSimState = IccCardConstants.State.UNKNOWN;
        }
    }

    private final void updateVolumeZen() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        boolean zenVisible = false;
        int zenIconId = 0;
        String zenDescription = null;

        boolean volumeVisible = false;
        int volumeIconId = 0;
        String volumeDescription = null;

        if (DndTile.isVisible(mContext) || DndTile.isCombinedIcon(mContext)) {
            zenVisible = mZen != Global.ZEN_MODE_OFF;
            zenIconId = mZen == Global.ZEN_MODE_NO_INTERRUPTIONS
                    ? R.drawable.stat_sys_dnd_total_silence : R.drawable.stat_sys_dnd;
            zenDescription = mContext.getString(R.string.quick_settings_dnd_label);
        } else if (mZen == Global.ZEN_MODE_NO_INTERRUPTIONS) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_zen_none;
            zenDescription = mContext.getString(R.string.interruption_level_none);
        } else if (mZen == Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_zen_important;
            zenDescription = mContext.getString(R.string.interruption_level_priority);
        }

        if (DndTile.isVisible(mContext) && !DndTile.isCombinedIcon(mContext)
                && audioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_SILENT) {
            volumeVisible = true;
            volumeIconId = R.drawable.stat_sys_ringer_silent;
            volumeDescription = mContext.getString(R.string.accessibility_ringer_silent);
        } else if (mZen != Global.ZEN_MODE_NO_INTERRUPTIONS && mZen != Global.ZEN_MODE_ALARMS &&
                audioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_VIBRATE) {
            volumeVisible = true;
            volumeIconId = R.drawable.stat_sys_ringer_vibrate;
            volumeDescription = mContext.getString(R.string.accessibility_ringer_vibrate);
        }

//        if (zenVisible) {
//            mIconController.setIcon(mSlotZen, zenIconId, zenDescription);
//        }
//        if (zenVisible != mZenVisible) {
//            mIconController.setIconVisibility(mSlotZen, zenVisible);
//            mZenVisible = zenVisible;
//        }

        if (volumeVisible) {
            mIconController.setIcon(mSlotVolume, volumeIconId, volumeDescription);
        }
        if (volumeVisible != mVolumeVisible) {
            mIconController.setIconVisibility(mSlotVolume, volumeVisible);
            mVolumeVisible = volumeVisible;
        }
        updateAlarm();
    }

    @Override
    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    @Override
    public void onBluetoothStateChange(boolean enabled) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        int iconId = R.drawable.stat_sys_data_bluetooth;
        String contentDescription =
                mContext.getString(R.string.accessibility_quick_settings_bluetooth_on);
        boolean bluetoothEnabled = false;
        if (mBluetooth != null) {
            bluetoothEnabled = mBluetooth.isBluetoothEnabled();
            if (mBluetooth.isBluetoothConnected()) {
                iconId = R.drawable.stat_sys_data_bluetooth_connected;
                contentDescription = mContext.getString(R.string.accessibility_bluetooth_connected);
            }
        }

        mIconController.setIcon(mSlotBluetooth, iconId, contentDescription);
        mIconController.setIconVisibility(mSlotBluetooth, bluetoothEnabled);
    }

    private final void updateUndisturb() {
        boolean flagU = (Settings.Global.getInt(mContext.getContentResolver(),Settings.Global.ZEN_MODE,0) != 0);
        try{
            mService.setIconVisibility(SLOT_UNDISTURB, flagU);
        }catch(Exception e){
            LogHelper.se(TAG, e.toString());
        }
    }

    private final void updateTTY(Intent intent) {
        int currentTtyMode = intent.getIntExtra(TelecomManager.EXTRA_CURRENT_TTY_MODE,
                TelecomManager.TTY_MODE_OFF);
        boolean enabled = currentTtyMode != TelecomManager.TTY_MODE_OFF;

        if (DEBUG) Log.v(TAG, "updateTTY: enabled: " + enabled);

        if (enabled) {
            // TTY is on
            if (DEBUG) Log.v(TAG, "updateTTY: set TTY on");
            mIconController.setIcon(mSlotTty, R.drawable.stat_sys_tty_mode,
                    mContext.getString(R.string.accessibility_tty_enabled));
            mIconController.setIconVisibility(mSlotTty, true);
        } else {
            // TTY is off
            if (DEBUG) Log.v(TAG, "updateTTY: set TTY off");
            mIconController.setIconVisibility(mSlotTty, false);
        }
    }

    private void updateCast() {
        boolean isCasting = false;
        for (CastDevice device : mCast.getCastDevices()) {
            if (device.state == CastDevice.STATE_CONNECTING
                    || device.state == CastDevice.STATE_CONNECTED) {
                isCasting = true;
                break;
            }
        }
        if (DEBUG) Log.v(TAG, "updateCast: isCasting: " + isCasting);
        mHandler.removeCallbacks(mRemoveCastIconRunnable);
        if (isCasting) {
            mIconController.setIcon(mSlotCast, R.drawable.stat_sys_cast,
                    mContext.getString(R.string.accessibility_casting));
            mIconController.setIconVisibility(mSlotCast, true);
        } else {
            // don't turn off the screen-record icon for a few seconds, just to make sure the user
            // has seen it
            if (DEBUG) Log.v(TAG, "updateCast: hiding icon in 3 sec...");
            mHandler.postDelayed(mRemoveCastIconRunnable, 3000);
        }
    }

    private void updateQuietState() {
        mManagedProfileInQuietMode = false;
        int currentUserId = ActivityManager.getCurrentUser();
        for (UserInfo ui : mUserManager.getEnabledProfiles(currentUserId)) {
            if (ui.isManagedProfile() && ui.isQuietModeEnabled()) {
                mManagedProfileInQuietMode = true;
                return;
            }
        }
    }

    private void profileChanged(int userId) {
        UserInfo user = null;
        if (userId == UserHandle.USER_CURRENT) {
            try {
                user = ActivityManagerNative.getDefault().getCurrentUser();
            } catch (RemoteException e) {
                // Ignore
            }
        } else {
            user = mUserManager.getUserInfo(userId);
        }

        mManagedProfileFocused = user != null && user.isManagedProfile();
        if (DEBUG) Log.v(TAG, "profileChanged: mManagedProfileFocused: " + mManagedProfileFocused);
        // Actually update the icon later when transition starts.
    }

    private void updateManagedProfile() {
        if (DEBUG) Log.v(TAG, "updateManagedProfile: mManagedProfileFocused: "
                + mManagedProfileFocused);
        final boolean showIcon;
        if (mManagedProfileFocused && !mStatusBarKeyguardViewManager.isShowing()) {
            showIcon = true;
            mIconController.setIcon(mSlotManagedProfile,
                    R.drawable.stat_sys_managed_profile_status,
                    mContext.getString(R.string.accessibility_managed_profile));
        } else if (mManagedProfileInQuietMode) {
            showIcon = true;
            mIconController.setIcon(mSlotManagedProfile,
                    R.drawable.stat_sys_managed_profile_status_off,
                    mContext.getString(R.string.accessibility_managed_profile));
        } else {
            showIcon = false;
        }
        if (mManagedProfileIconVisible != showIcon) {
            mIconController.setIconVisibility(mSlotManagedProfile, showIcon);
            mManagedProfileIconVisible = showIcon;
        }
    }

    private final SynchronousUserSwitchObserver mUserSwitchListener =
            new SynchronousUserSwitchObserver() {
                @Override
                public void onUserSwitching(int newUserId) throws RemoteException {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mUserInfoController.reloadUserInfo();
                        }
                    });
                }

                @Override
                public void onUserSwitchComplete(final int newUserId) throws RemoteException {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateAlarm();
                            profileChanged(newUserId);
                            updateQuietState();
                            updateManagedProfile();
                        }
                    });
                }

                @Override
                public void onForegroundProfileSwitch(final int newProfileId) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            profileChanged(newProfileId);
                        }
                    });
                }
            };

    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        @Override
        public void onHotspotChanged(boolean enabled) {
//            mIconController.setIcon(mSlotHotspot, R.drawable.stat_sys_hotspot,
//                    mContext.getString(R.string.accessibility_status_bar_hotspot));
//            mIconController.setIconVisibility(mSlotHotspot, enabled);
        }
    };

    private final CastController.Callback mCastCallback = new CastController.Callback() {
        @Override
        public void onCastDevicesChanged() {
            updateCast();
        }
    };

    public void appTransitionStarting(long startTime, long duration) {
        updateManagedProfile();
    }

    public void notifyKeyguardShowingChanged() {
        updateManagedProfile();
    }

    public void setCurrentUserSetup(boolean userSetup) {
        if (mCurrentUserSetup == userSetup) return;
        mCurrentUserSetup = userSetup;
        updateAlarm();
        updateQuietState();
    }

    @Override
    public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
        boolean portrait = RotationLockTile.isCurrentOrientationLockPortrait(
                mRotationLockController, mContext);
        if (rotationLocked) {
            if (portrait) {
                mIconController.setIcon(mSlotRotate, R.drawable.stat_sys_rotate_portrait,
                        mContext.getString(R.string.accessibility_rotation_lock_on_portrait));
            } else {
                mIconController.setIcon(mSlotRotate, R.drawable.stat_sys_rotate_landscape,
                        mContext.getString(R.string.accessibility_rotation_lock_on_landscape));
            }
            mIconController.setIconVisibility(mSlotRotate, true);
        } else {
            mIconController.setIconVisibility(mSlotRotate, false);
        }
    }

    private void updateHeadsetPlug(Intent intent) {
        boolean connected = intent.getIntExtra("state", 0) != 0;
        boolean hasMic = intent.getIntExtra("microphone", 0) != 0;
        if (connected) {
            String contentDescription = mContext.getString(hasMic
                    ? R.string.accessibility_status_bar_headset
                    : R.string.accessibility_status_bar_headphones);
            mIconController.setIcon(mSlotHeadset, hasMic ? R.drawable.ic_headset_mic
                    : R.drawable.ic_headset, contentDescription);
            mIconController.setIconVisibility(mSlotHeadset, true);
        } else {
            mIconController.setIconVisibility(mSlotHeadset, false);
        }
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mIconController.setIconVisibility(mSlotDataSaver, isDataSaving);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_ALARM_CHANGED)) {
                updateAlarm(intent);
            }else if (action.equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                updateAlarm();
            } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION) ||
                    action.equals(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION)) {
                updateVolumeZen();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                updateSimState(intent);
            } else if (action.equals(TelecomManager.ACTION_CURRENT_TTY_MODE_CHANGED)) {
                updateTTY(intent);
            } else if (action.equals(Intent.ACTION_MANAGED_PROFILE_AVAILABLE) ||
                    action.equals(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE) ||
                    action.equals(Intent.ACTION_MANAGED_PROFILE_REMOVED)) {
                updateQuietState();
                updateManagedProfile();
            } else if (action.equals(AudioManager.ACTION_HEADSET_PLUG)) {
                updateHeadsetPlug(intent);
            }
        }
    };

    private Runnable mRemoveCastIconRunnable = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "updateCast: hiding icon NOW");
            mIconController.setIconVisibility(mSlotCast, false);
        }
    };
}
