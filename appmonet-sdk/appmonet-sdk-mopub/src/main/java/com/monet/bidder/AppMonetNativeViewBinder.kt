package com.monet.bidder

import java.util.HashMap

class AppMonetNativeViewBinder private constructor(builder: Builder) {
  val layoutId: Int
  val mediaLayoutId: Int
  val titleId: Int
  val textId: Int
  val callToActionId: Int
  val iconId: Int
  val extras: Map<String, Int>

  class Builder(internal val layoutId: Int) {
    internal var mediaLayoutId = 0
    internal var titleId = 0
    internal var textId = 0
    internal var iconId = 0
    internal var callToActionId = 0
    internal var extras = mutableMapOf<String, Int>()
    fun mediaLayoutId(mediaLayoutId: Int) = apply { this.mediaLayoutId = mediaLayoutId }

    fun titleId(titleId: Int) = apply { this.titleId = titleId }

    fun iconId(iconId: Int) = apply { this.iconId = iconId }

    fun textId(textId: Int) = apply { this.textId = textId }

    fun addExtras(resourceIds: Map<String, Int>?) = apply { this.extras = HashMap(resourceIds!!) }

    fun addExtra(
      key: String,
      resourceId: Int
    ) = apply { this.extras[key] = resourceId }

    fun callToActionId(callToActionId: Int) = apply {
      this.callToActionId = callToActionId
    }

    fun build() = AppMonetNativeViewBinder(this)
  }

  init {
    layoutId = builder.layoutId
    mediaLayoutId = builder.mediaLayoutId
    titleId = builder.titleId
    textId = builder.textId
    callToActionId = builder.callToActionId
    iconId = builder.iconId
    extras = builder.extras
  }
}