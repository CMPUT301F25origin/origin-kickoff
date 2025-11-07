package ca.team.originkickoff.models;

import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class UserTest {

    @Test
    public void testDefaultsAndSetters() {
        User u = new User();
        u.setId("user123");
        u.setDeviceId("dev456");
        u.setDisplayName("Alice");
        u.setEmail("alice@example.com");
        u.setPhone("555-1212");
        u.setNotifMarketing(true);
        u.setNotifService(false);
        u.setOrganizer(true);
        u.setAdmin(false);
        Date created = new Date();
        Date updated = new Date(created.getTime() + 1000);
        u.setCreatedAt(created);
        u.setUpdatedAt(updated);

        assertEquals("user123", u.getId());
        assertEquals("dev456", u.getDeviceId());
        assertEquals("Alice", u.getDisplayName());
        assertEquals("alice@example.com", u.getEmail());
        assertEquals("555-1212", u.getPhone());
        assertTrue(u.isNotifMarketing());
        assertFalse(u.isNotifService());
        assertTrue(u.isOrganizer());
        assertFalse(u.isAdmin());
        assertEquals(created, u.getCreatedAt());
        assertEquals(updated, u.getUpdatedAt());
    }

    @Test
    public void testNullFieldsAllowed() {
        User u = new User();
        assertNull(u.getId());
        assertNull(u.getDeviceId());
        assertNull(u.getDisplayName());
        assertNull(u.getEmail());
        assertNull(u.getPhone());
        assertNull(u.getCreatedAt());
        assertNull(u.getUpdatedAt());
    }
}

