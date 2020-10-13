package com.monet.bidder

import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This class is responsible of managing the communication between Publishers and Subscribers.
 */
class PubSubService {
  //Keeps set of subscriber topic wise, using set to prevent duplicates
  private val subscribersTopicMap: MutableMap<String, MutableSet<Subscriber>?> = ConcurrentHashMap()

  //Holds messages published by publishers
  private val messagesQueue = ConcurrentLinkedQueue<MonetPubSubMessage>()

  /**
   * This method adds the message sent by publisher to a queue.
   *
   * @param message The message sent by the publisher
   */
  @Synchronized fun addMessageToQueue(message: MonetPubSubMessage) {
    messagesQueue.add(message)
  }

  /**
   * This method adds a new Subscriber for a topic.
   *
   * @param topic      The topic to subscribe to.
   * @param subscriber The Subscriber reference to register.
   */
  @Synchronized fun addSubscriber(
    topic: String,
    subscriber: Subscriber
  ) {
    if (subscribersTopicMap.containsKey(topic)) {
      var subscribers = subscribersTopicMap[topic]
      subscribers = subscribers ?: HashSet()
      subscribers.add(subscriber)
      subscribersTopicMap[topic] = subscribers
    } else {
      val subscribers: MutableSet<Subscriber> = HashSet()
      subscribers.add(subscriber)
      subscribersTopicMap[topic] = subscribers
    }
  }

  /**
   * This method removes an existing subscriber for a topic.
   *
   * @param topic      The topic to unsubscribe to.
   * @param subscriber The Subscriber reference to remove.
   */
  @Synchronized fun removeSubscriber(
    topic: String,
    subscriber: Subscriber
  ) {
    if (subscribersTopicMap.containsKey(topic)) {
      val subscribers = subscribersTopicMap[topic]
      if (subscribers != null) {
        subscribers.remove(subscriber)
        subscribersTopicMap[topic] = subscribers
      }
    }
  }

  /**
   * This method broadcasts new messages added in queue to All subscribers of the topic. The
   * messageQueue will be emptied after the broadcast.
   */
  @Synchronized fun broadcast() {
    if (messagesQueue.isEmpty()) {
      sLogger.debug("No messages from publishers to display")
    } else {
      while (!messagesQueue.isEmpty()) {
        val message = messagesQueue.remove()
        val topic = message.topic
        sLogger.debug("Message Topic -> $topic")
        val subscribersOfTopic: Set<Subscriber>? = subscribersTopicMap[topic]
        if (subscribersOfTopic != null) {
          for (subscriber in subscribersOfTopic) {
            //add broadcasted message to subscribers message queue
            subscriber.onBroadcast(message)
          }
        }
      }
    }
  }

  companion object {
    private val sLogger = Logger("PubSubService")
  }
}