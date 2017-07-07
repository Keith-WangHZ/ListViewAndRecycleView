package com.android.systemui.quicksettings.bottom;

import java.util.List;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import android.annotation.Nullable;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickSettingIconView extends LinearLayout {

	private static final String TAG = "QuickSettingIconView";

	public static final String PACKAGE_FLASH = "flash";
	public static final String PACKAGE_CALCULATOR = "com.android.calculator2";
	public static final String PACKAGE_CAMERA = "camera";
	public static final String PACKAGE_QUICK_PAY = "com.ivvi.quickscan";
	public static final String PACKAGE_QUICK_PAY_BY = "com.ivvi.quickscan.payby";
	public static final Intent SECURE_CAMERA_INTENT = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
			.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
	public static final Intent INSECURE_CAMERA_INTENT = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);

	private PhoneStatusBar mStatusBar;
	private QSBottomPanel mQSBottomPanel;
	private ImageView mIcon;
	private TextView mText;

	private String mTargetPackage = "";

	public QuickSettingIconView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		setOnClickListener(mIconClickListener);
		setOnLongClickListener(mIconLongClickListener);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mIcon = (ImageView) findViewById(R.id.icon);
		mText = (TextView) findViewById(R.id.text);
		if (mIcon == null || mText == null) {
			throw new RuntimeException("QuickSettingIconView must contains icon and text.");
		}
	};

	private View.OnClickListener mIconClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			handleClick();
		}
	};

	private View.OnLongClickListener mIconLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			handleClick();
			return false;
		}
	};

	private boolean mIsCustomIcon = true;

	public void handleClick() {
		
		insureStatusBar();
		if (mTargetPackage.equals(PACKAGE_FLASH)) {
			sendBroadcastForFlashLight();
			
			return;
		}

		Intent intent = getIntentFromPackage();

		final Intent intentForCal = getIntentForCalculator();
		
		if (!(mTargetPackage.equals(PACKAGE_CALCULATOR) || 
				mTargetPackage.equals(PACKAGE_QUICK_PAY) || 
				mTargetPackage.equals(PACKAGE_QUICK_PAY_BY)) && intent == null) {
			return;
		}
		mQSBottomPanel.setBottomPanelVisible(false);

		final Intent i = intent;
		postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mStatusBar.isKeyguard() && isCustomIcon()
						&& i != SECURE_CAMERA_INTENT) {
					mStatusBar.dismissKeyguardThenExecute(
							new OnDismissAction() {
								@Override
								public boolean onDismiss() {
									if (mTargetPackage
											.equals(PACKAGE_CALCULATOR)) {
										mContext.startActivityAsUser(
												intentForCal, null,
												UserHandle.OWNER);
										Log.v(TAG,
												"starting Activity intentForCal = "
														+ intentForCal
																.getAction());
									} else if(mTargetPackage
											.equals(PACKAGE_QUICK_PAY)){
										Intent in = new Intent();
										in.setPackage("com.ivvi.quickscan");
										in.setAction("com.ivvi.quickscan.MainSerivce");
								        mContext.startServiceAsUser(in,UserHandle.OWNER);
										Log.v(TAG,
												"starting Activity in = "
														+ in.getAction());
									} else if(mTargetPackage
											.equals(PACKAGE_QUICK_PAY_BY)){
										Intent in = new Intent();
										in.setPackage("com.ivvi.quickscan");
										in.setAction("com.ivvi.quickscan.PaySerivce");
								        mContext.startServiceAsUser(in,UserHandle.OWNER);
										Log.v(TAG,
												"starting Activity in = "
														+ in.getAction());
									} else {
										mContext.startActivityAsUser(i, null,
												getCurrentUserHandle());
										Log.v(TAG,
												"starting Activity from keyguard i = "
														+ i.getAction());
									}

									return false;
								}
							}, false);
				} else {
					if (mTargetPackage.equals(PACKAGE_CALCULATOR)) {
						mContext.startActivityAsUser(intentForCal, null,
								UserHandle.OWNER);
						Log.v(TAG, "starting Activity intentForCal = "
								+ intentForCal.getAction());
					} else if(mTargetPackage
							.equals(PACKAGE_QUICK_PAY)){
						Intent in = new Intent();
						in.setPackage("com.ivvi.quickscan");
						in.setAction("com.ivvi.quickscan.MainSerivce");
						mContext.startServiceAsUser(in,UserHandle.OWNER);
							Log.v(TAG,
									"starting Activity in = "
											+ in.getAction());
					} else if(mTargetPackage
							.equals(PACKAGE_QUICK_PAY_BY)){
						Intent in = new Intent();
						in.setPackage("com.ivvi.quickscan");
						in.setAction("com.ivvi.quickscan.PaySerivce");
				        mContext.startServiceAsUser(in,UserHandle.OWNER);
						Log.v(TAG,
								"starting Activity in = "
										+ in.getAction());
					} else {
						mContext.startActivityAsUser(i, null,
								getCurrentUserHandle());
						Log.v(TAG, "starting Activity i = " + i.getAction());
					}

				}
			}
		}, 300);
	}

	private Intent getIntentForCalculator() {
		Intent intent = new Intent();
		ComponentName comp = new ComponentName(PACKAGE_CALCULATOR,
				"com.android.calculator2.Calculator");
		intent.setComponent(comp);

		if (mStatusBar.getBarState() != StatusBarState.SHADE) {
			KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor
					.getInstance(mContext);
			boolean canSkipBouncer = updateMonitor
					.getUserCanSkipBouncer(KeyguardUpdateMonitor
							.getCurrentUser());
			boolean secure = mStatusBar.isKeyguardSecure();
			if (secure && !canSkipBouncer) {
				intent.setAction("android.com.calculator2.locked");
			} else {
				intent.setAction("android.com.calculator2.shade");
			}
		} else {
			intent.setAction("android.com.calculator2.shade");
		}
		return intent;
	}

	protected boolean isCustomIcon() {
		return mIsCustomIcon;
	}

	private Intent getEditActivityIntent() {
		Intent i = new Intent();
		i.setClass(mContext.getApplicationContext(), BottomIconEditActivity.class);
		return i;
	}

	private void insureStatusBar() {
		if (mStatusBar != null && mQSBottomPanel != null) {
			return;
		}
		QuickSettingLauncher qsl = QuickSettingLauncher.getInstance(mContext);
		if(qsl != null){
		mStatusBar = qsl.getStatusBar();
		mQSBottomPanel = qsl.getQSPanel();
		}
	}

	private Intent getIntentFromPackage() {
		if (mTargetPackage.equals(PACKAGE_CAMERA)) {
			return getCameraIntent();
		}
		
		if (mTargetPackage.equals(mContext.getPackageName())) {
			return getEditActivityIntent();
		}
		
		PackageManager pm = mContext.getPackageManager();
		List<PackageInfo> packageList = pm.getInstalledPackages(getCurrentUserHandle().getIdentifier());
		for (PackageInfo packageInfo : packageList) {
			if (mTargetPackage.equals(packageInfo.packageName)) {
				return pm.getLaunchIntentForPackage(mTargetPackage);
			}
		}
		
		return null;
	}

	private Intent getCameraIntent() {
		if (mStatusBar.getBarState() != StatusBarState.SHADE) {
			KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
			boolean canSkipBouncer = updateMonitor.getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser());
			boolean secure = mStatusBar.isKeyguardSecure();
			return (secure && !canSkipBouncer) ? SECURE_CAMERA_INTENT : INSECURE_CAMERA_INTENT;
		} else {
			return INSECURE_CAMERA_INTENT;
		}
	}

	private UserHandle getCurrentUserHandle() {
		try {
			return ActivityManagerNative.getDefault().getCurrentUser().getUserHandle();
		} catch (RemoteException e) {
			return null;
		}
	}

	private void sendBroadcastForFlashLight() {
		String isOpen = SystemProperties.get("sys.yulong.flashlight", "0");
		LogHelper.sd(TAG, "sendBroadcastForFlashLight  isOpen = " + isOpen);
		String flashLight = "com.android.intent.action.Close_FlashLight";
		if (isOpen.equals("0")) {
			mIcon.setImageResource(R.drawable.ic_qs_flashlight_on);
			flashLight = "com.android.intent.action.Open_FlashLight";
		} else if (isOpen.equals("1")) {
			mIcon.setImageResource(R.drawable.ic_qs_flashlight_off);
			flashLight = "com.android.intent.action.Close_FlashLight";
		}
		Intent intent = new Intent(flashLight);
		mContext.sendBroadcast(intent);
	}

	public void setTargetPackage(String tPackage) {
		mTargetPackage = tPackage;
	}

	public void setImageResource(int resId) {
		mIcon.setImageResource(resId);
	}

	public void setText(String text) {
		mText.setText(text);
	}

	public void setIsCustomIcon(boolean isCustom) {
		mIsCustomIcon = isCustom;
	}

	public void setImageBitmap(Bitmap bitmap) {
		mIcon.setImageBitmap(bitmap);
	}
}
