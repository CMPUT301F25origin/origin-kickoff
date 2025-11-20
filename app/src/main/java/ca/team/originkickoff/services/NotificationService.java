/*
 * Firestore notification management service.
 * Retrieves, listens to, creates, and updates user notification documents.
 */
package ca.team.originkickoff.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.NotificationItem;

/**
 * Service for managing user notifications stored in Firestore.
 * Provides list retrieval (with index fallback), real‑time listening, mutation, and creation helpers.
 */
public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String NOTIFICATIONS_COLL = "notifications";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Get all notifications for a user ordered by creation time descending.
     * Falls back to client‑side sorting if a composite index is missing.
     *
     * @param userId Firestore user identifier
     * @return Task resolving with a list (possibly empty) of notifications
     */
    public Task<List<NotificationItem>> getNotificationsForUser(@NonNull String userId) {
        Log.d(TAG, "Fetching notifications for userId: " + userId);

        // Try the ordered query first (requires composite index)
        return db.collection(NOTIFICATIONS_COLL)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWithTask(task -> {
                    // If the query fails due to missing index, try without ordering
                    if (!task.isSuccessful() && task.getException() != null) {
                        String errorMsg = task.getException().getMessage();
                        Log.e(TAG, "Ordered query failed: " + errorMsg);

                        if (errorMsg != null && errorMsg.contains("index")) {
                            Log.w(TAG, "Composite index missing. Falling back to unordered query.");
                            Log.w(TAG, "Create the index in Firebase Console for better performance.");

                            // Fallback: Query without ordering (doesn't require composite index)
                            return db.collection(NOTIFICATIONS_COLL)
                                    .whereEqualTo("userId", userId)
                                    .get();
                        }
                    }
                    return task;
                })
                .continueWith(task -> {
                    List<NotificationItem> notifications = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot snapshot = task.getResult();
                        Log.d(TAG, "Found " + snapshot.size() + " notifications");

                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                NotificationItem item = doc.toObject(NotificationItem.class);
                                if (item != null) {
                                    item.setId(doc.getId());
                                    notifications.add(item);
                                    Log.d(TAG, "Notification: " + item.getTitle() + " | Type: " + item.getType() + " | EventId: " + item.getEventId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
                            }
                        }

                        // Sort by createdAt manually if we used the fallback query
                        if (!notifications.isEmpty() && notifications.get(0).getCreatedAt() != null) {
                            Collections.sort(notifications, (a, b) -> {
                                if (a.getCreatedAt() == null) return 1;
                                if (b.getCreatedAt() == null) return -1;
                                return b.getCreatedAt().compareTo(a.getCreatedAt()); // Descending
                            });
                        }
                    } else if (task.getException() != null) {
                        Log.e(TAG, "Error fetching notifications", task.getException());
                    }

                    Log.d(TAG, "Returning " + notifications.size() + " notifications");
                    return notifications;
                });
    }

    /**
     * Attach a real‑time listener for notifications belonging to a user.
     * Results are client‑side sorted descending by createdAt.
     *
     * @param userId   user identifier
     * @param onUpdate consumer invoked with latest list of notifications
     * @param onError  consumer invoked on listener error
     * @return Firestore listener registration (caller should remove when no longer needed)
     */
    public ListenerRegistration listenNotificationsForUser(@NonNull String userId,
                                                            @NonNull java.util.function.Consumer<List<NotificationItem>> onUpdate,
                                                            @NonNull java.util.function.Consumer<Exception> onError) {
        // Use simple whereEqualTo; ordering with snapshot requires composite index; we'll sort client-side
        return db.collection(NOTIFICATIONS_COLL)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Log.e(TAG, "Notification listener error", err);
                        onError.accept(err);
                        return;
                    }
                    List<NotificationItem> notifications = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            try {
                                NotificationItem item = doc.toObject(NotificationItem.class);
                                if (item != null) {
                                    item.setId(doc.getId());
                                    notifications.add(item);
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Failed to parse notification " + doc.getId(), ex);
                            }
                        }
                    }
                    // Client-side sort
                    Collections.sort(notifications, (a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    onUpdate.accept(notifications);
                });
    }

    /**
     * Mark a notification document as read.
     *
     * @param notificationId document ID of the notification
     * @return Task resolving when update completes
     */
    public Task<Void> markAsRead(@NonNull String notificationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);

        return db.collection(NOTIFICATIONS_COLL)
                .document(notificationId)
                .update(updates);
    }

    /**
     * Create and persist a lottery result notification for a user.
     *
     * @param userId    target user identifier
     * @param eventId   related event identifier
     * @param eventName human‑readable event name for message composition
     * @param isWinner  whether the user won the lottery
     * @return Task resolving when notification is stored
     */
    public Task<Void> createLotteryNotification(@NonNull String userId, @NonNull String eventId,
                                                 @NonNull String eventName, boolean isWinner) {
        String notificationId = db.collection(NOTIFICATIONS_COLL).document().getId();
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("eventId", eventId);
        notification.put("type", "result");
        notification.put("read", false);
        notification.put("createdAt", Timestamp.now());

        if (isWinner) {
            notification.put("title", "🎉 Lottery Result - You Won!");
            notification.put("message", "Congratulations! You were selected in the lottery for " + eventName);
        } else {
            notification.put("title", "Lottery Result");
            notification.put("message", "Unfortunately, you were not selected in the lottery for " + eventName);
        }

        return db.collection(NOTIFICATIONS_COLL)
                .document(notificationId)
                .set(notification);
    }

    /**
     * Broadcast a custom organizer message to waiting-list entrants (active state) for an event.
     * Each notification has type 'waitlist_broadcast'.
     *
     * @param userIds active waiting list user IDs
     * @param eventId event identifier
     * @param eventName event display name
     * @param title notification title (fallback applied if blank)
     * @param message body text (fallback applied if blank)
     * @return Task resolving when all notifications are written
     */
    public Task<Void> notifyWaitingListEntrants(@NonNull List<String> userIds,
                                                @NonNull String eventId,
                                                @NonNull String eventName,
                                                @Nullable String title,
                                                @Nullable String message) {
        String safeTitle = (title == null || title.trim().isEmpty()) ? "Update for " + eventName : title.trim();
        String safeMessage = (message == null || message.trim().isEmpty()) ? "There is an update regarding '" + eventName + "'." : message.trim();
        List<Task<Void>> tasks = new ArrayList<>();
        for (String uid : userIds) {
            String notificationId = db.collection(NOTIFICATIONS_COLL).document().getId();
            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("eventId", eventId);
            data.put("type", "waitlist_broadcast");
            data.put("title", safeTitle);
            data.put("message", safeMessage);
            data.put("createdAt", Timestamp.now());
            data.put("read", false);
            tasks.add(db.collection(NOTIFICATIONS_COLL).document(notificationId).set(data));
        }
        return Tasks.whenAll(tasks);
    }
}
