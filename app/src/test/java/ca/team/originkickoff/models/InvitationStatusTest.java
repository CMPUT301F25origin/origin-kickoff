package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import org.junit.Test;
import static org.junit.Assert.*;

public class InvitationStatusTest {

    @Test
    public void testConstructor() {
        Timestamp inviteTime = Timestamp.now();
        InvitationStatus status = new InvitationStatus("eventA", "userB", "chosen", inviteTime);
        assertEquals("eventA", status.getEventId());
        assertEquals("userB", status.getUserId());
        assertEquals("chosen", status.getStatus());
        assertEquals(inviteTime, status.getInvitedAt());
        assertNull(status.getRespondedAt());
    }

    @Test
    public void testSetters() {
        InvitationStatus s = new InvitationStatus();
        s.setEventId("E");
        s.setUserId("U");
        s.setStatus("enrolled");
        Timestamp invited = Timestamp.now();
        Timestamp responded = Timestamp.now();
        s.setInvitedAt(invited);
        s.setRespondedAt(responded);
        assertEquals("E", s.getEventId());
        assertEquals("U", s.getUserId());
        assertEquals("enrolled", s.getStatus());
        assertEquals(invited, s.getInvitedAt());
        assertEquals(responded, s.getRespondedAt());
    }
}

