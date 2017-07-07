/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

package com.android.keyguard;

import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

/**
 * Displays a PIN pad for unlocking.
 */
public class CoolUIPINView extends KeyguardPinBasedInputView implements CoolUIEditView.EditViewCallback {

	private static final String TAG = "CoolUIPINView";
	private final AppearAnimationUtils mAppearAnimationUtils;
	private final DisappearAnimationUtils mDisappearAnimationUtils;
	private ViewGroup mKeyguardBouncerFrame;
	private ViewGroup mRow0;
	private ViewGroup mRow1;
	private ViewGroup mRow2;
	private ViewGroup mRow3;
	private View mDivider;
	private int mDisappearYTranslation;
	private View[][] mViews;
	private View[][] mViewsReset;
	private TextView mDelAndCancelTextView;
	protected CoolUIEditView mEditView;
	private View mViewChildView;
	private LinearLayout mRightImageView;
	private LinearLayout mLeftImageView;
	private ImageView mViewLeft;
	private ImageView mViewRight;

	public CoolUIPINView(Context context) {
		this(context, null);
	}

	public CoolUIPINView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mAppearAnimationUtils = new AppearAnimationUtils(context);
		mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125, 0.6f /* translationScale */, 0.6f /* delayScale */,
				AnimationUtils.loadInterpolator(mContext, android.R.interpolator.fast_out_linear_in));
		mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
	}

	protected void resetState() {
		showUsabilityHint();
		mEditView.setEnabled(true);
	}
	
	@Override
	protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
		return mEditView.requestFocus(direction, previouslyFocusedRect);
	}

	@Override
	protected int getPasswordTextViewId() {
		return -1;
	}

	@Override
	protected void setPasswordEntryEnabled(boolean enabled) {
		mEditView.setEnabled(enabled);
	}

	@Override
	protected void setPasswordEntryInputEnabled(boolean enabled) {
		mEditView.setEnabled(enabled);
	}

	@Override
	protected void resetPasswordText(boolean animate, boolean announce) {
		if (animate) {
			mEditView.execuErrorAnimation();
		}
		postDelayed(new Runnable() {
			@Override
			public void run() {
				mEditView.deleteAllNums();
			}
		}, 500);
	}

	@Override
	public void reset() {
		mEditView.requestFocus();
        // start fresh
        resetPasswordText(false /* animate */, true);
        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline(
                KeyguardUpdateMonitor.getCurrentUser());
        Log.d(TAG, "deadline = " + deadline);
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
	}

	@Override
	protected String getPasswordText() {
		return mEditView.getStringNums();
	}

	@Override
	protected void onFinishInflate() {
        mButton0 = findViewById(R.id.key0);
        mButton1 = findViewById(R.id.key1);
        mButton2 = findViewById(R.id.key2);
        mButton3 = findViewById(R.id.key3);
        mButton4 = findViewById(R.id.key4);
        mButton5 = findViewById(R.id.key5);
        mButton6 = findViewById(R.id.key6);
        mButton7 = findViewById(R.id.key7);
        mButton8 = findViewById(R.id.key8);
        mButton9 = findViewById(R.id.key9);    

        mLockPatternUtils = new LockPatternUtils(mContext);
        mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);

        EmergencyButton button = (EmergencyButton) findViewById(R.id.emergency_call_button);
        if (button != null) {
            button.setCallback(this);
        }
        mLeftImageView = (LinearLayout)findViewById(R.id.left);
        mRightImageView = (LinearLayout)findViewById(R.id.right);
        mViewChildView = (View)findViewById(R.id.childView);
        mViewLeft = (ImageView)findViewById(R.id.leftText);
        mViewRight = (ImageView)findViewById(R.id.rightText);
      
		mKeyguardBouncerFrame = (ViewGroup) findViewById(R.id.keyguard_bouncer_frame);
		if (null!=mKeyguardBouncerFrame && null!=mViewLeft&&null!=mViewChildView&&null!=mViewRight) {
			mViewLeft.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mRightImageView.setVisibility(View.GONE);
					mLeftImageView.setVisibility(View.GONE);
					final TranslateAnimation animationLeft = getTranslateAnimation(mViewChildView, 0, dip2px(mContext, -32), 0, 0);
					mViewChildView.setAnimation(animationLeft);
					animationLeft.startNow();
				
				}
			});
			
			mViewRight.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mLeftImageView.setVisibility(View.GONE);
					mRightImageView.setVisibility(View.GONE);
					final TranslateAnimation animationRight = getTranslateAnimation(mViewChildView, 0, dip2px(mContext, 28), 0, 0);
					mViewChildView.setAnimation(animationRight);
					animationRight.startNow();
				}
			});
		}
		
//		mRow0 = (ViewGroup) findViewById(R.id.row0);
		mRow1 = (ViewGroup) findViewById(R.id.row1);
		mRow2 = (ViewGroup) findViewById(R.id.row2);
		mRow3 = (ViewGroup) findViewById(R.id.row3);
		mDivider = findViewById(R.id.divider);
		mEditView = (CoolUIEditView) findViewById(R.id.cooluipinEntry);
		mDelAndCancelTextView = (TextView) findViewById(R.id.key_coc);
		mViews = new View[][] {
				new View[] { findViewById(R.id.keyguard_message_area) },
				new View[] { findViewById(R.id.cooluipinEntry) },
				new View[] { mRow0, null, null },
				new View[] { findViewById(R.id.key1), findViewById(R.id.key2),
						findViewById(R.id.key3) },
				new View[] { findViewById(R.id.key4), findViewById(R.id.key5),
						findViewById(R.id.key6) },
				new View[]{mViewLeft},
				new View[]{mViewRight},
				new View[] { findViewById(R.id.key7), findViewById(R.id.key8),
						findViewById(R.id.key9) },
				new View[] { mEcaView, findViewById(R.id.key0),
						mDelAndCancelTextView } };
		mViewsReset = new View[][] {
				new View[] { mRow0, null, null },
				new View[] { findViewById(R.id.key1), findViewById(R.id.key2),
						findViewById(R.id.key3) },
				new View[] { findViewById(R.id.key4), findViewById(R.id.key5),
						findViewById(R.id.key6) },
				new View[]{mViewLeft},
				new View[]{mViewRight},
				new View[] { findViewById(R.id.key7), findViewById(R.id.key8),
						findViewById(R.id.key9) },
				new View[] { mEcaView, findViewById(R.id.key0),
						mDelAndCancelTextView } };
		mEditView.setEditViewCallback(this);
//		mSecurityMessageDisplay.setTimeout(3000);
		mSecurityMessageDisplay.setTimeout(0);
		mDelAndCancelTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mEditView.getNumsLength() == 0) {
					postDelayed(new Runnable() {
						@Override
						public void run() {
							mCallback.reset();
						}
					}, 32);
				} else {
					mEditView.deleteNums();
				}
			}
		});
	}

	@Override
	public void showUsabilityHint() {
		int msgId = mFingerDetectionRunning ? R.string.kg_pin_finger_config_enabled_message
				: R.string.kg_password_instructions;
		mSecurityMessageDisplay.setDefaultMessage(msgId, true);
	}

	@Override
	public int getWrongPasswordStringId() {
		return R.string.kg_wrong_password;
	}

	@Override
	public void startAppearAnimation() {
		enableClipping(false);
		setAlpha(1f);
		setTranslationY(mAppearAnimationUtils.getStartTranslation());
		animate().setDuration(500).setInterpolator(mAppearAnimationUtils.getInterpolator()).translationY(0);
		mAppearAnimationUtils.startAnimation2d(mViews, new Runnable() {
			@Override
			public void run() {
				enableClipping(true);
			}
		});
	}

	@Override
	public void startResetAppearAnimation(float startTranslationY, final Runnable resetRunnable) {
		enableClipping(false);
//		setTranslationY(0);
		setTranslationY(startTranslationY);
		animate().setDuration(280).setInterpolator(mDisappearAnimationUtils.getInterpolator()).translationY(getHeight()*2 - mDisappearYTranslation);
		mDisappearAnimationUtils.startAnimation2d(mViewsReset, new Runnable() {
			@Override
			public void run() {
				enableClipping(true);
				if (resetRunnable != null) {
					resetRunnable.run();
				}
			}
		});
	}
	
	@Override
	public boolean startDisappearAnimation(final Runnable finishRunnable) {
		enableClipping(false);
		setTranslationY(0);
		animate().setDuration(280).setInterpolator(mDisappearAnimationUtils.getInterpolator()).translationY(mDisappearYTranslation);
		mDisappearAnimationUtils.startAnimation2d(mViews, new Runnable() {
			@Override
			public void run() {
				enableClipping(true);
				if (finishRunnable != null) {
					finishRunnable.run();
				}
			}
		});
		return true;
	}

	private void enableClipping(boolean enable) {
		mKeyguardBouncerFrame.setClipToPadding(enable);
		mKeyguardBouncerFrame.setClipChildren(enable);
		mRow1.setClipToPadding(enable);
		mRow2.setClipToPadding(enable);
		mRow3.setClipToPadding(enable);
		setClipChildren(enable);
	}

	@Override
	public boolean hasOverlappingRendering() {
		return false;
	}

	@Override
	public void verifyPasswordAndUnlock() {
		super.verifyPasswordAndUnlock();
	}

	@Override
	public void updateButtonText(String txt) {
		mDelAndCancelTextView.setText(txt);
	}
	
	public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }
	
	
    public TranslateAnimation getTranslateAnimation(final View view, float fromX, float toX, float fromY, float toY){
		TranslateAnimation animation = new TranslateAnimation(fromX, toX, fromY, toY);
		animation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation arg0) {
			}
			
			@Override
			public void onAnimationRepeat(Animation arg0) {
			}
			
			@Override
			public void onAnimationEnd(Animation arg0) {
				view.clearAnimation();
				view.setTranslationX(0);
			}
		});
		animation.setDuration(350);
		animation.setInterpolator(new BounceInterpolator());
		return animation;
	}

}
