package com.monet.bidder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * This class holds the views from the layout provided for native rendering.
 */
class AppMonetNativeViewHolder {
    @Nullable
    ImageView iconView;
    @Nullable
    View mainView;
    @Nullable
    TextView titleView;
    @Nullable
    TextView textView;
    @Nullable
    TextView callToActionView;
    @Nullable
    ViewGroup mediaLayout;

    private AppMonetNativeViewHolder() {
    }

    @NonNull
    static AppMonetNativeViewHolder fromViewBinder(@NonNull View view, @NonNull AppMonetNativeViewBinder viewBinder) {
        AppMonetNativeViewHolder appMonetNativeViewHolder = new AppMonetNativeViewHolder();
        appMonetNativeViewHolder.mainView = view;
        appMonetNativeViewHolder.titleView = view.findViewById(viewBinder.titleId);
        appMonetNativeViewHolder.textView = view.findViewById(viewBinder.textId);
        appMonetNativeViewHolder.callToActionView = view.findViewById(viewBinder.callToActionId);
        appMonetNativeViewHolder.mediaLayout = view.findViewById(viewBinder.mediaLayoutId);
        appMonetNativeViewHolder.iconView = view.findViewById(viewBinder.iconId);
        return appMonetNativeViewHolder;
    }
}
