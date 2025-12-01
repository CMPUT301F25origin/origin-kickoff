package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Entrant model.
 */
public class EntrantTest {

    @Test
    public void defaultConstructor_fieldsNull() {
        Entrant e = new Entrant();
        assertNull(e.getId());
        assertNull(e.getUserId());
        assertNull(e.getName());
        assertNull(e.getEmail());
        assertNull(e.getStatus());
        assertNull(e.getEventId());
    }

    @Test
    public void fullConstructor_setsAllFields() {
        Entrant e = new Entrant("EN1", "U1", "Alice", "alice@example.com", EntrantStatus.ACCEPTED, "EV1");
        assertEquals("EN1", e.getId());
        assertEquals("U1", e.getUserId());
        assertEquals("Alice", e.getName());
        assertEquals("alice@example.com", e.getEmail());
        assertEquals(EntrantStatus.ACCEPTED, e.getStatus());
        assertEquals("EV1", e.getEventId());
    }

    @Test
    public void setters_updateFields() {
        Entrant e = new Entrant();
        e.setId("EN2");
        e.setUserId("U2");
        e.setName("Bob");
        e.setEmail("bob@example.com");
        e.setStatus(EntrantStatus.PENDING);
        e.setEventId("EV2");
        assertEquals("EN2", e.getId());
        assertEquals("U2", e.getUserId());
        assertEquals("Bob", e.getName());
        assertEquals("bob@example.com", e.getEmail());
        assertEquals(EntrantStatus.PENDING, e.getStatus());
        assertEquals("EV2", e.getEventId());
    }
}

