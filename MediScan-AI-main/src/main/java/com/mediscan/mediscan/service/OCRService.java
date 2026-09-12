package com.mediscan.mediscan.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class OCRService {

    // Path to your tesseract.exe
    private static final String TESSERACT =
            "C:\\Users\\user\\Downloads\\tesseract.exe";

    // OCR for JPG / PNG
    public String readImage(File imageFile) throws Exception {

        ProcessBuilder builder = new ProcessBuilder(
                TESSERACT,
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

        process.waitFor();

        return text;
    }

    // OCR for scanned PDF
    public String readPDF(File pdfFile) throws Exception {

        StringBuilder text = new StringBuilder();

        PDDocument document = Loader.loadPDF(pdfFile);

        PDFRenderer renderer = new PDFRenderer(document);

        for (int i = 0; i < document.getNumberOfPages(); i++) {

            BufferedImage image = renderer.renderImageWithDPI(i, 300);

            File tempImage = File.createTempFile("page_", ".png");

            ImageIO.write(image, "png", tempImage);

            text.append(readImage(tempImage));

            text.append("\n\n");

            tempImage.delete();

        }

        document.close();

        return text.toString();
    }
}