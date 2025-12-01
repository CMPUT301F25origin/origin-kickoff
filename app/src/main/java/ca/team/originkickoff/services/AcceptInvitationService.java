package ca.team.originkickoff.services;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing a transactional acceptance flow for entrants who were chosen in the lottery.
 * Without altering existing code, this creates a separate enrollment record while updating invitation_status.
 *
 * Collections used:
 *  - invitation_status (existing) : status transition chosen -> enrolled
 *  - event_enrollments (new) : documents recording final enrolled entrants per event
 *
 * Firestore schema for event_enrollments documents:
 *   { event_id: <string>, user_id: <string>, enrolled_at: <Timestamp> }
 *   Document ID: eventId_userId (same composite pattern as waiting list)
 */
public class AcceptInvitationService {
    private static final String INVITATION_STATUS_COLL = "invitation_status";
    private static final String ENROLLMENTS_COLL = "event_enrollments";
    private static final String EVENTS_COLL = "events";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Confirm attendance for a chosen entrant. Idempotent: repeated calls keep state enrolled.
     * Transaction steps:
     *  1. Load invitation_status doc; ensure status == chosen or enrolled.
     *  2. If chosen -> set status enrolled + responded_at.
     *  3. Create enrollment doc if absent.
     *  4. Increment event.enrolledCount (creates if missing) iff we transitioned from not enrolled.
     *
     * @param eventId target event ID
     * @param userId entrant user ID
     * @return Task<Boolean> true if enrollment confirmed (new or existing), false if not allowed
     */
    public Task<Boolean> confirmAttendance(@NonNull String eventId, @NonNull String userId) {
        DocumentReference inviteRef = db.collection(INVITATION_STATUS_COLL).document(eventId + "_" + userId);
        DocumentReference enrollRef = db.collection(ENROLLMENTS_COLL).document(eventId + "_" + userId);
        DocumentReference eventRef = db.collection(EVENTS_COLL).document(eventId);

        return db.runTransaction((Transaction.Function<Boolean>) transaction -> {
            DocumentSnapshot inviteSnap = transaction.get(inviteRef);
            if (!inviteSnap.exists()) {
                return false; // no invitation to accept
            }
            String status = inviteSnap.getString("status");
            if (status == null) return false;
            boolean alreadyEnrolled = "enrolled".equals(status);
            boolean chosen = "chosen".equals(status);
            if (!chosen && !alreadyEnrolled) {
                return false; // cannot accept if cancelled or other state
            }
            boolean createdEnrollment = false;
            DocumentSnapshot enrollSnap = transaction.get(enrollRef);
            if (!alreadyEnrolled) {
                // Transition chosen -> enrolled
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "enrolled");
                updates.put("responded_at", Timestamp.now());
                transaction.update(inviteRef, updates);
            }
            if (!enrollSnap.exists()) {
                Map<String, Object> enrollData = new HashMap<>();
                enrollData.put("event_id", eventId);
                enrollData.put("user_id", userId);
                enrollData.put("enrolled_at", Timestamp.now());
                transaction.set(enrollRef, enrollData);
                createdEnrollment = true;
            }
            if (createdEnrollment && !alreadyEnrolled) {
                // Increment enrolledCount field defensively (create if missing)
                transaction.update(eventRef, "enrolledCount", FieldValue.increment(1));
            }
            return true; // enrollment confirmed (new or existing)
        });
    }

    /**
     * List all enrolled user IDs for an event based on enrollment documents.
     * @param eventId event identifier
     * @return Task<List<String>> of user IDs (possibly empty)
     */
    public Task<List<String>> listEnrolled(@NonNull String eventId) {
        Query q = db.collection(ENROLLMENTS_COLL).whereEqualTo("event_id", eventId);
        return q.get().continueWith(task -> {
            List<String> ids = new ArrayList<>();
            if (!task.isSuccessful() || task.getResult() == null) return ids;
            QuerySnapshot snaps = task.getResult();
            for (DocumentSnapshot s : snaps.getDocuments()) {
                String uid = s.getString("user_id");
                if (uid != null) ids.add(uid);
            }
            return ids;
        });
    }

    /**
     * Check if a user is already enrolled (final list).
     * @param eventId event identifier
     * @param userId user identifier
     * @return Task<Boolean> true if enrolled
     */
    public Task<Boolean> isEnrolled(@NonNull String eventId, @NonNull String userId) {
        DocumentReference enrollRef = db.collection(ENROLLMENTS_COLL).document(eventId + "_" + userId);
        return enrollRef.get().continueWith(t -> t.isSuccessful() && t.getResult() != null && t.getResult().exists());
    }
}

