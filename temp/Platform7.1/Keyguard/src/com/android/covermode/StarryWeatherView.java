/*
 * 
 * Copyright (C) 20013-2015 YULONG Corporation
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
/*
 *creator  zhangbo 2013 1312
 */
package com.android.covermode;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;
import com.android.keyguard.KeyguardUpdateMonitor.WeatherData;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StarryWeatherView extends LinearLayout {
	private ImageView mWeatherIconView;
	private TextView mWeatherTempText;
	private TextView mWeatherText;
	private String keyguard_weather_ctemp;
	static private String TAG = "WeatherView";
	private int mWeatherTextResId;
	private int mTemperatureTextResId;
	private int mIconResId;

	private int[] weatherSmallIcIds = new int[] { R.drawable.weather_null, R.drawable.weather_01, R.drawable.weather_17, R.drawable.weather_03,
			R.drawable.weather_04, R.drawable.weather_17, R.drawable.weather_06, R.drawable.weather_07, R.drawable.weather_08, R.drawable.weather_09,
			R.drawable.weather_07, R.drawable.weather_08, R.drawable.weather_17, R.drawable.weather_13, R.drawable.weather_07, R.drawable.weather_07,
			R.drawable.weather_17, R.drawable.weather_17, R.drawable.weather_08, R.drawable.weather_08, R.drawable.weather_07, R.drawable.weather_17,
			R.drawable.weather_22, R.drawable.weather_17, R.drawable.weather_17, R.drawable.weather_25, R.drawable.weather_26, R.drawable.weather_27,
			R.drawable.weather_28, R.drawable.weather_29, R.drawable.weather_08, R.drawable.weather_08, R.drawable.weather_32 };

	private void init() {
		mWeatherIconView = (ImageView) findViewById(mIconResId);
		mWeatherText = (TextView) findViewById(mWeatherTextResId);
		mWeatherTempText = (TextView) findViewById(mTemperatureTextResId);
		keyguard_weather_ctemp = mContext.getString(R.string.keyguard_weather_ctemp);
		mWeatherIconView.setImageResource(R.drawable.weather_null);// lxb add for test
		// weatherTempView.setText("27"+keyguard_weather_ctemp);
		// mContext.sendBroadcast(new
		// Intent("android.icoolme.intent.action.GET_WEATHER_DATA"));
	}

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.getLayoutParams();
		// lp.bottomMargin=30;
		KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
		if (mWeatherTempText.getText() == null || "".equals(mWeatherTempText.getText())) {
			mContext.sendBroadcast(new Intent("android.icoolme.intent.action.GET_WEATHER_DATA"));
		}
	}

	public void updateDate(WeatherData weatherData) {
		int temp = weatherData.mWeatherType;
		Log.v(TAG, "temp = " + temp);
		if (temp < 0 || temp > 32) {
			mWeatherIconView.setImageResource(weatherSmallIcIds[0]);
		} else {
			mWeatherIconView.setImageResource(weatherSmallIcIds[temp]);
		}
		mWeatherTempText.setText(weatherData.mCurrentTemperature + keyguard_weather_ctemp);
		if (mWeatherText!=null) {
			mWeatherText.setText(weatherData.mWeatherDes);
		}
	}
	
	public StarryWeatherView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
		if (!SystemProperties.getBoolean("ro.secure.system", false)) {
			Log.v(TAG, "what's that?");
		} else {
			Log.v(TAG, "that?");
			this.setVisibility(View.INVISIBLE);
		}
	}

	private void init(AttributeSet attrs) {
		TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.WeatherItem);
		try {
			mWeatherTextResId = a.getResourceId(R.styleable.WeatherItem_weatherText, 0);
			mTemperatureTextResId = a.getResourceId(R.styleable.WeatherItem_temperatureText, 0);
			mIconResId = a.getResourceId(R.styleable.WeatherItem_weatherIcon, 0);
		} finally {
			a.recycle();
		}
	}

	protected void onFinishInflate() {
		super.onFinishInflate();
		init();
	}

	private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onWeatherChanged(WeatherData weatherData) {
			if (weatherData != null) {
				updateDate(weatherData);
			}
		}
	};

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
	}

}
