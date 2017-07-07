package com.android.systemui.quicksettings;

import java.io.Serializable;

import com.android.systemui.statusbar.phone.QuickSettingsItemView;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;

public class QuickSettingsItem{
	private static final long serialVersionUID = -7347696827444187081L;
	public State state;
	public QuickSettingsItemView vItem;
	public QuickSettingsItem(State s)
	{
		state = s;
	}
}
