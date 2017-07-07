package com.android.systemui.statusbar.preferences;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.recents.misc.Utilities;
public class YLPreferenceActivity extends Activity {

    private TextView mTitle;
    private ImageView mBack;
    private LinearLayout mRightButtonlayout;
    private ImageView mRightButtonIcon;
    private TextView mRightButtonText;
    private int mStyle = 0;

    @Override
    protected void onCreate(Bundle arg0) {
        // TODO Auto-generated method stub
        super.onCreate(arg0);
        setStatusBarOverlayingActivity(YLPreferenceActivity.this);
        mStyle = Utilities.getCoolpadThemeStyle();
        if(mStyle == 1){
        }else{
        	this.getWindow().setBackgroundDrawableResource(R.drawable.background_holo_light_young);
        }
        //----------璁剧疆actionBar鏍峰紡---------------
        initActionBar();
        //----------璁剧疆actionBar鏍峰紡---------------
    }

    public static void setStatusBarOverlayingActivity(Activity p_Activity){
        //鎵樼洏閲嶅彔鏄剧ず鍦ˋctivity涓�
//        View decorView = p_Activity.getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                Vi;
//        decorView.setSystemUiVisibility(uiOptions);
        /*if(VERSION.SDK_INT >= VERSION_CODES.KITKAT){
            //璁剧疆鎵樼洏閫忔槑
            p_Activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            Log.d("test","VERSION.SDK_INT =" + VERSION.SDK_INT);
        }else{
            //setupSystemUI();
            Log.d("test", "SDK 灏忎簬19涓嶈缃姸鎬佹爮閫忔槑鏁堟灉");
        }*/
    	p_Activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
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
                } else {
            	viewActionBar = getLayoutInflater().inflate(R.layout.yl_actionbar_young, null);
            }
            if (viewActionBar != null) {
                LinearLayout layoutStatusBar = (LinearLayout)viewActionBar.findViewById(R.id.status_bar_layout);
                layoutStatusBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getStatusBarHeight(YLPreferenceActivity.this)));
                mTitle = (TextView)viewActionBar.findViewById(R.id.topbar_title);
                mBack = (ImageView)viewActionBar.findViewById(R.id.topbar_back);
                
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

    public void setActionBarBackButtonVisibility(boolean p_bVisible){
        if(p_bVisible){
            mBack.setVisibility(View.VISIBLE);
            //mDivider.setVisibility(View.VISIBLE);
        }else{
            mBack.setVisibility(View.GONE);
            //mDivider.setVisibility(View.GONE);
        }
    }
    
    public void setActionBarTitle(String p_strTitle){
        mTitle.setText(p_strTitle);
    }
    
    public String getActionBarTitle(){
        return mTitle.getText().toString();
    }
}
