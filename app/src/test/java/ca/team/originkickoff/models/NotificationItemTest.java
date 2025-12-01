package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for NotificationItem verifying simple POJO behavior without Firebase runtime.
 */
public class NotificationItemTest {

    @Test
    public void defaultConstructor_fieldsNullAndReadFalse() {
        NotificationItem item = new NotificationItem();
        assertNull(item.getId());
        assertNull(item.getTitle());
        assertNull(item.getMessage());
        assertNull(item.getTimestamp());
        assertNull(item.getType());
        assertNull(item.getEventId());
        assertNull(item.getUserId());
        assertNull(item.getCreatedAt());
        assertFalse(item.isRead());
    }

    @Test
    public void convenienceConstructor_setsBasicFields() {
        NotificationItem item = new NotificationItem("N1", "Title", "Body", "2025-12-01T10:00:00Z");
        assertEquals("N1", item.getId());
        assertEquals("Title", item.getTitle());
        assertEquals("Body", item.getMessage());
        assertEquals("2025-12-01T10:00:00Z", item.getTimestamp());
        // others remain null/default
        assertNull(item.getType());
        assertNull(item.getEventId());
        assertNull(item.getUserId());
        assertNull(item.getCreatedAt());
        assertFalse(item.isRead());
    }

    @Test
    public void setters_updateFields_andReadFlag() {
        NotificationItem item = new NotificationItem();
        item.setId("N2");
        item.setTitle("New Title");
        item.setMessage("New Body");
        item.setTimestamp("TS");
        item.setType("result");
        item.setEventId("E1");
        item.setUserId("U1");
        item.setRead(true);
        // createdAt left null to avoid Firebase dependency
        assertEquals("N2", item.getId());
        assertEquals("New Title", item.getTitle());
        assertEquals("New Body", item.getMessage());
        assertEquals("TS", item.getTimestamp());
        assertEquals("result", item.getType());
        assertEquals("E1", item.getEventId());
        assertEquals("U1", item.getUserId());
        assertTrue(item.isRead());
        assertNull(item.getCreatedAt());
    }
}

