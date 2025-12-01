package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for LotteryMethod enum logic.
 */
public class LotteryMethodTest {

    @Test
    public void values_declaredOrder() {
        LotteryMethod[] vals = LotteryMethod.values();
        assertEquals(2, vals.length);
        assertEquals(LotteryMethod.RANDOM, vals[0]);
        assertEquals(LotteryMethod.EARLY_PRIORITY_RANDOM, vals[1]);
    }

    @Test
    public void getValue_returnsExpectedStrings() {
        assertEquals("random", LotteryMethod.RANDOM.getValue());
        assertEquals("early_priority_random", LotteryMethod.EARLY_PRIORITY_RANDOM.getValue());
    }

    @Test
    public void fromString_exactMatch() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString("random"));
        assertEquals(LotteryMethod.EARLY_PRIORITY_RANDOM, LotteryMethod.fromString("early_priority_random"));
    }

    @Test
    public void fromString_caseInsensitive() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString("RaNdOm"));
        assertEquals(LotteryMethod.EARLY_PRIORITY_RANDOM, LotteryMethod.fromString("EARLY_PRIORITY_RANDOM"));
    }

    @Test
    public void fromString_nullOrUnknownDefaultsToRandom() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString(null));
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString("unknown"));
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString(""));
    }
}

