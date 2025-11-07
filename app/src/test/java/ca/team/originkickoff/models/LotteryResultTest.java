package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class LotteryResultTest {

    @Test
    public void testConstructorAndFields() {
        Timestamp conducted = Timestamp.now();
        List<String> winners = Arrays.asList("u1", "u2");
        LotteryResult result = new LotteryResult("eventX", conducted, "random", 10, 2, winners, "orgZ");
        result.setAllEntrantIds(Arrays.asList("u1", "u2", "u3"));
        assertEquals("eventX", result.getEventId());
        assertEquals(conducted, result.getConductedAt());
        assertEquals("random", result.getLotteryMethod());
        assertEquals(10, result.getTotalEntrants());
        assertEquals(2, result.getNumWinners());
        assertEquals(winners, result.getWinnerIds());
        assertEquals("orgZ", result.getConductedBy());
        assertEquals(Arrays.asList("u1", "u2", "u3"), result.getAllEntrantIds());
    }

    @Test
    public void testEmptyConstructorSetters() {
        LotteryResult res = new LotteryResult();
        Timestamp t = Timestamp.now();
        res.setEventId("E");
        res.setConductedAt(t);
        res.setLotteryMethod("early_priority_random");
        res.setTotalEntrants(5);
        res.setNumWinners(3);
        res.setWinnerIds(Arrays.asList("a","b","c"));
        res.setAllEntrantIds(Arrays.asList("a","b","c","d","e"));
        res.setConductedBy("ORG");
        assertEquals("E", res.getEventId());
        assertEquals(t, res.getConductedAt());
        assertEquals("early_priority_random", res.getLotteryMethod());
        assertEquals(5, res.getTotalEntrants());
        assertEquals(3, res.getNumWinners());
        assertEquals(Arrays.asList("a","b","c"), res.getWinnerIds());
        assertEquals(Arrays.asList("a","b","c","d","e"), res.getAllEntrantIds());
        assertEquals("ORG", res.getConductedBy());
    }
}

