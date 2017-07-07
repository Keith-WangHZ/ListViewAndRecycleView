/*
 * Copyright (c) 2013-2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.covermode;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;
import com.android.keyguard.KeyguardUpdateMonitor.WeatherData;
import com.yulong.android.feature.FeatureConfig;
import com.yulong.android.feature.FeatureString;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StarrySmallWindowTimeView extends LinearLayout {
	private static final String TAG = "StarrySmallWindowTimeView";

	private TextClock mDateView;
	private TextClock mClockView;
	TextView mCityName;

	private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

		@Override
		public void onTimeChanged() {
			refresh();
		}

		@Override
		public void onKeyguardVisibilityChanged(boolean showing) {
			if (showing) {
				Slog.v(TAG, "refresh statusview showing:" + showing);
				refresh();
			}
		}

		@Override
		public void onStartedWakingUp() {
			setEnableMarquee(true);
		}

		@Override
		public void onFinishedGoingToSleep(int why) {
			setEnableMarquee(false);
		}

		@Override
		public void onUserSwitchComplete(int userId) {
			refresh();
		}

		@Override
		public void onWeatherChanged(WeatherData weatherData) {
			if (weatherData != null) {
				updateCityName(weatherData.mCity);
			}
		}
	};

	public StarrySmallWindowTimeView(Context context) {
		this(context, null, 0);
	}

	public StarrySmallWindowTimeView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StarrySmallWindowTimeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private void setEnableMarquee(boolean enabled) {

	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		mDateView = (TextClock) findViewById(R.id.starry_small_window_date_view);
		mClockView = (TextClock) findViewById(R.id.starry_small_window_clock_view);
		mCityName = (TextView) findViewById(R.id.starry_small_window_city_name);

		final boolean screenOn = KeyguardUpdateMonitor.getInstance(mContext).isScreenOn();
		setEnableMarquee(screenOn);
		refresh();
		// updateOwnerInfo();

		// Disable elegant text height because our fancy colon makes the ymin
		// value huge for no
		// reason.
		mClockView.setElegantTextHeight(false);
	}

	protected void updateCityName(String city) {
		if (city.isEmpty() || mCityName == null) {
			return;
		}
		mCityName.setText(city);
		mCityName.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
		mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
	}

	public void refreshTime() {
		mClockView.setFormat12Hour(Patterns.clockView12);
		mClockView.setFormat24Hour(Patterns.clockView24);
		mDateView.setFormat24Hour(Patterns.dateView);
		mDateView.setFormat12Hour(Patterns.dateView);
	}

	private void refresh() {
		Patterns.update(mContext, false);
		refreshTime();
	}

	public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
		if (info == null) {
			return "";
		}
		String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma";
		String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
		return DateFormat.format(pattern, info.getTriggerTime()).toString();
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
	public boolean hasOverlappingRendering() {
		return false;
	}

	// DateFormat.getBestDateTimePattern is extremely expensive, and refresh is
	// called often.
	// This is an optimization to ensure we only recompute the patterns when the
	// inputs change.
	private static final class Patterns {
		static String dateView;
		static String clockView12;
		static String clockView24;
		static String cacheKey;

		static void update(Context context, boolean hasAlarm) {
			final Locale locale = Locale.getDefault();
			final Resources res = context.getResources();
			final String dateViewSkel = res.getString(locale.toString().equals("zh_CN") ? R.string.abbrev_wday_month_day_no_year
					: R.string.abbrev_wday_month_day_short);
			final String clockView12Skel = res.getString(R.string.clock_12hr_format);
			final String clockView24Skel = res.getString(R.string.clock_24hr_format);

			final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
			if (key.equals(cacheKey)) {
				return;
			}
			dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

			if (dateView != null && locale.toString().equals("zh_CN") && dateView.contains("EEEE")) {
				dateView = dateView.replace("EEEE", "  EEEE");
			}

			cacheKey = key;
			
			clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
			// CLDR insists on adding an AM/PM indicator even though it wasn't
			// in the skeleton
			// format. The following code removes the AM/PM indicator if we
			// didn't want it.
			if (!clockView12Skel.contains("a")) {
				clockView12 = clockView12.replaceAll("a", "").trim();
			}

			clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

			// Use fancy colon.
			clockView24 = clockView24.replace(':', '\uee01');
			clockView12 = clockView12.replace(':', '\uee01');
		}
	}
}
