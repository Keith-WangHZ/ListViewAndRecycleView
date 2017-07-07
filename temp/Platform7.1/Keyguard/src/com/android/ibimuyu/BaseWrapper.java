package com.android.ibimuyu;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public abstract class BaseWrapper {

	protected Context mContext;
	protected String mPackageName;

	protected ViewGroup mLayout = null;
	protected FrameworkWrapper mFrameWrapper;
	protected Runnable mUnlockRunnable = null;
	protected Handler mCallbackHandler = null;

	private static final String TAG = "BaseWrapper";
	private static final String DEFAULT_FRAMEWORK_PACKAGE_NAME = "com.ibimuyu.lockscreen";

	public boolean create() {
		mFrameWrapper = new FrameworkWrapper();
		boolean ok = mFrameWrapper.create(mContext, mPackageName);

		if (ok) {
			mLayout = new FrameLayout(mContext);

			mCallbackHandler = new Handler(Looper.getMainLooper()) {
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case FrameworkWrapper.MSG_F2W_UNLOCK:
						if (mUnlockRunnable != null) {
							mUnlockRunnable.run();
						}
						break;

					default:
						break;
					}
				};
			};
			return true;
		} else {
			mFrameWrapper = null;
		}

		return false;
	}

	public boolean destory() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_DESTROY;
			Object obj = mFrameWrapper.callFWMethod(msg);

			mUnlockRunnable = null;

			mLayout.removeAllViews();
			mLayout = null;

			mCallbackHandler.removeMessages(FrameworkWrapper.MSG_F2W_UNLOCK);
			mCallbackHandler = null;

			mFrameWrapper.destory();
			mFrameWrapper = null;

			return obj != null && (Boolean) obj;
		}
		return false;
	}

	public boolean play() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_PLAY;
			return mFrameWrapper.sendMsgToFW(msg);
		}
		return false;
	}

	public boolean stop() {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_STOP;
			return mFrameWrapper.sendMsgToFW(msg);
		}
		return false;
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		if (mFrameWrapper != null) {
			Message msg = Message.obtain();
			msg.what = FrameworkWrapper.MSG_W2F_DISPATCH_KEY_EVENT;
			msg.obj = event;
			Object obj = mFrameWrapper.callFWMethod(msg);
			return obj != null && (Boolean) obj;
		}

		return false;
	}

	public BaseWrapper(Context context, String packageName) {
		mContext = context;
		mPackageName = packageName;
	}

	protected BaseWrapper(Context context) {
		this(context, DEFAULT_FRAMEWORK_PACKAGE_NAME);
	}

	protected View updateLayout(View view) {
		if (view != null && mLayout != null) {
			mLayout.removeAllViews();
			mLayout.addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			return mLayout;
		}
		return null;
	}
}
