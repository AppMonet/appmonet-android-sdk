package com.monet

class Greeting {
  fun greeting(): String {
    return "Hello, ${Platform().platform}!"
  }
}