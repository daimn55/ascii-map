package com.art.ascii;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders UK postcode coordinates as ASCII art resembling a map of the UK.
 * This version includes dynamic boundary calculation based on the coordinates.
 */
public class SimpleAsciiMapRenderer {
    private static final int GRID_HEIGHT = 60;
    private static final int GRID_WIDTH = 120;
    
    // These are only used for validation, not for rendering boundaries
    private static final double VALID_LAT_MIN = 49.0;
    private static final double VALID_LAT_MAX = 61.0;
    private static final double VALID_LON_MIN = -9.0;
    private static final double VALID_LON_MAX = 3.0;
    
    /**
     * Represents a latitude and longitude coordinate.
     */
    public static class Coordinate {
        double latitude;
        double longitude;

        Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /**
     * Result class containing coordinates and their boundaries.
     */
    private static class CoordinateResult {
        List<Coordinate> coordinates;
        double minLatitude;
        double maxLatitude;
        double minLongitude;
        double maxLongitude;

        CoordinateResult(List<Coordinate> coordinates,
                        double minLatitude, double maxLatitude,
                        double minLongitude, double maxLongitude) {
            this.coordinates = coordinates;
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
        }
    }

    /**
     * Creates a SimpleAsciiMapRenderer.
     */
    public SimpleAsciiMapRenderer() {
    }

    /**
     * Loads postcode coordinates from the CSV file with dynamic boundary calculation.
     * @return CoordinateResult containing coordinates and their boundaries
     */
    private CoordinateResult loadCoordinatesWithBoundaries() {
        List<Coordinate> coordinates = new ArrayList<>();

        // Initialize boundaries to extreme values
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("ukpostcodes.csv");
             CSVReader csvReader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            // Skip header
            csvReader.readNext();
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                try {
                    // Assume columns: id, postcode, latitude, longitude
                    double lat = Double.parseDouble(line[2]);
                    double lon = Double.parseDouble(line[3]);

                    // Basic validation to filter out extreme outliers
                    if (lat >= VALID_LAT_MIN && lat <= VALID_LAT_MAX &&
                        lon >= VALID_LON_MIN && lon <= VALID_LON_MAX) {

                        coordinates.add(new Coordinate(lat, lon));

                        // Update boundaries
                        minLat = Math.min(minLat, lat);
                        maxLat = Math.max(maxLat, lat);
                        minLon = Math.min(minLon, lon);
                        maxLon = Math.max(maxLon, lon);
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                    // Skip invalid rows
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Error loading CSV: " + e.getMessage());
        }
        
        // Add a small buffer to the boundaries (1% of range)
        if (!coordinates.isEmpty()) {
            double latBuffer = (maxLat - minLat) * 0.01;
            double lonBuffer = (maxLon - minLon) * 0.01;

            minLat -= latBuffer;
            maxLat += latBuffer;
            minLon -= lonBuffer;
            maxLon += lonBuffer;

            System.out.println("Calculated boundaries: lat [" + minLat + ", " + maxLat +
                    "], lon [" + minLon + ", " + maxLon + "]");
        } else {
            // Use defaults if no coordinates were found
            minLat = 50.0;
            maxLat = 59.0;
            minLon = -8.0;
            maxLon = 2.0;
        }

        return new CoordinateResult(coordinates, minLat, maxLat, minLon, maxLon);
    }
    
    /**
     * Creates an ASCII grid from coordinates with dynamic boundaries.
     * @return 2D char array representing the ASCII art.
     */
    private char[][] createAsciiGridWithDynamicBoundaries() {
        CoordinateResult result = loadCoordinatesWithBoundaries();
        List<Coordinate> coordinates = result.coordinates;
        
        if (coordinates.isEmpty()) {
            return new char[0][0];
        }
        
        // Use the calculated boundaries
        double latMin = result.minLatitude;
        double latMax = result.maxLatitude;
        double lonMin = result.minLongitude;
        double lonMax = result.maxLongitude;

        // Initialize grid with spaces
        char[][] grid = new char[GRID_HEIGHT][GRID_WIDTH];
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                grid[i][j] = ' ';
            }
        }

        // Map coordinates to grid using dynamic boundaries
        for (Coordinate coord : coordinates) {
            // Scale latitude to row (invert: higher latitude = lower row)
            int row = (int) ((latMax - coord.latitude) / (latMax - latMin) * (GRID_HEIGHT - 1));
            // Scale longitude to column
            int col = (int) ((coord.longitude - lonMin) / (lonMax - lonMin) * (GRID_WIDTH - 1));
            // Place '*' if within bounds
            if (row >= 0 && row < GRID_HEIGHT && col >= 0 && col < GRID_WIDTH) {
                grid[row][col] = '*';
            }
        }
        return grid;
    }

    /**
     * Converts the ASCII grid to a string.
     * @param grid 2D char array to convert.
     * @return String representation of the ASCII art.
     */
    private String gridToString(char[][] grid) {
        if (grid.length == 0) {
            return "No grid to display.";
        }

        StringBuilder sb = new StringBuilder();
        for (char[] row : grid) {
            sb.append(new String(row)).append('\n');
        }
        return sb.toString();
    }

    /**
     * Renders the ASCII map from the postcode CSV.
     * @return String containing the rendered ASCII map
     */
    public String renderMap() {
        System.out.println("Rendering map with dynamic boundaries...");
        char[][] grid = createAsciiGridWithDynamicBoundaries();
        if (grid.length == 0) {
            return "No valid coordinates to render.";
        }
        return gridToString(grid);
    }

    public static void main(String[] args) {
        String map = new SimpleAsciiMapRenderer().renderMap();
        System.out.println(map);
    }
}