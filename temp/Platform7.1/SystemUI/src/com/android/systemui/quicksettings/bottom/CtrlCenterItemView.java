package com.android.systemui.quicksettings.bottom;

import com.android.systemui.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CtrlCenterItemView extends RelativeLayout {
    private static final String TAG = CtrlCenterItemView.class.getSimpleName();
    public Model model;
    private RelativeLayout mThisView;
    private LinearLayout mMain;
    private ImageView mIcon;
    private TextView mTitle;
    private Resources mRes;
    private int mThisWidth, mThisHeidht;

    public CtrlCenterItemView(Context context) {
        super(context);
        model = new Model();
        mRes = getResources();
        mThisWidth = mRes.getDimensionPixelSize(R.dimen.item_width);
        mThisHeidht = mRes.getDimensionPixelSize(R.dimen.item_height);

        setLayoutParams(new LayoutParams(mThisWidth, mThisHeidht));

        mThisView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.ctrlcenter_item_view, this);
        mMain = (LinearLayout) mThisView.findViewById(R.id.main);
        mIcon = (ImageView) mMain.findViewById(R.id.icon);
        mTitle = (TextView) mMain.findViewById(R.id.title);
        //setBackgroundColor(Color.argb(0x99, 0xff, 0, 0));
    }

    public static class Model {
        int id = -1;
        int pos = -1;
        int value = -1;
        int iconId = -1;
        int visibable;
        String title;
        Bitmap icon;
        Point p = new Point();
        CtrlCenterItemView v;

        public Model() {
        }

        public Model(String t, int b) {
            title = t;
            iconId = b;
        }

        public Model(String t, int i, int p) {
            this(t, i);
            pos = p;
        }

        public String toString() {
            return new StringBuilder("pos:").append(pos).append(";\tid:").append(id).append(";\ttitle:").append(title)
                    .toString();
        }
    }

    public String getTitle() {
        return model.title;
    }

    public CtrlCenterItemView setVisiable(int visibable) {
        model.visibable = visibable;
        mMain.setVisibility(model.visibable);
        return this;
    }

    public CtrlCenterItemView setModel(Model m) {
        model = m;
        model.v= this;
        mTitle.setText(model.title);
        mIcon.setImageBitmap(BitmapFactory.decodeResource(mRes, model.iconId));
        return this;
    }

    public CtrlCenterItemView setPoint(int x, int y) {
        model.p.x = x;
        model.p.y = y;
        return this;
    }

}
