package com.android.systemui.statusbar.phone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import java.util.HashMap;

public class SettingManager {
	/**  */
	public static final String TAG = "SettingManager";

	/**
	 * 屏幕特效key
	 */
	public static final String SCROLLANIMATION_KEY = "scrollAnimation";
	/**
	 * 桌面循环切换key
	 */
	public static final String SCREENCYCLE_KEY = "deskScreenCycle";
	/**
	 * 是否显示瀑布流
	 */
	public static final String SHOW_TTWINDOW_KEY = "showTTWindow";
	/**
	 * 壁纸滚动key
	 */
	public static final String WALLPAPERSCROLL_KEY = "wallpaperScroll";
	/**
	 * 图标大小key
	 */
	public static final String ICONSIZE_KEY = "iconSize";
	/**
	 * 壁纸幻灯key
	 */
	public static final String WALLPAPER_LANTERN_KEY = "wallpaperLantern";
	/**
	 * 屏幕点亮切换待机壁纸key
	 */
	public static final String WALLPAPER_AUTO_CHANGE_KEY = "wallpaperAutoChange";
	/**
	 * 壁纸幻灯频率
	 */
	public static final String WALLPAPER_FREQUENCY_KEY = "wallpaperFrequency";
	/**
	 * 是否显示日期和时间
	 */
	public static final String SHOW_DATE_TIME_KEY = "showDateAndTime";
	/**
	 * 是否切换超大日期和时间
	 */
	public static final String SUPER_DATE_TIME_KEY = "superDateAndTime";
	/**
	 * 是否开启精灵解锁功能
	 */
	public static final String SPRITE_LOCKSCREEN_KEY = "spriteLockscreenSwitch";
	
	/**
	 * 图标大小字段
	 */
	public static final String ICON_SMALL = "small";
	public static final String ICON_NORMAL = "normal";
	public static final String ICON_LARGE = "large";

	static final String AUTHORITY = "com.yulong.android.launcher3.settings";
	static final String TABLE_SIMPLESTORAGE = "simplestorage";
	static final String PARAMETER_NOTIFY = "notify";
	static final String KEY = "key";
	static final String VALUE = "value";
	static final String EXTRADATA_URI_STR = "content://" + AUTHORITY + "/" + TABLE_SIMPLESTORAGE;
	/**
	 * The content:// style URL for 通用存储
	 */
	static final Uri EXTRADATA_URI = Uri.parse(EXTRADATA_URI_STR);

	static Uri getContentUriNoNotify(long id) {
		return Uri.parse(EXTRADATA_URI_STR + "/" + id);
	}

	static Uri getContentUri(long id) {
		return getContentUri(id, true);
	}

	static Uri getContentUri(long id, boolean notify) {
		return Uri.parse(EXTRADATA_URI_STR + "/" + id + "?" + PARAMETER_NOTIFY + "=" + notify);
	}

	static Uri getContentUriNoNotify(String key) {
		return Uri.parse(EXTRADATA_URI_STR + "/key_" + key);
	}

	static Uri getContentUri(String key) {
		return getContentUri(key, true);
	}

	static Uri getContentUri(String key, boolean notify) {
		return Uri.parse(EXTRADATA_URI_STR + "/key_" + key + "?" + PARAMETER_NOTIFY + "=" + notify);
	}

	/**  */
	Context mContext;

	/**
	 * DOCUMENT ME!
	 */
	/**
     * 
     */
	HashMap<String, String> mExtraDataMap = new HashMap<String, String>();

	/**
	 * DOCUMENT ME!
	 */
	/**
     * 
     */
	HashMap<String, Integer> mExtraIdMap = new HashMap<String, Integer>();

	/**
	 * 
	 * @param context
	 *            context
	 */
	private SettingManager(Context context) {
		mContext = context;
	}

	/**
	 * 
	 DOCUMENT ME!
	 * 
	 * @param context
	 *            context
	 * 
	 * @return SettingManager
	 */
	public static SettingManager createInstance(Context context) {
		return new SettingManager(context);
	}

	/**
	 * 初始化并载入管理的数据
	 * 
	 * @return boolean
	 */
	public synchronized boolean loadData() {
		mExtraDataMap.clear();
		mExtraIdMap.clear();
		// 载入数据库数据//
		final ContentResolver contentResolver = mContext.getContentResolver();
		final Cursor c = contentResolver.query(EXTRADATA_URI, null, null, null, null);

		if (c == null) {
			return true;
		}

		try {
			final int idIndex = c.getColumnIndexOrThrow(BaseColumns._ID);
			final int keyIndex = c.getColumnIndexOrThrow(KEY);
			final int valueIndex = c.getColumnIndexOrThrow(VALUE);

			while (c.moveToNext()) {
				int id = c.getInt(idIndex);
				String key = c.getString(keyIndex);
				String value = c.getString(valueIndex);
				mExtraDataMap.put(key, value);
				mExtraIdMap.put(key, id);
			}
		} catch (Exception e) {
			Log.e(TAG, "init() error");
		} finally {
			c.close();
		}

		return true;
	}

	/**
	 * 
	 DOCUMENT ME!
	 * 
	 * @param key
	 *            key
	 * 
	 * @return String
	 */
	public synchronized String getData(String key) {
		return this.mExtraDataMap.get(key);
	}

	/**
	 * 这里要自己确保数据的有效性
	 * 
	 * @param key
	 *            key
	 * 
	 * @return int
	 */
	public synchronized int getIntData(String key) {
		int intData = -1;
		try {
			intData = Integer.parseInt(getData(key));
		} catch (NumberFormatException e) {
			Log.d("SettingManager", "NumberFormatException!, string data is :" + getData(key));
		}
		return intData;
	}

	/**
	 * 这里要自己确保数据的有效性
	 * 
	 * @param key
	 *            key
	 * 
	 * @return boolean
	 */
	public synchronized boolean getBooleanData(String key) {
		return Boolean.parseBoolean(getData(key));
	}

	/**
	 * 如果没有此数据，则自动添加
	 * 
	 * @param key
	 *            key
	 * @param defaultValue
	 *            defaultValue
	 * 
	 * @return String
	 */
	public synchronized String getData(String key, String defaultValue) {
		Integer id = this.mExtraIdMap.get(key);

		if (id == null) {
			this.setData(key, defaultValue);
		}

		return this.mExtraDataMap.get(key);
	}

	/**
	 * 这里要自己确保数据的有效性
	 * 
	 * @param key
	 *            key
	 * @param defaultValue
	 *            defaultValue
	 * 
	 * @return int
	 */
	public synchronized int getIntData(String key, int defaultValue) {
		return Integer.parseInt(getData(key, "" + defaultValue));
	}

	/**
	 * 这里要自己确保数据的有效性
	 * 
	 * @param key
	 *            key
	 * @param defaultValue
	 *            defaultValue
	 * 
	 * @return int
	 */
	public synchronized boolean getBooleanData(String key, boolean defaultValue) {
		return Boolean.parseBoolean(getData(key, "" + defaultValue));
	}

	public synchronized boolean setData(String key, String value) {
		return this.setData(key, value, true);
	}

	/**
	 * 参数不允许为null
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 * 
	 * @return boolean
	 */
	public synchronized boolean setData(String key, String value, boolean notify) {
		if ((key == null) || (value == null)) {
			return false;
		}

		Integer id = this.mExtraIdMap.get(key);
		final ContentValues values = new ContentValues();
		final ContentResolver cr = mContext.getContentResolver();
		values.put(KEY, key);
		values.put(VALUE, value);

		if (id != null) {
			int c = cr.update(getContentUri(key), values, null, null);
			//String.valueOf(c);
		} else {
			Uri result = cr.insert(EXTRADATA_URI, values);

			if (result == null) {
				return false;
			}

			this.mExtraIdMap.put(key, id);
		}

		this.mExtraDataMap.put(key, value);

		return true;
	}

	/**
	 * 
	 DOCUMENT ME!
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 * 
	 * @return boolean
	 */
	public synchronized boolean setData(String key, int value) {
		return this.setData(key, "" + value);
	}

	/**
	 * 
	 DOCUMENT ME!
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 * 
	 * @return boolean
	 */
	public synchronized boolean setData(String key, boolean value) {
		return this.setData(key, "" + value);
	}
}
