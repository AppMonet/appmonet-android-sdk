package com.monet.bidder.threading

import androidx.annotation.VisibleForTesting
import java.util.concurrent.*

class BackgroundThread {
    private val service: ScheduledThreadPoolExecutor

    constructor() {
        this.service = ScheduledThreadPoolExecutor(5)
    }

    @VisibleForTesting
    constructor(service: ScheduledThreadPoolExecutor) {
        this.service = service
    }

    fun execute(runnable: Runnable): Future<*>? {
        return service.submit(runnable)
    }

    fun <T> submit(runnable: Callable<T>): Future<T>? {
        return service.submit(runnable)
    }

    fun submit(runnable: Runnable): Future<*>? {
        return service.submit(runnable)
    }

    fun scheduleAtFixedRate(runnable: Runnable, interval: Long, unit: TimeUnit): ScheduledFuture<*>? {
        return service.scheduleAtFixedRate(runnable, 0, interval, unit)
    }
}