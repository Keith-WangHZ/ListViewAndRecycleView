
package com.android.systemui.screenshot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.SystemProperties;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.screenshot.GlobalScreenshot.ScreenShotTask;
import com.android.systemui.screenshot.PathImageView.OnModeChangeListener;
import com.yulong.android.feature.FeatureConfig;
import com.yulong.android.feature.FeatureString;
import com.android.systemui.statusbar.phone.QuickSettingsModel;

public class EditActivity extends Activity {
	private static final String TAG = "SaveImageInBackgroundTaskEditActivity";
    private static final int NO_OPTION_TIME_OUT = 2 * 1000;
    private ImageView mScreenView;
    private PathImageView mPathView;
    private ViewGroup mButtonView;
    private View mButtonQuit;
    private Bitmap mScreenImage;
    private int mControlBtnHeight;
    private int mQuitBtnSize;
    private static long mLastOptionTime = 0;
    private Handler mHandler = new Handler();

    private DisplayMetrics mDisplayMetrics;
    private Display mDisplay;
    private WindowManager mWindowManager;
    private int mFinishOption = GlobalScreenshot.OPTION_NONE;
    private int mId;
    boolean isShowLongScreenShot = false;
    boolean isShowInterestingScreenShot = false;
    ScreenShotTask mScreenShotTask;
    private static final String LONG_SCREENSHOT_ACTION = "com.yulong.intent.longscreenshot";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	LogHelper.sv(TAG, "onCreate start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_activity);
    	IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(LONG_SCREENSHOT_ACTION);
		registerReceiver(mLongScreenShotReceiver, intentFilter);
        Intent intent = this.getIntent();
        mId = (Integer)intent.getIntExtra("taskId", -1);
        if(mId == -1) {
        	
        	LogHelper.se(TAG, "onCreate mId == " + mId);
        	
        	this.finish();
        }
        if(TakeScreenshotService.getGlobalScreenshot()==null){
        	
        	LogHelper.se(TAG, "TakeScreenshotService.getGlobalScreenshot() == null");
        	
        	this.finish();  
        	return;
        }
        mScreenShotTask = TakeScreenshotService.getGlobalScreenshot().registerScreenShotTaskActivity(mId, this);
        if (mScreenShotTask == null){
        
        	LogHelper.se(TAG, "onCreate mScreenShotTask == null");
        	
        	this.finish();  
        	return;
        }
        
        mLastOptionTime = System.currentTimeMillis();

        mScreenView = (ImageView) findViewById(R.id.image_view);
        mPathView = (PathImageView) findViewById(R.id.path_view);
        mButtonView = (ViewGroup) findViewById(R.id.control_view);
        mButtonQuit = findViewById(R.id.btn_quit);

        mWindowManager = getWindowManager();
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getMetrics(mDisplayMetrics);

        mScreenImage = mScreenShotTask.getBitmap();
        mPathView.setImageBitmap(mScreenImage);
        isShowLongScreenShot=QuickSettingsModel.getMiscInterfaceResult("is_support_long_screenshot");
        isShowInterestingScreenShot= QuickSettingsModel.getMiscInterfaceResult("is_support_interest_screenshot");

        View v = findViewById(R.id.btn_share);
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mFinishOption = GlobalScreenshot.OPTION_SHARE;
                saveAndFinish(true);
            }
        });

        v = findViewById(R.id.btn_edit);
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mFinishOption = GlobalScreenshot.OPTION_EDIT;
                saveAndFinish(true);
            }
        });
        if(isShowLongScreenShot){
        	v.setVisibility(View.GONE);
        }else{
        	v.setVisibility(View.VISIBLE);
        }
       
//	    String flag = SystemProperties.get("ro.secure.system", "false");
//		LogHelper.se(TAG, "SystemProperties.get(ro.secure.system)="
//				+ flag);
//		if (flag.equals("true")) {
//			v.setVisibility(View.INVISIBLE);
//			LogHelper.se(TAG, "Display status_bar_screenshot_edit no");
//		}else{
//			v.setVisibility(View.VISIBLE);
//			LogHelper.se(TAG, "Display status_bar_screenshot_edit yes" );
//		}
		
        v = findViewById(R.id.btn_save);
        v.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mFinishOption = GlobalScreenshot.OPTION_NONE;
				saveAndFinish(true);
			}
		});
        if(isShowInterestingScreenShot){
        	v.setVisibility(View.GONE);
        }else{
        	v.setVisibility(View.VISIBLE);
        }
        
        
        v = findViewById(R.id.btn_interesting);
        v.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mFinishOption = GlobalScreenshot.OPTION_QUIT;
				saveAndFinish(false);
				Intent intent = new Intent("com.yulong.android.coolfunscreenshot.startactivity");
				sendBroadcastAsUser(intent, UserHandle.CURRENT);
			}
		});
        if(isShowInterestingScreenShot){
        	v.setVisibility(View.VISIBLE);
        }else{
        	v.setVisibility(View.GONE);
        }
        
        
        v = findViewById(R.id.btn_long_screenshot);
        v.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				mFinishOption = GlobalScreenshot.OPTION_QUIT;
				saveAndFinish(false);
				Intent intent = new Intent("com.yulong.android.startactivity.longscreenshot");
				sendBroadcastAsUser(intent, UserHandle.CURRENT);

			}
		});
        if(isShowLongScreenShot){
        	v.setVisibility(View.VISIBLE);
        }else{
        	v.setVisibility(View.GONE);
        }
        
     v = findViewById(R.id.btn_exit);
        v.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mFinishOption = GlobalScreenshot.OPTION_QUIT;
				saveAndFinish(false);
			}
		});

        
        mButtonQuit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mFinishOption = GlobalScreenshot.OPTION_QUIT;
                saveAndFinish(false);
            }
        });

//        mButtonView.measure(ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
        LogHelper.sv(TAG, "onCreate end");
    }

    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Intent intent = new Intent("com.yulong.intent.start.switch.screenshot");
		sendBroadcast(intent);
		super.onResume();
	}

    boolean mHasPostSaveIamge = false;
    private void postSaveImage() {
    	LogHelper.sv(TAG, "postSaveImage start");
    	LogHelper.sv(TAG, "mHasPostSaveIamge == " + mHasPostSaveIamge);
    	if (mScreenShotTask == null){
    		return;    		
    	}
    	if(mHasPostSaveIamge) {
    		return;
    	}
    	mHasPostSaveIamge = true;
    	
    	if(mPathView.hasEdit()) {
    	
    		this.mScreenShotTask.setEditBitmap(mPathView.getClipImage());
    	}
    	TakeScreenshotService.getGlobalScreenshot().saveScreenshotInWorkerThread(mId,mFinishOption);
        // Toast.makeText(getBaseContext(), getText(R.string.screenshot_saving_ticker), Toast.LENGTH_SHORT).show();
    	LogHelper.sv(TAG, "postSaveImage end");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	LogHelper.sv(TAG, "onPostCreate start");
        Handler h = new Handler();
        h.postDelayed(new Runnable() {

            @Override
            public void run() {
//                mButtonView.measure(ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mButtonQuit.measure(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mControlBtnHeight = mButtonView.getHeight();
                mQuitBtnSize = mButtonQuit.getWidth();
                mButtonView.setTranslationY(mControlBtnHeight);
                mButtonQuit.setTranslationX(mQuitBtnSize);
                mButtonQuit.setTranslationY(-mQuitBtnSize);

                mPathView.SetOnModeChangeListener(mModeChangeListener);

                noOptionTimeOut(true);
            }
        }, 200);
        super.onPostCreate(savedInstanceState);
        LogHelper.sv(TAG, "onPostCreate end");
    }

    @Override
    protected void onStop() {
    	LogHelper.sv(TAG, "onStop start");
        super.onStop();
        LogHelper.sv(TAG, "onStop end");
    }
    
    @Override
    protected void onDestroy() {
    	LogHelper.sv(TAG, "onDestroy start");
    	unregisterReceiver(mLongScreenShotReceiver);
    	super.onDestroy();
    	if (mScreenShotTask == null){
    		return;    		
    	}    	
    	noOptionTimeOut(false);
    	if(!mHasPostSaveIamge) {
    		TakeScreenshotService.getGlobalScreenshot().saveScreenshotInWorkerThread(mId,GlobalScreenshot.OPTION_QUIT);
    	}
    	LogHelper.sv(TAG, "onDestroy end");
    }
    
    public void saveAndFinish(boolean saveImage) {
    	LogHelper.sv(TAG, "saveAndFinish start");
    	if(saveImage) {
        	postSaveImage();
    	} 
    	this.finish();
    	LogHelper.sv(TAG, "saveAndFinish end");
    }
    @Override
    public void finish() {
    	LogHelper.sv(TAG, "finish start");
    	if(this.isFinishing()) {
    		return;
    	}
    	noOptionTimeOut(false);
    	super.finish();
    	LogHelper.sv(TAG, "finish end");
    }
    
    

    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
    	
    	int action = event.getAction();
    	if(action == KeyEvent.ACTION_DOWN) {
    		noOptionTimeOut(false);
    	} else if(action == KeyEvent.ACTION_UP) {
    		//edit by wz
    		//noOptionTimeOut(true);
    		 if(event.getKeyCode() == KeyEvent.KEYCODE_BACK)
    		     saveAndFinish(true);
    		 else noOptionTimeOut(true);
    		 //end wz
    	}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		if(action == MotionEvent.ACTION_DOWN) {
			this.noOptionTimeOut(false);
		} else if(action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			this.noOptionTimeOut(true);
		}
		return super.dispatchTouchEvent(ev);
	}
	

	public void noOptionTimeOut(boolean start) {
		mHandler.removeCallbacks(mNoOptionTimeOutRunnable); 
		if(start) {
			mHandler.postDelayed(mNoOptionTimeOutRunnable, NO_OPTION_TIME_OUT);
		}
	}

	private OnModeChangeListener mModeChangeListener = new OnModeChangeListener() {

        @Override
        public void onChange(int mode) {
           
            boolean hide = mode != PathImageView.MODE_DRAGING;
            mButtonView.animate()
                    .translationY(hide ? 0 : mControlBtnHeight)
                    .setDuration(200);

            mButtonQuit.animate()
                    .translationX(hide ? 0 : mQuitBtnSize)
                    .translationY(hide ? 0 : -mQuitBtnSize)
                    .setDuration(200);
        }
    };

    public static void updateLastOptionTime() {
        mLastOptionTime = System.currentTimeMillis();
    }

    private Runnable mNoOptionTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
        	saveAndFinish(true);
        }
    };
    
	private BroadcastReceiver mLongScreenShotReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (LONG_SCREENSHOT_ACTION.equals(intent.getAction())) {
				Log.e(TAG,".........LONG_SCREENSHOT_ACTION  is receiver");
				mFinishOption = GlobalScreenshot.OPTION_QUIT;
				saveAndFinish(false);
				
			}
		}
	};

}
