/**
 * Notification payload model used to render in-app notifications and store metadata.
 */
package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;

/**
 * Represents a single notification entry, including display text and linkage to events/users.
 */
public class NotificationItem {
    /** Firestore document id. */
    private String id;
    /** Title text shown prominently. */
    private String title;
    /** Body message text. */
    private String message;
    /** Display-ready timestamp string. */
    private String timestamp;
    /** Notification category such as result, update, or general. */
    private String type;
    /** Related event id for context (when applicable). */
    private String eventId;
    /** User id this notification is intended for. */
    private String userId;
    /** Server creation timestamp. */
    private com.google.firebase.Timestamp createdAt;
    /** Read state flag. */
    private boolean read;

    /** No-arg constructor for Firebase. */
    public NotificationItem() {}

    /**
     * Convenience constructor for minimal display-only notifications.
     */
    public NotificationItem(String id, String title, String message, String timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
