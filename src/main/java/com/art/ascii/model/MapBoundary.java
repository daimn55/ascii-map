package com.art.ascii.model;

/**
 * Represents geographic boundaries for a map, containing minimum and maximum
 * latitude and longitude values. Used for transforming coordinates into
 * a displayable map grid.
 */
public class MapBoundary {
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    
    /**
     * Creates a new MapBoundary with the specified coordinates.
     *
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     */
    public MapBoundary(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }
    
    /**
     * Gets the minimum latitude.
     * 
     * @return Minimum latitude value
     */
    public double getMinLat() {
        return minLat;
    }
    
    /**
     * Gets the maximum latitude.
     * 
     * @return Maximum latitude value
     */
    public double getMaxLat() {
        return maxLat;
    }
    
    /**
     * Gets the minimum longitude.
     * 
     * @return Minimum longitude value
     */
    public double getMinLon() {
        return minLon;
    }
    
    /**
     * Gets the maximum longitude.
     * 
     * @return Maximum longitude value
     */
    public double getMaxLon() {
        return maxLon;
    }
    
    /**
     * Creates a new MapBoundary with padding added to the current boundaries.
     * 
     * @param paddingPercent Percentage of the range to add as padding (e.g., 0.05 for 5%)
     * @return A new MapBoundary with padding added
     */
    public MapBoundary withPadding(double paddingPercent) {
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;
        double latPadding = latRange * paddingPercent;
        double lonPadding = lonRange * paddingPercent;
        
        return new MapBoundary(
                minLat - latPadding,
                maxLat + latPadding,
                minLon - lonPadding,
                maxLon + lonPadding
        );
    }
    
    @Override
    public String toString() {
        return "MapBoundary{" +
                "latRange=[" + minLat + ", " + maxLat + "], " +
                "lonRange=[" + minLon + ", " + maxLon + "]" +
                '}';
    }
}
