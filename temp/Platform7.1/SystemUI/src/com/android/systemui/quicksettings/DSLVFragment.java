package com.android.systemui.quicksettings;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.quicksettings.dslv.DragSortController;
import com.android.systemui.quicksettings.dslv.DragSortListView;
import com.android.systemui.statusbar.phone.QuickSettingsItemView;
import com.android.systemui.statusbar.phone.YulongQuickSettings;
import com.android.systemui.statusbar.phone.QuickSettingsModel;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
public class DSLVFragment extends ListFragment implements QuickSettingsModel.IUpdateView{
	
	public static final String KEY_HEADERS = "headers"; //value : int
	public static final String KEY_FOOTERS = "footers"; //value : int
	public static final String KEY_ITEMS = "items";		//value : ArrayList<QuickSettingsObj>
	
	QuickSettingsAdapter mAdapter;
	
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                    	LogHelper.sd("DataControler","updateView drop from:" + from + " to:"+ to);
                        QuickSettingsItem item = (QuickSettingsItem)mAdapter.getItem(from);
                        mAdapter.remove(item);
                        mAdapter.insert(item, to);
                        //onPositionChange(from, to, mAdapter);
                    	if(from > to){
                    		int temp = to;
                    		to = from;
                    		from = temp;
                    	}                        
                        for(int i = from; i <= to; i++){
                            item = (QuickSettingsItem)mAdapter.getItem(i);
                        	LogHelper.sd("DataControler","updateView setOrder id:" +item.state.id +  " order:" + i);
                        	YulongQuickSettings.getInstance(null).setOrder(item.state.id,i);
                        }
                        DSLVFragment.this.getView().postDelayed(
	                        		new Runnable(){
	                        			public void run() {
		                                	YulongQuickSettings.getInstance(null).RefreshView(-1);           				
	                        			};    			
	                        		},100
                        		);
                    }
                }
            };

    private DragSortListView.RemoveListener onRemove = 
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    mAdapter.remove((QuickSettingsItem)mAdapter.getItem(which));
                }
            };

    protected int getLayout() {
        // this DSLV xml declaration does not call for the use
        // of the default DragSortController; therefore,
        // DSLVFragment has a buildController() method.
        return R.layout.quick_settings_dslv_fragment_main;
    }
    
    /**
     * Return list item layout resource passed to the ArrayAdapter.
     */
    protected int getItemLayout() {
        /*if (removeMode == DragSortController.FLING_LEFT_REMOVE || removeMode == DragSortController.SLIDE_LEFT_REMOVE) {
            return R.layout.list_item_handle_right;
        } else */
    	/*
    	if (removeMode == DragSortController.CLICK_REMOVE) {
            return R.layout.quick_settings_list_item_click_remove;
        } else {
            return R.layout.quick_settings_list_item_handle_right;
        }
        */    
    	return R.layout.quick_settings_list_item_handle_right;
    }

    private DragSortListView mDslv;
    private DragSortController mController;

    public int dragStartMode = DragSortController.ON_DOWN;
    public boolean removeEnabled = false;
    public int removeMode = DragSortController.FLING_REMOVE;
    public boolean sortEnabled = true;
    public boolean dragEnabled = true;

    public static DSLVFragment newInstance(int headers, int footers, ArrayList<QuickSettingsItem> itmes) {
        DSLVFragment f = new DSLVFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_HEADERS, headers);
        args.putInt(KEY_FOOTERS, footers);
        //args.putSerializable(KEY_ITEMS, itmes);
        f.setArguments(args);

        return f;
    }

    public DragSortController getController() {
        return mController;
    }

    /**
     * Called from DSLVFragment.onActivityCreated(). Override to
     * set a different adapter.
     */
	ArrayList<QuickSettingsItem> mItems = new ArrayList<QuickSettingsItem>();    
    public void setAdapter() {
    	YulongQuickSettings.getInstance(null).AddUpdateViewCallback(this);
    	LogHelper.sd("DSLVFragment", "DSLVFragment onFinishInflate");
        mAdapter = new QuickSettingsAdapter(getActivity(), getItemLayout(), mItems);
        setListAdapter(mAdapter);
    }
	@Override
	public void updateView(State state) {
		if(state.order >= 0){
			if(mAdapter == null){
				while(mItems.size() <= state.order){
					State sNew = new State(-1);
					QuickSettingsItem item = new QuickSettingsItem(sNew);
					mItems.add(item);
				}
				QuickSettingsItem item = mItems.get(state.order);
				item.state = state;
			}else{
				QuickSettingsItem item = mAdapter.getItem(state.order);
				if(item != null && item.state.id == state.id){
					item.state = state;
					mAdapter.notifyDataSetChanged();
				}else{
					if(null != item)Log.e("updateView","updateView error id:" + state.id + " order:" + state.order + " item.state.id:" + item.state.id+ " item.state.order:" + item.state.order);
				}
			}
		}
	}
	

	@Override
	public void resetQsView(Boolean bReinitialize) {
		// TODO Auto-generated method stub
		mItems.clear();
	}
    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(DragSortListView dslv) {
        // defaults are
        //   dragStartMode = onDown
        //   removeMode = flingRight
        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.quick_settings_list_item_icon_drag_handle);
        
        /* lcy: 不用 "remove" 功能 */
        //controller.setClickRemoveId(R.id.click_remove);
        
        controller.setRemoveEnabled(removeEnabled);
        controller.setSortEnabled(sortEnabled);
        controller.setDragInitMode(dragStartMode);
        controller.setRemoveMode(removeMode);
        return controller;
    }


    /** Called when the activity is first created. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        //mDslv = (DragSortListView) inflater.inflate(getLayout(), container, false);
    	mDslv = new DragSortListView(getActivity());
    	
        mController = buildController(mDslv);
        mDslv.setFloatViewManager(mController);
        mDslv.setOnTouchListener(mController);
        mDslv.setDragEnabled(dragEnabled);

        return mDslv;
    }
    
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		YulongQuickSettings.getInstance(null).RemoveUpdateViewCallback(this);
	}
    @SuppressWarnings("unchecked")
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDslv = (DragSortListView) getListView(); 

        mDslv.setDropListener(onDrop);
        mDslv.setRemoveListener(onRemove);

        Bundle args = getArguments();
        int headers = 0;
        int footers = 0;
        ArrayList<QuickSettingsItem> objects = new ArrayList<QuickSettingsItem>();
        if (args != null) {
            headers = args.getInt(KEY_HEADERS, 0);
            footers = args.getInt(KEY_FOOTERS, 0);
            objects = (ArrayList<QuickSettingsItem>)args.getSerializable(KEY_ITEMS);
        }

        for (int i = 0; i < headers; i++) {
            addHeader(getActivity(), mDslv);
        }
        for (int i = 0; i < footers; i++) {
            addFooter(getActivity(), mDslv);
        }

        setAdapter();
    }


    public static void addHeader(Activity activity, DragSortListView dslv) {
        int count = dslv.getHeaderViewsCount();
        //LayoutInflater inflater = activity.getLayoutInflater();
        //TextView header = (TextView) inflater.inflate(R.layout.header_footer, null);
        TextView header = new TextView(activity);
        header.setText("Header #" + (count + 1));

        dslv.addHeaderView(header, null, false);
    }

    public static void addFooter(Activity activity, DragSortListView dslv) {
        int count = dslv.getFooterViewsCount();
        //LayoutInflater inflater = activity.getLayoutInflater();
        //TextView footer = (TextView) inflater.inflate(R.layout.header_footer, null);
        TextView footer = new TextView(activity);
        footer.setText("Footer #" + (count + 1));

        dslv.addFooterView(footer, null, false);
    }

	public static class ViewHolder
	{
		QuickSettingsItemView mIvIcon;
		TextView mTvText;
		ImageView mIvDragHandle;
	}
    
    public class QuickSettingsAdapter extends ArrayAdapter<QuickSettingsItem>
    {
    	private LayoutInflater mLayoutInflater;
    	public QuickSettingsAdapter(Context context, int layoutResourceId, List<QuickSettingsItem> items) {
			super(context, layoutResourceId, items);
			mLayoutInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null)
			{
				convertView = mLayoutInflater.inflate(getItemLayout(), null);
				holder = new ViewHolder();
				holder.mIvIcon = (QuickSettingsItemView) convertView.findViewById(R.id.quick_settings_list_item_icon);
				holder.mIvIcon.mIsConfigItem = true;
				holder.mTvText = (TextView) convertView.findViewById(R.id.quick_settings_list_item_text);
				holder.mIvDragHandle = (ImageView) convertView.findViewById(R.id.quick_settings_list_item_icon_drag_handle);
				YulongQuickSettings.getInstance(null).setItemOnClickListen(holder.mIvIcon);
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder)convertView.getTag();
			}
			
			QuickSettingsItem item = super.getItem(position);
			item.vItem = holder.mIvIcon;
			holder.mIvIcon.updateState(item.state);
			if(item.state.textId > 0){
				holder.mTvText.setText(item.state.textId);
			}
			return convertView;
		}
		
		public ArrayList<QuickSettingsItem> getQuickSettingsItems()
		{
	    	ArrayList<QuickSettingsItem> items = null;
	    	if (getCount() > 0)
	    	{
	    		items = new ArrayList<QuickSettingsItem>();
	    		for (int i = 0; i < getCount(); i++)
	    		{
	    			items.add((QuickSettingsItem)getItem(i));
	    		}
	    	}
	    	return items;
		}
    }
    
    
    public ArrayList<QuickSettingsItem> getQuickSettingsItems()
    {
    	ArrayList<QuickSettingsItem> items = null;
    	if (mAdapter != null)
    	{
    		items = mAdapter.getQuickSettingsItems();
    	}
    	return items;
    }


}
