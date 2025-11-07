package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents an invitation status for a lottery winner.
 * Tracks whether they accepted, declined, or haven't responded.
 */
public class InvitationStatus {
    @PropertyName("event_id")
    private String eventId;

    @PropertyName("user_id")
    private String userId;

    @PropertyName("status")
    private String status; // "chosen", "cancelled", "enrolled"

    @PropertyName("invited_at")
    private Timestamp invitedAt;

    @PropertyName("responded_at")
    private Timestamp respondedAt;

    // Empty constructor for Firebase
    public InvitationStatus() {}

    public InvitationStatus(String eventId, String userId, String status, Timestamp invitedAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
        this.invitedAt = invitedAt;
    }

    // Getters and Setters
    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("invited_at")
    public Timestamp getInvitedAt() { return invitedAt; }
    @PropertyName("invited_at")
    public void setInvitedAt(Timestamp invitedAt) { this.invitedAt = invitedAt; }

    @PropertyName("responded_at")
    public Timestamp getRespondedAt() { return respondedAt; }
    @PropertyName("responded_at")
    public void setRespondedAt(Timestamp respondedAt) { this.respondedAt = respondedAt; }
}

