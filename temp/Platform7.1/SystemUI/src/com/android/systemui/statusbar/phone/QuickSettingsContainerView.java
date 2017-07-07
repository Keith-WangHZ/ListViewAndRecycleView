
package com.android.systemui.statusbar.phone;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.widget.PagerAdapter;
import com.android.internal.widget.ViewPager;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
//import com.android.systemui.quicksettings.QuickSettingsConfigActivity;
import com.android.systemui.quicksettings.QuickSettingsItem;
//import com.android.systemui.recent.RecentsView;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
import com.android.systemui.statusbar.policy.CurrentUserTracker;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class QuickSettingsContainerView extends LinearLayout implements QuickSettingsModel.IUpdateView {
    private static final String TAG = "QuickSettingsContainerView";

	class MyPageAdapter extends PagerAdapter{
		public MyPageAdapter() {
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return viewList.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == (View) obj;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (position < viewList.size()){
				View v = viewList.get(position);
				container.addView(v);
				return v;
			}
			return null;
		}
	}
    private MyPageAdapter mPageAdapter = new MyPageAdapter();	
    private ViewPager mViewPager;
    private List<View> viewList;
    private List<QuickSettingsItemView> mItems = new ArrayList<QuickSettingsItemView>();
    private int mEditItem = 0;
    
    public QuickSettingsContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        viewList = new ArrayList<View>();        
    }
    private void AddPage(){
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View page1 = inflater.inflate(R.layout.quick_setting_page, null);
        mItems.add((QuickSettingsItemView)page1.findViewById(R.id.item0));
        mItems.add((QuickSettingsItemView)page1.findViewById(R.id.item1));
        mItems.add((QuickSettingsItemView)page1.findViewById(R.id.item2));
        mItems.add((QuickSettingsItemView)page1.findViewById(R.id.item3));
        mItems.add((QuickSettingsItemView)page1.findViewById(R.id.item4));
        UpdateEditItem();
        viewList.add(page1);
        mPageAdapter.notifyDataSetChanged();
    }
    private void UpdateEditItem(){
        for(int i = mItems.size() - 5; i >= 0 && i < mItems.size(); i++){
        	if (i < mEditItem){
                YulongQuickSettings yqs = YulongQuickSettings.getInstance(mContext); 
        		yqs.setItemOnClickListen(mItems.get(i));                
            	mItems.get(i).setVisibility(View.VISIBLE);        		
        	}else if(i == mEditItem){
            	mItems.get(i).setOnClickListener(mEditClick);    
            	mItems.get(i).setVisibility(View.VISIBLE);
        	}else if( i > mEditItem){
            	mItems.get(i).setVisibility(View.INVISIBLE);        		
        	}
        }    	
    }
    @Override
    protected void onFinishInflate() {
    	super.onFinishInflate();
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(mPageAdapter);
    	AddPage();
        mViewPager.setCurrentItem(0); //璁剧疆榛樿褰撳墠椤�    	
        mViewPager.setHorizontalFadingEdgeEnabled(false);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        YulongQuickSettings yqs = YulongQuickSettings.getInstance(mContext);
        yqs.AddUpdateViewCallback(this);    
        LogHelper.sd(TAG, "onFinishInflate");
    }
    View.OnClickListener mEditClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//RecentsView.Show(false);//===modify
			Intent intent = new Intent();
			intent.setClassName("com.android.systemui","com.android.systemui.quicksettings.QuickSettingsConfigActivity");
			//intent.setClass(mContext, QuickSettingsActivity.class);
			CurrentUserTracker.startActivityAsCurrentUser(intent);				
		}
	};   
    public void onConfigurationChanged(){
    	State state = new State(-1);
    	state.iconId = R.drawable.quicksettings_more;
    	state.textId = R.string.quicksettings_edit_disable;
    	state.status = State.STATUS_DISABLE;
    	mItems.get(mEditItem).updateState(state);
    }
    @Override
    public void updateView(State state) {
    	int id = state.id;
    	int order = state.order;
        LogHelper.sd(TAG, "updateView id = " + id + " order = " + order);
        if(order >= 0){
	        LayoutInflater inflater = LayoutInflater.from(mContext);
	        if(order + 1 > mEditItem ){
	        	mEditItem = order + 1;
	        	UpdateEditItem();
	        	while(mEditItem >= mItems.size()){
	        		AddPage();
	        	}
	        }
	        mItems.get(order).updateState(state);
        }
        onConfigurationChanged();
    }    
    
    @Override
	public void resetQsView(Boolean bReinitialize) {
		// TODO Auto-generated method stub
    	if(bReinitialize){
    		mItems.clear();
        	viewList.clear();
        	mEditItem = 0;
    	}
	}
    public void updateVisibleSize(int size) {

    }
}
