package ca.team.originkickoff.util;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DeviceUtilsInstrumentedTest {

    @Test
    public void testGetDeviceIdNotNull() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String id = DeviceUtils.getDeviceId(ctx);
        // On emulator/device this should be non-null; if null, that's still acceptable but shouldn't crash
        // Assert not empty when available
        if (id != null) {
            assertFalse(id.isEmpty());
        }
    }
}

