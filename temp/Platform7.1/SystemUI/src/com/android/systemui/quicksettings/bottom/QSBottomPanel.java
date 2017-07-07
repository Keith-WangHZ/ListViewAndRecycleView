package com.android.systemui.quicksettings.bottom;

import java.util.TreeMap;

import com.android.systemui.IvviGaussBlurViewFeature;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailClipper;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QuickQSPanel;
import com.android.systemui.quicksettings.bottom.BottomIconEditActivity.CheckablePackageInfo;
import com.android.systemui.quicksettings.bottom.QuickSettingsPannelView;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import android.content.Context;
import android.content.res.Configuration;
//import android.graphics.BlurParams;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QSBottomPanel extends QSPanel{
	private final Context mContext;
	PhoneStatusBar mStatusBar;
	private int pOrientation = Configuration.ORIENTATION_PORTRAIT;
	private int mOrientation = 1;
	
	//===modify by ty begin
	//===modify by ty end

	
	public QSBottomPanel(Context context, AttributeSet attrs, int Oriention, PhoneStatusBar statusBar) {
		super(context, attrs);
		mStatusBar = statusBar;
		mContext = context;
		mOrientation = Oriention;
		BottomFlag = true;
		removeAllViews();
		
		if(!Utilities.showFullGaussBlurForDDQS()){
//			setBlurRadiusDp(12f);
//			setBlurChromaContrast(1.5f);
//			setBlurAlpha(0.8f);
//			setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//			setBackground(null);
			
			IvviGaussBlurViewFeature.setBlurRadiusDp(this, 12f);
			IvviGaussBlurViewFeature.setBlurChromaContrast(this, 1.5f);
			IvviGaussBlurViewFeature.setBlurAlpha(this, 0.8f);
			IvviGaussBlurViewFeature.setBlurMode(this, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
			IvviGaussBlurViewFeature.setBackground(this, null);
		}
		
		mDetail = new QuickSettingDetailView(context, mOrientation);
//		if (mOrientation == pOrientation) {
//			mDetail = LayoutInflater.from(context).inflate(R.layout.qs_detail_bottom, this, false);
//		} else {
//			mDetail = LayoutInflater.from(context).inflate(R.layout.qs_detail_bottom_land, this, false);
//		}

		updateDetailText();
		
		if(!Utilities.showFullGaussBlurForDDQS()){
//			mDetail.setBlurRadiusDp(12f);
//			mDetail.setBlurChromaContrast(1.5f);
//			mDetail.setBlurAlpha(0.8f);
//			mDetail.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//			mDetail.setBackground(null);
			
			IvviGaussBlurViewFeature.setBlurRadiusDp(mDetail, 12f);
			IvviGaussBlurViewFeature.setBlurChromaContrast(mDetail, 1.5f);
			IvviGaussBlurViewFeature.setBlurAlpha(mDetail, 0.8f);
			IvviGaussBlurViewFeature.setBlurMode(mDetail, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
			IvviGaussBlurViewFeature.setBackground(mDetail, null);
		}

		mClipper = new QSDetailClipper(mDetail);
		addView(mDetail);
		View qsDetail = ((QuickSettingDetailView)mDetail).getDetailView();
		if(qsDetail != null){
			mDetailTitleText = (TextView) qsDetail.findViewById(R.id.textView1);
			mDetailContent = (ViewGroup) qsDetail.findViewById(android.R.id.content);
			mDetailTitleIcon = (ImageView) qsDetail.findViewById(R.id.downIcon);
			mDetailDoneButton = (TextView) qsDetail.findViewById(android.R.id.button1);
			mDetailSettingsButton = (TextView) qsDetail.findViewById(android.R.id.button2);
		}

		mQSRoot = new QuickSettingsPannelView(mContext, mStatusBar, Oriention, this);
		mQSRoot.setVisibility(View.VISIBLE);
		mQSRoot.setClickable(true);
		addView(mQSRoot);
		mQSRoot.bringToFront();
		// daixianglong
		mMobilePanelView = new QuickSettingMobilePanelView(mContext, Oriention);
		mMobilePanelView.setQSPanel(this);
		mMobilePanelView.setVisibility(View.GONE);
		mMobilePanelView.setClickable(true);

		mMobilePanelView.setBackgroundResource(R.drawable.qs_detail_background);
		mDataClipper = new QSDetailClipper(mMobilePanelView);
		
		if(!Utilities.showFullGaussBlurForDDQS()){
//			mMobilePanelView.setBlurRadiusDp(12f);
//			mMobilePanelView.setBlurChromaContrast(1.5f);
//			mMobilePanelView.setBlurAlpha(0.8f);
//			mMobilePanelView.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//			mMobilePanelView.setBackground(null);
			
			IvviGaussBlurViewFeature.setBlurRadiusDp(mMobilePanelView, 12f);
			IvviGaussBlurViewFeature.setBlurChromaContrast(mMobilePanelView, 1.5f);
			IvviGaussBlurViewFeature.setBlurAlpha(mMobilePanelView, 0.8f);
			IvviGaussBlurViewFeature.setBlurMode(mMobilePanelView, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
			IvviGaussBlurViewFeature.setBackground(mMobilePanelView, null);
		}
		addView(mMobilePanelView);
		/*
		 * mDetailDoneButton.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { closeDetail();
		 * mQSRoot.setVisibility(View.VISIBLE); } });
		 * 
		 * mDetailSettingsButton.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { closeDetail();
		 * mQSRoot.setVisibility(View.VISIBLE); } });
		 * 
		 * mDetailTitleIcon.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View arg0) { closeDetail();
		 * mQSRoot.setVisibility(View.VISIBLE); } });
		 */
		// bringChildToFront(mQSRoot);
	}



	private void updateDetailText() {
		if(mDetailDoneButton == null){
			View qsDetail = ((QuickSettingDetailView)mDetail).getDetailView();
			if(qsDetail != null){
				mDetailTitleText = (TextView) qsDetail.findViewById(R.id.textView1);
				mDetailContent = (ViewGroup) qsDetail.findViewById(android.R.id.content);
				mDetailTitleIcon = (ImageView) qsDetail.findViewById(R.id.downIcon);
				mDetailDoneButton = (TextView) qsDetail.findViewById(android.R.id.button1);
				mDetailSettingsButton = (TextView) qsDetail.findViewById(android.R.id.button2);
			}
			if(mDetailDoneButton == null){
				return;
			}
		}
		mDetailDoneButton.setText(R.string.quick_settings_done);
		mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
		mDetail.setVisibility(GONE);
		mDetailTitleText.setVisibility(GONE);
		mDetail.setClickable(true);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		updateDetailText();
	}

	protected OnQuickSettingsPannelViewListener mOnQuickSettingsPannelView;

	public void setOnQuickSettingsPannelViewListener(OnQuickSettingsPannelViewListener lstn) {
		mOnQuickSettingsPannelView = lstn;
	}

	public interface OnQuickSettingsPannelViewListener {
		void setVisibility(boolean isShow);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		getMeasuredWidth();

		int mPainterPosX = l;
		int mPainterPosY = t;

		int childCount = getChildCount();

		for (int i = 0; i < childCount; i++) {

			View childView = getChildAt(i);

			int width = childView.getMeasuredWidth();
			int height = childView.getMeasuredHeight();

			childView.layout(mPainterPosX, mPainterPosY, mPainterPosX + width, mPainterPosY + height);

			mPainterPosX += width;
		}
		final int dh = Math.min(mDetail.getMeasuredHeight(), getMeasuredHeight());
		mDetail.layout(0, 0, mDetail.getMeasuredWidth(), dh);
		final int dhMobile = Math.min(mMobilePanelView.getMeasuredHeight(), getMeasuredHeight());
		mMobilePanelView.layout(0, 0, mMobilePanelView.getMeasuredWidth(), dhMobile);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measureWidth = measureWidth(widthMeasureSpec);
		int measureHeight = measureHeight(heightMeasureSpec);
		measureChildren(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(measureWidth, measureHeight);

	}

	private int measureWidth(int pWidthMeasureSpec) {
		int result = 0;
		MeasureSpec.getMode(pWidthMeasureSpec);
		int widthSize = MeasureSpec.getSize(pWidthMeasureSpec);

		result = widthSize;
		return result;
	}

	private int measureHeight(int pHeightMeasureSpec) {
		int result = 0;

		int heightMode = MeasureSpec.getMode(pHeightMeasureSpec);
		int heightSize = MeasureSpec.getSize(pHeightMeasureSpec);

		switch (heightMode) {
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			result = heightSize;
			break;
		}
		return result;
	}

	public void setBottomPanelVisible(boolean isShow) {
		mOnQuickSettingsPannelView.setVisibility(isShow);
	}

	public void resetPagers() {
		mQSRoot.resetPagers();
	}

	public void setVisible(boolean visible) {
		if (visible) {
			mQSRoot.setVisibility(View.VISIBLE);
		} else {
			mQSRoot.setVisibility(View.INVISIBLE);
		}
	}

	public void onDestroy() {
		mQSRoot.onDestroy();
		mMobilePanelView.onDestroy();
		mOnQuickSettingsPannelView = null;
		removeAllViews();
	}

	public QuickSettingsPannelView getQSPannelView() {
		return mQSRoot;
	}

	public QuickSettingMobilePanelView getMobilePanelView() {
		return mMobilePanelView;
	}

	public TextView getDetailTitleText() {
		return mDetailTitleText;
	}

	public ViewGroup getDetailContent() {
		return mDetailContent;
	}

	public ImageView getDetailTitleIcon() {
		return mDetailTitleIcon;
	}

	public TextView getDetailDoneButton() {
		return mDetailDoneButton;
	}

	public TextView getDetailSettingsButton() {
		return mDetailSettingsButton;
	}

	public View getDetail() {
		return mDetail;
	}

	public QSDetailClipper getClipper() {
		return mClipper;
	}

	public QSDetailClipper getDataClipper() {
		return mDataClipper;
	}

	public void resetFlashLight() {
		mQSRoot.setFlashLightState();
	}

	public int getOrientation() {
		return mOrientation;
	}

	public void onBottomIconUpdate(TreeMap<Integer, CheckablePackageInfo> treeMap) {
		mQSRoot.onBottomIconUpdate(treeMap);
	}



//	public void setMobilePanelViewVisible(boolean b) {//===modify by ty
		// TODO Auto-generated method stub
		
//	}

}
