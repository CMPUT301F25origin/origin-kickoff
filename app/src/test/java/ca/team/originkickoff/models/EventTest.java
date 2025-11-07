package ca.team.originkickoff.models;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for Event model. Covers all getters/setters (basic integrity),
 * and edge cases for helper methods isRegistrationOpen() and hasAvailableSpots().
 */
public class EventTest {

    // Helper to build a minimal Event instance
    private Event buildBaseEvent() {
        Event e = new Event();
        e.setId("evt-1");
        e.setName("Test Event");
        e.setDescription("Desc");
        e.setOrganizerId("org-1");
        e.setOrganizerName("Organizer");
        e.setCapacity(10);
        e.setWaitlistCount(0);
        e.setPrice(0.0);
        e.setCreatedAt(System.currentTimeMillis());
        return e;
    }

    @Test
    public void testGettersAndSettersIntegrity() {
        Event e = buildBaseEvent();
        e.setCategory("sports");
        e.setPosterUrl("http://example.com/poster.png");
        e.setPosterBase64("base64data");
        e.setSelectionSize(5);
        e.setLimitWaitlist(true);
        e.setWaitlistLimit(100);
        e.setGeolocationRequired(true);
        e.setQrCodeUrl("http://qr");
        e.setQrCodeBase64("qrbase64");
        e.setLotteryCriteria("random");
        e.setLotteryStatus("not_conducted");

        assertEquals("evt-1", e.getId());
        assertEquals("Test Event", e.getName());
        assertEquals("Desc", e.getDescription());
        assertEquals("org-1", e.getOrganizerId());
        assertEquals("Organizer", e.getOrganizerName());
        assertEquals("sports", e.getCategory());
        assertEquals("http://example.com/poster.png", e.getPosterUrl());
        assertEquals("base64data", e.getPosterBase64());
        assertEquals(5, e.getSelectionSize());
        assertTrue(e.isLimitWaitlist());
        assertEquals(100, e.getWaitlistLimit());
        assertTrue(e.isGeolocationRequired());
        assertEquals("http://qr", e.getQrCodeUrl());
        assertEquals("qrbase64", e.getQrCodeBase64());
        assertEquals("random", e.getLotteryCriteria());
        assertEquals("not_conducted", e.getLotteryStatus());
    }

    // Registration window tests

    @Test
    public void testRegistrationOpen_NullTimes_False() {
        Event e = buildBaseEvent();
        // start/end unset
        assertFalse(e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_StartAfterEnd_False() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        e.setRegistrationStartTime(new Date(now + 10_000));
        e.setRegistrationEndTime(new Date(now - 10_000));
        assertFalse(e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_BeforeStart_False() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        e.setRegistrationStartTime(new Date(now + 5_000));
        e.setRegistrationEndTime(new Date(now + 10_000));
        assertFalse(e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_AtStart_True() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        Date start = new Date(now);
        Date end = new Date(now + 10_000);
        e.setRegistrationStartTime(start);
        e.setRegistrationEndTime(end);
        assertTrue("Should be open exactly at start boundary", e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_Between_True() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        e.setRegistrationStartTime(new Date(now - 5_000));
        e.setRegistrationEndTime(new Date(now + 5_000));
        assertTrue(e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_AtEnd_True() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        Date start = new Date(now - 10_000);
        // Put end slightly in the future to avoid race condition with currentTime fetch
        Date end = new Date(now + 1_000);
        e.setRegistrationStartTime(start);
        e.setRegistrationEndTime(end);
        assertTrue("Should be open exactly at end boundary (with small buffer)", e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_AfterEnd_False() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        e.setRegistrationStartTime(new Date(now - 10_000));
        e.setRegistrationEndTime(new Date(now - 5_000));
        assertFalse(e.isRegistrationOpen());
    }

    @Test
    public void testRegistrationOpen_LongDuration_True() {
        Event e = buildBaseEvent();
        long now = System.currentTimeMillis();
        // Very long window (simulate multi-month event)
        e.setRegistrationStartTime(new Date(now - 30L * 24 * 3600 * 1000)); // 30 days ago
        e.setRegistrationEndTime(new Date(now + 30L * 24 * 3600 * 1000)); // 30 days ahead
        assertTrue(e.isRegistrationOpen());
    }

    // Spot availability tests

    @Test
    public void testHasAvailableSpots_WaitlistLessThanCapacity_True() {
        Event e = buildBaseEvent();
        e.setCapacity(10);
        e.setWaitlistCount(9);
        assertTrue(e.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_WaitlistEqualsCapacity_False() {
        Event e = buildBaseEvent();
        e.setCapacity(10);
        e.setWaitlistCount(10);
        assertFalse(e.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_WaitlistGreaterThanCapacity_False() {
        Event e = buildBaseEvent();
        e.setCapacity(10);
        e.setWaitlistCount(11);
        assertFalse(e.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_CapacityZero_False() {
        Event e = buildBaseEvent();
        e.setCapacity(0);
        e.setWaitlistCount(0);
        assertFalse("Zero capacity should report no available spots", e.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_NegativeCapacity_False() {
        Event e = buildBaseEvent();
        e.setCapacity(-1);
        e.setWaitlistCount(0);
        assertFalse("Negative capacity should be treated as full", e.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_LargeNumbers() {
        Event e = buildBaseEvent();
        e.setCapacity(100_000);
        e.setWaitlistCount(99_999);
        assertTrue(e.hasAvailableSpots());
        e.setWaitlistCount(100_000);
        assertFalse(e.hasAvailableSpots());
    }
}
