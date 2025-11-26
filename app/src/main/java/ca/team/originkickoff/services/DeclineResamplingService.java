package ca.team.originkickoff.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import ca.team.originkickoff.models.LotteryResult;
import ca.team.originkickoff.models.WaitingListEntry;

/**
 * Service that automatically resamples remaining entrants when a chosen user declines (status becomes cancelled).
 * This class is additive and does not modify existing logic; integrate by instantiating and calling startMonitoring(eventId).
 *
 * Workflow:
 * 1. Listen for invitation_status documents with status == "cancelled" for the event.
 * 2. For each newly observed decline, attempt to pick a replacement entrant from active waiting list users
 *    who are not already winners and have not previously declined.
 * 3. Update the existing lottery_results document (winner_ids + num_winners if needed) and create a new invitation_status for the replacement.
 * 4. Optionally create a notification for the replacement winner (uses minimal inline write to notifications collection).
 *
 * Selection honors the original lottery method stored in the lottery_results document:
 *  - random -> uniform random among remaining
 *  - early_priority_random -> weighted (earlier join time higher weight, exponential decay with same constant as LotteryService)
 * If no remaining entrants exist, the declined user is removed and num_winners decremented (capacity effectively reduced).
 */
public class DeclineResamplingService {
    private static final String INVITATION_STATUS_COLL = "invitation_status";
    private static final String LOTTERY_RESULTS_COLL = "lottery_results";
    private static final String WAITLIST_COLL = "waiting_list_entries";
    private static final String EVENTS_COLL = "events";
    private static final double EARLY_PRIORITY_DECAY = 0.5; // mirror LotteryService

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SecureRandom secureRandom = new SecureRandom();

    private final Set<String> processedDeclines = ConcurrentHashMap.newKeySet();
    private ListenerRegistration declineListener;

    /** Callback invoked after a replacement winner is selected (or none). */
    public interface ReplacementCallback {
        /**
         * @param declinedUserId user who declined
         * @param replacementUserId newly chosen replacement (null if none found)
         */
        void onReplacement(@NonNull String declinedUserId, @Nullable String replacementUserId);
    }

    /**
     * Start monitoring invitation declines for an event. Idempotent; calling again will restart listener.
     * @param eventId event identifier
     * @param callback optional callback notified on each replacement attempt
     */
    public void startMonitoring(@NonNull String eventId, @Nullable ReplacementCallback callback) {
        stop();
        // Listen only to cancelled statuses for this event
        declineListener = db.collection(INVITATION_STATUS_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("status", "cancelled")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        return; // passive failure; caller can log externally
                    }
                    if (snap == null) return;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String declinedUserId = doc.getString("user_id");
                        if (declinedUserId == null) continue;
                        // Only process each decline once per session
                        if (processedDeclines.add(declinedUserId)) {
                            handleDecline(eventId, declinedUserId, callback);
                        }
                    }
                });
    }

    /** Stop monitoring declines (if active). */
    public void stop() {
        if (declineListener != null) {
            declineListener.remove();
            declineListener = null;
        }
        processedDeclines.clear();
    }

    private void handleDecline(String eventId, String declinedUserId, @Nullable ReplacementCallback callback) {
        // Fetch current lottery result first (needed for method & winners list)
        db.collection(LOTTERY_RESULTS_COLL).document(eventId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return Tasks.forResult(null); // nothing to do if no lottery
                    }
                    LotteryResult result = task.getResult().toObject(LotteryResult.class);
                    if (result == null || result.getWinnerIds() == null) {
                        return Tasks.forResult(null);
                    }
                    List<String> currentWinners = new ArrayList<>(result.getWinnerIds());
                    if (!currentWinners.contains(declinedUserId)) {
                        // Declined user not currently in winners; just return
                        return Tasks.forResult(null);
                    }
                    String methodValue = result.getLotteryMethod();
                    // Collect already declined users to exclude
                    Task<QuerySnapshot> declinedTask = db.collection(INVITATION_STATUS_COLL)
                            .whereEqualTo("event_id", eventId)
                            .whereEqualTo("status", "cancelled")
                            .get();
                    Task<QuerySnapshot> activeWaitlistTask = db.collection(WAITLIST_COLL)
                            .whereEqualTo("event_id", eventId)
                            .whereEqualTo("state", "active")
                            .get();

                    return Tasks.whenAllSuccess(declinedTask, activeWaitlistTask)
                            .continueWith(finalTask -> {
                                // Build exclusion set: existing winners (minus declined later), all declined
                                Set<String> exclude = new HashSet<>(currentWinners);
                                exclude.remove(declinedUserId); // we'll remove this user from winners
                                QuerySnapshot declinedSnaps = declinedTask.getResult();
                                if (declinedSnaps != null) {
                                    for (DocumentSnapshot d : declinedSnaps.getDocuments()) {
                                        String uid = d.getString("user_id");
                                        if (uid != null) exclude.add(uid);
                                    }
                                }
                                // Build candidate list from active waitlist
                                List<WaitingListEntry> candidateEntries = new ArrayList<>();
                                QuerySnapshot waitlistSnaps = activeWaitlistTask.getResult();
                                if (waitlistSnaps != null) {
                                    for (DocumentSnapshot w : waitlistSnaps.getDocuments()) {
                                        WaitingListEntry e = w.toObject(WaitingListEntry.class);
                                        if (e != null && e.getUserId() != null && !exclude.contains(e.getUserId())) {
                                            candidateEntries.add(e);
                                        }
                                    }
                                }
                                String replacementUserId = null;
                                if (!candidateEntries.isEmpty()) {
                                    replacementUserId = pickCandidate(methodValue, candidateEntries);
                                }
                                // Perform atomic update of lottery result + invitation_status creation
                                String finalReplacement = replacementUserId; // effectively final for lambda
                                return db.runTransaction(tr -> {
                                    DocumentReference lotteryRef = db.collection(LOTTERY_RESULTS_COLL).document(eventId);
                                    DocumentSnapshot snap = tr.get(lotteryRef);
                                    if (!snap.exists()) {
                                        return null; // aborted
                                    }
                                    List<String> winners = (List<String>) snap.get("winner_ids");
                                    if (winners == null) winners = new ArrayList<>();
                                    if (!winners.contains(declinedUserId)) {
                                        return null; // already processed externally
                                    }
                                    winners = new ArrayList<>(winners); // copy to mutate
                                    winners.remove(declinedUserId);
                                    if (finalReplacement != null) {
                                        // Add new winner
                                        winners.add(finalReplacement);
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("winner_ids", winners);
                                        // num_winners remains unchanged
                                        tr.update(lotteryRef, updates);
                                        // Create invitation_status for replacement
                                        DocumentReference inviteRef = db.collection(INVITATION_STATUS_COLL)
                                                .document(eventId + "_" + finalReplacement);
                                        Map<String, Object> inviteData = new HashMap<>();
                                        inviteData.put("event_id", eventId);
                                        inviteData.put("user_id", finalReplacement);
                                        inviteData.put("status", "chosen");
                                        inviteData.put("invited_at", Timestamp.now());
                                        tr.set(inviteRef, inviteData);
                                        // Minimal notification for replacement winner (non-transactional write after commit)
                                    } else {
                                        // No replacement; decrement num_winners
                                        Long numWinners = snap.getLong("num_winners");
                                        long newCount = (numWinners == null ? winners.size() : Math.max(0, numWinners - 1));
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("winner_ids", winners);
                                        updates.put("num_winners", (int) newCount);
                                        tr.update(lotteryRef, updates);
                                    }
                                    return finalReplacement;
                                }).continueWith(replTask -> {
                                    String repl = replTask.isSuccessful() ? (String) replTask.getResult() : null;
                                    if (repl != null) {
                                        // Post-commit notification creation (best-effort, not retried here)
                                        String notificationId = db.collection("notifications").document().getId();
                                        Map<String, Object> notif = new HashMap<>();
                                        notif.put("userId", repl);
                                        notif.put("eventId", eventId);
                                        notif.put("type", "result_resample");
                                        notif.put("title", "🎉 You're now selected!");
                                        notif.put("message", "A spot opened up and you were selected for the event.");
                                        notif.put("createdAt", Timestamp.now());
                                        notif.put("read", false);
                                        db.collection("notifications").document(notificationId).set(notif);
                                    }
                                    if (callback != null) {
                                        callback.onReplacement(declinedUserId, repl);
                                    }
                                    return null;
                                });
                            });
                });
    }

    /**
     * Select a candidate respecting the original lottery method.
     * @param methodValue stored method string
     * @param entries remaining candidate entries (active waitlist not excluded)
     * @return selected userId or null if list empty
     */
    private String pickCandidate(@Nullable String methodValue, @NonNull List<WaitingListEntry> entries) {
        if (entries.isEmpty()) return null;
        if (methodValue == null || methodValue.equalsIgnoreCase("random")) {
            // Uniform random (Fisher-Yates single step)
            int idx = secureRandom.nextInt(entries.size());
            return entries.get(idx).getUserId();
        }
        if (methodValue.equalsIgnoreCase("early_priority_random")) {
            // Weighted by early join time (same normalization & decay as LotteryService)
            long earliest = Long.MAX_VALUE;
            long latest = Long.MIN_VALUE;
            for (WaitingListEntry e : entries) {
                if (e.getJoinedAt() == null) continue;
                long t = e.getJoinedAt().getSeconds();
                earliest = Math.min(earliest, t);
                latest = Math.max(latest, t);
            }
            long range = latest - earliest;
            class WeightedEntry { String userId; double weight; WeightedEntry(String u, double w){userId=u;weight=w;} }
            class ScoredEntry { String userId; double score; ScoredEntry(String u,double s){userId=u;score=s;} }
            List<WeightedEntry> weighted = new ArrayList<>();
            for (WaitingListEntry e : entries) {
                long t = e.getJoinedAt() != null ? e.getJoinedAt().getSeconds() : earliest;
                double normalized = range == 0 ? 0 : (double) (t - earliest) / range;
                double weight = Math.exp(-EARLY_PRIORITY_DECAY * normalized);
                weighted.add(new WeightedEntry(e.getUserId(), weight));
            }
            List<ScoredEntry> scored = new ArrayList<>();
            for (WeightedEntry w : weighted) {
                double r = secureRandom.nextDouble();
                if (r < 1e-10) r = 1e-10; // avoid log(0)
                double score = Math.exp(Math.log(r) / w.weight);
                scored.add(new ScoredEntry(w.userId, score));
            }
            scored.sort((a,b) -> Double.compare(b.score, a.score));
            return scored.get(0).userId;
        }
        // Fallback to uniform random
        int idx = secureRandom.nextInt(entries.size());
        return entries.get(idx).getUserId();
    }
}

