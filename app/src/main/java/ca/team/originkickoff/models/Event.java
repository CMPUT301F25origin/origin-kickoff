package ca.team.originkickoff.models;

import java.io.Serializable;
import java.util.Date;

public class Event implements Serializable {
    private String id;
    private String name;
    private String description;
    private String organizerId;
    private String organizerName;
    private Date eventDate;
    private String location;
    private String category;
    private int capacity;
    private int waitlistCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private String posterUrl;
    private boolean geolocationRequired;
    private double price;
    private long createdAt;

    // Empty constructor for Firebase
    public Event() {
    }

    public Event(String id, String name, String description, String organizerId, String organizerName,
                 Date eventDate, String location, String category, int capacity, int waitlistCount,
                 Date registrationStartTime, Date registrationEndTime, String posterUrl,
                 boolean geolocationRequired, double price, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.eventDate = eventDate;
        this.location = location;
        this.category = category;
        this.capacity = capacity;
        this.waitlistCount = waitlistCount;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.posterUrl = posterUrl;
        this.geolocationRequired = geolocationRequired;
        this.price = price;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getWaitlistCount() { return waitlistCount; }
    public void setWaitlistCount(int waitlistCount) { this.waitlistCount = waitlistCount; }

    public Date getRegistrationStartTime() { return registrationStartTime; }
    public void setRegistrationStartTime(Date registrationStartTime) { this.registrationStartTime = registrationStartTime; }

    public Date getRegistrationEndTime() { return registrationEndTime; }
    public void setRegistrationEndTime(Date registrationEndTime) { this.registrationEndTime = registrationEndTime; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public boolean isGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Helper method to check if registration is still open
    public boolean isRegistrationOpen() {
        long now = System.currentTimeMillis();
        return registrationStartTime != null && registrationEndTime != null &&
                now >= registrationStartTime.getTime() && now <= registrationEndTime.getTime();
    }

    // Helper method to check if spots are available
    public boolean hasAvailableSpots() {
        return waitlistCount < capacity;
    }
}
