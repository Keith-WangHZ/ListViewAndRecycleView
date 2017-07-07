package com.android.systemui.quicksettings.bottom;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.IvviGaussBlurViewFeature;
import com.android.systemui.R;
import com.android.systemui.quicksettings.bottom.BottomIconEditActivity.CheckablePackageInfo;
import com.android.systemui.quicksettings.bottom.QSBottomPanel.OnQuickSettingsPannelViewListener;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.DataControler.DatabaseRecord;
import com.android.systemui.statusbar.phone.KeyguardAffordanceHelper;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

@SuppressLint({ "ClickableViewAccessibility", "HandlerLeak" })
public class QuickSettingLauncher {
	private static String TAG = QuickSettingLauncher.class.getSimpleName();
	private static final String START_QUICK_SETTING = "com.yulong.android.systemui.start";
	private static final String START_QS_GUIDE = "com.yulong.action.QsBottomPanelGuide";
	private static final String START_QS_GUIDE_ACTION = "android.intent.action.QS_GUIDE_START";
//	private static final String FACTORYPARRERN_PULLINTERFACE_FORBID = "yulong.intent.action.PULLINTERFACE_FORBID";
	private static boolean DEBUG = true, DEBUG2 = false;//===modify by ty
    private File debugFlag= new File("/data/systemui_debug.txt");
	private static QuickSettingLauncher mInstance;
	private QSBottomPanel mQSBottomPanel;
	private Context mContext;
	private WindowManager mWm;
	private LayoutParams mWmParams = new WindowManager.LayoutParams();
	private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
	private Resources mResources;
	private Integer mScreenHeight, mScreenWidth, mStatusBarHeight, mMainHeight;
	private int mQSRootHeight, mTopBlankHeight, mNaviBarHeight, mGetBgLmt, mTopLimit, mBottomLimit, mHintViewLimit,
			mKeyguardBlankHeight;
	private FrameLayout mBgFrame;
	private View mBackKeyView, mDwArraw;
	public static boolean isPopuped;
	private int mOrientation = Configuration.ORIENTATION_PORTRAIT;
	private KeyguardManager mKeyguardManager;

	private PhoneStatusBar mStatusBar;
	private StatusBarManager statusBM;

    private FrameLayout.LayoutParams mHvParams = new FrameLayout.LayoutParams(-2, -2);

	private ImageView mTailImageView;
	private int hvWidth = 0, hvHeight = 0;

	public static boolean isQSEvent = false;

	private NotificationPanelView mNotificationPanelView;
	private Integer mEdgeTapAreaWidth;
	private boolean showNav;
	private ActivityManager mActivityManager;
//	private boolean mShouldForbid;

	public synchronized static QuickSettingLauncher getInstance(Context context) {
		if (Utilities.showDragUpQuickSettings() && mInstance == null) {
			mInstance = new QuickSettingLauncher(context);
		}
		return mInstance;
	}

	private QuickSettingLauncher(Context context) {
		try {
			showNav = Utilities.needFakeNavigationBarView() || WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
		} catch (RemoteException e) {

		}
		initQuickSettingLauncher(context);
	}

	private void initQuickSettingLauncher(Context context) {
		mContext = context;
		mActivityManager = (ActivityManager) context .getSystemService(Activity.ACTIVITY_SERVICE);
		mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		statusBM = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
		mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mEdgeTapAreaWidth = mContext.getResources().getDimensionPixelSize(R.dimen.edge_tap_area_width);

		IntentFilter filter = new IntentFilter();
		filter.addAction(START_QUICK_SETTING);
		filter.addAction(START_QS_GUIDE);
//		filter.addAction(FACTORYPARRERN_PULLINTERFACE_FORBID);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mContext.registerReceiver(mBroadcast, filter);

        IntentFilter filter4CTS = new IntentFilter();
        filter4CTS.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter4CTS.addDataScheme("package");
        filter4CTS.addAction(Intent.ACTION_PACKAGE_REMOVED);
        mContext.registerReceiver(mReceiver4CTS, filter4CTS);
		KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUpdateCallback);
	}

	private void updateStates() {
		updateWindowManager();
		updateHintViewParams();
		updateViews();
	}

	private void updateViews() {
	    if (null == mQSBottomPanel) {
	        mQSBottomPanel = new QSBottomPanel(mContext, null, mOrientation, mStatusBar);
	    }
		mQSBottomPanel.setOnQuickSettingsPannelViewListener(mOqspv);
		mQSBottomPanel.setOnTouchListener(mVotl);
		mDwArraw = mQSBottomPanel.findViewById(R.id.image_icon);
		mDwArraw.setOnTouchListener(mVotl);

		if (Configuration.ORIENTATION_LANDSCAPE == mOrientation) {
			mTailImageView = (ImageView) LayoutInflater.from(mContext).inflate(R.layout.bo_view, null);
			mTailImageView.setOnTouchListener(mHintl);
		}

		if (null == mBgFrame) {
	        mBgFrame = new FrameLayout(mContext) {
	            protected void onMeasure(int width, int height) {
	                super.onMeasure(width, height);
	                int innerWidth = mScreenWidth;
	                int innerHeight = mScreenHeight;
	                if (DEBUG) {
	                    Log.d(TAG, "innerWidth:" + innerWidth + "; innerHeight:" + innerHeight);
	                }
	                setMeasuredDimension(innerWidth, innerHeight);
	            }
	        };
		}
		mBgFrame.setOnTouchListener(mVotl);
		if(!Utilities.showFullGaussBlurForDDQS()){
			mBgFrame.setBackgroundColor(Color.argb(0x80, 0, 0, 0));
		}
		mBgFrame.setAlpha(0f);
        if (DEBUG2) {
            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
            mBgFrame.setAlpha(1f);
        }
        mBgFrame.removeAllViews();
		mBgFrame.addView(mQSBottomPanel);
	}

	private OnQuickSettingsPannelViewListener mOqspv = new OnQuickSettingsPannelViewListener() {
		public void setVisibility(boolean isShow) {
			if (isShow) {
				// mScreenHandler.sendEmptyMessage(UP);
				showBottomView(0f);
			} else {
				// mScreenHandler.sendEmptyMessage(DW);
				hideBottomView();
			}
		}
	};

	private void start() {
		start(mStatusBar);
	}

	public void start(PhoneStatusBar phoneStatusBar) {
		mStatusBar = phoneStatusBar;
		updateStates();
		mQSBottomPanel.setY(mBottomLimit);
		if(!isScreenChange()){
			mQSBottomPanel.getQSPannelView().setY(Utilities.dipToPixel(mContext, 8));
			if(mQSBottomPanel.getDetail() != null){
				mQSBottomPanel.getDetail().setY(Utilities.dipToPixel(mContext, 8));
			}
			if(mQSBottomPanel.getMobilePanelView() != null){
				mQSBottomPanel.getMobilePanelView().setY(Utilities.dipToPixel(mContext, 8));
			}
		}
		if (!mBgFrame.isAttachedToWindow()) {
			mWm.addView(mBgFrame, mWmParams);

			if (Configuration.ORIENTATION_LANDSCAPE == mOrientation) {
				mBgFrame.addView(mTailImageView, mHvParams);
			}
		}
		if (mStatusBar != null) {
			mStatusBar.onBottomQSUpdates();
		}
		updateStatusBar();
	}
	

	private boolean isScreenChange() {
		Configuration mConfiguration = mContext.getResources().getConfiguration();
		int ori = mConfiguration.orientation;
		if(Configuration.ORIENTATION_LANDSCAPE == ori){
			return true;
		}else if(Configuration.ORIENTATION_PORTRAIT == ori){
			return false;
		}
		return false;
	}

	private void updateWindowManager() {
		mResources = mContext.getResources();
		if (Configuration.ORIENTATION_PORTRAIT == mOrientation) {
			mQSRootHeight = (int) mResources.getDimension(R.dimen.new_systemui_height);
		} else {
			mQSRootHeight = (int) mResources.getDimension(R.dimen.new_systemui_height_new);
		}
		if (showNav) {
			if ((mStatusBar != null && mStatusBar.getBarState() != StatusBarState.SHADE)
					|| Configuration.ORIENTATION_LANDSCAPE == mOrientation) {
				mTopBlankHeight = (int) mResources.getDimension(R.dimen.quick_setting_top_pan_height_landscape);
				mTopBlankHeight = SystemProperties.getInt("ro.yulong.qspanel.bottom.margin", mTopBlankHeight);// YLH20160803
			} else {
				mTopBlankHeight = 0;
			}
		} else {
			mTopBlankHeight = (int) mResources.getDimension(R.dimen.quick_setting_top_pan_height);
		}
		
		// mTopBlankHeight = (int)
		// mResources.getDimension(R.dimen.quick_setting_top_pan_height);
		mNaviBarHeight = (int) mResources.getDimension(R.dimen.navigation_bar_size);
		mKeyguardBlankHeight = (int) mResources.getDimension(R.dimen.quick_setting_Keyguard_height);
		// mMoveDownLimit = (int)
		// mResources.getDimension(R.dimen.move_down_height);

		mWm.getDefaultDisplay().getRealMetrics(mDisplayMetrics);
		mScreenWidth = mDisplayMetrics.widthPixels;
		mScreenHeight = mDisplayMetrics.heightPixels;
		int tempHeight = 0;
		if ((Configuration.ORIENTATION_PORTRAIT == mOrientation && mScreenWidth > mScreenHeight)
				|| (Configuration.ORIENTATION_LANDSCAPE == mOrientation && mScreenWidth < mScreenHeight)) {
			tempHeight = mScreenHeight;
			mScreenHeight = mScreenWidth;
			mScreenWidth = tempHeight;
		}
		//mWmParams.type = LayoutParams.TYPE_KEYGUARD_DIALOG;
		mWmParams.type = LayoutParams.TYPE_MAGNIFICATION_OVERLAY;
		//mWmParams.type = LayoutParams.TYPE_STATUS_BAR_PANEL;
		//LayoutParams.TYPE_SYSTEM_ALERT;
		//mWmParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
		//mWmParams.type = LayoutParams.TYPE_DOCK_DIVIDER;
		//mWmParams.type = LayoutParams.TYPE_INPUT_METHOD;

		mWmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_NO_LIMITS
				| LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		mWmParams.format = PixelFormat.RGBA_8888;
		mWmParams.gravity = Gravity.TOP | Gravity.START;
        mWmParams.width = mScreenWidth;
        mWmParams.height = mScreenHeight;
		mStatusBarHeight = getStatusBarHeight();
		mMainHeight = mScreenHeight;
		mGetBgLmt = mMainHeight - mNaviBarHeight / 2;

		//Log.d("","500005643 desY="+mQSBottomPanel.getY());
		if(isScreenChange()){
			mTopLimit = mMainHeight - mQSRootHeight;
		}else{
			mTopLimit = mMainHeight - mQSRootHeight/* + Utilities.dipToPixel(mContext, -8)*/;
		}
        mBottomLimit = mMainHeight - mTopBlankHeight;

		mWmParams.y = mBottomLimit;
		if (DEBUG) {
			Log.d(TAG, "mScreenHeight:" + mScreenHeight + "; mStatusBarHeight:" + mStatusBarHeight + "; mNaviBarHeight:"
					+ mNaviBarHeight + "; QSRootHeight:" + mQSRootHeight + "; topBlankHeight:" + mTopBlankHeight + "; mTopLimit:"
					+ mTopLimit + "; mBottomLimit:" + mBottomLimit + "; mWmParams.y:" + mWmParams.y);
		}
	}

	private void updateHintViewParams() {
		mHvParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		hvWidth = (int) mResources.getDimension(R.dimen.hint_view_width);
		hvHeight = (int) mResources.getDimension(R.dimen.hint_view_height);
		mHvParams.width = hvWidth;
		mHvParams.height = hvHeight;

		mHintViewLimit = mMainHeight - hvHeight;
		// mBgFrame.setLayoutParams(mHvParams);
	}

	public void dispatchTouchListener(MotionEvent event) {
		mBgFrame.dispatchTouchEvent(event);
	}

	private View.OnTouchListener mHintl = new View.OnTouchListener() {
		float oldRawY, deltaY, oldY;

		public boolean onTouch(View v, MotionEvent e) {
			float newRawY = e.getRawY();
			float newY = mQSBottomPanel.getY();
			float newPos = newRawY;
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				oldRawY = newRawY;
				oldY = newY;
				deltaY = 0;
				mTailImageView.setVisibility(View.GONE);
				mQSBottomPanel.setVisibility(View.VISIBLE);
				mQSBottomPanel.bringToFront();
				setBgColorAlpha(0f);

				break;
			case MotionEvent.ACTION_MOVE:
				deltaY = newRawY - oldRawY;
				if (!isPopuped) {
					if (newPos > mTopLimit) {
						float alpha;
						
						alpha = calcBgColorAlpha(mQSRootHeight - (mMainHeight - mQSBottomPanel.getY()), true);
						if(Utilities.showFullGaussBlurForDDQS()){
							mBgFrame.setAlpha(0.8f);
						}else{
							mBgFrame.setBackgroundColor(Color.argb((int) (255 * (0.5f - alpha)), 0, 0, 0));
						}
					
				        if (DEBUG2) {
				            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
				            mBgFrame.setAlpha(1f);
				        }
						mQSBottomPanel.setY(oldY + deltaY);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mTailImageView.setVisibility(View.GONE);
				isHintShow = false;
				if (deltaY < 0) {
					// mScreenHandler.sendEmptyMessage(UP);
					showBottomView(0f);
				} else {
					// mScreenHandler.sendEmptyMessage(DW);
					hideBottomView();
				}
				break;

			}
			return true;
		}
	};
	String mCurrentActivityName;

	private String getCurrentActivityName(Context context) {
		if(mActivityManager != null){
			List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
			if(taskInfo != null && taskInfo.get(0)!=null && taskInfo.get(0).topActivity!=null &&
					taskInfo.get(0).topActivity.getClassName()!=null){
				return taskInfo.get(0).topActivity.getClassName();
			}
		}
		return "";
	}
	private boolean isWizardUI = false;
	private boolean isIconlocation = false;
	private boolean isSatusbarShow = false;
	private boolean isFullScreen = false;
	private boolean isHintShow = false;
	private boolean isDown = false;
	private boolean isForbidden = false;

	private View.OnTouchListener mVotl = new View.OnTouchListener() {
		float oldRawY, downRawY, deltaY;
		boolean canShotscreen;
		long start;

		public boolean onTouch(View v, MotionEvent e) {
			float newRawY = e.getRawY();
			if (DEBUG) {
				Log.d(TAG, "getRawY:" + newRawY + "; view.getY:" + v.getY() + "; mLayoutParams.y:" + mWmParams.y + "; action:"
						+ e.getAction() + "; View:" + v);
			}
			
			/*if (mStatusBar.isSuperSavePowerMode() || isForbidden || !mStatusBar.panelsEnabled()) {
				return false;
			}*/

			if (isIconlocation) {
				mNotificationPanelView.mAfforanceHelper.onTouchEvent(e);
			}
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
                if (DEBUG2) {
                    Log.w(TAG, "ACTION_DOWN:newRawY=" + newRawY + "; mWmParams.y:" + mWmParams.y + "; mBottomLimit=" + mBottomLimit);
                }
				oldRawY = newRawY;
				downRawY = newRawY;
				deltaY = 0;
				isSatusbarShow = false;
				isDown = false;

				isQSEvent = true;

				setBgColorAlpha(0f);
                mCurrentActivityName = getCurrentActivityName(mContext);

				if (isHintShow) {
					mScreenHandler.removeMessages(HINTDW);
					mScreenHandler.sendEmptyMessage(HINTDW);
					isDown = true;
					return false;
				}

				mQSBottomPanel.setVisibility(View.VISIBLE);
				if (!isPopuped) {
					boolean isNavBar = Utilities.needFakeNavigationBarView() || mContext.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
					// when window is keyguard,cant't popup sytemui where is
					// cameraicon location
					if (mKeyguardManager.isKeyguardLocked() && !isNavBar) {
						if (e.getX() > mScreenWidth - mEdgeTapAreaWidth) {
							isIconlocation = true;
							//mNotificationPanelView.onEdgeClicked(true);//===modify by ty
							mNotificationPanelView.mAfforanceHelper.onTouchEvent(e);
						}
					}
					Log.d(TAG, "NO1 = " + mStatusBar.mExpandedVisible + ",NO2=" + !mKeyguardManager.isKeyguardLocked()
							+ ",NO3=" + mStatusBar.isQsExpanded() + ",NO4=" + mStatusBar.isFullyExpanded());
					if ((mStatusBar.mExpandedVisible && !mKeyguardManager.isKeyguardLocked())
							|| mStatusBar.isQsExpanded()
							|| (mStatusBar.getBarState() != StatusBarState.KEYGUARD && mStatusBar.isFullyExpanded())) {
						isSatusbarShow = true;
					}
					canShotscreen = true;
					isWizardUI = !KeyguardUpdateMonitor.getInstance(mContext).isDeviceProvisioned();

					avoidDimScreen();

					isFullScreen = false;
					if ((Configuration.ORIENTATION_LANDSCAPE == mOrientation)) {
						if (invokeIsFullScreen()/* mWm.isTopWindowFullScreen() */) {
							isFullScreen = true;
							mTailImageView.setVisibility(View.VISIBLE);
						} else {
							mTailImageView.setVisibility(View.GONE);
						}

					}
				}
                mQSBottomPanel.mQSRoot.updateQuickPayState();
				if(/*isWizardUI || */( (mCurrentActivityName!=null) &&//"com.android.cts.ui.ScrollingActivity"
						mCurrentActivityName.equalsIgnoreCase("com.yulong.android.factorypattern.touchcalibrate.PointerLocation"))){
//					Log.d(TAG, "isWizardUI = " + isWizardUI+" mCurrentActivityName="+mCurrentActivityName);
					return false;
				}

				if (DEBUG) {
					Log.d(TAG, "isPopuped = " + isPopuped);
					Log.d(TAG, "isSatusbarShow = " + isSatusbarShow);
					Log.d(TAG, "isWizardUI = " + isWizardUI);
					Log.d(TAG, "isFullScreen = " + isFullScreen);
					Log.d(TAG, "isIconlocation = " + isIconlocation);
					Log.d(TAG, "isHintShow = " + isHintShow);
					Log.d(TAG, "isDown = " + isDown);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				start = System.currentTimeMillis();
				if(Utilities.showFullGaussBlurForDDQS()){
					mBgFrame.setAlpha(0.8f);
				}
				
				if ((isFullScreen && !isPopuped && !isHintShow) || isSatusbarShow) {
					deltaY = newRawY - oldRawY;
				} else {
//					Boolean bDisableNavForCts = (mCurrentActivityName!=null) &&
//							mCurrentActivityName.equalsIgnoreCase("com.android.cts.ui.ScrollingActivity");
					if (isWizardUI || isIconlocation || isHintShow /*|| bDisableNavForCts*/) {
						return false;
					}
					if(( (mCurrentActivityName!=null) &&//"com.android.cts.ui.ScrollingActivity"
							mCurrentActivityName.equalsIgnoreCase("com.yulong.android.factorypattern.touchcalibrate.PointerLocation"))){
						Log.d(TAG, " mCurrentActivityName="+mCurrentActivityName);
						return false;
					}
					if (canShotscreen && newRawY < mGetBgLmt) {
						canShotscreen = false;
						mWmParams.y = 0;
						mWm.updateViewLayout(mBgFrame, mWmParams);
					}

					deltaY = newRawY - oldRawY;

					if (!isPopuped || (isPopuped && (v.equals(mDwArraw) || v.equals(mBgFrame)))) {
						float alpha;
						if (!isPopuped) {
							alpha = calcBgColorAlpha(mQSRootHeight - (mMainHeight - mQSBottomPanel.getY()), true);
							if(!Utilities.showFullGaussBlurForDDQS()){
								mBgFrame.setBackgroundColor(Color.argb((int) (255 * (0.5f - alpha)), 0, 0, 0));
							}
						
                            if (DEBUG2) {
                                mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
                                mBgFrame.setAlpha(1f);
                            }
						} else {
							alpha = calcBgColorAlpha(mMainHeight - mQSBottomPanel.getY(), false);
							if(!Utilities.showFullGaussBlurForDDQS()){
								mBgFrame.setBackgroundColor(Color.argb((int) (255 * alpha), 0, 0, 0));
							}							
                            if (DEBUG2) {
                                mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
                                mBgFrame.setAlpha(1f);
                            }
						}
						if (newRawY > mTopLimit+Utilities.dipToPixel(mContext, 8)) {
							if (!isPopuped
							/* && newRawY > mMainHeight - mNaviBarHeight / 2 */
							&& Math.abs(newRawY - downRawY) < mNaviBarHeight / 2
									&& Configuration.ORIENTATION_PORTRAIT == mOrientation && !mKeyguardManager.isKeyguardLocked()) {
							} else {
								if (deltaY < 0) {
									if(!Utilities.showFullGaussBlurForDDQS()){
										mBgFrame.setAlpha(1f);
									}
									
								}
								mQSBottomPanel.setY(newRawY);
								//Log.d("","5000050 desY="+mQSBottomPanel.getY());
							}
						}
					}
					oldRawY = newRawY;
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				isQSEvent = false;

				if (isFullScreen && !isPopuped) {
					if (isHintShow) {
						// mScreenHandler.sendEmptyMessage(HINTDW);
						return false;
					} else {
						if (deltaY < 0 && !isDown) {
							mScreenHandler.removeMessages(HINTDW);
							mScreenHandler.sendEmptyMessage(HINTUP);
							mScreenHandler.sendEmptyMessageDelayed(HINTDW, 2500);
						}
					}
				} else if (isSatusbarShow) {
					if (deltaY <= 0) {
						statusBM.collapsePanels();
						if (mStatusBar.isQsExpanded()) {
							mStatusBar.setQsExpanded(false);
						}
						isSatusbarShow = false;
					}
				} else {
					if (isIconlocation) {
						isIconlocation = false;
						return false;
					}
					Log.d(TAG, "ACTION_MOVE: isWizardUI:" + isWizardUI + "; isIconlocation:" + isIconlocation
							 + "; isHintShow:" + isHintShow);
					Log.d(TAG, "ACTION_UP: deltaY:" + deltaY + "; isPopuped:" + isPopuped);
					if (isPopuped && deltaY >= 0 && (v.equals(mBgFrame) || v.equals(mDwArraw))) {
						// mScreenHandler.sendEmptyMessage(DW);
						hideBottomView();
					} else if (deltaY >= 0) {
						if (mQSBottomPanel.getY() > mTopLimit + mQSRootHeight / 3) {
							Log.e(TAG, "move DW");
							// mScreenHandler.sendEmptyMessage(DW);
							hideBottomView();
						} else {
							Log.e(TAG, "move UP");
							// mScreenHandler.sendEmptyMessage(UP);
							showBottomView(0f);
						}
					} else {

						long deltaTime = System.currentTimeMillis() - start;
						float vPixes, remainTime;

						if (deltaTime != 0 && deltaY != 0) {
							vPixes = Math.abs(deltaY) / deltaTime;
							remainTime = (oldRawY - mTopLimit) / vPixes;
						} else {
							remainTime = 0;
						}

						if (mQSBottomPanel.getY() < mBottomLimit - mQSRootHeight / 8) {
							Log.d(TAG, "go UP");
							// mScreenHandler.sendEmptyMessage(UP);

							showBottomView(remainTime);
						} else {
							Log.d(TAG, "go DW");
							// mScreenHandler.sendEmptyMessage(DW);
							hideBottomView();
						}

					}
				}

				break;
			default:
				Log.e(TAG, "===== ACTION_OUTSIDE ===== :" + e.getAction());
				break;
			}
			return true;
		}

	};

	private boolean invokeIsFullScreen() {
		boolean isFull = false;
		try {
			Method method = WindowManager.class.getDeclaredMethod("isTopWindowFullScreen");
			method.setAccessible(true);
			isFull = (boolean) method.invoke(mWm);
		} catch (Exception e) {
		}

		return isFull;
	}

	TranslateAnimation tAnim;

	private void showBottomView(float remainTime) {
		int animT = 200;

		if (remainTime != 0 && remainTime < animT) {
			animT = remainTime < 80 ? 80 : (int) remainTime;
		}

		ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
		valueAnimator.setDuration(animT);
		final float distance = mQSBottomPanel.getY()- mTopLimit-Utilities.dipToPixel(mContext, 8);
		final float initRootY = mQSBottomPanel.getY();
		float finaY = initRootY + distance;
		//Log.d("","5000051 t="+remainTime+" animT="+animT+" dis="+distance+" getY="+initRootY+" destY="+finaY);
		valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (float) animation.getAnimatedValue();
				if(!Utilities.showFullGaussBlurForDDQS()){
					mBgFrame.setBackgroundColor(Color.argb((int) (255 * (mAlpha + (0.5 - mAlpha) * value)), 0, 0, 0));
				}
				
		        if (DEBUG2) {
		            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
		        }
				mQSBottomPanel.setTranslationY(initRootY - distance * value);
				//Log.d("","5000052 desY="+mQSBottomPanel.getY());
			}
		});
		valueAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				popUP();

				Intent intentUp = new Intent();
				intentUp.setAction("android.intent.action.BOTTOMPANEL_UP");
				mContext.sendBroadcast(intentUp);
			}

			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}
		});

		valueAnimator.start();
	}

	public void hideBottomView() {
		int animT = 350;

		if (mQSBottomPanel.getY() <= mTopLimit) {
			animT = 350;
			mAlpha = 0.5f;
		} else if (mQSBottomPanel.getY() > mTopLimit && mQSBottomPanel.getY() < mTopLimit + mQSRootHeight / 4) {
			animT = 200;
		} else if (mQSBottomPanel.getY() >= mTopLimit + mQSRootHeight / 4
				&& mQSBottomPanel.getY() < mTopLimit + mQSRootHeight / 2) {
			animT = 150;
		} else {
			animT = 100;
		}

		ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0);
		valueAnimator.setDuration(animT);
		final float distance = mBottomLimit - mQSBottomPanel.getY();
		final float initRootY = mQSBottomPanel.getY();
		valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (float) animation.getAnimatedValue();
				if(!Utilities.showFullGaussBlurForDDQS()){
					mBgFrame.setBackgroundColor(Color.argb((int) (255 * mAlpha * value), 0, 0, 0));
				}
				
		        if (DEBUG2) {
		            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
		            mBgFrame.setAlpha(1f);
		        }
				mQSBottomPanel.setTranslationY(initRootY + distance * (1 - value));
			}
		});
		valueAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				popDW();
				mQSBottomPanel.setVisibility(View.VISIBLE);
				Intent intentDw = new Intent();
				intentDw.setAction("android.intent.action.BOTTOMPANEL_DOWN");
				mContext.sendBroadcast(intentDw);

				mQSBottomPanel.resetPagers();
			}

			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}
		});

		valueAnimator.start();
	}

	private void avoidDimScreen() {
		mStatusBar.userActivity();
	}

	private float calcBgColorAlpha(float distance, boolean ispopup) {
		float alpha = 0.5f;
		alpha = alpha * (distance / mQSRootHeight);
		if (ispopup) {
			setBgColorAlpha(0.5f - alpha);
		} else {
			setBgColorAlpha(alpha);
		}
		return alpha;
	}

	private float mAlpha = 0.5f;

	private void setBgColorAlpha(float alpha) {
		mAlpha = alpha;
	}

	int animTime = 0;
	private static final int UP = 0, DW = 1, HINTUP = 2, HINTDW = 3, GUIDE = 4;
	private Handler mScreenHandler = new Handler() {
		public void handleMessage(Message msg) {
			TranslateAnimation tAnim;
			final int animT = 200;
			super.handleMessage(msg);
			switch (msg.what) {
			case UP:
				tAnim = new TranslateAnimation(0, 0, 0, mTopLimit - mQSBottomPanel.getY());
				tAnim.setFillAfter(true);
				tAnim.setDuration(animT);
				tAnim.setAnimationListener(new MyAnimationListener() {
					public void onAnimationEnd(Animation animation) {
						popUP();

						Intent intentUp = new Intent();
						intentUp.setAction("android.intent.action.BOTTOMPANEL_UP");
						mContext.sendBroadcast(intentUp);
					}
				});
				mQSBottomPanel.startAnimation(tAnim);
				break;
			case DW:
				tAnim = new TranslateAnimation(0, 0, 0, mBottomLimit - mQSBottomPanel.getY());
				tAnim.setFillAfter(true);
				tAnim.setDuration(animT);
				tAnim.setAnimationListener(new MyAnimationListener() {
					public void onAnimationEnd(Animation animation) {
						popDW();
						mQSBottomPanel.resetPagers();

						Intent intentDw = new Intent();
						intentDw.setAction("android.intent.action.BOTTOMPANEL_DOWN");
						mContext.sendBroadcast(intentDw);
					}
				});
				mQSBottomPanel.startAnimation(tAnim);
				break;
			case HINTUP:
				if (isFullScreen && !isPopuped && !isHintShow) {
					// popUPHvView();

					mQSBottomPanel.setVisibility(View.GONE);

					mTailImageView.setY(mMainHeight);
					mTailImageView.setVisibility(View.VISIBLE);
					mTailImageView.bringToFront();

//					mBgFrame.setBackgroundColor(Color.argb(0x0, 0, 0, 0));
//					mBgFrame.setAlpha(1f);

					ValueAnimator valueanimator = ValueAnimator.ofFloat(0, 1);
					valueanimator.setDuration(100);
					valueanimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							float value = (float) animation.getAnimatedValue();
							mTailImageView.setY(mMainHeight - hvHeight * value);
						}
					});

					valueanimator.addListener(new Animator.AnimatorListener() {

						@Override
						public void onAnimationStart(Animator arg0) {
							popUPHvView();
						}

						@Override
						public void onAnimationRepeat(Animator arg0) {
						}

						@Override
						public void onAnimationEnd(Animator arg0) {
						}

						@Override
						public void onAnimationCancel(Animator arg0) {
						}
					});
					valueanimator.start();
				}
				break;
			case HINTDW:
				if (isHintShow) {
					Log.e("Test", "HINTDW:isHintShow");
					ValueAnimator valueanimator = ValueAnimator.ofFloat(1, 0);
					valueanimator.setDuration(100);
					valueanimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							float value = (float) animation.getAnimatedValue();

							mTailImageView.setY(mMainHeight - hvHeight * value);
						}
					});

					valueanimator.addListener(new Animator.AnimatorListener() {

						@Override
						public void onAnimationStart(Animator arg0) {
						}

						@Override
						public void onAnimationRepeat(Animator arg0) {
						}

						@Override
						public void onAnimationEnd(Animator arg0) {
							popDWHvView();
							mQSBottomPanel.setVisibility(View.VISIBLE);
							mTailImageView.setVisibility(View.GONE);
//							mBgFrame.setBackgroundColor(Color.argb(0x80, 0, 0, 0));
						}

						@Override
						public void onAnimationCancel(Animator arg0) {
						}
					});
					valueanimator.start();
				}
				break;
			case GUIDE:
				Intent qsIntent = new Intent(START_QS_GUIDE_ACTION);
				qsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(qsIntent);
				break;
			}
		}
	};

	private void popUPHvView() {
		mWmParams.y = 0;
		if(!Utilities.showFullGaussBlurForDDQS()){
			mBgFrame.setAlpha(1f);
		}
		
        if (DEBUG2) {
            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
            mBgFrame.setAlpha(1f);
        }
		if (mBgFrame.isAttachedToWindow()) {
			mWm.updateViewLayout(mBgFrame, mWmParams);
		}
		mTailImageView.setY(mHintViewLimit);
		// img.clearAnimation();
		isHintShow = true;
	}

	private void popDWHvView() {
		mWmParams.y = mBottomLimit;
		mBgFrame.setAlpha(0.0f);
        if (DEBUG2) {
            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
            mBgFrame.setAlpha(1f);
        }
		if (mBgFrame.isAttachedToWindow()) {
			mWm.updateViewLayout(mBgFrame, mWmParams);
		}
		mTailImageView.setY(0);

		// img.clearAnimation();
		isHintShow = false;
	}

    private void popUP() {
        mQSBottomPanel.setY(mTopLimit+Utilities.dipToPixel(mContext, 12));//8
    	//Log.d("","5000054 desY="+mQSBottomPanel.getY());
        if(Utilities.showFullGaussBlurForDDQS()){
			mBgFrame.setAlpha(0.8f);
		}else{
			mBgFrame.setAlpha(1.0f);
		}
        if (DEBUG2) {
            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
        }
        //mWmParams.y = Utilities.dipToPixel(mContext, 8);
        if (mBgFrame.isAttachedToWindow()) {
            mWm.updateViewLayout(mBgFrame, mWmParams);
        }
        addBackKeyListener();
        mQSBottomPanel.clearAnimation();
        isPopuped = true;
    }

    private void popDW() {
        DEBUG2 = debugFlag.exists();// yaolihui@yulong.com_20160804:push '/data/systemui_debug.txt' open switcher
        DEBUG = debugFlag.length() > 0;
        if (null != mBackKeyView && mBackKeyView.isAttachedToWindow()) {
            mBackKeyView.setOnKeyListener(null);
            mWm.removeViewImmediate(mBackKeyView);
        }
        mWmParams.y = mBottomLimit;
        mBgFrame.setAlpha(0.0f);
        if (DEBUG2) {
            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
            mBgFrame.setAlpha(1f);
        }
        if (mBgFrame.isAttachedToWindow()) {
            mWm.updateViewLayout(mBgFrame, mWmParams);
        }
        mQSBottomPanel.setY(mBottomLimit);

        if(Utilities.showFullGaussBlurForDDQS()){
//        	mQSBottomPanel.getQSPannelView().setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//    		mQSBottomPanel.getMobilePanelView().setBlurMode(BlurParams.BLUR_MODE_NONE);
//    		mQSBottomPanel.getDetail().setBlurMode(BlurParams.BLUR_MODE_NONE);
    		
			IvviGaussBlurViewFeature.setBlurMode(mQSBottomPanel.getQSPannelView(), 
					IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
			IvviGaussBlurViewFeature.setBlurMode(mQSBottomPanel.getMobilePanelView(), 
					IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_NONE"));
			IvviGaussBlurViewFeature.setBlurMode(mQSBottomPanel.getDetail(), 
					IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_NONE"));
		}
        
        mQSBottomPanel.clearAnimation();
        isPopuped = false;
        isFullScreen = false;
        System.gc();
    }

	private class MyAnimationListener implements AnimationListener {
		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	private BroadcastReceiver mBroadcast = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			Log.d(TAG, "action:" + action + ";  isKeyguardLocked:" + mKeyguardManager.isKeyguardLocked());
			if (mKeyguardManager.isKeyguardLocked()) {
				mTopLimit = mMainHeight - mQSRootHeight;
				popDW();
			} else {
				mTopLimit = mMainHeight - mQSRootHeight;
			}

			if (START_QUICK_SETTING.equals(action)) {
				mScreenHandler.sendEmptyMessage(UP);
				// showBottomView(0f);
			} else if (START_QS_GUIDE.equals(action)) {
				mScreenHandler.sendEmptyMessageDelayed(GUIDE, 5000);
			} else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				mScreenHandler.sendEmptyMessage(DW);
				// hideBottomView();
			} else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action) && isPopuped) {
				mQSBottomPanel.setY(mBottomLimit);
				mWmParams.y = mBottomLimit;
				mWm.updateViewLayout(mBgFrame, mWmParams);

				if (null != mBackKeyView && mBackKeyView.isAttachedToWindow()) {
					mBackKeyView.setOnKeyListener(null);
					mWm.removeViewImmediate(mBackKeyView);
				}

				isPopuped = false;
				mBgFrame.setAlpha(0.0f);
                if (DEBUG2) {
                    mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
                    mBgFrame.setAlpha(1f);
                }
            }
			//wanghuazhi forbid slidup while test FactoryPattern screenTouch
			/* else if (FACTORYPARRERN_PULLINTERFACE_FORBID.equals(action)){
            	mShouldForbid = (Boolean)intent.getExtras().get("FORBID");
            	Log.d(TAG, "FACTORYPARRERN_PULLINTERFACE_FORBID, shouldForbid = " + mShouldForbid);
            	if(mShouldForbid){
            		mWmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_NO_LIMITS
            				| LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_NOT_TOUCHABLE;
            		mWmParams.y = mMainHeight;
                    mWm.updateViewLayout(mBgFrame, mWmParams);
            	}else{
            		mWmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_NO_LIMITS
            				| LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            		mWmParams.y = mMainHeight - mTopBlankHeight;
                    mWm.updateViewLayout(mBgFrame, mWmParams);
            	}
            }*/


		}
	};
    private BroadcastReceiver mReceiver4CTS = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mReceiver4CTS, action = " + action);
            if (Configuration.ORIENTATION_PORTRAIT == mOrientation) {
                String data = intent.getDataString();
                if (null != data) {
                    data = data.substring(8);
                    if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                        Log.e(TAG, "ACTION_PACKAGE_ADDED:" + data);
                        if ("android.jank.cts.ui.CtsDeviceJankUi".contains(data)) {//com.android.cts.ui
                            mWmParams.y = mMainHeight;
                            mWm.updateViewLayout(mBgFrame, mWmParams);
                        }
                    } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                        Log.e(TAG, "ACTION_PACKAGE_REMOVED:" + data);
                        if ("android.jank.cts.ui.CtsDeviceJankUi".contains(data)) {
                            mWmParams.y = mMainHeight - mTopBlankHeight;
                            mWm.updateViewLayout(mBgFrame, mWmParams);
                        }
                    }
                }
            }

        }
    };
	KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
		
		@Override
		public void onUserSwitchComplete(int userId) {
			recreateViews();
			if (!Utilities.isPrimaryUser()) {
				disableQuickSetting(true);
			}
		}
		
		@Override
		public void onKeyguardVisibilityChanged(boolean showing) {
			if (!Utilities.isPrimaryUser() && !showing) {
				disableQuickSetting(false);
			}
		};
	};

	public void onDestroy() {
		mQSBottomPanel.onDestroy();
		mQSBottomPanel.removeAllViews();
		mBgFrame.removeAllViews();
		mWm.removeView(mBgFrame);

		if (tAnim != null) {
			tAnim.cancel();
			tAnim = null;
		}

		mQSBottomPanel = null;
		mBgFrame = null;

		initTouchFlag();
		// mContext.unregisterReceiver(mBroadcast);
	}

	public void bitMap2Png(final Bitmap src, final String path) {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(path));
			src.compress(Bitmap.CompressFormat.PNG, 100, bos);
			bos.flush();
		} catch (Exception e) {
			Log.e(TAG, "outputPic:" + e.getMessage());
		} finally {
			if (null != bos) {
				try {
					bos.close();
				} catch (IOException e) {
					Log.e(TAG, "close BufferedOutputStream:" + e.getMessage());
				}
			}
		}
	}

	private int getStatusBarHeight() {
		if (null == mStatusBarHeight) {
			Class<?> c = null;
			Object obj = null;
			Field field = null;
			int x = 0;
			try {
				c = Class.forName("com.android.internal.R$dimen");
				obj = c.newInstance();
				field = c.getField("status_bar_height");
				x = Integer.parseInt(field.get(obj).toString());
				mStatusBarHeight = mResources.getDimensionPixelSize(x);
			} catch (Exception e1) {
				e1.printStackTrace();
				mStatusBarHeight = 72;
			}
		}
		return mStatusBarHeight;
	}

	private void addBackKeyListener() {
//		if(true){
//			return;
//		}
//		if (mBackKeyView != null) {
//			return;
//		}
		if (null == mBackKeyView) {
			mBackKeyView = new View(mContext);
		}
		if (mBackKeyView.isAttachedToWindow()) {
			return;
		}
		mBackKeyView.setOnKeyListener(mBackKeyLst);
		//mWmParams.type = LayoutParams.TYPE_DOCK_DIVIDER;
		
		mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		mWmParams.height = 0;
		mWmParams.width = 0;
		mWm.addView(mBackKeyView, mWmParams);

		mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		mWmParams.width = mScreenWidth;
		mWmParams.height = mQSRootHeight;
	}

	private OnKeyListener mBackKeyLst = new OnKeyListener() {
		public boolean onKey(View v, int key, KeyEvent e) {
			if (KeyEvent.ACTION_UP == e.getAction() && KeyEvent.KEYCODE_BACK == e.getKeyCode() && isPopuped) {
				// mScreenHandler.sendEmptyMessage(DW);
				mQSBottomPanel.setMobilePanelViewVisible(false);

				if (null != mBackKeyView && mBackKeyView.isAttachedToWindow()) {
					mBackKeyView.setOnKeyListener(null);
					mWm.removeViewImmediate(mBackKeyView);
				}

				mQSBottomPanel.setY(mBottomLimit);
				mWmParams.y = mBottomLimit;
				if (mBgFrame.isAttachedToWindow()) {
					mWm.updateViewLayout(mBgFrame, mWmParams);
				}

				isPopuped = false;
				mBgFrame.setAlpha(0.0f);
		        if (DEBUG2) {
		            mBgFrame.setBackgroundColor(Color.argb(0x80, 0xFF, 0, 0));
		            mBgFrame.setAlpha(1f);
		        }
			}

			return true;
		}
	};

    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG, "onConfigurationChanged: " + newConfig.orientation);
        if (newConfig != null) {
            if (mOrientation != newConfig.orientation) {
                Log.d(TAG, "newConfig=" + newConfig);
                mOrientation = newConfig.orientation;
                recreateViews();
                System.gc();
            }
        }
    }

	private void recreateViews() {
		if (isPopuped)
			popDW();
		if (isHintShow)
			popDWHvView();
		onDestroy();
		start();
		mQSBottomPanel.resetFlashLight();
	}

	public void updateBottomQsPanel(ArrayList<DatabaseRecord> data) {
		if (data != null) {
			onDestroy();
			// mThis = null;
			QuickSettingsPannelView.setInitFlag(true);
			start();
			QuickSettingsPannelView.setInitFlag(false);

			mQSBottomPanel.resetFlashLight();
		}
	}

	private void initTouchFlag() {
		isWizardUI = false;
		isIconlocation = false;
		isSatusbarShow = false;
		isFullScreen = false;
		isHintShow = false;
		isDown = false;
		isForbidden = false;
	}

	public void updateStatusBar() {
		mStatusBar.onBottomQSUpdates();
		mNotificationPanelView = mStatusBar.mNotificationPanel;
		new KeyguardAffordanceHelper(mNotificationPanelView, mContext);
	}

	public void setBottomPanelVisible(boolean isShow) {
		mQSBottomPanel.setBottomPanelVisible(isShow);
	}

	public void setMobilePanelViewVisible(boolean show) {
		mQSBottomPanel.setMobilePanelViewVisible(show);
	}

	public PhoneStatusBar getStatusBar() {
		return mStatusBar;
	}

	public QSBottomPanel getQSPanel() {
		return mQSBottomPanel;
	}

    public void setBlankLimitHeight(boolean isKeyguard) {
        boolean isKeyguardLocked = mKeyguardManager.isKeyguardLocked();
        Log.w(TAG, "setBlankLimitHeight=" + isKeyguard + "; isKeyguardLocked=" + isKeyguardLocked);
        if (isKeyguard && isKeyguardLocked) {
            mBottomLimit = mMainHeight - mKeyguardBlankHeight;
        } else {
            if (showNav)  {
                mTopBlankHeight = 0;
            } else {
                mTopBlankHeight = (int) mResources.getDimension(R.dimen.quick_setting_top_pan_height);
            }
            mBottomLimit = mMainHeight - mTopBlankHeight;
        }
//        mWmParams.y = mBottomLimit;
//        if (mBgFrame.isAttachedToWindow()) {
//            mWm.updateViewLayout(mBgFrame, mWmParams);
//        }
        popDW();
    }

	public void disableQuickSetting(boolean forbidden) {
		isForbidden = forbidden;
	}

	public void notifyBottomIconChanged(TreeMap<Integer, CheckablePackageInfo> treeMap) {
		mQSBottomPanel.onBottomIconUpdate(treeMap);
	}
	
//    private boolean isCTSMode() {
//        String CTS_flag = SystemProperties.get("persist.yulong.ctstest");
//        if (null != CTS_flag && CTS_flag.equals("1")) {
//            return true;
//        }
//        return false;
//    }

}
