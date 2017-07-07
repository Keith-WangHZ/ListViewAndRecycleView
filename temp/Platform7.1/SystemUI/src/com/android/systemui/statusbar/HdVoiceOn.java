package com.android.systemui.statusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.policy.CurrentUserTracker;

public class HdVoiceOn extends LinearLayout {
	private final static String TAG = "HdVoiceOn";
    private ImageView mHdVoiceOn;
    private static final String SHOW_HDVOICE = "hd_voice_on";//Settings.Global.AIRPLANE_MODE_ON;//
    private CHdVoiceObserver mCHdVoiceObserver;

    public HdVoiceOn(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public HdVoiceOn(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        Handler handler = new Handler();
        mHdVoiceOn = (ImageView) findViewById(R.id.hd_voice_on);
        mCHdVoiceObserver = new CHdVoiceObserver(handler);
		mCHdVoiceObserver.startObserving();
    }
    public void setHdVoiceViewColor(int color) {
	}
    
    private final class CHdVoiceObserver extends ContentObserver{
		public CHdVoiceObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			onHdVoiceChange();
			super.onChange(selfChange);
		}
		
		public void startObserving() {
			mContext.getContentResolver().registerContentObserver(Settings.Global
                    .getUriFor(SHOW_HDVOICE), false,
                    this, UserHandle.USER_ALL);
			onHdVoiceChange();
		}
	}
    
   

    /**
     *  The value of SHOW_HDVOICE://==1;//
     *  0----switch close
     *  1----switch open, default state, nv support not
     *  2----switch open, nv support, hd support
     */
    private void onHdVoiceChange(){//ImsManager.ACTION_IMS_STATE_CHANGED for MTK
    	boolean show = Settings.Global.getInt(mContext.getContentResolver(), SHOW_HDVOICE, ActivityManager.getCurrentUser()) == 2;
		LogHelper.sd(TAG, "onHdVoiceChange show = " + show);
		if(show){
			mHdVoiceOn.setVisibility(View.VISIBLE);
		}else{
			mHdVoiceOn.setVisibility(View.GONE);
		}
	}
}
