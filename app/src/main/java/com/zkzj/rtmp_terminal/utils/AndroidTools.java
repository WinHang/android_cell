package com.zkzj.rtmp_terminal.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.Display;
import android.view.WindowManager;

public class AndroidTools {

    /**
     * 判断横竖屏
     * @param activity
     * @return 1：竖 | 0：横
     */
    public static int ScreenOrient(Activity activity)
    {
        int orient = activity.getRequestedOrientation();
        if(orient != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && orient != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            WindowManager windowManager = activity.getWindowManager();
            Display display = windowManager.getDefaultDisplay();
            int screenWidth  = display.getWidth();
            int screenHeight = display.getHeight();
            orient = screenWidth < screenHeight ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        return orient;
    }
}