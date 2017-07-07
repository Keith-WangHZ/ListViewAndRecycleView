package com.android.systemui.statusbar.preferences;

import java.util.ArrayList;
import java.util.HashMap;

public class XmlDataBean {
	/**

	 */
	public HashMap<String, String> mControlmap;
	/**
	 */
	public HashMap<String, String> mWhiteMap;
	
	public HashMap<String, String> mWhiteMapStartWith;
	public HashMap<String, String> mBlackMap;
	public HashMap<String, String> mVipMap;
	public HashMap<String, String> mSysMap;
	public HashMap<String, String> mSystemListMap;
	public HashMap<String, String> mSystemListMapStartWith;
	
	public HashMap<String, Integer> mWhiteMapFinal;
	public ArrayList<String> mWhiteMapList0;
	public ArrayList<String> mWhiteMapList1;
	public ArrayList<String> mWhiteMapList2;
	public ArrayList<String> mWhiteMapList3;
	public ArrayList<String> mWhiteMapList4;
	
	public XmlDataBean() {
		mWhiteMap = null;
		mBlackMap = null;
		mVipMap = null;
		mSystemListMap = null;
		mWhiteMapFinal = null;
		mWhiteMapList0 = null;
		mWhiteMapList1 = null;
		mWhiteMapList2 = null;
		mWhiteMapList3 = null;
	}
	
}
