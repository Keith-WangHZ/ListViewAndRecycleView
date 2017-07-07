package com.android.systemui.recents;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.systemui.R;

public class CircleViewYL extends View{
	private int percent;
	private Drawable shuazi, shuazi_red;
	private boolean is_enter_anim, is_loaded;
	private float end_angle;
	private int angle_dex;
	private boolean is_clear_anim;
	private AnimationListener listener;
	DecelerateInterpolator interpolator;
	private long start_time;
	private long DURATION1 = 200;
	private long DURATION2 = 400;
	public CircleViewYL(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}
	
	public CircleViewYL(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		shuazi = context.getDrawable(R.drawable.recents_clear_normal);
		shuazi_red = context.getDrawable(R.drawable.shuazi_red);
		is_enter_anim = false;
		is_loaded = false;
		end_angle = 0;
		angle_dex = 5;
		is_clear_anim = false;
		interpolator = new DecelerateInterpolator();
	}

	public CircleViewYL(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public void setPercent(int per) {
		percent = per;
	}
	
	public void startAnim() {
		end_angle = 0;
		is_enter_anim = true;
		start_time = System.currentTimeMillis();
		this.invalidate();
		
	}
	
	public void startClearAnim(){
		is_clear_anim =true;
		start_time = System.currentTimeMillis();
		this.invalidate();
	}
	
	public void setAnimationListener(AnimationListener listener){
		this.listener = listener;		
	}
	@SuppressLint("NewApi")
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		int strokewidth = 4;
		Paint paint = new Paint();
		float angle1 = -90;
		float angle2 = 360 - 360*(100-percent)/100;
		Drawable drawable = null;
		
		paint.setAntiAlias(true);  
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(strokewidth);
		
		paint.setColor(0x4affffff);
		canvas.drawArc(strokewidth, strokewidth, getWidth()-strokewidth, getHeight()-strokewidth, 0, 360, false, paint);
		
		paint.setColor(0xccffffff);  
		drawable = shuazi;	
		
		if (is_clear_anim) {
			long times = System.currentTimeMillis() - start_time;
			float input = times/(float)DURATION2;
			float per = interpolator.getInterpolation(input);
			paint.setColor(0xcc4ab94f);  
			end_angle = per*360;
			if (end_angle >= 360 || times >= DURATION2) {
				end_angle = 0;
				//canvas.drawArc(strokewidth, strokewidth, getWidth()-strokewidth, getHeight()-strokewidth, angle1, angle2, false, paint);
				if (listener != null) {
					listener.OnAnimationEnd();
				}
				this.invalidate();
			}else {
				this.invalidate();
				canvas.drawArc(strokewidth, strokewidth, getWidth()-strokewidth, getHeight()-strokewidth, angle1, end_angle, false, paint);
			}	
			
		}else {
			if (is_loaded) {
				canvas.drawArc(strokewidth, strokewidth, getWidth()-strokewidth, getHeight()-strokewidth, angle1, angle2, false, paint);
			}else {
				if (is_enter_anim) {
					long times = System.currentTimeMillis() - start_time;
					float input = times/(float)DURATION1;
					float per = interpolator.getInterpolation(input);
					end_angle = angle2*per;
					if (end_angle >= angle2 || times >= DURATION1) {
						end_angle = 0;
						is_enter_anim = false;
						is_loaded = true;
						canvas.drawArc(strokewidth, strokewidth, getWidth()-strokewidth, getHeight()-strokewidth, angle1, angle2, false, paint);
					}else {
						this.invalidate();
						canvas.drawArc(strokewidth, strokewidth, getWidth()-strokewidth, getHeight()-strokewidth, angle1, end_angle, false, paint);
					}
				}
			}			
		}
		
		
		drawable.setBounds(getWidth()/4, getHeight()/4, getWidth()-getWidth()/4,  getHeight()-getHeight()/4);
		drawable.draw(canvas);
		super.onDraw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		// TODO Auto-generated method stub
		if (arg0.getAction() == MotionEvent.ACTION_DOWN) {
			this.invalidate();
		}
		return super.onTouchEvent(arg0);
	}
	
	public interface AnimationListener{
		void OnAnimationEnd();
	}
}
