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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public EntrantStatus getStatus() { return status; }
    public void setStatus(EntrantStatus status) { this.status = status; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}

