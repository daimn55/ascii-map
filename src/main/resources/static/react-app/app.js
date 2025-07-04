// Main App component
const App = () => {
    const [countries, setCountries] = React.useState({});
    const [loading, setLoading] = React.useState(false);
    const [mapData, setMapData] = React.useState(null);
    const [error, setError] = React.useState(null);
    const [formData, setFormData] = React.useState({
        countryCode: '',
        width: 120,
        height: 60,
        customCountryCode: ''
    });
    const [useCustomCode, setUseCustomCode] = React.useState(false);
    const [consoleTheme, setConsoleTheme] = React.useState(true);

    // Fetch countries on component mount
    React.useEffect(() => {
        fetchCountries();
    }, []);

    const fetchCountries = async () => {
        try {
            const response = await fetch('/api/countries');
            const data = await response.json();
            setCountries(data.countries || {});
        } catch (err) {
            console.error('Error fetching countries:', err);
            setError('Failed to load country data');
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleCustomCodeToggle = () => {
        setUseCustomCode(!useCustomCode);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMapData(null);
        setError(null);
        
        // Determine which country code to use
        const countryCode = useCustomCode ? formData.customCountryCode : formData.countryCode;
        
        if (!countryCode) {
            setError('Please select a country or enter a country code');
            setLoading(false);
            return;
        }
        
        try {
            const response = await fetch(`/api/render-map/${countryCode}?width=${formData.width}&height=${formData.height}`);
            const data = await response.json();
            
            if (data.error) {
                setError(data.error);
            } else {
                setMapData(data);
            }
        } catch (err) {
            console.error('Error generating map:', err);
            setError('An error occurred while generating the map');
        } finally {
            setLoading(false);
        }
    };

    const downloadMap = () => {
        if (!mapData) return;
        
        const element = document.createElement('a');
        const file = new Blob([mapData.asciiMap], { type: 'text/plain' });
        element.href = URL.createObjectURL(file);
        element.download = `ascii-map-${mapData.countryCode.toLowerCase()}.txt`;
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    };

    // Render popular and all countries options
    const renderCountryOptions = () => {
        return Object.entries(countries).map(([groupName, groupCountries]) => (
            <optgroup key={groupName} label={groupName}>
                {Object.entries(groupCountries).map(([code, name]) => (
                    <option key={code} value={code}>{name} ({code})</option>
                ))}
            </optgroup>
        ));
    };

    return (
        <div className="container">
            <div className="row">
                <div className="col-12 text-center">
                    <h1 className="app-title">ASCII Map Generator - React App</h1>
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
                        <div className="col-md-6 mb-3">
                            <label htmlFor="width" className="form-label">Width:</label>
                            <input 
                                type="number" 
                                className="form-control" 
                                id="width" 
                                name="width" 
                                value={formData.width} 
                                onChange={handleInputChange} 
                                min="40" 
                                max="200" 
                            />
                            <div className="form-text">Min: 40, Max: 200</div>
                        </div>
                        <div className="col-md-6 mb-3">
                            <label htmlFor="height" className="form-label">Height:</label>
                            <input 
                                type="number" 
                                className="form-control" 
                                id="height" 
                                name="height" 
                                value={formData.height} 
                                onChange={handleInputChange} 
                                min="20" 
                                max="100" 
                            />
                            <div className="form-text">Min: 20, Max: 100</div>
                        </div>
                    </div>
                    
                    <div className="d-grid">
                        <button type="submit" className="btn btn-primary" disabled={loading}>
                            {loading ? 'Generating...' : 'Generate ASCII Map'}
                        </button>
                    </div>
                </form>
            </div>
            
            {error && (
                <div className="alert alert-danger" role="alert">
                    {error}
                </div>
            )}
            
            {mapData && (
                <div className="result-container">
                    <div className="theme-toggle">
                        <div className="form-check form-switch">
                            <input 
                                className="form-check-input" 
                                type="checkbox" 
                                id="consoleTheme" 
                                checked={consoleTheme}
                                onChange={() => setConsoleTheme(!consoleTheme)}
                            />
                            <label className="form-check-label" htmlFor="consoleTheme">
                                Console Theme
                            </label>
                        </div>
                    </div>
                    <div className={`card ${consoleTheme ? 'console-theme' : ''}`}>
                        <div className="card-header bg-primary text-white d-flex justify-content-between align-items-center">
                            <h5 className="mb-0">ASCII Map: {mapData.countryName} ({mapData.countryCode})</h5>
                            <button 
                                className={`btn btn-sm ${consoleTheme ? 'btn-outline-success' : 'btn-light'}`}
                                onClick={downloadMap}
                            >
                                Download as Text
                            </button>
                        </div>
                        <div className="card-body">
                            <pre className={`ascii-map ${consoleTheme ? 'console-theme' : ''}`}>{mapData.asciiMap}</pre>
                        </div>
                        <div className="card-footer text-muted">
                            <div className="row">
                                <div className="col-6">Width: {mapData.width}</div>
                                <div className="col-6 text-end">Height: {mapData.height}</div>
                            </div>
                        </div>
                    </div>
                    <div className="d-grid gap-2 mt-3">
                        <button className="btn btn-secondary" onClick={() => setMapData(null)}>
                            Generate Another Map
                        </button>
                        <a href="/" className="btn btn-outline-secondary">
                            Back to Standard UI
                        </a>
                    </div>
                </div>
            )}
            
            <footer className="mt-5 text-center text-muted">
                <p>
                    <small>ASCII Map Generator | <a href="/">Switch to Standard UI</a></small>
                </p>
            </footer>
        </div>
    );
};

// Render the App component to the root element
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
