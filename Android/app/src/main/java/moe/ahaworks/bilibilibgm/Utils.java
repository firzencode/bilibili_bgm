package moe.ahaworks.bilibilibgm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by firzencode on 15/7/28.
 */
public class Utils {
    public static boolean sLogEnable = true;

    public static void LOGD(String tag, String log) {
        if (sLogEnable)
            Log.d(tag, log);
    }

    public static void LOGE(String tag, String log) {
        if (sLogEnable)
            Log.e(tag, log);
    }

    public static String ConvertTime(long ms) {
        long second = ms / 1000;
        long minute = second / 60;
        long secondRemain = second - minute * 60;

        String minuteZero = "";
        String secondZero = "";

        if (minute < 10) {
            minuteZero = "0";
        }

        if (secondRemain < 10) {
            secondZero = "0";
        }

        return "" + minuteZero + minute + ":" + secondZero + secondRemain;
    }

    public static String getVertsionInfo(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return "此处应有版本号";
        }
    }
}
