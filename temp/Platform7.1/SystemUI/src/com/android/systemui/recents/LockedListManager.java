
package com.android.systemui.recents;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
 
public class LockedListManager {
	private static final String RECENTS_LOCK = "com.yulong.intent.action.RECENTS_LOCK";	
	private static final String DataType = "type";	
	private static final String Add = "add";	
	private static final String PKG = "pkg";	
	
	
	private String mDataName;
	private Context mContext;
	private SharedPreferences mShareData;
	private SharedPreferences.Editor mShareDataEditor;
	public LockedListManager(Context context,String dataname){
		mContext=context;
		mDataName=dataname;
		mShareData = mContext.getSharedPreferences(mDataName, Context.MODE_PRIVATE);
		mShareDataEditor=mShareData.edit();
	}
	public void SendLockPkg(String pkg,boolean bAdd){
		Intent mRecentsIntent = new Intent(RECENTS_LOCK);		
		mRecentsIntent.putExtra(DataType,mDataName);
		mRecentsIntent.putExtra(Add,bAdd);
		mRecentsIntent.putExtra(PKG,pkg);
		mContext.sendBroadcast(mRecentsIntent);		
	}
	public void AddLockedPkg(String pkg){	
		try{
		SendLockPkg(pkg,true);
		mShareDataEditor.putInt(pkg, 1);
		mShareDataEditor.commit();
		} catch(Exception e){
		 mShareData = mContext.getSharedPreferences(mDataName, 0);
		 mShareDataEditor=mShareData.edit();
		 mShareDataEditor.putInt(pkg, 1);
		 mShareDataEditor.commit();
		}
	} 
    public void RemoveLockedPkg(String pkg){
    	
    	try{
    		SendLockPkg(pkg,false);
    		mShareDataEditor.remove(pkg);
    		mShareDataEditor.commit();
    		} catch(Exception e){
    		 mShareData = mContext.getSharedPreferences(mDataName, 0);
    		 mShareDataEditor=mShareData.edit();
    		 mShareDataEditor.remove(pkg);
    		 mShareDataEditor.commit();
    		}
    }
	public boolean isInLockedList(String pkg){
		try{
			int lock= mShareData.getInt(pkg, 0);
			if(lock>0) return true;
			return false;
		} catch(Exception e){
			return false;
		}
	}
	


}
