package com.monet.bidder;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AppMonetNativeViewBinder {
    final int layoutId;
    final int mediaLayoutId;
    final int titleId;
    final int textId;
    final int callToActionId;
    final int iconId;

    @NonNull final Map<String, Integer> extras;

    private AppMonetNativeViewBinder(@NonNull Builder builder) {
        this.layoutId = builder.layoutId;
        this.mediaLayoutId = builder.mediaLayoutId;
        this.titleId = builder.titleId;
        this.textId = builder.textId;
        this.callToActionId = builder.callToActionId;
        this.iconId = builder.iconId;
        this.extras = builder.extras;
    }

    public static final class Builder {
        private final int layoutId;
        private int mediaLayoutId;
        private int titleId;
        private int textId;
        private int iconId;
        private int callToActionId;
        @NonNull
        private Map<String, Integer> extras = Collections.emptyMap();

        public Builder(int layoutId) {
            this.layoutId = layoutId;
            this.extras = new HashMap<>();
        }

        @NonNull
        public final Builder mediaLayoutId(int mediaLayoutId) {
            this.mediaLayoutId = mediaLayoutId;
            return this;
        }

        @NonNull
        public final Builder titleId(int titlteId) {
            this.titleId = titlteId;
            return this;
        }

        @NonNull
        public final Builder iconId(int iconId) {
            this.iconId = iconId;
            return this;
        }

        @NonNull
        public final Builder textId(int textId) {
            this.textId = textId;
            return this;
        }

        @NonNull
        public final Builder addExtras(final Map<String, Integer> resourceIds) {
            this.extras = new HashMap<>(resourceIds);
            return this;
        }

        @NonNull
        public final Builder addExtra(final String key, final int resourceId) {
            this.extras.put(key, resourceId);
            return this;
        }

        @NonNull
        public final Builder callToActionId(int callToActionId) {
            this.callToActionId = callToActionId;
            return this;
        }

        @NonNull
        public final AppMonetNativeViewBinder build() {
            return new AppMonetNativeViewBinder(this);
        }
    }
}
