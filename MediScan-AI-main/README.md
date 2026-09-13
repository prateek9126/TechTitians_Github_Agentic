# MediScan AI 🩺

MediScan AI is an AI-powered healthcare platform designed to simplify medical report analysis and improve access to healthcare services.

The platform allows users to upload medical reports such as Blood Reports, MRI, CT Scan, and X-Ray, receive AI-based insights, find suitable doctors, and book appointments online.

## 🚀 Features

- 🧬 AI Medical Report Analysis
- 🩸 Blood Report Interpretation
- 🧠 MRI, CT Scan & X-Ray Analysis
- 👨‍⚕️ Doctor Search by Location and Specialty
- 📅 Online Appointment Booking
- 🚨 Emergency Healthcare Support
- 🤖 AI Medical Assistant Interface
- 🔒 Secure Health Data Management

## 🛠️ Technologies Used

- HTML5
- CSS3
- JavaScript
- Responsive Web Design
- AI/ML Integration 

## 🎯 Objective

The goal of MediScan AI is to make healthcare more accessible by combining artificial intelligence with modern web technologies, helping users understand medical information and connect with healthcare professionals easily.

## 📌 Future Enhancements

- Real AI model integration for medical image analysis
- User authentication system
- Doctor dashboard
- Online consultation
- Health history tracking

## 👨‍💻 Developed By

**Priyanshu Kumar Singh**

---

## 🤖 Clinical Documentation & Follow-up Agent (Problem Statement 1)

The **Clinical Documentation & Follow-up Agent** is an autonomous module that converts simulated consultations into verified, guardrail-compliant follow-up records while reconciling conflicting patient information across multiple synthetic sources (consultation transcripts, EHR notes, medication histories, allergy lists, and diagnostic lab panels).

### 🌟 Key Capabilities
- **Multi-Source Reconciliation**: Automatically diffs unstructured dialogues against structured EHR records to detect contradictions.
- **Autonomous Tool Routing**: Intelligently selects tools based on conflict characteristics (`PatientRecordLookupService`, `MedicationAllergyLookupService`, `LabRetrievalService`, `ClinicalGuidelineRAGService`).
- **Deterministic 4-Rule Validation Engine**: Automated checks verify record completeness, zero unresolved high-severity conflicts, temporal/physiological bounds, and strict negative guardrails (forbids definitive diagnostic or prescriptive commands).
- **Incremental Mid-Session Adaptation**: Ingests new lab results (e.g. STAT CMP with critical eGFR decline) mid-session and re-evaluates only affected fields without restarting the workflow.
- **Graceful Failure Recovery**: Simulates transient tool outages (HTTP 504), executes an autonomous retry, and falls back to safety escalation rather than crashing or hallucinating.

---

### 🚀 Running the Clinical Agent Demo End-to-End

#### 1. Start the Spring Boot Application
```bash
./mvnw clean spring-boot:run
```
*(Or run `mvn spring-boot:run` or launch `MediscanApplication.java` in your IDE).*

#### 2. Open the Interactive Web Demo
Navigate your browser to:
```
http://localhost:8080/clinical-agent.html
```

#### 3. Test the Scenarios via UI
1. **Scenario 1 (Safety Escalation)**: Select **PAT-101 (Robert Shayne)**. Click **"Run Autonomous Pipeline"**.
   - Notice the deliberate contradiction: EHR allergy list states `NKDA`, but ED clinical notes document a severe anaphylactoid reaction to Augmentin.
   - The agent detects this, queries `MedicationAllergyLookupService` and `ClinicalGuidelineRAGService`, blocks the proposed Augmentin prescription, and **escalates** the high-severity conflict for mandatory clinician review.
2. **Scenario 2 (Mid-Session Adaptation)**: Select **PAT-102 (Maria Garcia)**. Click **"Run Autonomous Pipeline"**.
   - The agent reconciles Maria's verbal cessation of Metformin with her active EHR profile.
   - Click **"Simulate Mid-Session Lab Update"**: A STAT CMP posts showing an acute eGFR drop to 28 mL/min. The agent performs targeted incremental adaptation, marks Metformin as strictly contraindicated, and creates urgent nephrology follow-up actions.
3. **Scenario 3 (Failure Recovery)**: Check **"Simulate Tool Failure"** and run the agent.
   - The agent handles a simulated LIS timeout, attempts 1 autonomous retry, and safely escalates the item to clinician review.

#### 4. REST API Endpoints
- `GET /api/clinical-agent/patients` — List all synthetic patient bundles
- `GET /api/clinical-agent/patients/{id}` — Ingest patient bundle by ID
- `POST /api/clinical-agent/run` — Run the 7-step autonomous pipeline:
  ```json
  {
    "patientId": "PAT-101",
    "simulateToolFailure": false
  }
  ```
- `POST /api/clinical-agent/update-source` — Simulate mid-session data arrival and incremental adaptation:
  ```json
  {
    "patientId": "PAT-102",
    "updateType": "LAB_UPDATE",
    "updateContent": "STAT CMP: eGFR 28 mL/min, Creatinine 2.4 mg/dL"
  }
  ```
- `POST /api/clinical-agent/patients/{id}/reset` — Reset patient record to baseline state

---

### 📊 Sample Output from Synthetic Dataset

#### 1. Resolved Conflict Example (Patient PAT-102: Maria Garcia)
```json
{
  "id": "CONF-2",
  "field": "Medication Adherence (Metformin)",
  "sourceA": "Consultation Transcript",
  "valueA": "Patient reports stopped Metformin 3 weeks ago due to intolerable GI cramps and nausea",
  "sourceB": "EHR Medication Profile",
  "valueB": "Metformin 1000mg BID (Status: ACTIVE, Refilled recently)",
  "severity": "MEDIUM",
  "status": "RESOLVED",
  "resolvingTool": "ClinicalGuidelineRAGService",
  "autonomousRationale": "Medication cessation due to gastrointestinal adverse effects requires guideline-based reconciliation and secondary therapy options.",
  "resolutionNotes": "RESOLVED: Verbal cessation acknowledged and reconciled in draft. Metformin marked as discontinued by patient due to GI intolerance. Guideline ADA GL-ADA-02 cited: consider trial of extended-release or transition to SGLT2i/GLP-1 RA once renal panel is evaluated."
}
```

#### 2. Escalated Conflict Example (Patient PAT-101: Robert Shayne)
```json
{
  "id": "CONF-1",
  "field": "Allergy Documentation (Beta-Lactam / Penicillin)",
  "sourceA": "EHR Allergy Banner",
  "valueA": "No Known Drug Allergies (NKDA)",
  "sourceB": "ED Clinical Note (NOTE-2023-0412)",
  "valueB": "Documented severe anaphylactoid reaction with urticaria, wheezing, and IV Epinephrine to Augmentin",
  "severity": "HIGH",
  "status": "ESCALATED",
  "resolvingTool": "MedicationAllergyLookupService",
  "autonomousRationale": "Allergy contradiction involves potential beta-lactam exposure. Invoking MedicationAllergyLookupService to evaluate cross-reactivity and safety profile.",
  "resolutionNotes": "ESCALATED TO CLINICIAN: Severe beta-lactam anaphylaxis documented in ED note NOTE-2023-0412. Automated system cannot override allergy without clinician verification. Empiric Augmentin blocked. Recommended non-beta-lactam alternative per guideline GL-AAAAI-03."
}
```
