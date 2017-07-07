/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.RemoteException;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.IDockedStackListener.Stub;
import android.view.View.MeasureSpec;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.RecentsComponent;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.quicksettings.bottom.QuickSettingLauncher;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DelegateViewHelper;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NavigationBarView extends LinearLayout {
    final static boolean DEBUG = false;
    final static String TAG = "PhoneStatusBar/NavigationBarView";
    public static final String NAVIGATIONBAR_FIXED = "is_navigationbar_fixed";
    public static final String NAVIGATIONBAR_COMBINATION = "virtual_keys_combination_type";

    // slippery nav bar when everything is disabled, e.g. during setup
    final static boolean SLIPPERY_WHEN_DISABLED = true;
    private static boolean isForceShown = false;
    private static boolean mDisableHome = false;
    final Display mDisplay;
    View mCurrentView = null;
    //View[][] mRotatedTypeViews = new View[4][4];
    View[] mRotatedViews = new View[4];
    String[] mStrRotatedViews = new String[4];

    int mBarSize;
    boolean mVertical;
    boolean mScreenOn;

    boolean mShowMenu;
    int mDisabledFlags = 0;
    int mNavigationIconHints = 0;
    int mNavigationType = 0;

    private  int quide = 0;
    
	int navigationBarColor1 = 0xFF000000;
    int greyTransColor1 = 0xFFd4d4d4;
    public static final String NAVIGATIONBAR_BACKGROUND = "virtual_keys_background";
    
    private int mNavigationBarColor = Color.TRANSPARENT;
    private Drawable mBackIcon, mBackLandIcon, mBackAltIcon, mBackAltLandIcon;
    private Drawable mBackCarModeIcon, mBackLandCarModeIcon;
    private Drawable mBackAltCarModeIcon, mBackAltLandCarModeIcon;
    private Drawable mHomeDefaultIcon, mHomeCarModeIcon;
    private Drawable mRecentIcon;
    private Drawable mDockedIcon;
    private Drawable mImeIcon;
    private Drawable mMenuIcon;

    private DelegateViewHelper mDelegateHelper;
    private NavigationBarGestureHelper mGestureHelper;
    private DeadZone mDeadZone;
    private final NavigationBarTransitions mBarTransitions;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    // performs manual animation in sync with layout transitions
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();

    private OnVerticalChangedListener mOnVerticalChangedListener;
    private boolean mWakeAndUnlocking;
    private boolean mCarMode = false;
    private boolean mDockedStackExists;

    private final SparseArray<ButtonDispatcher> mButtonDisatchers = new SparseArray<>();
    private Configuration mConfiguration;
    private boolean mIsLayoutRtl;
    private boolean mDelegateIntercepted;
	private NavigationBarSettingObserver NavigationBarObserver;
    private boolean mLayoutTransitionsEnabled;
	private boolean mIsOverSea= false;
	private static int mOrientation = Configuration.ORIENTATION_PORTRAIT;
	private StatusBarManager statusBM;
	NavigationBarInflaterView mNavigationBarInflaterView;

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private boolean mHomeAppearing;
        private long mStartDelay;
        private long mDuration;
        private TimeInterpolator mInterpolator;

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = true;
                mStartDelay = transition.getStartDelay(transitionType);
                mDuration = transition.getDuration(transitionType);
                mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            ButtonDispatcher backButton = getBackButton();

            // When dismissing ime during unlock, force the back button to run the same appearance
            // animation as home (if we catch this condition early enough).
            if (!mBackTransitioning && backButton.getVisibility() == VISIBLE
                    && mHomeAppearing && getHomeButton().getAlpha() == 0) {
                getBackButton().setAlpha(0);
                ValueAnimator a = ObjectAnimator.ofFloat(backButton, "alpha", 0, 1);
                a.setStartDelay(mStartDelay);
                a.setDuration(mDuration);
                a.setInterpolator(mInterpolator);
                a.start();
            }
        }
    }

    private final OnClickListener mImeSwitcherClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mContext.getSystemService(InputMethodManager.class)
                    .showInputMethodPicker(true /* showAuxiliarySubtypes */);
        }
    };

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = getCurrentView().getWidth();
                    final int vh = getCurrentView().getHeight();

                    if (h != vh || w != vw) {
                        Log.w(TAG, String.format(
                            "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                            how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private int mScreenWidth;
    private int mScreenHeight;
    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDisplay = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplay.getRealMetrics(mDisplayMetrics);
		mScreenWidth = mDisplayMetrics.widthPixels;
		mScreenHeight = mDisplayMetrics.heightPixels;

        mBarSize = context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        mGestureHelper = new NavigationBarGestureHelper(context);
        mDelegateHelper = new DelegateViewHelper(this);

        mConfiguration = new Configuration();
        mConfiguration.updateFrom(context.getResources().getConfiguration());
        updateIcons(context, Configuration.EMPTY, mConfiguration);

        mBarTransitions = new NavigationBarTransitions(this);

        mButtonDisatchers.put(R.id.back, new ButtonDispatcher(R.id.back));
        mButtonDisatchers.put(R.id.home, new ButtonDispatcher(R.id.home));
        mButtonDisatchers.put(R.id.recent_apps, new ButtonDispatcher(R.id.recent_apps));
        mButtonDisatchers.put(R.id.menu, new ButtonDispatcher(R.id.menu));
        mButtonDisatchers.put(R.id.ime_switcher, new ButtonDispatcher(R.id.ime_switcher));
        mButtonDisatchers.put(R.id.hide, new ButtonDispatcher(R.id.hide));
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setDelegateView(View view) {
        mDelegateHelper.setDelegateView(view);
    }
    PhoneStatusBar mPhoneStatusBar;
    public void setBar(PhoneStatusBar phoneStatusBar) {
    	mPhoneStatusBar = phoneStatusBar;
        mDelegateHelper.setBar(phoneStatusBar);
    }
    public void setComponents(RecentsComponent recentsComponent, Divider divider) {
        mGestureHelper.setComponents(recentsComponent, divider, this);
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(mVertical);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	if(Utilities.needFakeNavigationBarView()){
    		int innerWidth = mScreenWidth;
            int innerHeight = Utilities.dipToPixel(mContext, 5);;
            if (DEBUG) {
                Log.d(TAG, "innerWidth:" + innerWidth + "; innerHeight:" + innerHeight);
            }
            setMeasuredDimension(innerWidth, innerHeight);
    	}else{
    		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    	}
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!QuickSettingLauncher.isPopuped && mOrientation != Configuration.ORIENTATION_LANDSCAPE){
            if(QuickSettingLauncher.getInstance(mContext) != null)QuickSettingLauncher.getInstance(mContext).dispatchTouchListener(event);
        }
        
        initDownStates(event);
        if (!mDelegateIntercepted && mGestureHelper.onTouchEvent(event)) {
            return true;
        }
        if (mDeadZone != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mDeadZone.poke(event);
        }
        if (mDelegateHelper != null && mDelegateIntercepted) {
            boolean ret = mDelegateHelper.onInterceptTouchEvent(event);
            if (ret) return true;
        }
        return super.onTouchEvent(event);
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDelegateIntercepted = false;
        }
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!QuickSettingLauncher.isPopuped && mOrientation != Configuration.ORIENTATION_LANDSCAPE){
            if(QuickSettingLauncher.getInstance(mContext) != null)QuickSettingLauncher.getInstance(mContext).dispatchTouchListener(event);
        }
        
        initDownStates(event);
        
        mDelegateIntercepted = mDelegateHelper.onInterceptTouchEvent(event);
        boolean intercept = mDelegateIntercepted;
        
        if(intercept){
        	return intercept;
        }
        return mGestureHelper.onInterceptTouchEvent(event);
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    private H mHandler = new H();

    public View getCurrentView() {
        return mCurrentView;
    }

    public View[] getAllViews() {
        return mRotatedViews;
    }

    public ButtonDispatcher getRecentsButton() {
        return mButtonDisatchers.get(R.id.recent_apps);
    }

    public ButtonDispatcher getMenuButton() {
        return mButtonDisatchers.get(R.id.menu);
    }

    public ButtonDispatcher getBackButton() {
        return mButtonDisatchers.get(R.id.back);
    }

    public ButtonDispatcher getHomeButton() {
        return mButtonDisatchers.get(R.id.home);
    }

    public ButtonDispatcher getImeSwitchButton() {
        return mButtonDisatchers.get(R.id.ime_switcher);
    }
    
    public ButtonDispatcher getHideButton() {
        //return mCurrentView.findViewById(R.id.hide);
        return mButtonDisatchers.get(R.id.hide);
    }

    private void updateCarModeIcons(Context ctx) {
        mBackCarModeIcon = ctx.getDrawable(R.drawable.ic_sysbar_back_carmode);
        mBackLandCarModeIcon = mBackCarModeIcon;
        mBackAltCarModeIcon = ctx.getDrawable(R.drawable.ic_sysbar_back_ime_carmode);
        mBackAltLandCarModeIcon = mBackAltCarModeIcon;
        mHomeCarModeIcon = ctx.getDrawable(R.drawable.ic_sysbar_home_carmode);
    }

    private void updateIcons(Context ctx, Configuration oldConfig, Configuration newConfig) {
        if (oldConfig.orientation != newConfig.orientation
                || oldConfig.densityDpi != newConfig.densityDpi) {
            mDockedIcon = ctx.getDrawable(R.drawable.ic_sysbar_docked);
        }
        if (oldConfig.densityDpi != newConfig.densityDpi) {
            mBackIcon = ctx.getDrawable(R.drawable.ic_sysbar_back);
            mBackLandIcon = mBackIcon;
            mBackAltIcon = ctx.getDrawable(R.drawable.ic_sysbar_back_ime);
            mBackAltLandIcon = mBackAltIcon;

            mHomeDefaultIcon = ctx.getDrawable(R.drawable.ic_sysbar_home);
            mRecentIcon = ctx.getDrawable(R.drawable.ic_sysbar_recent);
            mMenuIcon = ctx.getDrawable(R.drawable.ic_sysbar_menu);
            mImeIcon = ctx.getDrawable(R.drawable.ic_ime_switcher_default);

            updateCarModeIcons(ctx);
        }
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        // Reload all the icons
        updateIcons(getContext(), Configuration.EMPTY, mConfiguration);

        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
        setDisabledFlags(mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    private Drawable getBackIconWithAlt(boolean carMode, boolean landscape) {
        return landscape
                ? carMode ? mBackAltLandCarModeIcon : mBackAltLandIcon
                : carMode ? mBackAltCarModeIcon : mBackAltIcon;
    }

    private Drawable getBackIcon(boolean carMode, boolean landscape) {
        return landscape
                ? carMode ? mBackLandCarModeIcon : mBackLandIcon
                : carMode ? mBackCarModeIcon : mBackIcon;
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;
        final boolean backAlt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        if ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0 && !backAlt) {
            mTransitionListener.onBackAltCleared();
        }
        if (DEBUG) {
            android.widget.Toast.makeText(getContext(),
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        // We have to replace or restore the back and home button icons when exiting or entering
        // carmode, respectively. Recents are not available in CarMode in nav bar so change
        // to recent icon is not required.
        Drawable backIcon = (backAlt)
                ? getBackIconWithAlt(mCarMode, mVertical)
                : getBackIcon(mCarMode, mVertical);

        getBackButton().setImageDrawable(backIcon);

        updateRecentsIcon();

        if (mCarMode) {
            getHomeButton().setImageDrawable(mHomeCarModeIcon);
        } else {
            getHomeButton().setImageDrawable(mHomeDefaultIcon);
        }

        final boolean showImeButton = ((hints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) != 0);
        getImeSwitchButton().setVisibility(showImeButton ? View.VISIBLE : View.INVISIBLE);
        getImeSwitchButton().setImageDrawable(mImeIcon);

        // Update menu button in case the IME state has changed.
        setMenuVisibility(mShowMenu, true);
        //getMenuButton().setImageDrawable(mMenuIcon);

        setDisabledFlags(mDisabledFlags, true);
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);

        // Disable recents always in car mode.
        boolean disableRecent = (
                mCarMode || (disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0)
                && ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);
        final boolean disableSearch = ((disabledFlags & View.STATUS_BAR_DISABLE_SEARCH) != 0);

        if (SLIPPERY_WHEN_DISABLED) {
            setSlippery(disableHome && disableRecent && disableBack && disableSearch);
        }

        ViewGroup navButtons = (ViewGroup) getCurrentView().findViewById(R.id.nav_buttons);
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!lt.getTransitionListeners().contains(mTransitionListener)) {
                    lt.addTransitionListener(mTransitionListener);
                }
            }
        }
        if (inLockTask() && disableRecent && !disableHome) {
            // Don't hide recents when in lock task, it is used for exiting.
            // Unless home is hidden, then in DPM locked mode and no exit available.
            disableRecent = false;
        }

        getBackButton().setVisibility(disableBack      ? View.INVISIBLE : View.VISIBLE);
        getHomeButton().setVisibility(disableHome      ? View.INVISIBLE : View.VISIBLE);
        getRecentsButton().setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);
    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setLayoutTransitionsEnabled(boolean enabled) {
        mLayoutTransitionsEnabled = enabled;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        mWakeAndUnlocking = wakeAndUnlocking;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean enabled = !mWakeAndUnlocking && mLayoutTransitionsEnabled;
        ViewGroup navButtons = (ViewGroup) getCurrentView().findViewById(R.id.nav_buttons);
        LayoutTransition lt = navButtons.getLayoutTransition();
        if (lt != null) {
            if (enabled) {
                lt.enableTransitionType(LayoutTransition.APPEARING);
                lt.enableTransitionType(LayoutTransition.DISAPPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            } else {
                lt.disableTransitionType(LayoutTransition.APPEARING);
                lt.disableTransitionType(LayoutTransition.DISAPPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            }
        }
    }

    private void setUseFadingAnimations(boolean useFadingAnimations) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean old = lp.windowAnimations != 0;
            if (!old && useFadingAnimations) {
                lp.windowAnimations = R.style.Animation_NavigationBarFadeIn;
            } else if (old && !useFadingAnimations) {
                lp.windowAnimations = 0;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & WindowManager.LayoutParams.FLAG_SLIPPERY) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) return;

        mShowMenu = show;

        // Only show Menu if IME switcher not shown.
        final boolean shouldShow = mShowMenu &&
                ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) == 0);

        //getMenuButton().setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
    }

	//final int typeId[] = {R.id.type0,R.id.type1,R.id.type2,R.id.type3};

    @Override
    public void onFinishInflate() {
        updateRotatedViews();
        mNavigationBarInflaterView = ((NavigationBarInflaterView) findViewById(R.id.navigation_inflater));
        ((NavigationBarInflaterView) findViewById(R.id.navigation_inflater)).setButtonDispatchers(
                mButtonDisatchers);

        mStrRotatedViews[0] = mContext.getString(R.string.config_navBarLayout);
        mStrRotatedViews[1] = mContext.getString(R.string.config_navBarLayout_1);
        mStrRotatedViews[2] = mContext.getString(R.string.config_navBarLayout_2);
        mStrRotatedViews[3] = mContext.getString(R.string.config_navBarLayout_3);
        
        LogHelper.sd(TAG,"NavigationBar str0="+mStrRotatedViews[0]+
        		" str1="+mStrRotatedViews[1]+
        		" str2="+mStrRotatedViews[2]+
        		" str3="+mStrRotatedViews[3]);
        
        getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);

        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new Stub() {
                @Override
                public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
                }

                @Override
                public void onDockedStackExistsChanged(final boolean exists) throws RemoteException {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mDockedStackExists = exists;
                            updateRecentsIcon();
                        }
                    });
                }

                @Override
                public void onDockedStackMinimizedChanged(boolean minimized, long animDuration)
                        throws RemoteException {
                }

                @Override
                public void onAdjustedForImeChanged(boolean adjustedForIme, long animDuration)
                        throws RemoteException {
                }

                @Override
                public void onDockSideChanged(int newDockSide) throws RemoteException {
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Failed registering docked stack exists listener", e);
        }
        
        Handler handler = new Handler();
    	NavigationBarObserver = new NavigationBarSettingObserver(handler);
    	NavigationBarObserver.startObserving();        
    	setNavigationBarColor();
    	getHideButton().setOnClickListener(mHideClickListener);
    	getHideButton().setVisibility(View.INVISIBLE);
    }

    private void setNavigationBarColor() {
		
	}

	void updateRotatedViews() {
        mRotatedViews[Surface.ROTATION_0] =
                mRotatedViews[Surface.ROTATION_180] = findViewById(R.id.rot0);
        mRotatedViews[Surface.ROTATION_270] =
                mRotatedViews[Surface.ROTATION_90] = findViewById(R.id.rot90);

//        for(int type =0 ; type < 4; type++){    	
////	        mRotatedTypeViews[Surface.ROTATION_0][type] =
////	        mRotatedTypeViews[Surface.ROTATION_180][type] = findViewById(R.id.rot0).findViewById(typeId[type]);
//	        mRotatedViews[Surface.ROTATION_0] =
//	        mRotatedViews[Surface.ROTATION_180]	= findViewById(R.id.rot0);	
//	        
////	        mRotatedTypeViews[Surface.ROTATION_90][type] = findViewById(R.id.rot90).findViewById(typeId[type]);		        
////	        mRotatedTypeViews[Surface.ROTATION_270][type] = mRotatedTypeViews[Surface.ROTATION_90][type];
//	        mRotatedViews[Surface.ROTATION_90] =
//	        mRotatedViews[Surface.ROTATION_270]	= findViewById(R.id.rot90);	
//    	}
        
        updateCurrentView();
    }

    private void updateCurrentView() {
        final int rot = mDisplay.getRotation();
        for (int i=0; i<4; i++) {
            mRotatedViews[i].setVisibility(View.GONE);
        }
        mCurrentView = mRotatedViews[rot];
        //mCurrentView = mRotatedTypeViews[Surface.ROTATION_0][mNavigationType];
        mCurrentView.setVisibility(View.VISIBLE);
        for (int i = 0; i < mButtonDisatchers.size(); i++) {
            mButtonDisatchers.valueAt(i).setCurrentView(mCurrentView);
        }
        updateLayoutTransitionsEnabled();
    }

    private void updateRecentsIcon() {
        getRecentsButton().setImageDrawable(mDockedStackExists ? mDockedIcon : mRecentIcon);
    }

//    public boolean isVertical() {
//        return mVertical;
//    }

    private void typeNavigation(){
   	 final int settingType = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_COMBINATION, 4);

        int combileType = 0;
        switch(settingType){
        case 1:
        	combileType = 2;
        	break;
        case 2:
        	combileType = 1;
        	break;
        case 3:
        	combileType = 0;
        	break;
        case 4:
        	combileType = 3;
        	break;
        default:
        	combileType = 3;
        }
        if(combileType < 0 || combileType > 3)
        	combileType = 0;
        mNavigationType = combileType;
   }

       public boolean isVertical() {
           return mVertical;
       }

   	private class NavigationBarSettingObserver extends ContentObserver {

   		public NavigationBarSettingObserver(Handler handler) {
   			super(handler);
   			// TODO Auto-generated constructor stub
   		}
   		
   		public void startObserving() {
   			CurrentUserTracker.registerContentObserver(NAVIGATIONBAR_FIXED, true, this);
   			
   			CurrentUserTracker.registerContentObserver(NAVIGATIONBAR_COMBINATION, true, this);
   			
   			CurrentUserTracker.registerContentObserver(NAVIGATIONBAR_BACKGROUND, true, this);
   			
   			if(LogHelper.NOLOGGING)LogHelper.sd(TAG,"...................NAVIGATIONBAR_COMBINATION is startObserving()");
   			update();		
   			reorient();
   		}
   		

   		@Override
   		public void onChange(boolean selfChange) {
   			update();
   			reorient();
   			setNavigationBarColor();
   			super.onChange(selfChange);
   		}

   	}
   	
   	
   	private void update(){
   		if(mPhoneStatusBar != null){
   			mPhoneStatusBar.repositionNavigationBar();
   		}
   		 int isfixed = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_FIXED, 0);
   		 isForceShown = (isfixed == 1) ? true : false;
   		 LogHelper.sv(TAG,NAVIGATIONBAR_FIXED + " = " + isfixed + " isForceShown = " + isForceShown);
   		 //getHideButton().setVisibility(isForceShown ? View.INVISIBLE : View.VISIBLE);//===modify by ty
   		 getMenuButton().setVisibility(View.VISIBLE);
   	}
   	
   	  private View.OnClickListener mHideClickListener = new View.OnClickListener() {
   	        public void onClick(View v) {
   	            LogHelper.sv(TAG, "mHideClickListener hide navigationbar");
   	            //mNavigationBarView.setVisibility(View.GONE);
   	            quide = CurrentUserTracker.getIntForCurrentUser("hideNavigationBar.quide", 0);
   	           //fangzhengru
   	            Intent localIntent = new Intent("com.yulong.navigationbar.statuschange");
   	            localIntent.putExtra("hideNavigationBar", false);
   	            mContext.sendBroadcast(localIntent);
   	            
   	            if( quide== 0){
   	            	if(mIsOverSea){
   	            		Toast.makeText(mContext, mContext.getResources().getString(R.string.over_sea_hideNavigationBar_info), Toast.LENGTH_SHORT).show();
   	            	}else{
   	            	  startActivityForQuide();
   	            	}
   	            	CurrentUserTracker.putIntForCurrentUser("hideNavigationBar.quide",1);
   	            }
   	        }
   	    };
   	    
   	    private void startActivityForQuide(){
//   	    	Intent newintent = new Intent();
//   			newintent.setClass(mContext, ShowNavigationHideInfoActivity.class);
//   			newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//   			mContext.startActivityAsUser(newintent,UserHandle.CURRENT);
   	    }
   	    
    public void reorient() {
        final int settingType = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_COMBINATION, 4);
        //settingType
        //combileType 
		LogHelper.sd(TAG,"NavigationBar settingType="+settingType);
        int combileType = 0;
        switch(settingType){
        case 1:
        	combileType = 2;
        	break;
        case 2:
        	combileType = 1;
        	break;
        case 3:
        	combileType = 0;
        	break;
        case 4:
        	combileType = 3;
        	break;
        default:
        	combileType = 3;
        }
        if(combileType < 0 || combileType > 3)
        	combileType = 0;
        mNavigationType = combileType;
        
        try {
        	LogHelper.sd(TAG,"NavigationBar mNavigationType="+mNavigationType);
        	mNavigationBarInflaterView.resetLayout(mStrRotatedViews[mNavigationType]);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        updateCurrentView();

        getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);

        mDeadZone = (DeadZone) mCurrentView.findViewById(R.id.deadzone);

        // force the low profile & disabled states into compliance
        mBarTransitions.init();
        setDisabledFlags(mDisabledFlags, true /* force */);
        setMenuVisibility(mShowMenu, true /* force */);

        if (DEBUG) {
            Log.d(TAG, "reorient(): rot=" + mDisplay.getRotation());
        }

        updateTaskSwitchHelper();
        setNavigationIconHints(mNavigationIconHints, true);
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        mGestureHelper.setBarState(mVertical, isRtl);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));

        final boolean newVertical = w > 0 && h > w;
        if (newVertical != mVertical) {
            mVertical = newVertical;
            //Log.v(TAG, String.format("onSizeChanged: h=%d, w=%d, vert=%s", h, w, mVertical?"y":"n"));
            reorient();
            notifyVerticalChangedListener(newVertical);
        }

        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (mOnVerticalChangedListener != null) {
            mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean uiCarModeChanged = updateCarMode(newConfig);
        updateTaskSwitchHelper();
        updateIcons(getContext(), mConfiguration, newConfig);
        updateRecentsIcon();
        if (uiCarModeChanged || mConfiguration.densityDpi != newConfig.densityDpi) {
            // If car mode or density changes, we need to reset the icons.
            setNavigationIconHints(mNavigationIconHints, true);
        }
        mConfiguration.updateFrom(newConfig);
    }

    /**
     * If the configuration changed, update the carmode and return that it was updated.
     */
    private boolean updateCarMode(Configuration newConfig) {
        boolean uiCarModeChanged = false;
        if (newConfig != null) {
            int uiMode = newConfig.uiMode & Configuration.UI_MODE_TYPE_MASK;
            if (mCarMode && uiMode != Configuration.UI_MODE_TYPE_CAR) {
                mCarMode = false;
                uiCarModeChanged = true;
            } else if (uiMode == Configuration.UI_MODE_TYPE_CAR) {
                mCarMode = true;
                uiCarModeChanged = true;
            }
        }
        return uiCarModeChanged;
    }

    /*
    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onLayout: %s (%d,%d,%d,%d)",
                    changed?"changed":"notchanged", left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);
    }

    // uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT situations: if all else
    // fails, any touch on the display will fix the layout.
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: " + ev.toString());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            postCheckForInvalidLayout("touch");
        }
        return super.onInterceptTouchEvent(ev);
    }
    */


    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = getContext().getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        final Rect r = new Rect();
        final Point size = new Point();
        mDisplay.getRealSize(size);

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                        + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: "
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                        getResourceName(getCurrentView().getId()),
                        getCurrentView().getWidth(), getCurrentView().getHeight(),
                        visibilityToString(getCurrentView().getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());

        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, ButtonDispatcher button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(visibilityToString(button.getVisibility())
                    + " alpha=" + button.getAlpha()
                    );
        }
        pw.println();
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean isVertical);
    }

}