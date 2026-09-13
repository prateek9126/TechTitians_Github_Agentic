package com.mediscan.mediscan.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;

public class PDFReader {

    public static String read(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return "";
        }

        byte[] bytes = file.getBytes();

        // Try normal PDF text extraction first
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        } catch (Exception e) {
            System.err.println("[PDFReader] Standard PDFBox text extraction failed: " + e.getMessage());
        }

        // If no text found via standard PDFBox, attempt OCR if available
        File tempPdf = null;
        try {
            tempPdf = File.createTempFile("report_ocr_", ".pdf");
            try (FileOutputStream fos = new FileOutputStream(tempPdf)) {
                fos.write(bytes);
            }
            OCRService ocr = new OCRService();
            String ocrText = ocr.readPDF(tempPdf);
            return ocrText != null ? ocrText.trim() : "";
        } catch (Exception e) {
            System.err.println("[PDFReader] Scanned PDF OCR fallback failed: " + e.getMessage());
            return "";
        } finally {
            if (tempPdf != null && tempPdf.exists()) {
                tempPdf.delete();
            }
        }
    }

    public static boolean isPDF(MultipartFile file) {
        if (file == null) return false;
        String name = file.getOriginalFilename();
        if (name == null) return false;
        return name.toLowerCase().endsWith(".pdf");
    }

    public static boolean isImage(MultipartFile file) {
        if (file == null) return false;
        String name = file.getOriginalFilename();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp");
    }

    public static String getMimeType(MultipartFile file) {
        if (file == null) return "image/jpeg";
        String name = file.getOriginalFilename();
        if (name == null) return "image/jpeg";
        return name.toLowerCase().endsWith(".png")
                ? "image/png"
                : "image/jpeg";
    }
}