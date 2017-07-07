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

package com.android.systemui;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libcore.icu.LocaleData;
import android.annotation.NonNull;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.RemotableViewMethod;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewHierarchyEncoder;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RemoteViews.RemoteView;
import android.widget.TextView;

import com.android.internal.R;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;

/**
 * <p><code>TextClock</code> can display the current date and/or time as
 * a formatted string.</p>
 *
 * <p>This view honors the 24-hour format system setting. As such, it is
 * possible and recommended to provide two different formatting patterns:
 * one to display the date/time in 24-hour mode and one to display the
 * date/time in 12-hour mode. Most callers will want to use the defaults,
 * though, which will be appropriate for the user's locale.</p>
 *
 * <p>It is possible to determine whether the system is currently in
 * 24-hour mode by calling {@link #is24HourModeEnabled()}.</p>
 *
 * <p>The rules used by this widget to decide how to format the date and
 * time are the following:</p>
 * <ul>
 *     <li>In 24-hour mode:
 *         <ul>
 *             <li>Use the value returned by {@link #getFormat24Hour()} when non-null</li>
 *             <li>Otherwise, use the value returned by {@link #getFormat12Hour()} when non-null</li>
 *             <li>Otherwise, use a default value appropriate for the user's locale, such as {@code h:mm a}</li>
 *         </ul>
 *     </li>
 *     <li>In 12-hour mode:
 *         <ul>
 *             <li>Use the value returned by {@link #getFormat12Hour()} when non-null</li>
 *             <li>Otherwise, use the value returned by {@link #getFormat24Hour()} when non-null</li>
 *             <li>Otherwise, use a default value appropriate for the user's locale, such as {@code HH:mm}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>The {@link CharSequence} instances used as formatting patterns when calling either
 * {@link #setFormat24Hour(CharSequence)} or {@link #setFormat12Hour(CharSequence)} can
 * contain styling information. To do so, use a {@link android.text.Spanned} object.
 * Note that if you customize these strings, it is your responsibility to supply strings
 * appropriate for formatting dates and/or times in the user's locale.</p>
 *
 * @attr ref android.R.styleable#TextClock_format12Hour
 * @attr ref android.R.styleable#TextClock_format24Hour
 * @attr ref android.R.styleable#TextClock_timeZone
 */
@RemoteView
public class QsHeaderSplitTextClock extends TextView {
    private static final String TAG = QsHeaderSplitTextClock.class.getSimpleName();
    /**
     * The default formatting pattern in 12-hour mode. This pattern is used
     * if {@link #setFormat12Hour(CharSequence)} is called with a null pattern
     * or if no pattern was specified when creating an instance of this class.
     *
     * This default pattern shows only the time, hours and minutes, and an am/pm
     * indicator.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     *
     * @deprecated Let the system use locale-appropriate defaults instead.
     */
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";

    /**
     * The default formatting pattern in 24-hour mode. This pattern is used
     * if {@link #setFormat24Hour(CharSequence)} is called with a null pattern
     * or if no pattern was specified when creating an instance of this class.
     *
     * This default pattern shows only the time, hours and minutes.
     *
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour()
     *
     * @deprecated Let the system use locale-appropriate defaults instead.
     */
    
    
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";

    private CharSequence mFormat12;
    private CharSequence mFormat24;
    private CharSequence mDescFormat12;
    private CharSequence mDescFormat24;

    @ExportedProperty
    private CharSequence mFormat;
    @ExportedProperty
    private boolean mHasSeconds;

    private CharSequence mDescFormat;

    private boolean mAttached;

    private Calendar mTime;
    private String mTimeZone;

    private boolean mShowCurrentUserTime;
    private static final int MSG_TIME_TICK = 0;
    
    String clockView12Skel;
    String clockView24Skel;
    String cacheKey = "";

    private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            chooseFormat();
            onTimeChanged();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            chooseFormat();
            onTimeChanged();
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String timeZone = intent.getStringExtra("time-zone");
                createTime(timeZone);
            }
            onTimeChanged();
            mHandler.sendEmptyMessage(MSG_TIME_TICK);
        }
    };
    
    Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if(msg.what == MSG_TIME_TICK){
				 onTimeChanged();
			}
		}
    	
    };

    private final Runnable mTicker = new Runnable() {
        public void run() {
            onTimeChanged();
        	mHandler.sendEmptyMessage(MSG_TIME_TICK);
            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);

            getHandler().postAtTime(mTicker, next);
        }
    };

    /**
     * Creates a new clock using the default patterns for the current locale.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    @SuppressWarnings("UnusedDeclaration")
    public QsHeaderSplitTextClock(Context context) {
        super(context);
        init();
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are
     * intialized from the attributes specified in XML.
     *
     * This constructor uses a default style of 0, so the only attribute values
     * applied are those in the Context's Theme and the given AttributeSet.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     */
    @SuppressWarnings("UnusedDeclaration")
    public QsHeaderSplitTextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are
     * intialized from the attributes specified in XML.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public QsHeaderSplitTextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QsHeaderSplitTextClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.TextClock, defStyleAttr, defStyleRes);
        try {
            mFormat12 = a.getText(R.styleable.TextClock_format12Hour);
            mFormat24 = a.getText(R.styleable.TextClock_format24Hour);
            mTimeZone = a.getString(R.styleable.TextClock_timeZone);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
    	
    	 final Resources res = getContext().getResources();
         clockView12Skel = "hm";
         clockView24Skel = "Hm";
         changeTimeFormat();
       /* if (mFormat12 == null || mFormat24 == null) {
            LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
            if (mFormat12 == null) {
                mFormat12 = ld.timeFormat_hm;
            }
            if (mFormat24 == null) {
                mFormat24 = ld.timeFormat_Hm;
            }
        }*/

        createTime(mTimeZone);
        // Wait until onAttachedToWindow() to handle the ticker
        chooseFormat(false);
    }

    private void createTime(String timeZone) {
        if (timeZone != null) {
            mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            mTime = Calendar.getInstance();
        }
    }
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	changeTimeFormat();
    	chooseFormat(false);
    }

    /**
     * Returns the formatting pattern used to display the date and/or time
     * in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     *
     * @return A {@link CharSequence} or null.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    @ExportedProperty
    public CharSequence getFormat12Hour() {
        return mFormat12;
    }

    /**
     * <p>Specifies the formatting pattern used to display the date and/or time
     * in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.</p>
     *
     * <p>If this pattern is set to null, {@link #getFormat24Hour()} will be used
     * even in 12-hour mode. If both 24-hour and 12-hour formatting patterns
     * are set to null, the default pattern for the current locale will be used
     * instead.</p>
     *
     * <p><strong>Note:</strong> if styling is not needed, it is highly recommended
     * you supply a format string generated by
     * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}. This method
     * takes care of generating a format string adapted to the desired locale.</p>
     *
     *
     * @param format A date/time formatting pattern as described in {@link DateFormat}
     *
     * @see #getFormat12Hour()
     * @see #is24HourModeEnabled()
     * @see DateFormat#getBestDateTimePattern(java.util.Locale, String)
     * @see DateFormat
     *
     * @attr ref android.R.styleable#TextClock_format12Hour
     */
    @RemotableViewMethod
    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        onTimeChanged();
    }
    
    /**
     * Like setFormat12Hour, but for the content description.
     * @hide
     */
    public void setContentDescriptionFormat12Hour(CharSequence format) {
        mDescFormat12 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Returns the formatting pattern used to display the date and/or time
     * in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     *
     * @return A {@link CharSequence} or null.
     *
     * @see #setFormat24Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    @ExportedProperty
    public CharSequence getFormat24Hour() {
        return mFormat24;
    }

    /**
     * <p>Specifies the formatting pattern used to display the date and/or time
     * in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.</p>
     *
     * <p>If this pattern is set to null, {@link #getFormat24Hour()} will be used
     * even in 12-hour mode. If both 24-hour and 12-hour formatting patterns
     * are set to null, the default pattern for the current locale will be used
     * instead.</p>
     *
     * <p><strong>Note:</strong> if styling is not needed, it is highly recommended
     * you supply a format string generated by
     * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}. This method
     * takes care of generating a format string adapted to the desired locale.</p>
     *
     * @param format A date/time formatting pattern as described in {@link DateFormat}
     *
     * @see #getFormat24Hour()
     * @see #is24HourModeEnabled()
     * @see DateFormat#getBestDateTimePattern(java.util.Locale, String)
     * @see DateFormat
     *
     * @attr ref android.R.styleable#TextClock_format24Hour
     */
    @RemotableViewMethod
    public void setFormat24Hour(CharSequence format) {
        mFormat24 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Like setFormat24Hour, but for the content description.
     * @hide
     */
    public void setContentDescriptionFormat24Hour(CharSequence format) {
        mDescFormat24 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Sets whether this clock should always track the current user and not the user of the
     * current process. This is used for single instance processes like the systemUI who need
     * to display time for different users.
     *
     * @hide
     */
    public void setShowCurrentUserTime(boolean showCurrentUserTime) {
        mShowCurrentUserTime = showCurrentUserTime;

        chooseFormat();
        onTimeChanged();
        unregisterObserver();
        registerObserver();
    }

    /**
     * Indicates whether the system is currently using the 24-hour mode.
     *
     * When the system is in 24-hour mode, this view will use the pattern
     * returned by {@link #getFormat24Hour()}. In 12-hour mode, the pattern
     * returned by {@link #getFormat12Hour()} is used instead.
     *
     * If either one of the formats is null, the other format is used. If
     * both formats are null, the default formats for the current locale are used.
     *
     * @return true if time should be displayed in 24-hour format, false if it
     *         should be displayed in 12-hour format.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour()
     */
    public boolean is24HourModeEnabled() {
        if (mShowCurrentUserTime) {
            return DateFormat.is24HourFormat(getContext(), ActivityManager.getCurrentUser());
        } else {
            return DateFormat.is24HourFormat(getContext());
        }
    }

    /**
     * Indicates which time zone is currently used by this view.
     *
     * @return The ID of the current time zone or null if the default time zone,
     *         as set by the user, must be used
     *
     * @see TimeZone
     * @see java.util.TimeZone#getAvailableIDs()
     * @see #setTimeZone(String)
     */
    public String getTimeZone() {
        return mTimeZone;
    }

    /**
     * Sets the specified time zone to use in this clock. When the time zone
     * is set through this method, system time zone changes (when the user
     * sets the time zone in settings for instance) will be ignored.
     *
     * @param timeZone The desired time zone's ID as specified in {@link TimeZone}
     *                 or null to user the time zone specified by the user
     *                 (system time zone)
     *
     * @see #getTimeZone()
     * @see java.util.TimeZone#getAvailableIDs()
     * @see TimeZone#getTimeZone(String)
     *
     * @attr ref android.R.styleable#TextClock_timeZone
     */
    @RemotableViewMethod
    public void setTimeZone(String timeZone) {
        mTimeZone = timeZone;

        createTime(timeZone);
        onTimeChanged();
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()}
     * depending on whether the user has selected 24-hour format.
     *
     * Calling this method does not schedule or unschedule the time ticker.
     */
    private void chooseFormat() {
        chooseFormat(true);
    }

    /**
     * Returns the current format string. Always valid after constructor has
     * finished, and will never be {@code null}.
     *
     * @hide
     */
    public CharSequence getFormat() {
        return mFormat;
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()}
     * depending on whether the user has selected 24-hour format.
     *
     * @param handleTicker true if calling this method should schedule/unschedule the
     *                     time ticker, false otherwise
     */
    private void chooseFormat(boolean handleTicker) {
        final boolean format24Requested = is24HourModeEnabled();

        LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);

        if (format24Requested) {
            mFormat = abc(mFormat24, mFormat12, ld.timeFormat_Hm);
            mDescFormat = abc(mDescFormat24, mDescFormat12, mFormat);
        } else {
            mFormat = abc(mFormat12, mFormat24, ld.timeFormat_hm);
            mDescFormat = abc(mDescFormat12, mDescFormat24, mFormat);
        }

        boolean hadSeconds = mHasSeconds;
        mHasSeconds = DateFormat.hasSeconds(mFormat);

        if (handleTicker && mAttached && hadSeconds != mHasSeconds) {
            if (hadSeconds) getHandler().removeCallbacks(mTicker);
            else mTicker.run();
        }
    }
    
    /**
     * Returns a if not null, else return b if not null, else return c.
     */
    private static CharSequence abc(CharSequence a, CharSequence b, CharSequence c) {
        return a == null ? (b == null ? c : b) : a;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            registerReceiver();
            registerObserver();

            createTime(mTimeZone);

            if (mHasSeconds) {
                mTicker.run();
            } else {
                onTimeChanged();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            unregisterReceiver();
            unregisterObserver();

            getHandler().removeCallbacks(mTicker);

            mAttached = false;
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_SCREEN_ON);//add by tanyi on 20160407 for updateTime

        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    	//KeyguardUpdateMonitor.getInstance(getContext()).registerCallback(mUpdateCallback);
    }

    private void registerObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        if (mShowCurrentUserTime) {
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true,
                    mFormatChangeObserver, UserHandle.USER_ALL);
        } else {
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true,
                    mFormatChangeObserver);
        }
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
//        if(mUpdateCallback != null){
//			KeyguardUpdateMonitor.getInstance(getContext()).removeCallback(mUpdateCallback);
//		}
    }

    private void unregisterObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(mFormatChangeObserver);
    }

	public static String trimBlank(String str) {
		String dest = "";
		if (str != null) {
			Pattern p = Pattern.compile("\t|\r|\n");
			Matcher m = p.matcher(str);
			dest = m.replaceAll("");
		}
		return dest;
	}

	private static final boolean isEnglilsh(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if ((c >= 0x0 && c <= 31) || (c >= 33 && c <= 0x7f)) {
			return true;
		}
		return false;
	}
	
	public static boolean containedChinese(String src) {
		boolean result = false;

		Pattern p;
		Matcher m;

		p = Pattern.compile("[\u4e00-\u9fa5]");
		m = p.matcher(src);
		if (m.find()) {
			result = true;
		}
		return result;
	}

	public static boolean containedBlanksAndEnglish(String src) {
		char[] ch = src.toCharArray();
		for (int i = 0; i < ch.length; i++) {
			char c = ch[i];
			if (!(('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c==32)) {
				return false;
			}
		}
		return true;
	}
	
	static String FirstNBytes(String s, int n) {
		Pattern p = Pattern.compile("^[\\u4e00-\\u9fa5]$");
		int i = 0, j = 0;
		for (char c : s.toCharArray()) {
			Matcher m = p.matcher(String.valueOf(c));
			i += m.find() ? 2 : 1;
			++j;
			if (i == n)
				break;
			if (i > n) {
				--j;
				break;
			}
		}
		return s.substring(0, j);
	}

    private int mTextDp = 16;
    public void setTextDp(int size){
        mTextDp = size;
    }
    

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        
        CharSequence s1 = DateFormat.format(mFormat, mTime);
        CharSequence s2 = DateFormat.format(mDescFormat, mTime);
        String time = s1.toString();
       
        int start = -1;
		if (time.contains(":")) {
			start = time.indexOf(":") + 1;
		} else {
			start = time.indexOf('\uee01') + 1;
		}
		if (start < 0) {
			setText(time);
		} else {
			SpannableStringBuilder style = new SpannableStringBuilder(time);
			int end = start + 2;
			if (end < style.length()) {
                Locale locale = getContext().getResources().getConfiguration().locale;
                // YLH20160713:keep same size between HH and mm in Indonesia
                int tmpTextDb = mTextDp;
                if ("in".equals(locale.getLanguage()) && "ID".equals(locale.getCountry())) {
                    int inSize = (int) getResources().getDimension(com.android.systemui.R.dimen.qs_time_collapsed_size);
                    tmpTextDb = (int) (inSize / mContext.getResources().getDisplayMetrics().density);
                }
                style.setSpan(new AbsoluteSizeSpan((int) (tmpTextDb * mContext.getResources().getDisplayMetrics().density)),
                        end, style.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
			}
			setText(style);
		}
		setContentDescription(DateFormat.format(mDescFormat, mTime));
		
        //Log.d("","11 timeString="+s1.toString()+" s2="+s2.toString());
//		String shangwu = getContext().getResources().getString(com.android.systemui.R.string.qs_header_morning);
//        String xiawu = getContext().getResources().getString(com.android.systemui.R.string.qs_header_afternoon);
//        if(!getResources().getConfiguration().locale.getCountry().equalsIgnoreCase("MM")&&(time.contains(shangwu) || time.contains(xiawu))){
//        	 int start = time.indexOf(shangwu)==-1?time.indexOf(xiawu):time.indexOf(shangwu);
//        	SpannableStringBuilder style = new SpannableStringBuilder(time);
//    		style.setSpan(new AbsoluteSizeSpan(24, true), start, start+shangwu.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
//    		setText(style);
//        }else if(getResources().getConfiguration().locale.getCountry().equalsIgnoreCase("MM")&&(time.contains(shangwu) || time.contains(xiawu))){
//        	int start = time.indexOf(shangwu)==-1?time.indexOf(xiawu):time.indexOf(shangwu);
//        	SpannableStringBuilder style = new SpannableStringBuilder(time);
//        	if(time.contains(shangwu)){
//        		style.setSpan(new AbsoluteSizeSpan(24, true), start, start+shangwu.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
//        	}else {
//        		style.setSpan(new AbsoluteSizeSpan(24, true), start, start+xiawu.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
//			}
//    		setText(style);
//        }else{
//        	setText(time);
//        }
//        
//        setContentDescription(s2);
    }

	public void changeTimeFormat() {
		final Locale locale = Locale.getDefault();
		mFormat12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
		final String key = locale.toString() + clockView12Skel + clockView24Skel;
		if (key.equals(cacheKey))
			return;

		if (!clockView12Skel.contains("a")) {
			mFormat12 = mFormat12.toString().replaceAll("a", "").trim();
		}
		mFormat12 = mFormat12 + "a";
		// clockView12 = clockView12.replace(':', '\uee01');
		mFormat24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);
	}
    
    /** @hide */
    @Override
    protected void encodeProperties(@NonNull ViewHierarchyEncoder stream) {
        super.encodeProperties(stream);

        CharSequence s = getFormat12Hour();
        stream.addProperty("format12Hour", s == null ? null : s.toString());

        s = getFormat24Hour();
        stream.addProperty("format24Hour", s == null ? null : s.toString());
        stream.addProperty("format", mFormat == null ? null : mFormat.toString());
        stream.addProperty("hasSeconds", mHasSeconds);
    }
}
