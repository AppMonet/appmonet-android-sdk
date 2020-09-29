package com.monet.bidder;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.ConsoleMessage;

class MonetLogger {
    private static final String TAG_BASE = "AppMonet/";
    private static final String LL_DEBUG = "debug";
    private static final String LL_INFO = "info";
    private static final String LL_WARN = "warn";
    private static final String LL_ERROR = "error";
    private static final String LL_LOG = "log";
    private static int sPriority = Log.DEBUG;
    private final String mTag;

    MonetLogger(String tag) {
        mTag = TAG_BASE + tag;
    }

    static void setGlobalLevel(int priority) {
        sPriority = priority;
    }

    static String levelString() {
        switch (sPriority) {
            case Log.DEBUG:
                return LL_DEBUG;
            case Log.INFO:
                return LL_INFO;
            case Log.WARN:
                return LL_WARN;
            case Log.ERROR:
                return LL_ERROR;
            default:
                return LL_LOG;
        }
    }

    private void log(int priority, String[] msg) {
        try {
            if (msg == null || msg.length < 1) {
                return;
            }

            String formattedMsg = TextUtils.join(" ", msg);
            boolean levelActive = sPriority != 0 && priority >= sPriority;

            if (levelActive) {
                Log.println(priority, mTag, formattedMsg);
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    private int consoleLevelToPriority(ConsoleMessage.MessageLevel level, String msg) {
        if (level == ConsoleMessage.MessageLevel.DEBUG || msg.contains(LL_DEBUG)) {
            return Log.DEBUG;
        }
        if (level == ConsoleMessage.MessageLevel.LOG || msg.contains(LL_INFO)) {
            return Log.INFO;
        }
        if (level == ConsoleMessage.MessageLevel.ERROR || msg.contains(LL_ERROR)) {
            return Log.ERROR;
        }
        if (level == ConsoleMessage.MessageLevel.WARNING || msg.contains(LL_WARN)) {
            return Log.WARN;
        }
        return Log.INFO;
    }

    void forward(ConsoleMessage cm, String... msg) {
        if (cm == null || msg == null || msg.length < 1) {
            return;
        }
        log(consoleLevelToPriority(cm.messageLevel(), msg[0]), msg);
    }

    void info(String... msg) {
        log(Log.INFO, msg);
    }

    void error(String... msg) {
        log(Log.ERROR, msg);
    }

    void warn(String... msg) {
        log(Log.WARN, msg);
    }

    void debug(String... msg) {
        log(Log.DEBUG, msg);
    }
}
