package com.android.covermode;

import java.util.Locale;

import com.android.keyguard.R;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

public class StarryTopStatusView extends LinearLayout {
	
	private TextClock mDateView;

	public StarryTopStatusView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mDateView = (TextClock) findViewById(R.id.date_view);
		mDateView.setShowCurrentUserTime(true);
		refresh();
	}
	
    private void refresh() {
        Patterns.update(mContext);
        refreshTime();
    }
    
    public void refreshTime() {
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);
    }
    
    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;

        static void update(Context context) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);

			final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;

			dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

			if (dateView != null && locale.toString().equals("zh_CN") && dateView.contains("EEEE")) {
				dateView = dateView.replace("EEEE", "  EEEE");
			}
        }
    }
}
