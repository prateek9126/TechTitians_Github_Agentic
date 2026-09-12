package com.mediscan.mediscan.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public class PDFReader {

    public static String read(MultipartFile file) throws Exception {

        // Try normal PDF text extraction first
        PDDocument document = Loader.loadPDF(file.getBytes());

        PDFTextStripper stripper = new PDFTextStripper();

        String text = stripper.getText(document);

        document.close();

        // If text exists, return it
        if (text != null && !text.trim().isEmpty()) {
            return text;
        }

        // Otherwise perform OCR
        File tempPdf = File.createTempFile("report", ".pdf");

        file.transferTo(tempPdf);

        OCRService ocr = new OCRService();

        String ocrText = ocr.readPDF(tempPdf);

        tempPdf.delete();

        return ocrText;
    }

    public static boolean isPDF(MultipartFile file) {

        String name = file.getOriginalFilename();

        if (name == null)
            return false;

        return name.toLowerCase().endsWith(".pdf");
    }

    public static boolean isImage(MultipartFile file) {

        String name = file.getOriginalFilename();

        if (name == null)
            return false;

        String lower = name.toLowerCase();

        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png");
    }

    public static String getMimeType(MultipartFile file) {

        String name = file.getOriginalFilename();

        if (name == null)
            return "image/jpeg";

        return name.toLowerCase().endsWith(".png")
                ? "image/png"
                : "image/jpeg";
    }

}