package com.android.keyguard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.os.IBinder;
import android.os.Parcel;

public class YLClassProxy {
    
    static Class<?> SystemUtil;
    static Class<?> GlobalKeys;
    static Class<?> SystemManager;
    static Class<?> ISystemInterface;
    static Class<?> SystemInterfaceFactory;
    static Class<?> WindowManager$LayoutParams;
    
    static {
        try {
            SystemUtil = Class.forName("com.yulong.android.server.systeminterface.util.SystemUtil");
            GlobalKeys = Class.forName("com.yulong.android.server.systeminterface.GlobalKeys");
            SystemManager = Class.forName("com.yulong.android.server.systeminterface.SystemManager");
            ISystemInterface = Class.forName("com.yulong.android.server.systeminterface.ISystemInterface");
            SystemInterfaceFactory = Class.forName("com.yulong.android.server.systeminterface.SystemInterfaceFactory");
            WindowManager$LayoutParams = Class.forName("android.view.WindowManager$LayoutParams");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String SystemUtil_getYLParam(String key) {
        try {
            Method method = SystemUtil.getDeclaredMethod("getYLParam", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void SystemUtil_setYLParam(String key, String value) {
        try {
            Method method = SystemUtil.getDeclaredMethod("setYLParam", String.class, String.class);
            method.setAccessible(true);
            method.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String SystemUtil_getLockLevel() {
        try {
            Method method = SystemUtil.getDeclaredMethod("getLockLevel");
            method.setAccessible(true);
            return (String) method.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void SystemUtil_setLockLevel(String value) {
        try {
            Method method = SystemUtil.getDeclaredMethod("setLockLevel", String.class);
            method.setAccessible(true);
            method.invoke(null, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String GlobalKeys_SYS_SERVICE() {
        try {
            Field field = GlobalKeys.getDeclaredField("SYS_SERVICE");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String SystemManager_getSecurityManagerPassword(Object pthis) {
        try {
            Method method = SystemManager.getDeclaredMethod("getSecurityManagerPassword");
            method.setAccessible(true);
            return (String) method.invoke(pthis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object SystemInterfaceFactory_getSysteminterface() {
        try {
            Method method = SystemInterfaceFactory.getDeclaredMethod("getSysteminterface");
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean ISystemInterface_isRebootPassEnable(Object pthis) {
        try {
            Method method = ISystemInterface.getDeclaredMethod("isRebootPassEnable");
            method.setAccessible(true);
            return (Boolean) method.invoke(pthis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int ISystemInterface_validateKeyguardSecurityPass(Object pthis, String key) {
        try {
            Method method = ISystemInterface.getDeclaredMethod("validateKeyguardSecurityPass", String.class);
            method.setAccessible(true);
            return (Integer) method.invoke(pthis, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int ISystemInterface_getShowKeyguardType(Object pthis) {
        try {
            Method method = ISystemInterface.getDeclaredMethod("getShowKeyguardType");
            method.setAccessible(true);
            return (Integer) method.invoke(pthis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public static Boolean ISystemInterface_dismissBeidao(Object pthis) {
        try {
            Method method = ISystemInterface.getDeclaredMethod("dismissBeidao");
            method.setAccessible(true);
            return (Boolean) method.invoke(pthis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean ISystemInterface_isShowGraphicWithKeyguardEnable(Object pthis) {
        try {
            Method method = ISystemInterface.getDeclaredMethod("isShowGraphicWithKeyguardEnable");
            method.setAccessible(true);
            return (Boolean) method.invoke(pthis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public static int LC_FLAG_YL_FULLSCREEN() {
        try {
            Field field = WindowManager$LayoutParams.getDeclaredField("FLAG_YL_FULLSCREEN");
            field.setAccessible(true);
            return (Integer) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static IBinder ServiceManager_getService(String serviceName) {
        try {
            Class<?> classObject = Class.forName("android.os.ServiceManager");
            Method method = classObject.getDeclaredMethod("getService", String.class);
            return (IBinder) method.invoke(null, serviceName);
        } catch (Exception e) {
        }
        return null;
    }

    public static void SystemProperties_setForRemote(String key, String value) {
        IBinder uitechnoService = YLClassProxy.ServiceManager_getService("uitechnoService");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(key);
        data.writeString(value);
        try {
            uitechnoService.transact(IBinder.FIRST_CALL_TRANSACTION + 1, data, reply, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.recycle();
        reply.recycle();
    }
}
