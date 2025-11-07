/*
 * Map marker utilities for turning vector drawables into tinted BitmapDescriptor icons.
 * Centralizes size and tint handling for Google Maps markers.
 */
package ca.team.originkickoff;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Helper for creating tinted, sized bitmap descriptors from vector drawables.
 */
public final class MapMarkerUtil {
    public static final int BLUE_TINT = Color.parseColor("#1976D2"); // Material Blue 700

    private MapMarkerUtil() {}

    /**
     * Create a BitmapDescriptor from a vector drawable, optionally tinted and scaled to a dp size.
     *
     * @param context     context used to resolve resources
     * @param vectorResId vector drawable resource id
     * @param sizeDp      target square size in density-independent pixels
     * @param tintColor   color tint to apply, or Color.TRANSPARENT to keep original
     * @return a BitmapDescriptor suitable for use with GoogleMap markers
     */
    @NonNull
    public static BitmapDescriptor bitmapDescriptorFromVector(@NonNull Context context,
                                                              @DrawableRes int vectorResId,
                                                              int sizeDp,
                                                              @ColorInt int tintColor) {
        Drawable drawable = AppCompatResources.getDrawable(context, vectorResId);
        if (drawable == null) {
            throw new IllegalArgumentException("Drawable not found: " + vectorResId);
        }

        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        if (tintColor != Color.TRANSPARENT) {
            DrawableCompat.setTint(wrapped, tintColor);
        }

        int sizePx = dpToPx(context, sizeDp);
        wrapped.setBounds(0, 0, sizePx, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        wrapped.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Convert density-independent pixels to raw pixels using device metrics.
     *
     * @param context context providing resources and display metrics
     * @param dp      size in density-independent pixels
     * @return size in raw pixels for current display
     */
    private static int dpToPx(Context context, int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics));
    }
}
