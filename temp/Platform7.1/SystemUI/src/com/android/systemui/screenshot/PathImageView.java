package com.android.systemui.screenshot;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.BoringLayout.Metrics;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PathImageView extends ImageView {

    private static final String TAG = "PathImageView";
    private Path mFingerPath, mFinalPath;
    private Paint mPaint;
    private float mLastX, mLastY;
    private float mDownX, mDownY;
    private PathEffect mPathEffect;
    private int mCurDrawState = MODE_NORMAL;
    private int mClipState = CLIP_INSIDE;
    private ArrayList<Point> mFingerPathList = new ArrayList<Point>();
    private ArrayList<Point> mPathList = new ArrayList<Point>();
    private OnModeChangeListener mListener;
    private Bitmap mImage;
    
    private static int CLIP_TOP = 0;
    private static int CLIP_BOTTOM = 1;
    private static int CLIP_RIGHT = 2;
    private static int CLIP_LEFT = 3;
    private static int CLIP_INSIDE = 4;
    private static int CLIP_NONE = 5;

    public static int MODE_NORMAL  = 0x0100;      
    public static int MODE_DRAGING = 0x0200;      
    public static int MODE_COVER   = 0x0300;      
    private Bitmap mTemp;

    public PathImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(0xffeae8dd);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5);
        
        
        mFingerPath = new Path();
        mFinalPath  = new Path();
        mFinalPath.setFillType(Path.FillType.WINDING);
        //mFingerPath.setFillType(Path.FillType.INVERSE_WINDING);
        mFingerPath.setFillType(Path.FillType.EVEN_ODD);
        
        mPathEffect = new DashPathEffect(new float []{10, 5, 5, 5}, 0);
        mPaint.setPathEffect(mPathEffect);
        
    }
    
    public void SetOnModeChangeListener(OnModeChangeListener listener){
        mListener = listener;
        listener.onChange(mCurDrawState);
    }
    
    public boolean hasEdit() {
    	RectF rect = new RectF();
    	this.mFinalPath.computeBounds(rect,true);
    	Log.e("error", "rect == " + rect);
    	if (rect.left >= rect.right
    			|| rect.top >= rect.bottom){
    		return false;
    	}
    	return true;
    }
    
    public Bitmap getClipImage(){
        
    
    	if(!hasEdit()) {
    		return mImage;
    	}
		//huangjianqing add
    	setDrawingCacheEnabled(true);   
    	buildDrawingCache();   
    	Bitmap viewbitmap = getDrawingCache();  
    	if (viewbitmap == null) {
			return null;
		}
    	mImage = viewbitmap;
        
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        Matrix  matrix = new Matrix ();
        float sx = (float)displayMetrics.widthPixels / mImage.getWidth();
        float sy = (float)displayMetrics.heightPixels / mImage.getHeight();
        matrix.postScale(sx, sy);
        //Bitmap b = mImage.copy(Config.ARGB_8888, true);//Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_4444);
        //Bitmap b = Bitmap.createBitmap(mImage, 0, 0, mImage.getWidth(), mImage.getHeight(), matrix, true);
        //b.eraseColor(Color.argb(0, 0, 0, 0));
        Bitmap b = Bitmap.createBitmap(mImage.getWidth(), mImage.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        
        Rect rcBound = new Rect();

        canvas.save();
        mFinalPath.setFillType(Path.FillType.WINDING);
        canvas.clipPath(mFinalPath);
        canvas.getClipBounds(rcBound);
        Log.e("error", "rcBound == " + rcBound);
        if (rcBound.width() <= 0 || rcBound.height() <= 0)	
        {
        	return mImage;
        }
        canvas.restore();
        
//        Log.d(TAG, "rcBound.left = " + rcBound.left
//                + " r = " + rcBound.right 
//                + " t = " + rcBound.top
//                + " b = " + rcBound.bottom);
//        if (rcBound.left >= rcBound.right
//                || rcBound.top >= rcBound.bottom){
//            b.recycle();
//
//            return mImage;
//        }
        
       
        canvas.save();
        //canvas.clipPath(mFinalPath, Region.Op.DIFFERENCE);
        //canvas.drawARGB(0x00, 0x80, 0x80, 0x80);
        //canvas.drawPath(mFingerPath, mPaint);  
        //canvas.drawPaint(mPaint);
        //canvas.drawColor(0, Mode.CLEAR);
        canvas.clipPath(mFinalPath, Region.Op.REPLACE);
        canvas.drawBitmap(mImage, 0, 0, mPaint);
        canvas.restore();
        
        //mFinalPath.setFillType(Path.FillType.INVERSE_WINDING);
        //canvas.drawPath(mFingerPath, mPaint);

        
        Bitmap bitmap = Bitmap.createBitmap(b, rcBound.left, rcBound.top, rcBound.right - rcBound.left, rcBound.bottom - rcBound.top);
		if (b != null && !b.isRecycled()) {
			b.recycle();
		}
        //System.gc();
        Log.d(TAG, "getClipImage bitmap w = " + bitmap.getWidth() + " h = " + bitmap.getHeight());
        return bitmap;
    }
    
    @Override
    public void setImageBitmap(Bitmap bm) {
        mImage = bm;
        super.setImageBitmap(bm);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        Log.d(TAG, "onTouchEvent " + action + " x = " + x + " y = " + y);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                
                mFingerPathList.clear();
                mFingerPathList.add(new Point((int)x, (int)y));

                mLastX = mDownX = x;
                mLastY = mDownY = y;
                calcMode(false);

                break;
                
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - mLastX) > 2 && Math.abs(y - mLastY) > 2){
                    
                    mFingerPathList.add(new Point((int)x, (int)y));
                    calcMode(false);
                    if (mCurDrawState == MODE_DRAGING){
                        mFingerPath.quadTo(mLastX, mLastY, x, y);
                    }
                    
                    mLastX = x;
                    mLastY = y; 
                    
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                calcMode(true);
                break;
                
            default:
                break;
        }

        invalidate();
        return true;
    }
    
    private void setDrawMode(int mode){
        if (mCurDrawState != mode){
            Log.d(TAG, "change Mode = " + mode);
            mCurDrawState = mode;

            if (mode == MODE_COVER){
                
                ArrayList<Point> tempPathList = mPathList;
                mPathList  = mFingerPathList;
                mFingerPathList = tempPathList;
                mFingerPathList.clear();
                
                mFinalPath.set(mFingerPath);
                
                //mFinalPath.quadTo(mLastX, mLastY, mDownX, mDownY);
                mFinalPath.close();
                
                

            } else if (mode == MODE_DRAGING){
                mFingerPath.reset();
                mFingerPath.moveTo(mDownX, mDownY);
                float lastX = mDownX, lastY = mDownY;
                for (int i = 1; i < mFingerPathList.size(); ++i){
                    Point p = mFingerPathList.get(i);
                    mFingerPath.quadTo(lastX, lastY, p.x, p.y);
                    lastX = p.x;
                    lastY = p.y;
                }
            }
            if (mListener != null){
                mListener.onChange(mode);
            }
        }
    }

    
    
    private void calcMode(boolean bFinish){
        int clipType = CLIP_NONE;
        int mode = MODE_NORMAL;
       
        if (mFingerPathList.size() < 10 && mCurDrawState == MODE_COVER){

        } else {
            mode = bFinish ? MODE_COVER : MODE_DRAGING;
     

            setDrawMode(mode);
        }
        mClipState = clipType;
        
    }
    
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);
        if (mFingerPath != null && mFinalPath != null) {

            if (mCurDrawState == MODE_COVER) {
                mFinalPath.setFillType(Path.FillType.WINDING);
                canvas.save();
                canvas.clipPath(mFinalPath, Region.Op.DIFFERENCE);
                
                canvas.drawARGB(0x80, 0x80, 0x80, 0x80);
                //canvas.drawPath(mFinalPath, mPaint);
                canvas.restore();                
            }
            
            if (mCurDrawState != MODE_NORMAL){
                mFingerPath.setFillType(Path.FillType.EVEN_ODD);
                canvas.drawPath(mFingerPath, mPaint);
            }


        }

    }

    /*private static final class RectangleHelpser{
        private int mWidth, mHeight;
        
        private static final int POS_INSIDE = 0;
        private static final int POS_TOP = 0x01;
        private static final int POS_BOTTOM = 0x02;
        private static final int POS_LEFT = 0x10;
        private static final int POS_RIGHT = 0x20;
        
        private static final int EDGE_WIDTH = 4;
        
        public RectangleHelpser(int width, int height){
            
        }
        public void checkMode(ArrayList<Point> path){
            int fpos = getPosition(path.get(0));
            int lpos = getPosition(path.get(path.size()));
            
            if (fpos == POS_INSIDE && lpos == POS_INSIDE){
                
            } else if (fpos != POS_INSIDE && lpos != POS_INSIDE){
             
            } else {
                
            }
        }
        
       
        private int getPosition(Point point){
            int pos = 0;
            if (point.x <= EDGE_WIDTH){
                pos |= POS_LEFT;
            } else if (point.x >= mWidth - EDGE_WIDTH){
                pos |= POS_RIGHT;
            }
            if (point.y <= EDGE_WIDTH){
                pos |= POS_TOP;
            } else if(point.y >= mHeight - EDGE_WIDTH){
                pos |= POS_BOTTOM;
            }
            return POS_INSIDE; 
        }
        
      
        private boolean sameSide(int p0, int p1){
            if (p0 == POS_INSIDE || p1 == POS_INSIDE){
                return false;
            } else if ((p0 | 0xF0) == (p1 | 0xF0) || (p0 | 0x0F) == (p1 | 0x0F)){
                return true;
            }
            return false;
        }
        
    }*/
    
    public interface OnModeChangeListener{
        void onChange(int mode);
    }
}
