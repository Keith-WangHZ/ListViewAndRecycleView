package com.android.covermode;

import com.android.covermode.StarryPINView.DismissPin;
import com.android.covermode.StarryPatternView.DismissAction;
import com.android.covermode.StarrySecurityModel.SecurityMode;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

@SuppressLint("NewApi")
public class StarryHostView extends FrameLayout {

	private static final String TAG = "StarryHostView";
	public final static String DISMISS = "sys.keyguard.is.dismiss";
	private Context mContext;

	private StarryTimeView mStarryTimeView; // A
	// private StarryTopStatusView mStarryHealthHideView; // B
	private StarryShortcutView mStarryShortcutView; // C
	// private StarryBottomStatusView mStarryBottomStatusView;// D
	private StarryTopStatusView mStarryTopStatusView;

	private StarryChargeView mStarryChargeView;

	private FrameLayout mStarryParentView;
	private ImageButton mStarryUpView;
	private ImageButton mCameraButton;

	ObjectAnimator mStartShow1;
	ObjectAnimator mStartShow2;

	private int mfirstBootom;
	private int mfirstLeft;
	private float mDownRawY;
	private float mBeRawY;
	private float mDownRawY1;
	private boolean globalDrag;

	private boolean isTimeViewSliding = false;
	private boolean isSecure = false;

	private GestureDetector mGestureDetector;

	private StarrySecurityModel mStarrySecurityModel;

	private static final int HIDE_VIEW = 0;

	private StarryPatternView mStarryPatternView;
	private StarryPINView mStarryPINView;
	private boolean hasAddPattern = false;
	private boolean hasAddPIN = false;
	private boolean isAnimNotFinish = false;
	private LockPatternUtils mLockPatternUtils;

	private DismissAction mDismissAction = new DismissAction() {

		@Override
		public boolean Dismiss() {
			Log.v(TAG, "--------------Host Dismiss()---------------------");
			if (hasAddPattern) {
				StarryHostView.this.removeView(mStarryPatternView);
				StarryHostView.this.mStarryParentView.setVisibility(View.VISIBLE);
				hasAddPattern = false;
			}
			HolsterFixableView.mStarryDismissed = true;
			mHandler.removeMessages(HIDE_VIEW);
			mHandler.sendEmptyMessageDelayed(HIDE_VIEW, 1500);
			return false;
		}

	};

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		return true;
	}

	private DismissPin mDismissPin = new DismissPin() {

		@Override
		public boolean Dismiss() {
			Log.v(TAG, "--------------Host Dismiss()---------------------");
			HolsterFixableView.mStarryDismissed = true;
			if (hasAddPIN) {
				StarryHostView.this.removeView(mStarryPINView);
				StarryHostView.this.mStarryParentView.setVisibility(View.VISIBLE);
				hasAddPIN = false;
			}
			HolsterFixableView.mStarryDismissed = true;
			mHandler.removeMessages(HIDE_VIEW);
			mHandler.sendEmptyMessageDelayed(HIDE_VIEW, 1500);
			return false;
		}

	};

	// private OnGestureListener mOnGestureListener;
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case HIDE_VIEW:
				hideView();
				break;

			}
		}
	};

	private void hideView() {
		translateAnimation();
		mStarryUpView.setVisibility(View.VISIBLE);
		mStarryChargeView.setVisibility(View.VISIBLE);
		mCameraButton.setVisibility(View.INVISIBLE);
		mStarryTopStatusView.setTranslationY(HolsterFixableView.mSubViewHeight - mStarryTopStatusView.getHeight());
	}

	public StarryHostView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mLockPatternUtils = mLockPatternUtils == null ? new LockPatternUtils(context) : mLockPatternUtils;
		mStarrySecurityModel = StarrySecurityModel.getInstance(mContext);
		isSecure = isSecure();
		mGestureDetector = new GestureDetector(mContext, new MyGestureListener());
	}

	private boolean isSingle = true;

	class Myrunnable implements Runnable {
		MotionEvent ev;

		Myrunnable(MotionEvent event) {
			ev = event;
		}

		@Override
		public void run() {
			if (isSingle) {
				handleSingleTap(ev);
			}
		}

	}

	class MyGestureListener extends SimpleOnGestureListener {
		public boolean onSingleTapUp(MotionEvent ev) {
			isSingle = true;
			postDelayed(new Myrunnable(ev), 100);
			return false;
		}

		public void onLongPress(MotionEvent ev) {
		}

		public boolean onScroll(MotionEvent ev1, MotionEvent ev, float distanceX, float distanceY) {
			handleTimeViewMove(null, ev);
			return false;
		}

		public boolean onFling(MotionEvent ev1, MotionEvent ev2, float velocityX, float velocityY) {
			return false;
		}

		public void onShowPress(MotionEvent ev) {

		}

		public boolean onDown(MotionEvent ev) {
			if ((mStartShow1 != null && (mStartShow1.isRunning()))) {
				mStartShow1.cancel();
				isAnimNotFinish = true;
			}
			mDownRawY = ev.getRawY();
			mBeRawY = mDownRawY;
			return false;
		}

		public boolean onDoubleTap(MotionEvent ev) {
			isSingle = false;
			return false;
		}

		public boolean onDoubleTapEvent(MotionEvent ev) {
			return false;
		}

		public boolean onSingleTapConfirmed(MotionEvent ev) {
			return false;
		}

	}

	static class ViewMode {
		static private int mStarryTimemode = 0;
		static private int mStarryHealthHidemode = 0;
		static private int mStarryShortcutmode = 0;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (hasAddPattern || hasAddPIN) {
			onRestart();
		}
		// initStartAnimation();
		// ViewMode.mStarryTimemode = 0;
		// ViewMode.mStarryHealthHidemode = 0;
		// ViewMode.mStarryShortcutmode = 0;
		// HolsterFixableView.mStarryDismissed = false;
	}
	
	public void onRestart() {

		mStarryShortcutView.setTranslationY(0);

		mStarryTimeView.setTranslationY(0);

		if (hasAddPattern) {
			removeView(mStarryPatternView);
			hasAddPattern = false;
		}
		if (hasAddPIN) {
			removeView(mStarryPINView);
			hasAddPIN = false;
		}
		mStarryParentView.setVisibility(View.VISIBLE);
		if (mStarrySecurityModel.getSecurityModel() == SecurityMode.None) {
			HolsterFixableView.mStarryDismissed = true;
		} else {
			HolsterFixableView.mStarryDismissed = false;
		}
		mStarryShortcutView.setVisibility(View.INVISIBLE);
		mStarryChargeView.setAlpha(255);
		mStarryChargeView.setVisibility(View.VISIBLE);
		mCameraButton.setVisibility(View.VISIBLE);
		mStarryTopStatusView.setTranslationY(0);
		mStarryUpView.setVisibility(View.INVISIBLE);
		isTimeViewSliding = false;
		HolsterFixableView.mStarryDismissed = false;
		ViewMode.mStarryTimemode = 0;
		ViewMode.mStarryHealthHidemode = 0;
		ViewMode.mStarryShortcutmode = 0;
		if (mStarryPatternView != null) {
			mStarryPatternView.setStealMode(!mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
		}
		if (mStarryPINView != null) {
			mStarryPINView.reset();
		}
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mCameraButton = (ImageButton) this.findViewById(R.id.starry_status_camera_button);
		mStarryParentView = (FrameLayout) this.findViewById(R.id.starry_parent_view);
		mStarryTimeView = (StarryTimeView) this.findViewById(R.id.starry_time_view); // A
		mStarryShortcutView = (StarryShortcutView) this.findViewById(R.id.starry_shortcut_view); // C
		mStarryTopStatusView = (StarryTopStatusView) this.findViewById(R.id.starry_top_status_view);
		mStarryPatternView = (StarryPatternView) View.inflate(mContext, R.layout.starry_pattern_view, null);
		mStarryChargeView = (StarryChargeView) this.findViewById(R.id.starry_charge_view);
		mStarryUpView = (ImageButton) this.findViewById(R.id.starry_up_view);
		mStarryTopStatusView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				return true;
			}
		});

		mStarryTimeView.setOnTouchListener(mListener);
		mCameraButton.setOnTouchListener(mCameraListener);
		mStarryShortcutView.setVisibility(View.INVISIBLE);
		mStarryChargeView.setVisibility(View.VISIBLE);
		mCameraButton.setVisibility(View.VISIBLE); // camera
		mStarryUpView.setOnTouchListener(mListener);
		mStarryShortcutView.setOnTouchListener(mShortCutListener);
		mStarryPatternView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				return true;
			}
		});
		mStarryPINView = (StarryPINView) View.inflate(mContext, R.layout.starry_pin_view, null);
		if (mStarrySecurityModel.getSecurityModel() == SecurityMode.None) {
			HolsterFixableView.mStarryDismissed = true;
		} else {
			HolsterFixableView.mStarryDismissed = false;
		}

	}

	View.OnTouchListener mShortCutListener = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent ev) {
			// TODO Auto-generated method stub
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (ViewMode.mStarryShortcutmode == 1) {
					ViewMode.mStarryShortcutmode = 0;
					mStarryTopStatusView.setTranslationY(HolsterFixableView.mSubViewHeight - mStarryTopStatusView.getHeight());
					translateAnimation();
					if (ViewMode.mStarryHealthHidemode == 0) {
						mStarryChargeView.setAlpha(255);
						mCameraButton.setVisibility(View.VISIBLE);
					}
				}
				break;
			}
			return true;

		}

	};

	View.OnTouchListener mListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent ev) {
			int vid = v.getId();
			if (vid == R.id.starry_time_view) {
				mGestureDetector.onTouchEvent(ev);
				switch (ev.getAction()) {
				case MotionEvent.ACTION_DOWN:
					break;
				case MotionEvent.ACTION_MOVE:
					// handleTimeViewMove(v,ev);
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					handleTimeViewUp(v, ev);
					break;
				}
			} else if (vid == R.id.starry_up_view) {
				switch (ev.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mLastMoveY = mDownRawY1 = ev.getRawY();
					break;
				case MotionEvent.ACTION_MOVE:
					handleShortCutViewMove(v, ev);
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					handleShortCutViewUp(v, ev);
					break;
				}
			}
			return true;
		}
	};

	View.OnTouchListener mCameraListener = new View.OnTouchListener() {

		private float mGestureStartX, mGestureStartY; // where did you first
														// touch the screen?
		private float mGestureStartRawX, mGestureStartRawY;
		private float mBeRawY;
		private boolean mBlockDrag;
		private boolean mDragging;
		private int mGestureStartChallengeBottom; // where was the challenge at
													// that time?
		private int mGestureStartChallengeLeft; // where was the challenge at
												// that time?

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// mCameraButton.setBackgroundResource(R.drawable.starry_status_camera_pressed);
				if (mDragging) {
					break;
				}
				if (globalDrag) {
					mBlockDrag = true;
					break;
				}
				mGestureStartX = event.getX();
				mGestureStartY = event.getY();
				mGestureStartRawX = event.getRawX();
				mGestureStartRawY = event.getRawY();
				break;
			case MotionEvent.ACTION_MOVE:
				if (mBlockDrag) {
					break;
				}
				mGestureStartChallengeBottom = v.getBottom();
				mGestureStartChallengeLeft = v.getLeft();
				if (!mDragging) {
					mfirstBootom = mGestureStartChallengeBottom;
					mfirstLeft = mGestureStartChallengeLeft;
					mDragging = true;
				}
				float posX = event.getX() - mGestureStartX;
				float posY = event.getY() - mGestureStartY;
				float mvDistX = event.getRawX() - mGestureStartRawX;
				float mvDistY = event.getRawY() - mGestureStartRawY;
				if (Math.abs(mvDistY) <= 160 && Math.abs(mvDistX) <= 160) {
					// int mvPosX = mGestureStartChallengeLeft + (int)posX;
					int mvPosY = mGestureStartChallengeBottom + (int) posY;
					if (Math.abs(mvDistY) >= 120) {
						mvPosY -= (160 - Math.abs(mvDistY));
						mBlockDrag = true;
						/*
						 * boolean isLock = false; if
						 * (mStarrySecurityModel.getSecurityModel() ==
						 * SecurityMode.Pattern ||
						 * mStarrySecurityModel.getSecurityModel() ==
						 * SecurityMode.PIN) {
						 * 
						 * isLock = true; }
						 * 
						 * Intent it = new
						 * Intent("com.android.camera.skywindow"); boolean
						 * isSecurity = (!StarryHostViewNew.isDismiss());
						 * isSecurity = (isLock & isSecurity);
						 * it.putExtra("SecureSkyCamera", isSecurity);
						 * mContext.startActivity(it);
						 */
						boolean isLock = false;
						if (mStarrySecurityModel.getSecurityModel() == SecurityMode.Pattern
								|| mStarrySecurityModel.getSecurityModel() == SecurityMode.PIN) {

							isLock = true;
						}
						boolean isSecurity = !isDismiss();
						isSecurity = (isLock && isSecurity);
						Intent mIntent = new Intent();
						mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mIntent.setAction(isSecurity ? "android.media.action.STILL_IMAGE_CAMERA_SECURE"
								: "com.android.camera.action.CAMERA_FAST_CAPTURE");
						mIntent.putExtra("request", "SkyWindow");
						try {
							mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
						} catch (ActivityNotFoundException e) {
							Log.d(TAG, "camera not found");
						}
						setApacheAnimation(v);
					}
					moveChallengeTo(v
					// , mvPosX
							, v.getLeft(), mvPosY);
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				// mCameraButton.setBackgroundResource(R.drawable.starry_status_camera_normal);
				if (globalDrag) {
					break;
				}
				if (!mBlockDrag) {
					if (mDragging) {
						moveChallengeTo(v, mfirstLeft, mfirstBootom);
					}
				}
				mDragging = mBlockDrag = false;
				break;
			case MotionEvent.ACTION_CANCEL:
				Log.d(TAG, "event.type=cancel");
			default:
				break;
			}
			return true;
		}
	};

	void setApacheAnimation(View v) {
		final View v1;
		v1 = v;
		v.setAlpha(0.8f);
		Animator anim = ObjectAnimator.ofFloat(v, "alpha", 0f);
		anim.setDuration(400);
		anim.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				// TODO Auto-generated method stub
				mCameraButton.setAlpha(1.0f);
				moveChallengeTo(mCameraButton, mfirstLeft, mfirstBootom);
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationStart(Animator arg0) {
				// TODO Auto-generated method stub

			}

		});
		anim.start();

	}

	private boolean moveChallengeTo(View v, int left, int bottom) {
		Log.d(TAG, "move left=" + v.getLeft() + ",top=" + (bottom - v.getHeight()) + ",right=" + v.getRight() + ",bottom=" + bottom);
		v.layout(left, bottom - v.getHeight(), left + v.getWidth(), bottom);
		postInvalidateOnAnimation();
		return false;
	}

	private boolean isSecure() {
		if (mStarrySecurityModel.getSecurityModel() == SecurityMode.Pattern || mStarrySecurityModel.getSecurityModel() == SecurityMode.PIN) {
			return true;
		}
		return false;
	}

	private void handleTimeViewMove(View v, MotionEvent ev) {
		if (ViewMode.mStarryHealthHidemode == 1) {
			return;
		}
		if (ViewMode.mStarryShortcutmode == 1) {
			return;
		}
		if (mDownRawY - ev.getRawY() < 12) {
			return;
		}
		if (Math.abs(mDownRawY - ev.getRawY()) > dip2px(mContext, 135)) {
			return;
		}
		mStarryTopStatusView.setTranslationY(mStarryTopStatusView.getTranslationY() + ev.getRawY() - mBeRawY);
		mStarryChargeView.setAlpha(0);
		mCameraButton.setVisibility(View.INVISIBLE);
		mStarryTimeView.setTranslationY(mStarryTimeView.getTranslationY() + ev.getRawY() - mBeRawY);
		mBeRawY = ev.getRawY();
		isTimeViewSliding = true;
	}

	private void handleSingleTap(MotionEvent ev) {
		Log.v(TAG, "handleSingeTap() mStarryShortcutmode = " + ViewMode.mStarryShortcutmode);
		if (ViewMode.mStarryShortcutmode == 1) {
			ViewMode.mStarryShortcutmode = 0;
			translateAnimation();
			if (ViewMode.mStarryHealthHidemode == 1) {
				mStarryTopStatusView.setTranslationY(HolsterFixableView.mSubViewHeight - mStarryTopStatusView.getHeight());
			}
			if (ViewMode.mStarryHealthHidemode == 0) {
				mStarryChargeView.setAlpha(255);
				mCameraButton.setVisibility(View.VISIBLE);
			}
			return;
		}
		if (ViewMode.mStarryHealthHidemode == 1) {
			return;
		}
		// if(!isTimeViewSliding ){
		if (!isAnimNotFinish) {
			initStartAnimation();
		} else {
			ObjectAnimator mStartShow3 = ObjectAnimator.ofFloat(mStarryTimeView, "translationY", mStarryTimeView.getTranslationY(), 0.0f);
			mStartShow3.setDuration(300);
			mStartShow3.start();
			isAnimNotFinish = false;
			return;
		}

		// }
	}

	@SuppressLint("NewApi")
	private void handleTimeViewUp(View v, MotionEvent ev) {
		if (ViewMode.mStarryHealthHidemode == 1) {
			return;
		}
		if (isTimeViewSliding && (mDownRawY - ev.getRawY()) > dip2px(mContext, 10)) {
			Log.d(TAG, "isTimeViewSliding && ev.getRawY()< mDownRawY = " + (isTimeViewSliding && ev.getRawY() < mDownRawY));
			ViewMode.mStarryHealthHidemode = 1;
			dissmiss();
		} else {
			ViewMode.mStarryHealthHidemode = 0;
			onRestart();
			mStarryTopStatusView.setTranslationY(0);
		}
		isTimeViewSliding = false;

	}

	private float mLastMoveY;

	private void handleShortCutViewMove(View v, MotionEvent ev) {
		if (!HolsterFixableView.mStarryDismissed) {
			return;
		}

		if (ViewMode.mStarryShortcutmode == 1) {
			return;
		}
		if (mDownRawY1 - ev.getRawY() < 12) {
			return;
		}
		if (Math.abs(mDownRawY1 - ev.getRawY()) > mStarryShortcutView.getHeight()) {
			return;
		}
		mStarryShortcutView.setVisibility(View.VISIBLE);
		mStarryShortcutView.setAlpha(255);
		mStarryChargeView.setAlpha(0);
		mCameraButton.setVisibility(View.INVISIBLE);
		mStarryShortcutView.setTranslationY(mStarryShortcutView.getTranslationY() + (ev.getRawY() - mLastMoveY));
		mLastMoveY = ev.getRawY();
	}

	private void handleShortCutViewUp(View v, MotionEvent ev) {
		if (!HolsterFixableView.mStarryDismissed) {
			return;
		}
		if (ViewMode.mStarryShortcutmode == 1) {
			return;
		}
		if (mDownRawY1 - ev.getRawY() > (dip2px(mContext, 30))) {
			mStarryShortcutView.setTranslationY(0);
			mStarryShortcutView.setVisibility(View.VISIBLE);
			ViewMode.mStarryShortcutmode = 1;
			mStarryUpView.setVisibility(View.INVISIBLE);
			mHandler.removeMessages(HIDE_VIEW);
			mHandler.sendEmptyMessageDelayed(HIDE_VIEW, 5000);
		} else {
			mStarryChargeView.setAlpha(255);
			mCameraButton.setVisibility(View.VISIBLE);
			if (ViewMode.mStarryHealthHidemode == 1) {
				mStarryChargeView.setAlpha(0);
				mCameraButton.setVisibility(View.INVISIBLE);
			}
			mStarryUpView.setVisibility(View.VISIBLE);
			mStarryShortcutView.setTranslationY(mStarryShortcutView.getHeight());
			mStarryShortcutView.setVisibility(View.INVISIBLE);
			ViewMode.mStarryShortcutmode = 0;
		}

	}

	private void dissmiss() {
		mStarryUpView.setVisibility(View.INVISIBLE);
		mStarryShortcutView.setVisibility(View.VISIBLE);
		mStarryChargeView.setVisibility(View.INVISIBLE);
		mCameraButton.setVisibility(View.INVISIBLE);
		mStarryShortcutView.setTranslationY(0);
		mStarryTimeView.setTranslationY(-HolsterFixableView.mSubViewHeight / 10);
		switch (mStarrySecurityModel.getSecurityModel()) {
		case None:
			Log.v(TAG, "SecurityModel = None");
			HolsterFixableView.mStarryDismissed = true;
			mHandler.removeMessages(HIDE_VIEW);
			mHandler.sendEmptyMessageDelayed(HIDE_VIEW, 1500);
			break;
		case Pattern:
			Log.v(TAG, "SecurityModel = Pattern");
			mStarryPatternView.setDismissAction(mDismissAction);
			showBouncer(SecurityMode.Pattern);
			hasAddPattern = true;
			Log.v(TAG, "isDismiss() = " + isDismiss());
			break;
		case PIN:
			Log.v(TAG, "SecurityModel = PIN");
			mStarryPINView.setDismissPin(mDismissPin);
			showBouncer(SecurityMode.PIN);
			hasAddPIN = true;
			break;
		default:
			Log.v(TAG, "ERROR");
			break;

		}
		Log.e(TAG, "HolsterFixableView.mStarryDismissed = " + HolsterFixableView.mStarryDismissed);

	}

	private void showBouncer(SecurityMode pattern) {
		mStarryParentView.setVisibility(View.INVISIBLE);
		if (pattern == SecurityMode.Pattern) {
			addView(mStarryPatternView, 0);
		} else if (pattern == SecurityMode.PIN) {
			addView(mStarryPINView, 0);
		}
	}

	private void translateAnimation() {
		Log.v(TAG, "translateAnimation()");
		mStarryUpView.setVisibility(View.VISIBLE);
		mStartShow2 = ObjectAnimator.ofFloat(mStarryShortcutView, "translationY", 0.0f, mStarryShortcutView.getHeight());
		mStartShow2.setDuration(500);
		mStartShow2.addListener(new AnimatorListener() {

			@Override
			public void onAnimationCancel(Animator arg0) {
			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				ViewMode.mStarryShortcutmode = 0;
				mStarryShortcutView.setVisibility(View.INVISIBLE);
				mStarryUpView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
			}

			@Override
			public void onAnimationStart(Animator arg0) {
				ViewMode.mStarryShortcutmode = 1;
			}
		});
		mStartShow2.start();
	}

	private void initStartAnimation() {
		mStartShow1 = ObjectAnimator.ofFloat(mStarryTimeView, "translationY", 0.0f, -HolsterFixableView.mSubViewHeight / 10, 0.0f);
		BounceInterpolator interpolator = new BounceInterpolator();
		mStartShow1.setInterpolator(interpolator);
		mStartShow1.setDuration(3000);
		mStartShow1.addListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator arg0) {
				mStarryChargeView.update(false);
			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				mStarryChargeView.update(false);
			}

			@Override
			public void onAnimationStart(Animator arg0) {
				mStarryChargeView.update(true);
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
			}
		});
		mStartShow1.start();
	}

	public boolean isDismiss() {
		return HolsterFixableView.mStarryDismissed;
	}

	private int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

}
