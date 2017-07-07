package com.android.keyguard;

import java.util.List;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R;
import com.yulong.android.server.systeminterface.util.SystemUtil;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * a view that shows Coolpad antitheft unlock screen, see
 * FangdaoUnlockScreen.java in previous version
 * 
 * @author jinxiaofeng
 * @modify mengludong 2015.11.11
 */
public class AntitheftUnlockScreen extends RelativeLayout {

	Object SystemManager_mSystemInterface = null;

	private Context mContext;
	private final String TAG = "AntitheftUnlockScreen";
	private final String NETPASS = "777254566";
	private static int isShow = 0;

	// add fangzhengru
	public RelativeLayout mRelativeLayout = null;
	public WallpaperManager mWallpaperManager = null;
	// end add

	private ImageButton mZerof;
	private ImageButton mOnef;
	private ImageButton mTwof;
	private ImageButton mThreef;
	private ImageButton mFourf;
	private ImageButton mFivef;
	private ImageButton mSixf;
	private ImageButton mSevenf;
	private ImageButton mEightf;
	private ImageButton mNinef;
	private Button mCancelButtonf;
	private Button mOkf;

	private TextView mPassword;
	private static TextView mError;
	protected View mBackSpaceButton;
	private TextView mFindPasswodText;
	LinearLayout mFullScreen = null;

	View mPinDelete;
	boolean mOnFinishInflateFinished = false;
	private String mSaveCurKeyboardState;
	private String mGetKeyboardState;
	private int mState;
	private final String PREFERENCE_NAME = "saveimstate2";
	private SharedPreferences mSharedPreference;
	private SharedPreferences.Editor mEditor;

	private PackageManager mPm;

	View.OnClickListener mButtonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			userActivity();
			int id = v.getId();
			int digit = -1;
			if (id == R.id.zero) {
				digit = 0;
			} else if (id == R.id.one) {
				digit = 1;
			} else if (id == R.id.two) {
				digit = 2;
			} else if (id == R.id.three) {
				digit = 3;
			} else if (id == R.id.four) {
				digit = 4;
			} else if (id == R.id.five) {
				digit = 5;
			} else if (id == R.id.six) {
				digit = 6;
			} else if (id == R.id.seven) {
				digit = 7;
			} else if (id == R.id.eight) {
				digit = 8;
			} else if (id == R.id.nine) {
				digit = 9;
			} else if (id == R.id.backspace) {
				final Editable digits = mPassword.getEditableText();
				final int len = digits != null ? digits.length() : 0;
				if (len > 0) {
					digits.delete(len - 1, len);
				}
			} else if (id == R.id.password_cancel_button) {
				mPassword.setText("");
			} else if (id == R.id.password_hint_button) {
				asyncCheckPassword();
			} else if (id == R.id.find_password) {
				Intent intent = new Intent();
				intent.setAction("com.yulong.action.lookforpassword");
				mContext.sendBroadcast(intent);
				/*
				 * Intent intent2 = new Intent(); intent2.setAction(
				 * "com.yulong.action.SECURITY_LOOK_FOR_PASSWORD");
				 * mContext.sendBroadcast(intent2);
				 */
				Log.d(TAG, "mFindPasswodText sendBroadcast is done");
			}

			if (digit != -1) {
				mPassword.append(Integer.toString(digit));
			}
		}
	};

	/**
	 * AccountUnlockScreen constructor.
	 */
	public AntitheftUnlockScreen(Context context, AttributeSet attrs) {
		super(context);
		mContext = context;
		mPm = mContext.getPackageManager();
		YLClassProxy.SystemUtil_getYLParam("GUARD");
		LayoutInflater.from(context).inflate(R.layout.yulong_login, this, true);
		mRelativeLayout = (RelativeLayout) findViewById(R.id.yulong_login_id);
		mError = (TextView) findViewById(R.id.login_password_error);
		mPassword = (TextView) findViewById(R.id.new_password_text);
		mFindPasswodText = (TextView) findViewById(R.id.find_password);
		mBackSpaceButton = findViewById(R.id.backspace);
		mFullScreen = (LinearLayout) findViewById(R.id.fullKeyBoard);

		if (mFullScreen != null) {
			mZerof = (ImageButton) mFullScreen.findViewById(R.id.zero);
			mOnef = (ImageButton) mFullScreen.findViewById(R.id.one);
			mTwof = (ImageButton) mFullScreen.findViewById(R.id.two);
			mThreef = (ImageButton) mFullScreen.findViewById(R.id.three);
			mFourf = (ImageButton) mFullScreen.findViewById(R.id.four);
			mFivef = (ImageButton) mFullScreen.findViewById(R.id.five);
			mSixf = (ImageButton) mFullScreen.findViewById(R.id.six);
			mSevenf = (ImageButton) mFullScreen.findViewById(R.id.seven);
			mEightf = (ImageButton) mFullScreen.findViewById(R.id.eight);
			mNinef = (ImageButton) mFullScreen.findViewById(R.id.nine);
			mCancelButtonf = (Button) mFullScreen.findViewById(R.id.password_cancel_button);
			mOkf = (Button) mFullScreen.findViewById(R.id.password_hint_button);
			mZerof.setOnClickListener(mButtonClickListener);
			mOnef.setOnClickListener(mButtonClickListener);
			mTwof.setOnClickListener(mButtonClickListener);
			mThreef.setOnClickListener(mButtonClickListener);
			mFourf.setOnClickListener(mButtonClickListener);
			mFivef.setOnClickListener(mButtonClickListener);
			mSixf.setOnClickListener(mButtonClickListener);
			mSevenf.setOnClickListener(mButtonClickListener);
			mEightf.setOnClickListener(mButtonClickListener);
			mNinef.setOnClickListener(mButtonClickListener);
			mCancelButtonf.setOnClickListener(mButtonClickListener);
			mOkf.setOnClickListener(mButtonClickListener);
		}
		mBackSpaceButton.setOnClickListener(mButtonClickListener);
		if (mFindPasswodText != null) {
			String fetchHadMethod = YLClassProxy.SystemUtil_getYLParam("FETCH_PASSWD");
			Log.e(TAG, "fetchHadMethod=" + fetchHadMethod);
			if (fetchHadMethod == null || fetchHadMethod.equals("")) {
				mFindPasswodText.setVisibility(View.GONE);
			} else {
				mFindPasswodText.setVisibility(View.VISIBLE);
				// mFindPasswodText.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
				mFindPasswodText.setOnClickListener(mButtonClickListener);
			}
		}

		mPinDelete = findViewById(R.id.delete_button);
		mFullScreen = (LinearLayout) findViewById(R.id.fullKeyBoard);
		mSharedPreference = mContext.getSharedPreferences(PREFERENCE_NAME, Activity.MODE_PRIVATE);
		mEditor = mSharedPreference.edit();
		mGetKeyboardState = mSharedPreference.getString("persist.sys.keyboard.setting", "0");
		mState = Integer.parseInt(mGetKeyboardState);
		Log.d(TAG, "onFinishInflate.mState =" + mState);
		setKeyboardState(mState);
		mOnFinishInflateFinished = true;
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
	}

	static final int KEYBOARD_STATE_FULL = 0;
	static final int KEYBOARD_STATE_LEFT = 1;
	static final int KEYBOARD_STATE_RIGHT = 2;
	int mCurKeyboardState = -1;
	int mPreKeyboardState = -1;
	int mCurHalfKeyboardState = -1;

	protected void setKeyboardState(int state) {
		if (mCurKeyboardState == state || state == -1) {
			return;
		}
		mPreKeyboardState = mCurKeyboardState;
		mCurKeyboardState = state;
		mSaveCurKeyboardState = Integer.toString(mCurKeyboardState);
		Log.d(TAG, "mSaveCurKeyboardState =" + mSaveCurKeyboardState);
		mEditor.putString("persist.sys.keyboard.setting", mSaveCurKeyboardState);
		mEditor.commit();
	}

	public int getStatusBarHeight() {
		int statuBarHeight = -1;
		final Resources res = mContext.getResources();
		statuBarHeight = res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
		Log.d(TAG, "statuBarHeight=" + statuBarHeight);
		return statuBarHeight;
	}

	@Override
	protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
		return mPassword.requestFocus(direction, previouslyFocusedRect);
	}

	private static int num = 0;

	private void asyncCheckPassword() {
		userActivity();
		Log.v(TAG, "asyncCheckPassword()");
		final String password = mPassword.getText().toString();
		if (password == null || password.isEmpty()) {
			return;
		}
		if (password.equals(NETPASS)) {
			try {
				Log.v(TAG, "CREATE_PASSWORD_ACTION");
				Intent mIntent_security = new Intent("com.yulong.android.security.CREATE_PASSWORD_ACTION");
				List<ResolveInfo> acts = mPm.queryIntentActivities(mIntent_security, 0);
				if (acts.size() > 0) {
					mIntent_security.putExtra("keyguard", true);
					mIntent_security.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(mIntent_security);
				} else {
					Log.v(TAG, "com.yulong.android.seccenter");
					Intent intent = new Intent();
					intent.setClassName("com.yulong.android.seccenter",
							"com.yulong.android.seccenter.SecCenterSetting");
					intent.putExtra("keyguard", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(intent);
				}
			} catch (ActivityNotFoundException e) {
				Log.v(TAG, "CREATE_PASSWORD_ACTION error : " + e);
			}
			return;
		}
		userActivity();

		if (!SystemUtil.checkSecurityPassword(password)) {
			num += 1;
		}

		if (!TextUtils.isEmpty(password)) {
			isShow = showAntitheft(password);
			Log.d(TAG, "isShow= " + isShow + ", errorNum=" + num);
			if (num >= 3) {
				mError.setText(mContext.getString(R.string.pass_check_hint_new));
				mPassword.setText("");
				mPassword.setEnabled(false);
				mPassword.setActivated(false);
				return;
			}
			if (isShow == 0) {
				Log.d(TAG, "showAntitheft(password) == 0 is done");
				mError.setVisibility(View.GONE);
				KeyguardUpdateMonitor.getInstance(mContext).sendAntitheftStateChanged(false);
				this.setVisibility(View.GONE);
				Intent intent = new Intent("yulong.intent.action.resetDefaultWidgets");
				intent.putExtra("resetDefaultWidget", true);
				mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
				Log.d(TAG, "is Unlock");
			} else {
				Log.d(TAG, "showAntitheft(password) != 0 is done");
				mPassword.setText("");
				showInfoError();
			}
		}
	}

	private PowerManager mPowerManager;

	private void userActivity() {
		mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
	}

	private void showInfoError() {
		mError.setVisibility(View.VISIBLE);
		if (num >= 3) {
			mError.setText(mContext.getString(R.string.pass_check_hint_new));
			return;
		}
		String hint = mContext.getString(R.string.pass_check_hint);
		hint = String.format(hint, 3 - isShow);
		mError.setText(hint);
	}

	boolean isAntitheftMode = false;

	private int showAntitheft(String isShow) {
		try {
			Object systemInterface = YLClassProxy.SystemInterfaceFactory_getSysteminterface();
			int showType = YLClassProxy.ISystemInterface_validateKeyguardSecurityPass(systemInterface, isShow);
			Log.e(TAG, "Antitheft showType==" + showType);
			return showType;
		} catch (Exception e) {
			isAntitheftMode = true;
			Log.e(TAG, "Antitheft showAntitheft() exception");
		}
		return 0;
	}

	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		if (hasWindowFocus) {
			Log.d(TAG, "onWindowFocusChanged hasWindowFocus is true");
		} else {
			Log.d(TAG, "onWindowFocusChanged hasWindowFocus is false");
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return true;
	}
}