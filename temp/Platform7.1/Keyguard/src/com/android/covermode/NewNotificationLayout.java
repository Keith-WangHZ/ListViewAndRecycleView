package com.android.covermode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.android.keyguard.R;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NewNotificationLayout extends LinearLayout {

	private static final String TAG = "NewNotificationLayout";
	private ListView mNotificationListView;
	private TextView mTextView;
	private CoverNotificationCallback mCallback;
	CoverNotificationAdapter mAdapter;
	private boolean mShouldWaitUntilTimeOut = false;
	private HolsterFixableView mHolsterView;
	private PackageManager mPm;

	public final class ViewHolder {
		ImageView mListIcon;
		TextView mListTitle;
		TextView mListContent;
		TextView mListTime;
	}

	public NewNotificationLayout(Context context) {
		super(context, null);
	}

	public NewNotificationLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPm = mContext.getPackageManager();
		mAdapter = new CoverNotificationAdapter();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mNotificationListView = (ListView) findViewById(R.id.new_notification_list_View);
		mTextView = (TextView) findViewById(R.id.new_notification_text_View);
		mNotificationListView.setAdapter(mAdapter);
		mNotificationListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mShouldWaitUntilTimeOut) {
					return;
				}
				final TextView content = (TextView) view.findViewById(R.id.notification_content);
				final String oldContent = content.getText().toString();
				content.setText(mContext.getResources().getString(R.string.cover_notification_clicked));
				mShouldWaitUntilTimeOut = true;
				postDelayed(new Runnable() {
					public void run() {
						content.setText(oldContent);
						mShouldWaitUntilTimeOut = false;
					}
				}, 2000);
			}
		});
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		resetViews();
	}

	public void setHolsterView(HolsterFixableView holsterFixableView) {
		mHolsterView = holsterFixableView;
		resetViews();
	}
	
	public interface CoverNotificationCallback {
		public void onCoverNotificationRemoved(StatusBarNotification notification);
	}

	public void setCallback(CoverNotificationCallback callback) {
		mCallback = callback;
	}

	public void resetViews() {
		if (mHolsterView == null) {
			return;
		}
		Log.v(TAG, "resetViews mNotificationListView.getChildCount() = " + mNotificationListView.getChildCount()
				+ ", mViewList's size = " + mHolsterView.mNotificationsList.size());
		mAdapter.notifyDataSetChanged();
		if (mHolsterView.mNotificationsList.size() == 0) {
			mNotificationListView.setVisibility(View.GONE);
			mTextView.setVisibility(View.VISIBLE);
		} else {
			mTextView.setVisibility(View.GONE);
			mNotificationListView.setVisibility(View.VISIBLE);
		}
	}

	private Drawable getIcon(StatusBarNotification notification) {
		Resources r = null;

		Drawable defualt = mContext.getResources().getDrawable(R.drawable.xtime);

		if (notification.getPackageName() != null) {
			int userId = notification.getUser().getIdentifier();
			if (userId == UserHandle.USER_ALL) {
				userId = UserHandle.USER_OWNER;
			}
			try {
				r = mPm.getResourcesForApplicationAsUser(notification.getPackageName(), userId);
			} catch (PackageManager.NameNotFoundException ex) {
				Log.e(TAG, "Icon package not found: " + notification.getPackageName());
				return defualt;
			}
		} else {
			r = mContext.getResources();
		}

		if (notification.getNotification().icon == 0) {
			Log.e(TAG, "invalid icon id 0");
			return defualt;
		}

		try {
			return r.getDrawable(notification.getNotification().icon);
		} catch (RuntimeException e) {
			Log.w(TAG, "Icon not found in " + (notification.getPackageName() != null ? notification.getNotification().icon : "<system>") + ": "
					+ Integer.toHexString(notification.getNotification().icon));
		}
		return defualt;
	}

	public class CoverNotificationAdapter extends BaseAdapter implements ListAdapter {

		@Override
		public int getCount() {
			return mHolsterView == null ? 0 : mHolsterView.mNotificationsList.size();
		}

		@Override
		public Object getItem(int position) {
			return mHolsterView.mNotificationsList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = View.inflate(NewNotificationLayout.this.mContext, R.layout.new_notification_item_layout, null);
				holder.mListContent = (TextView) convertView.findViewById(R.id.notification_content);
				holder.mListTitle = (TextView) convertView.findViewById(R.id.notification_title);
				holder.mListTime = (TextView) convertView.findViewById(R.id.notification_time);
				holder.mListIcon = (ImageView) convertView.findViewById(R.id.notification_type_img);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			String title;
			String content;
			StatusBarNotification bean = mHolsterView.mNotificationsList.get(position);
			Bundle extraBundle = bean.getNotification().extras;
			if (extraBundle == null) {
				Log.d(TAG, "NotifyMsg extraBundle is null");
				String pkn = bean.getPackageName();
				content = title = pkn.substring(pkn.lastIndexOf("."));
			} else {
				title = extraBundle.getString(Notification.EXTRA_TITLE, null);
				content = extraBundle.getString(Notification.EXTRA_TEXT, null);
			}

			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
			long curDate = bean.getNotification().when;
			String dateTime = sdf.format(curDate);

			holder.mListIcon.setBackground(NewNotificationLayout.this.getIcon(mHolsterView.mNotificationsList.get(position)));
			holder.mListTitle.setText(title);
			holder.mListContent.setText(content);
			holder.mListTime.setText(dateTime);

			return convertView;
		}
	}
}
