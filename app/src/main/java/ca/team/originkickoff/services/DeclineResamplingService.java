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
    private static final String TAG = "DeclineResampling";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SecureRandom secureRandom = new SecureRandom();

    private final Set<String> processedDeclines = ConcurrentHashMap.newKeySet();
    private final Set<String> processedNewJoiners = ConcurrentHashMap.newKeySet();
    private ListenerRegistration declineListener;
    private ListenerRegistration newJoinerListener;
    private boolean newJoinerListenerInitialized = false;

    // Singleton optional helper (no global side effects unless used)
    private static DeclineResamplingService INSTANCE;

    /** Obtain a lazily-created singleton instance. */
    public static synchronized DeclineResamplingService getInstance() {
        if (INSTANCE == null) INSTANCE = new DeclineResamplingService();
        return INSTANCE;
    }

    /** Convenience to start monitoring using singleton. Safe to call multiple times. */
    public static void ensureMonitoring(@NonNull String eventId) {
        getInstance().startMonitoring(eventId, (declined, replacement) -> {
            android.util.Log.d(TAG, "Processed decline for user=" + declined + ", replacement=" + replacement);
        });
    }

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
                        android.util.Log.w(TAG, "Decline listener error", err);
                        return;
                    }
                    if (snap == null) return;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String declinedUserId = doc.getString("user_id");
                        if (declinedUserId == null) continue;
                        // Only process each decline once per session
                        if (processedDeclines.add(declinedUserId)) {
                            android.util.Log.d(TAG, "Detected decline for user=" + declinedUserId);
                            handleDecline(eventId, declinedUserId, callback);
                        }
                    }
                });

        // Check immediately for any unfilled spots and fill them
        checkAndFillUnfilledSpots(eventId);

        // Monitor for new waiting list joiners and auto-select them if spots available
        startNewJoinerMonitoring(eventId);
    }

    /** Stop monitoring declines (if active). */
    public void stop() {
        if (declineListener != null) {
            declineListener.remove();
            declineListener = null;
        }
        if (newJoinerListener != null) {
            newJoinerListener.remove();
            newJoinerListener = null;
        }
        processedDeclines.clear();
        processedNewJoiners.clear();
        newJoinerListenerInitialized = false;
    }

    /**
     * Start monitoring for new waiting list joiners who join AFTER the lottery.
     * These users will be automatically selected if there are unfilled spots.
     * @param eventId event identifier
     */
    private void startNewJoinerMonitoring(@NonNull String eventId) {
        newJoinerListener = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        android.util.Log.w(TAG, "New joiner listener error", err);
                        return;
                    }
                    if (snap == null) return;

                    // On first snapshot, mark all existing users as already processed
                    if (!newJoinerListenerInitialized) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String userId = doc.getString("user_id");
                            if (userId != null) {
                                processedNewJoiners.add(userId);
                            }
                        }
                        newJoinerListenerInitialized = true;
                        android.util.Log.d(TAG, "New joiner listener initialized. Marked " + processedNewJoiners.size() + " existing users as processed.");
                        return; // Don't process initial snapshot
                    }

                    // After initialization, only process truly new ADDED entries
                    for (DocumentChange change : snap.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = change.getDocument();
                            String userId = doc.getString("user_id");

                            if (userId != null && !processedNewJoiners.contains(userId)) {
                                processedNewJoiners.add(userId);
                                android.util.Log.d(TAG, "New user joined waiting list: " + userId + ". Checking for auto-selection.");

                                // Immediately check if they should be auto-selected
                                checkAndAutoSelectNewJoiner(eventId, userId);
                            }
                        }
                    }
                });
    }

    /**
     * Check for unfilled winner spots and fill them immediately from existing waiting list.
     * This is called when monitoring starts to handle cases where someone declined before monitoring began.
     * @param eventId event identifier
     */
    private void checkAndFillUnfilledSpots(@NonNull String eventId) {
        android.util.Log.d(TAG, "Checking for unfilled spots for event=" + eventId);

        db.collection(LOTTERY_RESULTS_COLL).document(eventId)
                .get()
                .addOnSuccessListener(lotteryDoc -> {
                    if (!lotteryDoc.exists()) {
                        android.util.Log.d(TAG, "No lottery conducted yet for event=" + eventId);
                        return;
                    }

                    LotteryResult result = lotteryDoc.toObject(LotteryResult.class);
                    if (result == null) return;

                    List<String> currentWinners = result.getWinnerIds() != null ? result.getWinnerIds() : new ArrayList<>();
                    Integer selectionSize = result.getNumWinners();

                    if (selectionSize == null || selectionSize <= 0) {
                        android.util.Log.d(TAG, "No selection size set for event=" + eventId);
                        return;
                    }

                    int unfilledSpots = selectionSize - currentWinners.size();
                    if (unfilledSpots <= 0) {
                        android.util.Log.d(TAG, "No unfilled spots for event=" + eventId + " (winners=" + currentWinners.size() + ", capacity=" + selectionSize + ")");
                        return;
                    }

                    android.util.Log.d(TAG, "Found " + unfilledSpots + " unfilled spots for event=" + eventId + ". Attempting to fill them.");

                    // Get all candidates from waiting list who opted in for resampling
                    String methodValue = result.getLotteryMethod();
                    Task<QuerySnapshot> declinedTask = db.collection(INVITATION_STATUS_COLL)
                            .whereEqualTo("event_id", eventId)
                            .whereEqualTo("status", "cancelled")
                            .get();
                    Task<QuerySnapshot> activeWaitlistTask = db.collection(WAITLIST_COLL)
                            .whereEqualTo("event_id", eventId)
                            .whereEqualTo("state", "active")
                            .get();

                    Tasks.whenAllSuccess(declinedTask, activeWaitlistTask)
                            .addOnSuccessListener(results -> {
                                // Build exclusion set (current winners + previously cancelled users)
                                Set<String> exclude = new HashSet<>(currentWinners);

                                QuerySnapshot declinedSnaps = declinedTask.getResult();
                                if (declinedSnaps != null) {
                                    for (DocumentSnapshot d : declinedSnaps.getDocuments()) {
                                        String uid = d.getString("user_id");
                                        if (uid != null) exclude.add(uid);
                                    }
                                }

                                // Get candidates who opted in for resampling
                                List<WaitingListEntry> candidateEntries = new ArrayList<>();
                                QuerySnapshot waitlistSnaps = activeWaitlistTask.getResult();
                                if (waitlistSnaps != null) {
                                    for (DocumentSnapshot w : waitlistSnaps.getDocuments()) {
                                        WaitingListEntry e = w.toObject(WaitingListEntry.class);
                                        Boolean resamplingOptIn = w.getBoolean("resampling_opt_in");
                                        if (e != null && e.getUserId() != null && !exclude.contains(e.getUserId())
                                                && Boolean.TRUE.equals(resamplingOptIn)) {
                                            candidateEntries.add(e);
                                        }
                                    }
                                }

                                android.util.Log.d(TAG, "Found " + candidateEntries.size() + " candidates who opted in for resampling");

                                // Fill each unfilled spot
                                if (!candidateEntries.isEmpty()) {
                                    fillSpotsSequentially(eventId, unfilledSpots, candidateEntries, methodValue, currentWinners);
                                } else {
                                    // No candidates available to fill spots - leave num_winners as-is so new joiners can fill them
                                    android.util.Log.d(TAG, "No candidates available to fill " + unfilledSpots + " unfilled spots. Leaving num_winners at " + selectionSize + " so new joiners can fill spots.");
                                }
                            });
                })
                .addOnFailureListener(e -> android.util.Log.e(TAG, "Error checking for unfilled spots", e));
    }

    /**
     * Fill unfilled spots sequentially by selecting candidates one at a time.
     * @param eventId event identifier
     * @param spotsToFill number of spots remaining
     * @param candidates list of candidate entries
     * @param methodValue lottery method to use for selection
     * @param currentWinners current list of winners (for exclusion)
     */
    private void fillSpotsSequentially(@NonNull String eventId, int spotsToFill,
                                       @NonNull List<WaitingListEntry> candidates,
                                       @Nullable String methodValue,
                                       @NonNull List<String> currentWinners) {
        if (spotsToFill <= 0 || candidates.isEmpty()) {
            android.util.Log.d(TAG, "No more spots to fill or no candidates available");
            return;
        }

        // Pick one candidate
        String selectedUserId = pickCandidate(methodValue, candidates);
        if (selectedUserId == null) {
            android.util.Log.d(TAG, "No candidate selected");
            return;
        }

        android.util.Log.d(TAG, "Attempting to auto-select user " + selectedUserId + " to fill unfilled spot");

        // Add them as a winner
        autoSelectUser(eventId, selectedUserId, currentWinners);

        // Remove from candidates and recurse for remaining spots
        candidates.removeIf(e -> selectedUserId.equals(e.getUserId()));
        currentWinners = new ArrayList<>(currentWinners);
        currentWinners.add(selectedUserId);

        // Continue filling remaining spots
        if (spotsToFill > 1 && !candidates.isEmpty()) {
            fillSpotsSequentially(eventId, spotsToFill - 1, candidates, methodValue, currentWinners);
        }
    }

    /**
     * Check if there are unfilled winner spots and automatically select the new joiner.
     * @param eventId event identifier
     * @param newUserId newly joined user ID
     */
    private void checkAndAutoSelectNewJoiner(@NonNull String eventId, @NonNull String newUserId) {
        db.collection(LOTTERY_RESULTS_COLL).document(eventId)
                .get()
                .addOnSuccessListener(lotteryDoc -> {
                    if (!lotteryDoc.exists()) {
                        android.util.Log.d(TAG, "No lottery conducted yet for event=" + eventId);
                        return;
                    }

                    LotteryResult result = lotteryDoc.toObject(LotteryResult.class);
                    if (result == null) return;

                    // Get current winner count and selection size
                    List<String> currentWinners = result.getWinnerIds() != null ? result.getWinnerIds() : new ArrayList<>();
                    Integer selectionSize = result.getNumWinners();

                    if (selectionSize == null || selectionSize <= 0) {
                        android.util.Log.d(TAG, "No selection size set for event=" + eventId);
                        return;
                    }

                    // Check if user is already a winner
                    if (currentWinners.contains(newUserId)) {
                        android.util.Log.d(TAG, "User " + newUserId + " is already a winner");
                        return;
                    }

                    // Check if there are unfilled spots
                    int unfilledSpots = selectionSize - currentWinners.size();
                    if (unfilledSpots <= 0) {
                        android.util.Log.d(TAG, "No unfilled spots for event=" + eventId);
                        return;
                    }

                    android.util.Log.d(TAG, "Found " + unfilledSpots + " unfilled spots. Auto-selecting user " + newUserId);

                    // Check if user has been cancelled/declined before
                    db.collection(INVITATION_STATUS_COLL)
                            .whereEqualTo("event_id", eventId)
                            .whereEqualTo("user_id", newUserId)
                            .whereEqualTo("status", "cancelled")
                            .get()
                            .addOnSuccessListener(cancelledSnap -> {
                                if (!cancelledSnap.isEmpty()) {
                                    android.util.Log.d(TAG, "User " + newUserId + " previously declined/cancelled, not auto-selecting");
                                    return;
                                }

                                // Auto-select this user
                                autoSelectUser(eventId, newUserId, currentWinners);
                            });
                })
                .addOnFailureListener(e -> android.util.Log.e(TAG, "Error checking lottery results", e));
    }

    /**
     * Automatically select a new user to fill an unfilled spot.
     * @param eventId event identifier
     * @param userId user to auto-select
     * @param currentWinners current list of winner IDs
     */
    private void autoSelectUser(@NonNull String eventId, @NonNull String userId, @NonNull List<String> currentWinners) {
        db.runTransaction(tr -> {
            DocumentReference lotteryRef = db.collection(LOTTERY_RESULTS_COLL).document(eventId);
            DocumentSnapshot snap = tr.get(lotteryRef);
            if (!snap.exists()) return null;

            List<String> winners = (List<String>) snap.get("winner_ids");
            if (winners == null) winners = new ArrayList<>();

            // Double-check user isn't already a winner (race condition check)
            if (winners.contains(userId)) return null;

            // Add user to winners
            winners = new ArrayList<>(winners);
            winners.add(userId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("winner_ids", winners);
            tr.update(lotteryRef, updates);

            // Create invitation status
            DocumentReference inviteRef = db.collection(INVITATION_STATUS_COLL)
                    .document(eventId + "_" + userId);
            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("event_id", eventId);
            inviteData.put("user_id", userId);
            inviteData.put("status", "chosen");
            inviteData.put("invited_at", Timestamp.now());
            tr.set(inviteRef, inviteData);

            return userId;
        }).addOnSuccessListener(selectedUserId -> {
            if (selectedUserId != null) {
                android.util.Log.d(TAG, "Auto-selected user " + selectedUserId + " for event " + eventId);

                // Send notification
                String notificationId = db.collection("notifications").document().getId();
                Map<String, Object> notif = new HashMap<>();
                notif.put("userId", selectedUserId);
                notif.put("eventId", eventId);
                notif.put("type", "result_resample");
                notif.put("title", "🎉 You're now selected!");
                notif.put("message", "A spot was available and you were automatically selected for the event!");
                notif.put("createdAt", Timestamp.now());
                notif.put("read", false);
                db.collection("notifications").document(notificationId).set(notif);
            }
        }).addOnFailureListener(e -> android.util.Log.e(TAG, "Error auto-selecting user", e));
    }

    private void handleDecline(String eventId, String declinedUserId, @Nullable ReplacementCallback callback) {
        db.collection(LOTTERY_RESULTS_COLL).document(eventId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        android.util.Log.d(TAG, "No lottery result found for event=" + eventId + "; skipping resample");
                        return Tasks.forResult(null); // nothing to do if no lottery
                    }
                    LotteryResult result = task.getResult().toObject(LotteryResult.class);
                    if (result == null || result.getWinnerIds() == null) {
                        return Tasks.forResult(null);
                    }
                    List<String> currentWinners = new ArrayList<>(result.getWinnerIds());
                    if (!currentWinners.contains(declinedUserId)) {
                        android.util.Log.d(TAG, "Declined user not in current winners list; ignoring");
                        return Tasks.forResult(null);
                    }
                    String methodValue = result.getLotteryMethod();
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
                                Set<String> exclude = new HashSet<>(currentWinners);
                                exclude.remove(declinedUserId);
                                QuerySnapshot declinedSnaps = declinedTask.getResult();
                                if (declinedSnaps != null) {
                                    for (DocumentSnapshot d : declinedSnaps.getDocuments()) {
                                        String uid = d.getString("user_id");
                                        if (uid != null) exclude.add(uid);
                                    }
                                }
                                List<WaitingListEntry> candidateEntries = new ArrayList<>();
                                QuerySnapshot waitlistSnaps = activeWaitlistTask.getResult();
                                if (waitlistSnaps != null) {
                                    for (DocumentSnapshot w : waitlistSnaps.getDocuments()) {
                                        WaitingListEntry e = w.toObject(WaitingListEntry.class);
                                        // Only include users who have opted in for resampling
                                        Boolean resamplingOptIn = w.getBoolean("resampling_opt_in");
                                        if (e != null && e.getUserId() != null && !exclude.contains(e.getUserId())
                                                && Boolean.TRUE.equals(resamplingOptIn)) {
                                            candidateEntries.add(e);
                                        }
                                    }
                                }
                                android.util.Log.d(TAG, "Found " + candidateEntries.size() + " candidates who opted in for resampling");
                                String replacementUserId = null;
                                if (!candidateEntries.isEmpty()) {
                                    replacementUserId = pickCandidate(methodValue, candidateEntries);
                                }
                                String finalReplacement = replacementUserId;
                                return db.runTransaction(tr -> {
                                    DocumentReference lotteryRef = db.collection(LOTTERY_RESULTS_COLL).document(eventId);
                                    DocumentSnapshot snap = tr.get(lotteryRef);
                                    if (!snap.exists()) {
                                        return null;
                                    }
                                    List<String> winners = (List<String>) snap.get("winner_ids");
                                    if (winners == null) winners = new ArrayList<>();
                                    if (!winners.contains(declinedUserId)) {
                                        return null;
                                    }
                                    winners = new ArrayList<>(winners);
                                    winners.remove(declinedUserId);
                                    if (finalReplacement != null) {
                                        winners.add(finalReplacement);
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("winner_ids", winners);
                                        tr.update(lotteryRef, updates);
                                        DocumentReference inviteRef = db.collection(INVITATION_STATUS_COLL)
                                                .document(eventId + "_" + finalReplacement);
                                        Map<String, Object> inviteData = new HashMap<>();
                                        inviteData.put("event_id", eventId);
                                        inviteData.put("user_id", finalReplacement);
                                        inviteData.put("status", "chosen");
                                        inviteData.put("invited_at", Timestamp.now());
                                        tr.set(inviteRef, inviteData);
                                    } else {
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

    // ===================== Added helper API for invitation actions =====================

    /**
     * Decline (cancel) a user's invitation. Only transitions from chosen or enrolled -> cancelled.
     * Triggers resample if monitoring is active.
     * @return Task resolving true if status changed; false if already cancelled or no doc.
     */
    public Task<Boolean> declineInvitation(@NonNull String eventId, @NonNull String userId) {
        DocumentReference inviteRef = db.collection(INVITATION_STATUS_COLL).document(eventId + "_" + userId);
        return inviteRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                return Tasks.forResult(false);
            }
            String status = task.getResult().getString("status");
            if (status == null) return Tasks.forResult(false);
            if ("cancelled".equals(status)) return Tasks.forResult(false); // already cancelled
            if (!"chosen".equals(status) && !"enrolled".equals(status)) return Tasks.forResult(false); // cannot cancel other states
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "cancelled");
            updates.put("responded_at", Timestamp.now());
            return inviteRef.update(updates).continueWith(u -> true);
        }).continueWith(t -> {
            boolean changed = t.isSuccessful() && Boolean.TRUE.equals(t.getResult());
            if (changed) {
                android.util.Log.d(TAG, "Invitation declined for user=" + userId);
            }
            return changed;
        });
    }

    /**
     * Accept a user's invitation. Transitions chosen -> enrolled.
     * @return Task resolving true if status changed; false otherwise.
     */
    public Task<Boolean> acceptInvitation(@NonNull String eventId, @NonNull String userId) {
        DocumentReference inviteRef = db.collection(INVITATION_STATUS_COLL).document(eventId + "_" + userId);
        return inviteRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                return Tasks.forResult(false);
            }
            String status = task.getResult().getString("status");
            if (!"chosen".equals(status)) return Tasks.forResult(false); // only chosen can enroll
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "enrolled");
            updates.put("responded_at", Timestamp.now());
            return inviteRef.update(updates).continueWith(u -> true);
        }).continueWith(t -> t.isSuccessful() && Boolean.TRUE.equals(t.getResult()));
    }

    /**
     * Simple debug hook to force a resample (e.g., for testing). Does not modify winner list; just logs candidate.
     */
    public Task<String> dryRunResample(@NonNull String eventId) {
        return db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return null;
                    List<WaitingListEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot s : task.getResult().getDocuments()) {
                        WaitingListEntry e = s.toObject(WaitingListEntry.class);
                        if (e != null && e.getUserId() != null) {
                            entries.add(e);
                        }
                    }
                    if (entries.isEmpty()) return null;
                    String methodValue = "early_priority_random"; // force weighted method
                    String selectedUserId = pickCandidate(methodValue, entries);
                    android.util.Log.d(TAG, "Dry run resample selected user: " + selectedUserId);
                    return selectedUserId;
                });
    }
}

