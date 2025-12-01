package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for User model.
 */
public class UserTest {

    @Test
    public void defaultConstructor_fieldsNullAndBooleansFalse() {
        User u = new User();
        assertNull(u.getId());
        assertNull(u.getDeviceId());
        assertNull(u.getDisplayName());
        assertNull(u.getEmail());
        assertNull(u.getPhone());
        assertFalse(u.isNotifMarketing());
        assertFalse(u.isNotifService());
        assertFalse(u.isOrganizer());
        assertFalse(u.isAdmin());
        assertNull(u.getCreatedAt());
        assertNull(u.getUpdatedAt());
    }

    @Test
    public void setters_updateFields() {
        User u = new User();
        Date created = new Date(1000L);
        Date updated = new Date(2000L);
        u.setId("U1");
        u.setDeviceId("DEV1");
        u.setDisplayName("Alice");
        u.setEmail("alice@example.com");
        u.setPhone("+15550001");
        u.setNotifMarketing(true);
        u.setNotifService(true);
        u.setOrganizer(true);
        u.setAdmin(true);
        u.setCreatedAt(created);
        u.setUpdatedAt(updated);
        assertEquals("U1", u.getId());
        assertEquals("DEV1", u.getDeviceId());
        assertEquals("Alice", u.getDisplayName());
        assertEquals("alice@example.com", u.getEmail());
        assertEquals("+15550001", u.getPhone());
        assertTrue(u.isNotifMarketing());
        assertTrue(u.isNotifService());
        assertTrue(u.isOrganizer());
        assertTrue(u.isAdmin());
        assertEquals(created, u.getCreatedAt());
        assertEquals(updated, u.getUpdatedAt());
    }
}

