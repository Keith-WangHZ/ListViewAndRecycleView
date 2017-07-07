package com.android.systemui.quicksettings.bottom;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.systemui.R;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.StatusBarIconView;
import com.yulong.android.common.app.CommonActivity;
import com.yulong.android.common.view.TopBar;
import com.yulong.android.common.view.TopBar.TopBarStyle;

public class BottomIconEditActivity extends CommonActivity {

	private static final String TAG = "BottomIconEditActivity";
	private ListView mAppListView;
	private List<CheckablePackageInfo> mPackageInfos;
	private PackageManager mPackageManager;
	private GridLayout mPreviewContainer;
	private Toast mToast;
	private int mOrder = 0;
	private ProgressDialog mLoadingProgressDialog;
	private String hyFlagfinal = "hy";
	private Boolean mShowWeiXinAliPaySaoYiSao;
	private Boolean mShowWeiXinAliPayFuKuan;

	private final static String START_TYPE = "QuickScan_StartType";
	private final static int weixin_saoyisao   = 1;
	private final static int zhifubao_saoyisao = 2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setBodyLayout(R.layout.qs_bottom_icon_edit_layout);
		mPreviewContainer = (GridLayout) findViewById(R.id.icon_preview_content);
		mAppListView = (ListView) findViewById(R.id.choose_app_listview);
		mPackageManager = getPackageManager();
		if(SystemProperties.get("ro.yulong.project").equals(hyFlagfinal)){
			mShowWeiXinAliPaySaoYiSao = false;
			mShowWeiXinAliPayFuKuan = false;
		}else{
			if(isWeiXinAliPayEnable("IS_SUPPORT_WECHAT_SCAN")){
				mShowWeiXinAliPaySaoYiSao = true;
			}else{
				mShowWeiXinAliPaySaoYiSao = false;
			}
			if(isWeiXinAliPayEnable("IS_SUPPORT_WECHAT_RECEIVABLES")){
				mShowWeiXinAliPayFuKuan = true;
			}else{
				mShowWeiXinAliPayFuKuan = false;
			}
		}
		initFixedLocation();
		getLoadingProgressDialog().show();
		new LoadPackageAsyncTask().execute();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mLoadingProgressDialog != null) {
			mLoadingProgressDialog.dismiss();
		}
	}

	@Override
	protected void onCreateTopBar(TopBar topBar) {
		super.onCreateTopBar(topBar);
		topBar.setTopBarStyle(TopBarStyle.TOP_BAR_NOTMAL_STYLE);
		topBar.setTopBarTitle(getResources().getString(R.string.quicksettings_edit_disable));
		topBar.setTopBarTitleSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
		topBar.setDisplayUpView(true);
	}

	private void initFixedLocation() {
		if(mShowWeiXinAliPayFuKuan || mShowWeiXinAliPaySaoYiSao){
			int type = Settings.System.getInt(getContentResolver(), START_TYPE, weixin_saoyisao);
			String str = getResources().getString(R.string.qs_label_weixin_pay);
			if(type != weixin_saoyisao){
				str = getResources().getString(R.string.qs_label_ali_pay);
			}
			if(mShowWeiXinAliPayFuKuan && mShowWeiXinAliPaySaoYiSao){
				View containerSlot2 = mPreviewContainer.getChildAt(2);
				ImageView iconSlot2 = (ImageView) containerSlot2.findViewById(R.id.icon);
				TextView textSlot2 = (TextView) containerSlot2.findViewById(R.id.text);
				iconSlot2.setImageResource(R.drawable.ic_qs_alipay);
				textSlot2.setText(str);
				
				View containersSlot4 = mPreviewContainer.getChildAt(4);
				ImageView iconSlot4 = (ImageView) containersSlot4.findViewById(R.id.icon);
				TextView textSlot4 = (TextView) containersSlot4.findViewById(R.id.text);
				iconSlot4.setImageResource(R.drawable.ic_qs_camera_off);
				String strSlot4 = getResources().getString(R.string.qs_label_camera);
				textSlot4.setText(strSlot4);
				
				View containersSlot5 = mPreviewContainer.getChildAt(5);
				ImageView iconSlot5 = (ImageView) containersSlot5.findViewById(R.id.icon);
				TextView textSlot5 = (TextView) containersSlot5.findViewById(R.id.text);
				iconSlot5.setImageResource(R.drawable.ic_qs_blaster_off);
				String strSlot5 = getResources().getString(R.string.qs_label_blaster);
				textSlot5.setText(strSlot5);
			}else if(!mShowWeiXinAliPayFuKuan && mShowWeiXinAliPaySaoYiSao){
				View containerSlot2 = mPreviewContainer.getChildAt(2);
				ImageView iconSlot2 = (ImageView) containerSlot2.findViewById(R.id.icon);
				TextView textSlot2 = (TextView) containerSlot2.findViewById(R.id.text);
				iconSlot2.setImageResource(R.drawable.ic_qs_alipay);
				textSlot2.setText(str);
				
				View containersSlot3 = mPreviewContainer.getChildAt(3);
				ImageView iconSlot3 = (ImageView) containersSlot3.findViewById(R.id.icon);
				TextView textSlot3 = (TextView) containersSlot3.findViewById(R.id.text);
				iconSlot3.setImageResource(R.drawable.ic_qs_camera_off);
				String strSlot3 = getResources().getString(R.string.qs_label_camera);
				textSlot3.setText(strSlot3);
				
				View containersSlot4 = mPreviewContainer.getChildAt(4);
				ImageView iconSlot4 = (ImageView) containersSlot4.findViewById(R.id.icon);
				TextView textSlot4 = (TextView) containersSlot4.findViewById(R.id.text);
				iconSlot4.setImageResource(R.drawable.ic_qs_blaster_off);
				String strSlot4 = getResources().getString(R.string.qs_label_blaster);
				textSlot4.setText(strSlot4);
			}else if (mShowWeiXinAliPayFuKuan && !mShowWeiXinAliPaySaoYiSao){
				View containerSlot2 = mPreviewContainer.getChildAt(2);
				ImageView iconSlot2 = (ImageView) containerSlot2.findViewById(R.id.icon);
				TextView textSlot2 = (TextView) containerSlot2.findViewById(R.id.text);
				iconSlot2.setImageResource(R.drawable.ic_qs_pay_verse);
				String strText = getResources().getString(R.string.qs_label_pay_verse);
				textSlot2.setText(strText);
				
				View containersSlot3 = mPreviewContainer.getChildAt(3);
				ImageView iconSlot3 = (ImageView) containersSlot3.findViewById(R.id.icon);
				TextView textSlot3 = (TextView) containersSlot3.findViewById(R.id.text);
				iconSlot3.setImageResource(R.drawable.ic_qs_camera_off);
				String strSlot3 = getResources().getString(R.string.qs_label_camera);
				textSlot3.setText(strSlot3);
				
				View containersSlot4 = mPreviewContainer.getChildAt(4);
				ImageView iconSlot4 = (ImageView) containersSlot4.findViewById(R.id.icon);
				TextView textSlot4 = (TextView) containersSlot4.findViewById(R.id.text);
				iconSlot4.setImageResource(R.drawable.ic_qs_blaster_off);
				String strSlot4 = getResources().getString(R.string.qs_label_blaster);
				textSlot4.setText(strSlot4);
			}
		}else{
			View containerSlot2 = mPreviewContainer.getChildAt(2);
			ImageView iconSlot2 = (ImageView) containerSlot2.findViewById(R.id.icon);
			TextView textSlot2 = (TextView) containerSlot2.findViewById(R.id.text);
			iconSlot2.setImageResource(R.drawable.ic_qs_blaster_off);
			String strSlot2 = getResources().getString(R.string.qs_label_blaster);
			textSlot2.setText(strSlot2);
			
			View containersSlot3 = mPreviewContainer.getChildAt(3);
			ImageView iconSlot3 = (ImageView) containersSlot3.findViewById(R.id.icon);
			TextView textSlot3 = (TextView) containersSlot3.findViewById(R.id.text);
			iconSlot3.setImageResource(R.drawable.ic_qs_camera_off);
			String strSlot3 = getResources().getString(R.string.qs_label_camera);
			textSlot3.setText(strSlot3);
		}
	}
	
	private boolean isWeiXinAliPayEnable(String string) {
		boolean isEnable = false;
		try {
			String featureString = "";
			Class<?> featureClass = Class.forName("com.yulong.android.feature.FeatureString");
			Log.d(TAG, "featureClass is getted");
			Object featureObject = featureClass.newInstance();
			Field featureField = featureClass.getDeclaredField(string);
			featureField.setAccessible(true);
			featureString = featureField.get(featureObject).toString();
			Class<?> ConfigClass = Class.forName("com.yulong.android.feature.FeatureConfig");
			Method method = ConfigClass.getMethod("getBooleanValue", String.class);
			Object configoObject = ConfigClass.newInstance();
			isEnable = (Boolean) method.invoke(configoObject, featureString);
			Log.d(TAG, string + " is " + isEnable);
		} catch (Exception e) {
			Log.v(TAG, "cann't find field " + string +" : " + e);
		}
		return isEnable;
	}

	private Dialog getLoadingProgressDialog() {
		if (mLoadingProgressDialog == null) {
			mLoadingProgressDialog = new ProgressDialog(this);
			mLoadingProgressDialog.setMessage(getString(R.string.loading));
			mLoadingProgressDialog.setIndeterminate(true);
			mLoadingProgressDialog.setCancelable(false);
			mLoadingProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		}
		return mLoadingProgressDialog;
	}

	public boolean isSystemApp(PackageInfo packageInfo) {
		if (packageInfo == null) {
			return false;
		}
		if (!((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 || (packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)) {
			return false;
		}
		Intent launchIntent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> intents = mPackageManager.queryIntentActivitiesAsUser(launchIntent,
				PackageManager.GET_DISABLED_COMPONENTS, UserHandle.myUserId());
		if (intents != null) {
			final int N = intents.size();
			for (int j = 0; j < N; j++) {
				String packageName = intents.get(j).activityInfo.packageName;
				if (packageName.equalsIgnoreCase(packageInfo.packageName)) {
					return false;
				}
			}
		}
		return true;
	}

	private class ChooseAppAdapter implements ListAdapter {

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			// TODO
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			// TODO
		}

		@Override
		public int getCount() {
			return mPackageInfos.size();
		}

		@Override
		public Object getItem(int position) {
			return mPackageInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = View.inflate(BottomIconEditActivity.this, R.layout.bottom_icon_edit_list_content_view, null);
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
				holder.icon = (ImageView) convertView.findViewById(R.id.image);
				holder.appName = (TextView) convertView.findViewById(R.id.text);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
				if (holder.loadIconTask != null) {
					holder.loadIconTask.cancel(true);
				}
			}

			final CheckablePackageInfo pi = mPackageInfos.get(position);
			holder.icon.setImageBitmap(null);
			holder.appName.setText(pi.packageInfo.applicationInfo.loadLabel(mPackageManager).toString());
			holder.checkBox.setOnCheckedChangeListener(new OnListItemCheckedChangeListener(pi, holder));
			holder.checkBox.setChecked(pi.checked);
			holder.loadIconTask = new LoadIconAsyncTask(holder);
			holder.loadIconTask.execute(pi.packageInfo);

			return convertView;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return mPackageInfos.isEmpty();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}
	}

	public void onListItemClick(View v) {
		CheckBox cb = (CheckBox) v.findViewById(R.id.checkbox);
		cb.toggle();
	}

	private class LoadIconAsyncTask extends AsyncTask<PackageInfo, Integer, Bitmap> {

		private ViewHolder mHolder;

		public LoadIconAsyncTask(ViewHolder holder) {
			mHolder = holder;
		}

		@Override
		protected Bitmap doInBackground(PackageInfo... params) {
			return getIconForPackage(BottomIconEditActivity.this, params[0].applicationInfo.packageName);
		}

		@Override
		protected void onPostExecute(Bitmap icon) {
			mHolder.icon.setImageBitmap(icon);
			mHolder.loadIconTask = null;
		}
	}

	private class LoadPackageAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			List<PackageInfo> packageInfos = mPackageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
			mPackageInfos = new ArrayList<CheckablePackageInfo>();
			for (PackageInfo packageInfo : packageInfos) {
				if (!isSystemApp(packageInfo)) {
					CheckablePackageInfo pi = new CheckablePackageInfo();
					pi.packageInfo = packageInfo;
					mPackageInfos.add(pi);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void icon) {
			mAppListView.setAdapter(new ChooseAppAdapter());
			getCheckedPackagesFormXML();
			if (mLoadingProgressDialog != null) {
				mLoadingProgressDialog.dismiss();
			}
		}
	}

	private class ViewHolder {
		public LoadIconAsyncTask loadIconTask;
		public TextView appName;
		public ImageView icon;
		public CheckBox checkBox;
	}

	public class CheckablePackageInfo {
		public PackageInfo packageInfo;
		public boolean checked;
		public int order = -1;
	}

	private class OnListItemCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {

		private CheckablePackageInfo mPackageInfo;
		private ViewHolder mHolder;

		public OnListItemCheckedChangeListener(CheckablePackageInfo pi, ViewHolder holder) {
			mPackageInfo = pi;
			mHolder = holder;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (mPackageInfo.checked == isChecked) {
				return;
			}
			if (!isChecked) {
				clearIconContainerForPackage(mPackageInfo.packageInfo);
			} else {
				if (!addIconForPackage(mPackageInfo.packageInfo)) {
					mHolder.checkBox.setChecked(false);
					toast(getResources().getString(R.string.icon_editor_overflow_hint), Toast.LENGTH_SHORT);
				}
			}
		}
	}

	public View getNextEmptyIconContainer() {
		int count = mPreviewContainer.getChildCount();
		int start = 4;
		if(mShowWeiXinAliPayFuKuan && mShowWeiXinAliPaySaoYiSao){
			start = 6;
		}else if((!mShowWeiXinAliPayFuKuan && mShowWeiXinAliPaySaoYiSao)|| (mShowWeiXinAliPayFuKuan && !mShowWeiXinAliPaySaoYiSao)){
			start = 5;
		}
		for (int i = start; i < count; i++) {
			View container = mPreviewContainer.getChildAt(i);
			if (container.getTag() == null) {
				return container;
			}
		}
		return null;
	}

	public static Bitmap getIconForPackage(Context context, String pk) {
		Bitmap icon = null;
		Bitmap bp = StatusBarIconView.getPackageThemeBitmap(context, pk, UserHandle.myUserId());
		if (bp != null) {
			Bitmap temp = Utilities.formatIconBitmap(bp, context.getApplicationContext(), 0);
			if (temp != null && !temp.isRecycled()) {
				icon = temp;
			} else {
				icon = bp;
			}
		}
		return icon;
	}

	public void toast(String string, int length) {
		if (mToast == null) {
			mToast = Toast.makeText(this, string, length);
		} else {
			mToast.setText(string);
			mToast.setDuration(length);
		}
		mToast.show();
	}

	public boolean addIconForPackage(PackageInfo mPackageInfo) {
		if (updatePackageCheckState(mPackageInfo.applicationInfo.packageName, true)) {
			saveCheckedPackagesToXML();
			if(QuickSettingLauncher.getInstance(getApplicationContext()) != null){
				QuickSettingLauncher.getInstance(getApplicationContext()).notifyBottomIconChanged(getSelectedPackageTreeMap());
			}
			
			return true;
		} else {
			return false;
		}
	}

	private void saveCheckedPackagesToXML() {
		SharedPreferences mySharedPreferences = getSharedPreferences("bottom_icon_config", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = mySharedPreferences.edit();
		for (int i = 0; i < mPackageInfos.size(); i++) {
			editor.putString("icon" + i, "");
		}
		TreeMap<Integer, CheckablePackageInfo> selectedTreeMap = getSelectedPackageTreeMap();
		int j = 0;
		for (Iterator<Integer> iterator = selectedTreeMap.keySet().iterator(); iterator.hasNext();) {
			editor.putString("icon" + j, selectedTreeMap.get(iterator.next()).packageInfo.applicationInfo.packageName);
			j++;
		}
		editor.commit();
	}

	private void getCheckedPackagesFormXML() {
		SharedPreferences mySharedPreferences = getSharedPreferences("bottom_icon_config", Activity.MODE_PRIVATE);
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			String pk = mySharedPreferences.getString("icon" + i, "");
			if (TextUtils.isEmpty(pk)) {
				break;
			} else {
				updatePackageCheckState(pk, true);
			}
		}
	}

	private boolean updatePackageCheckState(String pk, boolean checked) {
		for (int j = 0; j < mPackageInfos.size(); j++) {
			CheckablePackageInfo cpi = mPackageInfos.get(j);
			if (cpi.packageInfo.applicationInfo.packageName.equals(pk)) {
				cpi.checked = checked;
				if (checked) {
					cpi.order = mOrder++;
				}
				if (updatePreviews()) {
					return true;
				} else {
					cpi.checked = false;
					return false;
				}
			}
		}
		return false;
	}

	private boolean updatePreviews() {
		// step 1, clear all previews
		int count = mPreviewContainer.getChildCount();
		int start = 4;
		if(mShowWeiXinAliPayFuKuan && mShowWeiXinAliPaySaoYiSao){
			start = 6;
		}else if((!mShowWeiXinAliPayFuKuan && mShowWeiXinAliPaySaoYiSao)|| (mShowWeiXinAliPayFuKuan && !mShowWeiXinAliPaySaoYiSao)){
			start = 5;
		}
		for (int i = start; i < count; i++) {
			View container = mPreviewContainer.getChildAt(i);
			ImageView icon = (ImageView) container.findViewById(R.id.icon);
			TextView text = (TextView) container.findViewById(R.id.text);
			icon.setImageBitmap(null);
			text.setText("");
			container.setTag(null);
		}

		// step 2,get checked package and sort by order
		TreeMap<Integer, CheckablePackageInfo> selectedPackages = getSelectedPackageTreeMap();

		// step 2, fill preview that user selected
		for (Iterator<Integer> iterator = selectedPackages.keySet().iterator(); iterator.hasNext();) {
			CheckablePackageInfo cpi = selectedPackages.get(iterator.next());
			View container = getNextEmptyIconContainer();
			if (container != null) {
				container.setTag(cpi.packageInfo.applicationInfo.packageName);
				ImageView icon = (ImageView) container.findViewById(R.id.icon);
				TextView text = (TextView) container.findViewById(R.id.text);
				icon.setImageBitmap(getIconForPackage(this, cpi.packageInfo.applicationInfo.packageName));
				try {
					text.setText(mPackageManager.getPackageInfo(cpi.packageInfo.applicationInfo.packageName,
							PackageManager.GET_UNINSTALLED_PACKAGES).applicationInfo.loadLabel(mPackageManager));
				} catch (Exception e) {
					text.setText("");
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private TreeMap<Integer, CheckablePackageInfo> getSelectedPackageTreeMap() {
		TreeMap<Integer, CheckablePackageInfo> sortedMap = new TreeMap<Integer, CheckablePackageInfo>();
		for (int j = 0; j < mPackageInfos.size(); j++) {
			CheckablePackageInfo cpi = mPackageInfos.get(j);
			if (cpi.checked) {
				sortedMap.put(cpi.order, cpi);
			}
		}
		return sortedMap;
	}

	public void clearIconContainerForPackage(PackageInfo packageInfo) {
		if (updatePackageCheckState(packageInfo.applicationInfo.packageName, false)) {
			saveCheckedPackagesToXML();
			if(QuickSettingLauncher.getInstance(getApplicationContext()) != null){
				QuickSettingLauncher.getInstance(getApplicationContext()).notifyBottomIconChanged(getSelectedPackageTreeMap());
			}
		}
	}
}
