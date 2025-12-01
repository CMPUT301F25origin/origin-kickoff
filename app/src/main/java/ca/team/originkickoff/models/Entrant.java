// filepath: /Users/sargun/StudioProjects/origin-kickoff/app/src/main/java/ca/team/originkickoff/models/Entrant.java
package ca.team.originkickoff.models;

import com.google.firebase.firestore.PropertyName;

/** Simple model representing an entrant (user entry) for an event. */
public class Entrant {
    private String id;
    @PropertyName("user_id")
    private String userId;
    private String name;
    private String email;
    private EntrantStatus status;
    private String eventId;

    public Entrant() {}

    public Entrant(String id, String userId, String name, String email, EntrantStatus status, String eventId) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.status = status;
        this.eventId = eventId;
    }

    /** @return entrant document ID */
    public String getId() { return id; }
    /** @param id entrant document ID */
    public void setId(String id) { this.id = id; }

    /** @return user ID of the entrant */
    @PropertyName("user_id")
    public String getUserId() { return userId; }
    /** @param userId user ID of the entrant */
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    /** @return entrant display name */
    public String getName() { return name; }
    /** @param name entrant display name */
    public void setName(String name) { this.name = name; }

    /** @return entrant email address */
    public String getEmail() { return email; }
    /** @param email entrant email address */
    public void setEmail(String email) { this.email = email; }

    /** @return current entrant status */
    public EntrantStatus getStatus() { return status; }
    /** @param status current entrant status */
    public void setStatus(EntrantStatus status) { this.status = status; }

    /** @return event ID this entrant is registered for */
    public String getEventId() { return eventId; }
    /** @param eventId event ID */
    public void setEventId(String eventId) { this.eventId = eventId; }
}

