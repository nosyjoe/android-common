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
    private static boolean isDebuggable;

    public static void setLogTag(String tag) {
        LOG_TAG = tag;
    }

    public static void setDebuggable(boolean nuIsDebuggable) {
        isDebuggable = nuIsDebuggable;
    }

    public static void v(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.VERBOSE)) Log.v(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void v(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.VERBOSE)) Log.v(LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void d(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void d(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void i(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.INFO)) Log.i(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void i(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.INFO)) Log.i(LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void w(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.WARN)) Log.w(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void w(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.WARN)) Log.w(LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void e(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.ERROR)) Log.e(LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void e(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.ERROR)) Log.e(LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    private static String getFormattedMessage(Object logSource, String message) {
        StringBuffer sb = new StringBuffer("[");
        // class name
        String simpleName;
        if (logSource instanceof String) {
            simpleName = (String) logSource;
        } else if (Class.class.getName().equals(logSource.getClass().getName())) {
            simpleName = ((Class)logSource).getSimpleName();
        } else {
            simpleName = logSource.getClass().getSimpleName();
        }
        sb.append(simpleName).append("] ");
        // thread id
        sb.append("(").append(Thread.currentThread().getId()).append(") ");
        sb.append(message);

        return sb.toString();
    }

    private static boolean isDebuggable() {
        return isDebuggable;
    }

    private static boolean isLoggable(String tag, int level) {
        if (isDebuggable()) {
            if (level != Log.VERBOSE) return true;
        }
        return Log.isLoggable(tag, level);
    }

}
