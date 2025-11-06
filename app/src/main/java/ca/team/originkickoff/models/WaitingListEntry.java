package ca.team.originkickoff.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

// Mirrors the RDBMS waiting_list_entries table. Used in Firestore with same field names.
public class WaitingListEntry {
    @PropertyName("event_id")
    private String eventId; // UUID in RDBMS; keep as String

    @PropertyName("user_id")
    private String userId; // UUID in RDBMS; keep as String

    @PropertyName("joined_at")
    private Timestamp joinedAt; // TIMESTAMPTZ

    private String source; // 'qr' | 'list'
    private String state;  // 'active' | 'left'

    // Optional geo fields
    private Double lat;         // DECIMAL(9,6)
    private Double lon;         // DECIMAL(9,6)
    @PropertyName("precision_m")
    private Integer precisionMeters; // INTEGER

    @PropertyName("location_consent")
    private boolean locationConsent; // DEFAULT FALSE

    // Firestore doc id we enforce as composite: `${event_id}_${user_id}`
    public static String docId(String eventId, String userId) {
        return eventId + "_" + userId;
    }

    public WaitingListEntry() {}

    public WaitingListEntry(String eventId, String userId, Timestamp joinedAt, String source, String state) {
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

