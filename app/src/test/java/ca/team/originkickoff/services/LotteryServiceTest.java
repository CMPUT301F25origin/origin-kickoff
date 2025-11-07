package ca.team.originkickoff.services;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import ca.team.originkickoff.models.LotteryMethod;
import ca.team.originkickoff.models.WaitingListEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Tests core random selection logic of LotteryService.
 * Firestore-dependent conductLottery() is now tested with a mock WaitingListService.
 */
@RunWith(RobolectricTestRunner.class)
public class LotteryServiceTest {

    @Mock
    private WaitingListService mockWaitingListService;

    @Mock
    private SecureRandom mockSecureRandom;

    private LotteryService lotteryService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        lotteryService = new LotteryService(mockWaitingListService, mockSecureRandom);

        when(mockSecureRandom.nextInt(anyInt())).thenAnswer(invocation -> {
            int bound = invocation.getArgument(0);
            return bound > 1 ? bound - 1 : 0;
        });
        when(mockSecureRandom.nextDouble()).thenReturn(0.5);
    }

    private List<WaitingListEntry> buildEntries(int count, long baseTimeSeconds, long spacingSeconds) {
        List<WaitingListEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WaitingListEntry e = new WaitingListEntry();
            e.setUserId("user" + i);
            e.setJoinedAt(new Timestamp(baseTimeSeconds + i * spacingSeconds, 0));
            e.setState("active");
            list.add(e);
        }
        return list;
    }

    @Test
    public void conductLottery_zeroWinnersRequested_returnsEmptyList() {
        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.RANDOM, 0);
        task.addOnSuccessListener(winners -> {
            assertTrue(winners.isEmpty());
        });
    }

    @Test
    public void conductLottery_oneWinner_nEntries_random_returnsOneWinner() {
        List<WaitingListEntry> entries = buildEntries(5, System.currentTimeMillis() / 1000, 60);
        when(mockWaitingListService.listActive("event-id")).thenReturn(Tasks.forResult(entries));

        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.RANDOM, 1);
        task.addOnSuccessListener(winners -> {
            assertEquals(1, winners.size());
            assertTrue(entries.stream().map(WaitingListEntry::getUserId).collect(Collectors.toList()).containsAll(winners));
        });
    }

    @Test
    public void conductLottery_nWinners_nEntries_random_returnsAllEntriesAsWinners() {
        List<WaitingListEntry> entries = buildEntries(5, System.currentTimeMillis() / 1000, 60);
        when(mockWaitingListService.listActive("event-id")).thenReturn(Tasks.forResult(entries));

        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.RANDOM, 5);
        task.addOnSuccessListener(winners -> {
            assertEquals(5, winners.size());
            Set<String> winnerSet = new HashSet<>(winners);
            assertEquals(5, winnerSet.size()); // All unique
            assertTrue(winnerSet.containsAll(entries.stream().map(WaitingListEntry::getUserId).collect(Collectors.toList())));
        });
    }

    @Test
    public void conductLottery_nWinners_earlyPriority_returnsWeightedWinners() {
        List<WaitingListEntry> entries = buildEntries(5, System.currentTimeMillis() / 1000, 3600);
        when(mockWaitingListService.listActive("event-id")).thenReturn(Tasks.forResult(entries));

        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.EARLY_PRIORITY_RANDOM, 3);
        task.addOnSuccessListener(winners -> {
            assertEquals(3, winners.size());
            Set<String> winnerSet = new HashSet<>(winners);
            assertEquals(3, winnerSet.size()); // All unique
        });
    }

    @Test
    public void conductLottery_waitingListEmpty_returnsEmptyList() {
        when(mockWaitingListService.listActive("event-id")).thenReturn(Tasks.forResult(new ArrayList<>()));

        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.RANDOM, 5);
        task.addOnSuccessListener(winners -> {
            assertTrue(winners.isEmpty());
        });
    }

    @Test
    public void conductLottery_waitingListFetchFails_throwsException() {
        when(mockWaitingListService.listActive("event-id")).thenReturn(Tasks.forException(new RuntimeException("DB error")));

        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.RANDOM, 5);
        task.addOnFailureListener(exception -> {
            assertNotNull(exception);
            assertEquals("Failed to retrieve waiting list entries", exception.getMessage());
        });
    }

    @Test
    public void conductLottery_moreWinnersThanEntries_returnsAllEntries() {
        List<WaitingListEntry> entries = buildEntries(3, System.currentTimeMillis() / 1000, 60);
        when(mockWaitingListService.listActive("event-id")).thenReturn(Tasks.forResult(entries));

        Task<List<String>> task = lotteryService.conductLottery("event-id", LotteryMethod.RANDOM, 10);
        task.addOnSuccessListener(winners -> {
            assertEquals(3, winners.size());
            assertTrue(winners.containsAll(entries.stream().map(WaitingListEntry::getUserId).collect(Collectors.toList())));
        });
    }
}
