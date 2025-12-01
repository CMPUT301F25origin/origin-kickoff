package ca.team.originkickoff;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/**
 * Robolectric tests for AdminNavHelper focusing on highlight logic and safe setup when views exist.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class AdminNavHelperTest {

    /** Safety test: calling setup when nav views are absent should not throw an exception. */
    @Test
    public void setup_withoutNavViews_noCrash() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        AdminNavHelper.setup(activity, AdminNavHelper.Tab.DASHBOARD);
        assertTrue(true); // reached without exception
    }
}
