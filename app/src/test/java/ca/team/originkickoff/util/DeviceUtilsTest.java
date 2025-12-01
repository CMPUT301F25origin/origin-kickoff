package ca.team.originkickoff.util;

import android.content.Context;
import android.provider.Settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Robolectric unit tests for DeviceUtils.getDeviceId (non-instrumented).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DeviceUtilsTest {

    @Test
    public void getDeviceId_returnsAndroidId() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ANDROID_ID, "test-device-id-123");
        String id = DeviceUtils.getDeviceId(context);
        assertEquals("test-device-id-123", id);
    }

    @Test
    public void getDeviceId_emptyId_returnsNullOrEmptyMatchesUnderlying() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ANDROID_ID, "");
        String id = DeviceUtils.getDeviceId(context);
        assertTrue(id == null || id.isEmpty());
    }
}

