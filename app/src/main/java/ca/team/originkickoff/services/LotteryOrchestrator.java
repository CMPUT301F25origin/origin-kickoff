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
        return hasLotteryBeenConducted(eventId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(task.getException());
                    }
                    if (Boolean.TRUE.equals(task.getResult())) {
                        return Tasks.forException(new IllegalStateException("Lottery has already been conducted for this event"));
                    }
                    return waitingListService.countActive(eventId);
                })
                .continueWithTask(countTask -> {
                    if (!countTask.isSuccessful()) {
                        return Tasks.forException(countTask.getException());
                    }
                    int totalEntrants = countTask.getResult();
                    if (totalEntrants == 0) {
                        return Tasks.forException(new IllegalStateException("No active entrants in waiting list"));
                    }
                    return lotteryService.conductLottery(eventId, method, numWinners)
                            .continueWithTask(lotteryTask -> {
                                if (!lotteryTask.isSuccessful()) {
                                    return Tasks.forException(lotteryTask.getException());
                                }
                                List<String> winnerIds = lotteryTask.getResult();
                                return waitingListService.getAllActiveUserIds(eventId)
                                        .continueWithTask(allEntrantsTask -> {
                                            if (!allEntrantsTask.isSuccessful()) {
                                                return Tasks.forException(allEntrantsTask.getException());
                                            }
                                            List<String> allEntrantIds = allEntrantsTask.getResult();
                                            LotteryResult result = new LotteryResult(
                                                    eventId,
                                                    Timestamp.now(),
                                                    method.getValue(),
                                                    totalEntrants,
                                                    winnerIds.size(),
                                                    winnerIds,
                                                    organizerId
                                            );
                                            result.setAllEntrantIds(allEntrantIds);
                                            return persistOutcome(result, winnerIds, allEntrantIds);
                                        });
                            });
                });
    }

    private Task<LotteryResult> persistOutcome(LotteryResult result, List<String> winnerIds, List<String> allEntrantIds) {
        // Save lottery result, then update event status and create invitation_status entries
        return saveLotteryResult(result).continueWithTask(saveTask -> {
            if (!saveTask.isSuccessful()) {
                return Tasks.forException(saveTask.getException());
            }
            // Update event's lotteryStatus field to 'conducted'
            Task<Void> updateEventTask = db.collection(EVENTS_COLL)
                    .document(result.getEventId())
                    .update("lotteryStatus", "conducted");

            // Build batch for invitation_status documents for winners
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            for (String winnerId : winnerIds) {
                DocumentReference inviteRef = db.collection("invitation_status").document(result.getEventId() + "_" + winnerId);
                Map<String, Object> inviteData = new HashMap<>();
                inviteData.put("event_id", result.getEventId());
                inviteData.put("user_id", winnerId);
                inviteData.put("status", "chosen");
                inviteData.put("invited_at", Timestamp.now());
                batch.set(inviteRef, inviteData);
            }
            Task<Void> batchCommit = batch.commit();

            // Optional: send notifications if NotificationService available
            NotificationService notificationService = new NotificationService();
            Task<Void> notifyTask = Tasks.whenAllSuccess(buildNotificationTasks(notificationService, result.getEventId(), winnerIds, allEntrantIds))
                    .continueWith(t -> null);

            return Tasks.whenAll(updateEventTask, batchCommit, notifyTask)
                    .continueWith(t -> {
                        if (!t.isSuccessful()) {
                            throw t.getException();
                        }
                        return result;
                    });
        });
    }

    private List<Task<Void>> buildNotificationTasks(NotificationService notificationService, String eventId, List<String> winnerIds, List<String> allEntrantIds) {
        // Only winners get result notifications currently (non-winners can be added if desired)
        // For now, send winner notifications; losers omitted to reduce noise
        List<Task<Void>> tasks = new java.util.ArrayList<>();
        // Fetch event name to include in notifications (best-effort)
        return tasks; // TODO: implement event name retrieval & notifications if required by spec
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
