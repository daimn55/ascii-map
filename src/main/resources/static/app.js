// Main App component
const App = () => {
    // State for storing popular and all countries
    const [countries, setCountries] = React.useState({
        popular: {},
        all: {}
    });
    // State for loading indicator
    const [loading, setLoading] = React.useState(false);
    // State to hold the generated map data
    const [mapData, setMapData] = React.useState(null);
    // State for error messages
    const [error, setError] = React.useState(null);
    // State for form inputs (selected country, width, height, custom code)
    const [formData, setFormData] = React.useState({
        countryCode: '',
        width: 120,
        height: 60,
        customCountryCode: ''
    });
    // Toggle for using custom country code input
    const [useCustomCode, setUseCustomCode] = React.useState(false);
    // Toggle for console theme (dark/light)
    const [consoleTheme, setConsoleTheme] = React.useState(true);

    // Fetch countries from backend when component mounts
    React.useEffect(() => {
        // Call the fetchCountries function to load country data
        fetchCountries();
    }, []);

    // Fetch country list from API
    const fetchCountries = async () => {
        try {
            // Make a GET request to the /api/countries endpoint
            const response = await fetch('/api/countries'); 
            // Parse the response data as JSON
            const data = await response.json(); 
            // Update the countries state with the fetched data
            setCountries(data || { popular: {}, all: {} }); 
        } catch (err) {
            // Log any errors that occur during the fetch
            console.error('Error fetching countries:', err);
            // Update the error state with a user-friendly message
            setError('Failed to load country data');
        }
    };

    // Handle changes in form inputs
    const handleInputChange = (e) => {
        // Get the name and value of the changed input
        const { name, value } = e.target;
        // Update the formData state with the new value
        setFormData({ ...formData, [name]: value });
    };

    // Toggle between dropdown and custom code input
    const handleCustomCodeToggle = () => {
        // Toggle the useCustomCode state
        setUseCustomCode(!useCustomCode);
    };

    // Handle form submission to generate ASCII map
    const handleSubmit = async (e) => {
        // Prevent the default form submission behavior
        e.preventDefault();
        // Set the loading state to true
        setLoading(true);
        // Reset the mapData and error states
        setMapData(null);
        setError(null);
        
        // Choose country code from dropdown or custom input
        const countryCode = useCustomCode ? formData.customCountryCode : formData.countryCode;
        
        // Check if a country code is selected or entered
        if (!countryCode) {
            // Update the error state with a user-friendly message
            setError('Please select a country or enter a country code');
            // Set the loading state to false
            setLoading(false);
            // Return early to prevent further execution
            return;
        }
        
        try {
            // Make a GET request to the /api/render-map endpoint with the selected country code and dimensions
            const response = await fetch(`/api/render-map/${countryCode}?width=${formData.width}&height=${formData.height}`);
            // Parse the response data as JSON
            const data = await response.json();
            
            // Check if the response contains an error
            if (data.error) {
                // Update the error state with the error message from the backend
                setError(data.error); 
            } else {
                // Update the mapData state with the generated map
                setMapData(data); 
            }
        } catch (err) {
            // Log any errors that occur during the fetch
            console.error('Error generating map:', err);
            // Update the error state with a user-friendly message
            setError('An error occurred while generating the map');
        } finally {
            // Set the loading state to false
            setLoading(false);
        }
    };

    // Download the generated ASCII map as a text file
    const downloadMap = () => {
        // Check if mapData is available
        if (!mapData) return;
        
        // Create a new anchor element
        const element = document.createElement('a'); 
        // Create a text blob with the map data
        const file = new Blob([mapData.map], { type: 'text/plain' }); 
        // Set the href attribute of the anchor element to the blob URL
        element.href = URL.createObjectURL(file); 
        // Set the download attribute of the anchor element to the desired file name
        element.download = `ascii-map-${mapData.countryCode.toLowerCase()}.txt`;
        // Add the anchor element to the DOM
        document.body.appendChild(element); 
        // Simulate a click on the anchor element to trigger the download
        element.click(); 
        // Remove the anchor element from the DOM
        document.body.removeChild(element); 
    };

    // Toggle console (theme) mode
    const toggleConsoleTheme = () => {
        // Toggle the consoleTheme state
        setConsoleTheme(!consoleTheme);
    };

    // Render <option> groups for popular and all countries
    const renderCountryOptions = () => {
        // Return a fragment containing the option groups
        return (
            <>
                {/* Check if popular countries are available */}
                {Object.keys(countries.popular).length > 0 && (
                    <optgroup label="Popular Countries">
                        {/* Map over the popular countries and render an option for each */}
                        {Object.entries(countries.popular).map(([code, name]) => (
                            <option key={code} value={code}>{name} ({code})</option>
                        ))}
                    </optgroup>
                )}
                {/* Check if all countries are available */}
                {Object.keys(countries.all).length > 0 && (
                    <optgroup label="All Countries">
                        {/* Map over the all countries and render an option for each */}
                        {Object.entries(countries.all).map(([code, name]) => (
                            <option key={code} value={code}>{name} ({code})</option>
                        ))}
                    </optgroup>
                )}
            </>
        );
    };

    // Main render
    return (
        <div className="container">
            <div className="row">
                <div className="col-12 text-center">
                    <h1 className="app-title">ASCII Map Generator</h1>
                    <p className="lead">Generate ASCII art maps of countries using postal code data</p>
                </div>
            </div>

            <div className="form-container">
                <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                        <label className="form-label">Select Country:</label>
                        <div className="form-check mb-2">
                            <input 
                                type="checkbox" 
                                className="form-check-input" 
                                id="useCustomCode" 
                                checked={useCustomCode} 
                                onChange={handleCustomCodeToggle} 
                            />
                            <label className="form-check-label" htmlFor="useCustomCode">
                                Use custom country code
                            </label>
                        </div>
                        
                        {/* Show dropdown or custom input based on toggle */}
                        {!useCustomCode ? (
                            <select 
                                className="form-select" 
                                name="countryCode" 
                                value={formData.countryCode} 
                                onChange={handleInputChange}
                                disabled={useCustomCode}
                            >
                                <option value="">-- Select a country --</option>
                                {renderCountryOptions()}
                            </select>
                        ) : (
                            <input 
                                type="text" 
                                className="form-control" 
                                placeholder="Enter a 2-letter country code (e.g., US, CA, JP)" 
                                name="customCountryCode" 
                                value={formData.customCountryCode} 
                                onChange={handleInputChange} 
                                maxLength="2"
                            />
                        )}
                    </div>
                    
                    <div className="row">
                        {/* Width input */}
                        <div className="col-md-6 mb-3">
                            <label htmlFor="width" className="form-label">Width:</label>
                            <input 
                                type="number" 
                                className="form-control" 
                                id="width" 
                                name="width" 
                                min="10" 
                                max="200" 
                                value={formData.width} 
                                onChange={handleInputChange} 
                            />
                        </div>
                        {/* Height input */}
                        <div className="col-md-6 mb-3">
                            <label htmlFor="height" className="form-label">Height:</label>
                            <input 
                                type="number" 
                                className="form-control" 
                                id="height" 
                                name="height" 
                                min="10" 
                                max="100" 
                                value={formData.height} 
                                onChange={handleInputChange} 
                            />
                        </div>
                    </div>

                    {/* Theme toggle */}
                    <div className="mb-3 form-check form-switch">
                        <input 
                            className="form-check-input" 
                            type="checkbox" 
                            id="consoleTheme" 
                            checked={consoleTheme}
                            onChange={toggleConsoleTheme}
                        />
                        <label className="form-check-label" htmlFor="consoleTheme">
                            Console theme
                        </label>
                    </div>

                    <div className="d-grid">
                        <button type="submit" className="btn btn-primary" disabled={loading}>
                            {loading ? 'Generating...' : 'Generate Map'}
                        </button>
                    </div>
                </form>
            </div>

            {/* Show error if exists */}
            {error && (
                <div className="alert alert-danger mt-3" role="alert">
                    {error}
                </div>
            )}

            {/* Show generated map and download option */}
            {mapData && (
                <div className={`map-container mt-4 ${consoleTheme ? 'console-theme' : ''}`}>
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <h3>
                            {mapData.countryName} ({mapData.countryCode})
                        </h3>
                        <div>
                            {/* Re-generate button */}
                            <button 
                                className="btn btn-success me-2" 
                                onClick={() => handleSubmit(new Event('click'))}
                            >
                                Generate Another
                            </button>
                            {/* Download button */}
                            <button 
                                className="btn btn-primary" 
                                onClick={downloadMap}
                            >
                                Download Map
                            </button>
                        </div>
                    </div>
                    {/* Render ASCII map in <pre> for formatting */}
                    <pre className={`ascii-map ${consoleTheme ? 'console-theme' : ''}`}>
                        {mapData.map}
                    </pre>
                </div>
            )}
        </div>
    );
};

// Render the App component to the root element
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
