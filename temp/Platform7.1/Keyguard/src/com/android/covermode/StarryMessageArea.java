/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import com.android.keyguard.R;

/***
 * Manages a number of views inside of the given layout. See below for a list of
 * widgets.
 */

class StarryMessageArea extends TextView {
	/** Handler token posted with accessibility announcement runnables. */
	private static final Object ANNOUNCE_TOKEN = new Object();

	/**
	 * Delay before speaking an accessibility announcement. Used to prevent
	 * lift-to-type from interrupting itself.
	 */
	private static final long ANNOUNCEMENT_DELAY = 250;

	static final int CHARGING_ICON = 0; // R.drawable.ic_lock_idle_charging; //0
	static final int BATTERY_LOW_ICON = 0; // R.drawable.ic_lock_idle_low_battery;//0

	static final int SECURITY_MESSAGE_DURATION = 5000;
	protected static final int FADE_DURATION = 750;

	private static final String TAG = "KeyguardPasswordMessageArea";

	// is the bouncer up?
	boolean mShowingBouncer = false;

	// Timeout before we reset the message to show charging/owner info
	long mTimeout = SECURITY_MESSAGE_DURATION;

	// Shadowed text values
	protected boolean mBatteryCharged;
	protected boolean mBatteryIsLow;

	private Handler mHandler;

	CharSequence mMessage;
	CharSequence mdefaultMessage;
	boolean mShowingMessage;
	Runnable mClearMessageRunnable = new Runnable() {
		@Override
		public void run() {
			mMessage = null;
			mShowingMessage = false;
			if (mShowingBouncer) {
				hideMessage(FADE_DURATION, true);
			} else {
				update();
			}
		}
	};

	public static class Helper implements SecurityMessageDisplay {
		StarryMessageArea mMessageArea;

		Helper(View v) {
			mMessageArea = (StarryMessageArea) v.findViewById(R.id.keyguard_message_area);
		}

		public StarryMessageArea getMessageArea(StarryMessageArea v) {
			return mMessageArea;
		}

		public void setMessage(CharSequence msg, boolean important) {
			if (mMessageArea == null) {
				return;
			}
			if (!TextUtils.isEmpty(msg)) {
				mMessageArea.mMessage = msg;
				mMessageArea.securityMessageChanged();
			}
		}

		public void setMessage(int resId, boolean important) {
			if (mMessageArea == null) {
				return;
			}
			if (resId != 0) {
				mMessageArea.mMessage = mMessageArea.getContext().getResources().getText(resId);
				mMessageArea.securityMessageChanged();
			}
		}

		public void setMessage(int resId, boolean important, Object... formatArgs) {
			if (mMessageArea == null) {
				return;
			}
			if (resId != 0) {
				mMessageArea.mMessage = mMessageArea.getContext().getString(resId, formatArgs);
				mMessageArea.securityMessageChanged();
			}
		}

		public void setDefaultMessage(int resId) {
			if (mMessageArea == null) {
				return;
			}
			if (resId != 0) {
				mMessageArea.mdefaultMessage = mMessageArea.getContext().getString(resId);
			}
		}

		@Override
		public void showBouncer(int duration) {
			if (mMessageArea == null) {
				return;
			}
			mMessageArea.hideMessage(duration, false);
			mMessageArea.mShowingBouncer = true;
		}

		@Override
		public void hideBouncer(int duration) {
			if (mMessageArea == null) {
				return;
			}
			mMessageArea.showMessage(duration);
			mMessageArea.mShowingBouncer = false;
		}

		@Override
		public void setTimeout(int timeoutMs) {
			if (mMessageArea == null) {
				return;
			}
			mMessageArea.mTimeout = timeoutMs;
		}
	}

	public StarryMessageArea(Context context) {
		this(context, null);
	}

	public StarryMessageArea(Context context, AttributeSet attrs) {
		super(context, attrs);

		mHandler = new Handler(Looper.myLooper());

		update();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// final boolean screenOn =
		// KeyguardUpdateMonitor.getInstance(mContext).isScreenOn();
		// setSelected(screenOn); // This is required to ensure marquee works
	}

	public void securityMessageChanged() {
		setAlpha(1f);
		mShowingMessage = true;
		update();
		mHandler.removeCallbacks(mClearMessageRunnable);
		if (mTimeout > 0) {
			mHandler.postDelayed(mClearMessageRunnable, mTimeout);
		}
		mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
		mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, (SystemClock.uptimeMillis() + ANNOUNCEMENT_DELAY));
	}

	/**
	 * Update the status lines based on these rules: AlarmStatus: Alarm state
	 * always gets it's own line. Status1 is shared between help, battery status
	 * and generic unlock instructions, prioritized in that order.
	 * 
	 * @param showStatusLines
	 *            status lines are shown if true
	 */
	void update() {
		// MutableInt icon = new MutableInt(0);
		// CharSequence string = getPriorityTextMessage(icon);
		CharSequence status = getCurrentMessage();
		// setCompoundDrawablesWithIntrinsicBounds(icon.value, 0, 0, 0);
		setText(status);
	}

	CharSequence getCurrentMessage() {
		Log.v(TAG, "mdefaultMessage =" + mdefaultMessage + " mMessage =" + mMessage);
		return mShowingMessage ? mMessage : mdefaultMessage;
	}

	private void hideMessage(int duration, boolean thenUpdate) {
		if (duration > 0) {
			Animator anim = ObjectAnimator.ofFloat(this, "alpha", 0f);
			anim.setDuration(duration);
			if (thenUpdate) {
				anim.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						update();
					}
				});
			}
			anim.start();
		} else {
			setAlpha(0f);
			if (thenUpdate) {
				update();
			}
		}
	}

	private void showMessage(int duration) {
		if (duration > 0) {
			Animator anim = ObjectAnimator.ofFloat(this, "alpha", 1f);
			anim.setDuration(duration);
			anim.start();
		} else {
			setAlpha(1f);
		}
	}

	/**
	 * Runnable used to delay accessibility announcements.
	 */
	private static class AnnounceRunnable implements Runnable {
		private final WeakReference<View> mHost;
		private final CharSequence mTextToAnnounce;

		public AnnounceRunnable(View host, CharSequence textToAnnounce) {
			mHost = new WeakReference<View>(host);
			mTextToAnnounce = textToAnnounce;
		}

		@Override
		public void run() {
			final View host = mHost.get();
			if (host != null) {
				host.announceForAccessibility(mTextToAnnounce);
			}
		}
	}

}
