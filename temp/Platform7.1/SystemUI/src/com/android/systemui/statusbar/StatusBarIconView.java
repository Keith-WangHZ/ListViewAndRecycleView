/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Parcelable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.misc.YLUtils;
import com.android.systemui.statusbar.preferences.NotificationCollapseManage;
import com.android.systemui.statusbar.preferences.SysAppProvider;
class StatusBarThemeIconManager{
	static private Map<String,Bitmap> mapPackageBitmap = new HashMap<String,Bitmap>();
	static private Context mContext;
	static PackageManager mPm;
	static private final String TAG = "StatusBarThemeIconManager";
	static public void InitStatusBarThemeIconManager(Context context){
		mContext = context;
		mPm = context.getPackageManager();
	}
	static public void resetStatusBarThemeIcon(){
		mapPackageBitmap.clear();
	}

	static public Bitmap getPackageThemeBitmap(Context context, String pkg, int uid){
		return getPackageThemeBitmap(context, pkg, null, uid);
	}
	static public Bitmap getPackageThemeBitmap(Context context, String pkg, String activityName, int uid){
		Bitmap bitmap = null;
		if (pkg == null){
			return null;
		}
		if (pkg != null) {
			String keyPkg = pkg + "_" + uid;
			if (mapPackageBitmap.containsKey(keyPkg)) {
				bitmap = mapPackageBitmap.get(keyPkg);
			} else {
				try {
					bitmap = YLUtils.getIcon(context, pkg, activityName, uid);
					if (bitmap != null) {
						mapPackageBitmap.put(keyPkg, bitmap);
					}
				} catch (Exception e) {
				}
			}
		}
		if (bitmap != null) {
			return bitmap;
		}
			bitmap = getAppIcon(context, pkg);
		if(bitmap != null){
			return bitmap;
		}
		return bitmap;
	}
	static public Bitmap getAppIcon(Context context, String pkg){
		try{
			if(mPm == null){
				mPm = context.getPackageManager();
			}
			ApplicationInfo info = mPm.getApplicationInfo(pkg,0);			
			BitmapDrawable d = (BitmapDrawable)info.loadIcon(mPm);
			return d.getBitmap();
		}catch(Exception e){
		}
		return null;
	}
    public Drawable getActivityIcon(ActivityInfo info, int userId) {
        if (mPm == null) return null;
//        if (Constants.DebugFlags.App.EnableSystemServicesProxy) {
//            return new ColorDrawable(0xFF666666);
//        }
        Drawable icon = info.loadIcon(mPm);
        return getBadgedIcon(icon, userId);
    }
    public Drawable getBadgedIcon(Drawable icon, int userId) {
        if (userId != UserHandle.myUserId()) {
            icon = mPm.getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
	}
}

public class StatusBarIconView extends AnimatedImageView {
    private static final String TAG = "StatusBarIconView";
    private boolean mAlwaysScaleIcon;

    private StatusBarIcon mIcon;
    @ViewDebug.ExportedProperty private String mSlot;
    @ViewDebug.ExportedProperty private String mPackageName;
    private Drawable mNumberBackground;
    private Paint mNumberPain;
    private int mNumberX;
    private int mNumberY;
    private String mNumberText;
    private Notification mNotification;
    private StatusBarNotification mSbn;
    private boolean bSysPkg = false;
    private StatusBarIconView mKeyguardIconView = null;
    private final boolean mBlocked;
    private int mDensity;
    private int initConstruct = 0;
    private int mSystemIconColor = -3;
    private int mSystemIconColorOld = -2;
    private static String mPackage = null;
    private float mScale = 0f;
    

    public static void resetStatusBarThemeIcon(){
    	StatusBarThemeIconManager.resetStatusBarThemeIcon();
    }
    public static  Bitmap getPackageThemeBitmap(Context context, String pkg){
    	return StatusBarThemeIconManager.getPackageThemeBitmap(context, pkg, -1);
    }
    public static  Bitmap getPackageThemeBitmap(Context context, String pkg, int uid){
    	return StatusBarThemeIconManager.getPackageThemeBitmap(context, pkg, uid);
    }
    public static  Bitmap getPackageThemeBitmap(Context context, String pkg, String activityName){
    	return StatusBarThemeIconManager.getPackageThemeBitmap(context, pkg, activityName, -1);
    }
    public static  Bitmap getPackageThemeBitmap(Context context, String pkg, String activityName, int uid){
    	return StatusBarThemeIconManager.getPackageThemeBitmap(context, pkg, activityName, uid);
    }
    
    public static Bitmap getPackageThemeBitmapNotification(Context context, final String packageName, int userId, Bitmap icon){
    	Bitmap newBp = null;
        if(!NotificationCollapseManage.getDefault(context).isUsedSelfNotificationPng(packageName)){
     		Bitmap bp= StatusBarIconView.getPackageThemeBitmap(context, packageName, 
     				UserHandle.getUserId(userId));
     		newBp = bp;
     		
		} else {
			Bitmap bp = icon;
        	if (bp != null){
				Bitmap temp = Utilities.formatIconBitmapTheme(bp,
						context, true);
				if (temp != null && !temp.isRecycled()) {
					newBp = temp;
				} else {
					newBp = bp;
				}
        	}
 		}
        return newBp;
    }
    public boolean isSysPackage(){
    	return bSysPkg;
    }
    
    public StatusBarIconView(Context context, StatusBarNotification sbn, String pkg, String slot, Notification notification) {
        this(context, sbn, pkg, slot, notification, false, true);
    }
    
    public StatusBarIconView(Context context, StatusBarNotification sbn, String pkg, String slot, Notification notification, boolean blocked) {
        this(context, sbn, pkg, slot, notification, blocked, true);
    }

    public StatusBarIconView(Context context, StatusBarNotification sbn, String pkg, String slot, Notification notification,
            boolean blocked, Boolean isCreateKeyguardIconView) {
        super(context);
        mPackage = pkg;
        mBlocked = blocked;
        mSlot = slot;
        mSbn = sbn;
        mNumberPain = new Paint();
        mNumberPain.setTextAlign(Paint.Align.CENTER);
        mNumberPain.setColor(context.getColor(R.drawable.notification_number_text_color));
        mNumberPain.setAntiAlias(true);
        setNotification(notification);
        maybeUpdateIconScale();
        setScaleType(ScaleType.CENTER_INSIDE);
        
        // We do not resize and scale system icons (on the right), only notification icons (on the left).
        // We have three choices for notification icons:
        //0. fillXY
        //1. if edge is transparent, scale not. 
        //2. if edge is untransparent, scale it.
        if(notification != null){
    		int pad = Utilities.dipToPixel(context, 4);
            setPadding(pad, pad, pad, pad);
    	}
        
        mDensity = context.getResources().getDisplayMetrics().densityDpi;
        
        if(isCreateKeyguardIconView){
        	mKeyguardIconView = new StatusBarIconView(context, sbn, pkg, slot, notification, blocked, false);
        	mKeyguardIconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        mScale = context.getResources().getDisplayMetrics().density;
//        Log.d(TAG,"40000411 sca="+mScale);
    }
    
	public void setIconViewColor(ImageView icon, String pkg) {
		/*if (icon == null) {
			return;
		}
		mSystemIconColorOld = mSystemIconColor;
		if (mIsRightIcon || (!mIsRightIcon && (pkg == null
				|| NotificationCollapseManage.getDefault(mContext)
						.isSystemNotification(pkg)))) {
			icon.clearColorFilter();
			icon.setColorFilter(mSystemIconColor, PorterDuff.Mode.SRC_IN);
		} else {
			icon.setAlpha(0.8f);
		}
		invalidate();*/
	}
	
	public boolean isNotificationReversedColorNeeded(){
		if(mIcon == null){
			return true;
		}
		String pkg = mIcon.pkg;
//		Log.d("","NotificationIcon pkg="+pkg);
		boolean b = ((pkg == null
				|| NotificationCollapseManage.getDefault(mContext)
				.isSystemNotification(pkg)));
		return b;
	}
    
    public void updateStatusIconColor(int color) {
    	mSystemIconColor = color;
    	setIconViewColor(this, mPackage);
	}

    private void maybeUpdateIconScale() {
        // We do not resize and scale system icons (on the right), only notification icons (on the
        // left).
        if (mNotification != null || mAlwaysScaleIcon) {
            updateIconScale();
        }
    }

    private void updateIconScale() {
        Resources res = mContext.getResources();
        final int outerBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        final int imageBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        float scale = (float)imageBounds / (float)outerBounds;
        
//        if("com.yulong.android.calendar".equalsIgnoreCase(mPackage)){
//        	scale = 1.1f;
//        	setScaleX(scale);
//            setScaleY(scale);
//        }
//        else if (!SysAppProvider.getInstance(null).isIconNotScale(mPackage))
//        {
//            setScaleX(scale);
//            setScaleY(scale);
//        }

    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int density = newConfig.densityDpi;
        if (density != mDensity) {
            mDensity = density;
            maybeUpdateIconScale();
            updateDrawable();
        }
    }

    public void setNotification(Notification notification) {
        mNotification = notification;
        setContentDescription(notification);
    }

    public StatusBarIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBlocked = false;
        mAlwaysScaleIcon = true;
        updateIconScale();
        mDensity = context.getResources().getDisplayMetrics().densityDpi;
    }

    private static boolean streq(String a, String b) {
        if (a == b) {
            return true;
        }
        if (a == null && b != null) {
            return false;
        }
        if (a != null && b == null) {
            return false;
        }
        return a.equals(b);
    }

    public boolean equalIcons(Icon a, Icon b) {
        if (a == b) return true;
        if (a.getType() != b.getType()) return false;
        switch (a.getType()) {
            case Icon.TYPE_RESOURCE:
                return a.getResPackage().equals(b.getResPackage()) && a.getResId() == b.getResId();
            case Icon.TYPE_URI:
                return a.getUriString().equals(b.getUriString());
            default:
                return false;
        }
    }
    /**
     * Returns whether the set succeeded.
     */
    public boolean mIsRightIcon = false;
    public void setRightIcon(boolean isRightIcon){
    	mIsRightIcon = isRightIcon;
    }
    public boolean mIsNotificationIcon = false;
    public void setNotificationIcon(boolean isNotificationIcon){
    	mIsNotificationIcon = isNotificationIcon;
    	
    }
    public boolean set(StatusBarIcon icon) {
        final boolean iconEquals = mIcon != null && equalIcons(mIcon.icon, icon.icon);
        final boolean levelEquals = iconEquals
                && mIcon.iconLevel == icon.iconLevel;
        final boolean visibilityEquals = mIcon != null
                && mIcon.visible == icon.visible;
        final boolean numberEquals = mIcon != null
                && mIcon.number == icon.number;
        mIcon = icon.clone();
        
        
        setContentDescription(icon.contentDescription);
        if (!iconEquals) {
            if (!updateDrawable(false /* no clear */)) return false;
        }
        if (!levelEquals) {
            setImageLevel(icon.iconLevel);
        }

        if (!numberEquals) {
            if (icon.number > 0 && getContext().getResources().getBoolean(
                        R.bool.config_statusBarShowNumber)) {
                if (mNumberBackground == null) {
                    mNumberBackground = getContext().getResources().getDrawable(
                            R.drawable.ic_notification_overlay);
                }
                placeNumber();
            } else {
                mNumberBackground = null;
                mNumberText = null;
            }
            invalidate();
        }
        if(mKeyguardIconView != null){
        	mKeyguardIconView.set(icon);
        }
        if (!visibilityEquals) {
            setVisibility(icon.visible && !mBlocked ? VISIBLE : GONE);
        }
        post(new Runnable() {
            public void run() {
            	setIconViewColor(StatusBarIconView.this, mPackage);
            }
        });
        //setVisibility(VISIBLE);
        return true;
    }

    public void updateDrawable() {
        updateDrawable(true /* with clear */);
    }

    private boolean updateDrawable(boolean withClear) {
        if (mIcon == null) {
            return false;
        }
        Drawable drawable = getIcon(mIcon);
        if (drawable == null) {
            Log.w(TAG, "No icon for slot " + mSlot);
            return false;
        }
        /*if (withClear) {
            setImageDrawable(null);
            return true;
        }*/
        setImageDrawable(drawable);
        post(new Runnable() {
            public void run() {
            	setIconViewColor(StatusBarIconView.this, mPackage);
            }
        });
        /*if(!mIsNotificationIcon){
        	setImageDrawable(drawable);
        	return true;
        }
        else if(mSbn != null && !NotificationCollapseManage.getDefault(mContext).isUsedSelfNotificationPng(mSbn.getPackageName())){
        	setImageDrawable(drawable);
        	return true;
        }
        else{
        	Drawable newIcon = drawable;
        	if(newIcon != null){
        		Bitmap bp = Utilities.drawableToBitamp(newIcon);
        		int w = bp.getWidth();
        		int h = bp.getHeight();
        		int p1=0,p2=0,p3=0,p4=0;
        		try {
        			p1 = bp.getPixel(0,0);
            		p2 = bp.getPixel(w-1,0);
            		p3 = bp.getPixel(0,h-1);
            		p4 = bp.getPixel(w-1,h-1);
    			} catch (Exception e) {
    			}
        		int a1 = (p1 & 0xff000000) >> 24;
        		int a2 = (p2 & 0xff000000) >> 24;
        		int a3 = (p3 & 0xff000000) >> 24;
        		int a4 = (p4 & 0xff000000) >> 24;
        		//Log.d("","updateDrawable2 "+a1+" "+a2+" "+a3+" "+a4+" w="+w+" h="+h);
        		float av = Utilities.pixelToDip(mContext, (w+h))/2.0f;
        		float s1 = 1.0f,s2=1.2f;
        		if(av > 0.1f){
        			s1 = (float)(20.0/24.0);
        			if(0 == Float.compare(mScale, 3.0f)){
        				s2 = (float)(26.0/24.0);
        			}else{
        				s2 = (float)(20.0/24.0);
        			}
        		}
            	if (bp != null){
    				Bitmap bm = Utilities.formatIconBitmapTheme(bp,
    						mContext, true);
    				Bitmap bm1 = bm;
    	            if(bm1 != null){
    	            	newIcon =  Utilities.bitmapToDrawable(bm1);
    	            }
    	            if(bm != null && bm.isRecycled()){
    	            	bm.recycle();
    	            }
    	            if(a1!=0 || a2!=0 || a3!=0 || a4!=0){//wifi
    	            	setScaleX(s1);
        	            setScaleY(s1);
    	            }else{//androidRobot-usb
    	            	setScaleX(s2);
        	            setScaleY(s2);
    	            }
    	            
    	            setImageDrawable(newIcon);
            	}
        	}
        }*/
    		
       
        
        return true;
    }

    private Drawable getIcon(StatusBarIcon icon) {
        return getIcon(getContext(), icon);
    }

    /**
     * Returns the right icon to use for this item
     *
     * @param context Context to use to get resources
     * @return Drawable for this item, or null if the package or item could not
     *         be found
     */
    public static Drawable getIcon(Context context, StatusBarIcon statusBarIcon) {
        int userId = statusBarIcon.user.getIdentifier();
        if (userId == UserHandle.USER_ALL) {
            userId = UserHandle.USER_SYSTEM;
        }

        Drawable icon = statusBarIcon.icon.loadDrawableAsUser(context, userId);

        Drawable newIcon = icon;
//        Bitmap bm = getPackageThemeBitmapNotification(context, mPackage, userId, Utilities.drawableToBitamp(icon));
//        newIcon = Utilities.bitmapToDrawable(bm);
        
        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float scaleFactor = typedValue.getFloat();

        // No need to scale the icon, so return it as is.
        if (scaleFactor == 1.f) {
            return newIcon;
        }

        return new ScalingDrawableWrapper(newIcon, scaleFactor);
    }

    public StatusBarIcon getStatusBarIcon() {
        return mIcon;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (mNotification != null) {
            event.setParcelableData(mNotification);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mNumberBackground != null) {
            placeNumber();
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateDrawable();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mNumberBackground != null) {
            mNumberBackground.draw(canvas);
            canvas.drawText(mNumberText, mNumberX, mNumberY, mNumberPain);
        }
        //updateDrawable();
    }

    @Override
    protected void debug(int depth) {
        super.debug(depth);
        Log.d("View", debugIndent(depth) + "slot=" + mSlot);
        Log.d("View", debugIndent(depth) + "icon=" + mIcon);
    }

    void placeNumber() {
        final String str;
        final int tooBig = getContext().getResources().getInteger(
                android.R.integer.status_bar_notification_info_maxnum);
        if (mIcon.number > tooBig) {
            str = getContext().getResources().getString(
                        android.R.string.status_bar_notification_info_overflow);
        } else {
            NumberFormat f = NumberFormat.getIntegerInstance();
            str = f.format(mIcon.number);
        }
        mNumberText = str;

        final int w = getWidth();
        final int h = getHeight();
        final Rect r = new Rect();
        mNumberPain.getTextBounds(str, 0, str.length(), r);
        final int tw = r.right - r.left;
        final int th = r.bottom - r.top;
        mNumberBackground.getPadding(r);
        int dw = r.left + tw + r.right;
        if (dw < mNumberBackground.getMinimumWidth()) {
            dw = mNumberBackground.getMinimumWidth();
        }
        mNumberX = w-r.right-((dw-r.right-r.left)/2);
        int dh = r.top + th + r.bottom;
        if (dh < mNumberBackground.getMinimumWidth()) {
            dh = mNumberBackground.getMinimumWidth();
        }
        mNumberY = h-r.bottom-((dh-r.top-th-r.bottom)/2);
        mNumberBackground.setBounds(w-dw, h-dh, w, h);
    }

    private void setContentDescription(Notification notification) {
        if (notification != null) {
            String d = contentDescForNotification(mContext, notification);
            if (!TextUtils.isEmpty(d)) {
                setContentDescription(d);
            }
        }
    }

    public String toString() {
        return "StatusBarIconView(slot=" + mSlot + " icon=" + mIcon
            + " notification=" + mNotification + ")";
    }

    public String getSlot() {
        return mSlot;
    }
    public String getPackage() {
        return mPackage;
    }


    public static String contentDescForNotification(Context c, Notification n) {
        String appName = "";
        try {
            Notification.Builder builder = Notification.Builder.recoverBuilder(c, n);
            appName = builder.loadHeaderAppName();
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to recover builder", e);
            // Trying to get the app name from the app info instead.
            Parcelable appInfo = n.extras.getParcelable(
                    Notification.EXTRA_BUILDER_APPLICATION_INFO);
            if (appInfo instanceof ApplicationInfo) {
                appName = String.valueOf(((ApplicationInfo) appInfo).loadLabel(
                        c.getPackageManager()));
            }
        }

        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence ticker = n.tickerText;

        CharSequence desc = !TextUtils.isEmpty(ticker) ? ticker
                : !TextUtils.isEmpty(title) ? title : "";

        return c.getString(R.string.accessibility_desc_notification_icon, appName, desc);
    }

    public StatusBarIconView getKeyguardIconView(){
    	return mKeyguardIconView;
    }
}
