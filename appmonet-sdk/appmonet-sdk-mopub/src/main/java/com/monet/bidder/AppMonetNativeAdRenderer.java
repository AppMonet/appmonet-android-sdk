package com.monet.bidder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.nativeads.BaseNativeAd;
import com.mopub.nativeads.MoPubAdRenderer;
import com.mopub.nativeads.NativeImageHelper;
import com.mopub.nativeads.NativeRendererHelper;

import java.util.WeakHashMap;

/**
 * Class responsible of rendering the app monet native ad.
 */
public class AppMonetNativeAdRenderer implements MoPubAdRenderer<AppMonetStaticNativeAd> {
    private static MonetLogger logger = new MonetLogger("AppMonetNativeAdRenderer");

    @NonNull
    private final AppMonetNativeViewBinder mViewBinder;
    @NonNull
    private final WeakHashMap<View, AppMonetNativeViewHolder> mViewHolderMap;

    public AppMonetNativeAdRenderer(@NonNull AppMonetNativeViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
        this.mViewHolderMap = new WeakHashMap<>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull Context context, @Nullable ViewGroup parent) {
        return LayoutInflater.from(context).inflate(this.mViewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(@NonNull View view, @NonNull AppMonetStaticNativeAd appMonetNativeAd) {
        AppMonetNativeViewHolder appMonetNativeViewHolder = this.mViewHolderMap.get(view);
        if (appMonetNativeViewHolder == null) {
            appMonetNativeViewHolder = AppMonetNativeViewHolder.fromViewBinder(view, this.mViewBinder);
            this.mViewHolderMap.put(view, appMonetNativeViewHolder);
        }

        this.update(appMonetNativeViewHolder, appMonetNativeAd);
        NativeRendererHelper.updateExtras(appMonetNativeViewHolder.mainView, mViewBinder.extras,
                appMonetNativeAd.getExtras());
        this.setViewVisibility(appMonetNativeViewHolder, 0);
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        return nativeAd instanceof AppMonetStaticNativeAd;
    }

    private void setIconView(@Nullable ImageView iconView, @Nullable String imageSrc) {
        if (imageSrc == null || iconView == null || imageSrc.isEmpty()) {
            return;
        }
        NativeImageHelper.loadImageView(imageSrc, iconView);
    }

    private void update(@NonNull AppMonetNativeViewHolder staticNativeViewHolder, @NonNull AppMonetStaticNativeAd staticNativeAd) {
        NativeRendererHelper.addTextView(staticNativeViewHolder.titleView, staticNativeAd.getTitle());
        NativeRendererHelper.addTextView(staticNativeViewHolder.textView, staticNativeAd.getText());
        NativeRendererHelper.addTextView(staticNativeViewHolder.callToActionView, staticNativeAd.getCallToAction());
        setIconView(staticNativeViewHolder.iconView, staticNativeAd.getIcon());

        if (staticNativeViewHolder.mediaLayout == null) {
            logger.debug("Attempted to add adView to null media layout");
        } else {
            if (staticNativeAd.getMedia() == null) {
                logger.debug("Attempted to set media layout content to null");
            } else {
                // it's possible that media already has a child -- be careful with this
                View mediaView = staticNativeAd.getMedia();
                if (mediaView == null) {
                    return;
                }

                // prevents media from being recycled
                if (mediaView.getParent() instanceof ViewGroup) {
                    logger.debug("media view has a parent; detaching");
                    // try to remove the parent
                    try {
                        ViewGroup parent = (ViewGroup) mediaView.getParent();
                        parent.removeView(mediaView);
                    } catch (Exception e) {
                        // do nothing
                    }
                }

                staticNativeViewHolder.mediaLayout.addView(staticNativeAd.getMedia());
            }
        }
    }

    private void setViewVisibility(@NonNull AppMonetNativeViewHolder staticNativeViewHolder, int visibility) {
        if (staticNativeViewHolder.mainView != null) {
            staticNativeViewHolder.mainView.setVisibility(visibility);
        }

    }
}
