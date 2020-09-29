package com.monet.bidder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.trusted.TrustedWebActivityDisplayMode;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.androidbrowserhelper.trusted.ChromeUpdatePrompt;
import com.google.androidbrowserhelper.trusted.LauncherActivityMetadata;
import com.google.androidbrowserhelper.trusted.TwaLauncher;
import com.google.androidbrowserhelper.trusted.TwaSharedPreferencesManager;
import com.monet.R;

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
import static com.monet.bidder.Constants.APPMONET_BROADCAST;
import static com.monet.bidder.Constants.APPMONET_BROADCAST_MESSAGE;

public class TrustedInterstitialActivity extends Activity {
  protected static final String URL = "url";
  protected static final String UUID = "uuid";
  private static final String BROWSER_WAS_LAUNCHED_KEY =
      "android.support.customtabs.trusted.BROWSER_WAS_LAUNCHED_KEY";
  private static final String FALLBACK_TYPE_WEBVIEW = "webview";
  private static boolean sChromeVersionChecked;
  private LauncherActivityMetadata mMetadata;
  private boolean mBrowserWasLaunched;

  protected String uuid;
  protected String url;

  @Nullable
  private TwaLauncher mTwaLauncher;

  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    uuid = getIntent().getStringExtra(UUID);
    url = getIntent().getStringExtra(URL);

    if (isTrustedActivity()) {
      setTheme(R.style.TrustedInterstitialActivityTheme);
      if (this.restartInNewTask()) {
        this.finish();
      } else if (savedInstanceState != null && savedInstanceState.getBoolean(
          BROWSER_WAS_LAUNCHED_KEY)) {
        this.finish();
      } else {
        this.mMetadata = LauncherActivityMetadata.parse(this);

        CustomTabColorSchemeParams darkModeColorScheme =
            (new CustomTabColorSchemeParams.Builder()).setToolbarColor(
                this.getColorCompat(this.mMetadata.statusBarColorDarkId))
                .setNavigationBarColor(this.getColorCompat(this.mMetadata.navigationBarColorDarkId))
                .build();
        TrustedWebActivityIntentBuilder twaBuilder =
            (new TrustedWebActivityIntentBuilder(this.getLaunchingUrl()))
                .setToolbarColor(this.getColorCompat(this.mMetadata.statusBarColorId))
                .setNavigationBarColor(this.getColorCompat(this.mMetadata.navigationBarColorId))
                .setColorScheme(0).setColorSchemeParams(2, darkModeColorScheme)
                .setDisplayMode(this.getDisplayMode());
        if (this.mMetadata.additionalTrustedOrigins != null) {
          twaBuilder.setAdditionalTrustedOrigins(this.mMetadata.additionalTrustedOrigins);
        }

        this.mTwaLauncher = new TwaLauncher(this);
        this.mTwaLauncher.launch(twaBuilder, null, new Runnable() {
          @Override
          public void run() {
            mBrowserWasLaunched = true;
          }
        }, this.getFallbackStrategy());
        if (!sChromeVersionChecked) {
          ChromeUpdatePrompt.promptIfNeeded(this, this.mTwaLauncher.getProviderPackage());
          sChromeVersionChecked = true;
        }
        (new TwaSharedPreferencesManager(this)).writeLastLaunchedProviderPackageName(
            this.mTwaLauncher.getProviderPackage());
      }
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (isTrustedActivity()) {
      this.finish();
    }
  }

  private int getColorCompat(int splashScreenBackgroundColorId) {
    return ContextCompat.getColor(this, splashScreenBackgroundColorId);
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    if (this.mBrowserWasLaunched) {
      this.finish();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (isTrustedActivity()) {
      if (this.mTwaLauncher != null) {
        this.mTwaLauncher.destroy();
      }
      LocalBroadcastManager.getInstance(this).sendBroadcast(
          new Intent(APPMONET_BROADCAST)
              .putExtra(APPMONET_BROADCAST_MESSAGE,
                  "interstitial_dismissed"));
    }
  }

  @Override
  public void onBackPressed() {
    //do nothing
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (isTrustedActivity()) {
      outState.putBoolean("BROWSER_WAS_LAUNCHED_KEY", this.mBrowserWasLaunched);
    }
  }

  protected Uri getLaunchingUrl() {
    return Uri.parse(url);
  }

  protected TwaLauncher.FallbackStrategy getFallbackStrategy() {
    return FALLBACK_TYPE_WEBVIEW.equalsIgnoreCase(this.mMetadata.fallbackStrategyType)
        ? TwaLauncher.WEBVIEW_FALLBACK_STRATEGY : TwaLauncher.CCT_FALLBACK_STRATEGY;
  }

  protected TrustedWebActivityDisplayMode getDisplayMode() {
    return new TrustedWebActivityDisplayMode
        .ImmersiveMode(true, LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT);
  }

  private boolean restartInNewTask() {
    boolean hasNewTask = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
    boolean hasNewDocument = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      hasNewDocument = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_DOCUMENT) != 0;
    }
    if (hasNewTask && !hasNewDocument) return false;

    Intent newIntent = new Intent(getIntent());

    int flags = getIntent().getFlags();
    flags |= Intent.FLAG_ACTIVITY_NEW_TASK;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      flags &= ~Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
    }
    newIntent.setFlags(flags);

    startActivity(newIntent);
    return true;
  }

  private boolean isTrustedActivity() {
    return uuid == null;
  }
}