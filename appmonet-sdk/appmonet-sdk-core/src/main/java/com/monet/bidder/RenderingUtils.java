package com.monet.bidder;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.VisibleForTesting;
import com.monet.bidder.adview.AdViewPositioning;
import com.monet.bidder.bid.BidResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.monet.bidder.FloatingPosition.BOTTOM;
import static com.monet.bidder.FloatingPosition.DP;
import static com.monet.bidder.FloatingPosition.END;
import static com.monet.bidder.FloatingPosition.HEIGHT;
import static com.monet.bidder.FloatingPosition.START;
import static com.monet.bidder.FloatingPosition.TOP;
import static com.monet.bidder.FloatingPosition.WIDTH;

public class RenderingUtils {
  private static Pattern sVastTrackingPattern =
      Pattern.compile("monet://vast/(?:v2/)?([^/]+)/?([^/]+)?");

  private static int BASE64_FLAGS = Base64.NO_WRAP | Base64.NO_PADDING | Base64.NO_CLOSE;

  static FrameLayout generateBlankLayout(Context context, int width, int height) {
    View blankView = new View(context);
    FrameLayout blankLayoutContainer = new FrameLayout(context);
    blankLayoutContainer.addView(blankView,
        new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
    return blankLayoutContainer;
  }

  public static String base64Encode(String source) {
    if (source == null || source.length() == 0) {
      return "";
    }

    return Base64.encodeToString(
        source.getBytes(StandardCharsets.UTF_8), BASE64_FLAGS);
  }

  public static String base64Decode(String source) {
    if (source == null || source.isEmpty()) {
      return "";
    }

    return new String(Base64.decode(source, BASE64_FLAGS), StandardCharsets.UTF_8);
  }

  static String encodeURIComponent(String string) {
    if (string == null || string.isEmpty()) {
      return "";
    }

    try {
      return (new URI(null, null, string, null)).getRawPath();
    } catch (URISyntaxException e) {
      return "";
    }
  }

  public static String appendQueryParam(String url, String queryKey, String queryValue) {
    if (url == null || url.isEmpty()) {
      return url;
    }

    return url + ((url.indexOf('?') != -1) ? "&" : "?") +
        encodeURIComponent(queryKey) + "=" + encodeURIComponent(queryValue);
  }

  public static String encodeStringByXor(String source) {
    if (source == null || source.isEmpty()) {
      return "";
    }

    Charset utf8 = Charset.forName("UTF-8");
    byte[] bytes = source.getBytes(utf8);
    byte[] output = new byte[bytes.length];

    for (int i = 0; i < bytes.length; i++) {
      output[i] = (byte) (((int) bytes[i]) ^ 1);
    }

    return new String(output, utf8);
  }

  private static Map<Object, Object> getAllActivities() {
    try {
      Class activityThreadClass = Class.forName("android.app.ActivityThread");
      Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
      Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
      activitiesField.setAccessible(true);

      return (Map<Object, Object>) activitiesField.get(activityThread);
    } catch (Exception e) {
      // do nothing
    }

    return null;
  }

  @VisibleForTesting
  static AdViewPositioning calculateAdViewPositioning(int viewHeight, int viewWidth, int viewStartX,
      int viewStartY,
      Map<String, FloatingPosition.Value> positionSettings) {
    String unit;
    int y = 0;
    int x = 0;
    int height = 0;
    int width = 0;
    for (Map.Entry<String, FloatingPosition.Value> entry : positionSettings.entrySet()) {
      unit = entry.getValue().getUnit();
      int valueInPixels = 0;
      //If unit is DP then convert it to pixels
      if (DP.equals(unit)) {
        valueInPixels = dpToPixels(entry.getValue().getValue());
      }
      switch (entry.getKey()) {
        case BOTTOM:
          y = (DP.equals(unit))
              ? viewStartY - valueInPixels
              : viewStartY - percentToPixels(viewHeight, entry.getValue().getValue());
          break;
        case TOP:
          y = (DP.equals(unit))
              ? viewStartY + valueInPixels
              : viewStartY + percentToPixels(viewHeight, entry.getValue().getValue());
          break;
        case START:
          x = (DP.equals(unit))
              ? viewStartX + valueInPixels
              : viewStartX + percentToPixels(viewWidth, entry.getValue().getValue());
          break;
        case END:
          int adWidth = (DP.equals(positionSettings.get(WIDTH).getUnit()))
              ? dpToPixels(positionSettings.get(WIDTH).getValue())
              : percentToPixels(viewWidth, positionSettings.get(WIDTH).getValue());

          int widthDifference = viewWidth - adWidth;
          x = (DP.equals(entry.getValue().getUnit()))
              ? widthDifference - valueInPixels
              : widthDifference - percentToPixels(viewWidth, entry.getValue().getValue());
          break;
        case HEIGHT:
          height = (DP.equals(unit))
              ? valueInPixels
              : percentToPixels(viewHeight, positionSettings.get(HEIGHT).getValue());
          break;
        case WIDTH:
          width = (DP.equals(unit))
              ? valueInPixels
              : percentToPixels(viewWidth, positionSettings.get(WIDTH).getValue());
          break;
        default:
          break;
      }
    }
    return new AdViewPositioning(x, y, width, height);
  }

  static AdViewPositioning getScreenPositioning(View view,
      Map<String, FloatingPosition.Value> positionSettings, Integer h, Integer w) {
    //retrieve location of view
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    int viewStartTopX = location[0];
    int viewStartTopY = location[1];

    // if the view layout matches parent then get the view's width since its already rendered
    // if not get the width provided
    int viewWidth = ((view.getParent() != null && view.getParent() instanceof FrameLayout &&
        ((FrameLayout) view.getParent()).getLayoutParams().width
            == ViewGroup.LayoutParams.WRAP_CONTENT)
        || view.getWidth() == 0)
        ? dpToPixels(w) : view.getWidth();

    int viewHeight = ((view.getParent() != null && view.getParent() instanceof FrameLayout &&
        ((FrameLayout) view.getParent()).getLayoutParams().height
            == ViewGroup.LayoutParams.WRAP_CONTENT)
        || view.getHeight() == 0)
        ? dpToPixels(h) : view.getHeight();
    return calculateAdViewPositioning(viewHeight, viewWidth, viewStartTopX, viewStartTopY,
        positionSettings);
  }

  static ImageView getBase64ImageView(Activity activity, Icons icon) {
    Drawable drawable = icon.createDrawable(activity);

    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
    Drawable d = new BitmapDrawable(activity.getResources(),
        Bitmap.createScaledBitmap(bitmap, Icons.asIntPixels(16F, activity),
            Icons.asIntPixels(16F, activity), true));
    ImageView closeButton = new ImageView(activity);
    closeButton.setImageDrawable(d);
    FrameLayout.LayoutParams closeButtonParams =
        new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.RIGHT);
    int closeButtonPadding = RenderingUtils.dpToPixels(16);
    closeButton.setPadding(closeButtonPadding, 0, 0, closeButtonPadding);
    closeButton.setLayoutParams(closeButtonParams);
    closeButton.bringToFront();
    return closeButton;
  }

  private static class ActivityClientRecordHelper {
    static boolean isPaused(Object record) {
      try {
        Class recordClass = record.getClass();
        Field pausedField = recordClass.getDeclaredField("paused");
        pausedField.setAccessible(true);
        return pausedField.getBoolean(record);
      } catch (Exception e) {
        // do nothing
      }

      return false;
    }

    static Activity getActivity(Object record) {
      try {
        Class recordClass = record.getClass();
        Field activityField = recordClass.getDeclaredField("activity");
        activityField.setAccessible(true);
        return (Activity) activityField.get(record);
      } catch (Exception e) {
        // do nothing
      }

      return null;
    }

    static String toString(Object record) {
      try {
        Class recordClass = record.getClass();
        Method getStateString = recordClass.getDeclaredMethod("getStateString");
        getStateString.setAccessible(true);
        String representation = (String) getStateString.invoke(record);

        Activity activity = getActivity(record);
        if (activity != null) {
          StringBuilder sb = new StringBuilder();
          sb.append("||cn:");
          sb.append(activity.getComponentName().toString());
          sb.append("||r:");
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            sb.append(activity.getReferrer() != null ? activity.getReferrer().toString() : "noref");
          } else {
            sb.append("unknown");
          }

          sb.append("||t:");
          sb.append(activity.getTitle());
          representation += sb.toString();
        } else {
          representation += "|no-name";
        }

        return representation;
      } catch (Exception e) {
        // do nothing
      }
      return null;
    }
  }

  public static boolean isScreenLocked(Context context) {
    KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    return km != null && km.inKeyguardRestrictedInputMode();
  }

  public static boolean isScreenOn(Context context) {
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    if (pm == null) {
      return true;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
      return pm.isInteractive();
    }

    return pm.isScreenOn();
  }

  public static String[] getActivitiesInfo() {
    Map<Object, Object> activities = getAllActivities();
    if (activities == null) {
      return new String[] {};
    }

    Object[] activityList = activities.values().toArray();
    String[] output = new String[activityList.length];

    for (int i = 0; i < activityList.length; i++) {
      output[i] = ActivityClientRecordHelper.toString(activityList[i]);
    }

    return output;
  }

  static int dpToPixels(int dp) {
    return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    //    final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    //    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
  }

  static int percentToPixels(int containerDimension, int percent) {
    return containerDimension * percent / 100;
  }

  public static int numVisibleActivities() {
    try {
      Class activityThreadClass = Class.forName("android.app.ActivityThread");
      Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
      Field visibleField = activityThreadClass.getDeclaredField("mNumVisibleActivities");
      visibleField.setAccessible(true);

      Integer numVisible = (Integer) visibleField.get(activityThread);
      if (numVisible == null) {
        return -1;
      }

      return numVisible;
    } catch (Exception e) {
      // do nothing
    }

    return -1;
  }

  private static Activity getForegroundActivity() {
    Object record = getForegroundActivityRecord();
    if (record != null) {
      return ActivityClientRecordHelper.getActivity(record);
    }

    return null;
  }

  public static Object getFlagByName(Class<?> flagSource, String flagName) {
    try {
      Field field = flagSource.getDeclaredField(flagName);
      return field.get(null);
    } catch (Exception e) {
      return null;
    }
  }

  private static Object getForegroundActivityRecord() {
    Map<Object, Object> activities = getAllActivities();
    if (activities == null) {
      return null;
    }

    for (Object record : activities.values()) {
      if (!ActivityClientRecordHelper.isPaused(record)) {
        Activity found = ActivityClientRecordHelper.getActivity(record);
        if (found != null) {
          return record;
        }
      }
    }

    return null;
  }

  static ViewGroup getHiddenLayout() {
    Activity activity;

    try {
      activity = getForegroundActivity();
    } catch (Exception e) {
      return null;
    }

    if (activity == null) {
      return null;
    }

    FrameLayout hiddenLayout = new FrameLayout(activity);
    hiddenLayout.setVisibility(View.INVISIBLE);
    hiddenLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    ViewGroup parent = activity.getWindow().getDecorView().findViewById(android.R.id.content);
    parent.addView(hiddenLayout);

    return hiddenLayout;
  }

  public static VastTrackingMatch parseVastTracking(String url) {
    if (url == null || url.isEmpty()) {
      return new VastTrackingMatch(null, null);
    }

    Matcher match = sVastTrackingPattern.matcher(url.toLowerCase());
    if (match == null || !match.matches()) {
      return new VastTrackingMatch(null, null);
    }

    return new VastTrackingMatch(match.group(1), match.group(2));
  }

  public static boolean isValidUrl(String url) {
    return url != null && !url.isEmpty() && URLUtil.isValidUrl(url);
  }

  public static Object setField(Object instance, String fieldName, Class<?> expectedType,
      Object value) {
    Field field = getField(instance, fieldName, expectedType);
    if (field == null) {
      return null;
    }

    try {
      field.set(instance, value);
      return value;
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  public static Field getField(Object instance, String fieldName, Class<?> expectedType) {
    if (instance == null || fieldName == null || fieldName.isEmpty()) {
      return null;
    }

    Field field;
    Class<?> klass =
        instance.getClass() == Class.class ? ((Class<?>) instance) : instance.getClass();

    try {
      field = klass.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      try {
        field = klass.getField(fieldName);
      } catch (Exception e1) {
        return null;
      }
    } catch (Exception e2) {
      return null;
    }

    if (!field.getType().equals(expectedType)) {
      return null;
    }

    field.setAccessible(true);
    return field;
  }

  static int[] getDisplayDimensions(Context context) {
    try {
      DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
      return new int[] {
          (int) Math.floor(displayMetrics.widthPixels / displayMetrics.density),
          (int) Math.floor(displayMetrics.heightPixels / displayMetrics.density)
      };
    } catch (Exception e) {
      return new int[] { 0, 0 };
    }
  }

  /**
   * Get the default URL to host the auction at. This can be overridden
   * by the configuration (set as a preference), but will default to the application's
   * bundle, backwards.
   *
   * @return a String representing the URL to host the auction at
   */
  public static String getDefaultAuctionURL(DeviceData deviceData) {
    ApplicationInfo app = deviceData.getAppInfo();

    if (app == null) {
      return Constants.DEFAULT_AUCTION_URL;
    }

    String[] parts = TextUtils.split(app.packageName, "\\.");
    int len = parts.length;

    StringBuilder buffer = new StringBuilder();
    while (--len >= 0) {
      buffer.append(parts[len]);
      if (len > 0) {
        buffer.append(".");
      }
    }

    return "http://" + buffer.toString();
  }

  public static FrameLayout.LayoutParams getCenterLayoutParams(Context context, AdSize adSize) {
    return new FrameLayout.LayoutParams(
        adSize.getWidthInPixels(context),
        adSize.getHeightInPixels(context),
        Gravity.CENTER);
  }

  public static class VastTrackingMatch {
    public String bidId;
    public String event;

    VastTrackingMatch(String evt, String id) {
      event = evt;
      bidId = id;
    }

    public boolean isForBid(BidResponse bid) {
      if (bid == null || bidId == null) {
        return true;
      }

      return bidId.equalsIgnoreCase(bid.getId());
    }

    public boolean matches() {
      return event != null;
    }
  }
}
