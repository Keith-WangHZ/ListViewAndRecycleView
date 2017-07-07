package com.android.covermode;

import java.util.Calendar;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

public class YLTimeView extends ViewGroup {
	private static final int DURATION = 500;
	private static final int DELAY = 80;
	private int SPACE_DIP = 15;
	private int SPACE;

	private Context mContext;
	private static final int[] NUM_RES_IDS = { R.drawable.starry_num_0, R.drawable.starry_num_1, R.drawable.starry_num_2, R.drawable.starry_num_3,
			R.drawable.starry_num_4, R.drawable.starry_num_5, R.drawable.starry_num_6, R.drawable.starry_num_7, R.drawable.starry_num_8,
			R.drawable.starry_num_9, };
	Drawable[] mNum = new Drawable[10];
	private int[] mNumId = new int[] { -1, -1, -1, -1 };
	int mImageWidth = 0;
	int mImageHeight = 0;
	private ImageView[] mImageView = new ImageView[4];

	private Calendar mCalendar;
	private String mFormat;
	private final static String M12 = "h:mm";
	private final static String M24 = "kk:mm";

	private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

		@Override
		public void onTimeChanged() {
			updateTime();
		}
	};

	public CharSequence getLocalTime() {
		CharSequence localTime;
		Calendar calendar = Calendar.getInstance();
		mFormat = android.text.format.DateFormat.is24HourFormat(mContext) ? M24 : M12;
		if (mFormat == M24) {
			localTime = DateFormat.format("kk:mm", calendar);
		} else {
			localTime = DateFormat.format("hh:mm", calendar);
		}
		return localTime;
	}

	private void updateTime() {
		mCalendar.setTimeInMillis(System.currentTimeMillis());
		mFormat = android.text.format.DateFormat.is24HourFormat(getContext()) ? M24 : M12;
		mCalendar.setTimeInMillis(System.currentTimeMillis());
		int min = Integer.valueOf(DateFormat.format("mm", mCalendar).toString());
		int hour = Integer.valueOf(DateFormat.format("kk", mCalendar).toString());
		setTime(hour, min);
	}

	public void setTimeString(String text) {
		String spStr[] = text.split(":");
		if (spStr == null || spStr.length < 2)
			return;
		setTime(Integer.valueOf(spStr[0]), Integer.valueOf(spStr[1]));
	}

	public YLTimeView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public YLTimeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;
		init();
	}

	@SuppressLint("NewApi")
	private void init() {
		SPACE = (int) (SPACE_DIP * this.getResources().getDisplayMetrics().density);
		for (int i = 0; i < mNum.length; i++) {
			mNum[i] = getResources().getDrawable(NUM_RES_IDS[i]);
		}
		mImageWidth = mNum[0].getIntrinsicWidth();
		mImageHeight = mNum[0].getIntrinsicHeight();

		for (int i = 0; i < mImageView.length; i++) {
			mImageView[i] = new ImageView(getContext());
			LayoutParams lp = new LayoutParams(mImageWidth, mImageHeight);
			addView(mImageView[i], lp);
		}
		/* add for test */
		// mImageView[0].setImageDrawable(mNum[0]);
		// mImageView[1].setImageDrawable(mNum[1]);
		// mImageView[2].setImageDrawable(mNum[2]);
		// mImageView[3].setImageDrawable(mNum[3]);
		/* add for test */
		mImageView[2].setAlpha(0.5f);
		mImageView[3].setAlpha(0.5f);
	}

	@SuppressLint("NewApi")
	public void setTime(int hour, int minute) {
		int[] oldNumId = new int[] { mNumId[0], mNumId[1], mNumId[2], mNumId[3] };
		mNumId[0] = hour / 10;
		mNumId[1] = hour % 10;
		mNumId[2] = minute / 10;
		mNumId[3] = minute % 10;
		if (oldNumId[0] == -1) {
			mImageView[0].setImageDrawable(mNum[mNumId[0]]);
			mImageView[1].setImageDrawable(mNum[mNumId[1]]);
			mImageView[2].setImageDrawable(mNum[mNumId[2]]);
			mImageView[3].setImageDrawable(mNum[mNumId[3]]);
		} else {
			AnimatorSet animSet = new AnimatorSet();
			int delay = 0;
			for (int i = 3; i >= 0; i--) {
				if (oldNumId[i] != mNumId[i]) {
					mImageView[i].setTag(oldNumId[i]);
					animSet.play(makeValueAnimator(mNumId[i], mImageView[i], delay));
					delay += DELAY;
				}
			}
			animSet.start();
		}
	}

	@SuppressLint("NewApi")
	private ValueAnimator makeValueAnimator(final int newId, final ImageView imageView, int delay) {
		ValueAnimator anim = ValueAnimator.ofFloat(0, 180).setDuration(DURATION);
		anim.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator arg0) {
				// TODO Auto-generated method stub
				float angle = (Float) arg0.getAnimatedValue();
				if (angle >= 90) {
					angle -= 180;
					if ((Integer) imageView.getTag() != newId) {
						imageView.setImageDrawable(mNum[newId]);
						imageView.setTag(newId);
					}
				}

				imageView.setRotationX(angle);
			}
		});
		anim.setStartDelay(delay);

		return anim;
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mCalendar = Calendar.getInstance();
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
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// // TODO Auto-generated method stub
		int width = mImageWidth * 2 + dip2px(mContext, 12.5);
		int height = mImageHeight * 2 + SPACE;
		mImageView[0].measure(widthMeasureSpec, heightMeasureSpec);
		mImageView[1].measure(widthMeasureSpec, heightMeasureSpec);
		mImageView[2].measure(widthMeasureSpec, heightMeasureSpec);
		mImageView[3].measure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		int minTop = mImageHeight + SPACE;
		mImageView[0].layout(0, 0, mImageWidth, mImageHeight);
		mImageView[1].layout(mImageWidth + dip2px(mContext, 12.5), 0, mImageWidth + mImageWidth + dip2px(mContext, 12.5), mImageHeight);
		mImageView[2].layout(0, minTop, mImageWidth, minTop + mImageHeight);
		mImageView[3].layout(mImageWidth + dip2px(mContext, 12.5), minTop, mImageWidth + mImageWidth + dip2px(mContext, 12.5), minTop + mImageHeight);
	}

	private int dip2px(Context context, double d) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (d * scale + 0.5f);
	}

}
