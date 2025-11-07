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
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.WaitingListEntry;

public class WaitingListService {
    private static final String EVENTS_COLL = "events";
    private static final String WAITLIST_COLL = "waiting_list_entries"; // mirrors RDBMS table name

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public DocumentReference waitlistDoc(String eventId, String userId) {
        return db.collection(WAITLIST_COLL).document(WaitingListEntry.docId(eventId, userId));
    }

    public Task<Boolean> isOnWaitlist(@NonNull String eventId, @NonNull String userId) {
        return waitlistDoc(eventId, userId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) return false;
                    DocumentSnapshot snap = task.getResult();
                    return snap != null && snap.exists() && "active".equals(snap.getString("state"));
                });
    }

    // Returns true if state changed to active (i.e., we actually joined now)
    public Task<Boolean> join(@NonNull String eventId, @NonNull String userId, boolean locationConsent,
                              Double lat, Double lon, Integer precisionMeters, String source) {
        DocumentReference wlRef = waitlistDoc(eventId, userId);
        DocumentReference eventRef = db.collection(EVENTS_COLL).document(eventId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(wlRef);
            boolean wasActive = snap.exists() && "active".equals(snap.getString("state"));
            if (wasActive) {
                return false; // no-op
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

    // Returns true if state changed to left (i.e., we actually left now)
    public Task<Boolean> leave(@NonNull String eventId, @NonNull String userId) {
        DocumentReference wlRef = waitlistDoc(eventId, userId);
        DocumentReference eventRef = db.collection(EVENTS_COLL).document(eventId);

        return db.runTransaction((Transaction.Function<Boolean>) transaction -> {
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

    public Task<Integer> countActive(@NonNull String eventId) {
        Query q = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active");
        return q.get().continueWith(t -> t.isSuccessful() ? t.getResult().size() : 0);
    }

    public Task<List<String>> getAllActiveUserIds(@NonNull String eventId) {
        Query q = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active");

        return q.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
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

    public Task<List<WaitingListEntry>> listActive(@NonNull String eventId) {
        Query q = db.collection(WAITLIST_COLL)
                .whereEqualTo("event_id", eventId)
                .whereEqualTo("state", "active");

        return q.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                // Propagate failure so callers can surface an error
                throw task.getException();
            }
            List<WaitingListEntry> out = new ArrayList<>();
            QuerySnapshot snaps = task.getResult();
            if (snaps != null) {
                for (DocumentSnapshot s : snaps.getDocuments()) {
                    WaitingListEntry e = s.toObject(WaitingListEntry.class);
                    if (e != null) out.add(e);
                }
            }
            // Sort client-side by joined_at ascending to avoid composite index need
            Collections.sort(out, new Comparator<WaitingListEntry>() {
                @Override
                public int compare(WaitingListEntry a, WaitingListEntry b) {
                    if (a.getJoinedAt() == null && b.getJoinedAt() == null) return 0;
                    if (a.getJoinedAt() == null) return 1;
                    if (b.getJoinedAt() == null) return -1;
                    long da = a.getJoinedAt().getSeconds();
                    long db = b.getJoinedAt().getSeconds();
                    return Long.compare(da, db);
                }
            });
            return out;
        });
    }
}
