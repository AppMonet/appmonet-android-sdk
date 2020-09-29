package com.monet.bidder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.nativeads.BaseNativeAd;
import com.mopub.nativeads.ClickInterface;
import com.mopub.nativeads.ImpressionInterface;
import com.mopub.nativeads.ImpressionTracker;
import com.mopub.nativeads.NativeClickHandler;
import com.mopub.nativeads.NativeErrorCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class which holds the logic and interface callbacks for the native ad to be displayed and
 * interacted with.
 */
public class AppMonetStaticNativeAd extends BaseNativeAd implements ClickInterface, ImpressionInterface {
  private static final MonetLogger logger = new MonetLogger("AppMonetStaticNativeAd");
  private final CustomEventNative.CustomEventNativeListener mCustomEventNativeListener;
  private final HashMap<String, Object> mExtras;
  private final AppMonetNativeEventCallback appMonetNativeEventCallback;

  enum Parameter {
    IMPRESSION_TRACKER("imptracker", false),
    TITLE("title", false),
    TEXT("text", false),
    ICON("icon", false),
    CALL_TO_ACTION("ctatext", false);

    @NonNull
    final String name;
    final boolean required;

    Parameter(@NonNull final String name, final boolean required) {
      this.name = name;
      this.required = required;
    }

    @Nullable
    static Parameter from(@NonNull final String name) {
      for (final Parameter parameter : values()) {
        if (parameter.name.equals(name)) {
          return parameter;
        }
      }
      return null;
    }

    @NonNull
    private static final Set<String> requiredKeys = new HashSet<>();

    static {
      for (final Parameter parameter : values()) {
        if (parameter.required) {
          requiredKeys.add(parameter.name);
        }
      }
    }
  }

  @NonNull
  private final Map<String, String> serverExtras;
  @Nullable
  private View mainView;
  @Nullable
  private String mTitle;
  @Nullable
  private String mIcon;
  @Nullable
  private String mText;
  @Nullable
  private String mCallToAction;
  @Nullable
  private View media;
  private boolean mImpressionRecorded;
  private int mImpressionMinTimeViewed = 1000;
  @NonNull
  private final ImpressionTracker mImpressionTracker;
  @NonNull
  private final NativeClickHandler mNativeClickHandler;

  public AppMonetStaticNativeAd(@NonNull Map<String, String> serverExtras, @Nullable View view, @NonNull ImpressionTracker impressionTracker,
                                @NonNull NativeClickHandler nativeClickHandler, @NonNull CustomEventNative.CustomEventNativeListener customEventNativeListener,
                                @NonNull AppMonetNativeEventCallback appMonetNativeEventCallback) {
    this.media = view;
    this.mCustomEventNativeListener = customEventNativeListener;
    this.serverExtras = serverExtras;
    this.mImpressionTracker = impressionTracker;
    this.mNativeClickHandler = nativeClickHandler;
    this.appMonetNativeEventCallback = appMonetNativeEventCallback;
    mExtras = new HashMap<>();
  }

  @Nullable
  public View getMainView() {
    return mainView;
  }

  public void setMainView(@Nullable View mainView) {
    this.mainView = mainView;
  }

  @Nullable
  public String getTitle() {
    return mTitle;
  }

  @Nullable
  public String getIcon() {
    return mIcon;
  }

  public void setTitle(@Nullable String title) {
    this.mTitle = title;
  }

  public void setIcon(@Nullable String iconSrc) {
    this.mIcon = iconSrc;
  }

  @Nullable
  public String getText() {
    return mText;
  }

  public void setText(@Nullable String text) {
    this.mText = text;
  }

  @Nullable
  public String getCallToAction() {
    return mCallToAction;
  }

  public void setCallToAction(@Nullable String callToAction) {
    this.mCallToAction = callToAction;
  }

  @Nullable
  public View getMedia() {
    return media;
  }

  public void setMedia(@Nullable View media) {
    this.media = media;
  }

  @Override
  public void prepare(@NonNull View view) {
    this.mImpressionTracker.addView(view, this);
    this.mNativeClickHandler.setOnClickListener(view, this);
  }

  @Override
  public void clear(@NonNull View view) {
    this.mImpressionTracker.removeView(view);
    this.mNativeClickHandler.clearOnClickListener(view);
  }

  @Override
  public void destroy() {
    this.mImpressionTracker.destroy();
    if (media != null) {
      this.appMonetNativeEventCallback.destroy(media);
    }
  }

  @Override
  public void recordImpression(@NonNull View view) {
    notifyAdImpressed();
  }

  @Override
  public final int getImpressionMinPercentageViewed() {
    return 50;
  }

  @Override
  public Integer getImpressionMinVisiblePx() {
    return null;
  }

  @Override
  public final int getImpressionMinTimeViewed() {
    return this.mImpressionMinTimeViewed;
  }

  @Override
  public final boolean isImpressionRecorded() {
    return this.mImpressionRecorded;
  }

  @Override
  public final void setImpressionRecorded() {
    this.mImpressionRecorded = true;
  }

  //
  @Override
  public void handleClick(@NonNull View view) {
    if (media != null) {
      appMonetNativeEventCallback.onClick(media);
      this.notifyAdClicked();
    }
  }

  @NonNull
  final public Map<String, Object> getExtras() {
    return new HashMap<>(mExtras);
  }

  public void onAdClicked() {
    notifyAdClicked();
  }

  public void loadAd() {
    if (!containsRequiredKeys(serverExtras)) {
      mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.SERVER_ERROR_RESPONSE_CODE);
    }

    Set<String> keys = serverExtras.keySet();
    for (String key : keys) {
      final Parameter parameter = Parameter.from(key);
      if (parameter != null) {
        try {
          addInstanceVariable(parameter, serverExtras.get(key));
        } catch (ClassCastException e) {
          mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNEXPECTED_RESPONSE_CODE);
        }
      } else {
        addExtra(key, serverExtras.get(key));
      }
    }

    mCustomEventNativeListener.onNativeAdLoaded(this);
  }

  private boolean containsRequiredKeys(@NonNull final Map<String, String> serverExtras) {
    final Set<String> keys = serverExtras.keySet();
    return keys.containsAll(Parameter.requiredKeys);
  }

  private void addInstanceVariable(@NonNull final Parameter key,
                                   @Nullable final Object value) {
    try {
      switch (key) {
        case ICON:
          setIcon((String) value);
          break;
        case CALL_TO_ACTION:
          setCallToAction((String) value);
          break;
        case TITLE:
          setTitle((String) value);
          break;
        case TEXT:
          setText((String) value);
          break;
        default:
          logger.debug("Unable to add JSON key to internal mapping: " + key.name);
          break;
      }
    } catch (ClassCastException e) {
      if (!key.required) {
        logger.debug("Ignoring class cast exception for optional key: " + key.name);
      } else {
        throw e;
      }
    }
  }

  final void addExtra(@NonNull final String key, @Nullable final Object value) {
    mExtras.put(key, value);
  }

  void swapViews(AppMonetViewLayout view, AdServerBannerListener listener) {
    ((AppMonetViewLayout) this.media).swapViews(view, listener);
  }
}