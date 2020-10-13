package com.monet.bidder

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.monet.bidder.InterstitialAdapter.InterstitialViewHolder
import com.monet.bidder.InterstitialData.InterstitialContent.Media
import com.monet.bidder.MonetVideoView.VideoListener
import java.io.IOException
import java.net.URL
import java.util.ArrayList

/**
 * Created by jose on 4/6/18.
 */
internal class InterstitialAdapter(
  private val interstitialData: InterstitialData,
  analyticsTracker: InterstitialAnalyticsTracker,
  listener: InterstitialVideoHolderEvents
) : Adapter<InterstitialViewHolder>() {
  private val holders: MutableList<InterstitialViewHolder>
  private val analyticsTracker: InterstitialAnalyticsTracker
  private var viewHolderBinded = false
  private val listener: InterstitialVideoHolderEvents
  private var recyclerView: RecyclerView? = null
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): InterstitialViewHolder {
    val contentView = InterstitialContentView(parent.context)
    return InterstitialViewHolder(contentView)
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    this.recyclerView = recyclerView
  }

  override fun onBindViewHolder(
    interstitialViewHolder: InterstitialViewHolder,
    position: Int
  ) {
    interstitialViewHolder.videoView.videoId = interstitialData.content[position].id()
    if (interstitialData.content[position].media() != null) {
      interstitialData.content[position].media()?.let { media ->
        interstitialViewHolder.videoView.videoUrl = media.source()
        interstitialViewHolder.thumbnailUrl = media.thumbnail()
        interstitialViewHolder.setContentViewDimensions(media)
        interstitialViewHolder.setContentPadding(position, media)
      }

    }
    interstitialViewHolder.contentTitle.text = interstitialData.content[position].title()
    interstitialViewHolder.contentView.setTitleContent(
        interstitialData.content[position].title()
    )
    if (position == 0) {
      if (!viewHolderBinded) {
        interstitialViewHolder.contentView.removeDarkView()
        interstitialViewHolder.videoView.trackVideoAttached()
      }
      interstitialViewHolder.videoView.focused = !viewHolderBinded
    }
    viewHolderBinded = true
  }

  override fun getItemCount(): Int {
    return interstitialData.content.size
  }

  override fun onViewAttachedToWindow(holder: InterstitialViewHolder) {
    super.onViewAttachedToWindow(holder)
    holder.videoView.loadVideo()
    holder.setVideoThumbnail()
    holders.add(holder)
  }

  override fun onViewDetachedFromWindow(holder: InterstitialViewHolder) {
    holders.remove(holder)
    holder.isFocused = false
    holder.isBuffered = false
    if (holder.videoView.isPlaying) {
      holder.videoView.stopPlayback()
    }
    holder.videoView.setOnPreparedListener(null)
  }

  fun playVideoInPosition(snapPosition: Int) {
    for (holder in holders) {
      if (holder.layoutPosition == snapPosition) {
        holder.videoView.trackVideoAttached()
        holder.videoView.focused = true
        holder.videoView.playVideo()
      } else if (holder.videoView.isFocused) {
        holder.videoView.trackVideoDetached()
        holder.videoView.resetVideo()
      }
    }
  }

  fun cleanup() {
    for (holder in holders) {
      if (holder.videoView.isFocused) {
        holder.videoView.resetVideo()
      }
    }
    holders.clear()
  }

  internal inner class InterstitialViewHolder(itemView: View) : ViewHolder(itemView),
      OnClickListener,
      VideoListener {
    private val thumbnail: ImageView
    val contentTitle: TextView
    var videoView: MonetVideoView
    var thumbnailDrawable: BitmapDrawable? = null
    var thumbnailUrl: String? = null
    var dimView: View
    var contentView: InterstitialContentView
    var isBuffered = false
    var isFocused = false
    var videoId: String? = null
    var impressionId: String? = null
    fun setVideoThumbnail() {
      if (thumbnailDrawable == null) {
        AsyncTask.execute {
          try {
            thumbnailDrawable = BitmapDrawable(
                recyclerView!!.context
                    .resources, BitmapFactory.decodeStream(
                URL(thumbnailUrl)
                    .openConnection().getInputStream()
            )
            )
            val handler = Handler(Looper.getMainLooper())
            handler.post { setThumbnailView() }
          } catch (e: IOException) {
            ///Do nothing.
          }
        }
      } else {
        setThumbnailView()
      }
    }

    fun setThumbnailView() {
      thumbnail.setImageDrawable(thumbnailDrawable)
      thumbnail.visibility = View.VISIBLE
      thumbnail.bringToFront()
      dimView.bringToFront()
    }

    fun setContentPadding(
      position: Int,
      media: Media
    ) {
      if (position == 0 || position == itemCount - 1) {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val calculatedHeight = (screenWidth * media.height().toInt()
            / media.width().toInt())
        val padding = (screenHeight - calculatedHeight) / 2
        val params = contentView.layoutParams as LayoutParams
        if (position == 0) {
          params.setMargins(0, padding, 0, 0)
        } else {
          params.setMargins(0, 0, 0, padding)
        }
        contentView.layoutParams = params
      }
    }

    fun removeVideoThumbnail() {
      thumbnail.visibility = View.GONE
    }

    fun setContentViewDimensions(media: Media) {
      val screenWidth = Resources.getSystem().displayMetrics.widthPixels
      val calculatedHeight = (screenWidth * media.height().toInt()
          / media.width().toInt())
      contentView.layoutParams = LayoutParams(
          screenWidth,
          calculatedHeight + contentView.contentTitleHeight
      )
    }

    override fun onClick(v: View) {
      if (videoId != null && impressionId != null) {
        analyticsTracker.trackVideoClickEvent(videoId!!, impressionId!!)
      }
      listener.onVideoClicked(recyclerView, layoutPosition)
    }

    override fun onVideoPlaying() {
      removeVideoThumbnail()
    }

    override fun onVideoCompleted() {
      if (adapterPosition == itemCount - 1) {
        listener.onVideoCompleted(recyclerView, -1)
      } else {
        listener.onVideoCompleted(recyclerView, layoutPosition)
      }
    }

    override fun onResetVideo() {
      setVideoThumbnail()
    }

    init {
      contentView = itemView as InterstitialContentView
      contentTitle = itemView.contentTitle
      videoView = itemView.videoView
      videoView.setAnalyticsTracker(analyticsTracker)
      videoView.setVideoListener(this)
      thumbnail = itemView.imageView
      dimView = itemView.dimView
      itemView.setOnClickListener(this)
    }
  }

  internal interface InterstitialVideoHolderEvents {
    fun onVideoClicked(
      recyclerView: RecyclerView?,
      layoutPosition: Int
    )

    fun onVideoCompleted(
      recyclerView: RecyclerView?,
      layoutPosition: Int
    )
  }

  init {
    holders = ArrayList()
    this.listener = listener
    this.analyticsTracker = analyticsTracker
  }
}