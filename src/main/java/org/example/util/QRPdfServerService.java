package org.example.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Génère un PDF complet pour une ordonnance et le sert via un mini serveur HTTP local.
 * Le QR code encode l'URL http://IP:PORT/ordonnance_XXX.pdf
 * → Scannable par n'importe quel téléphone sans application dédiée.
 */
public class QRPdfServerService {

    private static QRPdfServerService instance;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private static final int PORT = 8765;
    private Path tempDir;

    private QRPdfServerService() {
        try {
            tempDir = Files.createTempDirectory("curavita_qr");
            startServer();
        } catch (Exception e) {
            System.err.println("[QRServer] Erreur init : " + e.getMessage());
        }
    }

    public static QRPdfServerService getInstance() {
        if (instance == null) instance = new QRPdfServerService();
        return instance;
    }

    /** Retourne la vraie IP locale (WiFi/Ethernet), en évitant les adresses APIPA et loopback */
    public String getLocalIP() {
        try {
            // Priorité : IP obtenue en se "connectant" à un DNS public (pas de trafic réel)
            try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                socket.connect(InetAddress.getByName("8.8.8.8"), 80);
                String ip = socket.getLocalAddress().getHostAddress();
                if (ip != null && !ip.startsWith("169.254") && !ip.equals("0.0.0.0")) {
                    return ip;
                }
            } catch (Exception ignored) {}

            // Fallback : parcourir les interfaces réseau
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                java.util.Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    // Garder uniquement IPv4, pas loopback, pas APIPA
                    if (addr instanceof java.net.Inet4Address
                            && !addr.isLoopbackAddress()
                            && !ip.startsWith("169.254")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[QRServer] Erreur détection IP : " + e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Génère le PDF et retourne l'URL à encoder dans le QR code.
     */
    public String genererPdfEtGetUrl(String numeroOrdonnance, String patient,
                                      List<QRCodeService.TraitementInfo> traitements) throws Exception {
        String nomFichier = "ordonnance_" + numeroOrdonnance.replaceAll("[^a-zA-Z0-9\\-]", "_") + ".pdf";
        Path pdfPath = tempDir.resolve(nomFichier);
        genererPdf(pdfPath.toFile(), numeroOrdonnance, patient, traitements);
        return "http://" + getLocalIP() + ":" + PORT + "/" + nomFichier;
    }

    /** Génère le PDF complet avec instructions + images de sensibilisation */
    private void genererPdf(File file, String numeroOrdonnance, String patient,
                             List<QRCodeService.TraitementInfo> traitements) throws Exception {
        DeviceRgb green     = new DeviceRgb(31, 111, 92);
        DeviceRgb lightGreen = new DeviceRgb(240, 247, 244);
        DeviceRgb orange    = new DeviceRgb(230, 126, 34);
        DeviceRgb lightOrange = new DeviceRgb(255, 248, 225);
        DeviceRgb blue      = new DeviceRgb(41, 128, 185);
        DeviceRgb lightBlue = new DeviceRgb(234, 244, 251);
        DeviceRgb gray      = new DeviceRgb(108, 117, 125);

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);
        doc.setMargins(30, 40, 30, 40);

        // ── En-tête ──────────────────────────────────────────────────────
        Paragraph titre = new Paragraph("ORDONNANCE MÉDICALE — CuraVita")
            .setFontSize(20).setBold().setFontColor(green)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4);
        doc.add(titre);

        Paragraph sousTitre = new Paragraph("Instructions de traitement — Scanné le " + LocalDate.now())
            .setFontSize(10).setFontColor(gray)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16);
        doc.add(sousTitre);

        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f)).setMarginBottom(16));

        // ── Infos ordonnance ─────────────────────────────────────────────
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
            .useAllAvailableWidth().setMarginBottom(20);
        addInfoRow(infoTable, "Numéro", numeroOrdonnance, green);
        addInfoRow(infoTable, "Patient", patient, green);
        addInfoRow(infoTable, "Date", LocalDate.now().toString(), green);
        doc.add(infoTable);

        // ── Traitements ──────────────────────────────────────────────────
        doc.add(new Paragraph("💊 Instructions de traitement")
            .setFontSize(14).setBold().setFontColor(green).setMarginBottom(10));

        if (traitements.isEmpty()) {
            doc.add(new Paragraph("Aucun traitement associé.")
                .setFontColor(gray).setItalic());
        } else {
            for (int i = 0; i < traitements.size(); i++) {
                QRCodeService.TraitementInfo t = traitements.get(i);
                Table traitTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth().setMarginBottom(12);

                // En-tête produit
                Cell headerCell = new Cell(1, 2)
                    .add(new Paragraph((i + 1) + ".  " + t.produit)
                        .setBold().setFontSize(13).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(green).setPadding(8)
                    .setBorder(Border.NO_BORDER);
                traitTable.addCell(headerCell);

                if (t.dosage != null && !t.dosage.isBlank())
                    addTraitRow(traitTable, "Dosage", t.dosage, lightGreen);
                if (t.frequence != null && !t.frequence.isBlank())
                    addTraitRow(traitTable, "Fréquence", t.frequence, ColorConstants.WHITE);
                if (t.repas != null && !t.repas.isBlank())
                    addTraitRow(traitTable, "Repas", t.repas, lightGreen);
                if (t.dureeJours > 0)
                    addTraitRow(traitTable, "Durée", t.dureeJours + " jours", ColorConstants.WHITE);

                doc.add(traitTable);
            }
        }

        // ── Sensibilisation dosage ────────────────────────────────────────
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)).setMarginTop(10).setMarginBottom(14));

        Table sensiTable = new Table(UnitValue.createPercentArray(new float[]{1}))
            .useAllAvailableWidth().setMarginBottom(16);
        Cell sensiCell = new Cell()
            .add(new Paragraph("⚠  Respectez toujours le dosage prescrit")
                .setBold().setFontSize(13).setFontColor(orange))
            .add(new Paragraph(
                "Ne dépassez jamais la dose recommandée, même en cas de douleur persistante. " +
                "Un surdosage peut entraîner des effets graves sur le foie, les reins ou le cœur. " +
                "En cas de doute, consultez votre médecin ou pharmacien avant toute prise.")
                .setFontSize(10).setFontColor(new DeviceRgb(127, 82, 0)))
            .setBackgroundColor(lightOrange)
            .setPadding(12).setBorder(Border.NO_BORDER)
            .setBorderLeft(new com.itextpdf.layout.borders.SolidBorder(orange, 4));
        sensiTable.addCell(sensiCell);
        doc.add(sensiTable);

        // ── Images de sensibilisation ─────────────────────────────────────
        doc.add(new Paragraph("📋 Bons conseils sur la prise des médicaments")
            .setFontSize(13).setBold().setFontColor(blue).setMarginBottom(8));

        // Charger les images depuis les ressources
        String[] imagePaths = {
            "/images/conseils_medicaments.png",
            "/images/sensibilisation_pharmacie.png"
        };

        Table imgTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth().setMarginBottom(16);

        boolean hasImages = false;
        for (String imgPath : imagePaths) {
            try (InputStream is = QRPdfServerService.class.getResourceAsStream(imgPath)) {
                if (is != null) {
                    byte[] imgBytes = is.readAllBytes();
                    com.itextpdf.layout.element.Image img =
                        new com.itextpdf.layout.element.Image(ImageDataFactory.create(imgBytes))
                            .setWidth(UnitValue.createPercentValue(95))
                            .setAutoScale(true);
                    imgTable.addCell(new Cell().add(img)
                        .setBorder(Border.NO_BORDER).setPadding(4));
                    hasImages = true;
                }
            } catch (Exception ignored) {}
        }

        if (hasImages) {
            doc.add(imgTable);
        } else {
            // Fallback texte si images non disponibles
            Table conseilsTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth().setMarginBottom(16);
            String[] conseils = {
                "1. Boire un volume d'eau suffisant avec vos médicaments",
                "2. Prendre ses médicaments à la même heure chaque jour",
                "3. Bien se renseigner sur l'arrêt d'un traitement",
                "4. Connaître la posologie (avant ou après manger)",
                "5. Prendre conscience du risque d'allergie",
                "6. Attention avec l'alcool",
                "7. Bien conserver ses médicaments"
            };
            for (String conseil : conseils) {
                conseilsTable.addCell(new Cell()
                    .add(new Paragraph(conseil).setFontSize(11).setFontColor(new DeviceRgb(44, 62, 80)))
                    .setBackgroundColor(lightBlue).setPadding(8)
                    .setBorder(Border.NO_BORDER)
                    .setBorderLeft(new com.itextpdf.layout.borders.SolidBorder(blue, 3))
                    .setMarginBottom(4));
            }
            doc.add(conseilsTable);
        }

        // ── Pied de page ─────────────────────────────────────────────────
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)).setMarginTop(8));
        doc.add(new Paragraph("Document généré le " + LocalDate.now() + " par CuraVita — Votre santé, notre priorité.")
            .setFontSize(9).setFontColor(gray)
            .setTextAlignment(TextAlignment.CENTER).setMarginTop(8));

        doc.close();
    }

    private void addInfoRow(Table table, String label, String value, DeviceRgb labelColor) {
        table.addCell(new Cell()
            .add(new Paragraph(label).setBold().setFontColor(labelColor).setFontSize(11))
            .setPadding(6).setBorder(Border.NO_BORDER));
        table.addCell(new Cell()
            .add(new Paragraph(value != null ? value : "-").setFontSize(11))
            .setPadding(6).setBorder(Border.NO_BORDER));
    }

    private void addTraitRow(Table table, String label, String value,
                              com.itextpdf.kernel.colors.Color bg) {
        table.addCell(new Cell()
            .add(new Paragraph(label).setBold().setFontSize(11))
            .setBackgroundColor(bg).setPadding(6));
        table.addCell(new Cell()
            .add(new Paragraph(value != null ? value : "-").setFontSize(11))
            .setBackgroundColor(bg).setPadding(6));
    }

    /** Démarre le mini serveur HTTP qui sert les PDFs générés */
    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "QRServer");
                t.setDaemon(true);
                return t;
            });
            executor.submit(() -> {
                System.out.println("[QRServer] Serveur démarré sur port " + PORT);
                while (!serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.submit(() -> handleRequest(client));
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception e) {
            System.err.println("[QRServer] Impossible de démarrer le serveur : " + e.getMessage());
        }
    }

    /** Gère une requête HTTP simple — sert le fichier PDF demandé */
    private void handleRequest(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) return;

            // Extraire le nom du fichier : "GET /ordonnance_XXX.pdf HTTP/1.1"
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String fileName = parts[1].substring(1); // retirer le "/"

            Path filePath = tempDir.resolve(fileName);
            System.out.println("[QRServer] Requête : " + fileName + " → " + filePath);
            if (!Files.exists(filePath) || !fileName.endsWith(".pdf")) {
                System.err.println("[QRServer] Fichier introuvable : " + filePath);
                String notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 9\r\n\r\nNot Found";
                out.write(notFound.getBytes());
                return;
            }

            byte[] pdfBytes = Files.readAllBytes(filePath);
            String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/pdf\r\n" +
                "Content-Disposition: inline; filename=\"" + fileName + "\"\r\n" +
                "Content-Length: " + pdfBytes.length + "\r\n" +
                "Connection: close\r\n\r\n";
            out.write(headers.getBytes());
            out.write(pdfBytes);
            out.flush();
            System.out.println("[QRServer] PDF servi : " + fileName);
        } catch (Exception e) {
            System.err.println("[QRServer] Erreur requête : " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) serverSocket.close();
            if (executor != null) executor.shutdownNow();
        } catch (Exception ignored) {}
    }
}
