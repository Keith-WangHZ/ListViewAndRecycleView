/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.SystemClock;
import android.service.trust.TrustAgentService;
import android.support.v4.view.GestureDetectorCompat;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityContainer.SecurityCallback;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Base class for keyguard view.  {@link #reset} is where you should
 * reset the state of your view.  Use the {@link KeyguardViewCallback} via
 * {@link #getCallback()} to send information back (such as poking the wake lock,
 * or finishing the keyguard).
 *
 * Handles intercepting of media keys that still work when the keyguard is
 * showing.
 */
public class KeyguardHostView extends FrameLayout implements SecurityCallback {
	
	public static final int POSITION_NORMAL = 0;
	public static final int POSITION_LEFT = 1;
	public static final int POSITION_RIGHT = 2;

    public interface OnDismissAction {
        /**
         * @return true if the dismiss should be deferred
         */
        boolean onDismiss();
    }

    private AudioManager mAudioManager;
    private TelephonyManager mTelephonyManager = null;
    protected ViewMediatorCallback mViewMediatorCallback;
    protected LockPatternUtils mLockPatternUtils;
    private OnDismissAction mDismissAction;
    private Runnable mCancelAction;
    private int mCurrentPosition;
	private float mPositionY;

    private final KeyguardUpdateMonitorCallback mUpdateCallback =
            new KeyguardUpdateMonitorCallback() {

        @Override
        public void onUserSwitchComplete(int userId) {
            getSecurityContainer().showPrimarySecurityScreen(false /* turning off */);
        }

        @Override
        public void onTrustGrantedWithFlags(int flags, int userId) {
            if (userId != KeyguardUpdateMonitor.getCurrentUser()) return;
            if (!isAttachedToWindow()) return;
            boolean bouncerVisible = isVisibleToUser();
            boolean initiatedByUser =
                    (flags & TrustAgentService.FLAG_GRANT_TRUST_INITIATED_BY_USER) != 0;
            boolean dismissKeyguard =
                    (flags & TrustAgentService.FLAG_GRANT_TRUST_DISMISS_KEYGUARD) != 0;

            if (initiatedByUser || dismissKeyguard) {
                if (mViewMediatorCallback.isScreenOn() && (bouncerVisible || dismissKeyguard)) {
                    if (!bouncerVisible) {
                        // The trust agent dismissed the keyguard without the user proving
                        // that they are present (by swiping up to show the bouncer). That's fine if
                        // the user proved presence via some other way to the trust agent.
                        Log.i(TAG, "TrustAgent dismissed Keyguard.");
                    }
                    dismiss(false /* authenticated */);
                } else {
                    mViewMediatorCallback.playTrustedSound();
                }
            }
        }
    };

    // Whether the volume keys should be handled by keyguard. If true, then
    // they will be handled here for specific media types such as music, otherwise
    // the audio service will bring up the volume dialog.
    private static final boolean KEYGUARD_MANAGES_VOLUME = false;
    public static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardViewBase";

    private KeyguardSecurityContainer mSecurityContainer;
	private Runnable resetRunnables = new Runnable() {
		
		@Override
		public void run() {
			reset();
		}
	};
	private float mPositonY2;
	private float mSlidLength;
	private int mTrackingPointer;
	private float mInitPositionY;
	private float mLastSlidLength;
	private GestureDetectorCompat mDetector;
	private boolean mFlingReset = false;

    public KeyguardHostView(Context context) {
        this(context, null);
    }

    public KeyguardHostView(Context context, AttributeSet attrs) {
        super(context, attrs);
        KeyguardUpdateMonitor.getInstance(context).registerCallback(mUpdateCallback);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mViewMediatorCallback != null) {
            mViewMediatorCallback.keyguardDoneDrawing();
        }
    }

    /**
     * Sets an action to run when keyguard finishes.
     *
     * @param action
     */
    public void setOnDismissAction(OnDismissAction action, Runnable cancelAction) {
        if (mCancelAction != null) {
            mCancelAction.run();
            mCancelAction = null;
        }
        mDismissAction = action;
        mCancelAction = cancelAction;
    }

    public void cancelDismissAction() {
        setOnDismissAction(null, null);
    }

    @Override
    protected void onFinishInflate() {
    	super.onFinishInflate();
        mSecurityContainer =
                (KeyguardSecurityContainer) findViewById(R.id.keyguard_security_container);
        mDetector = new GestureDetectorCompat(mContext, new MyGestureListener()); 
        mLockPatternUtils = new LockPatternUtils(mContext);
        mSecurityContainer.setLockPatternUtils(mLockPatternUtils);
        mSecurityContainer.setSecurityCallback(this);
        mSecurityContainer.showPrimarySecurityScreen(false);
        // mSecurityContainer.updateSecurityViews(false /* not bouncing */);
    }

    @Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
    	mFlingReset  = mDetector.onTouchEvent(ev);
		super.onInterceptTouchEvent(ev);
		int pointerIndex = ev.findPointerIndex(mTrackingPointer);
		if (pointerIndex < 0) {
			pointerIndex = 0;
			mTrackingPointer = ev.getPointerId(pointerIndex);
		}
		final float y = ev.getY(pointerIndex);
		userActivity();
		switch (ev.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			mLastSlidLength = 0;
			mInitPositionY = ev.getY(pointerIndex);
			mPositionY = y;
			break;

		case MotionEvent.ACTION_MOVE:

			if (ev.getPointerCount() == 1) {
				mPositonY2 = ev.getY() + mLastSlidLength;
				mSlidLength = mPositonY2 - mPositionY;
				Log.d(TAG, "ev:positionY=" + mPositionY + ", mPositonY2="
						+ mPositonY2 + ", mLastSlidLength=" + mLastSlidLength
						+ ", mSlidLength=" + mSlidLength + "mTrackingPointer="
						+ mTrackingPointer);
				if (mSlidLength > 0
						&& getSecurityMode() != SecurityMode.Pattern) {
					mSecurityContainer.setTranslationY(mSlidLength);
				} else if (mSlidLength > 0
						&& getSecurityMode() == SecurityMode.Pattern) {
					if (mPositionY < dip2px(mContext, 250)) {
						mSecurityContainer.setTranslationY(mSlidLength);
					}
				}
			}
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			break;

		case MotionEvent.ACTION_POINTER_UP:
			if (DEBUG) Log.d(TAG, "ACTION_POINTER_UP");
			final int upPointer = ev.getPointerId(ev.getActionIndex());
			if (mTrackingPointer == upPointer) {
				// gesture is ongoing, find a new pointer to track
				final int newIndex = ev.getPointerId(0) != upPointer ? 0 : 1;
				final float newY = ev.getY(newIndex);
				if (DEBUG) Log.d(TAG, "mSlidLength=" + mSlidLength);
				mTrackingPointer = ev.getPointerId(newIndex);
				mPositionY = newY;
				mLastSlidLength = mSlidLength;
			}

			if (DEBUG) Log.d(TAG, "mTrackingPointer=" + mTrackingPointer);
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if(mFlingReset){
				mSecurityContainer.setTranslationY(0);
			}else{
				if (getSecurityMode() != SecurityMode.Pattern
						&& resetRunnables != null) {
					if(mSlidLength > dip2px(mContext, 25)){
						startResetAppearAnimation(mSlidLength, resetRunnables);
					}else{
						mSecurityContainer.setTranslationY(0);
					}
					if (DEBUG) Log.d(TAG, ":positionY=" + mPositionY + ", mSlidLength="
							+ mSlidLength + "mTrackingPointer=" + mTrackingPointer
							+ "mInitPositionY=" + mInitPositionY);
				} else if (getSecurityMode() == SecurityMode.Pattern
						&& resetRunnables != null) {
					if (mInitPositionY > dip2px(mContext, 260) && mSlidLength > dip2px(mContext, 350)) {
						startResetAppearAnimation(mSlidLength, resetRunnables);
					} else if (mInitPositionY < dip2px(mContext, 260) && mSlidLength > dip2px(mContext, 25)) {
						startResetAppearAnimation(mSlidLength, resetRunnables);
					}else{
						mSecurityContainer.setTranslationY(0);
					}
					if (DEBUG) Log.d(TAG, ":positionY=" + mPositionY + ", mSlidLength="
							+ mSlidLength + "mTrackingPointer=" + mTrackingPointer
							+ "mInitPositionY=" + mInitPositionY);
				}
				mSlidLength = 0;
			}
			break;
		default:
			break;
		}
		invalidate();
		return false;
	}
    
   @Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
	    mFlingReset  = mDetector.onTouchEvent(event);
		super.onTouchEvent(event);
		int pointerIndex = event.findPointerIndex(mTrackingPointer);
		if (pointerIndex < 0) {
			pointerIndex = 0;
			mTrackingPointer = event.getPointerId(pointerIndex);
		}
		final float y = event.getY(pointerIndex);
		userActivity();
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			mInitPositionY = event.getY(pointerIndex);
			mLastSlidLength = 0;
			if (DEBUG) Log.d(TAG, "ACTION_DOWN" + "mLastSlidLength" + mLastSlidLength);
			mPositionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG, "ACTION_MOVE");
			Log.d(TAG, "mTrackingPointer-MOVE=" + event.getPointerCount()
					+ ", " + mTrackingPointer + "event.getY()=" + event.getY());
			if (event.getPointerCount() == 1) {
				mPositonY2 = event.getY() + mLastSlidLength;
				mSlidLength = mPositonY2 - mPositionY;
				Log.d(TAG, "event:positionY=" + mPositionY + ", mPositonY2="
						+ mPositonY2 + ", mLastSlidLength=" + mLastSlidLength
						+ ", mSlidLength=" + mSlidLength + "mTrackingPointer="
						+ mTrackingPointer + "y=" + y);
				if (mSlidLength > 0
						&& getSecurityMode() != SecurityMode.Pattern) {
					mSecurityContainer.setTranslationY(mSlidLength);
				} else if (mSlidLength > 0
						&& getSecurityMode() == SecurityMode.Pattern) {
					if (mPositionY < dip2px(mContext, 250)) {
						mSecurityContainer.setTranslationY(mSlidLength);
					}
				}
			}
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			if (DEBUG) Log.d(TAG, "ACTION_POINTER_DOWN");
			break;

		case MotionEvent.ACTION_POINTER_UP:
			if (DEBUG) Log.d(TAG, "ACTION_POINTER_UP");
			final int upPointer = event.getPointerId(event.getActionIndex());
			if (mTrackingPointer == upPointer) {
				final int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
				final float newY = event.getY(newIndex);
				if (DEBUG) Log.d(TAG, "mSlidLength=" + mSlidLength);
				mTrackingPointer = event.getPointerId(newIndex);
				mPositionY = newY;
				mLastSlidLength = mSlidLength;
			}
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if (DEBUG) Log.d(TAG, "ACTION_UP");
			if(mFlingReset){
				mSecurityContainer.setTranslationY(0);
			}else{
				if (getSecurityMode() != SecurityMode.Pattern
						&& resetRunnables != null) {
					if(mSlidLength > dip2px(mContext, 25)){
						startResetAppearAnimation(mSlidLength, resetRunnables);
					}else{
						mSecurityContainer.setTranslationY(0);
					}
					if (DEBUG) Log.d(TAG, "event:positionY2=" + mPositionY + ", mSlidLength="
							+ mSlidLength + "mTrackingPointer=" + mTrackingPointer
							+ "mInitPositionY=" + mInitPositionY);
				} else if (getSecurityMode() == SecurityMode.Pattern
						&& resetRunnables != null) {
					if (mInitPositionY > dip2px(mContext, 260) && mSlidLength > dip2px(mContext, 300)) {
						startResetAppearAnimation(mSlidLength, resetRunnables);
					} else if (mInitPositionY < dip2px(mContext, 260) && mSlidLength > dip2px(mContext, 25)) {
						startResetAppearAnimation(mSlidLength, resetRunnables);
					}else{
						mSecurityContainer.setTranslationY(0);
					}
					if (DEBUG) Log.d(TAG, "event:positionY2=" + mPositionY + ", mSlidLength="
							+ mSlidLength + "mTrackingPointer=" + mTrackingPointer
							+ "mInitPositionY=" + mInitPositionY);
				}
				mSlidLength = 0;
			}
			break;
		default:
			break;
		}
		invalidate();
		return true;
	}
    /**
     * Called when the view needs to be shown.
     */
    public void showPrimarySecurityScreen() {
        if (DEBUG) Log.d(TAG, "show()");
        mSecurityContainer.showPrimarySecurityScreen(false);
    }

    /**
     * Show a string explaining why the security view needs to be solved.
     *
     * @param reason a flag indicating which string should be shown, see
     *               {@link KeyguardSecurityView#PROMPT_REASON_NONE},
     *               {@link KeyguardSecurityView#PROMPT_REASON_RESTART} and
     *               {@link KeyguardSecurityView#PROMPT_REASON_TIMEOUT}.
     */
    public void showPromptReason(int reason) {
        mSecurityContainer.showPromptReason(reason);
    }

    public void showMessage(String message, int color) {
        mSecurityContainer.showMessage(message, color);
    }

    /**
     *  Dismisses the keyguard by going to the next screen or making it gone.
     *
     *  @return True if the keyguard is done.
     */
    public boolean dismiss() {
        return dismiss(false);
    }

    public boolean handleBackKey() {
        if (mSecurityContainer.getCurrentSecuritySelection() != SecurityMode.None) {
            mSecurityContainer.dismiss(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.getText().add(mSecurityContainer.getCurrentSecurityModeContentDescription());
            return true;
        } else {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
    }

    protected KeyguardSecurityContainer getSecurityContainer() {
        return mSecurityContainer;
    }

    @Override
    public boolean dismiss(boolean authenticated) {
        return mSecurityContainer.showNextSecurityScreenOrFinish(authenticated);
    }

    /**
     * Authentication has happened and it's time to dismiss keyguard. This function
     * should clean up and inform KeyguardViewMediator.
     *
     * @param strongAuth whether the user has authenticated with strong authentication like
     *                   pattern, password or PIN but not by trust agents or fingerprint
     */
    @Override
    public void finish(boolean strongAuth) {
        // If there's a pending runnable because the user interacted with a widget
        // and we're leaving keyguard, then run it.
        boolean deferKeyguardDone = false;
        if (mDismissAction != null) {
            deferKeyguardDone = mDismissAction.onDismiss();
            mDismissAction = null;
            mCancelAction = null;
        }
        if (mViewMediatorCallback != null) {
            if (deferKeyguardDone) {
                mViewMediatorCallback.keyguardDonePending(strongAuth);
            } else {
                mViewMediatorCallback.keyguardDone(strongAuth);
            }
        }
        mCurrentPosition = POSITION_NORMAL;
    }

    @Override
    public void reset() {
        mViewMediatorCallback.resetKeyguard(false);
    }

    @Override
    public void onSecurityModeChanged(SecurityMode securityMode, boolean needsInput) {
        if (mViewMediatorCallback != null) {
            mViewMediatorCallback.setNeedsInput(needsInput);
        }
    }

    public void userActivity() {
        if (mViewMediatorCallback != null) {
            mViewMediatorCallback.userActivity();
        }
    }

    /**
     * Called when the Keyguard is not actively shown anymore on the screen.
     */
    public void onPause() {
        if (DEBUG) Log.d(TAG, String.format("screen off, instance %s at %s",
                Integer.toHexString(hashCode()), SystemClock.uptimeMillis()));
        mSecurityContainer.showPrimarySecurityScreen(true);
        mSecurityContainer.onPause();
        clearFocus();
    }

    /**
     * Called when the Keyguard is actively shown on the screen.
     */
    public void onResume() {
        if (DEBUG) Log.d(TAG, "screen on, instance " + Integer.toHexString(hashCode()));
        mSecurityContainer.onResume(KeyguardSecurityView.SCREEN_ON);
        requestFocus();
    }

    /**
     * Starts the animation when the Keyguard gets shown.
     */
    public void startAppearAnimation() {
        mSecurityContainer.startAppearAnimation();
    }
    
    public void startResetAppearAnimation(float startTranslationY, Runnable resetRunnable) {
    	if(resetRunnable != null){
    		mSecurityContainer.startResetAppearAnimation(startTranslationY, resetRunnable);
    	}
    }

    public void startDisappearAnimation(Runnable finishRunnable) {
        if (!mSecurityContainer.startDisappearAnimation(finishRunnable) && finishRunnable != null) {
            finishRunnable.run();
        }
    }

    /**
     * Called before this view is being removed.
     */
    public void cleanUp() {
        getSecurityContainer().onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (interceptMediaKey(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Allows the media keys to work when the keyguard is showing.
     * The media keys should be of no interest to the actual keyguard view(s),
     * so intercepting them here should not be of any harm.
     * @param event The key event
     * @return whether the event was consumed as a media key.
     */
    public boolean interceptMediaKey(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    /* Suppress PLAY/PAUSE toggle when phone is ringing or
                     * in-call to avoid music playback */
                    if (mTelephonyManager == null) {
                        mTelephonyManager = (TelephonyManager) getContext().getSystemService(
                                Context.TELEPHONY_SERVICE);
                    }
                    if (mTelephonyManager != null &&
                            mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                        return true;  // suppress key event
                    }
                case KeyEvent.KEYCODE_MUTE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                case KeyEvent.KEYCODE_MEDIA_RECORD:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK: {
                    handleMediaKeyEvent(event);
                    return true;
                }

                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_MUTE: {
                    if (KEYGUARD_MANAGES_VOLUME) {
                        synchronized (this) {
                            if (mAudioManager == null) {
                                mAudioManager = (AudioManager) getContext().getSystemService(
                                        Context.AUDIO_SERVICE);
                            }
                        }
                        // Volume buttons should only function for music (local or remote).
                        // TODO: Actually handle MUTE.
                        mAudioManager.adjustSuggestedStreamVolume(
                                keyCode == KeyEvent.KEYCODE_VOLUME_UP
                                        ? AudioManager.ADJUST_RAISE
                                        : AudioManager.ADJUST_LOWER /* direction */,
                                AudioManager.STREAM_MUSIC /* stream */, 0 /* flags */);
                        // Don't execute default volume behavior
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MUTE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                case KeyEvent.KEYCODE_MEDIA_RECORD:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK: {
                    handleMediaKeyEvent(event);
                    return true;
                }
            }
        }
        return false;
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        synchronized (this) {
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) getContext().getSystemService(
                        Context.AUDIO_SERVICE);
            }
        }
        mAudioManager.dispatchMediaKeyEvent(keyEvent);
    }

    @Override
    public void dispatchSystemUiVisibilityChanged(int visibility) {
        super.dispatchSystemUiVisibilityChanged(visibility);

        if (!(mContext instanceof Activity)) {
            setSystemUiVisibility(STATUS_BAR_DISABLE_BACK);
        }
    }

    /**
     * In general, we enable unlocking the insecure keyguard with the menu key. However, there are
     * some cases where we wish to disable it, notably when the menu button placement or technology
     * is prone to false positives.
     *
     * @return true if the menu key should be enabled
     */
    private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
    public boolean shouldEnableMenuKey() {
        final Resources res = getResources();
        final boolean configDisabled = res.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
        final boolean isTestHarness = ActivityManager.isRunningInTestHarness();
        final boolean fileOverride = (new File(ENABLE_MENU_KEY_FILE)).exists();
        return !configDisabled || isTestHarness || fileOverride;
    }

    public void setViewMediatorCallback(ViewMediatorCallback viewMediatorCallback) {
        mViewMediatorCallback = viewMediatorCallback;
        // Update ViewMediator with the current input method requirements
        mViewMediatorCallback.setNeedsInput(mSecurityContainer.needsInput());
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
        mSecurityContainer.setLockPatternUtils(utils);
    }

    public SecurityMode getSecurityMode() {
        return mSecurityContainer.getSecurityMode();
    }

    public SecurityMode getCurrentSecurityMode() {
        return mSecurityContainer.getCurrentSecurityMode();
    }
    
    public void setCurrentPosition(int position) {
		View patternView;
		View viewChildView;
		if (position != POSITION_LEFT && position != POSITION_NORMAL
				&& position != POSITION_RIGHT) {
			position = POSITION_NORMAL;
		}

		mCurrentPosition = position;
		if (DEBUG) Log.d(TAG, "securityMode="+getSecurityMode().toString());
		if (getSecurityMode() == SecurityMode.Pattern) {
			patternView = (View) findViewById(R.id.keyguard_pattern_view);
			LinearLayout navigationHeight = (LinearLayout)patternView.findViewById(R.id.navigateHeight);
			if(null != navigationHeight){
				if(!checkDeviceHasNavigationBar(mContext)){
					navigationHeight.setVisibility(View.VISIBLE);
				}else{
					navigationHeight.setVisibility(View.GONE);
				}
			}
			viewChildView = (View) patternView.findViewById(R.id.patternContainer);
			LinearLayout rightImageView = (LinearLayout) patternView
					.findViewById(R.id.right);
			LinearLayout leftImageView = (LinearLayout) patternView
					.findViewById(R.id.left);
			LinearLayout childView = (LinearLayout) patternView
					.findViewById(R.id.childView);

			if (patternView != null && viewChildView != null) {
				LinearLayout.LayoutParams childViewParams = (android.widget.LinearLayout.LayoutParams) viewChildView
						.getLayoutParams();
				LinearLayout.LayoutParams rightViewParams = (android.widget.LinearLayout.LayoutParams) rightImageView
						.getLayoutParams();
				LinearLayout.LayoutParams leftViewParams = (android.widget.LinearLayout.LayoutParams) leftImageView
						.getLayoutParams();
				
				if (position == POSITION_LEFT) {
					rightImageView.setVisibility(View.VISIBLE);
					leftImageView.setVisibility(View.GONE);
					childViewParams.setMargins(dip2px(mContext, 35),
							dip2px(mContext, 30), dip2px(mContext, -12),
							dip2px(mContext, 0));
					childView.setTranslationX(dip2px(mContext, -48));
//					rightViewParams.setMarginsRelative(dip2px(mContext, -49), 0, 0, 0);
				} else if (position == POSITION_RIGHT) {
					rightImageView.setVisibility(View.GONE);
					leftImageView.setVisibility(View.VISIBLE);
					childViewParams.setMargins(dip2px(mContext, -14),
							dip2px(mContext, 30), dip2px(mContext, 35),
							dip2px(mContext, 0));
					childView.setTranslationX(dip2px(mContext, 52));
//					leftViewParams.setMarginsRelative(dip2px(mContext, 0), 0, 0, 0);
//					childViewParams.setMarginsRelative(dip2px(mContext, 0), 0, 0, 0);
				} else {
					rightImageView.setVisibility(View.GONE);
					leftImageView.setVisibility(View.GONE);
					childViewParams.setMargins(dip2px(mContext, 35),
							dip2px(mContext, 30), dip2px(mContext, 35),
							dip2px(mContext, 0));
					childView.setTranslationX(0);
				}
			}
		} else if (getSecurityMode() == SecurityMode.PIN) {
			if (!KeyguardUpdateMonitor.isSupportOversea()) {
				patternView = (View) findViewById(R.id.keyguard_pin_view);
				LinearLayout navigationHeight = (LinearLayout)patternView.findViewById(R.id.navigateHeight);
				if(null != navigationHeight){
					if(!checkDeviceHasNavigationBar(mContext)){
						navigationHeight.setVisibility(View.VISIBLE);
					}else{
						navigationHeight.setVisibility(View.GONE);
					}
				}
				viewChildView = (View) patternView.findViewById(R.id.childView);
				LinearLayout rightImageView = (LinearLayout) findViewById(R.id.right);
				LinearLayout leftTextView = (LinearLayout) findViewById(R.id.left);
				if (patternView != null && viewChildView != null) {
					if (position == POSITION_LEFT) {
						rightImageView.setVisibility(View.VISIBLE);
						leftTextView.setVisibility(View.GONE);
						viewChildView.setTranslationX(dip2px(mContext, -28));

					} else if (position == POSITION_RIGHT) {
						rightImageView.setVisibility(View.GONE);
						leftTextView.setVisibility(View.VISIBLE);
						viewChildView.setTranslationX(dip2px(mContext, 32));

					} else {
						rightImageView.setVisibility(View.GONE);
						leftTextView.setVisibility(View.GONE);
						viewChildView.setTranslationX(0);
					}
				}
			} else {
				patternView = (View) findViewById(R.id.keyguard_pin_view);
				LinearLayout navigationHeight = (LinearLayout)patternView.findViewById(R.id.navigateHeight);
				if(null != navigationHeight){
					if(!checkDeviceHasNavigationBar(mContext)){
						navigationHeight.setVisibility(View.VISIBLE);
					}else{
						navigationHeight.setVisibility(View.GONE);
					}
				}
				viewChildView = (View) patternView.findViewById(R.id.childViewOversea);
				LinearLayout rightImageView = (LinearLayout) findViewById(R.id.rightOversea);
				LinearLayout leftImageView = (LinearLayout) findViewById(R.id.leftOversea);
				if (patternView != null && viewChildView != null) {
					if (position == POSITION_LEFT) {
						rightImageView.setVisibility(View.VISIBLE);
						leftImageView.setVisibility(View.GONE);
						viewChildView.setTranslationX(dip2px(mContext, -23));
					} else if (position == POSITION_RIGHT) {
						rightImageView.setVisibility(View.GONE);
						leftImageView.setVisibility(View.VISIBLE);
						viewChildView.setTranslationX(dip2px(mContext, 25));
					} else {
						rightImageView.setVisibility(View.GONE);
						leftImageView.setVisibility(View.GONE);
						viewChildView.setTranslationX(0);
					}
				}
			}
		}
	}
    
    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }  
		
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {  
        private static final String DEBUG_TAG = "Gestures";   
          
        @Override  
        public boolean onDown(MotionEvent event) {   
            return true;  
        }  
  
        @Override  
        public boolean onFling(MotionEvent event1, MotionEvent event2,   
                float velocityX, float velocityY) {  
            Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString()+", velocityY="+velocityY);  
            if(velocityY<-2000){
            	return true;
            }else{
            	return false;
            }
        }  
    } 
    
    public static boolean checkDeviceHasNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
       return hasNavigationBar;
    }
}
