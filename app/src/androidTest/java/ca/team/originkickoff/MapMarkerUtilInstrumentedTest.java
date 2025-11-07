package ca.team.originkickoff;

import android.content.Context;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MapMarkerUtilInstrumentedTest {

    @Before
    public void setUp() {
        // It's important to initialize the MapsInitializer before using any Google Maps components.
        MapsInitializer.initialize(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testBitmapDescriptorFromVectorInvalidThrows() {
        Context ctx = ApplicationProvider.getApplicationContext();
        try {
            MapMarkerUtil.bitmapDescriptorFromVector(ctx, -1, 24, MapMarkerUtil.BLUE_TINT);
            fail("Expected Resources.NotFoundException for invalid resource");
        } catch (Resources.NotFoundException expected) {
            // pass
        }
    }

    @Test
    public void testBitmapDescriptorFromVectorValid() {
        Context ctx = ApplicationProvider.getApplicationContext();
        // Use android built-in ic_delete (may vary) - fallback skip test if not found
        int resId = android.R.drawable.ic_delete; // built-in drawable guaranteed to exist
        BitmapDescriptor desc = MapMarkerUtil.bitmapDescriptorFromVector(ctx, resId, 24, MapMarkerUtil.BLUE_TINT);
        assertNotNull(desc);
    }
}
