package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for WaitingListEntry verifying simple POJO logic without Firebase Timestamp instantiation.
 */
public class WaitingListEntryTest {

    @Test
    public void docId_concatenatesEventAndUser() {
        assertEquals("E1_U1", WaitingListEntry.docId("E1", "U1"));
        assertEquals("event-user_user-123", WaitingListEntry.docId("event-user", "user-123"));
    }

    @Test
    public void defaultConstructor_fieldsNullAndBooleansFalse() {
        WaitingListEntry e = new WaitingListEntry();
        assertNull(e.getEventId());
        assertNull(e.getUserId());
        assertNull(e.getJoinedAt());
        assertNull(e.getSource());
        assertNull(e.getState());
        assertNull(e.getLat());
        assertNull(e.getLon());
        assertNull(e.getPrecisionMeters());
        assertFalse(e.isLocationConsent());
    }

    @Test
    public void convenienceConstructor_setsBasicFields() {
        WaitingListEntry e = new WaitingListEntry("E2", "U2", null, "qr", "active");
        assertEquals("E2", e.getEventId());
        assertEquals("U2", e.getUserId());
        assertNull(e.getJoinedAt()); // passed null intentionally
        assertEquals("qr", e.getSource());
        assertEquals("active", e.getState());
        // others remain null/default
        assertNull(e.getLat());
        assertNull(e.getLon());
        assertNull(e.getPrecisionMeters());
        assertFalse(e.isLocationConsent());
    }

    @Test
    public void setters_updateOptionalFields() {
        WaitingListEntry e = new WaitingListEntry();
        e.setEventId("E3");
        e.setUserId("U3");
        e.setSource("list");
        e.setState("left");
        e.setLat(12.34);
        e.setLon(-56.78);
        e.setPrecisionMeters(5);
        e.setLocationConsent(true);
        assertEquals("E3", e.getEventId());
        assertEquals("U3", e.getUserId());
        assertEquals("list", e.getSource());
        assertEquals("left", e.getState());
        assertEquals(Double.valueOf(12.34), e.getLat());
        assertEquals(Double.valueOf(-56.78), e.getLon());
        assertEquals(Integer.valueOf(5), e.getPrecisionMeters());
        assertTrue(e.isLocationConsent());
        assertNull(e.getJoinedAt()); // left null
    }
}

