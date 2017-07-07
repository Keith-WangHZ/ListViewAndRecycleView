package com.android.systemui.quicksettings.bottom;

import java.util.ArrayList;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R;
import com.android.systemui.quicksettings.bottom.CtrlCenterViewContainer.CtrlCenterDataCallback;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.DataControler;
import com.android.systemui.statusbar.phone.DataControler.DatabaseRecord;
import com.yulong.android.common.app.CommonActivity;
import com.yulong.android.common.view.TopBar;
import com.yulong.android.common.view.TopBar.TopBarStyle;
import com.android.systemui.statusbar.phone.DataControlerContain;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CtrlCenterActivity extends CommonActivity {
	private static final String TAG = CtrlCenterActivity.class.getSimpleName();
	private static final boolean DEBUG = true;
	private ArrayList<DatabaseRecord> mDbdata = new ArrayList<DatabaseRecord>(24);
	private CtrlCenterViewContainer mCtrlCenterView;
	private DataControler mDataControler;
	
	private KeyguardUpdateMonitorCallback mUpdateCallBack = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onUserSwitching(int userId) {
			KeyguardUpdateMonitor.getInstance(getApplicationContext()).removeCallback(mUpdateCallBack);
			finish();			
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setBodyLayout(R.layout.ctrlcenter_layout);
		RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
		mCtrlCenterView = new CtrlCenterViewContainer(this);
		mCtrlCenterView.setCtrlCenterDataCallback(callback);
		container.addView(mCtrlCenterView);
		initData();
		TextView page2Hint = (TextView) findViewById(R.id.ctrl_center_shown_page2);
		TextView notShownHint = (TextView) findViewById(R.id.ctrl_center_not_shown);
		if (!Utilities.isPrimaryUser()) {
			page2Hint.setText(R.string.ctrl_center_non_shown);
			notShownHint.setVisibility(View.GONE);
		}
		KeyguardUpdateMonitor.getInstance(getApplicationContext()).registerCallback(mUpdateCallBack);
	}

	@Override
	protected void onCreateTopBar(TopBar topBar) {
		super.onCreateTopBar(topBar);
		topBar.setTopBarStyle(TopBarStyle.TOP_BAR_NOTMAL_STYLE);
		topBar.setTopBarTitle(getResources().getString(R.string.qs_label_controlcentor));
		topBar.setTopBarTitleSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
		// topBar.setTopBarColorStyle(TopBarColorStyle.TOP_BAR_BLACK_STYLE);
		topBar.setDisplayUpView(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCtrlCenterView.saveData();
	}
	
	@Override
	protected void onDestroy() {
		KeyguardUpdateMonitor.getInstance(getApplicationContext()).removeCallback(mUpdateCallBack);
		super.onDestroy();
		//android.os.Process.killProcess(android.os.Process.myPid());//YLH20160812:kill itself in standalone process space
	};

	private void initData() {
		mDataControler = DataControlerContain.getInstance(this, !Utilities.isPrimaryUser());
		mDataControler.getAllShortcutDataSync(mDbdata);
		mCtrlCenterView.setData(mDbdata);
	}

	private CtrlCenterDataCallback callback = new CtrlCenterDataCallback() {
		@Override
		public boolean saveDate(ArrayList<DatabaseRecord> data) {
			if (DEBUG) {
				for (DatabaseRecord databaseRecord : data) {
					Log.e(TAG, "return databaseRecord: id=" + databaseRecord.id + "; pos=" + databaseRecord.pos + "; title="
							+ databaseRecord.title);
				}
			}
			mDataControler.saveAllShortcutCtrlCenterData(data);
			QuickSettingLauncher qsl = QuickSettingLauncher.getInstance(CtrlCenterActivity.this);
			if(qsl != null){
				qsl.updateBottomQsPanel(data);
			}
					
			return false;
		}
	};

}
