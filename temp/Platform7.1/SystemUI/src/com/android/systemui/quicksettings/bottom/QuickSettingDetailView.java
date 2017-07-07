package com.android.systemui.quicksettings.bottom;

import java.util.List;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.IvviGaussBlurViewFeature;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import android.annotation.Nullable;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
//import android.graphics.BlurParams;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuickSettingDetailView extends LinearLayout {

	private static final String TAG = "QuickSettingDetailView";

	private View mDetail;
	private int pOrientation = Configuration.ORIENTATION_PORTRAIT;
	private int mOrientation = 1;

	public QuickSettingDetailView(Context context, int oriention) {
		super(context, null);
		
		mOrientation = oriention;
		mContext = context;

		if (mOrientation == pOrientation) {
			mDetail = LayoutInflater.from(mContext).inflate(R.layout.qs_detail_bottom, null);
		} else {
			mDetail = LayoutInflater.from(mContext).inflate(R.layout.qs_detail_bottom_land, null);
		}
		addView(mDetail);
		
		if(Utilities.showFullGaussBlurForDDQS()){
//			setBlurMode(BlurParams.BLUR_MODE_WINDOW);
			IvviGaussBlurViewFeature.setBlurMode(this, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
		}
	}
	
	public View getDetailView(){
		return mDetail;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
	};

}
