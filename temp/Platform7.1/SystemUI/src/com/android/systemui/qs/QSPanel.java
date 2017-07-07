/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs;

import android.animation.Animator.AnimatorListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.IvviGaussBlurViewFeature;
import com.android.systemui.R;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.QSTile.Host.Callback;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.quicksettings.bottom.QSBottomPanel;
import com.android.systemui.quicksettings.bottom.QuickSettingLauncher;
import com.android.systemui.quicksettings.bottom.QuickSettingMobilePanelView;
import com.android.systemui.quicksettings.bottom.QuickSettingsPannelView;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.phone.QuickSettingsData;
import com.android.systemui.statusbar.phone.QuickSettingsItemView;
import com.android.systemui.statusbar.phone.YulongQuickSettingsContain;
import com.android.systemui.statusbar.phone.DataControler.DatabaseRecord;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.securespaces.android.ssm.UserUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** View that represents the quick settings tile panel. **/
public class QSPanel extends LinearLayout implements Tunable, Callback {

	public static final String QS_SHOW_BRIGHTNESS = "qs_show_brightness";

	protected static final String TAG = "QSPanel";

	protected final Context mContext;
	protected final ArrayList<TileRecord> mRecords = new ArrayList<TileRecord>();
	protected final View mBrightnessView;
	private final H mHandler = new H();

	private int mPanelPaddingBottom;
	private int mBrightnessPaddingTop;
    protected boolean mExpanded;
	protected boolean mListening;

	private Callback mCallback;
	private BrightnessController mBrightnessController;
	protected QSTileHost mHost;

	protected QSFooter mFooter;
	private boolean mGridContentVisible = true;

	protected QSTileLayout mTileLayout;

	private QSCustomizer mCustomizePanel;
	private Record mDetailRecord;
	private boolean mTriggeredExpand;
	
	protected View mDetail;
    private BrightnessMirrorController mBrightnessMirrorController;
  	protected ViewGroup mDetailContent;
  	protected TextView mDetailSettingsButton;
  	protected TextView mDetailDoneButton;
  	protected TextView mDetailTitleText;
  	protected ImageView mDetailTitleIcon;
  	protected boolean BottomFlag;
  	public QuickSettingsPannelView mQSRoot;
  	protected QuickSettingMobilePanelView mMobilePanelView;
  	protected QSDetailClipper mClipper;
  	protected QSDetailClipper mDataClipper;
  	
  	private boolean bool = false;
  	public final static boolean mBottomPanel = true;

	public QSPanel(Context context) {
		this(context, null);
	}

	public QSPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		BottomFlag = false;

		setOrientation(VERTICAL);

		mBrightnessView = LayoutInflater.from(context).inflate(R.layout.quick_settings_brightness_dialog, this, false);
		addView(mBrightnessView);
		mBrightnessView.setVisibility(View.GONE);

		setupTileLayout();
		
		if (mBottomPanel && !isRegisteredBottomPanelReceiver) {
			registerBottomPanelBrocastReceiver();
		}

		mFooter = new QSFooter(this, context);
		addView(mFooter.getView());

		updateResources();

		mBrightnessController = new BrightnessController(getContext(), null,// (ImageView)
				// findViewById(R.id.brightness_icon),
                (ToggleSlider) findViewById(R.id.brightness_slider), (CompoundButton) findViewById(R.id.brightAuto));

	}

	protected void setupTileLayout() {
		mTileLayout = (QSTileLayout) LayoutInflater.from(mContext).inflate(R.layout.qs_paged_tile_layout, this, false);
		mTileLayout.setListening(mListening);
		addView((View) mTileLayout);
		((View) mTileLayout).setVisibility(View.GONE);
		findViewById(android.R.id.edit).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View view) {
				mHost.startRunnableDismissingKeyguard(new Runnable() {
					@Override
					public void run() {
						showEdit(view);
					}
				});
			}
		});
	}

	public boolean isShowingCustomize() {
		return mCustomizePanel != null && mCustomizePanel.isCustomizing();
	}

	@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		mFooter.getView().setY(Utilities.dipToPixel(getContext(), 35));
	}
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		TunerService.get(mContext).addTunable(this, QS_SHOW_BRIGHTNESS);
		if (mHost != null) {
			setTiles(mHost.getTiles());
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		TunerService.get(mContext).removeTunable(this);
		if (mHost != null) {
			mHost.removeCallback(this);
		}
		for (TileRecord record : mRecords) {
			record.tile.removeCallbacks();
		}
		super.onDetachedFromWindow();
		
		unregisterBroadcastReceiver();
	}

	@Override
	public void onTilesChanged() {
		setTiles(mHost.getTiles());
	}

	@Override
	public void onTuningChanged(String key, String newValue) {
//		if (QS_SHOW_BRIGHTNESS.equals(key)) {
//			mBrightnessView.setVisibility(newValue == null || Integer.parseInt(newValue) != 0 ? VISIBLE : GONE);
//		}
	}

	public void openDetails(String subPanel) {
		QSTile<?> tile = getTile(subPanel);
		showDetailAdapter(true, tile.getDetailAdapter(), new int[] { getWidth() / 2, 0 });
	}

	private QSTile<?> getTile(String subPanel) {
		for (int i = 0; i < mRecords.size(); i++) {
			if (subPanel.equals(mRecords.get(i).tile.getTileSpec())) {
				return mRecords.get(i).tile;
			}
		}
		return mHost.createTile(subPanel);
	}

	public void setBrightnessMirror(BrightnessMirrorController c) {
        mBrightnessMirrorController = c;
		ToggleSlider brightnessSlider = (ToggleSlider) findViewById(R.id.brightness_slider);
		ToggleSlider mirror = (ToggleSlider) c.getMirror().findViewById(R.id.brightness_slider);
		brightnessSlider.setMirror(mirror);
		brightnessSlider.setMirrorController(c);
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	public void setHost(QSTileHost host, QSCustomizer customizer) {
		mHost = host;
		mHost.addCallback(this);
		setTiles(mHost.getTiles());
		mFooter.setHost(host);
		mCustomizePanel = customizer;
		if (mCustomizePanel != null) {
			mCustomizePanel.setHost(mHost);
		}
	}

	public QSTileHost getHost() {
		return mHost;
	}

	public void updateResources() {
		final Resources res = mContext.getResources();
		mPanelPaddingBottom = res.getDimensionPixelSize(R.dimen.qs_panel_padding_bottom);
		mBrightnessPaddingTop = res.getDimensionPixelSize(R.dimen.qs_brightness_padding_top);
		setPadding(0, mBrightnessPaddingTop, 0, mPanelPaddingBottom);
		for (TileRecord r : mRecords) {
			r.tile.clearState();
		}
		if (mListening) {
			refreshAllTiles();
		}
		if (mTileLayout != null) {
			mTileLayout.updateResources();
		}
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mFooter.onConfigurationChanged();
        if (mBrightnessMirrorController != null) {
            // Reload the mirror in case it got reinflated but we didn't.
            setBrightnessMirror(mBrightnessMirrorController);
        }
	}

	public void onCollapse() {
		if (mCustomizePanel != null && mCustomizePanel.isCustomizing()) {
			mCustomizePanel.hide(mCustomizePanel.getWidth() / 2, mCustomizePanel.getHeight() / 2);
		}
	}

	public void setExpanded(boolean expanded) {
		if (mExpanded == expanded)
			return;
		mExpanded = expanded;
		if (!mExpanded && mTileLayout instanceof PagedTileLayout) {
			((PagedTileLayout) mTileLayout).setCurrentItem(0, false);
		}
		MetricsLogger.visibility(mContext, MetricsEvent.QS_PANEL, mExpanded);
		if (!mExpanded) {
			mTriggeredExpand = false;
			closeDetail();
		} else {
			logTiles();
		}
	}

	public void setListening(boolean listening) {
		if (mListening == listening)
			return;
		mListening = listening;
		if (mTileLayout != null) {
			mTileLayout.setListening(listening);
		}
		mFooter.setListening(mListening);
		if (mListening) {
			refreshAllTiles();
		}
		if (listening) {
			mBrightnessController.registerCallbacks();
		} else {
			mBrightnessController.unregisterCallbacks();
		}
	}

	public void refreshAllTiles() {
		for (TileRecord r : mRecords) {
			r.tile.refreshState();
		}
		mFooter.refreshState();
	}

	public void showDetailAdapter(boolean show, DetailAdapter adapter, int[] locationInWindow) {
		int xInWindow = locationInWindow[0];
		int yInWindow = locationInWindow[1];
		((View) getParent()).getLocationInWindow(locationInWindow);

		Record r = new Record();
		r.detailAdapter = adapter;
		r.x = xInWindow - locationInWindow[0];
		r.y = yInWindow - locationInWindow[1];

		locationInWindow[0] = xInWindow;
		locationInWindow[1] = yInWindow;

		showDetail(show, r);
	}

	protected void showDetail(boolean show, Record r) {
		mHandler.obtainMessage(H.SHOW_DETAIL, show ? 1 : 0, 0, r).sendToTarget();
	}

	public void setTiles(Collection<QSTile<?>> tiles) {
		setTiles(tiles, false);
	}

	public void setTiles(Collection<QSTile<?>> tiles, boolean collapsedView) {
		for (TileRecord record : mRecords) {
			mTileLayout.removeTile(record);
            record.tile.removeCallback(record.callback);
		}
		mRecords.clear();
		for (QSTile<?> tile : tiles) {
			addTile(tile, collapsedView);
		}
	}

	protected void drawTile(TileRecord r, QSTile.State state) {
		r.tileView.onStateChanged(state);
	}

	protected QSTileBaseView createTileView(QSTile<?> tile, boolean collapsedView) {
		return new QSTileView(mContext, tile.createTileView(mContext), collapsedView);
	}

    protected boolean shouldShowDetail() {
        return true;//mExpanded;
    }
	protected void addTile(final QSTile<?> tile, boolean collapsedView) {
		final TileRecord r = new TileRecord();
		r.tile = tile;
		r.tileView = createTileView(tile, collapsedView);
		final QSTile.Callback callback = new QSTile.Callback() {
			@Override
			public void onStateChanged(QSTile.State state) {
				drawTile(r, state);
			}

			@Override
			public void onShowDetail(boolean show) {
                if (shouldShowDetail()) {
                    QSPanel.this.showDetail(show, r);
                }
			}

			@Override
			public void onToggleStateChanged(boolean state) {
				if (mDetailRecord == r) {
					fireToggleStateChanged(state);
				}
			}

			@Override
			public void onScanStateChanged(boolean state) {
				r.scanState = state;
				if (mDetailRecord == r) {
					fireScanStateChanged(r.scanState);
				}
			}

			@Override
			public void onAnnouncementRequested(CharSequence announcement) {
				announceForAccessibility(announcement);
			}
		};
		r.tile.addCallback(callback);
        r.callback = callback;
		final View.OnClickListener click = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onTileClick(r.tile);
			}
		};
		final View.OnLongClickListener longClick = new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				r.tile.longClick();
				return true;
			}
		};
		r.tileView.init(click, longClick);
		callback.onStateChanged(r.tile.getState());
		r.tile.refreshState();
		mRecords.add(r);

		if (mTileLayout != null) {
			mTileLayout.addTile(r);
		}
	}

	private void showEdit(final View v) {
		v.post(new Runnable() {
			@Override
			public void run() {
				if (mCustomizePanel != null) {
					if (!mCustomizePanel.isCustomizing()) {
						int[] loc = new int[2];
						v.getLocationInWindow(loc);
						int x = loc[0];
						int y = loc[1];
						mCustomizePanel.show(x, y);
					}
				}

			}
		});
	}

	protected void onTileClick(QSTile<?> tile) {
		tile.click();
	}

	public void closeDetail() {
		if (mCustomizePanel != null && mCustomizePanel.isCustomizing()) {
			// Treat this as a detail panel for now, to make things easy.
			mCustomizePanel.hide(mCustomizePanel.getWidth() / 2, mCustomizePanel.getHeight() / 2);
			return;
		}
		showDetail(false, mDetailRecord);
	}

	public int getGridHeight() {
		return getMeasuredHeight();
	}

	protected void handleShowDetail(Record r, boolean show) {
		LogHelper.sd("","handleShowDetail");
//		try {
//			if (r.detailAdapter.getClass().toString().contains("DndTile")){
//				return;
//			}
//		} catch (Exception e) {
//		}
		if (show) {
			if (!mExpanded) {
				mTriggeredExpand = true;
				mHost.animateToggleQSExpansion();
			} else {
				mTriggeredExpand = false;
			}
		} else if (mTriggeredExpand) {
			mHost.animateToggleQSExpansion();
			mTriggeredExpand = false;
		}
		if (r instanceof TileRecord) {
			LogHelper.sd("","handleShowDetail TileRecord");
			handleShowDetailTile((TileRecord) r, show);
		} else {
			int x = 0;
			int y = 0;
			if (r != null) {
				x = r.x;
				y = r.y;
			}
			handleShowDetailImpl(r, show, x, y);
		}
		setListening(show);
	}

	private void handleShowDetailTile(TileRecord r, boolean show) {
	/*	if ((mDetailRecord != null) == show && mDetailRecord == r){
			return;
		}*/
		
		if (show) {
			mDetailRecord = null;
		}
		if ((mDetailRecord != null) == show)
			return;
			
		if (show) {
			r.detailAdapter = r.tile.getDetailAdapter();
			if (r.detailAdapter == null)
				return;
		}
		r.tile.setDetailListening(show);
		int x = r.tileView.getLeft() + r.tileView.getWidth() / 2;
		int y = r.tileView.getTop() + mTileLayout.getOffsetTop(r) + r.tileView.getHeight() / 2 + getTop();
		//Utilities.dipToPixel(getContext(), 200);//
		try {
			LogHelper.sd("","handleShowDetail show="+show+" x="+x+" y="+y+" tile="+
					r.detailAdapter.getClass().toString());
		} catch (Exception e) {
		}
		
		if(!mBottomPanel){
			handleShowDetailImpl(r, show, x, y);
		}else{
			int[] location = new int[2];
			location[0] = 0;
			location[1] = 0;
			if (r.detailAdapter.getClass().toString().contains("WifiTile")) {
				location = getIconLocaton(QuickSettingsData.QS_ID_WLAN);

			} else if (r.detailAdapter.getClass().toString().contains("BluetoothTile")) {
				location = getIconLocaton(QuickSettingsData.QS_ID_BLUETOOTH);
			} else if (r.detailAdapter.getClass().toString().contains("DndTile")) {
				location = getIconLocaton(QuickSettingsData.QS_ID_DND_MODE);
			}
	        mClipper.animateCircularClip(location[0] + (int) dip2px(8), location[1] + (int) dip2px(30), show, null);
			handleShowDetailImpl(r, show, location[0] + (int) dip2px(8), location[1] + (int) dip2px(30));
		}
	}
	
	public int[] getIconLocaton(int id) {
		int[] location = new int[2];
		ArrayList<DatabaseRecord> mIconRecord = null;
		
		mIconRecord = YulongQuickSettingsContain.getInstance(mContext, Utilities.isPrimaryUser()).getIconConfig();
		int pos = 0;
		boolean tmpFlag = false;
		for (int i = 0; i < mIconRecord.size(); ++i) {
			if (mIconRecord.get(i).id == id) {
				pos = mIconRecord.get(i).pos;
				break;
			}
		}
		onBottomQSUpdates();
		QuickSettingsItemView item = null;
		switch (pos) {
		case 0:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item0);
			break;
		case 1:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item1);
			break;
		case 2:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item2);
			break;
		case 3:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item3);
			break;
		case 4:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item4);
			tmpFlag = true;
			break;
		case 5:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item5);
			tmpFlag = true;
			break;
		case 6:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item6);
			tmpFlag = true;
			break;
		case 7:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[0].findViewById(R.id.item7);
			tmpFlag = true;
			break;
		case 8:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item0);
			break;
		case 9:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item1);
			break;
		case 10:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item2);
			break;
		case 11:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item3);
			break;
		case 12:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item4);
			tmpFlag = true;
			break;
		case 13:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item5);
			tmpFlag = true;
			break;
		case 14:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item6);
			tmpFlag = true;
			break;
		case 15:
			item = (QuickSettingsItemView) (mQSRoot.getPage())[1].findViewById(R.id.item7);
			tmpFlag = true;
			break;
		default:
			break;
		}
		if (item != null) {
			location[0] = item.getLeft() + item.getWidth() / 2;
			location[1] = item.getTop() + item.getHeight() / 2;
			if (tmpFlag && 1 == QuickSettingLauncher.getInstance(mContext).getQSPanel().getOrientation()) {
				location[1] += (int) dip2px(86);
			}
		}
		return location;
	}

	private void handleShowDetailImpl(Record r, boolean show, int x, int y) {
//		if(!show){
//			mQSRoot.setVisibility(View.VISIBLE);
//		}else{
//			mQSRoot.setVisibility(View.INVISIBLE);
//		}
//		setDetailRecord(show ? r : null);
//		fireShowingDetail(show ? r.detailAdapter : null, x, y);
		if(mBottomPanel){
			Boolean mDR = mDetailRecord != null;
			if ((mDetailRecord != null) == show)
				return; // already in right state
			DetailAdapter detailAdapter = null;
			AnimatorListener listener = null;
			// Boolean bool = YulongConfig.getDefault().isMultiUserSpace();
			// Boolean b = UserUtils.currentUserIsOwner();
			if (mDetailTitleText != null) {
				mDetailTitleText.setVisibility(VISIBLE);
			}
			if (show) {
				detailAdapter = r.detailAdapter;
				if (mBottomPanel) {
					if (detailAdapter.getClass().toString().contains("WifiTile")) {
						if (mDetailTitleText != null) {
							mDetailTitleText.setText("WLAN");
						}
					} else if (detailAdapter.getClass().toString().contains("BluetoothTile")) {
						if (mDetailTitleText != null) {
							mDetailTitleText.setText(getResources().getString(R.string.quick_settings_bluetooth_label));
						}
					} else if (detailAdapter.getClass().toString().contains("DndTile")) {
						if (mDetailTitleText != null) {
							mDetailTitleText.setText(getResources().getString(R.string.quick_settings_dnd_label));
						}
					}
				}
				View detailView = null;
				detailView = detailAdapter.createDetailView(mContext, detailView, mDetailContent);
				if (detailView == null)
					throw new IllegalStateException("Must return detail view");

				final Intent settingsIntent = detailAdapter.getSettingsIntent();
				Boolean mSI = settingsIntent != null;
				if (mDetailSettingsButton != null) {
					mDetailSettingsButton.setVisibility(settingsIntent != null ? VISIBLE : GONE);
					mDetailSettingsButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							closeDetail();
							if(QuickSettingLauncher.getInstance(mContext) != null)QuickSettingLauncher.getInstance(mContext).setBottomPanelVisible(false);
							//mHost.startSettingsActivity(settingsIntent);
							mHost.startActivityDismissingKeyguard(settingsIntent);
						}
					});
				}
				if (mDetailDoneButton != null) {
					mDetailDoneButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							closeDetail();
						}
					});
				}

				if (mDetailTitleIcon != null) {
					mDetailTitleIcon.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							closeDetail();
						}
					});
				}
				// if((bool==true && b==true) || !bool){
				// configView.setVisibility(View.GONE);
				// }else{
				// configViewSecure.setVisibility(View.GONE);
				// }

				mDetailContent.removeAllViews();//
				//mDetail.setY(Utilities.dipToPixel(mContext, 8));
				if (mBottomPanel) {
					if (mDetailContent.getY() != 0.0 && mDetailTitleText != null && mDetailTitleIcon != null) {
						mDetailContent.setY(mDetailTitleText.getHeight() + mDetailTitleIcon.getHeight() + 1);
					}else{
						mDetailContent.setY(0);
					}
				} else {
					mDetailContent.setY(0);
				}
				mDetail.setVisibility(View.VISIBLE);
				mDetailContent.setVisibility(View.VISIBLE);
				
				if(Utilities.showFullGaussBlurForDDQS()){
//					mDetail.setBlurRadiusDp(12f);//===modify by ty
//					mDetail.setBlurChromaContrast(1.5f);
//					mDetail.setBlurAlpha(0.8f);
//					mDetail.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//					mDetail.setBackground(null);
					
					IvviGaussBlurViewFeature.setBlurRadiusDp(mDetail, 12f);
					IvviGaussBlurViewFeature.setBlurChromaContrast(mDetail, 1.5f);
					IvviGaussBlurViewFeature.setBlurAlpha(mDetail, 0.8f);
					IvviGaussBlurViewFeature.setBlurMode(mDetail, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
					IvviGaussBlurViewFeature.setBackground(mDetail, null);
				}
				
				mDetail.bringToFront();
				mDetailContent.addView(detailView);
				mQSRoot.setVisibility(View.GONE);
				if(Utilities.showFullGaussBlurForDDQS()){
//					mQSRoot.setBlurMode(BlurParams.BLUR_MODE_NONE);
					IvviGaussBlurViewFeature.setBlurMode(mQSRoot, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_NONE"));
				}
				
				postInvalidate();
				setDetailRecord(r);
				if (mBottomPanel) {
					setGridContentVisibility(false);
				} else {
					//listener = mHideGridContentWhenDone;
				}
			} else {
				mQSRoot.setVisibility(View.VISIBLE);
				
				if(Utilities.showFullGaussBlurForDDQS()){
//					mQSRoot.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//					mQSRoot.setBlurRadiusDp(12f);//===modify by ty
//					mQSRoot.setBlurChromaContrast(1.5f);
//					mQSRoot.setBlurAlpha(0.8f);
//					mQSRoot.setBackground(null);
//					
//					mDetail.setBlurMode(BlurParams.BLUR_MODE_NONE);
					
					IvviGaussBlurViewFeature.setBlurRadiusDp(mQSRoot, 12f);
					IvviGaussBlurViewFeature.setBlurChromaContrast(mQSRoot, 1.5f);
					IvviGaussBlurViewFeature.setBlurAlpha(mQSRoot, 0.8f);
					IvviGaussBlurViewFeature.setBlurMode(mQSRoot, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
					IvviGaussBlurViewFeature.setBackground(mQSRoot, null);
					
					IvviGaussBlurViewFeature.setBlurMode(mDetail, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_NONE"));
				}

				setGridContentVisibility(true);
				if (mBottomPanel) {
					mDetailContent.removeAllViews();
					setDetailRecord(null);
				} else {
					//listener = mTeardownDetailWhenDone;
				}
				fireScanStateChanged(false);
			}
			sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
			//fireShowingDetail(show ? detailAdapter : null);
		}
	}

	private void setDetailRecord(Record r) {
		if (r == mDetailRecord)
			return;
		mDetailRecord = r;
		final boolean scanState = mDetailRecord instanceof TileRecord && ((TileRecord) mDetailRecord).scanState;
		fireScanStateChanged(scanState);
	}

	void setGridContentVisibility(boolean visible) {
		int newVis = visible ? VISIBLE : INVISIBLE;
		setVisibility(newVis);
		if (mGridContentVisible != visible) {
			MetricsLogger.visibility(mContext, MetricsEvent.QS_PANEL, newVis);
		}
		mGridContentVisible = visible;
	}

	private void logTiles() {
		for (int i = 0; i < mRecords.size(); i++) {
			TileRecord tileRecord = mRecords.get(i);
			MetricsLogger.visible(mContext, tileRecord.tile.getMetricsCategory());
		}
	}

	private void fireShowingDetail(DetailAdapter detail, int x, int y) {
		if (mCallback != null) {
			mCallback.onShowingDetail(detail, x, y);
		}
	}

	private void fireToggleStateChanged(boolean state) {
		if (mCallback != null) {
			mCallback.onToggleStateChanged(state);
		}
	}

	private void fireScanStateChanged(boolean state) {
		if (mCallback != null) {
			mCallback.onScanStateChanged(state);
		}
	}

	public void clickTile(ComponentName tile) {
		final String spec = CustomTile.toSpec(tile);
		final int N = mRecords.size();
		for (int i = 0; i < N; i++) {
			if (mRecords.get(i).tile.getTileSpec().equals(spec)) {
				mRecords.get(i).tile.click();
				break;
			}
		}
	}

	QSTileLayout getTileLayout() {
		return mTileLayout;
	}

	QSTileBaseView getTileView(QSTile<?> tile) {
		for (TileRecord r : mRecords) {
			if (r.tile == tile) {
				return r.tileView;
			}
		}
		return null;
	}

	private class H extends Handler {
		private static final int SHOW_DETAIL = 1;
		private static final int SET_TILE_VISIBILITY = 2;

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == SHOW_DETAIL) {
				handleShowDetail((Record) msg.obj, msg.arg1 != 0);
			}
		}
	}

	protected static class Record {
		DetailAdapter detailAdapter;
		int x;
		int y;
	}

	public static final class TileRecord extends Record {
		public QSTile<?> tile;
		public QSTileBaseView tileView;
		public boolean scanState;
        public QSTile.Callback callback;
	}

	public interface Callback {
		void onShowingDetail(DetailAdapter detail, int x, int y);

		void onToggleStateChanged(boolean state);

		void onScanStateChanged(boolean state);
	}

	public interface QSTileLayout {
		void addTile(TileRecord tile);

		void removeTile(TileRecord tile);

		int getOffsetTop(TileRecord tile);

		boolean updateResources();

		void setListening(boolean listening);
	}
	
	private int measureWidth(int pWidthMeasureSpec) {
		int result = 0;
		int widthMode = MeasureSpec.getMode(pWidthMeasureSpec);
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
	
//	public interface Callback {
//		void onShowingDetail(QSTile.DetailAdapter detail);
//
//		void onToggleStateChanged(boolean state);
//
//		void onScanStateChanged(boolean state);
//	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, Intent intent) {
			String action = intent.getAction();
			if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "action = " + action);
			if (Intent.ACTION_USER_SWITCHED.equals(action) || Intent.ACTION_USER_REMOVED.equals(action)
					|| Intent.ACTION_USER_ADDED.equals(action) || Intent.ACTION_USER_INFO_CHANGED.equals(action)
					|| Intent.ACTION_USER_INITIALIZE.equals(action) || Intent.ACTION_USER_FOREGROUND.equals(action)
					|| Intent.ACTION_USER_BACKGROUND.equals(action)) {
				updateConfigView();
				requestLayout();
			}
		}
	};
	private BroadcastReceiver mBottomPanelReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, Intent intent) {
			String action = intent.getAction();
			if(LogHelper.NOLOGGING)LogHelper.sd(TAG, "action = " + action);
			if ("android.intent.action.BOTTOMPANEL_UP".equals(action)) {
				setBottomPanelListen();
			} else if ("android.intent.action.BOTTOMPANEL_DOWN".equals(action)) {
				cancelBottomPanelListen();
			}
		}
	};

	private void updateConfigView() {
//		boolean b = UserUtils.currentUserIsOwner();
//		LogHelper.sd(TAG, "updateConfigView bPrimary=" + b);
//		removeView(mBrightnessView);
//		removeView(mFooter.getView());
//		addView(mBrightnessView);
//		addView(mFooter.getView());
	}

	private boolean isRegisteredBottomPanelReceiver = false;

	private void registerBottomPanelBrocastReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.BOTTOMPANEL_UP");
		filter.addAction("android.intent.action.BOTTOMPANEL_DOWN");
		mContext.registerReceiver(mBottomPanelReceiver, filter);
		isRegisteredBottomPanelReceiver = true;
	}

	private void registerBroadcastReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_USER_SWITCHED);
		filter.addAction(Intent.ACTION_USER_REMOVED);
		filter.addAction(Intent.ACTION_USER_ADDED);
		filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
		filter.addAction(Intent.ACTION_USER_INITIALIZE);
		filter.addAction(Intent.ACTION_USER_FOREGROUND);
		filter.addAction(Intent.ACTION_USER_BACKGROUND);
		mContext.registerReceiver(mReceiver, filter);
	}

	private void unregisterBroadcastReceiver() {
		if (bool) {
			mContext.unregisterReceiver(mReceiver);
		}
		if (isRegisteredBottomPanelReceiver) {
			mContext.unregisterReceiver(mBottomPanelReceiver);
			isRegisteredBottomPanelReceiver = false;
		}
	}

	DecelerateInterpolator in = new DecelerateInterpolator();

	public void setQSTitleView(float height) {
		// TODO Auto-generated method stub
		float cfH = 0;

		float DragY = (height - cfH);// 288//388
		if (DragY < 0) {
			DragY = 0;
		}
		float rata = in.getInterpolation(DragY / dip2px(80));
		if (rata > 0) {
			mBrightnessView.setAlpha(rata * 1f);
			mBrightnessView.setScaleX(0.9f + rata * 0.1f);
			mBrightnessView.setScaleY(0.9f + rata * 0.1f);
		}
		invalidate();
	}

	public void initUnShowQSTitleView() {
		if (mBrightnessView != null) {
			mBrightnessView.setAlpha(0f);
			mBrightnessView.setScaleX(0.9f);
			mBrightnessView.setScaleY(0.9f);
		}
		invalidate();
	}

	public void reset() {
		if (mBrightnessView != null) {
			mBrightnessView.setAlpha(1f);
			mBrightnessView.setScaleX(1f);
			mBrightnessView.setScaleY(1f);
		}
		invalidate();
	}

	protected float dip2px(int dip) {
		float scale = mContext.getResources().getDisplayMetrics().density;
		return (float) (dip * scale + 0.5f);
	}

	public void cancelBottomPanelListen() {
		if(mMobilePanelView != null)mMobilePanelView.setVisibility(View.INVISIBLE);
		if(mQSRoot != null)mQSRoot.setVisibility(View.VISIBLE);
		if (mHost != null) {
			Collection<QSTile<?>> tiles = mHost.getTiles();
			for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
				QSTile<?> qsTile = (QSTile<?>) iterator.next();
				qsTile.setListening(false);
			}
		}
		closeDetail();
	}

	public void setBottomPanelListen() {
		if (mHost != null) {
			Collection<QSTile<?>> tiles = mHost.getTiles();
			for (Iterator iterator = tiles.iterator(); iterator.hasNext();) {
				QSTile<?> qsTile = (QSTile<?>) iterator.next();
				qsTile.setListening(true);
			}
		}
	}

    public void setMobilePanelViewVisible(boolean isShow) {
        if (QuickSettingLauncher.isPopuped) {
            int[] location = new int[2];
            location[0] = 0;
            location[1] = 0;
            location = getIconLocaton(QuickSettingsData.QS_ID_MOBILEDATA);
            mDataClipper.animateCircularClip(location[0] + (int) dip2px(8), location[1] + (int) dip2px(30), isShow, null);
            if (isShow) {
                // if(mMobilePanelView != null)mMobilePanelView.setVisibility(View.VISIBLE);
                if(mMobilePanelView != null){
                	mMobilePanelView.bringToFront();
                }
                if(mMobilePanelView != null)mMobilePanelView.readSim();
                
                if(Utilities.showFullGaussBlurForDDQS()){
//                	mMobilePanelView.setBlurRadiusDp(12f);//===modify by ty
//                    mMobilePanelView.setBlurChromaContrast(1.5f);
//                    mMobilePanelView.setBlurAlpha(0.8f);
//                    mMobilePanelView.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//                    mMobilePanelView.setBackground(null);
                    
                    IvviGaussBlurViewFeature.setBlurRadiusDp(mMobilePanelView, 12f);
					IvviGaussBlurViewFeature.setBlurChromaContrast(mMobilePanelView, 1.5f);
					IvviGaussBlurViewFeature.setBlurAlpha(mMobilePanelView, 0.8f);
					IvviGaussBlurViewFeature.setBlurMode(mMobilePanelView, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
					IvviGaussBlurViewFeature.setBackground(mMobilePanelView, null);
                }
				
                mQSRoot.setVisibility(View.INVISIBLE);
                if(Utilities.showFullGaussBlurForDDQS()){
//                	mQSRoot.setBlurMode(BlurParams.BLUR_MODE_NONE);
                	IvviGaussBlurViewFeature.setBlurMode(mQSRoot, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_NONE"));
                }
            } else {
                if(mMobilePanelView != null)mMobilePanelView.setVisibility(View.GONE);
            	mQSRoot.setVisibility(View.VISIBLE);
            	
            	if(Utilities.showFullGaussBlurForDDQS()){
//            		mQSRoot.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
//    				mQSRoot.setBlurRadiusDp(12f);//===modify by ty
//    				mQSRoot.setBlurChromaContrast(1.5f);
//    				mQSRoot.setBlurAlpha(0.8f);
//    				mQSRoot.setBackground(null);
//    				
//    				mMobilePanelView.setBlurMode(BlurParams.BLUR_MODE_NONE);
    				
    				IvviGaussBlurViewFeature.setBlurRadiusDp(mQSRoot, 12f);
					IvviGaussBlurViewFeature.setBlurChromaContrast(mQSRoot, 1.5f);
					IvviGaussBlurViewFeature.setBlurAlpha(mQSRoot, 0.8f);
					IvviGaussBlurViewFeature.setBlurMode(mQSRoot, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_WINDOW"));
					IvviGaussBlurViewFeature.setBackground(mQSRoot, null);
					
					IvviGaussBlurViewFeature.setBlurMode(mMobilePanelView, IvviGaussBlurViewFeature.getPropertyBlurMode("BLUR_MODE_NONE"));
            	}
            }
        }
    }

	public void setQSBottomRoot(QuickSettingsPannelView qsPanel) {
		mQSRoot = qsPanel;
	}
	
	public void setMobilePanelView(QuickSettingMobilePanelView mobilePanelView) {
		mMobilePanelView = mobilePanelView;
	}

	public void onBottomQSUpdates() {
	  QSBottomPanel bottomPanel = QuickSettingLauncher.getInstance(mContext).getQSPanel();
	  if(mQSRoot == null){
		   mQSRoot = bottomPanel.getQSPannelView();
	    }
		mMobilePanelView = bottomPanel.getMobilePanelView();
		mDetail = bottomPanel.getDetail();
		mDetailTitleText = bottomPanel.getDetailTitleText();
		mDetailContent = bottomPanel.getDetailContent();
		mDetailTitleIcon = bottomPanel.getDetailTitleIcon();
		mDetailDoneButton = bottomPanel.getDetailDoneButton();
		mDetailSettingsButton = bottomPanel.getDetailSettingsButton();
		mClipper = bottomPanel.getClipper();
		mDataClipper = bottomPanel.getDataClipper();
	}
}
