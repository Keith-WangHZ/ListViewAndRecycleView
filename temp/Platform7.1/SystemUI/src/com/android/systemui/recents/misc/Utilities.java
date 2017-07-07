/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.recents.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.RectEvaluator;
import android.annotation.FloatRange;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.telecom.Log;
import android.util.ArraySet;
import android.util.IntProperty;
import android.util.Property;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.TaskViewTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* Common code */
public class Utilities {

    public static final Property<Drawable, Integer> DRAWABLE_ALPHA =
            new IntProperty<Drawable>("drawableAlpha") {
                @Override
                public void setValue(Drawable object, int alpha) {
                    object.setAlpha(alpha);
                }

                @Override
                public Integer get(Drawable object) {
                    return object.getAlpha();
                }
            };

    public static final Property<Drawable, Rect> DRAWABLE_RECT =
            new Property<Drawable, Rect>(Rect.class, "drawableBounds") {
                @Override
                public void set(Drawable object, Rect bounds) {
                    object.setBounds(bounds);
                }

                @Override
                public Rect get(Drawable object) {
                    return object.getBounds();
                }
            };

    public static final RectFEvaluator RECTF_EVALUATOR = new RectFEvaluator();
    public static final RectEvaluator RECT_EVALUATOR = new RectEvaluator(new Rect());
    public static final Rect EMPTY_RECT = new Rect();

    /**
     * @return the first parent walking up the view hierarchy that has the given class type.
     *
     * @param parentClass must be a class derived from {@link View}
     */
    public static <T extends View> T findParent(View v, Class<T> parentClass) {
        ViewParent parent = v.getParent();
        while (parent != null) {
            if (parent.getClass().equals(parentClass)) {
                return (T) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Initializes the {@param setOut} with the given object.
     */
    public static <T> ArraySet<T> objectToSet(T obj, ArraySet<T> setOut) {
        setOut.clear();
        if (obj != null) {
            setOut.add(obj);
        }
        return setOut;
    }

    /**
     * Replaces the contents of {@param setOut} with the contents of the {@param array}.
     */
    public static <T> ArraySet<T> arrayToSet(T[] array, ArraySet<T> setOut) {
        setOut.clear();
        if (array != null) {
            Collections.addAll(setOut, array);
        }
        return setOut;
    }

    /**
     * @return the clamped {@param value} between the provided {@param min} and {@param max}.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @return the clamped {@param value} between the provided {@param min} and {@param max}.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @return the clamped {@param value} between 0 and 1.
     */
    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /**
     * Scales the {@param value} to be proportionally between the {@param min} and
     * {@param max} values.
     *
     * @param value must be between 0 and 1
     */
    public static float mapRange(@FloatRange(from=0.0,to=1.0) float value, float min, float max) {
        return min + (value * (max - min));
    }

    /**
     * Scales the {@param value} proportionally from {@param min} and {@param max} to 0 and 1.
     *
     * @param value must be between {@param min} and {@param max}
     */
    public static float unmapRange(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    /** Scales a rect about its centroid */
    public static void scaleRectAboutCenter(RectF r, float scale) {
        if (scale != 1.0f) {
            float cx = r.centerX();
            float cy = r.centerY();
            r.offset(-cx, -cy);
            r.left *= scale;
            r.top *= scale;
            r.right *= scale;
            r.bottom *= scale;
            r.offset(cx, cy);
        }
    }

    /** Calculates the constrast between two colors, using the algorithm provided by the WCAG v2. */
    public static float computeContrastBetweenColors(int bg, int fg) {
        float bgR = Color.red(bg) / 255f;
        float bgG = Color.green(bg) / 255f;
        float bgB = Color.blue(bg) / 255f;
        bgR = (bgR < 0.03928f) ? bgR / 12.92f : (float) Math.pow((bgR + 0.055f) / 1.055f, 2.4f);
        bgG = (bgG < 0.03928f) ? bgG / 12.92f : (float) Math.pow((bgG + 0.055f) / 1.055f, 2.4f);
        bgB = (bgB < 0.03928f) ? bgB / 12.92f : (float) Math.pow((bgB + 0.055f) / 1.055f, 2.4f);
        float bgL = 0.2126f * bgR + 0.7152f * bgG + 0.0722f * bgB;
        
        float fgR = Color.red(fg) / 255f;
        float fgG = Color.green(fg) / 255f;
        float fgB = Color.blue(fg) / 255f;
        fgR = (fgR < 0.03928f) ? fgR / 12.92f : (float) Math.pow((fgR + 0.055f) / 1.055f, 2.4f);
        fgG = (fgG < 0.03928f) ? fgG / 12.92f : (float) Math.pow((fgG + 0.055f) / 1.055f, 2.4f);
        fgB = (fgB < 0.03928f) ? fgB / 12.92f : (float) Math.pow((fgB + 0.055f) / 1.055f, 2.4f);
        float fgL = 0.2126f * fgR + 0.7152f * fgG + 0.0722f * fgB;

        return Math.abs((fgL + 0.05f) / (bgL + 0.05f));
    }

    /** Returns the base color overlaid with another overlay color with a specified alpha. */
    public static int getColorWithOverlay(int baseColor, int overlayColor, float overlayAlpha) {
        return Color.rgb(
            (int) (overlayAlpha * Color.red(baseColor) +
                    (1f - overlayAlpha) * Color.red(overlayColor)),
            (int) (overlayAlpha * Color.green(baseColor) +
                    (1f - overlayAlpha) * Color.green(overlayColor)),
            (int) (overlayAlpha * Color.blue(baseColor) +
                    (1f - overlayAlpha) * Color.blue(overlayColor)));
    }

    /**
     * Cancels an animation ensuring that if it has listeners, onCancel and onEnd
     * are not called.
     */
    public static void cancelAnimationWithoutCallbacks(Animator animator) {
        if (animator != null && animator.isStarted()) {
            removeAnimationListenersRecursive(animator);
            animator.cancel();
        }
    }

    /**
     * Recursively removes all the listeners of all children of this animator
     */
    public static void removeAnimationListenersRecursive(Animator animator) {
        if (animator instanceof AnimatorSet) {
            ArrayList<Animator> animators = ((AnimatorSet) animator).getChildAnimations();
            for (int i = animators.size() - 1; i >= 0; i--) {
                removeAnimationListenersRecursive(animators.get(i));
            }
        }
        animator.removeAllListeners();
    }

    /**
     * Sets the given {@link View}'s frame from its current translation.
     */
    public static void setViewFrameFromTranslation(View v) {
        RectF taskViewRect = new RectF(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        taskViewRect.offset(v.getTranslationX(), v.getTranslationY());
        v.setTranslationX(0);
        v.setTranslationY(0);
        v.setLeftTopRightBottom((int) taskViewRect.left, (int) taskViewRect.top,
                (int) taskViewRect.right, (int) taskViewRect.bottom);
    }

    /**
     * Returns a view stub for the given view id.
     */
    public static ViewStub findViewStubById(View v, int stubId) {
        return (ViewStub) v.findViewById(stubId);
    }

    /**
     * Returns a view stub for the given view id.
     */
    public static ViewStub findViewStubById(Activity a, int stubId) {
        return (ViewStub) a.findViewById(stubId);
    }

    /**
     * Updates {@param transforms} to be the same size as {@param tasks}.
     */
    public static void matchTaskListSize(List<Task> tasks, List<TaskViewTransform> transforms) {
        // We can reuse the task transforms where possible to reduce object allocation
        int taskTransformCount = transforms.size();
        int taskCount = tasks.size();
        if (taskTransformCount < taskCount) {
            // If there are less transforms than tasks, then add as many transforms as necessary
            for (int i = taskTransformCount; i < taskCount; i++) {
                transforms.add(new TaskViewTransform());
            }
        } else if (taskTransformCount > taskCount) {
            // If there are more transforms than tasks, then just subset the transform list
            transforms.subList(taskCount, taskTransformCount).clear();
        }
    }

    /**
     * Used for debugging, converts DP to PX.
     */
    public static float dpToPx(Resources res, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    /**
     * Adds a trace event for debugging.
     */
    public static void addTraceEvent(String event) {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, event);
        Trace.traceEnd(Trace.TRACE_TAG_VIEW);
    }

    public static boolean isDescendentAccessibilityFocused(View v) {
        if (v.isAccessibilityFocused()) {
            return true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (isDescendentAccessibilityFocused(vg.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Returns the application configuration, which is independent of the activity's current
     * configuration in multiwindow.
     */
    public static Configuration getAppConfiguration(Context context) {
        return context.getApplicationContext().getResources().getConfiguration();
    }

    /**
     * Returns a lightweight dump of a rect.
     */
    public static String dumpRect(Rect r) {
        if (r == null) {
            return "N:0,0-0,0";
        }
        return r.left + "," + r.top + "-" + r.right + "," + r.bottom;
    }
	public static int pixelToDip(Context context, float pixelValue) {
		float density = context.getResources().getDisplayMetrics().density;
		int dipValue = (int) (pixelValue / density + 0.5f);
		//System.out.println("pixelToDip---> pixelValue=" + pixelValue+ ",density=" + density + ",dipValue=" + dipValue);
		return dipValue;
	}

	public static int dipToPixel(Context context, float dipValue) {
		float density = context.getResources().getDisplayMetrics().density;
		int pixelValue = (int) (dipValue * density + 0.5f);
		//System.out.println("dipToPixel---> dipValue=" + dipValue + ",density="+ density + ",pixelValue=" + pixelValue);
		return pixelValue;
	}

	public static int pixelToSp(Context context, float pixelValue) {
		float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
		int sp = (int) (pixelValue / scaledDensity + 0.5f);
		//System.out.println("pixelToSp---> pixelValue=" + pixelValue+ ",scaledDensity=" + scaledDensity + ",sp=" + sp);
		return sp;
	}

	public static int spToPixel(Context context, float spValue) {
		float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
		int pixelValue = (int) (spValue * scaledDensity + 0.5f);
		//System.out.println("spToPixel---> spValue=" + spValue+ ",scaledDensity=" + scaledDensity + ",pixelValue="+ pixelValue);
		return pixelValue;
	}
	private static int sIconWidth = -1;
	private static int sIconHeight = -1;
	public static int sIconTextureWidth = -1;
	public static int sIconTextureHeight = -1;
	public static float compressRatio = 0.1f;
	public static float compressRatioMax = 0.16f;
	private static Bitmap mIconPSBitmap;
	private static final Canvas sCanvas = new Canvas();
	private static final Rect sOldBounds = new Rect();
	private static final Paint mMaskPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static int []myColor;
	static {
		sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
				Paint.FILTER_BITMAP_FLAG));
		mMaskPaint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
	}
	public static void initStatics(Context context) {
		final Resources resources = context.getResources();
		sIconWidth = sIconHeight = (int) resources
				.getDimension(R.dimen.app_icon_size);
		sIconTextureWidth = sIconTextureHeight = sIconWidth;
	}
//	public static void setIconSize(int widthPx) {
//		sIconWidth = sIconHeight = widthPx;
//		sIconTextureWidth = sIconTextureHeight = widthPx;
//	}
//	public static Bitmap setIconPSBitmap(Bitmap bitmap) {
//		Bitmap old = mIconPSBitmap;
//		mIconPSBitmap = bitmap;
//		return old;
//	}
	private static Bitmap mIconNewBgBitmap;
//	public static Bitmap setIconNewBgBitmap(Bitmap bitmap) {
//		Bitmap old = mIconNewBgBitmap;
//		mIconNewBgBitmap = bitmap;
//		return old;
//	}
	private static Bitmap mIconClipBitmap = null;
	private static Bitmap mIconClipEdgeBitmap = null;
//	public static Bitmap setIconClipBitmap(Bitmap bitmap) {
//		Bitmap old = mIconClipBitmap;
//		mIconClipBitmap = bitmap;
//		return old;
//	}
	public static int[] setPixelsForColor(Bitmap bit, int rc, int gc, int bc){
		if(null == bit)return null;
		int[] arrBmp = new int[bit.getWidth()*bit.getHeight()];
        bit.getPixels(arrBmp,0,bit.getWidth(),0,0,bit.getWidth(),bit.getHeight());
        int WIDTH=bit.getWidth();
        int HEIGHT=bit.getHeight();
        int STRIDE=WIDTH;
		int[] colors=new int[STRIDE*HEIGHT];
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
               int dRgb = arrBmp[y*STRIDE+x];
				int a = (dRgb & 0xff000000) >> 24; 
				int r = rc;
				int g = gc;
				int b = bc;
				colors[y*STRIDE+x]=(a<<24)|(r<<16)|(g<<8)|(b);
			}
		}
		return colors;
	}
	public static void setCurrentIconClipBitmap(Context context){
	}
	public static void setCurrentIconClipEdgeBitmap(Context context){
	}

	public static Bitmap formatIconBitmap(Bitmap srcBitmap, Context context, int hashCode) {
		return formatIconBitmap(srcBitmap, context, hashCode, true);
	}
	
	public static int[] setPixelsForFrameColor(Bitmap bit, int rc, int gc, int bc){
		if(null == bit)return null;
		int[] arrBmp = new int[bit.getWidth()*bit.getHeight()];
        bit.getPixels(arrBmp,0,bit.getWidth(),0,0,bit.getWidth(),bit.getHeight());
        int WIDTH=bit.getWidth();
        int HEIGHT=bit.getHeight();
        int STRIDE=WIDTH;
		int[] colors = myColor;
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
               int dRgb = arrBmp[y*STRIDE+x];
				int a = (dRgb & 0xff000000) >> 24; 
				int r = 0xff;
				int g = 0xff;
				int b = 0xff;
				colors[y*STRIDE+x]=(a<<24)|(r<<16)|(g<<8)|(b);
			}
		}
		return colors;
	}
	public static int[] setPixelsForEdgeColor(Bitmap bit, int rc, int gc, int bc){
		if(null == bit)return null;
		int[] arrBmp = new int[bit.getWidth()*bit.getHeight()];
        bit.getPixels(arrBmp,0,bit.getWidth(),0,0,bit.getWidth(),bit.getHeight());
        int WIDTH=bit.getWidth();
        int HEIGHT=bit.getHeight();
        int STRIDE=WIDTH;
		int[] colors = myColor;
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
               int dRgb = arrBmp[y*STRIDE+x];
               int srcRgb = colors[y*STRIDE+x];
				int a = (srcRgb & 0xff000000) >> 24; 
				int r = rc;
				int g = gc;
				int b = bc;
				int a1 = (dRgb & 0xff000000) >> 24; 
				if(a1!=0){
					a = a1;
					r = 0;
					g = 0;
					b = 0;
					colors[y*STRIDE+x]=(a<<24)|(r<<16)|(g<<8)|(b);
				}
			}
		}
		return colors;
	}
	public static Bitmap resizeImage(Bitmap bitmap, int w, int h)   
    {    
        Bitmap BitmapOrg = bitmap;    
        int width = BitmapOrg.getWidth();    
        int height = BitmapOrg.getHeight();    
        int newWidth = w;    
        int newHeight = h;    
        float scaleWidth = ((float) newWidth) / width;    
        float scaleHeight = ((float) newHeight) / height;    
        Matrix matrix = new Matrix();    
        matrix.postScale(scaleWidth, scaleHeight);    
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,    
                        height, matrix, true);    
        return resizedBitmap;    
    } 
	public static Drawable resizeImage2(String path,  
            int width,int height)   
    {  
        BitmapFactory.Options options = new BitmapFactory.Options();  
        options.inJustDecodeBounds = true;//don't load bitmap to memory
        BitmapFactory.decodeFile(path,options);   
        int outWidth = options.outWidth;  
        int outHeight = options.outHeight;  
        options.inDither = false;  
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;  
        options.inSampleSize = 1;  
        if (outWidth != 0 && outHeight != 0 && width != 0 && height != 0)   
        {  
            int sampleSize=(outWidth/width+outHeight/height)/2;  
            options.inSampleSize = sampleSize;  
        }  
        options.inJustDecodeBounds = false;  
        return new BitmapDrawable(BitmapFactory.decodeFile(path, options));       
    }  
	public static Bitmap drawableToBitamp(Drawable drawable) {
		if(drawable == null){
			return null;
		}
		if (drawable instanceof BitmapDrawable) {  
            return ((BitmapDrawable) drawable).getBitmap();  
        }
		Bitmap bitmap;
		int w = drawable.getIntrinsicWidth();
		int h = drawable.getIntrinsicHeight();
		Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.RGB_565;
		bitmap = Bitmap.createBitmap(w, h, config);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, w, h);
		drawable.draw(canvas);
		return bitmap;
	}
	public static Drawable bitmapToDrawable(Bitmap bp){
		return new BitmapDrawable(bp);
	}
	public static Boolean isIvviOrCoolpad() {
		Boolean b = false;
		String brand = SystemProperties.get("ro.product.brand", "");
		if (brand.equals("ivvi")) {
			b = true;
		} else {
			b = false;
		}
		return b;
	}
	public static Boolean isSuperD() {
		Boolean b = false;
		String brand = SystemProperties.get("ro.product.brand", "");
		if (brand.equals("SuperD")) {
			b = true;
		} else {
			b = false;
		}
		return b;
	}
	public static int getCoolpadThemeStyle(){
		int style = 0;
		int myStyle = 1;
    	String str = SystemProperties.get("ro.coolpad.ui.theme", "");
    	if(str != null){
    		if(str.contains("GOLD")){
    			style = 1;
        	}else if(str.contains("YOUNG")){
        		style = 2;
        		myStyle = 2;
        	}else{
				if (Build.MODEL.contains("A8")) {
        			style = 1;
        		}else{
        			style = 3;
        		}
        	}
    	}
    	LogHelper.sd("","initActionBar style ="+style+" str="+str);
    	return myStyle;
	}
	public static boolean getMiscInterfaceResult(String keyName) {
		boolean resultStr = false;
		try {
			Class<?> mClass = Class
					.forName("com.yulong.android.feature.FeatureConfig");
			Object mObject = mClass.newInstance();
			Method method = mClass.getMethod("getBooleanValue", String.class);
			resultStr = (Boolean) method.invoke(mObject, keyName);
		} catch (Exception e) {
			resultStr = false;
		}
		return resultStr;
	}
	public static boolean isSupportOversea(){
		boolean mIsOverSea= false;
		mIsOverSea = getMiscInterfaceResult("is_support_oversea");
		return mIsOverSea;
	}
	public static boolean showDragDownQuickSettings(){
		return !showDragUpQuickSettings();
	}
	public static boolean showDragUpQuickSettings(){
		return true;
	}
	public static boolean showFullGaussBlurForDDQS(){//DDQS:DragDownQuickSettings
		return false;
	}
    public void saveBitmap(Bitmap bm){
    	saveBitmap(bm, "");
    }
    static int sn = 10000;
 	public static void saveBitmap(final Bitmap bm, final String s) {
 		new Thread(new Runnable() {

			@Override
			public void run() {
				LogHelper.sd("", "saveBitmap begin");
		 		sn++;
		 		String picName = "pic_"+sn+"_"+s+".png";
		 		File f = new File("/sdcard/mypic/", picName);
		 		if (f.exists()) {
		 			f.delete();
		 		}
		 		try {
		 			FileOutputStream out = new FileOutputStream(f);
		 			bm.compress(Bitmap.CompressFormat.PNG, 90, out);
		 			out.flush();
		 			out.close();
		 			LogHelper.sd("","saveBitmap end");
		 		} catch (FileNotFoundException e) {
		 			e.printStackTrace();
		 		} catch (IOException e) {
		 			e.printStackTrace();
		 		}
			}
		}).start();
 	}
 	
 	private static Bitmap mIconTheme = null;
 	private static Bitmap mIconThemeResize = null;
	private static Bitmap mIconClipBitmapTheme = null;

	public static void readLauncherThemeBitmap(Context context) {
		final String DENSITY_NAME;
		float density = context.getResources().getDisplayMetrics().density;
		if (Float.compare(density, (float) 3.0) >= 0) {
			DENSITY_NAME = "drawable-xxhdpi/";
		} else if (Float.compare(density, (float) 2.0) >= 0) {
			DENSITY_NAME = "drawable-xhdpi/";
		} else if (Float.compare(density, (float) 1.5) >= 0) {
			DENSITY_NAME = "drawable-hdpi/";
		} else {
			DENSITY_NAME = "drawable-mdpi/";
		}

		String picPathThree = "data/data/theme/com.yulong.android.launcher3/res/"
				+ DENSITY_NAME;
		String picPathDefault = "system/lib/uitechno/defaulttheme/com.yulong.android.launcher3/res/"
				+ DENSITY_NAME;
		String picName = "yl_icon_clip.png";
		File f = new File(picPathThree, picName);
		if (f.exists()) {
			try {
				mIconTheme = BitmapFactory.decodeFile(picPathThree
						+ picName);

			} catch (Exception e) {
			}
		} else {
			f = new File(picPathDefault, picName);
			if (f.exists()) {
				try {
					mIconTheme = BitmapFactory.decodeFile(picPathDefault
							+ picName);
				} catch (Exception e) {
				}
			}
		}
	}
	
	public final static Boolean mNotUseBitmapMask = true;
	public static Bitmap formatIconBitmapTheme(Bitmap srcBitmap, Context context, boolean resize) {
		if(mNotUseBitmapMask){
			return srcBitmap;
		}
		if(srcBitmap == null){
			return null;
		}
		Resources res = context.getResources();
		if(sIconWidth <= 0){
			initStatics(context);
		}
		if(true){
			if (mIconClipBitmapTheme == null) {
				//readLauncherThemeBitmap(context);
				
				if(mIconTheme != null){
					mIconClipBitmapTheme = resize ? resizeImage(mIconTheme, sIconTextureWidth, sIconTextureHeight)
							: mIconTheme;
				}else{
					Bitmap iconClipBitmap;
					if(isIvviOrCoolpad()){
						iconClipBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.yl_icon_clip_theme_ivvi);
					}else{
						iconClipBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.yl_icon_clip_theme_coolpad);
					}
					
					if (iconClipBitmap != null) {
						mIconClipBitmapTheme = resize ? resizeImage(iconClipBitmap, sIconTextureWidth, sIconTextureHeight)
								: iconClipBitmap;
						if (iconClipBitmap != null && iconClipBitmap.isRecycled()) {
							iconClipBitmap.recycle();
							iconClipBitmap = null;
						}
					}
				}
				
			}else{
				
			}
			Bitmap iconBitmapMask = Bitmap.createBitmap(mIconClipBitmapTheme);
			Bitmap bitmap = null;
			Bitmap icon = null;
			int w = dipToPixel(context, 39);
			int h = dipToPixel(context, 39);
			int w1 = sIconTextureWidth;
			int h1 = sIconTextureHeight;
			icon = resizeImage(srcBitmap, w, h);
			Bitmap iconBitmap = Bitmap.createBitmap(sIconTextureWidth,
					sIconTextureHeight, Config.ARGB_8888);
			Canvas iconCanvas = new Canvas();
			iconCanvas.setBitmap(iconBitmap);
			iconCanvas.drawColor(Color.TRANSPARENT);
			Rect rect = new Rect(0, 0, sIconTextureWidth, sIconTextureHeight);
			int mNumberX = (w1 - w) / 2;
			int mNumberY = (h1 - h) / 2;
			iconCanvas.drawBitmap(iconBitmapMask, null, rect, null);
			if (iconBitmapMask != null && !iconBitmapMask.isRecycled()) {
				mMaskPaint1.setXfermode(new PorterDuffXfermode(
						PorterDuff.Mode.SRC_IN));
				mMaskPaint1.setAntiAlias(true);
				iconCanvas.drawBitmap(icon, mNumberX, mNumberY, mMaskPaint1);
			}
			if (icon != null && icon.isRecycled()) {
				icon.recycle();
			}
			if (iconBitmapMask != null && iconBitmapMask.isRecycled()) {
				iconBitmapMask.recycle();
			}
			return iconBitmap;
		}
	return null;
}
	public static Bitmap formatIconBitmap(Bitmap srcBitmap, Context context, int i, boolean resize) {
		if(mNotUseBitmapMask){
			return srcBitmap;
		}
		if(srcBitmap == null){
			return null;
		}
		Resources res = context.getResources();
		if(sIconWidth <= 0){
			initStatics(context);
		}
		if(false){
		if(mIconClipBitmap == null){
				Bitmap iconClipBitmap;
				if(isIvviOrCoolpad()){
					iconClipBitmap = BitmapFactory.decodeResource(res, R.drawable.yl_icon_clip_ivvi);
				}else{
					iconClipBitmap = BitmapFactory.decodeResource(res, R.drawable.yl_icon_clip);
				}
				mIconClipBitmap = resizeImage(iconClipBitmap, sIconTextureWidth, sIconTextureHeight);
				if (iconClipBitmap != null && iconClipBitmap.isRecycled()) {
					iconClipBitmap.recycle();
				}
		}
		Bitmap bitmap = null;
		Bitmap icon = null;
		int w = dipToPixel(context, 36);
		int h = dipToPixel(context, 36);
		int w1 = sIconTextureWidth;
		int h1 = sIconTextureHeight;
		icon = resizeImage(srcBitmap, w, h);
		Bitmap iconBitmap = Bitmap.createBitmap(sIconTextureWidth,
				sIconTextureHeight, Config.ARGB_8888);
		Canvas iconCanvas = new Canvas();
		iconCanvas.drawColor(Color.TRANSPARENT);
		iconCanvas.setBitmap(iconBitmap);
		Rect rect = new Rect(0, 0, sIconTextureWidth, sIconTextureHeight);
		Rect rect1 = new Rect(0, 0, w, h);
		int mNumberX = (w1-w)/2;
		int mNumberY = (h1-h)/2;
		iconCanvas.drawBitmap(icon, mNumberX, mNumberY, null);
		if (mIconClipBitmap != null && !mIconClipBitmap.isRecycled()) {
			mMaskPaint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
			mMaskPaint1.setAntiAlias(true);
			iconCanvas.drawBitmap(mIconClipBitmap, rect, rect, mMaskPaint1);
		}
		if(icon!=null && icon.isRecycled()){
			icon.recycle();
		}
		return iconBitmap;
	}
		if(true){
			if (mIconClipBitmap == null) {
				Bitmap iconClipBitmap;
				if(isIvviOrCoolpad()){
					iconClipBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.yl_icon_mask_ivvi);
				}else{
					iconClipBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.yl_icon_mask_coolpad);
				}
				if (iconClipBitmap != null) {
					mIconClipBitmap = resize ? resizeImage(iconClipBitmap, sIconTextureWidth, sIconTextureHeight)
							: iconClipBitmap;
					if (iconClipBitmap != null && iconClipBitmap.isRecycled()) {
						iconClipBitmap.recycle();
						iconClipBitmap = null;
					}
				}
			}
			if (mIconClipEdgeBitmap == null) {
				Bitmap iconClipEdgeBitmap;
				if(isIvviOrCoolpad()){
					iconClipEdgeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.yl_icon_clip_ivvi);
				}else{
					iconClipEdgeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.yl_icon_clip);
				}
				if(iconClipEdgeBitmap != null){
				mIconClipEdgeBitmap = resizeImage(iconClipEdgeBitmap, sIconTextureWidth, sIconTextureHeight);
				if (iconClipEdgeBitmap != null && iconClipEdgeBitmap.isRecycled()) {
					iconClipEdgeBitmap.recycle();
						iconClipEdgeBitmap = null;
				}
			}
			}
			if(mIconClipBitmap != null && mIconClipEdgeBitmap != null && myColor == null){
				int WIDTH=mIconClipBitmap.getWidth();
		        int HEIGHT=mIconClipBitmap.getHeight();
		        int STRIDE=WIDTH;
				myColor=new int[STRIDE*HEIGHT];
				int clr = res.getColor(R.color.yl_list_content_line_color);
				int red   = (clr & 0x00ff0000) >> 16;  
				int green = (clr & 0x0000ff00) >> 8;
				int blue = clr & 0x000000ff;
				setPixelsForFrameColor(mIconClipBitmap, red, green, blue);
				setPixelsForEdgeColor(mIconClipEdgeBitmap, 0xff, 0xff, 0xff);
			}
			Bitmap iconBitmapMask = Bitmap.createBitmap(myColor, sIconTextureWidth, sIconTextureHeight, Bitmap.Config.ARGB_8888); 
			Bitmap bitmap = null;
			Bitmap icon = null;
			int w = dipToPixel(context, 39);
			int h = dipToPixel(context, 39);
			int w1 = sIconTextureWidth;
			int h1 = sIconTextureHeight;
			icon = resizeImage(srcBitmap, w, h);
			Bitmap iconBitmap = Bitmap.createBitmap(sIconTextureWidth,
					sIconTextureHeight, Config.ARGB_8888);
			Canvas iconCanvas = new Canvas();
			iconCanvas.drawColor(Color.TRANSPARENT);
			iconCanvas.setBitmap(iconBitmap);
			Rect rect = new Rect(0, 0, sIconTextureWidth, sIconTextureHeight);
			//Rect rect1 = new Rect(0, 0, w, h);
			int mNumberX = (w1 - w) / 2;
			int mNumberY = (h1 - h) / 2;
			iconCanvas.drawBitmap(icon, mNumberX, mNumberY, null);
			if (iconBitmapMask != null && !iconBitmapMask.isRecycled()) {
				mMaskPaint1.setXfermode(new PorterDuffXfermode(
						PorterDuff.Mode.SRC_ATOP));
				mMaskPaint1.setAntiAlias(true);
				iconCanvas.drawBitmap(iconBitmapMask, rect, rect, mMaskPaint1);
			}
			if (icon != null && icon.isRecycled()) {
				icon.recycle();
			}
			if (iconBitmapMask != null && iconBitmapMask.isRecycled()) {
				iconBitmapMask.recycle();
			}
			return iconBitmap;
		}
	return null;
}
	public static boolean isPrimaryUser() {
		/*UserInfo userInfo = null;
		try {
			userInfo = ActivityManagerNative.getDefault().getCurrentUser();
		} catch (RemoteException e) {
			return true;
		}
		return userInfo != null && userInfo.isPrimary();*/
		return true;
	}
	public static Boolean needFakeNavigationBarView(){
		return false;
	}
	
	public static void traversalView(View view) {
        if(null == view) {
            return;
        }
        int j = 0;
        if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            View v = viewGroup;
        	/*Log.d("","400004j="+j+" id="+v.getId()+" name="+v.getClass().toString()+
        			" alpha="+v.getAlpha()+" vis="+v.getVisibility());*/
            LinkedList<ViewGroup> queue = new LinkedList<ViewGroup>();
            queue.add(viewGroup);
            while(!queue.isEmpty()) {
                ViewGroup current = queue.removeFirst();
                //dosomething
                for(int i = 0; i < current.getChildCount(); i ++) {
                	j++;
                    if(current.getChildAt(i) instanceof ViewGroup) {
                    	View v1 = current.getChildAt(i);
                    	/*Log.d("","400004j="+j+" id="+v1.getId()+" name="+v1.getClass().toString()+
                    			" alpha="+v1.getAlpha()+" vis="+v1.getVisibility());*/
                        queue.addLast((ViewGroup) current.getChildAt(i));
                    }else {
                        //dosomething
                    	/*Log.d("","400004j="+j+" id="+current.getId()+" name="+current.getClass().toString()+
                    			" alpha="+current.getAlpha()+" vis="+current.getVisibility());*/
                    }
                }
            }
        }else {
            //dosomething
        	j++;
        	/*Log.d("","400004j="+j+" id="+view.getId()+" name="+view.getClass().toString()+
        			" alpha="+view.getAlpha()+" vis="+view.getVisibility());*/
        }
    }//traversalView
}
