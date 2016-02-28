package com.nosyjoe.android.common;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import java.util.List;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class Tools {

    private static float scale = -1;

    public static int toDDP(int dip) {
        if (scale < 0) {
            scale = Resources.getSystem().getDisplayMetrics().density;
        }

        return (int) (dip * scale + 0.5f);
    }

    public static boolean isDebuggable(Context context) {
        boolean debug = false;
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null && packageInfo.applicationInfo != null) {
            int flags = packageInfo.applicationInfo.flags;
            if ((flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                debug = true;
            } else
                debug = false;
        }
        return debug;
    }

    public static <T> String join(List<T> itemsToJoin, String delim) {
        StringBuilder sb = new StringBuilder();

        String loopDelim = "";

        for(T item : itemsToJoin) {

            sb.append(loopDelim);
            sb.append(item);

            loopDelim = delim;
        }

        return sb.toString();
    }

    public static <T> String getSqlInList(int count) {
        StringBuilder sb = new StringBuilder();

        String delim = ",";
        String loopDelim = "";

        for(int i = 0; i < count; i++) {

            sb.append(loopDelim);
            sb.append("?");

            loopDelim = delim;
        }

        return sb.toString();
    }

    public static String stackTraceToString(StackTraceElement[] trace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : trace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

}
