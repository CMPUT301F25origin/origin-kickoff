/**
 * Audit log record for notification delivery tracking.
 * Stores metadata about notifications sent to users for admin monitoring.
 */
package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;

/**
 * Represents a notification log entry with sender, recipient, and event context.
 */
public class NotificationLog {
    private String id;
    private String eventId;
    private String eventName; // optional denormalized
    private String senderId;
    private String senderName;
    private String recipientId; // could be group or user id
    private String recipientName;
    private String type; // e.g., broadcast, waitlist, chosen
    private Timestamp createdAt;

    public NotificationLog() {}

    /** @return notification log document ID */
    public String getId() { return id; }
    /** @param id notification log document ID */
    public void setId(String id) { this.id = id; }

    /** @return event identifier related to this notification */
    public String getEventId() { return eventId; }
    /** @param eventId event identifier */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return denormalized event name for display */
    public String getEventName() { return eventName; }
    /** @param eventName denormalized event name */
    public void setEventName(String eventName) { this.eventName = eventName; }

    /** @return user who sent the notification */
    public String getSenderId() { return senderId; }
    /** @param senderId user who sent the notification */
    public void setSenderId(String senderId) { this.senderId = senderId; }

    /** @return sender display name */
    public String getSenderName() { return senderName; }
    /** @param senderName sender display name */
    public void setSenderName(String senderName) { this.senderName = senderName; }

    /** @return user who received the notification */
    public String getRecipientId() { return recipientId; }
    /** @param recipientId user who received the notification */
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    /** @return recipient display name */
    public String getRecipientName() { return recipientName; }
    /** @param recipientName recipient display name */
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    /** @return notification type/category */
    public String getType() { return type; }
    /** @param type notification type */
    public void setType(String type) { this.type = type; }

    /** @return timestamp when notification was created */
    public Timestamp getCreatedAt() { return createdAt; }
    /** @param createdAt timestamp when notification was created */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

