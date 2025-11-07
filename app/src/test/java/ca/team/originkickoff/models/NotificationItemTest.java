package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationItemTest {

    @Test
    public void testConstructorsAndSetters() {
        NotificationItem item = new NotificationItem("id1", "Title", "Message", "tsString");
        assertEquals("id1", item.getId());
        assertEquals("Title", item.getTitle());
        assertEquals("Message", item.getMessage());
        assertEquals("tsString", item.getTimestamp());

        item.setType("result");
        item.setEventId("evt99");
        item.setUserId("userX");
        Timestamp created = Timestamp.now();
        item.setCreatedAt(created);
        item.setRead(true);

        assertEquals("result", item.getType());
        assertEquals("evt99", item.getEventId());
        assertEquals("userX", item.getUserId());
        assertEquals(created, item.getCreatedAt());
        assertTrue(item.isRead());
    }

    @Test
    public void testDefaultValues() {
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
}

