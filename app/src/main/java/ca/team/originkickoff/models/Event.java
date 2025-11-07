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
    private double locationLatitude;
    private double locationLongitude;
    private String locationPlaceId;
    private String category;
    private int capacity;
    private int waitlistCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private String posterUrl;
    private String posterBase64;
    private boolean geolocationRequired;
    private double price;
    private long createdAt;
    private String qrCodeUrl;
    private String qrCodeBase64;
    private String lotteryCriteria;
    private String lotteryStatus; // "not_conducted", "conducted", etc.

    // Empty constructor for Firebase
    public Event() {
    }

    public Event(String id, String name, String description, String organizerId, String organizerName,
                 Date eventDate, String location, String category, int capacity, int waitlistCount,
                 Date registrationStartTime, Date registrationEndTime, String posterUrl, String posterBase64,
                 boolean geolocationRequired, double price, long createdAt, String qrCodeUrl, String lotteryCriteria) {
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
        this.posterBase64 = posterBase64;
        this.geolocationRequired = geolocationRequired;
        this.price = price;
        this.createdAt = createdAt;
        this.qrCodeUrl = qrCodeUrl;
        this.lotteryCriteria = lotteryCriteria;
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

    public double getLocationLatitude() { return locationLatitude; }
    public void setLocationLatitude(double locationLatitude) { this.locationLatitude = locationLatitude; }

    public double getLocationLongitude() { return locationLongitude; }
    public void setLocationLongitude(double locationLongitude) { this.locationLongitude = locationLongitude; }

    public String getLocationPlaceId() { return locationPlaceId; }
    public void setLocationPlaceId(String locationPlaceId) { this.locationPlaceId = locationPlaceId; }

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

    public String getPosterBase64() { return posterBase64; }
    public void setPosterBase64(String posterBase64) { this.posterBase64 = posterBase64; }

    public boolean isGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public String getLotteryCriteria() { return lotteryCriteria; }
    public void setLotteryCriteria(String lotteryCriteria) { this.lotteryCriteria = lotteryCriteria; }

    public String getLotteryStatus() { return lotteryStatus; }
    public void setLotteryStatus(String lotteryStatus) { this.lotteryStatus = lotteryStatus; }

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
