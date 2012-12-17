package com.nosyjoe.android.common;

import android.util.Log;

/**
 * Logging helper that simplifies formatting of Log messages and pipes them through
 * android Log
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class NjLog {

    private static StringBuilder sb = new StringBuilder();
    private static String LOG_TAG = "tag_not_set";

    public static void setLogTag(String tag) {
        LOG_TAG = tag;
    }

    public static void v(Object logSource, String message) {
        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) Log.v(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void d(Object logSource, String message) {
        if (Log.isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void i(Object logSource, String message) {
        if (Log.isLoggable(LOG_TAG, Log.INFO)) Log.i(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void w(Object logSource, String message) {
        if (Log.isLoggable(LOG_TAG, Log.WARN)) Log.w(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void e(Object logSource, String message) {
        if (Log.isLoggable(LOG_TAG, Log.ERROR)) Log.e(LOG_TAG, getFormattedMessage(logSource, message));
    }

    private static String getFormattedMessage(Object logSource, String message) {
        StringBuffer sb = new StringBuffer("[");
        // class name
        sb.append(logSource.getClass().getSimpleName()).append("] ");
        // thread id
        sb.append("(").append(Thread.currentThread().getId()).append(") ");
        sb.append(message);

        return sb.toString();
    }

}
