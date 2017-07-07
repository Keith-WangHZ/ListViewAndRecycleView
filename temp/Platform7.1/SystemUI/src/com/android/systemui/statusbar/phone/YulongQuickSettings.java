package com.android.systemui.statusbar.phone;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.DataControler.DatabaseRecord;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
//import com.securespaces.android.ssm.UserUtils;



public class YulongQuickSettings implements QuickSettingsModel.IUpdateView,QuickSettingsModel.IUpdateData{	
	private static final String TAG = "YulongQuickSettings"; 
	private Context mContext;
	private QuickSettingsController    mController;
	private QuickSettingsModel		   mModel;
	private DataControler			   mDataController;
	private ArrayList<DatabaseRecord> mIconRecord = new ArrayList<DatabaseRecord>();
	private Handler					   mMainHandler = new Handler();
	private int                        mVisibleSize = 50;

	private ArrayList<QuickSettingsModel.IUpdateView> mUpdateCallbacks = new ArrayList<QuickSettingsModel.IUpdateView>();

	public void AddUpdateViewCallback(QuickSettingsModel.IUpdateView callback) {
		if (!mUpdateCallbacks.contains(callback)) {
			mUpdateCallbacks.add(callback);
		}
		for (State s : mStates) {
			callback.updateView(s);
		}
	}
	public void RemoveUpdateViewCallback(QuickSettingsModel.IUpdateView callback){
		if(mUpdateCallbacks.contains(callback)){
			mUpdateCallbacks.remove(callback);
		}		
	}
	@Override
	public void updateView(State state){
		for(QuickSettingsModel.IUpdateView cb:mUpdateCallbacks){
			cb.updateView(state);
		}
	}
	
	@Override
	public void resetQsView(Boolean bReinitialize) {
		// TODO Auto-generated method stub
		for(QuickSettingsModel.IUpdateView cb:mUpdateCallbacks){
			cb.resetQsView(bReinitialize);
		}
	}
	
	private ArrayList<State> mStates = new ArrayList<State>();	
	@Override
	public void updateData(State state){
		for(State s:mStates){
			if(s.id == state.id){
				//婵犵锟藉啿锟藉綊鎮樻径瀣鐎广儱娲ㄩ弸鍌氣槈閹垮嫭瀚�-1闂佹寧绋戦惌鍌炲磻閸涱喚鈻曢柛顐ｇ矌閻熸繈鏌￠崶褏鎽犻柡灞斤攻閹峰懎顓奸崶鈺傜��
				int order = state.order;
				if(order == -1){
					order = s.order;
				}
				s = state;
				s.order = order;
				break;
			}
		}		
	}
	@Override
	public void setOrder(int id,int order){
		for(State s:mStates){
			if(s.id == id){
				if(s.order != order){
					s.order = order;
					mDataController.changePos(s.id,s.order);							
				}
				break;
			}
		}		
	}
	@Override
	public State getState(int id){
		for(State s:mStates){
			if(s.id == id)
				return s;
		}
		State newState = new State(id);		
		if(!Utilities.showDragDownQuickSettings() && (id == QuickSettingsData.QS_ID_WLAN || 
				id == QuickSettingsData.QS_ID_BLUETOOTH)){
			newState.setVisibleSecondary(true);
		}
		mStates.add(newState);
		return newState;
	}
	@Override
	public void RefreshView(int id){
		for(State s:mStates){
			if(id== -1){
				updateView(s);
			}else if(id == s.id && s.order != -1){
				updateView(s);
				if (id == QuickSettingsData.QS_ID_CBUTTON){
				    mDataController.changeValue(id, s.status);
				}				
				break;
			}						
		}
	}

	public void setItemOnClickListen(QuickSettingsItemView v){
		v.setOnClickListener(mQuickSettingsClickListener);
		v.setLongClickable(true);
		v.setOnLongClickListener(mQuickSettingsLongClickListener);	
	}
	public View.OnClickListener getItemOnClickListen(){
		return mQuickSettingsClickListener;
	}	
	View.OnClickListener mQuickSettingsClickListener = new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			//LogHelper.sd(TAG, "mQuickSettingsClickListener v="+v+" id="+v.getId());
			try {
			QuickSettingsItemView view = (QuickSettingsItemView) v;
			mController.onClickQuickSetting(view.getQuickSettingId());
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
	};
	public View.OnClickListener getItemOnFirstClickListen(){
		return mQuickSettingsFirstClickListener;
	}	
	View.OnClickListener mQuickSettingsFirstClickListener = new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			//LogHelper.sd(TAG, "mQuickSettingsFirstClickListener v="+v+" id="+v.getId());
			try {
				QuickSettingsItemViewSub view = (QuickSettingsItemViewSub) v;
				mController.onClickQuickSetting(view.getQuickSettingId());
			} catch (Exception e) {
				LogHelper.sd(TAG, "mQuickSettingsFirstClickListener error");
				// TODO: handle exception
				e.printStackTrace();
			}

		}
	};
	
	public View.OnLongClickListener getItemOnFirstLongClickListen(){
		return mQuickSettingsFirstLongClickListener;
	}	
	View.OnLongClickListener mQuickSettingsFirstLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			//LogHelper.sd(TAG, "mQuickSettingsFirstLongClickListener v="+v+" id="+v.getId());
			QuickSettingsItemViewSub view = (QuickSettingsItemViewSub) v;
			mController.onLongClickQuickSetting(view.getQuickSettingId());
			return false;
		}
	};
	
	public View.OnClickListener getItemOnSecondaryClickListen(){
		return mQuickSettingsSecondaryClickListener;
	}	
	View.OnClickListener mQuickSettingsSecondaryClickListener = new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			//LogHelper.sd(TAG, "mQuickSettingsSecondaryClickListener v="+v+" id="+v.getId());
			try {
				QuickSettingsItemViewSub view = (QuickSettingsItemViewSub) v;
				mController.onSecondaryClickQuickSetting(view.getQuickSettingId());
			} catch (Exception e) {
				LogHelper.sd(TAG, "mQuickSettingsSecondaryClickListener error");
				// TODO: handle exception
				e.printStackTrace();
			}

		}
	};
	public View.OnLongClickListener getItemOnLongClickListen(){
		return mQuickSettingsLongClickListener;
	}	
	View.OnLongClickListener mQuickSettingsLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			QuickSettingsItemView view = (QuickSettingsItemView) v;
			boolean YL = mController.onLongClickQuickSetting(view.getQuickSettingId());
			// v.setPressed(false);
			if (YL != false) {
				return true;
			}
			return false;
		}
	};
	
	private Runnable mReadFinish = new Runnable() {
		
		@Override
		public void run() {
			mMainHandler.post(new Runnable() {				
				@Override
				public void run() {
					LayoutInflater inflater = LayoutInflater.from(mContext);
					
					// FIXME 
					int majorCount = 0;
					int cbuttonStatus = -1;
					LogHelper.sd(TAG, "mReadFinish mPrimary= mIconRecord.size:" + mIconRecord.size());
					mStates.clear();
					for (int i = 0; i < mIconRecord.size(); ++i){
						if(Utilities.showDragDownQuickSettings() && mIconRecord.get(i).id == 43){//don't show control center
							continue;
						}
						State s = getState(mIconRecord.get(i).id);
						s.status = mIconRecord.get(i).value;
						s.order = mIconRecord.get(i).pos;
						if (mIconRecord.get(i).type == 0){
						    majorCount++;
						}
						if (mIconRecord.get(i).id == QuickSettingsData.QS_ID_CBUTTON){
						    cbuttonStatus = mIconRecord.get(i).value;
						    cbuttonStatus = cbuttonStatus >=0 ? cbuttonStatus : 0;
						}
					}
					
					mController.initialize(mModel);					
                    mModel.initialize();
                    if (cbuttonStatus != -1)                    	
                    	mModel.setCbuttonStatus(cbuttonStatus);
                    
                    mModel.setHasDataRoaming(getState(QuickSettingsData.QS_ID_DATA_ROAMING).order!=-1);	 
                    //LogHelper.sd(TAG, "mReadFinish mUpdateCallbacks.size:" + mUpdateCallbacks.size());
                    resetQsView(true);
                    RefreshView(-1);//闂佸搫娲ら悺銊╁蓟婵犲洤绠ラ柨鐔剁矙瀵灚绻呭銆唚
				}
			});			
		}
	};

	private static YulongQuickSettings sInstance;
	private static YulongQuickSettings sInstanceSecure;
	public static YulongQuickSettings getInstance(Context context){
		LogHelper.sd(TAG,"mPrimary= getInstance(context, false);");
		return null;
	}
	
	public static YulongQuickSettings getInstance(Context context, Boolean bPrimary){
		return null;
	}

	public void setStatusBar(PhoneStatusBar bar) {
		if (bar != null) {
			mController.setStatusBar(bar);
		}
	}
	
	public YulongQuickSettings(Context context, Boolean bPrivate) {
		mContext = context;
		mController = new QuickSettingsController(context);
		mModel		= new QuickSettingsModel(context,bPrivate,this,this);
		mDataController = DataControlerContain.getInstance(context, bPrivate);		
		mIconRecord.clear();
		LogHelper.sd(TAG, "YulongQuickSettings bPrivate="+bPrivate);
		//if(Utilities.showDragDownQuickSettings())
		//{
		mDataController.getAllShortcutData(mIconRecord, mReadFinish);
		//}
		//Log.d("YulongQuickSettings(Context context)","YulongQuickSettings(Context context)");
	}
	
	public void onConfigurationChanged(){
		//闂佸憡甯掑ú锕�鐣烽懠顒佸珰妞ゆ挴妾ч弸鍛存煛閸愵厽纭鹃柨鐔诲Г閻熻京妲愬┑瀣嵆閻庢稒蓱閻擄拷婵炴垶鎼幏鐑藉级閸垺鍤�鐎规挸妫濆畷锝夋晸閿燂拷
		RefreshView(-1);
	}
	public ArrayList<DatabaseRecord> getIconConfig(){
		return mIconRecord;
	}
}
