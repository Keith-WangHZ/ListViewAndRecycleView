package com.android.systemui.quicksettings.bottom;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.android.internal.telephony.PhoneConstants;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * 2013.7.4.<br>
 * 灏佽绫荤敤浜庡瓨鍙栨暟鎹簱銆�<br>
 * 鍏ㄩ儴鍋氫簡寮傚父澶勭悊锛岃皟鐢ㄦ椂锛屾棤闇�鍐嶅仛澶勭悊銆�<br>
 * <p>
 * 娉細濡傛灉寮傚父锛屽垯閲囩敤鏈湴鐨剆hare椤�
 * @see java.lang.reflect.Method
 */
public class SimInfo {

    /** 鏃ュ織鏍囪瘑 */
    public static final String TAG = "SimInfo";
    /** 鍙犲姞ICCID浣滀负鍚嶇О鐨勫叧閿爜 */
    public static final String NAME = "name";
    /** 鍙犲姞ICCID浣滀负鍙风爜鐨勫叧閿爜 */
    public static final String NUMBER = "number";
    /** 鍙犲姞ICCID浣滀负鍥炬爣鐨勫叧閿爜 */
    public static final String ICON = "icon";
    /** 鍙犲姞ICCID浣滀负鍗℃Ы鐨勫叧閿爜 */
    public static final String SLOT = "slot";
    /** 鍙犲姞ICCID浣滀负绯诲垪缂栧彿鐨勫叧閿爜 */
    public static final String SIMID = "simId";
    /** 鍙犲姞绯诲垪缂栧彿浣滀负ICCID鐨勫叧閿爜 */
    public static final String GET_ICCID_BY_SIMID = "geticcidbysimid";

    public static final String SPLIT_FLAG = "_";

    public static final boolean BUG = false;
    public static final Uri CONTENT_URI = Uri.parse("content://telephony/siminfo");

    /** 鏍囪瘑涓烘墜鍔ㄤ慨鏀筍IM鍗＄殑鍚嶇О */
    public static final int MANU_UPDATE_NAME = 10;
    /** 鏍囪瘑涓烘墜鍔ㄤ慨鏀筍IM鍗＄殑鍙风爜 */
    public static final int MANU_UPDATE_NUMBER = 20;
    /** 鏍囪瘑涓烘墜鍔ㄤ慨鏀筍IM鍗＄殑鍥炬爣 */
    public static final int MANU_UPDATE_ICON = 30;

    public static final int NAME_SOURCE_UNDEFINDED = SubscriptionManager.NAME_SOURCE_UNDEFINDED;
    public static final int NAME_SOURCE_DEFAULT_SOURCE = SubscriptionManager.NAME_SOURCE_DEFAULT_SOURCE;
    public static final int NAME_SOURCE_SIM_SOURCE = SubscriptionManager.NAME_SOURCE_SIM_SOURCE;
    public static final int NAME_SOURCE_USER_INPUT = SubscriptionManager.NAME_SOURCE_USER_INPUT;

//    public static final int ACTIVE = SubscriptionManager.ACTIVE;
//    public static final int INACTIVE = SubscriptionManager.INACTIVE;
    // public static final String _ID = SubscriptionManager._ID;
    // public static final long INVALID_SUB_ID =
    // SubscriptionManager.INVALID_SUB_ID;
//    public static final String SUB_STATE = SubscriptionManager.SUB_STATE;
    public static final int CALL_STATE_IDLE = TelephonyManager.CALL_STATE_IDLE;

    /** 榛樿鍙戦�佷俊鎭殑鏍囪瘑鐮� */
    public static final String KEY_SETTING_MMS = "card_to_send_mms";

    /** 榛樿鎷ㄥ彿鐨勬爣璇嗙爜 */
    public static final String KEY_SETTING_DIAL = "card_to_dial";
    /** 閫氳瘽璁板綍鍥炴嫧鐨勬爣璇嗙爜 */
    public static final String KEY_SETTING_CALL_RECORD = "call_records_of_automatic_network_selection_callback";

    public static boolean isOpenBackDoor = false;
    /** 鏍囪瘑鏄惁寮�鍚數淇℃墜鏈虹殑鍗℃Ы涓�鏀寔G鍗℃敞鍐岀綉缁� */
    public static final String GSM_SIM_REGISTER_NETWORK = "service_gsm_switch";

    private String iccId = "";
    private String cardName = "";
    private String number = "";
    private int iconId = 0;
    private int colorId = 0;
    private int slot = -1;
    private int nameSource = 1;
    private int dataRoaming = 0;
    private String carrierName = "";

    private int subId = -100;
    private String mccmnc = "";
    private int mcc = 0;
    private int mnc = 0;
    private int mStatus = 1;
    private int mNwMode = 8;

    private boolean isNoCard = true;
    private boolean isShowIconInfo = true;
    private boolean isThisDefaultDial = false;
    private boolean isThisDefaultMms = false;
    private boolean isThisDefaultData = false;
    private boolean isThisDataRoamControl = false;
    private boolean isValidSIM = true;

    /** 涓婄綉鏁版嵁鐨勬爣璇� */
    public static final int FLAG_SETTING_DATA = 1;

    /** 鍙戦�佷俊鎭爣璇� */
    public static final int FLAG_SEND_MMS = 2;

    /** 榛樿鎷ㄥ彿鏍囪瘑 */
    public static final int FLAG_SETTING_DIAL = 3;

    public static final int FLAG_SETTING_DATAROAM = 4;

    public static final int FLAG_REGISTER_STATUS = 5;

    /**
     * @return 鍗＄殑ICCID
     */
    public String getIccId() {
        return iccId;
    }

    /**
     * @param iccId
     *            鍗＄殑ICCID
     */
    public void setIccId(String iccId) {
        this.iccId = iccId;
    }

    /**
     * @return 鍗＄殑鍙风爜
     */
    public String getNumber() {
        return number;
    }

    /**
     * @param number
     *            鍗＄殑鍙风爜
     */
    public void setNumber(String number) {
        this.number = number;
    }

    public int getIconId() {
        return iconId < 0 ? slot : iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public int getColorId() {
        return colorId < 0 ? slot : colorId;
    }

    public void setColorId(int colorId) {
        this.colorId = colorId;
    }

    /**
     * @return 鍗℃ЫID
     */
    public int getSlot() {
        return slot;
    }

    /**
     * @param slot
     *            鍗℃ЫID
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * @return 鍗″悕绉�
     */
    public String getDisplayName() {
        return cardName;
    }

    public String getName() {
        return cardName;
    }

    /**
     * @param mSimId
     *            鍗″悕绉�
     */
    public void setDisplayName(String cardName) {
        this.cardName = cardName;
    }

    public String getText() {
        return cardName;
    }

    /**
     * @param mSimId
     *            鍗″悕绉�
     */
    public void setText(String text) {
        this.cardName = text;
    }

    /**
     * @return the nameSource
     */
    public int getNameSource() {
        return nameSource;
    }

    /**
     * @param nameSource
     *            the nameSource to set
     */
    public void setNameSource(int nameSource) {
        this.nameSource = nameSource;
    }

    /**
     * @return the dataRoaming
     */
    public int getDataRoaming() {
        return dataRoaming;
    }

    public String getMccmnc() {
        return mccmnc;
    }

    public void setMccmnc(String mccmnc) {
        this.mccmnc = mccmnc;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public int getmNwMode() {
        return mNwMode;
    }

    public void setmNwMode(int mNwMode) {
        this.mNwMode = mNwMode;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carriorName) {
        this.carrierName = carriorName;
    }

    public boolean isShowIconInfo() {
        return isShowIconInfo;
    }

    public void setShowIconInfo(boolean isShowIconInfo) {
        this.isShowIconInfo = isShowIconInfo;
    }

    public boolean isThisDefaultDial() {
        return isThisDefaultDial;
    }

    public void setThisDefaultDial(boolean isThisDefaultDial) {
        this.isThisDefaultDial = isThisDefaultDial;
    }

    public boolean isThisDefaultMms() {
        return isThisDefaultMms;
    }

    public void setThisDefaultMms(boolean isThisDefaultMms) {
        this.isThisDefaultMms = isThisDefaultMms;
    }

    public boolean isThisDefaultData() {
        return isThisDefaultData;
    }

    public void setThisDefaultData(boolean isThisDefaultData) {
        this.isThisDefaultData = isThisDefaultData;
    }

    public boolean isThisDataRoamControl() {
        return isThisDataRoamControl;
    }

    public void setThisDataRoamControl(boolean isThisDataRoamControl) {
        this.isThisDataRoamControl = isThisDataRoamControl;
    }

    public boolean isRegisted() {
        return (mStatus == 1);
    }

    public void setNoCard(boolean isNoCard) {
        this.isNoCard = isNoCard;
    }

    /** 鏈夊崱鐨勫墠鎻愪笅锛屾墠鑳戒娇鐢ㄨ鏂规硶锛屽垽瀹氭槸鍚︿负鏈夋晥鍗★細true涓烘湁鏁堝崱 */
    public boolean isValidSIM() {
        return (isOpenBackDoor || isValidSIM);
    }

    public void setValidSIM(boolean isValidSIM) {
        this.isValidSIM = isValidSIM;
    }

    /**
     * @param dataRoaming
     *            the dataRoaming to set
     */
    public void setDataRoaming(int dataRoaming) {
        this.dataRoaming = dataRoaming;
    }

    public int getSubId() {
        return subId;
    }

    public void setSubId(int subId) {
        this.subId = subId;
    }

    public int getmStatus() {
        return mStatus;
    }

    public boolean isRegStatus() {
        return (mStatus == 1);
    }

    public void setmStatus(int mStatus) {
        this.mStatus = mStatus;
    }

    public boolean isNoCard() {
        return isNoCard;
    }

    public boolean isChecked(int flag) {
        boolean isChecked = false;
        switch (flag) {
            case FLAG_SETTING_DATA:
                isChecked = isThisDefaultData;
                break;
            case FLAG_SEND_MMS:
                isChecked = isThisDefaultMms;
                break;
            case FLAG_SETTING_DIAL:
                isChecked = isThisDefaultDial;
                break;
            case FLAG_SETTING_DATAROAM:
                isChecked = isThisDataRoamControl;
                break;
            case FLAG_REGISTER_STATUS:
                isChecked = isRegisted();
                break;
        }
        return isChecked;
    }

    /** 鏋勯�犳柟娉� */
    public SimInfo(String text) {
        this.iccId = null;
        this.cardName = text;
        this.number = null;
        this.iconId = -1;
        this.colorId = -1;
        this.slot = -1;
        this.carrierName = null;
        this.nameSource = -1;
        this.dataRoaming = -1;
        this.subId = -1;
        this.mccmnc = null;
        this.mcc = -1;
        this.mnc = -1;
        this.mStatus = 1;// need regist
        this.mNwMode = -1;
        this.isNoCard = false;// need has sim card
        this.isShowIconInfo = false;
        this.isThisDefaultDial = false;
        this.isThisDefaultMms = false;
        this.isThisDefaultData = false;
        this.isThisDataRoamControl = false;
    }

    /** 鏋勯�犳柟娉� */
    public SimInfo() {
        this.iccId = null;
        this.cardName = null;
        this.number = null;
        this.iconId = -1;
        this.colorId = -1;
        this.slot = -1;
        this.carrierName = null;
        this.nameSource = -1;
        this.dataRoaming = -1;
        this.subId = -1;
        this.mccmnc = null;
        this.mcc = -1;
        this.mnc = -1;
        this.mStatus = 0;
        this.mNwMode = -1;
        this.isNoCard = false;
        this.isShowIconInfo = false;
        this.isThisDefaultDial = false;
        this.isThisDefaultMms = false;
        this.isThisDefaultData = false;
        this.isThisDataRoamControl = false;
    }

    /** 鏋勯�犳柟娉� */
    public SimInfo(int mSlot) {
        this.iccId = "";
        this.cardName = "";
        this.number = "";
        this.iconId = slot == 0 ? 0 : 1;
        this.colorId = slot == 0 ? 0 : 1;
        this.slot = mSlot;
        this.carrierName = "";
        this.nameSource = 1;
        this.dataRoaming = 1;
        this.subId = -1;
        this.mccmnc = "";
        this.mcc = 0;
        this.mnc = 0;
        this.mStatus = 1;
        this.mNwMode = 8;
        this.isNoCard = true;
        this.isShowIconInfo = true;
        this.isThisDefaultDial = false;
        this.isThisDefaultMms = false;
        this.isThisDefaultData = false;
        this.isThisDataRoamControl = false;
    }

    /** 鏋勯�犳柟娉� */
    public SimInfo(SimInfo si) {
        this.iccId = si.iccId;
        this.cardName = si.cardName;
        this.number = si.number;
        this.iconId = si.iconId;
        this.colorId = si.colorId;
        this.slot = si.slot;
        this.carrierName = si.carrierName;
        this.nameSource = si.nameSource;
        this.dataRoaming = si.dataRoaming;
        this.subId = si.subId;
        this.mccmnc = si.mccmnc;
        this.mcc = si.mcc;
        this.mnc = si.mnc;
        this.mStatus = si.mStatus;
        this.mNwMode = si.mNwMode;
        this.isNoCard = si.isNoCard;
        this.isShowIconInfo = si.isShowIconInfo;
        this.isThisDefaultDial = si.isThisDefaultDial;
        this.isThisDefaultMms = si.isThisDefaultMms;
        this.isThisDefaultData = si.isThisDefaultData;
        this.isThisDataRoamControl = si.isThisDataRoamControl;
    }

    /** 鏋勯�犳柟娉� */
    public SimInfo(SubscriptionInfo mSubscriptionInfo) {
        this.iccId = mSubscriptionInfo.getIccId();
        this.cardName = (String) mSubscriptionInfo.getDisplayName();
        this.number = mSubscriptionInfo.getNumber();
        this.iconId = 1;
        this.colorId = mSubscriptionInfo.getIconTint();
        this.slot = mSubscriptionInfo.getSimSlotIndex();
        this.carrierName = (String) mSubscriptionInfo.getCarrierName();
        this.nameSource = mSubscriptionInfo.getNameSource();
        this.dataRoaming = mSubscriptionInfo.getDataRoaming();
        this.subId = mSubscriptionInfo.getSubscriptionId();
        this.mcc = mSubscriptionInfo.getMcc();// 涓変綅
        this.mnc = mSubscriptionInfo.getMnc();// 涓や綅
//        this.mStatus = mSubscriptionInfo.mStatus;
//        this.mNwMode = mSubscriptionInfo.mNwMode;
        this.isNoCard = false;
        this.isShowIconInfo = true;
        this.isThisDefaultDial = false;
        this.isThisDefaultMms = false;
        this.isThisDefaultData = false;
        this.isThisDataRoamControl = mSubscriptionInfo.getDataRoaming() == 1;

        mccmnc = "";
        if (mcc >= 100) {
            mccmnc = String.valueOf(mcc);
        } else if (mcc >= 10) {
            mccmnc = "0" + mcc;
        } else if (mcc > 0) {
            mccmnc = "00" + mcc;
        } else {
            mccmnc = "";
        }
        if (!TextUtils.isEmpty(mccmnc)) {
            if (mnc >= 10) {
                mccmnc += mnc;
            } else if (mnc >= 0) {
                mccmnc += "0" + mnc;
            }
        }
    }

    @Override
    public String toString() {
        return "isValidSIM: " + isValidSIM + "; isNoCard: " + isNoCard + "; iccId: " + iccId + "; cardName: "
                + cardName + "; number: " + number + "; iconId: " + iconId + "; colorId:" + colorId + "; slot: " + slot
                + "; subId: " + subId + "; mccmnc: " + mccmnc + "; mcc:" + mcc + "; mnc: " + mnc + "; mStatus: "
                + mStatus + "; mNwMode: " + mNwMode + "; isShowIconInfo:" + isShowIconInfo + "; isThisDefaultDial:"
                + isThisDefaultDial + "; isThisDefaultMms:" + isThisDefaultMms + "; isThisDefaultData:"
                + isThisDefaultData + "; isThisDataRoamControl:" + isThisDataRoamControl;
    }

    /*** 闈欐�佹柟娉曞尯鍩� ***/
    /**
     * 2015.3.12.<br>
     * 鑾峰彇鎵�鏈夊崱妲界殑鍗′俊鎭�
     * @param context
     */
    public static List<SimInfo> getActiveSimInfoList(Context context) {
        List<SimInfo> mSimInfo = null;
        SimInfo[] mSimInfoArrays = null;

        List<SubscriptionInfo> mSubInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (mSubInfoList != null && mSubInfoList.size() > 0) {
            int size = mSubInfoList.size();
            // Config.d(TAG, "getActiveSimInfoList sim number: " + size);
            mSimInfoArrays = new SimInfo[size];
            for (int i = 0; i < size; i++) {
                mSimInfoArrays[i] = new SimInfo(mSubInfoList.get(i));
            }
        }
        if (mSimInfoArrays != null && mSimInfoArrays.length > 0) {
            mSimInfo = new ArrayList<SimInfo>();
            for (SimInfo si : mSimInfoArrays) {
                // Config.d(TAG, "getActiveSimInfoList siminfo: " + si);
                if (si == null) {
                    continue;
                }
                if (si.getSlot() >= mSimInfo.size()) {
                    mSimInfo.add(si);
                } else {
                    mSimInfo.add(si.getSlot(), si);
                }
            }
        }
        return mSimInfo;
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁鍗℃Ы鑾峰彇鍗′俊鎭�
     * @param context
     * @param slotId
     *            鍗℃ЫID
     */
    public static SimInfo getActiveSimInfoBySlot(Context context, int slotId) {
        List<SimInfo> mSimInfo = getActiveSimInfoList(context);
        if (mSimInfo != null) {
            for (SimInfo m : mSimInfo) {
                if (slotId == m.getSlot()) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * 2015.3.12.<br>
     * 鏀寔鐨凷IM鍗℃暟鐩�
     * @param context
     */
    public static int getSimCount(Context context) {
        return TelephonyManager.from(context).getSimCount();
    }

    /**
     * 2015.3.12.<br>
     * 鏀寔鐨凱hone鏁扮洰
     */
    public static int getPhoneCount() {
        return TelephonyManager.getDefault().getPhoneCount();
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId鑾峰彇call鐘舵��
     * @param subId
     */
    public static int getCallState(int subId) {
        return TelephonyManager.getDefault().getCallState(subId);
    }

    /**
     * 2015.3.12.<br>
     * 璁剧疆鏁版嵁寮�鍏崇姸鎬�
     * @param context
     * @param enable
     *            true寮�鍚紱false鍏抽棴
     * @param slotNum
     *            鏀寔鐨勫崱妲芥暟鐩�
     */
    public static void setMobileDataEnabled(Context context, boolean enable, int slotNum) {
        TelephonyManager.from(context).setDataEnabled(enable);
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.MOBILE_DATA, (enable ? 1 : 0));
        for (int i = 0; i < slotNum; i++) {
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.MOBILE_DATA + i, (enable ? 1 : 0));
        }
    }

    /**
     * 2015.3.12.<br>
     * 鑾峰彇鏁版嵁寮�鍏崇姸鎬�
     * @param context
     */
    public static boolean isMobileDataEnabled(Context context) {
        if (TelephonyManager.from(context).getPhoneCount() > 1) { // 鍙屽崱璇绘暟鎹簱
            return (android.provider.Settings.Global.getInt(context.getContentResolver(),
                    android.provider.Settings.Global.MOBILE_DATA + PhoneConstants.SIM_ID_1, 0) != 0)
                    && (android.provider.Settings.Global.getInt(context.getContentResolver(),
                            android.provider.Settings.Global.MOBILE_DATA + PhoneConstants.SIM_ID_2, 0) != 0);
        } else { // 鍗曞崱璋冩帴鍙�
            return TelephonyManager.getDefault().getDataEnabled();
        }
    }

    /**
     * 2015.3.12.<br>
     * 鑾峰彇榛樿鐨勬暟鎹富鍗�
     * @param context
     */
    public static int getDefaultDataSubId(Context context) {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    /**
     * 2015.3.12.<br>
     * 鑾峰彇榛樿鐨勬嫧鍙蜂富鍗�
     * @param context
     */
    public static int getDefaultDialSubId(Context context) {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    /**
     * 2015.3.12.<br>
     * 鑾峰彇榛樿鐨勫彂閫佷俊鎭富鍗�
     * @param context
     */
    public static int getDefaultMmsSubId(Context context) {
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁PhoneId鑾峰彇褰撳墠婕父鐘舵�佸��
     * @param context
     * @param phoneId
     *            phoneId
     */
    public static int getDefaultDataRoaming(Context context, int phoneSubId) {
        return Settings.Global.getInt(context.getContentResolver(), (Settings.Global.DATA_ROAMING + phoneSubId), 0);
    }

    /**
     * 2015.3.12.<br>
     * 璁剧疆榛樿鏁版嵁鐨剆ubId
     * @param context
     * @param subId
     */
    public static void setDefaultDataBySubId(Context context, int subId) {
        SubscriptionManager.from(context).setDefaultDataSubId(subId);
    }

    /**
     * 2015.3.12.<br>
     * 璁剧疆榛樿鎷ㄦ墦鐢佃瘽鐨剆ubId
     * @param context
     * @param subId
     */
    public static void setDefaultDialBySubId(Context context, int subId) {
        int value = 0;
      if (subId == -1) {
      value = -1;
            // 鍏煎骞冲彴澶勭悊閫昏緫
            Settings.System.putInt(context.getContentResolver(), KEY_SETTING_DIAL, 0);
            Settings.System.putInt(context.getContentResolver(), KEY_SETTING_CALL_RECORD, 1);
        } else if (subId == 0) {
            value = 0;
            // 鍏煎骞冲彴澶勭悊閫昏緫
            Settings.System.putInt(context.getContentResolver(), KEY_SETTING_DIAL, value);
            Settings.System.putInt(context.getContentResolver(), KEY_SETTING_CALL_RECORD, 0);
        } else {
            // 鍏煎骞冲彴澶勭悊閫昏緫
            value = SubscriptionManager.getSlotId(subId) + 1;
            Settings.System.putInt(context.getContentResolver(), KEY_SETTING_DIAL, value);
            Settings.System.putInt(context.getContentResolver(), KEY_SETTING_CALL_RECORD, 0);
        }
        SubscriptionManager.from(context).setDefaultVoiceSubId(subId);
    }

    /**
     * 2015.3.12.<br>
     * 璁剧疆榛樿鍙戦�佷俊鎭殑subId
     * @param context
     * @param subId
     */
    public static void setDefaultMmsBySubId(Context context, int subId) {
        int value = 0;
        if (subId == -1) {
            value = 1;
        } else if (subId == 0) {
            value = 0;
        } else {
            value = SubscriptionManager.getSlotId(subId) + 2;
        }
        Settings.System.putInt(context.getContentResolver(), KEY_SETTING_MMS, value);
        SubscriptionManager.from(context).setDefaultSmsSubId(subId);// 鍏煎鍘熺敓銆備繚鎸佷竴鑷存�э紝鍐欒鍊笺��
    }

    /**
     * 2015.3.12.<br>
     * 璁剧疆PhoneId鐨勬极娓哥姸鎬�
     * @param context
     * @param phoneId
     * @param roam
     *            婕父鐘舵�佸��
     */
    public static void setDefaultDataRoaming(Context context, int phoneSubId, int roam) {
        Settings.Global.putInt(context.getContentResolver(), (Settings.Global.DATA_ROAMING + phoneSubId), roam);
        SubscriptionManager.from(context).setDataRoaming(roam, phoneSubId);
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId鑾峰彇缃戠粶绔痮perator
     * @param context
     * @param subId
     */
    public static String getNetworkOperatorForSubscription(Context context, int subId) {
        return TelephonyManager.from(context).getNetworkOperatorName(subId);//===modify by ty
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId寮�鍚叾缃戠粶
     * @param subId
     */
//    public static void activateSubId(int subId) {
//        SubscriptionManager.activateSubId(subId);
//    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId鍏抽棴鍏剁綉缁�
     * @param subId
     */
//    public static void deactivateSubId(int subId) {
//        SubscriptionManager.deactivateSubId(subId);
//    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId鑾峰彇PhoneId
     * @param subId
     */
    public static int getPhoneId(int subId) {
        return SubscriptionManager.getPhoneId(subId);
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId鑾峰彇SlotId
     * @param subId
     */
    public static int getSlotId(int subId) {
        return SubscriptionManager.getSlotId(subId);
    }

    /**
     * 2015.3.12.<br>
     * 鍗￠檺鍒舵帶鍒�
     * @param context
     */
    public static int getBackdoorFlag(Context context) {
        return Settings.System.getInt(context.getContentResolver(), SimInfo.GSM_SIM_REGISTER_NETWORK, 0);
    }

    /**
     * 2015.3.12.<br>
     * 璁剧疆鍗￠檺鍒舵帶鍒�
     * @param context
     * @param value
     */
    public static void setBackdoorFlag(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), SimInfo.GSM_SIM_REGISTER_NETWORK, value);
    }

    /**
     * 2015.3.12.<br>
     * 鏍规嵁subId鑾峰彇涓诲崱缃戠粶
     * @param context
     * @param sub
     */
    public static int getPreferredNetwork(Context context, int sub) {
        int nwMode = -1;
        try {
            nwMode = TelephonyManager.getIntAtIndex(context.getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE, sub);
        } catch (Exception snfe) {
        }
        return nwMode;
    }

    /**
     * 2013.7.6.<br>
     * 鏍规嵁绯诲垪缂栧彿鏇存柊鍙风爜淇℃伅<br>
     * 璇ユ柟娉曞厛浣跨敤鍙嶅皠鑾峰彇鍒版暟鎹簱涓唴瀹癸紝濡傛灉鏁版嵁搴撹鍙栧け璐ュ垯浣跨敤鏈湴瀛樺偍
     * @param context
     *            context
     * @param defaultNumber
     *            defaultNumber
     * @param mSIMId
     *            mSIMId
     */
    public static int updateNumber(Context context, String number, int subId) {
        int result = -1;
        try {
            result = SubscriptionManager.from(context).setDisplayNumber(number, subId);
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 2013.7.6.<br>
     * 鏍规嵁绯诲垪缂栧彿鏇存柊鍚嶇О淇℃伅<br>
     * 璇ユ柟娉曞厛浣跨敤鍙嶅皠鑾峰彇鍒版暟鎹簱涓唴瀹癸紝濡傛灉鏁版嵁搴撹鍙栧け璐ュ垯浣跨敤鏈湴瀛樺偍
     * @param context
     *            context
     * @param defaultName
     *            defaultName
     * @param mSIMId
     *            mSIMId
     * @param isAutoName
     *            鍖哄垎绯荤粺鑷姩鍛藉悕涓庢墜鍔ㄧ紪杈戯細true涓鸿嚜鍔ㄥ懡鍚嶏紱false涓烘墜鍔�
     */
    public static int updateDisplayName(Context context, String displayName, int subId) {
        int result = 1;
        try {
            SubscriptionManager.from(context).setDisplayName(displayName, subId);
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 2013.7.6.<br>
     * 鏍规嵁绯诲垪缂栧彿鏇存柊鍥炬爣淇℃伅<br>
     * 璇ユ柟娉曞厛浣跨敤鍙嶅皠鑾峰彇鍒版暟鎹簱涓唴瀹癸紝濡傛灉鏁版嵁搴撹鍙栧け璐ュ垯浣跨敤鏈湴瀛樺偍
     * @param context
     *            context
     * @param iconId
     *            iconId
     * @param mSIMId
     *            mSIMId
     */
    public static int updateColorId(Context context, int color, int subId) {
        int result = -1;
        try {
            result = SubscriptionManager.from(context).setIconTint(color, subId);
        } catch (Exception e) {
        }
        return result;
    }

    public static int updateIconId(Context context, int icon, int subId) {
        int result = -1;
        try {
            SubscriptionManager mSubscriptionManager = SubscriptionManager.from(context);
            Class<?> classSubscriptionManager = Class.forName("android.telephony.SubscriptionManager");
            Method setIconMethod = classSubscriptionManager.getMethod("setIcon", int.class, int.class);
            result = (int) setIconMethod.invoke(mSubscriptionManager, icon, subId);
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 2013.7.6.<br>
     * 鏍规嵁绯诲垪缂栧彿鏇存柊鍥炬爣淇℃伅<br>
     * 璇ユ柟娉曞厛浣跨敤鍙嶅皠鑾峰彇鍒版暟鎹簱涓唴瀹癸紝濡傛灉鏁版嵁搴撹鍙栧け璐ュ垯浣跨敤鏈湴瀛樺偍
     * @param context
     *            context
     * @param iconId
     *            iconId
     * @param mSIMId
     *            mSIMId
     */
    public static int updateDisplayNameEx(Context context, String displayName, int subId, long nameSource) {
        int result = 1;
        try {
            SubscriptionManager.from(context).setDisplayName(displayName, subId, nameSource);
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 2013.7.6.<br>
     * 鏍规嵁绯诲垪缂栧彿鏇存柊鍥炬爣淇℃伅<br>
     * 璇ユ柟娉曞厛浣跨敤鍙嶅皠鑾峰彇鍒版暟鎹簱涓唴瀹癸紝濡傛灉鏁版嵁搴撹鍙栧け璐ュ垯浣跨敤鏈湴瀛樺偍
     * @param context
     *            context
     * @param iconId
     *            iconId
     * @param mSIMId
     *            mSIMId
     */
    public static int updateDispalyNumberFormat(Context context, int format, int subId) {
        int result = 1;
        try {
            // SubscriptionManager.from(context).setDisplayNumberFormat(format,
            // subId);
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 2013.7.6.<br>
     * 鏍规嵁绯诲垪缂栧彿鏇存柊鍥炬爣淇℃伅<br>
     * 璇ユ柟娉曞厛浣跨敤鍙嶅皠鑾峰彇鍒版暟鎹簱涓唴瀹癸紝濡傛灉鏁版嵁搴撹鍙栧け璐ュ垯浣跨敤鏈湴瀛樺偍
     * @param context
     *            context
     * @param iconId
     *            iconId
     * @param mSIMId
     *            mSIMId
     */
    public static int updateDataRoaming(Context context, int roaming, int subId) {
        int result = 1;
        try {
            SubscriptionManager.from(context).setDataRoaming(roaming, subId);
        } catch (Exception e) {
        }
        return result;
    }
}
