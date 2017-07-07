package com.android.covermode;

import com.android.keyguard.R;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class CircleView extends FrameLayout {
	private final static String TAG = "CircleView";
	private Context mContext;
	LightView mLight;
	private Circle mCircle[] = new Circle[3];
	private float mStep = 0.05f;
	private static final String ANDROID_CLOCK_SOLID_FONT_FILE = "/system/fonts/AndroidClock_Solid.ttf";

	int[] blue = new int[] { R.drawable.starry_fixcircle_blue, R.drawable.light_blue, R.drawable.move_circle_1_blue, R.drawable.move_circle_2_blue,
			R.drawable.move_circle_3_blue };
	int[] purple = new int[] { R.drawable.starry_fixcircle_purple, R.drawable.light_purple, R.drawable.move_circle_1_purple,
			R.drawable.move_circle_2_purple, R.drawable.move_circle_3_purple };
	int[] green = new int[] { R.drawable.starry_fixcircle_green, R.drawable.light_green, R.drawable.move_circle_1_green,
			R.drawable.move_circle_2_green, R.drawable.move_circle_3_green };
	int[] yellow = new int[] { R.drawable.starry_fixcircle_yellow, R.drawable.light_yellow, R.drawable.move_circle_1_yellow,
			R.drawable.move_circle_2_yellow, R.drawable.move_circle_3_yellow };
	int starryCircle;
	int light;
	int[] moveCircle = new int[] { 0, 0, 0 };

	public CircleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		getResId();
		init();
	}

	public CircleView(Context context) {
		super(context);
		mContext = context;
		getResId();
		init();
	}

	public void getResId() {
		// ArrayList TempResId = new int[]{};
		int index = Settings.System.getInt(mContext.getContentResolver(), "view_window_bgcolor_index", 0);
		if (index < 0 || index > 3) {
			index = 0;
		}
		Log.v(TAG, "index=" + index);
		if (0 == index || 1 == index) {
			starryCircle = blue[0];
			light = blue[1];
			moveCircle[0] = blue[2];
			moveCircle[1] = blue[3];
			moveCircle[2] = blue[4];
		}
		// if(1==index){
		// starryCircle = purple[0];
		// light =purple[1]; //1a??
		// moveCircle1 = purple[2];
		// moveCircle2 = purple[3];
		// moveCircle3 = purple[4];
		// }
		if (2 == index) {
			starryCircle = green[0];
			light = green[1];
			moveCircle[0] = green[2];
			moveCircle[1] = green[3];
			moveCircle[2] = green[4];
		}
		if (3 == index) {
			starryCircle = purple[0];
			light = purple[1];
			moveCircle[0] = purple[2];
			moveCircle[1] = purple[3];
			moveCircle[2] = purple[4];
		}
		Log.v(TAG, "moveCircle[0]=" + moveCircle[0]);

	}

	private void init() {
		if (mCircle[0] == null) {
			ImageView v = new ImageView(mContext);
			v.setImageResource(moveCircle[0]);
			mCircle[0] = new Circle(v, dip2px(mContext, 8f));
		}
		if (mCircle[1] == null) {
			ImageView v = new ImageView(mContext);
			v.setImageResource(moveCircle[1]);
			mCircle[1] = new Circle(v, dip2px(mContext, 8f));
		}
		if (mCircle[2] == null) {
			ImageView v = new ImageView(mContext);
			v.setImageResource(moveCircle[2]);
			mCircle[2] = new Circle(v, dip2px(mContext, 8f));
		}
		if (mLight == null) {
			mLight = new LightView(mContext);
		}
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		addView(mCircle[2].mView, lp);
		addView(mCircle[1].mView, lp);
		addView(mCircle[0].mView, lp);
		lp.width = mCircle[0].mView.getDrawable().getMinimumWidth();
		lp.height = mCircle[0].mView.getDrawable().getMinimumHeight();
		addView(mLight, lp);
		mLight.postInvalidate();
	}

	void updateBg() {
		getResId();
		removeAllViews();
		mCircle[0] = mCircle[1] = mCircle[2] = null;
		mLight = null;
		init();
		postInvalidate();
	}

	@SuppressLint("NewApi")
	private void move2() {
		float degree = (float) (Math.random() * 2 * 3.14);
		for (int i = 0; i < 3; i++) {
			if (mCircle[i].mCircleanimator[0] == null) {
				mCircle[i].mCircleanimator[0] = ObjectAnimator.ofFloat(mCircle[i].mView, "translationX", 0,
						(float) (mCircle[i].mCircler * Math.sin(degree)), 0);
				mCircle[i].mCircleanimator[0].setDuration(2000);
			}
			if (mCircle[i].mCircleanimator[1] == null) {
				mCircle[i].mCircleanimator[1] = ObjectAnimator.ofFloat(mCircle[i].mView, "translationY", 0,
						(float) (mCircle[i].mCircler * Math.cos(degree)), 0);
				mCircle[i].mCircleanimator[1].setDuration(2000);
			}
			if (!mCircle[i].mCircleanimator[0].isRunning() && !mCircle[i].mCircleanimator[1].isRunning()) {
				mCircle[i].mCircleanimator[0].setFloatValues(0, (float) (mCircle[i].mCircler * Math.sin(degree)), 0);
				mCircle[i].mCircleanimator[1].setFloatValues(0, (float) (mCircle[i].mCircler * Math.cos(degree)), 0);
				mCircle[i].mCircleanimator[0].start();
				mCircle[i].mCircleanimator[1].start();
				degree = (float) (degree + 2 * 3.14 / 3);
			}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {

		super.dispatchDraw(canvas);
		move2();
		this.postInvalidateDelayed(50);
	}

	private int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	private class LightView extends ImageView {

		Bitmap mlight;
		Bitmap mfixcircle;
		float mdegree = 0;
		float mdegreeStep = 0.3f;
		int mAlpha = 0;
		float maxdegree = 50;

		public LightView(Context context) {
			super(context);
			init();
		}

		private void init() {
			if (mlight == null) {
				mlight = ((BitmapDrawable) (mContext.getResources().getDrawable(light))).getBitmap();
			}
			if (mfixcircle == null) {
				mfixcircle = ((BitmapDrawable) (mContext.getResources().getDrawable(starryCircle))).getBitmap();
			}
		}

		protected void onDraw(Canvas canvas) {
			canvas.drawBitmap(mfixcircle, 0, 0, null);
			Matrix matrix = new Matrix();
			matrix.postRotate(mdegree, mlight.getWidth() / 2, mlight.getHeight() / 2);
			Paint paint = new Paint();
			paint.setXfermode(new PorterDuffXfermode(Mode.SCREEN));
			paint.setAlpha(mAlpha);
			canvas.drawBitmap(mlight, matrix, paint);
			super.onDraw(canvas);
			mdegree = mdegree + mdegreeStep;
			/*
			 * if (mdegree >= maxdegree - mdegreeStep * 3) { mAlpha = (int)
			 * (mAlpha - mAlphaStep); }
			 */
			if (mdegree <= maxdegree / 2) {
				mAlpha = (int) (mdegree * 2 * 255 / maxdegree);
			} else if (mdegree > maxdegree / 2 && mdegree <= maxdegree) {
				mAlpha = (int) (255 - (mdegree - maxdegree / 2) * 2 * 255 / maxdegree);
			} else if (mdegree > maxdegree) {
				mdegree = 0;
				mAlpha = 0;
			}
			this.postInvalidateDelayed(50);
		}
	}

	private class TranslationPoint {
		float x;
		float y;

		void set(float x1, float y1) {
			x = x1;
			y = y1;
		}
	}

	private class Circle {
		ImageView mView;
		TranslationPoint mCircleCurrent = new TranslationPoint();
		ObjectAnimator mCircleanimator[] = new ObjectAnimator[2];
		float mCircler;

		Circle(ImageView view, float Circle) {
			mView = view;
			mCircler = Circle;
			mCircleCurrent.set(0, 0);
		}
	}
}
