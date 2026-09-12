// ===============================
// API URL (Dynamic for Deployed & Local Environments)
// ===============================
const API = (window.location.protocol.startsWith("http")
    ? (window.location.port === "5500" || window.location.port === "3000"
        ? "http://localhost:8080/api"
        : window.location.origin + "/api")
    : "http://localhost:8080/api");


// ===============================
// DOM
// ===============================

const stateSelect = document.getElementById("state");
const citySelect = document.getElementById("city");
const doctorList = document.getElementById("doctorList");
const aiSpecialist = document.getElementById("aiSpecialist");


// ===============================
// AI Recommendation
// ===============================

window.onload = function () {

    loadStates();

    loadAISuggestion();

};


// ===============================
// Scroll
// ===============================

function scrollToSearch(){

    document.getElementById("searchSection")
            .scrollIntoView({
                behavior:"smooth"
            });

}


// ===============================
// AI Specialist
// ===============================

function loadAISuggestion(){

    let specialist =
        localStorage.getItem("recommendedDoctor");

    if(specialist==null){

        specialist="General Physician";

    }

    aiSpecialist.innerHTML=specialist;

}
// ==================================
// LOAD STATES
// ==================================
// DEFAULT LOCATIONS & LOAD STATES
// ==================================

const DEFAULT_INDIAN_LOCATIONS = {
    "Odisha": ["Bhubaneswar", "Cuttack", "Rourkela", "Puri", "Sambalpur", "Berhampur"],
    "Delhi": ["New Delhi", "North Delhi", "South Delhi", "West Delhi", "East Delhi"],
    "Maharashtra": ["Mumbai", "Pune", "Nagpur", "Thane", "Nashik"],
    "Karnataka": ["Bengaluru", "Mysuru", "Mangaluru", "Hubballi"],
    "Telangana": ["Hyderabad", "Warangal", "Nizamabad"],
    "Tamil Nadu": ["Chennai", "Coimbatore", "Madurai", "Salem"],
    "West Bengal": ["Kolkata", "Howrah", "Durgapur", "Siliguri"],
    "Bihar": ["Patna", "Gaya", "Bhagalpur", "Muzaffarpur"],
    "Uttar Pradesh": ["Lucknow", "Noida", "Kanpur", "Varanasi", "Agra"]
};

async function loadStates(){
    if (!stateSelect) return;

    try {
        const response = await fetch(API + "/states");
        if (!response.ok) throw new Error("States endpoint unavailable");
        const states = await response.json();
        populateStateSelect(states);
        loadCities();
    } catch(e) {
        // Silent fallback without blocking alert
        populateStateSelect(Object.keys(DEFAULT_INDIAN_LOCATIONS));
        loadCities();
    }
}

function populateStateSelect(states) {
    if (!stateSelect) return;
    stateSelect.innerHTML = "";
    states.forEach(state => {
        let option = document.createElement("option");
        option.value = state;
        option.text = state;
        stateSelect.appendChild(option);
    });
}

// ==================================
// LOAD CITIES
// ==================================

async function loadCities(){
    if (!stateSelect || !citySelect) return;
    const state = stateSelect.value;

    try {
        const response = await fetch(API + "/cities?state=" + encodeURIComponent(state));
        if (!response.ok) throw new Error("Cities endpoint unavailable");
        const cities = await response.json();
        populateCitySelect(cities);
    } catch(e) {
        const fallbackCities = DEFAULT_INDIAN_LOCATIONS[state] || ["City Center", "Central", "North Zone"];
        populateCitySelect(fallbackCities);
    }
}

function populateCitySelect(cities) {
    if (!citySelect) return;
    citySelect.innerHTML = "";
    cities.forEach(city => {
        let option = document.createElement("option");
        option.value = city;
        option.text = city;
        citySelect.appendChild(option);
    });
}

stateSelect.addEventListener("change",loadCities);
// ==================================
// SEARCH DOCTORS
// ==================================

async function searchDoctors(){

    doctorList.innerHTML=`
    
    <h2 style="text-align:center;color:#0d6efd">
    
    Searching Doctors...
    
    </h2>
    
    `;

    const country="India";

    const state=
    stateSelect.value;

    const city=
    citySelect.value;

    const specialization=
    document.getElementById("specialization").value;

    try{

        const response=
        await fetch(

        API+

        "/doctors/search?"

        +

        new URLSearchParams({

            country,

            state,

            city,

            specialization

        })

        );

        const doctors=
        await response.json();

        displayDoctors(doctors);

    }

    catch(e){

        doctorList.innerHTML=`

        <h2 style="color:red">

        Unable to fetch doctors.

        </h2>

        `;

    }

}
// ==================================
// SEARCH DOCTORS
// ==================================

async function searchDoctors(){

    doctorList.innerHTML=`
    
    <h2 style="text-align:center;color:#0d6efd">
    
    Searching Doctors...
    
    </h2>
    
    `;

    const country="India";

    const state=
    stateSelect.value;

    const city=
    citySelect.value;

    const specialization=
    document.getElementById("specialization").value;

    try{

        const response=
        await fetch(

        API+

        "/doctors/search?"

        +

        new URLSearchParams({

            country,

            state,

            city,

            specialization

        })

        );

        const doctors=
        await response.json();

        displayDoctors(doctors);

    }

    catch(e){

        doctorList.innerHTML=`

        <h2 style="color:red">

        Unable to fetch doctors.

        </h2>

        `;

    }

}
// ==================================
// BOOK APPOINTMENT
// ==================================

function bookDoctor(name){

    localStorage.setItem("selectedDoctor",name);

    window.location.href="appointment.html";

}


// ==================================
// SAVE AI SPECIALIST
// ==================================

function updateAISpecialist(name){

    localStorage.setItem("recommendedDoctor",name);

}


// ==================================
// CLEAR RESULTS
// ==================================

function clearDoctors(){

    doctorList.innerHTML="";

}


// ==================================
// ENTER KEY SUPPORT
// ==================================

document.addEventListener("keypress",function(e){

    if(e.key==="Enter"){

        searchDoctors();

    }

});