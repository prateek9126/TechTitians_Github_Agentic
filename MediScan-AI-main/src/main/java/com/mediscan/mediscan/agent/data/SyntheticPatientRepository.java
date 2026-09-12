package com.mediscan.mediscan.agent.data;

import com.mediscan.mediscan.agent.model.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository providing synthetic multi-source patient data bundles.
 * 
 * =========================================================================
 * ALL DATA IN THIS FILE IS SYNTHETIC, DE-IDENTIFIED, AND FICTITIOUS.
 * DESIGNED EXCLUSIVELY FOR CLINICAL AGENT TESTING AND DEMONSTRATION.
 * =========================================================================
 */
@Repository
public class SyntheticPatientRepository {

    private final Map<String, PatientSourceBundle> patientStore = new ConcurrentHashMap<>();
    private final Map<String, PatientSourceBundle> initialSnapshots = new ConcurrentHashMap<>();

    public SyntheticPatientRepository() {
        initSyntheticData();
    }

    private void initSyntheticData() {
        PatientSourceBundle p1 = createPatient101();
        PatientSourceBundle p2 = createPatient102();
        PatientSourceBundle p3 = createPatient103();
        PatientSourceBundle p4 = createPatient104();

        savePatient(p1);
        savePatient(p2);
        savePatient(p3);
        savePatient(p4);
    }

    private void savePatient(PatientSourceBundle p) {
        patientStore.put(p.getPatientId(), p);
        initialSnapshots.put(p.getPatientId(), cloneBundle(p));
    }

    public List<PatientSourceBundle> getAllPatients() {
        return new ArrayList<>(patientStore.values());
    }

    public Optional<PatientSourceBundle> getPatientById(String id) {
        return Optional.ofNullable(patientStore.get(id));
    }

    public void updatePatient(PatientSourceBundle bundle) {
        patientStore.put(bundle.getPatientId(), bundle);
    }

    public void resetPatient(String id) {
        if (initialSnapshots.containsKey(id)) {
            patientStore.put(id, cloneBundle(initialSnapshots.get(id)));
        }
    }

    /* =========================================================================
     * PATIENT 101: Robert Shayne - High Severity Allergy Contradiction
     * Contradiction: Allergy Record = NKDA vs Clinical Note = Severe Penicillin/Augmentin Reaction
     * Consultation: Physician considering Augmentin for bacterial bronchitis
     * ========================================================================= */
    private PatientSourceBundle createPatient101() {
        PatientSourceBundle p = new PatientSourceBundle();
        p.setPatientId("PAT-101");
        p.setName("Robert Shayne");
        p.setAge(58);
        p.setGender("Male");
        p.setScenarioTitle("Penicillin Hypersensitivity Contradiction (Safety Critical)");
        p.setScenarioDescription("Patient presenting with bronchitis symptoms. EHR allergy banner states NKDA, but prior ED discharge notes document severe anaphylactoid reaction to Augmentin.");
        p.setDeliberateContradictionDescription("EHR Allergy List shows 'NKDA' (No Known Drug Allergies), whereas ED clinical note from 2023-04-12 documents severe acute urticaria and wheezing following Amoxicillin-Clavulanate.");

        p.setConsultationTranscript("""
Dr. Robert Evans (PCP): Good morning, Robert. What brings you into the clinic today?
Robert Shayne: Good morning doctor. I've had this persistent chesty cough for the last 5 days with yellowish phlegm, a low-grade fever around 100.4 F, and feeling completely worn out.
Dr. Evans: Let me listen to your lungs. Deep breath in... and out. Yes, there are scattered rhonchi and mild wheezes in both lower lobes. Given the discolored sputum and fever duration, this looks consistent with acute acute bacterial bronchitis.
Robert Shayne: Is there an antibiotic that can clear this up quickly?
Dr. Evans: If symptoms persist past tomorrow or your fever climbs, I would consider prescribing a 7-day course of Augmentin (Amoxicillin-Clavulanate 875/125mg) or Cefuroxime. Have you had any issues with antibiotics recently?
Robert Shayne: Not that I recall recently, but my records should show whatever I took in the past.
Dr. Evans: Okay, your intake chart says No Known Drug Allergies (NKDA). Let's do a chest X-ray and basic blood work first before calling in the prescription. In the meantime, rest, stay hydrated, and use an albuterol inhaler PRN.
""");

        List<ClinicalNote> notes = new ArrayList<>();
        ClinicalNote n1 = new ClinicalNote(
                "NOTE-2023-0412",
                "2023-04-12",
                "Dr. Sarah Jenkins, MD",
                "Emergency Medicine",
                "EMERGENCY_ENCOUNTER",
                """
CHIEF COMPLAINT: Acute allergic reaction / rash.
HISTORY: 56-year-old male presented to Emergency Dept 35 minutes after taking his first dose of oral Augmentin (Amoxicillin-Clavulanate 875mg) prescribed for sinusitis. Developed acute diffuse pruritic urticarial wheals across torso and arms, lip swelling, and mild inspiratory stridor/dyspnea.
EXAM: BP 108/68, HR 112, RR 22, SpO2 94% on room air. Diffuse erythematous urticaria. Bilateral expiratory wheezing.
INTERVENTIONS: Administered IM Epinephrine 0.3mg, IV Diphenhydramine 50mg, IV Famotidine 20mg, and IV Methylprednisolone 125mg. Symptoms resolved over 4 hours of intensive observation.
IMPRESSION: Severe Type-I anaphylactoid hypersensitivity to Amoxicillin-Clavulanate (Penicillin class).
DISCHARGE INSTRUCTIONS: Strict avoidance of ALL penicillins and aminopenicillins. Formal allergy warning flagged. Patient instructed to obtain medical alert bracelet.
""",
                "BP 108/68, HR 112, SpO2 94%",
                List.of("Severe Penicillin Allergy (Amoxicillin-Clavulanate)", "Urticaria", "Wheezing"),
                List.of("Acute Allergic Reaction (T78.40XA)", "Acute Sinusitis (J01.90)")
        );
        notes.add(n1);

        ClinicalNote n2 = new ClinicalNote(
                "NOTE-2024-0110",
                "2024-01-10",
                "Nurse Triage Ingest",
                "Outpatient Internal Medicine",
                "TRIAGE_INTAKE",
                "Annual wellness review intake. Triage nurse recorded: Vitals stable. System review non-contributory. Allergy dropdown inadvertently checked as 'No Known Drug Allergies (NKDA)' due to template auto-population.",
                "BP 128/82, HR 74, SpO2 98%",
                List.of("NKDA"),
                List.of("Essential Hypertension (I10)", "Hyperlipidemia (E78.5)")
        );
        notes.add(n2);
        p.setPreviousNotes(notes);

        List<MedicationRecord> meds = new ArrayList<>();
        meds.add(new MedicationRecord("MED-101-1", "Lisinopril", "10mg", "Once daily", "Oral", "ACTIVE", "2022-06-15", "2024-02-01", "Dr. Robert Evans", "Essential Hypertension"));
        meds.add(new MedicationRecord("MED-101-2", "Atorvastatin", "20mg", "Once daily at bedtime", "Oral", "ACTIVE", "2022-06-15", "2024-02-01", "Dr. Robert Evans", "Hyperlipidemia"));
        p.setMedicationHistory(meds);

        List<AllergyRecord> allergies = new ArrayList<>();
        // Deliberate contradiction: Listed as NKDA in current EHR allergy table!
        allergies.add(new AllergyRecord("ALG-101-1", "No Known Drug Allergies (NKDA)", "DRUG", "None", "MILD", "REPORTED_NKDA", "2024-01-10"));
        p.setAllergyList(allergies);

        List<LabReportRecord> labs = new ArrayList<>();
        List<LabResultItem> cbc = new ArrayList<>();
        cbc.add(new LabResultItem("White Blood Cells (WBC)", "11.4", "K/uL", "4.5 - 11.0", "HIGH"));
        cbc.add(new LabResultItem("Hemoglobin", "14.2", "g/dL", "13.8 - 17.2", "NORMAL"));
        cbc.add(new LabResultItem("Platelets", "265", "K/uL", "150 - 450", "NORMAL"));
        labs.add(new LabReportRecord("LAB-101-A", "2024-03-01", "Complete Blood Count (CBC)", cbc, "Mild leukocytosis consistent with acute respiratory infection."));
        p.setLabReports(labs);

        return p;
    }

    /* =========================================================================
     * PATIENT 102: Maria Garcia - Verbal Cessation Contradiction & Mid-Session Lab Update
     * Contradiction: Transcript = Patient stopped Metformin 3 weeks ago vs EHR = Active Metformin 1000mg BID
     * Adaptation: Mid-session new BMP report posts with eGFR 28 mL/min (severe contraindication)
     * ========================================================================= */
    private PatientSourceBundle createPatient102() {
        PatientSourceBundle p = new PatientSourceBundle();
        p.setPatientId("PAT-102");
        p.setName("Maria Garcia");
        p.setAge(62);
        p.setGender("Female");
        p.setScenarioTitle("Medication Cessation Discrepancy & Mid-Session Acute Renal Adaptation");
        p.setScenarioDescription("Patient with Type 2 Diabetes reports self-discontinuing Metformin 3 weeks ago due to nausea/cramps, while EHR lists it as active. Features mid-session adaptation when new renal labs post.");
        p.setDeliberateContradictionDescription("Consultation transcript documents patient discontinued Metformin 3 weeks ago due to severe GI intolerance; EHR medication record still shows Metformin 1000mg BID as Active with recent refill.");

        p.setConsultationTranscript("""
Dr. Lisa Vance: Good afternoon, Maria. How have you been managing your blood sugar and medications since our last visit?
Maria Garcia: Good afternoon, Dr. Vance. Actually, I have a confession. I completely stopped taking my Metformin pills about three weeks ago. I just couldn't handle the terrible stomach cramps, bloating, and constant nausea anymore.
Dr. Vance: I'm sorry to hear that Maria. Did you start anything else, or have you been tracking your numbers?
Maria Garcia: No other pills. My fasting blood sugars on my home meter have climbed to 175-210 mg/dL. I've also felt unusually fatigued and had some lower back ache.
Dr. Vance: We definitely need to address your glycemic control without causing you gastrointestinal distress. Your chart still listed Metformin 1000mg twice daily as active. We need an updated Comprehensive Metabolic Panel today to check your kidney filtration and electrolytes before deciding on an alternative like an SGLT2 inhibitor or GLP-1 receptor agonist.
Maria Garcia: Okay, I gave a blood sample at the lab downstairs right before coming up to your office.
Dr. Vance: Great, those results should post to our system shortly.
""");

        List<ClinicalNote> notes = new ArrayList<>();
        ClinicalNote n1 = new ClinicalNote(
                "NOTE-2023-1115",
                "2023-11-15",
                "Dr. Lisa Vance, MD",
                "Endocrinology & Primary Care",
                "OUTPATIENT_CONSULT",
                "61-year-old female with Type 2 Diabetes Mellitus and mild hypertension. Baseline HbA1c 7.6%. Tolerating Metformin 1000mg BID with meals. Baseline serum creatinine 0.9 mg/dL, eGFR 74 mL/min/1.73m2. Encouraged lifestyle modifications.",
                "BP 132/80, HR 72, Weight 78 kg",
                List.of("Penicillin - mild rash in childhood"),
                List.of("Type 2 Diabetes Mellitus (E11.9)", "Hyperlipidemia (E78.5)")
        );
        notes.add(n1);
        p.setPreviousNotes(notes);

        List<MedicationRecord> meds = new ArrayList<>();
        meds.add(new MedicationRecord("MED-102-1", "Metformin", "1000mg", "Twice daily with meals", "Oral", "ACTIVE", "2023-05-10", "2024-02-10", "Dr. Lisa Vance", "Type 2 Diabetes Mellitus"));
        meds.add(new MedicationRecord("MED-102-2", "Amlodipine", "5mg", "Once daily", "Oral", "ACTIVE", "2023-05-10", "2024-02-10", "Dr. Lisa Vance", "Mild Hypertension"));
        p.setMedicationHistory(meds);

        List<AllergyRecord> allergies = new ArrayList<>();
        allergies.add(new AllergyRecord("ALG-102-1", "Penicillin", "DRUG", "Mild maculopapular rash (childhood)", "MILD", "ACTIVE", "2018-03-12"));
        p.setAllergyList(allergies);

        List<LabReportRecord> labs = new ArrayList<>();
        List<LabResultItem> baseline = new ArrayList<>();
        baseline.add(new LabResultItem("Fasting Blood Glucose", "182", "mg/dL", "70 - 99", "HIGH"));
        baseline.add(new LabResultItem("Hemoglobin A1c", "8.3", "%", "4.0 - 5.6", "HIGH"));
        baseline.add(new LabResultItem("Serum Creatinine", "1.0", "mg/dL", "0.6 - 1.1", "NORMAL"));
        baseline.add(new LabResultItem("eGFR (Baseline)", "72", "mL/min/1.73m2", "> 60", "NORMAL"));
        labs.add(new LabReportRecord("LAB-102-A", "2024-01-20", "Endocrine Baseline Panel", baseline, "Suboptimal glycemic control."));
        p.setLabReports(labs);

        return p;
    }

    /* =========================================================================
     * PATIENT 103: Robert Chen - Multi-Morbid Polypharmacy & Triad Interaction
     * Contradiction & Gaps: High-dose OTC Ibuprofen added to Lisinopril + Spironolactone
     * "Triple Whammy" acute kidney injury risk + borderline Hyperkalemia (Potassium 5.2)
     * ========================================================================= */
    private PatientSourceBundle createPatient103() {
        PatientSourceBundle p = new PatientSourceBundle();
        p.setPatientId("PAT-103");
        p.setName("Robert Chen");
        p.setAge(67);
        p.setGender("Male");
        p.setScenarioTitle("Polypharmacy Triad Interaction ('Triple Whammy') & Guideline Gap");
        p.setScenarioDescription("Patient self-initiating high-dose OTC Ibuprofen while prescribed Lisinopril and Spironolactone, presenting with worsening hypertension and borderline hyperkalemia.");
        p.setDeliberateContradictionDescription("Unmonitored OTC NSAID self-administration contradicts clinical guidelines for concurrent ACE-inhibitor and potassium-sparing diuretic therapy, creating acute nephrotoxicity and hyperkalemia risk.");

        p.setConsultationTranscript("""
Dr. Karen Miller: Hello Mr. Chen. I see your blood pressure reading from triage today was 158/96 mmHg, which is noticeably higher than your usual 128/80. How have you been feeling?
Robert Chen: Hello Dr. Miller. My knees have been giving me severe grief from osteoarthritis over the last 4 weeks. Walking upstairs has been excruciating. To cope, I've been taking over-the-counter Ibuprofen 600mg three or four times every day.
Dr. Karen Miller: That is a substantial dose of Ibuprofen, especially with your daily heart and blood pressure prescriptions. Have you noticed any swelling or changes in urination?
Robert Chen: Yes, my ankles have felt puffy by evening, and my shoes feel tighter. I didn't think OTC pain medicine could affect my blood pressure or kidneys.
Dr. Karen Miller: NSAIDs like Ibuprofen can constrict kidney blood flow, cause fluid retention, and dangerous interactions with your Lisinopril and Spironolactone. Let's inspect your latest lab numbers immediately.
""");

        List<ClinicalNote> notes = new ArrayList<>();
        ClinicalNote n1 = new ClinicalNote(
                "NOTE-2023-1005",
                "2023-10-05",
                "Dr. Karen Miller, MD",
                "Cardiology & Internal Medicine",
                "OUTPATIENT_CONSULT",
                "66-year-old male with Heart Failure with Preserved Ejection Fraction (HFpEF) and hypertension. Stable on Lisinopril 20mg daily and Spironolactone 25mg daily. Emphasized avoidance of NSAIDs due to hemodynamic renal risk.",
                "BP 126/78, HR 68, K+ 4.6 mEq/L",
                List.of("NKDA"),
                List.of("Hypertension (I10)", "HFpEF (I50.30)", "Osteoarthritis Knee (M17.0)")
        );
        notes.add(n1);
        p.setPreviousNotes(notes);

        List<MedicationRecord> meds = new ArrayList<>();
        meds.add(new MedicationRecord("MED-103-1", "Lisinopril", "20mg", "Once daily", "Oral", "ACTIVE", "2022-03-01", "2024-02-15", "Dr. Karen Miller", "Hypertension & Cardioprotection"));
        meds.add(new MedicationRecord("MED-103-2", "Spironolactone", "25mg", "Once daily in morning", "Oral", "ACTIVE", "2022-03-01", "2024-02-15", "Dr. Karen Miller", "Heart Failure / Mineralocorticoid antagonist"));
        p.setMedicationHistory(meds);

        List<AllergyRecord> allergies = new ArrayList<>();
        allergies.add(new AllergyRecord("ALG-103-1", "NKDA", "DRUG", "None", "MILD", "REPORTED_NKDA", "2023-10-05"));
        p.setAllergyList(allergies);

        List<LabReportRecord> labs = new ArrayList<>();
        List<LabResultItem> bmp = new ArrayList<>();
        bmp.add(new LabResultItem("Potassium", "5.2", "mEq/L", "3.5 - 5.0", "HIGH"));
        bmp.add(new LabResultItem("Serum Creatinine", "1.4", "mg/dL", "0.7 - 1.3", "HIGH"));
        bmp.add(new LabResultItem("Blood Urea Nitrogen (BUN)", "26", "mg/dL", "7 - 20", "HIGH"));
        bmp.add(new LabResultItem("Sodium", "138", "mEq/L", "135 - 145", "NORMAL"));
        bmp.add(new LabResultItem("eGFR", "52", "mL/min/1.73m2", "> 60", "LOW"));
        labs.add(new LabReportRecord("LAB-103-A", "2024-03-02", "Basic Metabolic Panel (BMP)", bmp, "Early acute kidney injury and borderline hyperkalemia likely secondary to triple hemodynamic interaction."));
        p.setLabReports(labs);

        return p;
    }

    /* =========================================================================
     * PATIENT 104: David Kim - Objective Glycemic Lab Contradiction
     * Contradiction: Transcript = Patient reports normal blood sugar / glucose fine at 95 mg/dL
     * vs EHR Lab = Fasting Glucose 245 mg/dL (CRITICAL) and HbA1c 10.2% (CRITICAL)
     * Autonomous Resolution: LabRetrievalService queries panel; ADA Guideline GL-ADA-09 retrieved
     * ========================================================================= */
    private PatientSourceBundle createPatient104() {
        PatientSourceBundle p = new PatientSourceBundle();
        p.setPatientId("PAT-104");
        p.setName("David Kim");
        p.setAge(52);
        p.setGender("Male");
        p.setScenarioTitle("Objective Glycemic Lab Contradiction & Diagnostic Discordance");
        p.setScenarioDescription("Patient reports recent routine blood work showed normal blood sugar, while latest EHR Comprehensive Metabolic Panel documents fasting glucose 245 mg/dL and HbA1c 10.2%.");
        p.setDeliberateContradictionDescription("Consultation transcript documents patient states 'my blood work last week showed normal blood sugar, doctor said my glucose was fine at 95 mg/dL', directly contradicting EHR lab findings showing severe uncontrolled hyperglycemia.");

        p.setConsultationTranscript("""
Dr. Angela Park: Good morning, David. How have you been feeling since your last routine physical?
David Kim: Good morning, Dr. Park. I've felt a bit more thirsty than usual and getting up once or twice at night, but overall okay. My blood work last week showed normal blood sugar, doctor said my glucose was fine at 95 mg/dL, so I assumed everything was stable.
Dr. Angela Park: Let me pull up your laboratory results from the portal. Actually David, the Comprehensive Metabolic Panel from 3 days ago shows your fasting blood glucose is 245 mg/dL and your HbA1c is 10.2%, which is significantly elevated.
David Kim: Oh, really? That's shocking. I thought my numbers were normal.
Dr. Angela Park: We need to immediately evaluate your glycemic control and discuss starting glucose-lowering therapy.
""");

        List<ClinicalNote> notes = new ArrayList<>();
        ClinicalNote n1 = new ClinicalNote(
                "NOTE-2023-0810",
                "2023-08-10",
                "Dr. Angela Park, MD",
                "Outpatient Internal Medicine",
                "OUTPATIENT_CONSULT",
                "51-year-old male with mild hyperlipidemia and borderline hypertension. Vitals stable. Advised diet and exercise.",
                "BP 124/80, HR 70, SpO2 99%",
                List.of("NKDA"),
                List.of("Hyperlipidemia (E78.5)")
        );
        notes.add(n1);
        p.setPreviousNotes(notes);

        List<MedicationRecord> meds = new ArrayList<>();
        meds.add(new MedicationRecord("MED-104-1", "Atorvastatin", "20mg", "Once daily at bedtime", "Oral", "ACTIVE", "2023-01-15", "2024-02-15", "Dr. Angela Park", "Hyperlipidemia"));
        meds.add(new MedicationRecord("MED-104-2", "Amlodipine", "5mg", "Once daily", "Oral", "ACTIVE", "2023-01-15", "2024-02-15", "Dr. Angela Park", "Borderline Hypertension"));
        p.setMedicationHistory(meds);

        List<AllergyRecord> allergies = new ArrayList<>();
        allergies.add(new AllergyRecord("ALG-104-1", "NKDA", "DRUG", "None", "MILD", "REPORTED_NKDA", "2023-08-10"));
        p.setAllergyList(allergies);

        List<LabReportRecord> labs = new ArrayList<>();
        List<LabResultItem> panel = new ArrayList<>();
        panel.add(new LabResultItem("Fasting Blood Glucose", "245", "mg/dL", "70 - 99", "CRITICAL"));
        panel.add(new LabResultItem("Hemoglobin A1c", "10.2", "%", "4.0 - 5.6", "CRITICAL"));
        panel.add(new LabResultItem("Serum Creatinine", "0.9", "mg/dL", "0.6 - 1.2", "NORMAL"));
        panel.add(new LabResultItem("eGFR", "88", "mL/min/1.73m2", "> 60", "NORMAL"));
        labs.add(new LabReportRecord("LAB-104-A", "2024-03-05", "Comprehensive Metabolic Panel (CMP) & HbA1c", panel, "Marked hyperglycemia and elevated HbA1c confirming new-onset uncontrolled diabetes mellitus."));
        p.setLabReports(labs);

        return p;
    }

    /* Helper for deep cloning bundles */
    private PatientSourceBundle cloneBundle(PatientSourceBundle src) {
        PatientSourceBundle c = new PatientSourceBundle();
        c.setPatientId(src.getPatientId());
        c.setName(src.getName());
        c.setAge(src.getAge());
        c.setGender(src.getGender());
        c.setScenarioTitle(src.getScenarioTitle());
        c.setScenarioDescription(src.getScenarioDescription());
        c.setDeliberateContradictionDescription(src.getDeliberateContradictionDescription());
        c.setConsultationTranscript(src.getConsultationTranscript());
        c.setSyntheticNotice(src.getSyntheticNotice());

        List<ClinicalNote> notes = new ArrayList<>();
        for (ClinicalNote n : src.getPreviousNotes()) {
            notes.add(new ClinicalNote(n.getNoteId(), n.getDate(), n.getAuthor(), n.getDepartment(),
                    n.getNoteType(), n.getContent(), n.getRecordedVitals(),
                    new ArrayList<>(n.getRecordedAllergies()), new ArrayList<>(n.getRecordedDiagnoses())));
        }
        c.setPreviousNotes(notes);

        List<MedicationRecord> meds = new ArrayList<>();
        for (MedicationRecord m : src.getMedicationHistory()) {
            meds.add(new MedicationRecord(m.getId(), m.getMedicationName(), m.getDosage(), m.getFrequency(),
                    m.getRoute(), m.getStatus(), m.getPrescribedDate(), m.getLastRefillDate(),
                    m.getPrescriber(), m.getIndication()));
        }
        c.setMedicationHistory(meds);

        List<AllergyRecord> algs = new ArrayList<>();
        for (AllergyRecord a : src.getAllergyList()) {
            algs.add(new AllergyRecord(a.getId(), a.getAllergen(), a.getAllergenType(), a.getReaction(),
                    a.getSeverity(), a.getStatus(), a.getRecordedDate()));
        }
        c.setAllergyList(algs);

        List<LabReportRecord> labs = new ArrayList<>();
        for (LabReportRecord lr : src.getLabReports()) {
            List<LabResultItem> items = new ArrayList<>();
            for (LabResultItem it : lr.getResults()) {
                items.add(new LabResultItem(it.getTestName(), it.getValue(), it.getUnit(), it.getReferenceRange(), it.getFlag()));
            }
            labs.add(new LabReportRecord(lr.getReportId(), lr.getTestDate(), lr.getPanelName(), items, lr.getNotes()));
        }
        c.setLabReports(labs);

        return c;
    }
}
