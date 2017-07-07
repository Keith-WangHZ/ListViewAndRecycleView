package com.android.covermode;

import java.util.List;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.RequestThrottledException;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class StarryPatternView extends LinearLayout {

	private Context mContext;
	private static final String TAG = "StarryPatternVieww";
	private static final boolean DEBUG = false;

	private boolean mEnableFallback;
	private int mFailedPatternAttemptsSinceLastTimeout = 0;
	private int mTotalFailedPatternAttempts = 0;
	private CountDownTimer mCountdownTimer = null;
	private LockPatternUtils mLockPatternUtils;
	private LockPatternView mLockPatternView;
	private SecurityMessageDisplay mSecurityMessageDisplay;
	private static final int PATTERN_CLEAR_TIMEOUT_MS = 100;

	// how long we stay awake after each key beyond
	// MIN_PATTERN_BEFORE_POKE_WAKELOCK
	private static final int UNLOCK_PATTERN_WAKE_INTERVAL_MS = 7000;

	// how long we stay awake after the user hits the first dot.
	private static final int UNLOCK_PATTERN_WAKE_INTERVAL_FIRST_DOTS_MS = 2000;

	// how many cells the user has to cross before we poke the wakelock
	private static final int MIN_PATTERN_BEFORE_POKE_WAKELOCK = 2;

	private DismissAction dismiss = null;

	public StarryPatternView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;
	}

	enum FooterMode {
		Normal, ForgotLockPattern, VerifyUnlocked
	}

	/* package */public interface DismissAction {
		/* returns true if the dismiss should be deferred */
		boolean Dismiss();
	}

	public void setDismissAction(DismissAction mDismissAction) {
		dismiss = mDismissAction;
	}

	public void setStealMode(boolean stealMode) {
		if (mLockPatternView != null) {
			mLockPatternView.setInStealthMode(stealMode);
		}
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mLockPatternUtils = mLockPatternUtils == null ? new LockPatternUtils(mContext) : mLockPatternUtils;
		mLockPatternView = (LockPatternView) this.findViewById(R.id.lockPatternView);
		mLockPatternView.setSaveEnabled(false);
		mLockPatternView.setFocusable(false);
		mLockPatternView.setOnPatternListener(new UnlockPatternListener());
		// stealth mode will be the same for the life of this screen
		mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));

		// vibrate mode will be the same for the life of this screen
		mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
		mSecurityMessageDisplay = new StarryMessageArea.Helper(this);
		mSecurityMessageDisplay.setDefaultMessage(R.string.kg_pattern_instructions);
		displayDefaultSecurityMessage();
	}

	private Runnable mCancelPatternRunnable = new Runnable() {
		public void run() {
			mLockPatternView.clearPattern();
		}
	};

	private class UnlockPatternListener implements LockPatternView.OnPatternListener {

		public void onPatternStart() {
			mLockPatternView.removeCallbacks(mCancelPatternRunnable);
		}

		public void onPatternCleared() {
		}

		public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
			// To guard against accidental poking of the wakelock, look for
			// the user actually trying to draw a pattern of some minimal
			// length.
			if (pattern.size() > MIN_PATTERN_BEFORE_POKE_WAKELOCK) {
				// mCallback.userActivity(UNLOCK_PATTERN_WAKE_INTERVAL_MS);
				// //lxb cancel
			} else {
				// Give just a little extra time if they hit one of the first
				// few dots
				// mCallback.userActivity(UNLOCK_PATTERN_WAKE_INTERVAL_FIRST_DOTS_MS);
				// //lxb cancel
			}
		}

		public void onPatternDetected(List<LockPatternView.Cell> pattern) {
			try {
				if (mLockPatternUtils.checkPattern(pattern,KeyguardUpdateMonitor.getCurrentUser())) {
					reportSuccessfulUnlockAttempt(); // lxb cancel
					mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
					mTotalFailedPatternAttempts = 0;
					// mCallback.dismiss(true); // lxb cancel
					dismiss.Dismiss();
					mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
					dismiss = null;
					Log.v(TAG, "onPatternDetected");
				} else {
					if (pattern.size() > MIN_PATTERN_BEFORE_POKE_WAKELOCK) {
						// mCallback.userActivity(UNLOCK_PATTERN_WAKE_INTERVAL_MS);
						// //lxb cancel
					}
					mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
					if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
						mTotalFailedPatternAttempts++;
						mFailedPatternAttemptsSinceLastTimeout++;
						// mCallback.reportFailedUnlockAttempt(); // lxb cancel
					}
					if (mFailedPatternAttemptsSinceLastTimeout >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_WIPE_GRACE) {
						long deadline = mLockPatternUtils.setLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser(),3000);
						handleAttemptLockout(deadline);
					} else {
						mSecurityMessageDisplay.setMessage(R.string.kg_wrong_pattern, true); // lxb
																								// cncel
						mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
					}
				}
			} catch (RequestThrottledException e) {
				Log.v("StarryPatternView", "check password error : " + e);
			}
		}
	}

	private void handleAttemptLockout(long elapsedRealtimeDeadline) {
		mLockPatternView.clearPattern();
		mLockPatternView.setEnabled(false);
		final long elapsedRealtime = SystemClock.elapsedRealtime();
		mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				final int secondsRemaining = (int) (millisUntilFinished / 1000);
				mSecurityMessageDisplay.setMessage(R.string.kg_too_many_failed_attempts_countdown, true, secondsRemaining);
			}

			@Override
			public void onFinish() {
				mLockPatternView.setEnabled(true);
				displayDefaultSecurityMessage();
				// TODO mUnlockIcon.setVisibility(View.VISIBLE);
				mFailedPatternAttemptsSinceLastTimeout = 0;
			}

		}.start();
	}

	public void reportSuccessfulUnlockAttempt() {
		mFailedPatternAttemptsSinceLastTimeout = 0;
		mTotalFailedPatternAttempts = 0;
		mLockPatternUtils.reportSuccessfulPasswordAttempt(KeyguardUpdateMonitor.getCurrentUser());
	}

	private void displayDefaultSecurityMessage() {
		mSecurityMessageDisplay.setMessage(R.string.kg_pattern_instructions, false);
	}

}
