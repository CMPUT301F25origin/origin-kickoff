package ca.team.originkickoff;

/**
 * Simple in-memory session flags for runtime-only view mode tweaks.
 * Not persisted; resets when app process is killed.
 */
public final class SessionManager {
    private static boolean forceUserMode = false;

    private SessionManager() {}

    /**
     * @return true if admin chose to view the app as a normal user.
     */
    public static boolean isForceUserMode() {
        return forceUserMode;
    }

    /**
     * Enables/disables forced user mode (admin acts like entrant/organizer UI).
     * @param value new flag value
     */
    public static void setForceUserMode(boolean value) {
        forceUserMode = value;
    }
}

