/*
 * Device utility helper for retrieving stable per-install identifiers.
 * Centralizes access to ANDROID_ID for consistent querying.
 */

package ca.team.originkickoff.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

/**
 * Utility class for device-specific helpers.
 */
public class DeviceUtils {

    /**
     * Returns the ANDROID_ID for the current device (unique per app signing key + user).
     *
     * @param context application or activity context
     * @return stable device identifier string
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
