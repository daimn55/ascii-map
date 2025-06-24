package com.art.ascii.renderer;

import com.art.ascii.loader.CoordinateDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Renders ASCII maps of countries based on postal code latitude and longitude data.
 * This class provides functionality to:
 * 1. Create a density grid representing population distribution
 * 2. Generate an ASCII art representation of country shapes
 */
public class CountryAsciiMapRenderer implements AsciiMapRenderer {

    private static final Logger logger = LoggerFactory.getLogger(CountryAsciiMapRenderer.class);
    
    // Configuration constants
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 60;
    private static final String DEFAULT_CSV_PATH = "geonames-postal-code.csv";
    private static final int DENSITY_RADIUS = 1;
    
    // Rendering characters
    private static final char POINT_CHAR = '*';
    private static final char EMPTY_CHAR = ' ';
    
    // Instance variables
    private final int width;
    private final int height;
    private final CoordinateDataLoader dataLoader;
    
    /**
     * Creates a new CountryAsciiMapRenderer with the specified dimensions and configuration.
     *
     * @param width The width of the ASCII map
     * @param height The height of the ASCII map
     * @param csvFilePath Path to the CSV file containing postal code data
     */
    public CountryAsciiMapRenderer(int width, int height, String csvFilePath) {
        this.width = Math.max(10, width);  // Ensure minimum dimensions
        this.height = Math.max(5, height);
        this.dataLoader = null; // This constructor is deprecated and will be removed
    }
    
    /**
     * Creates a new CountryAsciiMapRenderer with default settings.
     */
    public CountryAsciiMapRenderer() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_CSV_PATH);
    }
    
    /**
     * Creates a new CountryAsciiMapRenderer with a specific data loader.
     *
     * @param width The width of the ASCII map
     * @param height The height of the ASCII map 
     * @param dataLoader The loader to use for coordinate data
     */
    public CountryAsciiMapRenderer(int width, int height, CoordinateDataLoader dataLoader) {
        this.width = Math.max(10, width);
        this.height = Math.max(5, height);
        this.dataLoader = dataLoader;
    }
    
    /**
     * Renders an ASCII map from a list of coordinates.
     * 
     * @param coordinates List of coordinate pairs [lat, lon] to render
     * @return String representation of the ASCII map
     */
    @Override
    public String renderMap(List<double[]> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            logger.warn("No coordinates provided for rendering");
            return "No coordinates provided";
        }
        
        logger.debug("Rendering {} coordinates as ASCII map", coordinates.size());
        
        // Calculate map boundaries from coordinates
        MapBoundary boundary = calculateBoundaries(coordinates);
        
        // Create a density grid and convert to ASCII characters
        int[][] densityGrid = createDensityGrid(coordinates, boundary);
        char[][] asciiGrid = convertDensityGridToAscii(densityGrid);
        
        return renderAsciiGrid(asciiGrid);
    }
    
    /**
     * Calculates the map boundaries from a list of coordinates.
     * 
     * @param coordinates List of coordinate pairs [lat, lon]
     * @return MapBoundary object containing min/max values for lat/lon
     */
    private MapBoundary calculateBoundaries(List<double[]> coordinates) {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        
        for (double[] coord : coordinates) {
            double lat = coord[0];
            double lon = coord[1];
            
            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
        }
        
        // Add a small padding to avoid points exactly at the edge
        double latPadding = (maxLat - minLat) * 0.05;
        double lonPadding = (maxLon - minLon) * 0.05;
        
        return new MapBoundary(
                minLat - latPadding,
                maxLat + latPadding,
                minLon - lonPadding,
                maxLon + lonPadding
        );
    }
    
    /**
     * Container for map boundary information.
     */
    private static class MapBoundary {
        private final double minLat;
        private final double maxLat;
        private final double minLon;
        private final double maxLon;
        
        public MapBoundary(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }
    
    /**
     * Creates a density grid from coordinates.
     * 
     * @param coordinates List of coordinate pairs [lat, lon]
     * @param boundary Map boundaries
     * @return 2D integer array representing the density at each grid point
     */
    private int[][] createDensityGrid(List<double[]> coordinates, MapBoundary boundary) {
        int[][] densityGrid = new int[height][width];
        
        for (double[] coord : coordinates) {
            double lat = coord[0];
            double lon = coord[1];
            
            // Map the coordinate to a cell in the grid
            int gridY = height - 1 - (int)((lat - boundary.minLat) / (boundary.maxLat - boundary.minLat) * (height - 1));
            int gridX = (int)((lon - boundary.minLon) / (boundary.maxLon - boundary.minLon) * (width - 1));
            
            // Skip if outside grid (shouldn't happen with proper boundaries)
            if (gridY < 0 || gridY >= height || gridX < 0 || gridX >= width) {
                continue;
            }
            
            // Increment density for the cell and surrounding cells (density radius)
            for (int y = Math.max(0, gridY - DENSITY_RADIUS); y <= Math.min(height - 1, gridY + DENSITY_RADIUS); y++) {
                for (int x = Math.max(0, gridX - DENSITY_RADIUS); x <= Math.min(width - 1, gridX + DENSITY_RADIUS); x++) {
                    int distance = Math.abs(y - gridY) + Math.abs(x - gridX);
                    if (distance <= DENSITY_RADIUS) {
                        densityGrid[y][x]++;
                    }
                }
            }
        }
        
        return densityGrid;
    }
    
    /**
     * Converts a density grid to an ASCII character grid.
     * 
     * @param densityGrid 2D integer array with density values
     * @return 2D character array with ASCII art
     */
    private char[][] convertDensityGridToAscii(int[][] densityGrid) {
        char[][] asciiGrid = new char[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Any cell with a density > 0 gets a point character, others are empty
                asciiGrid[y][x] = densityGrid[y][x] > 0 ? POINT_CHAR : EMPTY_CHAR;
            }
        }
        
        return asciiGrid;
    }
    
    /**
     * Renders a character grid as a formatted string.
     * 
     * @param asciiGrid 2D character array to render
     * @return String representation of the ASCII art
     */
    private String renderAsciiGrid(char[][] asciiGrid) {
        StringBuilder sb = new StringBuilder();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(asciiGrid[y][x]);
            }
            sb.append('\n');
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the current width setting for the renderer.
     * 
     * @return The width in characters
     */
    @Override
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the current height setting for the renderer.
     * 
     * @return The height in characters
     */
    @Override
    public int getHeight() {
        return height;
    }
    
    /**
     * Creates a new renderer with the specified dimensions.
     * 
     * @param width The width in characters
     * @param height The height in characters
     * @return A new renderer with the updated dimensions
     */
    @Override
    public AsciiMapRenderer withDimensions(int width, int height) {
        return new CountryAsciiMapRenderer(width, height, this.dataLoader);
    }
}
