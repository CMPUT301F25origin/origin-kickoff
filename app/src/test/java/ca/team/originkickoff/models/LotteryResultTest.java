package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for LotteryResult covering simple POJO behavior without Firebase timestamp instantiation.
 */
public class LotteryResultTest {

    @Test
    public void defaultConstructor_fieldsNullOrZero() {
        LotteryResult lr = new LotteryResult();
        assertNull(lr.getEventId());
        assertNull(lr.getConductedAt());
        assertNull(lr.getLotteryMethod());
        assertEquals(0, lr.getTotalEntrants());
        assertEquals(0, lr.getNumWinners());
        assertNull(lr.getWinnerIds());
        assertNull(lr.getAllEntrantIds());
        assertNull(lr.getConductedBy());
    }

    @Test
    public void convenienceConstructor_setsProvidedFields() {
        List<String> winners = Arrays.asList("U1", "U2");
        LotteryResult lr = new LotteryResult("E1", null, "random", 10, 2, winners, "ORG1");
        assertEquals("E1", lr.getEventId());
        assertNull(lr.getConductedAt()); // passed null intentionally
        assertEquals("random", lr.getLotteryMethod());
        assertEquals(10, lr.getTotalEntrants());
        assertEquals(2, lr.getNumWinners());
        assertEquals(winners, lr.getWinnerIds());
        assertNull(lr.getAllEntrantIds()); // not set via constructor
        assertEquals("ORG1", lr.getConductedBy());
    }

    @Test
    public void setters_updateMutableFields() {
        LotteryResult lr = new LotteryResult();
        lr.setEventId("E2");
        lr.setLotteryMethod("early_priority_random");
        lr.setTotalEntrants(25);
        lr.setNumWinners(5);
        lr.setWinnerIds(Arrays.asList("A", "B"));
        lr.setAllEntrantIds(Arrays.asList("A", "B", "C"));
        lr.setConductedBy("ORG2");
        // conductedAt left null to avoid Firebase dependency
        assertEquals("E2", lr.getEventId());
        assertEquals("early_priority_random", lr.getLotteryMethod());
        assertEquals(25, lr.getTotalEntrants());
        assertEquals(5, lr.getNumWinners());
        assertEquals(Arrays.asList("A", "B"), lr.getWinnerIds());
        assertEquals(Arrays.asList("A", "B", "C"), lr.getAllEntrantIds());
        assertEquals("ORG2", lr.getConductedBy());
        assertNull(lr.getConductedAt());
    }
}

