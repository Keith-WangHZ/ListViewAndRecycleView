/*
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class KeyguardStatusView extends LinearLayout {
	private static final boolean DEBUG = KeyguardConstants.DEBUG;
	private static final String TAG = "KeyguardStatusView";

	private final LockPatternUtils mLockPatternUtils;
	private final AlarmManager mAlarmManager;

	private TextView mAlarmStatusView;
	private TextClock mDateView;
	private CoolUITextClock mClockView;
	//private TextView mOwnerInfo;

	// add by mengludong to show double clock view when net work roaming
	// 2015.12.01
	private TextClock mBeijingDateView;
	private CoolUITextClock mBeijingClockView;
	private TextClock mLocalDateView;
	private CoolUITextClock mLocalClockView;
	private LinearLayout mSingleClockContainer;
	private LinearLayout mDoubleClockContainer;
	private TextView mLocalCityName;
	private boolean mIsRoaming = false;
	// add end
	// add by huazhi
	private LinearLayout.LayoutParams lParams;
	// add end

	private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

		@Override
		public void onTimeChanged() {
			refresh();
		}

		@Override
		public void onKeyguardVisibilityChanged(boolean showing) {
			if (showing) {
				if (DEBUG)
					Slog.v(TAG, "refresh statusview showing:" + showing);
				refresh();
				//updateOwnerInfo();
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
			//updateOwnerInfo();
		}
		
		/**
         * update double clock state when roaming state changed
         * @see com.android.keyguard.KeyguardUpdateMonitorCallback#onRoamingStateChange(boolean)
         * @add by mengludong, 2015.12.01
         */
		@Override
		public void onRoamingStateChange(boolean isRoaming) {
			Log.v(TAG, "onRoamingStateChange() isRoaming = " + isRoaming);
			mIsRoaming = isRoaming;
			updateVisibility();
			refresh();
		}
	};

	public KeyguardStatusView(Context context) {
		this(context, null, 0);
	}

	public KeyguardStatusView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mLockPatternUtils = new LockPatternUtils(getContext());
	}

	private void setEnableMarquee(boolean enabled) {
		if (DEBUG)
			Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
		if (mAlarmStatusView != null)
			mAlarmStatusView.setSelected(enabled);
		/*if (mOwnerInfo != null)
			mOwnerInfo.setSelected(enabled);*/
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
		mDateView = (TextClock) findViewById(R.id.date_view);
		mClockView = (CoolUITextClock) findViewById(R.id.clock_view);
		mDateView.setShowCurrentUserTime(true);
		mClockView.setShowCurrentUserTime(true);
		//mOwnerInfo = (TextView) findViewById(R.id.owner_info);

		// add by mengludong to show double clock view when net work roaming
		// 2015.12.01
		mBeijingClockView = (CoolUITextClock) findViewById(R.id.beijing_clock_view);
		mBeijingDateView = (TextClock) findViewById(R.id.beijing_date_view);
		mLocalDateView = (TextClock) findViewById(R.id.local_date_view);
		mLocalClockView = (CoolUITextClock) findViewById(R.id.local_clock_view);
		mLocalCityName = (TextView) findViewById(R.id.city_name_local);
		mSingleClockContainer = (LinearLayout) findViewById(R.id.keyguard_clock_container);
		mDoubleClockContainer = (LinearLayout) findViewById(R.id.keyguard_double_clock_container);
		if (mDoubleClockContainer != null) {
			mBeijingDateView.setShowCurrentUserTime(true);
			mBeijingClockView.setShowCurrentUserTime(true);
			mBeijingClockView.setTimeZone("Asia/Shanghai");
			mBeijingDateView.setTimeZone("Asia/Shanghai");
			mLocalDateView.setShowCurrentUserTime(true);
			mLocalClockView.setShowCurrentUserTime(true);
			mBeijingClockView.setTextSize(56f);
			mLocalClockView.setTextSize(56f);
		}
		// add end

		boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
		setEnableMarquee(shouldMarquee);
		refresh();
		//updateOwnerInfo();

		// Disable elegant text height because our fancy colon makes the ymin
		// value huge for no
		// reason.
		mClockView.setElegantTextHeight(false);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
				getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
		mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
				getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
		/*mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
				getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));*/

		// add by mengludong to show double clock view when net work roaming
		// 2015.12.01
		if (mDoubleClockContainer != null) {
			mBeijingClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
					getResources().getDimensionPixelSize(R.dimen.widget_big_font_size_dc));
			mBeijingDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
					getResources().getDimensionPixelSize(R.dimen.widget_label_font_size_dc));
			mLocalClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
					getResources().getDimensionPixelSize(R.dimen.widget_big_font_size_dc));
			mLocalDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
					getResources().getDimensionPixelSize(R.dimen.widget_label_font_size_dc));
		}
		// add end
	}
	
	/**
	 * update double clock state when roaming state changed
	 * @add by mengludong, 2015.12.01
	 */
	protected void updateVisibility() {
		Log.v(TAG, "updateVisibility() isRoaming = " + mIsRoaming + ", supportOversea = " + KeyguardUpdateMonitor.isSupportOversea());
		if (mDoubleClockContainer == null)
			return;			
		if (mIsRoaming && !KeyguardUpdateMonitor.isSupportOversea()) {
			mSingleClockContainer.setVisibility(View.GONE);
			mDoubleClockContainer.setVisibility(View.VISIBLE);
		} else {
			mSingleClockContainer.setVisibility(View.VISIBLE);
			mDoubleClockContainer.setVisibility(View.GONE);
		}
	}

	public void refreshTime() {
		if (mIsRoaming && mDoubleClockContainer != null) {
			mBeijingClockView.setFormat12Hour(Patterns.clockViewForDouble12);
			mBeijingClockView.setFormat24Hour(Patterns.clockView24);
			mLocalClockView.setFormat12Hour(Patterns.clockViewForDouble12);
			mLocalClockView.setFormat24Hour(Patterns.clockView24);
			mBeijingDateView.setFormat24Hour(Patterns.dateView);
			mBeijingDateView.setFormat12Hour(Patterns.dateViewForDouble12);
			mLocalDateView.setFormat24Hour(Patterns.dateView);
			mLocalDateView.setFormat12Hour(Patterns.dateViewForDouble12);
			TimeZone timeZone = TimeZone.getDefault();
			boolean daylight = timeZone.inDaylightTime(new Date());
			String lcn = timeZone.getDisplayName(daylight, TimeZone.LONG);
			mLocalCityName.setText(lcn);
			Log.v(TAG, "refreshTime() local city : " + lcn);
		} else {
			mDateView.setFormat24Hour(Patterns.dateView);
			mDateView.setFormat12Hour(Patterns.dateView);
			if (mClockView != null) {
				mClockView.setFormat12Hour(Patterns.clockView12);
				mClockView.setFormat24Hour(Patterns.clockView24);
			}
		}
	}

	private void refresh() {
		AlarmManager.AlarmClockInfo nextAlarm = mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
		Patterns.update(mContext, nextAlarm != null);

		refreshTime();
		refreshAlarmStatus(nextAlarm);
	}

	void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
		if (nextAlarm != null) {
			String alarm = formatNextAlarm(mContext, nextAlarm);
			mAlarmStatusView.setText(alarm);
			mAlarmStatusView
					.setContentDescription(getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
			mAlarmStatusView.setVisibility(View.VISIBLE);
		} else {
			mAlarmStatusView.setVisibility(View.GONE);
		}
	}

	public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
		if (info == null) {
			return "";
		}
		String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma";
		String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
		return DateFormat.format(pattern, info.getTriggerTime()).toString();
	}

	/*private void updateOwnerInfo() {
		if (mOwnerInfo == null)
			return;
		String ownerInfo = getOwnerInfo();
		if (!TextUtils.isEmpty(ownerInfo)) {
			mOwnerInfo.setVisibility(View.VISIBLE);
			mOwnerInfo.setText(ownerInfo);
		} else {
			mOwnerInfo.setVisibility(View.GONE);
		}
	}*/

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

	private String getOwnerInfo() {
		String info = null;
		if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
			// Use the device owner information set by device policy client via
			// device policy manager.
			info = mLockPatternUtils.getDeviceOwnerInfo();
		} else {
			// Use the current user owner information if enabled.
			final boolean ownerInfoEnabled = mLockPatternUtils
					.isOwnerInfoEnabled(KeyguardUpdateMonitor.getCurrentUser());
			if (ownerInfoEnabled) {
				info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
			}
		}
		return info;
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
		
		static String clockViewForDouble12;
		static String dateViewForDouble12;

		static void update(Context context, boolean hasAlarm) {
			final Locale locale = Locale.getDefault();
			final Resources res = context.getResources();
			final String dateViewSkel = res.getString(
					hasAlarm ? R.string.abbrev_wday_month_day_no_year_alarm : R.string.abbrev_wday_month_day_no_year);
			final String clockView12Skel = res.getString(R.string.clock_12hr_format);
			final String clockView24Skel = res.getString(R.string.clock_24hr_format);
			final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
			if (key.equals(cacheKey))
				return;

			dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);
			// add to split date and week with a blank space
			if (dateView != null && locale.toString().equals("zh_CN") && dateView.contains("EEEE")) {
				dateView = dateView.replace("EEEE", " EEEE");
			}
			
			dateViewForDouble12 = new String(dateView) + " a";

			clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
			// CLDR insists on adding an AM/PM indicator even though it wasn't
			// in the skeleton
			// format. The following code removes the AM/PM indicator if we
			// didn't want it.
			if (!clockView12Skel.contains("a")) {
				clockView12 = clockView12.replaceAll("a", "").trim();
            }
			
			clockViewForDouble12 = new String(clockView12);

			clockView12 = clockView12 + "a";

			clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

			try {
                final Configuration config = context.getResources().getConfiguration();
                final boolean isDefaultFont = true/*TextUtils.isEmpty(config.defaultFontName)*/;
                if (isDefaultFont) {
                    // Use fancy colon.
                    clockView24 = clockView24.replace(':', '\uee01');
                    clockView12 = clockView12.replace(':', '\uee01');   
                }
            } catch (NoSuchFieldError e) {
                Log.e(TAG, "NoSuchFieldError:" + e);
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e);
            }

			cacheKey = key;
		}
	}
}
