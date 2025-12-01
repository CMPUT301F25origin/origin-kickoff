package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for Event model focusing on core logic methods and field assignments.
 */
public class EventTest {

    @Test
    public void defaultConstructor_fieldsNullOrZero() {
        Event e = new Event();
        assertNull(e.getId());
        assertNull(e.getName());
        assertNull(e.getDescription());
        assertNull(e.getOrganizerId());
        assertNull(e.getOrganizerName());
        assertNull(e.getEventDate());
        assertNull(e.getLocation());
        assertEquals(0.0, e.getLocationLatitude(), 0.0);
        assertEquals(0.0, e.getLocationLongitude(), 0.0);
        assertNull(e.getLocationPlaceId());
        assertNull(e.getCategory());
        assertEquals(0, e.getCapacity());
        assertEquals(0, e.getWaitlistCount());
        assertEquals(0, e.getSelectionSize());
        assertFalse(e.isLimitWaitlist());
        assertEquals(0, e.getWaitlistLimit());
        assertNull(e.getRegistrationStartTime());
        assertNull(e.getRegistrationEndTime());
        assertNull(e.getPosterUrl());
        assertNull(e.getPosterBase64());
        assertFalse(e.isGeolocationRequired());
        assertEquals(0.0, e.getPrice(), 0.0);
        assertEquals(0L, e.getCreatedAt());
        assertNull(e.getQrCodeUrl());
        assertNull(e.getQrCodeBase64());
        assertNull(e.getLotteryCriteria());
        assertNull(e.getLotteryStatus());
        assertFalse(e.isRegistrationOpen());
        assertFalse(e.hasAvailableSpots()); // 0 < 0 is false, so no available spots when capacity == waitlistCount == 0
    }

    @Test
    public void hasAvailableSpots_whenCapacityGreaterThanWaitlist_returnsTrue() {
        Event e = new Event();
        e.setCapacity(10);
        e.setWaitlistCount(5);
        assertTrue(e.hasAvailableSpots());
    }

    @Test
    public void hasAvailableSpots_whenCapacityReached_returnsFalse() {
        Event e = new Event();
        e.setCapacity(5);
        e.setWaitlistCount(5);
        assertFalse(e.hasAvailableSpots());
    }

    @Test
    public void isRegistrationOpen_withinWindow_returnsTrue() {
        Event e = new Event();
        long now = System.currentTimeMillis();
        e.setRegistrationStartTime(new Date(now - 1000));
        e.setRegistrationEndTime(new Date(now + 1000));
        assertTrue(e.isRegistrationOpen());
    }

    @Test
    public void isRegistrationOpen_outsideWindow_returnsFalse() {
        Event e = new Event();
        long now = System.currentTimeMillis();
        e.setRegistrationStartTime(new Date(now - 5000));
        e.setRegistrationEndTime(new Date(now - 1000));
        assertFalse(e.isRegistrationOpen());
    }

    @Test
    public void isRegistrationOpen_nullWindow_returnsFalse() {
        Event e = new Event();
        e.setRegistrationStartTime(null);
        e.setRegistrationEndTime(null);
        assertFalse(e.isRegistrationOpen());
    }
}
