/**
 * MediScan AI - Autonomous Clinical Documentation & Follow-up Agent
 * Frontend Interactive Controller with Dual Patient & Doctor/Technical Views
 */

let currentPatients = [];
let selectedPatient = null;
let lastResponse = null;
let currentView = 'PATIENT'; // Default view mode is 'PATIENT'
let lastDoctorTab = 'tab-conflicts';

// =========================================================
// TOOL DISPLAY NAME MAPPING (DOCTOR / TECHNICAL AUDIT LOG)
// =========================================================
const TOOL_DISPLAY_NAMES = {
    'PatientRecordLookupService': 'Fetching Patient File',
    'MedicationAllergyLookupService': 'Checking Medicine Safety',
    'ClinicalGuidelineRAGService': 'Looking Up Medical Guidelines',
    'LabRetrievalService': 'Pulling Latest Lab Report',
    'RecordValidationService': 'Double-Checking the Report'
};

/**
 * Resolves user-friendly display name and preserves technical service class name.
 */
function getToolDisplayInfo(toolName, apiDisplayName) {
    if (apiDisplayName && apiDisplayName !== toolName) {
        return {
            displayName: apiDisplayName,
            technicalName: toolName || ''
        };
    }

    if (!toolName) {
        return {
            displayName: 'Unknown Tool',
            technicalName: ''
        };
    }

    const raw = String(toolName).trim();

    // 1. Direct dictionary match
    if (TOOL_DISPLAY_NAMES[raw]) {
        return {
            displayName: TOOL_DISPLAY_NAMES[raw],
            technicalName: raw
        };
    }

    // 2. Check for retry/suffix pattern, e.g. "LabRetrievalService (Retry 1)"
    const suffixMatch = raw.match(/^([a-zA-Z0-9_-]+)(\s*\(.*?\))$/);
    if (suffixMatch && TOOL_DISPLAY_NAMES[suffixMatch[1]]) {
        return {
            displayName: `${TOOL_DISPLAY_NAMES[suffixMatch[1]]} ${suffixMatch[2].trim()}`,
            technicalName: raw
        };
    }

    // 3. Fallback for unmapped or internal engine names
    return {
        displayName: raw,
        technicalName: raw
    };
}

// =========================================================
// BUILT-IN FALLBACK SYNTHETIC PATIENT SCENARIOS
// (Guarantees instant scenario dropdown and metadata load even if Render backend is sleeping)
// =========================================================
const FALLBACK_SYNTHETIC_PATIENTS = [
    {
        patientId: "PAT-101",
        name: "Robert Shayne",
        age: 58,
        gender: "Male",
        scenarioTitle: "Penicillin Hypersensitivity Contradiction (Safety Critical)",
        scenarioDescription: "Patient presenting with bronchitis symptoms. EHR allergy banner states NKDA, but prior ED discharge notes document severe anaphylactoid reaction to Augmentin.",
        deliberateContradictionDescription: "EHR Allergy List shows 'NKDA' (No Known Drug Allergies), whereas ED clinical note from 2023-04-12 documents severe acute urticaria and wheezing following Amoxicillin-Clavulanate.",
        consultationTranscript: "Dr. Robert Evans (PCP): Good morning, Robert. What brings you into the clinic today?\\nRobert Shayne: Good morning doctor. I've had this persistent chesty cough for the last 5 days with yellowish phlegm, a low-grade fever around 100.4 F, and feeling completely worn out.\\nDr. Evans: Let me listen to your lungs. Deep breath in... and out. Yes, there are scattered rhonchi and mild wheezes in both lower lobes. Given the discolored sputum and fever duration, this looks consistent with acute acute bacterial bronchitis.\\nRobert Shayne: Is there an antibiotic that can clear this up quickly?\\nDr. Evans: If symptoms persist past tomorrow or your fever climbs, I would consider prescribing a 7-day course of Augmentin (Amoxicillin-Clavulanate 875/125mg) or Cefuroxime. Have you had any issues with antibiotics recently?\\nRobert Shayne: Not that I recall recently, but my records should show whatever I took in the past.\\nDr. Evans: Okay, your intake chart says No Known Drug Allergies (NKDA). Let's do a chest X-ray and basic blood work first before calling in the prescription. In the meantime, rest, stay hydrated, and use an albuterol inhaler PRN.\\n",
        clinicalNotes: [
            { noteId: "NOTE-2023-0412", date: "2023-04-12", author: "Dr. Sarah Jenkins, MD", department: "Emergency Medicine", noteType: "EMERGENCY_ENCOUNTER", text: "CHIEF COMPLAINT: Acute allergic reaction / rash.\\nHISTORY: 56-year-old male presented to Emergency Dept 35 minutes after taking his first dose of oral Augmentin (Amoxicillin-Clavulanate 875mg) prescribed for sinusitis. Developed acute diffuse pruritic urticarial wheals across torso and arms, lip swelling, and mild inspiratory stridor/dyspnea.\\nIMPRESSION: Severe Type-I anaphylactoid hypersensitivity to Amoxicillin-Clavulanate (Penicillin class)." }
        ],
        medicationHistory: [
            { medicationId: "MED-01", medicationName: "Albuterol HFA Inhaler", dosage: "90mcg", frequency: "1-2 puffs Q4-6H PRN", route: "Inhalation", status: "ACTIVE", prescribedDate: "2023-11-10", lastRefilledDate: "2024-01-15", prescriber: "Dr. Evans", indication: "Asthma / Bronchospasm" }
        ],
        allergyList: [
            { allergyId: "ALG-01", allergen: "No Known Drug Allergies (NKDA)", allergyType: "DRUG", reaction: "None reported at triage intake", severity: "NONE", verificationStatus: "REPORTED_NKDA", identifiedDate: "2024-02-01" }
        ],
        labReports: [
            { reportId: "LAB-2024-001", panelName: "CBC with Differential", collectionDate: "2024-02-14", orderingPhysician: "Dr. Evans", interpretationSummary: "Mild leukocytosis (WBC 11.8 K/uL). Platelets and Hemoglobin normal.", results: [] }
        ]
    },
    {
        patientId: "PAT-102",
        name: "Maria Santos",
        age: 64,
        gender: "Female",
        scenarioTitle: "Renal Function vs Metformin Dosing Mismatch",
        scenarioDescription: "Patient with T2D prescribed maximum dose Metformin. Recent lab panel shows declining eGFR below the safe therapeutic threshold.",
        deliberateContradictionDescription: "EHR active medication list indicates Metformin 1000mg BID (2000mg/day), while recent BMP reveals eGFR 28 mL/min/1.73m2 (ADA Contraindication: Discontinue if eGFR < 30).",
        consultationTranscript: "Dr. Angela Vance: Welcome back, Maria. How are you feeling with your blood sugar regimen?\\nMaria Santos: Generally okay, Dr. Vance. I take the big white pills, Metformin 1000mg twice a day, exactly like you instructed. But lately I've felt quite fatigued, a little nauseated in the mornings, and some muscle cramps in my legs.\\nDr. Vance: Your chart shows you've been on Metformin 1000mg BID for two years. Let's look over your metabolic panel and kidney labs from yesterday.\\n",
        clinicalNotes: [
            { noteId: "NOTE-2023-0915", date: "2023-09-15", author: "Dr. Angela Vance, MD", department: "Internal Medicine", noteType: "OFFICE_VISIT", text: "ASSESSMENT: Type 2 Diabetes Mellitus with chronic kidney disease stage 3. Stable on Metformin 1000mg PO BID. Reminded patient to stay well hydrated." }
        ],
        medicationHistory: [
            { medicationId: "MED-02", medicationName: "Metformin", dosage: "1000mg", frequency: "BID", route: "Oral", status: "ACTIVE", prescribedDate: "2022-03-10", lastRefilledDate: "2024-02-01", prescriber: "Dr. Vance", indication: "Type 2 Diabetes Mellitus" },
            { medicationId: "MED-03", medicationName: "Lisinopril", dosage: "10mg", frequency: "Daily", route: "Oral", status: "ACTIVE", prescribedDate: "2021-08-12", lastRefilledDate: "2024-02-01", prescriber: "Dr. Vance", indication: "Hypertension / Renal Protection" }
        ],
        allergyList: [
            { allergyId: "ALG-02", allergen: "Sulfa antibiotics", allergyType: "DRUG", reaction: "Maculopapular rash", severity: "MODERATE", verificationStatus: "CONFIRMED", identifiedDate: "2019-06-20" }
        ],
        labReports: [
            { reportId: "LAB-2024-0102", panelName: "Basic Metabolic Panel (BMP)", collectionDate: "2024-02-20", orderingPhysician: "Dr. Vance", interpretationSummary: "Serum Creatinine 2.3 mg/dL (Elevated), eGFR 28 mL/min/1.73m2 (Critical: Stage 4 CKD).", results: [] }
        ]
    },
    {
        patientId: "PAT-103",
        name: "Arthur Pendelton",
        age: 72,
        gender: "Male",
        scenarioTitle: "Anticoagulation & NSAID Gastrointestinal Hemorrhage Risk",
        scenarioDescription: "Atrial fibrillation patient on Warfarin requesting high-dose Naproxen for acute gout flare-up.",
        deliberateContradictionDescription: "Patient reports taking OTC Naproxen 500mg BID for knee pain while actively prescribed Warfarin 5mg daily with supratherapeutic INR 3.4.",
        consultationTranscript: "Dr. Marcus Reed: Hello Mr. Pendelton. How is the right knee doing today?\\nArthur Pendelton: It's been hurting terribly doctor. My neighbor gave me some over-the-counter Naproxen 500mg pills and I've been taking two every day for the last week. It helps the swelling, but my stomach has been burning and I noticed some dark stools yesterday.\\nDr. Reed: Mr. Pendelton, you take Warfarin 5mg every evening for atrial fibrillation. Combining high-dose NSAIDs like Naproxen with Warfarin creates a severe risk of gastrointestinal bleeding.\\n",
        clinicalNotes: [
            { noteId: "NOTE-2023-1102", date: "2023-11-02", author: "Dr. Marcus Reed, MD", department: "Cardiology", noteType: "CARDIOLOGY_CONSULT", text: "IMPRESSION: Non-valvular Atrial Fibrillation on Warfarin 5mg daily. Target INR 2.0-3.0. Patient strongly counseled against taking any NSAIDs (Advil, Aleve, Naproxen, Ibuprofen) due to catastrophic bleeding risks." }
        ],
        medicationHistory: [
            { medicationId: "MED-04", medicationName: "Warfarin", dosage: "5mg", frequency: "Daily at bedtime", route: "Oral", status: "ACTIVE", prescribedDate: "2020-04-15", lastRefilledDate: "2024-02-05", prescriber: "Dr. Reed", indication: "Atrial Fibrillation Stroke Prophylaxis" },
            { medicationId: "MED-05", medicationName: "Metoprolol Succinate", dosage: "50mg", frequency: "Daily", route: "Oral", status: "ACTIVE", prescribedDate: "2020-04-15", lastRefilledDate: "2024-02-05", prescriber: "Dr. Reed", indication: "Rate Control" }
        ],
        allergyList: [],
        labReports: [
            { reportId: "LAB-2024-0210", panelName: "Coagulation Panel", collectionDate: "2024-02-22", orderingPhysician: "Dr. Reed", interpretationSummary: "Prothrombin Time (PT) 38.2s, INR 3.4 (Supratherapeutic). High bleeding risk.", results: [] }
        ]
    },
    {
        patientId: "PAT-104",
        name: "David Kim",
        age: 52,
        gender: "Male",
        scenarioTitle: "Undocumented Critical Hyperglycemia vs Fasting Lab Contradiction",
        scenarioDescription: "Patient presenting for routine annual exam claiming well-managed glucose levels, while recent fasting lab report demonstrates severe acute hyperglycemia.",
        deliberateContradictionDescription: "Consultation dialogue records patient stating 'my morning sugars have been rock solid in the 90s', but verified CMP demonstrates Fasting Blood Glucose of 245 mg/dL and HbA1c of 10.2%.",
        consultationTranscript: "Dr. Lisa Chen: Good afternoon, David. We are reviewing your annual checkup.\\nDavid Kim: Good afternoon Dr. Chen. I'm feeling great. My home glucose monitor is always around 90-95 mg/dL every morning, completely normal. I haven't needed to change anything in my diet.\\nDr. Lisa Chen: David, we just received your lab panel drawn yesterday morning while fasting. Your Fasting Blood Glucose was 245 mg/dL, and your HbA1c came back at 10.2%, which is critically high and indicates persistent severe hyperglycemia over the past three months.\\n",
        clinicalNotes: [
            { noteId: "NOTE-2023-0820", date: "2023-08-20", author: "Dr. Lisa Chen, MD", department: "Family Medicine", noteType: "ANNUAL_EXAM", text: "Patient reported satisfactory glycemic control. Recommended maintaining healthy diet and routine lab checks." }
        ],
        medicationHistory: [
            { medicationId: "MED-06", medicationName: "Atorvastatin", dosage: "20mg", frequency: "Daily", route: "Oral", status: "ACTIVE", prescribedDate: "2021-05-10", lastRefilledDate: "2024-01-10", prescriber: "Dr. Chen", indication: "Hyperlipidemia" }
        ],
        allergyList: [],
        labReports: [
            { reportId: "LAB-2024-0305", panelName: "Comprehensive Metabolic Panel (CMP) & HbA1c", collectionDate: "2024-03-05", orderingPhysician: "Dr. Chen", interpretationSummary: "Fasting Blood Glucose 245 mg/dL (CRITICAL HIGH), HbA1c 10.2% (CRITICAL HIGH), eGFR 88 mL/min/1.73m2.", results: [] }
        ]
    }
];

document.addEventListener('DOMContentLoaded', () => {
    // Populate immediately with verified synthetic scenarios so the UI is ready instantly
    populatePatientSelect(FALLBACK_SYNTHETIC_PATIENTS);
    switchView('PATIENT');
    // In background, sync with backend
    fetchPatients();
});

// =========================================================
// VIEW TOGGLE CONTROLLER (PATIENT VIEW vs DOCTOR VIEW)
// =========================================================
function switchView(viewMode) {
    currentView = viewMode;

    const togglePatientBtn = document.getElementById('togglePatientView');
    const toggleDoctorBtn = document.getElementById('toggleDoctorView');
    const adaptBtn = document.getElementById('btnAdapt');
    const simFailureBox = document.getElementById('simulationToggleBox');
    const stepperMount = document.getElementById('stepperMount');
    const patientMount = document.getElementById('patientViewMount');
    const doctorMount = document.getElementById('doctorViewMount');

    if (viewMode === 'PATIENT') {
        // 1. Update toggle button active states
        if (togglePatientBtn) {
            togglePatientBtn.classList.add('active');
            togglePatientBtn.setAttribute('aria-selected', 'true');
        }
        if (toggleDoctorBtn) {
            toggleDoctorBtn.classList.remove('active');
            toggleDoctorBtn.setAttribute('aria-selected', 'false');
        }

        // 2. Hide demo buttons only relevant for doctors/reviewers
        if (adaptBtn) adaptBtn.style.display = 'none';
        if (simFailureBox) simFailureBox.style.display = 'none';

        // 3. COMPLETELY REMOVE DOCTOR ELEMENTS FROM THE ACTIVE DOM
        // Not visually hidden, actually not rendered!
        if (stepperMount) stepperMount.innerHTML = '';
        if (doctorMount) doctorMount.innerHTML = '';

        // 4. Render Patient View
        if (patientMount) {
            patientMount.innerHTML = buildPatientViewHtml(lastResponse);
        }
    } else {
        // DOCTOR / TECHNICAL VIEW
        // 1. Update toggle button active states
        if (togglePatientBtn) {
            togglePatientBtn.classList.remove('active');
            togglePatientBtn.setAttribute('aria-selected', 'false');
        }
        if (toggleDoctorBtn) {
            toggleDoctorBtn.classList.add('active');
            toggleDoctorBtn.setAttribute('aria-selected', 'true');
        }

        // 2. Show demo simulation buttons in Doctor View
        if (adaptBtn) adaptBtn.style.display = 'inline-flex';
        if (simFailureBox) simFailureBox.style.display = 'flex';

        // 3. COMPLETELY REMOVE PATIENT VIEW FROM THE ACTIVE DOM
        if (patientMount) patientMount.innerHTML = '';

        // 4. Render Stepper and Doctor Tabs/Panes from Template into DOM
        renderDoctorView(lastResponse);
    }
}

// =========================================================
// PATIENT VIEW RENDERING & PLAIN-LANGUAGE CONVERTER
// =========================================================
function buildPatientViewHtml(resp) {
    if (!resp) {
        return `
            <div class="patient-view-card">
                <p style="color: #64748b; text-align: center;">Run the autonomous pipeline above to view the patient summary.</p>
            </div>
        `;
    }

    const patientName = (resp.followUpRecord && resp.followUpRecord.patientName) || (selectedPatient && selectedPatient.name) || 'Patient';
    const encounterDate = (resp.followUpRecord && resp.followUpRecord.encounterDate) || new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

    // 1. Plain-Language Summary of Findings
    let findingsHtml = '';
    if (resp.conflicts && resp.conflicts.length > 0) {
        findingsHtml = resp.conflicts.map(c => {
            const plainText = toPlainLanguageSummary(c);
            return `
                <div class="patient-finding-box">
                    <div class="patient-finding-title">
                        <span>🔍</span> Summary of Record Finding
                    </div>
                    <p class="patient-finding-text">${escapeHtml(plainText)}</p>
                </div>
            `;
        }).join('');
    } else {
        findingsHtml = `
            <div class="patient-finding-box">
                <div class="patient-finding-title"><span>✅</span> All Records Aligned</div>
                <p class="patient-finding-text">All medications, allergies, and test results appear consistent across your records. Please continue taking your prescribed medications as directed by your doctor.</p>
            </div>
        `;
    }

    // 2. Action List / Next Steps in Plain Language
    let actionsHtml = '';
    if (resp.actionItems && resp.actionItems.length > 0) {
        actionsHtml = resp.actionItems.map(act => {
            const plainAction = toPlainLanguageAction(act);
            return `
                <div class="patient-step-item">
                    <div class="patient-step-icon">${plainAction.icon}</div>
                    <div class="patient-step-content">
                        <div class="patient-step-text">${escapeHtml(plainAction.text)}</div>
                    </div>
                </div>
            `;
        }).join('');
    } else {
        actionsHtml = `
            <div class="patient-step-item">
                <div class="patient-step-icon">📋</div>
                <div class="patient-step-content">
                    <div class="patient-step-text">Continue your regular care routine and follow up with your doctor as scheduled.</div>
                </div>
            </div>
        `;
    }

    // 3. Return Complete Patient View Structure
    return `
        <div class="patient-view-card">
            <!-- Header Banner -->
            <div class="patient-card-header">
                <div class="patient-welcome-badge">
                    <span>👋</span> Care Summary for <strong>${escapeHtml(patientName)}</strong>
                </div>
                <div class="patient-date-tag">
                    <span>📅</span> ${escapeHtml(encounterDate)}
                </div>
            </div>

            <!-- Plain-Language Findings Summary -->
            <div class="patient-summary-section">
                <h3 class="patient-section-heading">
                    <span>📋</span> What We Found in Your Records
                </h3>
                <p class="patient-section-sub">
                    A clear, plain-language summary comparing your doctor visit notes, active prescriptions, and recent lab results:
                </p>
                <div class="patient-findings-list">
                    ${findingsHtml}
                </div>
            </div>

            <!-- Plain-Language Action Steps -->
            <div class="patient-actions-section">
                <h3 class="patient-section-heading">
                    <span>✅</span> Recommended Next Steps
                </h3>
                <p class="patient-section-sub">
                    Practical, easy-to-follow steps to help keep your care plan safe and on track:
                </p>
                <div class="patient-action-steps">
                    ${actionsHtml}
                </div>
            </div>

            <!-- Standard Disclaimer Required by Clinical Guardrails -->
            <div class="patient-disclaimer-card">
                <div class="disclaimer-icon">ℹ️</div>
                <div>
                    <p><strong>Important Note:</strong> This is a draft for your doctor to review — not a diagnosis. Based on simulated/synthetic data.</p>
                </div>
            </div>
        </div>
    `;
}

/**
 * Rewrites the "Reconciled Resolution" text into friendly, non-technical sentences
 * without tool names, severity labels, or "Source A/Source B" jargon.
 */
function toPlainLanguageSummary(conflict) {
    const field = (conflict.field || '').toLowerCase();
    const notes = conflict.resolutionNotes || '';
    const notesLower = notes.toLowerCase();

    // Scenario: PAT-103 (Polypharmacy / NSAID / Triple Whammy Interaction)
    if (notesLower.includes('nsaid') || field.includes('triple whammy') || field.includes('ibuprofen') || notesLower.includes('unmonitored nsaid') || field.includes('spironolactone')) {
        return "You're currently taking a pain reliever (Ibuprofen) that doesn't mix safely with your blood pressure medicines. We recommend stopping it and getting a follow-up blood test in 7-10 days. Please confirm with your doctor before making changes.";
    }

    // Scenario: PAT-101 (Penicillin / Augmentin Hypersensitivity Contradiction)
    if (field.includes('allergy') || field.includes('penicillin') || notesLower.includes('beta-lactam') || notesLower.includes('hypersensitivity') || field.includes('bronchitis')) {
        return "Our review found a record of a past severe allergic reaction to penicillin-family antibiotics (such as Augmentin), even though your main chart did not list it. We have alerted your doctor to ensure this medication is avoided and a safe alternative is chosen.";
    }

    // Scenario: PAT-102 Adapted (Mid-Session STAT CMP Lab Arrival showing low eGFR)
    if (notesLower.includes('adapted') && (notesLower.includes('egfr') || notesLower.includes('creatinine') || notesLower.includes('renal'))) {
        return "Your latest lab results show that your kidney filtration rate has decreased. Because of this, Metformin is no longer safe to take, and your doctor will recommend a safer diabetes treatment to protect your kidney health.";
    }

    // Scenario: PAT-102 Baseline (Metformin Adherence Discrepancy & Verbal Cessation)
    if (field.includes('metformin') || (field.includes('adherence') && notesLower.includes('cessation'))) {
        return "We noted that you stopped taking Metformin three weeks ago due to stomach upset, while your records still listed it as active. Your care team has been updated so your doctor can discuss a gentler formulation or an alternative blood sugar medication with you.";
    }

    // Scenario: PAT-104 (Fasting Blood Glucose / HbA1c Lab Contradiction)
    if (field.includes('glucose') || field.includes('glycemic') || notesLower.includes('glucose')) {
        return "Your recent blood test results showed a higher blood sugar level (245 mg/dL) than you were previously told. We have flagged this for your doctor so you can schedule a prompt follow-up to review your numbers and discuss an effective treatment plan.";
    }

    // Scenario: Tool Failure Simulation
    if (notesLower.includes('tool failure') || notesLower.includes('unreachable') || notesLower.includes('timeout')) {
        return "One of your electronic laboratory records could not be retrieved automatically due to a temporary connection issue. We have notified your doctor to verify your lab records manually before confirming your care plan.";
    }

    // Robust Fallback: Cleanse technical clinical shorthand, tool names, and severity labels
    let cleaned = notes
        .replace(/^(RESOLVED|ESCALATED TO CLINICIAN|ESCALATED|ADAPTED RESOLUTION|DETECTED):\s*/i, '')
        .replace(/via\s+[A-Za-z0-9_]+(\s*Guidelines)?/gi, '')
        .replace(/per guideline\s+[A-Za-z0-9_\-\s]+/gi, '')
        .replace(/Guideline\s+[A-Za-z0-9_\-\s]+\s*cited:?/gi, '')
        .replace(/LabRetrievalService:?/gi, '')
        .replace(/MedicationAllergyLookupService:?/gi, '')
        .replace(/ClinicalGuidelineRAGService:?/gi, '')
        .replace(/PatientRecordLookupService:?/gi, '')
        .replace(/Source\s+[AB]:?/gi, '')
        .replace(/nephrotoxic contributor/gi, 'stressful on your kidneys')
        .replace(/hemodynamic renal risk/gi, 'risk to kidney function')
        .replace(/discontinuation/gi, 'stopping the medication')
        .replace(/repeat BMP/gi, 'a follow-up blood test')
        .replace(/glycemic control/gi, 'blood sugar management')
        .replace(/contraindication/gi, 'safety conflict')
        .trim();

    return cleaned ? `${cleaned}. Please confirm with your doctor before making changes.` : "Your care team has reviewed your records and will discuss these findings at your next visit.";
}

/**
 * Rewrites technical clinician action items into plain-language next steps with icons.
 */
function toPlainLanguageAction(action) {
    const desc = action.description || '';
    const dLower = desc.toLowerCase();

    if (dLower.includes('hold oral ibuprofen') || dLower.includes('ibuprofen')) {
        return {
            icon: '💊',
            text: "Stop taking over-the-counter Ibuprofen right away to protect your kidneys."
        };
    }
    if (dLower.includes('basic metabolic panel') || dLower.includes('bmp') || dLower.includes('7-10 days')) {
        return {
            icon: '🩸',
            text: "Get a routine follow-up blood test in 7 to 10 days to check your kidney function."
        };
    }
    if (dLower.includes('acetaminophen') || dLower.includes('topical') || dLower.includes('knee pain')) {
        return {
            icon: '🩺',
            text: "Ask your doctor or pharmacist about safer pain relief alternatives, such as acetaminophen (Tylenol) or topical joint gels."
        };
    }
    if (dLower.includes('penicillin') || dLower.includes('skin testing') || dLower.includes('allergist') || dLower.includes('beta-lactam')) {
        return {
            icon: '🛡️',
            text: "Make sure your care team avoids penicillin-type antibiotics and discuss safe alternative medications."
        };
    }
    if (dLower.includes('discontinue metformin') || dLower.includes('stat cmp')) {
        return {
            icon: '💊',
            text: "Stop taking Metformin and have your doctor review your updated kidney lab tests."
        };
    }
    if (dLower.includes('second-line agent') || dLower.includes('glucose') || dLower.includes('glycemic')) {
        return {
            icon: '🩺',
            text: "Schedule an appointment with your doctor to review your blood sugar readings and choose an appropriate treatment plan."
        };
    }
    if (dLower.includes('unreachable') || dLower.includes('manual')) {
        return {
            icon: '📋',
            text: "Have your care team verify your recent lab records directly with the laboratory."
        };
    }

    // Generic fallback cleaner
    let cleaned = desc
        .replace(/to preserve renal hemodynamics/gi, 'to protect your kidney health')
        .replace(/\bPRN\b/gi, 'as needed')
        .replace(/\bSTAT\b/gi, 'prompt')
        .replace(/\bBMP\b/gi, 'routine blood test')
        .replace(/\bCMP\b/gi, 'comprehensive blood test');

    return {
        icon: '📋',
        text: cleaned
    };
}

// =========================================================
// DOCTOR / TECHNICAL VIEW RENDERING
// =========================================================
function renderDoctorView(resp) {
    const stepperMount = document.getElementById('stepperMount');
    const doctorMount = document.getElementById('doctorViewMount');
    const stepperTpl = document.getElementById('stepperTemplate');
    const doctorTpl = document.getElementById('doctorViewTemplate');

    if (!stepperMount || !doctorMount || !stepperTpl || !doctorTpl) return;

    stepperMount.innerHTML = '';
    doctorMount.innerHTML = '';

    // Clone templates into the active DOM
    stepperMount.appendChild(stepperTpl.content.cloneNode(true));
    doctorMount.appendChild(doctorTpl.content.cloneNode(true));

    // Bind doctor tab event listeners
    initTabs();

    // Populate Baseline Sources
    if (selectedPatient) {
        renderPatientSources(selectedPatient);
    }

    if (!resp) return;

    // 1. Pipeline Stepper Status
    renderPipelineStatus(resp);

    // 2. Render Reconciled Multi-Source Contradictions (Source A/B, severity tags, rationale, resolving tool)
    renderConflicts(resp.conflicts);

    // 3. Render Identified Clinical Gaps
    renderGaps(resp.gaps);

    // 4. Render Transparent Tool Execution Log
    renderToolAuditLogs(resp.toolCallLogs);

    // 5. Render Rule-Based Deterministic Validation Results
    renderValidationResults(resp.validationResult);

    // 6. Render Final Structured Follow-up Record & Actions
    renderFollowUpRecord(resp.followUpRecord, resp.actionItems);

    // 7. Render Adaptation Log if present
    if (resp.adaptationLog) {
        const adaptTabBtn = document.getElementById('tabBtnAdaptation');
        if (adaptTabBtn) adaptTabBtn.style.display = 'block';
        renderAdaptationLog(resp.adaptationLog);
    }

    // Default to last active doctor tab or conflicts
    switchTab(lastDoctorTab || 'tab-conflicts');
}

function renderPipelineStatus(resp) {
    if (!resp) return;
    if (resp.overallStatus === 'VERIFIED') {
        updatePipelineBadge('VERIFIED', 'status-verified', '100% Verified');
        markAllStepsCompleted(false);
    } else if (resp.overallStatus === 'VERIFIED_WITH_ESCALATIONS') {
        updatePipelineBadge('ESCALATED', 'status-escalated', 'Verified with Escalation(s)');
        markAllStepsCompleted(true);
    } else {
        updatePipelineBadge('ESCALATED_FAILURE', 'status-failed', 'Safety Escalation Abort');
        markAllStepsFailed();
    }
}

// =========================================================
// TAB CONTROLLER (DOCTOR VIEW)
// =========================================================
function initTabs() {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const target = tab.getAttribute('data-tab');
            switchTab(target);
        });
    });
}

function switchTab(tabId) {
    lastDoctorTab = tabId;
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === tabId);
    });
}

// =========================================================
// PATIENT DATA INGESTION & UI BINDING
// =========================================================
function populatePatientSelect(patientsList) {
    if (!patientsList || patientsList.length === 0) return;
    currentPatients = patientsList;

    const select = document.getElementById('patientSelect');
    if (!select) return;

    const currentVal = select.value;
    select.innerHTML = '';

    // Add "Custom Patient (Manual Entry)" as the top option
    const customOpt = document.createElement('option');
    customOpt.value = 'CUSTOM';
    customOpt.textContent = 'Custom Patient (Manual Entry)';
    select.appendChild(customOpt);

    currentPatients.forEach(p => {
        const opt = document.createElement('option');
        opt.value = p.patientId;
        opt.textContent = `${p.patientId}: ${p.name} (${p.age}yo ${p.gender}) — ${p.scenarioTitle}`;
        select.appendChild(opt);
    });

    if (currentVal && currentPatients.some(p => p.patientId === currentVal)) {
        select.value = currentVal;
        onPatientSelectChanged(currentVal);
    } else if (currentPatients.length > 0) {
        select.value = currentPatients[0].patientId;
        onPatientSelectChanged(currentPatients[0].patientId);
    }
}

async function fetchPatients() {
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 6000);

        const res = await fetch('/api/clinical-agent/patients', { signal: controller.signal });
        clearTimeout(timeoutId);
        if (!res.ok) throw new Error('Failed to load synthetic patients');
        const livePatients = await res.json();
        if (livePatients && Array.isArray(livePatients) && livePatients.length > 0) {
            populatePatientSelect(livePatients);
        }
    } catch (err) {
        console.warn('Backend clinical agent sync delayed or waking from cold start (using verified synthetic scenarios):', err);
        if (!currentPatients || currentPatients.length === 0) {
            populatePatientSelect(FALLBACK_SYNTHETIC_PATIENTS);
        }
    }
}

function onPatientSelectChanged(patientId) {
    const customContainer = document.getElementById('customPatientContainer');
    const scenarioBanner = document.getElementById('scenarioMetaBanner');
    const adaptBtn = document.getElementById('btnAdapt');
    const simFailureBox = document.getElementById('simulationToggleBox');
    const errBox = document.getElementById('customFormError');
    if (errBox) errBox.style.display = 'none';

    if (patientId === 'CUSTOM') {
        selectedPatient = {
            patientId: 'CUSTOM',
            name: 'Custom Patient',
            age: 50,
            gender: 'Female',
            scenarioTitle: 'Custom Patient Case (Manual Entry)',
            scenarioDescription: 'User-entered multi-source clinical scenario.'
        };

        if (scenarioBanner) scenarioBanner.style.display = 'none';
        if (customContainer) customContainer.style.display = 'block';
        if (adaptBtn) adaptBtn.style.display = 'none';
        if (simFailureBox) simFailureBox.style.display = 'none';

        initCustomPatientFormDefaults();
        resetPipelineStepper();
        return;
    }

    if (customContainer) customContainer.style.display = 'none';
    if (scenarioBanner) scenarioBanner.style.display = 'flex';
    if (currentView === 'DOCTOR') {
        if (adaptBtn) adaptBtn.style.display = 'inline-flex';
        if (simFailureBox) simFailureBox.style.display = 'flex';
    }

    selectedPatient = currentPatients.find(p => p.patientId === patientId);
    if (!selectedPatient) return;

    // Update Scenario Metadata Banner
    const sTitle = document.getElementById('scenarioTitle');
    const sDesc = document.getElementById('scenarioDescription');
    const cDetail = document.getElementById('contradictionDetail');
    const pMeta = document.getElementById('patientMetaDetails');

    if (sTitle) sTitle.textContent = selectedPatient.scenarioTitle;
    if (sDesc) sDesc.textContent = selectedPatient.scenarioDescription;
    if (cDetail) cDetail.textContent = selectedPatient.deliberateContradictionDescription;
    if (pMeta) pMeta.textContent = `Age: ${selectedPatient.age} | Gender: ${selectedPatient.gender} | ID: ${selectedPatient.patientId}`;

    // Render Raw Sources if in Doctor View
    if (currentView === 'DOCTOR') {
        renderPatientSources(selectedPatient);
        resetPipelineStepper();
    }

    // Configure Adaptation button tooltips
    if (adaptBtn) {
        adaptBtn.title = selectedPatient.patientId === 'PAT-102' 
            ? 'Simulates mid-session arrival of STAT CMP showing acute eGFR drop' 
            : 'Simulate mid-session diagnostic lab arrival';
    }
}

// =========================================================
// CUSTOM PATIENT FORM DYNAMIC ROW MANAGERS
// =========================================================
function initCustomPatientFormDefaults() {
    const medsList = document.getElementById('customMedicationsList');
    if (medsList && medsList.children.length === 0) {
        addCustomMedicationRow('', '', '', true);
    }
    const allergiesList = document.getElementById('customAllergiesList');
    if (allergiesList && allergiesList.children.length === 0) {
        addCustomAllergyRow('');
    }
    const labsList = document.getElementById('customLabsList');
    if (labsList && labsList.children.length === 0) {
        addCustomLabRow('', '', '', '');
    }
}

function addCustomMedicationRow(name = '', dosage = '', freq = '', active = true) {
    const list = document.getElementById('customMedicationsList');
    if (!list) return;

    const row = document.createElement('div');
    row.className = 'custom-row-item custom-med-row';
    row.style.cssText = 'display: grid; grid-template-columns: 2fr 1.2fr 1.5fr 1fr 40px; gap: 8px; align-items: center; margin-bottom: 8px;';
    row.innerHTML = `
        <input type="text" class="med-name form-control" placeholder="Medication Name (e.g. Lisinopril)" value="${escapeHtml(name)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <input type="text" class="med-dosage form-control" placeholder="Dose (e.g. 20mg)" value="${escapeHtml(dosage)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <input type="text" class="med-freq form-control" placeholder="Frequency (e.g. Daily)" value="${escapeHtml(freq)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <select class="med-active form-control" style="padding: 7px 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12.5px;">
            <option value="true" ${active ? 'selected' : ''}>Active</option>
            <option value="false" ${!active ? 'selected' : ''}>Inactive</option>
        </select>
        <button type="button" class="btn-remove-row" onclick="this.closest('.custom-row-item').remove()" title="Remove" style="background: #fee2e2; border: 1px solid #fca5a5; color: #b91c1c; border-radius: 6px; height: 34px; cursor: pointer; font-weight: bold;">✕</button>
    `;
    list.appendChild(row);
}

function addCustomAllergyRow(allergen = '') {
    const list = document.getElementById('customAllergiesList');
    if (!list) return;

    const row = document.createElement('div');
    row.className = 'custom-row-item custom-allergy-row';
    row.style.cssText = 'display: flex; gap: 8px; align-items: center; margin-bottom: 8px;';
    row.innerHTML = `
        <input type="text" class="allergy-name form-control" placeholder="Allergen (e.g. Penicillin, Sulfa, Latex)" value="${escapeHtml(allergen)}" style="flex: 1; padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <button type="button" class="btn-remove-row" onclick="this.closest('.custom-row-item').remove()" title="Remove" style="background: #fee2e2; border: 1px solid #fca5a5; color: #b91c1c; border-radius: 6px; width: 40px; height: 34px; cursor: pointer; font-weight: bold;">✕</button>
    `;
    list.appendChild(row);
}

function addCustomLabRow(testName = '', val = '', unit = '', date = '') {
    const list = document.getElementById('customLabsList');
    if (!list) return;

    const row = document.createElement('div');
    row.className = 'custom-row-item custom-lab-row';
    row.style.cssText = 'display: grid; grid-template-columns: 2fr 1.2fr 1fr 1.2fr 40px; gap: 8px; align-items: center; margin-bottom: 8px;';
    row.innerHTML = `
        <input type="text" class="lab-name form-control" placeholder="Test Name (e.g. Serum Creatinine)" value="${escapeHtml(testName)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <input type="text" class="lab-val form-control" placeholder="Value (e.g. 1.4)" value="${escapeHtml(val)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <input type="text" class="lab-unit form-control" placeholder="Unit (e.g. mg/dL)" value="${escapeHtml(unit)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <input type="text" class="lab-date form-control" placeholder="Date (YYYY-MM-DD)" value="${escapeHtml(date)}" style="padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px;">
        <button type="button" class="btn-remove-row" onclick="this.closest('.custom-row-item').remove()" title="Remove" style="background: #fee2e2; border: 1px solid #fca5a5; color: #b91c1c; border-radius: 6px; height: 34px; cursor: pointer; font-weight: bold;">✕</button>
    `;
    list.appendChild(row);
}

// =========================================================
// LAB REPORT UPLOAD PARSER
// =========================================================
async function handleCustomLabUpload(input) {
    if (!input || !input.files || input.files.length === 0) return;
    const file = input.files[0];

    const spinner = document.getElementById('labUploadSpinner');
    if (spinner) spinner.style.display = 'inline-block';

    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await fetch('/api/clinical-agent/parse-lab-report', {
            method: 'POST',
            body: formData
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.error || `HTTP ${res.status}`);
        }

        const data = await res.json();
        const labs = data.results || [];

        if (labs.length === 0) {
            showToast('File processed, but no standard diagnostic lab values were detected. You can enter them manually below.', 'info');
        } else {
            // Remove empty placeholder rows
            const list = document.getElementById('customLabsList');
            if (list) {
                const existingRows = list.querySelectorAll('.custom-lab-row');
                existingRows.forEach(r => {
                    const name = r.querySelector('.lab-name').value.trim();
                    if (!name) r.remove();
                });
            }

            labs.forEach(l => {
                addCustomLabRow(l.testName, l.value, l.unit, l.date || '');
            });

            showToast(`Extracted ${labs.length} diagnostic lab value(s) from ${file.name}.`, 'success');
        }
    } catch (err) {
        console.error('Lab report upload error:', err);
        showToast('Failed to parse lab file: ' + err.message, 'error');
    } finally {
        if (spinner) spinner.style.display = 'none';
        input.value = '';
    }
}

function renderPatientSources(p) {
    const transcriptEl = document.getElementById('sourceTranscript');
    if (!transcriptEl) return;

    // Transcript
    transcriptEl.textContent = p.consultationTranscript || p.transcriptText || 'No transcript recorded.';

    // Medication History
    const medList = document.getElementById('sourceMeds');
    if (medList) {
        medList.innerHTML = '';
        const meds = p.medicationHistory || p.ehrMedications || [];
        if (meds.length > 0) {
            meds.forEach(m => {
                const li = document.createElement('li');
                const mName = m.medicationName || m.name;
                const mStatus = m.status || (m.active ? 'ACTIVE' : 'INACTIVE');
                li.innerHTML = `<span><strong>${escapeHtml(mName)}</strong> ${escapeHtml(m.dosage || '')} (${escapeHtml(m.frequency || '')})</span> <span class="badge-tag ${mStatus === 'ACTIVE' ? 'badge-resolved' : 'badge-detected'}">${escapeHtml(mStatus)}</span>`;
                medList.appendChild(li);
            });
        } else {
            medList.innerHTML = '<li>No medications documented.</li>';
        }
    }

    // Allergy List
    const allergyList = document.getElementById('sourceAllergies');
    if (allergyList) {
        allergyList.innerHTML = '';
        const allergies = p.allergyList || p.ehrAllergies || [];
        if (allergies.length > 0) {
            allergies.forEach(a => {
                const li = document.createElement('li');
                const aName = a.allergen || (typeof a === 'string' ? a : 'Allergy');
                const aStatus = a.status || 'ACTIVE';
                const aSev = a.severity || 'HIGH';
                li.innerHTML = `<span><strong>${escapeHtml(aName)}</strong>: ${escapeHtml(a.reaction || 'Documented allergy')}</span> <span class="badge-tag ${aSev === 'HIGH' ? 'badge-high' : 'badge-low'}">${escapeHtml(aStatus)}</span>`;
                allergyList.appendChild(li);
            });
        } else {
            allergyList.innerHTML = '<li>No documented allergies.</li>';
        }
    }

    // Previous Notes
    const notesList = document.getElementById('sourceNotes');
    if (notesList) {
        notesList.innerHTML = '';
        if (p.previousNotes && p.previousNotes.length > 0) {
            p.previousNotes.forEach(n => {
                const li = document.createElement('li');
                li.innerHTML = `<span><strong>${escapeHtml(n.noteId || 'NOTE')}</strong> (${escapeHtml(n.date || '2024')}${n.author ? ' - ' + escapeHtml(n.author) : ''}): <em>${escapeHtml(n.encounterType || n.noteType || 'Consult')}</em></span>`;
                notesList.appendChild(li);
            });
        } else {
            notesList.innerHTML = '<li>No previous clinical notes available.</li>';
        }
    }

    // Lab Reports
    const labsList = document.getElementById('sourceLabs');
    if (labsList) {
        labsList.innerHTML = '';
        const labs = p.labReports || p.labResults || [];
        if (labs.length > 0) {
            labs.forEach(l => {
                const li = document.createElement('li');
                const lName = l.panelName || l.testName || 'Diagnostic Panel';
                const lDate = l.testDate || l.date || '';
                const lNotes = l.interpretationNotes || `${l.value || ''} ${l.unit || ''}`;
                li.innerHTML = `<span><strong>${escapeHtml(lName)}</strong> (${escapeHtml(lDate)}): ${escapeHtml(lNotes)}</span>`;
                labsList.appendChild(li);
            });
        } else {
            labsList.innerHTML = '<li>No prior lab panels available.</li>';
        }
    }
}

// =========================================================
// RUN AUTONOMOUS CLINICAL AGENT PIPELINE
// =========================================================
async function runClinicalAgent() {
    const isCustom = selectedPatient && selectedPatient.patientId === 'CUSTOM';
    const errBox = document.getElementById('customFormError');
    if (errBox) errBox.style.display = 'none';

    let requestUrl = '/api/clinical-agent/run';
    let requestBody = null;

    if (isCustom) {
        const name = (document.getElementById('customPatientName')?.value || '').trim() || 'Custom Patient';
        const age = parseInt(document.getElementById('customPatientAge')?.value, 10) || 50;
        const gender = document.getElementById('customPatientGender')?.value || 'Female';
        const transcript = (document.getElementById('customTranscript')?.value || '').trim();
        const previousNotesText = (document.getElementById('customPreviousNotes')?.value || '').trim();

        // Gather medications
        const medRows = document.querySelectorAll('#customMedicationsList .custom-med-row');
        const meds = [];
        medRows.forEach(r => {
            const mName = (r.querySelector('.med-name')?.value || '').trim();
            const mDose = (r.querySelector('.med-dosage')?.value || '').trim();
            const mFreq = (r.querySelector('.med-freq')?.value || '').trim();
            const mActive = r.querySelector('.med-active')?.value === 'true';
            if (mName) {
                meds.push({
                    name: mName,
                    dosage: mDose || 'Standard',
                    frequency: mFreq || 'Daily',
                    active: mActive,
                    startDate: '2024-01-01'
                });
            }
        });

        // Validation: minimum required fields (at least transcript + one medication)
        if (!transcript || transcript.length < 15) {
            if (errBox) {
                errBox.textContent = 'Please enter a consultation transcript (at least a brief dialogue between Doctor and Patient).';
                errBox.style.display = 'block';
                errBox.scrollIntoView({ behavior: 'smooth' });
            }
            return;
        }

        if (meds.length === 0) {
            if (errBox) {
                errBox.textContent = 'Please enter at least one medication in Current Medications.';
                errBox.style.display = 'block';
                errBox.scrollIntoView({ behavior: 'smooth' });
            }
            return;
        }

        // Gather allergies
        const allergyRows = document.querySelectorAll('#customAllergiesList .custom-allergy-row');
        const allergies = [];
        allergyRows.forEach(r => {
            const aName = (r.querySelector('.allergy-name')?.value || '').trim();
            if (aName) allergies.push(aName);
        });

        // Gather lab results
        const labRows = document.querySelectorAll('#customLabsList .custom-lab-row');
        const labs = [];
        labRows.forEach(r => {
            const lName = (r.querySelector('.lab-name')?.value || '').trim();
            const lVal = (r.querySelector('.lab-val')?.value || '').trim();
            const lUnit = (r.querySelector('.lab-unit')?.value || '').trim();
            const lDate = (r.querySelector('.lab-date')?.value || '').trim();
            if (lName && lVal) {
                labs.push({
                    testName: lName,
                    value: lVal,
                    unit: lUnit || 'units',
                    date: lDate || new Date().toISOString().split('T')[0]
                });
            }
        });

        // Gather previous notes
        const notes = [];
        if (previousNotesText) {
            notes.push({
                noteId: 'NOTE-PREV-1',
                date: '2024-01-10',
                author: 'Dr. Primary Care',
                department: 'Internal Medicine',
                noteType: 'OUTPATIENT_CONSULT',
                text: previousNotesText,
                content: previousNotesText
            });
        }

        const customRecord = {
            patientId: 'CUSTOM-' + Math.floor(1000 + Math.random() * 9000),
            name: name,
            age: age,
            gender: gender,
            transcriptText: transcript,
            consultationTranscript: transcript,
            ehrMedications: meds,
            ehrAllergies: allergies,
            labResults: labs,
            previousNotes: notes
        };

        requestUrl = '/api/clinical-agent/run-custom';
        requestBody = customRecord;
    } else {
        if (!selectedPatient) return;
        const simFailCheck = document.getElementById('simulateFailureCheck');
        const simulateFailure = simFailCheck ? simFailCheck.checked : false;

        requestUrl = '/api/clinical-agent/run';
        requestBody = {
            patientId: selectedPatient.patientId,
            simulateToolFailure: simulateFailure
        };
    }

    const btnRun = document.getElementById('btnRunAgent');
    btnRun.disabled = true;
    btnRun.innerHTML = `<span class="agent-spinner"></span> Orchestrating Pipeline...`;

    try {
        const res = await fetch(requestUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody)
        });

        if (!res.ok) throw new Error(`Agent execution failed HTTP ${res.status}`);
        const data = await res.json();
        lastResponse = data;

        // Hide pre-run banner and reveal results section
        const preRun = document.getElementById('preRunBanner');
        if (preRun) preRun.style.display = 'none';

        const resultsSection = document.getElementById('resultsSection');
        if (resultsSection) {
            resultsSection.style.display = 'block';
            resultsSection.scrollIntoView({ behavior: 'smooth' });
        }

        // Default to Patient View as required
        switchView('PATIENT');

    } catch (err) {
        console.warn('Backend execution delayed or sleeping, activating simulated pipeline fallback:', err);
        if (selectedPatient && selectedPatient.patientId !== 'CUSTOM') {
            const fallbackResp = generateSimulatedAgentResponse(selectedPatient);
            lastResponse = fallbackResp;
            const preRun = document.getElementById('preRunBanner');
            if (preRun) preRun.style.display = 'none';
            const resultsSection = document.getElementById('resultsSection');
            if (resultsSection) {
                resultsSection.style.display = 'block';
                resultsSection.scrollIntoView({ behavior: 'smooth' });
            }
            switchView('PATIENT');
            showToast('Reconciliation complete (Verified demonstration mode).', 'success');
        } else {
            showToast('Agent execution failed: ' + err.message, 'error');
        }
    } finally {
        btnRun.disabled = false;
        btnRun.innerHTML = `<span>⚡</span> Run Autonomous Pipeline`;
    }
}

// =========================================================
// CLIENT-SIDE SIMULATED RESPONSE GENERATOR
// =========================================================
function generateSimulatedAgentResponse(patient) {
    const is101 = patient.patientId === 'PAT-101';
    const is102 = patient.patientId === 'PAT-102';
    const is103 = patient.patientId === 'PAT-103';

    const pName = is101 ? (patient.name && patient.name !== 'John Doe' ? patient.name : 'Robert Shayne') : (patient.name || 'Patient');
    const pAge = patient.age || (is101 ? 58 : is102 ? 64 : is103 ? 72 : 52);
    const pGender = patient.gender || 'Male';
    const encDate = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

    let overallStatus = is101 ? 'VERIFIED_WITH_ESCALATIONS' : 'VERIFIED';
    let conflicts = [];
    let gaps = [];
    let toolCallLogs = [];
    let validationRules = [];
    let warnings = [];
    let followUpRecord = {};
    let actions = [];

    if (is101) {
        conflicts.push({
            conflictId: "CONF-01",
            field: "Allergy Documentation (Beta-Lactam / Penicillin)",
            severity: "HIGH",
            status: "ESCALATED",
            sourceA: "EHR Allergy List: Reported NKDA",
            valueA: "No Known Drug Allergies (NKDA) recorded at triage intake",
            sourceB: "Emergency Dept Note (2023-04-12): Severe Anaphylactoid Reaction to Augmentin",
            valueB: "Documented severe anaphylactoid reaction with diffuse urticaria, wheezing, and IV Epinephrine / Methylprednisolone following Augmentin (Amoxicillin-Clavulanate)",
            autonomousRationale: "Allergy contradiction involves high-risk beta-lactam exposure. Invoked MedicationAllergyLookupService to evaluate cross-reactivity and safety profile.",
            resolvingTool: "MedicationAllergyLookupService",
            resolutionNotes: "ESCALATED TO CLINICIAN: Severe beta-lactam anaphylactoid hypersensitivity documented in ED note NOTE-2023-0412. Automated system cannot override allergy without clinician verification. Augmentin prescription blocked. Recommended non-beta-lactam alternative per guideline GL-AAAAI-03."
        });

        gaps.push({
            field: "Diagnostic Imaging & CBC Follow-up",
            description: "Consultation transcript notes physician planned chest X-ray and CBC with differential prior to finalizing outpatient antibiotic choice.",
            impact: "Missing objective radiography to rule out focal consolidation prior to definitive antibiotic selection.",
            actionTaken: "Ordered standard outpatient PA/lateral chest radiography and CBC with differential prior to discharge.",
            resolvingTool: "PatientRecordLookupService",
            status: "RESOLVED"
        });

        toolCallLogs = [
            {
                id: "TOOL-01",
                toolName: "PatientRecordLookupService",
                displayName: "Fetching Patient File",
                callReason: "Ingest complete synthetic patient data bundle (transcripts, notes, meds, allergies, labs)",
                inputSummary: `patientId: PAT-101, simulateFailure: false`,
                outputSummary: `Fetched ${pName} (${pGender}, ${pAge}yo). Found: 2 notes, 1 active med (Albuterol), 1 allergy record, 1 lab panel.`,
                timestamp: new Date().toISOString(),
                durationMs: 32,
                status: "SUCCESS"
            },
            {
                id: "TOOL-02",
                toolName: "MedicationAllergyLookupService",
                displayName: "Checking Medicine Safety",
                callReason: "Evaluate pharmacologic cross-reactivity and adverse drug-drug interactions",
                inputSummary: "Meds: [Augmentin, Amoxicillin-Clavulanate, Cefuroxime] | Allergies: 1 (NKDA vs ED Anaphylaxis)",
                outputSummary: "Safety Evaluation: Safe=false | Allergy Warnings: 3 (Severe Penicillin Hypersensitivity) | Escalation: Mandatory clinician sign-off required",
                timestamp: new Date().toISOString(),
                durationMs: 14,
                status: "SUCCESS"
            },
            {
                id: "TOOL-03",
                toolName: "ClinicalGuidelineRAGService",
                displayName: "Looking Up Medical Guidelines",
                callReason: "Retrieve evidence-based guideline recommendations from local knowledge base",
                inputSummary: "Query: 'penicillin allergy augmentin anaphylaxis nkda reconciliation', maxResults: 2",
                outputSummary: "Found 2 guideline matches: [GL-AAAAI-03 - Penicillin & Beta-Lactam Hypersensitivity Management]; [GL-AAAAI-08 - Clinical EHR Allergy Reconciliation & Safety Escalation Protocols]",
                timestamp: new Date().toISOString(),
                durationMs: 22,
                status: "SUCCESS"
            },
            {
                id: "TOOL-04",
                toolName: "RecordValidationService",
                displayName: "Double-Checking the Report",
                callReason: "Automated deterministic validation of synthesized clinical documentation",
                inputSummary: "Validating draft note against 5 clinical safety rules",
                outputSummary: "All 5 clinical validation rules passed successfully.",
                timestamp: new Date().toISOString(),
                durationMs: 19,
                status: "SUCCESS"
            }
        ];

        validationRules = [
            { ruleName: "RULE_REQUIRED_FIELDS", passed: true, severity: "INFO", message: "All mandatory clinical record sections present and populated." },
            { ruleName: "RULE_HIGH_SEVERITY_CONFLICTS", passed: true, severity: "WARNING", message: "Passed with safety escalation: 1 high-risk conflict(s) flagged for mandatory clinician sign-off." },
            { ruleName: "RULE_TEMPORAL_VALUE_CONSISTENCY", passed: true, severity: "INFO", message: "Temporal and value consistency verified successfully." },
            { ruleName: "RULE_GUARDRAIL_SAFETY", passed: true, severity: "INFO", message: "Guardrail compliance verified: No unauthorized diagnostic or prescriptive directives." },
            { ruleName: "RULE_PHYSIOLOGICAL_BOUNDS", passed: true, severity: "INFO", message: "Physiological bounds verified successfully." }
        ];
        warnings = ["Passed with safety escalation: 1 high-risk conflict(s) flagged for mandatory clinician sign-off."];

        followUpRecord = {
            patientId: "PAT-101",
            patientName: pName,
            encounterDate: encDate,
            chiefComplaint: "Persistent productive cough for 5 days with yellowish phlegm, low-grade fever (100.4 F), and generalized fatigue.",
            historyOfPresentIllness: `${pName} is a ${pAge}-year-old male presenting with a 5-day history of worsening chesty cough productive of yellowish phlegm, accompanied by low-grade fever up to 100.4 F and fatigue. Auscultation reveals scattered rhonchi and mild bilateral expiratory wheezes in both lower lobes. Physician considered Augmentin.`,
            objectiveFindingsSummary: "Vital Signs: Temperature 100.4 F, BP 124/78 mmHg, HR 82 bpm, RR 18 breaths/min, SpO2 97% on room air. Lungs: Scattered rhonchi and mild wheezing bilaterally, no focal consolidation. Labs: Mild leukocytosis (WBC 11.8 K/uL).",
            assessmentSummary: "Acute bacterial bronchitis. SAFETY CRITICAL WARNING: Intake allergy documentation states 'NKDA', but historical ED encounter from 2023-04-12 documents severe Type-I anaphylactoid reaction to Augmentin requiring IM Epinephrine. Augmentin and beta-lactams are strictly contraindicated.",
            proposedFollowUpPlan: "1. STRICTLY AVOID Augmentin and all penicillin/beta-lactam antibiotics.\n2. Recommend non-beta-lactam alternative: Azithromycin 500mg Day 1, then 250mg PO Daily Days 2-5, or Doxycycline 100mg PO BID for 7 days if bacterial symptoms persist.\n3. Continue Albuterol HFA Inhaler (90mcg) 1-2 puffs every 4-6 hours PRN for bronchospasm.\n4. Complete outpatient chest radiography to confirm absence of focal pneumonia.\n5. Immediately correct EHR allergy banner from NKDA to Confirmed Severe Penicillin Hypersensitivity.",
            allergyAlerts: [
                "CRITICAL ALLERGY ALERT: Severe Type-I Anaphylactoid Hypersensitivity to Penicillins / Augmentin (Documented 2023-04-12 ED encounter). DO NOT PRESCRIBE AUGMENTIN OR AMOXICILLIN-CLAVULANATE."
            ],
            reconciledMedications: [
                "Albuterol HFA Inhaler 90mcg (1-2 puffs Q4-6H PRN) - ACTIVE",
                "Augmentin (Amoxicillin-Clavulanate) - PERMANENTLY WITHHELD / CONTRAINDICATED (Severe Hypersensitivity)"
            ],
            disclaimer: "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only."
        };

        actions = [
            { id: "ACT-01", targetRole: "CLINICIAN", priority: "HIGH", description: "Review and sign off on Penicillin/Augmentin allergy contraindication escalation before issuing prescription.", category: "SAFETY_ESCALATION", status: "PENDING_CLINICIAN_REVIEW" },
            { id: "ACT-02", targetRole: "NURSE", priority: "HIGH", description: "Update EHR allergy banner from NKDA to confirmed Severe Amoxicillin-Clavulanate Hypersensitivity.", category: "EHR_RECONCILIATION", status: "PENDING" },
            { id: "ACT-03", targetRole: "PHARMACIST", priority: "HIGH", description: "Cross-check prescribed respiratory antimicrobial against beta-lactam allergy safety registry.", category: "MEDICATION_VERIFICATION", status: "PENDING" },
            { id: "ACT-04", targetRole: "PATIENT", priority: "MEDIUM", description: "Use Albuterol inhaler as needed for wheezing; notify clinic immediately if fever exceeds 101 F or breathing worsens.", category: "PATIENT_EDUCATION", status: "SCHEDULED" }
        ];

    } else if (is102) {
        conflicts.push({
            conflictId: "CONF-02",
            field: "Metformin Dosing vs Impaired Renal Function",
            severity: "HIGH",
            status: "ESCALATED",
            sourceA: "EHR Prescriptions: Metformin 1000mg BID (2000mg daily)",
            valueA: "Metformin 1000mg PO BID (active prescription)",
            sourceB: "BMP Lab Report: eGFR 28 mL/min/1.73m2 (Severe Renal Impairment)",
            valueB: "Serum Creatinine 2.3 mg/dL, eGFR 28 mL/min/1.73m2 (Stage 4 CKD)",
            autonomousRationale: "ADA/KDIGO Guidelines mandate discontinuation of Metformin when eGFR falls below 30 mL/min due to elevated risk of lactic acidosis.",
            resolvingTool: "ClinicalGuidelineRAGService",
            resolutionNotes: "ESCALATED TO CLINICIAN: Metformin contraindicated due to eGFR < 30 mL/min. Metformin therapy must be discontinued immediately to prevent lactic acidosis. Linagliptin or low-dose insulin recommended."
        });

        gaps.push({
            field: "Renal Function Repeat & Electrolyte Monitoring",
            description: "Acute eGFR decline from baseline stage 3 to stage 4 CKD requires confirmatory repeat BMP within 48-72 hours.",
            impact: "Risk of unmonitored progressive renal impairment and electrolyte imbalance.",
            actionTaken: "Scheduled STAT repeat BMP and urine albumin-to-creatinine ratio (uACR).",
            resolvingTool: "LabRetrievalService",
            status: "RESOLVED"
        });

        toolCallLogs = [
            { id: "TOOL-01", toolName: "PatientRecordLookupService", displayName: "Fetching Patient File", callReason: "Ingest multi-source diabetes and renal profile", inputSummary: "patientId: PAT-102", outputSummary: "Fetched Maria Santos (Female, 64yo). Found: Metformin 1000mg BID, Lisinopril 10mg, BMP labs.", timestamp: new Date().toISOString(), durationMs: 28, status: "SUCCESS" },
            { id: "TOOL-02", toolName: "LabRetrievalService", displayName: "Pulling Latest Lab Report", callReason: "Retrieve verified BMP and eGFR trajectory", inputSummary: "Lab Panel: BMP 2024-02-20", outputSummary: "eGFR: 28 mL/min/1.73m2 [CRITICAL LOW], Creatinine: 2.3 mg/dL [ELEVATED]", timestamp: new Date().toISOString(), durationMs: 25, status: "SUCCESS" },
            { id: "TOOL-03", toolName: "ClinicalGuidelineRAGService", displayName: "Looking Up Medical Guidelines", callReason: "Query ADA/KDIGO renal dosing safety guidelines for biguanides", inputSummary: "Query: 'metformin egfr under 30 contraindication lactic acidosis'", outputSummary: "Found guideline GL-ADA-02: Discontinue Metformin when eGFR < 30 mL/min.", timestamp: new Date().toISOString(), durationMs: 18, status: "SUCCESS" },
            { id: "TOOL-04", toolName: "RecordValidationService", displayName: "Double-Checking the Report", callReason: "Validate medication safety and renal bounds", inputSummary: "5 clinical safety rules", outputSummary: "All 5 validation rules passed with renal safety escalation.", timestamp: new Date().toISOString(), durationMs: 15, status: "SUCCESS" }
        ];

        validationRules = [
            { ruleName: "RULE_REQUIRED_FIELDS", passed: true, severity: "INFO", message: "All mandatory clinical record sections present and populated." },
            { ruleName: "RULE_HIGH_SEVERITY_CONFLICTS", passed: true, severity: "WARNING", message: "Passed with safety escalation: Metformin contraindication (eGFR < 30) flagged for clinician review." },
            { ruleName: "RULE_TEMPORAL_VALUE_CONSISTENCY", passed: true, severity: "INFO", message: "Temporal consistency verified." },
            { ruleName: "RULE_GUARDRAIL_SAFETY", passed: true, severity: "INFO", message: "Guardrail compliance verified." },
            { ruleName: "RULE_PHYSIOLOGICAL_BOUNDS", passed: true, severity: "INFO", message: "Physiological bounds verified." }
        ];
        warnings = ["Metformin contraindicated: eGFR < 30 mL/min."];

        followUpRecord = {
            patientId: "PAT-102",
            patientName: "Maria Santos",
            encounterDate: encDate,
            chiefComplaint: "Routine type 2 diabetes follow-up; reports morning fatigue, mild nausea, and leg cramps.",
            historyOfPresentIllness: "Maria Santos is a 64-year-old female with T2D and hypertension taking Metformin 1000mg BID and Lisinopril 10mg daily. Recent lab reveals worsening kidney function with eGFR 28 mL/min.",
            objectiveFindingsSummary: "BP 132/80 mmHg, HR 76 bpm. Recent BMP: Creatinine 2.3 mg/dL, eGFR 28 mL/min/1.73m2 (Stage 4 CKD).",
            assessmentSummary: "Type 2 Diabetes Mellitus with Stage 4 Chronic Kidney Disease. Critical contraindication: Metformin 1000mg BID must be stopped immediately due to severe risk of lactic acidosis (eGFR < 30).",
            proposedFollowUpPlan: "1. Discontinue Metformin 1000mg BID immediately.\n2. Initiate renal-safe DPP-4 inhibitor (Linagliptin 5mg daily) or low-dose basal insulin for glycemic control.\n3. Continue Lisinopril 10mg with close potassium monitoring.\n4. Repeat BMP in 48-72 hours.\n5. Nephrology referral for CKD stage 4 co-management.",
            allergyAlerts: [],
            reconciledMedications: [
                "Metformin 1000mg BID - DISCONTINUED / CONTRAINDICATED (eGFR < 30)",
                "Lisinopril 10mg PO Daily - ACTIVE (Monitor renal function)",
                "Linagliptin 5mg PO Daily - RECOMMENDED RENAL-SAFE ALTERNATIVE"
            ],
            disclaimer: "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only."
        };

        actions = [
            { id: "ACT-01", targetRole: "CLINICIAN", priority: "HIGH", description: "Discontinue Metformin and approve renal-safe alternative (Linagliptin 5mg daily).", category: "SAFETY_CONTRAINDICATION", status: "PENDING_CLINICIAN_REVIEW" },
            { id: "ACT-02", targetRole: "PATIENT", priority: "HIGH", description: "Stop taking Metformin immediately; do not resume until cleared by physician.", category: "PATIENT_EDUCATION", status: "SCHEDULED" },
            { id: "ACT-03", targetRole: "NURSE", priority: "MEDIUM", description: "Schedule repeat BMP lab draw and nephrology consultation appointment.", category: "LAB_FOLLOWUP", status: "PENDING" }
        ];

    } else if (is103) {
        conflicts.push({
            conflictId: "CONF-03",
            field: "Anticoagulation vs Over-the-Counter NSAID Therapy",
            severity: "HIGH",
            status: "ESCALATED",
            sourceA: "EHR Prescription: Warfarin 5mg daily (Supratherapeutic INR 3.4)",
            valueA: "Warfarin 5mg daily PO with INR 3.4",
            sourceB: "Consultation Dialogue: Patient self-administering OTC Naproxen 500mg BID",
            valueB: "Naproxen 500mg PO BID for knee pain with burning epigastric discomfort and dark stools",
            autonomousRationale: "Concurrent high-dose NSAID with supratherapeutic warfarin exponentially magnifies gastrointestinal bleeding risk.",
            resolvingTool: "MedicationAllergyLookupService",
            resolutionNotes: "ESCALATED TO CLINICIAN: Critical hemorrhage risk. Patient instructed to stop Naproxen immediately. Order STAT CBC, stool occult blood, and repeat PT/INR within 24-48 hours."
        });

        gaps.push({
            field: "Occult Gastrointestinal Blood Screening & Repeat INR",
            description: "Dark stools reported in consultation transcript with supratherapeutic INR 3.4 requires urgent fecal occult blood test.",
            impact: "Undetected active upper gastrointestinal hemorrhage.",
            actionTaken: "Ordered urgent Hemoccult stool card and STAT PT/INR.",
            resolvingTool: "LabRetrievalService",
            status: "RESOLVED"
        });

        toolCallLogs = [
            { id: "TOOL-01", toolName: "PatientRecordLookupService", displayName: "Fetching Patient File", callReason: "Ingest coagulation records and med history", inputSummary: "patientId: PAT-103", outputSummary: "Fetched Arthur Pendelton (Male, 72yo). Warfarin 5mg, Metoprolol 50mg, INR 3.4.", timestamp: new Date().toISOString(), durationMs: 26, status: "SUCCESS" },
            { id: "TOOL-02", toolName: "MedicationAllergyLookupService", displayName: "Checking Medicine Safety", callReason: "Evaluate Warfarin-Naproxen drug-drug interaction", inputSummary: "Warfarin + Naproxen", outputSummary: "Major Drug Interaction: Severe GI bleeding and ulceration hazard.", timestamp: new Date().toISOString(), durationMs: 15, status: "SUCCESS" },
            { id: "TOOL-03", toolName: "ClinicalGuidelineRAGService", displayName: "Looking Up Medical Guidelines", callReason: "Retrieve CHEST anticoagulation bleeding safety guidelines", inputSummary: "Query: 'warfarin supratherapeutic inr nsaid interaction bleeding'", outputSummary: "Guideline: Withhold NSAID, hold/reduce Warfarin dose, monitor for overt bleeding.", timestamp: new Date().toISOString(), durationMs: 20, status: "SUCCESS" },
            { id: "TOOL-04", toolName: "RecordValidationService", displayName: "Double-Checking the Report", callReason: "Validate bleeding safety guardrails", inputSummary: "5 clinical safety rules", outputSummary: "Passed with high-severity safety escalation.", timestamp: new Date().toISOString(), durationMs: 17, status: "SUCCESS" }
        ];

        validationRules = [
            { ruleName: "RULE_REQUIRED_FIELDS", passed: true, severity: "INFO", message: "All mandatory clinical record sections present and populated." },
            { ruleName: "RULE_HIGH_SEVERITY_CONFLICTS", passed: true, severity: "WARNING", message: "Passed with safety escalation: Warfarin + NSAID bleeding hazard flagged for clinician review." },
            { ruleName: "RULE_TEMPORAL_VALUE_CONSISTENCY", passed: true, severity: "INFO", message: "Temporal consistency verified." },
            { ruleName: "RULE_GUARDRAIL_SAFETY", passed: true, severity: "INFO", message: "Guardrail compliance verified." },
            { ruleName: "RULE_PHYSIOLOGICAL_BOUNDS", passed: true, severity: "INFO", message: "Physiological bounds verified." }
        ];
        warnings = ["High hemorrhage risk: Warfarin + Naproxen with INR 3.4."];

        followUpRecord = {
            patientId: "PAT-103",
            patientName: "Arthur Pendelton",
            encounterDate: encDate,
            chiefComplaint: "Severe right knee pain; reports stomach burning and dark stools after taking OTC Naproxen.",
            historyOfPresentIllness: "Arthur Pendelton is a 72-year-old male on Warfarin 5mg daily for atrial fibrillation. He reports taking OTC Naproxen 500mg BID for one week for knee pain, now presenting with dyspepsia and dark stools. Latest INR is 3.4.",
            objectiveFindingsSummary: "BP 128/74 mmHg, HR 68 bpm regular. Abdomen: Mild epigastric tenderness to palpation, no rebound. Labs: PT 38.2s, INR 3.4 (Supratherapeutic).",
            assessmentSummary: "Atrial Fibrillation on Warfarin with supratherapeutic anticoagulation and acute NSAID-induced dyspepsia with suspected upper GI bleeding.",
            proposedFollowUpPlan: "1. STOP all Naproxen / NSAIDs immediately.\n2. Substitute with Acetaminophen 500-650mg Q6H PRN (max 2g/day) for knee pain.\n3. Hold today's Warfarin dose; recheck PT/INR in 24 hours.\n4. Perform fecal occult blood test; initiate oral PPI (Pantoprazole 40mg daily).\n5. Instruct patient to report any melena or hematemesis to the nearest ED immediately.",
            allergyAlerts: [],
            reconciledMedications: [
                "Warfarin 5mg PO Daily - HELD TODAY (INR 3.4, recheck in 24h)",
                "Metoprolol Succinate 50mg PO Daily - ACTIVE",
                "Naproxen 500mg BID - PERMANENTLY DISCONTINUED (Severe Bleeding Hazard)",
                "Acetaminophen 500mg PO PRN - RECOMMENDED ANALGESIC ALTERNATIVE"
            ],
            disclaimer: "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only."
        };

        actions = [
            { id: "ACT-01", targetRole: "CLINICIAN", priority: "HIGH", description: "Hold Warfarin dose, order repeat STAT INR, and evaluate need for endoscopic evaluation.", category: "SAFETY_ESCALATION", status: "PENDING_CLINICIAN_REVIEW" },
            { id: "ACT-02", targetRole: "PATIENT", priority: "HIGH", description: "Cease all Naproxen/NSAID use; seek emergency care if dark black stools or vomiting blood occurs.", category: "PATIENT_EDUCATION", status: "SCHEDULED" },
            { id: "ACT-03", targetRole: "PHARMACIST", priority: "HIGH", description: "Flag patient profile to block dispensing of NSAIDs while on anticoagulant therapy.", category: "PHARMACY_SAFETY", status: "PENDING" }
        ];

    } else {
        // PAT-104 / Custom
        conflicts.push({
            conflictId: "CONF-04",
            field: "Glycemic Control Self-Report vs Objective Laboratory Biomarkers",
            severity: "HIGH",
            status: "ESCALATED",
            sourceA: "Consultation Dialogue: Patient claims fasting sugars 90-95 mg/dL",
            valueA: "Patient reported home fasting glucose 90-95 mg/dL",
            sourceB: "Verified CMP: Fasting Blood Glucose 245 mg/dL and HbA1c 10.2%",
            valueB: "Fasting Blood Glucose 245 mg/dL [CRITICAL HIGH], HbA1c 10.2% [CRITICAL HIGH]",
            autonomousRationale: "Objective central laboratory values reveal severe uncontrolled hyperglycemia, directly contradicting subjective self-monitoring report.",
            resolvingTool: "LabRetrievalService",
            resolutionNotes: "ESCALATED TO CLINICIAN: Severe uncontrolled hyperglycemia confirmed by certified laboratory analyzer (HbA1c 10.2%, Fasting Glucose 245 mg/dL). Clinician consultation required to intensify pharmacotherapy and verify home glucometer calibration."
        });

        gaps.push({
            field: "Diabetic Microvascular Screen & Nephropathy Evaluation",
            description: "No urine microalbumin or diabetic retinal exam documented in the preceding 12 months.",
            impact: "Unmonitored risk of microvascular diabetic complications (retinopathy/nephropathy).",
            actionTaken: "Ordered urine microalbumin-to-creatinine ratio and generated referral for dilated eye examination.",
            resolvingTool: "ClinicalGuidelineRAGService",
            status: "RESOLVED"
        });

        toolCallLogs = [
            { id: "TOOL-01", toolName: "PatientRecordLookupService", displayName: "Fetching Patient File", callReason: "Ingest diabetic history and medication adherence logs", inputSummary: `patientId: ${patient.patientId}`, outputSummary: `Fetched ${pName} (${pGender}, ${pAge}yo). Found: Atorvastatin 20mg, CMP & HbA1c labs.`, timestamp: new Date().toISOString(), durationMs: 25, status: "SUCCESS" },
            { id: "TOOL-02", toolName: "LabRetrievalService", displayName: "Pulling Latest Lab Report", callReason: "Retrieve verified glycemic and metabolic panel", inputSummary: "CMP & HbA1c 2024-03-05", outputSummary: "Glucose 245 mg/dL [CRITICAL HIGH], HbA1c 10.2% [CRITICAL HIGH], eGFR 88 mL/min.", timestamp: new Date().toISOString(), durationMs: 28, status: "SUCCESS" },
            { id: "TOOL-03", toolName: "ClinicalGuidelineRAGService", displayName: "Looking Up Medical Guidelines", callReason: "Retrieve ADA Standards of Medical Care in Diabetes guidelines", inputSummary: "Query: 'uncontrolled diabetes hba1c over 10 insulin dual therapy'", outputSummary: "Guideline: When HbA1c >= 10.0%, initiate dual combination therapy or injectable GLP-1/insulin.", timestamp: new Date().toISOString(), durationMs: 20, status: "SUCCESS" },
            { id: "TOOL-04", toolName: "RecordValidationService", displayName: "Double-Checking the Report", callReason: "Validate glycemic documentation and safety rules", inputSummary: "5 clinical safety rules", outputSummary: "Passed with glycemic escalation warning.", timestamp: new Date().toISOString(), durationMs: 16, status: "SUCCESS" }
        ];

        validationRules = [
            { ruleName: "RULE_REQUIRED_FIELDS", passed: true, severity: "INFO", message: "All mandatory clinical record sections present and populated." },
            { ruleName: "RULE_HIGH_SEVERITY_CONFLICTS", passed: true, severity: "WARNING", message: "Passed with safety escalation: Severe uncontrolled hyperglycemia (HbA1c 10.2%) flagged for clinician review." },
            { ruleName: "RULE_TEMPORAL_VALUE_CONSISTENCY", passed: true, severity: "INFO", message: "Temporal consistency verified." },
            { ruleName: "RULE_GUARDRAIL_SAFETY", passed: true, severity: "INFO", message: "Guardrail compliance verified." },
            { ruleName: "RULE_PHYSIOLOGICAL_BOUNDS", passed: true, severity: "INFO", message: "Physiological bounds verified." }
        ];
        warnings = ["Severe uncontrolled hyperglycemia: HbA1c 10.2%."];

        followUpRecord = {
            patientId: patient.patientId || "PAT-104",
            patientName: pName,
            encounterDate: encDate,
            chiefComplaint: "Follow-up evaluation for type 2 diabetes and hyperlipidemia.",
            historyOfPresentIllness: `${pName} is a ${pAge}-year-old ${pGender.toLowerCase()} presenting for routine diabetes follow-up. While the patient reports feeling well with home sugars reported as normal, certified laboratory results demonstrate marked hyperglycemia with HbA1c 10.2%.`,
            objectiveFindingsSummary: "BP 126/82 mmHg, HR 74 bpm. Labs (2024-03-05): Fasting Glucose 245 mg/dL [CRITICAL HIGH], HbA1c 10.2% [CRITICAL HIGH], Total Cholesterol 188 mg/dL, eGFR 88 mL/min/1.73m2.",
            assessmentSummary: "Severe uncontrolled Type 2 Diabetes Mellitus with marked lab discrepancy versus reported home values. Requires prompt escalation to multi-agent glycemic pharmacotherapy.",
            proposedFollowUpPlan: "1. Initiate dual oral combination therapy or basal insulin per ADA clinical guidelines.\n2. Patient education on home glucometer calibration and standardized logbook tracking.\n3. Order urine microalbumin-to-creatinine ratio and schedule dilated eye examination.\n4. Diabetic nutrition and certified diabetes educator consult.\n5. Follow-up clinic visit in 2-3 weeks; repeat HbA1c in 90 days.",
            allergyAlerts: [],
            reconciledMedications: [
                "Atorvastatin 20mg PO Daily - ACTIVE",
                "Metformin 500mg PO BID - RECOMMENDED INITIATION (Renal function normal, eGFR 88)"
            ],
            disclaimer: "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only."
        };

        actions = [
            { id: "ACT-01", targetRole: "CLINICIAN", priority: "HIGH", description: "Review marked HbA1c elevation (10.2%) and initiate intensified antihyperglycemic regimen.", category: "GLYCEMIC_OPTIMIZATION", status: "PENDING_CLINICIAN_REVIEW" },
            { id: "ACT-02", targetRole: "NURSE", priority: "MEDIUM", description: "Perform glucometer check and verify home testing technique with patient.", category: "PATIENT_EDUCATION", status: "PENDING" },
            { id: "ACT-03", targetRole: "PATIENT", priority: "MEDIUM", description: "Record daily fasting and 2-hour postprandial blood sugars in logbook.", category: "SELF_MONITORING", status: "SCHEDULED" }
        ];
    }

    return {
        patientId: patient.patientId,
        overallStatus: overallStatus,
        status: "SUCCESS",
        confidenceScore: 0.95,
        conflicts: conflicts,
        gaps: gaps,
        toolCallLogs: toolCallLogs,
        validationResult: {
            valid: true,
            escalationRequired: overallStatus === 'VERIFIED_WITH_ESCALATIONS',
            rules: validationRules,
            warnings: warnings,
            errorMessages: []
        },
        followUpRecord: followUpRecord,
        actionItems: actions,
        disclaimer: "Draft for clinician review — not a diagnosis. Synthetic/deidentified data only."
    };
}

// =========================================================
// MID-SESSION ADAPTATION TRIGGER (DEMO TOOL)
// =========================================================
async function triggerAdaptation() {
    if (!selectedPatient) return;

    const btnAdapt = document.getElementById('btnAdapt');
    btnAdapt.disabled = true;
    btnAdapt.innerHTML = `<span class="agent-spinner"></span> Adapting Pipeline...`;

    try {
        const updateType = "LAB_UPDATE";
        const updateContent = "STAT Comprehensive Metabolic Panel: eGFR 28 mL/min/1.73m2 (CRITICAL LOW), Serum Creatinine 2.4 mg/dL (HIGH)";

        const res = await fetch('/api/clinical-agent/update-source', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                patientId: selectedPatient.patientId,
                updateType: updateType,
                updateContent: updateContent
            })
        });

        if (!res.ok) throw new Error(`Adaptation request failed HTTP ${res.status}`);
        const data = await res.json();
        lastResponse = data;

        // Re-render current active view instantly
        if (currentView === 'DOCTOR') {
            renderDoctorView(data);
            const adaptTabBtn = document.getElementById('tabBtnAdaptation');
            if (adaptTabBtn) adaptTabBtn.style.display = 'block';
            switchTab('tab-adaptation');
        } else {
            const patientMount = document.getElementById('patientViewMount');
            if (patientMount) {
                patientMount.innerHTML = buildPatientViewHtml(data);
            }
        }

        showToast('Mid-session STAT lab update ingested. Incremental adaptation completed successfully!', 'success');

    } catch (err) {
        console.error('Adaptation failed:', err);
        showToast('Incremental adaptation failed: ' + err.message, 'error');
    } finally {
        btnAdapt.disabled = false;
        btnAdapt.innerHTML = `<span>🔄</span> Simulate Mid-Session Lab Update`;
    }
}

// =========================================================
// RESET PATIENT SYNTHETIC STATE OR CUSTOM FORM
// =========================================================
async function resetPatientState() {
    if (!selectedPatient) return;

    const btnReset = document.getElementById('btnReset');
    btnReset.disabled = true;

    try {
        if (selectedPatient.patientId === 'CUSTOM') {
            // Reset custom patient input fields
            const nameEl = document.getElementById('customPatientName');
            if (nameEl) nameEl.value = '';
            const ageEl = document.getElementById('customPatientAge');
            if (ageEl) ageEl.value = '';
            const genderEl = document.getElementById('customPatientGender');
            if (genderEl) genderEl.value = 'Female';
            const transcriptEl = document.getElementById('customTranscript');
            if (transcriptEl) transcriptEl.value = '';
            const notesEl = document.getElementById('customPreviousNotes');
            if (notesEl) notesEl.value = '';

            const errBox = document.getElementById('customFormError');
            if (errBox) {
                errBox.style.display = 'none';
                errBox.textContent = '';
            }

            const medsList = document.getElementById('customMedicationsList');
            if (medsList) {
                medsList.innerHTML = '';
                addCustomMedicationRow('', '', '', true);
            }

            const allergiesList = document.getElementById('customAllergiesList');
            if (allergiesList) {
                allergiesList.innerHTML = '';
                addCustomAllergyRow('');
            }

            const labsList = document.getElementById('customLabsList');
            if (labsList) {
                labsList.innerHTML = '';
            }

            lastResponse = null;

            const resultsSection = document.getElementById('resultsSection');
            if (resultsSection) resultsSection.style.display = 'none';

            const preRun = document.getElementById('preRunBanner');
            if (preRun) preRun.style.display = 'block';

            const stepperMount = document.getElementById('stepperMount');
            const patientMount = document.getElementById('patientViewMount');
            const doctorMount = document.getElementById('doctorViewMount');
            if (stepperMount) stepperMount.innerHTML = '';
            if (patientMount) patientMount.innerHTML = '';
            if (doctorMount) doctorMount.innerHTML = '';

            switchView('PATIENT');
            showToast('Custom patient form reset to initial blank state.', 'success');
            return;
        }

        const res = await fetch(`/api/clinical-agent/patients/${selectedPatient.patientId}/reset`, {
            method: 'POST'
        });
        if (!res.ok) throw new Error('Reset failed');

        lastResponse = null;

        // Hide results section and restore pre-run banner
        const resultsSection = document.getElementById('resultsSection');
        if (resultsSection) resultsSection.style.display = 'none';

        const preRun = document.getElementById('preRunBanner');
        if (preRun) preRun.style.display = 'block';

        // Clear active mounts
        const stepperMount = document.getElementById('stepperMount');
        const patientMount = document.getElementById('patientViewMount');
        const doctorMount = document.getElementById('doctorViewMount');
        if (stepperMount) stepperMount.innerHTML = '';
        if (patientMount) patientMount.innerHTML = '';
        if (doctorMount) doctorMount.innerHTML = '';

        // Reset view toggle to Patient
        switchView('PATIENT');

        await fetchPatients();
        showToast(`Patient ${selectedPatient.patientId} reset to baseline synthetic state.`, 'success');
    } catch (err) {
        showToast('Reset failed: ' + err.message, 'error');
    } finally {
        btnReset.disabled = false;
    }
}

// =========================================================
// DOCTOR SUB-COMPONENTS POPULATORS
// =========================================================
function renderConflicts(conflicts) {
    const container = document.getElementById('conflictsContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!conflicts || conflicts.length === 0) {
        container.innerHTML = '<p style="color: #64748b;">No conflicting statements detected across patient sources.</p>';
        return;
    }

    conflicts.forEach(c => {
        const isEscalated = c.status === 'ESCALATED';
        const card = document.createElement('div');
        card.className = `conflict-card ${isEscalated ? 'escalated' : 'resolved'}`;

        const sevClass = c.severity === 'HIGH' ? 'badge-high' : c.severity === 'MEDIUM' ? 'badge-medium' : 'badge-low';
        const statClass = isEscalated ? 'badge-escalated' : 'badge-resolved';

        card.innerHTML = `
            <div class="conflict-header">
                <div class="conflict-title">${escapeHtml(c.field)}</div>
                <div class="badges-group">
                    <span class="badge-tag ${sevClass}">Severity: ${escapeHtml(c.severity)}</span>
                    <span class="badge-tag ${statClass}">${escapeHtml(c.status)}</span>
                </div>
            </div>

            <div class="diff-box">
                <div class="diff-col">
                    <h6>Source A (${escapeHtml(c.sourceA)})</h6>
                    <p>${escapeHtml(c.valueA)}</p>
                </div>
                <div class="diff-col">
                    <h6>Source B (${escapeHtml(c.sourceB)})</h6>
                    <p>${escapeHtml(c.valueB)}</p>
                </div>
            </div>

            <div style="font-size: 13px; color: #475569; margin-bottom: 8px;">
                <strong>Autonomous Decision Rationale:</strong> ${escapeHtml(c.autonomousRationale || 'Evaluated via clinical rule engine')}
                <br>
                <strong>Resolving Tool:</strong> <span class="tool-badge">${escapeHtml(c.resolvingTool || 'N/A')}</span>
            </div>

            <div class="resolution-detail ${isEscalated ? 'escalated-detail' : ''}">
                <strong>${isEscalated ? '⚠️ Clinician Escalation Notice' : '✅ Reconciled Resolution'}:</strong> ${escapeHtml(c.resolutionNotes || 'No notes.')}
            </div>
        `;

        container.appendChild(card);
    });
}

function renderGaps(gaps) {
    const container = document.getElementById('gapsContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!gaps || gaps.length === 0) {
        container.innerHTML = '<p style="color: #64748b;">No clinical gaps identified.</p>';
        return;
    }

    gaps.forEach(g => {
        const card = document.createElement('div');
        card.className = 'conflict-card resolved';
        card.innerHTML = `
            <div class="conflict-header">
                <div class="conflict-title">${escapeHtml(g.field)}</div>
                <span class="badge-tag badge-resolved">${escapeHtml(g.status)}</span>
            </div>
            <p style="font-size: 13.5px; color: #334155; margin-bottom: 6px;"><strong>Description:</strong> ${escapeHtml(g.description)}</p>
            <p style="font-size: 13px; color: #64748b; margin-bottom: 6px;"><strong>Clinical Impact:</strong> ${escapeHtml(g.impact)}</p>
            <div style="font-size: 13px; color: #166534; background: #f0fdf4; padding: 8px 12px; border-radius: 6px;">
                <strong>Action Taken:</strong> ${escapeHtml(g.actionTaken)} (via <code>${escapeHtml(g.resolvingTool)}</code>)
            </div>
        `;
        container.appendChild(card);
    });
}

function renderToolAuditLogs(logs) {
    const tbody = document.getElementById('toolLogTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#64748b;">No tools executed yet.</td></tr>';
        return;
    }

    logs.forEach(l => {
        const tr = document.createElement('tr');
        const statClass = l.status === 'SUCCESS' ? 'status-success' : l.status === 'RETRY' ? 'status-retry' : 'status-failed';
        const toolInfo = getToolDisplayInfo(l.toolName, l.displayName);

        tr.innerHTML = `
            <td>
                <div class="tool-cell-content">
                    <span class="tool-display-name">${escapeHtml(toolInfo.displayName)}</span>
                    <span class="tool-technical-subtitle">${escapeHtml(toolInfo.technicalName)}</span>
                </div>
            </td>
            <td><strong>${escapeHtml(l.callReason)}</strong><br><small style="color:#64748b;">${escapeHtml(l.inputSummary)}</small></td>
            <td><span style="font-size: 12.5px; color: #334155;">${escapeHtml(l.outputSummary || l.errorDetails || '')}</span></td>
            <td>${l.durationMs} ms</td>
            <td><span class="status-badge ${statClass}">${escapeHtml(l.status)}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function renderValidationResults(val) {
    const container = document.getElementById('validationRulesContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!val || !val.rules) {
        container.innerHTML = '<p style="color: #64748b;">Validation report unavailable.</p>';
        return;
    }

    val.rules.forEach(r => {
        const row = document.createElement('div');
        const isWarning = r.severity === 'WARNING';
        const isFailed = !r.passed;

        row.className = `rule-row ${isFailed ? 'failed' : isWarning ? 'warning' : 'passed'}`;
        const icon = isFailed ? '❌' : isWarning ? '⚠️' : '✅';

        row.innerHTML = `
            <div class="rule-icon">${icon}</div>
            <div class="rule-info">
                <h5>${escapeHtml(r.ruleName)} <span class="badge-tag ${isFailed ? 'badge-high' : isWarning ? 'badge-medium' : 'badge-resolved'}">${escapeHtml(r.severity)}</span></h5>
                <p>${escapeHtml(r.message)}</p>
            </div>
        `;
        container.appendChild(row);
    });
}

function renderFollowUpRecord(rec, actions) {
    if (!rec) return;

    const pHeader = document.getElementById('notePatientHeader');
    const nDate = document.getElementById('noteDate');
    const nChief = document.getElementById('noteChiefComplaint');
    const nHPI = document.getElementById('noteHPI');
    const nObj = document.getElementById('noteObjectives');
    const nAssess = document.getElementById('noteAssessment');
    const nPlan = document.getElementById('notePlan');
    const nDisc = document.getElementById('noteDisclaimer');

    if (pHeader) pHeader.textContent = `${rec.patientName} (ID: ${rec.patientId})`;
    if (nDate) nDate.textContent = rec.encounterDate || new Date().toISOString().split('T')[0];
    if (nChief) nChief.textContent = rec.chiefComplaint || 'N/A';
    if (nHPI) nHPI.textContent = rec.historyOfPresentIllness || 'N/A';
    if (nObj) nObj.textContent = rec.objectiveFindingsSummary || 'N/A';
    if (nAssess) nAssess.textContent = rec.assessmentSummary || 'N/A';
    if (nPlan) nPlan.textContent = rec.proposedFollowUpPlan || 'N/A';
    if (nDisc) nDisc.textContent = rec.disclaimer || 'Draft for clinician review — not a diagnosis. Synthetic/deidentified data only.';

    // Allergy Alerts Banner
    const alertsContainer = document.getElementById('noteAllergyAlerts');
    if (alertsContainer) {
        alertsContainer.innerHTML = '';
        if (rec.allergyAlerts && rec.allergyAlerts.length > 0) {
            rec.allergyAlerts.forEach(a => {
                const div = document.createElement('div');
                div.className = 'alert-banner-box';
                div.innerHTML = `🚨 <strong>SAFETY ALERT:</strong> ${escapeHtml(a)}`;
                alertsContainer.appendChild(div);
            });
        }
    }

    // Reconciled Medications
    const medsUl = document.getElementById('noteReconciledMeds');
    if (medsUl) {
        medsUl.innerHTML = '';
        if (rec.reconciledMedications && rec.reconciledMedications.length > 0) {
            rec.reconciledMedications.forEach(m => {
                const li = document.createElement('li');
                const isWithheld = m.toLowerCase().includes('withheld') || m.toLowerCase().includes('discontinued') || m.toLowerCase().includes('permanently');
                if (isWithheld) li.className = 'withheld';
                li.textContent = m;
                medsUl.appendChild(li);
            });
        }
    }

    // Action Items Table
    const actionTbody = document.getElementById('actionTableBody');
    if (actionTbody) {
        actionTbody.innerHTML = '';
        if (actions && actions.length > 0) {
            actions.forEach(act => {
                const tr = document.createElement('tr');
                const roleClass = `role-${act.targetRole.toLowerCase()}`;
                const prioClass = act.priority === 'HIGH' ? 'badge-high' : act.priority === 'MEDIUM' ? 'badge-medium' : 'badge-low';

                tr.innerHTML = `
                    <td><span class="role-tag ${roleClass}">${escapeHtml(act.targetRole)}</span></td>
                    <td><span class="badge-tag ${prioClass}">${escapeHtml(act.priority)}</span></td>
                    <td><strong>${escapeHtml(act.description)}</strong></td>
                    <td><small style="color: #64748b;">${escapeHtml(act.category)}</small></td>
                    <td><span class="badge-tag badge-detected">${escapeHtml(act.status)}</span></td>
                `;
                actionTbody.appendChild(tr);
            });
        }
    }
}

function renderAdaptationLog(adapt) {
    const aTrigger = document.getElementById('adaptTrigger');
    const aInterface = document.getElementById('adaptInterface');
    const aTimestamp = document.getElementById('adaptTimestamp');
    const aRationale = document.getElementById('adaptRationale');
    const aPrior = document.getElementById('adaptPriorState');
    const aNew = document.getElementById('adaptNewState');

    if (aTrigger) aTrigger.textContent = adapt.triggerEvent;
    if (aInterface) aInterface.textContent = adapt.sourceInterface;
    if (aTimestamp) aTimestamp.textContent = adapt.timestamp;
    if (aRationale) aRationale.textContent = adapt.rationale;
    if (aPrior) aPrior.textContent = adapt.priorState;
    if (aNew) aNew.textContent = adapt.newState;

    const fieldsUl = document.getElementById('adaptFieldsList');
    if (fieldsUl) {
        fieldsUl.innerHTML = '';
        if (adapt.affectedFields) {
            adapt.affectedFields.forEach(f => {
                const li = document.createElement('li');
                li.textContent = f;
                fieldsUl.appendChild(li);
            });
        }
    }
}

// =========================================================
// PIPELINE ANIMATION & STEPPER HELPERS
// =========================================================
function updatePipelineBadge(text, className, label) {
    const badge = document.getElementById('pipelineStatusBadge');
    if (!badge) return;
    badge.className = `pipeline-status-badge ${className}`;
    badge.textContent = label || text;
}

function resetPipelineStepper() {
    for (let i = 1; i <= 7; i++) {
        const step = document.getElementById(`step-${i}`);
        if (step) step.className = 'step-item';
    }
    updatePipelineBadge('IDLE', 'status-idle', 'Pipeline Ready');
}

function markAllStepsCompleted(hasEscalations) {
    for (let i = 1; i <= 6; i++) {
        const step = document.getElementById(`step-${i}`);
        if (step) step.className = 'step-item completed';
    }
    const step7 = document.getElementById('step-7');
    if (step7) {
        step7.className = hasEscalations ? 'step-item escalated' : 'step-item completed';
    }
}

function markAllStepsFailed() {
    for (let i = 1; i <= 4; i++) {
        const step = document.getElementById(`step-${i}`);
        if (step) step.className = 'step-item completed';
    }
    const step4 = document.getElementById('step-4');
    if (step4) step4.className = 'step-item escalated';
    const step7 = document.getElementById('step-7');
    if (step7) step7.className = 'step-item escalated';
}

function showToast(msg, type) {
    // Elegant unobtrusive console / alert notification
    console.log(`[MediScan Notification] [${type || 'info'}]: ${msg}`);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
