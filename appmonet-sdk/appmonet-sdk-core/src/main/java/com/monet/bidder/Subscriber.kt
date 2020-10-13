package com.monet.bidder

/**
 * This interface provides a blueprint for handling messages getting broadcasted.
 */
interface Subscriber {
  /**
   * This method is called by [PubSubService] when a message is broadcasted.
   *
   * @param subscriberMessages The message getting broadcasted.
   */
  fun onBroadcast(subscriberMessages: MonetPubSubMessage?)
}