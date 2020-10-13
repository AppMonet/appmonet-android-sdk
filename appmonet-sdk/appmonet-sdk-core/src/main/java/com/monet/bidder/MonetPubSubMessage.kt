package com.monet.bidder

/**
 * This class is a wrapper for the information getting passed between Publishers and Subscribers.
 */
data class MonetPubSubMessage(
  val topic: String,
  val payload: Any?
)