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

import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.RenderNode;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Displays a PIN pad for unlocking.
 */
public class KeyguardPINView extends KeyguardPinBasedInputView {

    private final AppearAnimationUtils mAppearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private ViewGroup mContainer;
    private ViewGroup mRow0;
    private ViewGroup mRow1;
    private ViewGroup mRow2;
    private ViewGroup mRow3;
    private View mDivider;
    private int mDisappearYTranslation;
    private View[][] mViews;
	private LinearLayout mLeftImageView;
	private LinearLayout mRightImageView;
	private View mViewChildView;
	private ImageView mViewLeft;
	private ImageView mViewRight;

    public KeyguardPINView(Context context) {
        this(context, null);
    }

    public KeyguardPINView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAppearAnimationUtils = new AppearAnimationUtils(context);
        mDisappearAnimationUtils = new DisappearAnimationUtils(context,
                125, 0.6f /* translationScale */,
                0.45f /* delayScale */, AnimationUtils.loadInterpolator(
                        mContext, android.R.interpolator.fast_out_linear_in));
        mDisappearYTranslation = getResources().getDimensionPixelSize(
                R.dimen.disappear_y_translation);
    }

    @Override
    protected void resetState() {
        super.resetState();
//        mSecurityMessageDisplay.setMessage(getMessageWithCount(R.string.kg_password_instructions), false);
        mSecurityMessageDisplay.setMessage(getMessageWithCount(R.string.kg_password_instructions), true);
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.pinEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSecurityMessageDisplay.setTimeout(0);
        mContainer = (ViewGroup) findViewById(R.id.container);
        mRow0 = (ViewGroup) findViewById(R.id.row0);
        mRow1 = (ViewGroup) findViewById(R.id.row1);
        mRow2 = (ViewGroup) findViewById(R.id.row2);
        mRow3 = (ViewGroup) findViewById(R.id.row3);
        mDivider = findViewById(R.id.divider);
        
        mLeftImageView = (LinearLayout)findViewById(R.id.leftOversea);
        mRightImageView = (LinearLayout)findViewById(R.id.rightOversea);
        mViewChildView = (View)findViewById(R.id.childViewOversea);
        mViewLeft = (ImageView)findViewById(R.id.leftTextOversea);
        mViewRight = (ImageView)findViewById(R.id.rightTextOversea);
        mViews = new View[][]{
        		new View[]{mViewLeft},
        		new View[]{mViewRight},
        		new View[] { findViewById(R.id.keyguard_message_area) },
                new View[]{
                        mRow0, null, null
                },
                new View[]{
                        findViewById(R.id.key1), findViewById(R.id.key2),
                        findViewById(R.id.key3)
                },
                new View[]{
                        findViewById(R.id.key4), findViewById(R.id.key5),
                        findViewById(R.id.key6)
                },
                new View[]{
                        findViewById(R.id.key7), findViewById(R.id.key8),
                        findViewById(R.id.key9)
                },
                new View[]{
                        null, findViewById(R.id.key0), findViewById(R.id.key_enter)
                },
                new View[]{
                        null, mEcaView, null
                }};
      
		if (null!=mViewLeft&&null!=mViewChildView&&null!=mViewRight) {
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
        AppearAnimationUtils.startTranslationYAnimation(this, 0 /* delay */, 500 /* duration */,
                0, mAppearAnimationUtils.getInterpolator());
        mAppearAnimationUtils.startAnimation2d(mViews,
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                    }
                });
    }
    
    @Override
    public void startResetAppearAnimation(float startTranslationY, final Runnable resetrRunnable) {
    	enableClipping(false);
//        setTranslationY(0);
        setTranslationY(startTranslationY);
        AppearAnimationUtils.startTranslationYAnimation(this, 0 /* delay */, 280 /* duration */,
                getHeight() - mDisappearYTranslation, mDisappearAnimationUtils.getInterpolator());
        mDisappearAnimationUtils.startAnimation2d(mViews,
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                        if (resetrRunnable != null) {
                        	resetrRunnable.run();
                        }
                    }
                });
    }
 
    @Override
    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        enableClipping(false);
        setTranslationY(0);
        AppearAnimationUtils.startTranslationYAnimation(this, 0 /* delay */, 280 /* duration */,
                mDisappearYTranslation, mDisappearAnimationUtils.getInterpolator());
        mDisappearAnimationUtils.startAnimation2d(mViews,
                new Runnable() {
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
        mContainer.setClipToPadding(enable);
        mContainer.setClipChildren(enable);
        mRow1.setClipToPadding(enable);
        mRow2.setClipToPadding(enable);
        mRow3.setClipToPadding(enable);
        setClipChildren(enable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
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
		animation.setDuration(350);//ÉèÖÃ¶¯»­³ÖÐøÊ±¼ä
		animation.setInterpolator(new BounceInterpolator());
		return animation;
	}
}
