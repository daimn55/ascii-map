# ASCII Map Generator

A Spring Boot application that renders ASCII art maps from geographic coordinate data. The application provides two distinct rendering features:

1. `UKAsciiMapRenderer` - A console-based UK map renderer
2. `CountryAsciiMapRenderer` - A web-based application with React UI for rendering maps of any country

## Table of Contents

- [UK ASCII Map Renderer](#uk-ascii-map-renderer)
- [Country ASCII Map Renderer](#country-ascii-map-renderer)
- [Technical Details](#technical-details)
- [Sample Outputs](#sample-outputs)

## UK ASCII Map Renderer

### Core Functionality

The `UKAsciiMapRenderer` reads UK latitude/longitude coordinates from `ukpostcodes.csv` and renders them as an ASCII representation of the UK map in the console. This feature:

- Parses CSV data containing UK postal codes with their geographic coordinates
- Maps these coordinates to a 2D grid, scaling them to fit the desired console dimensions
- Renders the map using ASCII characters (e.g., '*') to represent coordinates

### Quick Start Guide for UKAsciiMapRenderer

Follow these steps to run the UKAsciiMapRenderer locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/daimn55/ascii-map.git
   cd ascii-map
   ```

2. Unzip the UK data file into the resources directory:
   ```bash
   unzip src/main/resources/ukpostcodes.csv.zip -d src/main/resources/
   ```

3. Build the application:
   ```bash
   mvn clean install
   ```

4. Run the UKAsciiMapRenderer directly using the `main` method

**Requirements:**
- Java 17 or higher
- Maven 3.6 or higher

### Running via REST API

You can also access the UK ASCII Map through a REST API by running the Spring Boot application:

1. Start the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

2. Access the UK map via:
   ```bash
   # Using curl
   curl http://localhost:8080/api/uk-map

   # Or open in browser
   http://localhost:8080/api/uk-map
   ```

### Key Assumptions

- The CSV file (`ukpostcodes.csv`) contains valid UK coordinate data and enough coordinates to render a meaningful map.
- CSV format: Contains at least latitude and longitude columns
- Coordinates are within UK bounds (approximately latitude 49°N-61°N, longitude 8°W-2°E)
- Console has sufficient width and height to display the map effectively

## Country ASCII Map Renderer

### Core Functionality

The `CountryAsciiMapRenderer` is a web application that allows users to generate ASCII maps of any country using data from `geonames-postal-code.csv`. The feature:

- Presents a user-friendly web interface built with React
- Allows users to select countries from a dropdown or enter custom country codes
- Renders map data via an API endpoint and displays it on the webpage
- Provides options to customize map dimensions
- Allows maps to be downloaded as text files

### Quick Start Guide for CountryAsciiMapRenderer

Follow these steps to run the CountryAsciiMapRenderer locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/daimn55/ascii-map.git
   cd ascii-map
   ```

2. Unzip the global data file into the resources directory:
   ```bash
   unzip src/main/resources/geonames-postal-code.csv.zip -d src/main/resources/
   ```

3. Build and run the Spring Boot application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. Access the web UI:
   ```
   http://localhost:8080
   ```

5. API Access (for direct calls):
   ```bash
   curl http://localhost:8080/api/render-map/US?width=120&height=60
   ```

**Requirements:**
- Java 17 or higher
- Maven 3.6 or higher
- Spring Boot 3.0 or higher

### Online Demo

The application is hosted on AWS EC2 and can be accessed at:
```
http://ec2-13-235-80-164.ap-south-1.compute.amazonaws.com:8443
```

### Key Assumptions

- The CSV file (`geonames-postal-code.csv`) contains valid global coordinate data
- CSV format: First column contains country code, with coordinates in subsequent columns
- Coordinate data is available for the requested countries to render meaningful maps
- Reasonable map dimensions (width 10-200, height 10-100)


### Performance Considerations

- **Coordinate Caching**: Pre-loads country codes and lazily loads coordinates(can be controlled via configuration)
- **Batch Processing**: Processes large CSV files in batches
- **Concurrent Processing**: Uses CompletableFuture for parallel data processing
- **Data Filtering**: Only processes relevant country data

## Technical Details

### Application Architecture

```
ascii-map/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/art/ascii/
│   │   │       ├── AsciiMapApplication.java        # Main Spring Boot application
│   │   │       ├── UKAsciiMapRenderer.java         # UK map renderer implementation
│   │   │       ├── CountryAsciiMapRenderer.java    # Country map renderer implementation (standalone)
│   │   │       ├── cache/
│   │   │       │   └── CoordinateCache.java        # Caching for coordinates & data loading
│   │   │       ├── controller/
│   │   │       │   ├── UKAsciiMapController.java   # UK map API endpoint
│   │   │       │   └── CountryAsciiMapController.java # Country map API endpoint
│   │   │       ├── loader/
│   │   │       │   └── CoordinateDataLoader.java   # Interface for loading coordinate data
│   │   │       ├── model/
│   │   │       │   └── MapBoundary.java            # Model for map geographic boundaries
│   │   │       ├── renderer/
│   │   │       │   ├── AsciiMapRenderer.java       # Interface for rendering ASCII maps
│   │   │       │   └── CountryAsciiMapRenderer.java # Map renderer implementation
│   │   │       └── service/
│   │   │           └── AsciiMapService.java        # Service layer for ASCII map operations
│   │   ├── resources/
│   │   │   ├── application.properties              # Application configuration
│   │   │   ├── ukpostcodes.csv.zip                 # UK coordinate data (zipped)
│   │   │   ├── geonames-postal-code.csv.zip        # Global coordinate data (zipped)
│   │   │   └── static/                             # Web application static files
│   │   │       ├── index.html                      # Web application entry point
│   │   │       ├── app.js                          # React application
│   │   │       └── styles.css                      # Application styles
│   └── test/                                       # Unit and integration tests
├── images/                                         # Screenshots and example outputs
├── pom.xml                                         # Maven configuration
```
## Sample Outputs

### UKAsciiMapRenderer Console Output

When running the UKAsciiMapRenderer directly from its main method, you'll see an ASCII map of the UK in your console:

![UK Map Generator Console Output](./images/UK_map_generator_console_output.png)

The console output shows the geographical shape of the United Kingdom using asterisks (*) to represent postal code coordinates. This representation clearly outlines the distinctive shape of Great Britain and Northern Ireland.

### CountryAsciiMapRenderer Web Interface

The web-based CountryAsciiMapRenderer provides an interactive interface for generating ASCII maps of any country:

![Country Map Generator Web Interface](./images/Country_map_generator.png)
