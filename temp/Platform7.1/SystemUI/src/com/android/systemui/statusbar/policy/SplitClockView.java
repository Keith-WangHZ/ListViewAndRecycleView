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

package com.android.systemui.statusbar.policy;

import java.util.Locale;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.QsHeaderSplitTextClock;
import com.android.systemui.QsHeaderTextClock;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;

/**
 * Container for a clock which has two separate views for the clock itself and AM/PM indicator. This
 * is used to scale the clock independently of AM/PM.
 */
public class SplitClockView extends LinearLayout {

    private QsHeaderSplitTextClock mTimeView;
    private QsHeaderTextClock mAmPmView;
    String clockView12;
    String clockView24;
    String clockView12Skel;
    String clockView24Skel;
    String cacheKey = "";

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            if (Intent.ACTION_TIME_CHANGED.equals(action)
//                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
//                    || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                updatePatterns();
        }
    };

    public SplitClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimeView = (QsHeaderSplitTextClock) findViewById(R.id.time_view);
        mAmPmView = (QsHeaderTextClock) findViewById(R.id.am_pm_view);
        
        final Resources res = getContext().getResources();
        clockView12Skel = res.getString(R.string.clock_12hr_format);
        clockView24Skel = res.getString(R.string.clock_24hr_format);
        changeTimeFormat();
    }

//    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
//    	@Override
//    	public void onTimeChanged(Intent intent) {
//    		updatePatterns();
//    		super.onTimeChanged(intent);
//    	}
//	};
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);//add by tanyi on 20160407 for updateTime
        CurrentUserTracker.registerReceiverAsUser(mIntentReceiver, filter, null, new Handler());
//        KeyguardUpdateMonitor.getInstance(getContext()).registerCallback(mUpdateCallback);

        updatePatterns();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mIntentReceiver);
//        if(mUpdateCallback != null){
//			KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mUpdateCallback);
//		}
    }

    private void updatePatterns() {
//        String formatString = DateFormat.getTimeFormatString(getContext(),ActivityManager.getCurrentUser());
//        int index = getAmPmPartEndIndex(formatString);
//        String timeString;
//        String amPmString;
//        if (index == -1) {
//            timeString = formatString;
//            amPmString = "";
//        } else {
//            timeString = formatString.substring(0, index);
//            amPmString = formatString.substring(index);
//        }
        changeTimeFormat();
        mTimeView.setFormat12Hour(clockView12);
        mTimeView.setFormat24Hour(clockView24);
//        mAmPmView.setFormat12Hour(amPmString);
//        mAmPmView.setFormat24Hour(amPmString);
    }
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	updatePatterns();
    }

    /**
     * @return the index where the AM/PM part starts at the end in {@code formatString} including
     *         leading white spaces or {@code -1} if no AM/PM part is found or {@code formatString}
     *         doesn't end with AM/PM part
     */
//    private static int getAmPmPartEndIndex(String formatString) {
//        boolean hasAmPm = false;
//        int length = formatString.length();
//        for (int i = length - 1; i >= 0; i--) {
//            char c = formatString.charAt(i);
//            boolean isAmPm = c == 'a';
//            boolean isWhitespace = Character.isWhitespace(c);
//            if (isAmPm) {
//                hasAmPm = true;
//            }
//            if (isAmPm || isWhitespace) {
//                continue;
//            }
//            if (i == length - 1) {
//
//                // First character was not AM/PM and not whitespace, so it's not ending with AM/PM.
//                return -1;
//            } else {
//
//                // If we have AM/PM at all, return last index, or -1 to indicate that it's not
//                // ending with AM/PM.
//                return hasAmPm ? i + 1 : -1;
//            }
//        }
//
//        // Only AM/PM and whitespaces? The whole string is AM/PM. Else: Only whitespaces in the
//        // string.
//        return hasAmPm ? 0 : -1;
//    }
    
    public void changeTimeFormat() {
    	final Locale locale = Locale.getDefault();
    	clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
    	
    	 final String key = locale.toString() + clockView12Skel + clockView24Skel;
         if (key.equals(cacheKey)) return;
         
		if (!clockView12Skel.contains("a")) {
			clockView12 = clockView12.replaceAll("a", "").trim();
		}

		clockView12 = clockView12 + "a";
		//clockView12 = clockView12.replace(':', '\uee01');
		clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);
	}
}
