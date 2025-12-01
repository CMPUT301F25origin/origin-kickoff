package ca.team.originkickoff;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link SessionManager} covering static flag behavior.
 */
public class SessionManagerTest {

    @Test
    public void defaultFlag_isFalse() {
        // Ensure baseline
        SessionManager.setForceUserMode(false);
        assertFalse(SessionManager.isForceUserMode());
    }

    @Test
    public void setForceUserMode_trueThenFalse() {
        SessionManager.setForceUserMode(false);
        assertFalse(SessionManager.isForceUserMode());
        SessionManager.setForceUserMode(true);
        assertTrue(SessionManager.isForceUserMode());
        SessionManager.setForceUserMode(false);
        assertFalse(SessionManager.isForceUserMode());
    }

    @Test
    public void multipleSets_lastValueWins() {
        SessionManager.setForceUserMode(false);
        SessionManager.setForceUserMode(true);
        SessionManager.setForceUserMode(true);
        SessionManager.setForceUserMode(false);
        assertFalse(SessionManager.isForceUserMode());
        SessionManager.setForceUserMode(true);
        assertTrue(SessionManager.isForceUserMode());
    }
}

