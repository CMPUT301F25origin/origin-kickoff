package ca.team.originkickoff.models;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class UserTest {

    private User user;

    @Before
    public void setUp() {
        user = new User();
    }

    // region Constructor Test
    @Test
    public void testEmptyConstructor() {
        // Test Case: 0 (Zero) - Ensure default values are null, 0, or false
        assertNull("ID should be null", user.getId());
        assertNull("DeviceId should be null", user.getDeviceId());
        assertNull("DisplayName should be null", user.getDisplayName());
        assertNull("Email should be null", user.getEmail());
        assertNull("Phone should be null", user.getPhone());
        assertFalse("NotifMarketing should be false", user.isNotifMarketing());
        assertFalse("NotifService should be false", user.isNotifService());
        assertFalse("IsOrganizer should be false", user.isOrganizer());
        assertFalse("IsAdmin should be false", user.isAdmin());
        assertNull("CreatedAt should be null", user.getCreatedAt());
        assertNull("UpdatedAt should be null", user.getUpdatedAt());
    }
    // endregion

    // region Getters and Setters Tests
    @Test
    public void testGetAndSetId() {
        assertNull(user.getId()); // Null case
        user.setId("user-123");
        assertEquals("user-123", user.getId());
    }

    @Test
    public void testGetAndSetDeviceId() {
        assertNull(user.getDeviceId()); // Null case
        user.setDeviceId("device-abc");
        assertEquals("device-abc", user.getDeviceId());
    }

    @Test
    public void testGetAndSetDisplayName() {
        assertNull(user.getDisplayName()); // Null case
        user.setDisplayName("John Doe");
        assertEquals("John Doe", user.getDisplayName());
    }

    @Test
    public void testGetAndSetEmail() {
        assertNull(user.getEmail()); // Null case
        user.setEmail("john.doe@example.com");
        assertEquals("john.doe@example.com", user.getEmail());
    }

    @Test
    public void testGetAndSetPhone() {
        assertNull(user.getPhone()); // Null case
        user.setPhone("123-456-7890");
        assertEquals("123-456-7890", user.getPhone());
    }

    @Test
    public void testIsNotifMarketing() {
        assertFalse(user.isNotifMarketing()); // Default value
        user.setNotifMarketing(true);
        assertTrue(user.isNotifMarketing());
    }

    @Test
    public void testIsNotifService() {
        assertFalse(user.isNotifService()); // Default value
        user.setNotifService(true);
        assertTrue(user.isNotifService());
    }

    @Test
    public void testIsOrganizer() {
        assertFalse(user.isOrganizer()); // Default value
        user.setOrganizer(true);
        assertTrue(user.isOrganizer());
    }

    @Test
    public void testIsAdmin() {
        assertFalse(user.isAdmin()); // Default value
        user.setAdmin(true);
        assertTrue(user.isAdmin());
    }

    @Test
    public void testGetAndSetCreatedAt() {
        assertNull(user.getCreatedAt()); // Null case
        Date now = new Date();
        user.setCreatedAt(now);
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    public void testGetAndSetUpdatedAt() {
        assertNull(user.getUpdatedAt()); // Null case
        Date now = new Date();
        user.setUpdatedAt(now);
        assertEquals(now, user.getUpdatedAt());
    }
    // endregion
}