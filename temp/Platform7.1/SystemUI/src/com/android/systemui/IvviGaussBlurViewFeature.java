package com.android.systemui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.graphics.drawable.Drawable;
import android.view.View;

public class IvviGaussBlurViewFeature {
	public static final String TAG = "IvviGaussBlurViewFeature";
	/**
	 * 
	 * @param object
	 * @param methodName
	 * @param parameterTypes
	 * @return
	 */
    public static Method getDeclaredMethodParent(Object object, String methodName, Class<?> ... parameterTypes){  
        Method method = null ;  
          
        for(Class<?> clazz = object.getClass() ; clazz != Object.class ; clazz = clazz.getSuperclass()) {  
            try {  
                method = clazz.getDeclaredMethod(methodName, parameterTypes) ;  
                return method ;  
            } catch (Exception e) {  
              
            }  
        }  
          
        return null;  
    }  
    
    /**
     * 
     * @param object
     * @param fieldName
     * @return
     */
    public static Field getDeclaredFieldParent(Object object, String fieldName){  
        Field field = null ;  
          
        Class<?> clazz = object.getClass() ;  
          
        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {  
            try {  
                field = clazz.getDeclaredField(fieldName) ;  
                return field ;  
            } catch (Exception e) {  
                  
            }   
        }  
      
        return null;  
    }  
    
    /**
     * 
     * @param object
     * @param methodName
     * @param parameterTypes
     * @return
     */
    public static Method getDeclaredMethod(Object object, String methodName, Class<?> ... parameterTypes){  
        Method method = null ;  
        try {  
        	Class<?> clazz = object.getClass();
            method = clazz.getDeclaredMethod(methodName, parameterTypes) ;  
            return method ;  
        } catch (Exception e) {  
          
        }   
          
        return null;  
    }  
      
    /**
     * 
     * @param object
     * @param fieldName
     * @return
     */
    public static Field getDeclaredField(Object object, String fieldName){  
        Field field = null ;  
          
        Class<?> clazz = object.getClass() ;  
          
        try {  
            field = clazz.getDeclaredField(fieldName) ;  
            return field ;  
        } catch (Exception e) {  
              
        } 
      
        return null;  
    } 
      
    /**
     * 
     * @param object
     * @param methodName
     * @param parameterTypes
     * @param parameters
     * @return
     */
    public static Object invokeMethod(Object object, String methodName, Class<?> [] parameterTypes,  
            Object [] parameters) {  
        Method method = getDeclaredMethod(object, methodName, parameterTypes) ;  
          
        method.setAccessible(true) ;  
            try {  
                if(null != method) {  
                    return method.invoke(object, parameters) ;  
                }  
            } catch (IllegalArgumentException e) {  
                e.printStackTrace();  
            } catch (IllegalAccessException e) {  
                e.printStackTrace();  
            } catch (InvocationTargetException e) {  
                e.printStackTrace();  
            }  
          
        return null;  
    } 
    
    /**
     * Write fieldValue to object, ignore private/protected qualifiers, also ignore setter 
     * @param object
     * @param fieldName
     * @param value
     */
    public static void setFieldValue(Object object, String fieldName, Object value){  
      
        Field field = getDeclaredField(object, fieldName) ;  
          
        field.setAccessible(true) ;  
          
        try {  
             field.set(object, value) ;  
        } catch (IllegalArgumentException e) {  
            e.printStackTrace();  
        } catch (IllegalAccessException e) {  
            e.printStackTrace();  
        }  
          
    }  
      
    /**
     * Read fieldValue from object, ignore private/protected qualifiers, also ignore getter 
     * @param object
     * @param fieldName
     * @return
     */
    public static Object getFieldValue(Object object, String fieldName){  
          
        Field field = getDeclaredField(object, fieldName) ;  
          
        field.setAccessible(true) ;  
          
        try {  
            return field.get(object) ;  
              
        } catch(Exception e) {  
            e.printStackTrace() ;  
        }  
          
        return null;  
    }  
    
    public static int getPropertyBlurMode(String m){
    	int mode = 0;
    	try {
    		 Class<?> mClass = Class.forName("android.graphics.BlurParams");

             Object mObject = mClass.newInstance();
             
             mode = (int)getFieldValue(mObject, m);
		} catch (Exception e) {
		}
    	return mode;
    }
    
    public static Object getPropertyBlurModeNone(){
    	try {
    		 Class<?> mClass = Class.forName("android.graphics.BlurParams");

             Object mObject = mClass.newInstance();
             
             return getFieldValue(mObject, "BLUR_MODE_NONE");
		} catch (Exception e) {
		}
    	return null;
    }
    
    public static Object getPropertyBlurModeWindow(){
    	try {
   		 Class<?> mClass = Class.forName("android.graphics.BlurParams");

            Object mObject = mClass.newInstance();
            
            return getFieldValue(mObject, "BLUR_MODE_WINDOW");
		} catch (Exception e) {
		}
    	return null;
    }
    
//  public static final int BLUR_MODE_NONE = 0;
//	public static final int BLUR_MODE_VIEW = 1;
//	public static final int BLUR_MODE_WINDOW = 2;
//	public static final int DEFAULT_BLUR_MODE = BLUR_MODE_NONE;
    public static void setBlurMode(View view, int mode){  
    	//view.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
    	try {
            Class<?> mClass = Class.forName("android.view.View");

            Object mObject = view;
            
            Method method = mClass.getMethod("setBlurMode", int.class);
            method.setAccessible(true);
            method.invoke(mObject, mode);

        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    public static void setBlurModeWindow(View view){  
    	//view.setBlurMode(BlurParams.BLUR_MODE_WINDOW);
    	try {
            Class<?> mClass = Class.forName("android.view.View");

            Object mObject = view;
            
            Method method = mClass.getMethod("setBlurMode", int.class);
            method.setAccessible(true);
            method.invoke(mObject, getPropertyBlurModeWindow());

        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
  
    public static void setBlurModeNone(View view){  
    	//view.setBlurMode(BlurParams.BLUR_MODE_NONE); 
    	try {
            Class<?> mClass = Class.forName("android.view.View");

            Object mObject = view;
            
            Method method = mClass.getMethod("setBlurMode", int.class);

            method.invoke(mObject, getPropertyBlurModeNone());

        } catch (Exception e) {
        }
    }
    
	public static void setBlurRadiusDp(View view, float f) {
		//view.setBlurRadiusDp(f);
		try {
			Class<?> mClass = Class.forName("android.view.View");

			Object mObject = view;

			Method method = mClass.getMethod("setBlurRadiusDp", float.class);

			method.invoke(mObject, f);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setBlurChromaContrast(View view, float f) {
		//view.setBlurChromaContrast(f);
		try {
			Class<?> mClass = Class.forName("android.view.View");

			Object mObject = view;

			Method method = mClass.getMethod("setBlurChromaContrast", float.class);

			method.invoke(mObject, f);

		} catch (Exception e) {
		}
	}
	
	public static void setBlurAlpha(View view, float f) {
		//view.setBlurAlpha(f);
		try {
			Class<?> mClass = Class.forName("android.view.View");

			Object mObject = view;

			Method method = mClass.getMethod("setBlurAlpha", float.class);

			method.invoke(mObject, f);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setBackground(View view, Drawable d) {
		view.setBackground(d);
	}
    
}
