/**
 * Model representing a user's invitation outcome and response timestamps for an event lottery.
 */
package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents an invitation status for a lottery winner tracking acceptance state.
 */
public class InvitationStatus {
    @Exclude
    private String id;

    /** Event identifier. */
    @PropertyName("event_id")
    private String eventId;

    /** User identifier. */
    @PropertyName("user_id")
    private String userId;

    /** Status value: chosen | cancelled | enrolled. */
    @PropertyName("status")
    private String status;

    /** Timestamp when invitation was issued. */
    @PropertyName("invited_at")
    private Timestamp invitedAt;

    /** Timestamp when user responded (if any). */
    @PropertyName("responded_at")
    private Timestamp respondedAt;

    /** No-arg constructor for Firebase. */
    public InvitationStatus() {}

    /**
     * Convenience constructor.
     */
    public InvitationStatus(String eventId, String userId, String status, Timestamp invitedAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
        this.invitedAt = invitedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
