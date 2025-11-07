/**
 * Domain model representing an event with scheduling, capacity, lottery and media metadata.
 */
package ca.team.originkickoff.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Serializable Event entity persisted in Firestore and used throughout the app.
 */
public class Event implements Serializable {
    /** Unique Firestore document id (not necessarily user supplied). */
    private String id;
    /** Human-readable event name. */
    private String name;
    /** Long-form description displayed in detail views. */
    private String description;
    /** Organizer user id. */
    private String organizerId;
    /** Organizer display name (denormalized for convenience). */
    private String organizerName;
    /** Date/time when the event occurs. */
    private Date eventDate;
    /** Textual location (address or place summary). */
    private String location;
    /** Latitude for geolocation validation. */
    private double locationLatitude;
    /** Longitude for geolocation validation. */
    private double locationLongitude;
    /** Optional Google Places ID. */
    private String locationPlaceId;
    /** Category tag for filtering. */
    private String category;
    /** Maximum number of accepted entrants. */
    private int capacity;
    /** Current number of users on waitlist (entrants). */
    private int waitlistCount;
    /** Number of users to be selected by lottery. */
    private int selectionSize;
    /** Flag indicating whether waitlist size is capped. */
    private boolean limitWaitlist;
    /** Maximum allowed waiting list size when limitWaitlist is true. */
    private int waitlistLimit;
    /** Registration window start timestamp. */
    private Date registrationStartTime;
    /** Registration window end timestamp. */
    private Date registrationEndTime;
    /** Remote poster image URL. */
    private String posterUrl;
    /** Base64 inline poster image (fallback/offline). */
    private String posterBase64;
    /** Whether entrant geolocation is required. */
    private boolean geolocationRequired;
    /** Monetary price (if any). */
    private double price;
    /** Creation epoch millis. */
    private long createdAt;
    /** Remote QR code image URL. */
    private String qrCodeUrl;
    /** Base64 inline QR code image. */
    private String qrCodeBase64;
    /** Criteria string influencing lottery selection. */
    private String lotteryCriteria;
    /** Lottery progression status e.g. not_conducted / conducted. */
    private String lotteryStatus;

    /** Empty constructor for Firebase serialization. */
    public Event() { }

    /**
     * Full constructor for manual instantiation.
     */
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

    /** @return event id */
    public String getId() { return id; }
    /** @param id new event id */
    public void setId(String id) { this.id = id; }

    /** @return name */
    public String getName() { return name; }
    /** @param name event name */
    public void setName(String name) { this.name = name; }

    /** @return description */
    public String getDescription() { return description; }
    /** @param description long-form description */
    public void setDescription(String description) { this.description = description; }

    /** @return organizer user id */
    public String getOrganizerId() { return organizerId; }
    /** @param organizerId organizer user id */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /** @return organizer display name */
    public String getOrganizerName() { return organizerName; }
    /** @param organizerName display name */
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    /** @return event date */
    public Date getEventDate() { return eventDate; }
    /** @param eventDate occurrence date */
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    /** @return textual location */
    public String getLocation() { return location; }
    /** @param location textual location */
    public void setLocation(String location) { this.location = location; }

    /** @return latitude */
    public double getLocationLatitude() { return locationLatitude; }
    /** @param locationLatitude latitude */
    public void setLocationLatitude(double locationLatitude) { this.locationLatitude = locationLatitude; }

    /** @return longitude */
    public double getLocationLongitude() { return locationLongitude; }
    /** @param locationLongitude longitude */
    public void setLocationLongitude(double locationLongitude) { this.locationLongitude = locationLongitude; }

    /** @return place id */
    public String getLocationPlaceId() { return locationPlaceId; }
    /** @param locationPlaceId place id */
    public void setLocationPlaceId(String locationPlaceId) { this.locationPlaceId = locationPlaceId; }

    /** @return category */
    public String getCategory() { return category; }
    /** @param category category */
    public void setCategory(String category) { this.category = category; }

    /** @return capacity */
    public int getCapacity() { return capacity; }
    /** @param capacity max capacity */
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /** @return current waitlist count */
    public int getWaitlistCount() { return waitlistCount; }
    /** @param waitlistCount waitlist count */
    public void setWaitlistCount(int waitlistCount) { this.waitlistCount = waitlistCount; }

    /** @return selection size */
    public int getSelectionSize() { return selectionSize; }
    /** @param selectionSize number of winners to select */
    public void setSelectionSize(int selectionSize) { this.selectionSize = selectionSize; }

    /** @return whether waitlist has a limit */
    public boolean isLimitWaitlist() { return limitWaitlist; }
    /** @param limitWaitlist flag enabling waitlist limit */
    public void setLimitWaitlist(boolean limitWaitlist) { this.limitWaitlist = limitWaitlist; }

    /** @return waitlist limit value */
    public int getWaitlistLimit() { return waitlistLimit; }
    /** @param waitlistLimit max waitlist size */
    public void setWaitlistLimit(int waitlistLimit) { this.waitlistLimit = waitlistLimit; }

    /** @return registration start */
    public Date getRegistrationStartTime() { return registrationStartTime; }
    /** @param registrationStartTime start timestamp */
    public void setRegistrationStartTime(Date registrationStartTime) { this.registrationStartTime = registrationStartTime; }

    /** @return registration end */
    public Date getRegistrationEndTime() { return registrationEndTime; }
    /** @param registrationEndTime end timestamp */
    public void setRegistrationEndTime(Date registrationEndTime) { this.registrationEndTime = registrationEndTime; }

    /** @return poster url */
    public String getPosterUrl() { return posterUrl; }
    /** @param posterUrl remote poster url */
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /** @return poster base64 data */
    public String getPosterBase64() { return posterBase64; }
    /** @param posterBase64 base64 poster image */
    public void setPosterBase64(String posterBase64) { this.posterBase64 = posterBase64; }

    /** @return geolocation requirement flag */
    public boolean isGeolocationRequired() { return geolocationRequired; }
    /** @param geolocationRequired flag */
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    /** @return price */
    public double getPrice() { return price; }
    /** @param price monetary price */
    public void setPrice(double price) { this.price = price; }

    /** @return creation epoch millis */
    public long getCreatedAt() { return createdAt; }
    /** @param createdAt creation epoch millis */
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** @return qr code url */
    public String getQrCodeUrl() { return qrCodeUrl; }
    /** @param qrCodeUrl remote qr code url */
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    /** @return qr code base64 */
    public String getQrCodeBase64() { return qrCodeBase64; }
    /** @param qrCodeBase64 base64 qr image */
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    /** @return lottery criteria string */
    public String getLotteryCriteria() { return lotteryCriteria; }
    /** @param lotteryCriteria criteria string */
    public void setLotteryCriteria(String lotteryCriteria) { this.lotteryCriteria = lotteryCriteria; }

    /** @return lottery status */
    public String getLotteryStatus() { return lotteryStatus; }
    /** @param lotteryStatus status value */
    public void setLotteryStatus(String lotteryStatus) { this.lotteryStatus = lotteryStatus; }

    /**
     * Checks if registration window is currently open.
     * @return true if current time within [start, end] inclusive
     */
    public boolean isRegistrationOpen() {
        long now = System.currentTimeMillis();
        return registrationStartTime != null && registrationEndTime != null &&
                now >= registrationStartTime.getTime() && now <= registrationEndTime.getTime();
    }

    /**
     * Indicates if there are still capacity spots available (waitlist count < capacity).
     * @return true if capacity not yet reached
     */
    public boolean hasAvailableSpots() {
        return waitlistCount < capacity;
    }
}
