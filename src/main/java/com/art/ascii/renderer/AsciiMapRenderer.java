package com.art.ascii.renderer;

import java.util.List;

/**
 * Interface for rendering ASCII maps from coordinate data.
 * Defines methods for rendering ASCII maps with various configurations.
 */
public interface AsciiMapRenderer {
    
    /**
     * Renders an ASCII map from a list of coordinates.
     * 
     * @param coordinates List of coordinate pairs [lat, lon] to render
     * @return String representation of the ASCII map
     */
    String renderMap(List<double[]> coordinates);
    
    /**
     * Gets the current width setting for the renderer.
     * 
     * @return The width in characters
     */
    int getWidth();
    
    /**
     * Gets the current height setting for the renderer.
     * 
     * @return The height in characters
     */
    int getHeight();
    
    /**
     * Creates a new renderer with the specified dimensions.
     * 
     * @param width The width in characters
     * @param height The height in characters
     * @return A new renderer with the updated dimensions
     */
    AsciiMapRenderer withDimensions(int width, int height);

}
