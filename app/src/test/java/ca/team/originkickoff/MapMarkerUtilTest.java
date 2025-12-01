package ca.team.originkickoff;

import android.content.Context;
import android.util.DisplayMetrics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Robolectric test for private dpToPx in MapMarkerUtil using reflection.
 * Avoids bitmapDescriptorFromVector because it needs actual drawable resources.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class MapMarkerUtilTest {

    private int invokeDpToPx(Context ctx, int dp) {
        try {
            Method m = MapMarkerUtil.class.getDeclaredMethod("dpToPx", Context.class, int.class);
            m.setAccessible(true);
            return (int) m.invoke(null, ctx, dp);
        } catch (Exception e) {
            fail("Reflection invoke failed: " + e.getMessage());
            return -1;
        }
    }

    @Test
    public void dpToPx_convertsExpected() {
        Context ctx = Robolectric.buildActivity(android.app.Activity.class).create().get();
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        // For a given dp value, expected px = round(dp * density)
        int dp = 16;
        int expected = Math.round(dp * metrics.density);
        int actual = invokeDpToPx(ctx, dp);
        assertEquals(expected, actual);
    }

    @Test
    public void dpToPx_zeroReturnsZero() {
        Context ctx = Robolectric.buildActivity(android.app.Activity.class).create().get();
        assertEquals(0, invokeDpToPx(ctx, 0));
    }
}

