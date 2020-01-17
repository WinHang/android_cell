package com.zkzj.rtmp_terminal;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;
import com.zkzj.rtmp_terminal.service.FloatingWindowService;
import com.zkzj.rtmp_terminal.utils.DialogUIUtils;

import org.easydarwin.easypusher.EasyApplication;

/**
 * Author:maxuesong
 * Created on 2018/7/18
 */
public class TerminalApp extends EasyApplication {
    private static TerminalApp app;


    public int count = 0;

    //判断是否推流
    private static boolean isBackground = false;
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        DialogUIUtils.init(this);
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)
                .methodCount(2)
                .methodOffset(7)
                .tag("thidom")
                .build();
        app = this;
        AndroidLogAdapter adapter = new AndroidLogAdapter(formatStrategy) {
            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return BuildConfig.DEBUG;
            }
        };
        Logger.addLogAdapter(adapter);

        /*来监听前后端的变换*/
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStopped(Activity activity) {
                Log.v("viclee", activity + "onActivityStopped");
                count--;
                if (count == 0) {
                    Log.e("viclee", ">>>>>>>>>>>>>>>>>>>切到后台  lifecycle");
                    FloatingWindowService.isBackGroud=false;
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.e("viclee", activity + "onActivityStarted");
                if (count == 0) {
                    Log.e("viclee", ">>>>>>>>>>>>>>>>>>>切到前台  lifecycle");
                    FloatingWindowService.isBackGroud=true;
                }
                count++;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }
        });


    }

    public static Application getApp() {
        return app;
    }

    public static void setBackground(boolean background) {
        isBackground = background;
    }

    public static boolean isBackground() {
        return isBackground;
    }

    /**
     * 获取全局上下文
     */
    public static Context getContext() {
        return mContext;
    }


}
