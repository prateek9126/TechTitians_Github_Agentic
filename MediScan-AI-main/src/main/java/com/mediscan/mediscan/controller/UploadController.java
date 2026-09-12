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

            return ResponseEntity.ok(result);

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

}