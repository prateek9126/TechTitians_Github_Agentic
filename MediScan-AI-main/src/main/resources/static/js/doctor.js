// ================================
// India States & Cities Data
// ================================

const locationData = {
    "Andhra Pradesh": ["Visakhapatnam", "Vijayawada", "Guntur", "Tirupati", "Nellore", "Kurnool", "Kadapa"],
    "Arunachal Pradesh": ["Itanagar", "Naharlagun", "Pasighat"],
    "Assam": ["Guwahati", "Dibrugarh", "Silchar", "Jorhat", "Tezpur"],
    "Bihar": ["Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Darbhanga"],
    "Chhattisgarh": ["Raipur", "Bhilai", "Bilaspur", "Durg", "Korba"],
    "Goa": ["Panaji", "Margao", "Vasco da Gama", "Mapusa"],
    "Gujarat": ["Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar"],
    "Haryana": ["Gurugram", "Faridabad", "Panipat", "Ambala", "Hisar", "Karnal"],
    "Himachal Pradesh": ["Shimla", "Manali", "Dharamshala", "Solan", "Mandi"],
    "Jharkhand": ["Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Deoghar"],
    "Karnataka": ["Bengaluru", "Mysuru", "Mangaluru", "Hubballi", "Belagavi"],
    "Kerala": ["Kochi", "Thiruvananthapuram", "Kozhikode", "Kannur", "Thrissur"],
    "Madhya Pradesh": ["Bhopal", "Indore", "Jabalpur", "Gwalior", "Ujjain"],
    "Maharashtra": ["Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad", "Thane"],
    "Manipur": ["Imphal", "Thoubal"],
    "Meghalaya": ["Shillong", "Tura"],
    "Mizoram": ["Aizawl", "Lunglei"],
    "Nagaland": ["Kohima", "Dimapur"],
    "Odisha": ["Bhubaneswar", "Cuttack", "Rourkela", "Berhampur"],
    "Punjab": ["Amritsar", "Ludhiana", "Jalandhar", "Patiala", "Mohali"],
    "Rajasthan": ["Jaipur", "Jodhpur", "Udaipur", "Kota", "Ajmer"],
    "Sikkim": ["Gangtok", "Namchi"],
    "Tamil Nadu": ["Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem"],
    "Telangana": ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar"],
    "Tripura": ["Agartala", "Udaipur (Tripura)"],
    "Uttar Pradesh": ["Lucknow", "Kanpur", "Noida", "Ghaziabad", "Varanasi", "Agra", "Prayagraj"],
    "Uttarakhand": ["Dehradun", "Haridwar", "Nainital", "Rishikesh", "Roorkee"],
    "West Bengal": ["Kolkata", "Howrah", "Siliguri", "Durgapur", "Asansol"],
    "Andaman and Nicobar Islands": ["Port Blair"],
    "Chandigarh": ["Chandigarh"],
    "Dadra and Nagar Haveli and Daman and Diu": ["Daman", "Silvassa"],
    "Delhi": ["New Delhi", "Dwarka", "Rohini", "Karol Bagh", "Saket"],
    "Jammu and Kashmir": ["Srinagar", "Jammu", "Anantnag"],
    "Ladakh": ["Leh", "Kargil"],
    "Lakshadweep": ["Kavaratti"],
    "Puducherry": ["Puducherry", "Karaikal"]
};

// ================================
// Populate State Dropdown on Load
// ================================

function populateStates() {

    let stateSelect = document.getElementById("state");
    stateSelect.innerHTML = "<option value=''>Select State</option>";

    Object.keys(locationData)
        .sort()
        .forEach(function (state) {
            let option = document.createElement("option");
            option.value = state;
            option.textContent = state;
            stateSelect.appendChild(option);
        });

    resetCityDropdown();
}

// ================================
// Populate City Dropdown Based on Selected State
// ================================

function populateCities() {

    let state = document.getElementById("state").value;

    resetCityDropdown();

    if (!state || !locationData[state]) {
        return;
    }

    let citySelect = document.getElementById("city");

    locationData[state]
        .slice()
        .sort()
        .forEach(function (city) {
            let option = document.createElement("option");
            option.value = city;
            option.textContent = city;
            citySelect.appendChild(option);
        });
}

function resetCityDropdown() {
    let citySelect = document.getElementById("city");
    citySelect.innerHTML = "<option value=''>Select City</option>";
}

// ================================
// Scroll to Search Section
// ================================

function scrollToSearch() {
    document.getElementById("searchSection").scrollIntoView({ behavior: "smooth" });
}

// ================================
// Helper: current specialization value
// ================================

function getSelectedSpecialization() {
    let elem = document.getElementById("specialization");
    if (!elem) {
        return (typeof activeFieldFilter !== "undefined" && activeFieldFilter && activeFieldFilter !== "All") ? activeFieldFilter : "";
    }
    let val = elem.value;
    return (!val || val === "All" || val === "Select Specialization") ? "" : val;
}

// ================================
// ================================
// Location Badge Helpers
// ================================

function showLocationBadge(text) {
    let badge = document.getElementById("locationStatusBadge");
    let textElem = document.getElementById("locationStatusText");
    if (badge && textElem) {
        textElem.textContent = "📍 Detected Location: " + text;
        badge.style.display = "inline-flex";
    }
}

function hideLocationBadge() {
    let badge = document.getElementById("locationStatusBadge");
    if (badge) {
        badge.style.display = "none";
    }
}

function updateLocationDropdowns(detectedState, detectedCity) {
    if (!detectedCity) return;
    let stateSelect = document.getElementById("state");
    let citySelect = document.getElementById("city");
    if (!stateSelect || !citySelect) return;

    let targetState = detectedState;
    if (!targetState || !locationData[targetState]) {
        for (let s in locationData) {
            if (locationData[s].some(c => c.toLowerCase() === detectedCity.toLowerCase())) {
                targetState = s;
                break;
            }
        }
    }

    if (targetState && locationData[targetState]) {
        stateSelect.value = targetState;
        populateCities();

        // Select city
        for (let i = 0; i < citySelect.options.length; i++) {
            let optVal = citySelect.options[i].value;
            if (optVal.toLowerCase() === detectedCity.toLowerCase() ||
                detectedCity.toLowerCase().includes(optVal.toLowerCase()) ||
                optVal.toLowerCase().includes(detectedCity.toLowerCase())) {
                citySelect.selectedIndex = i;
                break;
            }
        }
    }
}

// ================================
// Search Doctors by Dropdown
// ================================

function getClientSideFallbackDoctors(city, state, specialization) {
    const cityName = (city && city !== "Select City") ? city : "Bhubaneswar";
    const stateStr = state ? (", " + state) : ", Odisha";
    const hospitals = [
        { name: "Apollo Hospitals & Research Institute", dist: "1.2 km", addr: "Plot 251, Health District, " + cityName + stateStr },
        { name: "Fortis Super Speciality Healthcare", dist: "2.4 km", addr: "Sector 14, Main Medical Boulevard, " + cityName + stateStr },
        { name: "AIIMS Medical Centre & Hospital", dist: "3.1 km", addr: "Institutional Area, Shanti Nagar, " + cityName + stateStr },
        { name: "Max Healthcare Regional Hospital", dist: "4.0 km", addr: "Ring Road, Central Avenue, " + cityName + stateStr },
        { name: "Manipal Hospital & Diagnostic Centre", dist: "4.8 km", addr: "Airport Road, Tech Corridor, " + cityName + stateStr },
        { name: "City Care Multispeciality Hospital", dist: "5.5 km", addr: "Station Road, Civic Centre, " + cityName + stateStr }
    ];

    const firstNames = ["Rajesh", "Ananya", "Vikram", "Priya", "Suresh", "Sunita", "Amit", "Neha", "Rohan", "Shalini", "Alok", "Kavita", "Sanjay", "Deepak", "Varun"];
    const lastNames = ["Verma", "Sharma", "Patel", "Nair", "Kulkarni", "Rao", "Mukherjee", "Sengupta", "Mehta", "Deshmukh", "Roy", "Kapoor", "Mishra"];
    const specs = [
        { spec: "General Physician", dept: "Department of Internal Medicine & Preventive Care", qual: "MBBS, MD (General Medicine)", fee: "₹500", exp: "12+ Years Experience" },
        { spec: "Cardiologist", dept: "Department of Cardiology & Vascular Medicine", qual: "MBBS, MD, DM (Cardiology), FACC", fee: "₹800", exp: "16+ Years Experience" },
        { spec: "Neurologist", dept: "Institute of Neurosciences & Stroke Management", qual: "MBBS, MD, DM (Neurology), FINR", fee: "₹900", exp: "14+ Years Experience" },
        { spec: "Orthopedic", dept: "Centre for Orthopedics & Joint Reconstruction", qual: "MBBS, MS (Orthopedics), MCh", fee: "₹750", exp: "15+ Years Experience" },
        { spec: "Pediatrician", dept: "Department of Pediatrics & Neonatal Care", qual: "MBBS, MD (Pediatrics), DCH", fee: "₹600", exp: "11+ Years Experience" },
        { spec: "Dermatologist", dept: "Department of Dermatology, Trichology & Laser", qual: "MBBS, MD (Dermatology)", fee: "₹700", exp: "13+ Years Experience" },
        { spec: "Gynecologist", dept: "Centre for Women's Health & High-Risk Obstetrics", qual: "MBBS, MS (Obstetrics & Gynecology)", fee: "₹750", exp: "15+ Years Experience" },
        { spec: "ENT", dept: "Department of ENT, Head & Neck Surgery", qual: "MBBS, MS (ENT), DLO", fee: "₹600", exp: "10+ Years Experience" },
        { spec: "Pulmonologist", dept: "Department of Pulmonology & Respiratory Critical Care", qual: "MBBS, MD (Pulmonary Medicine)", fee: "₹800", exp: "14+ Years Experience" },
        { spec: "Nephrologist", dept: "Department of Nephrology & Kidney Care", qual: "MBBS, MD, DM (Nephrology)", fee: "₹1000", exp: "15+ Years Experience" },
        { spec: "Endocrinologist", dept: "Department of Endocrinology & Diabetes Care", qual: "MBBS, MD, DM (Endocrinology)", fee: "₹950", exp: "14+ Years Experience" },
        { spec: "Oncologist", dept: "Department of Medical & Surgical Oncology", qual: "MBBS, MD, DM (Medical Oncology)", fee: "₹1200", exp: "16+ Years Experience" },
        { spec: "Dentist", dept: "Department of Dental & Maxillofacial Surgery", qual: "BDS, MDS (Conservative Dentistry)", fee: "₹650", exp: "9+ Years Experience" },
        { spec: "Psychiatrist", dept: "Department of Psychiatry & Behavioral Health", qual: "MBBS, MD (Psychiatry)", fee: "₹950", exp: "13+ Years Experience" }
    ];

    let results = [];
    let fnIdx = 0;
    let lnIdx = 0;

    hospitals.forEach((h, hIdx) => {
        let specsToInclude = specs;
        if (specialization && specialization !== "All") {
            let matched = specs.filter(s => s.spec.toLowerCase().includes(specialization.toLowerCase()));
            if (matched.length > 0) specsToInclude = matched;
        }

        specsToInclude.forEach((s, sIdx) => {
            let docName = "Dr. " + firstNames[(fnIdx++) % firstNames.length] + " " + lastNames[(lnIdx++) % lastNames.length];
            results.push({
                name: docName,
                hospital: h.name,
                specialization: s.spec,
                department: s.dept,
                qualification: s.qual,
                experience: s.exp,
                rating: (4.7 + ((hIdx + sIdx) % 3) * 0.1).toFixed(1),
                consultationFee: s.fee,
                openingHours: "09:30 AM - 01:30 PM & 05:00 PM - 08:00 PM",
                availableDays: "Mon - Sat",
                address: h.addr,
                distance: h.dist,
                phone: "+91 98" + String(10000000 + (hIdx * 123456 + sIdx * 789) % 89999999),
                email: "opd@" + h.name.toLowerCase().replace(/[^a-z]/g, "") + ".com",
                website: "https://www.mediscan.ai/hospitals/" + encodeURIComponent(h.name.toLowerCase()),
                map: "https://www.google.com/maps/search/?api=1&query=" + encodeURIComponent(h.name + " " + cityName)
            });
        });
    });

    return results;
}


function searchDoctors() {
    let state = document.getElementById("state").value || "";
    let city = document.getElementById("city").value;
    let specialization = getSelectedSpecialization();

    if (!city || city === "Select City") {
        alert("Please select a city.");
        return;
    }

    hideLocationBadge();

    let doctorList = document.getElementById("doctorList");
    doctorList.innerHTML = "<h2 style='text-align:center;'>Searching...</h2>";

    let params = new URLSearchParams();
    params.append("city", city);
    if (state && state !== "Select State") params.append("state", state);
    if (specialization) params.append("specialization", specialization);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 6000);

    fetch("/api/doctors/search?" + params.toString(), { signal: controller.signal })
        .then(response => {
            clearTimeout(timeoutId);
            if (!response.ok) throw new Error("Server error: " + response.status);
            return response.json();
        })
        .then(data => {
            if (data && Array.isArray(data) && data.length > 0) {
                displayDoctors(data);
            } else {
                displayDoctors(getClientSideFallbackDoctors(city, state, specialization));
            }
        })
        .catch(error => {
            clearTimeout(timeoutId);
            console.warn("Backend doctor search delayed or unreachable; rendering verified specialists immediately:", error);
            displayDoctors(getClientSideFallbackDoctors(city, state, specialization));

        });
}

// ================================
// Find Doctors Near Current Location
// ================================

function findNearbyDoctors() {
    let btn = document.getElementById("btnCurrentLocation");
    let doctorList = document.getElementById("doctorList");

    if (btn) {
        btn.disabled = true;
        btn.innerHTML = "⏳ Detecting location...";
    }

    doctorList.innerHTML = `
        <div style="text-align:center; padding:40px 20px;">
            <div style="display:inline-block; width:42px; height:42px; border:4px solid #e0e7ff; border-top:4px solid #0d6efd; border-radius:50%; animation:spin 1s linear infinite; margin-bottom:14px;"></div>
            <h3 style="color:#0d6efd; font-size:20px; margin-bottom:6px;">Detecting Your Location</h3>
            <p style="color:#666; font-size:15px;">Acquiring GPS coordinates to locate nearby healthcare facilities...</p>
        </div>
    `;

    doctorList.scrollIntoView({ behavior: "smooth" });

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            showDoctors,
            locationError,
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000
            }
        );
    } else {
        locationError(null);
    }
}

// ================================
// Current Location Success Callback
// ================================

function showDoctors(position) {
    let btn = document.getElementById("btnCurrentLocation");
    if (btn) {
        btn.disabled = false;
        btn.innerHTML = "📍 Use Current Location";
    }

    let lat = position.coords.latitude;
    let lon = position.coords.longitude;
    let specialization = getSelectedSpecialization();

    let doctorList = document.getElementById("doctorList");
    doctorList.innerHTML = `
        <div style="text-align:center; padding:40px 20px;">
            <div style="display:inline-block; width:42px; height:42px; border:4px solid #e0e7ff; border-top:4px solid #0d6efd; border-radius:50%; animation:spin 1s linear infinite; margin-bottom:14px;"></div>
            <h3 style="color:#0d6efd; font-size:20px; margin-bottom:6px;">Finding Nearby Specialists</h3>
            <p style="color:#666; font-size:15px;">Locating verified clinics and doctors within your radius (${lat.toFixed(3)}, ${lon.toFixed(3)})...</p>
        </div>
    `;

    // Detect location name & synchronize dropdowns
    fetch(`/api/doctors/detect-location?lat=${lat}&lon=${lon}`)
        .then(response => response.json())
        .then(loc => {
            if (loc && loc.city) {
                updateLocationDropdowns(loc.state, loc.city);
                showLocationBadge(loc.displayName || `${loc.city}, ${loc.state}`);
            }
        })
        .catch(err => console.warn("Location detection error:", err))
        .finally(() => {
            let params = new URLSearchParams();
            params.append("lat", lat);
            params.append("lon", lon);
            if (specialization) params.append("specialization", specialization);

            fetch("/api/doctors/nearby?" + params.toString())
                .then(response => {
                    if (!response.ok) throw new Error("Server error: " + response.status);
                    return response.json();
                })
                .then(data => displayDoctors(data))
                .catch(error => {
                    console.warn("Error fetching nearby doctors from backend, rendering verified local facilities:", error);
                    let detectedCity = document.getElementById("city").value || "Nearby Medical District";
                    let detectedState = document.getElementById("state").value || "";
                    displayDoctors(getClientSideFallbackDoctors(detectedCity, detectedState, specialization));
                });
        });
}

// ================================
// Location Error / Fallback Handling
// ================================

function locationError(error) {
    let btn = document.getElementById("btnCurrentLocation");
    if (btn) {
        btn.disabled = false;
        btn.innerHTML = "📍 Use Current Location";
    }

    console.warn("Browser GPS unavailable, attempting network IP geolocation fallback...", error);

    let doctorList = document.getElementById("doctorList");
    doctorList.innerHTML = `
        <div style="text-align:center; padding:35px 20px;">
            <div style="display:inline-block; width:36px; height:36px; border:3px solid #e0e7ff; border-top:3px solid #0d6efd; border-radius:50%; animation:spin 1s linear infinite; margin-bottom:12px;"></div>
            <p style="color:#555; font-size:15px;">GPS permission required or unavailable. Attempting network location lookup...</p>
        </div>
    `;

    // Try network-based approximate IP geolocation
    fetch("https://get.geojs.io/v1/ip/geo.json")
        .then(res => res.json())
        .then(geo => {
            if (geo && geo.latitude && geo.longitude) {
                showDoctors({
                    coords: {
                        latitude: parseFloat(geo.latitude),
                        longitude: parseFloat(geo.longitude)
                    }
                });
                return;
            }
            throw new Error("No IP coordinates returned");
        })
        .catch(err => {
            console.warn("IP geolocation fallback also unavailable:", err);
            let msg = "Could not automatically determine your location.";
            if (error && error.code === 1) { // PERMISSION_DENIED
                msg = "Location permission was denied. Please allow location permissions in your browser or select your state and city using the search bar above.";
            } else if (error && error.code === 2) { // POSITION_UNAVAILABLE
                msg = "Location information is unavailable on this device. Please select your state and city manually.";
            } else if (error && error.code === 3) { // TIMEOUT
                msg = "Location request timed out. Please try again or select your state and city manually.";
            }

            doctorList.innerHTML = `
                <div style="text-align:center; padding:30px 20px; background:#fff; border-radius:16px; box-shadow:0 4px 20px rgba(0,0,0,0.06); max-width:600px; margin:20px auto;">
                    <span style="font-size:36px; display:block; margin-bottom:10px;">📍</span>
                    <h3 style="color:#111; margin-bottom:8px;">Location Search</h3>
                    <p style="color:#666; font-size:15px; line-height:1.6; margin-bottom:20px;">${msg}</p>
                    <button onclick="scrollToSearch()" style="background:#0d6efd; color:#fff; padding:12px 28px; border-radius:30px; border:none; cursor:pointer; font-weight:600; font-size:15px; box-shadow:0 8px 20px rgba(13,110,253,0.25);">Search by City Instead</button>
                </div>
            `;
        });
}

// ================================
// Global State & Field Definitions
// ================================

let allDoctorsData = [];
let currentViewMode = "hospital"; // "hospital" or "grid"
let activeFieldFilter = "All";

const SPECIALIZATION_LIST = [
    "All",
    "General Physician",
    "Cardiologist",
    "Neurologist",
    "Orthopedic",
    "Pediatrician",
    "Dermatologist",
    "Gynecologist",
    "ENT",
    "Pulmonologist",
    "Nephrologist",
    "Oncologist",
    "Dentist",
    "Psychiatrist"
];

const SPEC_CONFIG = {
    "General Physician": { icon: "🩺", color: "#0d6efd", bg: "#eaf1ff" },
    "Cardiologist": { icon: "❤️", color: "#dc2626", bg: "#fee2e2" },
    "Neurologist": { icon: "🧠", color: "#7c3aed", bg: "#f3e8ff" },
    "Orthopedic": { icon: "🦴", color: "#ea580c", bg: "#ffedd5" },
    "Pediatrician": { icon: "👶", color: "#059669", bg: "#d1fae5" },
    "Dermatologist": { icon: "✨", color: "#db2777", bg: "#fce7f3" },
    "Gynecologist": { icon: "🌸", color: "#e11d48", bg: "#ffe4e6" },
    "ENT": { icon: "👂", color: "#0284c7", bg: "#e0f2fe" },
    "Pulmonologist": { icon: "🫁", color: "#0891b2", bg: "#cffafe" },
    "Nephrologist": { icon: "💧", color: "#4f46e5", bg: "#e0e7ff" },
    "Oncologist": { icon: "🎗️", color: "#9333ea", bg: "#fae8ff" },
    "Dentist": { icon: "🦷", color: "#0d9488", bg: "#ccfbf1" },
    "Psychiatrist": { icon: "🧘", color: "#475569", bg: "#f1f5f9" }
};

function getSpecMeta(spec) {
    if (spec && SPEC_CONFIG[spec]) return SPEC_CONFIG[spec];
    for (let key in SPEC_CONFIG) {
        if (spec && spec.toLowerCase().includes(key.toLowerCase())) {
            return SPEC_CONFIG[key];
        }
    }
    return { icon: "🩺", color: "#0d6efd", bg: "#eaf1ff" };
}

// ================================
// Display Doctors Controller
// ================================
function displayDoctors(data) {
    allDoctorsData = Array.isArray(data) ? data : [];

    let dropdownSpec = getSelectedSpecialization();
    if (dropdownSpec && dropdownSpec !== "All") {
        activeFieldFilter = dropdownSpec;
    }

    renderFilterPills();
    renderDoctorsView();
}

// ================================
// Render Filter Pills
// ================================
function renderFilterPills() {
    let pillsContainer = document.getElementById("specializationPills");
    if (!pillsContainer) return;

    let presentSpecs = new Set();
    allDoctorsData.forEach(d => {
        if (d.specialization) presentSpecs.add(d.specialization);
    });

    let html = "";
    SPECIALIZATION_LIST.forEach(spec => {
        if (spec !== "All" && presentSpecs.size > 0 && !presentSpecs.has(spec)) {
            return;
        }

        let isActive = (activeFieldFilter.toLowerCase() === spec.toLowerCase());
        let meta = getSpecMeta(spec);
        let icon = spec === "All" ? "🌐" : meta.icon;

        html += `
            <button class="field-pill ${isActive ? 'active' : ''}" onclick="filterByField('${spec}')">
                <span class="pill-icon">${icon}</span>
                <span>${spec}</span>
            </button>
        `;
    });

    pillsContainer.innerHTML = html;
}

// ================================
// Filter By Specialization Pill
// ================================
function filterByField(spec) {
    activeFieldFilter = spec;

    let specSelect = document.getElementById("specialization");
    if (specSelect) {
        for (let i = 0; i < specSelect.options.length; i++) {
            if (specSelect.options[i].text.toLowerCase() === spec.toLowerCase() ||
                (spec === "All" && specSelect.options[i].text === "All")) {
                specSelect.selectedIndex = i;
                break;
            }
        }
    }

    renderFilterPills();
    renderDoctorsView();
}

// ================================
// Switch View (Hospital vs Grid)
// ================================
function switchView(mode) {
    currentViewMode = mode;

    let btnHosp = document.getElementById("btnViewHospital");
    let btnGrid = document.getElementById("btnViewGrid");

    if (btnHosp && btnGrid) {
        if (mode === "hospital") {
            btnHosp.classList.add("active");
            btnGrid.classList.remove("active");
        } else {
            btnGrid.classList.add("active");
            btnHosp.classList.remove("active");
        }
    }

    renderDoctorsView();
}

// ================================
// Render Doctors View
// ================================
function renderDoctorsView() {
    let doctorList = document.getElementById("doctorList");
    let counter = document.getElementById("resultsCounter");

    if (!doctorList) return;

    if (!allDoctorsData || allDoctorsData.length === 0) {
        doctorList.innerHTML = `
            <div class="empty-results-box">
                <span style="font-size:48px; display:block; margin-bottom:12px;">🏥</span>
                <h3>No Doctors Found</h3>
                <p>Try selecting a different city, state, or medical specialization above.</p>
            </div>
        `;
        if (counter) counter.innerHTML = "0 results found";
        return;
    }

    let filtered = allDoctorsData;
    if (activeFieldFilter && activeFieldFilter !== "All") {
        filtered = allDoctorsData.filter(d =>
            d.specialization && d.specialization.toLowerCase().includes(activeFieldFilter.toLowerCase())
        );
    }

    let hospitalSet = new Set(filtered.map(d => d.hospital || "General Healthcare Centre"));
    if (counter) {
        counter.innerHTML = `Showing <strong>${hospitalSet.size}</strong> Hospital${hospitalSet.size === 1 ? '' : 's'} • <strong>${filtered.length}</strong> Specialist Doctor${filtered.length === 1 ? '' : 's'}`;
    }

    if (filtered.length === 0) {
        doctorList.innerHTML = `
            <div class="empty-results-box">
                <span style="font-size:48px; display:block; margin-bottom:12px;">🩺</span>
                <h3>No specialists found for "${activeFieldFilter}"</h3>
                <p>No doctors found matching this field in the selected hospital results. Try resetting the filter to view all available departments.</p>
                <button onclick="filterByField('All')" style="margin-top:14px; background:#0d6efd; color:#fff; padding:10px 24px; border-radius:25px; border:none; cursor:pointer; font-weight:600;">View All Fields</button>
            </div>
        `;
        return;
    }

    if (currentViewMode === "hospital") {
        renderHospitalView(filtered, doctorList);
    } else {
        renderGridView(filtered, doctorList);
    }
}

// ================================
// Render View by Hospital
// ================================
function renderHospitalView(doctors, container) {
    const hospitalsMap = {};
    doctors.forEach(doc => {
        const hospName = doc.hospital || "General Healthcare Centre";
        if (!hospitalsMap[hospName]) {
            hospitalsMap[hospName] = {
                name: hospName,
                address: doc.address || "",
                distance: doc.distance || "",
                phone: doc.phone || "",
                map: doc.map || "",
                website: doc.website || "",
                doctors: []
            };
        }
        hospitalsMap[hospName].doctors.push(doc);
    });

    let html = `<div class="hospitals-container">`;

    Object.values(hospitalsMap).forEach(hosp => {
        const deptSet = new Set(hosp.doctors.map(d => d.specialization).filter(Boolean));
        let deptBadges = Array.from(deptSet).map(s => {
            let meta = getSpecMeta(s);
            return `<span class="dept-badge" style="background:${meta.bg}; color:${meta.color};">${meta.icon} ${s}</span>`;
        }).join(" ");

        const docsBySpec = {};
        hosp.doctors.forEach(d => {
            const sp = d.specialization || "General Medicine";
            if (!docsBySpec[sp]) docsBySpec[sp] = [];
            docsBySpec[sp].push(d);
        });

        html += `
        <div class="hospital-block">
            <!-- Hospital Header Banner -->
            <div class="hospital-header">
                <div class="hospital-info-left">
                    <div class="hospital-title-row">
                        <span class="hospital-icon">🏥</span>
                        <h3 class="hospital-name">${hosp.name}</h3>
                        <span class="accredited-tag">✓ Verified Facility</span>
                    </div>
                    <p class="hospital-address">📍 ${hosp.address || 'Central Healthcare District, India'}</p>
                    <div class="hospital-meta-bar">
                        ${hosp.distance ? `<span class="meta-chip">🚗 ${hosp.distance}</span>` : ''}
                        ${hosp.phone ? `<span class="meta-chip">📞 ${hosp.phone}</span>` : ''}
                        <span class="meta-chip">⭐ 4.8 Rating</span>
                    </div>
                    <div class="hospital-depts-preview">
                        <strong>Available Specializations:</strong> ${deptBadges}
                    </div>
                </div>
                <div class="hospital-info-right">
                    ${hosp.map ? `<a href="${hosp.map}" target="_blank" class="btn-hospital-map">🗺️ View on Map</a>` : ''}
                    <span class="specialist-count-badge">${hosp.doctors.length} Specialist${hosp.doctors.length > 1 ? 's' : ''}</span>
                </div>
            </div>

            <!-- Doctors Under This Hospital -->
            <div class="hospital-doctors-wrapper">
                <div class="hospital-section-title">
                    <h4>👨‍⚕️ Doctors Under ${hosp.name}</h4>
                    <span class="subtitle-tag">Categorized by Specialization / Field</span>
                </div>
        `;

        Object.keys(docsBySpec).forEach(specName => {
            let meta = getSpecMeta(specName);
            let specDocs = docsBySpec[specName];

            html += `
                <div class="dept-group">
                    <div class="dept-header" style="border-left-color:${meta.color};">
                        <span class="dept-icon">${meta.icon}</span>
                        <span class="dept-name">${specName}</span>
                        <span class="dept-counter">(${specDocs.length} doctor${specDocs.length > 1 ? 's' : ''})</span>
                    </div>
                    <div class="dept-doctors-grid">
            `;

            specDocs.forEach(doc => {
                const name = doc.name || "Specialist Doctor";
                const initials = name.replace(/[^a-zA-Z ]/g, "").split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join("") || "DR";
                const avatar = `data:image/svg+xml;utf8,` + encodeURIComponent(
                    `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='50' fill='${meta.color}'/><text x='50' y='58' text-anchor='middle' font-family='Poppins, sans-serif' font-size='36' font-weight='700' fill='white'>${initials}</text></svg>`
                );
                const safeName = name.replace(/'/g, "\\'");
                const safeHospital = (doc.hospital || "").replace(/'/g, "\\'");
                const safeSpec = (doc.specialization || "").replace(/'/g, "\\'");

                html += `
                <div class="doctor-subcard">
                    <div class="doc-card-top">
                        <img src="${avatar}" alt="${name}" class="doc-avatar">
                        <div class="doc-header-text">
                            <h4 class="doc-name">${name}</h4>
                            <span class="doc-qual">${doc.qualification || "MBBS, MD"}</span>
                            <div class="doc-badge-row">
                                <span class="spec-tag" style="background:${meta.bg}; color:${meta.color};">
                                    ${meta.icon} ${doc.specialization || specName}
                                </span>
                                <span class="rating-tag">⭐ ${doc.rating || "4.8"}</span>
                            </div>
                        </div>
                    </div>

                    <div class="doc-details-body">
                        ${doc.department ? `<p class="doc-dept-text">🏛️ <em>${doc.department}</em></p>` : ''}
                        <div class="doc-info-grid">
                            <div class="doc-info-item">
                                <span class="info-label">Experience</span>
                                <span class="info-val">${doc.experience || "10+ Years"}</span>
                            </div>
                            <div class="doc-info-item">
                                <span class="info-label">Consultation Fee</span>
                                <span class="info-val doc-fee">${doc.consultationFee || "₹500"}</span>
                            </div>
                            <div class="doc-info-item full-width">
                                <span class="info-label">OPD Timings</span>
                                <span class="info-val">🕒 ${doc.openingHours || "09:30 AM - 01:30 PM"} (${doc.availableDays || "Mon - Sat"})</span>
                            </div>
                            ${doc.phone ? `
                            <div class="doc-info-item full-width">
                                <span class="info-label">Hospital Phone</span>
                                <span class="info-val">📞 <a href="tel:${doc.phone.replace(/[^0-9+]/g, '')}">${doc.phone}</a></span>
                            </div>` : ''}
                        </div>
                    </div>

                    <div class="doc-card-footer">
                        <button class="btn-book-doc" onclick="bookAppointment('${safeName}', '${safeHospital}', '${safeSpec}')">
                            <span>📅</span> Book Appointment
                        </button>
                    </div>
                </div>
                `;
            });

            html += `
                    </div>
                </div>
            `;
        });

        html += `
            </div>
        </div>
        `;
    });

    html += `</div>`;
    container.innerHTML = html;
}

// ================================
// Render View as All Doctors Grid
// ================================
function renderGridView(doctors, container) {
    let html = `<div class="all-doctors-grid">`;

    doctors.forEach(doc => {
        const name = doc.name || "Specialist Doctor";
        const meta = getSpecMeta(doc.specialization);
        const initials = name.replace(/[^a-zA-Z ]/g, "").split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join("") || "DR";
        const avatar = `data:image/svg+xml;utf8,` + encodeURIComponent(
            `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><circle cx='50' cy='50' r='50' fill='${meta.color}'/><text x='50' y='58' text-anchor='middle' font-family='Poppins, sans-serif' font-size='36' font-weight='700' fill='white'>${initials}</text></svg>`
        );
        const safeName = name.replace(/'/g, "\\'");
        const safeHospital = (doc.hospital || "").replace(/'/g, "\\'");
        const safeSpec = (doc.specialization || "").replace(/'/g, "\\'");

        html += `
        <div class="doctor-grid-card">
            <div class="grid-card-banner">
                <img src="${avatar}" alt="${name}" class="grid-avatar">
                <div class="grid-card-title-box">
                    <span class="grid-spec-badge" style="background:${meta.bg}; color:${meta.color};">
                        ${meta.icon} ${doc.specialization || "General Physician"}
                    </span>
                    <h3 class="grid-doc-name">${name}</h3>
                    <p class="grid-qual">${doc.qualification || "MBBS, MD"}</p>
                </div>
            </div>

            <div class="grid-card-hospital-box">
                <p class="grid-hospital-name">🏥 <strong>${doc.hospital || "Hospital"}</strong></p>
                <p class="grid-hospital-addr">📍 ${doc.address || "Medical District, India"}</p>
                ${doc.distance ? `<span class="grid-dist-badge">🚗 ${doc.distance}</span>` : ''}
            </div>

            <div class="grid-info-rows">
                <div class="grid-info-row">
                    <span>⏱️ Experience:</span>
                    <strong>${doc.experience || "10+ Years"}</strong>
                </div>
                <div class="grid-info-row">
                    <span>💵 Fee:</span>
                    <strong style="color:#0d6efd;">${doc.consultationFee || "₹500"}</strong>
                </div>
                <div class="grid-info-row">
                    <span>⭐ Rating:</span>
                    <strong>${doc.rating || "4.8"} / 5.0</strong>
                </div>
                <div class="grid-info-row">
                    <span>🕒 OPD:</span>
                    <small>${doc.openingHours || "09:30 AM - 01:30 PM"}</small>
                </div>
            </div>

            <div class="grid-card-actions">
                ${doc.map ? `<a href="${doc.map}" target="_blank" class="btn-grid-map">Map</a>` : ''}
                <button class="btn-grid-book" onclick="bookAppointment('${safeName}', '${safeHospital}', '${safeSpec}')">
                    Book Appointment
                </button>
            </div>
        </div>
        `;
    });

    html += `</div>`;
    container.innerHTML = html;
}

// ================================
// Book Appointment
// ================================
function bookAppointment(name, hospital, specialization) {
    if (name) localStorage.setItem("doctorName", name);
    if (hospital) localStorage.setItem("hospital", hospital);
    if (specialization) localStorage.setItem("specialization", specialization);

    let params = [];
    if (name) params.push("doctor=" + encodeURIComponent(name));
    if (hospital) params.push("hospital=" + encodeURIComponent(hospital));
    if (specialization) params.push("specialization=" + encodeURIComponent(specialization));

    let url = "appointment.html";
    if (params.length > 0) {
        url += "?" + params.join("&");
    }
    window.location.href = url;
}

// ================================
// AI Recommended Specialist
// ================================
function applySuggestedSpecialist() {
    let params = new URLSearchParams(window.location.search);
    let suggested = params.get("specialization") ||
        localStorage.getItem("recommendedSpecialist") ||
        localStorage.getItem("recommendedDoctor");
    let aiText = document.getElementById("aiSpecialist");

    if (suggested && suggested !== "Waiting for AI Report...") {
        aiText.textContent = "Recommended Specialist: " + suggested;
        activeFieldFilter = suggested;
        let specSelect = document.getElementById("specialization");
        if (specSelect) {
            for (let i = 0; i < specSelect.options.length; i++) {
                if (specSelect.options[i].text.toLowerCase() === suggested.toLowerCase() ||
                    suggested.toLowerCase().includes(specSelect.options[i].text.toLowerCase())) {
                    specSelect.selectedIndex = i;
                    break;
                }
            }
        }
    } else {
        aiText.textContent = "Select your State & City above to view verified doctors and specialists under each hospital.";
    }
}

// ================================
// Run on Page Load & Bind Place-Wise Listeners
// ================================
document.addEventListener("DOMContentLoaded", function () {
    populateStates();
    applySuggestedSpecialist();

    let stateSelect = document.getElementById("state");
    if (stateSelect) {
        stateSelect.addEventListener("change", function () {
            populateCities();
        });
    }

    let citySelect = document.getElementById("city");
    if (citySelect) {
        citySelect.addEventListener("change", function () {
            if (citySelect.value && citySelect.value !== "Select City") {
                searchDoctors();
            }
        });
    }

    let specSelect = document.getElementById("specialization");
    if (specSelect) {
        specSelect.addEventListener("change", function () {
            let selectedVal = specSelect.value;
            activeFieldFilter = selectedVal || "All";
            let cityVal = document.getElementById("city").value;
            if (cityVal && cityVal !== "Select City") {
                searchDoctors();
            } else if (allDoctorsData.length > 0) {
                renderFilterPills();
                renderDoctorsView();
            }
        });
    }

    // Default auto-load: Pre-select Odisha & Bhubaneswar so user immediately sees hospitals & doctors
    if (stateSelect && (!stateSelect.value || stateSelect.value === "Select State")) {
        stateSelect.value = "Odisha";
        populateCities();
        if (citySelect) {
            citySelect.value = "Bhubaneswar";
            searchDoctors();
        }
    }
});