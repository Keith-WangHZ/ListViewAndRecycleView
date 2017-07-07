
package com.android.systemui.recents;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import android.content.Context;
import android.graphics.AvoidXfermode;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ProgressImageView extends ImageView {
	
	public static final String TAG = "ProgressImageView";
	
	private Drawable mCircleDrawable;
	private Bitmap mCircleBmp;
		
	private Canvas mCanvas;
	private Bitmap mCanvasBmp;
	private Paint mPaint;
					
	private float mProgress;
	
	public ProgressImageView(Context context) {
		this(context, null);
	}

	public ProgressImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		mCircleDrawable = getResources().getDrawable(R.drawable.recents_clear_circle);
		mCircleBmp = ((BitmapDrawable)mCircleDrawable).getBitmap();		
		mCanvas = new Canvas();		
		mPaint = new Paint();
		mPaint.setColor(0x00000000);
		mPaint.setAntiAlias(true);		
		PorterDuffXfermode  mode = new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP);
		mPaint.setXfermode(mode);
	}
	
	private float sin(float p){
		return (float)Math.sin(p*Math.PI/2);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int h = getHeight();
		int w = getWidth();
		RectF  oval= new RectF(0f-w,0f-h,(float)w*2,(float)w*2);
		mCanvasBmp = Bitmap.createBitmap(mCircleBmp.getWidth(),mCircleBmp.getHeight(),mCircleBmp.getConfig());		
		mCanvas.setBitmap(mCanvasBmp);
		mCanvas.drawColor(0x00000000);
		mCanvas.drawBitmap(mCircleBmp, 0, 0, null);
		mCanvas.drawArc(oval, -90 + mProgress *360,(1-mProgress)*360 ,true, mPaint);
		mCanvas.setBitmap(null);
		canvas.drawBitmap(mCanvasBmp, 0,0, null);
	}
	
	public void setProgress(float progress){
		mProgress = progress;
		invalidate();
	}
	public float getProgress(){
		return mProgress;
	}
    public void startNoUserInteractionAnimation() {
        setVisibility(View.VISIBLE);
        setAlpha(0f);
        animate()
                .alpha(1f)
                .setStartDelay(0)
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(225)
                .withLayer()
                .start();
    }	
}
