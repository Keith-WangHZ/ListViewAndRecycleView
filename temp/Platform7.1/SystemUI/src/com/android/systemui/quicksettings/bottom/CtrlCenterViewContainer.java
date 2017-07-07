package com.android.systemui.quicksettings.bottom;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.android.systemui.R;
import com.android.systemui.quicksettings.bottom.CtrlCenterItemView.Model;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.DataControler.DatabaseRecord;
import com.android.systemui.statusbar.phone.QuickSettingsData;
import com.yulong.android.feature.FeatureConfig;
import com.yulong.android.feature.FeatureString;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
//import yulong.config.CPCommRunMode;

@SuppressLint("UseSparseArrays")
public class CtrlCenterViewContainer extends FrameLayout implements View.OnTouchListener {
    private static final String TAG = CtrlCenterViewContainer.class.getSimpleName();
    private static final boolean DEBUG2 = false, DEBUG = true;
    private static final int ANIM_T = 150, TOTAL_SHOW = 16, TOTAL_ALL = 24;
    private static int CTLR_CENTER_POS = 15;
    private int marging, mGap, mInterval, mItemWidth, mItemHeight, mItemWidthB, mIconWidth, mIconWidthB, mItemHeightB,
            mDWidth, mDHeight;
    private Resources mRes = getResources();
    private Context mContext;
    private static IPackageManager pm;

    @SuppressWarnings("serial")
    private HashMap<Integer, Model> mItemModes = new HashMap<Integer, Model>(24) {
        {
            long s = System.currentTimeMillis();
            Resources res = mRes;
            Model m = null;

            m = new Model(res.getString(R.string.qs_label_airplane), R.drawable.ic_qs_airplane_disable);
            put(QuickSettingsData.QS_ID_AIRPLANE_MODE, m);// 2:飞行模式

            m = new Model(res.getString(R.string.qs_label_wlan), R.drawable.ic_qs_wlan_disable);
            put(QuickSettingsData.QS_ID_WLAN, m);// 0:WLAN

            m = new Model(res.getString(R.string.quick_settings_mobile_data_label), R.drawable.ic_qs_mobile_disable);
            put(QuickSettingsData.QS_ID_MOBILEDATA, m);// 1:数据网络

            m = new Model(res.getString(R.string.qs_label_bluetooth), R.drawable.ic_qs_bluetooth_disable);
            put(QuickSettingsData.QS_ID_BLUETOOTH, m);// 8:蓝牙

            m = new Model(res.getString(R.string.qs_label_rotation), R.drawable.ic_qs_rotation_lock);
            put(QuickSettingsData.QS_ID_ROTATION, m);// 5:自动旋转
            
            m = new Model(res.getString(R.string.night_mode), R.drawable.ic_qs_night_mode_disable);
            put(QuickSettingsData.QS_ID_NIGHT_MODE, m);// 46:
            
            m = new Model(res.getString(R.string.quick_settings_dnd_label), R.drawable.ic_qs_dnd_off);
            put(QuickSettingsData.QS_ID_DND_MODE, m);
            
            m = new Model(res.getString(R.string.qs_label_wfc), R.drawable.ic_qs_wifi_calling_off);
            put(QuickSettingsData.QS_ID_WFC_MODE, m);

            m = new Model(res.getString(R.string.qs_label_ringer), R.drawable.ic_qs_ringer_disable);
            put(QuickSettingsData.QS_ID_RINGER_MODE, m);// 9:静音

            m = new Model(res.getString(R.string.qs_label_vibrate_ring), R.drawable.ic_qs_vibrate_ring_disable);
            put(QuickSettingsData.QS_ID_VIBRATE_RING_MODE, m);// 13:震动

            m = new Model(res.getString(R.string.qs_label_setting), R.drawable.ic_qs_setting_disable);
            put(QuickSettingsData.QS_ID_SETTING, m);// 22:设置

            m = new Model(res.getString(R.string.accessibility_search_light), R.drawable.ic_qs_search_disable);
            put(QuickSettingsData.QS_ID_SEARCH, m);// 50:搜索

            m = new Model(res.getString(R.string.qs_label_slience), R.drawable.ic_qs_screenshots_disable);
            put(QuickSettingsData.QS_ID_SLIENCE, m);// 11: 屏幕截图

            m = new Model(res.getString(R.string.qs_label_location), R.drawable.ic_qs_location_disable);
            put(QuickSettingsData.QS_ID_LOCATION, m);// 6:位置服务

            m = new Model(res.getString(R.string.qs_label_softap), R.drawable.ic_qs_sap_disable);
            put(QuickSettingsData.QS_ID_SOFTAP, m);// 7:热点

            m = new Model(res.getString(R.string.qs_label_super_saving), R.drawable.ic_qs_super_saving_power_disable);
            put(QuickSettingsData.QS_ID_SUPERSAVING_POWERMODE, m);// 14:超级省电模式

            m = new Model(res.getString(R.string.qs_label_viplist2), R.drawable.ic_qs_viplist_disable);
            put(QuickSettingsData.QS_ID_VIPLIST, m);// 12:VIP，骚扰拦截，免打扰

            m = new Model(res.getString(R.string.qs_label_magical), R.drawable.ic_qs_magical_off);
            put(QuickSettingsData.QS_ID_MAGICAL, m);// 44:魔音

            m = new Model(res.getString(R.string.qs_label_recording), R.drawable.ic_qs_screen_recording_disable);
            put(QuickSettingsData.QS_ID_RECORD_SCREEN, m);// 42:录屏

            m = new Model(res.getString(R.string.qs_label_controlcentor), R.drawable.ic_qs_control_center_disable_new);
            put(QuickSettingsData.QS_ID_CONTROL_CENTOR, m);// 43:控制中心

            m = new Model(res.getString(R.string.qs_label_cbutton), R.drawable.ic_qs_cbutton_disable);
            put(QuickSettingsData.QS_ID_CBUTTON, m);// 15:C Button

            m = new Model(res.getString(R.string.qs_label_meeting_mode), R.drawable.ic_qs_meeting_disable);
            put(QuickSettingsData.QS_ID_SCENEMODE, m);// 21:情景模式

            m = new Model(res.getString(R.string.qs_label_quick_pay), R.drawable.ic_qs_alipay);
            put(QuickSettingsData.QS_ID_QUICK_PAY, m);
            if (true) {//!CPCommRunMode.getDefault().isXianWangMode()//===modify by ty
                m = new Model(res.getString(R.string.accessibility_data_connection_4g), R.drawable.ic_qs_4g_disable);
                put(QuickSettingsData.QS_ID_4G, m);// 40:4G
            }
            if (hasNfcFeature()) {
                m = new Model(res.getString(R.string.qs_label_nfc), R.drawable.ic_qs_nfc_disable);
                put(QuickSettingsData.QS_ID_NFC, m);// 28:NFC
            }
            if (FeatureConfig.getBooleanValue(FeatureString.IS_SUPPORT_SCREEN_SETTINGS)) {
                m = new Model(res.getString(R.string.qs_label_white_black_patterns),
                        R.drawable.ic_ctrl_whiteblack_mode);
                put(QuickSettingsData.QS_ID_WHITE_BLACK, m);// 45:黑白模式
            }
            m = new Model(res.getString(R.string.qs_label_mutlwindow), R.drawable.ic_control_mutiwindow_disable);
            put(QuickSettingsData.QS_ID_MUTLWINDOW, m);// 27:智能多屏
            
            
            Log.d(TAG, "init map exhaust:" + (System.currentTimeMillis() - s) + "ms");
        }
    };

    private ArrayList<Map.Entry<Integer, Model>> mSqeItems = new ArrayList<Map.Entry<Integer, Model>>(
            mItemModes.entrySet());

    @SuppressLint("ClickableViewAccessibility")
    public CtrlCenterViewContainer(Context context) {
        super(context);
        mContext = context;
        mRes = context.getResources();
        marging = mRes.getDimensionPixelSize(R.dimen.gap_24);
        mInterval = mRes.getDimensionPixelSize(R.dimen.gap_5);
        mGap = mRes.getDimensionPixelSize(R.dimen.gap_3);
        mItemWidth = mRes.getDimensionPixelSize(R.dimen.item_width);
        mItemHeight = mRes.getDimensionPixelSize(R.dimen.item_height);
        mItemWidthB = mRes.getDimensionPixelSize(R.dimen.item_width_b);
        mItemHeightB = mRes.getDimensionPixelSize(R.dimen.item_height_b);

        mIconWidth = mRes.getDimensionPixelSize(R.dimen.icon_width);
        mIconWidthB = mRes.getDimensionPixelSize(R.dimen.icon_width_b);
        mDWidth = (mItemWidthB - mItemWidth) / 2;
        mDHeight = (mItemHeightB - mItemHeight) / 2;

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        Log.d(TAG, "size:" + mItemModes.size());

        setChildrenDrawingOrderEnabled(true);
        // refreshUI();

        setOnTouchListener(this);
        // setOnLongClickListener(this);
    }

    private void refreshUI() {
        removeAllViews();
        resortAllViews();
        addAllViews();
    }

    private void addAllViews() {
        for (Map.Entry<Integer, Model> e : mSqeItems) {
            Model m = e.getValue();
            if (-1 != m.id && -1 != m.pos) {
                CtrlCenterItemView child = new CtrlCenterItemView(mContext);
                addView(child.setModel(e.getValue()));
            }
        }
		for (int i = 0; i < (Utilities.isPrimaryUser() ? TOTAL_ALL : TOTAL_SHOW) - mDbItem.size(); i++) {
            addView(new CtrlCenterItemView(mContext).setVisiable(View.GONE));
        }
    }

    private static boolean hasNfcFeature() {
        if (pm == null) {
            pm = ActivityThread.getPackageManager();
        }
        if (pm == null) {
            Log.e(TAG, "Cannot get package manager, assuming no NFC feature");
            return false;
        }
        boolean hasNFC = false;
        try {
        	Method method = IPackageManager.class.getDeclaredMethod("hasSystemFeature");
			method.setAccessible(true);
			hasNFC = (boolean) method.invoke(pm);
			return hasNFC;
            //return pm.hasSystemFeature(PackageManager.FEATURE_NFC);//===modify by ty
        } catch (Exception e) {
            Log.e(TAG, "Package manager query failed, assuming no NFC feature", e);
            return false;
        }
    }

    private void resortAllViews() {
        Collections.sort(mSqeItems, new Comparator<Map.Entry<Integer, Model>>() {
            public int compare(Entry<Integer, Model> lhs, Entry<Integer, Model> rhs) {
                return lhs.getValue().pos - rhs.getValue().pos;
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        long s = System.currentTimeMillis();
        if (DEBUG) {
            Log.d(TAG, "onLayout: l=" + left + "; t=" + top + "; r=" + right + "; b=" + bottom);
        }
        final int cols = 4, block = 8, total = getChildCount();
        int l = marging, t = marging, r = l + mItemWidth, b = 0;
        for (int i = 0; i < total; i++) {
            if (i % block == 0) {// block
                l = marging;
                r = l + mItemWidth;
                t = b + marging;
                b = t + mItemHeight;
            } else if (i % cols == 0) {// col
                l = marging;
                r = l + mItemWidth;
                t = b + mGap;
                b = t + mItemHeight;
            } else {// row
                l = r + mInterval;
                r = l + mItemWidth;
            }
            if (-1 != mDragged && mDragged == i) {
                magnifyDraggedItem();
            } else {
                CtrlCenterItemView v = (CtrlCenterItemView) getChildAt(i);
                if (CTLR_CENTER_POS == i) {
                    View ctlCenter = v.findViewById(R.id.item_view);
                    int bg = mRes.getColor(R.color.ctrl_bg_color);
                    ctlCenter.setBackgroundColor(bg);
                    ctlCenter.findViewById(R.id.border_top).setVisibility(View.GONE);
                    ctlCenter.findViewById(R.id.border_bottom).setVisibility(View.GONE);
                    ctlCenter.findViewById(R.id.border_left).setVisibility(View.GONE);
                    ctlCenter.findViewById(R.id.border_right).setVisibility(View.GONE);

                    TextView mFont = (TextView) ctlCenter.findViewById(R.id.title);
                    mFont.setTextColor(mRes.getColor(R.color.ctrl_title_color));
                }
                v.setPoint(l, t).layout(l, t, r, b);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "onLayout exhaust:" + (System.currentTimeMillis() - s) + "ms");
        }
    }

    private float oldX, oldY;
    private int mDragged = -1, mTarget = -1, mBlank = -1;

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View view, MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onTouch: " + e.getAction());
        }
        float newX = e.getX(), newY = e.getY();

        switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
            oldX = newX;
            oldY = newY;
            onDownClick();
            break;
        case MotionEvent.ACTION_MOVE:
            if (-1 != mDragged && CTLR_CENTER_POS != mDragged) {
                CtrlCenterItemView draggedView = (CtrlCenterItemView) getChildAt(mDragged);
                int l = (int) (newX - mItemWidthB / 2), t = (int) (newY - mItemHeightB / 2);
                draggedView.layout(l, t, l + mItemWidthB, t + mItemHeightB);
                mTarget = getPosFromCoor(newX, newY);
                if (DEBUG) {
                    Log.d(TAG, "ACTION_MOVE: mDragged=" + mDragged + "; mBlank=" + mBlank + "; mTarget:" + mTarget);
                }
                boolean noCtrlCenter = CTLR_CENTER_POS != mTarget;
                boolean noBlankGap = -1 != mTarget;
                boolean noInvalidItem = mTarget < mSqeItems.size();
                boolean noRepeatAnim = mBlank != mTarget;
                if (noBlankGap && noCtrlCenter && noInvalidItem && noRepeatAnim) {
                    moveItemView(mBlank, mTarget);
                }
            }
            oldX = newX;
            oldY = newY;
            break;
        case MotionEvent.ACTION_UP:
            if (DEBUG) {
                Log.d(TAG, "ACTION_UP: mTarget=" + mTarget + "; mBlank=" + mBlank + "; mDragged=" + mDragged);
            }
            if (-1 != mDragged) {
                mTarget = getPosFromCoor(newX, newY);
                for (int i = 0; i < getChildCount(); i++) {
                    getChildAt(i).clearAnimation();
                }
                placeDraggedItem();
                refreshUI();
                if (DEBUG2) {
                    debugPrint();
                }
                // saveData();
            }
            mTarget = mDragged = mBlank = -1;
            if (DEBUG) {
                Log.d(TAG, "ACTION_UP: mTarget=" + mTarget + "; mBlank=" + mBlank + "; mDragged=" + mDragged);
            }
        }

        return true;
    }

    public void onDownClick() {
        mDragged = getPosFromCoor(oldX, oldY);
        if (-1 != mDragged && CTLR_CENTER_POS != mDragged) {
            mBlank = mDragged;
            if (DEBUG) {
                Log.d(TAG, "onLongClick:mBlank =" + mBlank);
            }
            magnifyDraggedItem();
        }
    }

    private void magnifyDraggedItem() {
        if (DEBUG) {
            Log.d(TAG, "magnifyDraggedItem:" + mDragged);
        }
        final CtrlCenterItemView draggedView = (CtrlCenterItemView) getChildAt(mDragged);
        draggedView.setLayoutParams(new LayoutParams(mItemWidthB, mItemHeightB));
        draggedView.setAlpha(0.7f);
        final int l = draggedView.model.p.x - mDWidth;
        final int t = draggedView.model.p.y - mDHeight;
        final int r = l + mItemWidthB;
        final int b = t + mItemHeightB;

        ImageView img = (ImageView) draggedView.findViewById(R.id.icon);
        LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(mIconWidthB, mIconWidthB);
        img.setLayoutParams(mParams);

        draggedView.layout(l, t, r, b);
        showItemShadow(draggedView);
    }

    private void showItemShadow(View view) {
        View innerView = (RelativeLayout) view.findViewById(R.id.item_view);
        innerView.setBackgroundResource(R.drawable.ic_qs_control_center_shadow);
        innerView.findViewById(R.id.border_top).setVisibility(View.INVISIBLE);;
        innerView.findViewById(R.id.border_bottom).setVisibility(View.INVISIBLE);
        innerView.findViewById(R.id.border_left).setVisibility(View.INVISIBLE);
        innerView.findViewById(R.id.border_right).setVisibility(View.INVISIBLE);
    }

    private void placeDraggedItem() {
        if (DEBUG) {
            Log.d(TAG, "resoreDraggedItem:" + mDragged);
        }
        if (-1 != mDragged) {
            final CtrlCenterItemView draggedView = (CtrlCenterItemView) getChildAt(mDragged);
            draggedView.setLayoutParams(new LayoutParams(mItemWidth, mItemHeight));
            final int l = draggedView.model.p.x;
            final int t = draggedView.model.p.y;
            final int r = l + mItemWidth;
            final int b = t + mItemHeight;

            ImageView img = (ImageView) draggedView.findViewById(R.id.icon);
            LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(mIconWidth, mIconWidth);
            img.setLayoutParams(mParams);

            draggedView.layout(l, t, r, b);
        }
    }

    private void moveItemView(int blank, int target) {
        if (DEBUG) {
            Log.d(TAG, "moveItemView :blank=" + blank + "; target=" + target);
        }
        if (DEBUG2) {
            debugPrint();
        }
        CtrlCenterItemView blankItem = (CtrlCenterItemView) lookForward(blank);
        if (DEBUG && blankItem != null) {
            Log.d(TAG, "$blank.p=" + blankItem.model.p + "; blank.pos=" + blankItem.model.pos + "; blank.title="
                    + blankItem.model.title);
        }
        Point tmp = null;
        if (blank < target) {
            if (DEBUG) {
                Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<");
            }
            for (int i = target; i >= blank + 1; i--) {
                if (DEBUG) {
                    Log.d(TAG, "i=" + i);
                }
                int oldPos = i;
                if (CTLR_CENTER_POS == oldPos) {
                    continue;
                }
                int newPos = oldPos - 1;
                if (CTLR_CENTER_POS == newPos) {
                    newPos--;
                }
                if (DEBUG) {
                    Log.d(TAG, "$oldPos=" + oldPos + "; newPos=" + newPos);
                }
                CtrlCenterItemView oldItem = (CtrlCenterItemView) lookForward(oldPos);
                CtrlCenterItemView newItem = (CtrlCenterItemView) lookForward(newPos);
                if (oldItem != null && newItem != null) {
                    if (i == target) {
                        tmp = oldItem.model.p;
                    }

                    animMoveItem(oldItem, newItem);

                    if (DEBUG && oldItem != null) {
                        Log.d(TAG, "$oldItem.p=" + oldItem.model.p + "; oldItem.pos=" + oldItem.model.pos
                                + "; oldItem.title=" + oldItem.model.title);
                    }
                    oldItem.model.p = newItem.model.p;
                    oldItem.model.pos = newItem.model.pos;
                    if (DEBUG) {
                        Log.d(TAG, "#oldItem.p=" + oldItem.model.p + "; oldItem.pos=" + oldItem.model.pos
                                + "; oldItem.title=" + oldItem.model.title);
                    }

                }
            }
        } else if (blank > target) {
            if (DEBUG) {
                Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>");
            }
            for (int i = target; i <= blank - 1; i++) {
                if (DEBUG) {
                    Log.d(TAG, "i=" + i);
                }
                int oldPos = i;
                if (CTLR_CENTER_POS == oldPos) {
                    continue;
                }
                int newPos = oldPos + 1;
                if (CTLR_CENTER_POS == newPos) {
                    newPos++;
                }
                CtrlCenterItemView oldItem = (CtrlCenterItemView) lookBackward(oldPos);
                CtrlCenterItemView newItem = (CtrlCenterItemView) lookBackward(newPos);

                if (oldItem != null && newItem != null) {
                    if (i == target) {
                        tmp = oldItem.model.p;
                    }

                    animMoveItem(oldItem, newItem);

                    if (DEBUG) {
                        Log.d(TAG, "$oldItem.p=" + oldItem.model.p + "; oldItem.pos=" + oldItem.model.pos
                                + "; oldItem.title=" + oldItem.model.title);
                    }
                    oldItem.model.p = newItem.model.p;
                    oldItem.model.pos = newItem.model.pos;
                    if (DEBUG) {
                        Log.d(TAG, "#oldItem.p=" + oldItem.model.p + "; oldItem.pos=" + oldItem.model.pos
                                + "; oldItem.title=" + oldItem.model.title);
                    }

                }
            }
        }

        if (blankItem != null) {
            blankItem.model.p = tmp;
            blankItem.model.pos = target;
            mBlank = target;
            if (DEBUG) {
                Log.d(TAG, "$blank.p=" + blankItem.model.p + "; blank.pos=" + blankItem.model.pos + "; blank.title="
                        + blankItem.model.title);
            }
        }

        resortAllViews();
        if (DEBUG) {
            debugPrint();
        }
    }

    private void debugPrint() {
        for (Map.Entry<Integer, Model> e : mSqeItems) {
            Model m = e.getValue();
            Log.d(TAG, "mSqeItems: pos=" + m.pos + "\tp=" + m.p + "\ttitle=" + m.title);
        }
    }

    private void animMoveItem(View oldView, View newView) {
        final CtrlCenterItemView oldItem = (CtrlCenterItemView) oldView;
        final CtrlCenterItemView newItem = (CtrlCenterItemView) newView;
        int deltaX = newItem.model.p.x - oldItem.model.p.x;
        int deltaY = newItem.model.p.y - oldItem.model.p.y;
        if (DEBUG) {
            Log.d(TAG, "deltaX:" + deltaX + ";deltaY:" + deltaY);
        }
        TranslateAnimation tran = new TranslateAnimation(0, deltaX, 0, deltaY);
        tran.setDuration(ANIM_T);
        tran.setFillEnabled(true);
        tran.setFillAfter(true);
        oldItem.clearAnimation();
        oldItem.startAnimation(tran);
        tran.setAnimationListener(new MyAnimationListener() {
            public void onAnimationEnd(Animation animation) {
                oldItem.clearAnimation();
                int l = oldItem.model.p.x;
                int t = oldItem.model.p.y;
                oldItem.layout(l, t, l + mItemWidth, t + mItemHeight);
            }
        });

    }

    private class MyAnimationListener implements AnimationListener {
        public void onAnimationStart(Animation animation) {}

        public void onAnimationRepeat(Animation animation) {}

        public void onAnimationEnd(Animation animation) {}
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (-1 == mDragged)
            return i;
        else if (i == childCount - 1)
            return mDragged;
        else if (i >= mDragged)
            return i + 1;
        return i;
    }

    private int getPosFromCoor(float x, float y) {
        long s = System.currentTimeMillis();
        int ret = -1;
        for (Map.Entry<Integer, Model> e : mSqeItems) {
            Model m = e.getValue();
            if (DEBUG2) {
                Log.d(TAG, "mSqeItems:m.pos=" + m.pos + "\t id=" + m.id + "\tl=" + m.p.x + "\tt=" + m.p.y + "\tr="
                        + (m.p.x + mItemWidth) + "\tb=" + (m.p.y + mItemHeight) + "\ttitle:" + m.title);
            }
            if (m.pos == -1) {
				continue;
			}
            if ((x > m.p.x && x < m.p.x + mItemWidth) && (y > m.p.y && y < m.p.y + mItemHeight)) {
                ret = m.pos;
                break;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "getPosFromCoor:" + ret + " exhaust:" + (System.currentTimeMillis() - s) + "ms");
        }
        return ret;
    }

    private CtrlCenterItemView lookForward(int index) {
        CtrlCenterItemView ret = null;
        int total = mSqeItems.size();
        for (int i = 0; i < total; i++) {
            Model m = mSqeItems.get(i).getValue();
            if (m.pos == index) {
                ret = m.v;
                break;
            }
        }
        return ret;
    }

    private CtrlCenterItemView lookBackward(int index) {
        CtrlCenterItemView ret = null;
        int total = mSqeItems.size();
        for (int i = total - 1; i >= 0; i--) {
            Model m = mSqeItems.get(i).getValue();
            if (m.pos == index) {
                ret = m.v;
                break;
            }
        }
        return ret;
    }

    // TOTAL_SHOW = 16;
    private ArrayList<DatabaseRecord> mDbItem;

    public void setData(ArrayList<DatabaseRecord> data) {
        mDbItem = data;
        if (DEBUG) {
            Collections.sort(data, new Comparator<DatabaseRecord>() {
                public int compare(DatabaseRecord lhs, DatabaseRecord rhs) {
                    return lhs.pos - rhs.pos;
                }
            });
            Log.e(TAG, "setData.size" + data.size());
        }

        for (DatabaseRecord record : data) {
            int key = record.id;
            Model m = mItemModes.get(key);// record.id == KEY
            if (null != m) {
                Log.e(TAG, "IN: DatabaseRecord: id=" + record.id + ";\tpos=" + record.pos + ";\ttitle=" + record.title);
                m.id = key;
                m.pos = record.pos;
                m.value = record.value;
                if (key == QuickSettingsData.QS_ID_CONTROL_CENTOR) {
                    m.id = QuickSettingsData.QS_ID_CONTROL_CENTOR;
                    if (m.pos > 15) {
                        m.pos = 15;
                    }
                    CTLR_CENTER_POS = m.pos;
                }
            }
        }

        resortAllViews();

        for (Map.Entry<Integer, Model> item : mSqeItems) {
            Log.e(TAG, "mSqeItems: Key=" + item.getKey() + "\tModel=" + item.getValue().toString());
        }

        addAllViews();
    }

    public void saveData() {
        ArrayList<DatabaseRecord> data = mDbItem;
        if (null != mDatabaseRecordCallback) {
            for (Map.Entry<Integer, Model> item : mSqeItems) {
                int key = item.getKey();
                Model m = item.getValue();
                if (-1 != m.id && -1 != m.pos) {
                    for (DatabaseRecord record : data) {
                        if (record.pos == m.pos) {
                            record.id = key;
                            record.title = m.title;
                            record.value = m.value;
                            Log.e(TAG, "DatabaseRecord: id=" + record.id + "\trecord.pos=" + record.pos
                                    + ";\tModel.value=" + m.toString());
                            break;
                        }
                    }
                }
            }
            Log.e(TAG, "saveData.size=" + data.size());
            if (null != mDatabaseRecordCallback) {
                mDatabaseRecordCallback.saveDate(data);
            }
        }
    }

    private CtrlCenterDataCallback mDatabaseRecordCallback;

    public static interface CtrlCenterDataCallback {
        public boolean saveDate(ArrayList<DatabaseRecord> data);
    }

    public void setCtrlCenterDataCallback(CtrlCenterDataCallback callback) {
        mDatabaseRecordCallback = callback;
    }

}
