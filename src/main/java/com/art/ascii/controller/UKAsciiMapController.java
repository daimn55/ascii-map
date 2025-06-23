package com.art.ascii.controller;

import com.art.ascii.UKAsciiMapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the UK ASCII Map functionality.
 * Uses SimpleAsciiMapRenderer for rendering UK ASCII maps.
 */
@RestController
public class UKAsciiMapController {

    private static final Logger logger = LoggerFactory.getLogger(UKAsciiMapController.class);
    
    /**
     * Creates a new controller.
     */
    public UKAsciiMapController() {
        logger.info("UKAsciiMapController initialized");
    }
    
    /**
     * Renders the UK ASCII map.
     * 
     * @return ResponseEntity with the rendered ASCII map
     */
    @GetMapping("/api/uk-map")
    public ResponseEntity<Map<String, Object>> renderUKMap() {
        logger.info("Rendering UK ASCII map");
        
        try {
            // Create a new renderer instance and get the map as a string
            UKAsciiMapRenderer renderer = new UKAsciiMapRenderer();
            String asciiMap = renderer.renderMap();
            
            // Check if map contains actual data
            if (asciiMap == null || asciiMap.isEmpty() || !asciiMap.contains("*")) {
                logger.warn("No renderable data found for UK map");
                return ResponseEntity.ok(Collections.singletonMap(
                        "error", "No renderable data available for UK map"));
            }
            
            // Build response
            Map<String, Object> response = new HashMap<>(3);
            response.put("countryName", "United Kingdom");
            response.put("map", asciiMap);
            
            logger.debug("Successfully rendered UK ASCII map");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error rendering UK map: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    Collections.singletonMap("error", "Error rendering map: " + e.getMessage()));
        }
    }
}
