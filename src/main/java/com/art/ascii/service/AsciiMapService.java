package com.art.ascii.service;

import com.art.ascii.cache.CoordinateCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating ASCII maps using cached coordinate data.
 * This service uses the CoordinateCache to avoid repeated CSV parsing.
 */
@Service
public class AsciiMapService {
    private static final Logger logger = LoggerFactory.getLogger(AsciiMapService.class);

    private final CoordinateCache coordinateCache;
    private final Map<String, String> countryNames;
    private final int defaultWidth;
    private final int defaultHeight;
    
    /**
     * Creates a new AsciiMapService with the coordinate cache.
     * 
     * @param coordinateCache Cache containing country coordinates
     * @param defaultWidth Default width for ASCII maps
     * @param defaultHeight Default height for ASCII maps
     */
    @Autowired
    public AsciiMapService(
            CoordinateCache coordinateCache,
            @Value("${ascii.map.default-width:80}") int defaultWidth,
            @Value("${ascii.map.default-height:25}") int defaultHeight) {
        
        this.coordinateCache = coordinateCache;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        
        // Initialize country name mapping
        this.countryNames = initializeCountryNames();
        logger.info("AsciiMapService initialized with {} country names", countryNames.size());
    }
    
    /**
     * Get list of available countries with names and codes.
     * 
     * @return Map of country code to country name for all available countries
     */
    public Map<String, String> getAvailableCountries() {
        Set<String> availableCodes = coordinateCache.getAvailableCountries();
        
        // Filter the full country list to only include countries with data
        Map<String, String> availableCountries = new HashMap<>();
        for (String code : availableCodes) {
            String name = countryNames.getOrDefault(code, code);
            availableCountries.put(code, name);
        }
        
        return availableCountries;
    }
    
    /**
     * Render ASCII map for a specific country.
     * 
     * @param countryCode Country code to render
     * @param width Width of the map (characters)
     * @param height Height of the map (characters)
     * @return ASCII representation of the country
     */
    public String renderCountryMap(String countryCode, int width, int height) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            logger.warn("Invalid country code provided: null or empty");
            return "Invalid country code";
        }
        
        String normalizedCode = countryCode.trim().toUpperCase();
        if (!coordinateCache.hasCountry(normalizedCode)) {
            logger.warn("No data found for country code: {}", normalizedCode);
            return "No data found for country code: " + normalizedCode;
        }
        
        // Apply dimension limits
        int mapWidth = (width > 0) ? Math.min(width, 200) : defaultWidth;
        int mapHeight = (height > 0) ? Math.min(height, 100) : defaultHeight;
        
        logger.info("Rendering ASCII map for country: {}, dimensions: {}x{}", 
                normalizedCode, mapWidth, mapHeight);
        
        // Get coordinates from cache
        List<double[]> coordinates = coordinateCache.getCoordinatesForCountry(normalizedCode);
        if (coordinates.isEmpty()) {
            return "No coordinate data available for " + normalizedCode;
        }
        
        return renderAsciiMap(coordinates, mapWidth, mapHeight);
    }
    
    /**
     * Render ASCII map from coordinates.
     * 
     * @param coordinates List of coordinate pairs [lat, lon]
     * @param width Map width
     * @param height Map height
     * @return ASCII representation of the coordinates
     */
    private String renderAsciiMap(List<double[]> coordinates, int width, int height) {
        // Calculate boundaries
        MapBoundary boundary = calculateBoundaries(coordinates);
        
        // Create and populate the grid
        int[][] densityGrid = new int[height][width];
        
        for (double[] coord : coordinates) {
            double lat = coord[0];
            double lon = coord[1];
            
            // Map the coordinate to a grid cell
            int x = (int) ((lon - boundary.minLon) / (boundary.maxLon - boundary.minLon) * (width - 1));
            // Note: We invert the y-axis to match the typical map orientation
            int y = height - 1 - (int) ((lat - boundary.minLat) / (boundary.maxLat - boundary.minLat) * (height - 1));
            
            // Ensure we stay within bounds (due to floating point precision issues)
            if (x >= 0 && x < width && y >= 0 && y < height) {
                // Create a "blob" effect by incrementing a small area around the point
                int radius = 1; // Density radius
                for (int cy = Math.max(0, y - radius); cy <= Math.min(height - 1, y + radius); cy++) {
                    for (int cx = Math.max(0, x - radius); cx <= Math.min(width - 1, x + radius); cx++) {
                        // Points closer to center get higher density values
                        int distance = Math.abs(cx - x) + Math.abs(cy - y);
                        int densityIncrement = radius + 1 - distance;
                        if (densityIncrement > 0) {
                            densityGrid[cy][cx] += densityIncrement;
                        }
                    }
                }
            }
        }
        
        // Convert density grid to ASCII
        StringBuilder sb = new StringBuilder((height + 1) * width);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (densityGrid[y][x] > 0) {
                    sb.append('*');
                } else {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        
        return sb.toString();
    }
    
    /**
     * Calculate map boundaries from coordinates.
     * 
     * @param coordinates List of coordinate pairs [lat, lon]
     * @return MapBoundary object with min/max values
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
        
        return new MapBoundary(
                minLat - latBuffer,
                maxLat + latBuffer,
                minLon - lonBuffer,
                maxLon + lonBuffer
        );
    }
    
    /**
     * Initialize mapping of country codes to names.
     * 
     * @return Map of country code to country name
     */
    private Map<String, String> initializeCountryNames() {
        Map<String, String> countries = new HashMap<>();
        
        // ISO 3166-1 alpha-2 country codes and names
        countries.put("AD", "Andorra");
        countries.put("AE", "United Arab Emirates");
        countries.put("AF", "Afghanistan");
        countries.put("AG", "Antigua and Barbuda");
        countries.put("AI", "Anguilla");
        countries.put("AL", "Albania");
        countries.put("AM", "Armenia");
        countries.put("AO", "Angola");
        countries.put("AQ", "Antarctica");
        countries.put("AR", "Argentina");
        countries.put("AS", "American Samoa");
        countries.put("AT", "Austria");
        countries.put("AU", "Australia");
        countries.put("AW", "Aruba");
        countries.put("AX", "Åland Islands");
        countries.put("AZ", "Azerbaijan");
        countries.put("BA", "Bosnia and Herzegovina");
        countries.put("BB", "Barbados");
        countries.put("BD", "Bangladesh");
        countries.put("BE", "Belgium");
        countries.put("BF", "Burkina Faso");
        countries.put("BG", "Bulgaria");
        countries.put("BH", "Bahrain");
        countries.put("BI", "Burundi");
        countries.put("BJ", "Benin");
        countries.put("BL", "Saint Barthélemy");
        countries.put("BM", "Bermuda");
        countries.put("BN", "Brunei Darussalam");
        countries.put("BO", "Bolivia");
        countries.put("BQ", "Bonaire, Sint Eustatius and Saba");
        countries.put("BR", "Brazil");
        countries.put("BS", "Bahamas");
        countries.put("BT", "Bhutan");
        countries.put("BV", "Bouvet Island");
        countries.put("BW", "Botswana");
        countries.put("BY", "Belarus");
        countries.put("BZ", "Belize");
        countries.put("CA", "Canada");
        countries.put("CC", "Cocos (Keeling) Islands");
        countries.put("CD", "Congo, Democratic Republic of");
        countries.put("CF", "Central African Republic");
        countries.put("CG", "Congo");
        countries.put("CH", "Switzerland");
        countries.put("CI", "Côte d'Ivoire");
        countries.put("CK", "Cook Islands");
        countries.put("CL", "Chile");
        countries.put("CM", "Cameroon");
        countries.put("CN", "China");
        countries.put("CO", "Colombia");
        countries.put("CR", "Costa Rica");
        countries.put("CU", "Cuba");
        countries.put("CV", "Cabo Verde");
        countries.put("CW", "Curaçao");
        countries.put("CX", "Christmas Island");
        countries.put("CY", "Cyprus");
        countries.put("CZ", "Czechia");
        countries.put("DE", "Germany");
        countries.put("DJ", "Djibouti");
        countries.put("DK", "Denmark");
        countries.put("DM", "Dominica");
        countries.put("DO", "Dominican Republic");
        countries.put("DZ", "Algeria");
        countries.put("EC", "Ecuador");
        countries.put("EE", "Estonia");
        countries.put("EG", "Egypt");
        countries.put("EH", "Western Sahara");
        countries.put("ER", "Eritrea");
        countries.put("ES", "Spain");
        countries.put("ET", "Ethiopia");
        countries.put("FI", "Finland");
        countries.put("FJ", "Fiji");
        countries.put("FK", "Falkland Islands");
        countries.put("FM", "Micronesia");
        countries.put("FO", "Faroe Islands");
        countries.put("FR", "France");
        countries.put("GA", "Gabon");
        countries.put("GB", "United Kingdom");
        countries.put("GD", "Grenada");
        countries.put("GE", "Georgia");
        countries.put("GF", "French Guiana");
        countries.put("GG", "Guernsey");
        countries.put("GH", "Ghana");
        countries.put("GI", "Gibraltar");
        countries.put("GL", "Greenland");
        countries.put("GM", "Gambia");
        countries.put("GN", "Guinea");
        countries.put("GP", "Guadeloupe");
        countries.put("GQ", "Equatorial Guinea");
        countries.put("GR", "Greece");
        countries.put("GS", "South Georgia and the South Sandwich Islands");
        countries.put("GT", "Guatemala");
        countries.put("GU", "Guam");
        countries.put("GW", "Guinea-Bissau");
        countries.put("GY", "Guyana");
        countries.put("HK", "Hong Kong");
        countries.put("HM", "Heard Island and McDonald Islands");
        countries.put("HN", "Honduras");
        countries.put("HR", "Croatia");
        countries.put("HT", "Haiti");
        countries.put("HU", "Hungary");
        countries.put("ID", "Indonesia");
        countries.put("IE", "Ireland");
        countries.put("IL", "Israel");
        countries.put("IM", "Isle of Man");
        countries.put("IN", "India");
        countries.put("IO", "British Indian Ocean Territory");
        countries.put("IQ", "Iraq");
        countries.put("IR", "Iran");
        countries.put("IS", "Iceland");
        countries.put("IT", "Italy");
        countries.put("JE", "Jersey");
        countries.put("JM", "Jamaica");
        countries.put("JO", "Jordan");
        countries.put("JP", "Japan");
        countries.put("KE", "Kenya");
        countries.put("KG", "Kyrgyzstan");
        countries.put("KH", "Cambodia");
        countries.put("KI", "Kiribati");
        countries.put("KM", "Comoros");
        countries.put("KN", "Saint Kitts and Nevis");
        countries.put("KP", "North Korea");
        countries.put("KR", "South Korea");
        countries.put("KW", "Kuwait");
        countries.put("KY", "Cayman Islands");
        countries.put("KZ", "Kazakhstan");
        countries.put("LA", "Lao People's Democratic Republic");
        countries.put("LB", "Lebanon");
        countries.put("LC", "Saint Lucia");
        countries.put("LI", "Liechtenstein");
        countries.put("LK", "Sri Lanka");
        countries.put("LR", "Liberia");
        countries.put("LS", "Lesotho");
        countries.put("LT", "Lithuania");
        countries.put("LU", "Luxembourg");
        countries.put("LV", "Latvia");
        countries.put("LY", "Libya");
        countries.put("MA", "Morocco");
        countries.put("MC", "Monaco");
        countries.put("MD", "Moldova");
        countries.put("ME", "Montenegro");
        countries.put("MF", "Saint Martin (French part)");
        countries.put("MG", "Madagascar");
        countries.put("MH", "Marshall Islands");
        countries.put("MK", "North Macedonia");
        countries.put("ML", "Mali");
        countries.put("MM", "Myanmar");
        countries.put("MN", "Mongolia");
        countries.put("MO", "Macao");
        countries.put("MP", "Northern Mariana Islands");
        countries.put("MQ", "Martinique");
        countries.put("MR", "Mauritania");
        countries.put("MS", "Montserrat");
        countries.put("MT", "Malta");
        countries.put("MU", "Mauritius");
        countries.put("MV", "Maldives");
        countries.put("MW", "Malawi");
        countries.put("MX", "Mexico");
        countries.put("MY", "Malaysia");
        countries.put("MZ", "Mozambique");
        countries.put("NA", "Namibia");
        countries.put("NC", "New Caledonia");
        countries.put("NE", "Niger");
        countries.put("NF", "Norfolk Island");
        countries.put("NG", "Nigeria");
        countries.put("NI", "Nicaragua");
        countries.put("NL", "Netherlands");
        countries.put("NO", "Norway");
        countries.put("NP", "Nepal");
        countries.put("NR", "Nauru");
        countries.put("NU", "Niue");
        countries.put("NZ", "New Zealand");
        countries.put("OM", "Oman");
        countries.put("PA", "Panama");
        countries.put("PE", "Peru");
        countries.put("PF", "French Polynesia");
        countries.put("PG", "Papua New Guinea");
        countries.put("PH", "Philippines");
        countries.put("PK", "Pakistan");
        countries.put("PL", "Poland");
        countries.put("PM", "Saint Pierre and Miquelon");
        countries.put("PN", "Pitcairn");
        countries.put("PR", "Puerto Rico");
        countries.put("PS", "Palestine");
        countries.put("PT", "Portugal");
        countries.put("PW", "Palau");
        countries.put("PY", "Paraguay");
        countries.put("QA", "Qatar");
        countries.put("RE", "Réunion");
        countries.put("RO", "Romania");
        countries.put("RS", "Serbia");
        countries.put("RU", "Russia");
        countries.put("RW", "Rwanda");
        countries.put("SA", "Saudi Arabia");
        countries.put("SB", "Solomon Islands");
        countries.put("SC", "Seychelles");
        countries.put("SD", "Sudan");
        countries.put("SE", "Sweden");
        countries.put("SG", "Singapore");
        countries.put("SH", "Saint Helena, Ascension and Tristan da Cunha");
        countries.put("SI", "Slovenia");
        countries.put("SJ", "Svalbard and Jan Mayen");
        countries.put("SK", "Slovakia");
        countries.put("SL", "Sierra Leone");
        countries.put("SM", "San Marino");
        countries.put("SN", "Senegal");
        countries.put("SO", "Somalia");
        countries.put("SR", "Suriname");
        countries.put("SS", "South Sudan");
        countries.put("ST", "Sao Tome and Principe");
        countries.put("SV", "El Salvador");
        countries.put("SX", "Sint Maarten (Dutch part)");
        countries.put("SY", "Syrian Arab Republic");
        countries.put("SZ", "Eswatini");
        countries.put("TC", "Turks and Caicos Islands");
        countries.put("TD", "Chad");
        countries.put("TF", "French Southern Territories");
        countries.put("TG", "Togo");
        countries.put("TH", "Thailand");
        countries.put("TJ", "Tajikistan");
        countries.put("TK", "Tokelau");
        countries.put("TL", "Timor-Leste");
        countries.put("TM", "Turkmenistan");
        countries.put("TN", "Tunisia");
        countries.put("TO", "Tonga");
        countries.put("TR", "Turkey");
        countries.put("TT", "Trinidad and Tobago");
        countries.put("TV", "Tuvalu");
        countries.put("TW", "Taiwan");
        countries.put("TZ", "Tanzania");
        countries.put("UA", "Ukraine");
        countries.put("UG", "Uganda");
        countries.put("UM", "United States Minor Outlying Islands");
        countries.put("US", "United States");
        countries.put("UY", "Uruguay");
        countries.put("UZ", "Uzbekistan");
        countries.put("VA", "Holy See");
        countries.put("VC", "Saint Vincent and the Grenadines");
        countries.put("VE", "Venezuela");
        countries.put("VG", "Virgin Islands (British)");
        countries.put("VI", "Virgin Islands (U.S.)");
        countries.put("VN", "Viet Nam");
        countries.put("VU", "Vanuatu");
        countries.put("WF", "Wallis and Futuna");
        countries.put("WS", "Samoa");
        countries.put("YE", "Yemen");
        countries.put("YT", "Mayotte");
        countries.put("ZA", "South Africa");
        countries.put("ZM", "Zambia");
        countries.put("ZW", "Zimbabwe");
        
        // Return unmodifiable version to prevent accidental modification
        return Collections.unmodifiableMap(countries);
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
}
