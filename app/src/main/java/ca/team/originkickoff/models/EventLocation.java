package ca.team.originkickoff.models;

import java.io.Serializable;

public class EventLocation implements Serializable {
    private String address;
    private double latitude;
    private double longitude;
    private String placeId; // Google Places ID if from API

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

