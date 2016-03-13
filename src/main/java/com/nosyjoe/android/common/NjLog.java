package com.nosyjoe.android.common;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Logging helper that simplifies formatting of Log messages and pipes them through
 * android Log
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public class NjLog {
    
    public enum LogLevel {
        verbose,
        debug,
        info,
        warning,
        error
    }
    
    private static List<Listener> logListeners = new ArrayList<>(Collections.singleton((Listener) new SysListener()));
    private static String LOG_TAG = "tag_not_set";
    private static boolean isDebuggable;

    public static void setLogTag(String tag) {
        LOG_TAG = tag;
    }

    public static void setDebuggable(boolean nuIsDebuggable) {
        isDebuggable = nuIsDebuggable;
    }
    
    public static void addLogListener(Listener l) {
        if (l == null) {
            throw new NullPointerException("listener can't be null");
        }
        logListeners.add(l);
    }

    public static void v(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.VERBOSE)) {
            logAll(LogLevel.verbose, LOG_TAG, getFormattedMessage(logSource, message));
        }
    }

    public static void v(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.VERBOSE)) logAll(LogLevel.verbose, LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void d(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.DEBUG))
            logAll(LogLevel.debug, LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void d(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.DEBUG))
            logAll(LogLevel.debug, LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void i(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.INFO)) logAll(LogLevel.info, LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void i(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.INFO)) logAll(LogLevel.info, LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void w(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.WARN)) logAll(LogLevel.warning, LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void w(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.WARN)) logAll(LogLevel.warning, LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    public static void e(Object logSource, String message) {
        if (isLoggable(LOG_TAG, Log.ERROR)) logAll(LogLevel.error, LOG_TAG, getFormattedMessage(logSource, message));
    }

    public static void e(Object logSource, String message, Exception e) {
        if (isLoggable(LOG_TAG, Log.ERROR)) logAll(LogLevel.error, LOG_TAG, getFormattedMessage(logSource, message), e);
    }

    private static void logAll(LogLevel level, String logTag, String formattedMessage) {
        for (Listener l : logListeners) {
            l.log(level, logTag, formattedMessage, null);
        }
    }

    private static void logAll(LogLevel level, String logTag, String formattedMessage, Exception e) {
        for (Listener l : logListeners) {
            l.log(level, logTag, formattedMessage, e);
        }
    }
    
    private static String getFormattedMessage(Object logSource, String message) {
        StringBuilder sb = new StringBuilder("[");
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

    public interface Listener {
        
        void log(LogLevel level, String tag, String text);
        
        void log(LogLevel level, String tag, String text, Exception e);
        
    }
    
    private static class SysListener implements Listener {
        
        @Override
        public void log(LogLevel level, String tag, String text) {
            switch (level) {
                case verbose:
                    Log.v(tag, text);
                    break;
                case debug:
                    Log.d(tag, text);
                    break;
                case info:
                    Log.i(tag, text);
                    break;
                case warning:
                    Log.w(tag, text);
                    break;
                case error:
                    Log.e(tag, text);
                    break;
                default:
            }
        }

        @Override
        public void log(LogLevel level, String tag, String text, Exception e) {
            switch (level) {
                case verbose:
                    Log.v(tag, text, e);
                    break;
                case debug:
                    Log.d(tag, text, e);
                    break;
                case info:
                    Log.i(tag, text, e);
                    break;
                case warning:
                    Log.w(tag, text, e);
                    break;
                case error:
                    Log.e(tag, text, e);
                    break;
                default:
            }
        }
    }
}
