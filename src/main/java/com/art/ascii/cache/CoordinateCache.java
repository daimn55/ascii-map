package com.art.ascii.cache;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service that caches country coordinates during application startup
 * to avoid repeated CSV file reading and parsing for each API request.
 */
@Service
public class CoordinateCache {
    private static final Logger logger = LoggerFactory.getLogger(CoordinateCache.class);
    
    // Cache structures
    private final Map<String, List<double[]>> coordinateCache = new ConcurrentHashMap<>();
    private final Set<String> availableCountries = ConcurrentHashMap.newKeySet();
    
    // Configuration
    private final String csvFilePath;
    private final boolean preloadCoordinates;
    private final int batchSize;
    
    // Constants
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    
    /**
     * Constructs a new CoordinateCache with configurable settings.
     * 
     * @param csvFilePath Path to the CSV file with coordinate data
     * @param preloadCoordinates Whether to preload all coordinates (true) or just country codes (false)
     * @param batchSize Size of batches for parallel processing
     */
    public CoordinateCache(
            @Value("${ascii.map.csv-file-path:geonames-postal-code.csv}") String csvFilePath,
            @Value("${ascii.map.preload-coordinates:true}") boolean preloadCoordinates,
            @Value("${ascii.map.batch-size:1000}") int batchSize) {
        this.csvFilePath = csvFilePath;
        this.preloadCoordinates = preloadCoordinates;
        this.batchSize = batchSize;
    }
    
    /**
     * Initializes the cache during application startup.
     */
    @PostConstruct
    public void initializeCache() {
        logger.info("Starting to load country data into cache");
        long startTime = System.currentTimeMillis();
        
        // Always load country codes
        loadCountryCodes();
        
        // Conditionally load all coordinates
        if (preloadCoordinates) {
            logger.info("Preloading coordinates for all countries");
            for (String countryCode : availableCountries) {
                loadCoordinatesForCountry(countryCode);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        int coordinateCount = coordinateCache.values().stream().mapToInt(List::size).sum();
        logger.info("Finished loading data for {} countries ({} coordinates) in {} ms", 
                    availableCountries.size(), coordinateCount, duration);
    }
    
    /**
     * Gets the set of all available country codes.
     * 
     * @return Unmodifiable set of country codes
     */
    public Set<String> getAvailableCountries() {
        return Collections.unmodifiableSet(availableCountries);
    }
    
    /**
     * Checks if coordinates for a country are available.
     * 
     * @param countryCode The country code to check
     * @return true if the country exists in the data, false otherwise
     */
    public boolean hasCountry(String countryCode) {
        if (countryCode == null) {
            return false;
        }
        return availableCountries.contains(countryCode.toUpperCase());
    }
    
    /**
     * Gets coordinates for a specific country.
     * If coordinates weren't preloaded, they will be loaded on demand.
     * 
     * @param countryCode The country code to get coordinates for
     * @return List of coordinate pairs [lat, lon]
     */
    public List<double[]> getCoordinatesForCountry(String countryCode) {
        if (countryCode == null) {
            return Collections.emptyList();
        }
        
        String normalizedCode = countryCode.toUpperCase();
        if (!availableCountries.contains(normalizedCode)) {
            return Collections.emptyList();
        }
        
        // Load coordinates if they're not already in the cache
        return coordinateCache.computeIfAbsent(normalizedCode, this::loadCoordinatesForCountry);
    }
    
    /**
     * Loads all country codes from the CSV file.
     */
    private void loadCountryCodes() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvFilePath)) {
            if (inputStream == null) {
                logger.error("Could not find CSV file: {}", csvFilePath);
                return;
            }
            
            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                    .build()) {
                
                String[] header = reader.readNext(); // Skip header
                
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length >= 1 && line[0] != null && !line[0].isEmpty()) {
                        availableCountries.add(line[0].toUpperCase());
                    }
                }
            }
            
            logger.info("Loaded {} country codes from {}", availableCountries.size(), csvFilePath);
            
        } catch (IOException | CsvValidationException e) {
            logger.error("Error reading country codes from CSV file: {}", e.getMessage());
        }
    }
    
    /**
     * Loads coordinates for a specific country.
     * 
     * @param countryCode The country code to load coordinates for
     * @return List of coordinate pairs [lat, lon]
     */
    private List<double[]> loadCoordinatesForCountry(String countryCode) {
        logger.debug("Loading coordinates for country: {}", countryCode);
        List<double[]> coordinates = Collections.synchronizedList(new ArrayList<>());
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvFilePath)) {
            if (inputStream == null) {
                logger.error("Could not find CSV file: {}", csvFilePath);
                return Collections.emptyList();
            }
            
            // Determine optimal thread count - don't use more threads than necessary
            int processors = Math.min(4, Runtime.getRuntime().availableProcessors());
            ExecutorService executor = Executors.newFixedThreadPool(processors);
            List<Future<List<double[]>>> futures = new ArrayList<>();
            
            try {
                // Create CSV parser with semicolon as separator
                try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                        .build()) {
                    
                    String[] header = reader.readNext(); // Skip header
                    
                    // Read and process the CSV file in batches
                    List<String[]> batch = new ArrayList<>(batchSize);
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        batch.add(line);
                        
                        if (batch.size() >= batchSize) {
                            submitBatch(batch, countryCode, executor, futures);
                        }
                    }
                    
                    // Process the final batch if it's not empty
                    if (!batch.isEmpty()) {
                        submitBatch(batch, countryCode, executor, futures);
                    }
                    
                    // Collect results from all threads
                    for (Future<List<double[]>> future : futures) {
                        try {
                            List<double[]> result = future.get();
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
            } finally {
                shutdownExecutor(executor);
            }
        } catch (IOException e) {
            logger.error("Error opening input stream for CSV file: {}", e.getMessage());
        }
        
        logger.info("Loaded {} coordinates for country code: {}", coordinates.size(), countryCode);
        return coordinates;
    }
    
    /**
     * Submits a batch of CSV lines for parallel processing.
     */
    private void submitBatch(List<String[]> batch, String countryCode, 
                            ExecutorService executor, List<Future<List<double[]>>> futures) {
        List<String[]> batchToProcess = new ArrayList<>(batch);
        batch.clear();
        
        futures.add(executor.submit(() -> processBatch(batchToProcess, countryCode)));
    }
    
    /**
     * Processes a batch of CSV lines to extract coordinates.
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
     */
    private boolean isValidCsvLine(String[] line, String countryCode) {
        return line.length >= 11 && countryCode.equalsIgnoreCase(line[0]);
    }
    
    /**
     * Extracts coordinates from a CSV line.
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
     */
    private boolean isValidCoordinate(double lat, double lon) {
        return lat >= MIN_LATITUDE && lat <= MAX_LATITUDE && 
               lon >= MIN_LONGITUDE && lon <= MAX_LONGITUDE;
    }
    
    /**
     * Safely shuts down the executor service.
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // Re-cancel if current thread also interrupted
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
