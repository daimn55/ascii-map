package com.art.ascii.model;

/**
 * Represents a geographic coordinate with latitude and longitude.
 * Used for rendering ASCII maps.
 */
public class Coordinate {
    final double latitude;
    final double longitude;

    /**
     * Creates a new Coordinate with the given latitude and longitude.
     *
     * @param latitude  the latitude value
     * @param longitude the longitude value
     */
    public Coordinate(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    /**
     * Gets the latitude value.
     *
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * Gets the longitude value.
     *
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }
    
    @Override
    public String toString() {
        return "Coordinate{lat=" + latitude + ", lon=" + longitude + "}";
    }
}
