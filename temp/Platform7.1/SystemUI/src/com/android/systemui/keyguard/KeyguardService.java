/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.keyguard;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.systemui.SystemUIApplication;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class KeyguardService extends Service {
    static final String TAG = "KeyguardService";
    static final String PERMISSION = android.Manifest.permission.CONTROL_KEYGUARD;

    private KeyguardViewMediator mKeyguardViewMediator;

    @Override
    public void onCreate() {
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        mKeyguardViewMediator =
                ((SystemUIApplication) getApplication()).getComponent(KeyguardViewMediator.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    void checkPermission() {
        // Avoid deadlock by avoiding calling back into the system process.
        if (Binder.getCallingUid() == Process.SYSTEM_UID) return;

        // Otherwise,explicitly check for caller permission ...
        if (getBaseContext().checkCallingOrSelfPermission(PERMISSION) != PERMISSION_GRANTED) {
            Log.w(TAG, "Caller needs permission '" + PERMISSION + "' to call " + Debug.getCaller());
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid()
                    + ", must have permission " + PERMISSION);
        }
    }

    private final IKeyguardService.Stub mBinder = new IKeyguardService.Stub() {

        @Override // Binder interface
        public void addStateMonitorCallback(IKeyguardStateCallback callback) {
            checkPermission();
            mKeyguardViewMediator.addStateMonitorCallback(callback);
        }

        @Override // Binder interface
        public void verifyUnlock(IKeyguardExitCallback callback) {
            checkPermission();
            mKeyguardViewMediator.verifyUnlock(callback);
        }

        @Override // Binder interface
        public void keyguardDone(boolean authenticated, boolean wakeup) {
            checkPermission();
            // TODO: Remove wakeup
            Log.d("fingerWakeupSlowLog:", "KeyguardService, keyguardDone");
            mKeyguardViewMediator.keyguardDone(authenticated);
        }

        //ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
        /*@Override*/ // Binder interface
        public void setOccluded(boolean isOccluded) {
            checkPermission();
            mKeyguardViewMediator.setOccluded(isOccluded);
        }

        //ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
        /*@Override*/ // Binder interface
        public void dismiss() {
            checkPermission();
            mKeyguardViewMediator.dismiss();
        }

        @Override // Binder interface
        public void onDreamingStarted() {
            checkPermission();
            mKeyguardViewMediator.onDreamingStarted();
        }

        @Override // Binder interface
        public void onDreamingStopped() {
            checkPermission();
            mKeyguardViewMediator.onDreamingStopped();
        }

        @Override // Binder interface
        public void onStartedGoingToSleep(int reason) {
            checkPermission();
            mKeyguardViewMediator.onStartedGoingToSleep(reason);
        }

        @Override // Binder interface
        public void onFinishedGoingToSleep(int reason, boolean cameraGestureTriggered) {
            checkPermission();
            mKeyguardViewMediator.onFinishedGoingToSleep(reason, cameraGestureTriggered);
        }

        @Override // Binder interface
        public void onStartedWakingUp() {
            checkPermission();
            Log.d("fingerWakeupSlowLog:", "KeyguardService, onStartedWakingUp");
            mKeyguardViewMediator.onStartedWakingUp();
        }

        @Override // Binder interface
        public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
            checkPermission();
            Log.d("fingerWakeupSlowLog:", "KeyguardService, onScreenTurningOn");
            mKeyguardViewMediator.onScreenTurningOn(callback);
        }

        @Override // Binder interface
        public void onScreenTurnedOn() {
            checkPermission();
            Log.d("fingerWakeupSlowLog:", "KeyguardService, onScreenTurnedOn");
            mKeyguardViewMediator.onScreenTurnedOn();
        }

        @Override // Binder interface
        public void onScreenTurnedOff() {
            checkPermission();
            mKeyguardViewMediator.onScreenTurnedOff();
        }

        @Override // Binder interface
        public void setKeyguardEnabled(boolean enabled) {
            checkPermission();
            mKeyguardViewMediator.setKeyguardEnabled(enabled);
        }

        @Override // Binder interface
        public void onSystemReady() {
            checkPermission();
            mKeyguardViewMediator.onSystemReady();
        }

        @Override // Binder interface
        public void doKeyguardTimeout(Bundle options) {
            checkPermission();
            mKeyguardViewMediator.doKeyguardTimeout(options);
        }

        @Override // Binder interface
        public void setCurrentUser(int userId) {
            checkPermission();
            mKeyguardViewMediator.setCurrentUser(userId);
        }

        @Override
        public void onBootCompleted() {
            checkPermission();
            mKeyguardViewMediator.onBootCompleted();
        }

        @Override
        public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
            checkPermission();
            Log.d("fingerWakeupSlowLog:", "KeyguardService, startKeyguardExitAnimation");
            mKeyguardViewMediator.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }

        @Override
        public void onActivityDrawn() {
            checkPermission();
            mKeyguardViewMediator.onActivityDrawn();
        }
        
        //ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
        //@Override
        public boolean isDismissable() throws RemoteException {
               return !mKeyguardViewMediator.isSecure();
        }

        //ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
		@Override
		public void dismiss(boolean allowWhileOccluded) throws RemoteException {
			// TODO Auto-generated method stub
			checkPermission();
            mKeyguardViewMediator.dismiss(/*allowWhileOccluded*/);
		}

		//ANROID_712_UPDATE: modify by ty on 20170221 for android7.1 update
		@Override
		public void setOccluded(boolean isOccluded, boolean animate)
				throws RemoteException {
			// TODO Auto-generated method stub
			checkPermission();
            mKeyguardViewMediator.setOccluded(isOccluded);
		}
    };
}

