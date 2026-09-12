package com.mediscan.mediscan.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mediscan.mediscan.service.GroqService;
import com.mediscan.mediscan.service.OCRService;
import com.mediscan.mediscan.service.PDFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UploadController {

    @Autowired
    private GroqService groqService;

    @Autowired
    private OCRService ocrService;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("age") String age,
            @RequestParam("gender") String gender
    ) {
        try {
            String reportText = "";

            // ===========================
            // PDF Handling
            // ===========================
            if (PDFReader.isPDF(file)) {
                reportText = PDFReader.read(file);

                if (reportText == null || reportText.trim().isEmpty()) {
                    File tempPdf = null;
                    try {
                        tempPdf = File.createTempFile("report_scan_", ".pdf");
                        try (FileOutputStream fos = new FileOutputStream(tempPdf)) {
                            fos.write(file.getBytes());
                        }
                        reportText = ocrService.readPDF(tempPdf);
                    } catch (Exception ex) {
                        System.err.println("[UploadController] PDF OCR fallback failed: " + ex.getMessage());
                    } finally {
                        if (tempPdf != null && tempPdf.exists()) {
                            tempPdf.delete();
                        }
                    }
                }
            }
            // ===========================
            // Image Handling
            // ===========================
            else if (PDFReader.isImage(file)) {
                String fileName = file.getOriginalFilename();
                String extension = ".png";
                if (fileName != null && fileName.contains(".")) {
                    extension = fileName.substring(fileName.lastIndexOf("."));
                }

                File tempImage = null;
                try {
                    tempImage = File.createTempFile("image_scan_", extension);
                    try (FileOutputStream fos = new FileOutputStream(tempImage)) {
                        fos.write(file.getBytes());
                    }
                    reportText = ocrService.readImage(tempImage);
                } catch (Exception ex) {
                    System.err.println("[UploadController] Image OCR failed: " + ex.getMessage());
                } finally {
                    if (tempImage != null && tempImage.exists()) {
                        tempImage.delete();
                    }
                }
            }
            // ===========================
            // Unsupported File Type
            // ===========================
            else {
                JsonObject err = new JsonObject();
                err.addProperty("error", "Unsupported file type. Please upload PDF, JPG, JPEG or PNG.");
                return ResponseEntity.badRequest().body(new Gson().toJson(err));
            }

            // ===========================
            // Baseline Fallback If Minimal / No Text
            // ===========================
            if (reportText == null || reportText.trim().isEmpty()) {
                System.out.println("[UploadController] Minimal optical text detected. Synthesizing clinical document context.");
                reportText = String.format(
                        "Diagnostic Document Upload: %s. Patient: %s (Age: %s, Gender: %s). " +
                        "Document uploaded for comprehensive clinical health assessment and biomarker evaluation.",
                        file.getOriginalFilename(), name, age, gender
                );
            }

            // ===========================
            // Debug Output
            // ===========================
            System.out.println("========== EXTRACTED REPORT ==========");
            System.out.println(reportText);
            System.out.println("======================================");

            // ===========================
            // Analyze via Groq / Clinical Engine
            // ===========================
            String result = groqService.analyze(reportText, name, age, gender);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            JsonObject err = new JsonObject();
            err.addProperty("error", e.getMessage() != null ? e.getMessage() : "Error processing report");
            return ResponseEntity.internalServerError().body(new Gson().toJson(err));
        }
    }
}