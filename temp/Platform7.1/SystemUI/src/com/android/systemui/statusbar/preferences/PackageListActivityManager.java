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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.SystemBarTintManager;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.misc.YLUtils;
import com.android.systemui.recents.misc.YLUtils.SettingsDB;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.preferences.PackageListActivity.AsyncTaskImageLoad;
import com.yulong.android.common.app.CommonActivity;
import com.yulong.android.common.view.TopBar;
import com.yulong.android.common.view.TopBar.TopBarStyle;
import com.yulong.android.view.tab.TabBar;

public class PackageListActivityManager extends CommonActivity {
	private MyListAdapter adapter;
	private View curView;
	private TextView mTitle;
	private ImageView mBack;
	
	private ListView listView;
	private int mStyle = 0;
	private TopBar mTopbar;
	private TabBar mTabbar;
	private int mTopBarHeight;
	private final static String TAG = "PackageListActivity";
	
	private List<PackageInfo> packageInfos;
	public class BatchItem {
		private CheckBox mAllCheckBox;
		private Boolean mAllChecked;
		private Boolean mCheckedAllIsSetting;
		private Set<Integer> mPositionSet;;
	}
	
    private BatchItem mBatchItem[];
    private int BATCH_0 = 0;
    private int BATCH_1 = 1;
    private boolean mShowSystemApps = false;
    private static List<PackageInfo> packageInfosAll;
	private static List<PackageInfo> packageInfosHideSystem;
	static final private int GET_CODE = 0;
	private PackageManager mPm;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.preference_package_listview);
        View body = setBodyLayout(R.layout.preference_package_listview);
        body.setPadding(0, mTopBarHeight, 0, 0);
        mBatchItem = new BatchItem[2];
        for(int i=0; i<mBatchItem.length; i++){
        	mBatchItem[i] = new BatchItem();
        	mBatchItem[i].mAllChecked = false;
        	mBatchItem[i].mCheckedAllIsSetting = false;
        	mBatchItem[i].mPositionSet = new HashSet<Integer>();
        }
        mPm = getPackageManager();
        
        mStyle = Utilities.getCoolpadThemeStyle();
        if (1 == mStyle) {//black gold
        } else {
        	this.getWindow().setBackgroundDrawableResource(R.drawable.background_holo_light_young);
        }
        
        //setStatusBarGradientColor();
        mShowSystemApps = getIntent().getBooleanExtra("ShowSystemApps",false);
        //LogHelper.sd(TAG, "onCreate()_1000");
        setStatusBarOverlayingActivity(PackageListActivityManager.this);
        //initActionBar();
        mapAllSysPackage(this);
        //LogHelper.sd(TAG, "onCreate()_1001");
        
        
        
        listView = (ListView) findViewById(R.id.listview);
		
        adapter = new MyListAdapter(this);
        try{
        	View convertView = LayoutInflater.from(this).inflate(
					R.layout.preference_package_list_item_head, null);
			
        	convertView.setClickable(false);
        	mBatchItem[ BATCH_0 ].mAllCheckBox = (CheckBox)convertView.findViewById(R.id.sub_switch_widget0);
        	mBatchItem[ BATCH_1 ].mAllCheckBox = (CheckBox)convertView.findViewById(R.id.sub_switch_widget1);
        	
        	//mBatchItem[ BATCH_0 ].mAllCheckBox.setChecked(mBatchItem[ BATCH_0 ].mAllChecked);
    		mBatchItem[ BATCH_0 ].mAllCheckBox.setOnCheckedChangeListener(new AllListener(this, BATCH_0));
    		
    		//mBatchItem[ BATCH_1 ].mAllCheckBox.setChecked(mBatchItem[ BATCH_1 ].mAllChecked);
    		mBatchItem[ BATCH_1 ].mAllCheckBox.setOnCheckedChangeListener(new AllListener(this, BATCH_1));
//        	for(int i=0; i<mBatchItem.length; i++){
//        		if(mBatchItem[ i ].mAllCheckBox != null){
//            		mBatchItem[ i ].mAllCheckBox.setChecked(mBatchItem[ i ].mAllChecked);
//            		mBatchItem[ i ].mAllCheckBox.setOnCheckedChangeListener(new AllListener(this, i));
//            	}
//        	}
        	
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
				if(true){
					return;
				}
				String packageName = "default.package.name";
	        	String appName = "default.app.name";
	        	TextView text = (TextView)v.findViewById(R.id.app_package);
	        	if (text != null){
	        		packageName = text.getText().toString();
	        	}else{
	        		return;
	        	}
	        	Intent intent = new Intent();
	        	intent.setClass(PackageListActivityManager.this, PackageSettingsActivity.class);
	        	
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
//		setActionBarTitle(getResources().getString(R.string.title_activity_package_list_batch));
//		setActionBarBackButtonVisibility(true);
	}
    @Override
    protected void onCreateTopBar(TopBar topBar) {
        super.onCreateTopBar(topBar);
        topBar.setTopBarStyle(TopBarStyle.TOP_BAR_NOTMAL_STYLE);
        topBar.setTopBarTitle(getResources().getString(R.string.title_activity_package_list_batch));
        topBar.setTopBarTitleSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
        mTabbar = topBar.getTabBar();
        mTopBarHeight = topBar.getHeight();
	}
    
    public class AllListener implements CheckBox.OnCheckedChangeListener{

    	private Context mContext;
    	private int mIndex = 0;
    	public AllListener(Context context, final int index){
    		mContext = context;
    		mIndex = index;
    	}
		@Override
		public void onCheckedChanged(CompoundButton toggle, boolean checked) {
			// TODO Auto-generated method stub
			final Boolean b = (Boolean)checked;
			LogHelper.sd(TAG,"mIndex="+mIndex+" b="+b+
					" ob="+mBatchItem[ mIndex ].mAllChecked);
        	if(mBatchItem[ mIndex ].mAllChecked != b){
        		mBatchItem[ mIndex ].mAllChecked = b;
        		mBatchItem[ mIndex ].mCheckedAllIsSetting = true;
        		//LogHelper.sd(TAG,"1400080 coun="+listView.getChildCount()+" c="+listView.getCount());
        		mBatchItem[ mIndex ].mPositionSet.clear();
        		for(int i=0; i<listView.getChildCount();i++){
        			if(listView.getChildAt(i) != null){
        				ViewHolder v = (ViewHolder)listView.getChildAt(i).getTag();
            			try {
            				if(v != null){
            					CheckBox chk = null;
            					if(BATCH_0 == mIndex){
            						chk = (CheckBox)v.sub_switch_widget0;
            					}else if(BATCH_1 == mIndex){
            						chk = (CheckBox)v.sub_switch_widget1;
            					}
            					
                    			if(chk != null){
                    				chk.setChecked(mBatchItem[ mIndex ].mAllChecked);
                    				mBatchItem[ mIndex ].mPositionSet.add(chk.getId());
                    				//save old position
                    				//Log.d("","1400082 pos="+chk.getId());
                    			}
            				}
    					} catch (Exception e) {
    						// TODO: handle exception
    					}
        			}
        		}
        		adapter.notifyDataSetChanged();
        		mBatchItem[ mIndex ].mCheckedAllIsSetting = false;
        		String str = "SystemUI PackageListViewManager:"+mIndex;
        		HandlerThread thr = new HandlerThread(str);
                thr.start();
                Handler mHandler = new Handler(thr.getLooper());
        	    mHandler.post(new Runnable(){
        	        @Override
        	        public void run(){
        	                // TODO Auto-generated method stub
        	        	final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PackageListActivityManager.this);
        				for (int pos = 0; pos < packageInfos.size(); pos++) {
        					if(mBatchItem[ mIndex ].mPositionSet.contains(pos)){
        						//LogHelper.sd(TAG,"1400082 pos1="+pos);
        						continue;
        					}
        					PackageInfo packageInfo = packageInfos.get(pos);
        					final String pkgName = packageInfo.packageName;
        					boolean notificationKeyValue = false;
        					String notificationKey = "";
        					if(BATCH_0 == mIndex){
        						notificationKeyValue = NotificationCollapseManage
            							.getDefault(mContext).getNotificationKeyValue(
            									packageInfo.packageName);
            					notificationKey = NotificationCollapseManage.getDefault(mContext).getNotificationKey(pkgName);
        					}else if(BATCH_1 == mIndex){
        						notificationKeyValue = NotificationCollapseManage
            							.getDefault(mContext).getLocksreenNotificationKeyValue(
            									packageInfo.packageName);
            					notificationKey = NotificationCollapseManage
            							.getDefault(mContext).getLocksreenNotificationKey(pkgName);
        					}
        					
        					
        					Boolean n = (Boolean) b;

        					if (sp != null && notificationKeyValue != n) {

        						final SharedPreferences.Editor se = sp.edit();
        						se.putBoolean(notificationKey, n);
        						se.commit();
        					}
        				}
        				Intent intent = new Intent(
        						"com.yulong.android.ntfmanager.RefreshStatusBar");
        				//sendBroadcast(intent);
        				sendBroadcastAsUser(intent, UserHandle.ALL);
        	         }
        	     });
        		
        	    Intent intent = new Intent(
						"com.yulong.android.ntfmanager.RefreshStatusBar");
				//sendBroadcast(intent);
				sendBroadcastAsUser(intent, UserHandle.ALL);
        	}
        }
    	
    }
   
    public class ItemListener implements OnCheckedChangeListener{

    	private  int mIndex = 0;
    	private Context mContext;
    	public ItemListener(Context context, int index){
    		mContext = context;
    		mIndex = index;
    	}
    	@Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    		//LogHelper.sd(TAG,"ItemListener mIndex="+mIndex);
    		final int pos = buttonView.getId();
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PackageListActivityManager.this);
			PackageInfo packageInfo = packageInfos.get(pos);
			final String pkgName = packageInfo.packageName;
			boolean notificationKeyValue = false;
			String notificationKey = "";
			if(BATCH_0 == mIndex){
				notificationKeyValue = NotificationCollapseManage.getDefault(mContext).getNotificationKeyValue(packageInfo.packageName);
	    		notificationKey = NotificationCollapseManage.getNotificationKey(pkgName);
			}else if(BATCH_1 == mIndex){
				notificationKeyValue = NotificationCollapseManage.getDefault(mContext).getLocksreenNotificationKeyValue(packageInfo.packageName);
	    		notificationKey = NotificationCollapseManage.getLocksreenNotificationKey(pkgName);
			}
			
    		Boolean n = (Boolean) isChecked;
    		
    		if(sp != null && notificationKeyValue != n){
//    			Log.d("","140005 onCheckedChanged notificationKey="+notificationKey+
//        				" Tobe isChecked="+isChecked+" pos="+pos);
    			
    			final SharedPreferences.Editor se = sp.edit();
        		se.putBoolean(notificationKey, n);
				se.commit();
				
				if(BATCH_0 == mIndex){
					YLUtils.putIntForAllUser(mContext, SettingsDB.SECURE, notificationKey, n?1:0);
				}
				//if now is setting then here don't need to broadcast
				if(!mBatchItem[ mIndex ].mCheckedAllIsSetting){
					Intent intent = new Intent(
							"com.yulong.android.ntfmanager.RefreshStatusBar");
					//sendBroadcast(intent);
					sendBroadcastAsUser(intent, UserHandle.ALL);
        		}
				
				
//				boolean notificationKeyValue1 = NotificationCollapseManage.getDefault(
//						mContext).getNotificationKeyValue(pkgName);
//				final String notificationKey1 = NotificationCollapseManage
//						.getNotificationKey(pkgName);
//				Log.d("","140007 onCheckedChanged notificationKey="+notificationKey1+
//						" notificationKeyValue="+notificationKeyValue1);
    		}else{
    			Log.d("","onCheckedChanged sp==null");
    		}
    		
    		//if now is setting then mBatchItem[ mIndex ].mAllCheckBox don't need to be setted
    		if(!mBatchItem[ mIndex ].mCheckedAllIsSetting){
    			//need after the upper code
        		if (isChecked) {
//					Log.d("","140007 onCheckedChanged isChecked="+getAllNotificationChecked()+
//							" notificationKeyValue="+mBatchItem[ mIndex ].mAllCheckBox.isChecked());
					if(mBatchItem[ mIndex ].mAllCheckBox.isChecked() != getAllNotificationChecked(mIndex)){
						mBatchItem[ mIndex ].mAllChecked = true;
						mBatchItem[ mIndex ].mAllCheckBox.setChecked(isChecked);
					}
				} else {
					if(mBatchItem[ mIndex ].mAllCheckBox.isChecked() != isChecked){
						mBatchItem[ mIndex ].mAllChecked = false;
						mBatchItem[ mIndex ].mAllCheckBox.setChecked(isChecked);
					}
				}
    		}
        }
    }
    public void onClick(View v){
    	//LogHelper.sd(TAG,"140001");
    	if(true){
    		return;
    	}
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
//	   if (curView != null && adapter!=null){
//			try {
//				ViewHolder vh = (ViewHolder)curView.getTag();
//				if(vh != null && vh.sub_switch_widget1 != null){
//					View child = adapter.getView((Integer) vh.sub_switch_widget1.getId(),
//							curView, listView);
//				}
//			} catch (Exception e) {
//				// TODO: handle exception
//			}
//		   //listView.addView(child, (Integer)curView.getTag());
//	   }
//        if (requestCode == GET_CODE) {
//            if (resultCode == RESULT_CANCELED) {
//            } else {
//            	data.getAction();       
//            }
//        }
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
   
	
    public void mapAllSysPackage(Context context){
    	if(packageInfos != null){
    		packageInfos.clear();
    	}else{
    	packageInfos = new ArrayList<PackageInfo>();
    	}
    	if(mShowSystemApps){
    		packageInfosAll = PackageListActivity.getPackAll();
    		if(packageInfosAll != null){
    			for(PackageInfo packageInfo:packageInfosAll){
    				packageInfos.add(packageInfo);
    	        }
    		}
    	}else{
    		packageInfosHideSystem = PackageListActivity.getPackHideSystem();
    		if(packageInfosHideSystem != null){
    			for(PackageInfo packageInfo:packageInfosHideSystem){
    				packageInfos.add(packageInfo);
    	        }
    		}
    	}
    	if(packageInfos == null){
    		List<PackageInfo> packageInfosTemp = mPm.getInstalledPackages(0);
		boolean bAll = true;//getIntent().getBooleanExtra("all_package",true);

		int count = 0;
        for(PackageInfo packageInfo:packageInfosTemp){
            String packageName = packageInfo.packageName;
           
            if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            		|| (packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0){
            	
            }else{
            	 count++;
//                 LogHelper.sd("","11packageInfo.packageName="+packageInfo.packageName+" title="+
//                         packageInfo.applicationInfo.loadLabel(PackageListActivity.this.getPackageManager())+
//                         " count="+count);
            	if(isPackageInRestrictions(packageInfo.packageName)){
            		
            		packageInfos.add(packageInfo);
            	}
            }
        }
        
        int size=packageInfos.size();
        int inc=size;
        for(PackageInfo packageInfo:packageInfosTemp){
        	
            String packageName = packageInfo.packageName;
            
            if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            		|| (packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0){
            	count++;
//                LogHelper.sd("","22packageInfo.packageName="+packageInfo.packageName+" title="+
//                packageInfo.applicationInfo.loadLabel(PackageListActivity.this.getPackageManager())+
//                " count="+count);
                
            	if (bAll){
            		if(isPackageInRestrictions(packageInfo.packageName)){
            			if(isPackageInYulong(packageInfo.packageName)){
            				packageInfos.add(inc,packageInfo);
            				inc++;
                		}else{
                    		}
                		}
                	}          		
            	}
            }
        } 
        
        //check all the notifications enabled or not
        for(int i=0; i<mBatchItem.length; i++){
        	if(getAllNotificationChecked(i)){
            	mBatchItem[ i ].mAllChecked = true;
            }else{
            	mBatchItem[ i ].mAllChecked = false;
            }
        }
        
    }
    
    public Boolean getAllNotificationChecked(int i){
    	if(i > BATCH_1){
    		LogHelper.sd(TAG, "error i > BATCH_1");
    		return false;
    	}
    	Boolean bChecked = false;
    	final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PackageListActivityManager.this);
        int pos = 0;
        for (pos = 0; pos < packageInfos.size(); pos++) {
			PackageInfo packageInfo = packageInfos.get(pos);
			final String pkgName = packageInfo.packageName;
			boolean notificationKeyValue =false;
			if(BATCH_0 == i){
				notificationKeyValue = NotificationCollapseManage
						.getDefault(getApplicationContext()).getNotificationKeyValue(
								packageInfo.packageName);
			}else if(BATCH_1 == i){
				notificationKeyValue = NotificationCollapseManage
						.getDefault(getApplicationContext()).getLocksreenNotificationKeyValue(
								packageInfo.packageName);
			}
			
			if(notificationKeyValue == false){
				break;
			}
		}
        //LogHelper.sd(TAG,"140002 pos="+pos+" size="+packageInfos.size());
        if(pos == packageInfos.size()){
        	bChecked = true;
        }
        return bChecked;
    }
    
    public class ViewHolder {    
    	public ImageView app_icon;
        public TextView app_package;    
        public TextView app_name;
        //public TextView app_status;
        public CheckBox sub_switch_widget0; 
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
						R.layout.preference_package_list_item_manager, parent, false);

				convertView.setTag(holder);
				holder.app_icon = (ImageView)convertView.findViewById(R.id.app_icon);
				holder.app_package = (TextView)convertView.findViewById(R.id.app_package);
				holder.app_name = (TextView)convertView.findViewById(R.id.app_name);
				//holder.app_status = (TextView)convertView.findViewById(R.id.app_status);
				holder.sub_switch_widget0 = (CheckBox)convertView.findViewById(R.id.sub_switch_widget0);
				holder.sub_switch_widget0.setVisibility(View.VISIBLE);
				
				holder.sub_switch_widget1 = (CheckBox)convertView.findViewById(R.id.sub_switch_widget1);
				holder.sub_switch_widget1.setVisibility(View.VISIBLE);
				
                holder.sub_switch_widget1.setOnCheckedChangeListener(new ItemListener(mContext, BATCH_1));
                holder.sub_switch_widget0.setOnCheckedChangeListener(new ItemListener(mContext, BATCH_0));
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
            if (packageInfos != null && packageInfos.size() > position){
            	holder.sub_switch_widget0.setId(position);
            	holder.sub_switch_widget1.setId(position);
            	PackageInfo packageInfo = packageInfos.get(position);
            	if(packageInfo != null){
            	int userId = 0;
				if(packageInfo!= null && packageInfo.applicationInfo != null){
					userId = UserHandle.getUserId(packageInfo.applicationInfo.uid);
				}
//            	Bitmap bp= StatusBarIconView.getPackageThemeBitmap(mContext, packageInfo.packageName, userId);
//            	if (bp != null){
//					Bitmap temp = Utilities.formatIconBitmap(bp,
//							getApplicationContext(), 0);
//					if (temp != null && !temp.isRecycled()) {
//						holder.app_icon.setImageBitmap(temp);
//					} else {
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
            	
            	final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PackageListActivityManager.this);
                String notificationKey = NotificationCollapseManage.getNotificationKey(packageInfo.packageName);
                String statubarKey = NotificationCollapseManage.getStatusBarKey(packageInfo.packageName);
                String otherNotificationKey = NotificationCollapseManage.getOtherNotificationKey(packageInfo.packageName);
                boolean notificationKeyValue = NotificationCollapseManage.getDefault(mContext).getNotificationKeyValue(packageInfo.packageName);
                boolean statubarKeyValue = NotificationCollapseManage.getDefault(mContext).getStatusBarKeyValue(packageInfo.packageName);
                boolean otherNotificationKeyValue = NotificationCollapseManage.getDefault(mContext).getOtherNotificationKeyValue(packageInfo.packageName);
                boolean locksreenNotificationKeyValue = NotificationCollapseManage.getDefault(mContext).getLocksreenNotificationKeyValue(packageInfo.packageName);
                String statusString = "";
                if(notificationKeyValue){
            		//statusString = PackageListActivityManager.this.getResources().getString(R.string.item_show_notifcaiton);
            		if (statubarKeyValue){
                    	//statusString += PackageListActivityManager.this.getResources().getString(R.string.item_show_dot) + PackageListActivityManager.this.getResources().getString(R.string.item_show_statusbar);                  	             		
                	} 
            		if(otherNotificationKeyValue){
            			//statusString += PackageListActivityManager.this.getResources().getString(R.string.item_show_dot) + PackageListActivityManager.this.getResources().getString(R.string.item_show_other);
            		}
            		if(locksreenNotificationKeyValue){
            			//statusString += PackageListActivityManager.this.getResources().getString(R.string.item_show_dot) + PackageListActivityManager.this.getResources().getString(R.string.item_show_lockscreen);
            		}
                }
//                LogHelper.sd("","140009 packageInfos234="+packageInfo.packageName+
//                		" name-"+packageInfo.applicationInfo.loadLabel(PackageListActivity.this.getPackageManager())+
//                		" str="+statusString+
//                		" pos="+position);
                //holder.app_status.setText(statusString);
                
                holder.sub_switch_widget0.setChecked(notificationKeyValue);
                holder.sub_switch_widget1.setChecked(locksreenNotificationKeyValue);
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
                layoutStatusBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getStatusBarHeight(PackageListActivityManager.this)));
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
         SystemBarTintManager tintManager = new SystemBarTintManager(this);
         tintManager.setStatusBarTintEnabled(true);
         tintManager.setStatusBarTintResource(R.drawable.gradient_bg);
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
