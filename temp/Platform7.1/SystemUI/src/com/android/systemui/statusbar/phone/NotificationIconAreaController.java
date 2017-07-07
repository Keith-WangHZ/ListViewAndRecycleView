package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.preferences.NotificationCollapseManage;

import java.util.ArrayList;

/**
 * A controller for the space in the status bar to the left of the system icons. This area is
 * normally reserved for notifications.
 */
public class NotificationIconAreaController {
    private final NotificationColorUtil mNotificationColorUtil;

    private int mIconSize;
    private int mIconHPadding;
    private int mIconTint = Color.WHITE;

    private PhoneStatusBar mPhoneStatusBar;
    
    protected View mNotificationIconAreaKeyguard;
    private IconMerger mNotificationIconsKeyguard;
    private ImageView mMoreIconKeyguard;
    
    
    protected View mNotificationIconAreaStatusbar;
    private IconMerger mNotificationIconsStatusbar;
    private ImageView mMoreIconStatusbar;
    
    private final Rect mTintArea = new Rect();
    private Boolean mIsKeyguard = false;
    private ViewGroup mKeyguardStatusBar;
    private Context mContext;

    public NotificationIconAreaController(Context context, PhoneStatusBar phoneStatusBar, ViewGroup keyguardStatusBar) {
        mPhoneStatusBar = phoneStatusBar;
        mNotificationColorUtil = NotificationColorUtil.getInstance(context);

        mContext = context;
        mKeyguardStatusBar = keyguardStatusBar;
        initializeNotificationAreaViews(context);
    }


    /**
     * Initializes the views that will represent the notification area.
     */
    protected void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);

       
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        mNotificationIconAreaKeyguard = layoutInflater.inflate(R.layout.notification_icon_area_keyguard, mKeyguardStatusBar);
        LayoutInflater layoutInflater1 = LayoutInflater.from(context);
        mNotificationIconAreaStatusbar = layoutInflater1.inflate(R.layout.notification_icon_area, null);

        
        mNotificationIconsKeyguard =
                (IconMerger) mNotificationIconAreaKeyguard.findViewById(R.id.notificationIcons_keyguard);
        mMoreIconKeyguard = (ImageView) mNotificationIconAreaKeyguard.findViewById(R.id.moreIcon_keyguard);
        
        mNotificationIconsStatusbar =
                (IconMerger) mNotificationIconAreaStatusbar.findViewById(R.id.notificationIcons);
        mMoreIconStatusbar = (ImageView) mNotificationIconAreaStatusbar.findViewById(R.id.moreIcon);
        
        if (mMoreIconStatusbar != null) {
            //mMoreIconStatusbar.setImageTintList(ColorStateList.valueOf(mIconTint));
            mNotificationIconsStatusbar.setOverflowIndicator(mMoreIconStatusbar);
        }
        if (mMoreIconKeyguard != null) {
            //mMoreIconKeyguard.setImageTintList(ColorStateList.valueOf(mIconTint));
            mNotificationIconsKeyguard.setOverflowIndicator(mMoreIconKeyguard);
        }
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        final LinearLayout.LayoutParams params = generateIconLayoutParams();
        for (int i = 0; i < mNotificationIconsStatusbar.getChildCount(); i++) {
            View child = mNotificationIconsStatusbar.getChildAt(i);
            child.setLayoutParams(params);
        }
        
        final LinearLayout.LayoutParams params1 = generateIconLayoutParams();
        for (int i = 0; i < mNotificationIconsKeyguard.getChildCount(); i++) {
            View child = mNotificationIconsKeyguard.getChildAt(i);
            child.setLayoutParams(params);
        }
    }

    @NonNull
    private LinearLayout.LayoutParams generateIconLayoutParams() {
        return new LinearLayout.LayoutParams(
                mIconSize + 2 * mIconHPadding, getHeight());
    }

    private void reloadDimens(Context context) {
        Resources res = context.getResources();
        mIconSize = res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_icon_size);
        mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
    }

    /**
     * Returns the view that represents the notification area.
     */
    public View getNotificationInnerAreaView() {
    	return mNotificationIconAreaStatusbar;
    }
    
    public View getNotificationInnerAreaViewKeyguard() {
    	return mNotificationIconAreaKeyguard;
    }

    /**
     * See {@link StatusBarIconController#setIconsDarkArea}.
     *
     * @param tintArea the area in which to tint the icons, specified in screen coordinates
     */
    public void setTintArea(Rect tintArea) {
        if (tintArea == null) {
            mTintArea.setEmpty();
        } else {
            mTintArea.set(tintArea);
        }
        applyNotificationIconsTint();
    }

    /**
     * Sets the color that should be used to tint any icons in the notification area. If this
     * method is not called, the default tint is {@link Color#WHITE}.
     */
    public void setIconTint(int iconTint) {
        mIconTint = iconTint;
//        if (mMoreIconStatusbar != null) {
//            mMoreIconStatusbar.setImageTintList(ColorStateList.valueOf(mIconTint));
//        }
//        if (mMoreIconKeyguard != null) {
//            mMoreIconKeyguard.setImageTintList(ColorStateList.valueOf(mIconTint));
//        }
        applyNotificationIconsTint();
    }

    protected int getHeight() {
        return mPhoneStatusBar.getStatusBarHeight();
    }

    protected boolean shouldShowNotification(NotificationData.Entry entry,
            NotificationData notificationData) {
        if (notificationData.isAmbient(entry.key)
                && !NotificationData.showNotificationEvenIfUnprovisioned(entry.notification)) {
            return false;
        }
        if (!PhoneStatusBar.isTopLevelChild(entry)) {
            return false;
        }
//        if (entry.row.getVisibility() == View.GONE) {
//            return false;
//        }

        return true;
    }

    /**
     * Updates the notifications with the given list of notifications to display.
     */
    public static ArrayMap<String, Integer> mPackageNotificationMap = new ArrayMap<>();
    public void updateNotificationIcons(NotificationData notificationData) {
        final LinearLayout.LayoutParams params = generateIconLayoutParams();

        
        ArrayList<NotificationData.Entry> activeNotifications =
                notificationData.getActiveNotifications();
        final int size = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(size);

        mPackageNotificationMap.clear();
        // Filter out ambient notifications and notification children.
        if(NotificationCollapseManage.getDefault(mContext).getMasterNotificationKeyValue()){
        	for (int i = 0; i < size; i++) {
                NotificationData.Entry ent = activeNotifications.get(i);
                if (shouldShowNotification(ent, notificationData)) {
                	/*Bitmap newBp = StatusBarIconView.getPackageThemeBitmapNotification(
                			sbn.getPackageName(), sbn.getUid());
                    if(!NotificationCollapseManage.getDefault(mContext).isUsedSelfNotificationPng(sbn.getPackageName())){
    	         		
    	         		Bitmap bp= StatusBarIconView.getPackageThemeBitmap(mContext, sbn.getPackageName(), 
    	         				UserHandle.getUserId(sbn.getUid()));
    					if (bp != null) {
    						v.setImageBitmap(null);
    						v.setBackground(new BitmapDrawable(bp));
    					}
    				} else {
             			if(v.getDrawable() != null){
             				Bitmap bp = ent.icon;
    	                	if (bp != null){
    	    					Bitmap temp = Utilities.formatIconBitmapTheme(bp,
    	    							mContext, true);
    	    					if (temp != null && !temp.isRecycled()) {
    	    						newBp = temp;
    	    					} else {
    	    						newBp = bp;
    	    					}
    	                	}
             			}
             		}*/
                	String curPkg = ent.notification.getPackageName();
                	if(!NotificationCollapseManage.getDefault(mContext).isUsedSelfNotificationPng(curPkg)){
                		if (mPackageNotificationMap != null) {
            				int num = 0;
            				try {
            					num = mPackageNotificationMap.get(curPkg);
        					} catch (Exception e) {
        					}
            				num++;
        					mPackageNotificationMap.put(curPkg, num);
            				if(num > 1){
            					continue;
            				}
            			}
                	}
        			
        			
                	if (mPhoneStatusBar.isStatusBarIconHide(ent.notification.getPackageName())) {
    					continue;
    				}
                    //toShow.add(ent.icon);
                    if (!mPhoneStatusBar.inKeyguardRestrictedInputMode() || mPhoneStatusBar.isOtherThemeMode() || !ent.showWhenLocked) {
    					toShow.add(ent.icon);
    				}
                }
            }
        }


        ArrayList<View> toRemove = new ArrayList<>();
        for (int i = 0; i < mNotificationIconsStatusbar.getChildCount(); i++) {
            View child = mNotificationIconsStatusbar.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }
        

        final int toRemoveCount = toRemove.size();
        for (int i = 0; i < toRemoveCount; i++) {
            mNotificationIconsStatusbar.removeView(toRemove.get(i));
            removeKeyguardIconView(mNotificationIconsKeyguard, toRemove.get(i));
        }

        
        for (int i = 0; i < toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mNotificationIconsStatusbar.addView(v, i, params);
                addKeyguardIconView(v, mNotificationIconsKeyguard, i, params);
            }
        }
       

        // Re-sort notification icons
        final int childCount = mNotificationIconsStatusbar.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View actual = mNotificationIconsStatusbar.getChildAt(i);
            if(i<toShow.size()){
            	StatusBarIconView expected = toShow.get(i);
                if (actual == expected) {
                    continue;
                }
                mNotificationIconsStatusbar.removeView(expected);
                mNotificationIconsStatusbar.addView(expected, i);
                
                removeKeyguardIconView(mNotificationIconsKeyguard, expected);
                addKeyguardIconView(expected, mNotificationIconsKeyguard, i, params);
            }
        }
        

        applyNotificationIconsTint();
    }

    public void removeKeyguardIconView(IconMerger mIconMerger, View v){
    	StatusBarIconView statusBarIconView = (StatusBarIconView)v;
    	StatusBarIconView statusBarIconViewKeyguard = statusBarIconView.getKeyguardIconView();
    	
    	if(statusBarIconViewKeyguard != null){
    		mIconMerger.removeView(statusBarIconViewKeyguard);
    	}
    }
    
    public void addKeyguardIconView(View v, IconMerger mIconMerger, int i, LinearLayout.LayoutParams params){
    	StatusBarIconView statusBarIconView = (StatusBarIconView)v;
    	StatusBarIconView statusBarIconViewKeyguard = statusBarIconView.getKeyguardIconView();
    	
    	if (statusBarIconViewKeyguard != null && statusBarIconViewKeyguard.getParent() == null) {
    		mIconMerger.addView(statusBarIconViewKeyguard, i, params);
    		Log.d("","addKeyguardIconView count="+i+" pkg="+statusBarIconViewKeyguard.getPackage());
        }
    }
    /**
     * Applies {@link #mIconTint} to the notification icons.
     */
    private void applyNotificationIconsTint() {
    	for (int i = 0; i < mNotificationIconsStatusbar.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mNotificationIconsStatusbar.getChildAt(i);
//            v.updateStatusIconColor(StatusBarIconController.getTint(mTintArea, v, mIconTint));
            if(v.isNotificationReversedColorNeeded()){
            	v.setImageTintList(ColorStateList.valueOf(
                        StatusBarIconController.getTint(mTintArea, v, mIconTint)));
            }
//            boolean isPreL = Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L));
//            boolean colorize = !isPreL || NotificationUtils.isGrayscale(v, mNotificationColorUtil);
//            if (colorize) {
//                v.setImageTintList(ColorStateList.valueOf(
//                        StatusBarIconController.getTint(mTintArea, v, mIconTint)));
//            }
        }
        /*
        
        for (int i = 0; i < mNotificationIconsKeyguard.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mNotificationIconsKeyguard.getChildAt(i);
            boolean isPreL = Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L));
            boolean colorize = !isPreL || NotificationUtils.isGrayscale(v, mNotificationColorUtil);
            if (colorize) {
                v.setImageTintList(ColorStateList.valueOf(
                        StatusBarIconController.getTint(mTintArea, v, mIconTint)));
            }
        }*/
    }
}
