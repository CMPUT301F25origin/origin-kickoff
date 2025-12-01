package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link EventLocation}. Fast, deterministic, no Android dependencies.
 */
public class EventLocationTest {

    @Test
    public void defaultConstructor_hasNullsAndZeroes() {
        EventLocation loc = new EventLocation();
        assertNull(loc.getAddress());
        assertEquals(0.0, loc.getLatitude(), 0.0);
        assertEquals(0.0, loc.getLongitude(), 0.0);
        assertNull(loc.getPlaceId());
        assertNull(loc.toString()); // toString returns address
    }

    @Test
    public void fullConstructor_assignsAllFields() {
        EventLocation loc = new EventLocation("123 Main St", 45.5, -73.6, "PLACE_ID_123");
        assertEquals("123 Main St", loc.getAddress());
        assertEquals(45.5, loc.getLatitude(), 0.0);
        assertEquals(-73.6, loc.getLongitude(), 0.0);
        assertEquals("PLACE_ID_123", loc.getPlaceId());
        assertEquals("123 Main St", loc.toString());
    }

    @Test
    public void threeArgConstructor_setsPlaceIdNull() {
        EventLocation loc = new EventLocation("Park", 10.1, 20.2);
        assertEquals("Park", loc.getAddress());
        assertEquals(10.1, loc.getLatitude(), 0.0);
        assertEquals(20.2, loc.getLongitude(), 0.0);
        assertNull(loc.getPlaceId());
        assertEquals("Park", loc.toString());
    }

    @Test
    public void setters_updateFields_andToStringReflectsAddress() {
        EventLocation loc = new EventLocation();
        loc.setAddress("New Address");
        loc.setLatitude(1.234);
        loc.setLongitude(-9.876);
        loc.setPlaceId("PID");
        assertEquals("New Address", loc.getAddress());
        assertEquals(1.234, loc.getLatitude(), 0.0);
        assertEquals(-9.876, loc.getLongitude(), 0.0);
        assertEquals("PID", loc.getPlaceId());
        assertEquals("New Address", loc.toString());
    }
}

