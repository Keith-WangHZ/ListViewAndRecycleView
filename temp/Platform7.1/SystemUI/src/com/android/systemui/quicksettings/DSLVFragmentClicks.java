package com.android.systemui.quicksettings;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class DSLVFragmentClicks extends DSLVFragment {

    public static DSLVFragmentClicks newInstance(int headers, int footers) {
        DSLVFragmentClicks f = new DSLVFragmentClicks();

        Bundle args = new Bundle();
        args.putInt(KEY_HEADERS, headers);
        args.putInt(KEY_FOOTERS, footers);
        //args.putSerializable(KEY_ITEMS, items);
        f.setArguments(args);

        return f;
    }

    AdapterView.OnItemClickListener mClickListener = 
    	new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	QuickSettingsItem item = (QuickSettingsItem)parent.getItemAtPosition(position); 
//            Toast.makeText(getActivity(), "Click " + item.mText, Toast.LENGTH_SHORT).show();
        }
    };
    
    AdapterView.OnItemLongClickListener mLongClickListener = 
        new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        	QuickSettingsItem item = (QuickSettingsItem)parent.getItemAtPosition(position); 
//            Toast.makeText(getActivity(), "Long Click " + item.mText, Toast.LENGTH_SHORT).show();
            return true;
        }
    };

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        ListView lv = getListView();
//        lv.setOnItemClickListener(mClickListener);
//        lv.setOnItemLongClickListener(mLongClickListener);
    }
}
