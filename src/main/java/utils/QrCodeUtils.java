package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/** Génération d’un QR Code standard (scannable) -> Image JavaFX. */
public final class QrCodeUtils {
    private QrCodeUtils() {}

    public static Image toQrImage(String text, int size) {
        if (text == null) text = "";

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            int on = 0x10121C;
            int off = 0xFFFFFF;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    img.setRGB(x, y, matrix.get(x, y) ? on : off);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return new Image(new ByteArrayInputStream(out.toByteArray()));

        } catch (Exception e) {
            throw new RuntimeException("QR generation failed: " + e.getMessage(), e);
        }
    }
}
