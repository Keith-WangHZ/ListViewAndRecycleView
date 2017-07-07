package com.android.systemui.keyguard;

import com.android.systemui.R;
import com.android.systemui.recents.misc.Utilities;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class KeyguardContentProvider extends ContentProvider {

	private static final String TAG = "KeyguardContentProvider";
	public static final String LEVECT_PACKAGE = "com.levect.lockscreen.proa";
	public static final String SINA_PACKAGE = "com.bizhiquan.lockscreen";
	private static final String AUTHORITY = "com.android.systemui.KeyguardContentProvider";
	private static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.systemui.PrimaryDomain";
	private static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.systemui.PrimaryDomain";
	private static final UriMatcher uriMatcher;
	private static final int PRIMARYDOMAIN = 0;
	private static final int SECURITYDOMAIN = 1;
	private static final int KEYGUARD_MAGAZINE_SETTINGS = 2;
	public static final Uri PRIMARYDOMAIN_URI = Uri.parse("content://" + AUTHORITY + "/PrimaryDomain");
	public static final Uri SECURITYDOMAIN_URI = Uri.parse("content://" + AUTHORITY + "/SecurityDomain");
	public static final Uri KEYGUARD_MAGAZINE_SETTINGS_URI = Uri.parse("content://" + AUTHORITY + "/keyguard_magazine_settings");
	public static final int CONSTANTS_ID = 0;
	public static final int CONSTANTS_KEYGUARD_STYLE = 1;
	public static final int CONSTANTS_KEYGUARD_STYLE_NAME = 2;
	public static final int CONSTANT_3RD_TYPE = 3;
	public static boolean FeatureConfig_isSupportOversea = Utilities.isSupportOversea();
	public static int NO_MAGAZINE_SUPPORT = 0;
	public static int LEVECT_SUPPORT = 1;
	public static int SINA_SUPPORT = 2;
	private KeyguardDataBaseHelper mKeyguardDataBaseHelper;
	private SQLiteDatabase mDatabase;
	private String[] mKeyguardStyle;
	private String mNewLanguage;
	private String mOldLanguage;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "PrimaryDomain", PRIMARYDOMAIN);
		uriMatcher.addURI(AUTHORITY, "SecurityDomain", SECURITYDOMAIN);
		uriMatcher.addURI(AUTHORITY, "keyguard_magazine_settings", KEYGUARD_MAGAZINE_SETTINGS);
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		if (mOldLanguage == null) {
			mOldLanguage = getContext().getResources().getConfiguration().locale.getLanguage();
			Log.d(TAG, "onCreate mOldLanguage =" + mOldLanguage);
		}
		mKeyguardDataBaseHelper = new KeyguardDataBaseHelper(getContext());
		mDatabase = mKeyguardDataBaseHelper.getReadableDatabase();
		initKeyguardDataBase(mDatabase);
		return false;
	}

    private void initKeyguardDataBase(SQLiteDatabase db) {
    	boolean isLevectAppExist = KeyguardToolsUtil.isSpecificPackageExist(getContext(),
                LEVECT_PACKAGE);
        boolean isSinaAppExist = KeyguardToolsUtil.isSpecificPackageExist(getContext(),
                SINA_PACKAGE);
        int keyguard_3rd_type = NO_MAGAZINE_SUPPORT;
        if (FeatureConfig_isSupportOversea) {
            if (isSinaAppExist) {
                keyguard_3rd_type = SINA_SUPPORT;
            }
        } else {
            if (isSinaAppExist) {
                keyguard_3rd_type = SINA_SUPPORT;
            } else {
                if (isLevectAppExist) {
                    keyguard_3rd_type = LEVECT_SUPPORT;
                }
            }
        }
        Log.d(TAG, "isLevectAppExist =" + isLevectAppExist + " isSinaAppExist =" +isSinaAppExist 
        		+ " FeatureConfig_isSupportOversea =" + FeatureConfig_isSupportOversea);
    	Cursor cursor = null;
		try {
			cursor = mDatabase.rawQuery("select * from PrimaryDomain", null);
			if (cursor != null) {
				cursor.moveToFirst();
				if(cursor.getCount() == 0){
					ContentValues values = new ContentValues();
					int id = 0;
					int keyguard_style = 1;//defaut style :magazine
			    	String keyguard_style_name = (getContext().getResources().getStringArray(R.array.keyguard_style))[keyguard_style];
			    	values.put("_id", id);
		            values.put("keyguard_style", keyguard_style);
		            values.put("keyguard_style_name", keyguard_style_name);
		            values.put("keyguard_3rd_type", keyguard_3rd_type);
		            db.insert("PrimaryDomain", "_id = 0", values);
					getContext().getContentResolver().notifyChange(PRIMARYDOMAIN_URI, null);
					Log.d(TAG, "PrimaryDomain keyguard_style =" + keyguard_style + " keyguard_style_name =" +keyguard_style_name 
							+ " keyguard_3rd_type =" + keyguard_3rd_type);
				}else{
					ContentValues values = new ContentValues();
		            values.put("keyguard_3rd_type", keyguard_3rd_type);
		            db.update("PrimaryDomain", values, "_id = 0", null);
					getContext().getContentResolver().notifyChange(PRIMARYDOMAIN_URI, null);
					Log.d(TAG, "PrimaryDomain keyguard_3rd_type =" + keyguard_3rd_type); 
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		
		try {
			cursor = mDatabase.rawQuery("select * from keyguard_magazine_settings", null);
			if (cursor != null) {
				cursor.moveToFirst();
				if (cursor.getCount() == 0) {
					ContentValues values = new ContentValues();
				    values.clear();
				    values.put("_id", 0);
				    values.put("guide_shown", 0);
				    values.put("disclaimer_reminded", 0);
				    values.put("is_auto_update", true);
				    values.put("is_can_scroll", true);
				    values.put("is_show_icon", true);
				    db.insert("keyguard_magazine_settings", "_id = 0", values);
				    getContext().getContentResolver().notifyChange(KEYGUARD_MAGAZINE_SETTINGS_URI, null);
				    Log.d(TAG, "init keyguard_magazine_settings"); 
				}else{
					Log.d(TAG, "cursor.getCount() == " + cursor.getCount()); 
				}
			}else{
				Log.d(TAG, "cursor == null"); 
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		
		
		try {
			cursor = mDatabase.rawQuery("select * from SecurityDomain", null);
			if (cursor != null) {
				cursor.moveToFirst();
				if (cursor.getCount() == 0) {
					ContentValues values = new ContentValues();
				    values.clear();
				    values.put("_id", 0);
				    values.put("keyguard_style", 0);
				    values.put("keyguard_style_name", "");
				    values.put("keyguard_3rd_type", 0);
				    db.insert("SecurityDomain", "_id = 0", values);
				    getContext().getContentResolver().notifyChange(SECURITYDOMAIN_URI, null);
				    Log.d(TAG, "init SecurityDomain"); 
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
    }

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		Cursor cs = null;
		int code = uriMatcher.match(uri);
		switch (code) {
		case PRIMARYDOMAIN:
			cs = mDatabase.rawQuery("select * from PrimaryDomain", null);
			break;
		case SECURITYDOMAIN:
			cs = mDatabase.rawQuery("select * from SecurityDomain", null);
			break;
		case KEYGUARD_MAGAZINE_SETTINGS:
			cs = mDatabase.rawQuery("select * from keyguard_magazine_settings", null);
		}
		return cs;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		int match = uriMatcher.match(uri);
		switch (match) {
		case PRIMARYDOMAIN:
			return CONTENT_TYPE;
		case SECURITYDOMAIN:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		int code = uriMatcher.match(uri);
		switch (code) {
		case PRIMARYDOMAIN:
			mDatabase.update("PrimaryDomain", values, selection, selectionArgs);
			break;
		case SECURITYDOMAIN:
			
			break;
		case KEYGUARD_MAGAZINE_SETTINGS:
			mDatabase.update("keyguard_magazine_settings", values, selection, selectionArgs);
		}
		return 0;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged");
		mNewLanguage = newConfig.locale.getLanguage();
		Log.d(TAG, "onConfigurationChanged mNewLanguage =" + mNewLanguage + " mOldLanguage =" + mOldLanguage);
	    if (mNewLanguage != null && !mNewLanguage.equals(mOldLanguage)) {
	    	Log.d(TAG, "language changed");
	    	int keyguardstyle = 0;
			Cursor cursor = null;
			try {
				cursor = mDatabase.rawQuery("select * from PrimaryDomain", null);
				cursor.moveToFirst();
				keyguardstyle = cursor.getInt(CONSTANTS_KEYGUARD_STYLE);
				ContentValues updateValues = new ContentValues();
				mKeyguardStyle = getContext().getResources().getStringArray(R.array.keyguard_style);
				String keyguard_style_name = mKeyguardStyle[keyguardstyle];
				updateValues.put("keyguard_style_name", keyguard_style_name);
				mDatabase.update("PrimaryDomain", updateValues, "_id = 0", null);
				getContext().getContentResolver().notifyChange(PRIMARYDOMAIN_URI, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				if (cursor != null) {
					cursor.close();
					cursor = null;
				}
			}
		}
	    mOldLanguage = mNewLanguage;
	}
	
}
