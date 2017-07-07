package com.android.systemui.statusbar.phone;

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;

public class QuickSettingsConfigView extends FrameLayout implements QuickSettingsModel.IUpdateView{

	private static final int INFINITE_POS = 10000;
	private int mNumColumns = -1;
	private int mNumQuicksOld = -1;
	private int mMajorRowNum = 4;
	private int mQSTileWidth = 0;
	private int mQSTileHeight = 0;
	private boolean  mIsDraging = false;
	private MoveObject mDragObject = null;
	
	private int 	mMarginTop = 0;//-10
	private int 	mMarginBottom = 18;
	private int 	mMarginLeft = 16;//left//20
	private int 	mMarginRight = 16;//20
	private int		mStartDragingPosX = 0;
	private int		mStartDragingPosY = 0;
	private int     mHeightMargin = 0;//11
	private int		mLastDragingPosX = -100;
	private int		mLastDragingPosY = -100;
	private int		mDropDestIndex = -1;
	private int		mDragingSourceIndex  = -1;
	private int		mDragingChildId = -1;
	private int     mItemLayoutId = 0;
	private int 	mMarginTopPixel = 0;
	private int 	mMarginBottomPixel = 18;
	private int 	mMarginLeftPixel = 16;
	private int 	mMarginRightPixel = 16;
	private int     mHeightMarginPixel = 0;//12
	private int     mHeightBrightnessView = 0;
	private int     mPanelWidth = 0;
	private int     mNumQuicks = 0;
	private boolean mIsConfig = true;
	private boolean mPrivateSpace = false;
	protected View mBrightnessView;
	private BrightnessController mBrightnessController;
	protected boolean mListening;

	private boolean mEditMode = false;
	public void setEditMode(boolean edit){
		if (mIsDraging)
			stopDrag();
		mEditMode = edit;
		updateAnimation();
	}
	
	private void updateAnimation(){
		for(MoveObject obj:mMajorTileViews){
			if(obj != null && obj.mView != null){
				obj.mView.shakeAnimate(mEditMode && !mIsDraging);
			}
		}		
	}
	private ArrayList<MoveObject> mMajorTileViews = new ArrayList<MoveObject>();
	private ArrayList<QuickSettingsItemView> mQuickSettingsItemView = new ArrayList<QuickSettingsItemView>();
	
	private static String TAG = "QuickSettingsConfigView";
	private ValueAnimator mAnimator;
	private AnimatorListenerAdapter mMoveListenerAdapter = new AnimatorListenerAdapter() {			
		@Override
		public void onAnimationEnd(Animator animation) {
			for(MoveObject o : mMajorTileViews){
				o.endMove();
			}
		}

	};

	private AnimatorUpdateListener mMoveUpdateListener = new AnimatorUpdateListener(){
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			float percent = ((Float)animation.getAnimatedValue()).floatValue();
			for(MoveObject o : mMajorTileViews){
				o.onMove(percent);
			}
		}		
	};
	
	private float mDensity;
	public QuickSettingsConfigView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mHeightBrightnessView = Utilities.dipToPixel(context, 60);
        
		mAnimator = ValueAnimator.ofFloat(0, 1.0f);//
		mAnimator.addListener(mMoveListenerAdapter);
		mAnimator.addUpdateListener(mMoveUpdateListener);
		setChildrenDrawingOrderEnabled(true);
		mDensity = context.getResources().getDisplayMetrics().density;
		mMarginTopPixel *= mDensity;
		mMarginBottomPixel *= mDensity;
		mMarginLeftPixel *= mDensity;
		mMarginRightPixel *= mDensity;
		mHeightMarginPixel *= mDensity;
		
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.QuickSettingsConfigView,
                0, 0);

        if(a != null){
        mItemLayoutId = 0;
        a.getResourceId(R.styleable.QuickSettingsConfigView_quickSettingsViewItemLayout, 0);
        if(mItemLayoutId == 0){
        	mItemLayoutId = R.layout.quick_setting_config_item2;
        	mIsConfig = false;
        }
        mEditMode = mIsConfig;
//             mPrivateSpace = !a.getBoolean(R.styleable.QuickSettingsConfigView_showDomainSpacesOwner, false);
        mPrivateSpace = Utilities.isPrimaryUser();
        a.recycle();
         	LogHelper.sd(TAG, " mItemLayoutId = " + mItemLayoutId + " mIsConfig = " + mIsConfig+" mPrivateSpace="+
         			mPrivateSpace);		
        }
	}
	
	public void setListening(boolean listening) {
		if (mListening == listening)
			return;
		mListening = listening;
		if (listening) {
			mBrightnessController.registerCallbacks();
		} else {
			mBrightnessController.unregisterCallbacks();
		}
	}
	
	private RippleDrawable mRipple;
	QuickSettingsItemViewSub mDualFirst;
	QuickSettingsItemViewSub mDualLabel;
	private void setRipple(RippleDrawable tileBackground) {
        mRipple = tileBackground;
        if (getWidth() != 0) {
        	updateRippleSize(getWidth(), getHeight());
        }
	}
	
	private void updateRippleSize(int width, int height) {
        // center the touch feedback on the center of the icon, and dial it down a bit
        final int cx = width / 2;
        final int cy = /*mDual ? mIcon.getTop() + mIcon.getHeight() / 2 : */ height / 2;
        int rad = width/3;
        mRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }
	
	@Override
	public void updateView(State state) {
		if(state.order >= 0){
			QuickSettingsItemView item;
			while(state.order >= mMajorTileViews.size()){
				item = (QuickSettingsItemView)LayoutInflater.from(mContext).inflate(mItemLayoutId, null);
				item.mIsConfigItem = mIsConfig;
				item.setVisibleSecondary(state);
				LogHelper.sd(TAG,"setVisibleSecondary id="+state.id);
				mDualFirst = (QuickSettingsItemViewSub)item.findViewById(R.id.dual_first);
				mDualLabel = (QuickSettingsItemViewSub)item.findViewById(R.id.dual_secondary);
				if(state.isVisibleSecondary){
					if(mItemFirstClickListener == null){
						mItemFirstClickListener = YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace).getItemOnFirstClickListen();
						mItemSecondaryClickListener = YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace).getItemOnSecondaryClickListen();
						mItemFirstLongClickListener = YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace).getItemOnFirstLongClickListen();
					}
					LogHelper.sd(TAG,"isVisibleSecondary id="+state.id);
					mDualFirst.setClickable(true);
					mDualFirst.setFocusable(true);
					mDualFirst.setOnClickListener(mCellFirstClickListener);
					mDualFirst.setOnLongClickListener(mCellFirstLongClickListener);
					mDualFirst.setBackgroundResource(R.drawable.ripple_drawable);
		            
		            mDualLabel.setClickable(true);
		            mDualLabel.setOnClickListener(mCellSecondaryClickListener);
		            mDualLabel.setBackgroundResource(R.drawable.ripple_drawable);
		            item.setClickable(false);
		            item.setFocusable(false);
		            item.setOnClickListener(null);
				}else{
				if(mItemClickListener == null){
					mItemClickListener = YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace).getItemOnClickListen();
					mItemLongClickListener = YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace).getItemOnLongClickListen();
				}
					LogHelper.sd(TAG,"isVisibleSecondary not id="+state.id);
					mDualFirst.setClickable(false);
					mDualFirst.setFocusable(false);
					mDualLabel.setClickable(false);
					mDualLabel.setFocusable(false);
					item.setClickable(true);
		            item.setFocusable(true);
				item.setOnClickListener(mCellClickListener);
				item.setOnLongClickListener(mCellLongClickListener);
					item.setBackgroundResource(R.drawable.ripple_drawable);
				}
				mDualFirst.setQuickSettingsItemView(item);
				mDualLabel.setQuickSettingsItemView(item);
				MoveObject object = new MoveObject();
				object.mView = item;
				mMajorTileViews.add(object);
				addView(item);
				
				mQuickSettingsItemView.add(item);
			}
			item = (QuickSettingsItemView)mMajorTileViews.get(state.order).mView;
			item.updateState(state);
		}
	}
	
	protected float dip2px(int dip) {
		float scale = mContext.getResources().getDisplayMetrics().density;
		return (float) (dip * scale + 0.5f);
	}
	
	Interpolator in = new DecelerateInterpolator();

	public void setQSTitleView(float height) {
		// TODO Auto-generated method stub
		if(mNumColumns<=0)return;
		
		for (QuickSettingsItemView record : mQuickSettingsItemView) {
			if (record.getQuickSettingIndex()/mNumColumns == 1) {
				float DragY = (height - dip2px(96));
				if (DragY < 0) {
					DragY = 0;
				}
				// DragY = DragY*0.818f;
				float rata = in.getInterpolation(DragY / dip2px(272));
				record.setAlpha(rata * 1f);
				record.setScaleX(0.6f + rata * 0.4f);
				record.setScaleY(0.6f + rata * 0.4f);
			}
			if (record.getQuickSettingIndex()/mNumColumns == 2) {
				float DragY = (height - dip2px(192));
				if (DragY < 0) {
					DragY = 0;
				}
				// DragY = DragY*0.818f;
				float rata = in.getInterpolation(DragY
						/ (dip2px(272) - dip2px(96)));
				record.setAlpha(rata * 1f);
				record.setScaleX(0.6f + rata * 0.4f);
				record.setScaleY(0.6f + rata * 0.4f);
			}
			
			if (record.getQuickSettingIndex()/mNumColumns == 3) {
				float DragY = (height - dip2px(288));
				if (DragY < 0) {
					DragY = 0;
				}
				// DragY = DragY*0.818f;
				float rata = in.getInterpolation(DragY
						/ (dip2px(272) - dip2px(96)));
				record.setAlpha(rata * 1f);
				record.setScaleX(0.6f + rata * 0.4f);
				record.setScaleY(0.6f + rata * 0.4f);
			}
			
			if (record.getQuickSettingIndex()/mNumColumns == 4) {
				float DragY = (height - dip2px(288+96));
				if (DragY < 0) {
					DragY = 0;
				}
				// DragY = DragY*0.818f;
				float rata = in.getInterpolation(DragY
						/ (dip2px(272) - dip2px(96)));
				record.setAlpha(rata * 1f);
				record.setScaleX(0.6f + rata * 0.4f);
				record.setScaleY(0.6f + rata * 0.4f);
			}
		}
		//invalidate();
	}

	public void reset() {
		for (QuickSettingsItemView record : mQuickSettingsItemView) {
			if (record.getQuickSettingIndex()/mNumColumns > 0) {
				record.setAlpha(1f);
				record.setScaleX(1f);
				record.setScaleY(1f);
			}
		}
		//invalidate();
	}
	@Override
	public void resetQsView(Boolean bReinitialize){
		if(bReinitialize){
			for(QuickSettingsItemView item: mQuickSettingsItemView){
				item.setOnClickListener(null);
				item.setOnLongClickListener(null);
			}
			for(MoveObject object: mMajorTileViews){
				object.mView = null;
			}
			for(QuickSettingsItemView item: mQuickSettingsItemView){
				removeView(item);
			}
			mQuickSettingsItemView.clear();
			mMajorTileViews.clear();
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace).RemoveUpdateViewCallback(this);
		setListening(false);
	};
	
	private static final int PORTRAIT_COLUMNS = 4;
	private static final int LANDSCAPE_COLUMNS = 8;
	private int mOrientation = Configuration.ORIENTATION_PORTRAIT;
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		float mDensity = mContext.getResources().getDisplayMetrics().density;
        int orientation = newConfig.orientation;
        mOrientation = orientation;
        LogHelper.sd(TAG, " onConfigurationChanged new orientation = " + orientation + " mNumColumns:" + mNumColumns + " mMarginTopPixel:" + mMarginTopPixel);
        if(mOrientation == Configuration.ORIENTATION_PORTRAIT){
        	mNumColumns = PORTRAIT_COLUMNS;
    	}else{
    		mNumColumns = LANDSCAPE_COLUMNS;
    	}
        requestLayout();
        yqs.onConfigurationChanged();

		LogHelper.sd(TAG, "onConfigurationChanged new orientation");

    }
    
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        mPanelWidth = width;
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int N = getChildCount();
        int QsItemCount = N-1;
        /*if(mNumColumns<0 || N>mNumQuicksOld){
        	if(mOrientation == Configuration.ORIENTATION_PORTRAIT){
        		if(N > 9){
        			mNumColumns = 4;
        		}else{
        			mNumColumns = 3;
        		}
        	}else{
        		mNumColumns = LANDSCAPE_COLUMNS;
        	}
        	
        	mNumQuicksOld = N;
        }*/
        if(mOrientation == Configuration.ORIENTATION_PORTRAIT){
        	mNumColumns = PORTRAIT_COLUMNS;
    	}else{
    		mNumColumns = LANDSCAPE_COLUMNS;
    	}
		if(QsItemCount > 9){
			mMarginTopPixel = -2;
			mMarginBottomPixel = 12;
			mMarginLeftPixel = 14;
			mMarginRightPixel = 14;
			mHeightMarginPixel = 0;
		}else{
			mMarginTopPixel = 0;
			mMarginBottomPixel = 18;
			mMarginLeftPixel = 16;
			mMarginRightPixel = 16;
			mHeightMarginPixel = 0;
		}
		mMarginTopPixel *= mDensity;
		mMarginBottomPixel *= mDensity;
		mMarginLeftPixel *= mDensity;
		mMarginRightPixel *= mDensity;
		mHeightMarginPixel *= mDensity;
		int availableWidth = width - mMarginLeftPixel - mMarginRightPixel;
		mQSTileWidth = availableWidth / mNumColumns;
        View p = (View)getParent();		
		int availableHeight = p.getMeasuredHeight() - mMarginTopPixel - mMarginBottomPixel;
			mQSTileHeight = mQSTileWidth + mHeightMarginPixel;			
		for (int i = 0; i < N; ++i){
			View v = getChildAt(i);
			if (v.getVisibility() != View.GONE){
				if(v instanceof QuickSettingsItemView){
					ViewGroup.LayoutParams lp = v.getLayoutParams();
					lp.width = mQSTileWidth;				
					lp.height = mQSTileHeight;
	                int newWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
	                int newHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
	                v.measure(newWidthSpec, newHeightSpec);
				}else{
					v.measure(widthMeasureSpec, heightMeasureSpec);
				}
				
			}
		}	
		mMajorRowNum = (QsItemCount + mNumColumns - 1)/mNumColumns;
		height = mHeightBrightnessView + mQSTileHeight * mMajorRowNum + mMarginTopPixel + mMarginBottomPixel;
		LogHelper.temp(TAG, "onMeasure mNumColumns:" + mNumColumns + "width = " + width + " height = " + height + " heightMeasureSpec = " + heightMeasureSpec + 
				" mQSTileWidth = " + mQSTileWidth + " mQSTileHeight = " + mQSTileHeight);
		setMeasuredDimension(width,height);		
	}

	public void updateCallback(){
		yqs.AddUpdateViewCallback(this);
	}
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		ArrayList<MoveObject> viewList = mMajorTileViews;
		mBrightnessView.layout(0, mMarginTopPixel, 
				mPanelWidth, mHeightBrightnessView);
		
		int startX = mMarginLeftPixel;
		int startY = mMarginTopPixel + mHeightBrightnessView;
		int x = 0, y = 0, cursor = 0;
		int N = viewList.size();
		
		for (int i = 0; i < N; ++i){
			MoveObject object = viewList.get(i);
			ViewGroup.LayoutParams lp = object.mView.getLayoutParams();
			if (object.mView.getVisibility() == View.GONE){
				continue;
			}
			int row = cursor / mNumColumns;
			int col = cursor % mNumColumns;	
			x = col * mQSTileWidth + startX;
			y = row * mQSTileHeight + startY;			
			LogHelper.sd(TAG, "doLayout Child:" + i + " x = " + x + " y = " + y + " cursor = " + cursor+
					" row="+row+" col="+col);
			object.mView.layout(x, y, x + lp.width, y + lp.height);
			object.mPos.set(x, y);
			object.mCurPos.set(x, y);
			object.mDest.set(x, y);
			mMajorTileViews.get(i).mView.setQuickSettingIndex(cursor);
			cursor++;
		}

	}

	YulongQuickSettings yqs;
	@Override
	protected void onFinishInflate() {
	    super.onFinishInflate();
        yqs = YulongQuickSettingsContain.getInstance(mContext, mPrivateSpace);
        updateCallback();
        requestLayout();
        
        mBrightnessView = LayoutInflater.from(mContext).inflate(R.layout.quick_settings_brightness_dialog_new, null);
		addView(mBrightnessView);
		
		mBrightnessController = new BrightnessController(getContext(), null,
                (ToggleSlider) findViewById(R.id.brightness_slider), (CompoundButton) findViewById(R.id.brightAuto));
        setListening(true);
        
	}
	
	@Override
	protected int getChildDrawingOrder(int childCount, int i) {
		// TODO Auto-generated method stub
		//return super.getChildDrawingOrder(childCount, i);
		int ret = i;
		
		if (mDragingChildId < 0){
			ret = i;
		} else if (i == (childCount - 1)){
			ret = mDragingChildId;
		} else if (i >= mDragingChildId){
			ret = (i + 1);
		} else {
			ret = i;
		}
		//Log.d(TAG, "getChildDrawingOrder ret = " + ret + " i = " + i + " mDragingChildId = " + mDragingChildId);
		return ret;
	}
		
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mStartDragingPosX = (int)ev.getX();
			mStartDragingPosY = (int)ev.getY();
			if (mEditMode && !mIsDraging){
				startDrag();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if(mIsDraging && mDragObject != null){
				mDragObject.dragTo((int)ev.getX(), (int)ev.getY());
				onDrag((int)ev.getX(), (int)ev.getY());
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mIsDraging){
				stopDrag();
			}
			break;
		default:
			break;
		}
		return super.onInterceptTouchEvent(ev);
	}

	View.OnClickListener mItemClickListener = null;
	View.OnClickListener mCellClickListener = new View.OnClickListener() {		
		public void onClick(View v) {
			if(!mEditMode && mItemClickListener != null){
				mItemClickListener.onClick(v);
			}
		};
	};
	View.OnLongClickListener mItemLongClickListener = null;
	private View.OnLongClickListener mCellLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			if(!mEditMode && mItemLongClickListener != null){
				return mItemLongClickListener.onLongClick(v);
			}			
			return false;
		}
	};
	View.OnClickListener mItemFirstClickListener = null;
	View.OnClickListener mCellFirstClickListener = new View.OnClickListener() {		
		public void onClick(View v) {
			if(!mEditMode && mItemFirstClickListener != null){
				mItemFirstClickListener.onClick(v);
			}
		};
	};
	
	View.OnLongClickListener mItemFirstLongClickListener = null;
	private View.OnLongClickListener mCellFirstLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			if(!mEditMode && mItemFirstLongClickListener != null){
				return mItemFirstLongClickListener.onLongClick(v);
			}			
			return false;
		}
	};
	
	View.OnClickListener mItemSecondaryClickListener = null;
	View.OnClickListener mCellSecondaryClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if(!mEditMode && mItemSecondaryClickListener != null){
				mItemSecondaryClickListener.onClick(v);
			}			
		}
	};
	
	void startDrag(){
		getParent().requestDisallowInterceptTouchEvent(true);
		mIsDraging = true;
		updateAnimation();
		mDragingSourceIndex = getIndexByPos(mStartDragingPosX, mStartDragingPosY);
		if(mDragingSourceIndex < 0 || mDragingSourceIndex >= mMajorTileViews.size())
			return;
		mDragObject = mMajorTileViews.get(mDragingSourceIndex);		
		if (mDragObject != null){
			int N = getChildCount();
			for (int i = 0; i < N; ++i){
				if (mDragObject.mView == getChildAt(i)){
					mDragingChildId = i;
					break;
				}
			}
			mDragObject.dragTo(mStartDragingPosX, mStartDragingPosY);
			mDragObject.mView.setPressed(false);
		    onDrag(mStartDragingPosX, mStartDragingPosY);
		    invalidate();
		}
	}
	
	void stopDrag(){
		getParent().requestDisallowInterceptTouchEvent(false);
		mIsDraging = false;
		updateAnimation();
		int oldRow=-1;
		oldRow=mMajorTileViews.get(mDragingSourceIndex).mView.getQuickSettingIndex();
		if (mDropDestIndex >= 0){
			MoveObject changObject = null;		
			changObject = mMajorTileViews.remove(mDragingSourceIndex);
			if (mDropDestIndex > mMajorTileViews.size()){
				mDropDestIndex = mMajorTileViews.size();
			}
			mMajorTileViews.add(mDropDestIndex, changObject);
			for(MoveObject o : mMajorTileViews){
				o.cancelMove();
			}
			for (int i = 0; i < mMajorTileViews.size(); ++i){
				int id = ((QuickSettingsItemView)mMajorTileViews.get(i).mView).getQuickSettingId();
				mMajorTileViews.get(i).mView.setQuickSettingIndex(i);
				YulongQuickSettingsContain.getInstance(mContext, Utilities.isPrimaryUser()).setOrder(id, i);
			}
			YulongQuickSettingsContain.getInstance(mContext, Utilities.isPrimaryUser()).RefreshView(-1);
			
		}
		if (mDragObject != null){
			mDragObject.stopDrag();
			mDragObject = null;
		}

		mDragingSourceIndex = -1;
		mDragingChildId = -1;
		mDropDestIndex = -1;
		mLastDragingPosX = -100;
		mLastDragingPosY = -100;

		requestLayout();
	}
	
	void onDrag(int touchX, int touchY){
		int d1 = (touchX - mLastDragingPosX);
		d1 *= d1;
		int d2 = (touchY - mLastDragingPosY);
		d2 *= d2;
		if (d1 + d2 < 10){
			return;
		}
		
		mLastDragingPosX = touchX;
		mLastDragingPosY = touchY;
		
		int index = getIndexByPos(touchX, touchY);
		
		if (index != mDropDestIndex){
			mDropDestIndex = index;
			mAnimator.end();
			
			Point point = new Point();
			int minPos = Math.min(mDropDestIndex, mDragingSourceIndex + 1);
			int maxPos = Math.max(mDropDestIndex, mDragingSourceIndex - 1);
			LogHelper.sd(TAG, "onDrag mDragingSourceIndex = " + mDragingSourceIndex + " mDropDestIndex = " + mDropDestIndex
					+ " minPos:" + minPos + " maxPos:" + maxPos);
		
			int N = mMajorTileViews.size();
			for (int i = 0; i < N; ++i){
				MoveObject object = mMajorTileViews.get(i);
				if (object.mView.getVisibility() == View.GONE )
					continue;
				LogHelper.sd(TAG, "onDrag obj:" + i);
				if (i == mDragingSourceIndex){
					// nothing
				} else if (i < minPos	|| i > maxPos){
					object.startMove(object.mPos); 
				} else if (mDragingSourceIndex < mDropDestIndex){
					getPosByObjectIndex(i-1, point);
					object.startMove(point); 
				} else if (mDragingSourceIndex > mDropDestIndex){
					getPosByObjectIndex(i+1, point);
					object.startMove(point); 
				}
			}
			mAnimator.start();
		}
		
	}
	
	private int getIndexByPos(int touchX, int touchY){
		int index = mMajorTileViews.size();
		for(int i = 0; i < mMajorTileViews.size(); i++){
			MoveObject obj = mMajorTileViews.get(i);
			if (obj != null && obj.mView.getVisibility() != View.GONE){
				int left = mMajorTileViews.get(i).mView.getLeft();
				int right = mMajorTileViews.get(i).mView.getRight();
				int top = mMajorTileViews.get(i).mView.getTop();
				int bottom = mMajorTileViews.get(i).mView.getBottom();
				if(touchX >= left && touchX < right && touchY >= top && touchY < bottom)
					return i;
			}
		}
		return index;
	}
	
	private void getPosByObjectIndex(int index, Point point){
		if(index < mMajorTileViews.size()){
			int left = mMajorTileViews.get(index).mView.getLeft();
			int right = mMajorTileViews.get(index).mView.getRight();
			int top = mMajorTileViews.get(index).mView.getTop();
			int bottom = mMajorTileViews.get(index).mView.getBottom();
			point.x = left;
			point.y = top;
		}
	}

	private static final class MoveObject{
        public QuickSettingsItemView mView;
        public Point mCurPos;
        public Point mPos;
        public Point mDest;
        
        MoveObject(){
        	mCurPos = new Point();
        	mPos    = new Point();
        	mDest	= new Point();
        }
        
        public void cancelMove(){
        	mView.setTranslationX(0);
        	mView.setTranslationY(0);
        	mDest.set(mPos.x, mPos.y);
        	mCurPos.set(mPos.x, mPos.y);
        }
        
        public void endMove(){
        	Log.d("MoveObject", "endMove mDest " + mDest);
        	mCurPos.set(mDest.x, mDest.y);
        }
        
        public void onMove(float percent){
        	//Log.d("MoveObject", "onMove percent = " + percent);
        	mView.setTranslationX(mCurPos.x - mPos.x + percent * (mDest.x - mCurPos.x));
        	mView.setTranslationY(mCurPos.y - mPos.y + percent * (mDest.y - mCurPos.y));
        }
        
        public void startMove(Point dest){
        	LogHelper.sd(TAG, "startMove dest:" + dest);
        	mDest.set(dest.x, dest.y);        	
        }
        
        public void dragTo(Point dest){
        	LogHelper.sd(TAG, "dragTo dest:" + dest);
        	dragTo(dest.x, dest.y);
        }
        
        public void dragTo(int x, int y){
        	mCurPos.set(x - mView.getMeasuredWidth() / 2, y - mView.getMeasuredHeight() / 2);
        	mDest.set(mCurPos.x, mCurPos.y);
        	mView.setTranslationX(mCurPos.x - mPos.x);
        	mView.setTranslationY(mCurPos.y - mPos.y);
        }
        
        public void stopDrag(){        	
        	mDest.set(mPos.x, mPos.y);
        }
	}
}
