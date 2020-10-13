package com.monet.bidder

import android.text.TextUtils
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ConsoleMessage.MessageLevel
import android.webkit.ConsoleMessage.MessageLevel.DEBUG
import android.webkit.ConsoleMessage.MessageLevel.ERROR
import android.webkit.ConsoleMessage.MessageLevel.LOG
import android.webkit.ConsoleMessage.MessageLevel.WARNING

class Logger(tag: String) {
  private val mTag: String
  @Synchronized private fun log(
    priority: Int,
    msg: Array<out String?>
  ) {
    try {
      if (msg == null || msg.isEmpty()) {
        return
      }
      val formattedMsg = TextUtils.join(" ", msg)
      val levelActive = sPriority != 0 && priority >= sPriority
      if (levelActive) {
        Log.println(priority, mTag, formattedMsg)
      }
    } catch (e: Exception) {
      // do nothing
    }
  }

  private fun consoleLevelToPriority(
    level: MessageLevel,
    msg: String
  ): Int {
    if (level == DEBUG || msg.contains(LL_DEBUG)) {
      return Log.DEBUG
    }
    if (level == LOG || msg.contains(LL_INFO)) {
      return Log.INFO
    }
    if (level == ERROR || msg.contains(LL_ERROR)) {
      return Log.ERROR
    }
    return if (level == WARNING || msg.contains(
            LL_WARN
        )
    ) {
      Log.WARN
    } else Log.INFO
  }

  fun forward(
    cm: ConsoleMessage?,
    vararg msg: String
  ) {
    if (cm == null || msg == null || msg.size < 1) {
      return
    }
    log(consoleLevelToPriority(cm.messageLevel(), msg[0]), msg)
  }

  fun info(vararg msg: String?) {
    log(Log.INFO, msg)
  }

  fun error(vararg msg: String?) {
    log(Log.ERROR, msg)
  }

  fun warn(vararg msg: String?) {
    log(Log.WARN, msg)
  }

  fun debug(vararg msg: String?) {
    log(Log.DEBUG, msg)
  }

  companion object {
    private const val TAG_BASE = "AppMonet/"
    private const val LL_DEBUG = "debug"
    private const val LL_INFO = "info"
    private const val LL_WARN = "warn"
    private const val LL_ERROR = "error"
    private const val LL_LOG = "log"
    private var sPriority = Constants.LOG_LEVEL
    @Synchronized fun setGlobalLevel(priority: Int) {
      sPriority = priority
    }

    @Synchronized fun levelString(): String {
      return when (sPriority) {
        Log.DEBUG -> LL_DEBUG
        Log.INFO -> LL_INFO
        Log.WARN -> LL_WARN
        Log.ERROR -> LL_ERROR
        else -> LL_LOG
      }
    }
  }

  init {
    mTag = TAG_BASE + tag
  }
}