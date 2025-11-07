package ca.team.originkickoff.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.team.originkickoff.models.NotificationItem;

/**
 * Service for managing user notifications in Firestore.
 */
public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String NOTIFICATIONS_COLL = "notifications";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Get all notifications for a user, sorted by creation time (newest first).
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
     * Mark a notification as read.
     */
    public Task<Void> markAsRead(@NonNull String notificationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);

        return db.collection(NOTIFICATIONS_COLL)
                .document(notificationId)
                .update(updates);
    }

    /**
     * Create a lottery result notification.
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
}
