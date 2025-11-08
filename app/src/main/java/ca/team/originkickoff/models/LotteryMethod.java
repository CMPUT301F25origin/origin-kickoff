/**
 * Enumeration of supported lottery selection strategies for events.
 */
package ca.team.originkickoff.models;

/**
 * Enum representing the lottery selection methods available for an event.
 */
public enum LotteryMethod {
    /** Pure random selection - all entrants have equal probability. */
    RANDOM("random"),

    /** Early priority random - earlier entrants get higher weight (exponential decay by join time). */
    EARLY_PRIORITY_RANDOM("early_priority_random");

    private final String value;

    LotteryMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LotteryMethod fromString(String value) {
        if (value == null) return RANDOM;
        for (LotteryMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        return RANDOM;
    }
}
