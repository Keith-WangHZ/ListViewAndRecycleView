
package com.android.systemui.statusbar.phone;

import java.util.ArrayList;

import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.YulongConfig;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * @description A class for status bar save and get shortcut data
 * @author zhangyang1
 * 
 */
public class DataControler {
    private static final String TAG = "DataControler";
    private static final boolean DEBUG = true;

    private static final String DB_NAME = "statusbar.db";
    private static final int DB_VERSION = 15;
    private static final int DB_SUB_VERSION = 0;
    private String DB_TABLE = "shortcut";
    private static final String DB_TABLE_PRIMARY = "shortcut_primary";
    private static final String DB_TABLE_PERSONAL = "shortcut";

    private static final String KEY_ID = "id";
    private static final String KEY_POS = "pos";
    private static final String KEY_VALUE = "value";
    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";

    private HandlerThread mDbWorkerThread = null;
    private Handler mDbWorkerHandler = null;
    private final Context mContext;
    private final Object  mGetAllDataLock = new Object();

    /* message ids for handler */
    private static final int MSG_CHANGE_POS = 1;
    private static final int MSG_GET_DATA = 2;
    private static final int MSG_GET_DATA_SYNC = 3;
    private static final int MSG_QUIT = 4;
    private static final int MSG_SAVE_DATA = 5;
    private static final int MSG_CHANGE_VALUE = 6;
    private static final int MSG_CHANGE_TYPE = 7;
    private static final int MSG_SAVE_CTRL_CENTER_DATA = 8;

    private Boolean mPrimary;

    public DataControler(Context context, Boolean bPrimary) {
    	mPrimary = bPrimary;
    	if(mPrimary){
        	DB_TABLE = DB_TABLE_PRIMARY;
        }else{
        	DB_TABLE = DB_TABLE_PERSONAL;
        }
    	LogHelper.sd(TAG,"DB_TABLE="+DB_TABLE);
        mContext = context;
        startHandler();
    }
    
    private void startHandler() {
        mDbWorkerThread = new HandlerThread("statusbarDBWorker");
        mDbWorkerThread.start();
        mDbWorkerHandler = new DBWorkerHandler(mDbWorkerThread.getLooper(), mPrimary);
	}
    
	/**
     * 同步方式获取快捷方式数据
     */
    public void getAllShortcutDataSync(ArrayList<DatabaseRecord> data){
        LogHelper.sd(TAG, "enter getAllShortcutDataSync");
        boolean flag = true;
        try {
            synchronized (data) {
                mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_GET_DATA_SYNC, data));            
                while(flag){
                    data.wait();
                    flag = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            flag = false;
        }
        LogHelper.sd(TAG, "leave getAllShortcutDataSync");        
    }
    /**
     * @category get all shortcut data
     * @author zhangyang1
     */
    public void getAllShortcutData(ArrayList<DatabaseRecord> data, Runnable callback) {
        LogHelper.sd(TAG, "enter getAllShortcutData mPrimary="+mPrimary);
        synchronized (this) {
        if (data == null || callback == null) {
                 LogHelper.sd(TAG, "data or callback is null");
            return;
        }

        DatabaseParam p = new DatabaseParam(data, callback);
        try {
            mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_GET_DATA, p));            
        } catch (Exception e) {
            	LogHelper.sd(TAG, "sendMessage error");
            e.printStackTrace();
            }
        }
        LogHelper.sd(TAG, "leave getAllShortcutData");
    }
    
    public void saveAllShortcutData(ArrayList<DatabaseRecord> data){
        LogHelper.sd(TAG, "enter saveAllShortcutData");
        try {
            mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_SAVE_DATA, data));
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogHelper.sd(TAG, "leave saveAllShortcutData");
    }

    public void saveAllShortcutCtrlCenterData(ArrayList<DatabaseRecord> data) {
        Log.d(TAG, "enter saveAllShortcutData");
        try {
            mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_SAVE_CTRL_CENTER_DATA, data));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "leave saveAllShortcutData");
    }

    public void changePos(int id, int pos) {
    	//Log.d(TAG, "enter changePos");
        try {
            mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_CHANGE_POS, id, pos));            
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "leave changePos");
    }
    
    public void changeValue(int id, int value){
        LogHelper.sd(TAG, "enter changeValue"+" mPrimary="+mPrimary);
        try {
            mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_CHANGE_VALUE, id, value));            
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "leave changeValue");        
    }
    
    public void changeType(int id, int type){
        LogHelper.sd(TAG, "enter changeType");
        try {
            mDbWorkerHandler.sendMessage(mDbWorkerHandler.obtainMessage(MSG_CHANGE_TYPE, id, type));            
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "leave changeType");  
    }

    @SuppressLint("UseValueOf")
    public void waitForClose() {
    	LogHelper.sd(TAG, "enter waitForClose");
    	final Integer lock = new Integer(0);
        if (mDbWorkerHandler != null) {
            synchronized (lock) {
                if (mDbWorkerHandler == null){
                    return;
                }
                mDbWorkerHandler.sendEmptyMessage(MSG_QUIT);
                try {
                    mDbWorkerHandler.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                } 
                mDbWorkerHandler = null;
            }
        }
        LogHelper.sd(TAG, "leave waitForClose");
    }

    /**
     * @category one record in database
     * @author zhangyang1
     */
    public static class DatabaseRecord {
        public int id = -1;
        public int pos = -1;
        public int value = -1;
        public int type = -1;
        public String title;
    }

    private static class DatabaseParam {
        DatabaseParam(ArrayList<DatabaseRecord> data, Runnable callback) {
            mData = data;
            mCallback = callback;
        }

        ArrayList<DatabaseRecord> mData;
        Runnable mCallback;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private final Context mContext;
        
        // yulong begin :add for read database Exception,shenyupeng,2016.4.6
        private static DatabaseHelper helper;

        public static synchronized DatabaseHelper getInstance(Context context) {
            if (helper == null) {
                helper = new DatabaseHelper(context);
            }
            return helper;
        }
        // yulong end

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (DEBUG) {
                LogHelper.sd(TAG, "onCreate database " + DB_NAME);
            }
            InitShortcut(db, true);
            InitShortcut(db, false);
  
//            // 插入测试数据
//            for (int i = 0; i < 17; i++) {
//                ContentValues v = new ContentValues();
//                v.put(KEY_ID, i);
//                v.put(KEY_POS, i);
//                v.put(KEY_VALUE, -1);
//                
//                db.insert(DB_TABLE, KEY_ID, v);
//            }
//            String sql = "UPDATE " + DB_TABLE 
//                    + " set " + KEY_POS + "=" + KEY_POS 
//                    + "-1 WHERE " + KEY_ID + ">8";
//            Log.d(TAG, sql);
//            db.execSQL(sql);
//            sql = "UPDATE " + DB_TABLE 
//                    + " set " + KEY_POS + "=-1"
//                    + " WHERE " + KEY_ID + "=8";
//            Log.d(TAG, sql);
//            db.execSQL(sql);
//            
//            sql = "UPDATE " + DB_TABLE 
//                    + " set " + KEY_POS + "=10"
//                    + " WHERE " + KEY_ID + "=15";
//            db.execSQL(sql);
//            
//            sql = "UPDATE " + DB_TABLE 
//                    + " set " + KEY_POS + "=14"
//                    + " WHERE " + KEY_ID + "=11";
//            db.execSQL(sql);
//            
//            // TODO 临时代码，之后从配置读初始数据         
//            if (YulongConfig.getDefault().getMode() > 0){
//                sql = "UPDATE " + DB_TABLE 
//                        + " set " + KEY_POS + "=-1"
//                        + " WHERE " + KEY_POS + "=7";
//                Log.d(TAG, sql);
//                db.execSQL(sql);
//                sql = "UPDATE " + DB_TABLE 
//                        + " set " + KEY_POS + "=" + KEY_POS 
//                        + "-1 WHERE " + KEY_POS + ">7";
//                Log.d(TAG, sql);
//                db.execSQL(sql);
//                
//                sql = "UPDATE " + DB_TABLE 
//                        + " set " + KEY_POS + "=-1"
//                        + " WHERE " + KEY_POS + "=8";
//                Log.d(TAG, sql);
//                db.execSQL(sql);
//                sql = "UPDATE " + DB_TABLE 
//                        + " set " + KEY_POS + "=" + KEY_POS 
//                        + "-1 WHERE " + KEY_POS + ">8";
//                Log.d(TAG, sql);
//                db.execSQL(sql);                
//            } else {
//                sql = "UPDATE " + DB_TABLE 
//                        + " set " + KEY_POS + "=12"
//                        + " WHERE " + KEY_ID + "=11";
//                Log.d(TAG, sql);
//                db.execSQL(sql);
//                sql = "UPDATE " + DB_TABLE 
//                        + " set " + KEY_POS + "=14"
//                        + " WHERE " + KEY_ID + "=13";
//                Log.d(TAG, sql);
//                db.execSQL(sql); 
//            }
        }

        void InitShortcut(SQLiteDatabase db, Boolean isPrimary) {

            String TEMP_TABLE = "";
            if (isPrimary) {
                TEMP_TABLE = DB_TABLE_PRIMARY;
            } else {
                TEMP_TABLE = DB_TABLE_PERSONAL;
            }
            String sql = "CREATE TABLE " + TEMP_TABLE + " (" + "id  INTEGER PRIMARY KEY" + ",pos INTEGER "
                    + ",value INTEGER" + ",type INTEGER" + ",title TEXT" + ")";
            db.execSQL(sql);
            Log.e(TAG, "InitShortcut:" + sql);
            ArrayList<Integer> defData = YulongConfig.getDefault().getDefShortcutOrder(isPrimary);
            int maxMajor = YulongConfig.getDefault().mYulongMajorMaxShortcut;
            int i = 0;
            for (int id : defData) {
                ContentValues v = new ContentValues();
                v.put(KEY_ID, id);
                v.put(KEY_POS, i);
                
                v.put(KEY_TYPE, i < maxMajor ? 0 : 1);
                
                // CButton
                if (id == QuickSettingsData.QS_ID_CBUTTON){
                    v.put(KEY_VALUE, YulongConfig.getDefault().getCButtonDefStatus(isPrimary));
                }
                db.insert(TEMP_TABLE, KEY_ID, v);
                i++;
            }        	
        }  
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(TAG, "onUpgrade: oldVersion=" + oldVersion + ";newVersion=" + newVersion);
            if (oldVersion != newVersion) {
                Log.e(TAG, "onUpgrade: 1");
                try {
                    db.execSQL("Drop TABLE IF EXISTS " + DB_TABLE_PRIMARY);
                    InitShortcut(db, true);

                    db.execSQL("Drop TABLE IF EXISTS " + DB_TABLE_PERSONAL);
                    InitShortcut(db, false);
                } catch (SQLiteException e) {
                    LogHelper.se(TAG, "onUpgrade SQLiteException:" + e);
                }
            }
        }
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(TAG, "onDowngrade: oldVersion=" + oldVersion + ";newVersion=" + newVersion);
            if (oldVersion != newVersion) {
                try {
                    db.execSQL("Drop TABLE IF EXISTS " + DB_TABLE_PRIMARY);
                    InitShortcut(db, true);

                    db.execSQL("Drop TABLE IF EXISTS " + DB_TABLE_PERSONAL);
                    InitShortcut(db, false);
                } catch (SQLiteException e) {
                    LogHelper.se(TAG, "onDowngrade SQLiteException:" + e);
                }
            }
        }
    }

    private class DBWorkerHandler extends Handler {

        private static final String TAG = "DataControler.DBWorkerHandler";
        private SQLiteDatabase mSQLiteDatabase = null;
        private DatabaseHelper mDatabaseHelper = null;
        private Boolean mPrimary;
        private String DB_TABLE = "shortcut";
        private static final String DB_TABLE_PRIMARY = "shortcut_primary";
        private static final String DB_TABLE_PERSONAL = "shortcut";
        DBWorkerHandler(Looper looper, boolean bPrimary) {
            super(looper);
            mPrimary = bPrimary;
        	if(mPrimary){
            	DB_TABLE = DB_TABLE_PRIMARY;
            }else{
            	DB_TABLE = DB_TABLE_PERSONAL;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            LogHelper.sd(TAG, "enter handleMessage msg = " + msg.what+" mPrimary="+mPrimary);
            // TODO 先放这里
            try {
                if (mDbWorkerThread == null){
                    LogHelper.sd(TAG, "mDbWorkerThread == null return");
					switch (msg.what) {
					case MSG_GET_DATA:
						handleGetData((DatabaseParam) msg.obj);
						break;
					}
                    return;
                }else if (mDatabaseHelper == null) {
                	try {
                		openDatabase();
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
						LogHelper.sd(TAG, "openDatabase exception");
					}
                }

                switch (msg.what) {
                case MSG_CHANGE_POS:
                    handleChangePos(msg.arg1, msg.arg2);
                    break;
                case MSG_GET_DATA:
                    handleGetData((DatabaseParam) msg.obj);
                    break;
                case MSG_SAVE_CTRL_CENTER_DATA:
                    handleSaveCtrlCenterData((ArrayList<DatabaseRecord>) msg.obj);
                    break;
                case MSG_SAVE_DATA:
                    handleSaveData((ArrayList<DatabaseRecord>) msg.obj);
                case MSG_QUIT:
                    closeDatabase();
                    break;
                case MSG_GET_DATA_SYNC:
                    handleGetDataSync((ArrayList<DatabaseRecord>) msg.obj);
                    break;
                case MSG_CHANGE_VALUE:
                    handleChangeValue(msg.arg1, msg.arg2);
                    break;
                case MSG_CHANGE_TYPE:
                    handleChangeType(msg.arg1, msg.arg2);
                    break;
                default:
                    break;
                }
            } catch (Exception e) {
                //LogHelper.sd(TAG, e.toString());
                e.printStackTrace();
            }

            //Log.d(TAG, "leave handleMessage msg " + msg.what);

        }

        private void handleChangeValue(int id, int value){
            ContentValues v = new ContentValues();
            v.put(KEY_VALUE, value);
            int c = mSQLiteDatabase.update(DB_TABLE, v, "id=" + id, null);
            //Log.d(TAG, "handleChangePos id = " + id + " value = " + value + " " + c + " row changed");
            
        }
        private void handleChangePos(int id, int pos) {
            // mDatabaseHelper.
            ContentValues v = new ContentValues();
            v.put(KEY_POS, pos);
            int c = mSQLiteDatabase.update(DB_TABLE, v, "id=" + id, null);
            LogHelper.sd(TAG, "handleChangePos id = " + id + " pos = " + pos + " " + c + " row changed");
        }
        
        private void handleChangeType(int id, int type){
            ContentValues v = new ContentValues();
            v.put(KEY_TYPE, type);
            int c = mSQLiteDatabase.update(DB_TABLE, v, "id=" + id, null);
            LogHelper.sd(TAG, "handleChangeType id = " + id + " type = " + type + " " + c + " row changed");
        }

        private void handleGetData(DatabaseParam p) {
        	LogHelper.sd(TAG, "enter handleGetData DB_TABLE="+DB_TABLE+" mPrimary="+mPrimary);
//        	Log.d(TAG, "1111 handleGetData mPrimary="+mPrimary+" DB_TABLE="+DB_TABLE);
            //Cursor result = mSQLiteDatabase.query(DB_TABLE, null, null, null, null, null, null);
        	boolean needSetDefault = false;
        	try {
        		 Cursor result = mSQLiteDatabase.query(DB_TABLE, null, null, null, null, null, KEY_POS);
                 //mSQLiteDatabase.qu
                 if (result != null && result.moveToFirst()) {
                     do {
                         DatabaseRecord r = new DatabaseRecord();
                         r.id = result.getInt(result.getColumnIndexOrThrow(KEY_ID));
                         r.pos = result.getInt(result.getColumnIndexOrThrow(KEY_POS));
                         r.value = result.getInt(result.getColumnIndexOrThrow(KEY_VALUE));
                         int index = result.getColumnIndex(KEY_TYPE);
                         if (index >= 0){
                             r.type = result.getInt(index);
                         }
                         p.mData.add(r);

                     } while (result.moveToNext());
                 } 
                 //yulong begin:add for read a defaultsort when read database failed,shenyupeng,2016.3.22
                 else {
                	 needSetDefault = true;
                	 LogHelper.sd(TAG, "handleGetData read data null");
                 }
                 if (result != null) {
 					result.close();
 				}
                 //yulong end
			} catch (Exception e) {
				e.printStackTrace();
				
				needSetDefault = true;
			}
            
			if(needSetDefault){
				 int[] defaultsort = { 2, 0, 1, 8, 5, 9, 13, 22, 11, 6, 7, 14, 12, 44, 42, 43 };
                 for (int index = 0; index < defaultsort.length; index++) {
                     DatabaseRecord r = new DatabaseRecord();
                     r.id = defaultsort[index];
                     r.pos = index;
                     r.value = 0;
                     r.type = -1;
                     p.mData.add(r);
                 }
			}
            p.mCallback.run();
            LogHelper.sd(TAG, "leave handleGetData");
        }
        private void handleGetDataSync(ArrayList<DatabaseRecord> data){
        	LogHelper.sd(TAG, "enter handleGetDataSync");
            synchronized (data) {
                Cursor result = mSQLiteDatabase.query(DB_TABLE, null, null, null, null, null, null);
                if (result != null && result.moveToFirst()) {
    
                    do {
                        DatabaseRecord r = new DatabaseRecord();
                        r.id = result.getInt(result.getColumnIndexOrThrow(KEY_ID));
                        r.pos = result.getInt(result.getColumnIndexOrThrow(KEY_POS));
                        r.value = result.getInt(result.getColumnIndexOrThrow(KEY_VALUE));
                        data.add(r);
                    } while (result.moveToNext());
                }

                data.notify();
                try {
    				if (result != null) {
    					result.close();
    				}
    			} catch (Exception e) {
                    
                }
            }
            
            
            LogHelper.sd(TAG, "leave handleGetDataSync");            
        }

        private void openDatabase() throws SQLException {
            LogHelper.sd(TAG, "enter openDatabase"+" mPrimary="+mPrimary);
            synchronized (this) {
                // yulong begin: add for read database Exception,shenyupeng,2016.4.6
                // mDatabaseHelper = new DatabaseHelper(mContext);
                if (mSQLiteDatabase != null && mSQLiteDatabase.isOpen()) {
                    return;
                }
                mDatabaseHelper = DatabaseHelper.getInstance(mContext);
                // yulong end
                mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
            }
            LogHelper.sd(TAG, "leave openDatabase");
        }

        private void closeDatabase() {
        	LogHelper.sd(TAG, "enter closeDatabase" );
            synchronized (this) {
                if (mDbWorkerThread != null){
                    
                    mSQLiteDatabase.close();
                    mDatabaseHelper.close();
                    removeMessages(MSG_QUIT);
                    mDbWorkerThread.quit(); // 之前消息处理完以后Thread才会退出。要忽略后续的所有消息
                    
                    mDbWorkerThread = null;
                    mDatabaseHelper = null;
                    mSQLiteDatabase = null;
                    notifyAll();
                    LogHelper.sd(TAG, "done closeDatabase");
                }
            }
            LogHelper.sd(TAG, "leave closeDatabase");
        }
        private void handleSaveData(ArrayList<DatabaseRecord> data){
        	LogHelper.sd(TAG, "enter handleSaveData data.size = " + data.size() );
            ContentValues v = new ContentValues();
            for (int i = 0; i < data.size(); ++i){
                v.clear();
                v.put(KEY_POS, data.get(i).pos);
                v.put(KEY_VALUE, data.get(i).value);
                v.put(KEY_TYPE, data.get(i).type);
                int c = mSQLiteDatabase.update(DB_TABLE, v, "id=" + data.get(i).id, null);
                LogHelper.sd(TAG, "handleSaveData id = " + data.get(i).id + " pos = " + data.get(i).pos + " " + c + " row changed");
            }
            LogHelper.sd(TAG, "leave handleSaveData");            
        }

        private void handleSaveCtrlCenterData(ArrayList<DatabaseRecord> data) {
            Log.d(TAG, "enter handleSaveCtrlCenterData data.size = " + data.size() + "; isOpen:"
                    + mSQLiteDatabase.isOpen());
            ContentValues v = new ContentValues();
            mSQLiteDatabase.beginTransaction();
            for (int i = 0; i < data.size(); ++i) {
                v.put(KEY_ID, data.get(i).id);
                v.put(KEY_POS, data.get(i).pos);
                v.put(KEY_VALUE, data.get(i).value);
                v.put(KEY_TYPE, data.get(i).type);
                v.put(KEY_TITLE, data.get(i).title);
                long c = mSQLiteDatabase.insertWithOnConflict(DB_TABLE, KEY_ID, v, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "insertWithOnConflict ContentValues:" + v + " c=" + c);
            }
            //mSQLiteDatabase.execSQL("DELETE FROM " + DB_TABLE + " WHERE pos >= 16");// 闄ゅ幓鏄剧ず浠ュ鐨勬暟鎹�
            mSQLiteDatabase.setTransactionSuccessful();
            mSQLiteDatabase.endTransaction();
            Log.d(TAG, "leave handleSaveCtrlCenterData");
        }
    }
}
