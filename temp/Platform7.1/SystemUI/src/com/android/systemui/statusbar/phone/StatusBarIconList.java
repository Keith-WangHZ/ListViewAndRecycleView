/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.io.PrintWriter;
import java.util.ArrayList;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;

public class StatusBarIconList {
    private ArrayList<String> mSlots = new ArrayList<>();
    private ArrayList<StatusBarIcon> mIcons = new ArrayList<>();
    
    private ArrayList<String> mSlotsKeyguard = new ArrayList<>();
    private ArrayList<StatusBarIcon> mIconsKeyguard = new ArrayList<>();

    private Boolean mKeyguard = false;
    private final String TAG = "StatusBarIconList";
    
    private KeyguardUpdateMonitorCallback mUpdateCallBack = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onKeyguardVisibilityChanged(boolean showing) {
			mKeyguard = showing;
			LogHelper.sd(TAG,"mKeyguard="+mKeyguard);
		};
	};
	
    public StatusBarIconList(String[] slots) {
        setStatusBarIconList(slots, mSlots, mIcons, true);
        setStatusBarIconList(slots, mSlotsKeyguard, mIconsKeyguard, false);
        KeyguardUpdateMonitor.getInstance(SystemUIApplication.getContext()).registerCallback(mUpdateCallBack);
    }
    
    public void unRegisterUpdateCallBack(){
    	KeyguardUpdateMonitor.getInstance(SystemUIApplication.getContext()).removeCallback(mUpdateCallBack);
    }
    
	public void setStatusBarIconList(String[] slots, ArrayList<String> mSlots,
    		ArrayList<StatusBarIcon> mIcons, boolean isKeyguard) {
        mSlots.clear();
        final int N = slots.length;
        for (int i=0; i < N; i++) {
            mSlots.add(slots[i]);
            mIcons.add(null);
        }
    }

	public int getSlotIndex(String slot, ArrayList<String> mSlots,
    		ArrayList<StatusBarIcon> mIcons, boolean isKeyguard) {
		 final int N = mSlots.size();
	        for (int i=0; i<N; i++) {
	            if (slot.equals(mSlots.get(i))) {
	                return i;
	            }
	        }
	        // Auto insert new items at the beginning.
	        mSlots.add(slot);
	        mIcons.add(null);
	        return N;
	}
	
    public int getSlotIndex(String slot) {
    	int N = getSlotIndex(slot, mSlots, mIcons, true);
    	getSlotIndex(slot, mSlotsKeyguard, mIconsKeyguard, false);
    	return N;
    }

    public int size() {
    	if(!mKeyguard){
    		return mSlots.size();
        }else{
        	return mSlotsKeyguard.size();
        }
    }

    public void setIcon(int index, StatusBarIcon icon) {
    	mIcons.set(index, icon);
        mIconsKeyguard.set(index, icon);
    }

    public void removeIcon(int index) {
    	mIcons.set(index, null);
    	mIconsKeyguard.set(index, null);
    }

    public String getSlot(int index) {
    	if(!mKeyguard){
    		return mSlots.get(index);
        }else{
        	return mSlotsKeyguard.get(index);
        }
    }

    public StatusBarIcon getIcon(int index) {
    	if(!mKeyguard){
    		return mIcons.get(index);
        }else{
        	return mIconsKeyguard.get(index);
        }
    }

    public int getViewIndex(int index, ArrayList<String> mSlots,
    		ArrayList<StatusBarIcon> mIcons, boolean isKeyguard) {
        int count = 0;
        for (int i = 0; i < index; i++) {
            if (mIcons.get(i) != null) {
                count++;
            }
        }
        return count;
    }
    
    public int getViewIndex(int index) {
    	if(!mKeyguard){
    		return getViewIndex(index, mSlots, mIcons, true);
        }else{
        	return getViewIndex(index, mSlotsKeyguard, mIconsKeyguard, false);
        }
    }

    public void dump(PrintWriter pw) {
        final int N = mSlots.size();
        pw.println("Icon list:");
        for (int i=0; i<N; i++) {
            pw.printf("mSlots  %2d: (%s) %s\n", i, mSlots.get(i), mIcons.get(i));
        }
        for (int i=0; i<mSlotsKeyguard.size(); i++) {
            pw.printf("mSlotsKeyguard  %2d: (%s) %s\n", i, mSlotsKeyguard.get(i), mSlotsKeyguard.get(i));
        }
    }
}
