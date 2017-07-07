package com.android.covermode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;
import com.yulong.android.feature.FeatureConfig;
import com.yulong.android.feature.FeatureString;

/**
 * 
 * @author liaozhidan
 * 
 */
public class HolsterFixableView extends FrameLayout {

	private final static String TAG = "HolsterFixableView";
	private float mDensity = 0;
	public static int mVersion = 0;
	public static float mTopLeftX = 0;
	public static float mTopLeftY = 0;
	public static float mSubViewWidth = 0;
	public static float mSubViewHeight = 0;

	private StarryHostView mStarryHostView;
	private StarrySimpleHostView mSimpleHostView;
	private CircleView mCircleView;
	private StarryChargeView mChargeView;
	private StarryShortcutView mShortcutView;
	private NewNotificationLayout mNewNotificationView;
	private ViewPager mPager;
	private List<View> mListViews;
	public static Boolean mStarryDismissed = false;
	public ArrayList<StatusBarNotification> mNotificationsList;

	public HolsterFixableView(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public HolsterFixableView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	/**
	 * 
	 */
	public void init() {
		mDensity = getResources().getDisplayMetrics().density;

		String attributeName = getAttributeName("COVER_MODE_VERSION");
		if (attributeName != null) {
			try {
				mVersion = FeatureConfig.getIntValue(attributeName);
			} catch (Exception e) {
				Log.d(TAG, "get cover mode version error : " + e.toString());
			}
		}
		if (!versionValid()) {
			return;
		}
		attributeName = getAttributeName("HOLSTER_X_POSITION");
		if (attributeName != null) {
			mTopLeftX = FeatureConfig.getIntValue(attributeName);
		}
		attributeName = getAttributeName("HOLSTER_Y_POSITION");
		if (attributeName != null) {
			mTopLeftY = FeatureConfig.getIntValue(attributeName) + 25 /* statusbar */;
		}
		attributeName = getAttributeName("HOLSTER_WIDTH");
		if (attributeName != null) {
			mSubViewWidth = FeatureConfig.getIntValue(attributeName);
		}
		attributeName = getAttributeName("HOLSTER_HEIGHT");
		if (attributeName != null) {
			mSubViewHeight = FeatureConfig.getIntValue(attributeName) - 25 /* statusbar */;
		}

		int color = mContext.getResources().getColor(android.R.color.black);
		this.setBackgroundColor(color);

		Log.d(TAG, "before---mTopLeftX = " + mTopLeftX + ", mTopLeftY = " + mTopLeftY + ", mSubViewWidth = " + mSubViewWidth + ", mSubViewHeight = "
				+ mSubViewHeight + ", mVersion = " + mVersion);

		mTopLeftX = (mTopLeftX > 0 ? mTopLeftX : 35) * mDensity;
		mTopLeftY = (mTopLeftY > 0 ? mTopLeftY : 50) * mDensity;
		mSubViewWidth = (mSubViewWidth > 0 ? mSubViewWidth : 300) * mDensity;
		mSubViewHeight = getFixedViewHeight(mSubViewHeight);

		Log.d(TAG, "after---mTopLeftX = " + mTopLeftX + " mTopLeftY = " + mTopLeftY + " mSubViewWidth = " + mSubViewWidth + " mSubViewHeight = "
				+ mSubViewHeight);
	}

	private boolean versionValid() {
		return mVersion == 1 || mVersion == 2;
	}
	
	KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onFinishedGoingToSleep(int why) {
			if (!versionValid()) {
				return;
			}
			if (getVisibility() == View.VISIBLE) {
				resetViews();
			}
		};

		@Override
		public void onStartedWakingUp() {
			if (!versionValid()) {
				return;
			}
			// add to unlock coolshow's lock
			if (getVisibility() == View.VISIBLE) {
				Intent unlock = new Intent("com.ibimuyu.lockscreen.unlock");
				mContext.sendBroadcast(unlock);
			}
		}

		@Override
		public void onFingerprintAuthenticated(int userId) {
			if (!versionValid()) {
				return;
			}
			if (getVisibility() != View.VISIBLE) {
				return;
			}
			HolsterFixableView.mStarryDismissed = true;
			if (mChargeView == null) {
				return;
			}
			mChargeView.notice(getResources().getString(R.string.fingerprint_recognized_hint));
		}

		@Override
		public void onFingerprintError(int msgId, String errString) {
			if (!versionValid()) {
				return;
			}
			if (getVisibility() != View.VISIBLE) {
				return;
			}
			if (mChargeView == null) {
				return;
			}
			mChargeView.notice(getResources().getString(R.string.fingerprint_error_hint));
		}
		
		@Override
		public void onUserSwitching(int userId) {
			mChargeView.notice(getResources().getString(R.string.user_switching_hint));
		}

		@Override
		public void onUserSwitchComplete(int userId) {
			UserInfo userInfo = null;
			try {
				userInfo = ActivityManagerNative.getDefault().getCurrentUser();
			} catch (RemoteException e) {
				Log.d(TAG, "ActivityManagerNative.getDefault().getCurrentUser() error: " + e.toString());
			}
			String hint;
			if (userInfo != null && userInfo.isPrimary()) {
				hint = getResources().getString(
						R.string.user_switched_to_primary_hint);
			} else {
				hint = getResources().getString(
						R.string.user_switched_to_home_hint);
			}
			mChargeView.notice(hint);
		};
	};

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!versionValid()) {
			return;
		}
		KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUpdateCallback);
	}

	int lastIndex = -1;

	private void setViewBg() {
		if (mVersion != 1) {
			return;
		}
		int index = Settings.System.getInt(mContext.getContentResolver(), "view_window_bgcolor_index", 0);
		Log.d(TAG, "setCurrentWindowBg index = " + index);
		if (index == lastIndex) {
			return;
		}
		switch (index) {
		case 0:// no background
			setBackgroundColor(Color.BLACK);
			break;
		case 1:// gray
			setBackgroundResource(R.drawable.yl_bg_gray);
			break;
		case 2:// green
			setBackgroundResource(R.drawable.yl_bg_green);
			break;
		case 3:// purple
			setBackgroundResource(R.drawable.yl_bg_purple);
			break;
		default:
			setBackgroundColor(Color.BLACK);
			break;
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (!versionValid()) {
			return;
		}
		KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mUpdateCallback);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		Log.v(TAG, "onFinishInflate() mVersion = " + mVersion);
		mPager = (ViewPager) findViewById(R.id.vPager);
		switch (mVersion) {
		case 0:
			// holster doesn't have transparent windows, needn't inflate views
			break;
		case 1:
			// version 1, bigger transparent window. For example, 9971,8676 etc.
			if (mNotificationsList == null) {
				mNotificationsList = new ArrayList<StatusBarNotification>();
			}
			if (mListViews == null) {
				mListViews = new ArrayList<View>();
			}
			mStarryHostView = (StarryHostView) View.inflate(mContext, R.layout.starry_host_view, null);
			mChargeView = (StarryChargeView) mStarryHostView.findViewById(R.id.starry_charge_view);
			mNewNotificationView = (NewNotificationLayout) View.inflate(mContext, R.layout.new_notification_layout, null);
			mNewNotificationView.setHolsterView(this);
			mCircleView = (CircleView) mStarryHostView.findViewById(R.id.time_circle_view);
			mShortcutView = (StarryShortcutView) mStarryHostView.findViewById(R.id.starry_shortcut_view);
			mListViews.add(mNewNotificationView);
			mListViews.add(mStarryHostView);
			mPager.setAdapter(new MyPagerAdapter(mListViews));
			Uri uri = Settings.System.getUriFor("view_window_bgcolor_index");
			mContext.getContentResolver().registerContentObserver(uri, true, mWinbgObserver);
			break;
		case 2:
			// version 2, smaller window.
			if (mListViews == null) {
				mListViews = new ArrayList<View>();
			}
			mSimpleHostView = (StarrySimpleHostView) View.inflate(mContext, R.layout.starry_simple_host_view, null);
			mChargeView = (StarryChargeView) mSimpleHostView.findViewById(R.id.starry_charge_view);
			mListViews.add(mSimpleHostView);
			mPager.setAdapter(new MyPagerAdapter(mListViews));
			break;
		default:
			break;
		}
		resetViews();
		setViewBg();
	}

	private ContentObserver mWinbgObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			setViewBg();
			if (mVersion == 1) {
				mCircleView.updateBg();
				mShortcutView.updateBg();
			}
		}
	};

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		if (visibility == View.GONE) {
			resetViews();
		}
		super.onVisibilityChanged(changedView, visibility);
	}

	public void resetViews() {
		if (mVersion == 1) {
			mStarryHostView.onRestart();
			mPager.setCurrentItem(1);
		}
		if (mVersion == 2 && !KeyguardUpdateMonitor.getInstance(mContext).isOccluded()) {
			HolsterFixableView.mStarryDismissed = false;
		}
	}

	public class MyPagerAdapter extends PagerAdapter {
		public List<View> listViews;

		public MyPagerAdapter(List<View> mListViews) {
			listViews = mListViews;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(listViews.get(arg1));
		}

		@Override
		public int getCount() {
			return listViews.size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(listViews.get(arg1), 0);
			return listViews.get(arg1);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == (arg1);
		}
	}

	private String getAttributeName(String name) {
		try {
			Field field = FeatureString.class.getDeclaredField(name);
			String attributeName = (String) field.get(null);
			return attributeName;
		} catch (Exception e) {
			Log.v(TAG, "get " + name + " field error  e: " + e);
		}
		return null;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!versionValid()) {
			return;
		}
		int cellWidthSpec = MeasureSpec.makeMeasureSpec((int) mSubViewWidth, MeasureSpec.EXACTLY);
		int cellHeightSpec = MeasureSpec.makeMeasureSpec((int) mSubViewHeight, MeasureSpec.EXACTLY);

		int count = getChildCount();
		for (int index = 0; index < count; index++) {
			final View child = getChildAt(index);
			child.measure(cellWidthSpec, cellHeightSpec);
		}

		setMeasuredDimension(resolveSize(cellWidthSpec, widthMeasureSpec), resolveSize(cellHeightSpec, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!versionValid()) {
			return;
		}
		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(i);

			int w = view.getMeasuredWidth();
			int h = view.getMeasuredHeight();

			l = (int) (l + mTopLeftX);
			t = (int) (t + mTopLeftY);
			r = l + w;
			b = t + h;
			view.layout(l, t, r, b);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		if (!versionValid()) {
			return false;
		}
		return true;
	}

	private float getFixedViewHeight(float configHeight) {
		float height = 0;
		height = (configHeight > 0 ? configHeight : 394) * mDensity;

		if (isElderModel()) {
			height -= getDValueFromStatusBar();
		}

		return height;
	}

	private int getDValueFromStatusBar() {
		int dValue = 0;
		int statuBarHeight = 0;
		int elderStatuBarHeight = 0;
		Resources res = mContext.getResources();
		int statusbarId = getAndroidInternalResId("dimen", "status_bar_height");
		int elderstatusbarId = getAndroidInternalResId("dimen", "status_bar_elder_height");

		if (statusbarId == 0 || elderstatusbarId == 0) {
			statuBarHeight = 0;
			elderStatuBarHeight = 0;
		} else {
			try {
				statuBarHeight = res.getDimensionPixelSize(statusbarId);
				elderStatuBarHeight = res.getDimensionPixelSize(elderstatusbarId);
			} catch (Exception e) {
				statuBarHeight = 0;
				elderStatuBarHeight = 0;
			}
		}

		Log.d(TAG, "statuBarHeight = " + statuBarHeight + " elderStatuBarHeight = " + elderStatuBarHeight);

		dValue = elderStatuBarHeight - statuBarHeight;
		dValue = dValue > 0 ? dValue : 0;
		return dValue;
	}

	public void addHolsterView(View HolsterView) {
		this.removeAllViews();
		this.addView(HolsterView);
	}

	public void setHolsterParantViewBackground(int resId) {
		super.setBackgroundResource(resId);
	}

	private boolean isElderModel() {
		Class<?> isystemInterface = null;
		Method getCurrentModel = null;
		Method asInterface = null;
		int result = 0;

		IBinder iBinder = ServiceManager.getService("coolpadSystem");

		try {
			isystemInterface = Class.forName("com.yulong.android.server.systeminterface.ISystemInterface$Stub");
			asInterface = isystemInterface.getMethod("asInterface", IBinder.class);
			getCurrentModel = isystemInterface.getMethod("getCurrentModel");
			result = (Integer) getCurrentModel.invoke(asInterface.invoke(isystemInterface, iBinder));
		} catch (Exception e) {
			Log.d(TAG, "get current model error: " + e.toString());
		}

		Log.d(TAG, "result = " + result);

		return result == 9;
	}

	public int getAndroidInternalResId(String type, String name) {
		try {
			Class<?> localClass = Class.forName("com.android.internal.R$" + type);
			int resId = Integer.parseInt(localClass.getField(name).get(localClass).toString());
			return resId;
		} catch (Exception e) {
			Log.d(TAG, "get internal res id error : " + e.toString());
		}
		return 0;
	}

	public void addCoverNotification(StatusBarNotification notification) {
		if (mVersion != 1) {
			return;
		}
		Log.v(TAG, "addCoverNotification notification = " + notification);
		if (notification == null) {
			return;
		}
		Bundle extraBundle = notification.getNotification().extras;
		if (extraBundle == null) {
			Log.d(TAG, "NotifyMsg extraBundle is null");
			return;
		}
		String title = extraBundle.getString(Notification.EXTRA_TITLE, null);
		if (title == null || title.equals("")) {
			Log.d(TAG, "NotifyMsg doesn't have title");
			return;
		}
		if (mNotificationsList == null) {
			mNotificationsList = new ArrayList<StatusBarNotification>();
		}
		for (StatusBarNotification bean : mNotificationsList) {
			if (bean.getKey().equals(notification.getKey())) {
				Log.d(TAG, "duplicate msg");
				return;
			}
		}
		mNotificationsList.add(notification);
	}

	public void removeCoverNotification(String key) {
		if (mVersion != 1) {
			return;
		}
		Log.v(TAG, "removeCoverNotification key = " + key);
		for (StatusBarNotification bean : mNotificationsList) {
			if (bean.getKey().equals(key)) {
				mNotificationsList.remove(bean);
				break;
			}
		}
	}

	public void resetNotificationViews() {
		if (mVersion == 1 && mNewNotificationView != null) {
			mNewNotificationView.resetViews();
		}
	}
}
