package com.android.systemui.keyguard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KeyguardDataBaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "Keyguard.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_PRIMARY = "PrimaryDomain";
	private static final String TABLE_KEYGUARD_MAGAZINE_SETTINGS = "keyguard_magazine_settings";
	private static final String TABLE_SECURITY = "SecurityDomain";

	public KeyguardDataBaseHelper(Context context) {
		// TODO Auto-generated constructor stub
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		String sql_primary = "CREATE TABLE IF NOT EXISTS " + TABLE_PRIMARY + 
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, keyguard_style INTEGER, keyguard_style_name VARCHAR, keyguard_3rd_type INTEGER)";  
        db.execSQL(sql_primary);  
        
        String sql_keyguard_magazine_settings_pd =  "CREATE TABLE IF NOT EXISTS " + TABLE_KEYGUARD_MAGAZINE_SETTINGS + 
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, guide_shown INTEGER, disclaimer_reminded INTEGER, is_auto_update BOOLEAN, is_can_scroll BOOLEAN ,is_show_icon BOOLEAN)";  
        db.execSQL(sql_keyguard_magazine_settings_pd);  
        
        String sql_security = "CREATE TABLE IF NOT EXISTS " + TABLE_SECURITY +  
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, keyguard_style INTEGER, keyguard_style_name VARCHAR, keyguard_3rd_type INTEGER)";  
        db.execSQL(sql_security);  

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRIMARY);
		onCreate(db);
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYGUARD_MAGAZINE_SETTINGS);
		onCreate(db);
		
		db.execSQL("DROP TABLE IF EXISTS "+ TABLE_SECURITY);
		onCreate(db);
	}

}
