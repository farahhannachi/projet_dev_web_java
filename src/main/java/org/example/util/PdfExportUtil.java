package org.example.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.model.ResponseQuestion;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportUtil {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void exportResponseQuestions(List<ResponseQuestion> responses, Path outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 40;
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 14;

            PDPageContentStream content = new PDPageContentStream(document, page);
            try {
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("CuraVita - Response Questions");
                content.endText();

                y -= leading * 2;
                content.setFont(PDType1Font.HELVETICA, 10);

                for (ResponseQuestion response : responses) {
                    if (y < margin + leading * 4) {
                        content.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        content = new PDPageContentStream(document, page);
                        content.setFont(PDType1Font.HELVETICA, 10);
                    }

                    String line = String.format("#%d | Q: %s | Auteur: %s | Role: %s | Statut: %s | %s",
                            response.getId(),
                            safe(response.getQuestionObjet()),
                            response.getAuteurType() != null ? response.getAuteurType().getLabel() : "",
                            response.getReponseRole() != null ? response.getReponseRole().getLabel() : "",
                            response.getImpactStatut() != null ? response.getImpactStatut().getLabel() : "",
                            response.getCreatedAt() != null ? response.getCreatedAt().format(DATE_FORMAT) : "");

                    content.beginText();
                    content.newLineAtOffset(margin, y);
                    content.showText(truncate(line, 110));
                    content.endText();

                    y -= leading;
                    content.beginText();
                    content.newLineAtOffset(margin, y);
                    content.showText("Reponse: " + truncate(safe(response.getReponseText()), 120));
                    content.endText();

                    y -= leading * 1.5f;
                }
            } finally {
                content.close();
            }

            document.save(outputPath.toFile());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}
