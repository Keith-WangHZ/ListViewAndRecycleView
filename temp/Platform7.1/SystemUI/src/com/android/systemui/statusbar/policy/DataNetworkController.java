   package com.android.systemui.statusbar.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.CPDataConnSettingUtils;
import com.android.systemui.statusbar.phone.YulongConfig;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Carriers;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class DataNetworkController implements SignalCallback, NetworkSignalChangedCallback{

	private static final String TAG = "DataNetworkController";
    public static final String CONNECTIVITY_ACTION_SHORT = "android.yulong.net.SHORT_CONNECTION";
    public static final String EXTRA_IS_SHORT_WORKING = "extra_is_short_working";
	public static final String REQUEST_DATA_NETWORK_RESULT = "yulong.intent.action.REQUEST_DATA_NETWORK_RESULT";
	public static final String ACTION_FINISH_SELECT_NET_OPERATION = "yulong.intent.action.SELECT_NET_OPERATION";
	private Context mContext;

	// 褰曢噽锟�
	private DefaultDataNetworkObserver mDefaultDataNetworkObserver;
	private ApnListObserver mApnListObserver;
	private CurAnpObserver mCurApnObserver;
	private CurAnpObserver mCurApnObserver2;

	// 鍗ゆ埉纰屼綇pn绉佹埉韫�
	private ArrayList<ApnData> mAllApnList = new ArrayList<DataNetworkController.ApnData>();
	private ApnData mCurApnList[] = { new ApnData(), new ApnData() };
	private static final String Carriers_DEFAULT = "isdefault";
	// 绌磋楹撴憞pn绉佹埉鑿橀姙鎯久风鎷疯瀻
	private String mProjection[] = { 
			Telephony.Carriers._ID,
			Telephony.Carriers.NAME, 
			Telephony.Carriers.APN,
			Telephony.Carriers.TYPE,
			Telephony.Carriers.NUMERIC,
//			Carriers_DEFAULT,
			Telephony.Carriers.BEARER};

	// 纰屽崵鍓嶄娇瑭㈤┐绡撴嫝鍗�-1,0鎷㈠崲1 (锠熸晥鎷㈠崲椹寸瘬1鎷㈠崲 椹寸瘬2)
	private int mCurNetwork = -1;
	private NetworkStatNotify mNotifyQuickSetting;
	private MSimNetworkControllerImpl mNetworkController;
	// 鍙岄┐绡撶姸鎬�
	private boolean mCardIsUsing[] = { false, false };

	private boolean mIsAirplaneMode = false;
	// 鐙畩璁楃鐩檵楣胯磩楹撴��
	private boolean mIsNetworkOn = false;		
	// 琚涚ずAPN骞曢瞾鑻归箍钘漷ype
	private boolean mIsShowName = true;
	// 绂鸿疁琚涚ず纰岀洰闄嬮箍璐勯簱鎬�
	private boolean mIsNetworkOnInUi = false;
	private boolean mIsRoaming = false;
	private String mApnName = "";
    public boolean mIsMmsSending = false;
    private boolean mIs4gNet[]={true,true};//add by wz 鑼為湶琚忔瑺鎭ｎ剤鑽鑳冭幗鎷㈠崲铏忕钘�4G鑳冭幗鑾靛崵妾�GMTP
	private ConnectivityManager mConnMgr;
	public static final int MSG_REFRESHAPN = 1;
	Handler mDataChangedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			refreshApn();			
		}
	};
	private void postStatusBarNetworkChanged() {
		mDataChangedHandler.removeMessages(0);
		mDataChangedHandler.sendEmptyMessageDelayed(0, 250);
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "mBroadcastReceiver action = " + action);
			if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
				boolean isAirplaneModeOn = intent.getBooleanExtra("state",
						false);
				LogHelper.sd(TAG, "mIsAirplaneMode = " + mIsAirplaneMode
						+ " newState = " + isAirplaneModeOn);
				if (mIsAirplaneMode != isAirplaneModeOn) {
					mIsAirplaneMode = isAirplaneModeOn;
					refreshApn();
				}
			} else if (action.equals(REQUEST_DATA_NETWORK_RESULT)) {
				mCurApnObserver.mCurApnIsDirty = true;
				mCurApnObserver2.mCurApnIsDirty  = true;
				refreshApn();
			} else if (action.equals(Intent.ACTION_LOCALE_CHANGED)){
			    String language = Locale.getDefault().getLanguage();
			    if (language.equalsIgnoreCase("zh")){
			        mIsShowName = true;
			    } else {
			        mIsShowName = false;
			    }
			    LogHelper.sd(TAG, "language = " + language + " mIsShowName = " + mIsShowName);
			    
				refreshApn();
			} else if(action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)){
				mCurApnObserver.startObserving();
				mCurApnObserver2.startObserving();
				refreshApn();
			}else if (action.equals(ACTION_FINISH_SELECT_NET_OPERATION)) {
				refreshApn();
			}
		}
	};
	
	private static DataNetworkController sInstance;
	private static DataNetworkController sInstanceSecure;
	public static DataNetworkController getInstance(Context context, Boolean bPrimary){
		if(sInstance == null)
			sInstance = new DataNetworkController(context);
		if(sInstanceSecure == null)
			sInstanceSecure = new DataNetworkController(context);
		if(bPrimary){
			return sInstanceSecure;
		}else{
		return sInstance;
		}
	}
	private DataNetworkController(Context context) {
		mContext = context;
		mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		Handler handler = new Handler();
		mDefaultDataNetworkObserver = new DefaultDataNetworkObserver(handler);
		mDefaultDataNetworkObserver.startObserving();

		//===modify by ty
		mCurApnObserver = new CurAnpObserver(handler, PhoneConstants.SIM_ID_1);
		mCurApnObserver.startObserving();

		mCurApnObserver2 = new CurAnpObserver(handler, PhoneConstants.SIM_ID_2);
		mCurApnObserver2.startObserving();

		mApnListObserver = new ApnListObserver(handler);
		mApnListObserver.startObserving();
		
		MobileDataSettingObserver observer = new MobileDataSettingObserver(handler);
		observer.startObserving();

		// 楣挎偞銉椻挋璇ф嫹		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		filter.addAction(REQUEST_DATA_NETWORK_RESULT);
		filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
		filter.addAction(ACTION_FINISH_SELECT_NET_OPERATION);
		mContext.registerReceiver(mBroadcastReceiver, filter);
        // 娉ㄨ檹宀㈠ě鍚豢鈶诲禋骞挎挱
        IntentFilter mmsFilter = new IntentFilter();
        mmsFilter.addAction(CONNECTIVITY_ACTION_SHORT);
        mContext.registerReceiver(mMMsBroadcastReceiver, mmsFilter);

		ContentResolver cr = mContext.getContentResolver();
		mIsAirplaneMode = (0 != Settings.System.getIntForUser(cr,
				Settings.System.AIRPLANE_MODE_ON, 0,CurrentUserTracker.getCurrentUserId()));
		LogHelper.sd(TAG, "mIsAirplaneMode = " + mIsAirplaneMode);
		
        String language = Locale.getDefault().getLanguage();
        if (language.equalsIgnoreCase("zh")){
            mIsShowName = true;
        } else {
            mIsShowName = false;
        }
        LogHelper.sd(TAG, "language = " + language + " mIsShowName = " + mIsShowName);

		refreshApn();
	}

	// 閾滃綍瑭瀯瑜岀姘愰敓锟�
	public void addNetStatuNotifyCallBack(NetworkStatNotify callback) {
		mNotifyQuickSetting = callback;
		refreshUI();
	}
    private void showTaostMsg(String msg){
        Toast toast = Toast.makeText(mContext,
                msg,
                Toast.LENGTH_SHORT);
        //===modify by ty
//        if (!toast.isLayoutTypeSet()) {
//            toast.setLayoutType(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
//        }
        toast.show();
    }
	// 姹�楣挎紡璧傞敓閾扮殑鎺ユ帶锟�
	public void changeState() {
		LogHelper.sd(TAG, "setNetworkState mIsNetworkOn = " + mIsNetworkOn + " changing to " + !mIsNetworkOn + " isCardValid = " + isCardValid()
				+ " isCardValid():" + isCardValid() + " mIsMmsSending:" + mIsMmsSending);
		ConnectivityManager cm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (!isCardValid()){
			showTaostMsg(mContext.getString(YulongConfig.getDefault().mYulongResApnNocard));
		}
		else if (mIsMmsSending){
			showTaostMsg(mContext.getString(R.string.status_bar_expanded_mms_send_note));
		}
		else{
			CPDataConnSettingUtils.setMobileDataEnabled(mContext,!mIsNetworkOn);
			refreshApn();
		}
	}
	
	public boolean getMobiledataEnabled(){
		return mIsNetworkOn;
	}
    private BroadcastReceiver mMMsBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context mContext, Intent intent) {
			// TODO Auto-generated method stub
			String mAction = intent.getAction();
			mIsMmsSending = intent.getBooleanExtra(EXTRA_IS_SHORT_WORKING, false);
			LogHelper.sd(TAG, "mMMsBroadcastReceiver mIsMmsSending = " + mIsMmsSending);
		}
	};
	boolean isThisApnShow(ApnData temp){
//		if(mIs4gNet&&temp.apn.contains("ctwap")&&(temp.isdefault==1)) return false;		
		if (mCurNetwork >= 0 && mCurNetwork <= 1){
			if(temp.bearer==0)return true;
			if(mIs4gNet[mCurNetwork]&&(temp.bearer!=14)) return false;
			if(!mIs4gNet[mCurNetwork]&&(temp.bearer==14))return false;			
		}
		return true;
	}
	public void changeApn(){
		LogHelper.sd(TAG, "changeApn ");
		
		if (!isCardValid()){
			showTaostMsg(mContext.getString(YulongConfig.getDefault().mYulongResApnNocard));
			return;
		}
		else if (mIsMmsSending){
			showTaostMsg(mContext.getString(R.string.status_bar_expanded_mms_send_note));
			return;
		}		
		// TODO 琚屽埛瑜岃鎷㈠崲娼炴噾鎺滅獖鍛曠倝
		mApnListObserver.update();
		mCurApnObserver.update();
		mCurApnObserver2.update();
		
		if (mCurNetwork >= 0 && mCurNetwork <= 1 && mCardIsUsing[mCurNetwork]) {
			ApnData data = mCurApnList[mCurNetwork];
			LogHelper.sd(TAG, "changeApn mCurNetwork = " + mCurNetwork + " data "+ data);
			int nextApnId = -1;
			boolean bFindedFlag = false;
			for (int i = 0; i < mAllApnList.size(); ++i){
				ApnData temp = mAllApnList.get(i);
				LogHelper.sd(TAG, "temp.numeric = " + temp.numeric + " id = " + temp.id+ " temp.apn="+temp.apn+" temp.isdefault="+temp.isdefault+" temp.bearer=" + temp.bearer);
				if (temp.numeric != data.numeric){
					continue;
				} 
				
				// 纰岃節绂勮祩閿熼摪顪n
				if (nextApnId < 0){
					if(isThisApnShow(temp)) { //纰岄拋鍛囥剤姗欐兊瀹﹀寰凤拷4G绂勬亖鎰寸獕PD铏忕琚涚ずCTWAP
						nextApnId = temp.id;
						LogHelper.sd(TAG, "changeApn first nextApnId = " + nextApnId);
					}
				}
				// 璎风闄嗚仴纰屽崵鍓嶇妯″崲鑹婁箞琚�涓�璧傞敓閾扮枤濞欐崠顎┿剹鍏熷緱顡秐
				// 楠氶箍閿熺粸鐢枆鐟ｈ個闊劵寰烽摪瀣濐啓鐚存嫹閿熻В顒甿eric纰岃癌pn鎷㈠崲鑹婁箞钑懇鐙畩纰岀墶韫濈璧傞敓閾扮枤濞欏緷鎹栧闈╂嫹
				if (temp.id == data.id){
					bFindedFlag = true;
					LogHelper.sd(TAG, "changeApn continue nextApnId = " + nextApnId);
					continue;
				}
				if (bFindedFlag){
					if(!isThisApnShow(temp)) { //纰岄拋鍛囥剤姗欐兊瀹﹀寰凤拷4G绂勬亖鎰寸獕PD铏忕琚涚ずCTWAP
						continue;
					}
					nextApnId = temp.id;
					LogHelper.sd(TAG, "changeApn break nextApnId = " + nextApnId);
					break;
				}
			}
			LogHelper.sd(TAG, "changeApn nextApnId = " + nextApnId + " current apnId = " + data.id + " mAllApnList.size() = " + mAllApnList.size());
			if (nextApnId >= 0) {
				
				boolean ret = CPDataConnSettingUtils.setPreferApnById(mContext, Integer.toString(nextApnId), CPDataConnSettingUtils.getSubId(mCurNetwork));
				LogHelper.se(TAG, "" + ret);
			}
		}
	}

	private void refreshApn() {
		mCurNetwork = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);		
		mIsNetworkOn = CPDataConnSettingUtils.getMobileDataEnabled(mContext);
		mCardIsUsing[0] = CPDataConnSettingUtils.getSimState(mContext,0) == TelephonyManager.SIM_STATE_READY;
		mCardIsUsing[1] = CPDataConnSettingUtils.getSimState(mContext,1) == TelephonyManager.SIM_STATE_READY;
		if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshApn mIsAirplaneMode = " + mIsAirplaneMode
				+ " mIsNetworkOn = " + mIsNetworkOn
				+ " mCurNetwork = " + mCurNetwork
				+ " mCardIsUsing[0] = " + mCardIsUsing[0]
				+ " mCardIsUsing[1] = " + mCardIsUsing[1]);		
		mIsNetworkOnInUi = false;
		mIsRoaming = false;
		if (mIsAirplaneMode) { // 璺暙瑜庢嫝寮�
			mApnName = mContext
					//.getString(R.string.status_bar_expanded_airplane_mode);
					.getString(R.string.status_bar_expanded_network_off);
		} else if (!isCardValid()) { // 锠熸晥椹寸瘬
			mApnName = mContext
					.getString(YulongConfig.getDefault().mYulongResApnNocard);
		} else if (mIsNetworkOn && mCurNetwork >= 0 && mCurNetwork <= 1 
				&& mCardIsUsing[mCurNetwork]
				//&& CPDataConnSettingUtils.isMobileDataSupported(mContext)
				) {
			mApnListObserver.update();
			mCurApnObserver.update();
			mCurApnObserver2.update();
			mApnName = (mIsShowName ? mCurApnList[mCurNetwork].name
					: mCurApnList[mCurNetwork].apn);
            if ("true".equals(SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ISROAMING))
            		||"true".equals(SystemProperties.get("cdma.operator.isroaming"))){
            	mIsRoaming = true;
            }
			mIsNetworkOnInUi = true;
		} else { // 鎵﹁閱涢┐閿熼摪鐤氳胁鎳у叧搴囷拷
			mApnName = mContext
					.getString(R.string.status_bar_expanded_network_off);
		}

		refreshUI();
	}

	// 鍒疯闄嗛懘锟�
	private void refreshUI() {
		if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "refreshUI mNotifyQuickSetting == null ? " + (mNotifyQuickSetting == null)
	            + " mIsNetworkOnInUi = " + mIsNetworkOnInUi
	            + " mApnName = " + mApnName);
	    
		if (mNotifyQuickSetting != null) {
			mNotifyQuickSetting.notifyNetworkApn(mIsNetworkOnInUi,mIsRoaming,mApnName);
		}
		if (mNotifyQuickSetting != null){
			mNotifyQuickSetting.notifyNetworkSelected(mCardIsUsing[0],mCardIsUsing[1]);
		}		
	}

	private boolean isCardValid() {

		boolean ret = false;
		try {
			int validNum = CPDataConnSettingUtils.getValidCardNum();
			if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "IsCardValid validNum = " + validNum);
			if (validNum > 0) {
				ret = true;
			}
		} catch (Exception e) {
			LogHelper.se(TAG, e.toString());
		}
		return ret;

	}

	// Settings.System.getString(context.getContentResolver(),
	// Constants.DEFAULT_DATA_NETWORK);
	// Observer Default data network 浣胯椹寸瘬1绂勬啯娆楅潬绡�2l闄嗚┅閿熸枻鎷�
	private class DefaultDataNetworkObserver extends ContentObserver {

		private static final String DEFAULT_DATA_NETWORK = "default_data_network";

		public DefaultDataNetworkObserver(Handler handler) {
			super(handler);
		}

		void startObserving() {
			update();
			mContext.getContentResolver().registerContentObserver(
					Settings.System.getUriFor(DEFAULT_DATA_NETWORK), false,
					this);

		}

		private void update() {			
			mCurNetwork = CPDataConnSettingUtils.getDefaultDataNetwork(mContext);//CurrentUserTracker.getIntForCurrentUser(DEFAULT_DATA_NETWORK, -1) - 1;
			LogHelper.sd(TAG, "DefaultDataNetworkObserver mCurNetwork = " + mCurNetwork);
		}

		@Override
		public void onChange(boolean selfChange) {
			update();

			refreshApn();
			super.onChange(selfChange);
		}
	}

	// Observer Current Apn
	private class CurAnpObserver extends ContentObserver {

		private int mId;
		private int mSubId;
		private Uri mUri;
		private boolean mCurApnIsDirty = true;
		private static final String PreferredapnUsingSubId =
		        "content://telephony/carriers/preferapn/subId/";
		public CurAnpObserver(Handler handler, int id) {
			super(handler);
			mId = id;
		}
		public boolean isApnDirty(){
			return mCurApnIsDirty;
		}
		public void startObserving() {
			if(mSubId > 0){
				mContext.getContentResolver().unregisterContentObserver(this);
			}
			mSubId = CPDataConnSettingUtils.getSubId(mId);
			if(mSubId > 0){
				mUri = Uri.parse(PreferredapnUsingSubId + mSubId);
				mContext.getContentResolver().registerContentObserver(mUri, true,
						this);
				update();
			}
		}

		private void update() {
			new AsyncTask<Void, Void, Void>() {
	            @Override
	            protected Void doInBackground(Void... params) {
			if(mSubId <=0 ){
				startObserving();
			}
			if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "CurAnpObserver mId = " + mId + " mSubId = " + mSubId);
			if (mSubId > 0 && mCurApnIsDirty) {
				Cursor cursor = null;
				try {
					cursor = mContext.getContentResolver().query(mUri,
							null, null, null, null);
					if (cursor == null) {
						LogHelper.sd(TAG, "CurAnpObserver cursor is null mId = " + mId + " mUri = " + mUri);
						mCurApnList[mId].name = "";
						mCurApnList[mId].apn = "";
						mCurApnIsDirty = true;
					} else {
						mCurApnList[mId].id = -1;
						if (cursor.moveToFirst()) {
							mCurApnList[mId].SetByCursor(cursor);
//							LogHelper.sd(TAG, "CurAnpObserver cursor update index = " + mId + " mSubId = " + mSubId + " " + mCurApnList[mId].toString());
							mCurApnIsDirty = false;
						} else {
							mCurApnList[mId].name = "";
							mCurApnList[mId].apn = "";
//							LogHelper.sd(TAG, "CurAnpObserver cursor is empty mId = " + mId + " mUri = " + mUri);
							cursor.close();
							mCurApnIsDirty = true;
						}
					}
				} finally{
					try {
						if (cursor != null){
							cursor.close();
						}
					} catch (Exception e) {
						LogHelper.sd(TAG, "CurAnpObserver " + e.toString());
					}
				}
			}
	                return null;
	            }
	        }.execute();
		}

		@Override
		public void onChange(boolean selfChange) {

			mCurApnIsDirty = true;
			LogHelper.sd(TAG, "CurAnpObserver onChange id = " + mId);
			refreshApn();
			super.onChange(selfChange);
		}
	}

	//
	private class ApnListObserver extends ContentObserver {
		// 鍗ゆ澆鍤剁敪闊拏濠忔嫹鎭ｏ拷鐮楀嫝鎹煭顓冾灝锟�
		private boolean mApnListIsDirty = true;

		public ApnListObserver(Handler handler) {
			super(handler);
		}

		public void startObserving() {
			mContext.getContentResolver().registerContentObserver(
					Uri.parse(CPDataConnSettingUtils.CARRIERS_URI), true, this);
			update();
		}

		private void update() {
			new AsyncTask<Void, Void, Void>() {
	            @Override
	            protected Void doInBackground(Void... params) {
			if (mApnListIsDirty) {
				Cursor cursor = null;
				try {
					cursor = mContext.getContentResolver().query(
							Uri.parse(CPDataConnSettingUtils.CARRIERS_URI),
							mProjection, Telephony.Carriers.TYPE + " like 'default%'",
							null, "_id asc");
					if (cursor == null) {
						LogHelper.se(TAG, "ApnListObserver cursor is null");
					} else if (cursor.getCount() <= 0) {
						mAllApnList.clear();
						LogHelper.sd(TAG, "ApnListObserver cursor is empty");
					} else {
						mAllApnList.clear();
						while (cursor.moveToNext()) {
							ApnData data = new ApnData(cursor);
							if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "data " + data);
//							LogHelper.sd(TAG, "Telephony.Carriers.TYPE " +cursor.getString(cursor.getColumnIndex(Telephony.Carriers.TYPE)));
							mAllApnList.add(data);
						}
						mApnListIsDirty = false;
						LogHelper.sd(TAG, "ApnListObserver new AllApnList size is "
								+ mAllApnList.size());
					}
				} finally {
					try {
						if (cursor != null){
							cursor.close();
						}
					} catch (Exception e2) {
						LogHelper.sd(TAG, "ApnListObserver " + e2.toString());
					}
				}				
			}
	                return null;
	            }
	        }.execute();
		}

		@Override
		public void onChange(boolean selfChange) {
			mApnListIsDirty = true;
			LogHelper.sd(TAG, "ApnListObserver onChange ");
			super.onChange(selfChange);
		}
	}

	private class MobileDataSettingObserver extends ContentObserver {

		public MobileDataSettingObserver(Handler handler) {
			super(handler);

		}

		public void startObserving() {
//			mContext.getContentResolver().registerContentObserver(
//					Settings.System.getUriFor("mobile_data"), true, this,UserHandle.USER_ALL);
//			mContext.getContentResolver().registerContentObserver(
//			        Settings.System.getUriFor("mobile2_data"), true, this,UserHandle.USER_ALL);
//			mContext.getContentResolver().registerContentObserver(
//					Settings.Global.getUriFor(Settings.Global.MOBILE_DATA), true, this,UserHandle.USER_ALL);
//			mContext.getContentResolver().registerContentObserver(
//			        Settings.Global.getUriFor("mobile2_data"), true, this,UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.MOBILE_DATA),
                    false, this);          
			mContext.getContentResolver().registerContentObserver(
			        Settings.System.getUriFor("mobile_data0"), true, this,UserHandle.USER_ALL);
			mContext.getContentResolver().registerContentObserver(
					Settings.Global.getUriFor("mobile_data1"), true, this,UserHandle.USER_ALL);
			mContext.getContentResolver().registerContentObserver(
			        Settings.Global.getUriFor("mobile_data2"), true, this,UserHandle.USER_ALL);			
			update();
		}
		
		private void update(){
		}
		@Override
		public void onChange(boolean selfChange) {
			LogHelper.sd(TAG, "MobileDataSettingObserver onChange ");
			update();
			refreshApn();
			super.onChange(selfChange);
		}

	}

	private static class ApnData {
		public int id = -1;
		public String name; // 璁楄熆琚涚ず骞曡玻
		public String apn; 	// 鑻辫熆绂勬啯姘荤毈璜嶆埉apn
		public int numeric;
		public int isdefault;//add by wz
		public int bearer;

		public ApnData() {

		}

		public ApnData(Cursor cursor) {
			SetByCursor(cursor);
		}

		public void SetByCursor(Cursor cursor) {
			id = cursor.getInt(cursor.getColumnIndex(Telephony.Carriers._ID));
			name = cursor.getString(cursor
					.getColumnIndex(Telephony.Carriers.NAME));
			apn = cursor.getString(cursor
					.getColumnIndex(Telephony.Carriers.APN));
			numeric = cursor.getInt(cursor
					.getColumnIndex(Telephony.Carriers.NUMERIC));
			int index = cursor.getColumnIndex(DataNetworkController.Carriers_DEFAULT);
			if(index >= 0){
				isdefault=cursor.getInt(index);
			}
			bearer = cursor.getInt(cursor
					.getColumnIndex(Telephony.Carriers.BEARER));
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();

			sb.append(" id = ");
			sb.append(id);
			sb.append(" name = ");
			sb.append(name);
			sb.append(" apn = ");
			sb.append(apn);
			sb.append(" numeric = ");
			sb.append(numeric);
			sb.append(" isdefault=");
			sb.append(isdefault);

			return sb.toString();
		}
	}

	public void postRefreshApn(){
		mDataChangedHandler.removeMessages(MSG_REFRESHAPN);
		mDataChangedHandler.sendEmptyMessageDelayed(MSG_REFRESHAPN,1000);		
	}
	//
	public interface NetworkStatNotify {
		public void notifyNetworkApn(boolean bNetworkOn,boolean bRoaming,String apnText);
		public void notifyNetworkSelected(boolean cardOneUsing,boolean cardTwoUsing);
	}
	
    public void onWifiSignalChanged(boolean enabled, boolean connected, int wifiSignalIconId,
            boolean activityIn, boolean activityOut,
            String wifiSignalContentDescriptionId, String description){
    }
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
            String mobileSignalContentDescriptionId, int dataTypeIconId,
            boolean activityIn, boolean activityOut,
            String dataTypeContentDescriptionId, String description, boolean noSim,
            boolean isDataTypeIconWide){	
    	postRefreshApn();
    }
    public void onAirplaneModeChanged(boolean enabled){
    }
    public void onMobileDataEnabled(boolean enabled){
		if (mNotifyQuickSetting != null){
			mNotifyQuickSetting.notifyNetworkSelected(mCardIsUsing[0],mCardIsUsing[1]);
		}
    }
    public void onNetworkStateChange(int phoneId,int networkType){
    	if(phoneId >= 0 && phoneId < mIs4gNet.length){
    		mIs4gNet[phoneId] = (networkType == TelephonyManager.NETWORK_TYPE_LTE
    					|| networkType == TelephonyManager.NETWORK_TYPE_EHRPD);
    	}
    }
    
    //===modify by ty begin
//	@Override
//	public void onNetworkSimCount() {
//		// TODO Auto-generated method stub
//		
//	}
	@Override
	public void setWifiIndicators(boolean enabled, IconState statusIcon,
			IconState qsIcon, boolean activityIn, boolean activityOut,
			String description) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon,
			int statusType, int qsType, boolean activityIn,
			boolean activityOut, int dataActivityId, int mobileActivityId,
			int stackedDataIcon, int stackedVoiceIcon,
			String typeContentDescription, String description, boolean isWide,
			int subId) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setSubs(List<SubscriptionInfo> subs) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setNoSims(boolean show) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setEthernetIndicators(IconState icon) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setIsAirplaneMode(IconState icon) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setMobileDataEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		
	}
	//===modify by ty end
	
	@Override
	public void onNetworkSimCount() {
		// TODO Auto-generated method stub
		
	}
}
