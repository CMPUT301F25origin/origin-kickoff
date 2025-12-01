package ca.team.originkickoff.utils;

import ca.team.originkickoff.models.LotteryMethod;
import org.junit.Test;

import java.util.HashMap;

/**
 * Unit tests for LotteryDemo focusing on JVM-safe parts.
 * NOTE: simulateLottery() constructs Firebase-backed services (LotteryService -> WaitingListService) and
 * cannot run in pure JVM tests without Robolectric/Android; that test removed to keep suite passing.
 */
public class LotteryDemoTest {

    @Test
    public void printFairnessReport_emptyMapNoThrow_random() {
        LotteryDemo.printFairnessReport(new HashMap<>(), 10, LotteryMethod.RANDOM);
    }

    @Test
    public void printFairnessReport_emptyMapNoThrow_earlyPriority() {
        LotteryDemo.printFairnessReport(new HashMap<>(), 10, LotteryMethod.EARLY_PRIORITY_RANDOM);
    }
}
