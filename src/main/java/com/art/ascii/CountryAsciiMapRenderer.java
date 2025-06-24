package com.art.ascii;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.CSVParserBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Renders ASCII maps of countries based on postal code latitude and longitude data.
 * This class provides functionality to:
 * 1. Extract coordinates for specific countries from a CSV file
 * 2. Create a density grid representing population distribution
 * 3. Generate an ASCII art representation of country shapes
 */
public class CountryAsciiMapRenderer {

    private static final Logger logger = LoggerFactory.getLogger(CountryAsciiMapRenderer.class);

    // Configuration constants
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 60;
    private static final String DEFAULT_CSV_PATH = "geonames-postal-code.csv";
    private static final int BATCH_SIZE = 1000;
    private static final int DENSITY_RADIUS = 1;

    // Coordinate validation boundaries
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    // Rendering characters
    private static final char POINT_CHAR = '*';
    private static final char EMPTY_CHAR = ' ';
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    // Cache of available country codes (thread-safe)
    private static final Map<String, Set<String>> COUNTRY_CODE_CACHE = new ConcurrentHashMap<>();

    // Instance variables
    private final int width;
    private final int height;
    private final String csvFilePath;
    private final boolean useAnsiColors;

    /**
     * Creates a new CountryAsciiMapRenderer with the specified dimensions and configuration.
     *
     * @param width The width of the ASCII map
     * @param height The height of the ASCII map
     * @param csvFilePath Path to the CSV file containing postal code data
     * @param useAnsiColors Whether to use ANSI color codes in the output
     */
    public CountryAsciiMapRenderer(int width, int height, String csvFilePath, boolean useAnsiColors) {
        this.width = Math.max(10, width);  // Ensure minimum dimensions
        this.height = Math.max(5, height);
        this.csvFilePath = csvFilePath != null ? csvFilePath : DEFAULT_CSV_PATH;
        this.useAnsiColors = useAnsiColors;
    }

    /**
     * Creates a new CountryAsciiMapRenderer with default settings.
     */
    public CountryAsciiMapRenderer() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_CSV_PATH, false);
    }

    /**
     * Gets a set of all country codes available in the CSV file.
     * Uses caching to avoid repeated file reads.
     *
     * @param csvFilePath Path to the CSV file
     * @return Set of country codes that have data in the CSV file
     */
    public static Set<String> getAvailableCountryCodes(String csvFilePath) {
        String path = csvFilePath != null ? csvFilePath : DEFAULT_CSV_PATH;

        // Check if we already have the country codes in the cache
        return COUNTRY_CODE_CACHE.computeIfAbsent(path, CountryAsciiMapRenderer::loadCountryCodesFromCsv);
    }

    /**
     * Loads country codes from the CSV file.
     * This is a helper method for the country code cache.
     *
     * @param csvFilePath Path to the CSV file
     * @return Set of country codes
     */
    private static Set<String> loadCountryCodesFromCsv(String csvFilePath) {
        Set<String> countryCodes = new HashSet<>();

        try (InputStream inputStream = CountryAsciiMapRenderer.class.getClassLoader().getResourceAsStream(csvFilePath)) {
            if (inputStream == null) {
                logger.error("Could not find CSV file: {}", csvFilePath);
                return Collections.emptySet();
            }

            // Create CSV parser with semicolon as separator
            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                    .build()) {

                String[] header = reader.readNext(); // Skip header

                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length >= 1 && line[0] != null && !line[0].isEmpty()) {
                        countryCodes.add(line[0]);
                    }
                }
            }

            logger.info("Loaded {} country codes from {}", countryCodes.size(), csvFilePath);
            return Collections.unmodifiableSet(countryCodes);

        } catch (IOException | CsvValidationException e) {
            logger.error("Error reading CSV file: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Renders an ASCII map for the specified country code.
     *
     * @param countryCode The two-letter country code to filter coordinates by
     * @return ASCII representation of the country's map
     */
    public String renderCountryMap(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            logger.warn("Invalid country code provided: null or empty");
            return "Invalid country code";
        }

        String normalizedCountryCode = countryCode.trim().toUpperCase();
        logger.info("Rendering ASCII map for country: {}, dimensions: {}x{}",
                normalizedCountryCode, width, height);

        List<double[]> filteredCoordinates = loadCoordinatesForCountry(normalizedCountryCode);

        if (filteredCoordinates.isEmpty()) {
            logger.warn("No coordinates found for country code: {}", normalizedCountryCode);
            return "No data found for country code: " + normalizedCountryCode;
        }

        // Calculate map boundaries from coordinates
        MapBoundary boundary = calculateBoundaries(filteredCoordinates);

        // Create the grid with density values
        int[][] densityGrid = createDensityGrid(filteredCoordinates, boundary);

        // Convert density grid to ASCII grid
        char[][] asciiGrid = convertDensityGridToAscii(densityGrid);

        // Render the ASCII grid to a string
        return renderAsciiGrid(asciiGrid);
    }

    /**
     * Calculates the map boundaries from a list of coordinates.
     *
     * @param coordinates List of coordinate pairs [lat, lon]
     * @return MapBoundary object containing min/max values for lat/lon
     */
    private MapBoundary calculateBoundaries(List<double[]> coordinates) {
        double minLat = coordinates.stream().mapToDouble(coord -> coord[0]).min().orElse(0);
        double maxLat = coordinates.stream().mapToDouble(coord -> coord[0]).max().orElse(0);
        double minLon = coordinates.stream().mapToDouble(coord -> coord[1]).min().orElse(0);
        double maxLon = coordinates.stream().mapToDouble(coord -> coord[1]).max().orElse(0);

        // Add a small buffer (1% of the range) to the boundaries for better visualization
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;
        double latBuffer = latRange * 0.01;
        double lonBuffer = lonRange * 0.01;

        minLat -= latBuffer;
        maxLat += latBuffer;
        minLon -= lonBuffer;
        maxLon += lonBuffer;

        return new MapBoundary(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Helper class to store map boundaries.
     */
    private static class MapBoundary {
        final double minLat;
        final double maxLat;
        final double minLon;
        final double maxLon;

        MapBoundary(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }

    /**
     * Loads coordinates for a specific country from the CSV file.
     * Uses parallel processing with CompletableFuture for efficient handling of large files.
     *
     * @param countryCode The country code to filter by
     * @return List of coordinate pairs [lat, lon]
     */
    private List<double[]> loadCoordinatesForCountry(String countryCode) {
        logger.debug("Loading coordinates for country: {}", countryCode);
        List<double[]> coordinates = new ArrayList<>();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvFilePath)) {
            if (inputStream == null) {
                logger.error("Could not find CSV file: {}", csvFilePath);
                return Collections.emptyList();
            }

            logger.debug("Processing CSV with CompletableFuture");

            try {
                // Create CSV parser with semicolon as separator
                try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                        .build()) {

                    String[] header = reader.readNext(); // Skip header

                    // Read and process the CSV file in batches
                    List<String[]> batch = new ArrayList<>(BATCH_SIZE);
                    List<CompletableFuture<List<double[]>>> futures = new ArrayList<>();

                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        batch.add(line);

                        if (batch.size() >= BATCH_SIZE) {
                            submitBatch(batch, countryCode, futures);
                        }
                    }

                    // Process the final batch if it's not empty
                    if (!batch.isEmpty()) {
                        submitBatch(batch, countryCode, futures);
                    }

                    // Wait for all futures to complete
                    CompletableFuture<Void> allFutures = CompletableFuture
                            .allOf(futures.toArray(new CompletableFuture[0]));

                    // Join and collect results
                    allFutures.join();

                    for (CompletableFuture<List<double[]>> future : futures) {
                        try {
                            List<double[]> result = future.get(); // This shouldn't block now as allOf.join() completed
                            if (result != null) {
                                coordinates.addAll(result);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.error("Thread interrupted while processing batch", e);
                            break;
                        } catch (ExecutionException e) {
                            logger.error("Error processing batch: {}", e.getCause().getMessage());
                        }
                    }
                }
            } catch (IOException | CsvValidationException e) {
                logger.error("Error reading CSV file: {}", e.getMessage());
            }
        } catch (IOException e) {
            logger.error("Error opening input stream for CSV file: {}", e.getMessage());
        }

        logger.info("Loaded {} coordinates for country code: {}", coordinates.size(), countryCode);
        return coordinates;
    }

    /**
     * Submits a batch of CSV lines for parallel processing using CompletableFuture.
     *
     * @param batch The batch of CSV lines to process
     * @param countryCode The country code to filter by
     * @param futures Collection to store the CompletableFuture results
     */
    private void submitBatch(List<String[]> batch, String countryCode,
                            List<CompletableFuture<List<double[]>>> futures) {
        List<String[]> batchToProcess = new ArrayList<>(batch);
        batch.clear();

        CompletableFuture<List<double[]>> future = CompletableFuture
            .supplyAsync(() -> processBatch(batchToProcess, countryCode))
            .exceptionally(ex -> {
                logger.error("Error processing batch: {}", ex.getMessage());
                return Collections.emptyList();
            });

        futures.add(future);
    }

    /**
     * Processes a batch of CSV lines to extract coordinates for a specific country.
     *
     * @param batch The batch of CSV lines
     * @param countryCode The country code to filter by
     * @return List of coordinate pairs for matching country codes
     */
    private List<double[]> processBatch(List<String[]> batch, String countryCode) {
        return batch.stream()
                .filter(line -> isValidCsvLine(line, countryCode))
                .map(this::extractCoordinates)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a CSV line is valid and matches the required country code.
     *
     * @param line The CSV line to check
     * @param countryCode The country code to match
     * @return true if valid, false otherwise
     */
    private boolean isValidCsvLine(String[] line, String countryCode) {
        return line.length >= 11 && countryCode.equalsIgnoreCase(line[0]);
    }

    /**
     * Extracts coordinates from a CSV line.
     *
     * @param line The CSV line to extract from
     * @return Coordinate pair [lat, lon] or null if invalid
     */
    private double[] extractCoordinates(String[] line) {
        try {
            double lat = Double.parseDouble(line[9]);  // Latitude
            double lon = Double.parseDouble(line[10]); // Longitude

            if (isValidCoordinate(lat, lon)) {
                return new double[] { lat, lon };
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Silently skip invalid entries
        }
        return null;
    }

    /**
     * Validates if a coordinate is within valid latitude and longitude ranges.
     *
     * @param lat Latitude value
     * @param lon Longitude value
     * @return true if valid, false otherwise
     */
    public static boolean isValidCoordinate(double lat, double lon) {
        return lat >= MIN_LATITUDE && lat <= MAX_LATITUDE &&
               lon >= MIN_LONGITUDE && lon <= MAX_LONGITUDE;
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

        logger.debug("Creating density grid with {} coordinates", coordinates.size());

        // Enhanced approach - use a configurable radius for each point to increase density
        for (double[] coord : coordinates) {
            double lat = coord[0];
            double lon = coord[1];

            // Map the coordinate to a grid cell
            int centerX = (int) ((lon - boundary.minLon) / (boundary.maxLon - boundary.minLon) * (width - 1));
            // Note: We invert the y-axis to match the typical map orientation
            int centerY = height - 1 - (int) ((lat - boundary.minLat) / (boundary.maxLat - boundary.minLat) * (height - 1));

            // Ensure we stay within bounds (due to floating point precision issues)
            if (centerX >= 0 && centerX < width && centerY >= 0 && centerY < height) {
                // Create a "blob" effect by incrementing a small area around the point
                for (int y = Math.max(0, centerY - DENSITY_RADIUS); y <= Math.min(height - 1, centerY + DENSITY_RADIUS); y++) {
                    for (int x = Math.max(0, centerX - DENSITY_RADIUS); x <= Math.min(width - 1, centerX + DENSITY_RADIUS); x++) {
                        // Points closer to center get higher density values
                        int distance = Math.abs(x - centerX) + Math.abs(y - centerY);
                        int densityIncrement = DENSITY_RADIUS + 1 - distance;
                        if (densityIncrement > 0) {
                            densityGrid[y][x] += densityIncrement;
                        }
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

        // Simply convert any cell with density > 0 to POINT_CHAR
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
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
        StringBuilder sb = new StringBuilder((height + 1) * width);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (useAnsiColors && asciiGrid[y][x] != EMPTY_CHAR) {
                    // Add ANSI color codes for non-empty characters
                    sb.append(ANSI_GREEN)
                      .append(asciiGrid[y][x])
                      .append(ANSI_RESET);
                } else {
                    sb.append(asciiGrid[y][x]);
                }
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}
