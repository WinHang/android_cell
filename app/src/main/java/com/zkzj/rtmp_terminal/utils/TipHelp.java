package com.zkzj.rtmp_terminal.utils;

import android.app.Activity;
import android.app.Service;
import android.os.Vibrator;

import com.zkzj.rtmp_terminal.rtmp.MainActivity;

public class TipHelp {
    /*
        *  final Activity activity ：调用该方法的Activity实例

        long milliseconds ：震动的时长，单位是毫秒

        long[] pattern ：自定义震动模式 。数组中数字的含义依次是[静止时长，震动时长，静止时长，震动时长。。。]时长的单位是毫秒

        boolean isRepeat ： 是否反复震动，如果是true，反复震动，如果是false，只震动一次
        TipHelper.Vibrate(getActivity(), new long[]{800, 1000, 800, 1000, 800, 1000}, true）;
        */
    private static Vibrator vib;

    public static void Vibrate(final Activity activity, long milliseconds) {
        vib = (Vibrator) activity.getSystemService(Service.VIBRATOR_SERVICE);
        vib.vibrate(milliseconds);
    }

    public static void Vibrate(final Activity activity, long[] pattern, boolean isRepeat) {
        vib = (Vibrator) activity.getSystemService(Service.VIBRATOR_SERVICE);
        vib.vibrate(pattern, isRepeat ? 1 : -1);
    }

    public static void Vibrate(MainActivity activity, long[] pattern, boolean isRepeat) {
        vib = (Vibrator) activity.getSystemService(Service.VIBRATOR_SERVICE);
        vib.vibrate(pattern, isRepeat ? 1 : -1);

    }

    /**
     *          * 停止震动
     *         
     */
    public static void stop() {
        vib.cancel();
    }

    //取消震动
    public static void virateCancle(final Activity activity) {
        Vibrator vib = (Vibrator) activity.getSystemService(Service.VIBRATOR_SERVICE);
        vib.cancel();
    }
}
