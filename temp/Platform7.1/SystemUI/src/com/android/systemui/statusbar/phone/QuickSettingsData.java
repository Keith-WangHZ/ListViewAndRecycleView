package com.android.systemui.statusbar.phone;

import java.util.ArrayList;

import android.view.View;

public class QuickSettingsData {

	public static final class Entry {
		public int id;
		public int state;
		public int order;
		public View view;
	}

	public static final int QS_ID_WLAN = 0;				// WLAN
	public static final int QS_ID_MOBILEDATA = 1;      	// 数据网络	
	public static final int QS_ID_AIRPLANE_MODE = 2;    // 飞行模式
	public static final int QS_ID_BRIGHTNESS = 3;		// 亮度/手电筒，暂不实现
	public static final int QS_ID_SPEED = 4;			// 一键加速	
	public static final int QS_ID_ROTATION = 5;			// 自动旋转	
	public static final int QS_ID_LOCATION = 6;			// 位置服务	
	public static final int QS_ID_SOFTAP = 7;			// 热点
	public static final int QS_ID_BLUETOOTH = 8;		// 蓝牙
	//前面9个用于两页显示
	public static final int QS_ID_RINGER_MODE = 9;    	// 静音
	public static final int QS_ID_SAFESWITCH = 10; 		// 流量监控
	public static final int QS_ID_SLIENCE = 11;			// 屏幕截图，暂不用
	public static final int QS_ID_VIPLIST = 12;			// VIP，骚扰拦截，免打扰	
	public static final int QS_ID_SCENEMODE = 13;    	
	public static final int QS_ID_SUPERSAVING_POWERMODE = 14;   // 超级省电模式
	public static final int QS_ID_CBUTTON = 15;         		// C键 
	public static final int QS_ID_SINGLE_HAND_OPERATION_MODE = 16;    //智能缩屏，智能缩屏
	public static final int QS_ID_SELECTNETWORK = 17;   		// 新规范，选择卡1和卡2
	public static final int QS_ID_SWITCHNETWORKTYPE = 18;    	//3G/4G优先
	public static final int QS_ID_APNLIST = 19;         		// apn列表 
	//下面的忽略，不支持
	public static final int QS_ID_POWERMODE = 20;		// 省电
	public static final int QS_ID_VIBRATE_RING_MODE = 21;		
	public static final int QS_ID_SETTING   = 22;		// 设置 just for 4.1
	public static final int QS_ID_DOUBLECARD = 23;		// 双卡选择
	public static final int QS_ID_SDCARD = 24;			// sd卡
	public static final int QS_ID_NOTIFY = 25;			// 通知 just for 4.1
	public static final int QS_ID_BLACKLIST = 26;		// 通信卫士,骚扰拦截
	public static final int QS_ID_MUTLWINDOW = 27;		// 智能多屏 （目前只有5950支持）
	public static final int QS_ID_NFC = 28;				// NFC
	public static final int QS_ID_SECURE_MODE = 29;		// 安全模式，一键数据保护
	public static final int QS_ID_DRIVING_MODE = 30;	// 车载模式
	public static final int QS_ID_SOUND = 31;			// 声音
	public static final int QS_ID_DATA_ROAMING = 32;	// 数据漫游
	public static final int QS_ID_TTWINDOW = 33;        // 瀑布流
	public static final int QS_ID_DATAPROTECTION = 34;   // 数据保护，Coolmini的入口	
	public static final int QS_ID_MUL_ACCOUNTS = 35;        // 多账户	
	public static final int QS_ID_SUPER_SECURE_MODE = 36;    // 超级安全
	public static final int QS_ID_FLASHLIGHT_MODE = 37;    // 手电筒
	public static final int QS_ID_DND_MODE = 38;
	public static final int QS_ID_WFC_MODE = 39;

    // yulong begin: add for systemUI 6.0 ,by shenyupeng,2016.01.20
    public static final int QS_ID_4G = 40;
    public static final int QS_ID_RECORD_SCREEN = 42;
    public static final int QS_ID_CONTROL_CENTOR = 43;
    public static final int QS_ID_MAGICAL = 44; // 魔音
    public static final int QS_ID_WHITE_BLACK  = 45; // 黑白模式
    public static final int QS_ID_NIGHT_MODE = 46;
    public static final int QS_ID_QUICK_PAY = 47;
    // 固定应用
    public static final int QS_ID_SEARCH = 50; // 搜索
    public static final int QS_ID_CALCULATOR = 51; // 计算器
    public static final int QS_ID_BLASTER = 52; // 录音机
    public static final int QS_ID_CAMERA = 53; // 相机

    // yulong end

	private final ArrayList<Entry> mEntries = new ArrayList<Entry>();

}
