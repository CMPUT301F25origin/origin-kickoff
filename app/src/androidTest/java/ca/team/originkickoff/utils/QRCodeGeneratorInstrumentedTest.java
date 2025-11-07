package ca.team.originkickoff.utils;

import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class QRCodeGeneratorInstrumentedTest {

    @Test
    public void testGenerateQRCodeNull() {
        assertNull(QRCodeGenerator.generateQRCode(null));
    }

    @Test
    public void testGenerateQRCodeEmpty() {
        assertNull(QRCodeGenerator.generateQRCode(""));
    }

    @Test
    public void testGenerateQRCodeValid() {
        Bitmap bmp = QRCodeGenerator.generateQRCode("event-123");
        assertNotNull(bmp);
        assertEquals(512, bmp.getWidth());
        assertEquals(512, bmp.getHeight());
    }

    @Test
    public void testBitmapToByteArray() {
        Bitmap bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565);
        byte[] data = QRCodeGenerator.bitmapToByteArray(bmp);
        assertNotNull(data);
        assertTrue(data.length > 0);
    }

    @Test
    public void testBitmapToByteArrayNull() {
        assertNull(QRCodeGenerator.bitmapToByteArray(null));
    }
}
