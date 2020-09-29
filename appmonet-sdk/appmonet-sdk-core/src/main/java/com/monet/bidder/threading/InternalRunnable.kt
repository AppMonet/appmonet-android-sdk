package com.monet.bidder.threading

/**
 * Created by jose on 10/4/17.
 */
abstract class InternalRunnable : Runnable {
    override fun run() {
        try {
            runInternal()
        } catch (e: Exception) {
            catchException(e)
        }
    }

    abstract fun runInternal()
    abstract fun catchException(e: Exception?)
}