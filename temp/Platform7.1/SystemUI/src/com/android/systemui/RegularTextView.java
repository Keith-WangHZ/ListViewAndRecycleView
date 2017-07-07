package com.android.systemui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class RegularTextView extends TextView{

	public RegularTextView(Context arg0, AttributeSet arg1, int arg2, int arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
		setTypeface(Utils.getInstance(arg0).getTypeface(Utils.TYPE_REGULAR));
	}
	
	public RegularTextView(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2, 0);
		// TODO Auto-generated constructor stub
	}
	
	public RegularTextView(Context arg0, AttributeSet arg1) {
		super(arg0, arg1, 0, 0);
		// TODO Auto-generated constructor stub
	}
	
	public RegularTextView(Context arg0) {
		super(arg0, null, 0, 0);
		// TODO Auto-generated constructor stub
	}
}
