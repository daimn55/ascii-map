package com.art.ascii.controller;

import com.art.ascii.service.AsciiMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for ASCII Map Generator application.
 * Provides REST API endpoints for rendering ASCII maps of countries
 * and serves the React Single Page Application (SPA).
 */
@Controller
public class CountryAsciiMapController {

    private static final Logger logger = LoggerFactory.getLogger(CountryAsciiMapController.class);
    
    // Constants for input validation
    private final int minWidth;
    private final int maxWidth;
    private final int minHeight;
    private final int maxHeight;
    private final int defaultWidth;
    private final int defaultHeight;
    
    // List of popular country codes to show at the top of the dropdown
    private static final List<String> POPULAR_COUNTRY_CODES = Arrays.asList(
            "US", "GB", "CA", "AU", "DE", "FR", "JP", "CN", "IN", "BR"
    );
    
    // Service for ASCII map operations
    private final AsciiMapService asciiMapService;
    
    /**
     * Creates a new AsciiMapController with dependencies.
     * 
     * @param asciiMapService Service for ASCII map operations
     * @param minWidth Minimum allowed width for ASCII maps
     * @param maxWidth Maximum allowed width for ASCII maps
     * @param minHeight Minimum allowed height for ASCII maps
     * @param maxHeight Maximum allowed height for ASCII maps
     * @param defaultWidth Default width for ASCII maps
     * @param defaultHeight Default height for ASCII maps
     */
    @Autowired
    public CountryAsciiMapController(
            AsciiMapService asciiMapService,
            @Value("${ascii.map.min-width:10}") int minWidth,
            @Value("${ascii.map.max-width:200}") int maxWidth,
            @Value("${ascii.map.min-height:10}") int minHeight,
            @Value("${ascii.map.max-height:100}") int maxHeight,
            @Value("${ascii.map.default-width:80}") int defaultWidth,
            @Value("${ascii.map.default-height:30}") int defaultHeight) {
        
        this.asciiMapService = asciiMapService;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        
        logger.info("AsciiMapController initialized with dimensions: {}x{} (default), {}x{} (min), {}x{} (max)",
                defaultWidth, defaultHeight, minWidth, minHeight, maxWidth, maxHeight);
    }
    
    /**
     * Serve the React SPA for the root path
     * 
     * @return String indicating to forward to index.html
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Get the list of countries with valid coordinates in the CSV
     * 
     * @return ResponseEntity with a map containing popular and all countries
     */
    @GetMapping("/api/countries")
    @ResponseBody
    public ResponseEntity<Map<String, Map<String, String>>> getCountries() {
        logger.debug("Retrieving country list");
        
        // Get available countries from the service
        Map<String, String> availableCountries = asciiMapService.getAvailableCountries();
        
        // Prepare response with popular and all countries
        Map<String, Map<String, String>> response = new HashMap<>(2);
        
        // Popular countries (that are available in the data)
        Map<String, String> popularCountries = new LinkedHashMap<>();
        for (String code : POPULAR_COUNTRY_CODES) {
            String name = availableCountries.get(code);
            if (name != null) {
                popularCountries.put(code, name);
            }
        }
        
        // Sort all countries by name
        Map<String, String> sortedCountries = availableCountries.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        
        response.put("popular", popularCountries);
        response.put("all", sortedCountries);
        
        logger.debug("Returning {} popular countries and {} total countries", 
                popularCountries.size(), sortedCountries.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Render an ASCII map for the given country code
     * 
     * @param countryCode The ISO country code to render
     * @param width The width of the ASCII map
     * @param height The height of the ASCII map
     * @return ResponseEntity with the rendered ASCII map or error message
     */
    @GetMapping("/api/render-map/{countryCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> renderMap(
            @PathVariable String countryCode,
            @RequestParam(defaultValue = "${ascii.map.default-width:80}") int width,
            @RequestParam(defaultValue = "${ascii.map.default-height:30}") int height) {
        
        // Validate input
        if (countryCode == null || countryCode.trim().isEmpty()) {
            logger.warn("Received empty country code");
            return createErrorResponse("Country code is required");
        }
        
        // Format country code (uppercase, trim)
        String formattedCountryCode = countryCode.trim().toUpperCase();
        
        logger.info("Rendering ASCII map for country: {}, width: {}, height: {}", 
                formattedCountryCode, width, height);
        
        // Validate dimensions
        if (!isValidDimension(width, minWidth, maxWidth) || 
            !isValidDimension(height, minHeight, maxHeight)) {
            
            logger.warn("Invalid dimensions provided: width={}, height={}", width, height);
            return createErrorResponse(
                    String.format("Invalid dimensions. Width must be %d-%d, height must be %d-%d.", 
                    minWidth, maxWidth, minHeight, maxHeight));
        }
        
        // Render the ASCII map using the service
        try {
            String asciiMap = asciiMapService.renderCountryMap(formattedCountryCode, width, height);
            
            // Check if map is empty or has an error message
            if (asciiMap == null || asciiMap.trim().isEmpty() || !asciiMap.contains("*")) {
                if (asciiMap != null && asciiMap.startsWith("No data") || asciiMap.startsWith("Invalid")) {
                    logger.warn("No data found: {}", asciiMap);
                    return ResponseEntity.ok(Collections.singletonMap("error", asciiMap));
                }
                
                logger.warn("No renderable data found for country: {}", formattedCountryCode);
                return ResponseEntity.ok(Collections.singletonMap(
                        "error", "No data found for country code: " + formattedCountryCode));
            }
            
            // Get country name
            Map<String, String> countries = asciiMapService.getAvailableCountries();
            String countryName = countries.getOrDefault(formattedCountryCode, formattedCountryCode);
            
            // Build response
            Map<String, Object> response = new HashMap<>(5);
            response.put("countryCode", formattedCountryCode);
            response.put("countryName", countryName);
            response.put("width", width);
            response.put("height", height);
            response.put("map", asciiMap);
            
            logger.debug("Successfully rendered ASCII map for {}", formattedCountryCode);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error rendering map for {}: {}", formattedCountryCode, e.getMessage(), e);
            return createErrorResponse("Error rendering map: " + e.getMessage());
        }
    }
    
    /**
     * Validates if a dimension value is within the acceptable range
     * 
     * @param value The dimension value to check
     * @param min Minimum acceptable value
     * @param max Maximum acceptable value
     * @return true if valid, false otherwise
     */
    private boolean isValidDimension(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /**
     * Creates an error response with the given message
     * 
     * @param message The error message
     * @return ResponseEntity with bad request status and error message
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        return ResponseEntity.badRequest().body(Collections.singletonMap("error", message));
    }
}
