# MediScan AI 🩺

MediScan AI is an AI-powered healthcare platform that simplifies medical report analysis and improves access to healthcare services. Users can upload medical reports (Blood Reports, MRI, CT Scan, X-Ray), receive AI-generated insights, find suitable doctors, and book appointments online.

## 🚀 Features

- 🧬 AI-powered medical report analysis (Groq LLM, with a deterministic clinical fallback engine when no API key is configured)
- 🩸 Blood report interpretation with biomarker-level risk flags (glucose, HbA1c, creatinine, eGFR, potassium, cholesterol, hemoglobin, WBC, and more)
- 🧠 MRI, CT Scan & X-Ray report support
- 📄 OCR and PDF text extraction for uploaded reports (Tesseract via Tess4j, Apache PDFBox)
- 👨‍⚕️ Doctor search by location and specialty
- 📅 Online appointment booking
- 🚨 Emergency healthcare support page
- 🤖 Clinical Documentation & Follow-up Agent — an autonomous module that reconciles conflicting patient data across consultation transcripts, EHR notes, medications, allergies, and lab panels
- 🔒 Secure health data handling

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.4 (Web, Thymeleaf, Validation) |
| AI Analysis | Groq API (`llama-3.3-70b-versatile`) via OkHttp, with rule-based fallback analyzer |
| Document Processing | Apache PDFBox 3.0.3, Tess4j 5.13.0 (OCR) |
| JSON | Gson, Jackson |
| Frontend | HTML5, CSS3, JavaScript |
| Build | Maven |

## 📋 Prerequisites

- Java 21+
- Maven (or use the included `./mvnw` wrapper)
- A [Groq API key](https://console.groq.com) *(optional — the app runs in fallback mode without one)*

## ⚙️ Configuration

The app reads its Groq API key from an environment variable at runtime:

```properties
# src/main/resources/application.properties
groq.api.key=${GROQ_API_KEY:demo_key}
```

| Variable | Required | Description |
|---|---|---|
| `GROQ_API_KEY` | No | Your Groq API key. If unset, defaults to `demo_key` and the app automatically falls back to a deterministic, rule-based clinical analyzer instead of calling the Groq API. |

## 🏃 Running Locally

```bash
# Clone the repo
git clone https://github.com/prateek9126/Mediscan.git
cd Mediscan

# (Optional) set your Groq API key
export GROQ_API_KEY=your_key_here      # macOS/Linux
set GROQ_API_KEY=your_key_here         # Windows CMD

# Run
./mvnw clean spring-boot:run
```

The app starts on **http://localhost:8080**.

## 🐳 Running with Docker

```bash
docker build -t mediscan-ai .
docker run -p 8080:8080 -e GROQ_API_KEY=your_key_here mediscan-ai
```

## ☁️ Deploying on Render

1. Connect this repository to a new Render Web Service (Docker environment).
2. Add an environment variable `GROQ_API_KEY` with your Groq key (optional).
3. Deploy — Render will build the Docker image and run the Spring Boot app on port `8080`.

## 🤖 Clinical Documentation & Follow-up Agent

An autonomous module that converts simulated consultations into verified, guardrail-compliant follow-up records while reconciling conflicting patient information across multiple synthetic sources.

**Key capabilities:**
- **Multi-source reconciliation** — diffs unstructured dialogue against structured EHR records to detect contradictions
- **Autonomous tool routing** — selects tools based on conflict type (`PatientRecordLookupService`, `MedicationAllergyLookupService`, `LabRetrievalService`, `ClinicalGuidelineRAGService`)
- **4-rule validation engine** — checks record completeness, unresolved high-severity conflicts, temporal/physiological bounds, and negative guardrails (no definitive diagnostic/prescriptive claims)
- **Mid-session adaptation** — ingests new lab results and re-evaluates only affected fields
- **Graceful failure recovery** — retries on simulated tool outages, then escalates safely rather than failing silently

### Try the demo

1. Start the app, then open `http://localhost:8080/clinical-agent.html`
2. Select a synthetic patient (`PAT-101` or `PAT-102`) and click **Run Autonomous Pipeline**
3. Try **Simulate Mid-Session Lab Update** or **Simulate Tool Failure** to see adaptive/recovery behavior

### API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/clinical-agent/patients` | List all synthetic patient bundles |
| `GET` | `/api/clinical-agent/patients/{id}` | Get a patient bundle by ID |
| `POST` | `/api/clinical-agent/run` | Run the autonomous reconciliation pipeline |
| `POST` | `/api/clinical-agent/update-source` | Simulate mid-session data arrival |
| `POST` | `/api/clinical-agent/patients/{id}/reset` | Reset a patient record to baseline |

**Example — run the pipeline:**
```json
POST /api/clinical-agent/run
{
  "patientId": "PAT-101",
  "simulateToolFailure": false
}
```

## 📁 Project Structure

```
src/main/java/com/mediscan/
├── mediscan/
│   ├── MediscanApplication.java
│   ├── controller/          # Doctor, home, upload endpoints
│   ├── service/             # Groq AI, OCR, PDF reading, doctor logic
│   └── agent/
│       ├── controller/      # Clinical agent REST API
│       ├── service/         # Agent orchestration, reconciliation, draft generation
│       ├── tool/            # Lookup/validation/RAG tool services
│       ├── data/            # Synthetic patient dataset
│       └── model/           # Domain models
└── model/
    └── Doctor.java
src/main/resources/
├── application.properties
├── clinical-guidelines.json
└── static/                  # HTML, CSS, JS, images
```

## 📌 Roadmap

- [ ] Real AI model integration for medical image analysis (MRI/CT/X-ray)
- [ ] User authentication system
- [ ] Doctor dashboard
- [ ] Online consultation
- [ ] Longitudinal health history tracking

## 👨‍💻 Authors

**Priyansi Sahoo**  
**Nikita**  
**Kumar Piyush**  
**Priyanshu Kumar Singh**

## 📄 License

Specify your license here (e.g. MIT).
