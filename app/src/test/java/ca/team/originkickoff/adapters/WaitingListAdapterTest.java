package ca.team.originkickoff.adapters;

import com.google.firebase.Timestamp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Lightweight tests for WaitingListAdapter's private relative time formatter using reflection.
 * Avoids constructor and submit() to prevent Firebase & RecyclerView observable initialization.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class WaitingListAdapterTest {

    private WaitingListAdapter newAdapterUnsafe() {
        try {
            Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
            return (WaitingListAdapter) allocate.invoke(unsafe, WaitingListAdapter.class);
        } catch (Exception e) {
            fail("Unsafe allocate failed: " + e.getMessage());
            return null;
        }
    }

    private String callCalculateJoinedAgo(Timestamp ts) {
        try {
            Method m = WaitingListAdapter.class.getDeclaredMethod("calculateJoinedAgo", Timestamp.class);
            m.setAccessible(true);
            WaitingListAdapter adapter = newAdapterUnsafe();
            assertNotNull(adapter);
            return (String) m.invoke(adapter, ts);
        } catch (Exception e) {
            fail("Reflection invoke failed: " + e.getMessage());
            return null;
        }
    }

    @Test
    public void relativeTime_null() {
        assertEquals("Joined recently", callCalculateJoinedAgo(null));
    }

    @Test
    public void relativeTime_justNow() {
        long nowSec = System.currentTimeMillis() / 1000;
        assertEquals("Joined just now", callCalculateJoinedAgo(new Timestamp(nowSec - 30, 0))); // 30s ago
    }

    @Test
    public void relativeTime_minutesHoursDays() {
        long nowSec = System.currentTimeMillis() / 1000;
        assertEquals("Joined 2 minutes ago", callCalculateJoinedAgo(new Timestamp(nowSec - 120, 0)));
        assertEquals("Joined 1 hour ago", callCalculateJoinedAgo(new Timestamp(nowSec - 3600, 0)));
        assertEquals("Joined 3 hours ago", callCalculateJoinedAgo(new Timestamp(nowSec - 3 * 3600, 0)));
        assertEquals("Joined 1 day ago", callCalculateJoinedAgo(new Timestamp(nowSec - 24 * 3600, 0)));
        assertEquals("Joined 3 days ago", callCalculateJoinedAgo(new Timestamp(nowSec - 3 * 24 * 3600, 0)));
    }
}
