package com.art.ascii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Spring Boot application class for the ASCII Map Generator web application.
 * This application allows users to generate ASCII maps of different countries
 * based on postal code coordinates.
 * 
 * Features:
 * - RESTful API endpoints for retrieving country data and generating ASCII maps
 * - Embedded React frontend for user interaction
 * - Dynamic boundary calculation for optimal map rendering
 * - Support for multiple countries based on available coordinate data
 */
@SpringBootApplication
public class AsciiMapApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(AsciiMapApplication.class);
    
    /**
     * Main entry point for the application.
     * Starts the Spring Boot application with the specified command line arguments.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AsciiMapApplication.class);
        Environment env = app.run(args).getEnvironment();
        
        String protocol = "http";
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        if (contextPath.isEmpty()) {
            contextPath = "/";
        }
        
        String activeProfiles = String.join(", ", env.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = "default";
        }
        
        logger.info("""
                
                ----------------------------------------------------------
                Application '{}' is running!
                Profile(s): {}
                Access URLs:
                Local:      {}://localhost:{}{}
                External:   {}://localhost:{}{}
                ----------------------------------------------------------
                """, 
                env.getProperty("spring.application.name", "ASCII Map Generator"),
                activeProfiles,
                protocol, serverPort, contextPath,
                protocol, serverPort, contextPath);
    }
}
