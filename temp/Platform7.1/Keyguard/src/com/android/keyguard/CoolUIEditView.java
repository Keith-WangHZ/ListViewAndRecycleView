package com.android.keyguard;

import java.util.LinkedList;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ImageView;

public class CoolUIEditView extends ViewGroup{

	
	private static final String  TAG = "CoolUIEditView" ;
	private Context mContext;
	private ImageView[] mImageViews = new ImageView[4];
	private static final int[] RES_ID = {R.drawable.default_dot,R.drawable.press_dot};
	private int mImageWidth;
	private int mImageHeight;
	private Bitmap mBitmap;
	private LinkedList<String> mNums= new LinkedList<String>();
	private EditViewCallback mEditViewCallback;
	
	public CoolUIEditView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initView();
	}

	private void initView(){
		Resources res = mContext.getResources();
		if(mBitmap== null)
		     mBitmap = BitmapFactory.decodeResource(res, R.drawable.default_dot);
		mImageWidth = mBitmap.getWidth();
		mImageHeight = mBitmap.getHeight();
		for(int i=0;i< mImageViews.length;i++){
			mImageViews[i] = new ImageView(mContext);
			mImageViews[i].setImageResource(RES_ID[0]);
			LayoutParams lp = new LayoutParams(mImageWidth,mImageHeight);
			this.addView(mImageViews[i], lp);			
		}
	}
	
	
	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
       for(int i=0;i<mImageViews.length;i++){
    	   mImageViews[i].layout(i*mImageWidth, 0, (i+1)*mImageWidth, mImageHeight);
       }
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = mImageWidth*4;
		int height = mImageHeight;
		this.setMeasuredDimension(width, height);
	}
	
	public void addNums(String value){
		if(mNums==null) return;
		if(value == null) return;
		if(mNums.size()>=4) return;
		mNums.add(value);
		updateDeleteButtonState();
		updateView();
		if(mNums.size()>=4){
		}
	}
	
	public void deleteNums(){
		if(mNums==null) return;
		if(mNums.isEmpty()) return;
		Log.v(TAG,"the pop elepment is " + mNums.removeLast());
		updateDeleteButtonState();
		updateView();
	}
	
	public void deleteAllNums(){
		mNums.clear();
		updateDeleteButtonState();
		updateView();
	}
	
	public int getNumsLength(){
		return mNums.size();
	}
	
	
	public String getStringNums(){
		if(mNums.size() < 4) return null;
		StringBuilder sb= new StringBuilder();
		for(int i=0;i< mNums.size();i++){
			sb.append(mNums.get(i));
		}
        String temp = sb.toString();
		Log.v(TAG,"The Input String is "+ temp);
		return temp;
	}
	
	private void updateView(){
		if(mNums==null) return;
		if(mNums.size()>4) return;
		for(int i =0; i<4;i++){
             if(i<mNums.size()){
            	 mImageViews[i].setImageResource(RES_ID[1]);
             }else{
            	 mImageViews[i].setImageResource(RES_ID[0]);
             }
		}
	}
	
	public void inputNums(String value) {
		if (getNumsLength() >= 4)
			return;
		if (getNumsLength() < 3) {
			addNums(value);
		} else if (getNumsLength() == 3) {
			addNums(value);
			if(mEditViewCallback!=null){
				mEditViewCallback.verifyPasswordAndUnlock();
			}
		}
	}
	
	public void execuErrorAnimation(){
		ValueAnimator mValueBounceAnimator = ValueAnimator.ofFloat(0f,1f);
		CycleInterpolator interpolator = new CycleInterpolator(4);
		mValueBounceAnimator.setInterpolator(interpolator);
		mValueBounceAnimator.setDuration(300);
		mValueBounceAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
				  float value = (Float) animation.getAnimatedValue(); 
				  value = value * 100;
				  setTranslationX(value);
		}});
		mValueBounceAnimator.start();	
		postDelayed(new Runnable(){
			@Override
			public void run() {
				deleteAllNums();
			}
		}, 150);
	}
	
	private void updateDeleteButtonState(){
		if(mEditViewCallback == null) return;
		if(getNumsLength() < 0 || getNumsLength() > 4) return;
		if (getNumsLength() == 0) {
			mEditViewCallback.updateButtonText(mContext.getResources().getString(R.string.cooluipin_button_cancel));
		} else {
			mEditViewCallback.updateButtonText(mContext.getResources().getString(R.string.cooluipin_button_delete));
		}
	}
	
	
	public void setEditViewCallback(EditViewCallback editViewCallback){
		mEditViewCallback = editViewCallback;
	}
	
	public interface EditViewCallback{
		void updateButtonText(String buttonText);
		void verifyPasswordAndUnlock();
	}
	
    public interface UpdateButtonTextCallback{
    	void updateButtonText(int state);
    } 
 
	public class CycleInterpolator implements Interpolator {
	    /**
	     * 
	     * @param cycles
	     */
		private float mCycles;
		
	    public CycleInterpolator(float cycles) {
	        mCycles = cycles;
	    }
	 
	    public CycleInterpolator(Context context, AttributeSet attrs) {
	        TypedArray a =
	                context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.CycleInterpolator);
	 
	        mCycles = a.getFloat(com.android.internal.R.styleable.CycleInterpolator_cycles, 1.0f);
	 
	        a.recycle();
	    }
	 
	    @Override
	    public float getInterpolation(float input) {
	    	float time =1/mCycles;
	    	for(int i =0;i<mCycles;i++){
	    		if(input<time*i){
	    			return (float)(Math.sin(2 * mCycles * Math.PI * input))*(1-time*i);
	    		}
	    	}
	    	return 0;
	    }
	 
	}
	
	

}
