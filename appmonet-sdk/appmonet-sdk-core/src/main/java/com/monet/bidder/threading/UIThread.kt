package com.monet.bidder.threading

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting

class UIThread {
    private var handler: Handler

    @VisibleForTesting
    constructor(handler: Handler) {
        this.handler = handler
    }

    constructor() {
        this.handler = Handler(Looper.getMainLooper())
    }

    fun run(runnable: Runnable) {
        this.handler.post(runnable)
    }

    fun runDelayed(runnable: Runnable, delay: Long) {
        this.handler.postDelayed(runnable, delay)
    }

    fun removeCallbacks(runnable: Runnable){
       this.handler.removeCallbacks(runnable)
    }
}