package ca.team.originkickoff.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for EntrantStatus enum.
 */
public class EntrantStatusTest {

    @Test
    public void values_containsAllInDeclaredOrder() {
        EntrantStatus[] vals = EntrantStatus.values();
        assertEquals(4, vals.length);
        assertEquals(EntrantStatus.PENDING, vals[0]);
        assertEquals(EntrantStatus.ACCEPTED, vals[1]);
        assertEquals(EntrantStatus.FINAL_ENROLLED, vals[2]);
        assertEquals(EntrantStatus.CANCELLED, vals[3]);
    }

    @Test
    public void valueOf_matchesNames() {
        assertEquals(EntrantStatus.PENDING, EntrantStatus.valueOf("PENDING"));
        assertEquals(EntrantStatus.ACCEPTED, EntrantStatus.valueOf("ACCEPTED"));
        assertEquals(EntrantStatus.FINAL_ENROLLED, EntrantStatus.valueOf("FINAL_ENROLLED"));
        assertEquals(EntrantStatus.CANCELLED, EntrantStatus.valueOf("CANCELLED"));
    }
}

