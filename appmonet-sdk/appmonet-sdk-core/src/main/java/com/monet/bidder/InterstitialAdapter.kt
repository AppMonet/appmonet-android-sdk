package com.monet.bidder;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jose on 4/6/18.
 */

class InterstitialAdapter extends RecyclerView.Adapter<InterstitialAdapter.InterstitialViewHolder> {
  private final InterstitialData interstitialData;
  private final List<InterstitialViewHolder> holders;
  private final InterstitialAnalyticsTracker analyticsTracker;
  private boolean viewHolderBinded;
  private final InterstitialVideoHolderEvents listener;
  private RecyclerView recyclerView;

  InterstitialAdapter(InterstitialData interstitialData,
      InterstitialAnalyticsTracker analyticsTracker, InterstitialVideoHolderEvents listener) {
    this.interstitialData = interstitialData;
    this.holders = new ArrayList<>();
    this.listener = listener;
    this.analyticsTracker = analyticsTracker;
  }

  @NonNull @Override
  public InterstitialViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InterstitialContentView contentView = new InterstitialContentView(parent.getContext());
    return new InterstitialViewHolder(contentView);
  }

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    this.recyclerView = recyclerView;
  }

  @Override
  public void onBindViewHolder(final InterstitialViewHolder interstitialViewHolder,
      final int position) {
    interstitialViewHolder.videoView.videoId = interstitialData.content.get(position).id();
    if (interstitialData.content.get(position).media() != null) {
      interstitialViewHolder.videoView.videoUrl =
          interstitialData.content.get(position).media().source();
      interstitialViewHolder.thumbnailUrl =
          interstitialData.content.get(position).media().thumbnail();
      interstitialViewHolder.setContentViewDimensions(interstitialData.content.get(position).media());
      interstitialViewHolder.setContentPadding(position,
          interstitialData.content.get(position).media());
    }

    interstitialViewHolder.contentTitle.setText(interstitialData.content.get(position).title());
    interstitialViewHolder.contentView.setTitleContent(
        interstitialData.content.get(position).title());

    if (position == 0) {
      if (!viewHolderBinded) {
        interstitialViewHolder.contentView.removeDarkView();
        interstitialViewHolder.videoView.trackVideoAttached();
      }
      interstitialViewHolder.videoView.isFocused = !viewHolderBinded;
    }
    viewHolderBinded = true;
  }

  @Override
  public int getItemCount() {
    return interstitialData.content.size();
  }

  @Override
  public void onViewAttachedToWindow(@NonNull final InterstitialViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    holder.videoView.loadVideo();
    holder.setVideoThumbnail();
    holders.add(holder);
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull InterstitialViewHolder holder) {
    holders.remove(holder);
    holder.isFocused = false;
    holder.isBuffered = false;
    if (holder.videoView.isPlaying()) {
      holder.videoView.stopPlayback();
    }
    holder.videoView.setOnPreparedListener(null);
  }

  void playVideoInPosition(int snapPosition) {
    for (InterstitialViewHolder holder : holders) {
      if (holder.getLayoutPosition() == snapPosition) {
        holder.videoView.trackVideoAttached();
        holder.videoView.isFocused = true;
        holder.videoView.playVideo();
      } else if (holder.videoView.isFocused) {
        holder.videoView.trackVideoDetached();
        holder.videoView.resetVideo();
      }
    }
  }

  void cleanup() {
    for (InterstitialViewHolder holder : holders) {
      if (holder.videoView.isFocused) {
        holder.videoView.resetVideo();
      }
    }
    holders.clear();
  }

  class InterstitialViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
      MonetVideoView.VideoListener {
    private final ImageView thumbnail;
    final TextView contentTitle;
    MonetVideoView videoView;
    BitmapDrawable thumbnailDrawable;
    String thumbnailUrl;
    View dimView;
    InterstitialContentView contentView;
    boolean isBuffered;
    boolean isFocused;
    String videoId;
    String impressionId;

    InterstitialViewHolder(View itemView) {
      super(itemView);
      contentView = ((InterstitialContentView) itemView);
      contentTitle = ((InterstitialContentView) itemView).contentTitle;
      videoView = ((InterstitialContentView) itemView).videoView;
      videoView.setAnalyticsTracker(analyticsTracker);
      videoView.setVideoListener(this);
      thumbnail = ((InterstitialContentView) itemView).imageView;
      dimView = ((InterstitialContentView) itemView).dimView;
      itemView.setOnClickListener(this);
    }

    void setVideoThumbnail() {
      if (thumbnailDrawable == null) {
        AsyncTask.execute(() -> {
          try {

            thumbnailDrawable = new BitmapDrawable(recyclerView.getContext()
                .getResources(), BitmapFactory.decodeStream(new URL(thumbnailUrl)
                .openConnection().getInputStream()));
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(this::setThumbnailView);
          } catch (IOException e) {
            ///Do nothing.
          }
        });
      } else {
        setThumbnailView();
      }
    }

    void setThumbnailView() {
      thumbnail.setImageDrawable(thumbnailDrawable);
      thumbnail.setVisibility(View.VISIBLE);
      thumbnail.bringToFront();
      dimView.bringToFront();
    }

    void setContentPadding(int position, InterstitialData.InterstitialContent.Media media) {
      if (position == 0 || position == getItemCount() - 1) {
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int calculatedHeight = (screenWidth * Integer.parseInt(media.height()))
            / Integer.parseInt(media.width());
        int padding = (screenHeight - calculatedHeight) / 2;
        RelativeLayout.LayoutParams params =
            (RelativeLayout.LayoutParams) contentView.getLayoutParams();
        if (position == 0) {
          params.setMargins(0, padding, 0, 0);
        } else {
          params.setMargins(0, 0, 0, padding);
        }
        contentView.setLayoutParams(params);
      }
    }

    void removeVideoThumbnail() {
      thumbnail.setVisibility(View.GONE);
    }

    void setContentViewDimensions(InterstitialData.InterstitialContent.Media media) {
      int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
      int calculatedHeight = (screenWidth * Integer.parseInt(media.height()))
          / Integer.parseInt(media.width());
      contentView.setLayoutParams(new RelativeLayout.LayoutParams(screenWidth,
          calculatedHeight + contentView.contentTitleHeight));
    }

    @Override
    public void onClick(View v) {
      analyticsTracker.trackVideoClickEvent(videoId, impressionId);
      listener.onVideoClicked(recyclerView, getLayoutPosition());
    }

    @Override
    public void onVideoPlaying() {
      removeVideoThumbnail();
    }

    @Override
    public void onVideoCompleted() {
      if (getAdapterPosition() == getItemCount() - 1) {
        listener.onVideoCompleted(recyclerView, -1);
      } else {
        listener.onVideoCompleted(recyclerView, getLayoutPosition());
      }
    }

    @Override
    public void onResetVideo() {
      setVideoThumbnail();
    }
  }

  interface InterstitialVideoHolderEvents {
    void onVideoClicked(RecyclerView recyclerView, int layoutPosition);

    void onVideoCompleted(RecyclerView recyclerView, int layoutPosition);
  }
}