package com.art.ascii;

import com.art.ascii.model.Coordinate;
import com.art.ascii.model.MapBoundary;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders UK postcode coordinates as ASCII art resembling a map of the UK.
 */
@Component
public class UKAsciiMapRenderer {
    private static final int GRID_HEIGHT = 60;
    private static final int GRID_WIDTH = 120;
    
    // These are only used for validation of lat long boundaries, not for rendering boundaries
    private static final double VALID_LAT_MIN = 49.0;
    private static final double VALID_LAT_MAX = 61.0;
    private static final double VALID_LON_MIN = -9.0;
    private static final double VALID_LON_MAX = 3.0;

    private static final String UK_POSTCODES_CSV = "ukpostcodes.csv";

    // Character constants
    private static final char MAP_CHAR = '*';
    private static final char EMPTY_CHAR = ' ';
    
    // Default batch size for parallel processing
    private static final int DEFAULT_BATCH_SIZE = 10000;

    public UKAsciiMapRenderer() {
    }

    /**
     * Main method to run the renderer.
     */
    public static void main(String[] args) {
        String map = new UKAsciiMapRenderer().renderMap();
        System.out.println(map);
    }

    /**
     * Renders the ASCII map from the postcode CSV.
     * @return String containing the rendered ASCII map
     */
    public String renderMap() {
        System.out.println("Rendering UK ASCII map...");
        long startTime = System.currentTimeMillis();

        char[][] grid = createAsciiGridWithDynamicBoundaries();

        if (grid.length == 0) {
            return "No valid coordinates to render.";
        }

        String result = gridToString(grid);

        long endTime = System.currentTimeMillis();
        System.out.println("Rendering completed in " + (endTime - startTime) + "ms");

        return result;
    }

    /**
     * Gets the width of the ASCII map grid.
     * 
     * @return The width in characters
     */
    public int getWidth() {
        return GRID_WIDTH;
    }
    
    /**
     * Gets the height of the ASCII map grid.
     * 
     * @return The height in characters
     */
    public int getHeight() {
        return GRID_HEIGHT;
    }

    /**
     * Creates an ASCII grid from coordinates with dynamic boundaries.
     * @return 2D char array representing the ASCII art.
     */
    private char[][] createAsciiGridWithDynamicBoundaries() {
        LoadResult result = loadCoordinatesWithBoundaries();
        List<Coordinate> coordinates = result.getCoordinates();

        if (coordinates.isEmpty()) {
            System.err.println("No coordinates to render");
            return new char[0][0];
        }

        // Get the calculated boundaries
        MapBoundary boundary = result.getBoundary();
        
        // Initialize grid with spaces
        char[][] grid = new char[GRID_HEIGHT][GRID_WIDTH];
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                grid[i][j] = EMPTY_CHAR;
            }
        }

        // Map coordinates to grid using dynamic boundaries.
        for (Coordinate coord : coordinates) {
            // Scale latitude to row (invert: higher latitude = lower row)
            int row = (int) ((boundary.getMaxLat() - coord.getLatitude()) / 
                    (boundary.getMaxLat() - boundary.getMinLat()) * (GRID_HEIGHT - 1));
            // Scale longitude to column
            int col = (int) ((coord.getLongitude() - boundary.getMinLon()) / 
                    (boundary.getMaxLon() - boundary.getMinLon()) * (GRID_WIDTH - 1));

            // Place character if within bounds
            if (row >= 0 && row < GRID_HEIGHT && col >= 0 && col < GRID_WIDTH) {
                grid[row][col] = MAP_CHAR;
            }
        }

        return grid;
    }

    /**
     * Loads postcode coordinates from the CSV file with dynamic boundary calculation.
     * This implementation uses parallel batch processing for improved performance.
     * @return LoadResult containing coordinates and their boundaries
     */
    private LoadResult loadCoordinatesWithBoundaries() {
        ConcurrentLinkedQueue<Coordinate> coordinateQueue = new ConcurrentLinkedQueue<>();

        double[] boundaries = {
            Double.MAX_VALUE,  // minLat
            -Double.MAX_VALUE, // maxLat
            Double.MAX_VALUE,  // minLon
            -Double.MAX_VALUE  // maxLon
        };

        AtomicInteger totalRows = new AtomicInteger(0);
        AtomicInteger validRows = new AtomicInteger(0);
        AtomicInteger skippedRows = new AtomicInteger(0);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(UK_POSTCODES_CSV)) {
            if (is == null) {
                System.err.println("Error: Could not find resource " + UK_POSTCODES_CSV);
                return createDefaultResult();
            }
            
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                // Skip header
                csvReader.readNext();
                
                // Process batches of rows in parallel
                List<List<String[]>> batches = new ArrayList<>();
                List<String[]> currentBatch = new ArrayList<>(DEFAULT_BATCH_SIZE);
                String[] line;
                
                // Prepare batches
                while ((line = csvReader.readNext()) != null) {
                    totalRows.incrementAndGet();
                    currentBatch.add(line);
                    
                    if (currentBatch.size() >= DEFAULT_BATCH_SIZE) {
                        batches.add(currentBatch);
                        currentBatch = new ArrayList<>(DEFAULT_BATCH_SIZE);
                    }
                }
                
                // Add the last batch if it's not empty
                if (!currentBatch.isEmpty()) {
                    batches.add(currentBatch);
                }
                
                // Process batches in parallel
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (List<String[]> batch : batches) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        processBatch(batch, coordinateQueue, boundaries, validRows, skippedRows);
                    });
                    futures.add(future);
                }
                
                // Wait for all futures to complete
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
                );
                
                try {
                    allFutures.get();  // Wait for all batches to be processed
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error processing batches: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    return createDefaultResult();
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Error loading CSV: " + e.getMessage());
            return createDefaultResult();
        }
        
        // Convert queue to list
        List<Coordinate> coordinates = new ArrayList<>(coordinateQueue);
        
        System.out.println("CSV processing complete. Total rows: " + totalRows.get() + 
                ", Valid: " + validRows.get() + ", Skipped: " + skippedRows.get());
        
        if (coordinates.isEmpty()) {
            return createDefaultResult();
        }
        
        // Add small padding to boundaries for better visualization
        double latRange = boundaries[1] - boundaries[0];
        double lonRange = boundaries[3] - boundaries[2];
        double latPadding = latRange * 0.05;
        double lonPadding = lonRange * 0.05;
        
        MapBoundary boundary = new MapBoundary(
                boundaries[0] - latPadding, // minLat
                boundaries[1] + latPadding, // maxLat
                boundaries[2] - lonPadding, // minLon
                boundaries[3] + lonPadding  // maxLon
        );
        
        return new LoadResult(coordinates, boundary);
    }
    
    /**
     * Process a batch of CSV lines in parallel.
     * 
     * @param batch The batch of CSV lines to process
     * @param coordinateQueue Queue to store valid coordinates
     * @param boundaries Array to track min/max lat/lon boundaries [minLat, maxLat, minLon, maxLon]
     * @param validRows Counter for valid rows
     * @param skippedRows Counter for skipped rows
     */
    private void processBatch(List<String[]> batch, ConcurrentLinkedQueue<Coordinate> coordinateQueue, 
                             double[] boundaries, AtomicInteger validRows, AtomicInteger skippedRows) {
        for (String[] line : batch) {
            // CSV format: id, postcode, latitude, longitude
            if (line.length < 4) {
                skippedRows.incrementAndGet();
                continue;
            }
            
            try {
                // Extract latitude and longitude values from columns 2 and 3 (0-indexed)
                double lat = Double.parseDouble(line[2]);
                double lon = Double.parseDouble(line[3]);
                
                // Filter out obviously invalid coordinates
                if (lat < VALID_LAT_MIN || lat > VALID_LAT_MAX || lon < VALID_LON_MIN || lon > VALID_LON_MAX) {
                    skippedRows.incrementAndGet();
                    continue;
                }
                
                // Add to coordinates
                coordinateQueue.add(new Coordinate(lat, lon));
                validRows.incrementAndGet();
                
                // Update boundaries (thread-safe)
                synchronized (boundaries) {
                    updateBoundaries(boundaries, lat, lon);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                skippedRows.incrementAndGet();
            }
        }
    }
    
    /**
     * Thread-safe update of boundaries array.
     * 
     * @param boundaries Array holding [minLat, maxLat, minLon, maxLon]
     * @param lat Latitude value
     * @param lon Longitude value
     */
    private void updateBoundaries(double[] boundaries, double lat, double lon) {
        boundaries[0] = Math.min(boundaries[0], lat); // minLat
        boundaries[1] = Math.max(boundaries[1], lat); // maxLat
        boundaries[2] = Math.min(boundaries[2], lon); // minLon
        boundaries[3] = Math.max(boundaries[3], lon); // maxLon
    }
    
    /**
     * Creates a default result when no valid coordinates are found.
     */
    private LoadResult createDefaultResult() {
        return new LoadResult(
            new ArrayList<>(),
            new MapBoundary(VALID_LAT_MIN, VALID_LAT_MAX, VALID_LON_MIN, VALID_LON_MAX)
        );
    }
    
    /**
     * Converts the ASCII grid to a string.
     * Optimized for performance with StringBuilder.
     * @param grid 2D char array to convert.
     * @return String representation of the ASCII art.
     */
    private String gridToString(char[][] grid) {
        StringBuilder sb = new StringBuilder(GRID_HEIGHT * (GRID_WIDTH + 1));
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                sb.append(grid[i][j]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
    /**
     * Container class for coordinates and their boundary.
     */
    private static class LoadResult {
        private final List<Coordinate> coordinates;
        private final MapBoundary boundary;
        
        public LoadResult(List<Coordinate> coordinates, MapBoundary boundary) {
            this.coordinates = coordinates;
            this.boundary = boundary;
        }
        
        public List<Coordinate> getCoordinates() {
            return coordinates;
        }
        
        public MapBoundary getBoundary() {
            return boundary;
        }
    }
}