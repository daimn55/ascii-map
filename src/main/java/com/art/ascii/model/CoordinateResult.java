package com.art.ascii.model;

import java.util.List;

/**
 * Represents the result of coordinate processing, including 
 * a list of coordinates and their geographical boundaries.
 */
public class CoordinateResult {
    final List<Coordinate> coordinates;
    final double minLatitude;
    final double maxLatitude;
    final double minLongitude;
    final double maxLongitude;

    /**
     * Creates a new CoordinateResult with the given coordinates and boundaries.
     *
     * @param coordinates  list of coordinates
     * @param minLatitude  minimum latitude boundary
     * @param maxLatitude  maximum latitude boundary
     * @param minLongitude minimum longitude boundary
     * @param maxLongitude maximum longitude boundary
     */
    public CoordinateResult(List<Coordinate> coordinates,
                    double minLatitude, double maxLatitude,
                    double minLongitude, double maxLongitude) {
        this.coordinates = coordinates;
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }
    
    /**
     * Gets the list of coordinates.
     *
     * @return the list of coordinates
     */
    public List<Coordinate> getCoordinates() {
        return coordinates;
    }
    
    /**
     * Gets the minimum latitude boundary.
     *
     * @return the minimum latitude
     */
    public double getMinLatitude() {
        return minLatitude;
    }
    
    /**
     * Gets the maximum latitude boundary.
     *
     * @return the maximum latitude
     */
    public double getMaxLatitude() {
        return maxLatitude;
    }
    
    /**
     * Gets the minimum longitude boundary.
     *
     * @return the minimum longitude
     */
    public double getMinLongitude() {
        return minLongitude;
    }
    
    /**
     * Gets the maximum longitude boundary.
     *
     * @return the maximum longitude
     */
    public double getMaxLongitude() {
        return maxLongitude;
    }
}
