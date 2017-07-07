package com.android.systemui.quicksettings.bottom;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.app.ActivityManagerInternal.SleepToken;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
//import android.graphics.BlurParams;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.IvviGaussBlurViewFeature;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.quicksettings.bottom.BottomIconEditActivity.CheckablePackageInfo;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.QuickSettingsData;
import com.android.systemui.statusbar.phone.QuickSettingsItemView;
import com.android.systemui.statusbar.phone.QuickSettingsModel;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
import com.android.systemui.statusbar.phone.YulongQuickSettings;
import com.android.systemui.statusbar.phone.YulongQuickSettingsContain;

//import com.android.systemui.gaussianblur.GaussianBlur;

public class QuickSettingsPannelView extends LinearLayout implements QuickSettingsModel.IUpdateView {
	private static final String TAG = "QuickSettingsPannelView";

	private static final String ACTION_FLASHLIGHT_ON_FLAG = "com.android.intent.action.FlashLight_On_Flag";
	private static final String ACTION_FLASHLIGHT_CLOSE_FLAG = "com.android.intent.action.FlashLight_Close_Flag";
	private QuickSettingsContainerViewPager mQuickSettingPager;
	private PanelPagerAdapter mQuickSettingPagerAdapter;
	private ArrayList<View> mQuickSettingPagerDots = new ArrayList<View>();
	private ArrayList<View> mQuickSettingViewList = new ArrayList<View>();

	private int mQuickSetingPagerOldPosition = 0;//
	private PanelPagerAdapter mBottomIconPagerAdapter;
	private ArrayList<View> mBottmIconViewList = new ArrayList<View>();
	private ImageView mImage;
	public LinearLayout mFloatMenuLayout;
	private TextView mIconText1, mIconText2, mIconText3, mIconText4;
	private BrightnessController mBrightnessController;
	YulongQuickSettings mYLQuickSettings;
	private QSBottomPanel mQSBottomPanel;

	private int mCurrentOrientation = Configuration.ORIENTATION_PORTRAIT;

	public static boolean initFlag = false;

	View mQuickSettingPage1;
	View mQuickSettingPage2;

	PhoneStatusBar mStatusBar;
	ArrayList<SimInfo> pSiminfos = null;

	private ViewPager mBottomIconPager;

	private QuickSettingIconView mFlashIcon;

	private LinearLayout.LayoutParams mDotLayoutParams;

	private LinearLayout mBottomPagerDotsContainer;

	protected int mBottomIconPagerOldPosition;

	private PackageManager mPackageManager;

	private CompoundButton mButtonBrightAuto;

	private ToggleSlider mBrightSlider;

	private ImageView mBrightImage;

	private static final int MSG_UPDATE_UI = 1000;
	
	private Boolean mShowWeiXinAliPaySaoYiSao;
	private String hyFlagfinal = "hy";
	private boolean mIsRegistered = false;
	private boolean mShowWeiXinAliPayFuKuan;

	public QuickSettingsPannelView(Context context, PhoneStatusBar StatusBar, int Oriention, QSBottomPanel qsBottomPanel) {
		super(context);
		mContext = context;
		mStatusBar = StatusBar;
		mCurrentOrientation = Oriention;
		mQSBottomPanel = qsBottomPanel;
		mPackageManager = mContext.getPackageManager();
		if(SystemProperties.get("ro.yulong.project").equals(hyFlagfinal)){
			mShowWeiXinAliPaySaoYiSao = false;
			mShowWeiXinAliPayFuKuan = false;
		}else{
			if(isWeiXinAliPayEnable("IS_SUPPORT_WECHAT_SCAN")){
				mShowWeiXinAliPaySaoYiSao = true;
			}else{
				mShowWeiXinAliPaySaoYiSao = false;
			}
			if(isWeiXinAliPayEnable("IS_SUPPORT_WECHAT_RECEIVABLES")){
				mShowWeiXinAliPayFuKuan = true;
			}else{
				mShowWeiXinAliPayFuKuan = false;
			}
		}
		int dotWidth = Utilities.dipToPixel(mContext, 6);
		mDotLayoutParams = new LinearLayout.LayoutParams(dotWidth, dotWidth);
		int dotMargin = Utilities.dipToPixel(mContext, 3);
		mDotLayoutParams.setMarginStart(dotMargin);
		mDotLayoutParams.setMarginEnd(dotMargin);

		loadCustomIcons();

		layoutInflate();

		int height = 0;
		if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			height = (int) context.getResources().getDimension(R.dimen.new_systemui_height);
		} else {
			height = (int) context.getResources().getDimension(R.dimen.new_systemui_height_new);
		}
		this.setLayoutParams(new LayoutParams(-1, height));
		
		if(Utilities.showFullGaussBlurForDDQS()){
//			setBlurRadiusDp(12f);//===modify by ty
//			setBlurChromaContrast(1.5f);
//			setBlurAlpha(0.8f);
//			setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//			setBackground(null);
			
			IvviGaussBlurViewFeature.setBlurRadiusDp(this, 12f);
			IvviGaussBlurViewFeature.setBlurChromaContrast(this, 1.5f);
			IvviGaussBlurViewFeature.setBlurAlpha(this, 0.8f);
			IvviGaussBlurViewFeature.setBlurMode(this, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
			IvviGaussBlurViewFeature.setBackground(this, null);
		}
	}

	private boolean isWeiXinAliPayEnable(String string) {
		boolean isEnable = false;
		try {
			String featureString = "";
			Class<?> featureClass = Class.forName("com.yulong.android.feature.FeatureString");
			Log.d(TAG, "featureClass is getted");
			Object featureObject = featureClass.newInstance();
			Field featureField = featureClass.getDeclaredField(string);
			featureField.setAccessible(true);
			featureString = featureField.get(featureObject).toString();
			Class<?> ConfigClass = Class.forName("com.yulong.android.feature.FeatureConfig");
			Method method = ConfigClass.getMethod("getBooleanValue", String.class);
			Object configoObject = ConfigClass.newInstance();
			isEnable = (Boolean) method.invoke(configoObject, featureString);
			Log.d(TAG, string + " is " + isEnable);
		} catch (Exception e) {
			Log.v(TAG, "cann't find field " + string +" : " + e);
		}
		return isEnable;
	}

	private void loadCustomIcons() {
		SharedPreferences sharedPreferences = mContext.getSharedPreferences("bottom_icon_config", Activity.MODE_PRIVATE);
		for (int i = 0; i < mCustomIcon.length; i++) {
			mCustomIcon[i] = sharedPreferences.getString("icon" + i, "");
		}
	}

	protected void layoutInflate() {
		createSystemUIView();
		initImage();
		initQuickSettingViewPager();
		initBottomIconViewPager();
		initBrighnessDialog();
		mUIHandler = new UIHandler();

		if (initFlag) {
			YulongQuickSettingsContain.initInstance();
		}

		mYLQuickSettings = YulongQuickSettingsContain.getInstance(mContext, Utilities.isPrimaryUser());
		mYLQuickSettings.AddUpdateViewCallback(this);
		if (mStatusBar != null) {
			mYLQuickSettings.setStatusBar(mStatusBar);
		}

		// broadcasts
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		filter.addAction(ACTION_FLASHLIGHT_ON_FLAG);
		filter.addAction(ACTION_FLASHLIGHT_CLOSE_FLAG);
		mContext.registerReceiver(mBroadcast, filter);
		mIsRegistered = true;
	}

	private void createSystemUIView() {
		if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			mFloatMenuLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.quick_setting_panel_new, this);
		} else {
			mFloatMenuLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.quick_setting_panel_new_rotator,
					this);
		}
		
		View v1 = mFloatMenuLayout.findViewById(R.id.image_icon);
		View v2 = mFloatMenuLayout.findViewById(R.id.vPager);
		View v3 = mFloatMenuLayout.findViewById(R.id.qs_brightness);
		if(Utilities.showFullGaussBlurForDDQS()){
			mFloatMenuLayout.setBackground(null);
			mFloatMenuLayout.setBackgroundColor(Color.parseColor("#818A99"));
			v1.setBackground(null);
			v2.setBackground(null);
			if(v3 != null){
				v3.setBackground(null);
			}
		}else{
			mFloatMenuLayout.setBackground(null);
			v1.setBackgroundColor(Color.parseColor("#818A99"));
			v2.setBackgroundColor(Color.parseColor("#818A99"));
			if(v3 != null){
				v3.setBackgroundColor(Color.parseColor("#707682"));
			}
		}
		
		mBottomPagerDotsContainer = (LinearLayout) mFloatMenuLayout.findViewById(R.id.icon_dots_container);
	}

	private void initImage() {
		mImage = (ImageView) mFloatMenuLayout.findViewById(R.id.image_icon);

		mImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mQSBottomPanel.mOnQuickSettingsPannelView.setVisibility(false);
			}
		});
	}

	private void initQuickSettingViewPager() {
		mQuickSettingPager = (QuickSettingsContainerViewPager) mFloatMenuLayout.findViewById(R.id.vPager);
		LayoutInflater mInflater = LayoutInflater.from(mContext);

		if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			mQuickSettingPage1 = mInflater.inflate(R.layout.quick_setting_page1, null);
			mQuickSettingPage2 = mInflater.inflate(R.layout.quick_setting_page2, null);
		} else {
			mQuickSettingPage1 = mInflater.inflate(R.layout.quick_setting_new_page1, null);
			mQuickSettingPage2 = mInflater.inflate(R.layout.quick_setting_new_page2, null);
		}
		View dot1 = (View) mFloatMenuLayout.findViewById(R.id.dot_1);
		View dot2 = (View) mFloatMenuLayout.findViewById(R.id.dot_2);

		mQuickSettingPagerDots.add(dot1);
		mQuickSettingPagerDots.add(dot2);

		mQuickSettingViewList.add(mQuickSettingPage1);
		
		if (Utilities.isPrimaryUser()) {
			mQuickSettingViewList.add(mQuickSettingPage2);
		} else {
			for (int i = 0; i < mQuickSettingPagerDots.size(); i++) {
				mQuickSettingPagerDots.get(i).setVisibility(INVISIBLE);
			}
		}
		
		mQuickSettingPagerAdapter = new PanelPagerAdapter(mQuickSettingViewList);

		mQuickSettingPager.setAdapter(mQuickSettingPagerAdapter);

		mQuickSettingPager.measure(0, 0);
		mQuickSettingPager.setCurrentItem(0);
		mQuickSettingPagerDots.get(0).setBackgroundResource(R.drawable.dot_focused);
		mQuickSettingPagerDots.get(1).setBackgroundResource(R.drawable.dot_normal);

		mQuickSettingPager.addOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				mQuickSettingPagerDots.get(mQuickSetingPagerOldPosition).setBackgroundResource(R.drawable.dot_normal);
				mQuickSettingPagerDots.get(position).setBackgroundResource(R.drawable.dot_focused);
				mQuickSetingPagerOldPosition = position;
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}

	private void initBrighnessDialog() {
		mBrightnessController = new BrightnessController(mContext, mBrightImage, mBrightSlider, mButtonBrightAuto);
		mBrightnessController.registerCallbacks();
	}

	private void initBottomIconViewPager() {
		mBottomIconPager = (ViewPager) mFloatMenuLayout.findViewById(R.id.iconPager);
		View page1 = View.inflate(mContext,
				mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT ? R.layout.quick_startapp_containerview
						: R.layout.quick_startapp_containerview_new, null);
		loadPage(page1, 0, false);
		mBottmIconViewList.add(page1);
		View mBrightness;
		if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			mButtonBrightAuto = (CompoundButton) findViewById(R.id.brightAuto);
			mBrightSlider = (ToggleSlider) mFloatMenuLayout.findViewById(R.id.brightness_slider);
			mBrightImage = (ImageView) mFloatMenuLayout.findViewById(R.id.brightness_icon);
			mBrightness = mFloatMenuLayout.findViewById(R.id.brightness_container);
		} else {
			mButtonBrightAuto = (CompoundButton) page1.findViewById(R.id.brightAuto);
			mBrightSlider = (ToggleSlider) page1.findViewById(R.id.brightness_slider);
			mBrightImage = (ImageView) page1.findViewById(R.id.brightness_icon);
			mBrightness = mFloatMenuLayout.findViewById(R.id.brightness_container);
		}
		if(Utilities.showFullGaussBlurForDDQS()){
			if(mBrightness != null){
				mBrightness.setBackground(null);
			}
		}

		if (Utilities.isPrimaryUser()) {
			ViewGroup page2 = (ViewGroup) View.inflate(mContext,
					mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT ? R.layout.quick_startapp_containerview
							: R.layout.quick_startapp_containerview_new, null);
			loadPage(page2, 1, true);
			mBottmIconViewList.add(page2);
			if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				page2.removeView(page2.findViewById(R.id.brightness_container));
			}			
		}

		mBottomIconPagerAdapter = new PanelPagerAdapter(mBottmIconViewList);
		mBottomIconPager.setAdapter(mBottomIconPagerAdapter);

		if (Utilities.isPrimaryUser()) {
			View dot = new View(mContext);
			dot.setLayoutParams(mDotLayoutParams);
			dot.setBackgroundResource(R.drawable.dot_focused);
			mBottomPagerDotsContainer.addView(dot);

			dot = new View(mContext);
			dot.setLayoutParams(mDotLayoutParams);
			dot.setBackgroundResource(R.drawable.dot_normal);
			mBottomPagerDotsContainer.addView(dot);
		}

		mBottomIconPager.addOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int position) {
				if (Utilities.isPrimaryUser()) {
					mBottomPagerDotsContainer.getChildAt(mBottomIconPagerOldPosition)
							.setBackgroundResource(R.drawable.dot_normal);
					mBottomPagerDotsContainer.getChildAt(position).setBackgroundResource(R.drawable.dot_focused);
					mBottomIconPagerOldPosition = position;
				}
			}
		});
	}
	private final static String START_TYPE = "QuickScan_StartType";
	private final static int weixin_saoyisao   = 1;
	private final static int zhifubao_saoyisao = 2;
	
	public void updateQuickPayState(){
		if(mShowWeiXinAliPaySaoYiSao){
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					int type = Settings.System.getInt(mContext.getContentResolver(), START_TYPE, weixin_saoyisao);
					String str = getResources().getString(R.string.qs_label_weixin_pay);
					if(type != weixin_saoyisao){
						str = getResources().getString(R.string.qs_label_ali_pay);
					}
					
					QuickSettingIconView icons2 = (QuickSettingIconView) mBottmIconViewList.get(0).findViewById(R.id.L13);
					icons2.setText(str);
				}
			});
		}
	}

	private void loadPage(View page, int index, boolean delay) {
		QuickSettingIconView icons[] = new QuickSettingIconView[4];
		icons[0] = (QuickSettingIconView) page.findViewById(R.id.L11);
		icons[1] = (QuickSettingIconView) page.findViewById(R.id.L12);
		icons[2] = (QuickSettingIconView) page.findViewById(R.id.L13);
		icons[3] = (QuickSettingIconView) page.findViewById(R.id.L14);

		if (index == 0) {
			icons[0].setTargetPackage(QuickSettingIconView.PACKAGE_FLASH);
			icons[0].setImageResource(R.drawable.ic_qs_flashlight_off);
			icons[0].setText(getResources().getString(R.string.quick_settings_flashlight_label));
			icons[0].setIsCustomIcon(false);
			mFlashIcon = icons[0];

			icons[1].setTargetPackage("com.android.calculator2");
			icons[1].setImageResource(R.drawable.ic_qs_calculator_off);
			icons[1].setText(getResources().getString(R.string.qs_label_calculator));
			icons[1].setIsCustomIcon(false);

			if(mShowWeiXinAliPaySaoYiSao){
				int type = Settings.System.getInt(mContext.getContentResolver(), START_TYPE, weixin_saoyisao);
				String str = getResources().getString(R.string.qs_label_weixin_pay);
				if(type != weixin_saoyisao){
					str = getResources().getString(R.string.qs_label_ali_pay);
				}
				icons[2].setTargetPackage("com.ivvi.quickscan");
				icons[2].setImageResource(R.drawable.ic_qs_alipay);
				icons[2].setText(str);
				if(mShowWeiXinAliPayFuKuan){
					icons[3].setTargetPackage("com.ivvi.quickscan.payby");
					icons[3].setImageResource(R.drawable.ic_qs_pay_verse);
					icons[3].setText(getResources().getString(R.string.qs_label_pay_verse));
				}else{
					icons[3].setTargetPackage(QuickSettingIconView.PACKAGE_CAMERA);
					icons[3].setImageResource(R.drawable.ic_qs_camera_off);
					icons[3].setText(getResources().getString(R.string.qs_label_camera));
				}
			}else{
				if(mShowWeiXinAliPayFuKuan){
					icons[2].setTargetPackage("com.ivvi.quickscan.payby");
					icons[2].setImageResource(R.drawable.ic_qs_pay_verse);
					icons[2].setText(getResources().getString(R.string.qs_label_pay_verse));
					
					icons[3].setTargetPackage(QuickSettingIconView.PACKAGE_CAMERA);
					icons[3].setImageResource(R.drawable.ic_qs_camera_off);
					icons[3].setText(getResources().getString(R.string.qs_label_camera));
				}else{
					icons[2].setTargetPackage("com.yulong.android.soundrecorder");
					icons[2].setImageResource(R.drawable.ic_qs_blaster_off);
					icons[2].setText(getResources().getString(R.string.qs_label_blaster));
					icons[2].setIsCustomIcon(false);
					
					icons[3].setTargetPackage(QuickSettingIconView.PACKAGE_CAMERA);
					icons[3].setImageResource(R.drawable.ic_qs_camera_off);
					icons[3].setText(getResources().getString(R.string.qs_label_camera));
			}
			
			}
			return;
		}
		if (index == 1) {
			int i;
			int count = 0;
			if(mShowWeiXinAliPaySaoYiSao || mShowWeiXinAliPayFuKuan){
				if(mShowWeiXinAliPaySaoYiSao && mShowWeiXinAliPayFuKuan){
					count = 2;
				icons[0].setTargetPackage(QuickSettingIconView.PACKAGE_CAMERA);
				icons[0].setImageResource(R.drawable.ic_qs_camera_off);
				icons[0].setText(getResources().getString(R.string.qs_label_camera));
				
				icons[1].setTargetPackage("com.yulong.android.soundrecorder");
				icons[1].setImageResource(R.drawable.ic_qs_blaster_off);
				icons[1].setText(getResources().getString(R.string.qs_label_blaster));
				icons[1].setIsCustomIcon(false);
				}else{
					count = 1;
					icons[0].setTargetPackage("com.yulong.android.soundrecorder");
					icons[0].setImageResource(R.drawable.ic_qs_blaster_off);
					icons[0].setText(getResources().getString(R.string.qs_label_blaster));
					icons[0].setIsCustomIcon(false);
				}				
				for (int j = count; j < icons.length; j++) {
					icons[j].setTargetPackage("");
					icons[j].setImageBitmap(null);
					icons[j].setText(null);
				}
				
				for (i = 0; i < mCustomIcon.length-count; i++) {
					if (!loadIcon(icons[i+count], mCustomIcon[i], delay)) {
						break;
					}
				}
				i = i + count;
			}else{
				for (int j = 0; j < icons.length; j++) {
					icons[j].setTargetPackage("");
					icons[j].setImageBitmap(null);
					icons[j].setText(null);
				}
				for (i = 0; i < mCustomIcon.length; i++) {
					if (!loadIcon(icons[i], mCustomIcon[i], delay)) {
						break;
					}
				}
			}
			icons[i].setTargetPackage(mContext.getPackageName());
			icons[i].setImageResource(R.drawable.ic_bottom_icon_edit_enable);
			icons[i].setText(getResources().getString(R.string.quicksettings_edit_disable));
		}
	}

	private boolean loadIcon(final QuickSettingIconView quickSettingIconView, final String tPackage, boolean delay) {
		if (TextUtils.isEmpty(tPackage)) {
			return false;
		}
		quickSettingIconView.setTargetPackage(tPackage);
		if (delay) {
			postDelayed(new Runnable() {
				@Override
				public void run() {
					new LoadIconAsyncTask(quickSettingIconView).execute(tPackage);
				}
			}, 1000);
		} else {
			new LoadIconAsyncTask(quickSettingIconView).execute(tPackage);
		}

		quickSettingIconView.setText(getAppNameForPackage(tPackage));
		return true;
	}

	private class LoadIconAsyncTask extends AsyncTask<String, Integer, Bitmap> {

		private QuickSettingIconView mImageView;

		public LoadIconAsyncTask(QuickSettingIconView quickSettingIconView) {
			mImageView = quickSettingIconView;
		}

		@Override
		protected Bitmap doInBackground(String... packageName) {
			return BottomIconEditActivity.getIconForPackage(mContext, packageName[0]);
		}

		@Override
		protected void onPostExecute(Bitmap icon) {
			mImageView.setImageBitmap(icon);
		}
	}

	private String getAppNameForPackage(String tPackage) {
		List<PackageInfo> packageInfos = mPackageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
		for (PackageInfo packageInfo : packageInfos) {
			if (tPackage.equalsIgnoreCase(packageInfo.applicationInfo.packageName)) {
				return packageInfo.applicationInfo.loadLabel(mPackageManager).toString();
			}
		}
		return null;
	}

	protected void handleIconClick(View v) {
		if (v instanceof QuickSettingIconView) {
			((QuickSettingIconView) v).handleClick();
		}
	}

	public void setFlashLightState() {
		String isOpen = SystemProperties.get("sys.yulong.flashlight", "0");
		if (isOpen.equals("1")) {
			mFlashIcon.setImageResource(R.drawable.ic_qs_flashlight_on);
		} else {
			mFlashIcon.setImageResource(R.drawable.ic_qs_flashlight_off);
		}
	}

	QuickSettingsItemView item;
	QuickSettingsItemView itemTemp;

	@Override
	public void updateView(State state) {
		int id = state.id;
		int order = state.order;

		if (order == -1 || order > 15)
			return;
		
		Log.d(TAG, "page1.findViewById :" + order);
		switch (order) {
		case 0:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item0);
			break;
		case 1:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item1);
			break;
		case 2:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item2);
			break;
		case 3:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item3);
			break;
		case 4:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item4);
			break;
		case 5:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item5);
			break;
		case 6:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item6);
			break;
		case 7:
			item = (QuickSettingsItemView) mQuickSettingPage1.findViewById(R.id.item7);
			break;
		case 8:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item0);
			break;
		case 9:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item1);
			break;
		case 10:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item2);
			break;
		case 11:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item3);
			break;
		case 12:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item4);
			break;
		case 13:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item5);
			break;
		case 14:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item6);
			break;
		case 15:
			item = (QuickSettingsItemView) mQuickSettingPage2.findViewById(R.id.item7);
			break;
		default:
			break;
		}

		if (item != null) {
			YulongQuickSettings yq = YulongQuickSettingsContain.getInstance(mContext, Utilities.isPrimaryUser());
			yq.setItemOnClickListen(item);

			if (id == QuickSettingsData.QS_ID_BLUETOOTH || id == QuickSettingsData.QS_ID_WLAN) {
				item.setTextViewIcon();
				item.setTextViewOnclickListener(id);
			} else if (id == QuickSettingsData.QS_ID_MOBILEDATA) {
				Log.e("dxl", "enter into qs");
				itemTemp = item;
				Message msg = mUIHandler.obtainMessage(MSG_UPDATE_UI, id, 0);
				mUIHandler.sendMessage(msg);
			}

			item.setVisibility(VISIBLE);
			item.updateState(state);
		}
	}

	private UIHandler mUIHandler;

	private class UIHandler extends Handler {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == MSG_UPDATE_UI) {
				pSiminfos = (ArrayList<SimInfo>) SimInfo.getActiveSimInfoList(mContext);
				int id = msg.arg1;
				if (pSiminfos != null && pSiminfos.size() > 1) {
					itemTemp.setTextViewIcon();
					itemTemp.setMobileTextViewOnclickListiner(id);
				} else {
					itemTemp.removeTextViewIcon();
				}
			}
		};
	}

	public class PanelPagerAdapter extends PagerAdapter {
		public List<View> mViewList;

		public PanelPagerAdapter(List<View> viewsList) {
			this.mViewList = viewsList;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(mViewList.get(arg1));
		}

		@Override
		public void finishUpdate(View arg0) {
		}

		@Override
		public int getCount() {
			return mViewList.size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(mViewList.get(arg1), 0);
			return mViewList.get(arg1);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
	}

	@Override
	public void resetQsView(Boolean bReinitialize) {
		if (bReinitialize) {
			item = null;
			mQuickSettingPagerAdapter.notifyDataSetChanged();
		}
	}

	public void setBottomPanelVisible(boolean isShow) {
		mQSBottomPanel.mOnQuickSettingsPannelView.setVisibility(isShow);
	}

	public void resetPagers() {
		mQuickSettingPager.setCurrentItem(0);
		mBottomIconPager.setCurrentItem(0);
	}

	public View[] getPage() {
		View[] v = new View[2];
		v[0] = mQuickSettingPage1;
		v[1] = mQuickSettingPage2;
		return v;
	}

	private BroadcastReceiver mBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			LogHelper.sd(TAG, "intent=" + action);
			if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
				if (mIconText1 != null && mIconText2 != null && mIconText3 != null && mIconText4 != null) {
					mIconText1.setText(R.string.qs_label_flashlight);
					mIconText2.setText(R.string.qs_label_calculator);
					if(!mShowWeiXinAliPaySaoYiSao && !mShowWeiXinAliPayFuKuan){
						mIconText3.setText(R.string.qs_label_blaster);
					}
					mIconText4.setText(R.string.qs_label_camera);
				}
			} else if (action.equals(ACTION_FLASHLIGHT_CLOSE_FLAG)) {
				onFlashLightStateChange();
			} else if (action.equals(ACTION_FLASHLIGHT_ON_FLAG)) {
				onFlashLightStateChange();
			}
		}
	};

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mYLQuickSettings != null) {
			mYLQuickSettings.onConfigurationChanged();
		}
		for (int i = 0; i < mBottmIconViewList.size(); i++) {
			loadPage(mBottmIconViewList.get(i), i, false);
		}
		mBottomIconPagerAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	  if(mBroadcast == null){
			return;
		}
		if(mIsRegistered){
			mContext.unregisterReceiver(mBroadcast);
			mIsRegistered = false;
		}
	}

	private void onFlashLightStateChange() {
		String isOpen = SystemProperties.get("sys.yulong.flashlight", "0");
		LogHelper.sd(TAG, "onFlashLightStateChange isOpen=" + isOpen);
		if (isOpen.equals("0")) {
			mFlashIcon.setImageResource(R.drawable.ic_qs_flashlight_off);
		} else if (isOpen.equals("1")) {
			mFlashIcon.setImageResource(R.drawable.ic_qs_flashlight_on);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

	float oldRawy = 0f, newRawy = 0f, oldRawx = 0f, newRawx = 0f, deltaY, deltaX;

	private String[] mCustomIcon = new String[3];

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		int moveSizeY = (int) mContext.getResources().getDimension(R.dimen.navigation_bar_size);
		// int moveSizeX = (int)
		// mContext.getResources().getDimension(R.dimen.qs_detail_header_text_size);

		if (QuickSettingLauncher.isPopuped) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				oldRawy = event.getRawY();
				oldRawx = event.getRawX();
				break;
			case MotionEvent.ACTION_MOVE:
				break;
			case MotionEvent.ACTION_UP:
				newRawy = event.getRawY();
				newRawx = event.getRawX();
				deltaY = newRawy - oldRawy;
				deltaX = newRawx - oldRawx;
				if (deltaY > moveSizeY && Math.abs(deltaX) < moveSizeY) {
					setBottomPanelVisible(false);
					newRawy = 0f;
					oldRawy = 0f;
				}
				break;
			}
		}

		return super.dispatchTouchEvent(event);
	}

	public void onDestroy() {
		mBrightnessController.onDestroy();
		mBrightnessController = null;
		mYLQuickSettings.RemoveUpdateViewCallback(this);
		mYLQuickSettings = null;
		mQuickSettingPager.setAdapter(null);
		mQuickSettingPager = null;
		removeAllViews();
	}

	public void setStatusBar(PhoneStatusBar phoneStatusBar) {
		mStatusBar = phoneStatusBar;
	}

	public static void setInitFlag(boolean flag) {
		initFlag = flag;
	}

	public void onBottomIconUpdate(TreeMap<Integer, CheckablePackageInfo> treeMap) {
		for (int i = 0; i < mCustomIcon.length; i++) {
			mCustomIcon[i] = "";
		}
		int j = 0;
		for (Iterator<Integer> iterator = treeMap.keySet().iterator(); iterator.hasNext();) {
			mCustomIcon[j++] = treeMap.get(iterator.next()).packageInfo.applicationInfo.packageName;
		}
		loadPage(mBottmIconViewList.get(1), 1, false);
		mBottomIconPagerAdapter.notifyDataSetChanged();
	}
}
