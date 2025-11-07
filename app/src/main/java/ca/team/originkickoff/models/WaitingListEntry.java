/**
 * Firestore model representing a user's entry on an event's waiting list, including geo metadata.
 */
package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Mirrors the relational waiting list schema with Firestore-friendly field names.
 */
public class WaitingListEntry {
    /** Event identifier (string-typed, RDBMS UUID equivalent). */
    @PropertyName("event_id")
    private String eventId;

    /** User identifier (string-typed, RDBMS UUID equivalent). */
    @PropertyName("user_id")
    private String userId;

    /** Time when user joined the waiting list. */
    @PropertyName("joined_at")
    private com.google.firebase.Timestamp joinedAt;

    /** Source of the join action, e.g., qr or list. */
    private String source;
    /** Current state, e.g., active or left. */
    private String state;

    /** Optional latitude in decimal degrees. */
    private Double lat;
    /** Optional longitude in decimal degrees. */
    private Double lon;
    /** Horizontal accuracy in meters, if provided. */
    @PropertyName("precision_m")
    private Integer precisionMeters;

    /** Whether the user consented to location collection. */
    @PropertyName("location_consent")
    private boolean locationConsent;

    /**
     * Builds a composite doc id from eventId and userId.
     * @param eventId event identifier
     * @param userId user identifier
     * @return composite document id in the form eventId_userId
     */
    public static String docId(String eventId, String userId) {
        return eventId + "_" + userId;
    }

    /** No-arg constructor for Firebase. */
    public WaitingListEntry() {}

    /**
     * Convenience constructor for typical entries.
     */
    public WaitingListEntry(String eventId, String userId, com.google.firebase.Timestamp joinedAt, String source, String state) {
        this.eventId = eventId;
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.source = source;
        this.state = state;
    }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("joined_at")
    public Timestamp getJoinedAt() { return joinedAt; }
    @PropertyName("joined_at")
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    @PropertyName("precision_m")
    public Integer getPrecisionMeters() { return precisionMeters; }
    @PropertyName("precision_m")
    public void setPrecisionMeters(Integer precisionMeters) { this.precisionMeters = precisionMeters; }

    @PropertyName("location_consent")
    public boolean isLocationConsent() { return locationConsent; }
    @PropertyName("location_consent")
    public void setLocationConsent(boolean locationConsent) { this.locationConsent = locationConsent; }
}
