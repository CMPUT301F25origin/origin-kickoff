package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String timestamp; // display-ready string for now
    private String type; // "result", "update", "general"
    private String eventId; // Reference to event for result type
    private String userId; // User this notification is for
    private Timestamp createdAt; // Firestore timestamp
    private boolean read; // Whether notification has been read

    public NotificationItem() {}

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
