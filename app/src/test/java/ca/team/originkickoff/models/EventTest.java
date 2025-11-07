package ca.team.originkickoff.models;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class EventTest {

    private Event event;
    private final long now = System.currentTimeMillis();

    @Before
    public void setUp() {
        event = new Event();
    }

    // region Constructor Tests
    @Test
    public void testEmptyConstructor() {
        // Test Case: 0 (Zero) - Ensure default values are null or zero
        Event newEvent = new Event();
        assertNull("ID should be null", newEvent.getId());
        assertNull("Name should be null", newEvent.getName());
        assertNull("Description should be null", newEvent.getDescription());
        assertNull("EventDate should be null", newEvent.getEventDate());
        assertEquals("Capacity should be 0", 0, newEvent.getCapacity());
        assertEquals("Price should be 0.0", 0.0, newEvent.getPrice(), 0.0);
        assertFalse("isGeolocationRequired should be false", newEvent.isGeolocationRequired());
    }

    @Test
    public void testParameterizedConstructor() {
        // Test Case: 1 (One) - Ensure all fields are set correctly
        Date eventDate = new Date(now);
        Date regStartDate = new Date(now - 100000);
        Date regEndDate = new Date(now + 100000);

        Event fullEvent = new Event(
                "evt-001", "Community BBQ", "A fun community event.", "org-123", "City Hall",
                eventDate, "Central Park", "Community", 200, 10,
                regStartDate, regEndDate, "http://example.com/poster.jpg", "base64string",
                true, 15.50, now, "http://example.com/qr.png", "all_attendees"
        );

        assertEquals("evt-001", fullEvent.getId());
        assertEquals("Community BBQ", fullEvent.getName());
        assertEquals("A fun community event.", fullEvent.getDescription());
        assertEquals("org-123", fullEvent.getOrganizerId());
        assertEquals("City Hall", fullEvent.getOrganizerName());
        assertEquals(eventDate, fullEvent.getEventDate());
        assertEquals("Central Park", fullEvent.getLocation());
        assertEquals("Community", fullEvent.getCategory());
        assertEquals(200, fullEvent.getCapacity());
        assertEquals(10, fullEvent.getWaitlistCount());
        assertEquals(regStartDate, fullEvent.getRegistrationStartTime());
        assertEquals(regEndDate, fullEvent.getRegistrationEndTime());
        assertEquals("http://example.com/poster.jpg", fullEvent.getPosterUrl());
        assertEquals("base64string", fullEvent.getPosterBase64());
        assertTrue(fullEvent.isGeolocationRequired());
        assertEquals(15.50, fullEvent.getPrice(), 0.0);
        assertEquals(now, fullEvent.getCreatedAt());
        assertEquals("http://example.com/qr.png", fullEvent.getQrCodeUrl());
        assertEquals("all_attendees", fullEvent.getLotteryCriteria());
    }
    // endregion

    // region Getters and Setters Tests
    @Test
    public void testGetAndSetId() {
        assertNull(event.getId()); // Null case
        event.setId("test-id");
        assertEquals("test-id", event.getId());
    }

    @Test
    public void testGetAndSetName() {
        assertNull(event.getName()); // Null case
        event.setName("New Year Gala");
        assertEquals("New Year Gala", event.getName());
    }

    @Test
    public void testGetAndSetDescription() {
        assertNull(event.getDescription()); // Null case
        event.setDescription("A description.");
        assertEquals("A description.", event.getDescription());
    }

    @Test
    public void testGetAndSetDate() {
        assertNull(event.getEventDate()); // Null case
        Date date = new Date();
        event.setEventDate(date);
        assertEquals(date, event.getEventDate());
    }

    @Test
    public void testGetAndSetCapacity() {
        assertEquals(0, event.getCapacity()); // Zero case
        event.setCapacity(150);
        assertEquals(150, event.getCapacity());
    }
    // endregion

    // region Helper Method Tests
    @Test
    public void testIsRegistrationOpen_WhenOpen() {
        // Test Case: More than 1 (Range of time)
        Date startTime = new Date(now - 1000); // 1 second in the past
        Date endTime = new Date(now + 1000);   // 1 second in the future
        event.setRegistrationStartTime(startTime);
        event.setRegistrationEndTime(endTime);
        assertTrue("Registration should be open", event.isRegistrationOpen());
    }

    @Test
    public void testIsRegistrationOpen_WhenClosed() {
        Date startTime = new Date(now - 2000);
        Date endTime = new Date(now - 1000);
        event.setRegistrationStartTime(startTime);
        event.setRegistrationEndTime(endTime);
        assertFalse("Registration should be closed", event.isRegistrationOpen());
    }

    @Test
    public void testIsRegistrationOpen_WhenNotStarted() {
        Date startTime = new Date(now + 1000);
        Date endTime = new Date(now + 2000);
        event.setRegistrationStartTime(startTime);
        event.setRegistrationEndTime(endTime);
        assertFalse("Registration should not have started yet", event.isRegistrationOpen());
    }
    
    @Test
    public void testIsRegistrationOpen_WithNullDates() {
        event.setRegistrationStartTime(null); // Null case
        event.setRegistrationEndTime(null);
        assertFalse("Registration should be closed if dates are null", event.isRegistrationOpen());
    }

    @Test
    public void testHasAvailableSpots_WhenSpotsAvailable() {
        // Test Case: More than 1
        event.setCapacity(100);
        event.setWaitlistCount(50);
        assertTrue("Should have available spots", event.hasAvailableSpots());
    }
    
    @Test
    public void testHasAvailableSpots_WhenFull() {
        // Test Case: 1 (Exactly one state - full)
        event.setCapacity(100);
        event.setWaitlistCount(100);
        assertFalse("Should not have available spots when full", event.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_WhenOverfilled() {
        event.setCapacity(100);
        event.setWaitlistCount(120); // More on waitlist than capacity
        assertFalse("Should not have available spots when overfilled", event.hasAvailableSpots());
    }

    @Test
    public void testHasAvailableSpots_WithZeroCapacity() {
        // Test Case: 0 (Zero)
        event.setCapacity(0);
        event.setWaitlistCount(0);
        assertFalse("Should not have spots with zero capacity", event.hasAvailableSpots());
    }
    // endregion
}
