package com.android.systemui.quicksettings.bottom;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.SurfaceControl;
import android.view.WindowManager;

public class CatchScreenView {
	
	private static int sScreenWidth;
	private static int sScreenHeight;
	private static WindowManager mWindowManager;
	
	public static Bitmap myShot(Context context) {
		if (mWindowManager == null){
			 mWindowManager = (WindowManager) context
						.getApplicationContext().getSystemService(
								Context.WINDOW_SERVICE);
		}
		
		DisplayMetrics mDisplayMetrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getRealMetrics(mDisplayMetrics);
		sScreenWidth = mDisplayMetrics.widthPixels;
		sScreenHeight = mDisplayMetrics.heightPixels;
		
		Bitmap screenBitmap = SurfaceControl.screenshot(sScreenWidth,
				sScreenHeight);
		return screenBitmap;
    }
	
	public static Bitmap myShot(Context context, int ori) {
		WindowManager mWindowManager = (WindowManager) context
				.getApplicationContext().getSystemService(
						Context.WINDOW_SERVICE);
		DisplayMetrics mDisplayMetrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getRealMetrics(mDisplayMetrics);

		// 闁兼儳鍢茶ぐ鍥╀沪韫囨挾顔庨柛鎺戞妞存悂鎮抽敓锟� 閻庣妫勭�癸拷 濡ゅ倹锚鐎癸拷 閻忕偛绻愮粻椋庯拷闈涙鐎癸拷
		sScreenWidth = mDisplayMetrics.widthPixels;
		sScreenHeight = mDisplayMetrics.heightPixels;
		
		
		Bitmap screenBitmap;
		if (ori == Configuration.ORIENTATION_PORTRAIT){
			 screenBitmap = SurfaceControl.screenshot(sScreenWidth,
					sScreenHeight);
		}else{
			 screenBitmap = SurfaceControl.screenshot(sScreenHeight,
					sScreenWidth);
		}
//		Bitmap screenBitmap = SurfaceControl.screenshot(sScreenWidth,
//				sScreenHeight);
		return screenBitmap;
		

    }
}
