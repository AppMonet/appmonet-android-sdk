package com.monet.bidder;

/**
 * This class is a wrapper for the information getting passed between Publishers and Subscribers.
 */
public class MonetPubSubMessage {
    public final String topic;
    public final Object payload;

    public MonetPubSubMessage(String topic, Object payload) {
        this.topic = topic;
        this.payload = payload;
    }
}