package com.monet.bidder;

/**
 * This interface provides a blueprint for handling messages getting broadcasted.
 */
public interface Subscriber {

    /**
     * This method is called by {@link PubSubService} when a message is broadcasted.
     *
     * @param subscriberMessages The message getting broadcasted.
     */
    void onBroadcast(MonetPubSubMessage subscriberMessages);
}