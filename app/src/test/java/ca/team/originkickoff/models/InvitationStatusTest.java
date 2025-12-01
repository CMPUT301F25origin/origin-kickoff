package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InvitationStatus focusing on simple POJO behavior without Firebase runtime.
 */
public class InvitationStatusTest {

    @Test
    public void defaultConstructor_fieldsNull() {
        InvitationStatus is = new InvitationStatus();
        assertNull(is.getEventId());
        assertNull(is.getUserId());
        assertNull(is.getStatus());
        assertNull(is.getInvitedAt());
        assertNull(is.getRespondedAt());
    }

    @Test
    public void convenienceConstructor_setsProvidedFields() {
        InvitationStatus is = new InvitationStatus("E1", "U1", "chosen", null);
        assertEquals("E1", is.getEventId());
        assertEquals("U1", is.getUserId());
        assertEquals("chosen", is.getStatus());
        assertNull(is.getInvitedAt()); // passed null
        assertNull(is.getRespondedAt()); // not set yet
    }

    @Test
    public void setters_mutateFields() {
        InvitationStatus is = new InvitationStatus();
        is.setEventId("E2");
        is.setUserId("U2");
        is.setStatus("cancelled");
        // intentionally keep timestamps null to avoid Firebase dependency in JVM test
        assertEquals("E2", is.getEventId());
        assertEquals("U2", is.getUserId());
        assertEquals("cancelled", is.getStatus());
        assertNull(is.getInvitedAt());
        assertNull(is.getRespondedAt());
    }
}

