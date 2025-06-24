package com.art.ascii.service;

import com.art.ascii.cache.CoordinateCache;
import com.art.ascii.loader.CoordinateDataLoader;
import com.art.ascii.renderer.AsciiMapRenderer;
import com.art.ascii.renderer.CountryAsciiMapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating ASCII maps using coordinate data.
 * This service uses the CoordinateDataLoader to retrieve coordinate data
 * and the AsciiMapRenderer to render ASCII maps.
 */
@Service
public class AsciiMapService {
    private static final Logger logger = LoggerFactory.getLogger(AsciiMapService.class);

    private final CoordinateDataLoader coordinateDataLoader;
    private final Map<String, String> countryNames;
    private final int defaultWidth;
    private final int defaultHeight;
    
    /**
     * Creates a new AsciiMapService with the coordinate data loader and default settings.
     * 
     * @param coordinateCache Cache containing country coordinates and implementing CoordinateDataLoader
     * @param defaultWidth Default width for ASCII maps
     * @param defaultHeight Default height for ASCII maps
     */
    @Autowired
    public AsciiMapService(
            CoordinateCache coordinateCache,
            @Value("${ascii.map.default-width:80}") int defaultWidth,
            @Value("${ascii.map.default-height:25}") int defaultHeight) {
        
        // CoordinateCache now implements CoordinateDataLoader
        this.coordinateDataLoader = coordinateCache;
        
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
        Set<String> availableCodes = coordinateDataLoader.getAvailableCountryCodes();
        
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
        if (!coordinateDataLoader.hasCountry(normalizedCode)) {
            logger.warn("No data found for country code: {}", normalizedCode);
            return "No data found for country code: " + normalizedCode;
        }
        
        // Apply dimension limits
        int mapWidth = (width > 0) ? Math.min(width, 200) : defaultWidth;
        int mapHeight = (height > 0) ? Math.min(height, 100) : defaultHeight;
        
        logger.info("Rendering ASCII map for country: {}, dimensions: {}x{}", 
                normalizedCode, mapWidth, mapHeight);
        
        // Get coordinates from data loader
        List<double[]> coordinates = coordinateDataLoader.loadCoordinatesForCountry(normalizedCode);
        if (coordinates.isEmpty()) {
            return "No coordinate data available for " + normalizedCode;
        }
        
        // Create renderer with appropriate dimensions and render the map
        AsciiMapRenderer renderer = new CountryAsciiMapRenderer(mapWidth, mapHeight, coordinateDataLoader);
        return renderer.renderMap(coordinates);
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
