package com.android.systemui;

import java.lang.reflect.Method;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

public class YLOpenUtils {
	private static final String LAUNCHER_PACKAGE_NAME = "com.yulong.android.launcherL";
    private static final String RESOURCE_TYPE_COLOR = "color";
    private static final String RESOURCE_ID_WORKSPACE_ICON_TEXT_COLOR = "workspace_icon_text_color";
    private static final String RESOURCE_ID_WORKSPACE_ICON_DARK_TEXT_COLOR = "workspace_icon_dark_text_color";
    public static final int BRIGHTNESS_THRESHOLD = 210;    
    private static final int GRADIENT_STEP = 4; 
    private static final int METHOD_STANDARD = 1;
    private static final int METHOD_RGB2XYZ_LED = 2;        //sRGB (LED)
    private static final int METHOD_RGB2XYZ_AMOLED = 3;     //Adobe RGB (AMOLED)
    private static final float SHADOW_LAYER_RADIUS = 10f;
    private static int currentDarkTextColor = Color.BLACK;
    
    private static int getAverageBrightness(int method, int points, long sumRed, long sumGreen, long sumBlue)
    {
        int averageBrightness;
        
        switch (method)
        {
            case METHOD_RGB2XYZ_LED:
                averageBrightness = (int) ((sumRed * 0.2126729 + sumGreen * 0.7151522 + sumBlue * 0.0721750) / points);
                break;
            case METHOD_RGB2XYZ_AMOLED:
                averageBrightness = (int) ((sumRed *  0.2973769 + sumGreen * 0.6273491 + sumBlue *  0.0752741) / points);
                break;
            default:
                averageBrightness = (int) ((sumRed * 0.299 + sumGreen * 0.587 + sumBlue * 0.114) / points);
                break;
        }
        
        return averageBrightness;
    }

    public static int getBrightness(Bitmap bmp, Rect rect, int points, int [] averageColor, long [] costTime) {
        long startTime = System.currentTimeMillis();
        int averageBrightness = 0;
        try {
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();
            Rect fence = rect;
            if (rect == null) {
                fence = new Rect(0, 0, bmpWidth, bmpHeight);
            }
            
            
            if (fence.left < 0) {
                fence.left = 0;
            }

            if (fence.right > bmpWidth) {
                fence.right = bmpWidth;
            }

            if (fence.top < 0) {
                fence.top = 0;
            }

            if (fence.bottom > bmpHeight) {
                fence.bottom = bmpHeight;
            }
            
            int fenceWidth = fence.width();
            int fenceHeight = fence.height();
            int fenceDimension = fenceWidth * fenceHeight;
            
            int selectedPoints = fenceDimension < points ? fenceDimension : points;

            final int vPoints = (int) Math.sqrt((fenceHeight * (double)selectedPoints) / fenceWidth);

            final int hPoints = (int) (fenceWidth * (float)vPoints / fenceHeight);
            
            selectedPoints = hPoints * vPoints;
            
            int pixel;
            long sumR = 0;
            long sumG = 0;
            long sumB = 0;
            
            final int hDistance = fenceWidth / hPoints; 
            final int vDistance = fenceHeight / vPoints; 
            final int hOffset = hDistance >> 1; 
            final int vOffset = vDistance >> 1; 
            
            int shift; 
            for (int i = 0; i < hPoints; i++)
            {
                for (int j = 0; j < vPoints; j++)
                {
                    shift = (i+j) & 1; 
                    pixel = bmp.getPixel(hOffset + i * hDistance  + shift, vOffset + j * vDistance  + shift);
                    sumR += Color.red(pixel);
                    sumG += Color.green(pixel);
                    sumB += Color.blue(pixel);
                }
            }
    
            if (averageColor != null && averageColor.length > 0) {
                averageColor[0] = Color.rgb((int)((float)sumR/selectedPoints), (int)((float)sumG/selectedPoints), (int)((float)sumB/selectedPoints));
            }
            
            /*
            http://www.easyrgb.com/index.php?X=MATH
            http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
            ----------------------------------------------------------------
            dark theme (0 <= Y <= 180) text color #FFFFFFFF
            light theme (180 < Y < 255) text color #DE000000
            */
            averageBrightness = getAverageBrightness(METHOD_RGB2XYZ_LED, selectedPoints, sumR, sumG, sumB);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (costTime != null && costTime.length > 0) {
            costTime[0] = System.currentTimeMillis() - startTime;
        }
        return averageBrightness;
    }
    
    public static int getTextColor(int brightness, int color) {
        int textColor = Color.WHITE;
        int color1 = color & 0x00ffffff;
        boolean b = color1 < 0x7f7f7f;
        if (brightness >= BRIGHTNESS_THRESHOLD) {
            
        	textColor =0x5c000000;
        } else {
            textColor = Color.WHITE;
        }
        return textColor;
    }

    private static int getDarkColor(int inColor) {
//	        int color = Color.BLACK;
//	        float [] HSL = new float[3];
//	        rgb2HSL(inColor, HSL);
//	        HSL[2] = 0.05f; 
//	        color = hsl2RGB(HSL);
//	        return color;
        int color = Color.BLACK;
        float [] HSV = new float[3];
        Color.colorToHSV(color, HSV);
        HSV[1] = HSV[2] = 0.3f; 
        color = Color.HSVToColor(HSV);
        return color;

    }
    
    public static int getBrightnessThreshold() {
        return BRIGHTNESS_THRESHOLD;
    }
    
    private static int hsl2RGB(float[] HSL) {
        float R,G,B, H = HSL[0], S = HSL[1], L = HSL[2];
        float var_1, var_2;
        if (S == 0) {
            R = L * 255.0f;
            G = L * 255.0f;
            B = L * 255.0f;
        } else {
            if (L < 0.5f) {
                var_2 = L * (1 + S);
            } else {
                var_2 = (L + S) - (S * L);
            }

            var_1 = 2.0f * L - var_2;

            R = 255.0f * hue2RGB(var_1, var_2, H + (1.0f / 3.0f));
            G = 255.0f * hue2RGB(var_1, var_2, H);
            B = 255.0f * hue2RGB(var_1, var_2, H - (1.0f / 3.0f));
        }
        
        return Color.argb(230, (int)R, (int)G, (int)B); //alpha 90%
    }

    private static float hue2RGB(float v1, float v2, float vH) {
        if (vH < 0) {
            vH += 1;
        } else if (vH > 1) {
            vH -= 1;
        }
        if (6.0f * vH < 1) {
            return v1 + (v2 - v1) * 6.0f * vH;
        }
        if (2.0f * vH < 1) {
            return v2;
        }
        if (3.0f * vH < 2) {
            return v1 + (v2 - v1) * ((2.0f / 3.0f) - vH) * 6.0f;
        }
        return v1;
    }

    //hukun findbugs deadcode
//	    private static void rgb2HSL(int averageColor, float[] HSL) {
//	        float R,G,B,Max,Min,del_R,del_G,del_B,del_Max, H = 0f, S = 0f , L = 0f;
//	        R = Color.red(averageColor) / 255.0f;
//	        G = Color.green(averageColor) / 255.0f;
//	        B = Color.blue(averageColor) / 255.0f;
//
//	        Min = Math.min(R, Math.min(G, B));    //Min. value of RGB
//	        Max = Math.max(R, Math.max(G, B));    //Max. value of RGB
//	        del_Max = Max - Min;        //Delta RGB value
//
//	        L = (Max + Min) / 2.0f;
//
//	        if (del_Max == 0) {           //This is a gray, no chroma...
//	            //H = 2.0/3.0;          
//	            H = 0;
//	            S = 0;
//	        }
//	        else {                       //Chromatic data...
//	            if (L < 0.5) {
//	                S = del_Max / (Max + Min);
//	            } else {
//	                S = del_Max / (2 - Max - Min);
//	            }
//
//	            del_R = (((Max - R) / 6.0f) + (del_Max / 2.0f)) / del_Max;
//	            del_G = (((Max - G) / 6.0f) + (del_Max / 2.0f)) / del_Max;
//	            del_B = (((Max - B) / 6.0f) + (del_Max / 2.0f)) / del_Max;
//
//	            if (R == Max) {
//	                H = del_B - del_G;
//	            } else if (G == Max) {
//	                H = (1.0f / 3.0f) + del_R - del_B;
//	            } else if (B == Max) {
//	                H = (2.0f / 3.0f) + del_G - del_R;
//	            }
//
//	            if (H < 0) {
//	                H += 1;
//	            } else if (H > 1) {
//	                H -= 1;
//	            }
//	        }
//	    }
    
    public static void forgetLoadedAllWallpaper(WallpaperManager wm) {
        if (wm != null) {
            try {
//	                Method method = WallpaperManager.class.getMethod("forgetLoadedAllWallpaper");
                Method method = WallpaperManager.class.getMethod("forgetLoadedWallpaper");
                method.invoke(wm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
