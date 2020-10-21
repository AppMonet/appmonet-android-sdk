package com.monet.threading

typealias ThreadRunnable = () -> Unit

//expect abstract class ThreadRunnable {
//  actual abstract fun runInternal()
//  actual abstract fun catchException(e: Exception?)
//}