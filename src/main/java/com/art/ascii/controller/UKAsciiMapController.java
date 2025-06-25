package com.art.ascii.controller;

import com.art.ascii.UKAsciiMapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the UK ASCII Map functionality.
 * Uses UKAsciiMapRenderer for rendering UK ASCII maps.
 */
@RestController
public class UKAsciiMapController {

    private static final Logger logger = LoggerFactory.getLogger(UKAsciiMapController.class);

    @Autowired
    UKAsciiMapRenderer renderer;
    /**
     * Renders the UK ASCII map.
     * 
     * @return ResponseEntity with the rendered ASCII map
     */
    @GetMapping("/api/uk-map")
    public ResponseEntity<Map<String, Object>> renderUKMap() {
        logger.info("Rendering UK ASCII map");
        
        try {
            String asciiMap = renderer.renderMap();
            
            // Check if map contains actual data
            if (asciiMap == null || asciiMap.isEmpty() || !asciiMap.contains("*")) {
                logger.warn("No renderable data found for UK map");
                return createErrorResponse("No renderable data available for UK map");
            }
            
            // Build the response
            Map<String, Object> response = new HashMap<>();
            response.put("map", asciiMap);
            response.put("country", "GB");
            response.put("width", renderer.getWidth());
            response.put("height", renderer.getHeight());
            
            logger.debug("Successfully rendered UK ASCII map");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error rendering UK map", e);
            return createErrorResponse("Error rendering UK map: " + e.getMessage());
        }
    }
    
    /**
     * Creates an error response with the given message.
     * 
     * @param message The error message
     * @return ResponseEntity with bad request status and error message
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.badRequest().body(error);
    }
}
