package com.monet.bidder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is responsible of managing the communication between Publishers and Subscribers.
 */
public class PubSubService {
    private final static Logger sLogger = new Logger("PubSubService");
    //Keeps set of subscriber topic wise, using set to prevent duplicates
    private Map<String, Set<Subscriber>> subscribersTopicMap = new ConcurrentHashMap<>();
    //Holds messages published by publishers
    private ConcurrentLinkedQueue<MonetPubSubMessage> messagesQueue = new ConcurrentLinkedQueue<>();

    /**
     * This method adds the message sent by publisher to a queue.
     *
     * @param message The message sent by the publisher
     */
    public synchronized void addMessageToQueue(MonetPubSubMessage message) {
        messagesQueue.add(message);
    }

    /**
     * This method adds a new Subscriber for a topic.
     *
     * @param topic      The topic to subscribe to.
     * @param subscriber The Subscriber reference to register.
     */
    public synchronized void addSubscriber(String topic, Subscriber subscriber) {
        if (subscribersTopicMap.containsKey(topic)) {
            Set<Subscriber> subscribers = subscribersTopicMap.get(topic);
            subscribers = (subscribers != null) ? subscribers : new HashSet<Subscriber>();
            subscribers.add(subscriber);
            subscribersTopicMap.put(topic, subscribers);
        } else {
            Set<Subscriber> subscribers = new HashSet<>();
            subscribers.add(subscriber);
            subscribersTopicMap.put(topic, subscribers);
        }
    }

    /**
     * This method removes an existing subscriber for a topic.
     *
     * @param topic      The topic to unsubscribe to.
     * @param subscriber The Subscriber reference to remove.
     */
    synchronized void removeSubscriber(String topic, Subscriber subscriber) {
        if (subscribersTopicMap.containsKey(topic)) {
            Set<Subscriber> subscribers = subscribersTopicMap.get(topic);
            if(subscribers != null) {
                subscribers.remove(subscriber);
                subscribersTopicMap.put(topic, subscribers);
            }
        }
    }

    /**
     * This method broadcasts new messages added in queue to All subscribers of the topic. The
     * messageQueue will be emptied after the broadcast.
     */
    public synchronized void broadcast() {
        if (messagesQueue.isEmpty()) {
            sLogger.debug("No messages from publishers to display");
        } else {
            while (!messagesQueue.isEmpty()) {
                MonetPubSubMessage message = messagesQueue.remove();
                String topic = message.topic;
                sLogger.debug("Message Topic -> " + topic);
                Set<Subscriber> subscribersOfTopic = subscribersTopicMap.get(topic);
                if(subscribersOfTopic != null) {
                    for (Subscriber subscriber : subscribersOfTopic) {
                        //add broadcasted message to subscribers message queue
                        subscriber.onBroadcast(message);
                    }
                }
            }
        }
    }
}