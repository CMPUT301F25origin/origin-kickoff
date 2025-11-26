package ca.team.originkickoff.models;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventLocationTest {

    private EventLocation location;

    @Before
    public void setUp() {
        location = new EventLocation();
    }

    // region Constructor Tests
    @Test
    public void testEmptyConstructor() {
        // Test Case: 0 (Zero) - Ensure default values are null or zero
        assertNull("Address should be null", location.getAddress());
        assertEquals("Latitude should be 0.0", 0.0, location.getLatitude(), 0.0);
        assertEquals("Longitude should be 0.0", 0.0, location.getLongitude(), 0.0);
        assertNull("PlaceId should be null", location.getPlaceId());
    }

    @Test
    public void testFullConstructor() {
        // Test Case: 1 (One) - All arguments
        EventLocation fullLocation = new EventLocation("123 Main St, Anytown", 40.7128, -74.0060, "ChIJgUbEo8FbwokR5C4e9vJkM");
        assertEquals("123 Main St, Anytown", fullLocation.getAddress());
        assertEquals(40.7128, fullLocation.getLatitude(), 0.0);
        assertEquals(-74.0060, fullLocation.getLongitude(), 0.0);
        assertEquals("ChIJgUbEo8FbwokR5C4e9vJkM", fullLocation.getPlaceId());
    }

    @Test
    public void testConstructorWithoutPlaceId() {
        // Test Case: More than 1 (Multiple arguments, one variant)
        EventLocation noPlaceIdLocation = new EventLocation("City Park", 40.7128, -74.0060);
        assertEquals("City Park", noPlaceIdLocation.getAddress());
        assertEquals(40.7128, noPlaceIdLocation.getLatitude(), 0.0);
        assertEquals(-74.0060, noPlaceIdLocation.getLongitude(), 0.0);
        assertNull("PlaceId should be null", noPlaceIdLocation.getPlaceId());
    }
    // endregion

    // region Getters and Setters Tests
    @Test
    public void testGetAndSetAddress() {
        assertNull(location.getAddress()); // Null case
        location.setAddress("University Campus");
        assertEquals("University Campus", location.getAddress());
    }

    @Test
    public void testGetAndSetLatitude() {
        assertEquals(0.0, location.getLatitude(), 0.0); // Zero case
        location.setLatitude(53.5232);
        assertEquals(53.5232, location.getLatitude(), 0.0);
    }

    @Test
    public void testGetAndSetLongitude() {
        assertEquals(0.0, location.getLongitude(), 0.0); // Zero case
        location.setLongitude(-113.5263);
        assertEquals(-113.5263, location.getLongitude(), 0.0);
    }

    @Test
    public void testGetAndSetPlaceId() {
        assertNull(location.getPlaceId()); // Null case
        location.setPlaceId("some-google-place-id");
        assertEquals("some-google-place-id", location.getPlaceId());
    }
    // endregion

    // region Other Method Tests
    @Test
    public void testToString() {
        assertNull("toString() should return null for a null address", location.toString()); // Null case
        location.setAddress("123 Test Street");
        assertEquals("toString() should return the address", "123 Test Street", location.toString());
    }
    // endregion
}