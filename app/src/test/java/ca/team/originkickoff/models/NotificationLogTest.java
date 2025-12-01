package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for NotificationLog focusing on simple POJO behavior without Firebase runtime.
 */
public class NotificationLogTest {

    @Test
    public void defaultConstructor_fieldsNull() {
        NotificationLog log = new NotificationLog();
        assertNull(log.getId());
        assertNull(log.getEventId());
        assertNull(log.getEventName());
        assertNull(log.getSenderId());
        assertNull(log.getSenderName());
        assertNull(log.getRecipientId());
        assertNull(log.getRecipientName());
        assertNull(log.getType());
        assertNull(log.getCreatedAt());
    }

    @Test
    public void setters_updateFields() {
        NotificationLog log = new NotificationLog();
        log.setId("L1");
        log.setEventId("E1");
        log.setEventName("Event Name");
        log.setSenderId("S1");
        log.setSenderName("Sender");
        log.setRecipientId("R1");
        log.setRecipientName("Recipient");
        log.setType("broadcast");
        // createdAt left null to avoid Firebase dependency
        assertEquals("L1", log.getId());
        assertEquals("E1", log.getEventId());
        assertEquals("Event Name", log.getEventName());
        assertEquals("S1", log.getSenderId());
        assertEquals("Sender", log.getSenderName());
        assertEquals("R1", log.getRecipientId());
        assertEquals("Recipient", log.getRecipientName());
        assertEquals("broadcast", log.getType());
        assertNull(log.getCreatedAt());
    }
}

