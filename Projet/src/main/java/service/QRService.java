package service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class QRService {

    private static QRService instance;

    private QRService() {
        // Singleton pattern
    }

    public static QRService getInstance() {
        if (instance == null) {
            instance = new QRService();
        }
        return instance;
    }

    public void generateQRCode(String data, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    public String generateServiceQRCodeWithInfo(int id, String nom, String type, String specialite) throws WriterException, IOException {
        String data = String.format("id:%d|nom:%s|type:%s|specialite:%s", id, nom, type, specialite);
        String filePath = String.format("qr_codes/service_%d.png", id);
        generateQRCode(data, filePath);
        return filePath;
    }
}
