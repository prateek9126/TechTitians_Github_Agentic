package com.mediscan.mediscan.agent.controller;

import com.mediscan.mediscan.agent.data.SyntheticPatientRepository;
import com.mediscan.mediscan.agent.model.AgentResponse;
import com.mediscan.mediscan.agent.model.LabResult;
import com.mediscan.mediscan.agent.model.PatientRecord;
import com.mediscan.mediscan.agent.model.PatientSourceBundle;
import com.mediscan.mediscan.agent.service.ClinicalAgentService;
import com.mediscan.mediscan.agent.service.LabReportParser;
import com.mediscan.mediscan.agent.tool.ClinicalGuidelineRAGService;
import com.mediscan.mediscan.agent.tool.RecordValidationService;
import com.mediscan.mediscan.service.OCRService;
import com.mediscan.mediscan.service.PDFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for the autonomous Clinical Documentation & Follow-up Agent module.
 * Endpoints under /api/clinical-agent/*
 * 
 * ADDITIVE ONLY: Does not modify or interfere with UploadController or DoctorController.
 */
@RestController
@RequestMapping("/api/clinical-agent")
@CrossOrigin(origins = "*")
public class ClinicalAgentController {

    private final ClinicalAgentService agentService;
    private final SyntheticPatientRepository patientRepository;
    private final ClinicalGuidelineRAGService guidelineService;
    private final RecordValidationService validationService;
    private final OCRService ocrService;

    @Autowired
    public ClinicalAgentController(ClinicalAgentService agentService,
                                   SyntheticPatientRepository patientRepository,
                                   ClinicalGuidelineRAGService guidelineService,
                                   RecordValidationService validationService,
                                   OCRService ocrService) {
        this.agentService = agentService;
        this.patientRepository = patientRepository;
        this.guidelineService = guidelineService;
        this.validationService = validationService;
        this.ocrService = ocrService;
    }

    /**
     * List all synthetic demo patients with clinical scenario metadata.
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientSourceBundle>> getAllPatients() {
        return ResponseEntity.ok(patientRepository.getAllPatients());
    }

    /**
     * Retrieve the complete synthetic multi-source data bundle for a specific patient.
     */
    @GetMapping("/patients/{id}")
    public ResponseEntity<PatientSourceBundle> getPatientById(@PathVariable String id) {
        return patientRepository.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reset a synthetic patient record back to its original baseline state.
     */
    @PostMapping("/patients/{id}/reset")
    public ResponseEntity<Map<String, String>> resetPatient(@PathVariable String id) {
        patientRepository.resetPatient(id);
        return ResponseEntity.ok(Map.of("message", "Patient " + id + " reset to initial synthetic state."));
    }

    /**
     * Run the autonomous 7-step Clinical Agent pipeline.
     */
    @PostMapping("/run")
    public ResponseEntity<AgentResponse> runAgent(@RequestBody Map<String, Object> request) {
        String patientId = (String) request.getOrDefault("patientId", "PAT-101");
        boolean simulateToolFailure = false;
        if (request.containsKey("simulateToolFailure")) {
            simulateToolFailure = Boolean.parseBoolean(request.get("simulateToolFailure").toString());
        }

        AgentResponse response = agentService.runClinicalAgent(patientId, simulateToolFailure);
        return ResponseEntity.ok(response);
    }

    /**
     * Simulate mid-session source update and trigger targeted incremental adaptation.
     */
    @PostMapping("/update-source")
    public ResponseEntity<AgentResponse> updateSourceAndAdapt(@RequestBody Map<String, String> request) {
        String patientId = request.getOrDefault("patientId", "PAT-102");
        String updateType = request.getOrDefault("updateType", "LAB_UPDATE");
        String updateContent = request.getOrDefault("updateContent", "STAT CMP: eGFR 28 mL/min, Creatinine 2.4 mg/dL");

        AgentResponse response = agentService.adaptToSourceUpdate(patientId, updateType, updateContent);
        return ResponseEntity.ok(response);
    }

    /**
     * Run the autonomous 7-step Clinical Agent pipeline for user-entered custom patient data.
     * Guaranteed 100% full pipeline execution (reconciliation, tool routing, validation).
     */
    @PostMapping("/run-custom")
    public ResponseEntity<AgentResponse> runCustomAgent(@RequestBody PatientRecord customPatient) {
        if (customPatient == null) {
            return ResponseEntity.badRequest().build();
        }
        AgentResponse response = agentService.runClinicalAgent(customPatient, false);
        return ResponseEntity.ok(response);
    }

    /**
     * Parse uploaded lab report (PDF/Image) using text extraction (PDFReader & OCRService)
     * and convert raw text to structured LabResult rows via LabReportParser.
     * Isolated from GroqService single-shot summarization.
     */
    @PostMapping(value = "/parse-lab-report", consumes = {"multipart/form-data"})
    public ResponseEntity<?> parseLabReport(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded."));
        }

        try {
            String reportText;
            if (PDFReader.isPDF(file)) {
                reportText = PDFReader.read(file);
                if (reportText == null || reportText.trim().isEmpty()) {
                    File tempPdf = File.createTempFile("lab_", ".pdf");
                    file.transferTo(tempPdf);
                    reportText = ocrService.readPDF(tempPdf);
                    tempPdf.delete();
                }
            } else if (PDFReader.isImage(file)) {
                String fileName = file.getOriginalFilename();
                String ext = ".png";
                if (fileName != null && fileName.contains(".")) {
                    ext = fileName.substring(fileName.lastIndexOf("."));
                }
                File tempImage = File.createTempFile("lab_img_", ext);
                file.transferTo(tempImage);
                reportText = ocrService.readImage(tempImage);
                tempImage.delete();
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type. Please upload PDF, JPG, JPEG, or PNG."));
            }

            if (reportText == null || reportText.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No text could be extracted from the file.", "results", List.of(), "count", 0));
            }

            List<LabResult> extractedLabs = LabReportParser.extractLabValues(reportText);
            return ResponseEntity.ok(Map.of(
                    "rawText", reportText,
                    "results", extractedLabs,
                    "count", extractedLabs.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to parse lab report: " + e.getMessage()));
        }
    }

    /**
     * Retrieve all clinical guidelines in the RAG knowledge base.
     */
    @GetMapping("/guidelines")
    public ResponseEntity<List<ClinicalGuidelineRAGService.GuidelineSnippet>> getGuidelines() {
        return ResponseEntity.ok(guidelineService.getAllGuidelines());
    }
}
