package com.art.ascii.loader;

import java.util.List;
import java.util.Set;

/**
 * Interface for loading coordinate data from various sources.
 * Provides methods for retrieving available country codes and coordinate data.
 */
public interface CoordinateDataLoader {
    
    /**
     * Gets a set of all country codes available in the data source.
     * 
     * @return Set of country codes that have data available
     */
    Set<String> getAvailableCountryCodes();
    
    /**
     * Loads coordinates for a specific country from the data source.
     * 
     * @param countryCode The two-letter country code to filter coordinates by
     * @return List of coordinate pairs [lat, lon] for the specified country
     */
    List<double[]> loadCoordinatesForCountry(String countryCode);
    
    /**
     * Checks if the specified country code is available in the data source.
     * 
     * @param countryCode The two-letter country code to check
     * @return true if the country code is available, false otherwise
     */
    boolean hasCountry(String countryCode);
}
