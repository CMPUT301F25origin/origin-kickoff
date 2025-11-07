/*
 * High-level coordinator for running an event lottery end‑to‑end.
 * Validates state, draws winners, persists results, and triggers side effects.
 */
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
 * Orchestrates the complete lottery process including validation, draw, persistence,
 * and updating other collections (event status, invitation statuses, notifications).
 */
public class LotteryOrchestrator {
    private static final String LOTTERY_RESULTS_COLL = "lottery_results";
    private static final String EVENTS_COLL = "events";

    private final FirebaseFirestore db;
    private final LotteryService lotteryService;
    private final WaitingListService waitingListService;

    /**
     * Constructs a new orchestrator using default Firestore-backed services.
     */
    public LotteryOrchestrator() {
        this.db = FirebaseFirestore.getInstance();
        this.lotteryService = new LotteryService();
        this.waitingListService = new WaitingListService();
    }

    /**
     * Conduct a lottery for an event.
     *
     * @param eventId     ID of the event whose lottery is being run
     * @param organizerId ID of organizer performing the action (audit info)
     * @param numWinners  desired number of winners (typically capacity)
     * @param method      lottery selection method
     * @return Task resolving with the persisted {@link LotteryResult}
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

    /**
     * Persist lottery outcome and perform follow‑up updates (event status, invitations, notifications).
     *
     * @param result       finalized lottery result object
     * @param winnerIds    list of winners
     * @param allEntrantIds list of all entrant user IDs
     * @return Task resolving with the same {@link LotteryResult}
     */
    private Task<LotteryResult> persistOutcome(LotteryResult result, List<String> winnerIds, List<String> allEntrantIds) {
        return saveLotteryResult(result).continueWithTask(saveTask -> {
            if (!saveTask.isSuccessful()) {
                return Tasks.forException(saveTask.getException());
            }
            Task<Void> updateEventTask = db.collection(EVENTS_COLL)
                    .document(result.getEventId())
                    .update("lotteryStatus", "conducted");

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

    /**
     * Build notification creation tasks for winners (placeholder implementation currently).
     *
     * @param notificationService service used to create notifications
     * @param eventId             event identifier
     * @param winnerIds           list of winner user IDs
     * @param allEntrantIds       all entrant user IDs (unused currently)
     * @return list of tasks (possibly empty) representing notification operations
     */
    private List<Task<Void>> buildNotificationTasks(NotificationService notificationService, String eventId, List<String> winnerIds, List<String> allEntrantIds) {
        List<Task<Void>> tasks = new java.util.ArrayList<>();
        return tasks; // TODO: implement event name retrieval & notifications if required by spec
    }

    /**
     * Check if lottery has already been conducted for an event.
     *
     * @param eventId event identifier
     * @return Task resolving true if a result document already exists
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
     * Retrieve an existing lottery result if present.
     *
     * @param eventId event identifier
     * @return Task resolving with result or null if absent / failure
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
     * Determine if a user appears in the winner set.
     *
     * @param eventId event identifier
     * @param userId  user identifier
     * @return Task resolving true if user is a winner
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
     * Persist the lottery result document.
     *
     * @param result result representation to store
     * @return Task resolving with the same result on success
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
