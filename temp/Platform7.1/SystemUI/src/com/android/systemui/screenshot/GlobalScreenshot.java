/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.screenshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.Notification.BigPictureStyle;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.screenshot.GlobalScreenshot.ScreenShotTask;

/**
 * POD used in the AsyncTask which saves an image in the background.
 */
class SaveImageInBackgroundData {
    Context context;
    Bitmap image;
    Uri imageUri;
    Runnable finisher;
    int iconSize;
    int result;
    int finishOption;
    int taskId;
}

/**
 * An AsyncTask that saves an image to the media store in the background.
 */
class SaveImageInBackgroundTask extends AsyncTask<SaveImageInBackgroundData, Void,
        SaveImageInBackgroundData> {
	static final String TAG = "SaveImageInBackgroundTask";
    private static final String SCREENSHOTS_DIR_NAME = "ivvi/Screenshots";
    private static final String SCREENSHOT_FILE_NAME_TEMPLATE = "Screenshot_%s.png";
    private static final String SCREENSHOT_FILE_PATH_TEMPLATE = "%s/%s/%s";

    private int mNotificationId;
    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private String mImageFileName;
    private ArrayList<String> mImageFilePaths = new ArrayList<String>();
    private long mImageTime = System.currentTimeMillis();
    private BigPictureStyle mNotificationStyle;

    // WORKAROUND: We want the same notification across screenshots that we update so that we don't
    // spam a user's notification drawer.  However, we only show the ticker for the saving state
    // and if the ticker text is the same as the previous notification, then it will not show. So
    // for now, we just add and remove a space from the ticker text to trigger the animation when
    // necessary.
    private static boolean mTickerAddSpace;
    private Context mContext;
    private Intent  mShareIntent = new Intent();
    private Intent  mEditIntent  = new Intent();

    private static String []mCustomPaths = {"/mnt/external_sd","/mnt/sdcard","/mnt/extsd","/mnt/sdcard2"};
    private static boolean mInitStoragePath = false;
    private static ArrayList<String> mInternalStoragePaths = new ArrayList<String>();
    private static ArrayList<String> mExternalStoragePaths = new ArrayList<String>();
    private void AddStoragePathsToUsblePath(ArrayList<String> mPaths,String strPath,StorageManager storageManager){
    	if (storageManager != null){
    		String strStorageState = "";
    		try{
    			strStorageState = storageManager.getVolumeState(strPath);
    		}catch(Exception e){
    			LogHelper.se(TAG, "AddStoragePathsToUsblePath storageManager.getVolumeState Exception path = " + strPath + " exception = " + e);
    		}    		
    		if (strStorageState.equals(Environment.MEDIA_MOUNTED)){
                strPath += "/" + SCREENSHOTS_DIR_NAME;
    	        File file = new File(strPath);
    	        if (file != null ){
    	        	file.mkdirs();
    	        	if (file.exists()){
                        LogHelper.sv(TAG, "AddStoragePathsToUsblePath path mounted :" + strPath);	            		
            			mPaths.add(strPath);     	        		
    	        	}   	        	
    	        }
    		}
    	}
    }
    private void getUsablePaths(Context context,ArrayList<String> mPaths){
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (!mInitStoragePath){
    		mInitStoragePath = true;
            if (null != storageManager) {
	            StorageVolume[] volumeStorages = storageManager.getVolumeList();
	            if (null != volumeStorages) {
	            	for (int i = 0; i < volumeStorages.length; i++){
	                    LogHelper.sv(TAG, "getUsablePaths " + i + ":" + volumeStorages[i]);	            		
	            		if (!volumeStorages[i].isRemovable()){
	            			mInternalStoragePaths.add(volumeStorages[i].getPath());
	            		}else{
	            			mExternalStoragePaths.add(volumeStorages[i].getPath());
	            		}
	            	}
	            }
            }
	        for(int iCustomPath = 0; iCustomPath <= mCustomPaths.length; iCustomPath++){
	        	String strPath;
	        	if (iCustomPath == mCustomPaths.length){
	        		strPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	        	}else{
	        		strPath = mCustomPaths[iCustomPath];
	        	}
	        	int iInternal;
	            for (iInternal = 0; iInternal < mInternalStoragePaths.size(); iInternal++){
	            	if (mInternalStoragePaths.get(iInternal).equalsIgnoreCase(strPath)){
	            		break;
	            	}
	            }
	            int iExternal;
	            for (iExternal = 0; iInternal == mInternalStoragePaths.size() && iExternal < mExternalStoragePaths.size(); iExternal++){
	            	if (mExternalStoragePaths.get(iExternal).equalsIgnoreCase(strPath)){
	            		break;
	            	}
	            }	    	            
	            if (iInternal == mInternalStoragePaths.size() && iExternal == mInternalStoragePaths.size()){
	            	mInternalStoragePaths.add(strPath);
	            }
	        }    
        }	
    	for (int i = 0; i < mExternalStoragePaths.size(); i++){
    		String strPath = mExternalStoragePaths.get(i);
    		AddStoragePathsToUsblePath(mPaths,strPath,storageManager);
    	}
    	for (int i = 0; i < mInternalStoragePaths.size(); i++){
    		String strPath = mInternalStoragePaths.get(i);
    		AddStoragePathsToUsblePath(mPaths,strPath,storageManager);
    	}
    }	
    
    SaveImageInBackgroundTask(Context context, SaveImageInBackgroundData data,
            NotificationManager nManager, int nId) {
        Resources r = context.getResources();
        mContext = context;
        // Prepare all the output metadata
        mImageTime = System.currentTimeMillis();
        String imageDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(mImageTime));
        
        mImageFileName = String.format(SCREENSHOT_FILE_NAME_TEMPLATE, imageDate);
        LogHelper.sv(TAG, "SaveImageInBackgroundTask mImageFileName = " + mImageFileName);
        getUsablePaths(context,mImageFilePaths);        
        LogHelper.sv(TAG, "SaveImageInBackgroundTask mImageFilePaths = " + mImageFilePaths);

        // Create the large notification icon
        int imageWidth = data.image.getWidth();
        int imageHeight = data.image.getHeight();
        int iconSize = data.iconSize;

        final int shortSide = imageWidth < imageHeight ? imageWidth : imageHeight;
        Bitmap preview = Bitmap.createBitmap(shortSide, shortSide, data.image.getConfig());
        Canvas c = new Canvas(preview);
        Paint paint = new Paint();
        ColorMatrix desat = new ColorMatrix();
        desat.setSaturation(0.25f);
        paint.setColorFilter(new ColorMatrixColorFilter(desat));
        Matrix matrix = new Matrix();
        matrix.postTranslate((shortSide - imageWidth) / 2,
                            (shortSide - imageHeight) / 2);
        c.drawBitmap(data.image, matrix, paint);
        c.drawColor(0x40FFFFFF);

        Bitmap croppedIcon = Bitmap.createScaledBitmap(preview, iconSize, iconSize, true);

        // Show the intermediate notification
        mTickerAddSpace = !mTickerAddSpace;
        mNotificationId = nId;
        mNotificationManager = nManager;
        mNotificationBuilder = new Notification.Builder(context)
            .setTicker(r.getString(R.string.screenshot_saving_ticker)
                    + (mTickerAddSpace ? " " : ""))
            .setContentTitle(r.getString(R.string.screenshot_saving_title))
            .setContentText(r.getString(R.string.screenshot_saving_text))
            .setSmallIcon(R.drawable.stat_notify_image)
            .setWhen(System.currentTimeMillis());

        mNotificationStyle = new Notification.BigPictureStyle()
            .bigPicture(preview);
        mNotificationBuilder.setStyle(mNotificationStyle);
        BitmapDrawable bp = (BitmapDrawable)r.getDrawable(R.drawable.stat_notify_screenshot);
        if (bp != null){
        	mNotificationBuilder.setLargeIcon(bp.getBitmap());
        }
        Notification n = mNotificationBuilder.build();
        n.flags |= Notification.FLAG_NO_CLEAR;
        n.extras.putInt("title", R.string.screenshot_saving_title);
        n.extras.putInt("text", R.string.screenshot_saving_text);
        mNotificationManager.notify(nId, n);

        // On the tablet, the large icon makes the notification appear as if it is clickable (and
        // on small devices, the large icon is not shown) so defer showing the large icon until
        // we compose the final post-save notification below.
        //mNotificationBuilder.setLargeIcon(croppedIcon);
        // But we still don't set it for the expanded view, allowing the smallIcon to show here.
        //mNotificationStyle.bigLargeIcon(null);
        if (bp != null){
        	mNotificationStyle.bigLargeIcon(bp.getBitmap());
        }        
    }

    @Override
    protected SaveImageInBackgroundData doInBackground(SaveImageInBackgroundData... params) {
        if (params.length != 1) return null;
    	LogHelper.sv(TAG, "doInBackground start");

        // By default, AsyncTask sets the worker thread to have background thread priority, so bump
        // it back up so that we save a little quicker.
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

        Context context = params[0].context;
        Bitmap image = params[0].image;
        Resources r = context.getResources();
        OutputStream out = null;
        InputStream in = null;
        ScreenShotTask screenShotTask = TakeScreenshotService.getGlobalScreenshot().getScreenShotTask(params[0].taskId);
        params[0].result = 1;
       ContentResolver resolver = context.getContentResolver();
       Uri uri = null;
       for (int iPath = 0; iPath < mImageFilePaths.size(); iPath++){	
	        try {
	        	String mImageFilePath = mImageFilePaths.get(iPath) + "/" + mImageFileName;
	        	LogHelper.sv(TAG, "mImageFilePath = "+ mImageFilePath);
	            // Save the screenshot to the MediaStore
	            ContentValues values = new ContentValues();
	            String imageTitle=mImageFileName.substring(0,mImageFileName.lastIndexOf("."));
	            values.put(MediaStore.Images.ImageColumns.DATA, mImageFilePath);
	            // values.put(MediaStore.Images.ImageColumns.TITLE, mImageFileName);//edit by wz 
	            values.put(MediaStore.Images.ImageColumns.TITLE, imageTitle);//add by wz	 
	            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, mImageFileName);
	            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, mImageTime);
	            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, mImageTime/1000);
	            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, mImageTime/1000);
	            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
	            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	
	            LogHelper.sd(TAG, "SaveImageInBackgroundData uri = " + uri);

	            // zy end
	
	            out = resolver.openOutputStream(uri);
	            
	            if(!screenShotTask.bitmapIsEdit()) {
	            	LogHelper.sv(TAG, "bitmapIsNotEdit");
	            	
	            	synchronized (screenShotTask.mTempFile) {
	            		while(!screenShotTask.mTempFileWriteFinished) {
	            			screenShotTask.mTempFile.wait();
	            		}
	            	
	            		if(screenShotTask.mTempFile.exists()) {
	            			
	            			LogHelper.sv(TAG, "FileCopy start");
	            			byte[] buffer = new byte[1024];
	            			in = new FileInputStream(screenShotTask.mTempFile);
	                		while(true) {
	                			int readCount = in.read(buffer);
	                			if(readCount == -1) {
	                				break;
	                			}
	                			out.write(buffer,0,readCount);
	                		}
	                		LogHelper.sv(TAG, "FileCopy end");
	            		} else {
	            			LogHelper.sv(TAG, "image.compress(Bitmap.CompressFormat.PNG, 100, out) start");
	            			image.compress(Bitmap.CompressFormat.PNG, 100, out);
	            			LogHelper.sv(TAG, "image.compress(Bitmap.CompressFormat.PNG, 100, out) end");
	            		}
	            	}
	            } else {
	            	LogHelper.sv(TAG, "image.compress(Bitmap.CompressFormat.PNG, 100, out) start");
	                image.compress(Bitmap.CompressFormat.PNG, 100, out);
	                LogHelper.sv(TAG, "image.compress(Bitmap.CompressFormat.PNG, 100, out) end");
	            }
	            
	            out.flush();
	            out.close();
	
	            // update file size in the database
	            values.clear();
	            values.put(MediaStore.Images.ImageColumns.SIZE, new File(mImageFilePath).length());
	            resolver.update(uri, values, null, null);
	            
	            params[0].imageUri = uri;
	            params[0].result = 0;
	            
	            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
	            sharingIntent.setType("image/png");
	            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
	            
	            Intent chooserIntent = Intent.createChooser(sharingIntent, null);
	            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK 
	                    | Intent.FLAG_ACTIVITY_NEW_TASK);
	            mShareIntent = chooserIntent;
	


	            mNotificationBuilder.addAction(R.drawable.ic_menu_share,
	                     r.getString(R.string.status_bar_screenshot_share),
	                     PendingIntent.getActivity(context, 0, chooserIntent, 
	                             PendingIntent.FLAG_CANCEL_CURRENT));
	            
	            // zy add 
//	            Intent editIntent = new Intent(Intent.ACTION_EDIT);
              Intent editIntent = new Intent("android.yulong.intent.action.DOODLE");
	            Uri mShareUri = uri;//Uri.parse("file://" + mImageFilePath);
	            editIntent.setDataAndType(mShareUri, "image/*");
	            editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	            
	            Intent chooserEdit = Intent.createChooser(editIntent, null);
	            chooserEdit.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
	                    | Intent.FLAG_ACTIVITY_NEW_TASK);
	            mEditIntent = chooserEdit;
	            






	            mNotificationBuilder.addAction(R.drawable.ic_menu_edit,
	                    r.getString(R.string.status_bar_screenshot_edit), 
	                    PendingIntent.getActivity(context, 1, chooserEdit, 
	                            PendingIntent.FLAG_CANCEL_CURRENT));
	            
	            break;
	        } catch (Exception e) {
	            // IOException/UnsupportedOperationException may be thrown if external storage is not
	            // mounted
	        	if (uri != null){
	        		resolver.delete(uri, null, null);
	        	}
	        	LogHelper.se(TAG, e.toString());
	            params[0].result = 1;
	            
	            //jinxiaofeng 20140303 add begin
	            String mImageFilePath = mImageFilePaths.get(iPath) + "/" + mImageFileName;
	            File file = new File(mImageFilePath);
	            if(file.exists()){
	            	file.delete();
	            }
	            //jinxiaofeng 20140303 add end
	            
	        } finally {
	        	if(out != null) {
	        		try {
						out.close();
					} catch (IOException e) {
					}
	        	}
	        	if(in != null) {
	        		try {
	        			in.close();
					} catch (IOException e) {
					}
	        	}
	        }        	
        }

        TakeScreenshotService.getGlobalScreenshot().screenShotTaskFinished(params[0].taskId);
        LogHelper.sv(TAG, "doInBackground end");
        return params[0];
    }

    @Override
    protected void onPostExecute(SaveImageInBackgroundData params) {
        if (params.result > 0) {
            // Show a message that we've failed to save the image to disk
            GlobalScreenshot.notifyScreenshotError(params.context, mNotificationManager);
        } else {
            // Show the final notification to indicate screenshot saved
            Resources r = params.context.getResources();

            // Create the intent to show the screenshot in gallery
            Intent launchIntent = new Intent(Intent.ACTION_VIEW);
            launchIntent.setDataAndType(params.imageUri, "image/png");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mNotificationBuilder
                .setContentTitle(r.getString(R.string.screenshot_saved_title))
                .setContentText(r.getString(R.string.screenshot_saved_text))
                .setContentIntent(PendingIntent.getActivity(params.context, 0, launchIntent, 0))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
            BitmapDrawable bp = (BitmapDrawable)r.getDrawable(R.drawable.stat_notify_screenshot);
            if (bp != null){
            	mNotificationBuilder.setLargeIcon(bp.getBitmap());
            }
            Notification n = mNotificationBuilder.build();
            n.extras.putInt("title", R.string.screenshot_saved_title);
            n.extras.putInt("text", R.string.screenshot_saved_text);
            n.flags &= ~Notification.FLAG_NO_CLEAR;
            mNotificationManager.notify(mNotificationId, n);
            
            if (params.finishOption == GlobalScreenshot.OPTION_EDIT){
                startEditActivity();
            } else if (params.finishOption == GlobalScreenshot.OPTION_SHARE){
                startShareActivity();
            } 
            
            try {
            	LogHelper.sv(TAG, "ClipboardManager.setImage start");
                ClipboardManager cbm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                for (int iPath = 0; iPath < mImageFilePaths.size(); iPath++){
    	        	String mImageFilePath = mImageFilePaths.get(iPath) + "/" + mImageFileName;   	        	
                    //===modify by ty
    	        	//cbm.setImage(mImageFilePath);
        	        File file = new File(mImageFilePath);
        	        if (file.exists())
        	        	break;
                }
                Log.w("Build","ClipboardManager.setImage end Model:" + Build.MODEL + " PRODUCT:" + Build.PRODUCT +
                		" DEVICE:" + Build.DEVICE + " DISPLAY:" + Build.DISPLAY + " ID:" + Build.ID);
//                if(!Build.MODEL.contains("V1-C")){
//                    Toast.makeText(mContext, mContext.getText(R.string.status_bar_screenshot_copytoclipboard), Toast.LENGTH_SHORT).show();                	
//                }
            } catch(Exception e){
            	LogHelper.se(TAG, e.toString());
            } 
        }

        params.finisher.run();
    }
    
    void startShareActivity(){
        mContext.startActivity(mShareIntent);

    }
    
    void startEditActivity(){
        mContext.startActivity(mEditIntent);
    }
    
    private static boolean isExternalStoragePresent() {
        
        IBinder service = ServiceManager.getService("mount");
        IMountService mountService = IMountService.Stub.asInterface(service);
        String stats = "";
        try {
            stats = mountService.getVolumeState("/mnt/sdcard/external_sd");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Environment.MEDIA_MOUNTED.equals(stats);
        // 
        // return
        // Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
     }
}

/**
 * TODO:
 *   - Performance when over gl surfaces? Ie. Gallery
 *   - what do we say in the Toast? Which icon do we get if the user uses another
 *     type of gallery?
 */
public class GlobalScreenshot {
    private static final int SCREENSHOT_NOTIFICATION_ID = 789;
    private static final int SCREENSHOT_FLASH_TO_PEAK_DURATION = 130;
    private static final int SCREENSHOT_DROP_IN_DURATION = 430;
    private static final int SCREENSHOT_DROP_OUT_DELAY = 500;
    private static final int SCREENSHOT_DROP_OUT_DURATION = 430;
    private static final int SCREENSHOT_DROP_OUT_SCALE_DURATION = 370;
    private static final int SCREENSHOT_FAST_DROP_OUT_DURATION = 320;
    private static final float BACKGROUND_ALPHA = 0.5f;
    private static final float SCREENSHOT_SCALE = 1f;
    private static final float SCREENSHOT_DROP_IN_MIN_SCALE = SCREENSHOT_SCALE * 0.725f;
    private static final float SCREENSHOT_DROP_OUT_MIN_SCALE = SCREENSHOT_SCALE * 0.45f;
    private static final float SCREENSHOT_FAST_DROP_OUT_MIN_SCALE = SCREENSHOT_SCALE * 0.6f;
    private static final float SCREENSHOT_DROP_OUT_MIN_SCALE_OFFSET = 0f;

    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private NotificationManager mNotificationManager;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;

    private Bitmap mScreenBitmap;
    private View mScreenshotLayout;
    private ImageView mBackgroundView;
    private ImageView mScreenshotView;
    private ImageView mScreenshotFlash;

    private AnimatorSet mScreenshotAnimation;

    private int mNotificationIconSize;
    private float mBgPadding;
    private float mBgPaddingScale;
    private Runnable mFinisher;

    private MediaActionSound mCameraSound;
    public static final int OPTION_NONE  = 0;
    public static final int OPTION_SHARE = 1;
    public static final int OPTION_EDIT  = 2;
    public static final int OPTION_QUIT  = 3;
    
    
    /**
     * @param context everything needs a context :(
     */
    public GlobalScreenshot(Context context) {
        Resources r = context.getResources();
        mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate the screenshot layout
        mDisplayMatrix = new Matrix();
        mScreenshotLayout = layoutInflater.inflate(R.layout.global_screenshot, null);
        mBackgroundView = (ImageView) mScreenshotLayout.findViewById(R.id.global_screenshot_background);
        mScreenshotView = (ImageView) mScreenshotLayout.findViewById(R.id.global_screenshot);
        mScreenshotFlash = (ImageView) mScreenshotLayout.findViewById(R.id.global_screenshot_flash);
        mScreenshotLayout.setFocusable(true);
        mScreenshotLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Intercept and ignore all touch events
                return true;
            }
        });

        // Setup the window that we are going to use
        mWindowLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        mWindowLayoutParams.setTitle("ScreenshotAnimation");
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);

        // Get the various target sizes
        mNotificationIconSize =
            r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        // Scale has to account for both sides of the bg
        mBgPadding = (float) r.getDimensionPixelSize(R.dimen.global_screenshot_bg_padding);
        mBgPaddingScale = mBgPadding /  mDisplayMetrics.widthPixels;

        // Setup the Camera shutter sound
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
    }

    /**
     * Creates a new worker thread and saves the screenshot to the media store.
     */
    public void saveScreenshotInWorkerThread(int id,int finishOption){
    	ScreenShotTask screenShotTask = this.getScreenShotTask(id);
    	if(screenShotTask==null)
    	{
    		return;
    	}
        if (finishOption == OPTION_QUIT){
        	this.screenShotTaskFinished(id);
        	screenShotTask.mFinisher.run();
        } else {
            SaveImageInBackgroundData data = new SaveImageInBackgroundData();
            data.context = mContext;
            data.image = screenShotTask.getSaveBitmap();
            data.iconSize = mNotificationIconSize;
            data.finisher = screenShotTask.mFinisher;
            data.finishOption = finishOption;
            data.taskId = id;
//            playScreenShotSound();
            new SaveImageInBackgroundTask(mContext, data, mNotificationManager,
                    SCREENSHOT_NOTIFICATION_ID).execute(data);     	
        }
    }

    private void saveScreenshotInWorkerThread(Runnable finisher) {
        SaveImageInBackgroundData data = new SaveImageInBackgroundData();
        data.context = mContext;
        data.image = mScreenBitmap;
        data.iconSize = mNotificationIconSize;
        data.finisher = finisher;
//        playScreenShotSound();
        new SaveImageInBackgroundTask(mContext, data, mNotificationManager,
                SCREENSHOT_NOTIFICATION_ID).execute(data);
    }

    /**
     * @return the current display rotation in degrees
     */
    private float getDegreesForRotation(int value) {
        switch (value) {
        case Surface.ROTATION_90:
            return 360f - 90f;
        case Surface.ROTATION_180:
            return 360f - 180f;
        case Surface.ROTATION_270:
            return 360f - 270f;
        }
        return 0f;
    }

    /**
     * Takes a screenshot of the current display and shows an animation.
     */
    boolean takeScreenshot(Runnable finisher, boolean statusBarVisible, boolean navBarVisible) {
        // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
        // only in the natural orientation of the device :!)
        mDisplay.getRealMetrics(mDisplayMetrics);
        float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(mDisplay.getRotation());
        boolean requiresRotation = (degrees > 0);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        // Take the screenshot
        mScreenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);
        if (mScreenBitmap == null) {
            notifyScreenshotError(mContext, mNotificationManager);
            finisher.run();
            return false;
        }

        if (requiresRotation) {
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(mScreenBitmap, 0, 0, null);
            c.setBitmap(null);
            mScreenBitmap = ss;
        }

        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();

        mFinisher = finisher;
      /*mScreenshotLayout.post(new Runnable() {//getHandler().
            @Override
            public void run() {
                // Play the shutter sound to notify that we've taken a screenshot
            	playScreenShotSound();
            }
        });*/
      playScreenShotSound();
        
//        ActivityOptions opts = ActivityOptions.makeCustomAnimation(mContext,
//                R.anim.fade, ActivityOptions.ANIM_NONE);
//        Intent intent = new Intent();
//        intent.setClass(mContext, EditActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        mContext.startActivity(intent, opts.toBundle());
        
        
        
        // Start the post-screenshot animation
        //startAnimation(finisher, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels,
        //        statusBarVisible, navBarVisible);
        
        return true;
    }
    
    public void playScreenShotSound(){
    	AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    	if(mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT){
    		//LogHelper.sd("", "playScreenShotSound mode="+mAudioManager.getRingerMode()+" hwMute="+mHwMuteEnable);
    		mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
    	}
    }

    /**
     * Takes a screenshot of the current display and shows an animation.
     */
    public static float mRotationDegrees = 0;
    public Bitmap getScreenshot(boolean statusBarVisible, boolean navBarVisible) {
        // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
        // only in the natural orientation of the device :!)
        mDisplay.getRealMetrics(mDisplayMetrics);
        float[] dims = {mDisplayMetrics.widthPixels , mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(mDisplay.getRotation());
        boolean requiresRotation = (degrees > 0);
        GlobalScreenshot.setRoationDegrees(degrees);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        // Take the screenshot
        Bitmap mScreenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);
        if (mScreenBitmap == null) {
//        	if (Utils.isNeedLog) {
//        		Log.w("zxh "+TAG, "mScreenBitmap == null");
//			}
            return null;
        }
        /*if (requiresRotation) {
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(mScreenBitmap, 0, 0, null);
            c.setBitmap(null);
            // Recycle the previous bitmap
            mScreenBitmap.recycle();
            mScreenBitmap = ss;
        }*/
        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();

        return mScreenBitmap;
    }
    
    private static void setRoationDegrees(float degrees) {
    	mRotationDegrees = degrees;
	}

	/**
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	private Bitmap add2BitmapH(Bitmap first, Bitmap second) {
		int width = first.getWidth() + second.getWidth();
		int height = Math.max(first.getHeight(), second.getHeight());
		Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(first, 0, 0, null);
		canvas.drawBitmap(second, first.getWidth(), 0, null);
		return result;
	}
	
	 /**
		 * 
		 * @param first
		 * @param second
		 * @return
		 */
	private Bitmap add2BitmapV(Bitmap first, Bitmap second) {
		int width = Math.max(first.getWidth(), second.getWidth());
		int height = first.getHeight()+second.getHeight();
		Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(first, 0, 0, null);
		canvas.drawBitmap(second, 0, first.getHeight(), null);
		return result;
	}
	
    public Bitmap getStatusBarScreenshot() {
        // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
        // only in the natural orientation of the device :!)
        mDisplay.getRealMetrics(mDisplayMetrics);
        float[] dims = {mDisplayMetrics.widthPixels , mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(mDisplay.getRotation());
        boolean requiresRotation = (degrees > 0);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        // Take the screenshot
        Bitmap screenBitmap = SurfaceControl.screenshot((int)dims[0], (int)dims[1]);
        if (screenBitmap == null) {
            return null;
        }

        if (requiresRotation) {
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(screenBitmap, 0, 0, null);
            c.setBitmap(null);
            // Recycle the previous bitmap
            screenBitmap.recycle();
            screenBitmap = ss;
        }

        // Optimizations
        screenBitmap.setHasAlpha(false);
        screenBitmap.prepareToDraw();
        
        //int height = mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        final int dpHeight = 1;
        final int multiply = 28;
        int height = Utilities.dipToPixel(mContext, dpHeight);//*0.3
        Bitmap  cutBmpBitmap = Bitmap.createBitmap(screenBitmap, 0, (int)(height), (int)(dims[0]), (int)(height));
        
        Bitmap bmp = Bitmap.createBitmap((int)(dims[0]),dpHeight*multiply,Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        int curHeight = 0;
        if(cutBmpBitmap != null){
        	curHeight =cutBmpBitmap.getHeight();
        for(int i=0; i<multiply; i++){
        	 canvas.drawBitmap(cutBmpBitmap, 0, i*curHeight, null);
            }
        }
        
        if (screenBitmap != null) {
        	screenBitmap.recycle();
        	screenBitmap = null;
		}

        if (cutBmpBitmap != null) {
        	cutBmpBitmap.recycle();
        	cutBmpBitmap = null;
		}

        return bmp;
    }

    /**
     * Starts the animation after taking the screenshot
     */
    private void startAnimation(final Runnable finisher, int w, int h, boolean statusBarVisible,
            boolean navBarVisible) {
        // Add the view for the animation
        mScreenshotView.setImageBitmap(mScreenBitmap);
        mScreenshotLayout.requestFocus();

        // Setup the animation with the screenshot just taken
        if (mScreenshotAnimation != null) {
            mScreenshotAnimation.end();
        }

        mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
        ValueAnimator screenshotDropInAnim = createScreenshotDropInAnimation();
        ValueAnimator screenshotFadeOutAnim = createScreenshotDropOutAnimation(w, h,
                statusBarVisible, navBarVisible);
        mScreenshotAnimation = new AnimatorSet();
        mScreenshotAnimation.playSequentially(screenshotDropInAnim, screenshotFadeOutAnim);
        mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Save the screenshot once we have a bit of time now
                saveScreenshotInWorkerThread(finisher);
                mWindowManager.removeView(mScreenshotLayout);
            }
        });
        mScreenshotLayout.post(new Runnable() {
            @Override
            public void run() {
                // Play the shutter sound to notify that we've taken a screenshot
            	playScreenShotSound();

                mScreenshotView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mScreenshotView.buildLayer();
                mScreenshotAnimation.start();
            }
        });
    }
    private ValueAnimator createScreenshotDropInAnimation() {
        final float flashPeakDurationPct = ((float) (SCREENSHOT_FLASH_TO_PEAK_DURATION)
                / SCREENSHOT_DROP_IN_DURATION);
        final float flashDurationPct = 2f * flashPeakDurationPct;
        final Interpolator flashAlphaInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float x) {
                // Flash the flash view in and out quickly
                if (x <= flashDurationPct) {
                    return (float) Math.sin(Math.PI * (x / flashDurationPct));
                }
                return 0;
            }
        };
        final Interpolator scaleInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float x) {
                // We start scaling when the flash is at it's peak
                if (x < flashPeakDurationPct) {
                    return 0;
                }
                return (x - flashDurationPct) / (1f - flashDurationPct);
            }
        };
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(SCREENSHOT_DROP_IN_DURATION);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBackgroundView.setAlpha(0f);
                mBackgroundView.setVisibility(View.VISIBLE);
                mScreenshotView.setAlpha(0f);
                mScreenshotView.setTranslationX(0f);
                mScreenshotView.setTranslationY(0f);
                mScreenshotView.setScaleX(SCREENSHOT_SCALE + mBgPaddingScale);
                mScreenshotView.setScaleY(SCREENSHOT_SCALE + mBgPaddingScale);
                mScreenshotView.setVisibility(View.VISIBLE);
                mScreenshotFlash.setAlpha(0f);
                mScreenshotFlash.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                mScreenshotFlash.setVisibility(View.GONE);
            }
        });
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                float scaleT = (SCREENSHOT_SCALE + mBgPaddingScale)
                    - scaleInterpolator.getInterpolation(t)
                        * (SCREENSHOT_SCALE - SCREENSHOT_DROP_IN_MIN_SCALE);
                mBackgroundView.setAlpha(scaleInterpolator.getInterpolation(t) * BACKGROUND_ALPHA);
                mScreenshotView.setAlpha(t);
                mScreenshotView.setScaleX(scaleT);
                mScreenshotView.setScaleY(scaleT);
                mScreenshotFlash.setAlpha(flashAlphaInterpolator.getInterpolation(t));
            }
        });
        return anim;
    }
    private ValueAnimator createScreenshotDropOutAnimation(int w, int h, boolean statusBarVisible,
            boolean navBarVisible) {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setStartDelay(SCREENSHOT_DROP_OUT_DELAY);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBackgroundView.setVisibility(View.GONE);
                mScreenshotView.setVisibility(View.GONE);
                mScreenshotView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        if (!statusBarVisible || !navBarVisible) {
            // There is no status bar/nav bar, so just fade the screenshot away in place
            anim.setDuration(SCREENSHOT_FAST_DROP_OUT_DURATION);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = (Float) animation.getAnimatedValue();
                    float scaleT = (SCREENSHOT_DROP_IN_MIN_SCALE + mBgPaddingScale)
                            - t * (SCREENSHOT_DROP_IN_MIN_SCALE - SCREENSHOT_FAST_DROP_OUT_MIN_SCALE);
                    mBackgroundView.setAlpha((1f - t) * BACKGROUND_ALPHA);
                    mScreenshotView.setAlpha(1f - t);
                    mScreenshotView.setScaleX(scaleT);
                    mScreenshotView.setScaleY(scaleT);
                }
            });
        } else {
            // In the case where there is a status bar, animate to the origin of the bar (top-left)
            final float scaleDurationPct = (float) SCREENSHOT_DROP_OUT_SCALE_DURATION
                    / SCREENSHOT_DROP_OUT_DURATION;
            final Interpolator scaleInterpolator = new Interpolator() {
                @Override
                public float getInterpolation(float x) {
                    if (x < scaleDurationPct) {
                        // Decelerate, and scale the input accordingly
                        return (float) (1f - Math.pow(1f - (x / scaleDurationPct), 2f));
                    }
                    return 1f;
                }
            };

            // Determine the bounds of how to scale
            float halfScreenWidth = (w - 2f * mBgPadding) / 2f;
            float halfScreenHeight = (h - 2f * mBgPadding) / 2f;
            final float offsetPct = SCREENSHOT_DROP_OUT_MIN_SCALE_OFFSET;
            final PointF finalPos = new PointF(
                -halfScreenWidth + (SCREENSHOT_DROP_OUT_MIN_SCALE + offsetPct) * halfScreenWidth,
                -halfScreenHeight + (SCREENSHOT_DROP_OUT_MIN_SCALE + offsetPct) * halfScreenHeight);

            // Animate the screenshot to the status bar
            anim.setDuration(SCREENSHOT_DROP_OUT_DURATION);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = (Float) animation.getAnimatedValue();
                    float scaleT = (SCREENSHOT_DROP_IN_MIN_SCALE + mBgPaddingScale)
                        - scaleInterpolator.getInterpolation(t)
                            * (SCREENSHOT_DROP_IN_MIN_SCALE - SCREENSHOT_DROP_OUT_MIN_SCALE);
                    mBackgroundView.setAlpha((1f - t) * BACKGROUND_ALPHA);
                    mScreenshotView.setAlpha(1f - scaleInterpolator.getInterpolation(t));
                    mScreenshotView.setScaleX(scaleT);
                    mScreenshotView.setScaleY(scaleT);
                    mScreenshotView.setTranslationX(t * finalPos.x);
                    mScreenshotView.setTranslationY(t * finalPos.y);
                }
            });
        }
        return anim;
    }

    static void notifyScreenshotError(Context context, NotificationManager nManager) {
        Resources r = context.getResources();

        BitmapDrawable bp = (BitmapDrawable)r.getDrawable(R.drawable.stat_notify_screenshot);
        // Clear all existing notification, compose the new notification and show it
        Notification n = new Notification.Builder(context)
            .setTicker(r.getString(R.string.screenshot_failed_title))
            .setContentTitle(r.getString(R.string.screenshot_failed_title))
            .setContentText(r.getString(R.string.screenshot_failed_text))
            .setSmallIcon(R.drawable.stat_notify_image_error)
            .setLargeIcon(bp.getBitmap())
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .getNotification();
        n.extras.putInt("title", R.string.screenshot_failed_title);
        n.extras.putInt("text", R.string.screenshot_failed_text);
        nManager.notify(SCREENSHOT_NOTIFICATION_ID, n);
    }
    
    

    static class ScreenShotTask {
        public static int mMaxId = -1;
    	
    	int mId;
    	
    	Bitmap mBitmap;
    	
    	File mTempFile;
    	boolean mTempFileWriteFinished;
   
    	WeakReference<Activity> mActivity;
    	Bitmap mEditBitmap;
    	Runnable mFinisher;
    	public ScreenShotTask(Context context) {
    		mId = ++mMaxId;
    		mBitmap = null;
    		File cacheDir = context.getCacheDir();
    		String tempFilePath;
			if (cacheDir != null) {
				tempFilePath = cacheDir.getAbsolutePath() + "/screenshot/" + mId + ".png";
			} else {
				tempFilePath = "sdcard/screenshot/" + mId + ".png";
			}
    		mTempFile = new File(tempFilePath);
    		synchronized (mTempFile) {
        		mTempFileWriteFinished = false;
    		}
    		mActivity =null;
    		mFinisher = null;
    	}
    	public Bitmap getBitmap() {
    		return mBitmap;
    	}
    	public void setEditBitmap(Bitmap bitmap) {
    		mEditBitmap = bitmap;
    	}
    	public boolean bitmapIsEdit() {
    		if(this.mEditBitmap == null) {
    			return false;
    		}
    		return true;
    	}
    	public Bitmap getSaveBitmap() {
    		if(mEditBitmap != null) {
    			return mEditBitmap;
    		}
    		return mBitmap;
    	}
    }
    public HashMap<Integer,ScreenShotTask> mTaskMap = new HashMap<Integer,ScreenShotTask>();
    public ScreenShotTask getScreenShotTask(int id) {
    	return mTaskMap.get(id);
    }
    public void screenShotTaskFinished(int id) {
   
    	ScreenShotTask screenShotTask = getScreenShotTask(id);
    	if(screenShotTask.mTempFile.exists()) {
    		screenShotTask.mTempFile.delete();
    	}
    	this.mTaskMap.remove(id);
    }
    public ScreenShotTask registerScreenShotTaskActivity(int id,Activity activity) {
    	ScreenShotTask screenShotTask = this.getScreenShotTask(id);
    	if (screenShotTask != null && screenShotTask.mActivity == null){
    		screenShotTask.mActivity = new WeakReference<Activity>(activity);
        	return screenShotTask;
    	}
    	return null;
    }
    public void startScreenShotTask(Runnable finisher, boolean statusBarVisible, boolean navBarVisible) {
    	
    	LogHelper.sv("", "startScreenShotTask statusBarVisible " + statusBarVisible + " navBarVisible " + navBarVisible);
    	ScreenShotTask screenShotTask = new ScreenShotTask(mContext);
    	screenShotTask.mFinisher = finisher;
    
    	boolean result = takeScreenshot(finisher,statusBarVisible,navBarVisible);
    	if(result == false) {
    		return;
    	}
    	
    	mTaskMap.put(screenShotTask.mId, screenShotTask);
    	screenShotTask.mBitmap = this.mScreenBitmap;
    	
    	new SaveTempFileThread(mContext,screenShotTask).start();
    	
    	ActivityOptions opts = ActivityOptions.makeCustomAnimation(mContext,R.anim.fade, ActivityOptions.ANIM_NONE);
    	Intent intent = new Intent();
    	intent.setClass(mContext, EditActivity.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    	intent.putExtra("taskId", screenShotTask.mId);
    	mContext.startActivity(intent, opts.toBundle());
    }
    static class SaveTempFileThread extends Thread {
    	ScreenShotTask mScreenShotTask;
    	Bitmap mBitmap;
    	File mTempFile;
    	Context mContext;
    	SaveTempFileThread(Context context,ScreenShotTask screenShotTask) {
    		mContext = context;
    		mScreenShotTask = screenShotTask;
    		mBitmap = mScreenShotTask.getBitmap();
    		mTempFile = mScreenShotTask.mTempFile;
    	}
		@Override
		public void run() {
			LogHelper.sv(SaveImageInBackgroundTask.TAG,"SaveTempFileThread start");
			super.run();
			synchronized (mTempFile) {
				OutputStream out = null;
				try {
					if(mTempFile.exists()) {
						mTempFile.delete();
					}
					mTempFile.getParentFile().mkdirs();
					mTempFile.createNewFile();
		            out = new FileOutputStream(mTempFile);

		            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

		            out.flush();
				} catch (Exception e) {
					LogHelper.se(SaveImageInBackgroundTask.TAG,"e == " + e);
					if(mTempFile.exists()) {
						mTempFile.delete();
					}
				}finally {
					if(out != null) {
			            try {
							out.close();
						} catch (IOException e) {
						}
					}
					mScreenShotTask.mTempFileWriteFinished = true;
					mTempFile.notify();
				}
			}
			LogHelper.sv(SaveImageInBackgroundTask.TAG,"SaveTempFileThread end");
		}
    }
   
    //</zcg add>//
    
}
