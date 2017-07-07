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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.systemui.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateView extends TextView {
    private static final String TAG = "DateView";

    private final Date mCurrentTime = new Date();

    private SimpleDateFormat mDateFormat;
    private String mLastText;
    private String mDatePattern;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                    || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                if (Intent.ACTION_LOCALE_CHANGED.equals(action)
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    // need to get a fresh date format
                    mDateFormat = null;
                }
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DateView,
                0, 0);

        try {
            mDatePattern = a.getString(R.styleable.DateView_datePattern);
        } finally {
            a.recycle();
        }
        if (mDatePattern == null) {
            mDatePattern = getContext().getString(R.string.system_ui_date_pattern);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter, null, null);

        updateClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mDateFormat = null; // reload the locale next time
        getContext().unregisterReceiver(mIntentReceiver);
    }

    private boolean isZh() {
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    
    protected void updateClock() {
        String  dateFormat = null;
        
        String text;
        if(!isZh()){
        	dateFormat=getDateFormat();
        	text=(String) DateFormat.format(dateFormat, new Date());
            setText(text);
        }else{
        	dateFormat="<small>MM"+getContext().getString(R.string.dateview_month)+
        			"dd"+getContext().getString(R.string.dateview_day)+
        			"</small><br><small>EEEE</small>";
        	text=(String) DateFormat.format(dateFormat, new Date());
            setText(Html.fromHtml(text));
        }
    }
    
//    protected void updateClock() {
//        if (mDateFormat == null) {
//            final Locale l = Locale.getDefault();
//            final String fmt = DateFormat.getBestDateTimePattern(l, mDatePattern);
//            mDateFormat = new SimpleDateFormat(fmt, l);
//        }
//
//        mCurrentTime.setTime(System.currentTimeMillis());
//        final String text = getDateFormat();
//        if (!text.equals(mLastText)) {
//            setText(text);
//            mLastText = text;
//        }
//    }

    private String getDateFormat() {
    	final String dateView;
    	final Locale locale = Locale.getDefault();
    	final Resources res = getContext().getResources();
        final String dateViewSkel = res.getString(false
                ? R.string.abbrev_wday_month_day_no_year_alarm
                : R.string.abbrev_wday_month_day_no_year);

    	dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);
    	return dateView;
    }
}
