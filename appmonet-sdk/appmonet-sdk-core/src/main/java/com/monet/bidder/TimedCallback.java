package com.monet.bidder;

public interface  TimedCallback{
  void execute(int remainingTime);

  void timeout();
}
