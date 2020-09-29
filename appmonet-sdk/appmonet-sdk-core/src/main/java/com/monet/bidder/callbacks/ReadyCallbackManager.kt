package com.monet.bidder.callbacks

import com.monet.bidder.Logger
import java.util.concurrent.atomic.AtomicBoolean

internal class ReadyCallbackManager<T> {
    private val sLogger: Logger = Logger("ReadyCallbackManager")
    private val readyCallbacks: MutableList<(T) -> Unit>
    private val isReady = AtomicBoolean(false)
    private var instance: T? = null

    init {
        readyCallbacks = ArrayList()
    }

    @Synchronized
    fun executeReady(instance: T) {
        this.instance = instance
        isReady.set(true)
        if (readyCallbacks.isEmpty()) {
            return
        }
        for (cb in readyCallbacks) {
            try {
                cb(instance)
            } catch (e: Exception) {
                sLogger.warn("error in callback queue: ", e.message)
            }
        }
        readyCallbacks.clear()
    }

    @Synchronized
    fun onReady(callback: (T) -> Unit) {
        if (isReady.get()) {
            try {
                instance?.let { callback(it) }
            } catch (e: Exception) {
                sLogger.warn("error in onready:", e.message)
            }
            return
        }
        readyCallbacks.add(callback)
    }

    @Synchronized
    fun onReadySync(): T? {
        if (isReady.get()) {
            try {
                return instance
            } catch (e: Exception) {
                sLogger.warn("error in onreadysync", e.message)
            }
        }
        return null
    }
}