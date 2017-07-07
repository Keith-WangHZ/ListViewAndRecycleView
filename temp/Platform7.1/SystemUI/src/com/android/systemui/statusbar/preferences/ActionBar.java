/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.statusbar.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

/**
 * 
 * @author Cyril Mottier
 */
public class ActionBar extends LinearLayout {

    /**
     * Default identifier applied to a newly added {@link ActionBarItem}s.
     * 
     * @deprecated Adding items to the ActionBar with no identifier does not
     *             allow client to retrieve a particular {@link ActionBarItem}
     *             safely. In order to avoid this problem, {@link ActionBarItem}
     *             s should be added with methods that requires explicit
     *             identifiers such as
     *             {@link ActionBar#addItem(ActionBarItem, int)} or
     *             {@link ActionBar#addItem(greendroid.widget.ActionBarItem.Type, int)}
     */
    public static final int NONE = 0;

    /**
     * The Type specifies the layout of the ActionBar.
     * 
     * @author Cyril Mottier
     */
    public enum Type {
        /**
         * ActionBar layout will contain a home item on the left and optional
         * {@link ActionBarItem}s on the right. The space that left between is
         * used to display the title of the current Activity.
         */
        Normal,

        /**
         * ActionBar layout will contain the application Drawable on the left
         * and optional {@link ActionBarItem}s on the right. Please note the
         * Dashboard type does not display the title of the current Activity.
         * 
         * @see R.attr#YLActionBarApplicationDrawable
         */
        Dashboard,

        /**
         * ActionBar layout will contain optional {@link ActionBarItem}s on the
         * right. The space that left will be used to display the title of the
         * current Activity.
         */
        Empty
    }

    /**
     * Interface definition for a callback to be invoked when a user is
     * interacting with an {@link ActionBar}.
     * 
     * @author Cyril Mottier
     */
    public interface OnActionBarListener {

        /**
         * Index used to indicate the ActionBar home item has been clicked.
         */
        int HOME_ITEM = -1;

        /**
         * Clients may listen to this method in order to be notified the user
         * has clicked on an item.
         * 
         * @param position The position of the item in the action bar.
         *            {@link OnActionBarListener#HOME_ITEM} means the user
         *            pressed the "Home" button. 0 means the user clicked the
         *            first {@link ActionBarItem} (the leftmost item) and so on.
         */
        void onActionBarItemClicked(int position);
    }

    private TextView mTitleView;
    private ImageButton mHomeButton;
    private ImageView mHomeView;

    private boolean mMerging = false;

    private CharSequence mTitle;
    private ActionBar.Type mType;
    private OnActionBarListener mOnActionBarListener;

    private Drawable mDividerDrawable;
    private Drawable mHomeDrawable;
    private int mDividerWidth;

    private int mMaxItemsCount;

    public ActionBar(Context context) {
        this(context, null);
    }

    public ActionBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.YLActionBarStyle);
    }

    public ActionBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        initActionBar();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionBar, defStyle, 0);

        mTitle = a.getString(R.styleable.ActionBar_title);

        mDividerDrawable = a.getDrawable(R.styleable.ActionBar_dividerDrawable);
        mDividerWidth = a.getDimensionPixelSize(R.styleable.ActionBar_dividerWidth, -1);
        mHomeDrawable = a.getDrawable(R.styleable.ActionBar_homeDrawable);
        mMaxItemsCount = a.getInt(R.styleable.ActionBar_maxItems, 3);

        int layoutID;
        mType = Type.Normal;
        layoutID = R.layout.yl_action_bar_normal;

        // HACK Cyril: Without this, the onFinishInflate is called twice !?!
        // This issue is due to a bug when Android inflates a layout with a
        // parent - which is compulsory with a <merge /> tag. I've reported this
        // bug to Romain Guy who fixed it (patch will probably be available in
        // the Gingerbread release).
        mMerging = true;
        LayoutInflater.from(context).inflate(layoutID, this);
        mMerging = false;

        a.recycle();
    }

    private void initActionBar() {
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (!mMerging) {
        	mHomeButton = (ImageButton) findViewById(R.id.yl_action_bar_home_item);
            mHomeButton.setOnClickListener(mClickHandler);
            mHomeButton.setImageDrawable(mHomeDrawable);
            mTitleView = (TextView) findViewById(R.id.yl_action_bar_title);
            setTitle(mTitle);
        }
    }

    /**
     * Register a callback to be invoked when the user interacts with the
     * {@link ActionBar}.
     * 
     * @param listener The callback that will run.
     */
    public void setOnActionBarListener(OnActionBarListener listener) {
        mOnActionBarListener = listener;
    }

    /**
     * @param title The title to set to this {@link ActionBar}
     */
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }
    
    private OnClickListener mClickHandler = new OnClickListener() {

        public void onClick(View v) {
            if (mOnActionBarListener != null) {

                if (v == mHomeButton) {
                    mOnActionBarListener.onActionBarItemClicked(OnActionBarListener.HOME_ITEM);
                    return;
                }
        }
       }

    };

}
