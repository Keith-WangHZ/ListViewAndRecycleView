package com.android.systemui.quicksettings.bottom;

import com.android.systemui.R;
import com.android.systemui.recents.misc.Utilities;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class QsBottomPanelGuideActivity extends Activity {

    public static final String TAG = QsBottomPanelGuideActivity.class.getSimpleName();
    private Button mBtn;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.qs_bottompanel_guide);
        mBtn = (Button) findViewById(R.id.qs_bottom_first_btn);
        mImageView = (ImageView)findViewById(R.id.imageId);
        if (Utilities.needFakeNavigationBarView() || getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar)) {
            Log.v(TAG, "navigation key");
            mImageView.setBackgroundResource(R.drawable.qs_bottompanel_first_frame);
        }else{
            Log.v(TAG, "physical key");
            mImageView.setBackgroundResource(R.drawable.qs_bottompanel_first_frame_phy);
        }
        Log.v(TAG, "before persist.yulong.bottompanel = " + SystemProperties.getInt("persist.yulong.bottompanel", 0));
        if (0 == SystemProperties.getInt("persist.yulong.bottompanel", 0)) {
            Log.v(TAG, "0 == persist.yulong.bottompanel");
           try{
                SystemProperties.set("persist.yulong.bottompanel", "1");
            }catch(Exception e){
            	Log.v(TAG, "Exception  SystemProperties.set ");
            }
        }
        Log.v(TAG, "after persist.yulong.bottompanel = " + SystemProperties.getInt("persist.yulong.bottompanel", 0));
        mBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
    }
}