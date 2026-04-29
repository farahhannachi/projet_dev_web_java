package org.example.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.model.ResponseQuestion;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PdfExportUtil {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final float PAGE_MARGIN = 42f;
    private static final float TOP_HEADER_SPACE = 62f;
    private static final float FOOTER_SPACE = 28f;
    private static final float CARD_PADDING = 9f;
    private static final float CARD_GAP = 10f;

    private static final float TITLE_FONT_SIZE = 16f;
    private static final float SUBTITLE_FONT_SIZE = 9.5f;
    private static final float BODY_FONT_SIZE = 10f;
    private static final float META_FONT_SIZE = 9.5f;
    private static final float LINE_HEIGHT = 14f;

    private static final Color COLOR_TEXT = new Color(30, 41, 59);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_ACCENT = new Color(22, 86, 63);
    private static final Color COLOR_CARD_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BORDER = new Color(220, 228, 236);
    private static final Color COLOR_SEPARATOR = new Color(212, 220, 230);

    private PdfExportUtil() {
    }

    public static void exportResponseQuestions(List<ResponseQuestion> responses, Path outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            RenderContext context = startPage(document, 1, responses.size());
            int pageNumber = 1;

            try {
                if (responses == null || responses.isEmpty()) {
                    ensureSpaceFor(context, 70);
                    drawCardBackground(context.stream, context.contentLeft, context.y - 56, context.contentWidth, 56);
                    drawText(context.stream, "Aucune reponse a exporter.", context.contentLeft + CARD_PADDING,
                            context.y - 24, PDType1Font.HELVETICA_BOLD, 11, COLOR_TEXT);
                    drawText(context.stream, "Ajustez les filtres puis relancez l'export.", context.contentLeft + CARD_PADDING,
                            context.y - 40, PDType1Font.HELVETICA, BODY_FONT_SIZE, COLOR_MUTED);
                    context.y -= 68;
                } else {
                    int index = 1;
                    for (ResponseQuestion response : responses) {
                        pageNumber = renderResponseCard(document, context, response, index, pageNumber);
                        index++;
                    }
                }
            } finally {
                closePage(context);
            }

            document.save(outputPath.toFile());
        }
    }

    private static int renderResponseCard(PDDocument document,
                                          RenderContext context,
                                          ResponseQuestion response,
                                          int index,
                                          int pageNumber) throws IOException {
        List<String> questionLines = wrapText(
                "Question: " + defaultValue(response.getQuestionObjet(), "(sans objet)"),
                PDType1Font.HELVETICA,
                BODY_FONT_SIZE,
                context.contentWidth - (CARD_PADDING * 2));
        questionLines = clampLines(questionLines, 3);

        List<String> metaLine1 = wrapText(
                String.format("Auteur: %s   Role: %s   Action: %s",
                        enumLabel(response.getAuteurType()),
                        enumLabel(response.getReponseRole()),
                        enumLabel(response.getActionType())),
                PDType1Font.HELVETICA,
                META_FONT_SIZE,
                context.contentWidth - (CARD_PADDING * 2));
        metaLine1 = clampLines(metaLine1, 2);

        String dateText = response.getCreatedAt() != null ? response.getCreatedAt().format(DATE_FORMAT) : "-";
        List<String> metaLine2 = wrapText(
                String.format("Statut: %s   Lu client: %s   Date: %s",
                        enumLabel(response.getImpactStatut()),
                        response.isLuParClient() ? "Oui" : "Non",
                        dateText),
                PDType1Font.HELVETICA,
                META_FONT_SIZE,
                context.contentWidth - (CARD_PADDING * 2));
        metaLine2 = clampLines(metaLine2, 2);

        List<String> fileLines = new ArrayList<>();
        if (!isBlank(response.getFileName())) {
            String fileSize = response.getFileSize() == null ? "-" : humanReadableSize(response.getFileSize());
            fileLines.addAll(wrapText(
                    String.format("Fichier: %s (%s)", safe(response.getFileName()), fileSize),
                    PDType1Font.HELVETICA,
                    META_FONT_SIZE,
                    context.contentWidth - (CARD_PADDING * 2)));
            fileLines = clampLines(fileLines, 2);
        }

        List<String> responseLines = wrapText(
                defaultValue(response.getReponseText(), "(aucune reponse)"),
                PDType1Font.HELVETICA,
                BODY_FONT_SIZE,
                context.contentWidth - (CARD_PADDING * 2));

        int responseStart = 0;
        boolean firstChunk = true;
        while (responseStart < responseLines.size()) {
            int staticLines = 1 + questionLines.size() + metaLine1.size() + metaLine2.size() + fileLines.size() + 1;
            int maxTextLines = maxLinesForCurrentPage(context);

            if (maxTextLines <= staticLines + 1) {
                closePage(context);
                pageNumber++;
                context.copyFrom(startPage(document, pageNumber, -1));
                continue;
            }

            int chunkSize = Math.min(responseLines.size() - responseStart, maxTextLines - staticLines);
            List<String> responseChunk = responseLines.subList(responseStart, responseStart + chunkSize);

            float lineCount = staticLines + responseChunk.size();
            float cardHeight = CARD_PADDING * 2 + (lineCount * LINE_HEIGHT) + 2;
            ensureSpaceFor(context, cardHeight + CARD_GAP);

            float cardTopY = context.y;
            float cardBottomY = cardTopY - cardHeight;

            drawCardBackground(context.stream, context.contentLeft, cardBottomY, context.contentWidth, cardHeight);

            float textX = context.contentLeft + CARD_PADDING;
            float yCursor = cardTopY - CARD_PADDING - 10;

            String title = firstChunk
                    ? String.format("Reponse #%d  |  Question #%d", response.getId(), response.getQuestionId())
                    : String.format("Reponse #%d  |  Suite", response.getId());
            drawText(context.stream, title, textX, yCursor, PDType1Font.HELVETICA_BOLD, 11, COLOR_TEXT);
            yCursor -= LINE_HEIGHT;

            for (String line : questionLines) {
                drawText(context.stream, line, textX, yCursor, PDType1Font.HELVETICA, BODY_FONT_SIZE, COLOR_TEXT);
                yCursor -= LINE_HEIGHT;
            }
            for (String line : metaLine1) {
                drawText(context.stream, line, textX, yCursor, PDType1Font.HELVETICA, META_FONT_SIZE, COLOR_MUTED);
                yCursor -= LINE_HEIGHT;
            }
            for (String line : metaLine2) {
                drawText(context.stream, line, textX, yCursor, PDType1Font.HELVETICA, META_FONT_SIZE, COLOR_MUTED);
                yCursor -= LINE_HEIGHT;
            }
            for (String line : fileLines) {
                drawText(context.stream, line, textX, yCursor, PDType1Font.HELVETICA, META_FONT_SIZE, COLOR_MUTED);
                yCursor -= LINE_HEIGHT;
            }

            drawText(context.stream, "Reponse:", textX, yCursor, PDType1Font.HELVETICA_BOLD, BODY_FONT_SIZE, COLOR_TEXT);
            yCursor -= LINE_HEIGHT;
            for (String line : responseChunk) {
                drawText(context.stream, line, textX, yCursor, PDType1Font.HELVETICA, BODY_FONT_SIZE, COLOR_TEXT);
                yCursor -= LINE_HEIGHT;
            }

            context.y = cardBottomY - CARD_GAP;
            responseStart += chunkSize;
            firstChunk = false;
        }

        if (responseLines.isEmpty()) {
            // Keep empty-answer entries visible if wrapping produced no lines.
            float cardHeight = CARD_PADDING * 2 + (6 * LINE_HEIGHT);
            ensureSpaceFor(context, cardHeight + CARD_GAP);
            drawCardBackground(context.stream, context.contentLeft, context.y - cardHeight, context.contentWidth, cardHeight);
            drawText(context.stream, String.format("Reponse #%d", response.getId()), context.contentLeft + CARD_PADDING,
                    context.y - CARD_PADDING - 10, PDType1Font.HELVETICA_BOLD, 11, COLOR_TEXT);
            context.y -= (cardHeight + CARD_GAP);
        }

        if (index % 18 == 0) {
            closePage(context);
            pageNumber++;
            context.copyFrom(startPage(document, pageNumber, -1));
        }

        return pageNumber;
    }

    private static RenderContext startPage(PDDocument document, int pageNumber, int totalItems) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float contentLeft = PAGE_MARGIN;
        float contentWidth = pageWidth - (PAGE_MARGIN * 2);
        float topY = pageHeight - PAGE_MARGIN;

        drawText(stream, "CuraVita - Export des reponses", contentLeft, topY,
                PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE, COLOR_TEXT);
        drawText(stream, "Document genere le " + LocalDateTime.now().format(EXPORT_DATE_FORMAT),
                contentLeft, topY - 16,
                PDType1Font.HELVETICA, SUBTITLE_FONT_SIZE, COLOR_MUTED);

        String rightHeader = pageNumber > 0 ? "Page " + pageNumber : "";
        if (totalItems >= 0) {
            rightHeader += "    Total: " + totalItems;
        }
        float rightHeaderWidth = textWidth(rightHeader, PDType1Font.HELVETICA, SUBTITLE_FONT_SIZE);
        drawText(stream, rightHeader, contentLeft + contentWidth - rightHeaderWidth, topY - 16,
                PDType1Font.HELVETICA, SUBTITLE_FONT_SIZE, COLOR_MUTED);

        float separatorY = topY - 26;
        stream.setStrokingColor(COLOR_SEPARATOR);
        stream.setLineWidth(1f);
        stream.moveTo(contentLeft, separatorY);
        stream.lineTo(contentLeft + contentWidth, separatorY);
        stream.stroke();

        return new RenderContext(page, stream, contentLeft, contentWidth, pageHeight - PAGE_MARGIN - TOP_HEADER_SPACE);
    }

    private static void closePage(RenderContext context) throws IOException {
        if (context == null || context.stream == null) {
            return;
        }
        float footerY = PAGE_MARGIN - 8;
        context.stream.setStrokingColor(COLOR_SEPARATOR);
        context.stream.setLineWidth(0.8f);
        context.stream.moveTo(context.contentLeft, footerY + 12);
        context.stream.lineTo(context.contentLeft + context.contentWidth, footerY + 12);
        context.stream.stroke();

        drawText(context.stream, "CuraVita - Support Client", context.contentLeft, footerY,
                PDType1Font.HELVETICA, 8.8f, COLOR_MUTED);
        context.stream.close();
    }

    private static void drawCardBackground(PDPageContentStream stream,
                                           float x,
                                           float y,
                                           float width,
                                           float height) throws IOException {
        stream.setNonStrokingColor(COLOR_CARD_BG);
        stream.addRect(x, y, width, height);
        stream.fill();

        stream.setStrokingColor(COLOR_CARD_BORDER);
        stream.setLineWidth(0.9f);
        stream.addRect(x, y, width, height);
        stream.stroke();

        stream.setNonStrokingColor(COLOR_ACCENT);
        stream.addRect(x, y, 4, height);
        stream.fill();
    }

    private static void drawText(PDPageContentStream stream,
                                 String text,
                                 float x,
                                 float y,
                                 PDFont font,
                                 float fontSize,
                                 Color color) throws IOException {
        stream.beginText();
        stream.setNonStrokingColor(color);
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(safe(text));
        stream.endText();
    }

    private static void ensureSpaceFor(RenderContext context, float heightNeeded) {
        float minY = PAGE_MARGIN + FOOTER_SPACE;
        if (context.y - heightNeeded < minY) {
            context.y = minY;
        }
    }

    private static int maxLinesForCurrentPage(RenderContext context) {
        float freeHeight = context.y - (PAGE_MARGIN + FOOTER_SPACE);
        if (freeHeight <= 0) {
            return 0;
        }
        return (int) Math.floor((freeHeight - (CARD_PADDING * 2)) / LINE_HEIGHT);
    }

    private static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (isBlank(text)) {
            lines.add("");
            return lines;
        }

        String normalized = text.replace("\r", "");
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (current.length() == 0) {
                    current.append(word);
                    continue;
                }

                String candidate = current + " " + word;
                if (textWidth(candidate, font, fontSize) <= maxWidth) {
                    current.append(' ').append(word);
                } else {
                    lines.add(current.toString());
                    if (textWidth(word, font, fontSize) <= maxWidth) {
                        current = new StringBuilder(word);
                    } else {
                        List<String> hardWrapped = wrapLongToken(word, font, fontSize, maxWidth);
                        lines.addAll(hardWrapped.subList(0, Math.max(0, hardWrapped.size() - 1)));
                        current = new StringBuilder(hardWrapped.get(hardWrapped.size() - 1));
                    }
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private static List<String> wrapLongToken(String token, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            current.append(token.charAt(i));
            if (textWidth(current.toString(), font, fontSize) > maxWidth) {
                if (current.length() == 1) {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    char overflow = current.charAt(current.length() - 1);
                    current.deleteCharAt(current.length() - 1);
                    result.add(current.toString());
                    current.setLength(0);
                    current.append(overflow);
                }
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private static float textWidth(String text, PDFont font, float fontSize) throws IOException {
        return (font.getStringWidth(safe(text)) / 1000f) * fontSize;
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

    private static List<String> clampLines(List<String> lines, int maxLines) {
        if (lines.size() <= maxLines || maxLines <= 0) {
            return lines;
        }
        List<String> out = new ArrayList<>(lines.subList(0, maxLines));
        String last = out.get(maxLines - 1);
        out.set(maxLines - 1, truncateWithEllipsis(last, 90));
        return out;
    }

    private static String truncateWithEllipsis(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, Math.max(0, limit - 3)) + "...";
    }

    private static String enumLabel(Object enumValue) {
        if (enumValue == null) {
            return "-";
        }
        try {
            return safe((String) enumValue.getClass().getMethod("getLabel").invoke(enumValue));
        } catch (Exception ignored) {
            return safe(enumValue.toString());
        }
    }

    private static String defaultValue(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class RenderContext {
        private PDPage page;
        private PDPageContentStream stream;
        private float contentLeft;
        private float contentWidth;
        private float y;

        private RenderContext(PDPage page, PDPageContentStream stream, float contentLeft, float contentWidth, float y) {
            this.page = page;
            this.stream = stream;
            this.contentLeft = contentLeft;
            this.contentWidth = contentWidth;
            this.y = y;
        }

        private void copyFrom(RenderContext other) {
            this.page = other.page;
            this.stream = other.stream;
            this.contentLeft = other.contentLeft;
            this.contentWidth = other.contentWidth;
            this.y = other.y;
        }
    }
}
