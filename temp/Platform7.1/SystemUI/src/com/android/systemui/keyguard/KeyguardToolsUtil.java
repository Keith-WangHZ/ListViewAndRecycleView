package com.android.systemui.keyguard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import com.levect.lockscreen.proa.aidl.ImageBean;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class KeyguardToolsUtil {
	
	private static final String TAG = "KeyguardToolsUtil";
	private static PackageManager mPm;

	public static boolean isSpecificPackageExist(Context context, String packageName) {
		if (packageName == null || "".equals(packageName))
			return false;
		try {
			if (mPm == null) {
				mPm = context.getPackageManager();
			}
			ApplicationInfo info = mPm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	public static ArrayList<ImageBean> getDefaultImageBean(Context context, ArrayList<ImageBean> array) {

		try {
			String jsonString = readFileToString(context);
			if (!TextUtils.isEmpty(jsonString)) {
				JSONArray jsonArray = new JSONArray(jsonString);
				JSONObject jsonObject;
				ImageBean imageBean;
				// defaultIds.clear();
				for (int i = 0; i < jsonArray.length(); i++) {
					jsonObject = jsonArray.getJSONObject(i);
					imageBean = new ImageBean();
					String img_id = jsonObject.getString("img_id");
					// defaultIds.add(img_id);
					imageBean.setImg_id(img_id);
					imageBean.setTitle(jsonObject.getString("title"));
					imageBean.setContent(jsonObject.getString("content"));
					imageBean.setType_name(jsonObject.getString("type_name"));
					imageBean.setType_id(jsonObject.getString("type_id"));
					imageBean.setUrl_img(jsonObject.getString("url_img"));
					imageBean.setUrl_assets(jsonObject.getString("url_assets"));
					imageBean.setUrl_local(jsonObject.getString("url_local"));
					imageBean.setUrl_pv(jsonObject.getString("url_pv"));
					imageBean.setUrl_click(jsonObject.getString("url_click"));
					// screenImg.setImg_type(jsonObject.getInt("img_type"));
					imageBean.setAlways(jsonObject.getInt("always"));
					imageBean.setIs_collection(jsonObject.getInt("is_collection"));
					imageBean.setMagazine_id(jsonObject.getString("magazine_id"));
					imageBean.setDaily_id(jsonObject.getString("daily_id"));
					imageBean.setIs_collection(jsonObject.getInt("is_collection"));
					array.add(imageBean);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return array;
	}

	private static String readFileToString(Context context) {
		String path = getPathImage(context) + "default_img_data.txt";
		String res = null;
		FileInputStream fin = null;
		try {
			File f = new File(path);
			if (f != null && f.exists()) {
				fin = new FileInputStream(f);
				int length = fin.available();
				byte[] buffer = new byte[length];
				fin.read(buffer);
				// res = EncodingUtils.getString(buffer, "UTF-8");
				res = new String(buffer, "UTF-8");
			}
		} catch (Exception e) {
			Log.v("magazine", " readfiletostring error e = " + e);
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					Log.v("magazine", " readfiletostring close error e = " + e);
				}
			}
		}
		return res;
	}

	public static final String SDCardGen = "/.haokanScreen/";

	public static String getPathImage(Context context) {
		String imageFolderName;
		boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED); //
		// sdCardExist = false;
		if (sdCardExist) {
			imageFolderName = Environment.getExternalStorageDirectory() + SDCardGen + "image/";
		} else {
			imageFolderName = context.getDir("image", Context.MODE_PRIVATE).getAbsolutePath() + "/";
		}
		try {
			new File(imageFolderName).mkdirs();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return imageFolderName;
	}
	
	public static final String WALLPAPER_CLASSNAME = "android.app.WallpaperManager";
    public static final String WALLPAPER_SET_KEYGUARD = "setKeyguardBitmap";
	
	public static void setKeyguardWallpaper(Context mContext,Bitmap bitmap) {

		try {
			Method method;
			method = getMethod(WALLPAPER_CLASSNAME, WALLPAPER_SET_KEYGUARD, Bitmap.class);
			if (null != method)
				invokeMethod(method, WallpaperManager.getInstance(mContext), bitmap);
			Log.i(TAG, "setLockScreenWallpaper complete");
		} catch (IllegalArgumentException e){
			Log.e(TAG, "failed setKeyguardWallpaper", e);
		}

	}

	public static Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
		Class<?> clazz = null;
		Method method = null;
		try {
			clazz = Class.forName(className);
			method = clazz.getMethod(methodName, parameterTypes);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return method;
	}

	public static Object invokeMethod(Method method, Object obj, Object... parameters) {

		Object retObj = null;
		if (null == method || null == obj){
			return null;
		}

		try {
			retObj = method.invoke(obj, parameters);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.out.print(e.getTargetException());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retObj;
		
	}
	
	public static Bitmap drawableToBitamp(Drawable drawable) {
		
		BitmapDrawable bd = (BitmapDrawable) drawable;
		Bitmap bitmap = bd.getBitmap();
		return bitmap;
		
	}
	

}
