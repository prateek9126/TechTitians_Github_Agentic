package com.mediscan.mediscan.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.mediscan.mediscan.controller.UploadController;
import com.mediscan.mediscan.service.GroqService;
import com.mediscan.mediscan.service.OCRService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class UploadControllerIntegrationTest {

    @Test
    void testUploadCBCPdf_Returns14BiomarkersAndZeroProblems() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.setLeading(14.0f);
                cs.newLineAtOffset(50, 720);

                String[] lines = {
                        "DEMO COMPLETE BLOOD COUNT (CBC) REPORT",
                        "SYNTHETIC SAMPLE - NOT A REAL MEDICAL REPORT",
                        "Patient Name Demo Patient Age / Sex 25 / Male",
                        "Patient ID DEMO-001 Report Date 12-Sep-2026",
                        "Specimen Whole Blood (EDTA) Status Sample",
                        "Investigation Result Unit Reference Range",
                        "Hemoglobin (Hb) 14.2 g/dL 13.0 - 17.0",
                        "RBC Count 4.8 million/cumm 4.5 - 5.5",
                        "Hematocrit (PCV) 43 % 40 - 50",
                        "MCV 89 fL 83 - 101",
                        "MCH 29.6 pg 27 - 32",
                        "MCHC 33.2 g/dL 31.5 - 34.5",
                        "RDW 13.1 % 11.6 - 14.0",
                        "Total WBC Count 7,200 /cumm 4,000 - 11,000",
                        "Neutrophils 58 % 40 - 80",
                        "Lymphocytes 34 % 20 - 40",
                        "Eosinophils 3 % 1 - 6",
                        "Monocytes 4 % 2 - 10",
                        "Basophils 1 % 0 - 2",
                        "Platelet Count 245,000 /cumm 150,000 - 410,000",
                        "Demo Interpretation: Sample CBC values shown above are within the illustrative reference ranges."
                };

                for (String line : lines) {
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(baos);
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cbc_report.pdf",
                "application/pdf",
                baos.toByteArray()
        );

        UploadController controller = new UploadController();
        GroqService groqService = new GroqService();
        OCRService ocrService = new OCRService();
        ReflectionTestUtils.setField(controller, "groqService", groqService);
        ReflectionTestUtils.setField(controller, "ocrService", ocrService);

        ResponseEntity<String> response = controller.upload(file, "Demo Patient", "25", "Male");
        assertEquals(200, response.getStatusCode().value());

        String json = response.getBody();
        assertNotNull(json);

        JsonObject obj = new Gson().fromJson(json, JsonObject.class);
        assertEquals("Low", obj.get("riskLevel").getAsString());

        JsonArray problems = obj.getAsJsonArray("problems");
        assertNotNull(problems);
        assertEquals(0, problems.size(), "Normal CBC report must have 0 problems");

        JsonArray tests = obj.getAsJsonArray("tests");
        assertNotNull(tests);
        assertEquals(14, tests.size(), "Must extract all 14 CBC test biomarkers from PDF");

        for (int i = 0; i < tests.size(); i++) {
            JsonObject t = tests.get(i).getAsJsonObject();
            assertEquals("NORMAL", t.get("flag").getAsString());
            assertEquals("Normal", t.get("status").getAsString());
        }

        System.out.println("=== END-TO-END PDF UPLOAD TEST PASSED ===");
        System.out.println("Total tests extracted: " + tests.size());
        System.out.println("Risk Level: " + obj.get("riskLevel").getAsString());
        System.out.println("Problems: " + problems);
    }

    @Test
    void generateDemoPdfs() throws Exception {
        String[] cbcLines = {
                "DEMO COMPLETE BLOOD COUNT (CBC) REPORT",
                "SYNTHETIC SAMPLE - NOT A REAL MEDICAL REPORT",
                "Patient Name Demo Patient Age / Sex 25 / Male",
                "Patient ID DEMO-001 Report Date 12-Sep-2026",
                "Specimen Whole Blood (EDTA) Status Completed",
                "Laboratory MediScan AI Central Diagnostic Laboratories",
                "Investigation Result Unit Reference Range",
                "Hemoglobin (Hb) 14.2 g/dL 13.0 - 17.0",
                "RBC Count 4.8 million/cumm 4.5 - 5.5",
                "Hematocrit (PCV) 43 % 40 - 50",
                "MCV 89 fL 83 - 101",
                "MCH 29.6 pg 27 - 32",
                "MCHC 33.2 g/dL 31.5 - 34.5",
                "RDW 13.1 % 11.6 - 14.0",
                "Total WBC Count 7,200 /cumm 4,000 - 11,000",
                "Neutrophils 58 % 40 - 80",
                "Lymphocytes 34 % 20 - 40",
                "Eosinophils 3 % 1 - 6",
                "Monocytes 4 % 2 - 10",
                "Basophils 1 % 0 - 2",
                "Platelet Count 245,000 /cumm 150,000 - 410,000",
                "Demo Interpretation: Sample CBC values shown above are within the illustrative reference ranges."
        };

        String[] cmpLines = {
                "DEMO COMPREHENSIVE METABOLIC PANEL (CMP) REPORT",
                "SYNTHETIC SAMPLE - NOT A REAL MEDICAL REPORT",
                "Patient Name Rajesh Verma Age / Sex 54 / Male",
                "Patient ID DEMO-002 Report Date 12-Sep-2026",
                "Specimen Serum Status Completed",
                "Laboratory MediScan AI Central Diagnostic Laboratories",
                "Investigation Result Unit Reference Range",
                "Fasting Blood Glucose 215 mg/dL 70 - 99",
                "Hemoglobin A1c 9.4 % < 5.7",
                "Serum Creatinine 2.2 mg/dL 0.6 - 1.2",
                "eGFR 32 mL/min/1.73m2 > 60",
                "Blood Urea Nitrogen (BUN) 28 mg/dL 7 - 20",
                "Potassium 5.3 mEq/L 3.5 - 5.0",
                "Sodium 140 mEq/L 136 - 145",
                "Total Cholesterol 248 mg/dL < 200",
                "Demo Interpretation: Marked hyperglycemia and reduced eGFR with elevated creatinine noted."
        };

        writePdf(cbcLines, "src/main/resources/static/samples/Demo_Blood_Report_CBC.pdf");
        writePdf(cmpLines, "src/main/resources/static/samples/Demo_Abnormal_Metabolic_Report.pdf");

        writePdf(cbcLines, "target/classes/static/samples/Demo_Blood_Report_CBC.pdf");
        writePdf(cmpLines, "target/classes/static/samples/Demo_Abnormal_Metabolic_Report.pdf");

        writePdf(cbcLines, "../resources/static/samples/Demo_Blood_Report_CBC.pdf");
        writePdf(cmpLines, "../resources/static/samples/Demo_Abnormal_Metabolic_Report.pdf");
    }

    private void writePdf(String[] lines, String path) throws Exception {
        java.io.File file = new java.io.File(path);
        file.getParentFile().mkdirs();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.setLeading(16.0f);
                cs.newLineAtOffset(50, 720);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(file);
        }
        System.out.println("Generated PDF: " + file.getAbsolutePath());
    }
}
