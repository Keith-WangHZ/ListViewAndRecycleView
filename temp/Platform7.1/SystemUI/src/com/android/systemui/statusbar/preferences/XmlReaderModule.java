package com.android.systemui.statusbar.preferences;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Xml;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.helper.LogHelper;
import com.yulong.android.feature.FeatureConfig;
import com.yulong.android.feature.FeatureString;

public class XmlReaderModule {
	private String mControlmapTag;
	private String mBlackTag;
	private String mWhiteTag;
	private String mSystemTag;
	private String mSystemListTag;
	private String mVipTag;
	private String mItem;
	private String mKeyToken;
	private String mValueToken;
	private String mXmlAddress;
	private String mXmlAddressSystem;
	private XmlDataBean bean;
	public static final String TAG = "XmlReaderModule";
	private final String mRootDirectory = "data/system/seccenter/";
	private final String mRootDirectorySystem = "system/lib/uitechno/";
	
	static private XmlReaderModule self = null;
	static public XmlReaderModule getInstance(String file){
		if (self == null){
			self = new XmlReaderModule(file);
			LogHelper.sd(TAG,"new XmlReaderModule");
		}
		return self;
	}
	public XmlReaderModule(String file) {
		//默认安全配置文件
		mControlmapTag = "controlmap";
	    mBlackTag = "blacklist";
	    mWhiteTag = "whitelist";
	    mSystemTag = "system_blacklist";
	    mVipTag = "viplist";
	    mItem = "item";
	    mKeyToken = "key";
	    mValueToken = "value";
	    mSystemListTag = "systemlist";
	    mXmlAddress = mRootDirectory+ file;
	    //FeatureConfig mFeatureConfig = new FeatureConfig();
        String mDeviceName;
        mDeviceName = FeatureConfig.getValue(FeatureString.DEVICE_NAME);
        String flag = SystemProperties.get("yulong.radio.is_united_cpb", "0");
        String filename = flag.equals("1")? ("system/lib/uitechno_"+mDeviceName+"/"+file) : ("system/lib/uitechno/"+file);
        LogHelper.sd(TAG, "XmlReaderModule.........filename="+filename);
	    mXmlAddressSystem = filename;
	}

	
	public XmlReaderModule(String controlmapTag, String black, String white,String sysTag,
	        String vip, String item,
			String key, String value, String address) {
		mControlmapTag = controlmapTag;
		mBlackTag = black;
		mWhiteTag = white;
		mSystemTag = sysTag;
		mVipTag = vip;
		mItem = item;
		mKeyToken = key;
		mValueToken = value;
		mXmlAddress = address;
		mXmlAddressSystem = "";
	}
	HashMap<String, String> systemListMap = new HashMap<String, String>();
	HashMap<String, String> systemListMapStartWith = new HashMap<String, String>();

	public XmlDataBean getXmlDatas(Context context) {
		LogHelper.sd(TAG,"getXmlDatas");

		HashMap<String, String> blackMap = new HashMap<String, String>();;
		HashMap<String, String> whiteMap = new HashMap<String, String>();
		HashMap<String, String> whiteMapStartWith = new HashMap<String, String>();
		HashMap<String, String> vipMap = new HashMap<String, String>();
		HashMap<String, String> ControlMap = new HashMap<String, String>();
		HashMap<String, String> systeMap = new HashMap<String, String>();
		
		HashMap<String, Integer> whiteMapFinal = new HashMap<String, Integer>();
		ArrayList<String> whiteMapList0 = new ArrayList<String>();
		ArrayList<String> whiteMapList1 = new ArrayList<String>();
		ArrayList<String> whiteMapList2 = new ArrayList<String>();
		ArrayList<String> whiteMapList3 = new ArrayList<String>();
		ArrayList<String> whiteMapList4 = new ArrayList<String>();
		
		systemListMap = new HashMap<String, String>();
		systemListMapStartWith = new HashMap<String, String>();
		bean = new XmlDataBean();
		bean.mWhiteMap = whiteMap;
		bean.mWhiteMapStartWith = whiteMapStartWith;
		bean.mBlackMap = blackMap;
		bean.mVipMap = vipMap;
		bean.mControlmap = ControlMap;
		bean.mSysMap = systeMap;
		bean.mSystemListMap = systemListMap;
		bean.mSystemListMapStartWith = systemListMapStartWith;
		
		bean.mWhiteMapFinal = whiteMapFinal;
		bean.mWhiteMapList0 = whiteMapList0;
		bean.mWhiteMapList1 = whiteMapList1;
		bean.mWhiteMapList2 = whiteMapList2;
		bean.mWhiteMapList3 = whiteMapList3;
		bean.mWhiteMapList4 = whiteMapList4;
		
		if (mBlackTag == null || mBlackTag.isEmpty() || mWhiteTag == null
				|| mWhiteTag.isEmpty() || mVipTag == null || mVipTag.isEmpty()
				|| mItem == null || mItem.isEmpty() || mKeyToken == null
				|| mKeyToken.isEmpty() || mValueToken == null
				|| mValueToken.isEmpty()) {
			LogHelper.sd(TAG, "Bad Params!");
			return bean;
		}		
		
		int flagSystem = 0;
//		FileReader permReader = null;
		InputStreamReader inputStreamReader = null;
		InputStream input = null;
		BufferedReader br = null;
		ByteArrayInputStream stream;
		String temp;
		String result = "";
		XmlPullParser parserDownload = null;
		try {
		    File fileDirectory = new File(mRootDirectory);
		    if(!fileDirectory.exists()){
		        fileDirectory.mkdir();
		        flagSystem = 1;
		    }else{
				File file = new File(mXmlAddress);	
				LogHelper.sd(TAG,"mXmlAddress="+mXmlAddress);
				if (file.exists()){
					input = new FileInputStream(file);
					inputStreamReader = new InputStreamReader(input);
					br = new BufferedReader(inputStreamReader);
					while ((temp = br.readLine()) != null) {
						result = result + temp;
					}
					stream = new ByteArrayInputStream(  
			                result.getBytes());  
					parserDownload = Xml.newPullParser();
					parserDownload.setInput(stream, "UTF-8");					
				}else{
					flagSystem = 1;
				}
		    }
		} catch (Exception e) {
			LogHelper.sd(TAG, "readFromXml : Couldn't find or open autopush XML for directory seccenter " + e);
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (Exception e2) {
					// TODO: handle exception
				}
			}
		}
		
		try {
			if (flagSystem == 1) {
				File file = new File(mXmlAddressSystem);
				LogHelper.sd(TAG, "mXmlAddressSystem=" + mXmlAddressSystem);
				if (file.exists()) {
					input = new FileInputStream(file);
					inputStreamReader = new InputStreamReader(input);
					br = new BufferedReader(inputStreamReader);
					while ((temp = br.readLine()) != null) {
						result = result + temp;
					}
					stream = new ByteArrayInputStream(result.getBytes());
					parserDownload = Xml.newPullParser();
					parserDownload.setInput(stream, "UTF-8");
				}
			}
		    XmlPullParser parser = SystemUIApplication.getContext().getResources().getXml(R.xml.seccenter_notice_applist);
		    do{
				int eventType = parser.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					switch (eventType) {
					case XmlPullParser.START_DOCUMENT:
						LogHelper.sd("","Start Document");
						eventType = parser.next();
						break;
					case XmlPullParser.START_TAG:
						if (parser.getName().equals(mBlackTag)) {
							getItems(parser, blackMap, mBlackTag,null);
							eventType = parser.next();
						} else if (parser.getName().equals(mWhiteTag)) {
							getItems(parser, whiteMap, mWhiteTag,whiteMapStartWith);
							eventType = parser.next();

//						}else if (parser.getName().equals(mSystemTag)) {
//	                        getItems(parser, systeMap, mSystemTag,null);
//	                        eventType = parser.next();
//	                    }else if (parser.getName().equals(mVipTag)) {
//							getItems(parser, vipMap, mVipTag,null);
//							eventType = parser.next();
//						} else if (parser.getName().equals(mControlmapTag)) {
//							getItems(parser, ControlMap, mControlmapTag,null);
//							eventType = parser.next();
						}else {
							eventType = parser.next();
						}
						break;
					default :
						eventType = parser.next();
					}					
				}
				parser = parserDownload;
				parserDownload = null;
		    }while(parser != null);		    			
		} catch (Exception e) {
			//e.printStackTrace();
			LogHelper.sd(TAG, "readFromXml : Couldn't find or open autopush XML " + e);
			/*try {
			if (br != null) {
				br.close();
			}
			} catch (Exception e11) {
			}
			try {
				if (inputStreamReader != null) {
					inputStreamReader.close();
				}
			} catch (Exception e12) {
			}
			try {
				if (input != null) {
					input.close();
				}
			} catch (Exception e13) {
			}*/
			flagSystem = -1;
			//return bean;
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (Exception e3) {
					// TODO: handle exception
				}
			}
		}

		if(-1 == flagSystem){
		  return bean;
		}

		if (bean != null && bean.mWhiteMap != null) {
			for (Map.Entry<String, String> entry : bean.mWhiteMap.entrySet()) {
				int total = 0;
				int pow = 1;
				String str = entry.getValue();
				if(str.length() >= 12 && str.length()<=15){
					char cc[] = new char[5];
					int nn[] = new int[5];
					for(int j=0; j<nn.length; j++){
						nn[j] = 0;
					}
					int max = str.length()/3-1;
					for(int i=str.length()/3-1; i>=0; i--){
						char c = str.charAt(3*i);//1A_1B_1C_1D
						cc[i] = str.charAt(3*i);
						
						try {
							nn[i] = Integer.parseInt(String.valueOf(cc[i]))%2;
							
							int num = Integer.parseInt(String.valueOf(c))%2;
							total += num*pow;
							pow *= 2;
						} catch (Exception e) {
							// TODO: handle exception
							LogHelper.sd(TAG, "Parse error at key="+entry.getKey()+" value="+entry.getValue());
							break;
						}
					}//for
					if(nn[0] == 1){
						whiteMapList0.add(entry.getKey());
					}
					if(nn[1] == 1){
						whiteMapList1.add(entry.getKey());
						//LogHelper.sd(TAG,"whiteMapList1 key= " + entry.getKey()+ " and value= " + entry.getValue()+" total="+total);
					}
					if(nn[2] == 1){
						whiteMapList2.add(entry.getKey());
						//LogHelper.sd(TAG,"whiteMapList2 key= " + entry.getKey()+ " and value= " + entry.getValue()+" total="+total);
					}
					if(nn[3] == 1){
						whiteMapList3.add(entry.getKey());
						//LogHelper.sd(TAG,"whiteMapList3 key= " + entry.getKey()+ " and value= " + entry.getValue()+" total="+total);
					}
					if(nn[4] == 1){
						whiteMapList4.add(entry.getKey());
						//LogHelper.sd(TAG,"whiteMapList3 key= " + entry.getKey()+ " and value= " + entry.getValue()+" total="+total);
					}
				}
				
				whiteMapFinal.put(entry.getKey(), total);
//				LogHelper.sd(TAG,"initNtfConfigList key= " + entry.getKey()+ " and value= " + entry.getValue()+" total="+total);
			}
		}
		/*		
		try {
			if (br != null) {
				br.close();
			}
		} catch (Exception e21) {
		}
		try {
			if (inputStreamReader != null) {
				inputStreamReader.close();
			}
		} catch (Exception e22) {
		}
		try {
			if (input != null) {
				input.close();
			}
		} catch (Exception e23) {
		}*/
		return bean;
	}
	public XmlDataBean getXmlDataBean(){
		return bean;
	}

	private void getItems(XmlPullParser xpp,
			HashMap<String, String> map, String tag,HashMap<String, String> mapStartWith){
		while (true) {
			try {
				xpp.next();
				if ((xpp.getEventType() == XmlPullParser.END_TAG && xpp.getName()
						.equals(tag))
						|| xpp.getEventType() == XmlPullParser.END_DOCUMENT) {
					break;
				}
				if (xpp.getEventType() == XmlPullParser.START_TAG) {
					if (xpp.getName().equals(mItem)) {
						if (xpp.getAttributeCount() == 2
								&& xpp.getAttributeName(0).equals(mKeyToken)
								&& xpp.getAttributeName(1).equals(mValueToken)) {
							map.put(xpp.getAttributeValue(0),
									xpp.getAttributeValue(1));							
							String pkg = xpp.getAttributeValue(0);
							if (pkg.endsWith("*") && mapStartWith != null){
								mapStartWith.put(pkg.replace("*", ""),
										xpp.getAttributeValue(1));								
							}
						}
					}else if(xpp.getName().equals(mSystemListTag)){
						getItems(xpp, systemListMap, mWhiteTag,systemListMapStartWith);
					}
				}
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
	}

}
