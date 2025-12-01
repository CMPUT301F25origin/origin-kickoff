/*
 * Service for managing event waiting list entries and related counts in Firestore.
 * Encapsulates join/leave flows, queries, and basic client-side ordering.
 */
package ca.team.originkickoff.services;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.WaitingListEntry;

/**
 * Firestore-backed operations for the waiting list lifecycle and queries.
 */
public class WaitingListService {
    private static final String EVENTS_COLL = "events";
    private static final String WAITLIST_COLL = "waiting_list_entries"; // mirrors RDBMS table name

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Reference helper for a user's waitlist document for a given event.
     *
     * @param eventId event identifier
     * @param userId  user identifier
     * @return document reference in the waiting list collection
     */
    public DocumentReference waitlistDoc(String eventId, String userId) {
        return db.collection(WAITLIST_COLL).document(WaitingListEntry.docId(eventId, userId));
    }

    /**
     * Checks whether the user currently has an active waitlist entry for an event.
     *
     * @param eventId event identifier
     * @param userId  user identifier
     * @return Task resolving true if active, else false
     */
    public Task<Boolean> isOnWaitlist(@NonNull String eventId, @NonNull String userId) {
        return waitlistDoc(eventId, userId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) return false;
                    DocumentSnapshot snap = task.getResult();
                    return snap != null && snap.exists() && "active".equals(snap.getString("state"));
                });
    }

    /**
     * Joins the user to the event's waitlist if not already active.
     * Increments the event waitlist counter within a transaction.
     *
     * @param eventId          event identifier
     * @param userId           user identifier
     * @param locationConsent  whether the user consented to share location
     * @param lat              latitude (nullable)
     * @param lon              longitude (nullable)
     * @param precisionMeters  location precision in meters (nullable)
     * @param source           origin of the join action (e.g., list/qr)
     * @return Task resolving true if state changed from not-active to active
     */
    public Task<Boolean> join(@NonNull String eventId, @NonNull String userId, boolean locationConsent,
                              Double lat, Double lon, Integer precisionMeters, String source) {
        DocumentReference wlRef = waitlistDoc(eventId, userId);
        DocumentReference eventRef = db.collection(EVENTS_COLL).document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(wlRef);
            boolean wasActive = snap.exists() && "active".equals(snap.getString("state"));
            if (wasActive) {
                return false;
            }

            // Enforce optional waitlist limit
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            Long waitlistCount = eventSnap.getLong("waitlistCount");
            Boolean limitWaitlist = eventSnap.getBoolean("limitWaitlist");
            Long waitlistLimit = eventSnap.getLong("waitlistLimit");
            long currentCount = waitlistCount != null ? waitlistCount : 0L;
            boolean isLimited = limitWaitlist != null && limitWaitlist;
            long limit = waitlistLimit != null ? waitlistLimit : Long.MAX_VALUE;
            if (isLimited && currentCount >= limit) {
                throw new IllegalStateException("Waitlist is full for this event");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("event_id", eventId);
            data.put("user_id", userId);
            data.put("joined_at", Timestamp.now());
            data.put("source", source == null ? "list" : source);
            data.put("state", "active");
            if (lat != null) data.put("lat", lat);
            if (lon != null) data.put("lon", lon);
            if (precisionMeters != null) data.put("precision_m", precisionMeters);
            data.put("location_consent", locationConsent);

            transaction.set(wlRef, data);
            transaction.update(eventRef, "waitlistCount", FieldValue.increment(1));
            return true; // state changed
        });
    }

    /**
     * Leaves the waitlist if currently active and decrements the event counter.
     *
     * @param eventId event identifier
     * @param userId  user identifier
     * @return Task resolving true if state changed to left
     */
    public Task<Boolean> leave(@NonNull String eventId, @NonNull String userId) {
        DocumentReference wlRef = waitlistDoc(eventId, userId);
        DocumentReference eventRef = db.collection(EVENTS_COLL).document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(wlRef);
            boolean wasActive = snap.exists() && "active".equals(snap.getString("state"));
            if (!wasActive) {
                return false; // nothing to do
            }
            transaction.update(wlRef, "state", "left");
            transaction.update(eventRef, "waitlistCount", FieldValue.increment(-1));
            return true;
        });
    }

    /**
     * Removes an entrant from the waitlist explicitly by organizer action, marking a flag.
     * NOTE: Must be invoked instead of leave() when organizer initiates removal so we can distinguish via removed_by_organizer.
     * TODO: Integrate this method in organizer removal flow (WaitingListActivity) to ensure proper cancelled grouping logic.
     */
    public Task<Boolean> removeByOrganizer(@NonNull String eventId, @NonNull String userId) {
        DocumentReference wlRef = waitlistDoc(eventId, userId);
        DocumentReference eventRef = db.collection(EVENTS_COLL).document(eventId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(wlRef);
            if (!snap.exists()) {
                return false; // nothing to remove
            }
            String state = snap.getString("state");
            boolean wasActive = "active".equals(state);
            Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("state", "left");
            updates.put("removed_by_organizer", true);
            transaction.update(wlRef, updates);
            if (wasActive) {
                transaction.update(eventRef, "waitlistCount", FieldValue.increment(-1));
            }
            return true;
        });
    }

    /**
     * Counts active waitlist entries for an event.
     *
     * @param eventId event identifier
     * @return Task resolving with the number of active entries
     */
    public Task<Integer> countActive(@NonNull String eventId) {
        Query q = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active");
        return q.get().continueWith(t -> t.isSuccessful() ? t.getResult().size() : 0);
    }

    /**
     * Retrieves user IDs for all active waitlist entries for an event.
     *
     * @param eventId event identifier
     * @return Task resolving with a list of user IDs
     */
    public Task<List<String>> getAllActiveUserIds(@NonNull String eventId) {
        Query q = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active");

        return q.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception ex = task.getException();
                if (ex != null) throw ex; else throw new IllegalStateException("Unknown error fetching active user ids");
            }
            List<String> userIds = new ArrayList<>();
            QuerySnapshot snaps = task.getResult();
            if (snaps != null) {
                for (DocumentSnapshot s : snaps.getDocuments()) {
                    String userId = s.getString("user_id");
                    if (userId != null) {
                        userIds.add(userId);
                    }
                }
            }
            return userIds;
        });
    }

    /**
     * Lists active waiting list entries for an event, client-side sorted by join time ascending.
     *
     * @param eventId event identifier
     * @return Task resolving with ordered entries
     */
    public Task<List<WaitingListEntry>> listActive(@NonNull String eventId) {
        Query q = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active");

        return q.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception ex = task.getException();
                if (ex != null) throw ex; else throw new IllegalStateException("Unknown error listing active entries");
            }
            List<WaitingListEntry> out = new ArrayList<>();
            QuerySnapshot snaps = task.getResult();
            if (snaps != null) {
                for (DocumentSnapshot s : snaps.getDocuments()) {
                    WaitingListEntry e = s.toObject(WaitingListEntry.class);
                    if (e != null) out.add(e);
                }
            }
            out.sort((a, b) -> {
                if (a.getJoinedAt() == null && b.getJoinedAt() == null) return 0;
                if (a.getJoinedAt() == null) return 1;
                if (b.getJoinedAt() == null) return -1;
                return Long.compare(a.getJoinedAt().getSeconds(), b.getJoinedAt().getSeconds());
            });
            return out;
        });
    }
}
