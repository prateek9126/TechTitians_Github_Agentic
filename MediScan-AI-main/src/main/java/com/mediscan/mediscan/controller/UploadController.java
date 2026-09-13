package com.mediscan.mediscan.controller;

import com.mediscan.mediscan.service.GroqService;
import com.mediscan.mediscan.service.OCRService;
import com.mediscan.mediscan.service.PDFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UploadController {

    @Autowired
    private GroqService groqService;

    @Autowired
    private OCRService ocrService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(

            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("age") String age,
            @RequestParam("gender") String gender

    ) {

        try {

            String reportText;

            // ===========================
            // PDF
            // ===========================

            if (PDFReader.isPDF(file)) {

                // Try normal PDF text extraction first
                reportText = PDFReader.read(file);

                // If no text found, perform OCR
                if (reportText == null || reportText.trim().isEmpty()) {

                    File tempPdf = File.createTempFile("report_", ".pdf");

                    file.transferTo(tempPdf);

                    reportText = ocrService.readPDF(tempPdf);

                    tempPdf.delete();
                }

            }

            // ===========================
            // IMAGE
            // ===========================

            else if (PDFReader.isImage(file)) {

                String fileName = file.getOriginalFilename();

                String extension = ".png";

                if (fileName != null && fileName.contains(".")) {
                    extension = fileName.substring(fileName.lastIndexOf("."));
                }

                File tempImage = File.createTempFile("image_", extension);

                file.transferTo(tempImage);

                reportText = ocrService.readImage(tempImage);

                tempImage.delete();

            }

            // ===========================
            // Unsupported File
            // ===========================

            else {

                return ResponseEntity.badRequest().body("""
                {
                  "error":"Unsupported file type. Please upload PDF, JPG, JPEG or PNG."
                }
                """);
            }

            // ===========================
            // No Text Found
            // ===========================

            if (reportText == null || reportText.trim().isEmpty()) {

                return ResponseEntity.badRequest().body("""
                {
                  "error":"No readable text found in the uploaded report."
                }
                """);
            }

            // ===========================
            // Debug Output
            // ===========================

            System.out.println("========== EXTRACTED REPORT ==========");
            System.out.println(reportText);
            System.out.println("======================================");

            // ===========================
            // Prompt for Groq AI
            // ===========================

            String prompt = """
You are an experienced physician and medical report analyzer.

Analyze the following medical report carefully.

IMPORTANT RULES:
- Explain everything in SIMPLE ENGLISH.
- Write as if you are explaining the report to the patient.
- Mention every important test value.
- Tell whether each value is Normal, Low or High.
- Explain what abnormal values may indicate.
- Do NOT invent diseases that are not supported by the report.
- Mention positive findings as well (normal values).
- Mention if the report needs follow-up.
- Mention if the condition is mild, moderate or severe.
- Emergency should be "Yes" only if immediate medical attention is required based on the report.

Return ONLY valid JSON.

Format:

{
  "summary":"A detailed summary of 6-10 sentences in easy language.",
  "riskLevel":"Low/Medium/High/Emergency",
  "problems":[
    "Problem 1",
    "Problem 2"
  ],
  "recommendations":[
    "Recommendation 1",
    "Recommendation 2"
  ],
  "specialist":"Recommended specialist",
  "emergency":"Yes or No"
}

Patient Details

Name: %s
Age: %s
Gender: %s

Medical Report

%s
""".formatted(name, age, gender, reportText);
            String result = groqService.analyze(prompt);

            try {
                com.google.gson.JsonObject jsonObj = new com.google.gson.Gson().fromJson(result, com.google.gson.JsonObject.class);
                jsonObj.addProperty("rawReportText", reportText);
                return ResponseEntity.ok(new com.google.gson.Gson().toJson(jsonObj));
            } catch (Exception ignored) {
                return ResponseEntity.ok(result);
            }

        }

        catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError().body("""
            {
              "error":"%s"
            }
            """.formatted(e.getMessage()));
        }

    }

    /**
     * Demo report endpoint for judges and instant demonstrations.
     * Provides preloaded CBC (Normal) or Metabolic (Abnormal) reports with instant analysis.
     */
    @GetMapping("/demo-report")
    public ResponseEntity<String> getDemoReport(
            @RequestParam(value = "type", defaultValue = "cbc") String type,
            @RequestParam(value = "name", defaultValue = "Prateek") String name,
            @RequestParam(value = "age", defaultValue = "23") String age,
            @RequestParam(value = "gender", defaultValue = "Male") String gender
    ) {
        String reportText;
        String reportTitle;
        if ("metabolic".equalsIgnoreCase(type)) {
            reportTitle = "Demo_Abnormal_Metabolic_Report.pdf";
            reportText = """
                    DEMO COMPREHENSIVE METABOLIC PANEL (CMP) REPORT
                    SYNTHETIC SAMPLE - NOT A REAL MEDICAL REPORT
                    Patient Name: %s    Age / Sex: %s / %s
                    Patient ID: DEMO-002           Report Date: 12-Sep-2026
                    Specimen: Serum                Status: Completed
                    Laboratory: MediScan AI Central Diagnostic Laboratories
                    
                    Investigation              Result   Unit             Reference Range
                    Fasting Blood Glucose      215      mg/dL            70 - 99
                    Hemoglobin A1c             9.4      %%                < 5.7
                    Serum Creatinine           2.2      mg/dL            0.6 - 1.2
                    eGFR                       32       mL/min/1.73m2    > 60
                    Blood Urea Nitrogen (BUN)  28       mg/dL            7 - 20
                    Potassium                  5.3      mEq/L            3.5 - 5.0
                    Sodium                     140      mEq/L            136 - 145
                    Total Cholesterol          248      mg/dL            < 200
                    
                    Demo Interpretation: Marked hyperglycemia and reduced eGFR with elevated creatinine noted.
                    Authorized Signatory: Dr. A. Sharma, MD | MediScan AI Diagnostics
                    """.formatted(name, age, gender);
        } else {
            reportTitle = "Demo_Blood_Report_CBC.pdf";
            reportText = """
                    DEMO COMPLETE BLOOD COUNT (CBC) REPORT
                    SYNTHETIC SAMPLE - NOT A REAL MEDICAL REPORT
                    Patient Name: %s    Age / Sex: %s / %s
                    Patient ID: DEMO-001           Report Date: 12-Sep-2026
                    Specimen: Whole Blood (EDTA)   Status: Completed
                    Laboratory: MediScan AI Central Diagnostic Laboratories
                    
                    Investigation              Result   Unit             Reference Range
                    Hemoglobin (Hb)            14.2     g/dL             13.0 - 17.0
                    RBC Count                  4.8      million/cumm     4.5 - 5.5
                    Hematocrit (PCV)           43       %%                40 - 50
                    MCV                        89       fL               83 - 101
                    MCH                        29.6     pg               27 - 32
                    MCHC                       33.2     g/dL             31.5 - 34.5
                    RDW                        13.1     %%                11.6 - 14.0
                    Total WBC Count            7,200    /cumm            4,000 - 11,000
                    Neutrophils                58       %%                40 - 80
                    Lymphocytes                34       %%                20 - 40
                    Eosinophils                3        %%                1 - 6
                    Monocytes                  4        %%                2 - 10
                    Basophils                  1        %%                0 - 2
                    Platelet Count             245,000  /cumm            150,000 - 410,000
                    
                    Demo Interpretation: Sample CBC values shown above are within healthy reference intervals.
                    Authorized Signatory: Dr. S. Rao, MD | MediScan AI Diagnostics
                    """.formatted(name, age, gender);
        }

        String prompt = """
                You are an experienced physician and medical report analyzer.
                Analyze the following medical report carefully.
                Patient Details
                Name: %s
                Age: %s
                Gender: %s
                Medical Report
                %s
                """.formatted(name, age, gender, reportText);

        String result = groqService.analyze(prompt);
        try {
            com.google.gson.JsonObject jsonObj = new com.google.gson.Gson().fromJson(result, com.google.gson.JsonObject.class);
            jsonObj.addProperty("rawReportText", reportText);
            jsonObj.addProperty("reportType", reportTitle);
            jsonObj.addProperty("isDemo", true);
            return ResponseEntity.ok(new com.google.gson.Gson().toJson(jsonObj));
        } catch (Exception e) {
            return ResponseEntity.ok(result);
        }
    }

}