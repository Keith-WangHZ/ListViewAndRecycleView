package com.android.systemui.quicksettings.bottom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.phone.CPDataConnSettingUtils;

public class QuickSettingMobilePanelView extends LinearLayout implements OnClickListener, OnKeyListener {
    private static final String TAG = QuickSettingMobilePanelView.class.getSimpleName();
    private LinearLayout floatMenuLayout;
    private RadioButton radio_sim1;
    private RadioButton radio_sim2;
    private RadioButton radio_sim3;
    private TextView mDetailTitleText;
    private TextView btn_finish;
    private TextView btn_more;
    private LinearLayout ll_sim1;
    private LinearLayout ll_sim2;
    private LinearLayout ll_sim3;
    private TextView tv_sim1_name;
    private TextView tv_sim2_name;
    private TextView tv_sim3_name;
    public static final Object locked = new Object();
    private int simNumbers = 0;
    private ImageView mImage;

    private Intent mIntent;
    private int pOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int currentOrientation = Configuration.ORIENTATION_PORTRAIT;
    private boolean mIsRegistered = false;
    
    public QuickSettingMobilePanelView(Context context, int Oriention) {
        super(context);
        mContext = context;
        currentOrientation = Oriention;
        layoutInflate();
        int height = 0;
        if (currentOrientation == pOrientation) {
            height = (int) context.getResources().getDimension(R.dimen.new_systemui_height);
        } else {
            height = (int) context.getResources().getDimension(R.dimen.new_systemui_height_new);
        }
        this.setLayoutParams(new LayoutParams(-1, height));

        this.setOnKeyListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mContext.registerReceiver(mBroadcast, filter);
        mIsRegistered = true;
    }

    private BroadcastReceiver mBroadcast = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                if(mQSPanel != null){
                	mQSPanel.setMobilePanelViewVisible(false);
                }
            }
        }
    };
    protected QSBottomPanel mQSPanel;

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

    protected void layoutInflate() {
        createSystemUIView();
        InitImage();
        initView();
    }

    private void createSystemUIView() {
        removeAllViews();
        if (currentOrientation == pOrientation) {
            floatMenuLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.quick_setting_mobile_panel,
                    this);
        } else {
            floatMenuLayout = (LinearLayout) LayoutInflater.from(mContext)
                    .inflate(R.layout.quick_setting_mobile_panel_landscape, this);
        }
    }

    private void InitImage() {
        mImage = (ImageView) floatMenuLayout.findViewById(R.id.image_icon);

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQSPanel.setMobilePanelViewVisible(false);
            }
        });
    }

    public void readSim() {
        updateDetailText();
        readDefaultDataNetWork();
    }

    private void readDefaultDataNetWork() {
        int card = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);
        if (card == 0) {
            radio_sim1.setChecked(true);
            radio_sim2.setChecked(false);
            radio_sim3.setChecked(false);
        } else if (card == 1) {
            radio_sim1.setChecked(false);
            radio_sim2.setChecked(true);
            radio_sim3.setChecked(false);
        } else if (card == 2) {
            radio_sim1.setChecked(false);
            radio_sim2.setChecked(false);
            radio_sim3.setChecked(true);
        }
    }

    private void updateDetailText() {
        Log.e(TAG, "updateDetailText");
        simNumbers = SimInfo.getPhoneCount();
        View[] cardRow = { ll_sim1, ll_sim2, ll_sim3 };
        TextView[] labelViews = { tv_sim1_name, tv_sim2_name, tv_sim3_name };
        int[] labelStrings = { R.string.sim1, R.string.sim2, R.string.sim3 };
        for (int i = 0; i < simNumbers; i++) {
            SimInfo simInfo = SimInfo.getActiveSimInfoBySlot(mContext, i);
            if (null != simInfo) {
                cardRow[i].setVisibility(View.VISIBLE);
                String displayName = simInfo.getDisplayName();
                if (!TextUtils.isEmpty(displayName)) {
                    String sim = getResources().getString(labelStrings[i]);
                    labelViews[i].setText(sim + "-" + displayName);
                } else {
                    labelViews[i].setText(getResources().getString(R.string.status_bar_expanded_data_net_status));
                }
            }
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateDetailText();
        try {
        	btn_finish.setText(getResources().getString(R.string.quick_settings_done));
            btn_more.setText(getResources().getString(R.string.quick_settings_more_settings));
            mDetailTitleText.setText(getResources().getString(R.string.quick_settings_cellular_detail_title));
		} catch (Exception e) {
		}
        
    }

    private void initView() {
        radio_sim1 = (RadioButton) floatMenuLayout.findViewById(R.id.id_sim1_radio);
        radio_sim2 = (RadioButton) floatMenuLayout.findViewById(R.id.id_sim2_radio);
        radio_sim3 = (RadioButton) floatMenuLayout.findViewById(R.id.id_sim3_radio);

        mDetailTitleText = (TextView) floatMenuLayout.findViewById(R.id.textView1);
        btn_finish = (TextView) floatMenuLayout.findViewById(R.id.id_finish);
        btn_more = (TextView) floatMenuLayout.findViewById(R.id.id_moresetting);

        ll_sim1 = (LinearLayout) floatMenuLayout.findViewById(R.id.id_ll_sim1);
        ll_sim2 = (LinearLayout) floatMenuLayout.findViewById(R.id.id_ll_sim2);
        ll_sim3 = (LinearLayout) floatMenuLayout.findViewById(R.id.id_ll_sim3);

        tv_sim1_name = (TextView) floatMenuLayout.findViewById(R.id.id_sim1);
        tv_sim2_name = (TextView) floatMenuLayout.findViewById(R.id.id_sim2);
        tv_sim3_name = (TextView) floatMenuLayout.findViewById(R.id.id_sim3);

        ll_sim1.setOnClickListener(this);
        ll_sim2.setOnClickListener(this);
        ll_sim3.setOnClickListener(this);

        btn_finish.setOnClickListener(this);
        btn_more.setOnClickListener(this);

        radio_sim1.setOnClickListener(this);
        radio_sim2.setOnClickListener(this);
        radio_sim3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.id_ll_sim1:
        case R.id.id_sim1_radio:
            if (radio_sim1.isChecked() == true) {
                radio_sim2.setChecked(false);
                radio_sim3.setChecked(false);
            } else {
                radio_sim1.setChecked(true);
                radio_sim2.setChecked(false);
                radio_sim3.setChecked(false);
            }
            CPDataConnSettingUtils.setDefaultDataNetwork(mContext, 0);
            SimInfo.setMobileDataEnabled(mContext, true, simNumbers);
            mIntent = new Intent("android.yulong.intent.action.MANUAL_SWITCH_4G_NETWORK");
            mIntent.putExtra("phone_id", 1);
            mContext.sendBroadcast(mIntent);
            break;

        case R.id.id_ll_sim2:
        case R.id.id_sim2_radio:
            if (radio_sim2.isChecked() == true) {
                radio_sim1.setChecked(false);
                radio_sim3.setChecked(false);
            } else {
                radio_sim1.setChecked(false);
                radio_sim2.setChecked(true);
                radio_sim3.setChecked(false);
            }
            CPDataConnSettingUtils.setDefaultDataNetwork(mContext, 1);
            SimInfo.setMobileDataEnabled(mContext, true, simNumbers);
            mIntent = new Intent("android.yulong.intent.action.MANUAL_SWITCH_4G_NETWORK");
            mIntent.putExtra("phone_id", 2);
            mContext.sendBroadcast(mIntent);
            break;

        case R.id.id_ll_sim3:
        case R.id.id_sim3_radio:
            if (radio_sim3.isChecked() == true) {
                radio_sim1.setChecked(false);
                radio_sim2.setChecked(false);
            } else {
                radio_sim1.setChecked(false);
                radio_sim2.setChecked(false);
                radio_sim3.setChecked(true);
            }
            CPDataConnSettingUtils.setDefaultDataNetwork(mContext, 2);
            SimInfo.setMobileDataEnabled(mContext, true, simNumbers);
            mIntent = new Intent("android.yulong.intent.action.MANUAL_SWITCH_4G_NETWORK");
            mIntent.putExtra("phone_id", 2);
            mContext.sendBroadcast(mIntent);
            break;
        case R.id.id_finish:
            saveSimInfo();
            mQSPanel.setMobilePanelViewVisible(false);
            break;

        case R.id.id_moresetting:
            mQSPanel.setMobilePanelViewVisible(false);
            if(QuickSettingLauncher.getInstance(mContext) != null)QuickSettingLauncher.getInstance(mContext).setBottomPanelVisible(false);
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("com.android.settings.sim.SIM_SUB_INFO_SETTINGS");
            mContext.startActivity(intent);
            break;

        default:
            break;
        }
    }

    private void saveSimInfo() {
        boolean isSim1On = radio_sim1.isChecked();
        boolean isSim2On = radio_sim2.isChecked();
        boolean isSim3On = radio_sim3.isChecked();
        if (isSim1On) {
            CPDataConnSettingUtils.setDefaultDataNetwork(mContext, 0);
        } else if (isSim2On) {
            CPDataConnSettingUtils.setDefaultDataNetwork(mContext, 1);
        } else if (isSim3On) {
            CPDataConnSettingUtils.setDefaultDataNetwork(mContext, 2);
        }
    }

    @Override
    public boolean onKey(View v, int key, KeyEvent e) {
        if (KeyEvent.KEYCODE_HOME == e.getAction() || KeyEvent.KEYCODE_BACK == e.getAction()) {
            saveSimInfo();
            mQSPanel.setMobilePanelViewVisible(false);
        }
        return true;
    }

    public void setQSPanel(QSBottomPanel qsBottomPanel) {
        mQSPanel = qsBottomPanel;
    }

    public void onDestroy() {
        btn_finish.setOnClickListener(null);
        radio_sim1.setOnClickListener(null);
        radio_sim2.setOnClickListener(null);
        radio_sim3.setOnClickListener(null);
        mImage.setOnClickListener(null);
        setOnKeyListener(null);
        mQSPanel = null;
        removeAllViews();
    }
}
