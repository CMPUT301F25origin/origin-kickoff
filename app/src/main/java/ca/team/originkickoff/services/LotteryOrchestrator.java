package ca.team.originkickoff.services;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.LotteryMethod;
import ca.team.originkickoff.models.LotteryResult;

/**
 * Orchestrates the complete lottery process:
 * 1. Validates lottery can be conducted
 * 2. Conducts the lottery draw
 * 3. Saves results to Firestore
 * 4. Updates winner status
 */
public class LotteryOrchestrator {
    private static final String LOTTERY_RESULTS_COLL = "lottery_results";
    private static final String EVENTS_COLL = "events";

    private final FirebaseFirestore db;
    private final LotteryService lotteryService;
    private final WaitingListService waitingListService;

    public LotteryOrchestrator() {
        this.db = FirebaseFirestore.getInstance();
        this.lotteryService = new LotteryService();
        this.waitingListService = new WaitingListService();
    }

    /**
     * Conduct a lottery for an event.
     *
     * @param eventId The event ID
     * @param organizerId The organizer conducting the lottery (for audit)
     * @param numWinners Number of winners to select (typically event capacity)
     * @param method The lottery method to use
     * @return Task containing the lottery result
     */
    public Task<LotteryResult> conductLottery(@NonNull String eventId,
                                               @NonNull String organizerId,
                                               int numWinners,
                                               @NonNull LotteryMethod method) {
        // Step 1: Check if lottery already conducted
        return hasLotteryBeenConducted(eventId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(task.getException());
                    }

                    if (task.getResult()) {
                        return Tasks.forException(
                            new IllegalStateException("Lottery has already been conducted for this event")
                        );
                    }

                    // Step 2: Get active entrants count
                    return waitingListService.countActive(eventId);
                })
                .continueWithTask(countTask -> {
                    if (!countTask.isSuccessful()) {
                        return Tasks.forException(countTask.getException());
                    }

                    int totalEntrants = countTask.getResult();
                    if (totalEntrants == 0) {
                        return Tasks.forException(
                            new IllegalStateException("No active entrants in waiting list")
                        );
                    }

                    // Step 3: Conduct the lottery
                    return lotteryService.conductLottery(eventId, method, numWinners)
                            .continueWithTask(lotteryTask -> {
                                if (!lotteryTask.isSuccessful()) {
                                    return Tasks.forException(lotteryTask.getException());
                                }

                                List<String> winnerIds = lotteryTask.getResult();

                                // Step 4: Create and save lottery result
                                LotteryResult result = new LotteryResult(
                                    eventId,
                                    Timestamp.now(),
                                    method.getValue(),
                                    totalEntrants,
                                    winnerIds.size(),
                                    winnerIds,
                                    organizerId
                                );

                                return saveLotteryResult(result);
                            });
                });
    }

    /**
     * Check if lottery has already been conducted for an event.
     */
    public Task<Boolean> hasLotteryBeenConducted(@NonNull String eventId) {
        return db.collection(LOTTERY_RESULTS_COLL)
                .document(eventId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) return false;
                    DocumentSnapshot doc = task.getResult();
                    return doc != null && doc.exists();
                });
    }

    /**
     * Get lottery result for an event.
     */
    public Task<LotteryResult> getLotteryResult(@NonNull String eventId) {
        return db.collection(LOTTERY_RESULTS_COLL)
                .document(eventId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return null;
                    }
                    return task.getResult().toObject(LotteryResult.class);
                });
    }

    /**
     * Check if a user is a winner in the lottery.
     */
    public Task<Boolean> isWinner(@NonNull String eventId, @NonNull String userId) {
        return getLotteryResult(eventId)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return false;
                    }
                    LotteryResult result = task.getResult();
                    return result.getWinnerIds() != null &&
                           result.getWinnerIds().contains(userId);
                });
    }

    /**
     * Save lottery result to Firestore.
     */
    private Task<LotteryResult> saveLotteryResult(LotteryResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_id", result.getEventId());
        data.put("conducted_at", result.getConductedAt());
        data.put("lottery_method", result.getLotteryMethod());
        data.put("total_entrants", result.getTotalEntrants());
        data.put("num_winners", result.getNumWinners());
        data.put("winner_ids", result.getWinnerIds());
        data.put("conducted_by", result.getConductedBy());

        return db.collection(LOTTERY_RESULTS_COLL)
                .document(result.getEventId())
                .set(data)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return result;
                });
    }
}

