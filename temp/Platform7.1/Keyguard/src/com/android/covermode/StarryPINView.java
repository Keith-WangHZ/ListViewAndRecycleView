/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.covermode;

import com.android.covermode.StarryPatternView.DismissAction;
import com.android.keyguard.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * Displays a PIN pad for unlocking.
 */
public class StarryPINView extends StarryAbsKeyInputView implements OnEditorActionListener, TextWatcher {

	public StarryPINView(Context context) {
		this(context, null);
	}

	public StarryPINView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private DismissPin dismisspin = null;

	/* package */public interface DismissPin {
		/* returns true if the dismiss should be deferred */
		boolean Dismiss();
	}

	public void setDismissPin(DismissPin mDismissPin) {
		setDismissAbs(mDismissPin);
	}

	protected void resetState() {

		mSecurityMessageDisplay.setMessage(R.string.kg_pin_instructions, false);

		mPasswordEntry.setEnabled(true);
	}

	@Override
	protected int getPasswordTextViewId() {
		return R.id.pinEntry;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mSecurityMessageDisplay = new StarryMessageArea.Helper(this);
		mSecurityMessageDisplay.setDefaultMessage(R.string.kg_pin_instructions);
		reset();
		final View ok = findViewById(R.id.starry_pin_enter);
		if (ok != null) {
			ok.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					doHapticKeyClick();
					if (mPasswordEntry.isEnabled()) {
						verifyPasswordAndUnlock();
					}
				}
			});
			ok.setOnHoverListener(new LiftToActivateListener(getContext()));
		}

		// The delete button is of the PIN keyboard itself in some (e.g. tablet)
		// layouts,
		// not a separate view
		View pinDelete = findViewById(R.id.delete_button);
		if (pinDelete != null) {
			pinDelete.setVisibility(View.VISIBLE);
			pinDelete.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// check for time-based lockouts
					if (mPasswordEntry.isEnabled()) {
						CharSequence str = mPasswordEntry.getText();
						if (str.length() > 0) {
							mPasswordEntry.setText(str.subSequence(0, str.length() - 1));
						}
					}
					doHapticKeyClick();
				}
			});
			pinDelete.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					// check for time-based lockouts
					if (mPasswordEntry.isEnabled()) {
						mPasswordEntry.setText("");
					}
					doHapticKeyClick();
					return true;
				}
			});
		}

		mPasswordEntry.setKeyListener(DigitsKeyListener.getInstance());
		mPasswordEntry.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

		mPasswordEntry.requestFocus();
	}

	@Override
	public int getWrongPasswordStringId() {
		return R.string.kg_wrong_pin;
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		return false;
	}
}
