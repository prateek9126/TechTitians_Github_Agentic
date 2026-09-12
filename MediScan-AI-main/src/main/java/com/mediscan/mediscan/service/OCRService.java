package com.mediscan.mediscan.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class OCRService {

    /**
     * Dynamically resolves the Tesseract executable path across Docker / Linux / Windows environments.
     */
    public String resolveTesseractCommand() {
        // 1. Environment variable or system property
        String customPath = System.getenv("TESSERACT_PATH");
        if (customPath == null || customPath.isBlank()) {
            customPath = System.getProperty("tesseract.path");
        }
        if (customPath != null && !customPath.isBlank() && new File(customPath).exists()) {
            return customPath;
        }

        // 2. Common Linux / Docker paths (e.g. apt install tesseract-ocr)
        String[] unixCandidates = {
                "/usr/bin/tesseract",
                "/usr/local/bin/tesseract",
                "/bin/tesseract"
        };
        for (String p : unixCandidates) {
            if (new File(p).exists()) {
                return p;
            }
        }

        // 3. Common Windows paths
        String[] winCandidates = {
                "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe",
                "C:\\Users\\user\\Downloads\\tesseract.exe"
        };
        for (String p : winCandidates) {
            if (new File(p).exists()) {
                return p;
            }
        }

        // 4. Default to standard PATH executable
        return "tesseract";
    }

    /**
     * Checks whether Tesseract binary can be executed.
     */
    public boolean isTesseractAvailable() {
        try {
            String cmd = resolveTesseractCommand();
            Process p = new ProcessBuilder(cmd, "--version").redirectErrorStream(true).start();
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // OCR for JPG / PNG
    public String readImage(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            return "";
        }

        try {
            String cmd = resolveTesseractCommand();
            ProcessBuilder builder = new ProcessBuilder(
                    cmd,
                    imageFile.getAbsolutePath(),
                    "stdout",
                    "-l",
                    "eng"
            );

            builder.redirectErrorStream(true);

            Process process = builder.start();

            String text = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            boolean completed = process.waitFor(45, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                System.err.println("[OCRService] Tesseract process timed out for image: " + imageFile.getName());
                return "";
            }

            return text != null ? text.trim() : "";
        } catch (Exception e) {
            System.err.println("[OCRService] OCR image scan skipped/failed (" + e.getMessage() + ")");
            return "";
        }
    }

    // OCR for scanned PDF
    public String readPDF(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return "";
        }

        StringBuilder text = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), 10);

            for (int i = 0; i < pages; i++) {
                File tempImage = null;
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, 200);
                    tempImage = File.createTempFile("page_", ".png");
                    ImageIO.write(image, "png", tempImage);

                    String pageText = readImage(tempImage);
                    if (pageText != null && !pageText.isBlank()) {
                        text.append(pageText).append("\n\n");
                    }
                } finally {
                    if (tempImage != null && tempImage.exists()) {
                        tempImage.delete();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[OCRService] OCR PDF scan skipped/failed (" + e.getMessage() + ")");
        }

        return text.toString().trim();
    }
}