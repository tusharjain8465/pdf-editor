package com.example.pdf_editor.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "https://pdf-editor-frontend-vercel.vercel.app")
public class PdfController {

    @PostMapping(value = "/finalize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> finalizePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("changes") String changesJson,
            @RequestParam("viewerWidth") float viewerWidth,
            @RequestParam("viewerHeight") float viewerHeight) {

        try {

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> changes = mapper.readValue(changesJson, List.class);

            PDDocument document = PDDocument.load(file.getInputStream());

            for (Map<String, Object> change : changes) {

                float uiX = Float.parseFloat(change.get("x").toString());
                float uiY = Float.parseFloat(change.get("y").toString());
                int pageIndex = Integer.parseInt(change.get("page").toString());

                PDPage page = document.getPage(pageIndex);

                float pdfWidth = page.getMediaBox().getWidth();
                float pdfHeight = page.getMediaBox().getHeight();

                // SCALE UI → PDF
                float x = (uiX / viewerWidth) * pdfWidth;
                float y = pdfHeight - ((uiY / viewerHeight) * pdfHeight);

                float fontSize = change.get("size") != null
                        ? Float.parseFloat(change.get("size").toString())
                        : 12;

                float lineHeight = fontSize + 2;

                PDPageContentStream contentStream = new PDPageContentStream(
                        document,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        false,
                        true);

                String type = (String) change.get("type");

                // =========================
                // ✅ STAMP (CENTER ALIGN)
                // =========================
                if ("stamp".equals(type)) {

                    String name = (String) change.get("name");
                    String id = (String) change.get("id");
                    String time = formatDate((String) change.get("time"));

                    String text = "Digital Signature\n" +
                            name + " (" + id + ")\n" +
                            time;

                    String[] lines = text.split("\n");

                    float padding = 6;

                    // calculate max width
                    float maxWidth = 0;
                    for (int i = 0; i < lines.length; i++) {
                        float w = (i == 0
                                ? PDType1Font.HELVETICA_BOLD.getStringWidth(lines[i])
                                : PDType1Font.HELVETICA.getStringWidth(lines[i]))
                                / 1000 * fontSize;

                        maxWidth = Math.max(maxWidth, w);
                    }

                    float boxWidth = maxWidth + padding * 2;
                    float boxHeight = lineHeight * lines.length + padding * 2;

                    float boxX = x;
                    float boxY = y - boxHeight + padding;

                    // DRAW BOX
                    contentStream.setLineWidth(1);
                    contentStream.addRect(boxX, boxY, boxWidth, boxHeight);
                    contentStream.stroke();

                    // DRAW CENTERED TEXT
                    contentStream.beginText();

                    float centerX = boxX + boxWidth / 2;
                    float startY = y - fontSize;

                    for (int i = 0; i < lines.length; i++) {

                        String line = lines[i];

                        // font
                        if (i == 0) {
                            contentStream.setFont(PDType1Font.HELVETICA_BOLD, fontSize);
                        } else {
                            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                        }

                        float textWidth = (i == 0
                                ? PDType1Font.HELVETICA_BOLD.getStringWidth(line)
                                : PDType1Font.HELVETICA.getStringWidth(line))
                                / 1000 * fontSize;

                        float textX = centerX - (textWidth / 2);
                        float textY = startY - (i * lineHeight);

                        contentStream.newLineAtOffset(textX, textY);
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(-textX, -textY); // reset
                    }

                    contentStream.endText();
                }

                // =========================
                // ✅ NORMAL TEXT
                // =========================
                else {

                    String text = (String) change.get("text");

                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                    contentStream.newLineAtOffset(x, y);
                    contentStream.showText(text);
                    contentStream.endText();
                }

                contentStream.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=final.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================
    // DATE FORMAT
    // =========================
    private String formatDate(String isoDate) {
        try {
            Instant instant = Instant.parse(isoDate);
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");

            return zdt.format(formatter);
        } catch (Exception e) {
            return isoDate;
        }
    }

}
