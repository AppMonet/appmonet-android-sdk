package com.monet.bidder

/**
 * This class is a wrapper for the information getting passed between Publishers and Subscribers.
 */
data class MonetPubSubMessage(
  @JvmField val topic: String,
  @JvmField val payload: Any?
)