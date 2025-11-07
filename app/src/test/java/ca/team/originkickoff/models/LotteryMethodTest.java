package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

public class LotteryMethodTest {

    @Test
    public void testFromStringValidRandom() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString("random"));
    }

    @Test
    public void testFromStringValidEarlyPriority() {
        assertEquals(LotteryMethod.EARLY_PRIORITY_RANDOM, LotteryMethod.fromString("early_priority_random"));
    }

    @Test
    public void testFromStringCaseInsensitive() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString("RaNdOm"));
    }

    @Test
    public void testFromStringUnknownDefaultsRandom() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString("unknown_method"));
    }

    @Test
    public void testFromStringNullDefaultsRandom() {
        assertEquals(LotteryMethod.RANDOM, LotteryMethod.fromString(null));
    }

    @Test
    public void testGetValue() {
        assertEquals("random", LotteryMethod.RANDOM.getValue());
        assertEquals("early_priority_random", LotteryMethod.EARLY_PRIORITY_RANDOM.getValue());
    }
}

