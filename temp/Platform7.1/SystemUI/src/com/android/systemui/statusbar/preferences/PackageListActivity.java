package com.android.systemui.statusbar.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.systemui.R;
import com.android.systemui.SystemBarTintManager;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.misc.YLUtils;
import com.android.systemui.statusbar.StatusBarIconView;
import com.yulong.android.common.app.CommonActivity;
import com.yulong.android.common.view.TopBar;
import com.yulong.android.common.view.TopBar.TopBarStyle;
import com.yulong.android.common.widget.CommonSwitch;
import com.yulong.android.common.widget.CommonSwitch.OnCheckedChangeListener;
import com.yulong.android.view.tab.TabBar;

import android.util.LruCache;
public class PackageListActivity extends CommonActivity {
	private MyListAdapter adapter;
	private View curView;
	private TextView mTitle;
	private ImageView mBack;
	
	private ListView listView;
	private CommonSwitch mAllCheckedNotificationBox;
	private View mBatchDisposeView;
	private Boolean mAllCheckedNotification = false;
	private Boolean mCheckedAllIsSetting = false;
	private Set<Integer> mPositionSet  = new HashSet<Integer>();
	private int mStyle = 0;
	private boolean mKeyValue_1;
	private String mKeyString_1;
    private TopBar mTopbar;
    private TabBar mTabbar;
    private int mTopBarHeight;
    private boolean mShowSystemApps = false;
	private final static String TAG = "PackageListActivity";
	
	private int mUserId = 0;
	private PackageManager mPm;
	private List<ResolveInfo> mIntents;
	static final private int GET_CODE = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.preference_package_listview);
        View body = setBodyLayout(R.layout.preference_package_listview);
        body.setPadding(0, mTopBarHeight, 0, 0);
        
        //LogHelper.sd(TAG, "onCreate()_1000");
        setStatusBarOverlayingActivity(PackageListActivity.this);
        mStyle = Utilities.getCoolpadThemeStyle();
        if (1 == mStyle) {//black gold
        } else {
        	this.getWindow().setBackgroundDrawableResource(R.drawable.background_holo_light_young);
        }
        //initActionBar();
        //setStatusBarGradientColor();
        mUserId = UserHandle.myUserId();;
        mPm = getPackageManager();
		Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
        .addCategory(Intent.CATEGORY_LAUNCHER);
		mIntents = mPm.queryIntentActivitiesAsUser(launchIntent,
                PackageManager.GET_DISABLED_COMPONENTS, mUserId);
        mapAllSysPackagePrepare(PackageListActivity.this);
        //LogHelper.sd(TAG, "onCreate()_1001");
        
        
        
        listView = (ListView) findViewById(R.id.listview);
		
        adapter = new MyListAdapter(this);
        try{
        	View convertView = LayoutInflater.from(this).inflate(
					R.layout.preference_package_list_item_top, null);
			
        	convertView.setClickable(false);
        	mAllCheckedNotificationBox = (CommonSwitch)convertView.findViewById(R.id.sub_switch_widget0);
        	if(mAllCheckedNotificationBox != null){
        		//mAllCheckedNotificationBox.setChecked(mAllCheckedNotification);
        		mAllCheckedNotificationBox.setOnCheckedChangeListener(mCheckListener);
        		mKeyString_1 = NotificationCollapseManage
        				.getMasterNotificationKey();
        		mKeyValue_1 = NotificationCollapseManage.getDefault(this)
        				.getMasterNotificationKeyValue();
        		mAllCheckedNotificationBox.setChecked(mKeyValue_1);
        	}
        	mBatchDisposeView = convertView.findViewById(R.id.package_list_top);
        	mBatchDisposeView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
		        	intent.setClass(getApplicationContext(), PackageListActivityManager.class);
		        	intent.putExtra("ShowSystemApps", mShowSystemApps);
		        	startActivityAsUser(intent,UserHandle.OWNER);
				}
			});
        	if (Utilities.isSupportOversea()) {
        		View v = convertView.findViewById(R.id.package_list_divider_top);
        		if(v != null){
        			v.setVisibility(View.GONE);
        		}
        		mBatchDisposeView.setVisibility(View.GONE);
    		}
        	
        	listView.addHeaderView(convertView,null,false);//disable click event
			
        	listView.setAdapter(adapter);    	       	
        }catch(NotFoundException e){
        	LogHelper.se("PackageListActivity", "onResume()_exception " + e);
        }      
        
        listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				// TODO Auto-generated method stub
				//LogHelper.sd(TAG,"140000");
				String packageName = "default.package.name";
	        	String appName = "default.app.name";
	        	TextView text = (TextView)v.findViewById(R.id.app_package);
	        	if (text != null){
	        		packageName = text.getText().toString();
	        	}else{
	        		return;
	        	}
	        	Intent intent = new Intent();
	        	intent.setClass(PackageListActivity.this, PackageSettingsActivity.class);
	        	
	        	intent.putExtra("package", packageName);
	        	text = (TextView)v.findViewById(R.id.app_name);
	        	if (text != null){
	        		appName = text.getText().toString();
	        	}
	        	intent.putExtra("app_name", appName);
	        	startActivityForResultAsUser(intent,GET_CODE,UserHandle.OWNER);  
			}
        	 
        });
    }
    
    
    @Override
	protected void onResume() {
	    super.onResume();
//		setActionBarTitle(getResources().getString(R.string.title_activity_package_list));
//		setActionBarBackButtonVisibility(true);
	}
    @Override
    protected void onCreateTopBar(TopBar topBar) {
        super.onCreateTopBar(topBar);
        topBar.setTopBarStyle(TopBarStyle.TOP_BAR_NOTMAL_STYLE);
        topBar.setTopBarTitle(getResources().getString(R.string.title_activity_package_list));
        topBar.setTopBarTitleSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
        mTabbar = topBar.getTabBar();
        mTopBarHeight = topBar.getHeight();
   		Menu menu = topBar.getMenu();
   		MenuItem item1 = menu.add(getResources().getString(R.string.manage_notification_access_title));
   		item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
   			@Override
   			public boolean onMenuItemClick(MenuItem item) {
   				try {
   					startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
				} catch (Exception e) {
					e.printStackTrace();
				}
   				return false;
   			}
   		});
   		final MenuItem item2 = menu.add(getResources().getString(R.string.notification_show_system_app));
   		item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
   			@Override
   			public boolean onMenuItemClick(MenuItem item) {
   				if(mShowSystemApps){
   					mShowSystemApps = false;
   					item2.setTitle(getResources().getString(R.string.notification_show_system_app));
   				}else{
   					mShowSystemApps = true;
   					item2.setTitle(getResources().getString(R.string.notification_hide_system_app));
   				}
   				mapAllSysPackageExist(PackageListActivity.this);
   				adapter.notifyDataSetChanged();
   				return false;
   			}
   		});
	}
    
    private final OnCheckedChangeListener mCheckListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CommonSwitch toggle, boolean checked) {
			// TODO Auto-generated method stub
			final Boolean b = (Boolean)checked;
        	if(mAllCheckedNotification != b){
        		mAllCheckedNotification = b;
        		
				final SharedPreferences.Editor se = PreferenceManager
						.getDefaultSharedPreferences(
								PackageListActivity.this).edit();
				se.putBoolean(mKeyString_1, b);
				se.commit();
        	    Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				sendBroadcast(intent);
        	}
		}
    };
   
    public void onClick(View v){
    	//LogHelper.sd(TAG,"140001");
    	curView = v;
    	String packageName = "default.package.name";
    	String appName = "default.app.name";
    	TextView text = (TextView)v.findViewById(R.id.app_package);
    	if (text != null){
    		packageName = text.getText().toString();
    	}else{
    		return;
    	}
    	
    	Intent intent = new Intent();
    	intent.setClass(this, PackageSettingsActivity.class);
    	
    	intent.putExtra("package", packageName);
    	text = (TextView)v.findViewById(R.id.app_name);
    	if (text != null){
    		appName = text.getText().toString();
    	}
    	intent.putExtra("app_name", appName);
    	startActivityForResultAsUser(intent,GET_CODE,UserHandle.OWNER);	    	
    }
    
   @Override
	protected void onActivityResult(int requestCode, int resultCode,
		Intent data) {
	   if (curView != null && adapter!=null){
			try {
				ViewHolder vh = (ViewHolder)curView.getTag();
				if(vh != null && vh.sub_switch_widget1 != null){
					View child = adapter.getView((Integer) vh.sub_switch_widget1.getId(),
							curView, listView);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		   //listView.addView(child, (Integer)curView.getTag());
	   }
        if (requestCode == GET_CODE) {
            if (resultCode == RESULT_CANCELED) {
            } else {
            	data.getAction();       
            }
        }
    }    
   static private String controlCenterNotificationPkg[]={
   	};
   
   static private String controlCenterNotificationPkgNotIn[]={
};
   
   static private Map<String,Integer> mapPackageList = new HashMap<String,Integer>();
   
   static public boolean isPackageInRestrictions(String pkg){
	   int val = SysAppProvider.getInstance(null).isShowNotificationInCenter(pkg);
	   if(val == 1){
		   return true;
	   }else if(val == 0){
		   return false;
	   }
	   
//	   for(String s:controlCenterNotificationPkg){
//      		if(s.equalsIgnoreCase(pkg)){
//      			return true;
//      		}
//  		}
//	   for(String s:controlCenterNotificationPkgNotIn){
//     		if(s.equalsIgnoreCase(pkg)){//
//     			return false;
//     		}
// 		}
      	return true;
   }
   
   static public boolean isPackageInYulong(String pkg){
	   int val = SysAppProvider.getInstance(null).isShowNotificationInCenter(pkg);
	   if(val == 1){
		   return true;
	   }
	   
	   for(String s:controlCenterNotificationPkg){
      		if(s.equalsIgnoreCase(pkg)){
      			return true;
      		}
  		}
      	return false;
   }
   
    private List<PackageInfo> packageInfosTemp;
	private List<PackageInfo> packageInfos;
	private static List<PackageInfo> packageInfosAll;
	private static List<PackageInfo> packageInfosHideSystem;
	public static final List<PackageInfo> getPackAll() {
		return packageInfosAll;
	}
	public static final List<PackageInfo> getPackHideSystem() {
		return packageInfosHideSystem;
	}
	public static final void setPackAll() {
		packageInfosAll = new ArrayList<PackageInfo>();
	}
	public static final void setPackHideSystem() {
		packageInfosHideSystem = new ArrayList<PackageInfo>();
	}
	public boolean isSystemApp(PackageInfo packageInfo){
		if(packageInfo == null){
			return false;
		}
		Boolean b = true;
		b = (packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        		|| (packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
		if(b == false){
			return b;
		}
		b = true;
		if(mIntents != null){
			final int N = mIntents.size();
	        for (int j = 0; j < N; j++) {
	            String packageName = mIntents.get(j).activityInfo.packageName;
	            //LogHelper.sd("","j="+j+" pkg="+packageName);
	            if(packageName.equalsIgnoreCase(packageInfo.packageName)){
	            	b = false;
	            	return b;
	            }
	        }
		}
        //LogHelper.sd("","flags="+b+" pkg="+packageInfo.packageName+" mUserId="+mUserId);//Integer.toHexString(packageInfo.applicationInfo.flags)
        return b;
	}
	public void mapAllSysPackagePrepare(Context context){
    	packageInfosTemp = mPm.getInstalledPackagesAsUser(0, mUserId);
    	//int usid = ActivityManager.getCurrentUser();////UserHandle.USER_CURRENT//ActivityManager.getCurrentUser()
    	//LogHelper.sd("","size="+packageInfosTemp.size()+" uid="+usid+" musid="+mUserId);
//    	List<PackageInfo> packageInfosTemp = context.getPackageManager()
//				.getInstalledPackages(0, 0);
//		List<PackageInfo> packageInfosTemp1 = context.getPackageManager()
//				.getInstalledPackages(0, 10);
    	packageInfos = new ArrayList<PackageInfo>();

		int count = 0;
		int nn = 0;
        for(PackageInfo packageInfo:packageInfosTemp){
            String packageName = packageInfo.packageName;
            if (isSystemApp(packageInfo)){
            	nn++;
            }else{
            	 count++;
//                 LogHelper.sd("","packageInfo.packageName="+packageInfo.packageName+" title="+
//                         packageInfo.applicationInfo.loadLabel(PackageListActivity.this.getPackageManager())+
//                         " count="+count);
            	if(isPackageInRestrictions(packageInfo.packageName)){
            		
            		packageInfos.add(packageInfo);
            	}
            	if(count >= 8){
            		break;
            	}
            }
        }
        
        AsyncTask switchDdsAsyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }
            @Override
            protected Void doInBackground(Void... params) {
            	mapAllSysPackage(PackageListActivity.this);
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                packageInfos.clear();
                for(PackageInfo packageInfo:packageInfosHideSystem){
                	packageInfos.add(packageInfo);
                }
                adapter.notifyDataSetChanged();
            }
        }.execute();
	}
    public void mapAllSysPackage(Context context){
//    	packageInfosAll = new ArrayList<PackageInfo>();
//    	packageInfosHideSystem = new ArrayList<PackageInfo>();
    	setPackAll();
    	setPackHideSystem();
    	packageInfosAll.clear();
    	packageInfosHideSystem.clear();
		boolean bAll = getIntent().getBooleanExtra("all_package",true);
		int count = 0;
        for(PackageInfo packageInfo:packageInfosTemp){
            String packageName = packageInfo.packageName;
            if (isSystemApp(packageInfo)){
            }else{
            	 count++;
            	if(isPackageInRestrictions(packageInfo.packageName)){
            		packageInfosHideSystem.add(packageInfo);
            		packageInfosAll.add(packageInfo);
            	}
            }
        }
        int size=packageInfos.size();
        int inc=size;
        int inc_1=size;
        for(PackageInfo packageInfo:packageInfosTemp){
        	
            String packageName = packageInfo.packageName;
            
            if (isSystemApp(packageInfo)){
            	count++;
//                LogHelper.sd("","packageInfo.packageName="+packageInfo.packageName+" title="+
//                packageInfo.applicationInfo.loadLabel(PackageListActivity.this.getPackageManager())+
//                " count="+count);
                
            	if (bAll){
            		if(isPackageInRestrictions(packageInfo.packageName)){
            			if(true){//isPackageInYulong(packageInfo.packageName)
            				String name = null;
            				name = YLUtils.getActivities(getApplicationContext(), packageName);
                			if(true){
                				packageInfosAll.add(inc_1,packageInfo);
                				inc_1++;
                			}
                            if(name!=null){
                            	//packageInfosHideSystem.add(inc,packageInfo);
            				inc++;
                			}
                		}else{
                		}
                	}          		
            	}
            }
        } 
        
    }
    public void mapAllSysPackageExist(Context context){
		if (mShowSystemApps) {
			packageInfos.clear();
			for(PackageInfo packageInfo:packageInfosAll){
				packageInfos.add(packageInfo);
	        }
		} else {
			packageInfos.clear();
			for(PackageInfo packageInfo:packageInfosHideSystem){
				packageInfos.add(packageInfo);
	        }
		}
    }
    
    public class ViewHolder {    
    	public ImageView app_icon;
        public TextView app_package;    
        public TextView app_name;
        public TextView app_status;
        public CheckBox sub_switch_widget1;    
    }
    
    private class MyListAdapter extends BaseAdapter {
        public MyListAdapter(Context context) {
            mContext = context;
        }

        public int getCount() {
            return packageInfos == null ? 0: packageInfos.size();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	 ViewHolder holder = null;   
			if (convertView == null) {
				holder = new ViewHolder();  
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.preference_package_list_item, parent, false);

				if(convertView != null){
				convertView.setTag(holder);
				holder.app_icon = (ImageView)convertView.findViewById(R.id.app_icon);
				holder.app_package = (TextView)convertView.findViewById(R.id.app_package);
				holder.app_name = (TextView)convertView.findViewById(R.id.app_name);
				holder.app_status = (TextView)convertView.findViewById(R.id.app_status);
				holder.sub_switch_widget1 = (CheckBox)convertView.findViewById(R.id.sub_switch_widget1);
				
				CheckBox checkbox0 = (CheckBox)convertView.findViewById(R.id.sub_switch_widget0);
				if(checkbox0 != null){
					checkbox0.setVisibility(View.GONE);
				}
				
				ImageView imageView = (ImageView)convertView.findViewById(R.id.preference_detail_next_black);
				if(imageView != null){
					imageView.setVisibility(View.VISIBLE);
				}
				View view = (View)convertView.findViewById(R.id.preference_detail_next_black__divider);
				if(view != null){
					view.setVisibility(View.VISIBLE);
				}
				if (position == 0 && convertView != null) {
					View vL = (View) convertView
							.findViewById(R.id.package_list_divider);
					vL.setVisibility(View.INVISIBLE);
					}
				}
			} else {
				holder = (ViewHolder) convertView.getTag();
				View vL = (View)convertView.findViewById(R.id.package_list_divider);
				 if(position==0)
                 {
            		vL.setVisibility(View.INVISIBLE);
             	}else{
             		vL.setVisibility(View.VISIBLE);
            	}
			}
			
            if (packageInfos != null && packageInfos.size() > position){
            	holder.sub_switch_widget1.setId(position);
            	PackageInfo packageInfo = packageInfos.get(position);
            	if(packageInfo != null){
            	int userId = 0;
				if(packageInfo!= null && packageInfo.applicationInfo != null){
					userId = UserHandle.getUserId(packageInfo.applicationInfo.uid);
				}
//				Bitmap bp= StatusBarIconView.getPackageThemeBitmap(mContext, packageInfo.packageName, userId);
//            	if (bp != null){
//					Bitmap temp = Utilities.formatIconBitmap(bp,
//							getApplicationContext(), 0);
//					if (temp != null && !temp.isRecycled()) {
//						holder.app_icon.setImageBitmap(temp);

				Bitmap bp= initAppIconByAsyncTask(holder.app_icon, packageInfo.packageName, userId);
				holder.app_icon.setBackground(null);
            	if (bp != null){
            		holder.app_icon.setImageBitmap(bp);
            	} else {
            		holder.app_icon.setBackground(mContext.getDrawable(android.R.drawable.sym_def_app_icon));
            	}
            	
            	holder.app_package.setText(packageInfo.packageName);
					holder.app_name.setText(packageInfo.applicationInfo != null ? packageInfo.applicationInfo
							.loadLabel(mPm) : "");					
            	
            	final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PackageListActivity.this);
                String notificationKey = NotificationCollapseManage.getNotificationKey(packageInfo.packageName);
                String statubarKey = NotificationCollapseManage.getStatusBarKey(packageInfo.packageName);
                String otherNotificationKey = NotificationCollapseManage.getOtherNotificationKey(packageInfo.packageName);
                boolean notificationKeyValue = NotificationCollapseManage.getDefault(mContext).getNotificationKeyValue(packageInfo.packageName);
                boolean statubarKeyValue = NotificationCollapseManage.getDefault(mContext).getStatusBarKeyValue(packageInfo.packageName);
                boolean otherNotificationKeyValue = NotificationCollapseManage.getDefault(mContext).getOtherNotificationKeyValue(packageInfo.packageName);
                boolean locksreenNotificationKeyValue = NotificationCollapseManage.getDefault(mContext).getLocksreenNotificationKeyValue(packageInfo.packageName);
                boolean floatNotificationKeyValue = NotificationCollapseManage.getDefault(mContext).getFloatingNotificationKeyValue(packageInfo.packageName);
                boolean detailNotificationKeyValue = NotificationCollapseManage.getDefault(mContext).getDetailNotificationKeyValue(packageInfo.packageName);
                String statusString = statusString = PackageListActivity.this.getResources().getString(R.string.item_show_none);
                if(notificationKeyValue){
            		statusString = PackageListActivity.this.getResources().getString(R.string.item_show_notifcaiton);
            		if (statubarKeyValue){
                    	//statusString += PackageListActivity.this.getResources().getString(R.string.item_show_dot) + PackageListActivity.this.getResources().getString(R.string.item_show_statusbar);                  	             		
                	} 
            		if(otherNotificationKeyValue){
            			//statusString += PackageListActivity.this.getResources().getString(R.string.item_show_dot) + PackageListActivity.this.getResources().getString(R.string.item_show_other);
            		}
            		if(floatNotificationKeyValue){
            			statusString += PackageListActivity.this.getResources().getString(R.string.item_show_dot) + PackageListActivity.this.getResources().getString(R.string.item_show_floating);
            		}
            		if(locksreenNotificationKeyValue){
            			statusString += PackageListActivity.this.getResources().getString(R.string.item_show_dot) + PackageListActivity.this.getResources().getString(R.string.item_show_lockscreen);
            		}
            		if(detailNotificationKeyValue){
            			//statusString += PackageListActivity.this.getResources().getString(R.string.item_show_dot) + PackageListActivity.this.getResources().getString(R.string.item_show_detail);
            		}
                }
//                    LogHelper.sd("","packageInfos234="+packageInfo.packageName+
//                    		" name-"+packageInfo.applicationInfo.loadLabel(PackageListActivity.this.getPackageManager())+
//                    		" str="+statusString+
//                    		" pos="+position);
                holder.app_status.setText(statusString);
            	}//packageInfo
            }
            return convertView;
        }

        private Context mContext;
    }
	private final static boolean DEBUG = false;
	public static Bitmap initAppIconByAsyncTask(ImageView imageView,
			String packageName, int appUid) {
		if (packageName == null) {
			return null;
		}
		String key = packageName + appUid;
		imageView.setTag(key);
		Bitmap bitmap = getBitmapFromMem(key);
		if (bitmap != null) {
			if (DEBUG)
				Log.d(TAG, "getBitmapFromMem : " + key);
			return bitmap;
		}
		String[] para = { packageName, String.valueOf(appUid) };
		new AsyncTaskImageLoad(imageView, key).execute(para);
		return null;
	}
	static final int MEM_CACHE_DEFAULT_SIZE = 5 * 1024 * 1024;
	private static LruCache<String, Bitmap> memCache = new LruCache<String, Bitmap>(
			MEM_CACHE_DEFAULT_SIZE) {
		@Override
		protected int sizeOf(String key, Bitmap bitmap) {
			return bitmap.getByteCount();
		}
	};
	public static Bitmap getBitmapFromMem(String url) {
		return memCache.get(url);
	}
	public static void putBitmapToMem(String url, Bitmap bitmap) {
		memCache.put(url, bitmap);
	}
	public static class AsyncTaskImageLoad extends
			AsyncTask<String, Integer, Bitmap> {
		private ImageView Image = null;
		private Object Tag;
		public AsyncTaskImageLoad(ImageView img, Object tag) {
			Image = img;
			Tag = tag;
		}
		protected Bitmap doInBackground(String... params) {
			if (Image == null || Image.getTag() == null
					|| !Image.getTag().equals(Tag)) {
				if (DEBUG)
					Log.d(TAG, "doInBackground:" + Image.getTag() + " Tag:"
							+ Tag);
				cancel(true);
				return null;
			}
			try {
				Bitmap bp = StatusBarIconView.getPackageThemeBitmap(
						Image.getContext(), params[0],
						Integer.parseInt(params[1]));
				Bitmap temp = Utilities.formatIconBitmap(bp,
						Image.getContext(), 0);
				if (temp == null) {
					return bp;
				} else {
					return temp;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(Bitmap result) {
			if (Image != null && result != null) {
				if (DEBUG)
					Log.d(TAG, "onPostExecute Image:" + Image.getTag()
							+ " Tag:" + Tag);
				if (Image.getTag() != null && Image.getTag().equals(Tag)) {
					Image.setBackground(null);
					Image.setImageBitmap(result);
					putBitmapToMem(Tag.toString(), result);
				}
			}
			super.onPostExecute(result);
		}
	}
    
    public static void setStatusBarOverlayingActivity(Activity p_Activity){
    	//p_Activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }
    
    public static int getStatusBarHeight(Activity activity){
        int statusHeight = 0;
        Rect localRect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);
        statusHeight = localRect.top;
        if (0 == statusHeight){
            Class<?> localClass;
            try {
                localClass = Class.forName("com.android.internal.R$dimen");
                Object localObject = localClass.newInstance();
                int nHeightValue = Integer.parseInt(localClass.getField("status_bar_height").get(localObject).toString());
                statusHeight = activity.getResources().getDimensionPixelSize(nHeightValue);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return statusHeight;
    }
    
    private void initActionBar() {
        ActionBar objActionBar = getActionBar();
        if(objActionBar!=null){
            objActionBar.hide();
        }

        FrameLayout layoutContent = (FrameLayout)findViewById(com.android.internal.R.id.content);
        if(layoutContent!=null){
        	View viewActionBar = null;
            if(1 == mStyle){
            	viewActionBar = getLayoutInflater().inflate(R.layout.yl_actionbar, null);
            }else{
            	viewActionBar = getLayoutInflater().inflate(R.layout.yl_actionbar_young, null);
            }
            if (viewActionBar != null) {
                LinearLayout layoutStatusBar = (LinearLayout)viewActionBar.findViewById(R.id.status_bar_layout);
                layoutStatusBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getStatusBarHeight(PackageListActivity.this)));
                mTitle = (TextView)viewActionBar.findViewById(R.id.topbar_title);
                mBack = (ImageView)viewActionBar.findViewById(R.id.topbar_back);
                
//                LinearLayout mLinear = (LinearLayout)viewActionBar.findViewById(R.id.topbar_rightbuttonlayout);
//                if(mLinear != null){
//                	mLinear.setVisibility(View.VISIBLE);
//                }
//                
//                final View mMore = (View)viewActionBar.findViewById(R.id.topbar_right_more);
//                mMore.setOnClickListener(new View.OnClickListener() {
//        			@Override
//        			public void onClick(View v) {
//        				try {
//        					Intent intent = new Intent (PackageListActivity.this,MainTopRightDialog.class);			
//            				startActivity(intent);	
//						} catch (Exception e) {
//							// TODO: handle exception
//							e.printStackTrace();
//						}
//        				
//        			}
//        		});
//                mMore.setVisibility(View.GONE);
                
                
                mTitle.setText(getTitle());
                ImageView upView = (ImageView)viewActionBar.findViewById(R.id.topbar_back);
                upView.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        // TODO Auto-generated method stub
                        finish();
                    }
                });
                int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
                int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
                viewActionBar.measure(w, h); 
                int height =viewActionBar.getMeasuredHeight();
                viewActionBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
                View viewParent = getWindow().getDecorView();
                if(viewParent!=null){
                    ((ViewGroup)viewParent).addView(viewActionBar,0);
                }
                layoutContent.setPadding(0, height, 0, 0);
                setStatusBarGradientColor();
        		}
            }
        }


    public void setActionBarTitle(String p_strTitle){
        mTitle.setText(p_strTitle);
    }
    
    public void setActionBarBackButtonVisibility(boolean p_bVisible){
        if(p_bVisible){
            mBack.setVisibility(View.VISIBLE);
            //mDivider.setVisibility(View.VISIBLE);
        }else{
            mBack.setVisibility(View.GONE);
            //mDivider.setVisibility(View.GONE);
        }
    }
    public void setStatusBarGradientColor(){
    	if (1 == mStyle) {//black gold
    		return;
        }
    	 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
 			setTranslucentStatus(true);//
 		}
//         SystemBarTintManager tintManager = new SystemBarTintManager(this);
//         tintManager.setStatusBarTintEnabled(true);
//         tintManager.setStatusBarTintResource(R.drawable.gradient_bg);
    }
    @TargetApi(19) 
	private void setTranslucentStatus(boolean on) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}
}
