package ca.team.originkickoff.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;

/**
 * Utility class for generating QR codes
 */
public class QRCodeGenerator {
    private static final int QR_CODE_SIZE = 512; // Size in pixels

    /**
     * Generates a QR code bitmap from the given content
     *
     * @param content The content to encode in the QR code (e.g., event ID)
     * @return Bitmap of the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a Bitmap to a byte array for uploading to Firebase Storage
     *
     * @param bitmap The bitmap to convert
     * @return byte array of the bitmap in PNG format
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}

