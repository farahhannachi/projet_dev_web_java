package org.example.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.model.Question;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class QuestionPdfExportUtil {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private QuestionPdfExportUtil() {
    }

    public static void exportQuestion(Question question, Path outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 42f;
            float contentWidth = page.getMediaBox().getWidth() - (margin * 2);
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                draw(content, "CuraVita - Ticket Client", margin, y, PDType1Font.HELVETICA_BOLD, 16);
                y -= 18;
                draw(content, "Genere le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        margin, y, PDType1Font.HELVETICA, 9);
                y -= 16;
                drawLine(content, margin, y, margin + contentWidth);
                y -= 20;

                y = drawField(content, "ID", String.valueOf(question.getId()), margin, y, contentWidth);
                y = drawField(content, "Objet", safe(question.getObjet()), margin, y, contentWidth);
                y = drawField(content, "Type", safe(question.getTypeTicket()), margin, y, contentWidth);
                y = drawField(content, "Priorite", safe(question.getPriorite()), margin, y, contentWidth);
                y = drawField(content, "Statut", safe(question.getStatut()), margin, y, contentWidth);
                y = drawField(content, "Date creation",
                        question.getCreatedAt() != null ? question.getCreatedAt().format(DATE_FORMAT) : "-",
                        margin, y, contentWidth);

                if (!isBlank(question.getFileName())) {
                    String fileLabel = question.getFileName();
                    if (question.getFileSize() != null) {
                        fileLabel += " (" + humanReadableSize(question.getFileSize()) + ")";
                    }
                    y = drawField(content, "Fichier joint", fileLabel, margin, y, contentWidth);
                }

                y -= 8;
                draw(content, "Description", margin, y, PDType1Font.HELVETICA_BOLD, 11);
                y -= 14;

                List<String> descriptionLines = wrapText(safe(question.getDescription()), 95);
                if (descriptionLines.isEmpty()) {
                    descriptionLines.add("-");
                }
                for (String line : descriptionLines) {
                    if (y < 60) {
                        break;
                    }
                    draw(content, line, margin, y, PDType1Font.HELVETICA, 10);
                    y -= 13;
                }
            }

            document.save(outputPath.toFile());
        }
    }

    private static float drawField(PDPageContentStream content,
                                   String label,
                                   String value,
                                   float x,
                                   float y,
                                   float width) throws IOException {
        draw(content, label + ":", x, y, PDType1Font.HELVETICA_BOLD, 10);
        List<String> lines = wrapText(value, 95);
        if (lines.isEmpty()) {
            lines.add("-");
        }
        float valueX = x + Math.min(110, width * 0.2f);
        float currentY = y;
        for (String line : lines) {
            draw(content, line, valueX, currentY, PDType1Font.HELVETICA, 10);
            currentY -= 13;
        }
        return currentY - 4;
    }

    private static void draw(PDPageContentStream content,
                             String text,
                             float x,
                             float y,
                             PDType1Font font,
                             int size) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
    }

    private static void drawLine(PDPageContentStream content, float x1, float y, float x2) throws IOException {
        content.setLineWidth(0.8f);
        content.moveTo(x1, y);
        content.lineTo(x2, y);
        content.stroke();
    }

    private static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (isBlank(text)) {
            return lines;
        }

        String normalized = text.replace("\r", "");
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (candidate.length() <= maxChars) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                    }
                    if (word.length() <= maxChars) {
                        line.setLength(0);
                        line.append(word);
                    } else {
                        int start = 0;
                        while (start < word.length()) {
                            int end = Math.min(start + maxChars, word.length());
                            lines.add(word.substring(start, end));
                            start = end;
                        }
                        line.setLength(0);
                    }
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }
        return lines;
    }

    private static String humanReadableSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        return String.format("%.2f MB", kb / 1024.0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

