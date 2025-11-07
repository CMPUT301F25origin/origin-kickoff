package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaitingListEntryTest {

    @Test
    public void testDocIdComposition() {
        String id = WaitingListEntry.docId("evt1", "usr1");
        assertEquals("evt1_usr1", id);
    }

    @Test
    public void testConstructorAndGetters() {
        Timestamp ts = Timestamp.now();
        WaitingListEntry e = new WaitingListEntry("evtX", "userY", ts, "qr", "active");
        assertEquals("evtX", e.getEventId());
        assertEquals("userY", e.getUserId());
        assertEquals(ts, e.getJoinedAt());
        assertEquals("qr", e.getSource());
        assertEquals("active", e.getState());
    }

    @Test
    public void testOptionalGeoFieldsNullByDefault() {
        WaitingListEntry e = new WaitingListEntry();
        assertNull(e.getLat());
        assertNull(e.getLon());
        assertNull(e.getPrecisionMeters());
        assertFalse(e.isLocationConsent());
    }

    @Test
    public void testSettersIncludingGeo() {
        WaitingListEntry e = new WaitingListEntry();
        e.setEventId("E");
        e.setUserId("U");
        Timestamp t = Timestamp.now();
        e.setJoinedAt(t);
        e.setSource("list");
        e.setState("active");
        e.setLat(53.5);
        e.setLon(-113.4);
        e.setPrecisionMeters(25);
        e.setLocationConsent(true);

        assertEquals("E", e.getEventId());
        assertEquals("U", e.getUserId());
        assertEquals(t, e.getJoinedAt());
        assertEquals("list", e.getSource());
        assertEquals("active", e.getState());
        assertEquals(Double.valueOf(53.5), e.getLat());
        assertEquals(Double.valueOf(-113.4), e.getLon());
        assertEquals(Integer.valueOf(25), e.getPrecisionMeters());
        assertTrue(e.isLocationConsent());
    }
}

