/**
 * Value object for an event's physical or virtual location including optional Places ID.
 */
package ca.team.originkickoff.models;

import java.io.Serializable;

/**
 * Simple serializable location wrapper used by events and geolocation checks.
 */
public class EventLocation implements Serializable {
    /** Human-readable address or label. */
    private String address;
    /** Latitude in WGS84. */
    private double latitude;
    /** Longitude in WGS84. */
    private double longitude;
    /** Optional Google Places identifier. */
    private String placeId;

    public EventLocation() {
    }

    public EventLocation(String address, double latitude, double longitude, String placeId) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeId = placeId;
    }

    public EventLocation(String address, double latitude, double longitude) {
        this(address, latitude, longitude, null);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    @Override
    public String toString() {
        return address;
    }
}
